/********************************************************************************/
/*										*/
/*		DymonRemote.java						*/
/*										*/
/*	Class for using DYMON remotely						*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymon/DymonRemote.java,v 1.12 2016/11/02 18:59:13 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymonRemote.java,v $
 * Revision 1.12  2016/11/02 18:59:13  spr
 * Move to asm5
 *
 * Revision 1.11  2013/09/04 18:36:32  spr
 * Minor bug fixes.
 *
 * Revision 1.10  2011-03-10 02:26:34  spr
 * Code cleanup.
 *
 * Revision 1.9  2010-04-29 20:03:45  spr
 * Add some debugging for the mac.
 *
 * Revision 1.8  2010-03-30 16:22:05  spr
 * Clean up the code.
 *
 * Revision 1.7  2009-10-07 01:00:13  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.6  2009-09-27 23:57:44  spr
 * Updates for mac os/x.
 *
 * Revision 1.5  2009-09-19 00:09:46  spr
 * Update dymon for seletive agent insertion and removal.
 *
 * Revision 1.4  2009-06-04 18:53:50  spr
 * Set up for binary distribution.
 *
 * Revision 1.3  2009-03-20 02:06:51  spr
 * Update dymon: add time-based information, updated memory, etc.
 *
 * Revision 1.2  2008-11-24 23:59:42  spr
 * Update remote error handling.  Use address rather than name for hosts.
 *
 * Revision 1.1  2008-11-24 23:41:36  spr
 * Add DymonRemote for remote use of dymon.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymon;


import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.exec.IvySetup;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.mint.*;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.util.*;


public class DymonRemote implements DymonConstants, MintConstants {




/********************************************************************************/
/*										*/
/*	Exported interfaces							*/
/*										*/
/********************************************************************************/

public interface Patcher {

   void send(String cmd,String body,MintReply hdlr,int flags);
   void send(String cmd);
   void send(String cmd,String body);

}


public interface ProcessManager {

   List<String> findProcess(String cls);
   void forceUpdate();
   boolean checkFor(String pid);
   String getName(String pid);
   String getArgs(String pid);
   String getStartClass(String pid);

}



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Socket	dymon_socket;
private BufferedReader	dymon_reader;
private Writer	dymon_writer;
private ProcessHandler process_handler;

private Timer	dymon_timer;

public static boolean do_debug = false;

private static MintControl	mint_control = null;

private static MintSyncMode DEFAULT_SYNC = MintSyncMode.ONLY_REPLIES;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public DymonRemote()
{
   dymon_socket = null;
   dymon_reader = null;
   dymon_writer = null;
   process_handler = null;

   dymon_timer = new Timer("DymonRemoteTimer",true);

   setupDymonSocket();

   dymonCommand(IvyFile.expandName("SETUP $(HOME) $(HOST)"));
}




/********************************************************************************/
/*										*/
/*	Methods to handle recurrent events					*/
/*										*/
/********************************************************************************/

public synchronized void scheduleEvery(TimerTask tt,long every)
{
   dymon_timer.scheduleAtFixedRate(tt,every,every);
}



/********************************************************************************/
/*										*/
/*	Methods to issue commands to DYMON					*/
/*										*/
/********************************************************************************/

public String dymonCommand(String cmd)
{
   StringBuilder buf = new StringBuilder();

   if (do_debug) System.err.println("DYMON: SEND: " + cmd);

   try {
      synchronized (this) {
	 dymon_writer.write(cmd);
	 dymon_writer.write('\n');
	 dymon_writer.flush();

	 for ( ; ; ) {
	    String x = dymon_reader.readLine();
	    if (x == null) throw new IOException("Unexpected EOF from DYMON");
	    if (x.equals(DYMON_UI_EOM)) break;
	    buf.append(x);
	    buf.append("\n");
	  }
       }
    }
   catch (IOException e) {
      System.err.println("DYMONREMOTE: Problem communicating with DYMON: " + e);
      System.exit(2);
    }

   if (buf.length() == 0) return null;

   String r = buf.toString().trim();

   if (r.length() == 0) return null;

   return r;
}



/********************************************************************************/
/*										*/
/*	Methods to connect to dymon						*/
/*										*/
/********************************************************************************/

private void setupDymonSocket()
{
   if (openDymonSocket()) return;

   if (!startDymon()) {
      System.err.println("DYMONREMOTE: Can't start DYMON service");
      System.exit(1);
    }

   for (int i = 0; i < 1000; ++i) {
      if (openDymonSocket()) return;
      try {
	 Thread.sleep(1000);
       }
      catch (InterruptedException _e) { }
    }

   System.err.println("DYMONREMOTE: Problem connecting to DYMON service");
   System.exit(1);
}




