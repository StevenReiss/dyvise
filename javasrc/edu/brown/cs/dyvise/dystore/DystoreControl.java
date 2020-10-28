/********************************************************************************/
/*										*/
/*		DystoreControl.java						*/
/*										*/
/*	DYVISE controller for tuple storage					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dystore/DystoreControl.java,v 1.5 2013-05-09 12:29:03 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DystoreControl.java,v $
 * Revision 1.5  2013-05-09 12:29:03  spr
 * Dylock update.	Last 1.6 update
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


import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.*;

import java.io.*;
import java.util.*;



public class DystoreControl implements DystoreConstants {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<String,DystoreTableImpl>	table_defs;
private Map<DystoreTable,DystoreStoreImpl> data_storage;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public DystoreControl(Element tuplemodel)
{
   table_defs = new HashMap<String,DystoreTableImpl>();
   boolean abst = IvyXml.getAttrBool(tuplemodel,"ABSTIME",false);

   for (Element te : IvyXml.children(tuplemodel,"TUPLE")) {
      DystoreTableImpl td = new DystoreTableImpl(te);
      table_defs.put(td.getName(),td);
    }

   data_storage = new HashMap<DystoreTable,DystoreStoreImpl>();
   for (DystoreTableImpl td : table_defs.values()) {
      DystoreStoreImpl ds = new DystoreStoreImpl(td);
      if (abst) ds.setUseAbsoluteTime(true);
      data_storage.put(td,ds);
    }
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public int getNumTables()			{ return table_defs.size(); }

public DystoreTable getTable(String id) 	{ return table_defs.get(id); }

public Collection<DystoreTable> getTables()
{
   return new ArrayList<DystoreTable>(table_defs.values());
}


public DystoreStore getStore(DystoreTable dt)	{ return data_storage.get(dt); }
public DystoreStore getStore(String id)
{
   DystoreTable dt = table_defs.get(id);
   if (dt == null) return null;
   return data_storage.get(dt);
}



public double getStartTime()
{
   double t = 0;

   for (DystoreStoreImpl ds : data_storage.values()) {
      double t1 = ds.getStartTime();
      if (t1 != 0) {
	 if (t == 0 || t1 < t) t = t1;
       }
    }

   return t;
}


public double getEndTime()
{
   double t = 0;

   for (DystoreStoreImpl ds : data_storage.values()) {
      double t1 = ds.getEndTime();
      if (t1 != 0) {
	 if (t == 0 || t1 > t) t = t1;
       }
    }

   return t;
}



public Collection<String> getValueSet(DystoreField fld)
{
   DystoreStore ds = getStore(fld.getTable());
   return ds.getValueSet(fld);
}



public ValueRange getValueRange(DystoreField fld)
{
   DystoreStore ds = getStore(fld.getTable());
   return ds.getValueRange(fld);
}



public void clear(boolean values)
{
   for (DystoreStoreImpl ds : data_storage.values()) {
      ds.clear(values);
    }
}


public void addValueListener(ValueCallback vc)
{
   for (DystoreStoreImpl ds : data_storage.values()) ds.addValueListener(vc);
}




public void removeValueListener(ValueCallback vc)
{
   for (DystoreStoreImpl ds : data_storage.values()) ds.removeValueListener(vc);
}




/********************************************************************************/
/*										*/
/*	Load/Store methods							*/
/*										*/
/********************************************************************************/

public void save(File file) throws IOException
{
   DystoreIdSet idset = new DystoreIdSet();

   IvyXmlWriter xw = new IvyXmlWriter(file);
   xw.begin("TUPLESTORE");

   xw.begin("TUPLEMODEL");
   for (DystoreTableImpl td : table_defs.values()) td.outputXml(xw);
   xw.end("TUPLEMODEL");

   xw.begin("TUPLEDATA");
   for (DystoreStoreImpl ds : data_storage.values()) ds.outputXml(xw,idset);
   xw.end("TUPLEDATA");

   idset.outputXml(xw);

   xw.end("TUPLESTORE");
   xw.close();
}



public void load(File file) throws IOException
{
   SAXParserFactory spf = SAXParserFactory.newInstance();
   spf.setValidating(false);
   spf.setXIncludeAware(false);
   spf.setNamespaceAware(false);

   try {
      SAXParser sp = spf.newSAXParser();

      IdHandler idh = new IdHandler();
      FileInputStream fis = new FileInputStream(file);
      sp.parse(fis,idh);
      fis.close();

      TupleHandler tph = new TupleHandler(idh);
      fis = new FileInputStream(file);
      sp.parse(fis,tph);
      fis.close();
    }
   catch (SAXException e) {
      System.err.println("DYSTORE: Problem parsing data file: " + e);
      throw new IOException("Bad xml trace file: " + e);
    }
   catch (ParserConfigurationException e) {
      System.err.println("DYSTORE: Problem configuring parser: " + e);
      throw new IOException("Unable to configure XML parser: " + e);
    }
}



private static class IdHandler extends DefaultHandler {

   String [] id_map;
   int last_id;

   IdHandler() {
      id_map = null;
      last_id = -1;
    }

   String getValue(int id) {
      if (id == 0) return null;
      return id_map[id];
    }

   public void startElement(String uri,String nm,String qn,Attributes attr) {
      if (qn.equals("IDMAP")) {
	 int sz = Integer.parseInt(attr.getValue("SIZE"));
	 id_map = new String[sz];
       }
      else if (qn.equals("I")) {
	 last_id = Integer.parseInt(attr.getValue("N"));
       }
    }

   public void endElement(String uri,String nm,String qn) {
      if (qn.equals("I")) last_id = -1;
    }

   public void characters(char [] ch,int st,int len) {
      String s = new String(ch,st,len).trim().intern();
      if (last_id >= 0) {
	 if (id_map[last_id] == null) id_map[last_id] = s;
	 else id_map[last_id] += s;
       }
    }

}	// end of inner class IdHandler





private class TupleHandler extends DefaultHandler {

   DystoreStore for_store;
   IdHandler id_handler;
   Set<String> number_fields;

   TupleHandler(IdHandler idh) {
      id_handler = idh;
      for_store = null;
      number_fields = new HashSet<String>();
    }

   public void startElement(String uri,String nm,String qn,Attributes attr) {
      if (qn.equals("TABLE")) {
         String tnm = attr.getValue("NAME");
         DystoreTable tbl = getTable(tnm);
         number_fields.clear();
         for (DystoreField fld : tbl.getFields()) {
            if (fld.isDoubleField()) number_fields.add(fld.getName());
          }
         for_store = getStore(tbl);
       }
      else if (qn.equals("D")) {
         Map<String,Object> values = new HashMap<String,Object>();
         for (int i = 0; i < attr.getLength(); ++i) {
            String fnm = attr.getLocalName(i);
            if (number_fields.contains(fnm)) {
               Double l = Double.valueOf(attr.getValue(i));
               values.put(fnm,l);
             }
            else {
               int idx = Integer.parseInt(attr.getValue(i));
               String iv = id_handler.getValue(idx);
               if (iv != null) values.put(fnm,iv);
             }
          }
         for_store.addTuple(values);
       }
    }

   public void endElement(String uri,String nm,String qn) {
      if (qn.equals("TABLE")) {
	 for_store = null;
	 number_fields.clear();
       }
    }

}	// end of inner class IdHandler



}	// end of class DystoreControl




/* end of DystoreControl.java */


