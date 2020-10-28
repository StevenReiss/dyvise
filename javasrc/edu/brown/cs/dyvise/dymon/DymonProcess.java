/********************************************************************************/
/*										*/
/*		DymonProcess.java						*/
/*										*/
/*	DYPER monitor representation of a process				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymon/DymonProcess.java,v 1.14 2016/11/02 18:59:13 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymonProcess.java,v $
 * Revision 1.14  2016/11/02 18:59:13  spr
 * Move to asm5
 *
 * Revision 1.13  2013-05-09 12:29:02  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.12  2011-03-10 02:26:33  spr
 * Code cleanup.
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
 * Revision 1.7  2009-05-01 23:15:12  spr
 * Fix up state computation.  Clean up code.
 *
 * Revision 1.6  2009-04-28 18:01:15  spr
 * Update state information to produce state output.
 *
 * Revision 1.5  2009-04-12 02:11:34  spr
 * Change .so to .jnilib for the mac.
 *
 * Revision 1.4  2009-04-11 23:46:18  spr
 * Use IvyFormat rather than local code.
 *
 * Revision 1.3  2009-03-20 02:06:51  spr
 * Update dymon: add time-based information, updated memory, etc.
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


import edu.brown.cs.ivy.exec.IvyExecQuery;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.mint.*;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.concurrent.*;


public class DymonProcess implements DymonConstants, MintConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private DymonMain dymon_main;

private String process_id;
private boolean is_attached;
private String start_class;
private String start_jar;
private List<String> java_args;
private List<String> program_args;
private Map<String,String> program_props;
private boolean is_active;
private boolean is_detached;
private DymonRemote.Patcher patch_process;
private double allowed_overhead;
private Set<String> active_vjmti;
private long next_check;
private Element last_report;
private boolean ibm_jvm;
private DymonAgentManager agent_manager;

private long	total_samples;
private long	active_samples;
private long	report_lasttime;

private double report_time = 0.1;
private double check_time = 0.9;
private double detail_time = 0.0;

private double continuous_fraction = 0.1;
private double continuous_significance = 4.0;

private Map<DymonDetailing,Boolean> continuous_detailings;

private boolean monitor_enabled;
private boolean reporting_enabled;
private boolean try_save;
private Writer	save_reports;

private static final int	MAX_ARG_LENGTH = 24;



enum PatchState {
   NOT_RUNNING,
   STARTED,
   READY
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DymonProcess(DymonMain dm,String id,String startargs,boolean tryattach)
{
   dymon_main = dm;
   process_id = id;
   start_class = null;
   start_jar = null;
   java_args = new ArrayList<String>();
   program_args = new ArrayList<String>();
   program_props = new HashMap<String,String>();
   is_active = false;
   is_attached = false;
   active_vjmti = new HashSet<String>();
   monitor_enabled = false;
   reporting_enabled = false;
   last_report = null;
   is_detached = false;
   continuous_detailings = null;
   next_check = System.currentTimeMillis() + DYMON_REPORT_CHECK_FIRST;
   try_save = true;
   save_reports = null;
   ibm_jvm = false;
   agent_manager = new DymonAgentManager(dm,this);

   patch_process = DymonRemote.getPatcher(process_id);

   if (startargs != null) {
      dymon_main.checkResources();
      StringTokenizer tok = new StringTokenizer(startargs);
      if (tok.hasMoreTokens()) start_class = tok.nextToken();
      fixStartClass();
      while (tok.hasMoreTokens()) program_args.add(tok.nextToken());
      if (start_class != null) {
	 monitor_enabled = dymon_main.getBooleanResource(start_class,"MONITOR");
	 reporting_enabled = dymon_main.getBooleanResource(start_class,"REPORTING");
       }
    }

   if (tryattach) attach();
}




/********************************************************************************/
/*										*/
/*	Code to setup communication						*/
/*										*/
/********************************************************************************/

