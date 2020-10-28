/********************************************************************************/
/*										*/
/*		DyvisionMain.java						*/
/*										*/
/*	Main program for dyper performance evaluation interface 		*/
/*										*/
/********************************************************************************/
/*	Copyright 2007 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2007, Brown University, Providence, RI.				 *
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyvision/DyvisionMain.java,v 1.11 2010-03-30 16:24:34 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyvisionMain.java,v $
 * Revision 1.11  2010-03-30 16:24:34  spr
 * Clean up statistic display.
 *
 * Revision 1.10  2009-10-07 22:39:53  spr
 * Eclipse code cleanup.
 *
 * Revision 1.9  2009-10-07 01:00:24  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.8  2009-06-04 18:55:11  spr
 * Handle bad trace directory.
 *
 * Revision 1.7  2009-04-28 18:01:26  spr
 * Add graphs to time lines.
 *
 * Revision 1.6  2009-04-11 23:47:31  spr
 * Handle formating using IvyFormat.
 *
 * Revision 1.5  2009-03-20 02:09:09  spr
 * Add memory view and related controls. Clean code.
 *
 * Revision 1.4  2008-12-04 01:11:27  spr
 * Fix up time display.  Add termination.
 *
 * Revision 1.3  2008-11-24 23:38:14  spr
 * Move to dymaster.  Update views.
 *
 * Revision 1.2  2008-11-12 14:11:20  spr
 * Clean up the output and bug fixes.
 *
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dyvision;


import edu.brown.cs.dyvise.dyvise.*;
import edu.brown.cs.dyvise.dymon.DymonRemote;
import edu.brown.cs.dyvise.dymem.DymemFrame;

import edu.brown.cs.ivy.mint.*;
import edu.brown.cs.ivy.swing.SwingSetup;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.awt.event.*;
import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;


public class DyvisionMain implements DyvisionConstants, MintConstants, DyviseConstants.TimeListener
{


/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   DyvisionMain dm = new DyvisionMain(args);

   dm.process();
}




/********************************************************************************/
/*										*/
/*	Local Storage								*/
/*										*/
/********************************************************************************/

private DymonRemote dymon_remote;

private DyvisionView  active_view;
private Map<String,DyvisionTableSpec> table_specs;
private String table_file;
private MintControl mint_control;
private DyviseTimeManager time_manager;

private long		last_time;
private long		report_time;
private Element 	last_analysis;

private BufferedOutputStream  trace_output;
private FileChannel	trace_channel;
private SortedMap<Long,TraceLoc> trace_locs;
private RandomAccessFile trace_reader;

private DyvisionTime time_frame;
private DymemFrame memory_frame;

private DyvisionControlPanel control_panel;
private double		cur_overhead;
private boolean 	is_enabled;
private String		start_class;
private boolean 	is_alive;


