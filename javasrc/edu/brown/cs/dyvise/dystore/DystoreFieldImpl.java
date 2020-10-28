/********************************************************************************/
/*										*/
/*		DystoreFieldImpl.java						*/
/*										*/
/*	DYVISE implementation of a data field					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dystore/DystoreFieldImpl.java,v 1.3 2013-05-09 12:29:03 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DystoreFieldImpl.java,v $
 * Revision 1.3  2013-05-09 12:29:03  spr
 * Dylock update.	Last 1.6 update
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


import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;



class DystoreFieldImpl implements DystoreField, DystoreConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String field_name;
private FieldType field_type;
private DystoreTableImpl for_table;
private int     double_index;
private int	string_index;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DystoreFieldImpl(DystoreTableImpl td,Element e)
{
   for_table = td;
   field_name = IvyXml.getAttrString(e,"NAME");
   field_type = IvyXml.getAttrEnum(e,"TYPE",FieldType.VOID);
   double_index = -1;
   string_index = -1;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public String getName() 			{ return field_name; }

public FieldType getType()			{ return field_type; }

public DystoreTable getTable()			{ return for_table; }



/********************************************************************************/
/*										*/
/*	Index methods for tuple access						*/
/*										*/
/********************************************************************************/

void setStringIndex(int sidx)			{ string_index = sidx; }

void setDoubleIndex(int didx)                   { double_index = didx; }

int getDoubleIndex()                            { return double_index; }

int getStringIndex()				{ return string_index; }

public boolean isStringField()			{ return string_index >= 0; }
public boolean isDoubleField()                  { return double_index >= 0; }



/********************************************************************************/
/*										*/
/*	Comparison methods							*/
/*										*/
/********************************************************************************/

public int compareTo(DystoreField fld)
{
   return field_name.compareTo(fld.getName());
}


/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(IvyXmlWriter xw)
{
   xw.begin("FIELD");
   xw.field("NAME",field_name);
   xw.field("TYPE",field_type);
   xw.end("FIELD");
}



}	// end of class DystoreFieldImpl




/* end of DystoreFieldImpl.java */

