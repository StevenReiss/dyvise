/********************************************************************************/
/*										*/
/*		DystoreTupleSet.java						*/
/*										*/
/*	DYVISE tuple store table for an ordered set of tuples			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dystore/DystoreTupleSet.java,v 1.4 2013-05-09 12:29:04 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DystoreTupleSet.java,v $
 * Revision 1.4  2013-05-09 12:29:04  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.3  2011-03-19 20:34:43  spr
 * Code speedup by adding range class.
 *
 * Revision 1.2  2010-03-30 16:23:05  spr
 * Make the store efficient enough to use with the display.
 *
 * Revision 1.1  2009-09-19 00:14:00  spr
 * In memory tuple store for visualization.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dystore;


import java.util.*;



class DystoreTupleSet implements DystoreConstants {




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private DystoreStoreImpl for_store;
private DystoreTable	 for_table;
private SortedSet<DystoreTuple> tuple_data;
private DystoreField start_field;
private DystoreField end_field;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DystoreTupleSet(DystoreStoreImpl ds)
{
   for_store = ds;
   for_table = ds.getTable();
   tuple_data = new TreeSet<DystoreTuple>(new TupleComparator());
   start_field = for_table.getStartTimeField();
   end_field = for_table.getEndTimeField();
}



/********************************************************************************/
/*										*/
/*	Methods to add tuples							*/
/*										*/
/********************************************************************************/

void addTuple(DystoreTuple dt)
{
   synchronized (tuple_data) {
      tuple_data.add(dt);
    }
}



void removeTuple(DystoreTuple dt)
{
   synchronized (tuple_data) {
      tuple_data.remove(dt);
    }
}



/********************************************************************************/
/*										*/
/*	Methods to access tuples						*/
/*										*/
/********************************************************************************/

Collection<DystoreTuple> getAllTuples()
{
   synchronized (tuple_data) {
      return new ArrayList<DystoreTuple>(tuple_data);
    }
}


DystoreRangeSet getRange(double from,double to)
{
   double f0 = from - for_store.getMaxInterval();

   return addToRange(f0,from,to,null);
}



DystoreRangeSet nextRange(double from,double to,boolean prune,boolean first,DystoreRangeSet rng)
{
   if (prune) pruneRange(from,to,rng);

   double f0 = from;
   if (first) f0 = from - for_store.getMaxInterval();

   rng = addToRange(f0,from,to,rng);

   return rng;
}



private void pruneRange(double from,double to,DystoreRangeSet rslt)
{
   if (rslt == null) return;

   rslt.removeBefore(from,end_field);
}



private DystoreRangeSet addToRange(double f0,double from,double to,DystoreRangeSet rslt)
{
   DystoreTuple tt = for_store.createTimeTuple(f0);

   if (rslt == null) rslt = new DystoreRangeSet();

   synchronized (tuple_data) {
      for (DystoreTuple dt : tuple_data.tailSet(tt)) {
	 if (dt.getTimeValue(start_field) > to) break;
	 if (dt.getTimeValue(end_field) >= from) {
	    // System.err.println("TUPLE ADD " + dt.getTimeValue(start_field) + " " +
	    //			  dt.getTimeValue(end_field) + " " + f0 + " " + from + " " + to + " " + dt);
	    rslt.add(dt);
	  }
       }
    }

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Clear  methods								*/
/*										*/
/********************************************************************************/

void clear()
{
   synchronized (tuple_data) {
      tuple_data.clear();
    }
}



/********************************************************************************/
/*										*/
/*	Tuple comparator for this tuple set					*/
/*										*/
/********************************************************************************/

private class TupleComparator implements Comparator<DystoreTuple> {

   private DystoreField cstart_field;
   private DystoreField cend_field;
   private List<DystoreField> string_fields;
   private List<DystoreField> number_fields;

   TupleComparator() {
      cstart_field = for_table.getStartTimeField();
      cend_field = for_table.getEndTimeField();
      if (cend_field == cstart_field) cend_field = null;
      string_fields = new ArrayList<DystoreField>();
      number_fields = new ArrayList<DystoreField>();
      for (DystoreField df : for_table.getFields()) {
	 DystoreFieldImpl dfi = (DystoreFieldImpl) df;
	 if (df != cstart_field && df != cend_field) {
	    if (dfi.getStringIndex() > 0) string_fields.add(df);
	    else number_fields.add(df);
	  }
       }
    }

   public int compare(DystoreTuple t1,DystoreTuple t2) {
      if (cstart_field != null) {
	 double v1 = t1.getTimeValue(cstart_field);
	 double v2 = t2.getTimeValue(cstart_field);
	 if (v1 < v2) return -1;
	 if (v1 > v2) return 1;
       }
      if (cend_field != null) {
	 double v1 = t1.getTimeValue(cend_field);
	 double v2 = t2.getTimeValue(cend_field);
	 if (v1 < v2) return -1;
	 if (v1 > v2) return 1;
       }
      for (DystoreField df : string_fields) {
	 String s1 = t1.getValue(df);
	 String s2 = t2.getValue(df);
	 if (s1 == null && s2 == null) continue;
	 if (s1 == null) return -1;
	 if (s2 == null) return 1;
	 int v = s1.compareTo(s2);
	 if (v != 0) return v;
       }
      for (DystoreField df : number_fields) {
	 double v1 = t1.getTimeValue(df);
	 double v2 = t2.getTimeValue(df);
	 if (v1 < v2) return -1;
	 if (v1 > v2) return 1;
       }

      return 0;
    }

}	// end of innerclass TupleComparator




}	// end of class DystoreTupleSet




/* end of DystoreTupleSet.java */
