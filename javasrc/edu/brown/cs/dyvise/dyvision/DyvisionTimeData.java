/********************************************************************************/
/*										*/
/*		DyvisionTimeData.java						*/
/*										*/
/*	Time view for dyper performance evaluation interface			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyvision/DyvisionTimeData.java,v 1.6 2009-09-19 00:14:57 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyvisionTimeData.java,v $
 * Revision 1.6  2009-09-19 00:14:57  spr
 * UPdate front end to clean up tables.
 *
 * Revision 1.5  2009-05-01 23:15:30  spr
 * Handle scaling graphs in time view.
 *
 * Revision 1.4  2009-04-28 18:01:26  spr
 * Add graphs to time lines.
 *
 * Revision 1.3  2009-04-11 23:47:31  spr
 * Handle formating using IvyFormat.
 *
 * Revision 1.2  2009-03-20 02:09:09  spr
 * Add memory view and related controls. Clean code.
 *
 * Revision 1.1  2008-11-24 23:40:04  spr
 * Add time line view.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dyvision;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.util.*;




class DyvisionTimeData implements DyvisionConstants {




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private long	start_time;
private long	last_time;
private long	num_intervals;
private IntData time_data;
private Map<String,SummaryData> summary_map;
private SortedSet<MarkData> mark_set;
private Map<String,MeterData> meter_map;

private static List<String> empty_marks = Collections.emptyList();

private static boolean use_deltas = false;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DyvisionTimeData()
{
   start_time = 0;
   last_time = 0;
   num_intervals = 0;
   time_data = new IntData();
   summary_map = new TreeMap<String,SummaryData>();
   meter_map = new LinkedHashMap<String,MeterData>();
   mark_set = new TreeSet<MarkData>();
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

void useMeter(String name)
{
   meter_map.put(name,new MeterData(name));
}



/********************************************************************************/
/*										*/
/*	Methods to add new data elements					*/
/*										*/
/********************************************************************************/

void addData(Element xml)
{
   if (xml == null) return;

   long now = IvyXml.getAttrLong(xml,"NOW");
   if (start_time == 0) start_time = now;
   if (last_time == now) return;
   last_time = now;

   int idx = time_data.add((int)(now - start_time));

   Element se = IvyXml.getElementByTag(xml,"SUMMARIES");

   for (Element e : IvyXml.elementsByTag(se,"SUMMARY")) {
      String id = IvyXml.getAttrString(e,"NAME");
      SummaryData sd = summary_map.get(id);
      if (sd == null) {
	 sd = new SummaryData(id);
	 summary_map.put(id,sd);
       }
      float sv = IvyXml.getAttrFloat(e,"VALUE",0f);
      float cv = IvyXml.getAttrFloat(e,"CONFIDENCE",0f);
      sd.set(idx,sv,cv);
      for (Element me : IvyXml.elementsByTag(e,"METER")) {
	 String nm = IvyXml.getAttrString(me,"NAME");
	 MeterData md = meter_map.get(nm);
	 if (md != null) md.set(idx,me);
       }
    }

   if (use_deltas) {
/*********
      Element ie = IvyXml.getElementByTag(xml,"IMMEDIATES");
      Element d = IvyXml.getElementByTag(ie,"CPUDELTA");

      for (Element del : IvyXml.children(d,"DELTA")) {
	 // process CPU DELTA ENTRY
       }
      d = IvyXml.getElementByTag(ie,"IODELTA");
      for (Element del : IvyXml.children(d,"DELTA")) {
	 // process IO DELTA ENTRY
       }
      d = IvyXml.getElementByTag(ie,"PHASEDELTA");
      for (Element del : IvyXml.children(d,"PHASE")) {
	 // process PHASE DELTA ENTRY
       }
      d = IvyXml.getElementByTag(ie,"REACTIONDELTA");
      for (Element del : IvyXml.children(d,"DELTA")) {
	 // process REACTION DELTA
       }
*********/       
    }

   ++num_intervals;
}



void addMark(long when,String type)
{
   MarkData md = new MarkData(when,type);
   mark_set.add(md);
}



/********************************************************************************/
/*										*/
/*	Methods to retrieve data						*/
/*										*/
/********************************************************************************/

