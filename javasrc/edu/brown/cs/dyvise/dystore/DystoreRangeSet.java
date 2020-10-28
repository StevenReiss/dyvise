/********************************************************************************/
/*										*/
/*		DystoreRangeSet.java						*/
/*										*/
/*	DYVISE tuple store table for a range of tuples			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dystore/DystoreRangeSet.java,v 1.2 2013-05-09 12:29:03 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DystoreRangeSet.java,v $
 * Revision 1.2  2013-05-09 12:29:03  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.1  2011-03-19 20:35:13  spr
 * Add range set file.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dystore;

import java.util.*;


public class DystoreRangeSet implements Iterable<DystoreTuple>
{

/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private List<DystoreTuple>	tuple_data;
private DystoreField		time_field;
private double			next_time;
private double			last_check;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public DystoreRangeSet()
{
   init(null);
}


DystoreRangeSet(DystoreRangeSet rs)
{
   init(rs.tuple_data);
}


DystoreRangeSet(Collection<DystoreTuple> cs)
{
   init(cs);
}



public DystoreRangeSet(DystoreTuple dt)
{
   init(null);
   tuple_data.add(dt);
}


private void init(Collection<DystoreTuple> tups)
{
   if (tups == null) tuple_data = new LinkedList<DystoreTuple>();
   else tuple_data = new LinkedList<DystoreTuple>(tups);
   time_field = null;
   next_time = 0;
   last_check = 0;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public Iterator<DystoreTuple> iterator()	{ return tuple_data.iterator(); }


public void add(DystoreTuple dt)
{
   tuple_data.add(dt);

   if (time_field != null) {
      double t0 = dt.getTimeValue(time_field);
      if (next_time < 0 || t0 < next_time) next_time = t0;
    }
}


public void addAll(DystoreRangeSet rs)
{
   tuple_data.addAll(rs.tuple_data);
   time_field = null;
}


public int size()				{ return tuple_data.size(); }

public boolean contains(DystoreTuple dt)	{ return tuple_data.contains(dt); }


				

/********************************************************************************/
/*										*/
/*	Pruning methods 							*/
/*										*/
/********************************************************************************/

void removeBefore(double when,DystoreField fld)
{
   if (fld == time_field && next_time >= when || tuple_data.size() == 0) return;

   if (tuple_data.size() > 1000000) {
      System.err.println("TUPLE SET SIZE = " + tuple_data.size() + " " + when + " " + next_time +
			    " " + last_check);
      for (DystoreTuple dt : tuple_data) {
	 System.err.println("\t" + dt);
       }
    }

   time_field = fld;
   next_time = -1;
   for (Iterator<DystoreTuple> it = tuple_data.iterator(); it.hasNext(); ) {
      DystoreTuple dt = it.next();
      double t0 = dt.getTimeValue(fld);
      if (t0 < when) {
	 // System.err.println("DYSTORE: REMOVE " + t0 + " " + when + " " + dt);
	 it.remove();
       }
      else if (next_time < 0 || t0 < next_time) next_time = t0;
    }

   last_check = when;
}




}	// end of class DystoreRangeSet




/* end of DystoreRangeSet.java */
