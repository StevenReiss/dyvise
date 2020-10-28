/********************************************************************************/
/*										*/
/*		DyperAgentThreads.java						*/
/*										*/
/*	Monitor agent that does lock checking					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyper/DyperAgentThreads.java,v 1.4 2016/11/02 18:59:19 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyperAgentThreads.java,v $
 * Revision 1.4  2016/11/02 18:59:19  spr
 * Move to asm5
 *
 * Revision 1.3  2009-09-19 00:13:28  spr
 * Update agents for dynamic insertion and removal.
 *
 * Revision 1.2  2009-06-04 18:54:43  spr
 * Better information handling for threads.
 *
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/


package edu.brown.cs.dyvise.dyper;


import java.lang.management.ThreadInfo;
import java.util.*;




class DyperAgentThreads extends DyperAgent {


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private Map<Long,LockInfo> lock_data;
private Map<Long,ThreadData>   class_data;
private long		 contention_start;
private long		last_check;
private long		check_counter;
private long		min_check;
private long		prev_check;

private static final long DEAD_TIME = 120000l;

private static Set<String> ignore_classes;


static {
   ignore_classes = new HashSet<String>();
   ignore_classes.add("java.lang.Thread");
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public DyperAgentThreads(DyperControl dc)
{
   super(dc,"THREADS");

   lock_data = new HashMap<Long,LockInfo>();
   class_data = new HashMap<Long,ThreadData>();
   last_check = 0;
   contention_start = 0;
   check_counter = 0;
   min_check = 0;
   prev_check = 0;
}




/********************************************************************************/
/*										*/
/*	Reporting methods							*/
/*										*/
/********************************************************************************/

public void generateReport(ReportType rt,DyperXmlWriter xw,long now)
{
   min_check = prev_check;
   prev_check = check_counter;

   xw.begin("THREADS");
   for (Iterator<ThreadData> it = class_data.values().iterator(); it.hasNext(); ) {
      ThreadData td = it.next();
      td.report(xw);
      if (td.isStale(last_check)) it.remove();
    }

   for (Iterator<LockInfo> it = lock_data.values().iterator(); it.hasNext(); ) {
      LockInfo li = it.next();
      li.report(xw);
      if (li.isStale()) it.remove();
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
      last_check = now;
      ++check_counter;
    }

   long tid = ti.getThreadId();

   ThreadData td = class_data.get(tid);
   if (td == null) {
      td = new ThreadData(now,tid,trc);
      class_data.put(tid,td);
    }
   else td.record(now);

   LockInfo li = lock_data.get(tid);

   switch (ti.getThreadState()) {
      case RUNNABLE :
      case BLOCKED :
	 if (li == null) {
	    li = new LockInfo(tid,ti.getThreadName(),now);
	    lock_data.put(tid,li);
	  }
	 li.record(now,ti,trc);
	 break;
      case WAITING :
      case TIMED_WAITING :
	 if (li != null) li.record(now,ti,trc);
	 break;
      default :
      case NEW :
      case TERMINATED :
	 break;
    }
}



public void handleContentionMonitoring(boolean start,long now)
{
   if (start) contention_start = now;

   for (LockInfo li : lock_data.values()) {
      li.handleContentionMonitoring(start,now);
    }

   if (!start) contention_start = 0;
}



public void handleClear(long now)
{
   // contention monitoring has been restart
   lock_data = new HashMap<Long,LockInfo>();
   class_data = new HashMap<Long,ThreadData>();
   last_check = 0;
   if (contention_start != 0) contention_start = now;
}



/********************************************************************************/
/*										*/
/*	Methods to identify base class associated with the thread		*/
/*										*/
/********************************************************************************/

@SuppressWarnings("unused")
private String getThreadClass(StackTraceElement [] trc)
{
   for (int i = trc.length-1; i >= 0; --i) {
      StackTraceElement te = trc[i];
      String nm = te.getClassName();
      if (!ignore_classes.contains(nm)) return nm;
    }

   return "*UNKNOWN*";
}



/********************************************************************************/
/*										*/
/*	LockInfo class								*/
/*										*/
/********************************************************************************/

private class LockInfo {

   private long 	thread_id;
   private String	thread_name;
   private long 	start_time;
   private long 	last_time;
   private long 	num_blocks;
   private long 	num_waits;
   private long 	num_runs;
   private long 	num_ios;
   private long 	num_sleeps;
   private long 	tot_blocks;
   private long 	tot_waits;
   private long 	wait_time;
   private long 	wait_saved;
   private long 	block_time;
   private long 	block_saved;
   private long 	contention_time;
   private Map<LockInfo,long []> lock_ons;
   private long 	tot_samples;
   private long 	last_count;
   private boolean	reported_dead;

   LockInfo(long tid,String nm,long now) {
      thread_id = tid;
      thread_name = nm;
      start_time = now;
      last_time = now;
      num_blocks = 0;
      num_runs = 0;
      num_waits = 0;
      num_ios = 0;
      num_sleeps = 0;
      tot_blocks = 0;
      tot_waits = 0;
      wait_time = 0;
      block_time = 0;
      lock_ons = new HashMap<LockInfo,long []>();
      last_count = 0;
      reported_dead = false;
      contention_time = 0;
    }

