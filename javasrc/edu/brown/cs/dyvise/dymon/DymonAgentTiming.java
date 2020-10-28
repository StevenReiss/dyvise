/********************************************************************************/
/*										*/
/*		DymonAgentTiming.java						*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymon/DymonAgentTiming.java,v 1.7 2010-03-30 16:22:05 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymonAgentTiming.java,v $
 * Revision 1.7  2010-03-30 16:22:05  spr
 * Clean up the code.
 *
 * Revision 1.6  2009-09-19 00:09:46  spr
 * Update dymon for seletive agent insertion and removal.
 *
 * Revision 1.5  2009-06-04 18:53:50  spr
 * Set up for binary distribution.
 *
 * Revision 1.4  2009-04-11 23:46:18  spr
 * Use IvyFormat rather than local code.
 *
 * Revision 1.3  2009-03-20 02:06:50  spr
 * Update dymon: add time-based information, updated memory, etc.
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



class DymonAgentTiming extends DymonAgent {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private int		num_processors;
private double		avg_live;
private double		stdev_live;
private long		tot_threads;
private boolean 	check_always;
private PatchRequest	patch_request;
private Map<String,TimeItem> thread_map;

private DymonDetailing	timing_detailing;

private static final double	report_threshold = 0.0;


private static final long	INSTRUMENT_TIME = 10000;
private static final long	OVERHEAD_FIXED_TIME = 40;
private static final double	OVERHEAD_SLOWDOWN = 0.01;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DymonAgentTiming(DymonMain dm,DymonProcess p)
{
   super(dm,p);

   thread_map = new TreeMap<String,TimeItem>();

   if (dm == null || p == null) check_always = false;
   else check_always = dm.getBooleanResource(p.getStartClass(),"TIMINGCHECK");

   patch_request = new PatchRequest();

   timing_detailing = new Detailing();
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getName()		{ return "TIMING"; }

@Override public String getDyperAgentName()	{ return "TIMING"; }

@Override protected String getMonitorClass()
{
   return "edu.brown.cs.dyvise.dyper.DyperAgentTiming";
}



/********************************************************************************/
/*										*/
/*	Startup methods 							*/
/*										*/
/********************************************************************************/

@Override protected void handleStart(DymonProcess dp)
{
   if (check_always) {
      dp.setDyperVar("CPUTIME","TRUE");
    }
}



/********************************************************************************/
/*										*/
/*	Clear methods								*/
/*										*/
/********************************************************************************/

@Override public void doClear()
{
   thread_map.clear();

   timing_detailing.doClear();
}




/********************************************************************************/
/*										*/
/*	Report handling methods 						*/
/*										*/
/********************************************************************************/

@Override public void handleReport(Element r)
{
   Element ce = IvyXml.getElementByTag(r,"TIMING");

   if (ce == null) return;

   num_processors = IvyXml.getAttrInt(ce,"PROCS");
   double ct = IvyXml.getAttrDouble(ce,"NCOUNT",0);
   if (ct > 1) {
      avg_live = IvyXml.getAttrDouble(ce,"NLIVE") / ct;
      double l2 = IvyXml.getAttrDouble(ce,"NLIVE2");
      stdev_live = 1.0 / (ct-1) * (l2 - ct * avg_live*avg_live);
      stdev_live = Math.sqrt(stdev_live);
    }
   tot_threads = IvyXml.getAttrLong(ce,"TOTAL");

   for (Element te : IvyXml.elementsByTag(ce,"THREAD")) {
      String tid = IvyXml.getAttrString(te,"ID");
      TimeItem li = findTimeItem(tid,te);
      li.update(te,this);
    }

   // IvyXml.getAttrLong(ce,"ACTIVE");
   // IvyXml.getAttrLong(ce,"CREATED");
   // IvyXml.getAttrLong(ce,"DIED");
}




/********************************************************************************/
/*										*/
/*	Analysis methods							*/
/*										*/
/********************************************************************************/

@Override public void outputAnalysis(IvyXmlWriter xw)
{
   xw.begin("TIMING");
   xw.field("PROCESSORS",num_processors);
   xw.field("LIVE",avg_live);
   xw.field("LIVESTDEV",stdev_live);
   xw.field("NTHREAD",tot_threads);
   xw.field("SUMMARY",Math.random());

   for (TimeItem li : thread_map.values()) {
      li.output(xw);
    }

   xw.end("TIMING");
}




