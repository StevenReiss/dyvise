/********************************************************************************/
/*										*/
/*		DyperMonitor.java						*/
/*										*/
/*	Monitoring thread for dyper performance evaluator			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyper/DyperMonitor.java,v 1.6 2016/11/02 18:59:20 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyperMonitor.java,v $
 * Revision 1.6  2016/11/02 18:59:20  spr
 * Move to asm5
 *
 * Revision 1.5  2010-03-30 16:19:22  spr
 * Remove debugging. Add xjar to Make.data.
 *
 * Revision 1.4  2009-10-07 01:00:17  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.3  2009-09-19 00:13:28  spr
 * Update agents for dynamic insertion and removal.
 *
 * Revision 1.2  2009-03-20 02:08:21  spr
 * Code cleanup; output information for incremental time-based display.
 *
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dyper;


import javax.management.*;

import java.lang.management.*;
import java.util.*;



class DyperMonitor implements DyperConstants {


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private DyperControl	the_control;
private ThreadMXBean	thread_bean;
private boolean 	doing_update;
private boolean 	no_updates;
private long		delay_time;
private long		disable_time;
private long		report_time;
private long		last_report;
private long		last_monitor;
private long		last_nano;
private boolean 	monitor_enabled;
private boolean 	reports_enabled;	// only used if monitoring disabled
private boolean 	contention_enabled;
private boolean 	timing_enabled;
private boolean 	show_stack;
private Map<String,DyperAgent> active_agents;
private Map<String,DyperAgent> inactive_agents;
private boolean 	need_report;
private boolean 	full_monitoring;

private double		report_total;
private long		num_reports;
private double		check_total;
private double		delay_total;
private long		num_checks;
private int		max_depth;
private double		last_check;

private static final double	CHECK_OVERHEAD = 1.000; 	// 1 ms for timer checks
private static final double	REPORT_OVERHEAD = 1.000;	// 1 ms for timer checks




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DyperMonitor(DyperControl dc)
{
   the_control = dc;
   doing_update = false;
   no_updates = false;
   delay_time = DYPER_CHECK_TIME;
   disable_time = DYPER_DISABLE_TIME;
   monitor_enabled = false;
   reports_enabled = false;
   contention_enabled = false;
   timing_enabled = false;
   report_time = DYPER_REPORT_TIME;
   last_report = 0;
   last_monitor = 0;
   last_nano = 0;
   show_stack = false;
   active_agents = new LinkedHashMap<String,DyperAgent>();
   inactive_agents = new HashMap<String,DyperAgent>();
   report_total = 0;
   num_reports = 0;
   check_total = 0;
   last_check = 0;
   delay_total = 0;
   num_checks = 0;
   need_report = false;
   max_depth = DYPER_MAX_DEPTH;
   full_monitoring = false;
   thread_bean = ManagementFactory.getThreadMXBean();

   Runtime.getRuntime().addShutdownHook(new DyperShutdown());
}




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

Thread start()
{
   ClassMonitor cm = new ClassMonitor();
   cm.start();

   return cm;
}



void addAgent(DyperAgent da)
{
   // System.err.println("DYPER: ADD AGENT " + da.getName());
   active_agents.put(da.getName(),da);
   da.enableMonitoring(monitor_enabled,0);
}



void deactivateAgent(String nm,long now)
{
   DyperAgent da = active_agents.remove(nm);
   // System.err.println("DYPER: REMOVE AGENT " + da.getName());
   if (da != null) {
      da.enableMonitoring(false,now);
      inactive_agents.put(da.getName(),da);
    }
}



void reactivateAgent(String nm)
{
   DyperAgent da = inactive_agents.remove(nm);
   // System.err.println("DYPER: REACTIVATE AGENT " + nm + " " + (da != null));
   if (da != null) addAgent(da);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void setDelayTime(long t)			{ delay_time = t; }

long getDelayTime()				{ return delay_time; }

void setDisableTime(long t)			{ disable_time = t; }

long getDisableTime()				{ return disable_time; }

void enableMonitoring(boolean fg)
{
   if (monitor_enabled == fg) return;

   // System.err.println("DYPER: MONITORING ENABLE: " + fg);

   monitor_enabled = fg;

   if (contention_enabled) enableContentionMonitoring(fg);
   if (timing_enabled) enableCpuTimeMonitoring(fg);

   long now = System.currentTimeMillis();
   for (DyperAgent da : active_agents.values()) {
      da.enableMonitoring(fg,now);
    }
}


void enableReporting(boolean fg)
{
   reports_enabled = fg;

   // System.err.println("DYPER: REPORTING ENABLE: " + fg);

   if (!monitor_enabled) the_control.wakeUp();
}




boolean isMonitoringEnabled()			{ return monitor_enabled; }
boolean isReportingEnabled()			{ return reports_enabled; }

void enableShowStack(boolean fg)		{ show_stack = fg; }

boolean isShowStackEnabled()			{ return show_stack; }


void setReportTime(long t)
{
   if (t == 0 || report_time == 0) last_report = 0;
   report_time = t;
}

long getReportTime()				{ return report_time; }



void setDetailing(String agt,String item,boolean fg)
{
   if (item == null) return;

   for (DyperAgent da : active_agents.values()) {
      if (agt == null || agt.equals(da.getName())) da.setDetailing(item,fg);
    }
}

void enableFullMonitoring(boolean fg)		{ full_monitoring = fg; }

boolean isFullMonitoring()			{ return full_monitoring; }




void setParameter(String nm,String val)
{
   try {
      if (nm.equals("CHECKTIME")) {
	 long t = Long.parseLong(val);
	 setDelayTime(t);
       }
      else if (nm.equals("DISABLETIME")) {
	 long t = Long.parseLong(val);
	 setDisableTime(t);
       }
      else if (nm.equals("REPORTTIME")) {
	 long t = Long.parseLong(val);
	 setReportTime(t);
       }
      else if (nm.equals("MONITOR")) {
	 boolean fg = Boolean.parseBoolean(val);
	 enableMonitoring(fg);
       }
      else if (nm.equals("SHOWSTACK")) {
	 boolean fg = Boolean.parseBoolean(val);
	 enableShowStack(fg);
       }
      else if (nm.equals("CONTENTION")) {
	 boolean fg = Boolean.parseBoolean(val);
	 enableContentionMonitoring(fg);
       }
      else if (nm.equals("CPUTIME")) {
	 boolean fg = Boolean.parseBoolean(val);
	 enableCpuTimeMonitoring(fg);
       }
      else if (nm.equals("MAXDEPTH")) {
	 int v = Integer.parseInt(val);
	 setMaxDepth(v);
       }
      else if (nm.equals("FULLMONITORING")) {
	 boolean fg = Boolean.parseBoolean(val);
	 enableFullMonitoring(fg);
       }
      else if (nm.equals("REPORTING")) {
	 reports_enabled = Boolean.parseBoolean(val);
       }
      else {
	 for (DyperAgent da : active_agents.values()) {
	    if (da.setParameter(nm,val)) return;
	  }
       }
    }
   catch (NumberFormatException e) {
    }
}



String getParameter(String nm)
{
   String rslt = null;

   if (nm.equals("CHECKTIME")) {
      rslt = Long.toString(getDelayTime());
    }
   else if (nm.equals("DISABLETIME")) {
      rslt = Long.toString(getDisableTime());
    }
   else if (nm.equals("REPORTTIME")) {
      rslt = Long.toString(getReportTime());
    }
   else if (nm.equals("MONITOR")) {
      rslt = Boolean.toString(isMonitoringEnabled());
    }
   else if (nm.equals("SHOWSTACK")) {
      rslt = Boolean.toString(isShowStackEnabled());
    }
   else if (nm.equals("CONTENTION")) {
      rslt = Boolean.toString(isContentionMonitoringEnabled());
    }
   else if (nm.equals("CPUTIME")) {
      rslt = Boolean.toString(isCpuTimeMonitoringEnabled());
    }
   else if (nm.equals("REPORTING")) {
      rslt = Boolean.toString(isReportingEnabled());
    }
   else {
      for (DyperAgent da : active_agents.values()) {
	 String v = da.getParameter(nm);
	 if (v != null) {
	    rslt = v;
	    break;
	  }
       }
    }

   return rslt;
}



String clear(String agt)
{
   long now = System.currentTimeMillis();

   if (isContentionMonitoringEnabled()) {
      enableContentionMonitoring(false);
      enableContentionMonitoring(true);
    }
   if (thread_bean.isThreadCpuTimeEnabled()) {
      enableCpuTimeMonitoring(false);
      enableCpuTimeMonitoring(true);
    }

   for (DyperAgent da : active_agents.values()) {
      if (agt == null || agt.equals(da.getName())) da.clear(now);
    }

   report_total = 0;
   num_reports = 0;
   check_total = 0;
   last_check = 0;
   delay_total = 0;
   num_checks = 0;

   return Long.toString(now);
}


void enableContentionMonitoring(boolean fg)
{
   contention_enabled = fg;
   if (!monitor_enabled) return;

   if (!fg && isContentionMonitoringEnabled()) {
      for (DyperAgent da : active_agents.values()) {
	 da.handleContentionMonitoring(false,last_report);
       }
      thread_bean.setThreadContentionMonitoringEnabled(fg);
    }
   else if (fg && !isContentionMonitoringEnabled()) {
      long now = System.currentTimeMillis();
      thread_bean.setThreadContentionMonitoringEnabled(fg);
      for (DyperAgent da : active_agents.values()) {
	 da.handleContentionMonitoring(true,now);
       }
    }
}

boolean isContentionMonitoringEnabled()
{
   return thread_bean.isThreadContentionMonitoringEnabled();
}



void enableCpuTimeMonitoring(boolean fg)
{
   timing_enabled = fg;
   if (!monitor_enabled) return;

   if (!fg && thread_bean.isThreadCpuTimeEnabled()) {
      for (DyperAgent da : active_agents.values()) {
	 da.handleCpuTimeMonitoring(false,last_report);
       }
      thread_bean.setThreadCpuTimeEnabled(fg);
    }
   else if (fg && !thread_bean.isThreadCpuTimeEnabled()) {
      long now = System.currentTimeMillis();
      thread_bean.setThreadCpuTimeEnabled(fg);
      for (DyperAgent da : active_agents.values()) {
	 da.handleCpuTimeMonitoring(true,now);
       }
    }
}

boolean isCpuTimeMonitoringEnabled()
{
   return thread_bean.isThreadCpuTimeEnabled();
}



int getMaxDepth()			{ return max_depth; }
void setMaxDepth(int v) 		{ max_depth = v; }



/********************************************************************************/
/*										*/
/*	Methods to dump the heap						*/
/*										*/
/********************************************************************************/