   void record(long now,ThreadInfo ti,StackTraceElement [] st) {
      last_time = now;
      last_count = check_counter;
      Thread.State ts = ti.getThreadState();
      reported_dead = false;

      tot_samples++;
      if (st.length == 0) return;

      if (ts == Thread.State.BLOCKED) {
	 if (st[0].getMethodName().equals("waitForEvents") &&
		st[0].getClassName().equals("sun.awt.X11.XToolkit")) ++num_ios;
	 else ++num_blocks;
       }
      else if (ts == Thread.State.RUNNABLE) {
	 if (the_control.isIOClass(st[0].getClassName())) ++num_ios;
	 else if (st[0].getMethodName().equals("sleep") &&
		     st[0].getClassName().equals("java.lang.Thread")) ++num_sleeps;
	 else if (st[0].getMethodName().equals("wait") &&
		     st[0].getClassName().equals("java.lang.Object")) ++num_waits;
	 else if (st[0].getMethodName().equals("waitForEvents") &&
		st[0].getClassName().equals("sun.awt.X11.XToolkit")) ++num_ios;
	 else ++num_runs;
       }
      else if (ts == Thread.State.WAITING) ++num_waits;
      else if (ts == Thread.State.TIMED_WAITING) ++num_waits;

      tot_blocks = ti.getBlockedCount();
      tot_waits = ti.getWaitedCount();
      if (contention_start > 0) {
	 wait_time = ti.getWaitedTime();
	 block_time = ti.getBlockedTime();
       }

      if (ts == Thread.State.BLOCKED) {
	 long bid = ti.getLockOwnerId();
	 if (bid >= 0) {
	    LockInfo bli = lock_data.get(bid);
	    if (bli != null) {
	       long [] ctr = lock_ons.get(bli);
	       if (ctr == null) {
		  ctr = new long[1];
		  lock_ons.put(bli,ctr);
		}
	       ctr[0]++;
	     }
	  }
       }
    }

   void handleContentionMonitoring(boolean start,long now) {
      if (!start) {
	 wait_saved += wait_time;
	 wait_time = 0;
	 block_saved = block_time;
	 block_time = 0;
	 if (last_time > contention_start && now >= start_time) {
	    long cstart = contention_start;
	    if (start_time > cstart) cstart = start_time;
	    contention_time += now - cstart;
	  }
       }
    }

   long getId() 					{ return thread_id; }
   String getName()					{ return thread_name; }
   @SuppressWarnings("unused")
   double getBlocks()					{ return num_blocks; }
   @SuppressWarnings("unused")
   double getRuns()					{ return num_runs; }
   long getStart()					{ return start_time; }

   void report(DyperXmlWriter xw) {
      if (thread_name.equals("Keep-Alive-Timer")) return;
      if (tot_samples == 0) return;
      if (reported_dead) return;
   
      xw.begin("THREAD");
      xw.field("ID",thread_id);
      xw.field("THREAD",thread_name);
      xw.field("START",start_time);
      if (last_time != last_check) xw.field("END",last_time);
      else xw.field("LAST",last_time);
      xw.field("TOTAL",tot_samples);
      xw.field("BLOCKS",num_blocks);
      xw.field("RUNS",num_runs);
      xw.field("WAITS",num_waits);
      xw.field("IOS",num_ios);
      xw.field("SLEEPS",num_sleeps);
      xw.field("TOTBLKS",tot_blocks);
      xw.field("TOTWAITS",tot_waits);
      if (last_count > 0 && last_count < min_check) {
         xw.field("TERMINATED",true);
         reported_dead = true;
       }
   
      if (wait_time + block_time + wait_saved + block_saved != 0) {
         long chktime = contention_time;
         if (last_time > contention_start && last_time > start_time && contention_start > 0) {
            long ctime = contention_start;
            if (start_time > contention_start) ctime = start_time;
            chktime += last_time - ctime;
          }
         xw.field("WTIME",wait_time + wait_saved);
         xw.field("BTIME",block_time + block_saved);
         xw.field("CTIME",chktime);
         if (chktime < 0) {
            xw.field("CTIME1",contention_time);
            xw.field("CTIME2",contention_start);
            xw.field("CTIME3",start_time);
            xw.field("CTIME4",last_time);
          }
       }
      for (Map.Entry<LockInfo,long []> ent : lock_ons.entrySet()) {
         xw.begin("LOCKON");
         xw.field("ID",ent.getKey().getId());
         xw.field("THREAD",ent.getKey().getName());
         xw.field("START",ent.getKey().getStart());     // need in case of creation
         xw.field("COUNT",ent.getValue()[0]);
         xw.end();
       }
      xw.end("THREAD");
    }

   boolean isStale() {
      if (last_check - last_time > DEAD_TIME) return true;
      return false;
    }

}	// end of subclass LockInfo




/********************************************************************************/
/*										*/
/*	Class for generic data about a thread					*/
/*										*/
/********************************************************************************/

private static class ThreadData {

   private long thread_id;
   private String base_class;
   private boolean is_reported;
   private long last_time;

   ThreadData(long now,long tid,StackTraceElement [] trc) {
      thread_id = tid;
      last_time = now;
      is_reported = false;
      base_class = "*UNKNOWN*";
      for (int i = trc.length-1; i >= 0; --i) {
	 StackTraceElement te = trc[i];
	 String nm = te.getClassName();
	 if (!ignore_classes.contains(nm)) {
	    base_class = nm;
	    break;
	  }
       }
    }

   void record(long now) {
      last_time = now;
    }

   void report(DyperXmlWriter xw) {
      if (is_reported) return;
      is_reported = true;
      xw.begin("THREADDATA");
      xw.field("TID",thread_id);
      xw.field("CLASS",base_class);
      xw.end("THREADDATA");
    }

   boolean isStale(long chk) {
      if (chk - last_time > DEAD_TIME) return true;
      return false;
    }



}	// end of subclass ThreadData




}	// end of class DyperAgentThreads




/* end of DyperAgentThreads.java */

