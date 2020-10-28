/********************************************************************************/
/*										*/
/*		DyviewVisualThreadState.java					*/
/*										*/
/*	DYname VIEW thread state visualization controller			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyview/DyviewVisualThreadState.java,v 1.6 2012-10-05 00:53:05 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyviewVisualThreadState.java,v $
 * Revision 1.6  2012-10-05 00:53:05  spr
 * Code clean up.
 *
 * Revision 1.5  2010-03-30 16:23:26  spr
 * Upgrades to the visualizer to handle new programs and provide better views.
 *
 * Revision 1.4  2009-10-07 01:00:21  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.3  2009-09-27 23:57:44  spr
 * Updates for mac os/x.
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


import edu.brown.cs.dyvise.dyvise.DyviseConstants;
import edu.brown.cs.dyvise.dyvise.DyviseDatabase;
import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import javax.swing.JPanel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;



class DyviewVisualThreadState extends DyviewVisual implements DyviewConstants, DyviseConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private DyviewClassPanel	thread_panel;
private DyviewClassPanel	transaction_panel;
private DyviewClassPanel	task_panel;
private Collection<String>	thread_used;
private Collection<String>	transaction_used;
private Collection<String>	task_used;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DyviewVisualThreadState(DyviewModel vm)
{
   super(vm);

   thread_panel = null;
   transaction_panel = null;
   task_panel = null;
   thread_used = new ArrayList<String>();
   transaction_used = new ArrayList<String>();
   task_used = new ArrayList<String>();
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public @Override Collection<String> getProflets()
{
   Collection<String> r = new ArrayList<String>();

   r.add("EVENTS");
   r.add("STATES");

   return r;
}


/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

public @Override String getIdName()
{
   return "ThreadState";
}


public String toString()
{
   return "Thread State Visualization";
}



/********************************************************************************/
/*										*/
/*	Setup panel methods							*/
/*										*/
/********************************************************************************/

public JPanel setupPanel(DyviewModel vm)
{
   SwingGridPanel pnl = new SwingGridPanel();

   pnl.beginLayout();

   Collection<String> cands1 = computeCandidates(0.20);
   Collection<String> cands2 = computeCandidates(0);

   CheckReady cr = new CheckReady();
   thread_panel = new DyviewClassPanel(vm,"Thread Class",true);
   thread_panel.requireUserClass(true);
   thread_panel.addSuperClass("java.lang.Runnable");
   thread_panel.addSuperClass("java.lang.Thread");
   thread_panel.addActionListener(cr);
   pnl.addRawComponent("Thread Class",thread_panel);

   transaction_panel = new DyviewClassPanel(vm,"Transaction Class",true);
   transaction_panel.requireUserClass(true);
   transaction_panel.addDefaultClass(cands1);
   transaction_panel.addActionListener(cr);
   pnl.addRawComponent("Transaction Class",transaction_panel);

   task_panel = new DyviewClassPanel(vm,"Task Class",true);
   task_panel.requireUserClass(false);
   task_panel.addDefaultClass(cands2);
   task_panel.addActionListener(cr);
   pnl.addRawComponent("Task Class",task_panel);

   for (String s : thread_used) thread_panel.addChosenClass(s);
   for (String s : transaction_used) transaction_panel.addChosenClass(s);
   for (String s : task_used) task_panel.addChosenClass(s);

   return pnl;
}



/********************************************************************************/
/*										*/
/*	Methods to compute candidate classes for transactions/tasks		*/
/*										*/
/********************************************************************************/

