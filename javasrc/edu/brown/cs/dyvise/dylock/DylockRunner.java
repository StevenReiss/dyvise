/********************************************************************************/
/*										*/
/*		DylockRunner.java						*/
/*										*/
/*	DYVISE lock analysis lock runner main progra				*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2009, Brown University, Providence, RI.				 *
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylock/DylockRunner.java,v 1.5 2016/11/02 18:59:10 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylockRunner.java,v $
 * Revision 1.5  2016/11/02 18:59:10  spr
 * Move to asm5
 *
 * Revision 1.4  2013-05-09 12:29:01  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.3  2012-10-05 00:52:41  spr
 * Update lock analysis.  Prepare for pattern display.
 *
 * Revision 1.2  2011-03-19 20:34:18  spr
 * Clean up and fix bugs in dylock.
 *
 * Revision 1.1  2011-03-10 02:25:00  spr
 * New version of dylock to produce visualizations.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dylock;

import edu.brown.cs.dyvise.dystore.*;
import edu.brown.cs.dyvise.dygraph.*;

import edu.brown.cs.ivy.exec.*;
import edu.brown.cs.ivy.file.*;
import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.Element;

import javax.swing.*;
import java.util.*;
import java.io.*;
import java.net.*;



public class DylockRunner implements DylockConstants, DylockConstants.DylockExec,
	DylockConstants.DylockLockDataManager, DylockConstants.DylockEventSetBuilder
{



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   DylockRunner dc = new DylockRunner(args);
   dc.process();
}



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private List<String>	arg_list;
private Set<Connection> active_connections;
private String		input_file;
private String		lock_file;
private DylockViewRef	view_reference;
private Map<Integer,DylockThreadData> thread_map;
private Map<Integer,EntryLock> location_map;
private Set<DylockLockEntry> current_entries;
private List<PatternEvent> output_events;
private DystoreControl	data_store;
private DygraphControl	graph_control;
private List<DylockRunPanel> run_panels;
private boolean 	show_views;
private double		max_time;
private Map<Integer,DylockLockData> active_locks;
private DylockEventGenerator event_generator;
private String		java_name;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DylockRunner(String [] args)
{
   arg_list = new ArrayList<String>();
   active_connections = new HashSet<Connection>();
   input_file = null;
   lock_file = null;

   view_reference = new DylockViewRef();
   thread_map = new HashMap<Integer,DylockThreadData>();
   location_map = new HashMap<Integer,EntryLock>();
   current_entries = new TreeSet<DylockLockEntry>(new LockComparator());
   active_locks = new HashMap<Integer,DylockLockData>();
   output_events = new ArrayList<PatternEvent>();
   event_generator = null;

   data_store = null;
   graph_control = null;
   run_panels = new ArrayList<DylockRunPanel>();
   show_views = true;
   max_time = 0;
   java_name = "java";

   scanArgs(args);
}




/********************************************************************************/
/*										*/
/*	Argument Scanning methods						*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   String outf = null;

   int start = 0;
   if (args.length > 0) {
      if (args[0].startsWith("-r")) start = 1;
    }

   while (args.length >= start+2) {
      if (args[start].startsWith("-o") && outf == null) {
	 outf = args[start+1];
	 show_views = false;
	 start += 2;
       }
      else if (!args[start].startsWith("-") && lock_file == null) {
	 lock_file = args[start];
	 start += 1;
       }
      else if (args[start].startsWith("-l") && lock_file == null) {
	 lock_file = args[start+1];
	 start += 2;
       }
      else if (args[start].startsWith("-i") && lock_file == null) {
	 lock_file = args[start+1] + ".view";
	 start += 2;
       }
      else if (args[start].startsWith("-t") && input_file == null) {
	 input_file = args[start+1];
	 start += 2;
       }
      else if (args[start].startsWith("-j")) {
	 java_name = args[start+1];
	 start += 2;
       }
      else break;
    }
   if (lock_file == null && args.length >= start+1) {
      lock_file = args[start++];
    }

   if (lock_file == null) badArgs();
   if (input_file != null) return;

   if (outf == null) {
      try {
	 SocketThread st = new SocketThread(true);
	 st.start();
	 SocketThread st1 = new SocketThread(false);
	 st1.start();
	 outf = createSocketName(st1.getSocket(),st.getSocket());
       }
      catch (IOException e) {
	 System.err.println("DYLOCK: Can't create connection socket: " + e);
	 System.exit(1);
       }
    }

   String a0 = IvyFile.expandName("-javaagent:$(BROWN_DYVISE_DYVISE)/lib/dylate.jar=LOCK=");
   a0 += lock_file;
   a0 += ":OUTPUT=" + outf;
   a0 += ":FULL";
   arg_list.add(a0);

   if (args.length <= start) badArgs();

   for (int i = start; i < args.length; ++i) {
      arg_list.add(args[i]);
    }
}


private String createSocketName(ServerSocket s1,ServerSocket s2)
{
   String h = s1.getInetAddress().getHostAddress();
   if (h.equals("0.0.0.0")) {
      try {
	 h = InetAddress.getLocalHost().getHostAddress();
       }
      catch (UnknownHostException e) { }
    }
   String p1 = Integer.toString(s1.getLocalPort());
   String p2 = Integer.toString(s2.getLocalPort());
   return p1 + "+" + p2 + "@" + h;
}




private void badArgs()
{
   System.err.println("DYLOCK: dylock -r [-o <output>] <lockfile> <javaargs>");
   System.err.println("DYLOCK: dylock -r [-t <input>] <lockfile>");
   System.exit(1);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getName()
{
   return "DYLOCK";
}


@Override public DylockThreadData findThread(int id)
{
   return thread_map.get(id);
}

@Override public DylockLockLocation findLocation(int id)
{
   return view_reference.findLocation(id);
}

@Override public double getMaxTime()
{
   return max_time;
}

@Override public DylockLockData findLock(int id)
{
   return active_locks.get(id);
}

@Override public DylockLockDataManager getManager()			{ return this; }

@Override public boolean missingWaitForWaited(DylockLockEntry ent)	{ return false; }

@Override public double getLockTime(DylockLockData ld,double t0)	{ return t0; }



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

@Override public void process()
{
   loadLocks();

   String tmdlf = IvyFile.expandName("$(BROWN_DYVISE_DYVISE)/lib/dylock.xml");
   Element elt = IvyXml.loadXmlFromFile(tmdlf);
   Element telt = IvyXml.getChild(elt,"TUPLEMODEL");
   data_store = new DystoreControl(telt);
   event_generator = new DylockEventGenerator(data_store);
   Element gelt = IvyXml.getChild(elt,"GRAPHMODEL");
   graph_control = new DygraphControl(gelt,data_store);

   if (show_views) {
      ToolTipManager ttm = ToolTipManager.sharedInstance();
      ttm.setDismissDelay(60*60*1000);
      ttm.setLightWeightPopupEnabled(false);
      JFrame frame = new DylockDisplay(graph_control,null);
      frame.setVisible(true);
    }

   if (input_file != null) {
      processInput();
      return;
    }

   arg_list.add(0,java_name);
   String [] argarr = new String[arg_list.size()];
   argarr = arg_list.toArray(argarr);

   System.err.print("RUN: ");
   for (String s : argarr) System.err.print(s + " ");
   System.err.println();

   try {
      IvyExec exec = new IvyExec(argarr,null,0);
      exec.waitFor();
    }
   catch (IOException e) {
      System.err.println("DYLOCK: Problem running java: " + e);
    }
}




private void processInput()
{
   try (BufferedReader br = new BufferedReader(new FileReader(input_file))) {
      for ( ; ; ) {
	 String ln = br.readLine();
	 if (ln == null) break;
	 processData(ln);
       }
      dataUpdated();
    }
   catch (IOException e) {
      System.err.println("DYLOCK: Problem reading input file: " + e);
    }
}




/********************************************************************************/
/*										*/
/*	Connection processing methods						*/
/*										*/
/********************************************************************************/

