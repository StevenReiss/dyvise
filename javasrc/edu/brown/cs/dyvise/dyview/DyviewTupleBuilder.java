/********************************************************************************/
/*										*/
/*		DyviewTupleBuilder.java 					*/
/*										*/
/*	DYVISE viewer tuble builer						*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyview/DyviewTupleBuilder.java,v 1.4 2013-05-09 12:29:05 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyviewTupleBuilder.java,v $
 * Revision 1.4  2013-05-09 12:29:05  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.3  2009-10-07 01:00:21  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.2  2009-09-21 19:34:42  spr
 * Updates for load/save, which tuple for links.
 *
 * Revision 1.1  2009-09-19 00:14:23  spr
 * Viewer with dialog based front end.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dyview;


import edu.brown.cs.dyvise.dystore.*;
import edu.brown.cs.dyvise.dyvise.DyviseEvent;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.util.Map;



class DyviewTupleBuilder implements DyviewConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private DyviseEvent []	event_table;
private DyviewTupleContext tuple_context;
private DystoreControl	tuple_store;
private DystoreStore [] tuple_data;

private static boolean do_debug = false;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DyviewTupleBuilder(Element emdl,DystoreControl store)
{
   int max = 0;
   for (Element e : IvyXml.children(emdl,"EVENT")) {
      int id = IvyXml.getAttrInt(e,"ID");
      if (id > max) max = id;
    }
   event_table = new DyviseEvent[max+1];

   for (Element e : IvyXml.children(emdl,"EVENT")) {
      int id = IvyXml.getAttrInt(e,"ID");
      DyviseEvent evt = new DyviseEvent(e);
      event_table[id] = evt;
    }

   tuple_store = store;

   tuple_context = new DyviewTupleContext(this);

   tuple_data = new DystoreStore[tuple_store.getNumTables()];
   for (DystoreTable dt : tuple_store.getTables()) {
      tuple_data[dt.getIndex()] = tuple_store.getStore(dt);
    }
}




/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

synchronized void processEvents(Element xml)
{
   double base = IvyXml.getAttrDouble(xml,"BASE");

   for (Element evt : IvyXml.children(xml)) {
      if (IvyXml.isElement(evt,"ID")) tuple_context.handleId(evt);
      else if (IvyXml.isElement(evt,"E")) {
	 int id = IvyXml.getAttrInt(evt,"I");
	 DyviseEvent de = event_table[id];
	 String ps = IvyXml.getAttrString(evt,"P");
	 tuple_context.setParameters(ps);
	 int t = IvyXml.getAttrInt(evt,"T");
	 tuple_context.setTime(base,t);
	 de.accept(tuple_context);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Tuple generation methods						*/
/*										*/
/********************************************************************************/

void generate(int which,Map<String,Object> values)
{
   DystoreStore store = tuple_data[which];

   if (do_debug) {
      System.err.println("GENERATE " + store.getTable().getName() + ":");
      for (Map.Entry<String,Object> ent : values.entrySet()) {
	 System.err.println("\t" + ent.getKey() + " = " + ent.getValue());
       }
    }

   store.addTuple(values);
}




}	// end of class DyviewTupleBuilder




/* end of DyviewTupleBuilder.java */
