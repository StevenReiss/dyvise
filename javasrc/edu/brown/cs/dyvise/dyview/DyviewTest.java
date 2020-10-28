/********************************************************************************/
/*										*/
/*		DyviewTest.java 						*/
/*										*/
/*	DYVIEW test program							*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyview/DyviewTest.java,v 1.5 2013-05-09 12:29:05 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyviewTest.java,v $
 * Revision 1.5  2013-05-09 12:29:05  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.4  2012-10-05 00:53:05  spr
 * Code clean up.
 *
 * Revision 1.3  2009-10-07 22:39:51  spr
 * Eclipse code cleanup.
 *
 * Revision 1.2  2009-10-07 01:00:21  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.1  2009-09-19 00:14:23  spr
 * Viewer with dialog based front end.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dyview;


import edu.brown.cs.dyvise.dygraph.DygraphControl;
import edu.brown.cs.dyvise.dygraph.DygraphView;
import edu.brown.cs.dyvise.dymon.DymonRemote;
import edu.brown.cs.dyvise.dynamo.DynamoPatchSetup;
import edu.brown.cs.dyvise.dystore.*;
import edu.brown.cs.dyvise.dyvise.DyviseConstants;
import edu.brown.cs.ivy.mint.*;
import edu.brown.cs.ivy.project.IvyProject;
import edu.brown.cs.ivy.project.IvyProjectManager;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import javax.swing.JFrame;
import javax.swing.JPanel;

import java.io.File;
import java.io.IOException;
import java.util.*;


public class DyviewTest implements DyviewConstants, DyviseConstants, MintConstants
{



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   DyviewTest dt = new DyviewTest(args);

   dt.runTest();
}





/********************************************************************************/
/*										*/
/*	Local Storage								*/
/*										*/
/********************************************************************************/

private WhichTest which_test;

private IvyProject user_project;
private String	process_id;
private String	patch_data;
private Map<String,String> name_map;
private String	start_class;
private MintDefaultReply model_reply;
private String model_name;
private MintControl mint_control;
private DymonRemote dymon_iface;

private DystoreControl tuple_store;
private DyviewTupleBuilder tuple_builder;

private DymonRemote.Patcher patch_process;


enum WhichTest {
   CREATE,
   VIEW
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private DyviewTest(String [] args)
{
   which_test = WhichTest.CREATE;

   user_project = null;
   process_id = null;
   model_name = null;
   model_reply = null;
   name_map = new HashMap<String,String>();
   start_class = null;
   patch_process = null;
   tuple_builder = null;

   scanArgs(args);
}




/********************************************************************************/
/*										*/
/*	Argument processing methods						*/
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
	 else if (args[i].startsWith("-v")) {                           // -view
	    which_test = WhichTest.VIEW;
	  }
	 else badArgs();
       }
      else if (patch_data == null) {
	 patch_data = args[i];
       }
      else badArgs();
    }

   if (projnm != null) {
      IvyProjectManager pm = IvyProjectManager.getManager(projdir);
      user_project = pm.findProject(projnm);
    }
}



private void badArgs()
{
   System.err.println("DYVIEWTEST: dyviewtest -J <binary> -P <process> [-p <project>] -i <databaseid> [-d <projectdir>] description_file");
   System.exit(1);
}



/********************************************************************************/
/*										*/
/*	Testing methods 							*/
/*										*/
/********************************************************************************/

private void runTest()
{
   switch (which_test) {
      case CREATE :
	 createTest();
	 break;
      case VIEW :
	 viewTest();
	 break;
    }
}



private void createTest()
{
   setupProcess();

   instrument(true);

   try {
      Thread.sleep(300000l);
    }
   catch (InterruptedException e) { }

   instrument(false);

   try {
      Thread.sleep(10000l);
    }
   catch (InterruptedException e) { }

   save("/pro/dyvise/dyview/src/test.out");

   System.exit(0);
}



@SuppressWarnings("unused")
private void viewTest()
{
   mint_control = DymonRemote.getMintControl();

   DynamoPatchSetup patcher = new DynamoPatchSetup(user_project,patch_data);
   String pm = patcher.setupPatchModel();
   Element xml = IvyXml.convertStringToXml(pm);

   Element em = IvyXml.getChild(xml,"EVENTMODEL");
   Element tm = IvyXml.getChild(xml,"TUPLEMODEL");
   Element gm = IvyXml.getChild(xml,"GRAPHMODEL");

   tuple_store = new DystoreControl(tm);
   try {
      tuple_store.load(new File("/pro/dyvise/dyview/src/test.out"));
      System.err.println("DYVIEW: Data file loaded");
    }
   catch (IOException e) {
      System.err.println("DYVIEW: Problem loading file: " + e);
      System.exit(1);
    }

   JFrame frm = new JFrame();
   JFrame fm1 = new JFrame();

   frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

   DygraphControl dg = new DygraphControl(gm,tuple_store);
   for (DygraphView dv : dg.getViews()) {
      JPanel pnl = dv.getDisplayPanel();
      frm.setContentPane(pnl);
      frm.pack();
      frm.setVisible(true);

      JPanel cpnl = dv.getControlPanel();
      if (cpnl != null) {
	 fm1.setContentPane(cpnl);
	 fm1.pack();
	 fm1.setVisible(true);
       }

      break;
    }


}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

