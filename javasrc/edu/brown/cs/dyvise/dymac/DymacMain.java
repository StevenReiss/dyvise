/********************************************************************************/
/*										*/
/*		DymacMain.java							*/
/*										*/
/*	DYVISE dyanmic analysis controller					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymac/DymacMain.java,v 1.4 2010-03-30 16:21:04 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymacMain.java,v $
 * Revision 1.4  2010-03-30 16:21:04  spr
 * Bug fixes in dynamic analysis.
 *
 * Revision 1.3  2009-10-07 00:59:45  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.2  2009-09-27 23:57:44  spr
 * Updates for mac os/x.
 *
 * Revision 1.1  2009-09-19 00:09:00  spr
 * Module to collect dynamic information from dymon about an applcation and store in database.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymac;


import edu.brown.cs.dyvise.dymon.DymonRemote;
import edu.brown.cs.dyvise.dyvise.DyviseConstants;
import edu.brown.cs.dyvise.dyvise.DyviseDatabase;
import edu.brown.cs.ivy.project.IvyProject;
import edu.brown.cs.ivy.project.IvyProjectManager;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.swing.*;

import org.w3c.dom.Element;

import java.util.*;



public class DymacMain implements DymacConstants, DyviseConstants
{


/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   DymacMain dm = new DymacMain(args);
   new SwingSetup();

   dm.process();
}



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IvyProject	user_project;

private List<DymacProflet> active_proflets;
private Map<String,DymacProflet> all_proflets;

private Map<String,String> name_map;
private String		process_id;
private String		start_class;

private DymonRemote	dymon_iface;

private boolean 	data_ready;
private boolean 	data_abort;

private int		stable_count;

private DyviseDatabase	sql_database;

private boolean 	do_debug;





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public DymacMain(IvyProject proj,Collection<String> proflets)
{
   this();

   user_project = proj;
   name_map.put("PROJECT",user_project.getName());

   for (String p : proflets) {
      DymacProflet pf = all_proflets.get(p);
      if (pf != null) active_proflets.add(pf);
      else {
	 System.err.println("DYMAC: Proflet " + p + " not found");
       }
    }
}



private DymacMain(String [] args)
{
   this();

   scanArgs(args);
}



private DymacMain()
{
   user_project = null;
   active_proflets = new ArrayList<DymacProflet>();
   name_map = new HashMap<String,String>();
   do_debug = false;
   process_id = null;
   start_class = null;
   dymon_iface = new DymonRemote();
   data_ready = false;
   data_abort = false;
   stable_count = 0;

   all_proflets = new HashMap<String,DymacProflet>();
   addProflet(new DymacProfletReaction(this));
   addProflet(new DymacProfletStates(this));
}




private void addProflet(DymacProflet dp)
{
   all_proflets.put(dp.getName(),dp);
}




/********************************************************************************/
/*										*/
/*	Argument scanning methods						*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   String projnm = null;
   String projdir = null;

   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
	 if (args[i].startsWith("-p") && i+1 < args.length) {           // -p <project>
	    projnm = args[++i];
	    name_map.put("PROJECT",projnm);
	  }
	 else if (args[i].startsWith("-d") && i+1 < args.length) {      // -d <project dir>
	    projdir = args[++i];
	    name_map.put("PROJDIR",projdir);
	  }
	 else if (args[i].startsWith("-D")) {                           // -DEBUG
	    do_debug = true;
	  }
	 else if (args[i].startsWith("-P") && i+1 < args.length) {      // -P <process>
	    process_id = args[++i];
	    name_map.put("PROCESS",process_id);
	  }
	 else if (args[i].startsWith("-J") && i+1 < args.length) {      // -J <java main>
	    start_class = args[++i];
	    name_map.put("MAIN",start_class);
	  }
	 else if (args[i].startsWith("-i") && i+1 < args.length) {      // -id <db id>
	    name_map.put("DBID",args[++i]);
	  }
	 else badArgs();
       }
      else {
	 String pfid = args[i];
	 DymacProflet dp = all_proflets.get(pfid);
	 if (dp == null) {
	    System.err.println("DYMAC: Proflet " + pfid + " not known");
	    System.exit(1);
	  }
	 active_proflets.add(dp);
       }
    }

   if (process_id == null && start_class == null) badArgs();

   if (projnm != null) {
      IvyProjectManager pm = IvyProjectManager.getManager(projdir);
      user_project = pm.findProject(projnm);
    }
}



private void badArgs()
{
   System.err.println("DYMAC: dymac -J <binary> -P <process> [-p <project>] -i <databaseid> [-d <projectdir>] proflet ...");
   System.exit(1);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

boolean doDebug()				{ return do_debug; }

IvyProject getProject() 			{ return user_project; }

DymonRemote getDymon()				{ return dymon_iface; }



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

public boolean analyze(String pid)
{
   process_id = pid;
   data_abort = false;

   return processAll();
}


public void abort()
{
   synchronized (this) {
      data_abort = true;
      notifyAll();
    }
}



private void process()
{
   getProcessId();

   processAll();
}



private void getProcessId()
{
   DymonRemote.ProcessManager pm = dymon_iface.getProcessManager();

   if (start_class != null && process_id == null) {
      for ( ; ; ) {
	 List<String> allproc = pm.findProcess(start_class);
	 if (allproc.size() == 1) {
	    process_id = allproc.get(0);
	    break;
	  }
	 else if (allproc.size() > 1) {
	    System.err.println("RERUN with one of the following processes: ");
	    for (String p : allproc) {
	       System.err.println("\t" + p);
	     }
	    System.exit(0);
	  }
	 else {
	    System.err.println("DYMAC: Waiting for user to start " + start_class);
	  }
	 try {
	    Thread.sleep(1000l);
	  }
	 catch (InterruptedException e) { }
       }
    }

   if (process_id != null) {
      if (!pm.checkFor(process_id)) {
	 System.err.println("DYMAC: Process " + process_id + " not valid");
       }
    }
}



private boolean processAll()
{
   DymonRemote.ProcessManager pm = dymon_iface.getProcessManager();

   if (process_id != null) {
      if (!pm.checkFor(process_id)) {
	 System.err.println("DYMAC: Process " + process_id + " not valid");
       }
    }

   if (do_debug) System.err.println("DYMAC: WORKING WITH PROCESS " + process_id);

   String snm = pm.getStartClass(process_id);

   String pnms = null;
   for (DymacProflet dp : active_proflets) {
      String nm = dp.getAgentName();
      if (pnms == null) pnms = nm;
      else pnms += "," + nm;
    }

   dymon_iface.dymonCommand("AGENTS " + process_id + " " + pnms);
   dymon_iface.dymonCommand("ATTACH " + process_id + " true");
   dymon_iface.dymonCommand("ENABLE " + process_id + " true");
   dymon_iface.dymonCommand("OVERHEAD " + process_id + " " + DYVISE_ANALYSIS_OVERHEAD);

   stable_count = 0;
   data_ready = false;
   dymon_iface.scheduleEvery(new Analyzer(),ANALYSIS_INTERVAL);

   synchronized (this) {
      while (!data_ready && !data_abort) {
	 try {
	    wait();
	  }
	 catch (InterruptedException e) { }
       }
    }

   if (data_abort) return false;

   for (DymacProflet dp : active_proflets) {
      if (!dp.verifyResults(snm)) return false;
    }

   String dbnm = DyviseDatabase.getDatabaseName(name_map);
   sql_database = new DyviseDatabase();
   sql_database.connect(dbnm);

   for (DymacProflet dp : active_proflets) {
      sql_database.updateTime(dp.getName(),snm);
    }

   for (DymacProflet dp : active_proflets) {
      dp.saveData(sql_database,snm);
    }

   dymon_iface.dymonCommand("ENABLE " + process_id + " false");
   dymon_iface.dymonCommand("ATTACH " + process_id + " false");

   return true;
}




/********************************************************************************/
/*										*/
/*	Callback to do the analysis						*/
/*										*/
/********************************************************************************/

private void doAnalysis()
{
   String rslt = dymon_iface.dymonCommand("ANALYSIS " + process_id);
   if (do_debug) System.err.println("GOT RESULT: " + rslt);
   if (rslt == null) {
      if (do_debug) System.err.println("DYMAC: Process terminated without stable results");
      abort();
      return;
    }

   Element xml = IvyXml.convertStringToXml(rslt);

   boolean chng = false;
   for (DymacProflet dp : active_proflets) {
      chng |= dp.processData(xml);
    }

   if (chng) stable_count = 0;
   else if (stable_count++ == STABLE_COUNT) {
      synchronized (this) {
	 data_ready = true;
	 notifyAll();
       }
    }
}



private class Analyzer extends TimerTask {

   public void run() {
      doAnalysis();
      if (data_ready || data_abort) cancel();
    }

}	// end of innerclass Analyzer


}	// end of class DymacMain



/* end of DymacMain.java */
