/********************************************************************************/
/*										*/
/*	`       DylockLockData.java                                             */
/*										*/
/*	DYVISE lock analysis lock information holder				*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2009, Brown University, Providence, RI.				 *
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylock/DylockLockData.java,v 1.6 2013-05-09 12:29:00 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylockLockData.java,v $
 * Revision 1.6  2013-05-09 12:29:00  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.5  2012-10-05 00:52:41  spr
 * Update lock analysis.  Prepare for pattern display.
 *
 * Revision 1.4  2011-04-18 19:24:29  spr
 * Bug fixes in dylock.
 *
 * Revision 1.3  2011-04-01 23:09:02  spr
 * Bug clean up.
 *
 * Revision 1.2  2011-03-19 20:34:18  spr
 * Clean up and fix bugs in dylock.
 *
 * Revision 1.1  2011-03-10 02:25:00  spr
 * New version of dylock to produce visualizations.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dylock;

import edu.brown.cs.ivy.xml.*;
import edu.brown.cs.ivy.file.*;

import org.w3c.dom.Element;

import java.util.*;


class DylockLockData implements DylockConstants, DylockConstants.TraceLockData
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private DylockMain lock_main;
private int lock_id;
private Set<String> lock_class;
private Set<String> merged_classes;
private Set<TraceLockLocation> location_set;
private int wait_count;
private int enter_count;
private int timed_count;
private int waited_count;
private double wait_time;
private double block_time;
private Set<DylockLockData> equiv_locks;
private DylockLockData merge_parent;
private boolean first_level;
private boolean is_prior;
private int	num_instance;

private int		view_id;
private DylockViewType	view_type;
private Set<DylockLockData> prior_locks;
private boolean 	is_monitored;
private Element 	xml_data;

private static int	lock_counter = 0;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/





DylockLockData(DylockMain dm,String id,String cls)
{
   lock_main = dm;
   lock_id = Integer.parseInt(id);
   lock_class = new TreeSet<String>();
   lock_class.add(cls);
   merged_classes = lock_class;
   location_set = new HashSet<TraceLockLocation>();
   wait_count = 0;
   enter_count = 0;
   timed_count = 0;
   waited_count = 0;
   num_instance = 1;
   wait_time = 0;
   block_time = 0;
   first_level = false;
   equiv_locks = null;
   merge_parent = null;
   is_prior = false;
   prior_locks = new HashSet<DylockLockData>();
}




DylockLockData(DylockViewRef dv,Element xml)
{
   lock_main = null;

   Element xdt = IvyXml.getChild(xml,"DATA");
   if (xdt == null && IvyXml.isElement(xml,"LOCK")) xdt = xml;

   lock_id = IvyXml.getAttrInt(xdt,"ID");
   view_id = IvyXml.getAttrInt(xdt,"VID");
   if (view_id <= 0) view_id = ++lock_counter;
   wait_count = IvyXml.getAttrInt(xdt,"WAITS");
   enter_count = IvyXml.getAttrInt(xdt,"ENTERS");
   timed_count = IvyXml.getAttrInt(xdt,"TIMEDS");
   waited_count = IvyXml.getAttrInt(xdt,"WAITEDS");
   num_instance = IvyXml.getAttrInt(xdt,"INSTANCES",1);
   first_level = IvyXml.getAttrBool(xdt,"FIRSTLEVEL");
   wait_time = IvyXml.getAttrDouble(xdt,"WAITTIME",0);
   block_time = IvyXml.getAttrDouble(xdt,"BLOCKTIME",0);
   lock_class = new TreeSet<String>();
   for (Element ce : IvyXml.children(xdt,"CLASS")) {
      String cs = IvyXml.getText(ce);
      if (cs.endsWith(";"))
	 cs = IvyFormat.formatTypeName(cs,true);
      lock_class.add(cs);
    }
   merged_classes = lock_class;
   location_set = new HashSet<TraceLockLocation>();
   for (Element le : IvyXml.children(xdt,"LOCATION")) {
      DylockLockLocation vl = dv.getLocation(le);
      boolean alias = IvyXml.getAttrBool(le,"ISALIAS");
      if (!alias) location_set.add(vl);
    }
   for (TraceLockLocation dll : location_set) {
      dll.finishLoad(dv);
    }

   Element vt = IvyXml.getChild(xml,"ANALYSIS");
   if (vt == null) vt = IvyXml.getChild(vt,"TYPE");
   view_type = DylockViewType.createViewType(dv,vt);
   if (view_type == null) {
      System.err.println("DYLOCKVIEW: Bad analysis: " + IvyXml.convertXmlToString(vt));
    }

   is_monitored = false;
   xml_data = xml;
   is_prior = false;
   prior_locks = null;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

int getLockId() 					{ return lock_id; }

boolean isUsed()					{ return location_set.size() > 0; }

boolean isPrior()					{ return is_prior; }

Iterable<TraceLockLocation> getLocations()		{ return location_set; }

@Override public Iterable<TraceLockLocation> getLockLocations()
{
   return new ArrayList<TraceLockLocation>(location_set);
}


/********************************************************************************/
/*										*/
/*	Set up methods								*/
/*										*/
/********************************************************************************/

