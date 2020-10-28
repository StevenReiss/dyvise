/********************************************************************************/
/*										*/
/*		DymacProfletStates.java 					*/
/*										*/
/*	DYVISE dynamic analysis proflet interface for state identification	*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymac/DymacProfletStates.java,v 1.4 2011-03-10 02:33:04 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymacProfletStates.java,v $
 * Revision 1.4  2011-03-10 02:33:04  spr
 * Code cleanup.
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
import edu.brown.cs.ivy.limbo.LimboFactory;
import edu.brown.cs.ivy.limbo.LimboLine;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.swing.*;

import org.w3c.dom.Element;

import javax.swing.*;
import java.io.File;
import java.util.*;



class DymacProfletStates extends DymacProfletBase implements DymacProflet, DyviseConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private DymacMain	dymac_main;
private Set<ThreadState> thread_states;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DymacProfletStates(DymacMain dm)
{
   dymac_main = dm;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public String getName() 			{ return "STATES"; }

public String getAgentName()			{ return "STATES"; }



/********************************************************************************/
/*										*/
/*	Data acquisition methods						*/
/*										*/
/********************************************************************************/

public boolean processData(Element xml)
{
   Element r = IvyXml.getChild(xml,"STATES");
   if (r == null) return true;
   Set<ThreadState> rslt = new HashSet<ThreadState>();

   for (Element ts : IvyXml.children(r,"STATE")) {
      rslt.add(new ThreadState(IvyXml.getAttrString(ts,"CLASS"),
				  IvyXml.getAttrString(ts,"METHOD"),
				  IvyXml.getAttrInt(ts,"LINE"),
				  IvyXml.getAttrString(ts,"THREAD"),
				  IvyXml.getAttrString(ts,"TYPE")));
    }

   if (rslt.equals(thread_states)) return false;

   thread_states = rslt;

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
   pnl.addBannerLabel("Verify Thread States from Dynamic Analysis");
   pnl.addSeparator();

   Map<JCheckBox,ThreadState> fldmap = new HashMap<JCheckBox,ThreadState>();

   for (ThreadState ts : thread_states) {
      String id = ts.getUserLabel();
      pnl.addBoolean(id,true,null);
    }
   pnl.addSeparator();

   int sts = JOptionPane.showOptionDialog(null,pnl,"Thread State Verification",
					     JOptionPane.OK_CANCEL_OPTION,
					     JOptionPane.PLAIN_MESSAGE,
					     null,null,null);
   if (sts != 0) return false;

   for (Map.Entry<JCheckBox,ThreadState> ent : fldmap.entrySet()) {
      JCheckBox cbx = ent.getKey();
      if (!cbx.isSelected()) {
	 thread_states.remove(ent.getValue());
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
   String tnm = db.createTable(DYMAC_THREAD_STATE_TABLE,snm,DYMAC_THREAD_STATE_FIELDS);

   for (ThreadState ts : thread_states) {
      String lineinfo = getLineInfo(ts.getClassName(),ts.getLineNumber());
      String cmd = db.beginInsert(tnm);
      cmd = db.addValue(cmd,ts.getClassName());
      cmd = db.addValue(cmd,ts.getMethodName());
      cmd = db.addValue(cmd,ts.getLineNumber());
      cmd = db.addValue(cmd,ts.getThread());
      cmd = db.addValue(cmd,ts.getType());
      cmd = db.addValue(cmd,lineinfo);
      db.endInsert(cmd);
    }

   db.runSql();
}



/********************************************************************************/
/*										*/
/*	Record information for reconstructing line				*/
/*										*/
/********************************************************************************/

private String getLineInfo(String cls,int line)
{
   if (line <= 0 || cls == null) return null;
   File f = dymac_main.getProject().findSourceFile(cls);
   if (f == null) return null;

   LimboLine ll = LimboFactory.createLine(f,line);

   return ll.getXml();
}




/********************************************************************************/
/*										*/
/*	Class representing an event handler					*/
/*										*/
/********************************************************************************/

private static class ThreadState {

   private String state_type;
   private String class_name;
   private String method_name;
   private int line_number;
   private String thread_name;

   ThreadState(String cls,String mthd,int line,String thrd,String typ) {
      class_name = cls;
      method_name = mthd;
      line_number = line;
      thread_name = thrd;
      state_type = typ;
    }

   String getClassName()		{ return class_name; }
   String getMethodName()		{ return method_name; }
   int getLineNumber()			{ return line_number; }
   String getType()			{ return state_type; }
   String getThread()			{ return thread_name; }

   String getUserLabel() {
      String id = getClassName() + "." + getMethodName();
      if (getLineNumber() > 0) id += " @" + getLineNumber();
      id += " " + getType();
      return id;
    }

   public boolean equals(Object o) {
      if (o instanceof ThreadState) {
	 ThreadState e = (ThreadState) o;
	 return e.class_name.equals(class_name) &&
	    e.method_name.equals(method_name) &&
	    e.thread_name.equals(thread_name) &&
	    e.line_number == line_number &&
	    e.state_type.equals(state_type);
       }
      return false;
    }

   public int hashCode() {
      return class_name.hashCode() + 3*method_name.hashCode() + 5*state_type.hashCode() +
	 7*line_number + 11*thread_name.hashCode();
    }

}	// end of subclass ThreadState


}	// end of interface DymacProfletStates




/* end of DymacProfletStates.java */



