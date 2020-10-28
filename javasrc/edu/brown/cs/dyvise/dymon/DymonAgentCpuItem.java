/********************************************************************************/
/*										*/
/*		DymonAgentCpuItem.java						*/
/*										*/
/*	Item for recording and analyzing information for CPU agent		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymon/DymonAgentCpuItem.java,v 1.5 2010-03-30 16:22:05 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymonAgentCpuItem.java,v $
 * Revision 1.5  2010-03-30 16:22:05  spr
 * Clean up the code.
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



class DymonAgentCpuItem implements DymonConstants, Comparable<DymonAgentCpuItem> {




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private DymonAgentCpuItem parent_item;
private Collection<DymonAgentCpuItem> child_items;

private String item_name;
private long   total_count;
private long   base_count;
private long   total_times;
private long   count_active;
private double total_cpu;
private long   last_count;
private long   countsq_active;
private long   num_active;

private double cpu_total;
private double cpu_base;
private double cpu_error;
private double base_error;
private double exec_count;
private double exec_time;


private static Collection<String> bad_items;


private static final double	Z_ALPHA = 1.96;


static {
   bad_items = new ArrayList<String>();
   bad_items.add("org.eclipse.jdt.internal.compiler.CompilationResult@record@");
   bad_items.add("org.eclipse.jdt.core.dom.ASTRecoveryPropagator@markIncludedProblems@");
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DymonAgentCpuItem(DymonAgentCpuItem par,String name)
{
   parent_item = par;
   child_items = null;
   item_name = name;
   total_count = 0;
   base_count = 0;
   cpu_total = 0;
   cpu_base = 0;
   total_times = 0;
   count_active = 0;
   total_cpu = 0;
   last_count = 0;
   countsq_active = 0;
   num_active = 0;
   cpu_error = 0;
   base_error = 0;

   if (par != null) par.addChild(this);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

long getTotalCount()				     { return total_count; }
long getBaseCount()				     { return base_count; }


private String getFullName()
{
   if (parent_item == null || item_name.startsWith("*")) return null;
   String nm = parent_item.getFullName();
   if (nm != null) nm = nm + "@" + item_name;
   else nm = item_name;
   return nm;
}





synchronized void addChild(DymonAgentCpuItem itm)
{
   if (child_items == null) child_items = new TreeSet<DymonAgentCpuItem>();
   child_items.add(itm);
}



private synchronized double getMaxCount()
{
   double max = base_count;
   if (child_items != null) {
      for (DymonAgentCpuItem itm : child_items) {
	 double m1 = itm.getMaxCount();
	 if (m1 > max) max = m1;
       }
    }
   return max;
}




/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

void update(Element e)
{
   total_count = IvyXml.getAttrLong(e,"TOTAL");
   base_count = IvyXml.getAttrLong(e,"BASE");
}




void updateTotals(Element e)
{
   total_count = IvyXml.getAttrLong(e,"CLASS");
   base_count = IvyXml.getAttrLong(e,"CBASE");
}




void updateCounters(Element e,long active,long times,boolean isact)
{
   count_active = active;
   total_times = IvyXml.getAttrLong(e,"COUNT",0);
   total_cpu = IvyXml.getAttrDouble(e,"TIME",0);

   if (!isact && times != num_active) {
      countsq_active += (count_active - last_count)*(count_active - last_count);
      last_count = count_active;
      num_active = times;
    }
}




/********************************************************************************/
/*										*/
/*	Computation methods							*/
/*										*/
 /********************************************************************************/

void computeRoot(double totaltime,double totalsamp)
{
   if (child_items == null || base_count == 0) return;
   double maxpct = getMaxCount()/base_count;
   maxpct *= 1.5;
   if (maxpct >= 1) maxpct = 1;
   else maxpct *= (1-maxpct);
   compute(totaltime,totalsamp,maxpct);
}