private boolean openDymonSocket()
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

      if (do_debug) System.err.println("DYMONREMOTE: Connecting to dymon at " + port + "  @ " + host);

      dymon_socket = new Socket(host,port);
      dymon_reader = new BufferedReader(new InputStreamReader(dymon_socket.getInputStream()));
      dymon_writer = new OutputStreamWriter(dymon_socket.getOutputStream());
    }
   catch (ConnectException e) {
      System.err.println("DYMONREMOTE: Connection failed: " + e);
      return false;
    }
   catch (IOException e) {
      System.err.println("DYMONREMOTE: IO ERROR OPENING SOCKET: " + e);
      return false;
    }

   return true;
}




private boolean startDymon()
{
   try {
      /********
      String cmd = DYMON_REMOTE_COMMAND;
      File f = new File(cmd);
      if (f.exists()) {
	 new IvyExec(cmd);
	 return true;
       }
      ************/
      dyviseJava(DYMON_REMOTE_CLASS,null,null);
    }
   catch (IOException _e) {
      return false;
    }

   return true;
}



/********************************************************************************/
/*										*/
/*	Methods for managing the patcher					*/
/*										*/
/********************************************************************************/

public static Patcher getPatcher(String pid)
{
   return new PatchInfo(pid);
}




private static enum PatchState {
   NOT_RUNNING,
   STARTED,
   READY
}


private static class PatchInfo implements Patcher {

   String process_id;
   IvyExec patch_process;
   PatchState patch_state;

   PatchInfo(String pid) {
      getMintControl(DEFAULT_SYNC);		// ensure mint is connected
      process_id = pid;
      patch_process = null;
      patch_state = PatchState.NOT_RUNNING;
    }

   public void send(String cmd) 		{ send(cmd,null,null,0); }
   public void send(String cmd,String body)	{ send(cmd,body,null,0); }
   public void send(String cmd,String body,MintReply hdlr,int flags) {
      checkPatcher();
      localSend(cmd,body,hdlr,flags);
    }

   private void checkPatcher() {
      pingPatcher();
      if (patch_state == PatchState.READY) return;

      for ( ; ; ) {
	 synchronized (this) {
	    if (patch_process == null || patch_state == PatchState.NOT_RUNNING ||
		   !patch_process.isRunning()) {
	       if (patch_process != null) {
		  if (do_debug) System.err.println("DYMON: Restart patch " + patch_state + " " +
						      patch_process.isRunning());
		}
	       try {
		  File f = new File(DYMON_PATCH_COMMAND);
		  if (f.exists()) {
		     String cmd = DYMON_PATCH_COMMAND + " -P " + process_id;
		     if (do_debug) System.err.println("DYMON: RUN :" + cmd);
		     patch_process = new IvyExec(cmd);
		   }
		  else {
		     if (do_debug)
			System.err.println("DYMON: START " + DYMON_PATCH_CLASS + " for " + process_id);
		     patch_process = dyviseJava(DYMON_PATCH_CLASS,null,"-P " + process_id);
		   }
		  patch_state = PatchState.STARTED;
		}
	       catch (IOException e) {
		  System.err.println("DYMON: Problem running patcher: " + e);
		}
	     }
	    else if (patch_state == PatchState.READY) break;
	  }

	 pingPatcher();
	 if (patch_state == PatchState.READY) break;

	 try {
	    Thread.sleep(100);
	  }
	 catch (InterruptedException e) { }
       }
    }

   private void localSend(String cmd,String body,MintReply hdlr,int flags) {
      String s = "<DYPATCH PID='" + process_id + "'";

      if (cmd != null) s += " COMMAND='" + cmd + "'";
      s += ">";
      if (body != null) s += body;
      s += "</DYPATCH>";

      if (do_debug) System.err.println("DYMON: SEND PATCH MESSAGE: " + s);

      if (hdlr == null) mint_control.send(s);
      mint_control.send(s,hdlr,flags);
    }

   private void pingPatcher() {
      MintDefaultReply r = new MintDefaultReply();
      localSend("PING",null,r,MINT_MSG_FIRST_NON_NULL);
      if (r.waitForString(120000) != null) {
	 patch_state = PatchState.READY;
       }
      else if (patch_process == null || !patch_process.isRunning() ||
		  patch_state != PatchState.STARTED) {
	 if (do_debug) {
	    if (patch_process == null) System.err.println("DYMON: No patcher");
	    else System.err.println("DYMON: PING FAILED: " + patch_process.isRunning() + " " + patch_state);
	  }
	 patch_state = PatchState.NOT_RUNNING;
       }
    }

}	// end of class PatchInfo




/********************************************************************************/
/*										*/
/*	Methods to get mint handler for DYVISE functionality			*/
/*										*/
/********************************************************************************/

public synchronized static MintControl getMintControl()
{
   return getMintControl(DEFAULT_SYNC);
}



