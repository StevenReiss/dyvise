/********************************************************************************/
/*										*/
/*		DyviseEvent.java						*/
/*										*/
/*	DYVISE dyanmic analysis event representation				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyvise/DyviseEvent.java,v 1.2 2009-10-07 01:00:23 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyviseEvent.java,v $
 * Revision 1.2  2009-10-07 01:00:23  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.1  2009-09-19 00:14:44  spr
 * Common files for use throughout the system.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dyvise;


import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;



public class DyviseEvent implements DyviseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private int		event_id;
private String		event_name;
private List<Action>	event_actions;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public DyviseEvent(int id,String name)
{
   event_id = id;
   event_name = name;
   event_actions = new ArrayList<Action>();
}




public DyviseEvent(Element e)
{
   event_id = IvyXml.getAttrInt(e,"ID");
   event_name = IvyXml.getAttrString(e,"NAME");
   event_actions = new ArrayList<Action>();
   for (Element a : IvyXml.children(e,"ACTION")) {
      String typ = IvyXml.getAttrString(a,"TYPE");
      Action act = null;
      if (typ.equals("BEGIN")) act = new BeginAction(a);
      else if (typ.equals("OUTPUT")) act = new OutputAction(a);
      else if (typ.equals("FIELD")) act = new FieldAction(a);
      else if (typ.equals("IFNEW")) act = new IfAction(a);
      else if (typ.equals("ENDIF")) act = new EndIfAction(a);
      else if (typ.equals("ELSE")) act = new ElseAction(a);
      else {
	 System.err.println("DYVISE: Unknown event type: " + typ);
       }

      event_actions.add(act);
    }

}


/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

public void accept(DyviseEventVisitor v)
{
   for (Action act : event_actions) {
      act.accept(v);
    }
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public int getEventId() 			{ return event_id; }
public String getEventName()			{ return event_name; }


/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

public void outputXml(IvyXmlWriter xw)
{
   xw.begin("EVENT");
   xw.field("ID",event_id);
   xw.field("NAME",event_name);
   for (Action a : event_actions) {
      a.outputXml(xw);
    }
   xw.end("EVENT");
}


/********************************************************************************/
/*										*/
/*	Event programming methods						*/
/*										*/
/********************************************************************************/

public void addBeginTuple(EventParam ep,int which)
{
   event_actions.add(new BeginAction(ep,which,true,true));
}

public void addFindTuple(EventParam ep,int which)
{
   event_actions.add(new BeginAction(ep,which,false,false));
}

public void addFindNewTuple(EventParam ep,int which)
{
   event_actions.add(new BeginAction(ep,which,false,true));
}

public void addSetField(String field,EventParam value)
{
   event_actions.add(new FieldAction(field,value,false));
}

public void addSetField(String field,String value)
{
   event_actions.add(new FieldAction(field,value));
}

public void addSetDiff(String field,EventParam value)
{
   event_actions.add(new FieldAction(field,value,true));
}

public void addOutputTuple()
{
   event_actions.add(new OutputAction(false));
}

public void addOutputDiscard()
{
   event_actions.add(new OutputAction(true));
}

public void addIfNew()
{
   event_actions.add(new IfAction());
}

public void addElse()
{
   event_actions.add(new ElseAction());
}

public void addEndIf()
{
   event_actions.add(new EndIfAction());
}



/********************************************************************************/
/*										*/
/*	Action abstration							*/
/*										*/
/********************************************************************************/

private static abstract class Action {

   protected abstract void outputXml(IvyXmlWriter xw);
   protected abstract void accept(DyviseEventVisitor v);

}	// end of inner interface Action



/********************************************************************************/
/*										*/
/*	Tuple actions								*/
/*										*/
/********************************************************************************/

private static class BeginAction extends Action {

   private EventParam id_param;
   private int which_tuple;
   private boolean force_new;
   private boolean allow_new;

   BeginAction(EventParam v,int which,boolean frc,boolean alw) {
      id_param = v;
      which_tuple = which;
      force_new = frc;
      allow_new = alw;
    }

