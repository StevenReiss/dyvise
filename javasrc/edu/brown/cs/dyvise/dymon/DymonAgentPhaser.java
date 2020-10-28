/********************************************************************************/
/*										*/
/*		DymonAgentPhaser.java						*/
/*										*/
/*	DYPER monitor agent for phase analysis					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymon/DymonAgentPhaser.java,v 1.6 2009-09-19 00:09:46 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymonAgentPhaser.java,v $
 * Revision 1.6  2009-09-19 00:09:46  spr
 * Update dymon for seletive agent insertion and removal.
 *
 * Revision 1.5  2009-03-20 02:06:50  spr
 * Update dymon: add time-based information, updated memory, etc.
 *
 * Revision 1.4  2008-12-04 01:11:00  spr
 * Update output and fix phaser summary.
 *
 * Revision 1.3  2008-11-24 23:38:03  spr
 * Update phaser to have summary.
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


import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.util.*;



class DymonAgentPhaser extends DymonAgent {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private PhaseData [] phase_steps;
private int current_phase;
private long current_start;
private List<PhaseData> phase_totals;
private int empty_phase;
private double keep_min;
private double use_min;
private int window_size;
private int time_step;
private int empty_count;
private long last_time;
private int same_count;

private Map<String,Item> item_map;


private static int		WINDOW_SIZE = 10;
private static int		EMPTY_SIZE = 3;
private static double		KEEP_PHASE_MIN = 0.800;
private static double		USE_PHASE_MIN = 0.800;


private static final String	NAME = "PHASE";



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DymonAgentPhaser(DymonMain dm,DymonProcess p)
{
   super(dm,p);

   phase_steps = null;
   current_phase = -1;
   current_start = 0;
   last_time = 0;
   phase_totals = null;
   empty_phase = -1;
   keep_min = KEEP_PHASE_MIN;
   use_min = USE_PHASE_MIN;
   window_size = WINDOW_SIZE;
   item_map = null;
   time_step = -1;
   empty_count = 0;
   same_count = 0;

   doClear();
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getName()				{ return NAME; }

@Override public Collection<DymonDetailing> getDetailings()	{ return null; }




/********************************************************************************/
/*										*/
/*	Clear Methods								*/
/*										*/
/********************************************************************************/

@Override public void doClear()
{
   phase_steps = new PhaseData[window_size+1];
   current_phase = -1;
   current_start = 0;
   phase_totals = new ArrayList<PhaseData>();
   empty_phase = -1;
   item_map = new HashMap<String,Item>();
   time_step = -1;
}



/********************************************************************************/
/*										*/
/*	Report handling methods 						*/
/*										*/
/********************************************************************************/