private Collection<String> computeCandidates(double cutoff)
{
   String rnm = DyviseDatabase.getTableName("DynEventCandidates",view_model.getStartClass());

   Collection<String> rslt = new ArrayList<String>();

   String q = "SELECT * FROM " + rnm;
   if (cutoff > 0) q += " WHERE fraction > " + cutoff;

   ResultSet rs = view_model.queryDatabase(q);
   try {
      while (rs.next()) {
	 String c = rs.getString(1);
	 rslt.add(c);
       }
      rs.close();
    }
   catch (SQLException e) {
      System.err.println("DYVIEW: Problem getting candidate list: " + e);
    }

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Method to check if we are ready 					*/
/*										*/
/********************************************************************************/

public @Override boolean isReady()
{
   if (thread_panel == null) return false;

   if (transaction_panel == null) return false;
   if (transaction_panel.getClasses().size() == 0) return false;

   if (task_panel == null) return false;

   return true;
}



private class CheckReady implements ActionListener {

   public void actionPerformed(ActionEvent evt) {
      view_model.setVisualReady(isReady());
    }

}



/********************************************************************************/
/*										*/
/*	Methods to handle relation setup when ready				*/
/*										*/
/********************************************************************************/

public @Override String setupRelations()
{
   IvyXmlWriter xw = new IvyXmlWriter();
   String restrict = null;

   xw.begin("ANALYSIS");

   if (!thread_panel.isEmpty()) {
      xw.begin("COMPUTE");
      xw.field("TYPE","METHODTHREAD");
      xw.begin("RESULT");
      xw.field("START",true);
      xw.text("CompThreadAccess");
      xw.end("RESULT");
      xw.begin("SOURCE");
      xw.field("START",true);
      xw.text("DynEventRoutines");
      xw.end("SOURCE");
      xw.begin("SOURCE");
      xw.field("START",true);
      xw.text("DynThreadStates");
      xw.end("SOURCE");
      for (String s :  thread_panel.getClasses()) {
	 xw.textElement("THREAD",s);
       }
      xw.end("COMPUTE");
      String rnm = DyviseDatabase.getTableName("CompThreadAccess",view_model.getStartClass());
      restrict = "((SOURCE.class, SOURCE.method) in (SELECT class,method FROM " + rnm + "))";
    }



   xw.begin("COMPUTE");
   xw.field("TYPE","METHODTYPE");
   xw.begin("RESULT");
   xw.field("START",true);
   xw.text("CompEventAccess");
   xw.end("RESULT");
   xw.begin("SOURCE");
   xw.field("START",true);
   xw.text("DynEventRoutines");
   xw.end("SOURCE");
   if (restrict != null) xw.textElement("RESTRICT",restrict);
   for (String s :  transaction_panel.getClasses()) {
      xw.textElement("TARGET",s);
    }
   for (String s :  task_panel.getClasses()) {
      xw.textElement("TASK",s);
    }
   xw.end("COMPUTE");

   xw.begin("COMPUTE");
   xw.field("TYPE","ALLOCTYPE");
   xw.begin("RESULT");
   xw.field("START",true);
   xw.text("CompEventAlloc");
   xw.end("RESULT");
   for (String s :  transaction_panel.getClasses()) {
      xw.begin("TARGET");
      xw.field("WHICH",1);
      xw.text(s);
      xw.end("TARGET");
    }
   for (String s :  task_panel.getClasses()) {
      xw.begin("TARGET");
      xw.field("WHICH",2);
      xw.text(s);
      xw.end("TARGET");
    }
   xw.end("COMPUTE");

   xw.begin("COMPUTE");
   xw.field("TYPE","VIEW");
   xw.begin("RESULT");
   xw.field("START",true);
   xw.text("CompThreadStates");
   xw.end("RESULT");
   xw.begin("SOURCE");
   xw.field("START",true);
   xw.text("DynThreadStates");
   xw.end("SOURCE");
   if (restrict != null) xw.textElement("RESTRICT",restrict);
   xw.end("COMPUTE");

   xw.begin("COMPUTE");
   xw.field("TYPE","VIEW");
   xw.begin("RESULT");
   xw.field("START",true);
   xw.text("CompEventRoutines");
   xw.end("RESULT");
   xw.begin("SOURCE");
   xw.field("START",true);
   xw.text("DynEventRoutines");
   xw.end("SOURCE");
   if (restrict != null) xw.textElement("RESTRICT",restrict);
   xw.end("COMPUTE");

   xw.end("ANALYSIS");

   String rslt = xw.toString();
   xw.close();
   return rslt;
}




/********************************************************************************/
/*										*/
/*	Method to return parameters						*/
/*										*/
/********************************************************************************/

public @Override Map<String,Object> getParameters()
{
   Map<String,Object> rslt = new LinkedHashMap<String,Object>();
   rslt.put("Thread Class",thread_panel.getClasses());
   rslt.put("Transaction Class",transaction_panel.getClasses());
   rslt.put("Task Class",task_panel.getClasses());

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Methods to setup patch model						*/
/*										*/
/********************************************************************************/

public @Override String getPatchModel()
{
   IvyXmlWriter xw = new IvyXmlWriter();

   xw.begin("DYNAMO");
   xw.field("METHOD","EVENTSTATE");
   xw.begin("SET");
   xw.field("NAME","EVENTS");
   xw.field("VALUE",DyviseDatabase.getTableName("CompEventAccess",view_model.getStartClass()));
   xw.end("SET");
   xw.begin("SET");
   xw.field("NAME","ALLOCS");
   xw.field("VALUE",DyviseDatabase.getTableName("CompEventAlloc",view_model.getStartClass()));
   xw.end("SET");
   xw.begin("SET");
   xw.field("NAME","THREADSTATES");
   xw.field("VALUE",DyviseDatabase.getTableName("CompThreadStates",view_model.getStartClass()));
   xw.end("SET");
   xw.begin("SET");
   xw.field("NAME","EVENTROUTINES");
   xw.field("VALUE",DyviseDatabase.getTableName("CompEventRoutines",view_model.getStartClass()));
   xw.end("SET");
   xw.end("DYNAMO");

   String rslt = xw.toString();
   xw.close();
   return rslt;
}



/********************************************************************************/
/*										*/
/*	Load/Store methods							*/
/*										*/
/********************************************************************************/

public @Override void outputXml(IvyXmlWriter xw)
{
   for (String s : thread_panel.getClasses()) {
      xw.textElement("THREAD",s);
    }
   for (String s : transaction_panel.getClasses()) {
      xw.textElement("TRANSACTION",s);
    }
   for (String s : task_panel.getClasses()) {
      xw.textElement("TASK",s);
    }
}



public @Override void loadXml(Element xml)
{
   thread_used.clear();
   transaction_used.clear();
   task_used.clear();

   for (Element c : IvyXml.children(xml)) {
      if (IvyXml.isElement(c,"THREAD")) {
	 thread_used.add(IvyXml.getText(c));
       }
      else if (IvyXml.isElement(c,"TRANSACTION")) {
	 transaction_used.add(IvyXml.getText(c));
       }
      else if (IvyXml.isElement(c,"TASK")) {
	 task_used.add(IvyXml.getText(c));
       }
    }
}



}	// end of class DyviewVisualThreadState




/* end of DyviewVisualThreadState.java */

