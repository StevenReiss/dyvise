/********************************************************************************/
/*										*/
/*		DyperAgentIO.java						*/
/*										*/
/*	Monitor agent that does I/O analysis					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyper/DyperAgentIO.java,v 1.4 2016/11/02 18:59:18 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyperAgentIO.java,v $
 * Revision 1.4  2016/11/02 18:59:18  spr
 * Move to asm5
 *
 * Revision 1.3  2012-10-05 00:53:01  spr
 * Code clean up.
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




public class DyperAgentIO extends DyperAgent {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private long	last_sample;
private long	sample_count;
private Map<String,IoCount> count_map;
private long	total_count;

private static DyperAgentIO io_agent = null;

private static long [] method_counts;
private static int [] method_ids;
private static int num_counts;

static {
   method_counts = new long[1024];
   method_ids = new int[1024];
   num_counts = 0;
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public DyperAgentIO(DyperControl dc)
{
   super(dc,"IO");

   io_agent = this;

   last_sample = 0;
   sample_count = 0;
   count_map = new HashMap<String,IoCount>();
   total_count = 0;
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
   StackTraceElement usr = null;

   for (int j = 0; j < trc.length; ++j) {
      StackTraceElement te = trc[j];
      String nm = te.getClassName();
      if (j == 0 && !the_control.isIOClass(nm)) return;
      if (the_control.isSystemClass(nm) && the_control.isIOClass(nm)) rpt = te;
      else {
	 usr = te;
	 break;
       }
    }

   if (rpt == null) return;

   ++total_count;

   String nm = rpt.getClassName() + "@" + rpt.getMethodName();
   IoCount ic = count_map.get(nm);
   if (ic == null) {
      ic = new IoCount(nm);
      count_map.put(nm,ic);
    }
   ic.accumulate(ti,usr);
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
   count_map = new HashMap<String,IoCount>();
   total_count = 0;

   synchronized (DyperAgentIO.class) {
      method_counts = new long[1024];
      method_ids = new int[1024];
      num_counts = 0;
    }
}



/********************************************************************************/
/*										*/
/*	Reporting methods							*/
/*										*/
/********************************************************************************/

public void generateReport(ReportType rt,DyperXmlWriter xw,long now)
{
   xw.begin("IOCOUNTERS");
   xw.field("MONTIME",getMonitoredTime(now));
   xw.field("SAMPLES",sample_count);
   xw.field("LAST",last_sample);
   xw.field("TOTAL",total_count);

   for (IoCount ic : count_map.values()) {
      ic.report(xw);
    }

   for (int i = 0; i < num_counts; ++i) {
      xw.begin("RTIO");
      xw.field("COUNTER",method_ids[i]);
      xw.field("COUNT",method_counts[i]);
      xw.end();
    }

   xw.end("IOCOUNTERS");
}



/********************************************************************************/
/*										*/
/*	Class to hold counter information					*/
/*										*/
/********************************************************************************/

private static class IoCount {

   private String source_item;
   private long total_count;
   private Map<String,long []> thread_counts;
   private StackTraceElement [] source_items;
   private long [] source_counts;
   private boolean has_changed;

   IoCount(String itm) {
      source_item = itm;
      total_count = 0;
      thread_counts = new HashMap<String,long[]>();
      source_items = new StackTraceElement[DYPER_MAX_SOURCES];
      source_counts = new long[DYPER_MAX_SOURCES];
    }

   void accumulate(ThreadInfo ti,StackTraceElement usr) {
      has_changed = true;
      ++total_count;
      String tnm = ti.getThreadName();
      long [] val = thread_counts.get(tnm);
      if (val == null) {
	 val = new long[2];
	 thread_counts.put(tnm,val);
       }
      val[0]++;
      val[1] = ti.getThreadId();
      if (usr != null) {
	 for (int i = 0; i < DYPER_MAX_SOURCES; ++i) {
	    if (source_items[i] == null) {
	       source_items[i] = usr;
	       source_counts[i] = 1;
	       break;
	     }
	    else if (usr.equals(source_items[i])) {
	       source_counts[i]++;
	       break;
	     }
	  }
       }
    }

   void report(DyperXmlWriter xw) {
      if (!has_changed) return;
      has_changed = false;
   
      xw.begin("ITEM");
      xw.field("NAME",source_item);
      xw.field("COUNT",total_count);
      for (Map.Entry<String,long[]> ent : thread_counts.entrySet()) {
         long[] v = ent.getValue();
         if (v[1] != 0) {
            xw.begin("THREAD");
            xw.field("NAME",ent.getKey());
            xw.field("ID",v[1]);
            xw.field("COUNT",v[0]);
            xw.end("THREAD");
            v[1] = 0;				// reset on next report
          }
       }
      Map<String,long []> classcounts = new HashMap<String,long []>();
      for (int i = 0; i < DYPER_MAX_SOURCES && source_items[i] != null; ++i) {
         String cnm = source_items[i].getClassName();
         long [] cct = classcounts.get(cnm);
         if (cct == null) {
            cct = new long[1];
            classcounts.put(cnm,cct);
          }
         cct[0] += source_counts[i];
         xw.begin("SOURCE");
         xw.field("FILE",source_items[i].getFileName());
         xw.field("CLASS",cnm);
         xw.field("METHOD",source_items[i].getMethodName());
         xw.field("LINE",source_items[i].getLineNumber());
         xw.field("COUNT",source_counts[i]);
         xw.end("SOURCE");
       }
      for (Map.Entry<String,long []> ent : classcounts.entrySet()) {
         xw.begin("CSOURCE");
         xw.field("CLASS",ent.getKey());
         xw.field("TOTAL",ent.getValue()[0]);
         xw.end("CSOURCE");
       }
      xw.end("ITEM");
    }

}	// end of subclass IoCount




/********************************************************************************/
/*										*/
/*	Entries for I/O counting						*/
/*										*/
/********************************************************************************/

public static void monMethodEnter(int no)
{
   increment(no);
}




private static void increment(int itm)
{
   if (!io_agent.the_control.useThread()) return;

   int n = num_counts;

   for (int i = 0; i < n; ++i) {
      if (method_ids[i] == itm) {
	 method_counts[i]++;
	 return;
       }
    }

   synchronized (DyperAgentIO.class) {
      for (int i = n; i < num_counts; ++i) {
	 if (method_ids[i] == itm) {
	    method_counts[i]++;
	    return;
	  }
       }
      method_ids[num_counts] = itm;
      method_counts[num_counts] = 1;
      ++num_counts;
    }
}




}	// end of class DyperAgentIO




/* end of DyperAgentIO.java */
