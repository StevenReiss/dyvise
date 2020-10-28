/********************************************************************************/
/*										*/
/*		DyviewStartPanel.java						*/
/*										*/
/*	DYname VIEW panel for choosing starting class				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyview/DyviewStartPanel.java,v 1.3 2013-06-03 13:02:59 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyviewStartPanel.java,v $
 * Revision 1.3  2013-06-03 13:02:59  spr
 * Minor bug fixes
 *
 * Revision 1.2  2009-10-07 01:00:21  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.1  2009-09-19 00:14:23  spr
 * Viewer with dialog based front end.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dyview;


import edu.brown.cs.dyvise.dyvise.DyviseConstants;
import edu.brown.cs.ivy.swing.SwingListSet;

import javax.swing.JComboBox;

import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;



class DyviewStartPanel extends JComboBox<String> implements ActionListener,
	DyviewConstants, DyviseConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private DyviewModel	view_model;
private SwingListSet<String>	candidate_classes;


private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DyviewStartPanel(DyviewModel mdl,String dflt)
{
   view_model = mdl;

   candidate_classes = new SwingListSet<String>(true);
   computeCandidates();

   if (dflt != null) candidate_classes.setSelectedItem(dflt);
   setEditable(true);
   setModel(candidate_classes);
}




/********************************************************************************/
/*										*/
/*	Methods to set and get the value					*/
/*										*/
/********************************************************************************/

String getStartClass()			{ return (String) getSelectedItem(); }

void setStartClass(String s)
{
   if (s != null) s = s.intern();

   setSelectedItem(s);
   candidate_classes.setSelectedItem(s);
   candidate_classes.update();
}



void recompute()
{
   computeCandidates();
}



/********************************************************************************/
/*										*/
/*	Method to find the start classes in the current project 		*/
/*										*/
/********************************************************************************/

private void computeCandidates()
{
   boolean havechng = false;

   String o = (String) candidate_classes.getSelectedItem();

   String q = "SELECT M.class from SrcMethod M";
   q += " WHERE M.name = 'main' AND M.static AND M.public AND M.type= '([Ljava/lang/String;)V'";

   ResultSet rs = view_model.queryDatabase(q);

   if (rs == null) {
      candidate_classes.removeAll();
      return;
    }

   Set<String> upd = new HashSet<String>();
   try {
      while (rs.next()) {
	 String cl = rs.getString(1);
	 upd.add(cl);
       }
      rs.close();
    }
   catch (SQLException e) { }

   for (Iterator<String> it = candidate_classes.iterator(); it.hasNext(); ) {
      String cl = it.next();
      if (upd.contains(cl)) upd.remove(cl);
      else {
	 it.remove();
	 havechng = true;
       }
    }
   if (upd.size() > 0) {
      for (String s : upd) candidate_classes.addElement(s.intern());
    }

   if (o != null && !candidate_classes.contains(o)) {
      candidate_classes.setSelectedItem(null);
      havechng = true;
    }

   if (havechng) candidate_classes.update();
}




}	// end of class DyviewStartPanel




/* end of DyviewStartPanel.java */