String dumpHeap(String file,boolean live)
{
   Object [] args = new Object[] { file, Boolean.valueOf(live) };
   String [] sgns = new String[] { "java.lang.String", "boolean" };
   ObjectName hsdo;

   try {
      hsdo = new ObjectName("com.sun.management:type=HotSpotDiagnostic");
    }
   catch (MalformedObjectNameException e) {
      return "BAD OBJECT NAME";
    }

   ArrayList<MBeanServer> mbsl = MBeanServerFactory.findMBeanServer(null);

   for (MBeanServer mbs : mbsl) {
      try {
	 mbs.invoke(hsdo,"dumpHeap",args,sgns);
	 return "OK";
       }
      catch (InstanceNotFoundException e) { }
      catch (Throwable t) {
	 return "ERROR: " + t;
       }
    }

   return "NOT FOUND";
}



/********************************************************************************/
/*										*/
/*	Methods to dump memory model						*/
/*										*/
/********************************************************************************/

String dumpMemory(String file)
{
   DyperNative.dumpHeapModel(file);

   return "OK";
}


/********************************************************************************/
/*										*/
/*	Time methods								*/
/*										*/
/********************************************************************************/

private long  getNextDelayTime()
{
   if (monitor_enabled) {
      if (full_monitoring) return 0;
      return (long)(-delay_time * Math.log(Math.random())) + 1;
    }
   else if (reports_enabled || need_report) return disable_time;
   else return -1;
}




