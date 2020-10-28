/********************************************************************************/
/*										*/
/*		DymonAgentCpu.java						*/
/*										*/
/*	DYPER monitor agent for CPU usage					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymon/DymonAgentCpu.java,v 1.5 2009-09-19 00:09:46 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymonAgentCpu.java,v $
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



class DymonAgentCpu extends DymonAgent {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private DymonAgentCpuItem   root_item;
private Map<String,DymonAgentCpuItem> item_map;
private DymonDetailing	cpu_detailing;

private double		total_time;
private long		total_samples;
private long		active_samples;
private int		detailed_count;
private PatchRequest	patch_request;

private double		full_threshold = 0.010;
private double		base_threshold = 0.005;
private double		summary_threshold = 0.010;

private static final long	MIN_SAMPLES = 100;
private static final double	MAX_CPU = 1.5;
private static final double	MAX_ITEMS = 25;


private static final long	INSTRUMENT_TIME = 15000;
private static final long	OVERHEAD_FIXED_TIME = 2000;
private static final double	OVERHEAD_SLOWDOWN = 1.00;
private static final double	DETAIL_THRESHOLD = 0.005;

private static final String	NAME = "CPU";

private static final boolean	use_active_time = true;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DymonAgentCpu(DymonMain dm,DymonProcess p)
{
   super(dm,p);

   root_item = new DymonAgentCpuItem(null,"*TOTAL*");
   item_map = new HashMap<String,DymonAgentCpuItem>();
   patch_request = new PatchRequest();

   cpu_detailing = new Detailing();

   total_time = 0;
   total_samples = 0;
   active_samples = 0;
   detailed_count = 0;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getName()			  { return NAME; }

@Override public String getDyperAgentName()		  { return "CPU"; }

@Override protected String getMonitorClass()
{
   return "edu.brown.cs.dyvise.dyper.DyperAgentCpu";
}



/********************************************************************************/
/*										*/
/*	Clear methods								*/
/*										*/
/********************************************************************************/

@Override public void doClear()
{
   root_item = new DymonAgentCpuItem(null,"*TOTAL*");
   item_map.clear();

   total_time = 0;
   total_samples = 0;
   active_samples = 0;
   detailed_count = 0;

   clearRecentCounts();

   cpu_detailing.doClear();
}




/********************************************************************************/
/*										*/
/*	Report handling methods 						*/
/*										*/
/********************************************************************************/

@Override public void handleReport(Element r)
{
   long now = IvyXml.getAttrLong(r,"TIME");

   Element ce = IvyXml.getElementByTag(r,"COUNTERS");
   long montime = IvyXml.getAttrLong(ce,"MONTIME");
   total_samples = IvyXml.getAttrLong(ce,"SAMPLES");
   active_samples = IvyXml.getAttrLong(ce,"ACTIVE");

   total_time = montime;
   if (use_active_time && active_samples > 0 && total_samples > 0) {
      total_time *= active_samples;
      total_time /= total_samples;
    }

   Element tot = IvyXml.getElementByTag(ce,"TOTALS");
   root_item.updateTotals(tot);

   for (Element e : IvyXml.elementsByTag(ce,"ITEM")) {
      String name = IvyXml.getAttrString(e,"NAME");
      DymonAgentCpuItem itm = findItem(name);
      itm.update(e);
    }
   for (Element e : IvyXml.elementsByTag(ce,"RTITEM")) {
      int id = IvyXml.getAttrInt(e,"COUNTER");
      CounterData cd = for_process.getCounterData(id);
      if (cd == null) {
	 continue;
       }
      if (!cd.isRange()) {
	 String name = cd.getName();
	 DymonAgentCpuItem itm = findItem(name);
	 itm.updateCounters(e,cd.getActiveRunTime(now,total_samples,active_samples,NAME),cd.getTimesActive(NAME),cd.isActive(NAME));
       }
      else {
	 for (int i = 0; ; ++i) {
	    String name = cd.getName(i);
	    if (name == null) break;
	    DymonAgentCpuItem itm = item_map.get(name);
	    if (itm != null) {
	       itm.updateCounters(e,cd.getActiveRunTime(now,total_samples,active_samples,NAME),cd.getTimesActive(NAME),cd.isActive(NAME));
	     }
	  }
       }
    }

   addDelta(new CpuDelta(now));

   updateRecentCounts(root_item.getBaseCount(),total_samples);
}




