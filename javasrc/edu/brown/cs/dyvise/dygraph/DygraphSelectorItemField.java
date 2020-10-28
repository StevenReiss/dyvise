/********************************************************************************/
/*										*/
/*		DygraphSelectorItemField.java					*/
/*										*/
/*	Abstract item as a choice for a selector for picking a field		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dygraph/DygraphSelectorItemField.java,v 1.5 2013-05-09 12:28:56 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DygraphSelectorItemField.java,v $
 * Revision 1.5  2013-05-09 12:28:56  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.4  2011-03-19 20:34:08  spr
 * Fix time block display and performance issues.
 *
 * Revision 1.3  2011-03-10 02:32:57  spr
 * Fixups for lock visualization.
 *
 * Revision 1.2  2010-06-01 02:46:01  spr
 * Minor bug fixes.
 *
 * Revision 1.1  2010-03-31 17:41:54  spr
 * Add selection mechanism.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dygraph;

import edu.brown.cs.dyvise.dystore.*;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.util.*;




class DygraphSelectorItemField extends DygraphSelectorItem
	implements DystoreConstants.ValueCallback
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private String field_name;
private SelectorOp merge_op;
private SelectorSortOp sort_op;
private Map<DystoreTable,DystoreField> table_fields;
private Map<String,Double> value_map;
private DystoreFilter using_filter;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DygraphSelectorItemField(DygraphSelector ds,Element e)
{
   super(ds,e);
   field_name = IvyXml.getAttrString(e,"NAME");
   merge_op = IvyXml.getAttrEnum(e,"OP",SelectorOp.NONE);
   sort_op = IvyXml.getAttrEnum(e,"SORT",SelectorSortOp.SORT_NAME);
   table_fields = new HashMap<DystoreTable,DystoreField>();
   using_filter = null;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override DystoreField getField(DystoreTable tbl)
{
   if (table_fields.containsKey(tbl)) return table_fields.get(tbl);
   DystoreField f1 = tbl.getField(field_name);
   if (table_fields.isEmpty() && f1.isStringField()) for_selector.getStore().addValueListener(this);
   table_fields.put(tbl,f1);
   return f1;
}



@Override DystoreDataMap getDefaultMap(DystoreTable tbl)
{
   DystoreDataMap rslt = null;

   switch (sort_op) {
      default :
      case SORT_NAME :
	 rslt = new DystoreDataMap(false,using_filter);
	 break;
      case SORT_TIME :
	 rslt = new DystoreDataMap(true,using_filter);
	 break;
    }

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Data access methods							*/
/*										*/
/********************************************************************************/

@Override double getValue(DygraphValueContext ctx,DystoreRangeSet data)
{
   if (data.size() == 0) return 0;
   DystoreField fld = getField(ctx.getTable());

   // TODO: take care of different functions

   DygraphValue dval = new DygraphValue(merge_op);
   if (fld.isStringField()) {
      Map<String,Double> vals = getValueMap(ctx);
      if (vals == null) vals = new HashMap<String,Double>();
      for (DystoreTuple dt : data) {
	 String s = dt.getValue(fld);
	 if (s == null) continue;
	 Double dv = vals.get(s);
	 if (dv != null) {
	    dval.addValue(s,dv);
	  }
	 else dval.addNullValue(s);
       }
    }
   else {
      DystoreConstants.ValueRange rng = for_selector.getStore().getValueRange(fld);
      // TODO: Restrict maxv,minv using range filter
      double maxv = rng.getMaxValue();
      double minv = rng.getMinValue();
      double den = (maxv - minv + 1);
      for (DystoreTuple dt : data) {
	 double v0 = dt.getTimeValue(fld);
	 if (v0 < minv || v0 > maxv) continue;
	 v0 = (v0 - rng.getMinValue())/den;
	 dval.addValue(null,v0);
       }
    }

   return dval.getValue(ctx);
}




