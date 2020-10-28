/********************************************************************************/
/*										*/
/*		DymonAgentIO.java						*/
/*										*/
/*	DYPER monitor agent for I/O monitoring					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymon/DymonAgentIO.java,v 1.6 2012-10-05 00:52:56 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymonAgentIO.java,v $
 * Revision 1.6  2012-10-05 00:52:56  spr
 * Code clean up.
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



class DymonAgentIO extends DymonAgent {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private long		total_samples;
private long		total_io;
private long		total_time;
private Map<String,IoData> io_map;
private Detailing	io_detailing;
private int		anal_counter;
private PatchRequest	patch_request;

private double		report_threshold = 0.005;


private static final int	MIN_SAMPLES = 100;

private static final long	INSTRUMENT_TIME = 15000;
private static final long	OVERHEAD_FIXED_TIME = 2000;
private static final double	OVERHEAD_SLOWDOWN = 0.30;

private static final String	NAME = "IO";

private static final int	CLEAR_EVERY = 100;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DymonAgentIO(DymonMain dm,DymonProcess p)
{
   super(dm,p);

   total_samples = 0;
   total_io = 0;
   total_time = 0;
   io_map = new HashMap<String,IoData>();
   anal_counter = 0;
   patch_request = new PatchRequest();

   io_detailing = new Detailing();
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getName()		{ return NAME; }

@Override public String getDyperAgentName()	{ return "IO"; }


@Override protected String getMonitorClass()
{
   return "edu.brown.cs.dyvise.dyper.DyperAgentIO";
}



/********************************************************************************/
/*										*/
/*	Clear methods								*/
/*										*/
/********************************************************************************/

@Override public void doClear()
{
   total_samples = 0;
   total_io = 0;
   total_time = 0;
   io_map.clear();

   clearRecentCounts();

   io_detailing.doClear();
}



/********************************************************************************/
/*										*/
/*	Report handling methods 						*/
/*										*/
/********************************************************************************/

@Override public void handleReport(Element r)
{
   long now = IvyXml.getAttrLong(r,"TIME");

   Element ce = IvyXml.getElementByTag(r,"IOCOUNTERS");

   long montime = IvyXml.getAttrLong(ce,"MONTIME");

   total_time = montime;
   total_samples = IvyXml.getAttrLong(ce,"SAMPLES");
   total_io = IvyXml.getAttrLong(ce,"TOTAL");

   for (Element te : IvyXml.elementsByTag(ce,"ITEM")) {
      String nm = IvyXml.getAttrString(te,"NAME");
      IoData id = io_map.get(nm);
      if (id == null) {
	 id = new IoData(nm);
	 io_map.put(nm,id);
       }
      id.update(te);
    }

   for (Element te : IvyXml.elementsByTag(ce,"RTIO")) {
      int id = IvyXml.getAttrInt(te,"COUNTER");
      long ct = IvyXml.getAttrLong(te,"COUNT");
      CounterData cd = for_process.getCounterData(id);
      if (cd == null) {
	 System.err.println("DYMON: Can't find io counter data for id = " + id);
	 continue;
       }
      String name = cd.getName();
      IoData iod = io_map.get(name);
      if (iod == null) {
	 // System.err.println("DYMON: Can't find iodata for " + name);
	 continue;
       }
      iod.updateCounter(ct,cd.getActiveTime(now,NAME),cd.getTimesActive(NAME),cd.isActive(NAME));
    }

   addDelta(new IODelta(now));

   updateRecentCounts(total_io,total_samples);
}




/********************************************************************************/
/*										*/
/*	Analysis methods							*/
/*										*/
/********************************************************************************/

@Override public void outputAnalysis(IvyXmlWriter xw)
{
   if (++anal_counter > CLEAR_EVERY) {
      anal_counter = 0;
      Map<String,Number> thrds = for_process.handleSimpleQuery("THREADS","REPORTABLE");
      if (thrds != null) {
	 for (IoData id : io_map.values()) {
	    id.clearThreads(thrds.keySet());
	  }
       }
    }

   xw.begin("IO");

   xw.field("TOTTIME",IvyFormat.formatTime(total_time));
   xw.field("TOTSAMP",total_samples);
   xw.field("TOTIO",total_io);
   xw.field("IOTIME",IvyFormat.formatTime(((double) total_io)/total_samples*total_time));
   xw.field("RECENT",getRecentRatio());

   Set<IoData> ioset = new TreeSet<IoData>(io_map.values());

   for (IoData id : ioset) {
      id.output(xw);
    }

   xw.end("IO");
}




