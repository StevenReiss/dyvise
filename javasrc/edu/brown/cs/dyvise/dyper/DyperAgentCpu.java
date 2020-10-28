/********************************************************************************/
/*										*/
/*		DyperAgentCpu.java						*/
/*										*/
/*	Monitor agent that does counting					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyper/DyperAgentCpu.java,v 1.5 2016/11/02 18:59:18 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyperAgentCpu.java,v $
 * Revision 1.5  2016/11/02 18:59:18  spr
 * Move to asm5
 *
 * Revision 1.4  2013/09/04 18:36:35  spr
 * Minor bug fixes.
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


import java.lang.management.ThreadInfo;
import java.util.HashMap;
import java.util.Map;




public class DyperAgentCpu extends DyperAgent {


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private Map<String,PerfCounter> counter_map;
private Map<String,PerfCounter> current_map;
private long		class_total;
private long		method_total;
private long		line_total;
private long		class_base;
private long		method_base;
private long		line_base;
private long		last_sample;
private boolean 	have_active;
private long		sample_count;
private long		active_samples;
private StackTraceElement [] sys_stack;

private static double	class_detail_min_total = CLASS_DETAIL_MIN_TOTAL_INIT;
private static double	class_detail_minimum = CLASS_DETAIL_MINIMUM_INIT;
private static double	class_detail_threshold = CLASS_DETAIL_THRESHOLD_INIT;
private static double	class_detail_stop_threshold = CLASS_DETAIL_STOP_THRESHOLD_INIT;

private static double	method_detail_min_total = METHOD_DETAIL_MIN_TOTAL_INIT;
private static double	method_detail_minimum = METHOD_DETAIL_MINIMUM_INIT;
private static double	method_detail_threshold = METHOD_DETAIL_THRESHOLD_INIT;
private static double	method_detail_stop_threshold = METHOD_DETAIL_STOP_THRESHOLD_INIT;

private static long [][][]	 item_counts;

private static int	min_counter = 0;
private static boolean	min_counter_set = false;


private static final int OCCUR_COUNT = 0;
private static final int TOTAL_TIME = 1;
private static final int TOTAL_TIME2 = 2;
private static final int NUM_STATS = 3;

static {
   item_counts = new long[10][][];
   item_counts[0] = new long[1024][NUM_STATS];
}

private static boolean	detail_all = true;
private static boolean	get_timings = false;

private static DyperAgentCpu cpu_agent = null;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public DyperAgentCpu(DyperControl dc)
{
   super(dc,"CPU");

   cpu_agent = this;

   counter_map = new HashMap<String,PerfCounter>();
   current_map = null;

   class_total = 0;
   method_total = 0;
   line_total = 0;
   class_base = 0;
   method_base = 0;
   line_base = 0;
   sample_count = 0;
   active_samples = 0;
   have_active = false;
   sys_stack = new StackTraceElement[1024];
}



/********************************************************************************/
/*										*/
/*	Parameter methods							*/
/*										*/
/********************************************************************************/

public boolean setParameter(String s,String sv)
{
   CounterParameter p;
   double v;

   try {
      p = CounterParameter.valueOf(s);
      v = Double.parseDouble(sv);
    }
   catch (IllegalArgumentException e) {
      return false;
    }

   switch (p) {
      case DETAIL_ALL :
	 detail_all = (v != 0);
	 break;
      case CLASS_DETAIL_MIN_TOTAL :
	 class_detail_min_total = v;
	 break;
      case CLASS_DETAIL_MINIMUM :
	 class_detail_minimum = v;
	 break;
      case CLASS_DETAIL_THRESHOLD :
	 class_detail_threshold = v;
	 break;
      case CLASS_DETAIL_STOP_THRESHOLD :
	 class_detail_stop_threshold = v;
	 break;
      case METHOD_DETAIL_MIN_TOTAL :
	 method_detail_min_total = v;
	 break;
      case METHOD_DETAIL_MINIMUM :
	 method_detail_minimum = v;
	 break;
      case METHOD_DETAIL_THRESHOLD :
	 method_detail_threshold = v;
	 break;
      case METHOD_DETAIL_STOP_THRESHOLD :
	 method_detail_stop_threshold = v;
	 break;
      case MIN_COUNTER :
	 if (!min_counter_set) {
	    min_counter = (int) v;
	    min_counter_set = true;
	  }
	 break;
      default :
	 return false;
    }

   return true;
}



