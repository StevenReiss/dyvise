/********************************************************************************/
/*										*/
/*		DyviewTupleContext.java 					*/
/*										*/
/*	DYVISE viewer tuple context						*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyview/DyviewTupleContext.java,v 1.6 2013-05-09 12:29:05 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyviewTupleContext.java,v $
 * Revision 1.6  2013-05-09 12:29:05  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.5  2012-10-05 00:53:05  spr
 * Code clean up.
 *
 * Revision 1.4  2011-03-10 02:33:25  spr
 * Code cleanup.
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


import edu.brown.cs.dyvise.dyvise.DyviseEventVisitor;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.util.*;



class DyviewTupleContext extends DyviseEventVisitor implements DyviewConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private List<Map<EventObject,TupleRepr>> tuple_map;
private Map<EventObject,TupleRepr> cur_map;
private EventObject		cur_key;
private TupleRepr		cur_tuple;
private EventObject []		cur_params;
private int			cur_time;
private int			cur_index;
private boolean 		is_new;
private double			base_time;
private boolean 		if_exclude;
private DyviewTupleBuilder	output_handler;

private Map<Integer,EventObject> id_map;

private static final boolean do_debug = false;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DyviewTupleContext(DyviewTupleBuilder outer)
{
   tuple_map = new ArrayList<Map<EventObject,TupleRepr>>();
   cur_map = null;
   cur_tuple = null;
   cur_key = null;
   cur_params = new EventObject[10];
   cur_time = 0;
   cur_index = -1;
   is_new = false;
   if_exclude = false;
   output_handler = outer;
   id_map = new HashMap<Integer,EventObject>();
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

void setParameters(String p)
{
   StringTokenizer tok = new StringTokenizer(p,",");
   int idx = 0;
   while (tok.hasMoreTokens()) {
      int id = Integer.parseInt(tok.nextToken());
      if (id == 0) cur_params[idx++] = null;
      else {
	 EventObject eo = id_map.get(id);
	 if (eo == null) {
	    System.err.println("DYVIEW: No object found for ID: " + id);
	  }
	 cur_params[idx++] = eo;
       }
    }
   if_exclude = false;

   if (do_debug) {
      System.err.println("EVENT PARAMS: " + p + " " + idx);
      for (int i = 0; i < idx; ++i) {
	 System.err.println("\t" + i + " = " + cur_params[i]);
       }
    }
}


void setTime(double base,int t)
{
   base_time = base;
   cur_time = t;
}



void handleId(Element e)
{
   int id = IvyXml.getAttrInt(e,"VALUE");
   EventObject obj = id_map.get(id);
   if (obj == null) {
      obj = new EventObject();
      id_map.put(id,obj);
    }
   obj.update(IvyXml.getAttrString(e,"TYPE"),IvyXml.getText(e));
}




/********************************************************************************/
/*										*/
/*	Actions 								*/
/*										*/
/********************************************************************************/

@Override public void visitBegin(EventParam id,int idx,boolean force,boolean allow)
{
   if (if_exclude) return;

   while (idx >= tuple_map.size()) {
      tuple_map.add(new HashMap<EventObject,TupleRepr>());
    }
   cur_map = tuple_map.get(idx);
   cur_index = idx;

   is_new = false;
   cur_tuple = null;
   cur_key = getValue(id);

   if (!force) {
      cur_tuple = cur_map.get(cur_key);
      if (cur_tuple != null) return;
    }
   if (!allow) return;

   is_new = true;
   cur_tuple = new TupleRepr();
   cur_map.put(cur_key,cur_tuple);
}



@Override public void visitOutput(boolean discard)
{
   if (if_exclude) return;
   if (cur_tuple == null) return;

   output(cur_index,cur_tuple);
   if (discard) {
      if (cur_key != null) cur_map.remove(cur_key);
      cur_tuple = null;
      cur_key = null;
      cur_map = null;
      cur_index = -1;
    }

   is_new = false;
}



@Override public void visitField(String nm,EventParam vl,String cv,boolean diff)
{
   if (if_exclude) return;
   if (cur_tuple == null) return;

   Object val = getValue(vl,cv);
   if (diff) cur_tuple.diffField(nm,val);
   else cur_tuple.setField(nm,val);
}



@Override public void visitIf()
{
   if_exclude = !is_new;
}


@Override public void visitEndIf()
{
   if_exclude = false;
}


@Override public void visitElse()
{
   if_exclude = !if_exclude;
}



/********************************************************************************/
/*										*/
/*	Methods to get values							*/
/*										*/
/********************************************************************************/

private EventObject getValue(EventParam ep)
{
   EventObject r = null;

   switch (ep) {
      case P0 :
	 r = cur_params[0];
	 break;
      case P1 :
	 r = cur_params[1];
	 break;
      case P2 :
	 r = cur_params[2];
	 break;
      default :
         break;
    }

   return r;
}



private Object getValue(EventParam ep,String cv)
{
   Object r = null;

   switch (ep) {
      case CONST :
	 r = cv;
	 break;
      case TIME :
	 r = Double.valueOf(cur_time+base_time);
	 break;
      case P0 :
	 r = cur_params[0];
	 break;
      case P1 :
	 r = cur_params[1];
	 break;
      case P2 :
	 r = cur_params[2];
	 break;
      default : 
         break;
    }

   return r;
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void output(int idx,TupleRepr t)
{
   output_handler.generate(idx,t.getValues());
}



/********************************************************************************/
/*										*/
/*	Event Object representation						*/
/*										*/
/********************************************************************************/

private static class EventObject {

   private String object_value;

   EventObject() {
      object_value = null;
    }

   void update(String typ,String val) {
      object_value = val;
    }

   public String toString()			{ return object_value; }

}	// end of innerclass EventObject




/********************************************************************************/
/*										*/
/*	Tuple Representation							*/
/*										*/
/********************************************************************************/

private static class TupleRepr {

   private Map<String,Object> value_map;

   TupleRepr() {
      value_map = new HashMap<String,Object>();
    }

   void setField(String nm,Object v) {
      value_map.put(nm,v);
    }

   void diffField(String nm,Object v) {
      Number n1 = (Number) value_map.get(nm);
      Number n2 = (Number) v;
      if (n1 == null || n2 == null) return;
      double r = n2.doubleValue() - n1.doubleValue();
      value_map.put(nm,r);
    }

   Map<String,Object> getValues()		{ return value_map; }


}	// end of innerclass TupleRepr



}	// end of class DyviewTupleContext




/* end of DyviewTupleContext.java */