public synchronized static MintControl getMintControl(MintSyncMode mode)
{
   if (mint_control == null) {
      String msgbus = getMintName();
      mint_control = MintControl.create(msgbus,mode);
    }

   return mint_control;
}


public static String getMintName()
{
   return IvyFile.expandName("DYVISE_$(USER)");
}



/********************************************************************************/
/*										*/
/*	Methods to handle process management					*/
/*										*/
/********************************************************************************/

public synchronized ProcessManager getProcessManager()
{
   if (process_handler == null) {
      process_handler = new ProcessHandler();
    }

   return process_handler;
}



private class ProcessHandler implements ProcessManager {

   private String last_id;
   private List<ProcessData> process_list;

   ProcessHandler() {
      last_id = null;
      process_list = null;
    }

   public List<String> findProcess(String start) {
      getProcesses();

      List<String> rslt = new ArrayList<String>();

      if (process_list != null) {
	 for (ProcessData p : process_list) {
	    if (!p.isMonitored()) {
	       if (start == null || start.equals(p.getStartClass()) ||
		      p.getStartClass().endsWith(start)) {
		  rslt.add(p.getId());
		}
	     }
	  }
       }

      return rslt;
    }

   public boolean checkFor(String pid) {
      getProcesses();
      if (process_list == null) return false;

      for (ProcessData p : process_list) {
	 if (p.getId().equals(pid) && !p.isAttached()) return true;
       }

      return false;
    }

   public String getName(String pid) {
      if (process_list == null) return null;
      for (ProcessData p : process_list) {
	 if (p.getId().equals(pid)) return p.getName();
       }
      return null;
    }

   public String getStartClass(String pid) {
      if (process_list == null) return null;
      for (ProcessData p : process_list) {
	 if (p.getId().equals(pid)) return p.getStartClass();
       }
      return null;
    }

   public String getArgs(String pid) {
      if (process_list == null) return null;
      for (ProcessData p : process_list) {
	 if (p.getId().equals(pid)) return p.getArgs();
       }
      return null;
    }

   public void forceUpdate() {
      dymonCommand("CHECK LOCAL");
    }

   private void getProcesses() {
      String cmd = "PTABLE";
      if (last_id != null) cmd += " " + last_id;
      String drslt = dymonCommand(cmd);
      if (drslt == null) return;

      List<ProcessData> rslt = new ArrayList<ProcessData>();
      Element xml = IvyXml.convertStringToXml(drslt);
      last_id = IvyXml.getAttrString(xml,"COUNT");
      for (Element px : IvyXml.children(xml,"PROCESS")) {
	 ProcessData p = new ProcessData(px);
	 rslt.add(p);
       }

      process_list = rslt;
    }

}	 // end of innerclass ProcessHandler



private static class ProcessData {

   private String process_id;
   private String start_class;
   private String show_args;
   private String show_name;
   private boolean is_monitored;
   private boolean is_attached;

   ProcessData(Element xml) {
      process_id = IvyXml.getTextElement(xml,"ID");
      start_class = IvyXml.getTextElement(xml,"START");
      show_args = IvyXml.getTextElement(xml,"SHOWARGS");
      show_name = IvyXml.getTextElement(xml,"NAME");
      is_monitored = Boolean.valueOf(IvyXml.getTextElement(xml,"MONITOR"));
      is_attached = Boolean.valueOf(IvyXml.getTextElement(xml,"ATTACHED"));
    }

   String getId()			{ return process_id; }
   String getStartClass()		{ return start_class; }
   String getName()			{ return show_name; }
   String getArgs()			{ return show_args; }
   boolean isMonitored()		{ return is_monitored; }
   boolean isAttached() 		{ return is_attached; }

}	// end of subclass ProcessData




/********************************************************************************/
/*										*/
/*	Methods for running java						*/
/*										*/
/********************************************************************************/

private static final String DEFS = "'-DBROWN_IVY_IVY=$(IVY)' '-DBROWN_DYVISE_DYVISE=$(DYVISE)' '-DBROWN_DYVISE_JAVA_ARCH=$(JAVA_ARCH)'";
private static final String PATHS = IvyFile.expandName("$(HOME)/.dyvise/Props");


