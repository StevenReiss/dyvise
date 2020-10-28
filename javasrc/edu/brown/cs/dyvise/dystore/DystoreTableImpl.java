/********************************************************************************/
/*										*/
/*		DystoreTableImpl.java						*/
/*										*/
/*	DYVISE implementation of a data table					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dystore/DystoreTableImpl.java,v 1.4 2013-05-09 12:29:04 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DystoreTableImpl.java,v $
 * Revision 1.4  2013-05-09 12:29:04  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.3  2011-03-19 20:34:43  spr
 * Code speedup by adding range class.
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

import java.util.*;



class DystoreTableImpl implements DystoreTable, DystoreConstants {


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String table_name;
private int table_index;
private Map<String,DystoreFieldImpl> table_fields;
private DystoreFieldImpl start_field;
private DystoreFieldImpl end_field;
private int string_index;
private int double_index;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DystoreTableImpl(Element e)
{
   table_name = IvyXml.getAttrString(e,"NAME");
   table_index = IvyXml.getAttrInt(e,"INDEX");
   table_fields = new LinkedHashMap<String,DystoreFieldImpl>();
   start_field = null;
   end_field = null;

   string_index = 0;
   double_index = 0;

   for (Element fe : IvyXml.children(e,"FIELD")) {
      DystoreFieldImpl fd = new DystoreFieldImpl(this,fe);
      table_fields.put(fd.getName(),fd);
      if (fd.getType() == FieldType.START_TIME && start_field == null) start_field = fd;
      if (fd.getType() == FieldType.END_TIME && end_field == null) end_field = fd;
      switch (fd.getType()) {
	 case VOID :
	    break;
	 case OBJECT :
	 case THREAD :
	 case STRING :
	    fd.setStringIndex(string_index++);
	    break;
	 case INT :
            fd.setDoubleIndex(double_index++);
            break;
	 case START_TIME :
	 case END_TIME :
	 case INTERVAL :
	    fd.setDoubleIndex(double_index++);
	    break;
       }
    }

   if (start_field == null) start_field = end_field;
   if (end_field == null) end_field = start_field;
}



/********************************************************************************/
/*										*/
/*	Access methoods 							*/
/*										*/
/********************************************************************************/

public String getName() 			{ return table_name; }
public int getIndex()				{ return table_index; }

public Collection<DystoreField> getFields() {
   return new ArrayList<DystoreField>(table_fields.values());
}

public DystoreField getField(String f)		{ return table_fields.get(f); }

public DystoreField getStartTimeField() 	{ return start_field; }
public DystoreField getEndTimeField()		{ return end_field; }



/********************************************************************************/
/*										*/
/*	Tuple methods								*/
/*										*/
/********************************************************************************/

DystoreTupleImpl newTuple()
{
   return new DystoreTupleImpl(string_index,double_index);
}



DystoreTupleImpl newTuple(double t0)
{
   DystoreTupleImpl ti = new DystoreTupleImpl(string_index,double_index);
   if (start_field != null) {
      int idx = start_field.getDoubleIndex();
      ti.setValue(idx,t0);
    }
   if (end_field != null) {
      int idx = end_field.getDoubleIndex();
      ti.setValue(idx,t0);
    }

   return ti;
}



DystoreTupleImpl newTuple(Map<String,Object> values)
{
   DystoreTupleImpl ti = new DystoreTupleImpl(string_index,double_index);

   updateTupleData(ti,values);

   return ti;
}



void updateTuple(DystoreTuple tup,Map<String,Object> values)
{
   updateTupleData((DystoreTupleImpl) tup,values);
}



private void updateTupleData(DystoreTupleImpl ti,Map<String,Object> values)
{
   for (DystoreFieldImpl fi : table_fields.values()) {
      String nm = fi.getName();
      if (!values.containsKey(nm)) continue;
      Object val = values.get(nm);
      int idx = fi.getStringIndex();
      if (idx >= 0) {
	 if (val == null) ti.setValue(idx,null);
	 else if (val instanceof String) ti.setValue(idx,(String) val);
	 else ti.setValue(idx,val.toString());
       }
      else {
	 idx = fi.getDoubleIndex();
	 if (idx >= 0) {
	    if (val instanceof Number) ti.setValue(idx,((Number) val).doubleValue());
	    else if (val instanceof String) ti.setValue(idx,Double.parseDouble((String) val));
	  }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Ouptut methods								*/
/*										*/
/********************************************************************************/

void outputXml(IvyXmlWriter xw)
{
   xw.begin("TUPLE");
   xw.field("NAME",table_name);
   for (DystoreFieldImpl fi : table_fields.values()) {
      fi.outputXml(xw);
    }
   xw.end("TUPLE");
}



void outputTuple(IvyXmlWriter xw,DystoreTuple dt,DystoreIdSet idset)
{
   xw.begin("D");
   for (DystoreFieldImpl fi : table_fields.values()) {
      if (fi.isStringField()) {
	 String v = dt.getValue(fi);
	 if (idset != null) xw.field(fi.getName(),idset.getId(v));
	 else if (v != null) xw.field(fi.getName(),v);
       }
      else if (fi.isDoubleField()) {
	 double v = dt.getTimeValue(fi); 
	 xw.field(fi.getName(),v);
       }
    }
   xw.end("D");
}



}	// end of class DystoreTableImpl



/* end of DystoreTableImpl.java */


