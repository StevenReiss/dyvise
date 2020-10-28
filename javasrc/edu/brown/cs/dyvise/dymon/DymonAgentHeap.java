/********************************************************************************/
/*										*/
/*		DymonAgentHeap.java						*/
/*										*/
/*	DYPER monitor agent for analyzing the heap				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymon/DymonAgentHeap.java,v 1.11 2012-10-05 00:52:56 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymonAgentHeap.java,v $
 * Revision 1.11  2012-10-05 00:52:56  spr
 * Code clean up.
 *
 * Revision 1.10  2010-03-30 16:22:05  spr
 * Clean up the code.
 *
 * Revision 1.9  2009-09-19 00:09:46  spr
 * Update dymon for seletive agent insertion and removal.
 *
 * Revision 1.8  2009-06-04 18:53:50  spr
 * Set up for binary distribution.
 *
 * Revision 1.7  2009-05-01 23:15:12  spr
 * Fix up state computation.  Clean up code.
 *
 * Revision 1.6  2009-04-20 23:23:30  spr
 * Updates to make things work on the mac.  Fix bug in dymti.
 *
 * Revision 1.5  2009-04-12 02:11:34  spr
 * Change .so to .jnilib for the mac.
 *
 * Revision 1.4  2009-04-11 23:46:18  spr
 * Use IvyFormat rather than local code.
 *
 * Revision 1.3  2009-03-20 02:06:50  spr
 * Update dymon: add time-based information, updated memory, etc.
 *
 * Revision 1.2  2008-11-12 14:10:44  spr
 * Various efficiency and bug fixups.  Readiness for immediate output.
 *
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymon;


import edu.brown.cs.ivy.file.IvyFormat;
import edu.brown.cs.ivy.mint.*;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.io.*;
import java.util.*;


class DymonAgentHeap extends DymonAgent implements MintConstants {




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private Map<String,HeapData> heap_map;


private long	processing_time = 2000;
private long	total_size;
private long	total_count;
private long	object_count;
private DymonDetailing heap_detailing;
private PatchRequest patch_request;

private boolean   no_history;
private int	  history_count;
private double [] mem_history;
private double [] gc_history;

private double	count_threshold = 0;
private double	size_threshold = 0.01;

private double	count_sum_threshold = 0;
private double	size_sum_threshold = 0.05;

private int	init_state;
private MintDefaultReply ping_handler;

private boolean save_opened;
private IvyXmlWriter save_writer;

private static final int	HISTORY_SIZE = 50;
private static final double	GC_MAX = 5;
private static final double	MEM_MAX = 1E8;

private boolean dump_heap;

private final static String DYMON_VJMTI_HEAP_AGENT = "$(DYVISE)/lib/$(BROWN_DYVISE_JAVA_ARCH)/libdymti$(SHARED_LIB_EXT)";




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DymonAgentHeap(DymonMain dm,DymonProcess dp)
{
   super(dm,dp);

   heap_map = new HashMap<String,HeapData>();
   total_size = 0;
   total_count = 0;
   object_count = 0;
   patch_request = new PatchRequest();

   init_state = -1;
   ping_handler = new MintDefaultReply();
   if (for_process != null) {
      for_process.sendMessage("DYMTI","PING",null,ping_handler,MINT_MSG_FIRST_NON_NULL);
    }

   mem_history = new double[HISTORY_SIZE];
   gc_history = new double[HISTORY_SIZE];
   history_count = 0;
   no_history = false;

   save_writer = null;
   save_opened = false;

   heap_detailing = new Detailing();

   dump_heap = System.getenv("BROWN_DYMON_DUMP_HEAP") != null;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getName()			{ return "HEAP"; }


@Override public String getDyperAgentName()		{ return "MEMORY"; }



@Override protected String getMonitorClass()
{
   return "edu.brown.cs.dyvise.dyper.DyperAgentMemory";
}



/********************************************************************************/
/*										*/
/*	Clear methods								*/
/*										*/
/********************************************************************************/

@Override public void doClear()
{
   heap_map.clear();
   total_size = 0;
   total_count = 0;
   object_count = 0;

   heap_detailing.doClear();
}



@Override public void noteDead()
{
   if (save_writer != null) {
      save_writer.close();
      save_writer = null;
    }

   super.noteDead();
}



@Override protected void handleStart(DymonProcess dp)
{
   doSave();
}



/********************************************************************************/
/*										*/
/*	Report handling methods 						*/
/*										*/
/********************************************************************************/