synchronized boolean attach()
{
   String ostart = start_class;

   if (is_detached) {
      is_detached = false;
      if (!checkAlive()) return false;
      reattach();
      return true;
    }

   if (is_attached) return true;

   if (try_save) {
      try_save = false;
      try {
	 File sf = new File(DYMON_TRACE_PREFIX + process_id + ".trace");
	 save_reports = new FileWriter(sf);
	 if (dymon_main.doDebug()) {
	    System.err.println("DYMON: Saving output in " + sf);
	  }
	 else {
	    sf.deleteOnExit();
	  }
       }
      catch (IOException e) { }
    }

   is_attached = true;

   Element e = null;
   long t0 = System.currentTimeMillis() + 15000;

   for ( ; ; ) {
      long t1 = System.currentTimeMillis();
      if (t1 >= t0) break;
      MintDefaultReply wh = new MintDefaultReply();
      sendDyperMessage("WHORU",null,wh,MINT_MSG_FIRST_NON_NULL);
      e = wh.waitForXml(t0-t1);
      if (e != null) break;
    }

   if (e != null) {
      is_active = true;
      getProcessProps(e);
      if (start_class == null) {
	 System.err.println("DYMON: Process " + process_id + " WHORU missing start: " +
			       IvyXml.convertXmlToString(e));
	 start_class = "UNKNOWN";
       }
      else {
	 System.err.println("DYMON: Detail for " + process_id + ": " + IvyXml.convertXmlToString(e));
       }
    }
   else {
      System.err.println("DYMON: No reply for WHORU from " + process_id);
    }

   if (start_class == null) {
      is_attached = false;
      return false;
    }

   String vend = program_props.get("java.vm.vendor");
   if (vend != null && vend.contains("IBM")) ibm_jvm = true;

   dymon_main.checkResources();

   is_attached = true;
   if (ostart == null) {
      monitor_enabled = dymon_main.getBooleanResource(start_class,"MONITOR");
      reporting_enabled = dymon_main.getBooleanResource(start_class,"REPORTING");
    }

   System.err.println("DYMON: PROCESS " + process_id + " ATTACHED");

   agent_manager.initialInstall();
   continuous_detailings = new ConcurrentHashMap<DymonDetailing,Boolean>();

   String d = dymon_main.getSettings(start_class);
   sendDyperMessage("SET",d,null,0);
   d = dymon_main.getClassSettings(start_class);
   if (d != null) sendDyperMessage("CLASSES",d,null,0);
   allowed_overhead = dymon_main.getDoubleResource(start_class,"OVERHEAD");
   if (allowed_overhead == 0) allowed_overhead = DYMON_DEFAULT_OVERHEAD;

   setDyperVar("MONITOR",Boolean.toString(monitor_enabled));
   setDyperVar("REPORTING",Boolean.toString(reporting_enabled));

   dymon_main.register("<DYPER REPORT='" + process_id + "' TIME='_VAR_0'><_VAR_1/></DYPER>",
			  new ReportHandler());
   dymon_main.register("<DYPER DUMP='" + process_id + "' TIME='_VAR_0'><_VAR_1/></DYPER>",
			  new DumpHandler());

   return true;
}



void detach()
{
   if (!is_attached) return;
   if (is_detached) return;

   dymon_main.disablePatcher(this);
   setDyperVar("MONITOR",Boolean.toString(false));

   for (DymonAgent da : agent_manager.getActiveAgents()) {
      da.detach();
    }

   is_detached = true;
}



