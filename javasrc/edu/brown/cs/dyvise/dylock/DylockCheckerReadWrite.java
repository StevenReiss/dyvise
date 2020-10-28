/********************************************************************************/
/*										*/
/*		DylockCheckerReadWrite.java					*/
/*										*/
/*	DYVISE lock analysis checker for a semaphore				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylock/DylockCheckerReadWrite.java,v 1.4 2013-05-09 12:28:59 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylockCheckerReadWrite.java,v $
 * Revision 1.4  2013-05-09 12:28:59  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.3  2012-10-05 00:52:40  spr
 * Update lock analysis.  Prepare for pattern display.
 *
 * Revision 1.2  2011-04-18 19:24:29  spr
 * Bug fixes in dylock.
 *
 * Revision 1.1  2011-03-10 02:24:59  spr
 * New version of dylock to produce visualizations.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dylock;

import edu.brown.cs.dyvise.dyvise.*;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.util.*;


class DylockCheckerReadWrite extends DylockCheckerBase implements DylockConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private List<ReadWriteCheck> to_check;

private static boolean	do_debug = false;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DylockCheckerReadWrite(TraceLockData ld)
{
   Collection<TraceLockLocation> alllock = new HashSet<TraceLockLocation>();
   Collection<TraceLockLocation> allunlock = new HashSet<TraceLockLocation>();

   to_check = new ArrayList<ReadWriteCheck>();

   for (TraceLockLocation ll : ld.getLockLocations()) {
      if (ll == null)
	 continue;
      if (ll.doesWait()) alllock.add(ll);
      if (ll.doesNotify()) allunlock.add(ll);
    }

   if (alllock.size() < 2 || allunlock.size() < 1) return;

   Collection<Collection<TraceLockLocation>> powerlk = powerSet(alllock);
   Collection<Collection<TraceLockLocation>> powerun = powerSet(allunlock);

   for (Collection<TraceLockLocation> ws : powerlk) {
      if (ws.size() > 0) {
	 for (Collection<TraceLockLocation> rs : powerlk) {
	    if (rs.size() > 0 && Collections.disjoint(ws,rs)) {
	       for (Collection<TraceLockLocation> wu : powerun) {
		  if (wu.size() > 0) {
		     for (Collection<TraceLockLocation> ru : powerun) {
			if (ru.size() > 0) {
			   ReadWriteCheck rwc = new ReadWriteCheck(ws,rs,wu,ru);
			   to_check.add(rwc);
			 }
		      }
		   }
		}
	     }
	  }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Consistency checking methods						*/
/*										*/
/********************************************************************************/

@Override public boolean isConsistent(List<TraceLockEntry> seq)
{
   for (Iterator<ReadWriteCheck> it = to_check.iterator(); it.hasNext(); ) {
      ReadWriteCheck sc = it.next();
      if (do_debug) System.err.println("WORK ON " + sc.hashCode() + " " + sc);
      if (!sc.isConsistent(seq)) {
	 if (do_debug) System.err.println("REMOVE " + sc.hashCode() + " " + sc);
	 it.remove();
       }
    }

   return to_check.size() > 0;
}




/********************************************************************************/
/*										*/
/*	Validity testing methods						*/
/*										*/
/********************************************************************************/

@Override public boolean isValidated()
{
   for (Iterator<ReadWriteCheck> it = to_check.iterator(); it.hasNext(); ) {
      ReadWriteCheck sc = it.next();
      if (!sc.isValidated()) {
	 if (do_debug) System.err.println("NOT VALID " + sc.hashCode() + " " + sc);
	 it.remove();
       }
    }

   for (Iterator<ReadWriteCheck> it = to_check.iterator(); it.hasNext(); ) {
      ReadWriteCheck rc1 = it.next();
      boolean elim = false;
      for (ReadWriteCheck rc2 : to_check) {
	 if (rc1 == rc2) continue;
	 if (rc2.betterThan(rc1)) elim = true;
       }
      if (elim) it.remove();
    }

   return to_check.size() > 0;
}




/********************************************************************************/
/*										*/
/*	Reporting methods							*/
/*										*/
/********************************************************************************/

