/********************************************************************************/
/*										*/
/*		DyperAgentMemory.java						*/
/*										*/
/*	Monitor agent that does memory and allocation analysis			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyper/DyperAgentMemory.java,v 1.7 2016/11/02 18:59:18 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyperAgentMemory.java,v $
 * Revision 1.7  2016/11/02 18:59:18  spr
 * Move to asm5
 *
 * Revision 1.6  2012-10-05 00:53:01  spr
 * Code clean up.
 *
 * Revision 1.5  2011-04-01 23:09:11  spr
 * Code clean up.
 *
 * Revision 1.4  2009-09-19 00:13:28  spr
 * Update agents for dynamic insertion and removal.
 *
 * Revision 1.3  2008-12-04 01:11:12  spr
 * Clean up memory agent.
 *
 * Revision 1.2  2008-11-12 14:11:10  spr
 * Handle continuous memory tracing.  Other minor cleanups.
 *
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/


package edu.brown.cs.dyvise.dyper;


import java.lang.management.*;
import java.util.*;




public class DyperAgentMemory extends DyperAgent {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private MemoryMXBean	memory_bean;
private List<MemoryPoolMXBean> pool_beans;
private List<GarbageCollectorMXBean> gc_beans;

private long		last_check;
private long		start_time;

private MemStats	heap_usage;
private MemStats	nonheap_usage;
private Map<String,MemStats> pool_usage;
private Map<String,GCStats> gc_usage;

private Map<String,ItemCount> class_counts;
private Map<Thread,ItemCount> thread_counts;

private static long	alloc_count = 0;
private static int	monitor_type = 0;
private long		count_start;
private double		total_count;


private static boolean [] in_new = new boolean[DYPER_MAX_THREADS];
private static DyperAgentMemory mem_agent = null;

private static final int	MONITOR_COUNT = 0;
private static final int	MONITOR_TYPE = 1;
private static final int	MONITOR_SOURCE = 2;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public DyperAgentMemory(DyperControl dc)
{
   super(dc,"MEMORY");

   mem_agent = this;

   memory_bean = ManagementFactory.getMemoryMXBean();
   pool_beans = ManagementFactory.getMemoryPoolMXBeans();
   gc_beans = ManagementFactory.getGarbageCollectorMXBeans();

   RuntimeMXBean rt = ManagementFactory.getRuntimeMXBean();
   start_time = rt.getStartTime();

   last_check = System.currentTimeMillis();

   count_start = 0;
   total_count = 0;

   heap_usage = new MemStats("HEAP");
   nonheap_usage = new MemStats("NONHEAP");
   pool_usage = new HashMap<String,MemStats>();
   gc_usage = new HashMap<String,GCStats>();
   class_counts = new HashMap<String,ItemCount>();
   thread_counts = new HashMap<Thread,ItemCount>();

   for (MemoryPoolMXBean pb : pool_beans) {
      pool_usage.put(pb.getName(),new MemStats("POOL_" + pb.getName()));
    }

   for (GarbageCollectorMXBean gb : gc_beans) {
      gc_usage.put(gb.getName(),new GCStats(gb.getName()));
    }

   long mid = dc.getMonitorThreadId();
   in_new[(int)mid] = true;	// don't monitor the monitor
}



/********************************************************************************/
/*										*/
/*	Parameter methods							*/
/*										*/
/********************************************************************************/

public void setDetailing(String nm,boolean fg)
{
   setMonitorType(nm,fg);
}


public boolean setParameter(String s,String v)
{
   if (s.equals("ALLOC_MONITOR")) {
      setMonitorType(v,true);
      return true;
    }
   else if (s.equals("ALLOC_UNMONITOR")) {
      setMonitorType(v,false);
      return true;
    }

   return super.setParameter(s,v);
}



private void setMonitorType(String typ,boolean fg)
{
   int val = MONITOR_COUNT;
   if (typ.contains("TYPE")) val = MONITOR_TYPE;
   else if (typ.contains("SOURCE")) val = MONITOR_SOURCE;
   
   System.err.println("DYPER: SET MONITOR TYPE " + typ + " " + fg + " " + val);

   if (val == MONITOR_COUNT) {
      long now = System.currentTimeMillis();
      if (fg && count_start == 0) count_start = now;
      else if (!fg && count_start > 0) {
	 total_count += now-count_start;
	 count_start = 0;
       }
    }

   if (!fg) monitor_type = MONITOR_COUNT;
   else monitor_type = val;
}



