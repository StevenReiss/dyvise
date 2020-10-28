/********************************************************************************/
/*										*/
/*		DylockViewType.java						*/
/*										*/
/*	DYVISE lock analysis lock viewer lock type representations		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylock/DylockViewType.java,v 1.5 2013/09/04 18:36:28 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylockViewType.java,v $
 * Revision 1.5  2013/09/04 18:36:28  spr
 * Minor bug fixes.
 *
 * Revision 1.4  2013-05-09 12:29:01  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.3  2012-10-05 00:52:43  spr
 * Update lock analysis.  Prepare for pattern display.
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

import edu.brown.cs.dyvise.dystore.*;

import edu.brown.cs.ivy.xml.*;

import java.util.*;
import org.w3c.dom.Element;

abstract class DylockViewType implements DylockConstants
{



/********************************************************************************/
/*										*/
/*	Creation methods							*/
/*										*/
/********************************************************************************/

static DylockViewType createViewType(DylockViewRef dv,Element xml)
{
   DylockViewType vt = null;
   int cnt = 0;

   if (IvyXml.isElement(xml,"TYPE")) return createSimpleViewType(dv,xml);

   for (Element vc : IvyXml.children(xml)) {
      DylockViewType xvt = createSimpleViewType(dv,vc);
      if (xvt == null) continue;
      if (vt == null) vt = xvt;
      else if (cnt++ == 0) {
	 vt = new DylockViewMultiType(vt,xvt);
       }
      else vt.addType(xvt);
    }

   if (vt == null) vt = new DylockViewUnknownType(dv);

   return vt;
}



static DylockViewType findViewType(int id)
{
   return id_map.get(id);
}


static DylockViewType createSimpleViewType(DylockViewRef dv,Element vc)
{
   if (!IvyXml.isElement(vc,"TYPE")) return null;
   Integer idv = IvyXml.getAttrInteger(vc,"ID");
   if (idv != null && idv != 0) {
      DylockViewType ovt = id_map.get(idv);
      if (ovt != null) return ovt;
    }
   
   String knd = IvyXml.getAttrString(vc,"KIND");
   if (knd == null) return null;

   DylockViewType xvt = null;

   if (knd.equals("MUTEX")) {
      xvt = new DylockViewMutexType(dv,vc);
    }
   else if (knd.equals("DELAY")) {
      xvt = new DylockViewDelayType(dv,vc);
    }
   else if (knd.equals("JOIN")) {
      xvt = new DylockViewJoinType(dv,vc);
    }
   else if (knd.equals("CONDITION")) {
      xvt = new DylockViewConditionType(dv,vc);
    }
   else if (knd.equals("SEMAPHORE")) {
      xvt = new DylockViewSemaphoreType(dv,vc);
    }
   else if (knd.equals("LATCH")) {
      xvt = new DylockViewLatchType(dv,vc);
    }
   else if (knd.equals("READ-WRITE") || knd.equals("READWRITE")) {
      xvt = new DylockViewReadWriteType(dv,vc);
    }
   else if (knd.equals("MULTIPLE")) {
      xvt = new DylockViewMultiType(dv,vc);
    }
  
   if (xvt != null && idv != null && idv != 0) id_map.put(idv,xvt);
   
   return xvt;
}




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private DylockViewRef for_viewer;
private Set<TraceLockLocation> used_locations;
private Map<String,DylockLockRunData> run_data;
private int type_id;

private static Map<Integer,DylockViewType> id_map = new HashMap<Integer,DylockViewType>();




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected DylockViewType(DylockViewRef dv,Element e)
{
   for_viewer = dv;
   if (e == null) type_id = 0;
   else type_id = IvyXml.getAttrInt(e,"ID",0);
   used_locations = new TreeSet<TraceLockLocation>();
   run_data = null;
}



/********************************************************************************/
/*										*/
/*	Abstract methods							*/
/*										*/
/********************************************************************************/

void addType(DylockViewType vt) 			{ }

List<DylockViewType> getViewTypes()
{
   List<DylockViewType> rslt = new ArrayList<DylockViewType>();
   rslt.add(this);
   return rslt;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

abstract String getShortString();
abstract void addToolTip(StringBuffer buf);
final void outputXml(IvyXmlWriter xw)
{
   xw.begin("TYPE");
   xw.field("ID",type_id);
   localOutputXml(xw);
   xw.end("TYPE");
}


abstract void localOutputXml(IvyXmlWriter xw);

int getTypeId() 		{ return type_id; }
DylockViewRef getViewer()	{ return for_viewer; }


/********************************************************************************/
/*										*/
/*	Location set management methods 					*/
/*										*/
/********************************************************************************/

DylockLockLocation getLocation(Element xml)
{
   return for_viewer.getLocation(xml);
}



protected void addLocation(DylockLockLocation vl)
{
   used_locations.add(vl);
}


protected void addLocations(Collection<TraceLockLocation> vs)
{
   used_locations.addAll(vs);
}



Set<TraceLockLocation> getLocations()			{ return used_locations; }



/********************************************************************************/
/*										*/
/*	Run data management							*/
/*										*/
/********************************************************************************/

protected boolean isInLockSet(int lid,Collection<TraceLockLocation> set)
{
   for (TraceLockLocation ll : set) {
      if (ll.getId() == lid) return true;
    }

   return false;
}


protected DylockLockRunData getRunData(String lock,double when)
{
   if (run_data == null) run_data = new HashMap<String,DylockLockRunData>();

   DylockLockRunData rd = run_data.get(lock);

   if (rd == null) {
      rd = new DylockLockRunData(lock + "-" + getTypeId(),when);
      run_data.put(lock,rd);
    }

   return rd;
}



/********************************************************************************/
/*										*/
/*	Run time processing methods						*/
/*										*/
/********************************************************************************/

void clear()  
{
   if (run_data != null) run_data.clear();
}


void processRunEntry(RunningEntry ent,String lock,DystoreStore ds)	{ }



/********************************************************************************/
/*										*/
/*	Pattern processing methods						*/
/*										*/
/********************************************************************************/

void processPatternEntry(DylockEventSetBuilder bldr,DylockLockEntry ent,DylockLockData ld,boolean valid)
{ }

boolean startsValidRegion(DylockLockDataManager bldr,DylockLockEntry ent)
{
   if (ent.getEntryType() == TraceEntryType.ENTER && ent.getThreadDepth() == 0) return true;
   
   return false;
}




/********************************************************************************/
/*										*/
/*	Output helper methods							*/
/*										*/
/********************************************************************************/

protected void outputTable(String ttl,Collection<TraceLockLocation> locs,StringBuffer buf)
{
   DylockViewRef.outputTable(ttl,locs,buf);
}



protected void outputTableHeader(String ttl,StringBuffer buf)
{
   DylockViewRef.outputTableHeader(ttl,buf);
}



protected void outputTableTrailer(StringBuffer buf)
{
   DylockViewRef.outputTableTrailer(buf);
}


protected void outputRowStart(StringBuffer buf)
{
   DylockViewRef.outputRowStart(buf);
}

protected void outputRowEnd(StringBuffer buf)
{
   DylockViewRef.outputRowEnd(buf);
}





}	// end of class DylockViewType




/* end of DylockViewType.java */