long getStartTime()			{ return start_time; }
long getEndTime()			{ return last_time; }
long getFullTime()			{ return last_time - start_time; }
long getNumIntervals()			{ return num_intervals; }
int getNumValues()			{ return summary_map.size(); }


List<String> getNames()
{
   List<String> r = new ArrayList<String>();
   for (SummaryData sd : summary_map.values()) {
      r.add(sd.getName());
    }

   return r;
}



float [] getIntervalData(long from,long to,float [] vals)
{
   int ft = (int)(from - start_time);
   int tt = (int)(to - start_time);

   int fi = time_data.getFromIndexOf(ft);
   int ti = time_data.getToIndexOf(tt);
   int sct = summary_map.size();

   if (vals == null || vals.length < sct) {
      vals = new float[sct];
    }

   int idx = 0;
   for (SummaryData sd : summary_map.values()) {
      if (idx >= sct) break;
      vals[idx++] = sd.getAverage(fi,ti);
    }
   while (idx < vals.length) vals[idx++] = 0;

   return vals;
}



Collection<String> getMarks(long from,long to)
{
   if (mark_set.isEmpty()) return empty_marks;

   MarkData fd = new MarkData(from,null);
   MarkData td = new MarkData(to+1,null);

   SortedSet<MarkData> ss = mark_set.subSet(fd,td);
   if (ss.isEmpty()) return empty_marks;

   Collection<String> r = new HashSet<String>();
   for (MarkData md : ss) r.add(md.getType());

   return r;
}



float [] getMeterData(long from,long to,float [] vals)
{
   int ft = (int)(from - start_time);
   int tt = (int)(to - start_time);

   int fi = time_data.getFromIndexOf(ft);
   int ti = time_data.getToIndexOf(tt);
   int mct = meter_map.size();

   if (vals == null || vals.length < mct) {
      vals = new float[mct];
    }

   int idx = 0;
   for (MeterData md : meter_map.values()) {
      if (idx >= mct) break;
      vals[idx++] = md.getAverage(fi,ti);
    }
   while (idx < vals.length) vals[idx++] = 0;

   return vals;
}



float [] getMeterMins(float [] vals)
{
   int mct = meter_map.size();

   if (vals == null || vals.length < mct) {
      vals = new float[mct];
    }

   int idx = 0;
   for (MeterData md : meter_map.values()) {
      if (idx >= mct) break;
      vals[idx++] = md.getMin();
    }
   while (idx < vals.length) vals[idx++] = 0;

   return vals;
}




float [] getMeterMaxs(float [] vals)
{
   int mct = meter_map.size();

   if (vals == null || vals.length < mct) {
      vals = new float[mct];
    }

   int idx = 0;
   for (MeterData md : meter_map.values()) {
      if (idx >= mct) break;
      vals[idx++] = md.getMax();
    }
   while (idx < vals.length) vals[idx++] = 0;

   return vals;
}



float getMeterMin(String nm)
{
   MeterData md = meter_map.get(nm);
   if (md == null) return 0;
   return md.getMin();
}



float getMeterMax(String nm)
{
   MeterData md = meter_map.get(nm);
   if (md == null) return 0;
   return md.getMax();
}



/********************************************************************************/
/*										*/
/*	Interval Data :: generic data for an interval				*/
/*										*/
/********************************************************************************/

private abstract static class IntervalData {

   private String item_name;
   private FloatData value_data;

   IntervalData(String id) {
      item_name = id;
      value_data = new FloatData();
    }

   String getName()				{ return item_name; }

   protected void setValue(int idx,float val) {
      value_data.set(idx,val);
    }

   float getAverage(int fidx,int tidx) {
      if (fidx + 1 >= tidx) return value_data.get(fidx);
      float tot = 0;
      for (int i = fidx; i < tidx; ++i) {
	 tot += value_data.get(i);
       }
      return tot / (tidx - fidx);
    }

}	// end of subclass SummaryData




/********************************************************************************/
/*										*/
/*	Summary Data								*/
/*										*/
/********************************************************************************/

private static class SummaryData extends IntervalData {

   SummaryData(String id) {
      super(id);
    }

   void set(int idx,float val,float cnfd) {
      setValue(idx,val);
    }

}	// end of subclass SummaryData




/********************************************************************************/
/*										*/
/*	Meter Data								*/
/*										*/
/********************************************************************************/