public String getParameter(String s)
{
   if (s.equals("ALLOC_MONITOR")) {
      if (monitor_type == 0) return "COUNT";
      else if (monitor_type == 1) return "TYPE";
      else if (monitor_type == 2) return "SOURCE";
    }

   return super.getParameter(s);
}




/********************************************************************************/
/*										*/
/*	Reporting methods							*/
/*										*/
/********************************************************************************/

public void generateReport(ReportType rt,DyperXmlWriter xw,long now)
{
   xw.begin("MEMORY");
   xw.field("START",start_time);
   xw.field("MONTIME",getMonitoredTime(now));
   xw.field("LAST",last_check);

   double tim = total_count;
   if (count_start > 0) tim += now - count_start;
   if (tim > 0) {
      xw.field("COUNTTIME",tim);
      xw.field("ALLOCS",alloc_count);
    }

   heap_usage.output(xw);
   nonheap_usage.output(xw);

   for (MemStats ms : pool_usage.values()) {
      ms.output(xw);
    }
   for (GCStats gs : gc_usage.values()) {
      gs.output(xw);
    }

   Collection<ItemCount> ccc;
   Collection<ItemCount> tcc;
   synchronized (this) {
      ccc = new ArrayList<ItemCount>(class_counts.values());
      tcc = new ArrayList<ItemCount>(thread_counts.values());
    }

   for (ItemCount cc : ccc) {
      cc.output(xw,"CLASS");
    }

   for (ItemCount cc : tcc) {
      cc.output(xw,"THREAD");
    }

   xw.end("MEMORY");
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

public void handleThreadStack(long now,ThreadInfo ti,StackTraceElement [] se)
{
   if (last_check != now) {
      long interval = now - last_check;
      last_check = now;
      getUsage(interval);
    }

   for (int i = 0; i < se.length; ++i) {
      if (se[i].getMethodName().equals("<init>")) {
	 String cls = se[i].getClassName();
	 for (int j = i+1; j < se.length; ++j) {
	    String nm = se[j].getClassName();
	    if (!the_control.isSystemClass(nm)) {
	       countStack(cls,se[j]);
	       break;
	     }
	    else {
	       String mnm = se[j].getMethodName();
	       if (mnm.equals("<init>")) break;         // guess subclass
	     }
	  }
       }
    }
}



private void countStack(String cls,StackTraceElement src)
{
   ItemCount cc = class_counts.get(cls);
   if (cc == null) {
      synchronized (this) {
	 cc = class_counts.get(cls);
	 if (cc == null) {
	    cc = new ItemCount(cls);
	    class_counts.put(cls,cc);
	  }
       }
      cc.countStack(src);
    }
}




private void getUsage(long interval)
{
   heap_usage.addUse(memory_bean.getHeapMemoryUsage());
   nonheap_usage.addUse(memory_bean.getNonHeapMemoryUsage());

   for (MemoryPoolMXBean pb : ManagementFactory.getMemoryPoolMXBeans()) {
      String nm = pb.getName();
      MemStats stats = pool_usage.get(nm);
      if (stats == null) {
	 stats = new MemStats("POOL_" + pb.getName());
	 pool_usage.put(nm,stats);
	 // System.err.println("DYPER: NEW memory pool " + nm);
       }
      stats.addUse(pb.getUsage());
    }

   for (GarbageCollectorMXBean gb : ManagementFactory.getGarbageCollectorMXBeans()) {
      String nm = gb.getName();
      GCStats stats = gc_usage.get(nm);
      if (stats == null) {
	 stats = new GCStats(nm);
	 gc_usage.put(nm,stats);
	 // System.err.println("DYPER: NEW GC pool " + nm);
       }
      stats.addUse(gb.getCollectionCount(),gb.getCollectionTime(),interval);
    }
}





public void handleClear(long now)
{
   last_check = now;

   heap_usage.clear();
   nonheap_usage.clear();
   for (MemStats ms : pool_usage.values()) ms.clear();
   for (GCStats gs : gc_usage.values()) gs.clear();

   synchronized (this) {
      class_counts = new HashMap<String,ItemCount>();
      thread_counts = new HashMap<Thread,ItemCount>();
    }
}



/********************************************************************************/
/*										*/
/*	Class to hold memory usage accumulation 				*/
/*										*/
/********************************************************************************/

private class MemStats {

   private String stats_name;
   private long init_value;
   private double used_total;
   private double used_total2;
   private double comm_total;
   private double comm_total2;
   private long num_mods;
   private long max_value;
   private double last_used;
   private double last_comm;

   MemStats(String nm) {
      stats_name = nm;
      init_value = 0;
      used_total = 0;
      used_total2 = 0;
      comm_total = 0;
      comm_total2 = 0;
      num_mods = 0;
      max_value = 0;
    }

   void addUse(MemoryUsage mu) {
      if (num_mods++ == 0) init_value = mu.getInit();
      long m = mu.getMax();
      if (m > max_value) max_value = m;
      double v = mu.getUsed();
      last_used = v;
      used_total += v;
      used_total2 += v*v;
      v = mu.getCommitted();
      last_comm = v;
      comm_total += v;
      comm_total2 += v*v;
    }

   void output(DyperXmlWriter xw) {
      xw.begin("USAGE");
      xw.field("NAME",stats_name);
      xw.field("INIT",init_value);
      xw.field("MAX",max_value);
      xw.field("USED",used_total/num_mods);
      xw.field("USED2",used_total2/num_mods);
      xw.field("COMM",comm_total/num_mods);
      xw.field("COMM2",comm_total2/num_mods);
      xw.field("MODS",num_mods);
      xw.field("LUSED",last_used);
      xw.field("LCOMM",last_comm);
      xw.end();
    }

   void clear() {
      init_value = 0;
      used_total = 0;
      used_total2 = 0;
      comm_total = 0;
      comm_total2 = 0;
      num_mods = 0;
      max_value = 0;
    }

}	// end of subclass MemStats




private class GCStats {

   private String gc_name;
   private long gc_count;
   private long gc_time;
   private long base_count;
   private long base_time;

   GCStats(String name) {
      gc_name = name;
      gc_count = 0;
      gc_time = 0;
      base_count = -1;
      base_time = 0;
    }

   void addUse(long ct,long time,long interval) {
      if (base_count < 0) {
	 base_count = gc_count;
	 base_time = gc_time;
       }
      gc_count = ct;
      gc_time = time;
    }

   void output(DyperXmlWriter xw) {
      xw.begin("GC");
      xw.field("NAME",gc_name);
      xw.field("COUNT",gc_count-base_count);
      xw.field("TIME",gc_time-base_time);
      xw.end();
    }

   void clear() {
      base_count = -1;
      base_time = 0;
      gc_count = 0;
      gc_time = 0;
    }

}	// end of subclass GCStats




/********************************************************************************/
/*										*/
/*	Run time entries							*/
/*										*/
/********************************************************************************/

public static void monObjectNew2(Object o)
{
   Thread t = Thread.currentThread();
   int tid = (int) t.getId();
   if (in_new[tid] || !mem_agent.the_control.useThread(tid)) return;
   in_new[tid] = true;

   StackTraceElement [] se = t.getStackTrace();
   StackTraceElement src = null;
   String cls = null;
   for (int i = 3; i < se.length; ++i) {
      if (se[i].getMethodName().equals("<init>")) {
	 cls = se[i].getClassName();
       }
      else {
	 if (cls == null) continue;
	 while (i < se.length) {
	    String nm = se[i].getClassName();
	    if (!mem_agent.the_control.checkSystemClass(nm)) break;
	    ++i;
	  }
	 if (i < se.length) src = se[i];
	 break;
       }
    }

   if (cls != null) {
      if (src == null) src = se[se.length-1];
      ItemCount cc = mem_agent.class_counts.get(cls);
      if (cc == null) {
	 synchronized (mem_agent) {
	    cc = mem_agent.class_counts.get(cls);
	    if (cc == null) {
	       cc = new ItemCount(cls);
	       mem_agent.class_counts.put(cls,cc);
	     }
	  }
       }
      cc.count(src);
    }

   ItemCount tc = mem_agent.thread_counts.get(t);
   if (tc == null) {
      synchronized(mem_agent) {
	 tc = mem_agent.thread_counts.get(t);
	 if (tc == null) {
	    tc = new ItemCount(t.getName());
	    mem_agent.thread_counts.put(t,tc);
	  }
       }
    }
   tc.count();

   in_new[tid] = false;
}



public static void monObjectNew1(Object o)
{
   Thread t = Thread.currentThread();
   int tid = (int) t.getId();
   if (in_new[tid] || !mem_agent.the_control.useThread(tid)) return;
   in_new[tid] = true;

   String cls = o.getClass().getName();

   ItemCount cc = mem_agent.class_counts.get(cls);
   if (cc == null) {
      synchronized (mem_agent) {
	 cc = mem_agent.class_counts.get(cls);
	 if (cc == null) {
	    cc = new ItemCount(cls);
	    mem_agent.class_counts.put(cls,cc);
	  }
       }
    }
   cc.count();

   ItemCount tc = mem_agent.thread_counts.get(t);
   if (tc == null) {
      synchronized(mem_agent) {
	 tc = mem_agent.thread_counts.get(t);
	 if (tc == null) {
	    tc = new ItemCount(t.getName());
	    mem_agent.thread_counts.put(t,tc);
	  }
       }
    }
   tc.count();

   in_new[tid] = false;
}



public static void monObjectNew(Object o)
{
   ++alloc_count;
   if (monitor_type == MONITOR_COUNT) return;
   if (monitor_type == MONITOR_TYPE) monObjectNew1(o);
   else monObjectNew2(o);
}



/********************************************************************************/
/*										*/
/*	Class to hold information about an allocated class			*/
/*										*/
/********************************************************************************/

private static class ItemCount {

   private String class_name;
   private long item_count;
   private StackTraceElement [] source_items;
   private long [] source_counts;
   private long [] source_hits;
   private long num_hits;
   private long num_counts;

   ItemCount(String name) {
      class_name = name;
      item_count = 0;
      source_items = new StackTraceElement[DYPER_MAX_SOURCES];
      source_counts = new long[DYPER_MAX_SOURCES];
      source_hits = new long[DYPER_MAX_SOURCES];
      num_hits = 0;
      num_counts = 0;
    }

   long count() {
      return ++item_count;
    }

   void count(StackTraceElement e) {
      ++item_count;
      ++num_counts;
      for (int i = 0; i < DYPER_MAX_SOURCES; ++i) {
	 if (source_items[i] == null) {
	    source_items[i] = e;
	    source_counts[i] = 1;
	    break;
	  }
	 else if (e.equals(source_items[i])) {
	    source_counts[i]++;
	    break;
	  }
       }
    }

   void countStack(StackTraceElement e) {
      ++num_hits;
      for (int i = 0; i < DYPER_MAX_SOURCES; ++i) {
	 if (source_items[i] == null) {
	    source_items[i] = e;
	    source_hits[i] = 1;
	    break;
	  }
	 else if (e.equals(source_items[i])) {
	    source_hits[i]++;
	    break;
	  }
       }
    }

   void output(DyperXmlWriter xw,String id) {
      if (item_count == 0) return;
      xw.begin("ALLOC");
      xw.field(id,class_name);
      xw.field("COUNT",item_count);
      if (num_counts > 0) xw.field("NCOUNT",num_counts);
      for (int i = 0; i < DYPER_MAX_SOURCES && source_items[i] != null; ++i) {
         long ct = 0;
         if (num_counts > 0) ct = source_counts[i] * item_count / num_counts;
         else ct = source_hits[i]*item_count/num_hits;
         if (ct == 0) continue;
         xw.begin("SOURCE");
         xw.field("FILE",source_items[i].getFileName());
         xw.field("CLASS",source_items[i].getClassName());
         xw.field("METHOD",source_items[i].getMethodName());
         xw.field("LINE",source_items[i].getLineNumber());
         xw.field("COUNT",ct);
         xw.end("SOURCE");
       }
      xw.end("ALLOC");
    }

}	// end of subclass ItemCount


}	// end of class DyperAgentMemory



/* end of DyperAgentMemory.java */
