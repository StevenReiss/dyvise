/********************************************************************************/
/*										*/
/*		DymonAgentMemory.java						*/
/*										*/
/*	DYPER monitor agent for memory and gc					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymon/DymonAgentMemory.java,v 1.8 2012-10-05 00:52:56 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymonAgentMemory.java,v $
 * Revision 1.8  2012-10-05 00:52:56  spr
 * Code clean up.
 *
 * Revision 1.7  2011-03-19 20:34:25  spr
 * Code cleanup
 *
 * Revision 1.6  2010-03-30 16:22:05  spr
 * Clean up the code.
 *
 * Revision 1.5  2009-09-19 00:09:46  spr
 * Update dymon for seletive agent insertion and removal.
 *
 * Revision 1.4  2009-06-04 18:53:50  spr
 * Set up for binary distribution.
 *
 * Revision 1.3  2009-04-11 23:46:18  spr
 * Use IvyFormat rather than local code.
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



class DymonAgentMemory extends DymonAgent {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private double		total_time;
private long		total_allocs;

private Map<String,MemUsage> mem_values;
private Map<String,GCUsage> gc_values;
private Map<String,AllocUsage> alloc_values;

private boolean 	do_continuous;
private ContinuousPatchRequest continuous_request;

private DymonDetailing	mem_detailing;
private DymonDetailing	full_detailing;
private DymonDetailing	continuous_detailing;

private static List<String>	patch_methods;

private static double count_threshold = 0.01;
private static double source_threshold = 0.05;

private static final long	INSTRUMENT_TIME = 10000;
private static final long	OVERHEAD_FIXED_TIME = 1000;
private static final double	OVERHEAD_SLOWDOWN = 0.25;

private static final long	FULL_INSTRUMENT_TIME = 5000;
private static final long	FULL_OVERHEAD_FIXED_TIME = 1000;
private static final double	FULL_OVERHEAD_SLOWDOWN = 10.0;

private static final double	CONTINUOUS_OVERHEAD = 0.01;

private static final int	HISTORY_SIZE = 50;
private static final double	GC_MAX = 5;
private static final double	MEM_MAX = 1E8;

private int	  history_count;
private double [] mem_history;
private double [] gc_history;


static {
   patch_methods = new ArrayList<String>();
   patch_methods.add("java.lang.Object@<init>");
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DymonAgentMemory(DymonMain dm,DymonProcess p)
{
   super(dm,p);

   mem_values = new TreeMap<String,MemUsage>();
   gc_values = new TreeMap<String,GCUsage>();
   alloc_values = new TreeMap<String,AllocUsage>();

   total_time = 0;

   mem_history = new double[HISTORY_SIZE];
   gc_history = new double[HISTORY_SIZE];
   history_count = 0;

   mem_detailing = new Detailing();
   full_detailing = new FullDetailing();
   continuous_detailing = new ContinuousDetailing();

   if (dm == null || p == null) do_continuous = false;
   else do_continuous = dm.getBooleanResource(p.getStartClass(),"MEMORYCOUNTS");
   continuous_request = new ContinuousPatchRequest();
}



/********************************************************************************/
/*										*/
/*	Clear methods								*/
/*										*/
/********************************************************************************/

