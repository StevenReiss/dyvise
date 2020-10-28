/********************************************************************************/
/*										*/
/*		DylockViewMutexType.java					*/
/*										*/
/*	Mutex view type handling						*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylock/DylockViewMutexType.java,v 1.3 2013/09/04 18:36:28 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylockViewMutexType.java,v $
 * Revision 1.3  2013/09/04 18:36:28  spr
 * Minor bug fixes.
 *
 * Revision 1.2  2013-05-09 12:29:01  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.1  2012-10-05 00:52:42  spr
 * Update lock analysis.  Prepare for pattern display.
 *
 *
 ********************************************************************************/





package edu.brown.cs.dyvise.dylock;

import edu.brown.cs.dyvise.dystore.*;
import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.Element;

import java.util.*;


class DylockViewMutexType extends DylockViewType implements DylockConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private Map<DylockLockData,LockInfo> lock_level;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DylockViewMutexType(DylockViewRef dv,Element e)
 {
   super(dv,e);
   for (Element le : IvyXml.children(e,"LOCATION")) {
      DylockLockLocation vl = getLocation(le);
      addLocation(vl);
    }
   lock_level = new HashMap<DylockLockData,LockInfo>();
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getShortString() 		{ return "MUTEX"; }

void addToolTip(StringBuffer buf)
{
   outputTable("MUTEX",getLocations(),buf);
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

void processRunEntry(RunningEntry ent,String lock,DystoreStore ds)
{
   String th = ent.getThreadName();
   DylockLockRunData rd = getRunData(lock,ent.getTime());
   switch (ent.getEntryType()) {
      case ENTER :
	 rd.addWaiting(ent.getTime(),th,ds,VIEW_TYPE_MUTEX);
	 break;
      case ENTERED :
	 rd.doneWaiting(ent.getTime(),th,ds);
	 rd.acquireLock(ent.getTime(),th,ds,VIEW_TYPE_MUTEX);
	 break;
      case UNLOCK :
	 rd.releaseLock(ent.getTime(),th,ds,VIEW_TYPE_MUTEX);
	 break;
      case RESET :
	 rd.clear();
	 break;
    }
}



/********************************************************************************/
/*										*/
/*	Pattern processing methods						*/
/*										*/
/********************************************************************************/

void processPatternEntry(DylockEventSetBuilder bldr,DylockLockEntry ent,DylockLockData ld,boolean valid)
{
   DylockPatternEvent pevt = null;
   LockInfo li = lock_level.get(ld);
   if (li == null) {
      li = new LockInfo();
      lock_level.put(ld,li);
    }
   if (ld.getLockId() == 36) {
      // System.err.println("TRACECHECK " + ent + " " + li.getTotalCount());
    }

   switch (ent.getEntryType()) {
      case ENTER :
	 if (ent.getNestedDepth() == 0 && valid) {
	    li.noteEnter(ent.getThread());
	    pevt = new DylockPatternEvent(ent,this,PatternEventType.ENTER,li.getTotalCount());
	  }
	 break;
      case ENTERED :
	 if (ent.getNestedDepth() == 1 && valid) {
	    pevt = new DylockPatternEvent(ent,this,PatternEventType.ENTERED,li.getTotalCount());
	  }
	 break;
      case UNLOCK :
	 if (ent.getNestedDepth() == 0 && valid) {
	    li.noteExit(ent.getThread());
	    pevt = new DylockPatternEvent(ent,this,PatternEventType.EXIT,li.getTotalCount());
	  }
	 break;
      case RESET :
	 lock_level.remove(ld);
	 break;
      default :
	 break;
    }

   if (pevt != null) {
      bldr.addEvent(pevt);
    }
}



private class LockInfo {

   private Map<DylockThreadData,Integer> thread_counts;
   private int total_count;

   LockInfo() {
      thread_counts = new HashMap<DylockThreadData,Integer>();
      total_count = 0;
    }

    void noteEnter(DylockThreadData td) {
       Integer v = thread_counts.get(td);
       if (v == null) thread_counts.put(td,1);
       else thread_counts.put(td,v+1);
       ++total_count;
     }

    void noteExit(DylockThreadData td) {
       Integer v = thread_counts.get(td);
       if (v == null || v == 0) return;
       thread_counts.put(td,v-1);
       --total_count;
     }

    int getTotalCount() 		{ return total_count; }

}	// end of class LockInfo




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void localOutputXml(IvyXmlWriter xw)
{
   xw.field("KIND","MUTEX");
   for (TraceLockLocation ll : getLocations()) {
      ll.outputXml(xw,false);
    }
}




}	// end of class DylockViewMutexType




/* end of DylockViewMutexType.java */