private static boolean do_debug = false;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private DyvisionMain(String [] args)
{
   new SwingSetup();

   if (System.getenv("BROWN_DYVISION_DEBUG") != null) do_debug = true;

   dymon_remote = new DymonRemote();

   active_view = null;
   table_file = DYVISION_TABLE_FILE;

   table_specs = new LinkedHashMap<String,DyvisionTableSpec>();

   scanArgs(args);

   String pid = active_view.getId();

   time_frame = new DyvisionTime(pid);
   memory_frame = new DymemFrame(pid,dymon_remote);
   time_manager = DyviseTimeManager.getTimeManager(pid);
   time_manager.addTimeListener(this);

   loadTableData();

   last_time = 0;
   report_time = 0;

   is_alive = true;

   trace_output = null;
   trace_channel = null;
   trace_locs = null;
   trace_reader = null;
   setupTrace();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

boolean doDebug()				{ return do_debug; }

boolean isAlive()				{ return is_alive; }

boolean isEnabled()				{ return is_enabled; }

long getTraceTime()				{ return time_manager.getCurrentTime(); }

void createUserMark(long when,String what)	{ time_manager.createUserMark(when,what); }

String getStartClass()				{ return start_class; }

double getOverhead()				{ return cur_overhead; }




/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

private void process()
{
   mint_control = MintControl.create(DYPER_MESSAGE_BUS,MintSyncMode.SINGLE);

   CommandHandler hdlr = new CommandHandler();
   String pid = active_view.getId();

   mint_control.register("<DYVISION PID='" + pid + "' COMMAND='_VAR_0'><_VAR_1/></DYVISION>",hdlr);

   dymon_remote.scheduleEvery(new Updater(),DYVISION_VIEW_UPDATE);

   active_view.addWindowListener(new CloseListener());
   active_view.addWindowStateListener(new CloseListener());

   active_view.start();
}




/********************************************************************************/
/*										*/
/*	Argument scanning methods						*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
	 if (args[i].startsWith("-t") && i+1 < args.length) {           // -table <file>
	    table_file = args[++i];
	  }
	 else if (args[i].startsWith("-D")) {                           // -Debug
	    do_debug = true;
	  }
	 else badArgs();
       }
      else if (active_view == null) {
	 active_view = new DyvisionView(this,args[i]);
       }
      else badArgs();
    }

   if (active_view == null) badArgs();
}



private void badArgs()
{
   System.err.println("DYVISION: dyvision pid[@host]");
   System.exit(1);
}



/********************************************************************************/
/*										*/
/*	Methods to handle table specifications					*/
/*										*/
/********************************************************************************/

private void loadTableData()
{
   Element xml = IvyXml.loadXmlFromFile(table_file);
   if (xml == null) {
      System.err.println("DYVISION: Problem loading table data from " + table_file);
      return;
    }

   for (Element e : IvyXml.elementsByTag(xml,"TABLE")) {
      String nm = IvyXml.getAttrString(e,"NAME");
      table_specs.put(nm,new DyvisionTableSpec(e));
    }
}


DyvisionTableSpec getTableSpec(String id)
{
   return table_specs.get(id);
}


void setupTables(DyvisionDetail dd,String id)
{
   Collection<DyvisionTableSpec> tbls = new ArrayList<DyvisionTableSpec>();

   for (DyvisionTableSpec ts : table_specs.values()) {
      if (ts != null && ts.useForAgent(id)) tbls.add(ts);
    }

   dd.setupTables(tbls);
}




/********************************************************************************/
/*										*/
/*	Handle interface with DYMON						*/
/*										*/
/********************************************************************************/

String dymonCommand(String cmd)
{
   return dymon_remote.dymonCommand(cmd);
}



void scheduleEvery(TimerTask tt,long every)
{
   dymon_remote.scheduleEvery(tt,every);
}



/********************************************************************************/
/*										*/
/*	Commands to handle messaging						*/
/*										*/
/********************************************************************************/

private class CommandHandler implements MintHandler {

   public void receive(MintMessage msg,MintArguments args) {
      String cmd = args.getArgument(0);
      String rslt = null;
      try {
	 if (cmd == null) return;
	 if (cmd.equals("PING")) rslt = "PONG";
	 else if (cmd.equals("SETTIME")) {
	    long when = args.getLongArgument(1);
	    time_manager.setCurrentTime(when);
	    rslt = Long.toString(report_time);
	  }
	 else if (cmd.equals("SETNOW")) {
	    time_manager.setCurrentTime(0);
	    rslt = Long.toString(report_time);
	  }
	 else if (cmd.equals("SHOW")) {
	    active_view.showView();
	    rslt = "OK";
	  }
       }
      catch (Throwable t) {
	 System.err.println("DYVISION: Problem processing request: " + t);
	 t.printStackTrace();
       }

      String s = "<DYVISION_REPLY ID='" + active_view.getId() + "'>";
      if (rslt != null) s += rslt;
      s += "</DYVISION_REPLY>";
      msg.replyTo(s);
    }

}	// end of subclass CommandHandler




/********************************************************************************/
/*										*/
/*	Trace File methods							*/
/*										*/
/********************************************************************************/

private void setupTrace()
{
   String fnm = DYVISION_TRACE_FILE + active_view.getId() + ".trace";
   File sf = new File(fnm);
   try {
      FileOutputStream fos = new FileOutputStream(sf);
      trace_channel = fos.getChannel();
      trace_output = new BufferedOutputStream(fos);
      trace_reader = new RandomAccessFile(sf,"r");
    }
   catch (IOException e) {
      System.err.println("DYVISION: Problem creating trace output " + sf + ": " + e);
    }

   if (trace_output != null) {
      trace_locs = new TreeMap<Long,TraceLoc>();
      if (doDebug()) {
	 System.err.println("DYVISION: Saving output in " + sf);
       }
      else {
	 sf.deleteOnExit();
       }
    }
}



/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

private void handleUpdates(Element e)
{
   try {
      if (time_manager.getCurrentTime() == 0) {
	 active_view.handleUpdates(e);
       }
      time_frame.update(e);
      if (e != null) {
	 cur_overhead = IvyXml.getAttrDouble(e,"OVERHEAD",0);
	 is_enabled = IvyXml.getAttrBool(e,"ENABLED");
	 start_class = IvyXml.getAttrString(e,"START");
	 if (control_panel != null) control_panel.update();
	 last_analysis = e;
       }
    }
   catch (Throwable t) {
      System.err.println("DYVISION: Error processing update: " + t);
      t.printStackTrace();
      System.err.print("DYVISION: Update = ");
      if (e == null) System.err.println("null");
      else System.err.println(IvyXml.convertXmlToString(e));
    }
}



private class Updater extends TimerTask {

   private int num_fail;

   Updater() {
      num_fail = 0;
    }


   public void run() {
      String cmd = "ANALYSIS " + active_view.getId();
      String rslt = dymon_remote.dymonCommand(cmd);
      if (rslt == null) {
	 if (num_fail++ > DYVISION_MAX_FAIL) {
	    is_alive = false;
	    handleUpdates(null);
	    cancel();
	    System.err.println("DYVIS: Canceling timer " + active_view.isVisible());
	    if (!active_view.isVisible()) System.exit(0);
	  }
	 // else handleUpdates(last_analysis);
	 return;
       }

      num_fail = 0;

      Element xml = IvyXml.convertStringToXml(rslt);
      last_time = IvyXml.getAttrLong(xml,"NOW");
      if (last_time == 0) last_time = System.currentTimeMillis();
      time_manager.setLastTime(last_time);

      rslt += "\n";
      byte [] bbuf = rslt.getBytes();

      if (trace_locs != null && trace_channel != null) {
	 try {
	    long pos = trace_channel.position();
	    trace_locs.put(last_time,new TraceLoc(pos,bbuf.length));
	  }
	 catch (IOException ex) {
	    System.err.println("DYVISION: Problem getting trace location: " + ex);
	    trace_channel = null;
	  }
       }

      if (trace_output != null) {
	 try {
	    trace_output.write(bbuf);
	    trace_output.flush();
	  }
	 catch (IOException ex) {
	    System.err.println("DYVISION: Problem saving trace data: " + ex);
	    trace_output = null;
	  }
       }

      handleUpdates(xml);
    }

}	// end of subclass Updater



/********************************************************************************/
/*										*/
/*	Time setting methods							*/
/*										*/
/********************************************************************************/

public void handleMark(long when,String what)			{ }

public void handleTimeSet(long when)
{
   if (when == 0) {
      handleUpdates(last_analysis);
      report_time = last_time;
    }
   else if (trace_locs != null) {
      SortedMap<Long,TraceLoc> hd = trace_locs.headMap(when+1);
      if (hd.size() == 0) report_time = trace_locs.firstKey();
      else report_time = hd.lastKey();
      TraceLoc tloc = trace_locs.get(report_time);
      try {
	 trace_reader.seek(tloc.getPosition());
	 byte [] buf = new byte[tloc.getSize()];
	 int ln = tloc.getSize();
	 int pos = 0;
	 while (ln > 0) {
	    int ct = trace_reader.read(buf,pos,ln);
	    ln -= ct;
	    pos += ct;
	  }
	 String s = new String(buf);
	 Element e = IvyXml.convertStringToXml(s);
	 active_view.handleUpdates(e);
       }
      catch (IOException e) {
	 System.err.println("DYVISION: Problem seeking to history: " + e);
       }
    }
}




/********************************************************************************/
/*										*/
/*	Class to hold trace information 					*/
/*										*/
/********************************************************************************/

private static class TraceLoc {

   private long file_position;
   private int	record_length;

   TraceLoc(long pos,int len) {
      file_position = pos;
      record_length = len;
    }

   long getPosition()				{ return file_position; }
   int getSize()				{ return record_length; }

}	// end of subclass TraceLoc




/********************************************************************************/
/*										*/
/*	Time line methods							*/
/*										*/
/********************************************************************************/

void showTimeLine()
{
   time_frame.setVisible(true);
}


void showMemoryView()
{
   memory_frame.setVisible(true);
}




/********************************************************************************/
/*										*/
/*	Control panel for a process						*/
/*										*/
/********************************************************************************/

void showControlPanel()
{
   if (control_panel == null) {
      control_panel = new DyvisionControlPanel(this,active_view);
    }

   control_panel.setVisible(true);
}



/********************************************************************************/
/*										*/
/*	Window listener for handling termination				*/
/*										*/
/********************************************************************************/

private class CloseListener extends WindowAdapter {

   @Override public void windowClosing(WindowEvent e) {
      if (!is_alive) System.exit(0);
    }

   @Override public void windowStateChanged(WindowEvent e) {
    }
}




}	// end of class DyvisionMain




/* end of DyvisionMain.java */
