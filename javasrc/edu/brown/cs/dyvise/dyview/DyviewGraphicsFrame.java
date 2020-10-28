/********************************************************************************/
/*										*/
/*		DyviewGraphics.java						*/
/*										*/
/*	Dynamic view graphics frame with controls				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyview/DyviewGraphicsFrame.java,v 1.8 2013-05-09 12:29:05 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyviewGraphicsFrame.java,v $
 * Revision 1.8  2013-05-09 12:29:05  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.7  2011-03-19 20:34:56  spr
 * Code speedup.
 *
 * Revision 1.6  2011-03-10 02:33:25  spr
 * Code cleanup.
 *
 * Revision 1.5  2010-03-30 16:23:25  spr
 * Upgrades to the visualizer to handle new programs and provide better views.
 *
 * Revision 1.4  2009-10-07 22:39:51  spr
 * Eclipse code cleanup.
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


import edu.brown.cs.dyvise.dygraph.DygraphControl;
import edu.brown.cs.dyvise.dygraph.DygraphView;
import edu.brown.cs.dyvise.dystore.DystoreControl;
import edu.brown.cs.dyvise.dyvise.DyviseConstants;
import edu.brown.cs.ivy.swing.*;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

import java.awt.event.*;



class DyviewGraphicsFrame extends JFrame implements DyviewConstants, DyviseConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private DygraphView	display_view;
private SwingRangeScrollBar time_bar;
private SwingRangeScrollBar range_bar;

private static Border bar_border = BorderFactory.createLineBorder(SwingColors.SWING_BORDER_COLOR,1);

private static final int MAX_TIME = 1000;
private static final int MAX_RANGE = 100;


private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DyviewGraphicsFrame(DyviewModel mdl,DygraphView dv)
{
   super(dv.getName());

   display_view = dv;

   setDefaultCloseOperation(HIDE_ON_CLOSE);

   setJMenuBar(new MenuBar());

   setupFrame();
}



/********************************************************************************/
/*										*/
/*	Methods to handle frame layout						*/
/*										*/
/********************************************************************************/

private void setupFrame()
{
   SwingGridPanel pnl = new SwingGridPanel();

   pnl.addGBComponent(display_view.getDisplayPanel(),0,0,1,1,100,100);

   time_bar = new SwingRangeScrollBar(SwingRangeScrollBar.HORIZONTAL,0,MAX_TIME,0,MAX_TIME);
   time_bar.addAdjustmentListener(new TimeChanger());
   time_bar.setBorder(new LineBorder(SwingColors.SWING_BORDER_COLOR,1));
   pnl.addGBComponent(new SwingBorderPanel(bar_border,time_bar),0,1,1,1,100,0);

   range_bar = new SwingRangeScrollBar(SwingRangeScrollBar.VERTICAL,0,MAX_RANGE,0,MAX_RANGE);
   range_bar.addAdjustmentListener(new RangeChanger());
   range_bar.setBorder(new LineBorder(SwingColors.SWING_BORDER_COLOR,1));
   pnl.addGBComponent(new SwingBorderPanel(bar_border,range_bar),1,0,1,1,0,100);

   setContentPane(pnl);

   pack();
}


void setProcess(String pid)
{
   setTitle(display_view.getName() + " @ " + pid);
}



/********************************************************************************/
/*										*/
/*	Automatic update methods						*/
/*										*/
/********************************************************************************/

public void handleDataUpdated()
{
   if (time_bar == null) return;

   SwingUtilities.invokeLater(new TimeUpdater());
}





/********************************************************************************/
/*										*/
/*	Menu bar								*/
/*										*/
/********************************************************************************/

private class MenuBar extends SwingMenuBar {

   private static final long serialVersionUID = 1;

   MenuBar() {
      JMenu fm = new JMenu("File");
      addButton(fm,"Close","Close this display");
      add(fm);
    }

   public void actionPerformed(ActionEvent e) {
      String cmd = e.getActionCommand();
      if (cmd.equals("Close")) {
	 DyviewGraphicsFrame.this.setVisible(false);
       }
    }

}	// end of innerclass MenuBar




/********************************************************************************/
/*										*/
/*	Handle time scrolling							*/
/*										*/
/********************************************************************************/

private class TimeChanger implements AdjustmentListener {

   public void adjustmentValueChanged(AdjustmentEvent e) {
      double v0 = time_bar.getLeftValue();
      double v1 = time_bar.getRightValue();
      DygraphControl dc = display_view.getControl();
      DystoreControl ds = dc.getTupleStore();
      double x0 = ds.getStartTime();
      double x1 = ds.getEndTime();
      double t0,t1;
      if (x1 == x0) {
	 t0 = 0;
	 t1 = MAX_TIME;
       }
      else {
	 t0 = (x0 + v0*(x1-x0)/MAX_TIME);
	 t1 = (x0 + v1*(x1-x0)/MAX_TIME);
       }
      boolean dyn = (v1 == MAX_TIME);

      dc.setTimeWindow(t0,t1,dyn);
    }

}	// end of innerclass TimeChanger



private class RangeChanger implements AdjustmentListener {

   public void adjustmentValueChanged(AdjustmentEvent e) {
      double v0 = ((double)(range_bar.getLeftValue())) / MAX_RANGE;
      double v1 = ((double)(range_bar.getRightValue())) / MAX_RANGE;

      // display_view.setYDataRegion(v0,v1);
      display_view.setYDataRegion(1.0-v1,1.0-v0);
    }

}	// end of innerclass RangeChanger




private class TimeUpdater implements Runnable {

   public void run() {
      DygraphControl dc = display_view.getControl();
      DystoreControl ds = dc.getTupleStore();
      double x0 = ds.getStartTime();
      double x1 = ds.getEndTime();
      double d0 = dc.getDisplayStartTime();
      double d1 = dc.getDisplayEndTime();
      int orv = time_bar.getRightValue();
   
      int t0,t1;
   
      if (x0 == x1) {
         t0 = 0;
         t1 = MAX_TIME;
       }
      else {
         t0 = (int)((d0-x0)/(x1-x0)*MAX_TIME);
         t1 = (int)((d1-x0)/(x1-x0)*MAX_TIME);
       }
   
      if (orv == MAX_TIME) t1 = MAX_TIME;
   
      time_bar.setValues(t0,t1);
    }

}	// end of inner class TimeUpdater


}	// end of class DyviewGraphicFrame



/* end of DyviewGraphicsFrame.java */