/********************************************************************************/
/*										*/
/*	Summary methods 							*/
/*										*/
/********************************************************************************/

@Override public double getSummaryValue()		{ return getAgentPriority(); }

@Override public double getConfidence()
{
   if (total_samples < MIN_SAMPLES) return 0;

   double conf = (io_detailing.getNumDetailing() > 0 ? 1 : 0.5);

   return conf;
}



@Override public void outputSummary(IvyXmlWriter xw)
{
   if (total_samples == 0) return;

   xw.begin("METER");
   xw.field("NAME","IO %");
   xw.field("VALUE",getRecentRatio());
   xw.field("TYPE","PERCENT");
   xw.field("MIN",0);
   xw.field("MAX",1.0);
   xw.end();
}




/********************************************************************************/
/*										*/
/*	Immediate methods							*/
/*										*/
/********************************************************************************/

@Override public void outputImmediate(IvyXmlWriter xw)
{
   xw.begin("IODELTA");
   processDeltas(xw);
   xw.end("IODELTA");
}



/********************************************************************************/
/*										*/
/*	Class to hold I/O information for a method				*/
/*										*/
/********************************************************************************/

private class IoData implements Comparable<IoData> {

   private String class_name;
   private String method_name;
   private double total_count;
   private Map<String,long []> thread_counts;
   private Map<StackTraceElement,Long> source_counts;
   private long count_active;
   private long total_times;
   private long num_active;

   IoData(String nm) {
      int idx = nm.indexOf('@');
      class_name = nm.substring(0,idx);
      method_name = nm.substring(idx+1);
      total_count = 0;
      thread_counts = new HashMap<String,long[]>();
      source_counts = new HashMap<StackTraceElement,Long>();
      count_active = 0;
      total_times = 0;
      num_active = 0;
    }

   void update(Element e) {
      total_count = IvyXml.getAttrDouble(e,"COUNT");
      for (Element te : IvyXml.elementsByTag(e,"THREAD")) {
	 String nm = IvyXml.getAttrString(te,"NAME");
	 long [] val = thread_counts.get(nm);
	 if (val == null) {
	    val = new long[1];
	    thread_counts.put(nm,val);
	  }
	 val[0] = IvyXml.getAttrLong(te,"COUNT");
       }
      for (Element se : IvyXml.elementsByTag(e,"SOURCE")) {
	 StackTraceElement ste = new StackTraceElement(IvyXml.getAttrString(se,"CLASS"),
							  IvyXml.getAttrString(se,"METHOD"),
							  IvyXml.getAttrString(se,"FILE"),
							  IvyXml.getAttrInt(se,"LINE"));
	 source_counts.put(ste,IvyXml.getAttrLong(se,"COUNT"));
       }
    }

   void updateCounter(long ct,long active,long times,boolean isact) {
      count_active = active;
      total_times = ct;
      if (!isact && times != num_active) {
	 num_active = times;
       }
    }

   void clearThreads(Set<String> active) {
      if (active == null) return;
      for (Iterator<String> it = thread_counts.keySet().iterator(); it.hasNext(); ) {
	 String tid = it.next();
	 if (!active.contains(tid)) it.remove();
       }
    }

   void output(IvyXmlWriter xw) {
      if (total_count/total_samples < report_threshold) return;

      xw.begin("IOMETHOD");
      xw.field("CLASS",class_name);
      xw.field("METHOD",method_name);
      xw.field("COUNT",IvyFormat.formatCount(total_count));
      xw.field("PCT",IvyFormat.formatPercent(total_count/total_samples));
      xw.field("TIME",IvyFormat.formatTime(total_count/total_samples * total_time));
      if (total_times > 0) {
	 double ct = ((double)total_times) * ((double) total_time) / count_active;
	 xw.field("CALLS",IvyFormat.formatCount(ct));
       }

      Set<Map.Entry<String,long []>> tset = new TreeSet<Map.Entry<String,long []>>(
	 new ThreadCompare());
      tset.addAll(thread_counts.entrySet());
      for (Map.Entry<String,long []> ent : tset) {
	 xw.begin("THREAD");
	 xw.field("NAME",ent.getKey());
	 double ct = ent.getValue()[0];
	 xw.field("COUNT",IvyFormat.formatCount(ct));
	 xw.field("PCT",IvyFormat.formatPercent(ct/total_count));
	 xw.field("TIME",IvyFormat.formatTime(ct/total_samples * total_time));
	 xw.end("THREAD");
       }

      Set<Map.Entry<StackTraceElement,Long>> eset = new TreeSet<Map.Entry<StackTraceElement,Long>>(
	 new SourceCompare());
      eset.addAll(source_counts.entrySet());
      for (Map.Entry<StackTraceElement,Long> ent : eset) {
	 double ct = ent.getValue().longValue();
	 xw.begin("SOURCE");
	 xw.field("FILE",ent.getKey().getFileName());
	 xw.field("CLASS",ent.getKey().getClassName());
	 xw.field("METHOD",ent.getKey().getMethodName());
	 xw.field("LINE",ent.getKey().getLineNumber());
	 xw.field("COUNT",IvyFormat.formatCount(ct));
	 xw.field("PCT",IvyFormat.formatPercent(ct/total_count));
	 xw.end("SOURCE");
       }
      xw.end("IOMETHOD");
    }