public String getParameter(String s)
{
   CounterParameter p;

   try {
      p = CounterParameter.valueOf(s);
    }
   catch (IllegalArgumentException e) {
      return null;
    }

   double v = 0;

   switch (p) {
      case CLASS_DETAIL_MIN_TOTAL :
	 v = class_detail_min_total;
	 break;
      case CLASS_DETAIL_MINIMUM :
	 v = class_detail_minimum;
	 break;
      case CLASS_DETAIL_THRESHOLD :
	 v = class_detail_threshold;
	 break;
      case CLASS_DETAIL_STOP_THRESHOLD :
	 v = class_detail_stop_threshold;
	 break;
      case METHOD_DETAIL_MIN_TOTAL :
	 v = method_detail_min_total;
	 break;
      case METHOD_DETAIL_MINIMUM :
	 v = method_detail_minimum;
	 break;
      case METHOD_DETAIL_THRESHOLD :
	 v = method_detail_threshold;
	 break;
      case METHOD_DETAIL_STOP_THRESHOLD :
	 v = method_detail_stop_threshold;
	 break;
      default :
	 return null;
    }

   return Double.toString(v);
}



public void setDetailing(String item,boolean fg)
{
   PerfCounter pc = counter_map.get(item);
   if (pc != null) {
      pc.setDetailed(System.currentTimeMillis(),fg);
    }
}



/********************************************************************************/
/*										*/
/*	Clear methods								*/
/*										*/
/********************************************************************************/

public void handleClear(long now)
{
   counter_map = new HashMap<String,PerfCounter>();
   class_total = 0;
   method_total = 0;
   line_total = 0;
   class_base = 0;
   method_base = 0;
   line_base = 0;
   sample_count = 0;
   active_samples = 0;
   last_sample = 0;
   have_active = false;

   int ln1 = item_counts.length;
   long [][][] nitms = new long[ln1][][];
   for (int i = 0; i < ln1; ++i) {
      long [][] itms = item_counts[i];
      if (itms == null) break;
      int ln2 = itms.length;
      nitms[i] = new long[ln2][NUM_STATS];
    }
   item_counts = nitms;
}




/********************************************************************************/
/*										*/
/*	Reporting methods							*/
/*										*/
/********************************************************************************/

