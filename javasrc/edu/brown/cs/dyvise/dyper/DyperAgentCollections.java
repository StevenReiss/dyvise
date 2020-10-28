/********************************************************************************/
/*										*/
/*		DyperAgentCollections.java					*/
/*										*/
/*	Monitor agent that does Collections analysis				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyper/DyperAgentCollections.java,v 1.3 2016/11/02 18:59:18 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyperAgentCollections.java,v $
 * Revision 1.3  2016/11/02 18:59:18  spr
 * Move to asm5
 *
 * Revision 1.2  2009-09-19 00:13:27  spr
 * Update agents for dynamic insertion and removal.
 *
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/


package edu.brown.cs.dyvise.dyper;


import java.lang.management.ThreadInfo;
import java.util.*;




public class DyperAgentCollections extends DyperAgent {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private long	last_sample;
private long	sample_count;
private Map<String,CollectionCount> count_map;
private long	total_count;
private Map<Object,CollectionItem> item_map;



private static boolean [] in_mon = new boolean[DYPER_MAX_THREADS];
private static DyperAgentCollections coll_agent = null;

private static final int     OP_GET = 0;
private static final int     OP_ADD = 1;
private static final int     OP_REMOVE = 2;
private static final int     OP_ITERATE = 3;
private static final int     NUM_OPS = 4;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public DyperAgentCollections(DyperControl dc)
{
   super(dc,"COLLECTIONS");

   coll_agent = this;

   last_sample = 0;
   sample_count = 0;
   count_map = new HashMap<String,CollectionCount>();
   total_count = 0;
   item_map = new IdentityHashMap<Object,CollectionItem>();

   long mid = dc.getMonitorThreadId();
   in_mon[(int)mid] = true;
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
      last_sample = now;
    }

   StackTraceElement rpt = null;

   for (int i = 0; i < trc.length; ++i) {
      StackTraceElement te = trc[i];
      String nm = te.getClassName();
      if (the_control.isCollectionsClass(nm)) rpt = te;
      else break;
    }

   if (rpt != null) {
      ++total_count;
      String km = rpt.getClassName() + "@" + rpt.getMethodName();
      CollectionCount ic = count_map.get(km);
      if (ic == null) {
	 ic = new CollectionCount(km);
	 count_map.put(km,ic);
       }
      ic.accumulate(ti);
    }
}




/********************************************************************************/
/*										*/
/*	Clear methods								*/
/*										*/
/********************************************************************************/

public void handleClear(long now)
{
   last_sample = 0;
   sample_count = 0;
   count_map = new HashMap<String,CollectionCount>();
   total_count = 0;
}




/********************************************************************************/
/*										*/
/*	Reporting methods							*/
/*										*/
/********************************************************************************/

public void generateReport(ReportType rt,DyperXmlWriter xw,long now)
{
   xw.begin("COLLECTIONS");
   xw.field("MONTIME",getMonitoredTime(now));
   xw.field("SAMPLES",sample_count);
   xw.field("LAST",last_sample);
   xw.field("TOTAL",total_count);

   for (CollectionCount ic : count_map.values()) {
      ic.report(xw);
    }

   for (CollectionItem ci : item_map.values()) {
      ci.report(xw);
    }
 
   xw.end("COLLECTIONS");
}



/********************************************************************************/
/*										*/
/*	Run time entries							*/
/*										*/
/********************************************************************************/

public static void monCollectionOp(Object c,int op)
{
   Thread t = Thread.currentThread();
   int tid = (int) t.getId();
   if (in_mon[tid] || !coll_agent.the_control.useThread(tid)) return;
   in_mon[tid] = true;

   CollectionItem ci = coll_agent.item_map.get(c);
   if (ci == null) {
      synchronized (coll_agent) {
	 ci = coll_agent.item_map.get(c);
	 if (ci == null) {
	    ci = new CollectionItem(c);
	    coll_agent.item_map.put(c,ci);
	  }
       }
    }
   ci.record(op);

   in_mon[tid] = false;
}




/********************************************************************************/
/*										*/
/*	Class to hold counter information					*/
/*										*/
/********************************************************************************/

private static class CollectionCount {

   private String source_item;
   private long total_count;
   private Map<String,long []> thread_counts;

   CollectionCount(String itm) {
      source_item = itm;
      total_count = 0;
      thread_counts = new HashMap<String,long[]>();
    }

   void accumulate(ThreadInfo ti) {
      ++total_count;
      String tnm = ti.getThreadName();
      long [] val = thread_counts.get(tnm);
      if (val == null) {
	 val = new long[1];
	 thread_counts.put(tnm,val);
       }
      val[0]++;
    }

   void report(DyperXmlWriter xw) {
      xw.begin("ITEM");
      xw.field("NAME",source_item);
      xw.field("COUNT",total_count);
      for (Map.Entry<String,long[]> ent : thread_counts.entrySet()) {
         xw.begin("THREAD");
         xw.field("NAME",ent.getKey());
         xw.field("COUNT",ent.getValue()[0]);
         xw.end("THREAD");
       }
      xw.end("ITEM");
    }

}	// end of subclass CollectionCount



/********************************************************************************/
/*										*/
/*	CollectionItem -- information about a particular collection		*/
/*										*/
/********************************************************************************/

private static class CollectionItem {

   private Map<?,?> collection_map;
   private Collection<?> collection_object;
   private boolean is_map;
   private int [] op_counts;
   private int min_size;
   private int max_size;

   CollectionItem(Object o) {
      is_map = (o instanceof Map<?,?>);
      if (is_map) collection_map = (Map<?,?>) o;
      else collection_object = (Collection<?>) o;
      op_counts = new int[NUM_OPS];
      min_size = -1;
      max_size = -1;
    }

   void record(int op) {
      op_counts[op]++;
      int sz = 0;
      if (is_map) sz = collection_map.size();
      else sz = collection_object.size();
      if (sz < min_size || min_size < 0) min_size = sz;
      if (sz > max_size) max_size = sz;
    }

   void report(DyperXmlWriter xw) {
      xw.begin("OBJECT");
      xw.field("MAP",is_map);
      if (is_map) {
         xw.field("KEY",System.identityHashCode(collection_map));
       }
      else {
         xw.field("KEY",System.identityHashCode(collection_object));
       }
      xw.field("MIN",min_size);
      xw.field("MAX",max_size);
      xw.field("GETS",op_counts[OP_GET]);
      xw.field("ADDS",op_counts[OP_ADD]);
      xw.field("REMOVES",op_counts[OP_REMOVE]);
      xw.field("ITERATES",op_counts[OP_ITERATE]);
      xw.end("OBJECT");
    }

}	// end of subclass CollectionItem




}	// end of class DyperAgentCollections




/* end of DyperAgentCollections.java */