   public int compareTo(IoData io) {
      double v = total_count - io.total_count;
      if (v > 0) return -1;
      else if (v < 0) return 1;
      int i = class_name.compareTo(io.class_name);
      if (i != 0) return i;
      return method_name.compareTo(io.method_name);
    }


   boolean detail() {
      double p0 = total_count/total_io;
      if (p0 > 0.10) return true;
      return false;
    }

   String getFullName() {
      return class_name + "@" + method_name;
    }

}	// end of subclass IoData



/********************************************************************************/
/*										*/
/*	Detailing methods and classes						*/
/*										*/
/********************************************************************************/

public Collection<DymonDetailing> getDetailings()
{
   Collection<DymonDetailing> r = new ArrayList<DymonDetailing>();
   r.add(io_detailing);
   return r;
}



private double getAgentPriority()
{
   if (total_samples < MIN_SAMPLES) return 0;

   double p1 = getRecentRatio();
   if (p1 > 1) p1 = 1;

   double p2 = io_map.size()/20.0;
   if (p2 > 1) p2 = 1;

   double p3 = p1 * 0.75 + p2 * 0.25;

   return p3;
}




private DymonPatchRequest getAgentPatchRequest(long interval,int prior)
{
   List<String> items = new ArrayList<String>();

   for (IoData id : io_map.values()) {
      if (id.detail()) items.add(id.getFullName());
    }

   patch_request.reset(items,interval,prior);

   if (patch_request.isEmpty()) return null;

   return patch_request;
}



private class Detailing extends DymonDetailing {

   Detailing()						{ super(for_process); }

   @Override public String getDetailName()		{ return getName(); }
   @Override protected double getLocalPriority()	{ return getAgentPriority(); }
   @Override public long getDetailInterval()		{ return INSTRUMENT_TIME; }
   @Override public long getDetailOverhead()		{ return OVERHEAD_FIXED_TIME; }
   @Override public double getDetailSlowdown()		{ return OVERHEAD_SLOWDOWN; }

   @Override protected DymonPatchRequest getPatchRequest(int p) {
      return getAgentPatchRequest(getDetailInterval(),p);
    }

}	// end of subclass Detailing


/********************************************************************************/
/*										*/
/*	Class to hold patch request						*/
/*										*/
/********************************************************************************/

private class PatchRequest extends DymonPatchRequest {

   PatchRequest() {
      super(for_process,"IOAGENT");
    }

   @Override protected void addPatchInfo(IvyXmlWriter xw,String c,String m) {
      xw.begin("PATCH");
      xw.field("WHAT","ENTER");
      xw.field("MODE","IOAGENT_ENTER");
      xw.end("PATCH");
    }

   @Override String getRequestName()			{ return getName(); }

   @Override public PatchOverlap getPatchOverlap()	{ return PatchOverlap.CLASS; }

}	// end of subclass PatchRequest



/********************************************************************************/
/*										*/
/*	Class to hold immediate information					*/
/*										*/
/********************************************************************************/

private class IODelta implements DeltaData {

   private long last_report;
   private long last_samples;
   private double last_total;

   IODelta(long now) {
      last_report = now;
      last_samples = total_samples;
      last_total = total_io;
    }

   public void outputDelta(IvyXmlWriter xw,DeltaData prevd) {
      if (prevd == null) return;
      IODelta prev = (IODelta) prevd;
      xw.begin("DELTA");
      xw.field("NOW",last_report);
      xw.field("SAMPLE",last_samples - prev.last_samples);
      xw.field("IO",last_total - prev.last_total);
      xw.end("DELTA");
    }

}	// end of subclass IODelta




}	// end of class DymonAgentIO




/* end of DymonAgentIO.java */

