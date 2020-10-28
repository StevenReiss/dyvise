/********************************************************************************/
/*										*/
/*		DymonAgentThreads.java						*/
/*										*/
/*	DYPER monitor agent for lock checking					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymon/DymonAgentThreads.java,v 1.9 2012-10-05 00:52:56 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymonAgentThreads.java,v $
 * Revision 1.9  2012-10-05 00:52:56  spr
 * Code clean up.
 *
 * Revision 1.8  2010-03-30 16:22:05  spr
 * Clean up the code.
 *
 * Revision 1.7  2009-09-19 00:09:46  spr
 * Update dymon for seletive agent insertion and removal.
 *
 * Revision 1.6  2009-06-04 18:53:50  spr
 * Set up for binary distribution.
 *
 * Revision 1.5  2009-04-11 23:46:18  spr
 * Use IvyFormat rather than local code.
 *
 * Revision 1.4  2009-03-20 02:06:50  spr
 * Update dymon: add time-based information, updated memory, etc.
 *
 * Revision 1.3  2008-12-04 01:11:00  spr
 * Update output and fix phaser summary.
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


import edu.brown.cs.ivy.file.IvyFormat;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.util.*;



class DymonAgentThreads extends DymonAgent {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<String,ThreadItem> thread_map;
private PatchRequest patch_request;

private DymonDetailing thread_detailing;

private boolean 	check_always;

@SuppressWarnings("unused")
private long		threads_active;
@SuppressWarnings("unused")
private long		threads_created;
@SuppressWarnings("unused")
private long		threads_died;



private static final long	INSTRUMENT_TIME = 10000;
private static final long	OVERHEAD_FIXED_TIME = 10;
private static final double	OVERHEAD_SLOWDOWN = 0.05;

private static final int	MIN_SAMPLES = 500;
private static final double	MAX_BLOCKS = 0.20;

private static final long	DEAD_TIME = 60000l;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DymonAgentThreads(DymonMain dm,DymonProcess p)
{
   super(dm,p);

   for_process = p;
   thread_map = new TreeMap<String,ThreadItem>();
   patch_request = new PatchRequest();

   threads_active = 0;
   threads_created = 0;
   threads_died = 0;

   if (dm == null || p == null) check_always = false;
   else check_always = dm.getBooleanResource(p.getStartClass(),"CONTENTIONCHECK");

   thread_detailing = new Detailing();
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getName()			{ return "THREADS"; }


@Override public String getDyperAgentName()		{ return "THREADS"; }


@Override protected String getMonitorClass()
{
   return "edu.brown.cs.dyvise.dyper.DyperAgentThreads";
}



/********************************************************************************/
/*										*/
/*	Clear methods								*/
/*										*/
/********************************************************************************/

@Override public void doClear()
{
   thread_map.clear();

   clearRecentCounts();

   threads_active = 0;
   threads_created = 0;
   threads_died = 0;

   thread_detailing.doClear();
}




/********************************************************************************/
/*										*/
/*	Startup methods 							*/
/*										*/
/********************************************************************************/

@Override protected void handleStart(DymonProcess dp)
{
   if (check_always) {
      dp.setDyperVar("CONTENTION","TRUE");
    }
}



/********************************************************************************/
/*										*/
/*	Report handling methods 						*/
/*										*/
/********************************************************************************/

@Override public void handleReport(Element r)
{
   Element ce = IvyXml.getElementByTag(r,"THREADS");

   for (Element te : IvyXml.elementsByTag(ce,"THREAD")) {
      String tid = IvyXml.getAttrString(te,"ID");
      ThreadItem li = findThreadItem(tid,te);
      li.update(te,this);
    }

   long totblk = 1;
   long totsam = 0;
   synchronized(thread_map) {
      for (ThreadItem li : thread_map.values()) {
	 totblk += li.getNumBlocks();
	 totsam += li.getNumSamples();
       }
    }
   updateRecentCounts(totblk,totsam);

   Element te = IvyXml.getElementByTag(r,"TIMING");
   threads_active = IvyXml.getAttrLong(te,"ACTIVE");
   threads_created = IvyXml.getAttrLong(te,"CREATED");
   threads_died = IvyXml.getAttrLong(te,"DIED");
}




