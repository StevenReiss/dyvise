/********************************************************************************/
/*										*/
/*		DystoreAccessorImpl.java					*/
/*										*/
/*	DYVISE tuple store accessor implementation				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dystore/DystoreAccessorImpl.java,v 1.4 2013-05-09 12:29:03 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DystoreAccessorImpl.java,v $
 * Revision 1.4  2013-05-09 12:29:03  spr
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



class DystoreAccessorImpl implements DystoreAccessor, DystoreConstants {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private DystoreStoreImpl	for_store;
private DystoreField		for_field;
private Map<String,DystoreTupleSet> tuple_store;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DystoreAccessorImpl(DystoreStoreImpl store,DystoreField fld)
{
   for_store = store;
   for_field = fld;

   tuple_store = new LinkedHashMap<String,DystoreTupleSet>();

   for (DystoreTuple dt : store.getAllTuples()) {
      addTuple(dt);
    }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public Collection<String> getValues()
{
   synchronized (tuple_store) {
      return Collections.unmodifiableCollection(tuple_store.keySet());
    }
}



@Override public DystoreRangeSet getRange(String value,double from,double to)
{
   DystoreTupleSet ts = tuple_store.get(value);
   if (ts == null) return new DystoreRangeSet();

   return ts.getRange(from,to);
}



@Override public DystoreRangeSet nextRange(String value,double from,double to,boolean prune,
						       boolean first,
						       DystoreRangeSet rng)
{
   DystoreTupleSet ts = tuple_store.get(value);
   if (ts == null) return new DystoreRangeSet();

   return ts.nextRange(from,to,prune,first,rng);
}



@Override public void nextRange(double from,double to,boolean prune,boolean first,
				   DystoreDataMap map)
{
   synchronized (tuple_store) {
      if (map.isEmpty()) {
	 for (Map.Entry<String,DystoreTupleSet> ent : tuple_store.entrySet()) {
	    String k = ent.getKey();
	    if (k != null) map.addTuples(k,ent.getValue().getRange(from,to));
	  }
       }
      else {
	 for (Map.Entry<String,DystoreTupleSet> ent : tuple_store.entrySet()) {
	    String k = ent.getKey();
	    if (k == null) continue;
	    DystoreRangeSet orng = map.getOriginal(k);
	    if (orng != null) ent.getValue().nextRange(from,to,prune,first,orng);
	    else {
	       // if map is set, the complete set of keys should be there
	       // orng = ent.getValue().getRange(from,to);
	       // map.put(ent.getKey(),orng);
	     }
	  }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Addition methods							*/
/*										*/
/********************************************************************************/

void addTuple(DystoreTuple ti)
{
   String v = ti.getValue(for_field);
   if (v == null) return;

   synchronized (tuple_store) {
      DystoreTupleSet ts = tuple_store.get(v);
      if (ts == null) {
	 ts = new DystoreTupleSet(for_store);
	 tuple_store.put(v,ts);
       }
      ts.addTuple(ti);
    }
}



void removeTuple(DystoreTuple ti)
{
   String v = ti.getValue(for_field);
   if (v == null) return;

   synchronized (tuple_store) {
      DystoreTupleSet ts = tuple_store.get(v);
      if (ts == null) return;
      ts.removeTuple(ti);
    }
}


}	// end of class DystoreAccessorImpl




/* end of DystoreAccessorImpl.java */



































































