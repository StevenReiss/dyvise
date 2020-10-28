/********************************************************************************/
/*										*/
/*		DylockCheckerDelay.java 					*/
/*										*/
/*	DYVISE lock analysis checker for a lock used only for delays		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylock/DylockCheckerDelay.java,v 1.3 2013-05-09 12:28:59 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylockCheckerDelay.java,v $
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


class DylockCheckerDelay extends DylockCheckerBase implements DylockConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<TraceLockLocation,Integer> wait_count;
private Set<TraceLockLocation>	locations_used;
private Set<TraceLockLocation>	bad_locations;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DylockCheckerDelay()
{
   wait_count = new HashMap<TraceLockLocation,Integer>();
   locations_used = new HashSet<TraceLockLocation>();
   bad_locations = new HashSet<TraceLockLocation>();
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
	    TraceLockLocation tll = te.getLocation();
	    if (!bad_locations.contains(tll)) {
	       if (wait_count.containsKey(tll)) {
		  int ct = wait_count.get(tll);
		  wait_count.put(tll,ct+1);
		}
	       else wait_count.put(tll,1);
	     }
	    break;
	 case WAITED :
	    bad_locations.add(te.getLocation());
	    break;
	 case WAITTIME :
	    tll = te.getLocation();
	    if (wait_count.containsKey(tll) && !bad_locations.contains(tll)) {
	       int ct = wait_count.get(tll);
	       wait_count.put(tll,ct-1);
	       locations_used.add(tll);
	     }
	    break;
	 case NOTIFY :
	    bad_locations.add(te.getLocation());
	    break;
	 case ENTERED :
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
   for (TraceLockLocation tll : bad_locations) {
      locations_used.remove(tll);
    }

   if (locations_used.isEmpty()) return false;

   return true;
}



/********************************************************************************/
/*										*/
/*	Reporting methods							*/
/*										*/
/********************************************************************************/

@Override public void generateReport(IvyXmlWriter xw,DyviseDatabase db,List<TraceLockLocation> used)
{
   Set<TraceLockLocation> rslt = new HashSet<TraceLockLocation>();
   for (TraceLockLocation tll : locations_used) {
      if (!used.contains(tll)) rslt.add(tll);
    }
   if (rslt.isEmpty()) return;

   if (xw != null) {
      xw.begin("TYPE");
      xw.field("KIND","DELAY");
      xw.field("ID",++item_counter);
      for (TraceLockLocation tll : rslt) {
	 tll.outputXml(xw,false);
       }
      xw.end("TYPE");
    }

   used.addAll(rslt);
}




/********************************************************************************/
/*										*/
/*	Debugging methods							*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   return "DELAY";
}




}	// end of class DylockCheckerDelay




/* end of DylockCheckerDelay.java */