@Override public void generateReport(IvyXmlWriter xw,DyviseDatabase db,List<TraceLockLocation> used)
{
   for (ReadWriteCheck rc : to_check) {
      Collection<TraceLockLocation> locs = rc.getLocations();
      boolean overlap = false;
      for (TraceLockLocation tll : used) {
	 if (locs.contains(tll)) overlap = true;
       }
      if (!overlap) {
	 rc.generateReport(xw,db);
	 used.addAll(locs);
       }
    }
}




/********************************************************************************/
/*										*/
/*	Debugging methods							*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   String rslt = "READ-WRITE:";
   for (ReadWriteCheck sc : to_check) rslt += " " + sc.toString();

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Subclass to hold potential semaphore elements				*/
/*										*/
/********************************************************************************/

private class ReadWriteCheck {

   private Set<TraceLockLocation> write_locks;
   private Set<TraceLockLocation> read_locks;
   private Set<TraceLockLocation> write_unlocks;
   private Set<TraceLockLocation> read_unlocks;

   private boolean		have_writewait;
   private boolean		have_readwait;
   private boolean		lock_valid;

   ReadWriteCheck(Collection<TraceLockLocation> wl,Collection<TraceLockLocation> rl,
		     Collection<TraceLockLocation> wu,Collection<TraceLockLocation> ru) {
      write_locks = new HashSet<TraceLockLocation>(wl);
      read_locks = new HashSet<TraceLockLocation>(rl);
      write_unlocks = new HashSet<TraceLockLocation>(wu);
      read_unlocks = new HashSet<TraceLockLocation>(ru);
      have_writewait = false;
      have_readwait = false;
      lock_valid = true;
    }

   boolean betterThan(ReadWriteCheck rc) {
      if (!write_locks.equals(rc.write_locks)) return false;
      if (!read_locks.equals(rc.read_locks)) return false;
      for (TraceLockLocation tll : read_unlocks) {
	 if (!rc.read_unlocks.contains(tll))
	    return false;
       }
      for (TraceLockLocation tll : write_unlocks) {
	 if (!rc.write_unlocks.contains(tll))
	    return false;
       }
      return true;
    }

   boolean isConsistent(List<TraceLockEntry> seq) {
      int wthread = -1;
      int wcount = 0;
      boolean lok = false;
      Set<Integer> rset = new HashSet<Integer>();
      Map<Integer,Integer> rthreads = new HashMap<Integer,Integer>();
      int firstwrite = -1;

      int ct = 0;
      for (TraceLockEntry te : seq) {
	 TraceLockLocation tl = te.getLocation();
	 if (do_debug) {
	    System.err.println("READWRITE: " + te.toString() + " " + (ct++) + " " +
		  tl.doesWait() + " " +
		  tl.doesNotify() + " " + wthread + " " + wcount + " " + rthreads.size() + " " +
		  lok + " " + lock_valid);
	 }
	 if (!lock_valid) break;

	 if (te.getEntryType() == TraceEntryType.RESET) {
	    lok = false;
	    rset.clear();
	    wthread = -1;
	    wcount = 0;
	    rthreads.clear();
	    continue;
	 }

	 if (write_locks.contains(tl)) {
	    switch (te.getEntryType()) {
	       default :
		  break;
	       case WAIT :
		  if (lok) {
		     have_writewait = true;
		     if (rthreads.isEmpty() && (wthread < 0 || wthread == te.getThreadId()))
			lock_valid = false;
		  }
		  break;
	       case UNLOCK :
		  if (!lok) {
		     rthreads.remove(te.getThreadId());
		     if (firstwrite < 0) firstwrite = te.getThreadId();
		     if (firstwrite != te.getThreadId()) lok = true;
		     int rsz = rset.size();
		     if (rset.contains(te.getThreadId())) --rsz;
		     if (rsz > 0) lok = true;
		  }
		  if (!rthreads.isEmpty()) lock_valid = false;
		  if (wthread < 0) {
		     wthread = te.getThreadId();
		     wcount = 1;
		  }
		  else if (wthread == te.getThreadId()) {
		     wcount += 1;
		  }
		  else lock_valid = false;
		  break;
	    }
	 }
	 else if (read_locks.contains(tl)) {
	    switch (te.getEntryType()) {
	       default :
		  break;
	       case WAIT :
		  if (lok) {
		     have_readwait = true;
		     if (wthread < 0 || wthread == te.getThreadId()) lock_valid = false;
		  }
		  break;
	       case UNLOCK :
		  if (wthread >= 0) {
		     if (wthread == te.getThreadId()) ++wcount;
		     else lock_valid = false;
		  }
		  else {
		     Integer rv = rthreads.get(te.getThreadId());
		     if (rv == null) rthreads.put(te.getThreadId(),1);
		     else rthreads.put(te.getThreadId(),rv + 1);
		     rset.add(te.getThreadId());
		  }
		  break;
	    }
	 }
	 else if (wthread >= 0 && write_unlocks.contains(tl)) {
	    switch (te.getEntryType()) {
	       default :
		  break;
	       case UNLOCK :
		  if (wthread != te.getThreadId()) {
		     if (wthread >= 0 || lok) lock_valid = false;
		     else break;
		  }
		  if (--wcount == 0) {
		     wthread = -1;
		  }
		  break;
	    }
	 }
	 else if (read_unlocks.contains(tl)) {
	    switch (te.getEntryType()) {
	       default :
		  break;
	       case UNLOCK :
		  if (wthread >= 0 && wthread == te.getThreadId()) {
		     --wcount;
		     if (wcount == 0) lock_valid = false;
		     continue;
		  }
		  if (wthread >= 0) lock_valid = false;
		  Integer rv = rthreads.get(te.getThreadId());
		  if (rv == null) {
		     if (lok) lock_valid = false;
		  }
		  else if (rv == 1) rthreads.remove(te.getThreadId());
		  else rthreads.put(te.getThreadId(),rv - 1);
		  break;
	    }
	 }
      }

      if (!lock_valid) return false;

      return true;
   }

