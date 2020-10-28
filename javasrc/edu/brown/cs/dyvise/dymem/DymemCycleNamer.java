/********************************************************************************/
/*										*/
/*		DymemGraph.java 						*/
/*										*/
/*	Provide consistent names for cycles					*/
/*										*/
/********************************************************************************/
/*	Copyright 2005 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2005, Brown University, Providence, RI.				 *
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymem/DymemCycleNamer.java,v 1.2 2009-04-11 23:45:29 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymemCycleNamer.java,v $
 * Revision 1.2  2009-04-11 23:45:29  spr
 * Provide updated visualizations with time line, hashing, etc.
 *
 * Revision 1.1  2009-03-20 02:10:12  spr
 * Add memory graph computation and display.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymem;


import java.util.*;



class DymemCycleNamer implements DymemConstants {



/********************************************************************************/
/*										*/
/*	Local Storage								*/
/*										*/
/********************************************************************************/

private Collection<Cycle> cycle_set;

private static final double	MIN_SCORE = 0.5;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DymemCycleNamer()
{
   cycle_set = new ArrayList<Cycle>();
}



/********************************************************************************/
/*										*/
/*	Access method								*/
/*										*/
/********************************************************************************/

String getCycleName(List<GraphItem> itms)
{
   double bestscore = 0;
   Cycle bestcycle = null;
   for (Cycle c : cycle_set) {
      double s = c.score(itms);
      if (s > MIN_SCORE && s > bestscore) {
	 bestscore = s;
	 bestcycle = c;
       }
    }

   if (bestcycle != null) {
      bestcycle.merge(itms);
      return bestcycle.getName();
    }

   Cycle c = new Cycle(itms);
   cycle_set.add(c);

   return c.getName();
}




/********************************************************************************/
/*										*/
/*	Subclass to hold cycle information					*/
/*										*/
/********************************************************************************/

private class Cycle {

   private Map<String,CycleElement> element_set;
   private String cycle_name;

   Cycle(Collection<GraphItem> itms) {
      element_set = new HashMap<String,CycleElement>();
      for (GraphItem itm : itms) {
	 CycleElement ce = new CycleElement(itm);
	 element_set.put(itm.getName(),ce);
       }
      // TODO: Come up with a better name
      cycle_name = "CYCLE_" + (cycle_set.size() + 1);
    }

   String getName()			{ return cycle_name; }

   void merge(Collection<GraphItem> itms) {
      for (GraphItem itm : itms) {
	 CycleElement ce = element_set.get(itm.getName());
	 if (ce != null) ce.merge(itm);
	 else {
	    ce = new CycleElement(itm);
	    element_set.put(itm.getName(),ce);
	  }
       }
    }

   double score(Collection<GraphItem> itms) {
      double score = 0;
      double etot = 0;
      double tot = 0;

      for (CycleElement ce : element_set.values()) {
	 etot += ce.getSize() * ce.getSize();
       }
      etot = Math.sqrt(etot);

      for (GraphItem gi : itms) {
	 tot += gi.getSize() * gi.getSize();
	 CycleElement ce = element_set.get(gi.getName());
	 if (ce != null) {
	    score += gi.getSize() / etot * ce.getSize();
	  }
       }

      score /= Math.sqrt(tot);

      return score;
    }
}




private static class CycleElement {

   private long element_size;

   CycleElement(GraphItem gi) {
      element_size = gi.getSize();
    }

   void merge(GraphItem gi) {
      element_size += gi.getSize();
    }

   long getSize()			{ return element_size; }

}	// end of subclass CycleElement





}	// end of class DymemCycleNamer




/* end of DymemCycleNamer.java */