/********************************************************************************/
/*										*/
/*	Analysis methods							*/
/*										*/
/********************************************************************************/

@Override public void outputAnalysis(IvyXmlWriter xw)
{
   long bct = root_item.getBaseCount();

   xw.begin("CPUTIME");
   xw.field("TOTTIME",IvyFormat.formatTime(total_time));
   xw.field("TOTSAMP",total_samples);
   xw.field("ACTSAMP",active_samples);
   xw.field("ACTIVE",bct);
   if (total_samples > 0) {
      double pct = bct;
      pct /= total_samples;
      xw.field("PCTUSED",IvyFormat.formatPercent(pct));
    }
   if (active_samples > 0) {
      double pct = bct;
      pct /= active_samples;
      xw.field("PCTACT",IvyFormat.formatPercent(pct));
    }
   xw.field("RECENT",getRecentRatio());
   computeTimes();

   root_item.outputTotals(xw,total_time);
   root_item.outputData(xw,total_time,total_time * full_threshold,total_time * base_threshold);

   xw.end("CPUTIME");
}




private void computeTimes()
{
   if (use_active_time && active_samples > 0) {
      root_item.computeRoot(total_time,active_samples);
    }
   else {
      root_item.computeRoot(total_time,total_samples);
    }
}



/********************************************************************************/
/*										*/
/*	Summary methods 							*/
/*										*/
/********************************************************************************/

@Override public double getSummaryValue()	{ return getAgentPriority(false); }

@Override public double getConfidence()
{
   if (total_samples < MIN_SAMPLES || detailed_count == 0) return 0;
   double conf = 0.50;
   if (cpu_detailing.getActiveTime() > 0) conf = 1;
   return conf;
}


@Override public void outputSummary(IvyXmlWriter xw)
{
   if (total_samples == 0) return;

   xw.begin("METER");
   xw.field("NAME","CPU LOAD");
   xw.field("TYPE","PERCENT");
   double pct = getRecentRatio();
   xw.field("VALUE",pct);
   xw.field("MIN",0);
   xw.field("MAX",1.0);
   xw.end("METER");

   xw.begin("BARGRAPH");
   xw.field("NAME","% CPU USAGE");
   xw.field("TOTAL",1.0);
   xw.field("TYPE","PERCENT");
   xw.field("MIN",0);
   root_item.outputSummary(xw,total_time,total_time * summary_threshold);
   xw.end("BARGRAPH");
}




/********************************************************************************/
/*										*/
/*	Immediate methods							*/
/*										*/
/********************************************************************************/

@Override public void outputImmediate(IvyXmlWriter xw)
{
   xw.begin("CPUDELTA");
   processDeltas(xw);
   xw.end("CPUDELTA");
}



/********************************************************************************/
/*										*/
/*	Methods to find and create items					*/
/*										*/
/********************************************************************************/

private DymonAgentCpuItem findItem(String name)
{
   DymonAgentCpuItem itm = item_map.get(name);
   if (itm != null) return itm;

   int idx = name.lastIndexOf("@");
   if (idx < 0) {
      itm = new DymonAgentCpuItem(root_item,name);
    }
   else {
      String pfx = name.substring(0,idx);
      String sfx = name.substring(idx+1);
      DymonAgentCpuItem par = findItem(pfx);
      itm = new DymonAgentCpuItem(par,sfx);
    }

   item_map.put(name,itm);

   return itm;
}





/********************************************************************************/
/*										*/
/*	Detailing methods							*/
/*										*/
/********************************************************************************/

public Collection<DymonDetailing> getDetailings()
{
   Collection<DymonDetailing> r = new ArrayList<DymonDetailing>();
   r.add(cpu_detailing);
   return r;
}



