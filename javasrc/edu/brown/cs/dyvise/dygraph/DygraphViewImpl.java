/********************************************************************************/
/*										*/
/*		DygraphViewImpl.java						*/
/*										*/
/*	DYVISE graphics (visualization) single view implementation		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dygraph/DygraphViewImpl.java,v 1.8 2013/09/04 18:35:14 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DygraphViewImpl.java,v $
 * Revision 1.8  2013/09/04 18:35:14  spr
 * Code cleanup.
 *
 * Revision 1.7  2013-05-09 12:28:57  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.6  2012-10-05 00:52:14  spr
 * Code cleanup
 *
 * Revision 1.5  2011-03-19 20:34:08  spr
 * Fix time block display and performance issues.
 *
 * Revision 1.4  2011-03-10 02:32:57  spr
 * Fixups for lock visualization.
 *
 * Revision 1.3  2010-03-30 16:20:44  spr
 * Fix bugs and features in graphical output.
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

import edu.brown.cs.dyvise.dystore.*;
import edu.brown.cs.ivy.file.IvyFormat;
import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import javax.swing.JPanel;

import java.awt.Color;
import java.awt.Component;
import java.text.DecimalFormat;
import java.util.*;



class DygraphViewImpl implements DygraphView, DygraphDisplayHandler, DygraphConstants {


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		display_name;
private DygraphControl	graph_control;
private DystoreControl	tuple_store;
private DygraphDisplay	using_display;
private Map<Enum<?>,DygraphSelector> selector_set;
private double		xdata_start;
private double		xdata_end;
private double		ydata_start;
private double		ydata_end;

private static final DecimalFormat TIME_FORMAT = new DecimalFormat("0.000");



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DygraphViewImpl(DygraphControl ctl,Element gdef)
{
   graph_control = ctl;
   tuple_store = ctl.getTupleStore();

   display_name = IvyXml.getAttrString(gdef,"NAME");

   using_display = DygraphDisplay.getDisplay(IvyXml.getAttrString(gdef,"TYPE"),this);
   Class<? extends Enum<?>> selectors = using_display.getSelectorEnums();

   xdata_start = 0;
   xdata_end = 1;
   ydata_start = 0;
   ydata_end = 1;

   selector_set = new HashMap<Enum<?>, DygraphSelector>();
   for (Element se : IvyXml.children(gdef,"SELECT")) {
      DygraphSelector ds = new DygraphSelector(this,se,tuple_store);
      String nm = ds.getName();
      Enum<?> e = null;
      for (Enum<?> e1 : selectors.getEnumConstants()) {
	 if (e1.name().equals(nm)) {
	    e = e1;
	    break;
	 }
      }
      if (e == null) {
	 System.err.println("DYGRAPH: Selector " + nm + " is not required by the view");
       }
      else {
	 selector_set.put(e,ds);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public String getName() 			{ return display_name; }


public JPanel getDisplayPanel()
{
   return using_display.getDisplayPanel();
}



public JPanel getControlPanel()
{
   SwingGridPanel pnl = new SwingGridPanel();

   pnl.beginLayout();

   int ctr = 0;
   Class<? extends Enum<?>> selectors = using_display.getSelectorEnums();
   for (Enum<?> e1 : selectors.getEnumConstants()) {
      DygraphSelector ds = selector_set.get(e1);
      if (ds == null) continue;
      Component c = ds.getChooser();
      if (c == null) continue;
      pnl.addRawComponent(using_display.getSelectorDescription(e1),c);
      ++ctr;
    }

   if (ctr == 0) return null;

   return pnl;
}



public void updateDisplay()
{
   JPanel pnl = using_display.checkDisplayPanel();

   if (pnl != null) {
      using_display.handleDataUpdated();
      pnl.repaint();
    }
}



public DygraphControl getControl()			{ return graph_control; }



public void setXDataRegion(double start,double end)
{
   xdata_start = start;
   xdata_end = end;

   updateDisplay();
}




public void setYDataRegion(double start,double end)
{
   ydata_start = start;
   ydata_end = end;

   updateDisplay();
}



/********************************************************************************/
/*										*/
/*	Data access methods for displays based on selectors			*/
/*										*/
/********************************************************************************/