@Override public void handleReport(Element r)
{
   long now = IvyXml.getAttrLong(r,"TIME");

   Element ce = IvyXml.getElementByTag(r,"MEMORY");

   if (ce == null) {
      no_history = true;
      return;
    }

   no_history = false;

   long totused = 0;
   for (Element e : IvyXml.elementsByTag(ce,"USAGE")) {
      String nm = IvyXml.getAttrString(e,"NAME");
      if (nm.equals("HEAP")) {
	 totused += IvyXml.getAttrDouble(e,"LUSED");
	 break;
       }
    }
   long gccount = 0;
   for (Element e : IvyXml.elementsByTag(ce,"GC")) {
      gccount += IvyXml.getAttrDouble(e,"COUNT");
    }

   synchronized (this) {
      ++history_count;
      int i = (history_count % HISTORY_SIZE);
      mem_history[i] = totused;
      gc_history[i] = gccount;
    }

   addDelta(new MemoryDelta(now,totused,gccount));
}



private void processResult(Element xml)
{
   if (xml == null) {
      System.err.println("DYMON: HEAP: no data returned from heap request");
      return;
    }

   dumpDeltas();
   if (doSave()) {
      synchronized (save_writer) {
	 save_writer.writeXml(xml);
	 save_writer.write("\n");
	 if (save_writer.checkError()) {
	    System.err.println("DYMON: Problem saving allocation report data");
	    save_writer = null;
	  }
       }
    }

   long t = IvyXml.getAttrLong(xml,"TIME");
   if (t > processing_time) processing_time = t;
   else processing_time = (processing_time + t)/2;

   long now = IvyXml.getAttrLong(xml,"NOW");

   object_count = IvyXml.getAttrLong(xml,"COUNT");

   synchronized (heap_map) {
      total_size = 0;
      total_count = 0;
      for (Element e : IvyXml.elementsByTag(xml,"CLASS")) {
	 String c = IvyXml.getAttrString(e,"NAME");
	 long ct = IvyXml.getAttrLong(e,"COUNT");
	 long sz = IvyXml.getAttrLong(e,"SIZE");
	 HeapData hd = heap_map.get(c);
	 if (hd == null) {
	    hd = new HeapData(c);
	    heap_map.put(c,hd);
	  }
	 hd.update(ct,sz);
	 total_size += sz;
	 ++total_count;
       }
    }

   for_process.sendMarkMessage("HEAP",now);
}



/********************************************************************************/
/*										*/
/*	Analysis methods							*/
/*										*/
/********************************************************************************/

@Override public void outputAnalysis(IvyXmlWriter xw)
{
   xw.begin("HEAP");

   synchronized (heap_map) {
      long minct = (long)(count_threshold * total_count);
      long minsz = (long)(size_threshold * total_size);
      xw.field("SIZE",total_size);
      xw.field("COUNT",total_count);
      xw.field("OBJECTS",object_count);
      Set<HeapData> rslt = new TreeSet<HeapData>(heap_map.values());
      for (HeapData hd : rslt) {
	 hd.output(xw,total_size,minct,minsz);
       }
    }

   xw.end("HEAP");

   dumpDeltas();
}



private void dumpDeltas()
{
   if (doSave()) {
      synchronized (save_writer) {
	 processDeltas(save_writer);
	 if (save_writer.checkError()) {
	    System.err.println("DYMON: Problem saving allocation report data");
	    save_writer = null;
	  }
       }
    }
}




/********************************************************************************/
/*										*/
/*	Summary methods 							*/
/*										*/
/********************************************************************************/

@Override public double getSummaryValue()	{ return getAgentPriority(); }

@Override public double getConfidence()
{
   if (heap_map.size() == 0) return 0.1;
   return 1;
}



@Override public void outputSummary(IvyXmlWriter xw)
{
   if (total_size == 0) return;

   xw.begin("METER");
   xw.field("NAME","HEAP USED");
   xw.field("VALUE",total_size);
   xw.field("TYPE","MEMORY");
   xw.end("METER");

   xw.begin("METER");
   xw.field("NAME","OBJECT COUNT");
   xw.field("VALUE",object_count);
   xw.field("TYPE","MEMORY");
   xw.end("METER");

   xw.begin("BARGRAPH");
   xw.field("NAME","% HEAP USAGE");
   xw.field("TOTAL",1.0);
   xw.field("TYPE","PERCENT");
   synchronized (heap_map) {
      long minct = (long)(count_sum_threshold * total_count);
      long minsz = (long)(size_sum_threshold * total_size);
      for (HeapData hd : heap_map.values()) {
	 hd.outputSummary(xw,total_size,minct,minsz);
       }
    }
   xw.end("BARGRAPH");
}



/********************************************************************************/
/*										*/
/*	Naming methods								*/
/*										*/
/********************************************************************************/