   boolean isValidated() {
      if (do_debug) {
	 System.err.println("VALIDATE " + have_writewait + " " + have_readwait + " " +
			       lock_valid + " " + hashCode() + " " + toString());
       }
      return have_writewait && have_readwait && lock_valid;
    }

   Collection<TraceLockLocation> getLocations() {
      Set<TraceLockLocation> rslt = new HashSet<TraceLockLocation>();
      rslt.addAll(read_locks);
      rslt.addAll(write_locks);
      rslt.addAll(read_unlocks);
      rslt.addAll(write_unlocks);
      return rslt;
    }

   void generateReport(IvyXmlWriter xw,DyviseDatabase db) {
      if (xw != null) {
         xw.begin("TYPE");
         xw.field("KIND","READ-WRITE");
         xw.field("ID",++item_counter);
         xw.begin("READLOCKS");
         for (TraceLockLocation tll : read_locks) tll.outputXml(xw,false);
         xw.end("READLOCKS");
         xw.begin("WRITELOCKS");
         for (TraceLockLocation tll : write_locks) tll.outputXml(xw,false);
         xw.end("WRITELOCKS");
         xw.begin("READUNLOCKS");
         for (TraceLockLocation tll : read_unlocks) tll.outputXml(xw,false);
         xw.end("READUNLOCKS");
         xw.begin("WRITEUNLOCKS");
         for (TraceLockLocation tll : write_unlocks) tll.outputXml(xw,false);
         xw.end("WRITEUNLOCKS");
         xw.end("TYPE");
       }
    }

   @Override public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("READWRITELOCK:");
      buf.append("\n   readLock:");
      for (TraceLockLocation ent : read_locks) buf.append(" " + ent.toString());
      buf.append("\n   writeLock:");
      for (TraceLockLocation ent : write_locks) buf.append(" " + ent.toString());
      buf.append("\n   readUNLock:");
      for (TraceLockLocation ent : read_unlocks) buf.append(" " +ent.toString());
      buf.append("\n   writeUNLock:");
      for (TraceLockLocation ent : write_unlocks) buf.append(" " + ent.toString());
      buf.append("\n");

      return buf.toString();
    }

}	// end of inner class ReadWriteCheck



}	// end of class DylockCheckerReadWrite




/* end of DylockCheckerReadWrite.java */