public DystoreDataMap nextTupleSet(DygraphValueContext ctx,Enum<?> key,
				      boolean prune,boolean first,
				      double dstart,double dend,
				      DystoreDataMap data,
				      DystoreDataMap rdata)
{
   DystoreTable dt = ctx.getTable();
   if (dt == null) return null;
   DystoreStore store = tuple_store.getStore(dt);

   DystoreField df = null;
   DygraphSelector ds2 = null;
   if (key != null) {
      ds2 = selector_set.get(key);
      if (ds2 != null) df = ds2.getField(dt);
    }

   if (data == null) {
      if (ds2 != null) data = ds2.getDefaultMap(dt);
      else data = new DystoreDataMap(false,null);
    }

   if (df != null) {
      DystoreAccessor da = store.getAccessor(df);
      if (data.size() == 0) {
	 if (dstart != 0 || dend != 1) {
	    if (rdata != null) {
	       for (String s : rdata.keySet()) data.addKey(s);
	     }
	    else {
	       Collection<String> vals = da.getValues();
	       for (String s : vals) data.addOriginalKey(s);

	       // need to restrict the set of values
	       int vct = vals.size();
	       int nval = (int)((dend - dstart)*vct + 0.5);
	       if (nval < 1) nval = 1;
	       int sval = (int)(dstart*vct + 0.5);
	       if (sval > vct-nval) sval = vct-nval;
	       int ct = 0;
	       for (Iterator<String> it = data.keySet().iterator(); it.hasNext(); ) {
		  it.next();
		  if (ct < sval || ct >= sval+nval) it.remove();
		  ++ct;
		}
	     }
	  }
       }
      da.nextRange(ctx.getFromTime(),ctx.getToTime(),prune,first,data);
    }
   else {
      DystoreRangeSet rng = data.get(DYGRAPH_ALL);
      if (rng == null) {
	 rng = store.getRange(ctx.getFromTime(),ctx.getToTime());
	 data.addTuples(DYGRAPH_ALL,rng);
       }
      else store.nextRange(ctx.getFromTime(),ctx.getToTime(),prune,first,rng);
    }

   return data;
}




/********************************************************************************/
/*										*/
/*	Value access methods							*/
/*										*/
/********************************************************************************/

public double getStartTime()
{
   return graph_control.getDisplayStartTime();
}

public double getEndTime()
{
   return graph_control.getDisplayEndTime();
}

public double getTimeSpan()
{
   return graph_control.getDisplayTimeDelta();
}

public double getTimeAtDelta(double d)
{
   return graph_control.getTimeAtDelta(d);
}

public double getXDataStart()				{ return xdata_start; }
public double getXDataEnd()				{ return xdata_end; }
public double getYDataStart()				{ return ydata_start; }
public double getYDataEnd()				{ return ydata_end; }



public double getValue(DygraphValueContext ctx,Enum<?> key,DystoreRangeSet data)
{
   DygraphSelector ds1 = selector_set.get(key);

   return ds1.getValue(ctx,data);
}



public boolean getBoolean(DygraphValueContext ctx,Enum<?> key,DystoreRangeSet data)
{
   DygraphSelector ds1 = selector_set.get(key);

   return ds1.getBoolean(ctx,data);
}



public List<DystoreRangeSet> splitTuples(DygraphValueContext ctx,Enum<?> key,
      DystoreRangeSet data)
{
   DygraphSelector ds1 = selector_set.get(key);

   return ds1.splitTuples(ctx,data);
}



public Color getColor(DygraphValueContext ctx,Enum<?> hkey,Enum<?> skey,Enum<?> vkey,
			 DystoreRangeSet data)
{
   double h = getValue(ctx,hkey,data);
   double s = getValue(ctx,skey,data);
   double v = getValue(ctx,vkey,data);

   int cv = Color.HSBtoRGB((float) h,(float) s,(float) v);

   return new Color(cv);
}