   BeginAction(Element xml) {
      id_param = IvyXml.getAttrEnum(xml,"ID",EventParam.NONE);
      force_new = IvyXml.getAttrBool(xml,"NEW");
      allow_new = IvyXml.getAttrBool(xml,"ALLOWNEW");
      which_tuple = IvyXml.getAttrInt(xml,"WHICH");
    }

   protected void outputXml(IvyXmlWriter xw) {
      xw.begin("ACTION");
      xw.field("TYPE","BEGIN");
      xw.field("NEW",force_new);
      xw.field("ALLOWNEW",allow_new);
      xw.field("ID",id_param);
      xw.field("WHICH",which_tuple);
      xw.end("ACTION");
    }

   protected void accept(DyviseEventVisitor v) {
      v.visitBegin(id_param,which_tuple,force_new,allow_new);
    }


}	// end of inner class BeginAction




private static class OutputAction extends Action {

   private boolean force_discard;

   OutputAction(boolean frc) {
      force_discard = frc;
    }

   OutputAction(Element xml) {
      force_discard = IvyXml.getAttrBool(xml,"DISCARD");
    }

   protected void outputXml(IvyXmlWriter xw) {
      xw.begin("ACTION");
      xw.field("TYPE","OUTPUT");
      xw.field("DISCARD",force_discard);
      xw.end("ACTION");
    }

   protected void accept(DyviseEventVisitor v) {
      v.visitOutput(force_discard);
    }

}	// end of inner class OutputAction




private static class FieldAction extends Action {

   private String field_name;
   private EventParam field_value;
   private String const_value;
   private boolean do_difference;

   FieldAction(String fld,EventParam ep,boolean diff) {
      field_name = fld;
      field_value = ep;
      const_value = null;
      do_difference = diff;
    }

   FieldAction(String fld,String val) {
      field_name = fld;
      field_value = EventParam.CONST;
      const_value = val;
      do_difference = false;
    }

   FieldAction(Element xml) {
      field_name = IvyXml.getAttrString(xml,"FIELD");
      field_value = IvyXml.getAttrEnum(xml,"VALUE",EventParam.NONE);
      const_value = IvyXml.getAttrString(xml,"CONST");
      if (field_value == EventParam.NONE && const_value != null) field_value = EventParam.CONST;
      do_difference = IvyXml.getAttrBool(xml,"DIFFERENCE");
    }

   protected void outputXml(IvyXmlWriter xw) {
      xw.begin("ACTION");
      xw.field("TYPE","FIELD");
      xw.field("FIELD",field_name);
      xw.field("VALUE",field_value);
      if (const_value != null) xw.field("CONST",const_value);
      if (do_difference) xw.field("DIFFERENCE",true);
      xw.end("ACTION");
    }

   protected void accept(DyviseEventVisitor v) {
      v.visitField(field_name,field_value,const_value,do_difference);
    }

}	// end of inner class FieldAction



private static class IfAction extends Action {

   IfAction() { }

   IfAction(Element xml)		{ }


   protected void outputXml(IvyXmlWriter xw) {
      xw.begin("ACTION");
      xw.field("TYPE","IFNEW");
      xw.end("ACTION");
    }

   protected void accept(DyviseEventVisitor v) {
      v.visitIf();
    }

}	// end of innerclass IfAction





private static class EndIfAction extends Action {

   EndIfAction() { }

   EndIfAction(Element xml)		{ }

   protected void outputXml(IvyXmlWriter xw) {
      xw.begin("ACTION");
      xw.field("TYPE","ENDIF");
      xw.end("ACTION");
    }

   protected void accept(DyviseEventVisitor v) {
      v.visitEndIf();
    }

}	// end of innerclass IfAction





private static class ElseAction extends Action {

   ElseAction() { }

   ElseAction(Element xml)		{ }

   protected void outputXml(IvyXmlWriter xw) {
      xw.begin("ACTION");
      xw.field("TYPE","ELSE");
      xw.end("ACTION");
    }

   protected void accept(DyviseEventVisitor v) {
      v.visitElse();
    }

}	// end of innerclass IfAction





}	// end of class DyEvent




/* end of DyEvent.java */


