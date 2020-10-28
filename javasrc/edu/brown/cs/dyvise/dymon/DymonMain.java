/********************************************************************************/
/*										*/
/*		DymonMain.java							*/
/*										*/
/*	DYPER monitor interface and agents					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymon/DymonMain.java,v 1.14 2016/11/02 18:59:13 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymonMain.java,v $
 * Revision 1.14  2016/11/02 18:59:13  spr
 * Move to asm5
 *
 * Revision 1.13  2011-03-19 20:34:25  spr
 * Code cleanup
 *
 * Revision 1.12  2010-06-01 19:26:24  spr
 * Upgrades to make dyview work on the mac
 *
 * Revision 1.11  2010-03-30 16:22:05  spr
 * Clean up the code.
 *
 * Revision 1.10  2009-10-07 01:00:13  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.9  2009-09-19 00:09:46  spr
 * Update dymon for seletive agent insertion and removal.
 *
 * Revision 1.8  2009-06-04 18:53:50  spr
 * Set up for binary distribution.
 *
 * Revision 1.7  2009-04-28 18:01:15  spr
 * Update state information to produce state output.
 *
 * Revision 1.6  2009-04-20 23:23:30  spr
 * Updates to make things work on the mac.  Fix bug in dymti.
 *
 * Revision 1.5  2009-03-20 02:06:50  spr
 * Update dymon: add time-based information, updated memory, etc.
 *
 * Revision 1.4  2008-12-04 01:11:00  spr
 * Update output and fix phaser summary.
 *
 * Revision 1.3  2008-11-24 23:59:42  spr
 * Update remote error handling.  Use address rather than name for hosts.
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


import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.mint.*;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.io.*;
import java.net.*;
import java.util.*;


public class DymonMain implements DymonConstants, MintConstants {



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   DymonMain dm = new DymonMain(args);

   dm.process();
}




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private MintControl	msg_handler;
private Map<String,DymonProcess> process_map;
private UIServer	ui_server;
private List<UIClient>	ui_clients;
private int		change_count;
private Timer		dymon_timer;
private Timer		remote_timer;
private DymonResources	resource_data;
private Map<DymonProcess,DymonPatchTracker> track_map;
private DymonPatchData	patch_data;
private DymonAttach	dymon_attach;
private long		report_every;
private boolean 	web_access;
private Object		check_lock;
private String		server_host;
private String		server_port;
private Socket		dyperserver_socket;

private static boolean do_debug = (System.getenv("BROWN_DYMON_DEBUG") != null);



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private DymonMain(String [] args)
{
   resource_data = new DymonResources();
   resource_data.loadFile(DYMON_GLOBAL_RESOURCE_FILE);
   resource_data.loadFile(DYMON_RESOURCE_FILE);
   report_every = 0;
   web_access = false;
   check_lock = new Object();

   IvyExec.usePolling(true);

   scanArgs(args);

   process_map = new TreeMap<String,DymonProcess>();
   track_map = new HashMap<DymonProcess,DymonPatchTracker>();
   patch_data = new DymonPatchData();
   dymon_attach = new DymonAttach(this);

   ui_server = new UIServer();
   ui_clients = new ArrayList<UIClient>();
   dymon_timer = new Timer("DymonTimerThread",true);
   remote_timer = new Timer("DymonRemoteTimerThread",true);

   change_count = 1;
}



/********************************************************************************/
/*										*/
/*	Argument scanning							*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
	 if (args[i].startsWith("-e")) {                                // -enabled
	    resource_data.setResource("MONITOR","TRUE");
	   }
	 else if (args[i].startsWith("-d")) {                           // -disabled
	    resource_data.setResource("MONITOR","FALSE");
	  }
	 else if (args[i].startsWith("-r") && i+1 < args.length) {      // -r <resource file>
	    if (!resource_data.loadFile(args[++i])) {
	       System.err.println("DYMON: Can't open resource file " + args[i]);
	       System.exit(1);
	     }
	  }
	 else if (args[i].startsWith("-R") && i+1 < args.length) {      // -R <report every>
	    report_every = Long.parseLong(args[++i]) * 1000;
	  }
	 else if (args[i].startsWith("-f")) {                           // -force
	    File f = new File(DYMON_LOCK_FILE);
	    f.delete();
	  }
	 else if (args[i].startsWith("-w")) {                           // -web
	    web_access = true;
	  }
	 else if (args[i].startsWith("-D")) {                           // -Debug
	    do_debug = true;
	  }
	 else badArgs();
       }
      else badArgs();
    }
}



private void badArgs()
{
   System.err.println("DYMON: dymon [-enable|-disable] [-r resource] [-R report] [-web]");
   System.exit(1);
}




/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

private void process()
{
   checkRunning();

   msg_handler = DymonRemote.getMintControl();

   startDyperServer();

   msg_handler.register("<DYPER START='_VAR_0' />",new StartCommand());
   msg_handler.register("<DYPER END='_VAR_0' />",new EndCommand());
   msg_handler.register("<DYMONUI>_VAR_0</DYMONUI>",new UICommand());

   ui_server.start();

   WhoHandler wh = new WhoHandler();
   msg_handler.send("<DYPER COMMAND='WHO'/>",wh,MINT_MSG_ALL_REPLIES);
   wh.setupProcesses();

   dymon_timer.schedule(new CheckProcs(true,false),DYMON_CHECK_PROCS_FIRST,DYMON_CHECK_PROCS_LOCAL);
   remote_timer.schedule(new CheckProcs(false,true),DYMON_CHECK_PROCS_FIRST,DYMON_CHECK_PROCS_REMOTE);

   if (report_every != 0) {
      dymon_timer.schedule(new Reporter(),report_every,report_every);
    }
}



private void checkRunning()
{
   try {
      File f = new File(DYMON_LOCK_FILE);
      File pf = f.getParentFile();
      if (pf != null && !pf.exists()) pf.mkdirs();

      if (!f.createNewFile()) {
	 if (!checkActive()) {
	    System.err.println("DYMON: Previous server apparently died");
	    f.delete();
	    if (!f.createNewFile()) {
	       System.err.println("DYMON: Problem creating lock file");
	       System.exit(1);
	     }
	  }
	 else {
	    System.err.println("DYMON: Previous server accessible");
	    System.err.println("DYMON: Server already running: " + f);
	    System.exit(0);
	  }
       }

      f.deleteOnExit();
    }
   catch (IOException e) {
      System.err.println("DYMON: Problem creating lock file: " + e);
    }
}



private boolean checkActive()
{
   try {
      File f1 = new File(DYMON_UI_SERVER_SOCKET);
      if (!f1.exists()) return false;
      BufferedReader fr = new BufferedReader(new FileReader(f1));
      String s = fr.readLine();
      fr.close();
      if (s == null) return false;
      StringTokenizer tok = new StringTokenizer(s,"\t");
      if (!tok.hasMoreTokens()) return false;
      String host = tok.nextToken();
      if (!tok.hasMoreTokens()) return false;
      int port = Integer.parseInt(tok.nextToken());
      if (port == 0) return false;

      Socket xs = new Socket(host,port);
      xs.close();
      return true;
    }
   catch (ConnectException e) { }
   catch (IOException e) { }

   return false;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getSettings(String start)
{
   return resource_data.getSettings(start);
}


String getClassSettings(String start)
{
   return resource_data.getClasses(start);
}



String getResource(String start,String name)
{
   return resource_data.getResource(start,name);
}


String getOutputName(String start,String jar,List<String> args)
{
   return resource_data.getName(start,jar,args);
}


boolean getBooleanResource(String start,String name)
{
   String s = getResource(start,name);
   if (s == null || s.length() == 0) return false;
   switch (s.charAt(0)) {
      case 't' :
      case 'T' :
      case '1' :
	 return true;
      default :
	 break;
    }
   return false;
}



long getLongResource(String start,String name)
{
   String s = getResource(start,name);
   if (s == null) return 0;

   try {
      return Long.parseLong(s);
    }
   catch (NumberFormatException e) { }

   return 0;
}




double getDoubleResource(String start,String name)
{
   String s = getResource(start,name);
   if (s == null) return 0;

   try {
      return Double.parseDouble(s);
    }
   catch (NumberFormatException e) { }

   return 0;
}



void checkResources()
{
   resource_data.checkFiles();
}



boolean doDebug()				{ return do_debug; }




/********************************************************************************/
/*										*/
/*	Host commands and queries						*/
/*										*/
/********************************************************************************/