/********************************************************************************/
/*										*/
/*	Analysis methods							*/
/*										*/
/********************************************************************************/

@Override public void outputAnalysis(IvyXmlWriter xw)
{
   xw.begin("THREADS");

   xw.field("SUMMARY",Math.random());

   Collection<ThreadItem> allitms = new TreeSet<ThreadItem>(new LockComparator());

   long rpt = for_process.getReportTime();

   synchronized (thread_map) {
      for (Iterator<ThreadItem> it = thread_map.values().iterator(); it.hasNext(); ) {
	 ThreadItem li = it.next();
	 if (li.checkUse(rpt)) {
	    li.output(xw);
	    allitms.add(li);
	  }
	 if (li.checkRemove(rpt)) it.remove();
       }
    }

   xw.begin("LOCKMAT");
   for (ThreadItem li : allitms) {
      if (li.getHasLocks()) li.outputMatrix(xw,allitms);
    }
   xw.end("LOCKMAT");

   xw.end("THREADS");
}




/********************************************************************************/
/*										*/
/*	Summary methods 							*/
/*										*/
/********************************************************************************/

@Override public double getSummaryValue()		{ return getAgentPriority(true); }

@Override public double getConfidence()
{
   return 1;
}



@Override public void outputSummary(IvyXmlWriter xw)
{
   double totblk = 0;

   long rpt = for_process.getReportTime();

   synchronized (thread_map) {
      for (ThreadItem li : thread_map.values()) {
	 if (li.checkUse(rpt)) {
	    double bt = li.getBlockTime();
	    if (bt >= 0) {
	       totblk += bt;
	     }
	  }
       }
    }

   xw.begin("METER");
   xw.field("NAME","% BLOCK");
   xw.field("VALUE",getRecentRatio());
   xw.field("TYPE","PERCENT");
   xw.end();

   xw.begin("METER");
   xw.field("NAME","BLOCK TIME");
   xw.field("VALUE",totblk);
   xw.field("TYPE","TIME");
   xw.end();
}




/********************************************************************************/
/*										*/
/*	Query methods								*/
/*										*/
/********************************************************************************/

Map<String,Number> handleSimpleQuery(String id)
{
   if (id.equals("REPORTABLE")) {
      long rpt = for_process.getReportTime();
      Map<String,Number> rslt = new HashMap<String,Number>();
      synchronized (thread_map) {
	 for (ThreadItem li : thread_map.values()) {
	    if (li.checkUse(rpt)) {
	       rslt.put(li.getThreadName(),li.getThreadId());
	       rslt.put(Long.toString(li.getThreadId()),li.getThreadId());
	     }
	  }
       }
      return rslt;
    }

   return super.handleSimpleQuery(id);
}




/********************************************************************************/
/*										*/
/*	Method to find/create a holder for a thread				*/
/*										*/
/********************************************************************************/

private ThreadItem findThreadItem(String tid,Element e)
{
   ThreadItem li = null;

   synchronized (thread_map) {
      li = thread_map.get(tid);
      if (li == null) {
	 li = new ThreadItem(tid,e);
	 thread_map.put(tid,li);
       }
    }

   return li;
}



/********************************************************************************/
/*										*/
/*	Detailing methods							*/
/*										*/
/********************************************************************************/

public Collection<DymonDetailing> getDetailings()
{
   Collection<DymonDetailing> r = new ArrayList<DymonDetailing>();
   r.add(thread_detailing);
   return r;
}