@Override public void doClear()
{
   mem_values.clear();
   gc_values.clear();
   alloc_values.clear();

   total_time = 0;
   total_allocs = 0;

   history_count = 0;

   clearRecentCounts();

   mem_detailing.doClear();
   full_detailing.doClear();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getName()		{ return "MEMORY"; }



@Override public String getDyperAgentName()	{ return "MEMORY"; }



@Override protected String getMonitorClass()
{
   return "edu.brown.cs.dyvise.dyper.DyperAgentMemory";
}



/********************************************************************************/
/*										*/
/*	Continuous tracing methods						*/
/*										*/
/********************************************************************************/

@Override protected void handleStart(DymonProcess dp)
{
   if (do_continuous) {
      startContinuous();
    }
}



private void startContinuous()
{
   dymon_main.requestPatch(continuous_request);
   for_process.setDyperDetail("MEMORY","ALLOC_COUNT",true);
   do_continuous = true;
}



private void endContinuous()
{
   dymon_main.removePatch(continuous_request);
   for_process.setDyperDetail("MEMORY","ALLOC_COUNT",false);
   do_continuous = false;
}



/********************************************************************************/
/*										*/
/*	Report handling methods 						*/
/*										*/
/********************************************************************************/

@Override public void handleReport(Element r)
{
   Element ce = IvyXml.getElementByTag(r,"MEMORY");
   total_time = IvyXml.getAttrLong(ce,"MONTIME");
   double ctime = IvyXml.getAttrDouble(ce,"COUNTTIME",0);
   long acnt = IvyXml.getAttrLong(ce,"ALLOCS");
   if (ctime > 0) total_allocs = (long) (acnt * (total_time / ctime));

   for (Element e : IvyXml.elementsByTag(ce,"USAGE")) {
      String nm = IvyXml.getAttrString(e,"NAME");
      MemUsage mu = mem_values.get(nm);
      if (mu == null) {
	 mu = new MemUsage(nm);
	 mem_values.put(nm,mu);
       }
      mu.update(e);
    }

   for (Element e : IvyXml.elementsByTag(ce,"GC")) {
      String nm = IvyXml.getAttrString(e,"NAME");
      GCUsage gu = gc_values.get(nm);
      if (gu == null) {
	 gu = new GCUsage(nm);
	 gc_values.put(nm,gu);
       }
      gu.update(e);
    }

   for (Element e : IvyXml.elementsByTag(ce,"ALLOC")) {
      String nm = IvyXml.getAttrString(e,"CLASS");
      if (nm != null) {
	 AllocUsage au = alloc_values.get(nm);
	 if (au == null) {
	    au = new AllocUsage(nm);
	    alloc_values.put(nm,au);
	  }
	 au.update(e);
       }
      else {
	 String tnm = IvyXml.getAttrString(e,"THREAD");
	 if (tnm != null) {
	  }
       }
    }

   long total = total_allocs;
   if (ctime == 0) {
      for (AllocUsage au : alloc_values.values()) total += au.getCount();
    }
   updateRecentCounts(total,(long) total_time);
}




/********************************************************************************/
/*										*/
/*	Analysis methods							*/
/*										*/
/********************************************************************************/

@Override public void outputAnalysis(IvyXmlWriter xw)
{
   xw.begin("MEMORY");

   double totused = 0;
   double totcomm = 0;
   for (MemUsage mu : mem_values.values()) {
      if (!mu.getName().startsWith("POOL_")) {
	 totused += mu.getUsed();
	 totcomm += mu.getCommitted();
       }
    }
   xw.field("USED",IvyFormat.formatMemory(totused));
   xw.field("COMMITTED",IvyFormat.formatMemory(totcomm));

   long totgc = 0;
   for (GCUsage gu : gc_values.values()) totgc += gu.getCount();
   xw.field("GCS",totgc);

   xw.field("ALLOCS",total_allocs);
   xw.field("TTIME",total_time);
   xw.field("ALLOCSPER",(total_allocs/total_time));

   xw.begin("REGIONS");
   for (MemUsage mu : mem_values.values()) {
      if (mu.getName().startsWith("POOL_")) continue;
      mu.output(xw);
    }
   xw.end();

   xw.begin("POOLS");
   for (MemUsage mu : mem_values.values()) {
      if (!mu.getName().startsWith("POOL_")) continue;
      mu.output(xw);
    }
   xw.end();

   xw.begin("GCS");
   for (GCUsage gu : gc_values.values()) {
      gu.output(xw);
    }
   xw.end();

   double cnttime = mem_detailing.getActiveTime();
   cnttime = mem_detailing.getActiveRunningTime();
   double fulltime = full_detailing.getActiveTime();
   fulltime = full_detailing.getActiveRunningTime();

   xw.begin("ALLOCATIONS");
   if (cnttime > 0) {
      double scale = total_time / cnttime * mem_detailing.getActiveFraction();
      if (fulltime > 0) {
	 scale = total_time / fulltime * full_detailing.getActiveFraction();
       }

      xw.field("TIME",IvyFormat.formatTime(total_time));
      xw.field("MONITOR",IvyFormat.formatTime(cnttime));
      xw.field("FULLMON",IvyFormat.formatTime(fulltime));
      xw.field("SCALE",scale);
      xw.field("ACTIVE",IvyFormat.formatTime(total_time * mem_detailing.getActiveFraction()));
      xw.field("RECENT",getRecentRatio());

      Set<AllocUsage> rslt = new TreeSet<AllocUsage>(alloc_values.values());
      long total = 0;
      for (AllocUsage au : rslt) total += au.getCount();
      xw.field("TOTAL",IvyFormat.formatCount(total*scale));

      for (AllocUsage au : rslt) {
	 if (au.getCount() >= total*count_threshold) {
	    au.output(xw,scale,source_threshold);
	  }
       }
    }
   else {
      xw.field("TOTAL",0);
    }
   xw.end();

   xw.end("MEMORY");
}




/********************************************************************************/
/*										*/
/*	Summary methods 							*/
/*										*/
/********************************************************************************/

@Override public double getSummaryValue()	{ return getAgentPriority(); }

@Override public double getConfidence() 	{ return 1.0; }

@Override public void outputSummary(IvyXmlWriter xw)
{
   double totused = 0;
   for (MemUsage mu : mem_values.values()) {
      if (!mu.getName().startsWith("POOL_")) {
	 totused += mu.getUsed();
       }
    }
   double totgc = 0;
   for (GCUsage gu : gc_values.values()) totgc += gu.getCount();

   if (total_time == 0) return;

   xw.begin("METER");
   xw.field("NAME","MEMORY USED");
   xw.field("VALUE",totused);
   xw.field("TYPE","MEMORY");
   xw.end();

   xw.begin("METER");
   xw.field("NAME","GCs PER Minute");
   xw.field("VALUE",totgc/total_time * 60000);
   xw.end();

   if (getRecentCount() > 0) {
      xw.begin("METER");
      xw.field("NAME","ALLOCS PER Minute");
      xw.field("VALUE",getRecentRatio() * 60000);
      xw.end();
    }
}




/********************************************************************************/
/*										*/
/*	Detailing methods							*/
/*										*/
/********************************************************************************/

public Collection<DymonDetailing> getDetailings()
{
   Collection<DymonDetailing> r = new ArrayList<DymonDetailing>();
   r.add(mem_detailing);
   r.add(full_detailing);
   r.add(continuous_detailing);
   return r;
}



private double getAgentPriority()
{
   double totused = 0;
   for (MemUsage mu : mem_values.values()) {
      if (!mu.getName().startsWith("POOL_")) {
	 totused += mu.getUsed() + mu.getCommitted();
       }
    }

   double gctot = 0;
   for (GCUsage gu : gc_values.values()) gctot += gu.getCount();

   int i = (history_count++ % HISTORY_SIZE);

   if (totused == 0) totused = 1024;
   if (gctot == 0) gctot = 1024;

   mem_history[i] = totused;
   gc_history[i] = gctot;

   if (history_count <= HISTORY_SIZE) return 0;
   int j = (i == 0 ? HISTORY_SIZE-1 : i-1);
   double dmem = Math.abs(mem_history[i] - mem_history[j]);
   double dgc = gc_history[i] - gc_history[j];
   double siz = Math.log(totused)/Math.log(2)/32;

   if (dmem > MEM_MAX) dmem = MEM_MAX;
   if (dgc > GC_MAX) dgc = GC_MAX;
   if (siz > 1) siz = 1;

   // should also use allocations per second if you have it

   return (dmem/MEM_MAX * 0.20) + (dgc/GC_MAX * 0.35) + (siz * 0.35) + 0.1;
}




private class Detailing extends DymonDetailing {

   private PatchRequest patch_request;

   Detailing() {
      super(for_process);
      patch_request = new PatchRequest();
    }

   @Override public String getDetailName()		{ return getName(); }
   @Override protected double getLocalPriority()	{ return getAgentPriority(); }
   @Override public long getDetailInterval()		{ return INSTRUMENT_TIME; }
   @Override public long getDetailOverhead()		{ return OVERHEAD_FIXED_TIME; }
   @Override public double getDetailSlowdown()		{ return OVERHEAD_SLOWDOWN; }

   @Override protected DymonPatchRequest getPatchRequest(int priority) {
      if (do_continuous) patch_request.reset(null,getDetailInterval(),priority);
      else patch_request.reset(patch_methods,getDetailInterval(),priority);
      return patch_request;
    }

}	// end of subclass Detailing





private class FullDetailing extends DymonDetailing {

   private FullPatchRequest patch_request;

   FullDetailing() {
      super(for_process);
      patch_request = new FullPatchRequest();
    }

   @Override public String getDetailName()		{ return getName() + "_FULL"; }
   @Override protected double getLocalPriority()	{ return getAgentPriority(); }
   @Override public long getDetailInterval()		{ return FULL_INSTRUMENT_TIME; }
   @Override public long getDetailOverhead()		{ return FULL_OVERHEAD_FIXED_TIME; }
   @Override public double getDetailSlowdown()		{ return FULL_OVERHEAD_SLOWDOWN; }

   @Override protected DymonPatchRequest getPatchRequest(int priority) {
      if (do_continuous) patch_request.reset(null,getDetailInterval(),priority);
      else patch_request.reset(patch_methods,getDetailInterval(),priority);
      return patch_request;
    }

}	// end of subclass FullDetailing




private class ContinuousDetailing extends DymonDetailing {

   ContinuousDetailing() {
      super(for_process);
    }

   @Override public String getDetailName()		{ return getName(); }
   @Override protected double getLocalPriority()	{ return 0; }

   @Override public double getContinuousPriority()	{ return getAgentPriority(); }
   @Override public double getContinuousOverhead()	{ return CONTINUOUS_OVERHEAD; }

   @Override public void startContinuousTracing()	{ startContinuous(); }
   @Override public void endContinuousTracing() 	{ endContinuous(); }

   @Override protected DymonPatchRequest getPatchRequest(int priority) {
      return null;
    }

}	// end of subclass ContinuousDetailing








/********************************************************************************/
/*										*/
/*	Class to hold information for instrumentation				*/
/*										*/
/********************************************************************************/

private class PatchRequest extends DymonPatchRequest {

   PatchRequest() {
      super(for_process,"MEMORYAGENT");
    }

   @Override String getRequestName()		{ return getName(); }

   @Override protected void addPatchInfo(IvyXmlWriter xw,String c,String m) {
      if (c.equals("java.lang.Object") && m.equals("<init>") && !do_continuous) {
	 xw.begin("PATCH");
	 xw.field("WHAT","ENTER");
	 xw.field("MODE","MEMAGENT_TYPE");
	 xw.end("PATCH");
       }
    }

   @Override boolean allowEmptyPatch()		{ return do_continuous; }
   @Override protected void addPatchCommands(IvyXmlWriter xw,boolean insert) {
      if (do_continuous) {
	 xw.begin("DETAIL");
	 xw.field("AGENT","MEMORY");
	 xw.field("ITEM","ALLOC_TYPE");
	 xw.field("VALUE",insert);
	 xw.end("DETAIL");
       }
    }

   @Override public PatchOverlap getPatchOverlap() { return PatchOverlap.ANY; }

}	// end of subclass PatchRequest




private class FullPatchRequest extends DymonPatchRequest {

   FullPatchRequest() {
      super(for_process,"MEMORYAGENTFULL");
    }

   @Override String getRequestName()		{ return getName() + "_FULL"; }

   @Override protected void addPatchInfo(IvyXmlWriter xw,String c,String m) {
      if (c.equals("java.lang.Object") && m.equals("<init>") && !do_continuous) {
	 xw.begin("PATCH");
	 xw.field("WHAT","ENTER");
	 xw.field("MODE","MEMAGENT_SOURCE");
	 xw.end("PATCH");
       }
    }

   @Override boolean allowEmptyPatch()		{ return do_continuous; }
   @Override protected void addPatchCommands(IvyXmlWriter xw,boolean insert) {
      if (do_continuous) {
	 xw.begin("DETAIL");
	 xw.field("AGENT","MEMORY");
	 xw.field("ITEM","ALLOC_SOURCE");
	 xw.field("VALUE",insert);
	 xw.end("DETAIL");
       }
    }


   @Override public PatchOverlap getPatchOverlap()	{ return PatchOverlap.ANY; }

}	// end of subclass FullPatchRequest




private class ContinuousPatchRequest extends DymonPatchRequest {

   ContinuousPatchRequest() {
      super(for_process,"MEMORYAGENTCONT");
      reset(patch_methods,0,PATCH_PRIORITY_HIGH);
    }

   protected void addPatchInfo(IvyXmlWriter xw,String c,String m) {
      if (c.equals("java.lang.Object") && m.equals("<init>")) {
	 xw.begin("PATCH");
	 xw.field("WHAT","ENTER");
	 xw.field("MODE","MEMAGENT_COUNT");
	 xw.end("PATCH");
       }
    }

   String getRequestName()			{ return getName(); }

   @Override public PatchOverlap getPatchOverlap()	{ return PatchOverlap.CLASS; }

}	// end of subclass ContinuousPatchRequest




/********************************************************************************/
/*										*/
/*	Class to hold information about memory usage				*/
/*										*/
/********************************************************************************/

private static class MemUsage {

   private String stats_name;
   private long init_value;
   private double used_total;
   private double used_total2;
   private double comm_total;
   private double comm_total2;
   private double last_used;
   private double last_comm;
   private long num_mods;
   private long max_value;

   MemUsage(String nm) {
      stats_name = nm;
      init_value = 0;
      used_total = 0;
      used_total2 = 0;
      comm_total = 0;
      comm_total2 = 0;
      num_mods = 0;
      max_value = 0;
    }

   String getName()			{ return stats_name; }
   double getUsed()			{ return last_used; }
   double getCommitted()		{ return last_comm; }

   void update(Element e) {
      init_value = IvyXml.getAttrLong(e,"INIT");
      max_value = IvyXml.getAttrLong(e,"MAX");
      used_total = IvyXml.getAttrDouble(e,"USED");
      used_total2 = IvyXml.getAttrDouble(e,"USED2");
      comm_total = IvyXml.getAttrDouble(e,"COMM");
      comm_total2 = IvyXml.getAttrDouble(e,"COMM2");
      num_mods = IvyXml.getAttrLong(e,"MODS");
      last_used = IvyXml.getAttrDouble(e,"LUSED");
      last_comm = IvyXml.getAttrDouble(e,"LCOMM");
    }

   void output(IvyXmlWriter xw) {
      xw.begin("USAGE");
      xw.field("NAME",stats_name);
      xw.field("INIT",IvyFormat.formatMemory(init_value));
      xw.field("MAX",IvyFormat.formatMemory(max_value));
      xw.field("USEDAVG",IvyFormat.formatMemory(used_total));
      double stdev = Math.sqrt(used_total2 - used_total*used_total);
      xw.field("USEDSTD",IvyFormat.formatMemory(stdev));
      xw.field("COMMAVG",IvyFormat.formatMemory(comm_total));
      stdev = Math.sqrt(comm_total2 - comm_total*comm_total);
      xw.field("COMMSTD",IvyFormat.formatMemory(stdev));
      xw.field("MODS",num_mods);
      xw.field("USED",last_used);
      xw.field("COMM",last_comm);
      xw.end();
    }

}	// end of subclass MemUsage



/********************************************************************************/
/*										*/
/*	Class to hold information about garbage collection			*/
/*										*/
/********************************************************************************/

private static class GCUsage {

   private String gc_name;
   private long gc_count;
   private long gc_time;

   GCUsage(String name) {
      gc_name = name;
      gc_count = 0;
      gc_time = 0;
    }

   long getCount()				{ return gc_count; }

   void update(Element e) {
      gc_count = IvyXml.getAttrLong(e,"COUNT");
      gc_time = IvyXml.getAttrLong(e,"TIME");
    }

   void output(IvyXmlWriter xw) {
      if (gc_count == 0 && gc_time == 0) return;
      xw.begin("GC");
      xw.field("NAME",gc_name);
      xw.field("COUNT",gc_count);
      xw.field("TIME",IvyFormat.formatTime(gc_time));
      xw.end();
    }

}	// end of subclass GCUsage




/********************************************************************************/
/*										*/
/*	Class to hold information about allocations				*/
/*										*/
/********************************************************************************/

private static class AllocUsage implements Comparable<AllocUsage> {

   private String class_name;
   private long alloc_count;
   private Map<StackTraceElement,Long> source_counts;

   AllocUsage(String name) {
      class_name = name;
      alloc_count = 0;
      source_counts = new HashMap<StackTraceElement,Long>();
    }

   void update(Element xml) {
      alloc_count = IvyXml.getAttrLong(xml,"COUNT");
      for (Element e : IvyXml.elementsByTag(xml,"SOURCE")) {
	 StackTraceElement ste = new StackTraceElement(IvyXml.getAttrString(e,"CLASS"),
							  IvyXml.getAttrString(e,"METHOD"),
							  IvyXml.getAttrString(e,"FILE"),
							  IvyXml.getAttrInt(e,"LINE"));
	 source_counts.put(ste,IvyXml.getAttrLong(e,"COUNT"));
       }
    }

   void output(IvyXmlWriter xw,double scale,double thresh) {
      xw.begin("ALLOC");
      xw.field("NAME",class_name);
      xw.field("COUNT",IvyFormat.formatCount(alloc_count * scale));
      xw.field("NSOURCE",source_counts.size());
      Set<Map.Entry<StackTraceElement,Long>> eset = new TreeSet<Map.Entry<StackTraceElement,Long>>(
	 new SourceCompare());
      eset.addAll(source_counts.entrySet());
      for (Map.Entry<StackTraceElement,Long> ent : eset) {
	 double ct = ent.getValue().longValue();
	 if (ct / alloc_count >= thresh) {
	    xw.begin("SOURCE");
	    xw.field("FILE",ent.getKey().getFileName());
	    xw.field("CLASS",ent.getKey().getClassName());
	    xw.field("METHOD",ent.getKey().getMethodName());
	    xw.field("LINE",ent.getKey().getLineNumber());
	    xw.field("COUNT",IvyFormat.formatCount(ct*scale));
	    xw.field("PCT",IvyFormat.formatPercent(ct/alloc_count));
	    xw.end("SOURCE");
	  }
       }
      xw.end("ALLOC");
    }

   public int compareTo(AllocUsage u) {
      if (alloc_count > u.alloc_count) return -1;
      if (alloc_count < u.alloc_count) return 1;
      return class_name.compareTo(u.class_name);
    }

   long getCount()				{ return alloc_count; }

}	// end of subclass AllocUsage



}	// end of class DymonAgentMemory




/* end of DymonAgentMemory.java */