Iterable<String> getRemoteHosts()
{
   return resource_data.getRemoteHosts();
}



private String handleHosts(String cmd,String host)
{
   if (cmd.equals("ADD") && host != null) {
      resource_data.addHost(host);
    }
   else if (cmd.equals("REMOVE") && host != null) {
      resource_data.removeHost(host);
    }
   else if (cmd.equals("LIST")) {
      StringBuffer buf = new StringBuffer();
      Set<String> hset = new TreeSet<String>();
      for (String s : resource_data.getRemoteHosts()) hset.add(s);
      for (String s : hset) {
	 if (buf.length() > 0) buf.append(",");
	 buf.append(s);
       }
      return buf.toString();
    }

   return null;
}




/********************************************************************************/
/*										*/
/*	Agent management methods						*/
/*										*/
/********************************************************************************/

Collection<DymonAgent> createAgents(DymonProcess dp)
{
   String agentset = dp.getAgentSet();

   if (agentset == null || agentset.equals("ALL") || agentset.equals("*")) agentset = null;

   Collection<DymonAgent> r = new ArrayList<DymonAgent>();

   checkAgent(r,agentset,true,new DymonAgentCpu(this,dp),dp);
   checkAgent(r,agentset,true,new DymonAgentThreads(this,dp),dp);
   checkAgent(r,agentset,true,new DymonAgentTiming(this,dp),dp);
   checkAgent(r,agentset,true,new DymonAgentMemory(this,dp),dp);
   checkAgent(r,agentset,true,new DymonAgentIO(this,dp),dp);
   checkAgent(r,agentset,true,new DymonAgentHeap(this,dp),dp);
   checkAgent(r,agentset,true,new DymonAgentReaction(this,dp,true),dp);
   // checkAgent(r,agentset,true,new DymonAgentSocket(this,dp),dp);
   checkAgent(r,agentset,true,new DymonAgentPhaser(this,dp),dp);
   checkAgent(r,agentset,false,new DymonAgentCollections(this,dp),dp);
   checkAgent(r,agentset,false,new DymonAgentStates(this,dp),dp);
   checkAgent(r,agentset,false,new DymonAgentReaction(this,dp,false),dp);
   checkAgent(r,agentset,false,new DymonAgentEvents(this,dp),dp);

   return r;
}



