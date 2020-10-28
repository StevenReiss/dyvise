/********************************************************************************/
/*										*/
/*		DylockViewSemaphoreType.java					*/
/*										*/
/*	description of class							*/
/*										*/
/*	Written by spr								*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylock/DylockViewSemaphoreType.java,v 1.2 2013-05-09 12:29:01 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylockViewSemaphoreType.java,v $
 * Revision 1.2  2013-05-09 12:29:01  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.1  2012-10-05 00:52:43  spr
 * Update lock analysis.  Prepare for pattern display.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dylock;


import edu.brown.cs.dyvise.dystore.DystoreStore;
import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.Element;

import java.util.*;


class DylockViewSemaphoreType extends DylockViewType implements DylockConstants
{

/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private Set<TraceLockLocation> lock_set;
private Set<TraceLockLocation> unlock_set;
private int initial_count;
private Map<DylockLockData,LockInfo> lock_map;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DylockViewSemaphoreType(DylockViewRef dv,Element xe)
 {
   super(dv,xe);
   initial_count = IvyXml.getAttrInt(xe,"COUNT");
   lock_set = new TreeSet<TraceLockLocation>();
   unlock_set = new TreeSet<TraceLockLocation>();
   for (Element le : IvyXml.children(IvyXml.getChild(xe,"LOCKSET"),"LOCATION")) {
      DylockLockLocation vl = getLocation(le);
      lock_set.add(vl);
      addLocation(vl);
    }
   for (Element le : IvyXml.children(IvyXml.getChild(xe,"UNLOCKSET"),"LOCATION")) {
      DylockLockLocation vl = getLocation(le);
      unlock_set.add(vl);
      addLocation(vl);
    }
   lock_map = new HashMap<DylockLockData,LockInfo>();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getShortString()
{
   if (initial_count == 0) return "PRODUCER";
   else return "SEMAPHORE";
}



void addToolTip(StringBuffer buf)
{
   String what = "PRODUCER-CONSUMER";
   if (initial_count > 0) what = "SEMAPHORE " + initial_count;
   outputTableHeader(what,buf);
   outputRowStart(buf);
   outputTable("Locks",lock_set,buf);
   outputRowEnd(buf);
   outputRowStart(buf);
   outputTable("Unlocks",unlock_set,buf);
   outputRowEnd(buf);
   outputTableTrailer(buf);
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void localOutputXml(IvyXmlWriter xw) {
   xw.field("KIND","SEMAPHORE");
   xw.field("COUNT",initial_count);
   xw.begin("LOCKSET");
   for (TraceLockLocation ll : lock_set) ll.outputXml(xw,false);
   xw.end("LOCKSET");
   xw.begin("UNLOCKSET");
   for (TraceLockLocation ll : unlock_set) ll.outputXml(xw,false);
   xw.end("UNLOCKSET");
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

void processRunEntry(RunningEntry ent,String lock,DystoreStore ds)
{
   int lid = ent.getLocationId();
   String th = ent.getThreadName();
   DylockLockRunData rd = getRunData(lock,ent.getTime());
   switch (ent.getEntryType()) {
      case UNLOCK :
	 if (isLock(lid)) {
	    rd.doneP(ent.getTime(),th,ds);
	  }
	 else if (isUnlock(lid)) {
	    rd.doneV(ent.getTime(),th,ds);
	  }
	 break;
      case WAIT :
	 if (isLock(lid)) {
	    rd.addWaiting(ent.getTime(),th,ds,VIEW_TYPE_R);
	  }
	 break;
      case WAITED :
      case WAITTIME :
	 if (isLock(lid)) {
	    rd.doneWaiting(ent.getTime(),th,ds);
	  }
	 break;
      default :
	 break;
    }
}

private boolean isLock(int lid)
{
   return isInLockSet(lid,lock_set);
}

private boolean isUnlock(int lid)
{
   return isInLockSet(lid,unlock_set);
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
	 if (li.isNormalWait())
            li.changeWait(-1);
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
	 if (li.isNormalWait()) ++ctr;
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
   private int		wait_count;

   LockInfo(DylockLockData ld,DylockLockDataManager bldr) {
      double lw = ld.getWaitTime();
      double tw = bldr.getMaxTime();
      normal_wait = lw >= tw/2.0;
      wait_count = 0;
    }

   boolean isNormalWait()			{ return normal_wait; }

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



}	// end of class DylockViewSemaphoreType




/* end of DylockViewSemaphoreType.java */