/********************************************************************************/
/*										*/
/*	Reporting methods							*/
/*										*/
/********************************************************************************/

void generateReport(ReportType rt,DyperXmlWriter xw)
{
   xw.begin("MONITOR");
   xw.field("TIME",last_monitor);
   xw.field("NANO",last_nano);
   xw.field("OVERHEAD",the_control.getOverhead());
   if (num_checks > 0) {
      xw.field("LASTCHECK",last_check);
      xw.field("CHECKTIME",check_total/num_checks + CHECK_OVERHEAD);
      xw.field("CHECKTOTAL",check_total);
    }
   if (num_reports > 0) xw.field("REPORTTIME",report_total/num_reports + REPORT_OVERHEAD);
   if (num_checks > 0) xw.field("DELAYAVG",delay_total/num_checks);

   for (DyperAgent da : active_agents.values()) {
      da.generateReport(rt,xw,last_monitor);
    }

   xw.end("MONITOR");
}



/********************************************************************************/
/*										*/
/*	Methods to handle Stack monitoring					*/
/*										*/
/********************************************************************************/

private void monitorStacks(long now,ThreadInfo [] tinfo,DyperXmlWriter xw)
{
   for (int i = 0; i < tinfo.length; ++i) {
      ThreadInfo ti = tinfo[i];
      if (ti == null) continue;
      long tid = ti.getThreadId();
      if (tid == Thread.currentThread().getId()) continue;
      if (!the_control.useThread(tid)) continue;
      if (xw != null || !active_agents.isEmpty()) {
	 StackTraceElement [] trc = ti.getStackTrace();
	 if (xw != null) dumpThreadInfo(ti,trc,xw);
	 for (DyperAgent da : active_agents.values()) {
	    da.handleThreadStack(now,ti,trc);
	  }
       }
    }
}