@Override public void handleReport(Element r)
{
   long now = IvyXml.getAttrLong(r,"TIME");

   last_time = now;

   Element ce = IvyXml.getElementByTag(r,"COUNTERS");
   for (Element e : IvyXml.children(ce,"ITEM")) {
      String name = IvyXml.getAttrString(e,"NAME");
      int idx1 = name.indexOf('@');
      if (idx1 < 0) {					// try only doing class items for now
	 Item itm = findItem(name);
	 itm.update(now,e);
       }
    }
   ce = IvyXml.getElementByTag(r,"IOCOUNTERS");
   for (Element e : IvyXml.children(ce,"ITEM")) {
      String inm = IvyXml.getAttrString(e,"NAME");
      for (Element se : IvyXml.children(e,"CSOURCE")) {
	 String name = IvyXml.getAttrString(se,"CLASS") + "@" + inm;
	 Item itm = findItem(name);
	 itm.update(now,se);
       }
    }

   float [] cts = new float[getNumItems()];
   for (Item itm : item_map.values()) {
      int idx = itm.getIndex();
      float ct = itm.getCount(now);
      cts[idx] = ct;
    }

   ++time_step;
   int min = time_step - window_size;
   if (min < 0) min = 0;

   for (int i = min; i < time_step; ++i) {
      int pidx = (i % phase_steps.length);
      PhaseData pd = phase_steps[pidx];
      if (pd == null) {
	 pd = new PhaseData(cts.length);
	 phase_steps[pidx] = pd;
       }
      pd.increment(cts,1.0);
    }

   computePhase(now);

   addDelta(new PhaserDelta(now));
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public void outputAnalysis(IvyXmlWriter xw)
{
   xw.begin("PHASER");
   xw.field("PHASE",current_phase);

   if (current_phase > 0) {
      PhaseData cp = phase_totals.get(current_phase);
      if (cp != null) {
	 xw.field("NAME",cp.getName());
       }
    }

   xw.end("PHASER");
}



@Override public void outputImmediate(IvyXmlWriter xw)
{
   xw.begin("PHASEDELTA");
   processDeltas(xw);
   xw.end("PHASEDELTA");
}



/********************************************************************************/
/*										*/
/*	Methods to handle items 						*/
/*										*/
/********************************************************************************/

private int getNumItems()		{ return item_map.size(); }


private Item findItem(String name)
{
   Item r = item_map.get(name);

   if (r == null) {
      r = new Item(name,item_map.size());
      item_map.put(name,r);
    }

   return r;
}




/********************************************************************************/
/*										*/
/*	Methods to handle phase computation					*/
/*										*/
/********************************************************************************/

private void computePhase(long now)
{
   PhaseData cp = null;

   int lph = current_phase;

   int min = time_step - window_size;
   if (min < 0) return;
   int idx = min % phase_steps.length;
   PhaseData pd = phase_steps[idx];

   // pd.normalize();

   if (pd.isEmpty()) {
      ++empty_count;
      if (current_phase >= 0 && empty_count < EMPTY_SIZE)
	 cp = phase_totals.get(current_phase);
      else {
	 if (empty_phase >= 0) {
	    cp = phase_totals.get(empty_phase);
	  }
	 else {
	    cp = addPhase(null);
	    empty_phase = cp.getPhaseId();
	  }
	 // System.err.println("DYMON: " + for_process.getProcessId() + " EMPTY PHASE " + cp.getPhaseId());
       }
    }
   else {
      empty_count = 0;
      if (current_phase >= 0) {
	 cp = phase_totals.get(current_phase);
	 double tvl = cp.getProduct(pd);
	 if (tvl < keep_min) {
	    // System.err.println("DYMON: " + for_process.getProcessId() + " DROP PHASE " + current_phase + " " + tvl);
	    current_phase = -1;
	    cp = null;
	    same_count = 0;
	  }
	 else {
	    ++same_count;
	    if (same_count > 20) {
	       same_count = 0;
	       setPhaseName(cp);
	     }
	    // System.err.println("DYMON: " + for_process.getProcessId() + " KEEP PHASE " + current_phase + " " + tvl);
	  }
       }
    }

   if (cp == null) {
      double bvl = use_min;
      for (PhaseData ncp : phase_totals) {
	 double tvl = ncp.getProduct(pd);
	 // System.err.println("DYMON: " + for_process.getProcessId() + " MATCH PHASE " + ncp.getPhaseId() + " " + tvl);
	 if (tvl > bvl) {
	    bvl = tvl;
	    cp = ncp;
	  }
       }
      if (cp != null) System.err.println("DYMON: " + for_process.getProcessId() + " SET PHASE " + cp.getPhaseId() + " " + bvl);
    }

   if (cp == null) {
      cp = addPhase(pd);
      System.err.println("DYMON: " + for_process.getProcessId() + " NEW PHASE " + cp.getPhaseId());
    }
   else {
      cp.addData(pd);
    }

   if (current_phase != cp.getPhaseId()) {
      if (lph >= 0) {
	 PhaseData xcp = phase_totals.get(lph);
	 xcp.addTime(now-current_start);
       }
      current_phase = cp.getPhaseId();
      current_start = now;
    }

   pd.clear();
}




private PhaseData addPhase(PhaseData pd)
{
   int idx = phase_totals.size();
   int sz = (pd == null ? 0 : pd.getSize());
   PhaseData npd = new PhaseData(sz);
   phase_totals.add(npd);
   if (pd != null) npd.addData(pd);
   npd.setPhaseId(idx);

   setPhaseName(npd);

   return npd;
}



private void setPhaseName(PhaseData npd)
{
   float [] cts = npd.getCounts();
   if (cts.length == 0) {
      npd.setName("EMPTY");
      return;
    }

   SortedSet<NameInfo> nis = new TreeSet<NameInfo>();
   for (Item itm : item_map.values()) {
      int idx = itm.getIndex();
      if (idx >= 0 && idx < cts.length) {
	 NameInfo ni = new NameInfo(itm.getName(),cts[idx]);
	 nis.add(ni);
       }
    }

   if (nis.size() == 0) {
      npd.setName("EMPTY");
      return;
    }

   double mx = nis.first().getValue();
   StringBuffer buf = new StringBuffer();

   for (NameInfo ni : nis) {
      if (ni.getValue() < mx * 0.5) break;
      if (buf.length() > 0) buf.append(" :: ");
      buf.append(ni.getName());
    }

   npd.setName(buf.toString());
}



private static class NameInfo implements Comparable<NameInfo> {

   private String item_name;
   private double item_value;

   NameInfo(String nm,double v) {
      item_value = v;
      item_name = nm;
    }

   public String getName()		{ return item_name; }
   public double getValue()		{ return item_value; }

   public int compareTo(NameInfo n) {
      if (item_value < n.item_value) return 1;
      if (item_value > n.item_value) return -1;
      return 0;
    }

}	// end of subclass NameInfo




/********************************************************************************/
/*										*/
/*	Summary methods 							*/
/*										*/
/********************************************************************************/

@Override public double getSummaryValue()
{
   if (last_time == 0) return 0.0;

   double tot = 0;
   double cur = 0;
   for (int i = 0; i < phase_totals.size(); ++i) {
      PhaseData pd = phase_totals.get(i);
      long ptim = pd.getTime();
      if (i == current_phase) {
	 ptim += last_time - current_start;
	 cur = ptim;
       }
      tot += ptim;
    }

   if (tot == 0) return 0;

   return (1.0 - cur/tot);
}



@Override public void outputSummary(IvyXmlWriter xw)
{
   if (last_time == 0 || current_phase < 0) return;

   int mx = phase_totals.size();

   xw.begin("METER");
   xw.field("NAME","PHASE NUMBER");
   xw.field("TYPE","NUMBER");
   xw.field("VALUE",current_phase);
   xw.field("MIN",0);
   xw.field("MAX",mx);
   xw.end("METER");

   xw.begin("BARGRAPH");
   xw.field("NAME","% PHASE TIME");
   xw.field("TOTAL",1.0);
   xw.field("TYPE","PERCENT");

   double tot = 0;
   for (int i = 0; i < mx; ++i) {
      PhaseData pd = phase_totals.get(i);
      long ptim = pd.getTime();
      if (i == current_phase) ptim += last_time - current_start;
      tot += ptim;
    }

   for (int i = 0; i < mx; ++i) {
      PhaseData pd = phase_totals.get(i);
      double ptim = pd.getTime();
      if (i == current_phase) ptim += last_time - current_start;
      xw.begin("ITEM");
      xw.field("NAME",pd.getPhaseId());
      xw.field("VALUE",ptim/tot);
      xw.end("ITEM");
    }

   xw.end("BARGRAPH");
}




/********************************************************************************/
/*										*/
/*	Class to hold data for phasing						*/
/*										*/
/********************************************************************************/

private static class PhaseData {

   private double norm_value;
   private float [] count_values;
   private int phase_id;
   private long total_time;
   private String phase_name;

   PhaseData(int ct) {
      norm_value = 0;
      count_values = new float[ct];
      Arrays.fill(count_values,0f);
      phase_id = -1;
      total_time = 0;
      phase_name = null;
    }

   void setPhaseId(int id)			{ phase_id = id; }
   int getPhaseId()				{ return phase_id; }
   int getSize()				{ return count_values.length; }

   void addTime(long delta)			{ total_time += delta; }
   long getTime()				{ return total_time; }

   String getName()				{ return phase_name; }
   void setName(String nm)			{ phase_name = nm; }

   float [] getCounts() 			{ return count_values; }

   void clear() {
      Arrays.fill(count_values,0);
      phase_id = -1;
      norm_value = 0;
      total_time = 0;
    }

   double getProduct(PhaseData pd) {
      int ln = count_values.length;
      if (pd.count_values.length < ln) ln = pd.count_values.length;
      double tot = 0;
      for (int i = 0; i < ln; ++i) {
	 tot += count_values[i]*pd.count_values[i];
       }
      return tot;
    }

   boolean isEmpty()					{ return norm_value == 0; }

   void addData(PhaseData pd) {
      if (pd == null) return;
      increment(pd.count_values,pd.norm_value);
    }

   void increment(float [] cts,double scale) {
      checkSize(cts.length);
      int ln = count_values.length;
      double tsq = 0;
      for (int i = 0; i < ln; ++i) {
	 long c0 = (long)(count_values[i] * norm_value + 0.5);
	 long c1 = (long)(cts[i] * scale + 0.5);
	 double v = c0 + c1;
	 tsq += v*v;
	 count_values[i] = (float) v;
       }
      tsq = Math.sqrt(tsq);
      norm_value = tsq;
      if (tsq == 0) return;
      for (int i = 0; i < ln; ++i) count_values[i] /= tsq;
    }

   private void checkSize(int len) {
      if (len > count_values.length) {
	 count_values = Arrays.copyOf(count_values,len);
       }
    }

}	// end of subclass PhaseData




/********************************************************************************/
/*										*/
/*	Class to hold class item information					*/
/*										*/
/********************************************************************************/

private static class Item {

   private String item_name;
   private long last_time;
   private long total_count;
   private long delta_total;
   private int item_index;

   Item(String name,int idx) {
      item_name = name;
      last_time = 0;
      total_count = 0;
      item_index = idx;
    }

   void update(long now,Element e) {
      last_time = now;
      long tc = IvyXml.getAttrLong(e,"TOTAL");
      delta_total = tc - total_count;
      total_count = tc;
    }

   int getIndex()					{ return item_index; }
   String getName()					{ return item_name; }

   long getCount(long now) {
      if (last_time == now) return delta_total;
      else return 0;
    }

}	// end of subclass Item




/********************************************************************************/
/*										*/
/*	Delta information for immediate output					*/
/*										*/
/********************************************************************************/

private class PhaserDelta implements DeltaData {

   private long last_report;
   private int	phase_id;

   PhaserDelta(long now) {
      last_report = now;
      phase_id = current_phase;
    }

   public void outputDelta(IvyXmlWriter xw,DeltaData prevd) {
      if (phase_id < 0) return;
      xw.begin("PHASE");
      xw.field("WHEN",last_report);
      xw.field("ID",phase_id);
      if (phase_id == empty_phase) xw.field("EMPTY",true);
      xw.end("PHASE");
    }

}	// end of subclass PhaserDelta




}	// end of class DymonAgentPhaser

/* end of DymonAgentPhaser.java */
