/********************************************************************************/
/*										*/
/*		DylockCollector.java						*/
/*										*/
/*	DYVISE lock analysis lock collector main program			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylock/DylockCollector.java,v 1.4 2013/09/04 20:34:32 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylockCollector.java,v $
 * Revision 1.4  2013/09/04 20:34:32  spr
 * Add -java option to allow running 1.6 applications.
 *
 * Revision 1.3  2013-05-09 12:28:59  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.2  2012-10-05 00:52:41  spr
 * Update lock analysis.  Prepare for pattern display.
 *
 * Revision 1.1  2011-03-10 02:24:59  spr
 * New version of dylock to produce visualizations.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dylock;

import edu.brown.cs.ivy.exec.*;
import edu.brown.cs.ivy.file.*;

import com.sun.tools.attach.VirtualMachine;

import java.util.*;
import java.io.*;



public class DylockCollector implements DylockConstants, DylockConstants.DylockExec
{



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   DylockCollector dc = new DylockCollector(args);
   dc.process();
}



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private List<String>	arg_list;
private String		remote_process;
private String		agent_args;
private String		agent_path;
private String		java_name;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DylockCollector(String [] args)
{
   arg_list = new ArrayList<String>();
   remote_process = null;
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
   String otf = "locks";
   boolean full = false;

   int start = 0;
   if (args.length > 0) {
      if (args[0].startsWith("-c")) start = 1;
    }

   while (args.length >= start+2) {
      if (args[start].startsWith("-o")) {
	 otf = args[start+1];
	 start += 2;
       }
      else if (args[start].startsWith("-p")) {
	 remote_process = args[start+1];
	 start += 2;
       }
      else if (args[start].startsWith("-java")) {
	 java_name = args[start+1];
	 start += 2;
       }
      else if (args[start].startsWith("-full")) {
	 full = true;
	 start += 1;
       }
      else break;
    }

   agent_args = "OUTPUT=" + otf;
   if (full) agent_args += ":FULL";
   agent_path = IvyFile.expandName("$(BROWN_DYVISE_DYVISE)/lib/dylate.jar");
   String a0 = "-javaagent:" + agent_path + "=" + agent_args;
   arg_list.add(a0);

   if (remote_process == null && args.length <= start) badArgs();

   for (int i = start; i < args.length; ++i) {
      if (args[i].equals("-cp") || args[i].equals("-classpath")) {
	 arg_list.add("-Xbootclasspath/a:" + args[i+1]);
	 break;
       }
    }

   for (int i = start; i < args.length; ++i) {
      // should get start class from command line and save for later
      // actually should save all arguments for later running
      arg_list.add(args[i]);
    }
}




private void badArgs()
{
   System.err.println("DYLOCK: dylock -c [-o <output>] <javaargs>");
   System.exit(1);
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

@Override public void process()
{
   if (remote_process != null) {
      processRemote();
      return;
    }

   String java = java_name;
   if (System.getenv("BROWN_DYVISE_DEBUG") != null) java = "djava";

   arg_list.add(0,java);
   String [] argarr = new String[arg_list.size()];
   argarr = arg_list.toArray(argarr);

   System.err.println("DYLOCK: Run: ");
   for (String s : argarr) System.err.println("DYLOCK:\t\t" + s);

   try {
      IvyExec exec = new IvyExec(argarr,null,0);
      int sts = exec.waitFor();
      System.err.println("DYLOCK: Run finished " + sts);
    }
   catch (IOException e) {
      System.err.println("DYLOCK: Problem running java: " + e);
    }
}


private void processRemote()
{
   VirtualMachine vm = null;

   //TODO: should check if the process is remote or local

   try {
      String pid = remote_process;
      int idx = pid.indexOf("@");
      if (idx >= 0) pid = pid.substring(0,idx);
      vm = VirtualMachine.attach(pid);
      vm.loadAgentLibrary(agent_path,agent_args);
    }
   catch (Exception e) {
      System.err.println("DYLOCK: Failed to attach to " + remote_process + ": " + e);
      System.exit(1);
    }
   finally {
      try {
	 if (vm != null) vm.detach();
       }
      catch (IOException e) { }
    }

   // pop up a dialog box

   // then detach the agent
}




}	// end of class DylockCollector




/* end of DylockCollector.java */