private double getAgentPriority(boolean force)
{
   if (!force && check_always) return 0;

   double totsam = getRecentTotal();

   if (!force && totsam < MIN_SAMPLES) return 0;

   double totblk = getRecentRatio();
   if (totblk > MAX_BLOCKS) totblk = MAX_BLOCKS;

   double p = totblk/MAX_BLOCKS * 0.9 + 0.1;

   if (p < 0) {
      System.err.println("DYMON: NEGATIVE THREAD PRIORITTY " + totsam + " " + totblk + " " +
			    getRecentCount());
    }

   return p;
}



private class Detailing extends DymonDetailing {

   Detailing()						{ super(for_process); }

   @Override public String getDetailName()		{ return getName(); }
   @Override protected double getLocalPriority()	{ return getAgentPriority(false); }
   @Override public long getDetailInterval()		{ return INSTRUMENT_TIME; }
   @Override public long getDetailOverhead()		{ return OVERHEAD_FIXED_TIME; }
   @Override public double getDetailSlowdown()		{ return OVERHEAD_SLOWDOWN; }

   @Override protected DymonPatchRequest getPatchRequest(int priority) {
      patch_request.reset(null,getDetailInterval(),priority);
      return patch_request;
    }

}	// end of subclass Detailing



/********************************************************************************/
/*										*/
/*	Class to hold information for instrumentation				*/
/*										*/
/********************************************************************************/

private class PatchRequest extends DymonPatchRequest {

   PatchRequest() {
      super(for_process,"LOCKAGENT");
    }

   @Override protected void addPatchCommands(IvyXmlWriter xw,boolean insert) {
      xw.begin("VAR");
      xw.field("NAME","CONTENTION");
      xw.field("VALUE",insert);
      xw.end("VAR");
    }

   @Override boolean allowEmptyPatch()		{ return true; }

   @Override String getRequestName()		{ return getName(); }


}	// end of subclass PatchRequest




/********************************************************************************/
/*										*/
/*	Class to hold lock information for a thread				*/
/*										*/
/********************************************************************************/

private static class ThreadItem implements Comparable<ThreadItem> {

   private long thread_id;
   private String thread_name;
   private double start_time;
   private double end_time;
   private double last_time;
   private double num_samples;
   private double num_blocks;
   private double num_waits;
   private double num_ios;
   private double num_sleeps;
   private double num_runs;
   private double tot_blocks;
   private double tot_waits;
   private double wait_time;
   private double block_time;
   private double check_time;
   private Map<ThreadItem,Long> count_map;
   private boolean is_terminated;
   private boolean has_locks;

   ThreadItem(String tid,Element e) {
      thread_id = Long.parseLong(tid);
      thread_name = IvyXml.getAttrString(e,"THREAD");
      start_time = IvyXml.getAttrLong(e,"START");
      end_time = 0;
      count_map = new TreeMap<ThreadItem,Long>();
      has_locks = false;
    }

   void update(Element e,DymonAgentThreads agt) {
      end_time = IvyXml.getAttrDouble(e,"END",0);
      if (end_time == 0) last_time = IvyXml.getAttrDouble(e,"LAST");
      else last_time = end_time;
      num_samples = IvyXml.getAttrDouble(e,"TOTAL");
      num_blocks = IvyXml.getAttrDouble(e,"BLOCKS");
      num_waits = IvyXml.getAttrDouble(e,"WAITS");
      num_ios = IvyXml.getAttrDouble(e,"IOS");
      num_sleeps = IvyXml.getAttrDouble(e,"SLEEPS");
      num_runs = IvyXml.getAttrDouble(e,"RUNS");
      tot_blocks = IvyXml.getAttrDouble(e,"TOTBLKS");
      tot_waits = IvyXml.getAttrDouble(e,"TOTWAITS");
      wait_time = IvyXml.getAttrDouble(e,"WTIME",0);
      block_time = IvyXml.getAttrDouble(e,"BTIME",0);
      check_time = IvyXml.getAttrDouble(e,"CTIME",0);
      for (Element lo : IvyXml.elementsByTag(e,"LOCKON")) {
	 String tid = IvyXml.getAttrString(lo,"ID");
	 ThreadItem litm = agt.findThreadItem(tid,lo);
	 litm.setHasLocks();
	 count_map.put(litm,IvyXml.getAttrLong(lo,"COUNT"));
       }
      is_terminated = IvyXml.getAttrBool(e,"TERMINATED");
      if (!has_locks && count_map.size() > 0) has_locks = true;
    }

