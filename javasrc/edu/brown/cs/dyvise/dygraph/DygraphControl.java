/********************************************************************************/
/*										*/
/*		DygraphControl.java						*/
/*										*/
/*	DYVISE graphics (visualization) controller implementation		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dygraph/DygraphControl.java,v 1.4 2013-05-09 12:28:56 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DygraphControl.java,v $
 * Revision 1.4  2013-05-09 12:28:56  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.3  2012-10-05 00:52:14  spr
 * Code cleanup
 *
 * Revision 1.2  2009-10-07 00:59:44  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.1  2009-09-19 00:08:37  spr
 * Module to draw various types of displays.  Only time rows implemented for now.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dygraph;

import edu.brown.cs.dyvise.dystore.DystoreControl;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.util.*;



public class DygraphControl implements DygraphConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private DystoreControl	tuple_store;
private List<DygraphViewImpl> our_views;
private Collection<TimeMark> time_marks;

private double		time_start;
private double		time_end;
private boolean 	time_dynamic;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public DygraphControl(Element mdl,DystoreControl store)
{
   tuple_store = store;

   our_views = new ArrayList<DygraphViewImpl>();
   for (Element e : IvyXml.children(mdl,"GRAPH")) {
      DygraphViewImpl dv = new DygraphViewImpl(this,e);
      our_views.add(dv);
    }

   time_marks = new ArrayList<TimeMark>();
   time_start = 0;
   time_end = 0;
   time_dynamic = true;

   updateTimes();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public List<DygraphView> getViews()
{
   return new ArrayList<DygraphView>(our_views);
}



public DystoreControl getTupleStore()		{ return tuple_store; }



/********************************************************************************/
/*										*/
/*	Data methods								*/
/*										*/
/********************************************************************************/

public void dataUpdated()
{
   updateTimes();

   for (DygraphViewImpl dv : our_views) {
      dv.updateDisplay();
    }
}


/********************************************************************************/
/*										*/
/*	Time mark methods							*/
/*										*/
/********************************************************************************/

public void clearTimeMarks()
{
   time_marks.clear();
}



public void addTimeMark(double when,String id)
{
   time_marks.add(new TimeMark(when,id));
}



String getMarks(double from,double to)
{
   String rslt = null;

   for (TimeMark tm : time_marks) {
      if (tm.inRange(from,to)) {
	 if (rslt == null) rslt = tm.getId();
	 else rslt = rslt + "," + tm.getId();
       }
    }

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Time scrolling methods							*/
/*										*/
/********************************************************************************/

public void setTimeWindow(double start,double end,boolean dynamic)
{
   time_start = start;
   time_end = end;
   time_dynamic = dynamic;
   if (dynamic) time_end = tuple_store.getEndTime();

   dataUpdated();
}


public double getDisplayStartTime()		{ return time_start; }
public double getDisplayEndTime() 		{ return time_end; }
public double getDisplayTimeDelta()               { return time_end - time_start + 1; }
public double getTimeAtDelta(double d) 
{
   return time_start + d;
}


private void updateTimes()
{
   double t0 = tuple_store.getStartTime();
   double t1 = tuple_store.getEndTime();

   if (time_start < t0) time_start = t0;
   if (time_dynamic) time_end = t1;
}




/********************************************************************************/
/*										*/
/*	Time Mark representation						*/
/*										*/
/********************************************************************************/

private static class TimeMark implements Comparable<TimeMark> {

   private double at_time;
   private String mark_id;

   TimeMark(double when,String id) {
      at_time = when;
      mark_id = id;
    }

   String getId()				{ return mark_id; }

   boolean inRange(double f,double t) {
      return (at_time >= f && at_time <= t);
    }

   public int compareTo(TimeMark m) {
      double v = at_time - m.at_time;
      if (v < 0) return -1;
      if (v > 0) return 1;
      return mark_id.compareTo(m.mark_id);
    }

}	// end of inner class TimeMark




}	// end of class DygraphControl




/* end of DygraphControl.java */
