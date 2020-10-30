/********************************************************************************/
/*										*/
/*		DymonAttach.java						*/
/*										*/
/*	DYPER monitor code for attaching to a Java process			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymon/DymonAttach.java,v 1.6 2016/11/02 18:59:12 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymonAttach.java,v $
 * Revision 1.6  2016/11/02 18:59:12  spr
 * Move to asm5
 *
 * Revision 1.5  2010-03-30 16:22:05  spr
 * Clean up the code.
 *
 * Revision 1.4  2009-09-19 00:09:46  spr
 * Update dymon for seletive agent insertion and removal.
 *
 * Revision 1.3  2009-04-20 23:23:30  spr
 * Updates to make things work on the mac.  Fix bug in dymti.
 *
 * Revision 1.2  2009-03-20 02:06:50  spr
 * Update dymon: add time-based information, updated memory, etc.
 *
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymon;


import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.exec.IvyExecQuery;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.io.*;
import java.util.*;




public class DymonAttach implements DymonConstants
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private DymonMain	dymon_main;
private String		local_host;
private Timer		kill_timer;

private static final long	KILL_TIME = 10*1000;

private static Collection<String> ignore_classes;

static {
   ignore_classes = new ArrayList<String>();
   ignore_classes.add("sun.rmi.registry.RegistryImpl");
   ignore_classes.add("edu.brown.cs.dyvise.dymaster.DymasterMain");
   ignore_classes.add("edu.brown.cs.dyvise.dypatch.DypatchMain");
   ignore_classes.add("edu.brown.cs.ivy.mint.server.MintServer");
   ignore_classes.add("edu.brown.cs.dyvise.dymon.DymonMain");
   ignore_classes.add("edu.brown.cs.dyvise.dymon.DymonDyperServer");
   ignore_classes.add("edu.brown.cs.dyvise.dyvision.DyvisionMain");
   ignore_classes.add("edu.brown.cs.dyvise.");
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DymonAttach(DymonMain dm)
{
   dymon_main = dm;

   local_host = IvyExecQuery.getHostName();

   kill_timer = new Timer("DymonAttachKillTimer",true);
}



/********************************************************************************/
/*										*/
/*	Method to return new unattached processes				*/
/*										*/
/********************************************************************************/

void checkRemoteProcesses(Set<DymonProcess> check)
{
   for (String rem : dymon_main.getRemoteHosts()) {
      if (!rem.equals(local_host)) checkRemoteHost(rem,check);
    }

   if (check != null) {
      for (DymonProcess dp : check) {
	 String host = dp.getHost();
	 if (host != null && !host.equals(local_host) && host.length() != 0) {
	    // System.err.println("DYMON: Remove attachable process not found: " + dp.getProcessId());
	    dymon_main.removeProcess(dp);
	  }
       }
    }
}




void checkLocalProcesses(Set<DymonProcess> check)
{
   for (String rem : dymon_main.getRemoteHosts()) {
      if (rem.equals(local_host)) checkLocalHost(check);
    }

   for (Iterator<DymonProcess> it = check.iterator(); it.hasNext(); ) {
      DymonProcess dp = it.next();
      if (dp.isIBM()) {
	 // check for IBM here
	 it.remove();
       }
    }

   if (check != null) {
      for (DymonProcess dp : check) {
	 String host = dp.getHost();
	 if (host == null || host.equals(local_host) || host.length() == 0) {
	    // System.err.println("DYMON: Attachable local process not found: " + dp.getProcessId());
	    dymon_main.removeProcess(dp);
	  }
       }
    }
}



void checkProcesses(Set<DymonProcess> check)
{
   for (String rem : dymon_main.getRemoteHosts()) {
      if (rem.equals(local_host)) checkLocalHost(check);
      else checkRemoteHost(rem,check);
    }

   for (Iterator<DymonProcess> it = check.iterator(); it.hasNext(); ) {
      DymonProcess dp = it.next();
      if (dp.isIBM()) {
	 // check for IBM here
	 it.remove();
       }
    }

   if (check != null) {
      for (DymonProcess dp : check) {
	 // System.err.println("DYMON: Attachable process not found: " + dp.getProcessId());
	 dymon_main.removeProcess(dp);
       }
    }
}



