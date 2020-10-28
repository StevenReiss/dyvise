/********************************************************************************/
/*										*/
/*		DylockCheckerSemaphore.java					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylock/DylockCheckerSemaphore.java,v 1.4 2013-05-09 12:28:59 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylockCheckerSemaphore.java,v $
 * Revision 1.4  2013-05-09 12:28:59  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.3  2012-10-05 00:52:41  spr
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


class DylockCheckerSemaphore extends DylockCheckerBase implements DylockConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private List<SemaphoreCheck> to_check;

private static boolean	     do_debug = false;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DylockCheckerSemaphore(TraceLockData ld)
{
   Collection<TraceLockLocation> allps = new HashSet<TraceLockLocation>();
   Collection<TraceLockLocation> allvs = new HashSet<TraceLockLocation>();

   to_check = new ArrayList<SemaphoreCheck>();

   for (TraceLockLocation ll : ld.getLockLocations()) {
      if (ll.doesWait()) allps.add(ll);
      if (ll.doesNotify()) allvs.add(ll);
    }

   if (allps.isEmpty() || allvs.isEmpty()) return;

   Collection<Collection<TraceLockLocation>> powerps = powerSet(allps);
   Collection<Collection<TraceLockLocation>> powervs = powerSet(allvs);

   for (Collection<TraceLockLocation> ps : powerps) {
      if (ps.size() > 0) {
	 for (Collection<TraceLockLocation> vs : powervs) {
	    if (vs.size() > 0) {
	       SemaphoreCheck sc = new SemaphoreCheck(ps,vs);
	       to_check.add(sc);
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
   for (Iterator<SemaphoreCheck> it = to_check.iterator(); it.hasNext(); ) {
      SemaphoreCheck sc = it.next();
      if (do_debug) System.err.println("WORK ON " + sc);
      if (!sc.isConsistent(seq)) it.remove();
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
   for (Iterator<SemaphoreCheck> it = to_check.iterator(); it.hasNext(); ) {
      SemaphoreCheck sc = it.next();
      if (do_debug) System.err.println("VALIDATE " + sc);
      if (!sc.isValidated()) it.remove();
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
   for (SemaphoreCheck sc : to_check) {
      Collection<TraceLockLocation> locs = sc.getLocations();
      boolean overlap = false;
      for (TraceLockLocation tll : used) {
	 if (locs.contains(tll)) overlap = true;
       }
      if (!overlap) {
	 sc.generateReport(xw,db);
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
   String rslt = "SEMAPHORE:";
   for (SemaphoreCheck sc : to_check) rslt += " " + sc.toString();

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Subclass to hold potential semaphore elements				*/
/*										*/
/********************************************************************************/

private class SemaphoreCheck {

   private Set<TraceLockLocation> p_locations;
   private Set<TraceLockLocation> v_locations;

   private boolean	   have_wait;
   private boolean	   have_notify;
   private boolean	   count_valid;
   private int		   initial_count;

   SemaphoreCheck(Collection<TraceLockLocation> ps,Collection<TraceLockLocation> vs) {
      p_locations = new HashSet<TraceLockLocation>(ps);
      v_locations = new HashSet<TraceLockLocation>(vs);
      have_wait = false;
      have_notify = false;
      count_valid = true;
      initial_count = -1;
    }

   boolean isConsistent(List<TraceLockEntry> seq) {
      int count = 0;
      int waiting = 0;
      initial_count = -1;
      count_valid = true;
      Set<TraceLockLocation> vset = new HashSet<TraceLockLocation>();

      for (TraceLockEntry te : seq) {
	 TraceLockLocation tl = te.getLocation();
	
	 if (do_debug) {
	    System.err.println("SEMAPHORE: " + te.toString() + " " +
		  tl.doesWait() + " " +
		  tl.doesNotify() + " " +
		  count + " " + waiting + " " + initial_count + " " +
		  count_valid);
	 }
	
	 if (!count_valid) break;
	
	 if (p_locations.contains(tl)) {
	    switch (te.getEntryType()) {
	       case WAIT :
		  have_wait = true;
		  if (initial_count < 0) initial_count = -count;
		  if (initial_count + count > 0) count_valid = false;
		  ++waiting;
		  break;
	       case WAITED :
		  --waiting;
		  break;
	       case WAITTIME :
		  --waiting;
		  break;
	       case UNLOCK :
		  --count;
		  if (initial_count >= 0 && count == 0) {
		     if (do_debug && vset.size() == 1) {
			System.err.print("ASSOC " + tl + " WITH ");
			for (TraceLockLocation tt : vset) System.err.println(tt);
		     }
		     vset.clear();
		  }
		  if (initial_count >= 0 && initial_count + count < 0) count_valid = false;
		  break;
	       case NOTIFY :
		  count_valid = false;
		  break;
	       default :
		  break;
	    }
	 }
	 else if (v_locations.contains(tl)) {
	    switch (te.getEntryType()) {
	       case ENTERED :
		  ++count;
		  vset.add(tl);
		  break;
	       case WAIT :
	       case WAITED :
	       case WAITTIME :
		  count_valid = false;
		  break;
	       case NOTIFY :
		  have_notify = true;
		  break;
	       default :
		  break;
	    }
	 }
      }

      if (!count_valid) return false;

      return true;
   }

   boolean isValidated() {
      return initial_count >= 0 && have_wait && have_notify && count_valid;
    }

   Collection<TraceLockLocation> getLocations() {
      Set<TraceLockLocation> locs = new HashSet<TraceLockLocation>();
      locs.addAll(p_locations);
      locs.addAll(v_locations);
      return locs;
    }

   void generateReport(IvyXmlWriter xw,DyviseDatabase db) {
      if (xw != null) {
         xw.begin("TYPE");
         xw.field("KIND","SEMAPHORE");
         xw.field("ID",++item_counter);
         xw.field("COUNT",initial_count);
         xw.begin("LOCKSET");
         for (TraceLockLocation ll : p_locations) ll.outputXml(xw,false);
         xw.end("LOCKSET");
         xw.begin("UNLOCKSET");
         for (TraceLockLocation ll : v_locations) ll.outputXml(xw,false);
         xw.end("UNLOCKSET");
         xw.end("TYPE");
       }
    }

   @Override public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("SEMAPHORE ");
      buf.append(initial_count);
      buf.append("\n P:");
      for (TraceLockLocation ll : p_locations) buf.append(" " + ll.toString());
      buf.append("\n V:");
      for (TraceLockLocation ll : v_locations) buf.append(" " + ll.toString());
      buf.append("\n");
      return buf.toString();
    }

}	// end of inner class SemaphoreCheck



}	// end of class DylockCheckerSemaphore




/* end of DylockCheckerSemaphore.java */