/********************************************************************************/
/*										*/
/*	Summary methods 							*/
/*										*/
/********************************************************************************/

@Override public double getSummaryValue()		{ return -1; }



/********************************************************************************/
/*										*/
/*	Method to find/create a holder for a thread				*/
/*										*/
/********************************************************************************/

private TimeItem findTimeItem(String tid,Element e)
{
   TimeItem li = thread_map.get(tid);
   if (li == null) {
      li = new TimeItem(tid,e);
      thread_map.put(tid,li);
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
   r.add(timing_detailing);
   return r;
}



private double getAgentPriority()
{
   if (check_always) return 0;

   return 0.3;					// its always worthwhile to get detailed timings
}




private class Detailing extends DymonDetailing {

   Detailing()						{ super(for_process); }

   @Override public String getDetailName()		{ return getName(); }
   @Override protected double getLocalPriority()	{ return getAgentPriority(); }
   @Override public long getDetailInterval()		{ return INSTRUMENT_TIME; }
   @Override public long getDetailOverhead()		{ return OVERHEAD_FIXED_TIME; }
   @Override public double getDetailSlowdown()		{ return OVERHEAD_SLOWDOWN; }

   @Override protected DymonPatchRequest getPatchRequest(int priority) {
      patch_request.reset(null,getDetailInterval(),priority);
      return patch_request;
    }

   @Override public double getContinuousPriority()	{ return 0.3; }
   @Override public double getContinuousOverhead()	{ return OVERHEAD_SLOWDOWN; }
   @Override public void startContinuousTracing() {
      for_process.setDyperVar("CPUTIME","TRUE");
      check_always = true;
    }
   @Override public void endContinuousTracing() {
      for_process.setDyperVar("CPUTIME","FALSE");
      check_always = false;
    }



}	// end of subclass Detailing




/********************************************************************************/
/*										*/
/*	Class to hold information for instrumentation				*/
/*										*/
/********************************************************************************/

private class PatchRequest extends DymonPatchRequest {

   PatchRequest() {
      super(for_process,"TIMINGAGENT");
    }

   @Override protected void addPatchCommands(IvyXmlWriter xw,boolean insert) {
      xw.begin("VAR");
      xw.field("NAME","CPUTIME");
      xw.field("VALUE",insert);
      xw.end("VAR");
    }

   @Override boolean allowEmptyPatch()			{ return true; }

   @Override String getRequestName()			{ return getName(); }

   @Override public PatchOverlap getPatchOverlap()	{ return PatchOverlap.ANY; }

}	// end of subclass PatchRequest




/********************************************************************************/
/*										*/
/*	Class to hold lock information for a thread				*/
/*										*/
/********************************************************************************/

private static class TimeItem implements Comparable<TimeItem> {

   private String thread_id;
   private String thread_name;
   private double clock_time;
   private double cpu_time;
   private double user_time;

   TimeItem(String tid,Element e) {
      thread_id = tid;
      thread_name = IvyXml.getAttrString(e,"NAME");
    }

   void update(Element e,DymonAgentTiming agt) {
      clock_time = IvyXml.getAttrDouble(e,"CLOCK");
      cpu_time = IvyXml.getAttrDouble(e,"CPU");
      user_time = IvyXml.getAttrDouble(e,"USER");
    }

   public int compareTo(TimeItem li) {
      return thread_name.compareTo(li.thread_name);
    }

   void output(IvyXmlWriter xw) {
      if (cpu_time/clock_time > report_threshold) {
	 xw.begin("THREAD");
	 xw.field("ID",thread_id);
	 xw.field("NAME",thread_name);
	 xw.field("PCTCPU",IvyFormat.formatPercent(cpu_time/clock_time));
	 xw.field("PCTUSER",IvyFormat.formatPercent(user_time/clock_time));
	 xw.field("PCTSYS",IvyFormat.formatPercent((cpu_time - user_time)/clock_time));
	 xw.end();
       }
    }

}	// end of subclass TimeItem




}	// end of class DymonAgentTiming




/* end of DymonAgentTiming.java */