public static IvyExec dyviseJava(String cls,String jargs,String args) throws IOException
{
   dyviseSetup();

   String defs = IvyFile.expandName(DEFS);

   String libpath = System.getProperty("java.library.path");
   String lp1 = IvyFile.expandName("$(DYVISE)/lib/$(BROWN_DYVISE_JAVA_ARCH)");
   if (libpath == null) libpath = lp1;
   else if (!libpath.contains(lp1)) libpath += File.pathSeparator + lp1;
   String lp2 = IvyFile.expandName("$(IVY)/lib/$(BROWN_DYVISE_JAVA_ARCH)");
   if (!libpath.contains(lp2)) libpath += File.pathSeparator + lp2;

   String cp = IvyFile.expandName("$(DYVISEPATH)");
   if (cp.length() == 0) {
      String fjn = IvyFile.expandName("$(DYVISE)/dyvisefull.jar");
      File f = new File(fjn);
      if (f.exists()) cp = fjn;
    }

   if (cp.length() == 0) {
      String jdir = IvyFile.expandName("$(DYVISE)/java");
      File jf = new File(jdir);
      String cp1,cp2;
      if (jf.exists() && jf.isDirectory()) {
	 cp1 = "$(DYVISE)/java";
	 cp2 = "$(IVY)/java";
       }
      else {
	 cp1 = "$(DYVISE)/lib/dyvise.jar";
	 cp2 = "$(IVY)/lib/ivy.jar";
       }
      cp1 = IvyFile.expandName(cp1);
      cp2 = IvyFile.expandName(cp2);
      String cp3 = IvyFile.expandName("$(IVY)/lib/jikesbt.jar");
      String cp4 = IvyFile.expandName("$(DYVISE)/lib/quadprog.jar");
      String cp5 = IvyFile.expandName("$(IVY)/lib/tools.jar");
      cp = cp1 + File.pathSeparator + cp2 + File.pathSeparator + cp3 + File.pathSeparator +
	 cp4 + File.pathSeparator + cp5;
      String cp6 = IvyFile.expandName("$(IVY)/lib/postgresqlcopy.jar");
      File f6 = new File(cp6);
      if (f6.exists()) cp += File.pathSeparator + cp6;
      else {
	 cp6 = IvyFile.expandName("$(IVY)/lib/postgresql.jar");
	 f6 = new File(cp6);
	 if (f6.exists()) cp += File.pathSeparator + cp6;
       }
      cp6 = IvyFile.expandName("$(IVY)/lib/mysql.jar");
      f6 = new File(cp6);
      if (f6.exists()) cp += File.pathSeparator + cp6;
    }

   StringBuffer cmd = new StringBuffer();
   cmd.append("java ");
   cmd.append("-Xmx1024m ");
   cmd.append(defs + " ");
   cmd.append("'-Djava.library.path=" + libpath + "' ");
   cmd.append("-cp '" + cp + "' ");
   if (jargs != null) cmd.append(jargs + " ");
   cmd.append(cls);
   if (args != null) {
      cmd.append(" " + args);
    }

   IvyExec ex = new IvyExec(cmd.toString());

   return ex;
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

private static final String DIR = IvyFile.expandName("$(HOME)/.dyvise");


public static void dyviseSetup()
{
   IvySetup.setup();

   String check = IvyFile.expandName("$(IVY) $(DYVISE)");
   StringTokenizer tok = new StringTokenizer(check);
   if (tok.countTokens() < 2) {
      File df = new File(DIR);
      if (!df.exists()) df.mkdir();
      File pf = new File(PATHS);
      if (!pf.exists()) {
	 System.err.println("DYVISE: Dyvise is not setup up correctly.  Please either");
	 System.err.println("  run the setup program or do the appropriate setup scripts");
	 System.exit(1);
       }
      try {
	 Properties p = new Properties();
	 FileInputStream fis = new FileInputStream(pf);
	 p.loadFromXML(fis);
	 for (String pn : p.stringPropertyNames()) {
	    String pv = p.getProperty(pn);
	    System.setProperty(pn,pv);
	  }
	 fis.close();
       }
      catch (IOException e) {
	 System.err.println("DYVISE: Problem reading property file " + pf + ": " + e);
	 System.exit(1);
       }
    }
   check = IvyFile.expandName("$(IVY) $(DYVISE)");
   tok = new StringTokenizer(check);
   if (tok.countTokens() < 2) {
      System.err.println("DYVISE: IVY AND DYVISE properties not set: " + check);
      for (String pn : System.getProperties().stringPropertyNames()) {
	 System.err.println("\tPROPERTY: " + pn + " = " + System.getProperty(pn));
       }
      System.exit(1);
    }

   String arch = null;
   if (System.getProperty("os.name").startsWith("Linux")) {
      if (System.getProperty("os.arch").equals("amd64")) arch = "x86_64";
      else arch = "i386";
    }
   else if (System.getProperty("os.name").startsWith("Mac")) {
      arch = "mac64";
    }
   else if (System.getProperty("os.name").startsWith("Win")) {
      arch = "x86";
    }
   else {
      arch = System.getProperty("os.arch");
    }
   System.setProperty("BROWN_DYVISE_JAVA_ARCH",arch);
   System.setProperty("edu.brown.cs.dyvise.JAVA_ARCH",arch);
}





}	// end of class DymonRemote




/* end of DymonRemote.java */