   public int compareTo(ThreadItem li) {
      return thread_name.compareTo(li.thread_name);
    }

   void output(IvyXmlWriter xw) {
      xw.begin("THREAD");
      xw.field("ID",thread_id);
      xw.field("NAME",thread_name);
      xw.field("RUNPCT",IvyFormat.formatPercent(num_runs/num_samples));
      xw.field("WAITPCT",IvyFormat.formatPercent(num_waits/num_samples));
      xw.field("IOPCT",IvyFormat.formatPercent(num_ios/num_samples));
      xw.field("BLOCKPCT",IvyFormat.formatPercent(num_blocks/num_samples));
      xw.field("SLEEPPCT",IvyFormat.formatPercent(num_sleeps/num_samples));
      xw.field("RUNTIME",IvyFormat.formatTime(num_runs/num_samples*(last_time-start_time)));
      xw.field("NUMBLOCK",tot_blocks);
      xw.field("NUMWAIT",tot_waits);
      if (check_time > 0) {
	 xw.field("WAITTIME",IvyFormat.formatTime(wait_time/check_time*(last_time-start_time)));
	 xw.field("BLOCKTIME",IvyFormat.formatTime(block_time/check_time*(last_time-start_time)));
       }
      xw.field("TOTTIME",IvyFormat.formatTime(last_time-start_time));
      xw.field("TERMINATED",is_terminated);
      xw.end();
    }

   void outputMatrix(IvyXmlWriter xw,Collection<ThreadItem> allitms) {
      double scale = 1;
      if (num_blocks > 0) scale = tot_blocks / num_blocks;

      xw.begin("FORTHREAD");
      xw.field("ID",thread_id);
      xw.field("NAME",thread_name);
      for (ThreadItem li : allitms) {
	 if (li.getHasLocks()) {
	    Long ct = count_map.get(li);
	    xw.begin("BLOCKON");
	    xw.field("ID",li.thread_id);
	    if (ct != null && ct.doubleValue() > 0) {
	       long est = (long)(ct.doubleValue() * scale);
	       xw.field("COUNT",est);
	     }
	    xw.end();
	  }
       }
      xw.end();
    }

   boolean checkUse(long now) {
      if (num_samples == 0) return false;
      if (is_terminated && now - end_time > DEAD_TIME) return false;
      return true;
    }

   boolean checkRemove(long now) {
      if (is_terminated && now - end_time > DEAD_TIME) return true;
      return false;
    }

   long getThreadId()			{ return thread_id; }
   String getThreadName()		{ return thread_name; }
   double getNumBlocks()		{ return num_blocks; }
   double getNumSamples()		{ return num_samples; }
   double getBlockTime() {
      if (check_time == 0) return -1;
      return block_time/check_time * (last_time - start_time);
    }

   boolean getHasLocks()		{ return has_locks; }
   void setHasLocks()			{ has_locks = true; }

}	// end of subclass ThreadItem




private static class LockComparator implements Comparator<ThreadItem> {

   public int compare(ThreadItem l1,ThreadItem l2) {
      if (l1.getThreadId() < l2.getThreadId()) return -1;
      if (l1.getThreadId() > l2.getThreadId()) return 1;
      return l1.getThreadName().compareTo(l2.getThreadName());
    }

}	// end of subclass LockComparator



}	// end of class DymonAgentThreads




/* end of DymonAgentThreads.java */
