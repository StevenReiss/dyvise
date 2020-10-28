/********************************************************************************/
/*										*/
/*		DyviewWindow.java						*/
/*										*/
/*	DYname window for holding the various control panels			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyview/DyviewWindow.java,v 1.2 2009-10-07 01:00:21 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyviewWindow.java,v $
 * Revision 1.2  2009-10-07 01:00:21  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.1  2009-09-19 00:14:23  spr
 * Viewer with dialog based front end.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dyview;


import edu.brown.cs.dyvise.dygraph.DygraphControl;
import edu.brown.cs.dyvise.dyvise.DyviseConstants;

import javax.swing.JFrame;


class DyviewWindow extends JFrame implements DyviewConstants, DyviseConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private DyviewModel	view_model;
private DyviewAbstractPanel current_panel;

private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DyviewWindow(DyviewModel mdl)
{
   view_model = mdl;

   view_model.addModelListener(new ModelHandler());

   setTitle("DYnamic VIEWer Setup Panel");
   setDefaultCloseOperation(EXIT_ON_CLOSE);

   current_panel = null;


   gotoSetupPanel();
}




/********************************************************************************/
/*										*/
/*	Routines to handle changing the current panel				*/
/*										*/
/********************************************************************************/

void gotoSetupPanel()
{
   setPanel(new DyviewSetupPanel(this,view_model));
}


void gotoVisualPanel()
{
   setPanel(new DyviewVisualPanel(this,view_model));
}


void gotoDisplayPanel()
{
   if (needDisplayPanel()) {
      setPanel(new DyviewDisplayPanel(this,view_model));
    }
   else gotoControlPanel();
}



void gotoControlPanel()
{
   setPanel(new DyviewControlPanel(this,view_model));
}



private void setPanel(DyviewAbstractPanel pnl)
{
   current_panel = pnl;
   setContentPane(pnl);
   pack();
}



boolean needDisplayPanel()
{
   DygraphControl dg = view_model.getGraphics();
   if (dg == null) return false;
   if (dg.getViews().size() > 1) return true;
   return false;
}



/********************************************************************************/
/*										*/
/*	Handle model changes							*/
/*										*/
/********************************************************************************/

private class ModelHandler implements ModelListener {

   public void projectChanged() {
      if (current_panel != null) current_panel.handleProjectChanged();
    }

   public void startClassChanged() {
      if (current_panel != null) current_panel.handleStartClassChanged();
    }

   public void visualChanged() {
      if (current_panel != null) current_panel.handleVisualChanged();
    }

   public void visualReadyChanged() {
      if (current_panel != null) current_panel.handleReadyChanged();
    }

   public void dataUpdated() {
      if (current_panel != null) current_panel.handleDataUpdated();
    }

}	// handle project changed



}	// end of class DyviewWindow.java




/* end of DyviewWindow.java */