private void compute(double totaltime,double totalsamp,double errorscale)
{
   double scale = totaltime / totalsamp;

   cpu_base = base_count * scale;
   cpu_total = total_count * scale;
   cpu_error = Z_ALPHA / 2.0 / Math.sqrt(totalsamp) * totaltime * errorscale;
   cpu_error = Math.sqrt(total_count) * totaltime / totalsamp;
   base_error = Math.sqrt(base_count) * totaltime / totalsamp;

   if (total_times > 0) {
      if (count_active > 0) exec_count = total_times * totaltime / count_active;
      else exec_count = 0;
      exec_time = total_cpu / total_times;
      double csq = count_active - last_count;
      csq = countsq_active + csq*csq;
      // System.err.println("DYMON: COMPUTE " + getFullName() + " " + total_times + " " + totaltime + " " +
      //		       count_active + " " + total_cpu + " " + count_active + " " + num_active);
      // System.err.println("DYMON:\tTIME " + exec_count + " " + exec_time + " " + cpu_total/exec_count);
    }

   if (child_items != null) {
      synchronized (this) {
	 for (DymonAgentCpuItem itm : child_items) itm.compute(totaltime,totalsamp,errorscale);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Methods to check for instrumentation					*/
/*										*/
/********************************************************************************/

void checkDetailing(double tot,double thresh,List<String> rslt)
{
   if (child_items != null) {
      // check if we need to redetail the children
      synchronized (this) {
	 for (DymonAgentCpuItem itm : child_items) itm.checkDetailing(tot,thresh,rslt);
       }
    }
   else {
      if (cpu_base/tot > thresh) {
	 String nm = getFullName();
	 if (canDetail(nm)) rslt.add(nm);
       }
    }
}



private boolean canDetail(String nm)
{
   int idx = nm.lastIndexOf('$');
   if (idx >= 0 && idx+1 < nm.length()) {
      char c = nm.charAt(idx+1);
      if (Character.isDigit(c)) return false;
    }

   for (String s : bad_items) {
      if (nm.startsWith(s)) return false;
    }

   return true;
}


/********************************************************************************/
/*										*/
/*	Ouptut methods								*/
/*										*/
/********************************************************************************/

void outputTotals(IvyXmlWriter xw,double time)
{
   xw.begin("ITEM");
   xw.field("NAME","TOTAL");
   xw.field("BASETIME",IvyFormat.formatTime(cpu_base));
   xw.field("BASEPCT",IvyFormat.formatPercent(cpu_base / time));
   xw.field("TOTALTIME",IvyFormat.formatTime(cpu_total));
   xw.field("TOTALPCT",IvyFormat.formatPercent(cpu_total / time));
   xw.field("TIMEERROR",IvyFormat.formatTime(cpu_error));
   xw.field("BASEERROR",IvyFormat.formatTime(base_error));
   xw.end("ITEM");
}




void outputData(IvyXmlWriter xw,double time,double fullthresh,double basethresh)
{
   String nm = getFullName();
   boolean usechildren = false;
   if (child_items != null) {
      synchronized (this) {
	 for (DymonAgentCpuItem itm : child_items) {
	    if (itm.cpu_total >= fullthresh || itm.cpu_base >= basethresh)
	       usechildren = true;
	  }
       }
    }

   if (nm != null && !usechildren &&
	  (cpu_base >= basethresh || cpu_total >= fullthresh)) {
      xw.begin("ITEM");
      xw.field("NAME",nm);
      xw.field("BASETIME",IvyFormat.formatTime(cpu_base));
      xw.field("BASEPCT",IvyFormat.formatPercent(cpu_base / time));
      xw.field("TOTALTIME",IvyFormat.formatTime(cpu_total));
      xw.field("TOTALPCT",IvyFormat.formatPercent(cpu_total / time));
      xw.field("TIMEERROR",IvyFormat.formatTime(cpu_error));
      xw.field("BASEERROR",IvyFormat.formatTime(base_error));
      if (exec_count > 0) {
	 xw.field("COUNT",IvyFormat.formatCount(exec_count));
	 if (exec_time == 0) {
	    xw.field("TIME",IvyFormat.formatTime(cpu_base));
	    xw.field("TIMEPER",IvyFormat.formatInterval(cpu_base/exec_count));
	  }
	 else {
	    xw.field("TIME",IvyFormat.formatTime(exec_time));
	    xw.field("TIMEPER",IvyFormat.formatInterval(exec_time/exec_count));
	  }
       }
      xw.end("ITEM");
    }

   if (usechildren) {
      synchronized (this) {
	 for (DymonAgentCpuItem itm : child_items) itm.outputData(xw,time,fullthresh,basethresh);
       }
    }
}



void outputSummary(IvyXmlWriter xw,double time,double threshold)
{
   outputSummaryItem(xw,time,threshold,2);
}


private void outputSummaryItem(IvyXmlWriter xw,double time,double threshold,int lvl)
{
   if (cpu_base < threshold || time == 0) return;

   if (lvl > 0 && child_items != null) {
      synchronized (this) {
	 for (DymonAgentCpuItem itm : child_items)
	    itm.outputSummaryItem(xw,time,threshold,lvl-1);
       }
      return;
    }

   String nm = getFullName();
   if (nm != null) {
      xw.begin("ITEM");
      xw.field("NAME",getFullName());
      xw.field("VALUE",cpu_base/time);
      xw.end();
    }
}




/********************************************************************************/
/*										*/
/*	Methods to implement comparable 					*/
/*										*/
/********************************************************************************/

public int compareTo(DymonAgentCpuItem itm)
{
   if (itm == this) return 0;
   else if (itm.parent_item != parent_item) {
      if (parent_item == null) return -1;
      else if (itm.parent_item == null) return 1;
      else return parent_item.compareTo(itm.parent_item);
    }
   else if (item_name == null) return -1;
   else if (itm.item_name == null) return 1;
   return item_name.compareTo(itm.item_name);
}




}	// end of class DymonAgentCpuItem




/* end of DymonAgentCpuItem.java */
