/********************************************************************************/
/*										*/
/*		DylockCheckerCondition.java					*/
/*										*/
/*	DYVISE lock analysis checker for a condition variable of some sort	*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylock/DylockCheckerCondition.java,v 1.3 2013-05-09 12:28:59 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylockCheckerCondition.java,v $
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


class DylockCheckerCondition extends DylockCheckerBase implements DylockConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private boolean 	have_wait;
private boolean 	have_notify;
private Set<TraceLockLocation> wait_locations;
private Set<TraceLockLocation> notify_locations;

private static boolean do_debug = false;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DylockCheckerCondition()
{
   have_wait = false;
   have_notify = false;
   wait_locations = new HashSet<TraceLockLocation>();
   notify_locations = new HashSet<TraceLockLocation>();
}




/********************************************************************************/
/*										*/
/*	Consistency checking methods						*/
/*										*/
/********************************************************************************/

@Override public boolean isConsistent(List<TraceLockEntry> seq)
{
   for (TraceLockEntry te : seq) {
      switch (te.getEntryType()) {
	 case WAIT :
	    if (do_debug) System.err.println("HAVE WAIT " + te.getLocation());
	    wait_locations.add(te.getLocation());
	    have_wait = true;
	    break;
	 case WAITED :
	    if (do_debug) System.err.println("HAVE WAITED " + te.getLocation());
	    if (wait_locations.contains(te.getLocation())) {
	       have_notify = true;
	     }
	    break;
	 case NOTIFY :
	    if (do_debug) System.err.println("HAVE NOTIFY " + te.getLocation());
	    notify_locations.add(te.getLocation());
	    break;
	 default :
	    break;
       }
    }

   return true;
}




/********************************************************************************/
/*										*/
/*	Validity testing methods						*/
/*										*/
/********************************************************************************/

@Override public boolean isValidated()
{
   return have_wait && have_notify;
}




/********************************************************************************/
/*										*/
/*	Reporting methods							*/
/*										*/
/********************************************************************************/

@Override public void generateReport(IvyXmlWriter xw,DyviseDatabase db,List<TraceLockLocation> used)
{
   if (!have_wait || !have_notify) return;

   Set<TraceLockLocation> waits = new HashSet<TraceLockLocation>();
   Set<TraceLockLocation> nots = new HashSet<TraceLockLocation>();
   for (TraceLockLocation tll : wait_locations) {
      if (!used.contains(tll)) waits.add(tll);
    }
   for (TraceLockLocation tll : notify_locations) {
      if (!used.contains(tll)) nots.add(tll);
    }
   if (waits.isEmpty() || nots.isEmpty()) return;

   if (xw != null) {
      xw.begin("TYPE");
      xw.field("KIND","CONDITION");
      xw.field("ID",++item_counter);
      xw.begin("WAITS");
      for (TraceLockLocation tll : waits) tll.outputXml(xw,false);
      xw.end("WAITS");
      xw.begin("NOTIFYS");
      for (TraceLockLocation tll : nots) tll.outputXml(xw,false);
      xw.end("NOTIFYS");
      xw.end("TYPE");
      used.addAll(waits);
      used.addAll(nots);
    }
}




/********************************************************************************/
/*										*/
/*	Debugging methods							*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   return "CONDITION";
}




}	// end of class DylockCheckerCondition




/* end of DylockCheckerCondition.java */


