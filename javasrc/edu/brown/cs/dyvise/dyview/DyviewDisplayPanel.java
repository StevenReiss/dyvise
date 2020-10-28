/********************************************************************************/
/*										*/
/*		DyviewDisplayPanel.java 					*/
/*										*/
/*	DYname VIEW control panel for setting up visualization			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyview/DyviewDisplayPanel.java,v 1.3 2009-10-07 22:39:51 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyviewDisplayPanel.java,v $
 * Revision 1.3  2009-10-07 22:39:51  spr
 * Eclipse code cleanup.
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


import edu.brown.cs.dyvise.dygraph.DygraphControl;
import edu.brown.cs.dyvise.dygraph.DygraphView;
import edu.brown.cs.dyvise.dyvise.DyviseConstants;

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;


class DyviewDisplayPanel extends DyviewAbstractPanel implements DyviewConstants, DyviseConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<DygraphView,DisplayProperties>	graph_props;
private JButton 				next_button;

private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DyviewDisplayPanel(DyviewWindow dw,DyviewModel mdl)
{
   super(dw,mdl);

   graph_props = new LinkedHashMap<DygraphView,DisplayProperties>();

   setupPanel();
}




/********************************************************************************/
/*										*/
/*	Panel setup methods							*/
/*										*/
/********************************************************************************/

private void setupPanel()
{
   beginLayout();
   addBannerLabel(view_model.getVisual().toString() + " Display");
   addSeparator();

   addSectionLabel("Visualization Settings");
   addDescription("Project",view_model.getProject().toString());
   addDescription("Start Class",view_model.getStartClass());
   addDescription("Visualization",view_model.getVisual().toString());

   Map<String,Object> parms = view_model.getVisual().getParameters();
   for (Map.Entry<String,Object> ent : parms.entrySet()) {
      addDescription(ent.getKey(),ent.getValue());
    }

   addSeparator();

   addSectionLabel("Displays");
   DygraphControl dg = view_model.getGraphics();
   for (DygraphView dv : dg.getViews()) {
      DisplayProperties dpl = new DisplayProperties(dv);
      graph_props.put(dv,dpl);
      Box bx = Box.createHorizontalBox();
      JCheckBox cbx = new JCheckBox("Enabled",view_model.isGraphViewEnabled(dv));
      cbx.addActionListener(dpl);
      bx.add(cbx);
      bx.add(Box.createHorizontalStrut(20));
      JButton adv = new JButton("Set Properties");
      adv.addActionListener(dpl);
      bx.add(adv);
      addRawComponent(dv.getName(),bx);
    }
   addSeparator();

   DisplayButtonListener dbl = new DisplayButtonListener();
   addBottomButton("BACK","BACK",dbl);
   addBottomButton("SAVE","BACK",dbl);
   addBottomButton("QUIT","QUIT",dbl);
   next_button = addBottomButton("VISUALIZE","VISUALIZE",dbl);
   addBottomButtons();

   checkNext();
}



/********************************************************************************/
/*										*/
/*	Update checks								*/
/*										*/
/********************************************************************************/

private void checkNext()
{
   int ct = 0;
   for (DisplayProperties dp : graph_props.values()) {
      if (dp.isEnabled()) ++ct;
    }

   if (ct == 0) next_button.setEnabled(false);
   else next_button.setEnabled(true);
}




/********************************************************************************/
/*										*/
/*	Display button handlers 						*/
/*										*/
/********************************************************************************/

private class DisplayButtonListener implements ActionListener {

   public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      if (cmd.equals("QUIT")) {
	 System.exit(0);
       }
      else if (cmd.equals("BACK")) {
	 for_window.gotoVisualPanel();
       }
      else if (cmd.equals("SAVE")) {
	 System.err.println("DO SAVE HERE");
       }
      else if (cmd.equals("VISUALIZE")) {
	 for_window.gotoControlPanel();
       }
    }


}	// end of inner class DisplayButtonListener



private class DisplayProperties implements ActionListener {

   private DygraphView graph_view;

   DisplayProperties(DygraphView dv) {
      graph_view = dv;
    }

   boolean isEnabled() {
      return view_model.isGraphViewEnabled(graph_view);
    }

   public void actionPerformed(ActionEvent evt) {
      if (evt.getSource() instanceof JCheckBox) {
	 JCheckBox cbx = (JCheckBox) evt.getSource();
	 view_model.enableGraphView(graph_view,cbx.isSelected());
	 checkNext();
       }
      else {
	 JOptionPane.showOptionDialog(DyviewDisplayPanel.this,
						   graph_view.getControlPanel(),
						   "Display Properties for " + graph_view.getName(),
						   JOptionPane.DEFAULT_OPTION,
						   JOptionPane.PLAIN_MESSAGE,
						   null,null,null);
       }
    }

}	// end of subclass DisplayProperties




}	// end of class DyviewDisplayPanel




/* end of DyviewDisplayPanel.java */
