/********************************************************************************/
/*										*/
/*		DymemHistory.java						*/
/*										*/
/*	Maintain the set of heap dumps for a process				*/
/*										*/
/********************************************************************************/
/*	Copyright 2005 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2005, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymem/DymemHistory.java,v 1.5 2012-10-05 00:52:50 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymemHistory.java,v $
 * Revision 1.5  2012-10-05 00:52:50  spr
 * Code clean up.
 *
 * Revision 1.4  2009-10-07 01:00:02  spr
 * Code cleanup for eclipse. Add support for standalone version.
 *
 * Revision 1.3  2009-06-04 18:53:35  spr
 * Clean up memory display.
 *
 * Revision 1.2  2009-04-28 18:00:57  spr
 * Update visualization with data panel.
 *
 * Revision 1.1  2009-04-11 23:45:29  spr
 * Provide updated visualizations with time line, hashing, etc.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymem;


import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlReader;

import org.w3c.dom.Element;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;



public class DymemHistory implements DymemConstants
{




/********************************************************************************/
/*										*/
/*	Local Storage								*/
/*										*/
/********************************************************************************/

private String		process_id;
private File		heap_file;
private FileInputStream file_stream;
private SortedMap<Long,HeapEntry> time_map;
private long		last_position;
private HeapEntry	active_entry;
private DymemGraph	current_graph;
private DymemGraph	previous_graph;
private HeapListener	heap_listener;