private void checkAgent(Collection<DymonAgent> r,String agentset,boolean dflt,DymonAgent agt,DymonProcess dp)
{
   String nm = agt.getName();
   boolean fg = false;

   if (agentset != null && agentset.indexOf(nm) >= 0) fg = true;
   if (!fg && dflt) {
      if (agentset == null || agentset.indexOf("*") >= 0 || agentset.indexOf("ALL") >= 0)
	 fg = true;
    }

   if (fg) {
      r.add(agt);
      agt.install();
    }
}



/********************************************************************************/
/*										*/
/*	Message methods 							*/
/*										*/
/********************************************************************************/

MintControl getMintControl()			{ return msg_handler; }


void send(String msg)
{
   msg_handler.send(msg);
}



void send(String msg,MintReply hdlr,int flags)
{
   msg_handler.send(msg,hdlr,flags);
}



void register(String pat,MintHandler hdlr)
{
   msg_handler.register(pat,hdlr);
}



/********************************************************************************/
/*										*/
/*	Methods for maintaining the set of processes				*/
/*										*/
/********************************************************************************/

private synchronized DymonProcess createProcess(String id)
{
   DymonProcess dp = findProcess(id);

   if (dp == null) {
      dp = new DymonProcess(this,id,null,false);
      addProcess(dp);
    }

   return dp;
}



private void attachProcess(DymonProcess dp)
{
   if (!dp.isAttached()) dp.attach();
}




synchronized void addProcess(DymonProcess dp)
{
   if (dp.isActive() || !dp.isAttached()) {
      process_map.put(dp.getProcessId(),dp);
      ++change_count;
      // notify front end
    }
}



synchronized void removeProcess(DymonProcess dp)
{
   if (dp != null && process_map.containsKey(dp.getProcessId())) {
      if (!dp.isAttached() && dp.isActive()) dp.checkAlive();

      // if (dp.isActive()) return;		   // don't remove if active
      // System.err.println("DYMON: REMOVE PROCESS " + dp.getProcessId());
      process_map.remove(dp.getProcessId());
      ++change_count;
      // notify front end
    }
}



synchronized DymonProcess findProcess(String id)
{
   return process_map.get(id);
}



String handleProcessCheck(String pid)
{
   DymonProcess dp = findProcess(pid);
   if (dp == null) return "NOT_FOUND";
   if (!dp.isActive()) return "INACTIVE";
   if (!dp.isAttached()) return "NOT_ATTACHED";

   MintDefaultReply ph = new MintDefaultReply();
   dp.sendDyperMessage("PING",null,ph,MINT_MSG_FIRST_NON_NULL);
   if (ph.waitForString(10000) != null) return "OK";

   return "PENDING";
}



private synchronized List<DymonProcess> getProcesses()
{
   return new ArrayList<DymonProcess>(process_map.values());
}



private synchronized String dumpProcessTable()
{
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("PROCESSES");
   xw.field("COUNT",change_count);
   for (DymonProcess dp : process_map.values()) {
      dp.outputUIData(xw);
    }
   xw.end("PROCESSES");

   return xw.toString();
}



