/********************************************************************************/
/*										*/
/*		DymemStats.java 						*/
/*										*/
/*	Compute and save statistics for correlating classes			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymem/DymemStats.java,v 1.4 2009-09-19 00:09:27 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymemStats.java,v $
 * Revision 1.4  2009-09-19 00:09:27  spr
 * Update dymem with some bug fixes; initial support for reading dump files.
 *
 * Revision 1.3  2009-04-28 18:00:57  spr
 * Update visualization with data panel.
 *
 * Revision 1.2  2009-04-11 23:45:29  spr
 * Provide updated visualizations with time line, hashing, etc.
 *
 * Revision 1.1  2009-03-20 02:10:12  spr
 * Add memory graph computation and display.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymem;


import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.util.*;


class DymemStats implements DymemConstants {



/********************************************************************************/
/*										*/
/*	Local Storage								*/
/*										*/
/********************************************************************************/

private Map<String,ClassData>	class_values;
private List<ClassData> 	use_classes;
private int			interval_count;
private List<Long>		interval_when;
private long [] 		interval_array;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DymemStats()
{
   class_values = new HashMap<String,ClassData>();
   interval_count = 0;
   use_classes = new ArrayList<ClassData>();
   interval_when = new ArrayList<Long>();
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

double [] getSizeValues(String nm,int mx)
{
   if (mx == 0) mx = interval_count;

   double [] r = new double[mx];

   ClassData cd = class_values.get(nm);
   if (cd == null) return r;

   int idx = 0;
   for (Long v : cd.getSizes()) {
      r[idx++] = v.doubleValue();
      if (idx >= mx) break;
    }

   return r;
}



double [] getCountValues(String nm,int mx)
{
   if (mx == 0) mx = interval_count;

   double [] r = new double[mx];

   ClassData cd = class_values.get(nm);
   if (cd == null) {
      if (nm.startsWith("*")) Arrays.fill(r,1);
      return r;
    }

   int idx = 0;
   for (Integer v : cd.getCounts()) {
      r[idx++] = v.doubleValue();
      if (idx >= mx) break;
    }

   return r;
}



double [] getRefValues(String nm,int mx)
{
   if (mx == 0) mx = interval_count;

   double [] r = new double[mx];

   ClassData cd = class_values.get(nm);
   if (cd == null) {
      if (nm.startsWith("*")) Arrays.fill(r,1);
      return r;
    }

   int idx = 0;
   for (Integer v : cd.getRefs()) {
      r[idx++] = v.doubleValue();
      if (idx >= mx) break;
    }

   return r;
}



double [] getPtrValues(String nm,int mx)
{
   if (mx == 0) mx = interval_count;

   double [] r = new double[mx];

   ClassData cd = class_values.get(nm);
   if (cd == null) {
      if (nm.startsWith("*")) Arrays.fill(r,1);
      return r;
    }

   int idx = 0;
   for (Integer v : cd.getPtrs()) {
      r[idx++] = v.doubleValue();
      if (idx >= mx) break;
    }

   return r;
}



double [] getNewCounts(String nm,int mx)
{
   if (mx == 0) mx = interval_count;

   double [] r = new double[mx];

   ClassData cd = class_values.get(nm);
   if (cd == null) {
      if (nm.startsWith("*")) Arrays.fill(r,1);
      return r;
    }

   int idx = 0;
   for (Integer v : cd.getNewCounts()) {
      r[idx++] = v.doubleValue();
      if (idx >= mx) break;
    }

   return r;
}



double [] getAccumSizes(String nm,int mx)
{
   if (mx == 0) mx = interval_count;

   double [] r = new double[mx];

   ClassData cd = class_values.get(nm);
   if (cd == null) {
      if (nm.startsWith("*")) Arrays.fill(r,1);
      return r;
    }

   int idx = 0;
   for (Long v : cd.getAccumSizes()) {
      r[idx++] = v.doubleValue();
      if (idx >= mx) break;
    }

   return r;
}




double [] getAccumCounts(String nm,int mx)
{
   if (mx == 0) mx = interval_count;

   double [] r = new double[mx];

   ClassData cd = class_values.get(nm);
   if (cd == null) {
      if (nm.startsWith("*")) Arrays.fill(r,1);
      return r;
    }

   int idx = 0;
   for (Integer v : cd.getAccumCounts()) {
      r[idx++] = v.doubleValue();
      if (idx >= mx) break;
    }

   return r;
}




double [] getAccumNewCounts(String nm,int mx)
{
   if (mx == 0) mx = interval_count;

   double [] r = new double[mx];

   ClassData cd = class_values.get(nm);
   if (cd == null) {
      if (nm.startsWith("*")) Arrays.fill(r,1);
      return r;
    }

   int idx = 0;
   for (Integer v : cd.getAccumNewCounts()) {
      r[idx++] = v.doubleValue();
      if (idx >= mx) break;
    }

   return r;
}



double [] getValues(String nm,OutputCompareBy what)
{
   double [] rslt = null;

   switch (what) {
      case NONE :
	 break;
      case LOCAL_SIZE :
	 rslt = getSizeValues(nm,0);
	 break;
      case TOTAL_SIZE :
	 rslt = getAccumSizes(nm,0);
	 break;
      case LOCAL_COUNT :
	 rslt = getCountValues(nm,0);
	 break;
      case TOTAL_COUNT :
	 rslt = getAccumCounts(nm,0);
	 break;
      case LOCAL_NEW :
	 rslt = getNewCounts(nm,0);
	 break;
      case TOTAL_NEW :
	 rslt = getAccumNewCounts(nm,0);
	 break;
    }

   return rslt;
}



long [] getIntervals()
{
   synchronized (interval_when) {
      if (interval_array == null) {
	 long [] rslt = new long[interval_when.size()];
	 for (int i = 0; i < rslt.length; ++i) rslt[i] = interval_when.get(i);
	 interval_array = rslt;
       }
      return interval_array;
    }
}




/********************************************************************************/
/*										*/
/*	Accumulation methods							*/
/*										*/
/********************************************************************************/

void accumulate(long when,Element xml)
{

   for (Element e : IvyXml.children(xml,"CLASS")) {
      String cnm = IvyXml.getAttrString(e,"NAME");
      int cct = IvyXml.getAttrInt(e,"COUNT");
      long siz = IvyXml.getAttrLong(e,"SIZE");
      int rct = IvyXml.getAttrInt(e,"REFS");
      int pct = IvyXml.getAttrInt(e,"PTRS");
      int nct = IvyXml.getAttrInt(e,"NEW",0);
      ClassData cd = class_values.get(cnm);
      if (cd == null) {
	 cd = new ClassData(interval_count);
	 class_values.put(cnm,cd);
       }
      if (cd.add(cct,siz,rct,pct,nct)) {
	 cd.setIndex(use_classes.size());
	 use_classes.add(cd);
       }
    }

   synchronized (interval_when) {
      interval_when.add(when);
      ++interval_count;
      interval_array = null;
    }
}




/********************************************************************************/
/*										*/
/*	Class to hold information for a class					*/
/*										*/
/********************************************************************************/

private static class ClassData {

   private List<Long> size_data;
   private List<Integer> count_data;
   private List<Integer> ref_data;
   private List<Integer> ptr_data;
   private List<Integer> new_data;
   private List<Long> accum_size;
   private List<Integer> accum_count;
   private List<Integer> accum_new;
   private boolean values_differ;
   private double size_total;
   private double count_total;
   private int num_intervals;

   ClassData(int ivl) {
      size_data = new ArrayList<Long>();
      count_data = new ArrayList<Integer>();
      ref_data = new ArrayList<Integer>();
      ptr_data = new ArrayList<Integer>();
      new_data = new ArrayList<Integer>();
      accum_size = new ArrayList<Long>();
      accum_count = new ArrayList<Integer>();
      accum_new = new ArrayList<Integer>();
      values_differ = false;
      size_total = 0;
      count_total = 0;
      num_intervals = 0;
      while (num_intervals < ivl) {
	 add(0,0,0,0,0);
       }
    }

   boolean add(int ct,long siz,int ref,int ptr,int nct) {
      size_total += siz;
      count_total += ct;
      size_data.add(siz);
      count_data.add(ct);
      ref_data.add(ref);
      ptr_data.add(ptr);
      new_data.add(nct);
      accum_size.add(0l);
      accum_count.add(0);
      accum_new.add(0);
      ++num_intervals;

      if (!values_differ && num_intervals > 0) {
	 if (siz != size_total/num_intervals || ct != count_total/num_intervals) {
	    values_differ = true;
	    return true;
	  }
       }

      return false;
    }

   void setValues(int idx,GraphNode gn) {
      while (idx >= num_intervals) add(0,0,0,0,0);

      if (count_data.get(idx) == 0) {
	 size_total -= size_data.get(idx);
	 size_data.set(idx,gn.getLocalSize());
	 size_total += gn.getLocalSize();
	 count_total -= count_data.get(idx);
	 count_data.set(idx,(int) gn.getLocalCount());
	 count_total += gn.getLocalCount();
	 ref_data.set(idx,(int) gn.getInRefs());
	 ptr_data.set(idx,(int) gn.getOutRefs());
	 new_data.set(idx,(int) gn.getLocalNewCount());
       }

      accum_size.set(idx,gn.getTotalSize());
      accum_count.set(idx,(int) gn.getTotalCount());
      accum_new.set(idx,(int) gn.getTotalNewCount());
    }

   void setIndex(int v) 			{ }

   List<Long> getSizes()			{ return size_data; }
   List<Integer> getCounts()			{ return count_data; }
   List<Integer> getRefs()			{ return ref_data; }
   List<Integer> getPtrs()			{ return ptr_data; }
   List<Integer> getNewCounts() 		{ return new_data; }
   List<Long> getAccumSizes()			{ return accum_size; }
   List<Integer> getAccumCounts()		{ return accum_count; }
   List<Integer> getAccumNewCounts()		{ return accum_new; }

}	// end of subclass ClassData




/********************************************************************************/
/*										*/
/*	Methods to save history information					*/
/*										*/
/********************************************************************************/

int getIntervalNumber(long when)
{
   synchronized (interval_when) {
      int idx = Collections.binarySearch(interval_when,when);
      if (idx < 0) {
	 idx = -idx - 2;
	 if (idx < 0) idx = 0;
       }
      return idx;
    }
}


void saveData(int ivl,GraphNode gn)
{
   ClassData cd = class_values.get(gn.getName());
   if (cd == null) {
      cd = new ClassData(interval_count);
      class_values.put(gn.getName(),cd);
    }
   cd.setValues(ivl,gn);
}



}	// end of class DymemStats




/* end of DymemStats.java */