/********************************************************************************/
/*										*/
/*	Methods to handle state dumps						*/
/*										*/
/********************************************************************************/

void generateStackDump(DyperXmlWriter xw)
{
   long now = System.currentTimeMillis();
   xw.begin("THREADS");
   xw.field("TIME",now);

   long [] tids = thread_bean.getAllThreadIds();
   ThreadInfo [] tinfo = thread_bean.getThreadInfo(tids,max_depth);

   for (int i = 0; i < tinfo.length; ++i) {
      if (tinfo[i] == null) continue;
      long tid = tinfo[i].getThreadId();
      if (tid == Thread.currentThread().getId()) continue;
      StackTraceElement [] trc = tinfo[i].getStackTrace();
      dumpThreadInfo(tinfo[i],trc,xw);
    }

   xw.end("THREADS");
}





private void dumpThreadInfo(ThreadInfo tinfo,StackTraceElement [] trc,DyperXmlWriter xw)
{
   xw.begin("THREAD");
   xw.field("ID",tinfo.getThreadId());
   xw.field("NAME",tinfo.getThreadName());
   xw.field("STATE",tinfo.getThreadState());
   xw.field("BLOCKCT",tinfo.getBlockedCount());
   xw.field("BLOCKTIME",tinfo.getBlockedTime());
   String lock = tinfo.getLockName();
   if (lock != null) {
      xw.field("LOCK",lock);
      xw.field("LOCKOWNER",tinfo.getLockOwnerId());
    }
   xw.field("WAITCT",tinfo.getWaitedCount());
   xw.field("WAITTIME",tinfo.getWaitedTime());
   for (int i = 0; i < trc.length; ++i) {
      xw.begin("STACK");
      xw.field("CLASS",trc[i].getClassName());
      xw.field("METHOD",trc[i].getMethodName());
      xw.field("LINE",trc[i].getLineNumber());
      xw.field("FILE",trc[i].getFileName());
      xw.end();
    }
   xw.end("THREAD");
}




/********************************************************************************/
/*										*/
/*	Methods for handling stack checking					*/
/*										*/
/********************************************************************************/