public Color getColor(DygraphValueContext ctx,Enum<?> hkey,Enum<?> skey,Enum<?> vkey,Enum<?> akey,
			 DystoreRangeSet data)
{
   double h = getValue(ctx,hkey,data);
   double s = getValue(ctx,skey,data);
   double v = getValue(ctx,vkey,data);
   double a = getValue(ctx,akey,data);

   int cv = Color.HSBtoRGB((float) h,(float) s,(float) v);

   int av = ((int)(255*a))&0xff;
   cv = (cv&0x00ffffff) | (av<< 24);

   return new Color(cv,true);
}


public DystoreTable getTable(Enum<?> tkey)
{
   DygraphSelector ds1 = selector_set.get(tkey);
   if (ds1 == null) return null;

   return ds1.getTable();
}



public DygraphValueContext getContext(Enum<?> tkey)
{
   DygraphSelector ds1 = selector_set.get(tkey);
   if (ds1 == null) return null;

   DystoreTable tbl = ds1.getTable();

   return new DygraphValueContext(tbl);
}



public boolean getBoolean(Enum<?> tkey)
{
   DygraphSelector ds1 = selector_set.get(tkey);
   if (ds1 == null) return false;

   return ds1.getBoolean();
}



public DystoreField getField(DystoreTable tbl,Enum<?> fkey)
{
   if (tbl == null) return null;

   DygraphSelector ds2 = selector_set.get(fkey);
   if (ds2 == null) return null;

   return ds2.getField(tbl);
}



public DystoreField getField(DygraphValueContext ctx,Enum<?> fkey)
{
   if (ctx == null) return null;
   DystoreTable tbl = ctx.getTable();
   if (tbl == null) return null;

   DygraphSelector ds2 = selector_set.get(fkey);
   if (ds2 == null) return null;

   return ds2.getField(tbl);
}



public DystoreConstants.ValueRange getValueRange(DystoreField fld)
{
   return tuple_store.getValueRange(fld);
}



public Collection<String> getValueSet(DystoreField fld)
{
   return tuple_store.getValueSet(fld);
}




/********************************************************************************/
/*										*/
/*	Formatting methods							*/
/*										*/
/********************************************************************************/

public String formatTuples(DygraphValueContext ctx,
			      DystoreRangeSet set,DystoreRangeSet sel)
{
   DystoreTable tbl = ctx.getTable();

   if (set.size() == 0) return null;
   if (tbl == null) return null;

   Collection<DystoreField> flds = tbl.getFields();
   DystoreField startfld = null;
   DystoreField endfld = null;
   DystoreField ivlfld = null;
   Collection<DystoreField> outflds = new ArrayList<DystoreField>();
   for (DystoreField df : flds) {
      switch (df.getType()) {
	 case OBJECT :
	 case THREAD :
	 case STRING :
	 case INT :
	    outflds.add(df);
	    break;
	 case START_TIME :
	    startfld = df;
	    break;
	 case END_TIME :
	    endfld = df;
	    break;
	 case INTERVAL :
	    ivlfld = df;
	    break;
	 default :
	    break;
       }
    }

   Map<DystoreField,TupleValueSet> rslt = new TreeMap<DystoreField,TupleValueSet>();
   for (DystoreField df : outflds) rslt.put(df,new TupleValueSet());

   for (DystoreTuple dt : set) {
      boolean select = false;
      if (sel != null && sel.contains(dt)) select = true;
      double t0 = ctx.getFromTime();
      double t1 = ctx.getToTime();
      if (startfld != null) {
	 double tx = dt.getTimeValue(startfld);
	 if (tx > t0) t0 = tx;
       }
      if (endfld != null) {
	 double tx = dt.getTimeValue(endfld);
	 if (tx < t1) t1 = tx;
       }
      else if (ivlfld != null) {
	 double tx = dt.getTimeValue(ivlfld);
	 if (ctx.getToTime() + tx < t1) t1 = t0+tx;
       }
      if (t1 < t0) t1 = t0;
      double pct = (t1-t0+1)/(ctx.getToTime()-ctx.getFromTime()+1);
      for (DystoreField df : outflds) {
	 TupleValueSet vs = rslt.get(df);
	 String v = dt.getValue(df);
	 vs.add(v,pct,select);
       }
    }

   double tstart = graph_control.getTupleStore().getStartTime();
   double t0 = (ctx.getFromTime() - tstart)/1000.0;
   double t1 = (ctx.getToTime() - tstart)/1000.0;

   StringBuilder buf = new StringBuilder();
   buf.append("<html>");
   buf.append("From " + TIME_FORMAT.format(t0) + " to " + TIME_FORMAT.format(t1));
   buf.append("<br>");
   buf.append("<table frame='box' border='1' rules='groups'>");
   for (Map.Entry<DystoreField,TupleValueSet> ent : rslt.entrySet()) {
      DystoreField df = ent.getKey();
      TupleValueSet vs = ent.getValue();
      if (vs.getSize() == 0) continue;
      buf.append("<tbody>");
      buf.append("<tr><td rowspan='" + vs.getSize() + "' align='right'>");
      buf.append(df.getName());
      buf.append("</td>");
      vs.outputValues(buf);
      buf.append("</tbody>");
    }
   buf.append("</table></html>");

   return buf.toString();
}