private void checkProcesses(boolean lcl,boolean rem)
{
   synchronized (check_lock) {
      checkResources();

      List<DymonProcess> dplist = getProcesses();

      Set<DymonProcess> chk = new HashSet<DymonProcess>();

      for (DymonProcess dp : dplist) {
	 if (dp.isAttached()) dp.checkAlive();
	 else if (!dp.isAttached()) chk.add(dp);
       }

      if (lcl) dymon_attach.checkLocalProcesses(chk);
      if (rem) dymon_attach.checkRemoteProcesses(chk);
    }
}



void attachVjmtiAgent(DymonProcess dp,String lib,String args)
{
   dymon_attach.attachVjmtiAgent(dp,lib,args);
}



private class StartCommand implements MintHandler {

   public void receive(MintMessage msg,MintArguments args) {
      msg.replyTo();
      DymonProcess dp = createProcess(args.getArgument(0));
      attachProcess(dp);
    }

}	// end of subclass StartCommand



private class EndCommand implements MintHandler {

   public void receive(MintMessage msg,MintArguments args) {
      msg.replyTo();
      String id = args.getArgument(0);
      DymonProcess dp = findProcess(id);
      if (dp != null) removeProcess(dp);
    }

}	// end of subclass EndCommand



private class WhoHandler implements MintReply {

   private Map<String,Boolean> processes_found;
   private boolean all_found;

   WhoHandler() {
      processes_found = new HashMap<String,Boolean>();
      all_found = false;
    }

   public void handleReply(MintMessage msg,MintMessage reply) {
      if (reply != null) {
	 Element e = reply.getXml();
	 if (e != null) {
	    String id = IvyXml.getAttrString(e,"ID");
	    boolean active = IvyXml.getAttrBool(e,"ACTIVE");
	    processes_found.put(id,active);
	  }
       }
    }

   public synchronized void handleReplyDone(MintMessage msg) {
      all_found = true;
      notifyAll();
    }

   void setupProcesses() {
      long start = System.currentTimeMillis();
      synchronized(this) {
	 while (!all_found) {
	    try {
	       wait(1000);
	     }
	    catch (InterruptedException e) { }
	    long waited = System.currentTimeMillis() - start;
	    if (waited > 10000) break;
	  }
       }
      for (Map.Entry<String,Boolean> ent : processes_found.entrySet()) {
	 String id = ent.getKey();
	 DymonProcess dp = createProcess(id);
	 attachProcess(dp);
	 if (!ent.getValue()) dp.detach();
       }
    }

}	// end of subclass WhoHandler




private class CheckProcs extends TimerTask {

   private boolean do_local;
   private boolean do_remote;

   CheckProcs(boolean l,boolean r) {
      do_local = l;
      do_remote = r;
    }

   public void run() {
      checkProcesses(do_local,do_remote);
    }

}	// end of subclass CheckProcs




/********************************************************************************/
/*										*/
/*	Analysis methods							*/
/*										*/
/********************************************************************************/

private void doAnalysis(String pid,String what,IvyXmlWriter xw)
{
   DymonProcess dp = findProcess(pid);

   if (dp != null && dp.isAttached()) dp.doAnalysis(what,xw);
}



private void analyzeAll()
{
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("REPORT");

   synchronized (this) {
      for (DymonProcess dp : process_map.values()) {
	 if (dp.isAttached()) dp.doAnalysis("*",xw);
       }
    }

   xw.end("REPORT");
   System.out.println(xw.toString());
}




private class Reporter extends TimerTask {

   public void run() {
      analyzeAll();
    }

}	// end of subclass Reporter




/********************************************************************************/
/*										*/
/*	Debugging methods							*/
/*										*/
/********************************************************************************/

private void showLastReport(String pid,IvyXmlWriter xw)
{
   DymonProcess dp = findProcess(pid);

   if (dp != null) dp.showLastReport(xw);
}



/********************************************************************************/
/*										*/
/*	Patching methods							*/
/*										*/
/********************************************************************************/

void requestPatch(DymonPatchRequest pr)
{
   if (pr == null) return;

   PatchThread pt = new PatchThread(pr);
   pt.start();
}



void removePatch(DymonPatchRequest pr)
{
   if (pr == null) return;
   for (DymonProcess dp : pr.getProcesses()) {
      DymonPatchTracker ptr = track_map.get(dp);
      if (ptr != null) ptr.removeRequest(pr);
    }
}



void disablePatcher(DymonProcess dp)
{
   DymonPatchTracker ptr = null;
   synchronized (this) {
      ptr = track_map.get(dp);
    }
   if (ptr != null) ptr.disable();
}



void enablePatcher(DymonProcess dp)
{
   DymonPatchTracker ptr = null;
   synchronized (this) {
      ptr = track_map.get(dp);
    }
   if (ptr != null) ptr.enable();
}