private static class MeterData extends IntervalData {

   private float min_value;
   private float max_value;
   private boolean is_used;

   MeterData(String id) {
      super(id);
      is_used = false;
      min_value = 0;
      max_value = 1;
    }

   void set(int idx,Element xml) {
      float val = IvyXml.getAttrFloat(xml,"VALUE",0);
      if (!is_used) {
	 min_value = IvyXml.getAttrFloat(xml,"MIN",val);
	 max_value = IvyXml.getAttrFloat(xml,"MAX",val);
	 is_used = true;
       }
      setValue(idx,val);
      if (val < min_value) min_value = val;
      if (val > max_value) max_value = val;
    }

   float getMin()			{ return min_value; }
   float getMax()			{ return max_value; }

}	// end of subclass MeterData




/********************************************************************************/
/*										*/
/*	Dynamic array of floats 						*/
/*										*/
/********************************************************************************/

private static class FloatData {

   List<float []> array_data;
   int num_data;

   FloatData() {
      array_data = new ArrayList<float []>();
      array_data.add(new float[8192]);
      num_data = 1;
    }

   void set(int idx,float v) {
      float [] fd;

      int idx1 = idx;
      for (int i = 0; i < num_data; ++i) {
	 fd = array_data.get(i);
	 if (idx1 >= fd.length) idx1 -= fd.length;
	 else {
	    fd[idx1] = v;
	    return;
	  }
       }
      synchronized (this) {
	 idx1 = idx;
	 int mln = 8192;
	 for (int i = 0; i < num_data; ++i) {
	    fd = array_data.get(i);
	    mln = fd.length;
	    if (idx1 >= fd.length) idx1 -= fd.length;
	    else {
	       fd[idx1] = v;
	       return;
	     }
	  }
	 while (idx1 >= mln) mln *= 2;
	 fd = new float[mln];
	 array_data.add(fd);
	 ++num_data;
	 fd[idx1] = v;
       }
    }

   float get(int idx) {
      for (int i = 0; i < num_data; ++i) {
	 float [] fd = array_data.get(i);
	 if (idx >= fd.length) idx -= fd.length;
	 else {
	    return fd[idx];
	  }
       }

      return 0;
    }

}	// end of subclass FloatData



private static class IntData {

   int [] int_data;
   int data_size;

   IntData() {
      int_data = new int[8192];
      data_size = 0;
    }

  synchronized int add(int v) {
     int idx = data_size++;
     if (idx >= int_data.length) {
	int_data = Arrays.copyOf(int_data,int_data.length*2);
      }
     int_data[idx] = v;
     return idx;
   }

   int getFromIndexOf(int v) {
      int idx = Arrays.binarySearch(int_data,0,data_size,v);
      if (idx >= 0) return idx;
      idx = -idx - 1;
      if (idx == 0) return 0;
      return idx-1;
    }

   int getToIndexOf(int v) {
      int idx = Arrays.binarySearch(int_data,0,data_size,v);
      if (idx >= 0) return idx;
      idx = -idx - 1;
      return idx;
    }

}	// end of subclass IntData




/********************************************************************************/
/*										*/
/*	Mark data representation						*/
/*										*/
/********************************************************************************/

private static class MarkData implements Comparable<MarkData> {

   private Long at_time;
   private String mark_type;

   MarkData(long when,String type) {
      at_time = when;
      mark_type = type;
    }

   String getType()			{ return mark_type; }

   public int compareTo(MarkData d) {
      int fg = at_time.compareTo(d.at_time);
      if (fg != 0) return fg;
      if (mark_type == null && d.mark_type == null) return 0;
      if (mark_type == null) return -1;
      if (d.mark_type == null) return 1;
      return mark_type.compareTo(d.mark_type);
    }

   public boolean equals(Object o) {
      if (o instanceof MarkData) {
	 MarkData md = (MarkData) o;
	 if (!at_time.equals(md.at_time)) return false;
	 if (mark_type == null) return md.mark_type == null;
	 return mark_type.equals(md.mark_type);
       }
      return false;
    }

   public int hashCode() {
      int v = at_time.hashCode();
      if (mark_type != null) v += mark_type.hashCode();
      return v;
    }

}	// end of subclass MarkData




}	// end of class DyvisionTimeData
