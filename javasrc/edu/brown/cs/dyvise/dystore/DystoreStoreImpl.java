/********************************************************************************/
/*										*/
/*		DystoreStoreImpl.java						*/
/*										*/
/*	DYVISE tuple store							*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dystore/DystoreStoreImpl.java,v 1.6 2013-05-09 12:29:04 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DystoreStoreImpl.java,v $
 * Revision 1.6  2013-05-09 12:29:04  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.5  2011-03-19 20:34:43  spr
 * Code speedup by adding range class.
 *
 * Revision 1.4  2011-03-10 02:33:19  spr
 * Code cleanup.
 *
 * Revision 1.3  2010-03-30 16:23:05  spr
 * Make the store efficient enough to use with the display.
 *
 * Revision 1.2  2009-10-07 01:00:20  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.1  2009-09-19 00:14:00  spr
 * In memory tuple store for visualization.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dystore;


import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.util.*;



class DystoreStoreImpl implements DystoreStore, DystoreConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private DystoreTableImpl for_table;
private DystoreTupleSet  all_tuples;
private double		start_time;
private double		end_time;
private double		max_interval;
private boolean 	abs_time;
private Map<DystoreField,DystoreAccessorImpl> access_map;
private Map<DystoreField,ValueData> value_map;
private Set<ValueCallback> value_callbacks;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DystoreStoreImpl(DystoreTableImpl tbl)
{
   for_table = tbl;
   start_time = 0;
   end_time = 0;
   max_interval = 0;
   abs_time = false;
   access_map = new HashMap<DystoreField,DystoreAccessorImpl>();

   value_map = new HashMap<DystoreField,ValueData>();
   value_callbacks = new HashSet<ValueCallback>();

   all_tuples = new DystoreTupleSet(this);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public final DystoreTable getTable()	{ return for_table; }

double getStartTime()			{ return start_time; }

double getEndTime()			{ return end_time; }

double getMaxInterval()		{ return max_interval; }

public Collection<DystoreTuple> getAllTuples() { return all_tuples.getAllTuples(); }

public DystoreRangeSet getRange(double from,double to)
{
   return all_tuples.getRange(from,to);
}

public DystoreRangeSet nextRange(double from,double to,boolean prune,boolean first,
      DystoreRangeSet rng)
{
   return all_tuples.nextRange(from,to,prune,first,rng);
}

public void addValueListener(ValueCallback vc)
{
   if (vc != null) value_callbacks.add(vc);
}

public void removeValueListener(ValueCallback vc)
{
   if (vc != null) value_callbacks.remove(vc);
}


public void setUseAbsoluteTime(boolean fg)
{
   abs_time = fg;
   start_time = 0;
}




/********************************************************************************/
/*										*/
/*	Accessor methods							*/
/*										*/
/********************************************************************************/

public DystoreAccessor getAccessor(DystoreField df)
{
   DystoreAccessorImpl dai;

   synchronized (access_map) {
      dai = access_map.get(df);
      if (dai == null) {
	 dai = new DystoreAccessorImpl(this,df);
	 access_map.put(df,dai);
       }
     }

   return dai;
}



/********************************************************************************/
/*										*/
/*	Data addition methods							*/
/*										*/
/********************************************************************************/

public DystoreTuple addTuple(Map<String,Object> values)
{
   DystoreTupleImpl ti = for_table.newTuple(values);

   checkTimes(ti);

   synchronized (this) {		// single add/clear at once
      synchronized (value_map) {
	 for (Map.Entry<DystoreField,ValueData> ent : value_map.entrySet()) {
	    DystoreField df1 = ent.getKey();
	    if (noteValue(ent.getValue(),ti,df1)) {
	       for (ValueCallback vc : value_callbacks) vc.valuesChanged(df1);
	     }
	  }
       }

      all_tuples.addTuple(ti);

      synchronized (access_map) {
	 for (DystoreAccessorImpl da : access_map.values()) {
	    da.addTuple(ti);
	  }
       }
    }

   return ti;
}



public void updateTuple(DystoreTuple dt,Map<String,Object> values)
{
   if (dt == null || values == null || values.size() == 0) return;
   DystoreTupleImpl ti = (DystoreTupleImpl) dt;

   synchronized (this) {
      all_tuples.removeTuple(ti);
      synchronized (access_map) {
	 for (DystoreAccessorImpl da : access_map.values()) {
	    da.removeTuple(ti);
	  }
       }

      for_table.updateTuple(ti,values);

      synchronized (value_map) {
	 for (Map.Entry<DystoreField,ValueData> ent : value_map.entrySet()) {
	    DystoreField df1 = ent.getKey();
	    if (noteValue(ent.getValue(),ti,df1)) {
	       for (ValueCallback vc : value_callbacks) vc.valuesChanged(df1);
	     }
	  }
       }

      all_tuples.addTuple(ti);

      synchronized (access_map) {
	 for (DystoreAccessorImpl da : access_map.values()) {
	    da.addTuple(ti);
	  }
       }
    }
}




