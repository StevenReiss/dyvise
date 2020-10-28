/********************************************************************************/
/*										*/
/*		DyperAgentTiming.java						*/
/*										*/
/*	Monitor agent that does timing-based analysis				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyper/DyperAgentTiming.java,v 1.3 2016/11/02 18:59:19 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyperAgentTiming.java,v $
 * Revision 1.3  2016/11/02 18:59:19  spr
 * Move to asm5
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


import java.lang.management.*;
import java.util.*;




public class DyperAgentTiming extends DyperAgent {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private Map<Long,TimeInfo> time_data;
private long		last_check;
private ThreadMXBean	thread_bean;
private int		num_processors;
private long		monitor_start;

private long		tot_threads;
private double		tot_live;
private double		tot_live2;
private long		num_checks;
private double		total_monitor;
private long		num_created;
private long		num_died;
private long		num_active;

private static final long DEAD_TIME = 120000l;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public DyperAgentTiming(DyperControl dc)
{
   super(dc,"TIMING");

   time_data = new HashMap<Long,TimeInfo>();
   last_check = 0;
   monitor_start = 0;
   tot_threads = 0;
   tot_live = 0;
   tot_live2 = 0;
   num_checks = 0;
   total_monitor = 0;
   num_created = 0;
   num_died = 0;

   thread_bean = ManagementFactory.getThreadMXBean();

   Runtime rt = Runtime.getRuntime();
   num_processors = rt.availableProcessors();

   if (thread_bean.isThreadCpuTimeEnabled()) monitor_start = the_control.getStartTime();
}



/********************************************************************************/
/*										*/
/*	Reporting methods							*/
/*										*/
/********************************************************************************/

public void generateReport(ReportType rt,DyperXmlWriter xw,long now)
{
   xw.begin("TIMING");
   double tmon = total_monitor;
   if (monitor_start > 0) tmon += last_check - monitor_start;
   xw.field("CLOCK",tmon);

   xw.field("PROCS",num_processors);
   xw.field("NLIVE",tot_live);
   xw.field("NLIVE2",tot_live2);
   xw.field("NCOUNT",num_checks);
   xw.field("TOTAL",tot_threads);
   xw.field("CREATED",num_created);
   xw.field("DIED",num_died);
   xw.field("ACTIVE",num_active);

   for (Iterator<TimeInfo> it = time_data.values().iterator(); it.hasNext(); ) {
      TimeInfo ti = it.next();
      ti.report(xw);
      if (ti.isStale()) {
	 ++num_died;
	 it.remove();
       }
    }

   xw.end();
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

public void handleThreadStack(long now,ThreadInfo ti,StackTraceElement [] trc)
{
   if (last_check != now) {
      ++num_checks;
      num_active = thread_bean.getThreadCount();
      double ct = num_active;
      tot_live += ct;
      tot_live2 += ct*ct;
      tot_threads = thread_bean.getTotalStartedThreadCount();
    }

   last_check = now;

   long tid = ti.getThreadId();
   TimeInfo td = time_data.get(tid);
   if (td == null) {
      td = new TimeInfo(tid,ti.getThreadName(),now);
      time_data.put(tid,td);
      ++num_created;
    }
   td.record(now,ti);
}



public void handleCpuTimeMonitoring(boolean start,long now)
{
   if (start) monitor_start = now;
   else total_monitor += now-monitor_start;

   for (TimeInfo ti : time_data.values()) {
      ti.handleCpuTimeMonitoring(start,now);
    }

   if (!start) monitor_start = 0;
}



public void handleClear(long now)
{
   // cpu time monitoring has been reset

   time_data = new HashMap<Long,TimeInfo>();

   last_check = 0;
   tot_threads = 0;
   tot_live = 0;
   tot_live2 = 0;
   num_checks = 0;
   total_monitor = 0;
   num_created = 0;
   num_died = 0;
}




/********************************************************************************/
/*										*/
/*	TimeInfo class								*/
/*										*/
/********************************************************************************/

private class TimeInfo {

   private long 	thread_id;
   private String	thread_name;
   private long 	start_time;
   private long 	last_time;
   private double	user_time;
   private double	cpu_time;
   private double	monitored_time;
   private double	user_delta;
   private double	user_saved;
   private double	cpu_delta;
   private double	cpu_saved;

   TimeInfo(long tid,String nm,long now) {
      thread_id = tid;
      thread_name = nm;
      start_time = now;
      last_time = now;
    }

   void record(long now,ThreadInfo ti) {
      last_time = now;
      if (monitor_start > 0) {
	 long t = thread_bean.getThreadCpuTime(thread_id);
	 if (t >= 0) cpu_time = t;
	 t = thread_bean.getThreadUserTime(thread_id);
	 if (t >= 0) user_time = t;
       }
    }

   void handleCpuTimeMonitoring(boolean start,long now) {
      if (start) {
	 cpu_delta = thread_bean.getThreadCpuTime(thread_id);
	 user_delta = thread_bean.getThreadUserTime(thread_id);
       }
      else {
	 if (cpu_time > cpu_delta) cpu_saved += cpu_time - cpu_delta;
	 if (user_time > user_delta) user_saved += user_time - user_delta;
	 cpu_time = cpu_delta = 0;
	 user_time = user_delta = 0;
	 long st = monitor_start;
	 if (start_time > st) start_time = st;
	 if (last_time > st) monitored_time += last_time - st;
       }
    }

   boolean isStale() {
      if (last_check - last_time > DEAD_TIME) return true;
      return false;
    }

   void report(DyperXmlWriter xw) {
      double clk = monitored_time;
      if (monitor_start != 0 && last_time > monitor_start) clk += last_time - monitor_start;
      if (clk == 0) return;
   
      xw.begin("THREAD");
      xw.field("ID",thread_id);
      xw.field("NAME",thread_name);
      xw.field("START",start_time);
      if (last_time != last_check) xw.field("END",last_time);
      else xw.field("LAST",last_time);
   
      xw.field("CLOCK",clk);
   
      double cpu = cpu_saved;
      if (cpu_time > cpu_delta) cpu += cpu_time - cpu_delta;
      xw.field("CPU",cpu / 1000000.0);
   
      double user = user_saved;
      if (user_time > user_delta) user += user_time - user_delta;
      xw.field("USER",user / 1000000.0);
   
      xw.end();
    }

}	// end of subclass TimeInfo




}	// end of class DyperAgentTiming



/* end of DyperAgentTiming.java */
