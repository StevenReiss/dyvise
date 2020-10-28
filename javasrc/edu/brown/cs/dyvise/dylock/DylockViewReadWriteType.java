/********************************************************************************/
/*										*/
/*		DylockViewReadWriteType.java					*/
/*										*/
/*	View implementation for read-write locks				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylock/DylockViewReadWriteType.java,v 1.2 2013-05-09 12:29:01 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylockViewReadWriteType.java,v $
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



class DylockViewReadWriteType extends DylockViewType implements DylockConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Set<TraceLockLocation> read_lock;
private Set<TraceLockLocation> read_unlock;
private Set<TraceLockLocation> write_lock;
private Set<TraceLockLocation> write_unlock;
private Map<DylockLockData,RwLock> pattern_locks;



/********************************************************************************/
/*										*/
/*	Constructores								*/
/*										*/
/********************************************************************************/

DylockViewReadWriteType(DylockViewRef dv,Element xe)
{
   super(dv,xe);
   read_lock = new TreeSet<TraceLockLocation>();
   write_lock = new TreeSet<TraceLockLocation>();
   read_unlock = new TreeSet<TraceLockLocation>();
   write_unlock = new TreeSet<TraceLockLocation>();
   pattern_locks = new HashMap<DylockLockData,RwLock>();

   for (Element le : IvyXml.children(IvyXml.getChild(xe,"READLOCKS"),"LOCATION")) {
      DylockLockLocation vl = getLocation(le);
      read_lock.add(vl);
      addLocation(vl);
    }
   for (Element le : IvyXml.children(IvyXml.getChild(xe,"WRITELOCKS"),"LOCATION")) {
      DylockLockLocation vl = getLocation(le);
      write_lock.add(vl);
      addLocation(vl);
    }
   for (Element le : IvyXml.children(IvyXml.getChild(xe,"READUNLOCKS"),"LOCATION")) {
      DylockLockLocation vl = getLocation(le);
      read_unlock.add(vl);
      addLocation(vl);
    }
   for (Element le : IvyXml.children(IvyXml.getChild(xe,"WRITEUNLOCKS"),"LOCATION")) {
      DylockLockLocation vl = getLocation(le);
      write_unlock.add(vl);
      addLocation(vl);
    }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getShortString() 		{ return "READ-WRITE"; }

void addToolTip(StringBuffer buf)
{
   outputTableHeader("READ-WRITE",buf);
   outputRowStart(buf);
   outputTable("Read Locks",read_lock,buf);
   outputRowEnd(buf);
   outputRowStart(buf);
   outputTable("Read Unlocks",read_unlock,buf);
   outputRowEnd(buf);
   outputRowStart(buf);
   outputTable("Write Locks",write_lock,buf);
   outputRowEnd(buf);
   outputRowStart(buf);
   outputTable("Write Unlocks",write_unlock,buf);
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
   xw.field("KIND","READ-WRITE");
   xw.begin("READLOCKS");
   for (TraceLockLocation ll : read_lock) ll.outputXml(xw,false);
   xw.end("READLOCKS");
   xw.begin("WRITELOCKS");
   for (TraceLockLocation ll : write_lock) ll.outputXml(xw,false);
   xw.end("WRITELOCKS");
   xw.begin("READUNLOCKS");
   for (TraceLockLocation ll : read_unlock) ll.outputXml(xw,false);
   xw.end("READUNLOCKS");
   xw.begin("WRITEUNLOCKS");
   for (TraceLockLocation ll : write_unlock) ll.outputXml(xw,false);
   xw.end("WRITEUNLOCKS");
}



/********************************************************************************/
/*										*/
/*	Pattern events								*/
/*										*/
/********************************************************************************/

@Override boolean startsValidRegion(DylockLockDataManager mgr,DylockLockEntry ent)
{
   DylockLockData ld = ent.getLock();
   RwLock rwl = pattern_locks.get(ld);
   if (rwl == null) return false;
   int ctr = rwl.getCounter();
   return ctr == 0;
}



@Override void processPatternEntry(DylockEventSetBuilder bldr,DylockLockEntry ent,
      DylockLockData ld,boolean valid)
{
   RwLock rwl = pattern_locks.get(ld);
   if (rwl == null) {
      rwl = new RwLock();
      pattern_locks.put(ld,rwl);
    }

   boolean isconc = false;
   String cnm = ld.getClassString();
   if (cnm.contains("java/util/concurrent/ReadWrite")) isconc = true;

   DylockPatternEvent pevt = null;
   PatternEventType etyp = null;

   TraceLockLocation location = ent.getLocation();
   if ((ent.getEntryType() == TraceEntryType.UNLOCK && !isconc) ||
	 (ent.getEntryType() == TraceEntryType.ENTERED && isconc)) {
      if (read_lock.contains(location)) {
	 rwl.readLock();
	 etyp = PatternEventType.READLOCK;
       }
      else if (read_unlock.contains(location)) {
	 rwl.readUnlock();
	 etyp = PatternEventType.READUNLOCK;
       }
      else if (write_lock.contains(location)) {
	 rwl.writeLock();
	 etyp = PatternEventType.WRITELOCK;
       }
      else if (write_unlock.contains(location)) {
	 rwl.writeUnlock();
	 etyp = PatternEventType.WRITEUNLOCK;
       }
    }
   else if (ent.getEntryType() == TraceEntryType.WAIT) {
      if (read_lock.contains(location) || write_lock.contains(location)) {
	 etyp = PatternEventType.WAIT;
       }
    }
   else if (ent.getEntryType() == TraceEntryType.WAITED) {
      if (read_lock.contains(location) || write_lock.contains(location)) {
	 etyp = PatternEventType.WAITED;
       }
    }
   else if (ent.getEntryType() == TraceEntryType.NOTIFY) {
      if (read_lock.contains(location) || write_lock.contains(location)) {
	 etyp = PatternEventType.NOTIFY;
       }
    }
   else if (ent.getEntryType() == TraceEntryType.RESET) {
      rwl.reset();
    }
   if (etyp == null) return;
   int ctr = rwl.getCounter();
   if (ctr < 0) return;
   pevt = new DylockPatternEvent(ent,this,etyp,ctr);
   bldr.addEvent(pevt);
}



private class RwLock {

   private int read_count;
   private int write_count;
   private boolean read_valid;
   private boolean write_valid;

   RwLock() {
      read_count = write_count = 0;
      read_valid = false;
      write_valid = false;
    }

   void readLock() {
      if (!write_valid) {
	 write_valid = true;
	 write_count = 0;
       }
      if (read_valid) ++read_count;
    }
   void readUnlock() {
      if (read_valid && read_count > 0) --read_count;
    }
   void writeLock() {
      if (!read_valid) {
	 read_valid = true;
	 read_count = 0;
       }
      if (write_valid) ++write_count;
    }
   void writeUnlock() {
      if (write_valid && write_count > 0) --write_count;
    }
   void reset() {
      read_count = write_count = 0;
      read_valid = write_valid = false;
    }

   int getCounter() {
      if (!read_valid || !write_valid) return -1;
      return read_count + write_count;
    }

}	// end of inner class RwLock



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
	 if (isInLockSet(lid,read_lock)) {
	    rd.acquireLock(ent.getTime(),th,ds,VIEW_TYPE_R);
	  }
	 else if (isInLockSet(lid,read_unlock)) {
	    rd.releaseLock(ent.getTime(),th,ds,VIEW_TYPE_R);
	  }
	 else if (isInLockSet(lid,write_lock)) {
	    rd.acquireLock(ent.getTime(),th,ds,VIEW_TYPE_W);
	  }
	 else if (isInLockSet(lid,write_unlock)) {
	    rd.releaseLock(ent.getTime(),th,ds,VIEW_TYPE_W);
	  }
	 break;
      case WAIT :
	 if (isInLockSet(lid,read_lock)) {
	    rd.addWaiting(ent.getTime(),th,ds,VIEW_TYPE_R);
	  }
	 else if (isInLockSet(lid,write_lock)) {
	    rd.addWaiting(ent.getTime(),th,ds,VIEW_TYPE_W);
	  }
	 break;
      case WAITED :
      case WAITTIME :
	 if (isInLockSet(lid,read_lock)) {
	    rd.doneWaiting(ent.getTime(),th,ds);
	  }
	 else if (isInLockSet(lid,write_lock)) {
	    rd.doneWaiting(ent.getTime(),th,ds);
	  }
	 break;
      default :
	 break;
    }
}



}	// end of class DylockViewReadWriteType




/* end of DylockViewReadWriteType.java */
