private void checkLocalHost(Set<DymonProcess> check)
{
   // System.err.println("CHECK LOCAL HOST " + local_host);

   List<VirtualMachineDescriptor> active = VirtualMachine.list();
   String cid = IvyExecQuery.getProcessId();

   for (VirtualMachineDescriptor vmd : active) {
      String pid = vmd.id();
      if (pid.equals(cid)) continue;
      String procid = pid + "@" + local_host;
      DymonProcess dp = dymon_main.findProcess(procid);
      if (dp != null) {
	 if (check != null) {
	    if (!dp.checkAlive()) check.remove(dp);
	  }
	 continue;
       }
      String cmdargs = vmd.displayName();
      if (checkIgnore(cmdargs)) continue;
      dp = new DymonProcess(dymon_main,procid,cmdargs,false);
      dymon_main.addProcess(dp);
    }
}




private void checkRemoteHost(String rem,Set<DymonProcess> check)
{
   try {
      String cmd = "ssh " + rem + " " + DYMON_REMOTE_ATTACH + " -q";
      IvyExec ex = new IvyExec(cmd,IvyExec.READ_OUTPUT);
      // System.err.println("DYMON: Execute: " + cmd);
      BufferedReader br = new BufferedReader(new InputStreamReader(ex.getInputStream()));
      KillTask kt = new KillTask(ex);
      kill_timer.schedule(kt,KILL_TIME);

      for ( ; ; ) {
	 String ln = br.readLine();
	 if (ln == null) break;
	 if (ln.startsWith("ATTACH\t")) {
	    StringTokenizer tok = new StringTokenizer(ln,"\t");
	    tok.nextToken();
	    String pid = tok.nextToken();
	    String args = null;
	    if (tok.hasMoreTokens()) args = tok.nextToken();
	    else continue;
	    if (checkIgnore(args)) continue;
	    DymonProcess dp = dymon_main.findProcess(pid);
	    if (dp != null) {
	       if (check != null) check.remove(dp);
	       continue;
	     }
	    dp = new DymonProcess(dymon_main,pid,args,false);
	    dymon_main.addProcess(dp);
	  }
       }
      br.close();
      int sts = ex.waitFor();
      kt.cancel();
      if (sts != 0 && sts != 143) {
	 System.err.println("DYMON: Status " + sts + ": " + cmd);
       }
      ex.destroy();
    }
   catch (IOException e) {
      System.err.println("DYMON: Problem checking remote processes on " + rem + ": " + e);
    }
}



private boolean checkIgnore(String args)
{
   String cmd = args;
   int idx = args.indexOf(' ');
   if (idx > 0) cmd = cmd.substring(0,idx);

   for (String s : ignore_classes) {
      if (args.startsWith(s)) return true;
    }

   if (dymon_main.getBooleanResource(cmd,"IGNORE")) return true;

   return false;
}



private static class KillTask extends TimerTask {

   private IvyExec attach_process;

   KillTask(IvyExec ex) {
      attach_process = ex;
    }

   public void run() {
      if (attach_process.isRunning()) {
	 attach_process.destroy();
       }
    }

}	// end of subclass KillTask




/********************************************************************************/
/*										*/
/*	Method to attach to a process						*/
/*										*/
/********************************************************************************/