private static String convertClassName(String sgn)
{
   if (sgn.startsWith("CLASS*")) {
      String sfx = convertClassName(sgn.substring(6));
      return "CLASS " + sfx;
    }
   else if (sgn.startsWith("THREAD*")) {
      return sgn;
    }

   try {
      sgn = getTypeFromStream(new StringReader(sgn));
    }
   catch (IOException e) {
      System.err.println("DYMON: I/O exception reading from string: " + e);
    }

   return sgn;
}


private static String getTypeFromStream(Reader sgn) throws IOException
{
   String jt = null;

   switch (sgn.read()) {
      default :
      case ')' :
      case 'A' :
	 break;
      case 'B' :
	 jt = "byte";
	 break;
      case 'C' :
	 jt = "char";
	 break;
      case 'F' :
	 jt = "float";
	 break;
      case 'D' :
	 jt = "double";
	 break;
      case 'I' :
	 jt = "int";
	 break;
      case 'J' :
	 jt = "long";
	 break;
      case 'S' :
	 jt = "short";
	 break;
      case 'V' :
	 jt = "void";
	 break;
      case 'Z' :
	 jt = "boolean";
	 break;
      case '[' :
	 jt = getTypeFromStream(sgn) + "[]";
	 break;
      case 'L' :
	 { StringBuffer buf = new StringBuffer();
	   for ( ; ; ) {
	      int ch = sgn.read();
	      if (ch <= 0 || ch == ';') break;
	      if (ch == '/' || ch == '\\') ch = '.';
	      buf.append((char) ch);
	    }
	   jt = buf.toString();
	  }
	 break;
      case '(' :
	 { StringBuffer args = new StringBuffer();
	   for ( ; ; ) {
	      jt = getTypeFromStream(sgn);
	      if (jt == null) break;			  // ')'
	      if (args.length() > 0) args.append(",");
	      args.append(jt);
	    }
	   jt = getTypeFromStream(sgn) + "(" + args + ")";
	  }
	 break;
      case '*' :
	 int ch = sgn.read();
	 sgn.mark(10);
	 while (Character.isDigit(ch)) {
	    sgn.mark(10);
	    ch = sgn.read();
	  }
	 sgn.reset();
	 return getTypeFromStream(sgn);
    }

   return jt;
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

boolean doSave()
{
   if (!save_opened) {
      try {
	 File f = new File(DYMON_HEAP_PREFIX + for_process.getProcessId() + ".trace");
	 save_writer = new IvyXmlWriter(f);
	 System.err.println("DYMON: HEAP reports saved in: " + f);
       }
      catch (IOException e) {
	 save_writer = null;
       }
      save_opened = true;
    }

   return save_writer != null;
}




/********************************************************************************/
/*										*/
/*	Detailing methods							*/
/*										*/
/********************************************************************************/

public Collection<DymonDetailing> getDetailings()
{
   Collection<DymonDetailing> r = new ArrayList<DymonDetailing>();
   r.add(heap_detailing);
   return r;
}



private double getAgentPriority()
{
   if (no_history) return 1.0;

   if (history_count <= HISTORY_SIZE) return 0;

   double dmem,dgc,tmem;

   synchronized (this) {
      int i = history_count % HISTORY_SIZE;
      int j = (i + HISTORY_SIZE-1) % HISTORY_SIZE;
      dmem = Math.abs(mem_history[i] - mem_history[j]);
      dgc = gc_history[i] - gc_history[j];
      if (dgc < 0) {
	 System.err.println("DYMON: GC history is odd: " + i + " " + j + " " +
			       gc_history[i] + " " + gc_history[j] + " " + history_count);
	 dgc = 0;
       }
      tmem = mem_history[i];
    }

   double siz = Math.log(tmem)/Math.log(2)/32;

   if (dmem > MEM_MAX) dmem = MEM_MAX;
   if (dgc > GC_MAX) dgc = GC_MAX;
   if (siz > 1) siz = 1;

   return (dmem/MEM_MAX * 0.25) + (dgc/GC_MAX * 0.25) + (siz * 0.25) + 0.25;
}




private class Detailing extends DymonDetailing {

   Detailing()						{ super(for_process); }

   @Override public String getDetailName()		{ return getName(); }
   @Override protected double getLocalPriority()	{ return getAgentPriority(); }
   @Override public long getDetailInterval()		{ return processing_time + 1000; }
   @Override public long getDetailOverhead()		{ return processing_time; }
   @Override public double getDetailSlowdown()		{ return 0; }

   @Override protected DymonPatchRequest getPatchRequest(int priority) {
      patch_request.reset(null,getDetailInterval(),priority);
      return patch_request;
    }

}	// end of subclass Detailing




/********************************************************************************/
/*										*/
/*	Patch Request Class							*/
/*										*/
/********************************************************************************/

private class PatchRequest extends DymonPatchRequest {

   PatchRequest() {
      super(for_process,"HEAPAGENT");
    }

   boolean allowEmptyPatch()		{ return true; }

   String getRequestName()		{ return getName(); }

   protected String getVjmtiAgent() {
      if (init_state == -1) {
	 String r = ping_handler.waitForString();
	 if (r != null) init_state = 1;
	 else init_state = 0;
	 ping_handler = null;
       }
      if (init_state > 0) return null;
      return for_process.expandName(DYMON_VJMTI_HEAP_AGENT);
    }

   protected String getVjmtiMintName() { return "DYMTI"; }

   protected void handlePatch(long when,boolean insert) {
      if (insert) {
	 for_process.sendMessage("DYMTI","OBJDUMP",null,new DumpHandler(),MINT_MSG_FIRST_NON_NULL);
	 if (dump_heap) {
	    for_process.sendMessage("DYMTI","DUMPHEAP",null,null,MINT_MSG_FIRST_NON_NULL);
	  }
       }
    }

}	// end of subclass PatchRequest



/********************************************************************************/
/*										*/
/*	Class to handle dump response						*/
/*										*/
/********************************************************************************/

private class DumpHandler implements MintReply {

   boolean have_result = false;

   public void handleReply(MintMessage msg,MintMessage reply) {
      have_result = true;
      processResult(reply.getXml());
    }

   public void handleReplyDone(MintMessage msg) {
      if (!have_result) System.err.println("DYMON: HEAP: No result from DYMTI");
    }

}	// end of subclass DumpHandler




/********************************************************************************/
/*										*/
/*	Class to hold information about a class on the heap			*/
/*										*/
/********************************************************************************/

private static class HeapData implements Comparable<HeapData> {

   private String class_name;
   private long num_objects;
   private long used_size;

   HeapData(String nm) {
      class_name = convertClassName(nm);
      num_objects = 0;
      used_size = 0;
    }

   void update(long ct,long sz) {
      num_objects = ct;
      used_size = sz;
    }

   public int compareTo(HeapData hd) {
      if (used_size > hd.used_size) return -1;
      if (used_size < hd.used_size) return 1;
      if (num_objects > hd.num_objects) return -1;
      if (num_objects < hd.num_objects) return 1;
      if (class_name == hd.class_name) return 0;
      if (class_name == null) return -1;
      if (hd.class_name == null) return 1;
      return class_name.compareTo(hd.class_name);
    }

   void output(IvyXmlWriter xw,long tot,long minct,long minsz) {
      if (minct > 0 || minsz > 0) {
	 if (minct == 0 || num_objects < minct) {
	    if (minsz == 0 || used_size < minsz) return;
	  }
       }

      double pct = ((double) used_size)/((double) tot);

      xw.begin("CLASS");
      xw.field("NAME",class_name);
      xw.field("COUNT",num_objects);
      xw.field("SIZE",IvyFormat.formatMemory(used_size));
      xw.field("PCT",IvyFormat.formatPercent(pct));
      xw.end();
    }

   void outputSummary(IvyXmlWriter xw,long tot,long minct,long minsz) {
      if (minct > 0 || minsz > 0) {
	 if (minct == 0 || num_objects < minct) {
	    if (minsz == 0 || used_size < minsz) return;
	  }
       }

      double pct = ((double) used_size)/((double) tot);

      xw.begin("ITEM");
      xw.field("NAME",class_name);
      xw.field("VALUE",IvyFormat.formatPercent(pct));
      xw.end("ITEM");
    }


}	// end of subclass HeapData




/********************************************************************************/
/*										*/
/*	Class to hold information about memory usage				*/
/*										*/
/********************************************************************************/

private class MemoryDelta implements DeltaData {

   private long last_report;
   private long total_memory;
   private long total_gcs;

   MemoryDelta(long now,long mem,long gc) {
      last_report = now;
      total_memory = mem;
      total_gcs = gc;
    }

   public void outputDelta(IvyXmlWriter xw,DeltaData prevd) {
      if (last_report == 0) return;
      xw.begin("MEMORY_USAGE");
      xw.field("WHEN",last_report);
      xw.field("MEMORY",total_memory);
      xw.field("GCS",total_gcs);
      xw.end();
      last_report = 0;
    }

}	// end of subclass MemoryDelta



}	// end of DymonAgentHeap



/* end of DymonAgentHeap.java */


