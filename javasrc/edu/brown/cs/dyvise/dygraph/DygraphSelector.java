/********************************************************************************/
/*										*/
/*		DygraphSelector.java						*/
/*										*/
/*	DYVISE graphics (visualization) selection model interface		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dygraph/DygraphSelector.java,v 1.6 2013-06-03 13:02:53 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DygraphSelector.java,v $
 * Revision 1.6  2013-06-03 13:02:53  spr
 * Minor bug fixes
 *
 * Revision 1.5  2012-10-05 00:52:14  spr
 * Code cleanup
 *
 * Revision 1.4  2011-03-19 20:34:08  spr
 * Fix time block display and performance issues.
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
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import javax.swing.JComboBox;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;


class DygraphSelector implements DygraphConstants, ActionListener {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private DygraphView for_view;
private DystoreControl for_store;
private String selector_name;
private SelectorType selector_type;
private List<DygraphSelectorItem> item_set;
private DygraphSelectorItem cur_item;
private int target_size;
private JComboBox<DygraphSelectorItem> combo_box;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DygraphSelector(DygraphView dv,Element xml,DystoreControl store)
{
   for_view = dv;
   for_store = store;
   selector_name = IvyXml.getAttrString(xml,"WHAT");
   selector_type = IvyXml.getAttrEnum(xml,"TYPE",SelectorType.GENERIC);

   target_size = IvyXml.getAttrInt(xml,"SIZE");
   cur_item = null;
   combo_box = null;

   item_set = new ArrayList<DygraphSelectorItem>();
   for (Element se : IvyXml.children(xml)) {
      DygraphSelectorItem si = null;
      if (IvyXml.isElement(se,"TABLE")) {
	 si = new DygraphSelectorItemTable(this,se);
       }
      else if (IvyXml.isElement(se,"FIELD")) {
	 si = new DygraphSelectorItemField(this,se);
       }
      else if (IvyXml.isElement(se,"CONST")) {
	 si = new DygraphSelectorItemConst(this,se);
       }
      else if (IvyXml.isElement(se,"INTERVAL")) {
	 si = new DygraphSelectorItemInterval(this,se);
       }

      if (si != null) {
	 item_set.add(si);
	 if (si.isDefault() && cur_item == null) cur_item = si;
       }
    }

   if (cur_item == null && item_set.size() > 0) cur_item = item_set.get(0);
   else if (cur_item == null) cur_item = new DygraphSelectorItemConst(this,0.5);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getName()			{ return selector_name; }

SelectorType getType()			{ return selector_type; }

DystoreControl getStore()		{ return for_store; }

int getTargetSize()			{ return target_size; }




/********************************************************************************/
/*										*/
/*	Current selection access methods					*/
/*										*/
/********************************************************************************/

DystoreTable getTable()
{
   return cur_item.getTable();
}



DystoreField getField(DystoreTable tbl)
{
   return cur_item.getField(tbl);
}



boolean getBoolean()
{
   if (cur_item == null) return false;

   return cur_item.getBoolean();
}



double getValue(DygraphValueContext ctx,DystoreRangeSet data)
{
   return cur_item.getValue(ctx,data);
}



boolean getBoolean(DygraphValueContext ctx,DystoreRangeSet data)
{
   return cur_item.getBoolean(ctx,data);
}



List<DystoreRangeSet> splitTuples(DygraphValueContext ctx,DystoreRangeSet data)
{
   return cur_item.splitTuples(ctx,data);
}


DystoreDataMap getDefaultMap(DystoreTable tbl)
{
   return cur_item.getDefaultMap(tbl);
}



/********************************************************************************/
/*										*/
/*	Control panel interface 						*/
/*										*/
/********************************************************************************/

Component getChooser()
{
   if (item_set.size() < 2) return null;

   Vector<DygraphSelectorItem> choices = new Vector<DygraphSelectorItem>(item_set);
   combo_box = new JComboBox<DygraphSelectorItem>(choices);
   combo_box.addActionListener(this);
   combo_box.setSelectedItem(cur_item);

   return combo_box;
}



/********************************************************************************/
/*										*/
/*	Operator-based evaluation						*/
/*										*/
/********************************************************************************/

double initialValue(SelectorOp op)
{
   switch (op) {
      case MIN :
	 return 1.0;
      default :
	 break;
    }

   return 0;
}




/******************
double computeValue(SelectorOp op,double val,double oldv,double ct)
{
   switch (op) {
      case NONE :
      case AVERAGE :
	 val = oldv + val/ct;
	 break;
      case INTERVAL :
      case SUM :
	 val = oldv + val;
	 break;
      case MAX :
	 if (val < oldv) val = oldv;
	 break;
      case MIN :
	 if (val > oldv) val = oldv;
	 break;
      case COUNT :
	 // TODO: count is wrong: need to know max count for intervals
	 val = ct;
	 break;
    }

   return val;
}
*************************/



/********************************************************************************/
/*										*/
/*	Load/store methods							*/
/*										*/
/********************************************************************************/

void outputValue(IvyXmlWriter xw)
{
   if (cur_item != null) cur_item.outputValue(xw);
}


void loadValue(Element xml)
{
   Element itm = IvyXml.getChild(xml,"ITEM");
   cur_item = null;
   if (itm != null) {
      for (DygraphSelectorItem se : item_set) {
	 if (se.match(itm)) {
	    cur_item = se;
	    if (combo_box != null) combo_box.setSelectedItem(cur_item);
	    break;
	  }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Callback for selector							*/
/*										*/
/********************************************************************************/

public void actionPerformed(ActionEvent evt)
{
   JComboBox<?> cbx = (JComboBox<?>) evt.getSource();
   cur_item = (DygraphSelectorItem) cbx.getSelectedItem();
   for_view.updateDisplay();
}




}	// end of class DygraphSelector




/* end of DygraphSelector.java */