boolean attach(DymonProcess dp)
{
   if (dp.isAttached()) return true;

   String rhost = dp.getRemoteHost();
   if (rhost != null) return remoteAttach(rhost,dp);

   boolean fg = false;
   VirtualMachine vm = null;

   if (dp.isIBM()) {
      return true;			// handle IBM here
    }

   try {
      String pid = dp.getProcessId();
      int idx = pid.indexOf('@');
      if (idx >= 0) pid = pid.substring(0,idx);
      String args = dymon_main.getAttachArgs();
      vm = VirtualMachine.attach(pid);
      System.err.println("DYMON: ATTACH: LOAD AGENT: " + DYPER_PATCH_JAR + " for " + pid +
			    " using " + args);
      vm.loadAgent(DYPER_PATCH_JAR,args);
      fg = dp.attach();
      if (!fg) System.err.println("DYMON: Problem attaching process: no response");
    }
   catch (Throwable e) {
      System.err.println("DYMON: Process attach failure for " + dp.getProcessId() +": " + e);
    }
   finally {
      try {
	 if (vm != null) vm.detach();
       }
      catch (IOException e) { }
    }

   return fg;
}




boolean remoteAttach(String host,DymonProcess dp)
{
   String cmd = "ssh " + host + " " + DYMON_REMOTE_ATTACH + " -p " + dp.getProcessId() +
      " -a " + DYPER_PATCH_JAR;

   // System.err.println("DYMON: Remote: " + cmd);

   try {
      IvyExec ex = new IvyExec(cmd);
      int sts = ex.waitFor();
      ex.destroy();
      if (sts == 0) return true;
    }
   catch (IOException e) {
      System.err.println("DYMON: Problem executing remote attach: " + e);
    }

   return false;
}




/********************************************************************************/
/*										*/
/*	Method to attach a vjmti agent						*/
/*										*/
/********************************************************************************/

boolean attachVjmtiAgent(DymonProcess dp,String lib,String args)
{
   if (!dp.isAttached()) return false;

   String vargs = "PID=" + dp.getProcessId() + ",MID=" + DYPER_MESSAGE_BUS;
   if (args != null) vargs += "," + args;

   String rhost = dp.getRemoteHost();
   if (rhost != null) return remoteVjmtiAttach(rhost,dp,lib,vargs);

   boolean fg = false;

   VirtualMachine vm = null;

   try {
      String pid = dp.getProcessId();
      int idx = pid.indexOf('@');
      if (idx >= 0) pid = pid.substring(0,idx);
      vm = VirtualMachine.attach(pid);
      vm.loadAgentPath(lib,vargs);
      fg = true;
    }
   catch (Exception e) {
      System.err.println("DYMON: Vjmti agent attach failure for " + dp.getProcessId() + ": " + e);
      System.err.println("DYMON: Agent = " + lib);
      System.err.println("DYMON: Args = " + vargs);
      System.err.println("DYMON: path = " + System.getenv("DYLD_LIBRARY_PATH"));
      e.printStackTrace();
    }
   finally {
      try {
	 if (vm != null) vm.detach();
       }
      catch (IOException e) { }
    }

   return fg;
}



boolean remoteVjmtiAttach(String host,DymonProcess dp,String lib,String args)
{
   String cmd = "ssh " + host + " " + DYMON_REMOTE_ATTACH + " -p " + dp.getProcessId();
   cmd += " " + lib;
   if (args != null) cmd += " " + args;

   System.err.println("DYMON: Remote: " + cmd);

   try {
      IvyExec ex = new IvyExec(cmd);
      int sts = ex.waitFor();
      if (sts == 0) return true;
      ex.destroy();
    }
   catch (IOException e) {
      System.err.println("DYMON: Problem executing remote vjmti attach: " + e);
    }

   return false;
}





/********************************************************************************/
/*										*/
/*	Test programs								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   List<VirtualMachineDescriptor> current = VirtualMachine.list();

   for (VirtualMachineDescriptor vmd : current) {
      System.err.println("VM " + vmd.displayName() + " " + vmd.id() + " " +
			    vmd.provider().name() + " " + vmd);
    }

   System.err.println("CURRENT PROCESS = " + IvyExecQuery.getProcessId());
   String [] argv = IvyExecQuery.getCommandLine();
   for (int i = 0; i < argv.length; ++i) {
      System.err.println("ARG " + i + ": " + argv[i]);
    }

}




}	// end of class DymonAttach



/* end of DymonAttach.java */