public void setTupleField(DystoreTuple dt,DystoreField df,double val)
{
   DystoreTupleImpl ti = (DystoreTupleImpl) dt;
   DystoreFieldImpl fi = (DystoreFieldImpl) df;

   int idx = fi.getDoubleIndex();
   if (idx >= 0) ti.setValue(idx,val);

   checkTimes(ti);
}



public void setTupleField(DystoreTuple dt,DystoreField df,String val)
{
   DystoreTupleImpl ti = (DystoreTupleImpl) dt;
   DystoreFieldImpl fi = (DystoreFieldImpl) df;

   int idx = fi.getStringIndex();
   if (idx >= 0) ti.setValue(idx,val);

   checkTimes(ti);
}



private void checkTimes(DystoreTupleImpl ti)
{
   DystoreField df = null;

   if (!abs_time) {
      df = for_table.getStartTimeField();
      if (df != null) {
	 double st = ti.getTimeValue(df);
	 if (st != 0 && st != CURRENT_TIME && (start_time == 0 || start_time > st)) start_time = st;
       }
    }

   df = for_table.getEndTimeField();
   if (df != null) {
      double st = ti.getTimeValue(df);
      if (st != 0 && st != CURRENT_TIME && (end_time == 0 || end_time < st)) end_time = st;
    }
   double d = end_time - start_time;
   if (d > max_interval) max_interval = d;
}



public void noteValue(DystoreField f,String value)
{
   ValueData vd = getValueData(f);
   vd.noteValue(value);
}


public void noteValue(DystoreField f,double value)
{
   ValueData vd = getValueData(f);
   vd.noteValue(value);
}


/********************************************************************************/
/*										*/
/*	Value set methods							*/
/*										*/
/********************************************************************************/

public ValueRange getValueRange(DystoreField f)
{
   ValueData vd = getValueData(f);
   return vd;
}


public Collection<String> getValueSet(DystoreField f)
{
   ValueData vd = getValueData(f);
   return vd.getValues();
}



private ValueData getValueData(DystoreField f)
{
   synchronized (value_map) {
      ValueData vd = value_map.get(f);
      if (vd == null) {
	 vd = new ValueData();
	 value_map.put(f,vd);

	 for (DystoreTuple dt : all_tuples.getAllTuples()) {
	    noteValue(vd,dt,f);
	  }
       }
      return vd;
    }
}




private boolean noteValue(ValueData vd,DystoreTuple ti,DystoreField fld)
{
   DystoreFieldImpl dfi = (DystoreFieldImpl) fld;
   boolean chng = false;

   if (dfi.isStringField()) {
      chng = vd.noteValue(ti.getValue(dfi));
    }
   else if (dfi.isDoubleField()) {
      chng = vd.noteValue(ti.getTimeValue(dfi));
    }

   return chng;
}



/********************************************************************************/
/*										*/
/*	Clear methods								*/
/*										*/
/********************************************************************************/

public void clear(boolean values)
{
   synchronized (this) {
      start_time = 0;
      end_time = 0;
      max_interval = 0;

      if (values) {
         synchronized (value_map) {
            value_map.clear();
          }
       }

      all_tuples.clear();

      synchronized (access_map) {
	 access_map.clear();
       }
    }

}



/********************************************************************************/
/*										*/
/*	Tuple creation methods							*/
/*										*/
/********************************************************************************/

DystoreTupleImpl createTimeTuple(double t0)
{
   return for_table.newTuple(t0);
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(IvyXmlWriter xw,DystoreIdSet idset)
{
   xw.begin("TABLE");
   xw.field("NAME",for_table.getName());

   for (DystoreTuple dt : all_tuples.getAllTuples()) {
      for_table.outputTuple(xw,dt,idset);
    }

   xw.end("TABLE");
}



/********************************************************************************/
/*										*/
/*	ValueData class to hold the set/range of values 			*/
/*										*/
/********************************************************************************/

private static class ValueData implements ValueRange {

   private boolean vals_set;
   private double min_value;
   private double max_value;
   private Collection<String> value_set;

   ValueData() {
      vals_set = false;
      min_value = max_value = 0;
      value_set = null;
    }

   public double getMinValue()		{ return min_value; }
   public double getMaxValue()		{ return max_value; }

   Collection<String> getValues()	{
      if (value_set == null) return new ArrayList<String>();
      synchronized (value_set) {
         return new ArrayList<String>(value_set);
       }
    }

   boolean noteValue(double v) {
      if (!vals_set) {
         min_value = max_value = v;
         vals_set = true;
       }
      else if (v < min_value) min_value = v;
      else if (v > max_value) max_value = v;
      else return false;
      return true;
   }

   boolean noteValue(String v) {
      if (v == null) return false;
      if (value_set == null) value_set = new TreeSet<String>();
      if (value_set.contains(v)) return false;
      synchronized (value_set) {
         value_set.add(v);
         max_value = value_set.size()-1;
       }
      return true;
    }

}	// end of innerclass ValueData




}	// end of class DystoreStoreImpl




/* end of DystoreStoreImpl.java */