private class PatchThread extends Thread {

   private DymonPatchRequest patch_request;

   PatchThread(DymonPatchRequest pr) {
      super("PatchSetupThread");
      patch_request = pr;
    }

   public void run() {
      patch_request.prepare();

      patch_request.donePrepare();

      for (DymonProcess pr : patch_request.getProcesses()) {
	 DymonPatchTracker ptr = null;
	 synchronized (this) {
	    ptr = track_map.get(pr);
	    if (ptr == null) {
	       ptr = new DymonPatchTracker(pr);
	       track_map.put(pr,ptr);
	     }
	  }
	 ptr.addRequest(patch_request);
       }
    }

}	// end of subclass PatchThread




/********************************************************************************/
/*										*/
/*	PatchData methods							*/
/*										*/
/********************************************************************************/

void addClassData(Element e)
{
   patch_data.addClassData(e);
}


void addMethodData(Element e)
{
   patch_data.addMethodData(e);
}



void handleItems(Collection<Integer> ids,long when,long totsam,long actsam,
		    boolean start,String forwhom)
{
   patch_data.manageActiveData(ids,when,totsam,actsam,start,forwhom);
}



CounterData getCounterData(int id)
{
   return patch_data.getCounterData(id);
}



/********************************************************************************/
/*										*/
/*	Methods to handle user settings for monitoring				*/
/*										*/
/********************************************************************************/

private synchronized void handleProcessEnable(String pid,String val)
{
   DymonProcess dp = findProcess(pid);
   if (dp == null) return;
   boolean fg;
   if (val == null || val.equals("*")) fg = !dp.isMonitoringEnabled();
   else if (val.startsWith("t") || val.startsWith("T") || val.startsWith("1")) fg = true;
   else fg = false;

   dp.enableMonitoring(fg);

   ++change_count;
}



private synchronized void handleProcessReporting(String pid,String val)
{
   DymonProcess dp = findProcess(pid);
   if (dp == null) return;
   boolean fg;
   if (val == null || val.equals("*")) fg = true;
   else if (val.startsWith("t") || val.startsWith("T") || val.startsWith("1")) fg = true;
   else fg = false;

   dp.enableReporting(fg);

   ++change_count;
}



private void handleProcessClear(String pid,String what)
{
   DymonProcess dp = findProcess(pid);

   if (dp == null) return;

   dp.doClear(what);
}




private void handleProcessOverhead(String pid,String what)
{
   DymonProcess dp = findProcess(pid);

   if (dp == null) return;

   double ovhd = DYMON_DEFAULT_OVERHEAD;
   try {
      if (what != null) ovhd = Double.parseDouble(what);
    }
   catch (NumberFormatException e) { }
   dp.setAllowedOverhead(ovhd);
}



private void handleProcessGC(String pid)
{
   DymonProcess dp = findProcess(pid);

   if (dp == null) return;

   dp.sendDyperMessage("GC",null,null,0);
}




private void handleProcessAgents(String pid,String agts)
{
   DymonProcess dp = findProcess(pid);

   if (dp == null) return;

   dp.setAgentSet(agts);
}




private void handleProcessShowHeap(String pid)
{
   DymonProcess dp = findProcess(pid);

   if (dp == null) return;

   DymonAgent hagt = dp.findAgent("HEAP");
   for (DymonDetailing dd : hagt.getDetailings()) {
      if (!dd.isDetailing()) {
	 DymonPatchRequest dpr = dd.setDetailing(0,0);
	 requestPatch(dpr);
       }
      break;
    }
}



private String handleProcessDumpHeap(String pid,String file,boolean live)
{
   DymonProcess dp = findProcess(pid);

   if (dp == null) return null;

   String arg = "<DUMP FILE='" + file + "' LIVE='" + live + "' />";
   MintDefaultReply mdr = new MintDefaultReply();

   dp.sendDyperMessage("DUMPHEAP",arg,mdr,MINT_MSG_FIRST_NON_NULL);

   String rslt = mdr.waitForString();

   return rslt;
}




private String handleProcessDumpMemory(String pid,String file)
{
   DymonProcess dp = findProcess(pid);

   if (dp == null) return null;

   String arg = "<DUMP FILE='" + file + "' />";
   MintDefaultReply mdr = new MintDefaultReply();

   dp.sendDyperMessage("DUMPMEMORY",arg,mdr,MINT_MSG_FIRST_NON_NULL);

   String rslt = mdr.waitForString();

   return rslt;
}