private static class TupleValueSet {

   private Map<String,ValueSet> value_set;

   TupleValueSet() {
      value_set = new TreeMap<String,ValueSet>();
    }

   void add(String val,double pct,boolean sel) {
      if (val == null) return;
      ValueSet v = value_set.get(val);
      if (v == null) {
	 v = new ValueSet();
	 value_set.put(val,v);
       }
      v.add(pct,sel);
    }

   int getSize()			{ return value_set.size(); }

   void outputValues(StringBuilder buf) {
      boolean needrow = false;
      for (Map.Entry<String,ValueSet> ent : value_set.entrySet()) {
	 String key = ent.getKey();
	 ValueSet vs = ent.getValue();
	 if (needrow) buf.append("<tr>");
	 needrow = true;
	 buf.append("<td>");
	 if (vs.getSelected() > 0) buf.append("*");
	 buf.append("</td><td>");
	 buf.append(key);
	 buf.append(" (");
	 buf.append(IvyFormat.formatPercent(vs.getTotal()));
	 buf.append("%");
	 if (vs.getSelected() > 0) {
	    buf.append(" <b>" + IvyFormat.formatPercent(vs.getSelected()) + "%</b>");
	  }
	 buf.append(")</td></tr>");
       }
    }

}	// end of inner class TupleValueSet



private static class ValueSet {

   private double total_percent;
   private double percent_selected;

   ValueSet() {
      total_percent = 0;
      percent_selected = 0;
    }

   void add(double pct,boolean sel) {
      total_percent += pct;
      if (sel) percent_selected += pct;
    }

   double getTotal()				{ return total_percent; }
   double getSelected() 			{ return percent_selected; }

}	// end of inner class ValueSet



/********************************************************************************/
/*										*/
/*	Load/store methods							*/
/*										*/
/********************************************************************************/

public void outputXml(IvyXmlWriter xw)
{
   Class<? extends Enum<?>> selectors = using_display.getSelectorEnums();
   for (Enum<?> e1 : selectors.getEnumConstants()) {
      xw.begin("SELECTOR");
      xw.field("NAME",e1.toString());
      DygraphSelector ds = selector_set.get(e1);
      if (ds != null) {
	 ds.outputValue(xw);
       }
      xw.end("SELECTOR");
    }
}



public void loadValues(Element xml)
{
   Class<? extends Enum<?>> selectors = using_display.getSelectorEnums();

   for (Element se : IvyXml.children(xml,"SELECTOR")) {
      String nm = IvyXml.getAttrString(se,"NAME");
      for (Enum<?> e1 : selectors.getEnumConstants()) {
	 if (e1.toString().equals(nm)) {
	    DygraphSelector ds = selector_set.get(e1);
	    if (ds == null) {
	       System.err.println("DYGRAPH: Can't find selector " + nm);
	       continue;
	     }
	    ds.loadValue(se);
	    break;
	  }
       }
    }
}



}	// end of class DygraphViewImpl




/* end of DygraphViewImpl.java */