private void handleConnection(Socket s,boolean info)
{
   Connection c = new Connection(s,info);
   active_connections.add(c);
   c.start();
}



private void removeConnection(Connection c)
{
   active_connections.remove(c);
}


/********************************************************************************/
/*										*/
/*	Data processing methods 						*/
/*										*/
/********************************************************************************/

private void processData(String s) throws IOException
{
   // System.err.println("DYLOCK: DATA: " + s);

   if (s.startsWith("ENDBLOCK")) {
      for (DylockLockEntry en : current_entries) {
	 EntryLock el = location_map.get(en.getLocation().getId());
	 if (el == null) continue;
	 DylockViewType vt = el.getViewType();
	 vt.processPatternEntry(this,en,el.getLock(),true);
       }
      Collections.sort(output_events,new PatternSorter());
      for (PatternEvent pe : output_events) {
	 event_generator.add(pe);
       }
      output_events.clear();
      current_entries.clear();
      dataUpdated();
    }
   else {
      DylockLockEntry ent = new DylockLockEntry(this,s);
      current_entries.add(ent);
      max_time = Math.max(max_time,ent.getTime());
    }

   // set max_time
}



private void processInfo(String ln)
{
    // System.err.println("INFO: " + ln);

   String [] cnts = DylockMain.splitLine(ln);
   if (cnts.length < 2) return;
   int lid = Integer.parseInt(cnts[1]);
   if (lid < 0) return;
   if (cnts[0].equals("LOC")) {
      int id = Integer.parseInt(cnts[1]);
      DylockLockLocation loc = view_reference.findLocation(id);
      if (loc == null) {
	 System.err.println("DYLOCK: Location missing: " + ln);
       }
    }
   else if (cnts[0].equals("THREAD")) {
      int v1 = Integer.parseInt(cnts[1]);
      int v3 = Integer.parseInt(cnts[3]);
      DylockThreadData td = new DylockThreadData(v1,cnts[2],v3,cnts[4]);
      thread_map.put(v1,td);
    }
   else if (cnts[0].equals("MONITOR")) {
      DylockLockData ld = new DylockLockData(null,cnts[1],cnts[2]);
      active_locks.put(ld.getLockId(),ld);
    }
   else if (cnts[0].equals("PRIOR")) {
      int v1 = Integer.parseInt(cnts[1]);
      int v2 = Integer.parseInt(cnts[2]);
      DylockLockData ld1 = active_locks.get(v1);
      DylockLockData ld2 = active_locks.get(v2);
      if (ld1 != null && ld2 != null) ld1.addPrior(ld2);
    }
   else if (cnts[0].equals("STATS")) { }
}




