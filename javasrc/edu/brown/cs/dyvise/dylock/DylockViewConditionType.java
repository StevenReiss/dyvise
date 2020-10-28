/********************************************************************************/
/*										*/
/*		DylockViewConditionType.java					*/
/*										*/
/*	View management for condition-type locks				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylock/DylockViewConditionType.java,v 1.2 2013-05-09 12:29:01 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylockViewConditionType.java,v $
 * Revision 1.2  2013-05-09 12:29:01  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.1  2012-10-05 00:52:41  spr
 * Update lock analysis.  Prepare for pattern display.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dylock;

import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.Element;

import java.util.*;


class DylockViewConditionType extends DylockViewType implements DylockConstants
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private Set<TraceLockLocation> wait_set;
private Set<TraceLockLocation> notify_set;
private Map<DylockLockData,LockInfo> lock_map;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DylockViewConditionType(DylockViewRef dv,Element xe)
{
   super(dv,xe);
   wait_set = new TreeSet<TraceLockLocation>();
   notify_set = new TreeSet<TraceLockLocation>();
   for (Element le : IvyXml.children(IvyXml.getChild(xe,"WAITS"),"LOCATION")) {
      DylockLockLocation vl = getLocation(le);
      wait_set.add(vl);
      addLocation(vl);
    }
   for (Element le : IvyXml.children(IvyXml.getChild(xe,"NOTIFYS"),"LOCATION")) {
      DylockLockLocation vl = getLocation(le);
      notify_set.add(vl);
      addLocation(vl);
    }
   lock_map = new HashMap<DylockLockData,LockInfo>();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getShortString() 			{ return "CONDITION"; }

void addToolTip(StringBuffer buf)
{
   outputTableHeader("CONDITION",buf);
   outputRowStart(buf);
   outputTable("Waits",wait_set,buf);
   outputRowEnd(buf);
   outputRowStart(buf);
   outputTable("Notifies",notify_set,buf);
   outputRowEnd(buf);
   outputTableTrailer(buf);
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void localOutputXml(IvyXmlWriter xw)
{
   xw.field("KIND","CONDITION");
   xw.begin("WAITS");
   for (TraceLockLocation ll : wait_set) ll.outputXml(xw,false);
   xw.end("WAITS");
   xw.begin("NOTIFYS");
   for (TraceLockLocation ll : notify_set) ll.outputXml(xw,false);
   xw.end("NOTIFYS");
}


/********************************************************************************/
/*										*/
/*	Pattern processing methods						*/
/*										*/
/********************************************************************************/

@Override
void processPatternEntry(DylockEventSetBuilder bldr,DylockLockEntry ent,DylockLockData ld,boolean valid)
{
   DylockPatternEvent pevt = null;
   LockInfo li = lock_map.get(ld);
   if (li == null) {
      li = new LockInfo(ld,bldr.getManager());
      lock_map.put(ld,li);
    }

   if (!valid) {
      if (ent.getEntryType() == TraceEntryType.RESET) li.reset();
      return;
    }

   switch (ent.getEntryType()) {
      case ENTER :
	 li.changeWait(1);
	 pevt = new DylockPatternEvent(ent,this,PatternEventType.ENTER,li.getCounter());
	 break;
      case ENTERED :
	 pevt = new DylockPatternEvent(ent,this,PatternEventType.ENTERED,li.getCounter());
	 break;
      case WAIT :
	 if (li.isNormalWait()) li.changeWait(-1);
	 pevt = new DylockPatternEvent(ent,this,PatternEventType.WAIT,li.getCounter());
	 break;
      case WAITED :
	 if (li.isNormalWait()) li.changeWait(1);
	 if (bldr.missingWaitForWaited(ent)) {
	    double t0 = bldr.getLockTime(ent.getLock(),ent.getTime())-10000;
	    DylockLockEntry e0 = new DylockLockEntry(ent,TraceEntryType.ENTER,t0);
	    pevt = new DylockPatternEvent(e0,this,PatternEventType.ENTER,1);
	    bldr.addEvent(pevt);
	    e0 = new DylockLockEntry(ent,TraceEntryType.ENTERED,t0+1000);
	    pevt = new DylockPatternEvent(e0,this,PatternEventType.ENTERED,1);
	    bldr.addEvent(pevt);
	    e0 = new DylockLockEntry(ent,TraceEntryType.WAIT,t0+2000);
	    pevt = new DylockPatternEvent(e0,this,PatternEventType.WAIT,1);
	    bldr.addEvent(pevt);
	  }
	 pevt = new DylockPatternEvent(ent,this,PatternEventType.WAITED,li.getCounter());
	 break;
      case NOTIFY :
	 pevt = new DylockPatternEvent(ent,this,PatternEventType.NOTIFY,li.getCounter());
	 break;
      case UNLOCK :
	 li.changeWait(-1);
	 int ctr = li.getCounter();
	 if (!li.isNormalRun()) ++ctr;
	 pevt = new DylockPatternEvent(ent,this,PatternEventType.EXIT,ctr);
	 break;
      case RESET :
	 li.reset();
	 break;
    }

   if (pevt != null) {
      bldr.addEvent(pevt);
    }
}


boolean startsValidRegion(DylockLockDataManager mgr,DylockLockEntry ent)
{
   DylockLockData ld = ent.getLock();
   LockInfo li = lock_map.get(ld);
   if (li == null) {
      li = new LockInfo(ld,mgr);
      lock_map.put(ld,li);
    }
   if (li.isNormalWait()) {
      if (ent.getEntryType() == TraceEntryType.WAITED && ent.getThreadDepth() == 1) return true;
    }
   return super.startsValidRegion(mgr,ent);
}



private class LockInfo {

   private boolean	normal_wait;
   private boolean	normal_run;
   private int		wait_count;

   LockInfo(DylockLockData ld,DylockLockDataManager bldr) {
      double lw = ld.getWaitTime();
      double tw = bldr.getMaxTime();
      normal_wait = lw >= tw/2.0;
      normal_run = !normal_wait;
      if (ld.getNumLock() > 10 * ld.getNumWait()) normal_run = true;
      wait_count = 0;
    }

   boolean isNormalWait()			{ return normal_wait; }
   boolean isNormalRun()			{ return normal_run; }

   int getCounter() {
      return wait_count;
    }

   void reset() {
      wait_count = 0;
    }

   void changeWait(int delta) {
      wait_count += delta;
    }

}	// end of inner class LockData




}	// end of class DylockViewConditionType




/* end of DylockViewConditionType.java */
