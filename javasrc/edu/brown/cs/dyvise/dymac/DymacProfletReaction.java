/********************************************************************************/
/*										*/
/*		DymacProfletReaction.java					*/
/*										*/
/*	DYVISE dynamic analysis proflet interface for event handlers		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymac/DymacProfletReaction.java,v 1.6 2013/09/04 18:36:31 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymacProfletReaction.java,v $
 * Revision 1.6  2013/09/04 18:36:31  spr
 * Minor bug fixes.
 *
 * Revision 1.5  2011-03-10 02:33:04  spr
 * Code cleanup.
 *
 * Revision 1.4  2010-06-01 02:46:02  spr
 * Minor bug fixes.
 *
 * Revision 1.3  2010-03-30 16:21:04  spr
 * Bug fixes in dynamic analysis.
 *
 * Revision 1.2  2009-10-07 00:59:45  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.1  2009-09-19 00:09:00  spr
 * Module to collect dynamic information from dymon about an applcation and store in database.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymac;


import edu.brown.cs.dyvise.dyvise.DyviseConstants;
import edu.brown.cs.dyvise.dyvise.DyviseDatabase;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.swing.*;

import org.w3c.dom.Element;

import javax.swing.*;
import java.util.*;
import java.awt.Dimension;


class DymacProfletReaction extends DymacProfletBase implements DymacProflet, DyviseConstants {



private Set<EventHandler> event_handlers;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DymacProfletReaction(DymacMain dm)
{
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public String getName() 			{ return "EVENTS"; }

public String getAgentName()			{ return "FINDREACT"; }



/********************************************************************************/
/*										*/
/*	Data acquisition methods						*/
/*										*/
/********************************************************************************/

public boolean processData(Element xml)
{
   Element r = IvyXml.getChild(xml,"REACTION");
   if (r == null) return true;
   Set<EventHandler> rslt = new HashSet<EventHandler>();
   for (Element cb : IvyXml.children(r,"CALLBACK")) {
      StringBuffer buf = new StringBuffer();
      int ct = 0;
      for (Element tc : IvyXml.children(cb,"THREAD")) {
	 String tnm = IvyXml.getAttrString(tc,"NAME");
	 if (ct++ > 0) buf.append(";");
	 buf.append(tnm);
       }
      rslt.add(new EventHandler(IvyXml.getAttrString(cb,"CLASS"),
				   IvyXml.getAttrString(cb,"MNAME"),buf.toString(),"CALLBACK"));
    }
   for (Element em : IvyXml.children(r,"EVENT")) {
      rslt.add(new EventHandler(IvyXml.getAttrString(em,"CLASS"),
				   IvyXml.getAttrString(em,"MNAME"),
				   IvyXml.getAttrString(em,"THREAD"),
				   IvyXml.getAttrString(em,"TYPE")));
    }
   if (rslt.equals(event_handlers)) return false;

   event_handlers = rslt;

   return true;
}



/********************************************************************************/
/*										*/
/*	Result verification methods						*/
/*										*/
/********************************************************************************/

public boolean verifyResults(String start)
{
   SwingGridPanel pnl = new SwingGridPanel();
   pnl.beginLayout();
   pnl.addBannerLabel("Verify Events and Callbacks from Dynamic Analysis");
   pnl.addSeparator();

   Map<JCheckBox,EventHandler> fldmap = new HashMap<JCheckBox,EventHandler>();

   for (EventHandler eh : event_handlers) {
      String id = eh.getUserLabel();
      pnl.addBoolean(id,true,null);
    }
   pnl.addSeparator();

   JScrollPane jsp = new JScrollPane(pnl);
   jsp.setMaximumSize(new Dimension(800,600));

   int sts = JOptionPane.showOptionDialog(null,jsp,"Event Handler Verification",
					     JOptionPane.OK_CANCEL_OPTION,
					     JOptionPane.PLAIN_MESSAGE,
					     null,null,null);
   if (sts != 0) return false;

   for (Map.Entry<JCheckBox,EventHandler> ent : fldmap.entrySet()) {
      JCheckBox cbx = ent.getKey();
      if (!cbx.isSelected()) {
	 event_handlers.remove(ent.getValue());
       }
    }

   return true;
}




/********************************************************************************/
/*										*/
/*	Methods to store the results						*/
/*										*/
/********************************************************************************/

public void saveData(DyviseDatabase db,String snm)
{
   String tnm = db.createTable(DYMAC_REACTION_TABLE,snm,DYMAC_REACTION_FIELDS);

   for (EventHandler eh : event_handlers) {
      String cmd = db.beginInsert(tnm);
      cmd = db.addValue(cmd,eh.getClassName());
      cmd = db.addValue(cmd,eh.getMethodName());
      cmd = db.addValue(cmd,eh.getThreads());
      cmd = db.addValue(cmd,eh.getType());
      db.endInsert(cmd);
    }

   db.runSql();
}



/********************************************************************************/
/*										*/
/*	Class representing an event handler					*/
/*										*/
/********************************************************************************/

private static class EventHandler {

   private String class_name;
   private String method_name;
   private String event_type;
   private String event_threads;

   EventHandler(String cls,String mthd,String thrd,String typ) {
      class_name = cls;
      method_name = mthd;
      event_threads = thrd;
      event_type = typ;
    }

   String getClassName()		{ return class_name; }
   String getMethodName()		{ return method_name; }
   String getType()			{ return event_type; }
   String getThreads()			{ return event_threads; }

   String getUserLabel() {
      String id = getClassName() + "." + getMethodName() + " (" + getType() + ")";
      return id;
    }

   public boolean equals(Object o) {
      if (o instanceof EventHandler) {
	 EventHandler e = (EventHandler) o;
	 return e.class_name.equals(class_name) &&
	    e.method_name.equals(method_name) &&
	    e.event_threads.equals(event_threads) &&
	    e.event_type.equals(event_type);
       }
      return false;
    }

   public int hashCode() {
      return class_name.hashCode() + 3*method_name.hashCode() + 5*event_type.hashCode() +
	 7*event_threads.hashCode();
    }

}	// end of subclass EventHandler


}	// end of interface DymacProfletReaction




/* end of DymacProfletReaction.java */