private void reattach()
{
   if (!is_attached || !is_detached) return;

   for (DymonAgent da : agent_manager.getActiveAgents()) {
      da.reattach();
    }

   is_detached = false;

   if (monitor_enabled) setDyperVar("MONITOR",Boolean.toString(true));
   if (reporting_enabled) setDyperVar("REPORTING",Boolean.toString(true));
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getProcessId()				{ return process_id; }
String getHost()
{
   int idx = process_id.indexOf('@');
   if (idx < 0) return null;
   return process_id.substring(idx+1);
}


boolean isActive()				{ return is_active; }

boolean isAttached()				{ return is_attached && !is_detached; }

void addClassData(Element e)			{ dymon_main.addClassData(e); }

void addMethodData(Element e)			{ dymon_main.addMethodData(e); }

void handleItems(Collection<Integer> ids,long when,boolean start,String forwhom)
{
   dymon_main.handleItems(ids,when,total_samples,active_samples,start,forwhom);
}

CounterData getCounterData(int id)		{ return dymon_main.getCounterData(id); }


boolean isMonitoringEnabled()			{ return monitor_enabled; }
boolean isReportingEnabled()			{ return reporting_enabled; }

double getAllowedOverhead()			{ return allowed_overhead; }

void setAllowedOverhead(double v)		{ allowed_overhead = v; }

String getStartClass()				{ return start_class; }

String getRemoteHost()
{
   int idx = process_id.indexOf("@");
   if (idx < 0) return null;
   String host = process_id.substring(idx+1);
   if (host.equals(IvyExecQuery.getHostName())) return null;
   return host;
}


long getReportTime()				{ return report_lasttime; }
long getTotalSamples()				{ return total_samples; }
long getActiveSamples() 			{ return active_samples; }

boolean isIBM() 				{ return ibm_jvm; }


void setAgentSet(String agts)
{
   agent_manager.setAgentSet(agts);
}


String getAgentSet()
{
   return agent_manager.getAgentSet();
}


Collection<String> listAllAgents()
{
   return agent_manager.listAllAgents();
}


/********************************************************************************/
/*										*/
/*	Monitoring methods							*/
/*										*/
/********************************************************************************/

void enableMonitoring(boolean fg)
{
   System.err.println("DYMON: ENABLE " + process_id + " " + fg);

   if (fg == monitor_enabled) return;
   monitor_enabled = fg;

   if (is_attached) {
      if (!fg) {
	 dymon_main.disablePatcher(this);
       }

      setDyperVar("MONITOR",Boolean.toString(fg));
    }
}




void enableReporting(boolean fg)
{
   reporting_enabled = fg;

   if (is_attached) {
      setDyperVar("REPORTING",Boolean.toString(fg));
    }
}




/********************************************************************************/
/*										*/
/*	Analysis methods							*/
/*										*/
/********************************************************************************/

void doAnalysis(String what,IvyXmlWriter xw)
{
   long now = System.currentTimeMillis();

   xw.begin("ANALYSIS");
   xw.field("PROCESS",process_id);
   xw.field("START",start_class);
   xw.field("OVERHEAD",allowed_overhead);
   xw.field("ENABLED",monitor_enabled);
   xw.field("ACTIVE",is_active);
   xw.field("NOW",now);

   xw.begin("SUMMARIES");
   for (DymonAgent agt : agent_manager.getActiveAgents()) {
      if (what == null || what.equals("*") || what.equals(agt.getName())) {
	 double v = agt.getSummaryValue();
	 if (v >= 0) {
	    double cv = agt.getConfidence();
	    if (cv == 0) v = 0;
	    xw.begin("SUMMARY");
	    xw.field("NAME",agt.getName());
	    xw.field("VALUE",v);
	    xw.field("CONFIDENCE",cv);
	    try {
	       agt.outputSummary(xw);
	     }
	    catch (Throwable t) {
	       System.err.println("DYMON: Problem doing summary: " + t);
	       t.printStackTrace();
	     }
	    xw.end("SUMMARY");
	  }
       }
    }
   xw.end("SUMMARIES");

   xw.begin("IMMEDIATES");
   for (DymonAgent agt : agent_manager.getActiveAgents()) {
      if (what == null || what.equals("*") || what.equals(agt.getName())) {
	 try {
	    agt.outputImmediate(xw);
	  }
	 catch (Throwable t) {
	    System.err.println("DYMON: Problem doing immediate: " + t);
	    t.printStackTrace();
	  }
       }
    }
   xw.end("IMMEDIATES");

   for (DymonAgent agt : agent_manager.getActiveAgents()) {
      if (what == null || what.equals("*") || what.equals(agt.getName())) {
	 try {
	    agt.outputAnalysis(xw);
	  }
	 catch (Throwable t) {
	    System.err.println("DYMON: Problem doing analysis: " + t);
	    t.printStackTrace();
	  }
       }
    }

   xw.end("ANALYSIS");
}




void doClear(String what)
{
   if (is_attached) {
      dymon_main.disablePatcher(this);
      String arg = null;
      if (what != null && !what.equals("*")) arg = "<CLEAR AGENT='" + what + "' />";
      MintDefaultReply ph = new MintDefaultReply();
      sendDyperMessage("CLEAR",arg,ph,MINT_MSG_FIRST_NON_NULL);
      ph.waitForString();
      dymon_main.enablePatcher(this);
    }

   for (DymonAgent agt : agent_manager.getActiveAgents()) {
      if (what == null || what.equals("*") || what.equals(agt.getName())) {
	 agt.doClear();
       }
    }
}



/********************************************************************************/
/*										*/
/*	Message handling methods						*/
/*										*/
/********************************************************************************/

boolean checkAlive()
{
   if (!is_active) return false;
   // if (is_detached) return false;

   long now = System.currentTimeMillis();
   MintDefaultReply ph = new MintDefaultReply();
   sendDyperMessage("PING",null,ph,MINT_MSG_FIRST_NON_NULL);
   if (ph.waitForString(1000) == null && ph.hadReply()) {
      long delay = System.currentTimeMillis() - now;
      System.err.println("DYMON: PING FAILED FOR " + process_id + " " + delay);
      noteDead();
      dymon_main.removeProcess(this);
      if (save_reports != null) {
	 try {
	    save_reports.close();
	  }
	 catch (IOException e) { }
	    save_reports = null;
       }
    }
   else if (ph.hadReply()) {
      // System.err.println("DYMON: PING OK FOR " + process_id);
    }
   else {
      System.err.println("DYMON: PING TIMEOUT FOR " + process_id);
    }

   return is_active;
}



void sendDyperMessage(String cmd,String body,MintReply hdlr,int flags)
{
   sendMessage("DYPER",cmd,body,hdlr,flags);
}



void sendMarkMessage(String mark,long when)
{
   String body = "<MARK TYPE='" + mark + "' WHEN='" + when + "' />";
   sendMessage("DYMON","MARK",body,null,0);
}



void sendMessage(String who,String cmd,String body,MintReply hdlr,int flags)
{
   String s = "<" + who + " PID='" + process_id + "'";

   if (cmd != null) s += " COMMAND='" + cmd + "'";
   s += ">";
   if (body != null) s += body;
   s += "</" + who +">";

   if (hdlr == null) dymon_main.send(s);
   else dymon_main.send(s,hdlr,flags);
}



void setDyperVar(String var,String val)
{
   String b = "<VAR NAME='" + var + "' VALUE='" + val + "' />";
   sendDyperMessage("SET",b,null,0);
}



void setDyperDetail(String agt,String var,boolean val)
{
   String b = "<DETAIL AGENT='" + agt + "' ITEM='" + var + "' VALUE='" + val + "' />";
   sendDyperMessage("SETDETAIL",b,null,0);
}



void setDyperAgentStatus(String agt,boolean activate)
{
   if (agt == null) return;

   String what = (activate ? "ACTIVATE" : "DEACTIVATE");
   String b = "<" + what + " AGENT='" + agt + "' />";
   sendDyperMessage("ACTIVATE",b,null,0);
}



private void noteDead()
{
   is_active = false;

   for (DymonAgent da : agent_manager.getActiveAgents()) {
      try {
	 da.noteDead();
       }
      catch (Throwable t) {
	 System.err.println("DYMON: Problem setting agent inactive for " + da.getName() + ":" + t);
	 t.printStackTrace();
       }
    }

   dymon_main.disablePatcher(this);
}




/********************************************************************************/
/*										*/
/*	Patch handling methods							*/
/*										*/
/********************************************************************************/

void sendPatchMessage(String cmd,String body,MintReply hdlr,int flags)
{
   patch_process.send(cmd,body,hdlr,flags);
}



/********************************************************************************/
/*										*/
/*	Vjmti handling methods							*/
/*										*/
/********************************************************************************/

void loadVjmtiAgent(String nm,String args)
{
   synchronized (active_vjmti) {
      if (active_vjmti.contains(nm)) return;
      dymon_main.attachVjmtiAgent(this,nm,args);
      active_vjmti.add(nm);
    }
}




/********************************************************************************/
/*										*/
/*	Methods to handle process properties					*/
/*										*/
/********************************************************************************/

private void getProcessProps(Element xml)
{
   for (Element e : IvyXml.elementsByTag(xml,"JAVAARG")) {
      java_args.add(IvyXml.getText(e));
    }
   start_class = IvyXml.getTextElement(xml,"START");
   fixStartClass();
   program_args.clear();
   for (Element e : IvyXml.elementsByTag(xml,"ARG")) {
      program_args.add(IvyXml.getText(e));
    }
   for (Element e : IvyXml.elementsByTag(xml,"ENV")) {
      String k = IvyXml.getAttrString(e,"KEY");
      String v = IvyXml.getText(e);
      program_props.put(k,v);
    }
   for (Element e : IvyXml.elementsByTag(xml,"PROPERTY")) {
      String k = IvyXml.getAttrString(e,"KEY");
      String v = IvyXml.getText(e);
      program_props.put(k,v);
    }

   program_props.put("SHARED_LIB_EXT",".so");
   String vend = program_props.get("java.vm.vendor");
   if (vend.contains("Apple")) {
      program_props.put("SHARED_LIB_EXT",".jnilib");
    }
}


String expandName(String nm)
{
   return IvyFile.expandName(nm,program_props);
}



/********************************************************************************/
/*										*/
/*	Methods for handling reports						*/
/*										*/
/********************************************************************************/

private void handleReport(long now,Element e)
{
   if (is_detached) return;

   if (!is_active) {
      for (DymonAgent da : agent_manager.getActiveAgents()) {
	 da.noteActive();
       }
      is_active = true;
    }

   last_report = e;

   report_lasttime = IvyXml.getAttrLong(e,"TIME");
   Element be = IvyXml.getElementByTag(e,"COUNTERS");
   total_samples = IvyXml.getAttrLong(be,"SAMPLES");
   active_samples = IvyXml.getAttrLong(be,"ACTIVE");

   for (DymonAgent da : agent_manager.getActiveAgents()) {
      try {
	 da.handleReport(e);
       }
      catch (Throwable t) {
	 System.err.println("DYMON: Problem handling report for agent " + da.getName() + ":" + t);
	 t.printStackTrace();
       }
    }

   checkInstrumentation(now,e);
}



void showLastReport(IvyXmlWriter xw)
{
   if (last_report == null) xw.textElement("REPORT",null);
   else xw.writeXml(last_report);
}



private class ReportHandler implements MintHandler {

   private long last_time;

   ReportHandler() {
      last_time = 0;
    }

   public void receive(MintMessage msg,MintArguments args) {
      msg.replyTo();

      synchronized (this) {
	 long time = args.getLongArgument(0);
	 Element e = args.getXmlArgument(1);
	 if (time < last_time) {
	    System.err.println("DYMON: REPORT OUT OF ORDER " + time + " " + last_time);
	  }
	 else {
	    last_time = time;
	    handleReport(time,e);
	  }
	 if (save_reports != null) {
	    try {
	       save_reports.write(IvyXml.convertXmlToString(e));
	       save_reports.write("\n");
	       save_reports.flush();
	     }
	    catch (IOException ex) {
	       System.err.println("DYMON: Problem saving report data: " + ex);
	       save_reports = null;
	     }
	  }
       }
    }

}	// end of subclass ReportHandler



private class DumpHandler implements MintHandler {

   public void receive(MintMessage msg,MintArguments args) {
      msg.replyTo();
      Element e = args.getXmlArgument(1);
      for (DymonAgent da : agent_manager.getActiveAgents()) {
	 da.handleDump(e);
       }
    }

}	// end of subclass DumpHandler



/********************************************************************************/
/*										*/
/*	Instrumentation methods 						*/
/*										*/
/********************************************************************************/

private void checkInstrumentation(long now,Element e)
{
   if (!isAttached()) return;

   double chktime = IvyXml.getAttrDouble(e,"CHECKTIME",0);
   double rpttime = IvyXml.getAttrDouble(e,"REPORTTIME",0);

   double totpri = 0;
   double tottim = 0;
   double numact = 0;
   for (DymonDetailing dd : agent_manager.getActiveDetailings()) {
      double p = dd.computePriority();
      if (p != 0) {
	 ++numact;
	 totpri += p;
	 tottim += getTimePriority(dd);
       }
    }

   double x = 0;
   if (numact > 0) x = totpri / numact;
   double y = 0.5 * x * x - 1.25 * x + 1;	// 0->1, 1/2 -> 1/2, 1 -> 1/4

   report_time = y * DYMON_REPORTING_FRACTION;
   check_time = y - report_time;
   detail_time = 1.0 - y;

   boolean didcheck = false;

   if (chktime > 0 && now >= next_check) {
      next_check = now + DYMON_REPORT_CHECK_EVERY;
      didcheck = true;
      setCheckAndReportTimes(chktime,rpttime);
    }

   if (detail_time == 0) return;

   checkContinuous();

   for (DymonDetailing dd : agent_manager.getActiveDetailings()) {
      long delay = getDetailingTime(dd,tottim);
      if (delay < 0) continue;

      if (didcheck) {
	 System.err.println("DYMON: Delay " + dd.getDetailName() + " " +
			       detail_time + " " + getTimePriority(dd) + " " +
			       totpri + " " + tottim + " " + dd.getPriority() + " " +
			       allowed_overhead + " " + dd.getDetailOverhead() + " " +
			       dd.getDetailSlowdown() + " " + dd.getDetailInterval() + " " + delay +
			       " " + dd.isDetailing());
       }

      if (!dd.isDetailing()) {
	 DymonPatchRequest dpr = dd.setDetailing(now,delay);
	 if (dpr != null) dymon_main.requestPatch(dpr);
       }
    }
}



private double getTimePriority(DymonDetailing dd)
{
   double d = dd.getPriority();
   double alpha = dd.getDetailSlowdown();
   double cost = dd.getDetailOverhead() + (alpha / (1+alpha)) * dd.getDetailInterval();

   // return d;
   // return d * cost;
   return d * Math.sqrt(cost);
}




private void setCheckAndReportTimes(double chktime,double rpttime)
{
   if (!monitor_enabled) return;

   double alpha = check_time * allowed_overhead;
   long chk = (long)(chktime * (1 - alpha) / alpha);
   if (chk < DYMON_MIN_CHECK_TIME) chk = DYMON_MIN_CHECK_TIME;
   setDyperVar("CHECKTIME",Long.toString(chk));
   if (rpttime > 0) {
      alpha = report_time * allowed_overhead;
      long rpt = (long)(rpttime * (1 - alpha) / alpha);
      if (rpt < chk) rpt = chk;
      if (rpt < DYMON_MIN_REPORT_TIME) rpt = DYMON_MIN_REPORT_TIME;
      setDyperVar("REPORTTIME",Long.toString(rpt));
      System.err.println("DYMON: Set times for " + process_id + ": " +
			    chk + " " + rpt + " " + chktime + " " +
			    rpttime + " " + check_time + " " + report_time + " " +
			    allowed_overhead + " " + detail_time);
    }
}




private long getDetailingTime(DymonDetailing dd,double tottim)
{
   double p = getTimePriority(dd);
   if (p == 0) return -1;

   double ovhd = detail_time * (p/tottim) * allowed_overhead;
   double alpha = dd.getDetailSlowdown();
   double cost = dd.getDetailOverhead() + (alpha / (1+alpha)) * dd.getDetailInterval();
   double v = cost * (1 - ovhd) / ovhd;

   return (long) v;
}




/********************************************************************************/
/*										*/
/*	Continuous detailing methods						*/
/*										*/
/********************************************************************************/

private void checkContinuous()
{
   double remain = detail_time * continuous_fraction;
   double start = remain;

   // first see if any continous detailings need to be removed
   for (DymonDetailing dd : continuous_detailings.keySet()) {
      double allow = remain;
      if (!agent_manager.getActiveDetailings().contains(dd)) allow = 0;
      for (DymonDetailing ad : agent_manager.getActiveDetailings()) {
	 if (!continuous_detailings.containsKey(ad) &&
		ad.getContinuousPriority() > 0 &&
		ad.getContinuousOverhead() > 0 &&
		isSignificantlyBetter(ad,dd) &&
		ad.getContinuousOverhead() <= remain) {
	    allow -= ad.getContinuousOverhead();
	  }
       }
      if (dd.getContinuousOverhead() <= allow) {
	 remain -= dd.getContinuousOverhead();
       }
      else {
	 stopContinuous(dd);
       }
    }

   Set<DymonDetailing> dlst = new TreeSet<DymonDetailing>(new ContinuousComparator());
   for (DymonDetailing ad : agent_manager.getActiveDetailings()) {
      if (!continuous_detailings.containsKey(ad) && ad.getContinuousPriority() > 0 &&
	     ad.getContinuousOverhead() > 0) {
	 dlst.add(ad);
       }
    }

   for (DymonDetailing dd : dlst) {
      if (dd.getContinuousOverhead() > 0 && dd.getContinuousOverhead() < remain) {
	 startContinuous(dd);
	 remain -= dd.getContinuousOverhead();
       }
    }

   detail_time -= start - remain;
}




private void startContinuous(DymonDetailing dd)
{
   continuous_detailings.put(dd,Boolean.TRUE);
   dd.startContinuousTracing();
   System.err.println("DYMON: Start continuous tracing for " + process_id + " : " + dd.getDetailName());
}



private void stopContinuous(DymonDetailing dd)
{
   continuous_detailings.remove(dd);
   dd.endContinuousTracing();
   System.err.println("DYMON: Stop continuous tracing for " + process_id + " : " + dd.getDetailName());
}



private boolean isSignificantlyBetter(DymonDetailing cand,DymonDetailing act)
{
   double p0 = cand.getContinuousPriority();
   double p1 = act.getContinuousPriority();
   if (p0 > continuous_significance*p1) return true;
   return false;
}



private static class ContinuousComparator implements Comparator<DymonDetailing> {

   public int compare(DymonDetailing d1,DymonDetailing d2) {
      double v0 = d2.getContinuousPriority() - d1.getContinuousPriority();
      if (v0 < 0) return -1;
      else if (v0 > 0) return 1;
      else return 0;
    }

}	// end of subclass ContinuousCompartor




/********************************************************************************/
/*										*/
/*	Query methods								*/
/*										*/
/********************************************************************************/

Map<String,Number> handleSimpleQuery(String agent,String id)
{
   DymonAgent da = findAgent(agent);
   if (da != null) return da.handleSimpleQuery(id);

   return null;
}



DymonAgent findAgent(String name)
{
   return agent_manager.getAgent(name);
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputUIData(IvyXmlWriter xw)
{
   if (!isActive() && isAttached()) return;
   if (start_class == null) return;

   xw.begin("PROCESS");
   xw.textElement("ID",process_id);
   xw.textElement("START",start_class);
   if (start_jar != null) xw.textElement("JAR",start_jar);
   StringBuffer buf = new StringBuffer();
   for (String s : program_args) {
      if (buf.length() > 0) buf.append(" ");
      xw.textElement("ARG",s);
      if (s.length() > MAX_ARG_LENGTH) {
	 String t = s.substring(s.length() - (MAX_ARG_LENGTH - 8 - 3));
	 s = s.substring(0,8) + "..." + t;
       }
      buf.append(s);
    }
   xw.textElement("SHOWARGS",buf.toString());
   boolean fg = monitor_enabled && is_attached;
   xw.textElement("MONITOR",Boolean.toString(fg));
   xw.textElement("ATTACHED",Boolean.toString(isAttached()));
   xw.textElement("NAME",dymon_main.getOutputName(start_class,start_jar,program_args));
   xw.textElement("AGENTSET",getAgentSet());
   xw.end("PROCESS");
}



/********************************************************************************/
/*										*/
/*	Methods for fixing up provided information				*/
/*										*/
/********************************************************************************/

void fixStartClass()
{
   start_jar = null;

   if (start_class == null) return;

   if (!start_class.endsWith(".jar")) return;

   start_jar = start_class;

   try {
      JarFile jf = new JarFile(new File(start_class));
      Manifest mf = jf.getManifest();
      Attributes ma = mf.getMainAttributes();
      start_class = ma.getValue(Attributes.Name.MAIN_CLASS);
      jf.close();
    }
   catch (IOException e) {
      System.err.println("DYMON: Problem reading jar file: " + e);
    }
}




}	// end of class DymonProcess




/* end of DymonProcess.java */