private String handleListAgents(String pid)
{
   Collection<String> agts = null;

   if (pid != null) {
      DymonProcess dp = findProcess(pid);
      if (dp == null) return null;
      agts = dp.listAllAgents();
    }
   else {
      agts = DymonAgentManager.getAllAgents();
    }

   StringBuffer buf = new StringBuffer();
   for (String s : agts) {
      if (buf.length() > 0) buf.append(",");
      buf.append(s);
    }

   return buf.toString();
}




private void handleProcessAttach(String pid,String val)
{
   DymonProcess dp = findProcess(pid);

   if (dp == null) return;

   boolean fg;
   if (val == null || val.equals("*")) fg = !dp.isAttached();
   else if (val.startsWith("t") || val.startsWith("T") || val.startsWith("1")) fg = true;
   else fg = false;

   if (fg == dp.isAttached()) return;

   if (fg) {
      AttachThread ath = new AttachThread(dp);
      ath.start();
    }
   else {
      dp.detach();
      ++change_count;
    }
}



private class AttachThread extends Thread {

   private DymonProcess for_process;

   AttachThread(DymonProcess p) {
      super("AttachThread_" + p.getProcessId());
      for_process = p;
    }

   public void run() {
      dymon_attach.attach(for_process);
      ++change_count;
    }

}	// end of subclass PatchThread






/********************************************************************************/
/*										*/
/*	Query methods for sharing data among agents				*/
/*										*/
/********************************************************************************/

Map<String,Number> handleSimpleQuery(String pid,String agent,String id)
{
   DymonProcess dp = findProcess(pid);

   if (dp == null) return null;

   return dp.handleSimpleQuery(agent,id);
}



/********************************************************************************/
/*										*/
/*	UIServer socket management						*/
/*										*/
/********************************************************************************/

private class UIServer extends Thread {

   private ServerSocket uiserver_socket;

   UIServer() {
      super("DYMON_UISERVER_ACCEPT");
      // setDaemon(true);
      try {
	 uiserver_socket = new ServerSocket(0);
       }
      catch (IOException e) {
	 System.err.println("DYMON: Problem creating server socket: " + e);
	 System.exit(1);
       }
    }

   public void run() {
      try {
	 FileWriter fw = new FileWriter(DYMON_UI_SERVER_SOCKET);
	 PrintWriter pw = new PrintWriter(fw);
	 InetAddress iad = InetAddress.getLocalHost();
	 pw.println(iad.getHostAddress() + "\t" + uiserver_socket.getLocalPort());
	 pw.close();
	 System.err.println("DYMON: UI server set on port " + uiserver_socket.getLocalPort());
	 if (web_access && System.getenv("BROWN_DYMON_WEB") != null) {
	    fw = new FileWriter(DYMON_UI_WEB_SERVER_SOCKET);
	    pw = new PrintWriter(fw);
	    iad = InetAddress.getLocalHost();
	    pw.println(iad.getHostAddress() + "\t" + uiserver_socket.getLocalPort());
	    pw.close();
	    System.err.println("DYMON: WEB server set on port " + uiserver_socket.getLocalPort());
	  }
       }
      catch (IOException e) {
	 System.err.println("DYMON: Problem creating server socket file: " + e);
	 System.exit(1);
       }

      try {
	 for ( ; ; ) {
	    Socket s = uiserver_socket.accept();
	    createClient(s);
	  }
       }
      catch (IOException e) {
	 System.err.println("DYMON: Problem with ui server accept: " + e);
	 System.exit(1);
       }
    }

}	// end of subclass UIServer



/********************************************************************************/
/*										*/
/*	User Interface client management					*/
/*										*/
/********************************************************************************/

private synchronized void createClient(Socket s)
{
   try {
      UIClient c = new UIClient(s);
      ui_clients.add(c);
      c.start();
    }
   catch (IOException e) {
      System.err.println("DYMON: Problem creating UI client: " + e);
    }
}


private synchronized void removeClient(UIClient c)
{
   ui_clients.remove(c);
}


private void processUI(UIClient c,String data)
{
   if (data == null) return;

   String rslt = null;

   // System.err.println("DYMON: UI REQUEST: " + data);

   StringTokenizer tok = new StringTokenizer(data);
   if (tok.hasMoreTokens()) {
      String cmd = tok.nextToken();
      try {
	 rslt = processUICommand(c,cmd,tok);
       }
      catch (Throwable t) {
	 System.err.println("DYMON: Problem with UI Command '" + data + "': " + t);
	 t.printStackTrace();
       }
    }

   // System.err.println("DYMON: SEND TO UI: " + rslt);

   c.send(rslt);
}



