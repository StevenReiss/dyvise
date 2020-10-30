/********************************************************************************/
/*										*/
/*		DyviewRunner.java						*/
/*										*/
/*	DYname VIEW class to handle attaching to a running system		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyview/DyviewRunner.java,v 1.9 2013/09/04 18:36:36 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyviewRunner.java,v $
 * Revision 1.9  2013/09/04 18:36:36  spr
 * Minor bug fixes.
 *
 * Revision 1.8  2013-05-09 12:29:05  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.7  2012-10-05 00:53:04  spr
 * Code clean up.
 *
 * Revision 1.6  2011-03-10 02:33:25  spr
 * Code cleanup.
 *
 * Revision 1.5  2010-06-01 19:26:24  spr
 * Upgrades to make dyview work on the mac
 *
 * Revision 1.4  2010-03-30 16:23:25  spr
 * Upgrades to the visualizer to handle new programs and provide better views.
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


import edu.brown.cs.dyvise.dymon.DymonRemote;
import edu.brown.cs.dyvise.dyvise.DyviseConstants;
import edu.brown.cs.ivy.mint.*;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;


class DyviewRunner implements DyviewConstants, DyviseConstants, MintConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private DyviewModel view_model;
private String	process_id;
private String model_name;
private MintDefaultReply model_reply;
private DymonRemote.Patcher patch_process;
private boolean is_instrumented;
private RunnerShutdown shutdown_thread;
private ReportHandler report_handler;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DyviewRunner(DyviewModel vm,String pid)
{
   view_model = vm;
   process_id = pid;
   is_instrumented = false;
   shutdown_thread = new RunnerShutdown();
   report_handler = null;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getProcessId()			{ return process_id; }




/********************************************************************************/
/*										*/
/*	Methods to get things ready						*/
/*										*/
/********************************************************************************/

void setupProcess()
{
   DymonRemote dymon = view_model.getDymonHandle();
   dymon.dymonCommand("AGENTS " + process_id + " " + DYVIEW_PATCH_AGENT);
   dymon.dymonCommand("ATTACH " + process_id + " TRUE");
   dymon.dymonCommand("REPORTING " + process_id + " TRUE");
   dymon.dymonCommand("ENABLE " + process_id + " TRUE");
   dymon.dymonCommand("CLEAR " + process_id + " " + DYVIEW_PATCH_AGENT);
   dymon.dymonCommand("OVERHEAD " + process_id + " " + DYVISE_RUNNING_OVERHEAD);

   for ( ; ; ) {
      String s = dymon.dymonCommand("CHECK " + process_id);
      System.err.println("DYVIEW: Process Check: " + s);
      if (!s.equals("PENDING") && !s.equals("INACTIVE")) break;
    }

   patch_process = DymonRemote.getPatcher(process_id);
   Element pm = view_model.getPatchModel();
   model_name = IvyXml.getAttrString(pm,"NAME");
   model_reply = new MintDefaultReply();
   System.err.println("DYVIEW: setup patch model: " + IvyXml.convertXmlToString(pm));

   patch_process.send("MODEL",IvyXml.convertXmlToString(pm),model_reply,MINT_MSG_ALL_REPLIES);

   MintControl mc = view_model.getMintControl();
   report_handler = new ReportHandler();
   mc.register("<DYPER REPORT='" + process_id + "' TIME='_VAR_0'><_VAR_1/></DYPER>",report_handler);
}



void removeProcess()
{
   enableMonitor(false);

   DymonRemote dymon = view_model.getDymonHandle();
   dymon.dymonCommand("ENABLE " + process_id + " FALSE");
   dymon.dymonCommand("REPORTING " + process_id + " FALSE");
   dymon.dymonCommand("ATTACH " + process_id + " FALSE");
   dymon.dymonCommand("AGENTS " + process_id + " NONE");

   if (report_handler != null) {
      view_model.getMintControl().unregister(report_handler);
      report_handler = null;
    }
}


void waitUntilSetup()
{
   model_reply.waitFor();

   instrument(true);
}



void enableMonitor(boolean fg)
{
   instrument(fg);
}



boolean checkActive()
{
   DymonRemote dymon = view_model.getDymonHandle();

   String sts = dymon.dymonCommand("CHECK " + process_id);

   // System.err.println("DYVIEW: Status check for " + process_id + " => " + sts);

   if (sts == null) return false;
   else if (sts.equals("OK") || sts.equals("PENDING")) return true;

   return false;
}



/********************************************************************************/
/*										*/
/*	Instrumentation methods 						*/
/*										*/
/********************************************************************************/

private void instrument(boolean fg)
{
   is_instrumented = fg;

   System.err.println("INSTRUMENT " + process_id + " " + fg);

   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("ACTIVATE");
   xw.begin("PATCHMODEL");
   xw.field("NAME",model_name);
   xw.field("ACTION",(fg ? "ADD" : "REMOVE"));
   xw.end("PATCHMODEL");
   xw.end("ACTIVATE");

   MintDefaultReply mh = new MintDefaultReply();
   patch_process.send("ACTIVATE",xw.toString(),mh,MINT_MSG_ALL_REPLIES);
   xw.close();

   Element pdata = mh.waitForXml();
   setupPatch(pdata,fg);

   Runtime rt = Runtime.getRuntime();
   if (shutdown_thread != null) {
      if (fg) rt.addShutdownHook(shutdown_thread);
      else rt.removeShutdownHook(shutdown_thread);
    }
}




/********************************************************************************/
/*										*/
/*	Patching methods							*/
/*										*/
/********************************************************************************/

private void setupPatch(Element pdata,boolean add)
{

   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("DYPER");
   xw.field("PID",process_id);
   xw.field("COMMAND","INSTRUMENT");

   xw.begin("PATCHMODEL");
   xw.field("NAME",model_name);
   xw.field("INSERT",add);

   xw.begin("VAR");
   xw.field("NAME","REPORTING");
   xw.field("VALUE",add);
   xw.end("VAR");
   xw.begin("VAR");
   xw.field("NAME","DISABLETIME");
   xw.field("VALUE",2000);
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
   xw.end("PATCHMODEL");
   xw.end("DYPER");

   MintControl mc = view_model.getMintControl();
   mc.send(xw.toString());
   xw.close();
}




/********************************************************************************/
/*										*/
/*	Methods to get process data						*/
/*										*/
/********************************************************************************/

private void handleReport(double when,Element xml)
{
   System.currentTimeMillis();

   Element e = IvyXml.getChild(xml,"EVENTS");
   if (e == null) return;

   // System.err.println("PROCESS EVENTS " + Thread.currentThread());
   view_model.processEvents(e);

   // System.err.println("PROCESS DONE " + Thread.currentThread());
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
	    System.err.println("DYVIEW: REPORT OUT OF ORDER " + time + " " + last_time);
	    handleReport(time,e);
	  }
	 else {
	    // System.err.println("DYVIEW: REPORT received at " + time);
	    // System.err.println("DYVIEW: RECEIVED AT " + time + ": " + IvyXml.convertXmlToString(e));
	    last_time = time;
	    handleReport(time,e);
	  }
       }
    }

}	// end of inner class ReportHandler



/********************************************************************************/
/*										*/
/*	Thread to ensure process is left without instrumentation		*/
/*										*/
/********************************************************************************/

private class RunnerShutdown extends Thread {

   RunnerShutdown() {
      super("Runner Shutdown " + process_id);
    }

   public void run() {
      shutdown_thread = null;
      if (is_instrumented) instrument(false);
    }

}	// end of inner class RunnerShutdown




}	// end of class DyviewRunner




/* end of DyviewRunner.java */