@Override List<DystoreRangeSet> splitTuples(DygraphValueContext ctx,
							DystoreRangeSet data)
{
   List<DystoreRangeSet> rslt = new ArrayList<DystoreRangeSet>();
   DystoreField fld = getField(ctx.getTable());

   if (fld.isStringField()) {
      Map<String,DystoreRangeSet> vmap = new HashMap<String,DystoreRangeSet>();
      Collection<String> valset = getFilteredValueSet(fld);
      for (String s : valset) {
	 DystoreRangeSet c = new DystoreRangeSet();
         rslt.add(c);
	 vmap.put(s,c);
       }
      for (DystoreTuple dt : data) {
	 String s = dt.getValue(fld);
         DystoreRangeSet c = vmap.get(s);
	 if (c != null) c.add(dt);
       }
    }
   else {
      DystoreConstants.ValueRange rng = for_selector.getStore().getValueRange(fld);
      int tsz = for_selector.getTargetSize();

      if (tsz <= 0) {
	 double d = rng.getMaxValue() - rng.getMinValue() + 1;
	 tsz = (int) d;
       }

      for (int i = 0; i < tsz; ++i) {
	 rslt.add(new DystoreRangeSet());
       }

      // TODO: if range filter, adjust min and max by range
      double maxv = rng.getMaxValue();
      double minv = rng.getMinValue();

      double den = (maxv - minv + 1);
      for (DystoreTuple dt : data) {
	 double v0 = dt.getTimeValue(fld);
	 if (v0 < minv || v0 > maxv) continue;
	 v0 = (v0 - minv)/den * tsz;
	 int v1 = (int) (v0 + 0.5);
	 if (v1 < 0) v1 = 0;
	 if (v1 >= tsz) v1 = tsz - 1;
	 if (v1 < 0) v1 = 0;
	 rslt.get(v1).add(dt);
       }
    }

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

public String toString()
{
   String nm = field_name;
   if (merge_op != SelectorOp.NONE) {
      nm += " (" + merge_op.toString() + ")";
    }
   else if (sort_op != null) {
      String sby = (sort_op == SelectorSortOp.SORT_NAME ? "Name" : "First Use");
      nm += " (Sort by " + sby + ")";
    }
   if (using_filter != null) {
      nm += " (Filter using " + using_filter + ")";
    }
   return nm;
}



@Override void outputValue(IvyXmlWriter xw)
{
   xw.begin("ITEM");
   xw.field("TYPE","FIELD");
   xw.field("FIELD",field_name);
   xw.field("MERGE",merge_op);
   xw.field("SORT",sort_op);
   if (using_filter != null) using_filter.output(xw);
   xw.end("ITEM");
}



/********************************************************************************/
/*										*/
/*	Matching methods							*/
/*										*/
/********************************************************************************/

@Override boolean match(Element xml)
{
   // TODO: compare filters here as well
   return IvyXml.getAttrString(xml,"TYPE").equals("FIELD") &&
      IvyXml.getAttrString(xml,"FIELD").equals(field_name) &&
      IvyXml.getAttrEnum(xml,"MERGE",merge_op) == merge_op &&
      IvyXml.getAttrEnum(xml,"SORT",sort_op) == sort_op;
}



/********************************************************************************/
/*										*/
/*	Value set maintenance methods						*/
/*										*/
/********************************************************************************/

public synchronized void valuesChanged(DystoreField df)
{
   if (value_map != null) {
      if (table_fields.values().contains(df)) {
	 value_map = null;
       }
    }
}



private Collection<String> getFilteredValueSet(DystoreField fld)
{
   Collection<String> valset = for_selector.getStore().getValueSet(fld);

   if (using_filter != null) {
      Map<String,String> tmap = new TreeMap<String,String>();
      for (String s : valset) {
	 String s1 = using_filter.filter(s);
	 if (s1 != null) tmap.put(s1,s);
       }
      valset = tmap.values();
    }

   return valset;
}



private Map<String,Double> getValueMap(DygraphValueContext ctx)
{
   Collection<String> valset = null;

   synchronized (this) {
      if (value_map != null) return value_map;
    }

   DystoreField fld = getField(ctx.getTable());
   valset = getFilteredValueSet(fld);

   synchronized (this) {
      if (value_map == null) {
	 value_map = new HashMap<String,Double>();
	 int idx = 1;
	 double sz = valset.size() + 1.0;
	 for (String s : valset) value_map.put(s,(idx++)/sz);
       }
    }

   return value_map;
}




}	// end of class DygraphSelectorItemField




/* end of DygraphSelectorItemField.java */