private String processUICommand(UIClient c,String cmd,StringTokenizer args)
{
   if (cmd.equals("PTABLE")) {
      if (args.hasMoreTokens()) {
	 int id = Integer.parseInt(args.nextToken());
	 if (id > 0 && id == change_count) return null;
       }
      return dumpProcessTable();
    }
   else if (cmd.equals("ANALYSIS")) {
      if (args.hasMoreTokens()) {
	 String pid = args.nextToken();
	 String what = "*";
	 if (args.hasMoreTokens()) what = args.nextToken();
	 IvyXmlWriter xw = new IvyXmlWriter();
	 doAnalysis(pid,what,xw);
	 if (xw.toString().equals("")) return null;
	 return xw.toString();
       }
    }
   else if (cmd.equals("ENABLE")) {
      if (args.hasMoreTokens()) {
	 String pid = args.nextToken();
	 String value = null;
	 if (args.hasMoreTokens()) value = args.nextToken();
	 handleProcessEnable(pid,value);
	 ++change_count;
       }
    }
   else if (cmd.equals("REPORTING")) {
      if (args.hasMoreTokens()) {
	 String pid = args.nextToken();
	 if (args.hasMoreTokens()) {
	    String value = args.nextToken();
	    handleProcessReporting(pid,value);
	  }
       }
    }
   else if (cmd.equals("CLEAR")) {
      if (args.hasMoreTokens()) {
	 String pid = args.nextToken();
	 String what = null;
	 if (args.hasMoreTokens()) what = args.nextToken();
	 handleProcessClear(pid,what);
       }
    }
   else if (cmd.equals("OVERHEAD")) {
      if (args.hasMoreTokens()) {
	 String pid = args.nextToken();
	 String what = null;
	 if (args.hasMoreTokens()) what = args.nextToken();
	 handleProcessOverhead(pid,what);
       }
    }
   else if (cmd.equals("ATTACH")) {
      if (args.hasMoreTokens()) {
	 String pid = args.nextToken();
	 String value = null;
	 if (args.hasMoreTokens()) value = args.nextToken();
	 handleProcessAttach(pid,value);
       }
    }
   else if (cmd.equals("LASTREPORT")) {
      if (args.hasMoreTokens()) {
	 String pid = args.nextToken();
	 IvyXmlWriter xw = new IvyXmlWriter();
	 showLastReport(pid,xw);
	 return xw.toString();
       }
    }
   else if (cmd.equals("GC")) {
      if (args.hasMoreTokens()) {
	 String pid = args.nextToken();
	 handleProcessGC(pid);
       }
    }
   else if (cmd.equals("SHOWHEAP")) {
      if (args.hasMoreTokens()) {
	 String pid = args.nextToken();
	 handleProcessShowHeap(pid);
       }
    }
   else if (cmd.equals("DUMPHEAP")) {
      if (args.hasMoreTokens()) {
	 String pid = args.nextToken();
	 String file = "heap.dump";
	 boolean live = true;
	 if (args.hasMoreTokens()) file = args.nextToken();
	 if (args.hasMoreTokens()) live = Boolean.getBoolean(args.nextToken());
	 return handleProcessDumpHeap(pid,file,live);
       }
    }
   else if (cmd.equals("DUMPMEMORY")) {
      if (args.hasMoreTokens()) {
	 String pid = args.nextToken();
	 String file = "heap.dump";
	 if (args.hasMoreTokens()) file = args.nextToken();
	 return handleProcessDumpMemory(pid,file);
       }
    }
   else if (cmd.equals("SETUP")) {
      if (args.hasMoreTokens()) {
	 String home = args.nextToken();
	 String host = null;
	 if (args.hasMoreTokens()) host = args.nextToken();
	 c.setup(home,host);
       }
    }
   else if (cmd.equals("HOST")) {
      if (args.hasMoreTokens()) {
	 String hcmd = args.nextToken();
	 String host = null;
	 if (args.hasMoreTokens()) host = args.nextToken();
	 return handleHosts(hcmd,host);
       }
    }
   else if (cmd.equals("AGENTS")) {
      if (args.hasMoreTokens()) {
	 String pid = args.nextToken();
	 String agts = null;
	 if (args.hasMoreTokens()) agts = args.nextToken();
	 handleProcessAgents(pid,agts);
      }
    }
   else if (cmd.equals("LISTAGENTS")) {
      String pid = null;
      if (args.hasMoreTokens()) pid = args.nextToken();
      return handleListAgents(pid);
    }
   else if (cmd.equals("CHECK")) {
      if (args.hasMoreTokens()) {
	 String pid = args.nextToken();
	 if (pid.equals("LOCAL")) checkProcesses(true,false);
	 else if (pid.equals("REMOTE")) checkProcesses(false,true);
	 else if (pid.equals("BOTH")) checkProcesses(true,true);
	 else {
	    return handleProcessCheck(pid);
	  }
       }
    }
   else {
      System.err.println("DYMON: Unknown command " + cmd);
    }

   return null;
}