void addPrior(DylockLockData ld)
{
   prior_locks.add(ld);
   ld.is_prior = true;
}

void addLocation(DylockLockLocation loc)
{
   if (loc != null) location_set.add(loc);
}


void setCounts(String waitct,String enterct,String timect,String wtdct,
      String waittime,String blocktime,String topcnt)
{
   wait_count = Integer.parseInt(waitct);
   enter_count = Integer.parseInt(enterct);
   timed_count = Integer.parseInt(timect);
   waited_count = Integer.parseInt(wtdct);
   wait_time = Double.parseDouble(waittime);
   block_time = Double.parseDouble(blocktime);
   first_level = !topcnt.equals("0");
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

boolean isMerged()					{ return merge_parent != null; }

DylockLockData getMergedLock()
{
   if (merge_parent == null) return this;
   return merge_parent.getMergedLock();
}


Iterable<DylockLockData> getEquivalents()
{
   if (equiv_locks == null) return Collections.emptyList();
   return equiv_locks;
}


void setWaitTime(double v)			{ wait_time = v; }
void setBlockTime(double v)			{ block_time = v; }


int getLockNumber()				{ return view_id; }

int getNumInstance()				{ return num_instance; }

int getNumLock()				{ return enter_count; }

double getBlockTime()				{ return block_time; }

int getNumWait()				{ return waited_count; }

double getWaitTime()				{ return wait_time; }

boolean isMonitored()				{ return is_monitored; }
void setMonitored(boolean fg)			{ is_monitored = fg; }

Collection<DylockLockData> getPriorLocks()	{ return prior_locks; }

List<DylockViewType> getViewTypes()		{ return view_type.getViewTypes(); }

boolean containsClass(String cnm)
{
   return lock_class.contains(cnm);
}




/********************************************************************************/
/*										*/
/*	Methods for merging locks						*/
/*										*/
/********************************************************************************/

boolean isSameLock(DylockLockData ld)
{
   int ct = getSetOverlap(ld);
   int sz = Math.min(location_set.size(),ld.location_set.size());
   boolean fg = false;

   if (merged_classes.containsAll(ld.lock_class)) {
      if (ct > 0) fg = true;
    }
   else {
      if (ct >= 0.75*sz) fg = true;
    }

   if (fg && lock_main != null && lock_main.getMergePriors() && xml_data == null) {
      for (DylockLockData ld1 : ld.prior_locks) {
	 ld1 = ld1.getMergedLock();
	 if (ld1 == this) return false;
       }
      for (DylockLockData ld1 : prior_locks) {
	 ld1 = ld1.getMergedLock();
	 if (ld1 == ld) return false;
       }
    }

   return fg;
}



void mergeLock(DylockLockData ld)
{
   if (equiv_locks == null) equiv_locks = new HashSet<DylockLockData>();
   equiv_locks.add(ld);
   location_set.addAll(ld.location_set);
   merged_classes.addAll(ld.merged_classes);
   wait_count += ld.wait_count;
   enter_count += ld.enter_count;
   num_instance += ld.num_instance;
   is_prior |= ld.is_prior;
   ld.merge_parent = this;
   for (DylockLockData ld1 : ld.prior_locks) {
      ld1 = ld1.getMergedLock();
      if (ld1 != this) prior_locks.add(ld1);
    }
}




private int getSetOverlap(DylockLockData ld)
{
   int ct = 0;
   for (TraceLockLocation ll : ld.location_set) {
      if (location_set.contains(ll)) ++ct;
    }
   return ct;
}



/********************************************************************************/
/*										*/
/*	Routines to clean up after locks merged 				*/
/*										*/
/********************************************************************************/

void fixup(DylockLockManager dv)
{
   if (xml_data != null) {
      prior_locks = new HashSet<DylockLockData>();
      Element xdt = IvyXml.getChild(xml_data,"DATA");
      for (Element pe : IvyXml.children(xdt,"PRIOR")) {
	 String pid = IvyXml.getText(pe);
	 int pi = Integer.parseInt(pid);
	 DylockLockData ld1 = dv.findLock(pi);
	 if (ld1 != null) {
	    prior_locks.add(ld1);
	    ld1.is_prior = true;
	  }
       }
      xml_data = null;
    }
}


void mergePriors(Set<DylockLockData> used)
{
   Set<DylockLockData> nset = new HashSet<DylockLockData>();
   for (DylockLockData ld : prior_locks) {
      ld = ld.getMergedLock();
      if (used.contains(ld)) {
	 ld.is_prior = true;
	 nset.add(ld);
       }
    }
   prior_locks = nset;
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(IvyXmlWriter xw)
{
   xw.begin("DATA");
   xw.field("ID",lock_id);
   if (view_id != 0) xw.field("VID",view_id);
   xw.field("WAITS",wait_count);
   xw.field("ENTERS",enter_count);
   xw.field("TIMEDS",timed_count);
   xw.field("WAITEDS",waited_count);
   xw.field("FIRSTLEVEL",first_level);
   xw.field("WAITTIME",wait_time);
   xw.field("BLOCKTIME",block_time);
   xw.field("INSTANCES",num_instance);
   xw.field("ISPRIOR",is_prior);

   for (String s : merged_classes) xw.textElement("CLASS",s);

   Set<TraceLockLocation> locks = new HashSet<TraceLockLocation>();
   for (TraceLockLocation ll : location_set) {
      locks.add(ll);
      locks.addAll(ll.getAliases());
    }
   for (TraceLockLocation ll : locks) {
      ll.outputXml(xw,!location_set.contains(ll));
    }

   if (prior_locks != null) {
      for (DylockLockData ld : prior_locks) {
	 xw.textElement("PRIOR",Integer.toString(ld.getLockId()));
       }
    }

   if (equiv_locks != null) {
      StringBuffer buf = new StringBuffer();
      int ct = 0;
      for (DylockLockData ld : equiv_locks) {
	 if (ct++ > 0) buf.append(",");
	 buf.append(ld.getLockId());
       }
      xw.textElement("EQUIV",buf.toString());
    }

   xw.end("DATA");
}



void outputVisualization(IvyXmlWriter xw)
{
   xw.begin("LOCK");

   xw.field("ID",lock_id);
   if (view_id != 0) xw.field("VID",view_id);

   for (String s : merged_classes) xw.textElement("CLASS",s);

   Set<TraceLockLocation> locks = new HashSet<TraceLockLocation>();
   for (TraceLockLocation ll : location_set) {
      locks.add(ll);
      locks.addAll(ll.getAliases());
    }
   for (TraceLockLocation ll : locks) {
      ll.outputXml(xw,!location_set.contains(ll));
    }

   xw.begin("ANALYSIS");
   view_type.outputXml(xw);
   xw.end("ANALYSIS");

   xw.end("LOCK");
}




/********************************************************************************/
/*										*/
/*	Viewing Output Methods							*/
/*										*/
/********************************************************************************/

String getGraphString()
{
   return Integer.toString(getLockNumber());
}


String getTypeString()
{
   if (view_type == null) return "<UNKNOWN>";

   return view_type.getShortString();
}


String getLocationString()
{
   TraceLockLocation loc = null;
   for (TraceLockLocation xl : location_set) {
      loc = xl;
      break;
    }

   if (loc == null) return null;

   String rslt = loc.getMethodName();

   if (location_set.size() > 1) rslt += " (+" + (location_set.size()-1) +")";

   return rslt;
}



String getClassString()
{
   String cnm = null;
   for (String cs : lock_class) {
      cnm = cs;
      break;
    }
   if (cnm == null) return null;

   int idx = cnm.lastIndexOf(".");
   if (idx >= 0) cnm = ">" + cnm.substring(idx+1);
   if (lock_class.size() > 1) cnm += " (+" + Integer.toString(lock_class.size()-1) + ")";

   return cnm;
}


String getDisplayName()
{
   StringBuffer buf = new StringBuffer();
   
   buf.append("LOCK ");
   buf.append(lock_id);
   buf.append(" (");
   for (String s : merged_classes) {
      buf.append(s);
      break;
    }
   buf.append(")");
   
   return buf.toString();
}



/********************************************************************************/
/*										*/
/*	Viewing Tool Tip Methods						*/
/*										*/
/********************************************************************************/

String getTypeToolTip()
{
   StringBuffer buf = new StringBuffer();
   buf.append("<html>");
   view_type.addToolTip(buf);

   return buf.toString();
}



String getLocationToolTip()
{
   StringBuffer buf = new StringBuffer();
   buf.append("<html>");
   DylockViewRef.outputTable("Locations",location_set,buf);
   return buf.toString();
}


String getClassToolTip()
{
   StringBuffer buf = new StringBuffer();
   buf.append("<html>");
   DylockViewRef.outputTableHeader("Lock Classes",buf);
   for (String cnm : lock_class) {
      // cnm = IvyFormat.formatTypeName(cnm);
      DylockViewRef.outputRowStart(buf);
      buf.append(cnm);
      DylockViewRef.outputRowEnd(buf);
    }
   DylockViewRef.outputTableTrailer(buf);
   return buf.toString();
}


String getOverviewToolTip()
{
   StringBuffer buf = new StringBuffer();
   buf.append("<html>");
   buf.append("Lock " + view_id + ":<br>");

   DylockViewRef.outputTableHeader("Lock Classes",buf);
   for (String cnm : lock_class) {
      // cnm = IvyFormat.formatTypeName(cnm);
      DylockViewRef.outputRowStart(buf);
      buf.append(cnm);
      DylockViewRef.outputRowEnd(buf);
    }
   DylockViewRef.outputTableTrailer(buf);

   view_type.addToolTip(buf);

   return buf.toString();
}




/********************************************************************************/
/*										*/
/*	Debugging methods							*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   StringBuffer buf = new StringBuffer();

   Set<Integer> idset = new TreeSet<Integer>();
   idset.add(lock_id);
   if (equiv_locks != null) {
      for (DylockLockData ld : equiv_locks) {
	 idset.add(ld.lock_id);
       }
    }

   buf.append("LOCK ");
   int ct = 0;
   for (Integer iv : idset) {
      if (ct++ > 0) buf.append(",");
      buf.append(iv);
    }
   buf.append(" (");
   ct = 0;
   for (String s : merged_classes) {
      if (ct++ > 0) buf.append(",");
      buf.append(s);
    }
   buf.append(") [");
   buf.append(wait_count);
   buf.append(",");
   buf.append(enter_count);
   buf.append("]\n");
   for (TraceLockLocation ll : location_set) {
      buf.append("   @ ");
      buf.append(ll.toString());
      buf.append("\n");
    }
   return buf.toString();
}



}	// end of class DylockLockData




/* end of DylockLockData.java */
