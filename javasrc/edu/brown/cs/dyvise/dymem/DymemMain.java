/********************************************************************************/
/*										*/
/*		DymemMain.java							*/
/*										*/
/*	Main program for running memory visualizer separately			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymem/DymemMain.java,v 1.3 2010-03-30 16:21:56 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymemMain.java,v $
 * Revision 1.3  2010-03-30 16:21:56  spr
 * Bug fixes and feature enhancements.  Start of interface for cycle elimination.
 *
 * Revision 1.2  2009-10-07 22:39:49  spr
 * Eclipse code cleanup.
 *
 * Revision 1.1  2009-10-07 01:00:58  spr
 * Add dymem main program.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymem;


import edu.brown.cs.dyvise.dyvise.*;
import edu.brown.cs.dyvise.dymon.*;

import edu.brown.cs.ivy.swing.SwingSetup;
import edu.brown.cs.ivy.exec.IvyExecQuery;

import javax.swing.*;
import java.util.*;



public class DymemMain implements DymemConstants, DyviseConstants
{



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   new SwingSetup();

   DymemMain dm = new DymemMain(args);

   dm.process();
}




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private String		process_id;
private DymemFrame	main_frame;
private DymonRemote	dymon_remote;
private String		start_class;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private DymemMain(String [] args)
{
   process_id = null;
   main_frame = null;
   dymon_remote = new DymonRemote();
   start_class = null;

   scanArgs(args);
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
	 if (args[i].startsWith("-p") && i+1 < args.length) {           // -p <process>
	    if (process_id != null) badArgs();
	    process_id = args[++i];
	  }
	 else if (args[i].startsWith("-s") && i+1 < args.length) {      // -s <start class>
	    start_class = args[++i];
	  }
	 else badArgs();
       }
      else if (process_id == null) process_id = args[i];
      else badArgs();
    }
}



private void badArgs()
{
   System.err.println("DYMEM: dymem [-p <process>] [-s <start class>]");
   System.exit(1);
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

private void process()
{
   if (process_id == null) {
      DyviseProcessChooser dpc = new DyviseProcessChooser(dymon_remote,start_class,"MONITOR");
      process_id = dpc.requestProcess(null);
      if (process_id == null) System.exit(0);
    }
   else if (!process_id.contains("@")) {
      process_id += "@" + IvyExecQuery.getHostName();
    }

   // System.err.println("DYMEM: Working on " + process_id);

   dymon_remote.dymonCommand("AGENTS " + process_id + " " + DYMEM_PATCH_AGENT);
   dymon_remote.dymonCommand("ENABLE " + process_id + " TRUE");
   dymon_remote.dymonCommand("ATTACH " + process_id + " TRUE");
   dymon_remote.dymonCommand("CLEAR " + process_id + " " + DYMEM_PATCH_AGENT);
   dymon_remote.dymonCommand("OVERHEAD " + process_id + " " + DYMEM_PATCH_OVERHEAD);

   boolean ready = false;
   boolean dead = false;
   while (!ready && !dead) {
      String sts = dymon_remote.dymonCommand("CHECK " + process_id);
      // System.err.println("STATUS FOR " + process_id + " = " + sts);
      if (sts == null) dead = true;
      else if (sts.equals("NOT_FOUND")) dead = true;
      else if (sts.equals("OK")) ready = true;
      else if (sts.equals("INACTIVE"));
      else if (sts.equals("PENDING") || sts.equals("NOT_ATTACHED")) ;
      else {
	 System.err.println("DYMEM: Unknown status: " + sts);
	 dead = true;
       }
      try {
	 Thread.sleep(100);
       }
      catch (InterruptedException e) { }
    }

   if (dead) {
      System.err.println("DYMEM: Warning: Process " + process_id + " exited");
    }

   dymon_remote.dymonCommand("ANALYSIS " + process_id + " HEAP");
   dymon_remote.dymonCommand("SHOWHEAP " + process_id);

   dymon_remote.scheduleEvery(new Updater(),DYMEM_UPDATE_TIME);

   main_frame = new DymemFrame(process_id,dymon_remote);
   main_frame.setVisible(true);
   main_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

   Shutdown sd = new Shutdown();
   Runtime rt = Runtime.getRuntime();
   rt.addShutdownHook(sd);
}



/********************************************************************************/
/*										*/
/*	Handle need to update							*/
/*										*/
/********************************************************************************/

private class Updater extends TimerTask {

   public void run() {
      String r = dymon_remote.dymonCommand("ANALYSIS " + process_id + " HEAP");

      if (r == null) {
	 System.err.println("DYMEM: No analysis");
	 cancel();
       }
    }

}	// end of inner class Updater




/********************************************************************************/
/*										*/
/*	Handle termination by removing tracing					*/
/*										*/
/********************************************************************************/

private class Shutdown extends Thread {

   Shutdown() {
      super("Dymem Shutdown " + process_id);
    }

   public void run() {
      System.err.println("DYMEM: Running shutdown");
      dymon_remote.dymonCommand("ENABLE " + process_id + " FALSE");
      dymon_remote.dymonCommand("ATTACH " + process_id + " FALSE");
    }

}	// end of inner class Shutdown



}	// end of class DymemMain



/* end of DymemMain.java */
