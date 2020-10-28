/********************************************************************************/
/*										*/
/*		DylockCheckerLatch.java 					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylock/DylockCheckerLatch.java,v 1.3 2013-05-09 12:28:59 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylockCheckerLatch.java,v $
 * Revision 1.3  2013-05-09 12:28:59  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.2  2012-10-05 00:52:40  spr
 * Update lock analysis.  Prepare for pattern display.
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


class DylockCheckerLatch extends DylockCheckerBase implements DylockConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private List<LatchCheck> to_check;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DylockCheckerLatch(TraceLockData ld)
{
   Collection<TraceLockLocation> allps = new HashSet<TraceLockLocation>();
   Collection<TraceLockLocation> allvs = new HashSet<TraceLockLocation>();

   to_check = new ArrayList<LatchCheck>();

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
	       LatchCheck sc = new LatchCheck(ps,vs);
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
   for (Iterator<LatchCheck> it = to_check.iterator(); it.hasNext(); ) {
      LatchCheck sc = it.next();
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
   for (Iterator<LatchCheck> it = to_check.iterator(); it.hasNext(); ) {
      LatchCheck sc = it.next();
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
   for (LatchCheck lc : to_check) {
      Collection<TraceLockLocation> locs = lc.getLocations();
      boolean overlap = false;
      for (TraceLockLocation tll : used) {
	 if (locs.contains(tll)) overlap = true;
       }
      if (!overlap) {
	 lc.generateReport(xw,db);
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
   String rslt = "LATCH:";
   for (LatchCheck sc : to_check) rslt += " " + sc.toString();

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Subclass to hold potential semaphore elements				*/
/*										*/
/********************************************************************************/

private class LatchCheck {

   private Set<TraceLockLocation> p_locations;
   private Set<TraceLockLocation> v_locations;

   private boolean	   have_wait;
   private boolean	   have_notify;
   private int		   exit_count;
   private int		   latch_count;
   private boolean	   latch_valid;

   LatchCheck(Collection<TraceLockLocation> ps,Collection<TraceLockLocation> vs) {
      p_locations = new HashSet<TraceLockLocation>(ps);
      v_locations = new HashSet<TraceLockLocation>(vs);
      have_wait = false;
      have_notify = false;
      exit_count = 0;
      latch_count = 0;
      latch_valid = true;
    }

   boolean isConsistent(List<TraceLockEntry> seq) {
      boolean latchset = true;

      for (TraceLockEntry te : seq) {
	 TraceLockLocation tl = te.getLocation();
	
	 if (!latch_valid) break;
	
	 if (p_locations.contains(tl)) {
	    switch (te.getEntryType()) {
	       case WAIT :
		  have_wait = true;
		  if (!latchset) latch_valid = false;
		  break;
	       case UNLOCK :
		  if (latchset) latch_valid = false;
		  ++exit_count;
		  break;
	       default :
		  break;
	    }
	 }
	 else if (v_locations.contains(tl)) {
	    switch (te.getEntryType()) {
	       case NOTIFY :
		  have_notify = true;
		  latchset = false;
		  ++latch_count;
		  break;
	       default :
		  break;
	    }
	 }
      }

      if (!latch_valid) return false;

      return true;
   }

   boolean isValidated() {
      return have_wait && have_notify && exit_count > 1.5*latch_count;
    }

   Set<TraceLockLocation> getLocations() {
      Set<TraceLockLocation> rslt = new HashSet<TraceLockLocation>();
      rslt.addAll(p_locations);
      rslt.addAll(v_locations);
      return rslt;
    }

   void generateReport(IvyXmlWriter xw,DyviseDatabase db) {
      if (xw != null) {
         xw.begin("TYPE");
         xw.field("KIND","LATCH");
         xw.field("ID",++item_counter);
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
      buf.append("LATCH");
      buf.append("\n WAIT:");
      for (TraceLockLocation ll : p_locations) buf.append(" " + ll.toString());
      buf.append("\n UNLATCH:");
      for (TraceLockLocation ll : v_locations) buf.append(" " + ll.toString());
      buf.append("\n");
      return buf.toString();
    }

}	// end of inner class LatchCheck



}	// end of class DylockCheckerLatch




/* end of DylockCheckerLatch.java */