private double getAgentPriority(boolean compute)
{
   if (total_samples < MIN_SAMPLES) return 0;

   if (compute) {
      computeTimes();
      List<String> items = new ArrayList<String>();
      root_item.checkDetailing(total_time,DETAIL_THRESHOLD,items);
      detailed_count = items.size();
    }

   double sz = detailed_count;
   double pct = getRecentRatio();

   if (sz > MAX_ITEMS) sz = MAX_ITEMS;
   if (pct > MAX_CPU) pct = MAX_CPU;

   double p = (pct/MAX_CPU)*0.50 + (sz/MAX_ITEMS)*0.25 + 0.25;

   // System.err.println("CPU: " + sz + " " + pct + " " + p);

   return p;
}




private DymonPatchRequest getAgentPatchRequest(long interval,int prior)
{
   // computeTimes has been called in checking priority

   List<String> items = new ArrayList<String>();
   root_item.checkDetailing(total_time,DETAIL_THRESHOLD,items);
   if (items.size() == 0) return null;

   patch_request.reset(items,interval,prior);

   if (patch_request.isEmpty()) return null;

   return patch_request;
}



private class Detailing extends DymonDetailing {

   Detailing()						{ super(for_process); }

   @Override public String getDetailName()		{ return getName(); }
   @Override protected double getLocalPriority()	{ return getAgentPriority(true); }
   @Override public long getDetailInterval()		{ return INSTRUMENT_TIME; }
   @Override public long getDetailOverhead()		{ return OVERHEAD_FIXED_TIME; }
   @Override public double getDetailSlowdown()		{ return OVERHEAD_SLOWDOWN; }

   @Override protected DymonPatchRequest getPatchRequest(int p) {
      return getAgentPatchRequest(getDetailInterval(),p);
    }

}	// end of subclass Detailing





/********************************************************************************/
/*										*/
/*	Class to hold information for instrumentation				*/
/*										*/
/********************************************************************************/

private class PatchRequest extends DymonPatchRequest {

   PatchRequest() {
      super(for_process,"CPUAGENT");
    }

   @Override protected void addPatchInfo(IvyXmlWriter xw,String c,String m) {
      xw.begin("PATCH");
      xw.field("WHAT","BLOCK");
      xw.field("MODE","CPUAGENT_BLOCKCTR");
      xw.end("PATCH");
      xw.begin("PATCH");
      xw.field("WHAT","ENTER");
      xw.field("MODE","CPUAGENT_ENTER");
      xw.end("PATCH");
      xw.begin("PATCH");
      xw.field("WHAT","EXIT");
      xw.field("MODE","CPUAGENT_EXIT");
      xw.end("PATCH");
    }

   @Override protected void donePrepare() {
      if (min_counter > 0) {
	 for_process.setDyperVar("MIN_COUNTER",Integer.toString(min_counter));
       }
    }

   @Override String getRequestName()		{ return getName(); }

}	// end of subclass PatchRequest




/********************************************************************************/
/*										*/
/*	Class to hold immediate information					*/
/*										*/
/********************************************************************************/

private class CpuDelta implements DeltaData {

   private long last_report;
   private long last_samples;
   private long last_active;
   private double last_total;

   CpuDelta(long now) {
      last_report = now;
      last_samples = total_samples;
      last_active = active_samples;
      last_total = 0;
      if (root_item != null) last_total = root_item.getBaseCount();
    }

   public void outputDelta(IvyXmlWriter xw,DeltaData prevd) {
      if (prevd == null) return;
      CpuDelta prev = (CpuDelta) prevd;
      xw.begin("DELTA");
      xw.field("NOW",last_report);
      xw.field("SAMPLE",last_samples - prev.last_samples);
      xw.field("ACTIVE",last_active - prev.last_active);
      xw.field("CPU",last_total - prev.last_total);
      xw.end("DELTA");
    }

}	// end of subclass CpuDelta




}	// end of class DymonAgentCpu




/* end of DymonAgentCpu.java */
