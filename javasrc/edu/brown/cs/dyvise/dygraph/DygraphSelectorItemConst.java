/********************************************************************************/
/*										*/
/*		DygraphSelectorTableConst.java					*/
/*										*/
/*	Abstract item as a choice for a selector for a constant value		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dygraph/DygraphSelectorItemConst.java,v 1.2 2011-03-19 20:34:08 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DygraphSelectorItemConst.java,v $
 * Revision 1.2  2011-03-19 20:34:08  spr
 * Fix time block display and performance issues.
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






class DygraphSelectorItemConst extends DygraphSelectorItem {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String display_name;
private double const_value;
private double null_value;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DygraphSelectorItemConst(DygraphSelector ds, Element e)
{
   super(ds,e);
   display_name = IvyXml.getAttrString(e,"NAME");
   const_value = IvyXml.getAttrDouble(e,"VALUE");
   null_value = IvyXml.getAttrDouble(e,"NULL",const_value);
}




DygraphSelectorItemConst(DygraphSelector ds,double v)
{
   super(ds,null);
   display_name = "CONSTANT " + v;
   const_value = v;
   null_value = v;
}




/********************************************************************************/
/*										*/
/*	Data access methods							*/
/*										*/
/********************************************************************************/

@Override boolean getBoolean()
{
   return const_value >= 0.5;
}




@Override double getValue(DygraphValueContext ctx,DystoreRangeSet data)
{
   if (data == null || data.size() == 0) return null_value;
   return const_value;
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

public String toString()			     { return display_name; }

@Override void outputValue(IvyXmlWriter xw)
{
   xw.begin("ITEM");
   xw.field("TYPE","CONST");
   xw.field("NAME",display_name);
   xw.field("VALUE",const_value);
   xw.field("NULL",null_value);
   xw.end("ITEM");
}



/********************************************************************************/
/*										*/
/*	Matching methods							*/
/*										*/
/********************************************************************************/

@Override boolean match(Element xml)
{
   return IvyXml.getAttrString(xml,"TYPE").equals("CONST") &&
      IvyXml.getAttrString(xml,"NAME").equals(display_name) &&
      IvyXml.getAttrDouble(xml,"VALUE") == const_value &&
      IvyXml.getAttrDouble(xml,"NULL",const_value) == null_value;
}



}	// end of class DygraphSelectorItemConst




/* end of DygraphSelectorItemConst.java */