@SuppressWarnings("unused")
private void setupProcess()
{
   dymon_iface = new DymonRemote();
   mint_control = DymonRemote.getMintControl();

   DynamoPatchSetup patcher = new DynamoPatchSetup(user_project,patch_data);
   String pm = patcher.setupPatchModel();
   Element xml = IvyXml.convertStringToXml(pm);

   Element em = IvyXml.getChild(xml,"EVENTMODEL");
   Element tm = IvyXml.getChild(xml,"TUPLEMODEL");
   Element gm = IvyXml.getChild(xml,"GRAPHMODEL");
   tuple_store = new DystoreControl(tm);
   tuple_builder = new DyviewTupleBuilder(em,tuple_store);
   DystoreTable dt = tuple_store.getTable("EVENT");
   DystoreStore ds = tuple_store.getStore(dt);
   DystoreField df = dt.getField("THREAD");
   DystoreAccessor da = ds.getAccessor(df);

   DymonRemote.ProcessManager pmgr = dymon_iface.getProcessManager();
   if (start_class != null && process_id == null) {
      for ( ; ; ) {
	 List<String> allproc = pmgr.findProcess(start_class);
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
	    System.err.println("DYVIEW: Waiting for user to start " + start_class);
	  }
	 try {
	    Thread.sleep(1000l);
	  }
	 catch (InterruptedException e) { }
       }
    }

   // need to disable all monitors first
   dymon_iface.dymonCommand("AGENTS " + process_id + " " + DYVIEW_PATCH_AGENT);
   dymon_iface.dymonCommand("ENABLE " + process_id + " FALSE");
   dymon_iface.dymonCommand("ATTACH " + process_id + " TRUE");
   dymon_iface.dymonCommand("CLEAR " + process_id + " " + DYVIEW_PATCH_AGENT);

   for ( ; ; ) {
      String sts = dymon_iface.dymonCommand("CHECK " + process_id);
      if (sts == null) {
	 System.err.println("DYVIEW: Process " + process_id + " has terminated");
	 System.exit(1);
       }
      else if (sts.equals("OK")) break;
      else System.err.println("DYVIEW: Status " + sts + " for " + process_id);
      try {
	 Thread.sleep(1000);
       }
      catch (InterruptedException e) { }
    }

   System.err.println("DYVIEWTEST: WORKING WITH PROCESS " + process_id);

   patch_process = DymonRemote.getPatcher(process_id);

   Element mdl = IvyXml.getChild(xml,"MODEL");
   model_name = IvyXml.getAttrString(mdl,"NAME");

   model_reply = new MintDefaultReply();
   patch_process.send("MODEL",IvyXml.convertXmlToString(mdl),model_reply,MINT_MSG_ALL_REPLIES);

   mint_control.register("<DYPER REPORT='" + process_id + "' TIME='_VAR_0'><_VAR_1/></DYPER>",
			    new ReportHandler());
}




private void instrument(boolean fg)
{
   model_reply.waitFor();

   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("ACTIVATE");
   xw.begin("MODEL");
   xw.field("NAME",model_name);
   xw.field("ACTION",(fg ? "ADD" : "REMOVE"));
   xw.end("MODEL");
   xw.end("ACTIVATE");

   PatchReplyHandler prh = new PatchReplyHandler(fg);
   patch_process.send("ACTIVATE",xw.toString(),prh,MINT_MSG_ALL_REPLIES);
   xw.close();
}



private void save(String file)
{
   try {
      tuple_store.save(new File(file));
    }
   catch (IOException e) {
      System.err.println("DYVIEW: Problem saving tuples: " + e);
      e.printStackTrace();
    }
}




/********************************************************************************/
/*										*/
/*	Patcher methods 							*/
/*										*/
/********************************************************************************/

private void setupPatch(Element pdata,boolean add)
{
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("DYPER");
   xw.field("PID",process_id);
   xw.field("COMMAND","INSTRUMENT");

   xw.begin("MODEL");
   xw.field("NAME",model_name);
   xw.field("INSERT",add);

   xw.begin("VAR");
   xw.field("NAME","REPORTING");
   xw.field("VALUE",add);
   xw.end("VAR");

   for (Element e : IvyXml.elementsByTag(pdata,"CLASS")) {
      String c = IvyXml.getAttrString(e,"NAME");
      String p = IvyXml.getAttrString(e,"FILE");
      boolean orig = IvyXml.getAttrBool(e,"ORIGINAL");
      xw.begin("PATCH");
      xw.field("CLASS",c);
      xw.field("PATCH",p);
      xw.field("CHANGE",!orig);
      xw.end("PATCH");
    }
   xw.end("MODEL");
   xw.end("DYPER");

   System.err.println("DYVIEW: SENDING PATCH MESSAGE: " + xw.toString());

   mint_control.send(xw.toString());
   xw.close();
}



private class PatchReplyHandler implements MintReply {

   private Element reply_data;
   private boolean is_insert;

   PatchReplyHandler(boolean add) {
      reply_data = null;
      is_insert = add;
    }

   public void handleReply(MintMessage msg,MintMessage rply) {
      reply_data = rply.getXml();
    }

   public void handleReplyDone(MintMessage msg) {
      setupPatch(reply_data,is_insert);
    }

}	// end of innerclass PatchReplyHandler



/********************************************************************************/
/*										*/
/*	Handler of reports from the running process				*/
/*										*/
/********************************************************************************/

private void handleReport(double when,Element xml)
{
   Element e = IvyXml.getChild(xml,"EVENTS");
   if (e == null) return;

   tuple_builder.processEvents(e);
}




private class ReportHandler implements MintHandler {

   private double last_time;

   ReportHandler() {
      last_time = 0;
    }

   public void receive(MintMessage msg,MintArguments args) {
      msg.replyTo();
   
      synchronized (this) {
         double time = args.getRealArgument(0);
         Element e = args.getXmlArgument(1);
         if (time < last_time) {
            System.err.println("DYMON: REPORT OUT OF ORDER " + time + " " + last_time);
            // handler report out of order
          }
         else {
            last_time = time;
            handleReport(time,e);
          }
       }
    }

}	// end of inner class ReportHandler





}	// end of class DyviewTest




/* end of DyviewTest.java */