private class ClassMonitor extends Thread {

   ClassMonitor() {
      super("DyperMonitorThread");
      setDaemon(true);
    }

   public void run() {
      the_control.sendStart();

      long nextcheck = 0;

      for ( ; ; ) {
	 long delay = getNextDelayTime();
	 // System.err.println("DYPER: Delay " + delay + " " + delay_time);
	 while (delay < 0) {
	    the_control.handleRequests(true);
	    delay = getNextDelayTime();
	    // System.err.println("DYPER: Sleep Delay " + delay + " " + delay_time);
	  }

	 if (delay > DYPER_MAX_DELAY) {
	    if (monitor_enabled && nextcheck == 0) nextcheck = delay;
	    delay = DYPER_MAX_DELAY;
	  }

	 if (delay != 0) {
	    // System.err.println("DYPER: Wait for " + delay);
	    synchronized (this) {
	       try {
		  wait(delay);
		}
	       catch (InterruptedException e) { }
	     }
	  }

	 // System.err.println("DYPER: Doing update " + no_updates + " " + monitor_enabled + " " + show_stack + " " + report_time);

	 doing_update = true;
	 if (no_updates) {
	    doing_update = false;
	    return;
	  }

	 long now = System.currentTimeMillis();
	 long nnow = System.nanoTime();
	 long tnow = nnow;

	 if (monitor_enabled || show_stack) {
	    if (nextcheck == 0 || now - last_monitor >= nextcheck) {
	       last_monitor = now;
	       last_nano = nnow;
	       try {
		  monitorThreads();
		}
	       catch (Throwable t) {
		  System.err.println("DYPER: Problem monitoring threads: " + t);
		  t.printStackTrace();
		}
	       tnow = System.nanoTime();
	       last_check = (tnow - nnow) / 1000000.0;
	       check_total += last_check;
	       delay_total += delay;
	       num_checks++;
	       nextcheck = 0;
	       need_report = true;
	     }
	  }

	 if (report_time > 0 && (monitor_enabled || reports_enabled || need_report)) {
	    try {
	       if (now - last_report >= report_time) {
		  // System.err.println("DYPER: SEND REPORT " + now);
		  tnow = System.nanoTime();
		  long rnow = tnow;
		  if (last_report != 0) rnow = the_control.sendReport(now);
		  last_report = now;
		  report_total += (rnow - tnow) / 1000000.0;
		  num_reports++;
		  need_report = false;
		}
	     }
	    catch (Throwable t) {
	       System.err.println("DYPER: Problem generating report: " + t);
	       t.printStackTrace();
	     }
	  }

	 the_control.handleRequests(false);

	 doing_update = false;
       }
    }

   private void monitorThreads() {
      long [] tids = thread_bean.getAllThreadIds();
      ThreadInfo [] tinfo = thread_bean.getThreadInfo(tids,max_depth);
      try {
         DyperXmlWriter xw = null;
         if (show_stack) {
            xw = new DyperXmlWriter();
            xw.begin("THREADS");
            xw.field("TIME",last_monitor);
          }
   
         if (monitor_enabled) {
            monitorStacks(last_monitor,tinfo,xw);
          }
   
         if (xw != null) {
            xw.end("THREADS");
            the_control.sendStackDump(last_monitor,xw.toString());
          }
       }
      catch (Throwable t) {
         System.err.println("DYPER: Problem during monitoring: " + t);
         t.printStackTrace();
       }
    }

}	// end of subclass ClassMonitor




/********************************************************************************/
/*										*/
/*	Exit management 							*/
/*										*/
/********************************************************************************/

private class DyperShutdown extends Thread {

   DyperShutdown() {
      super("DyperShutdown");
    }

   public void run() {
      no_updates = true;
      synchronized (DyperMonitor.this) { DyperMonitor.this.notifyAll(); }
      for (int i = 0; doing_update && i < 10000000; ++i) System.currentTimeMillis();
    }

}	// end of subclass DyperShutdown



}	// end of class DyperMonitor







/* end of DyperMonitor.java */

