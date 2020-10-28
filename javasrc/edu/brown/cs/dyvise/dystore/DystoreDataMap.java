/********************************************************************************/
/*										*/
/*		DystoreDataMap.java						*/
/*										*/
/*	General purpose data map allowing for filtered/mapped values		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dystore/DystoreDataMap.java,v 1.2 2011-03-19 20:34:43 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DystoreDataMap.java,v $
 * Revision 1.2  2011-03-19 20:34:43  spr
 * Code speedup by adding range class.
 *
 * Revision 1.1  2010-03-30 21:29:08  spr
 * Add the notion of filters and separate the data map to its own class.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dystore;



import java.util.*;


public class DystoreDataMap {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private boolean time_ordered;
private DystoreFilter using_filter;

private Map<String,String>	key_map;
private Map<String,DystoreRangeSet> data_map;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public DystoreDataMap(boolean timeorder,DystoreFilter filter)
{
   time_ordered = timeorder;
   using_filter = filter;

   if (time_ordered) data_map = new LinkedHashMap<String,DystoreRangeSet>();
   else data_map = new TreeMap<String,DystoreRangeSet>();

   if (using_filter == null) key_map = null;
   else key_map = new HashMap<String,String>();
}



/********************************************************************************/
/*										*/
/*	Map methods								*/
/*										*/
/********************************************************************************/

public int size()				{ return data_map.size(); }

public boolean isEmpty()			{ return data_map.isEmpty(); }

public Iterable<String> keySet()		{ return data_map.keySet(); }

public Iterable<Map.Entry<String,DystoreRangeSet>> entrySet()
{
   return data_map.entrySet();
}

public Iterable<DystoreRangeSet> values()
{
   return data_map.values();
}


public DystoreRangeSet get(String v)
{
   return data_map.get(v);
}


public void addKey(String k)
{
   if (k != null && !data_map.containsKey(k)) {
      data_map.put(k,new DystoreRangeSet());
    }
}




/********************************************************************************/
/*										*/
/*	Translation methods							*/
/*										*/
/********************************************************************************/

public void addOriginalKey(String k)
{
   k = mapValue(k);

   if (k != null) addKey(k);
}



public DystoreRangeSet getOriginal(String v)
{
   v = mapValue(v);

   if (v == null) return null;

   return data_map.get(v);
}



public void addTuples(String v,DystoreRangeSet tups)
{
   v = mapValue(v);

   if (v == null) return;

   DystoreRangeSet ov = data_map.get(v);
   if (ov == null) {
      data_map.put(v,tups);
    }
   else {
      ov.addAll(tups);
    }
}



/********************************************************************************/
/*										*/
/*	Mapping methods 							*/
/*										*/
/********************************************************************************/

private String mapValue(String v)
{
   if (using_filter == null) return v;

   if (key_map.containsKey(v)) return key_map.get(v);		// might be null

   String v1 = using_filter.filter(v);
   key_map.put(v,v1);

   return v1;
}




}	// end of DystoreDataMap




/* end of DystoreDataMap.java */
