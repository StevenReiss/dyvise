/********************************************************************************/
/*										*/
/*		DygraphSelectorItem.java					*/
/*										*/
/*	Abstract item as a choice for a selector				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dygraph/DygraphSelectorItem.java,v 1.2 2011-03-19 20:34:08 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DygraphSelectorItem.java,v $
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

import java.util.*;



abstract class DygraphSelectorItem implements DygraphConstants {


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

protected DygraphSelector for_selector;
protected boolean is_default;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected DygraphSelectorItem(DygraphSelector ds,Element e)
{
   for_selector = ds;
   if (e == null) is_default = true;
   else is_default = IvyXml.getAttrBool(e,"DEFAULT");
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

boolean isDefault()			     { return is_default; }

DystoreTable getTable() 		     { return null; }
DystoreField getField(DystoreTable tbl)      { return null; }
boolean getBoolean()			     { return false; }




/********************************************************************************/
/*										*/
/*	Data access methods							*/
/*										*/
/********************************************************************************/

double getValue(DygraphValueContext ctx,DystoreRangeSet data)
{
   if (data.size() == 0) return 0;
   return 1;
}



boolean getBoolean(DygraphValueContext ctx,DystoreRangeSet data)
{
   return getValue(ctx,data) >= 0.5;
}



List<DystoreRangeSet> splitTuples(DygraphValueContext ctx,DystoreRangeSet data)
{
   List<DystoreRangeSet> rslt = new ArrayList<DystoreRangeSet>();
   rslt.add(data);
   return rslt;
}



DystoreDataMap getDefaultMap(DystoreTable tbl)
{
   return new DystoreDataMap(false,null);
}



/********************************************************************************/
/*										*/
/*	Ouptut methods								*/
/*										*/
/********************************************************************************/

void outputValue(IvyXmlWriter xw)	     { }



/********************************************************************************/
/*										*/
/*	Matching methods							*/
/*										*/
/********************************************************************************/

boolean match(Element xml)		     { return false; }



}	// end of inner class DygraphSelectorItem




/* end of DygraphSelectorItem.java */
