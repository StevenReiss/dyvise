/********************************************************************************/
/*										*/
/*		DygraphSelectorTableItem.java					*/
/*										*/
/*	Abstract item as a choice for a selector for picking a data table	*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dygraph/DygraphSelectorItemTable.java,v 1.1 2010-03-31 17:41:54 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DygraphSelectorItemTable.java,v $
 * Revision 1.1  2010-03-31 17:41:54  spr
 * Add selection mechanism.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dygraph;

import edu.brown.cs.dyvise.dystore.DystoreTable;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;



class DygraphSelectorItemTable extends DygraphSelectorItem {


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private DystoreTable store_table;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DygraphSelectorItemTable(DygraphSelector ds,Element e) {
   super(ds, e);
   String tnm = IvyXml.getAttrString(e,"NAME");
   store_table = for_selector.getStore().getTable(tnm);
   if (store_table == null) {
      System.err.println("DYGRAPH: Table name " + tnm + " is invalid");
    }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override DystoreTable getTable()		{ return store_table; }




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

public String toString()		     { return store_table.getName(); }

@Override void outputValue(IvyXmlWriter xw) {
   xw.begin("ITEM");
   xw.field("TYPE","TABLE");
   xw.field("TABLE",store_table.getName());
   xw.end("ITEM");
}



/********************************************************************************/
/*										*/
/*	Matching methods							*/
/*										*/
/********************************************************************************/

@Override boolean match(Element xml) {
   return IvyXml.getAttrString(xml,"TYPE").equals("TABLE") &&
      IvyXml.getAttrString(xml,"TABLE").equals(store_table.getName());
}




}	// end of class DygraphSelectorTableItem



/* end of DygraphSelectorTableItem.java */