@Override public void addEvent(PatternEvent ev)
{
   output_events.add(ev);
}



/********************************************************************************/
/*										*/
/*	Handle updating displays						*/
/*										*/
/********************************************************************************/

private void dataUpdated()
{
   if (graph_control == null) return;

   graph_control.dataUpdated();

   for (DylockRunPanel rp : run_panels) {
      rp.handleDataUpdated();
    }
}


void timeUpdate(double t0,double t1,boolean dyn)
{
   graph_control.setTimeWindow(t0,t1,dyn);

   for (DylockRunPanel pnl : run_panels) {
      pnl.setTimeBar();
    }
}



/********************************************************************************/
/*										*/
/*	Load information about locks						*/
/*										*/
/********************************************************************************/

private void loadLocks()
{
   Element xml = IvyXml.loadXmlFromFile(lock_file);

   for (Element lke : IvyXml.children(xml,"LOCK")) {
      DylockLockData ld = new DylockLockData(view_reference,lke);
      for (DylockViewType dvt : ld.getViewTypes()) {
	 EntryLock el = new EntryLock(ld,dvt);
	 for (TraceLockLocation dll : dvt.getLocations()) {
	    int lid = dll.getId();
	    location_map.put(lid,el);
	  }
       }
    }
}





/********************************************************************************/
/*										*/
/*	Thread for accepting connections from debug clients			*/
/*										*/
/********************************************************************************/

private class SocketThread extends Thread {

   private boolean info_socket;
   private ServerSocket server_socket = null;

   SocketThread(boolean info) throws IOException {
      super("DylockConnectListener");
      server_socket = new ServerSocket(0);
      info_socket = info;
    }

   ServerSocket getSocket()			{ return server_socket; }

   @Override public void run() {
      Socket s;
      while (server_socket != null) {
	 try {
	    s = server_socket.accept();
	    System.err.println("CONNECT TO " + s + " " + info_socket);
	    handleConnection(s,info_socket);
	  }
	 catch (IOException e) { }
       }
    }

}	// end of inner class SocketThread



/********************************************************************************/
/*										*/
/*	Class representing a java agent connection for locks			*/
/*										*/
/********************************************************************************/

private class Connection extends Thread {

   private Socket client_socket;
   private boolean info_socket;

   Connection(Socket s,boolean info) {
      super("DylockRunnerConnection_" + s.getRemoteSocketAddress());
      client_socket = s;
      info_socket = info;
    }

   @Override public void run() {
      try {
	 InputStream ins = client_socket.getInputStream();
	 BufferedReader lnr = new BufferedReader(new InputStreamReader(ins));

	 boolean done = false;
	 while (!done) {
	    for ( ; ; ) {
	       String s = lnr.readLine();
	       if (s == null) {
		  done = true;
		  break;
		}
	       else if (info_socket) processInfo(s);
	       else processData(s);
	     }
	  }
       }
      catch (IOException e) { }

      removeConnection(this);
    }

}	// end of inner class Connection







private static class LockComparator implements Comparator<DylockLockEntry> {

   @Override public int compare(DylockLockEntry e1,DylockLockEntry e2) {
      double v1 = e1.getTime() - e2.getTime();
      if (v1 < 0) return -1;
      else if (v1 > 0) return 1;
      int v0 = e1.getLockId() - e2.getLockId();
      if (v0 < 0) return -1;
      else if (v0 > 0) return 1;
      int v2 = e1.getEntryType().ordinal() - e2.getEntryType().ordinal();
      if (v2 < 0) return -1;
      else if (v2 > 0) return 1;
      int v3 = e1.getThreadId() - e2.getThreadId();
      if (v3 < 0) return -1;
      else if (v3 > 0) return 1;
      return 0;
    }

}	// end of inner class LockComparator


private static class PatternSorter implements Comparator<PatternEvent> {

   @Override public int compare(PatternEvent e1,PatternEvent e2) {
      double d1 = e1.getTime() - e2.getTime();
      if (d1 < 0) return -1;
      if (d1 > 0) return 1;
      int d2 = e1.getThreadId() - e2.getThreadId();
      if (d2 < 0) return -1;
      if (d2 > 0) return 1;
      int d4 = e1.getType().ordinal() - e2.getType().ordinal();
      return d4;
    }

}	// end of inner class PatternSorter




private static class EntryLock {

   private DylockLockData for_lock;
   private DylockViewType for_type;

   EntryLock(DylockLockData ld,DylockViewType vt) {
      for_lock = ld;
      for_type = vt;
    }

   DylockLockData getLock()		{ return for_lock; }
   DylockViewType getViewType() 	{ return for_type; }

}	// end of inner class EntryLock




}	// end of class DylockRunner




/* end of DylockRunner.java */