public void generateReport(ReportType rt,DyperXmlWriter xw,long now)
{
   xw.begin("COUNTERS");
   xw.field("MONTIME",getMonitoredTime(now));
   xw.field("SAMPLES",sample_count);
   xw.field("ACTIVE",active_samples);
   xw.field("LAST",last_sample);

   xw.begin("TOTALS");
   xw.field("CLASS",class_total);
   xw.field("METHOD",method_total);
   xw.field("LINE",line_total);
   xw.field("CBASE",class_base);
   xw.field("MBASE",method_base);
   xw.field("LBASE",line_base);
   xw.end();

   for (Map.Entry<String,PerfCounter> ent : counter_map.entrySet()) {
      ent.getValue().report(ent.getKey(),xw);
    }

   int base = min_counter;
   for (int i = 0; item_counts[i] != null; ++i) {
      long [][] cts = item_counts[i];
      for (int j = 0; j < cts.length; ++j) {
	 if (cts[j][OCCUR_COUNT] != 0 || cts[j][TOTAL_TIME] != 0) {
	    xw.begin("RTITEM");
	    xw.field("COUNTER",j+base);
	    if (cts[j][OCCUR_COUNT] != 0) xw.field("COUNT",cts[j][OCCUR_COUNT]);
	    if (cts[j][TOTAL_TIME] != 0) xw.field("TIME",cts[j][TOTAL_TIME]/1000000.0);
	    xw.end("RTITEM");
	  }
       }
      base += cts.length;
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
   if (ti.getThreadState() != Thread.State.RUNNABLE) return;

   if (last_sample != now) {
      ++sample_count;
      current_map = counter_map;
      last_sample = now;
      have_active = false;
    }

   int depth = 0;
   boolean haveuser = false;
   int scnt = 0;
   for (int j = 0; j < trc.length; ++j) {
      StackTraceElement te = trc[j];
      String nm = te.getClassName();
      if (j == 0 && the_control.isIOClass(nm)) break;
      if (!haveuser && te.isNativeMethod()) continue;
      boolean sysfg = the_control.isSystemClass(nm);
      if (sysfg) {
	 if (haveuser) sys_stack[scnt++] = te;
	 continue;
       }
      if (!have_active) {
	 have_active = true;
	 ++active_samples;
       }
      haveuser = true;
      if (scnt > 0) {
	 for (int k = 0; k < scnt; ++k) {
	    handleStackItem(now,sys_stack[k],null,depth++);
	    sys_stack[k] = null;
	  }
	 scnt = 0;
       }
      handleStackItem(now,te,nm,depth++);
    }

   for (int k = 0; k < scnt; ++k) sys_stack[k] = null;
}


private void handleStackItem(long now,StackTraceElement te,String nm,int depth)
{
   if (nm == null) nm = te.getClassName();
   PerfCounter c = current_map.get(nm);
   if (c == null) {
      c = new PerfCounter();
      current_map.put(nm,c);
    }
   c.incr(depth);
   class_total++;
   if (depth == 0) ++class_base;
   if (c.checkClassDetailed(now,class_base)) {
      nm = nm + "@" + te.getMethodName();
      PerfCounter mc = current_map.get(nm);
      if (mc == null) {
	 mc = new PerfCounter();
	 current_map.put(nm,mc);
       }
      mc.incr(depth);
      method_total++;
      if (depth == 0) ++method_base;
      if (mc.checkMethodDetailed(now,method_base)) {
	 nm = nm + "@" + te.getLineNumber();
	 PerfCounter lc = current_map.get(nm);
	 if (lc == null) {
	    lc = new PerfCounter();
	    current_map.put(nm,lc);
	  }
	 lc.incr(depth);
	 line_total++;
	 if (depth == 0) ++line_base;
       }
    }
}




/********************************************************************************/
/*										*/
/*	PerfCounter class							*/
/*										*/
/********************************************************************************/

private static class PerfCounter {

   private long total_count;
   private long base_count;
   private boolean do_detail;
   private long detail_start;
   private long detail_delta;
   private boolean was_detailed;
   private boolean has_changed;

   PerfCounter() {
      total_count = 0;
      base_count = 0;
      do_detail = detail_all;
      detail_start = 0;
      detail_delta = 0;
      was_detailed = detail_all;
      has_changed = true;
    }

   void incr(int lvl) {
      total_count++;
      if (lvl == 0) base_count++;
      has_changed = true;
    }

   void setDetailed(long now,boolean fg) {
      if (do_detail == fg) return;
      do_detail = fg;
      was_detailed |= fg;
      if (fg) detail_start = now;
      else {
	 detail_delta += now - detail_start;
	 detail_start = 0;
       }
    }

   boolean checkClassDetailed(long now,double total) {
      if (total < class_detail_min_total) return do_detail;
      if (base_count < class_detail_minimum) return do_detail;
      double v = base_count/total;
      if (do_detail && !detail_all) {
	 if (v < class_detail_stop_threshold) setDetailed(now,false);
       }
      else if (v > class_detail_threshold) setDetailed(now,true);
      return do_detail;
    }

   boolean checkMethodDetailed(long now,double total) {
      if (total < method_detail_min_total) return do_detail;
      if (base_count < method_detail_minimum) return do_detail;
      double v = base_count/total;
      if (do_detail && !detail_all) {
	 if (v < method_detail_stop_threshold) setDetailed(now,false);
       }
      else if (v > method_detail_threshold) setDetailed(now,true);
      return do_detail;
    }

   void report(String nm,DyperXmlWriter xw) {
      if (!has_changed) return;
      has_changed = false;
      xw.begin("ITEM");
      xw.field("NAME",nm);
      if (!detail_all) {
         if (do_detail) xw.field("DETAILED",true);
         if (was_detailed) xw.field("PDET",true);
         if (detail_delta > 0) xw.field("DTOTAL",detail_delta);
         if (detail_start > 0) xw.field("DSTART",detail_start);
       }
      xw.field("TOTAL",total_count);
      xw.field("BASE",base_count);
      xw.end();
    }

}	// end of subclass PerfCounter




/********************************************************************************/
/*										*/
/*	Run time counter methods						*/
/*										*/
/********************************************************************************/

public static void monCountItem(int no)
{
   if (no < 0) return;

   increment(no,0);
}



private static void increment(int itm,long delta)
{
   // System.err.println("DYPER: COUNT " + itm + " " + delta);

   itm -= min_counter;
   if (itm < 0) return;
   if (!cpu_agent.the_control.useThread()) return;

   for (int i = 0; i < item_counts.length; ++i) {
      long [][] cts = item_counts[i];
      if (cts == null) {
	 synchronized (DyperAgentCpu.class) {
	    int len = item_counts[i-1].length;
	    cts = item_counts[i];
	    if (cts == null) {
	       cts = new long[len*2][NUM_STATS];
	       item_counts[i] = cts;
	     }
	  }
       }
      int ln = cts.length;
      if (itm < ln) {
	 cts[itm][OCCUR_COUNT]++;
	 if (delta > 0) {
	    cts[itm][TOTAL_TIME] += delta;
	    cts[itm][TOTAL_TIME2] += delta/1000*delta/1000;
	  }
	 break;
       }
      else itm -= ln;
    }
}




/********************************************************************************/
/*										*/
/*	Run time enter/exit methods						*/
/*										*/
/********************************************************************************/

public static long monMethodEnter(int no)
{
   if (get_timings) return System.nanoTime();

   return 0;
}



public static void monMethodExit(int no,long start)
{
   if (get_timings) {
      long now = System.nanoTime();
      long delta = now - start;
      if (delta == 0) delta = 500;		   // < 1 us -- give average

      increment(no,delta);
    }
   else {
      increment(no,0);
    }
}



}	// end of class DyperAgentCpu



/* end of DyperAgentCpu.java */