private DymemCycleNamer cycle_namer;
private DymemParameters param_values;
private DymemStats	stat_values;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DymemHistory(String pid,DymemParameters dp,HeapListener hl) throws DymemException
{
   process_id = pid;
   heap_listener = hl;
   param_values = dp;

   try {
      heap_file = new File(DYMEM_HEAP_PREFIX + process_id + ".trace");
      file_stream = new FileInputStream(heap_file);
    }
   catch (IOException e) {
      throw new DymemException("No heap trace file",e);
    }

   last_position = 0;
   time_map = new TreeMap<Long,HeapEntry>();
   active_entry = null;
   current_graph = null;
   previous_graph = null;

   cycle_namer = new DymemCycleNamer();
   stat_values = new DymemStats();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void start()
{
   StartUp su = new StartUp();
   su.start();
}




private class StartUp extends Thread {

   public void run() {
      try {
	 checkFile();
       }
      catch (IOException e) {
	 System.err.println("DYMEM: Problem reading memory dump: " + e);
       }

      javax.swing.Timer t = new javax.swing.Timer(DYMEM_CHECK_EVERY,new FileChecker());
      t.start();
    }

}	// end of subclass Startup




/********************************************************************************/
/*										*/
/*	Methods to read heap file as it is updated				*/
/*										*/
/********************************************************************************/

private void checkFile() throws IOException
{
   List<HeapEntry> newents = new ArrayList<HeapEntry>();
   HeapEntry prior = null;

   FileChannel fc = file_stream.getChannel();
   while (fc.size() > last_position + 10) {
      HeapEntry he = new HeapEntry(last_position);
      long at = he.getAtTime();
      long elen = he.getEntryLength();
      last_position += elen;
      Element e = he.getHeapData();
      if (at != 0 && IvyXml.isElement(e,"HEAP_MODEL")) {
	 time_map.put(at,he);
	 newents.add(he);
	 stat_values.accumulate(at,e);
	 DymemGraph dg = new DymemGraph(e,stat_values,param_values,cycle_namer);
	 int ivl = stat_values.getIntervalNumber(at);
	 for (GraphNode g : dg.getNodeList(OutputCompareBy.TOTAL_SIZE)) {
	    stat_values.saveData(ivl,g);
	  }
	 if (heap_listener != null) heap_listener.heapDumpTime(at);
	 // System.err.println("DYMEM: Add new entry at " + he.getAtTime());
	 if (prior != null && prior != active_entry) prior.clear();
	 prior = he;
       }
      else if (IvyXml.isElement(e,"MEMORY_USAGE")) {
	 if (heap_listener != null) heap_listener.memoryUsageUpdate(e);
       }
    }

   for (HeapEntry he : newents) {
      if (he != active_entry) he.clear();
    }

   if (prior != null && heap_listener != null) heap_listener.heapUpdated(prior.getAtTime());

   if (param_values.getStatWriter() != null) param_values.getStatWriter().flush();
}



private class FileChecker implements ActionListener {

   public void actionPerformed(ActionEvent ae) {
      javax.swing.Timer timer = (javax.swing.Timer) ae.getSource();
      try {
	 checkFile();
       }
      catch (IOException e) {
	 timer.stop();
       }
      catch (Throwable t) {
	 System.err.println("DYMEM: Problem reading file: " + t);
	 t.printStackTrace();
	 timer.stop();
       }
    }

}	// end of subclass FileChecker




/********************************************************************************/
/*										*/
/*	Access methods: time <= 0 implies latest time				*/
/*										*/
/********************************************************************************/

DymemGraph getCurrentGraph(long time)
{
   setActiveEntry(time);

   if (current_graph == null && active_entry != null) {
      try {
	 Element helt = active_entry.getHeapData();
	 current_graph = new DymemGraph(helt,stat_values,param_values,cycle_namer);
       }
      catch (IOException e) {
	 System.err.println("DYMEM: Problem rereading heap data: " + e);
	 System.exit(1);
       }
    }

   return current_graph;
}



DymemGraph getPreviousGraph(long time)
{
   setActiveEntry(time);

   if (previous_graph == null && active_entry != null) {
      try {
	 SortedMap<Long,HeapEntry> hmap = time_map.headMap(active_entry.getAtTime());
	 if (!hmap.isEmpty()) {
	    HeapEntry pent = time_map.get(hmap.lastKey());
	    Element helt = pent.getHeapData();
	    previous_graph = new DymemGraph(helt,stat_values,param_values,cycle_namer);
	    pent.clear();
	  }
       }
      catch (IOException e) {
	 System.err.println("DYMEM: Problem rereading heap data: " + e);
	 System.exit(1);
       }
    }

   return previous_graph;
}



private void setActiveEntry(long time)
{
   HeapEntry nent = null;

   if (!time_map.isEmpty()) {
      long when = 0;
      if (time <= 0) when = time_map.lastKey();
      else {
	 SortedMap<Long,HeapEntry> sm = time_map.headMap(time+1);
	 if (!sm.isEmpty()) when = sm.lastKey();
       }
      if (when != 0) nent = time_map.get(when);
    }

   if (active_entry != nent) {
      previous_graph = null;
      if (active_entry != null && current_graph != null) {
	 try {
	    long ptime = active_entry.getAtTime();
	    SortedMap<Long,HeapEntry> sm = time_map.tailMap(ptime+1);
	    if (!sm.isEmpty() && sm.get(sm.firstKey()) == nent) previous_graph = current_graph;
	  }
	 catch (IOException e) { }
       }
      current_graph = null;

      if (active_entry != null) active_entry.clear();
      active_entry = nent;
    }
}






/********************************************************************************/
/*										*/
/*	HeapEntry class: information for a heap file entry			*/
/*										*/
/********************************************************************************/

private class HeapEntry {

   private long at_time;
   private long file_position;
   private Element heap_data;
   private long entry_length;

   HeapEntry(long pos) {
      file_position = pos;
      heap_data = null;
      at_time = 0;
      entry_length = 0;
    }

   void clear() 				{ heap_data = null; }

   Element getHeapData() throws IOException {
      if (heap_data == null) {
         FileChannel fc = file_stream.getChannel();
         fc.position(file_position);
         
	 @SuppressWarnings("resource") IvyXmlReader hr = new IvyXmlReader(file_stream);
         String s = hr.readXml();
         if (s == null) return null;
         entry_length = s.length();
         heap_data = IvyXml.convertStringToXml(s);
         at_time = IvyXml.getAttrLong(heap_data,"NOW");
       }
      return heap_data;
    }

   long getAtTime() throws IOException {
      if (at_time == 0) getHeapData();
      return at_time;
    }

   long getEntryLength() throws IOException {
      if (entry_length == 0) getHeapData();
      return entry_length;
    }

}	// end of subclass HeapEntry




}	// end of class DymemHistory




/* end of DymemHistory.java */