private class UIClient extends Thread {

   private Socket client_socket;
   private BufferedReader input_reader;
   private PrintWriter print_writer;
   private DymonResources resource_set;

   UIClient(Socket s) throws IOException {
      super("DymonUIClient_" + s.getRemoteSocketAddress());
      client_socket = s;
      input_reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
      print_writer = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
      resource_set = new DymonResources();
      resource_set.loadFile(DYMON_GLOBAL_RESOURCE_FILE);
    }

   public void send(String s) {
      if (s != null) print_writer.println(s);
      print_writer.println(DYMON_UI_EOM);
      print_writer.flush();
    }

   public void run() {
      for ( ; ; ) {
	 try {
	    String x = input_reader.readLine();
	    if (x == null) break;
	    // System.err.println("DYMON: UI READ: " + x);
	    processUI(this,x);
	  }
	 catch (IOException e) {
	    System.err.println("DYMON: Problem reading from UI: " + e);
	    break;
	  }
       }
      // System.err.println("DYMON: CLIENT " + client_socket.getRemoteSocketAddress() + " EXITING");
      try {
	 client_socket.close();
       }
      catch (IOException e) { }
      removeClient(this);
    }

   public void setup(String home,String host) {
      if (home != null) {
	 String fnm = home + File.separator + DYMON_RESOURCE_FILE_NAME;
	 resource_set.loadFile(fnm);
       }
      if (host != null) resource_set.addHost(host);
    }


}	// end of subclass UIClient




/********************************************************************************/
/*										*/
/*	Methods to handle MSG-based communication				*/
/*										*/
/********************************************************************************/


private class UICommand implements MintHandler {

   public void receive(MintMessage msg,MintArguments args) {
      String data = args.getArgument(0);
      String rslt = null;
      StringTokenizer tok = new StringTokenizer(data);
      if (tok.hasMoreTokens()) {
	 String cmd = tok.nextToken();
	 try {
	    rslt = processUICommand(null,cmd,tok);
	  }
	 catch (Throwable t) {
	    System.err.println("DYMON: Problem with MSG UI Command '" + data + "': " + t);
	    t.printStackTrace();
	  }
       }
      msg.replyTo(rslt);
    }

}	// end of subclass EndCommand




/********************************************************************************/
/*										*/
/*	Start up dyper server							*/
/*										*/
/********************************************************************************/

private void startDyperServer()
{
   server_host = null;
   server_port = null;
   List<String> args = new ArrayList<String>();
   args.add("java");
   args.add("-cp");
   args.add(System.getProperty("java.class.path"));
   args.add("edu.brown.cs.dyvise.dymon.DymonDyperServer");
   args.add("-M");
   args.add(DymonRemote.getMintName());

   for (int i = 0; i < 5; ++i) {
      MintDefaultReply rply = new MintDefaultReply();
      msg_handler.send("<DYMON CMD='PORT' />",rply,MINT_MSG_FIRST_NON_NULL);
      Element rslt = rply.waitForXml(10000);
      if (rslt != null && IvyXml.isElement(rslt,"SOCKET")) {
	 server_host = fixHost(IvyXml.getAttrString(rslt,"HOST"));
	 server_port = IvyXml.getAttrString(rslt,"PORT");
	 break;
       }
      if (i == 0) {
	 try {
	    new IvyExec(args,null,0);
	  }
	 catch (IOException e) {
	    break;
	  }
       }
      try {
	 Thread.sleep(1000);
       }
      catch (InterruptedException e) { }
    }

   System.err.println("DYMON: HOST/PORT " + server_host + "@" + server_port);

   if (server_host != null) {
      try {
	 dyperserver_socket = new Socket(server_host,Integer.parseInt(server_port));
	 // s.close();
       }
      catch (Throwable t) {
	 System.err.println("DYMON: Problem connecting to dymondyperserver: " + t);
       }
    }
}


String getAttachArgs()
{
   String args = DYMON_ATTACH_ARGS;
   args += ":HOST=" + server_host;
   args += ":PORT=" + server_port;
   return args;
}



private static String fixHost(String h)
{
   if (h == null) return null;

   try {
      String h1 = InetAddress.getLocalHost().getHostName();
      String h2 = InetAddress.getLocalHost().getHostAddress();
      String h3 = InetAddress.getLocalHost().getCanonicalHostName();
      if (h.equals(h1) || h.equals(h2) || h.equals(h3)) {
	 return "127.0.0.1";
       }
    }
   catch (UnknownHostException e) { }

   return h;
}




}	// end of class DymonMain




/* end of DymonMain.java */

