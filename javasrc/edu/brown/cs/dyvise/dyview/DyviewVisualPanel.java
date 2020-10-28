/********************************************************************************/
/*										*/
/*		DyviewVisualPanel.java						*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyview/DyviewVisualPanel.java,v 1.5 2011-03-10 02:33:25 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyviewVisualPanel.java,v $
 * Revision 1.5  2011-03-10 02:33:25  spr
 * Code cleanup.
 *
 * Revision 1.4  2010-03-30 16:23:26  spr
 * Upgrades to the visualizer to handle new programs and provide better views.
 *
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


import edu.brown.cs.dyvise.dycomp.DycompMain;
import edu.brown.cs.dyvise.dynamo.DynamoPatchSetup;
import edu.brown.cs.dyvise.dyvise.DyviseConstants;
import edu.brown.cs.ivy.xml.IvyXml;

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;


class DyviewVisualPanel extends DyviewAbstractPanel implements DyviewConstants, DyviseConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private JButton 	visnext_button;

private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DyviewVisualPanel(DyviewWindow dw,DyviewModel mdl)
{
   super(dw,mdl);

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
   addBannerLabel(view_model.getVisual().toString() + " Setup");
   addSeparator();

   addDescription("Project",view_model.getProject().toString());
   addDescription("Start Class",view_model.getStartClass());
   addDescription("Visualization",view_model.getVisual().toString());
   addSeparator();

   JPanel spnl = view_model.getVisual().setupPanel(view_model);
   addLabellessRawComponent("OPTIONS",spnl);
   addSeparator();

   VisualButtonListener vbl = new VisualButtonListener();
   addBottomButton("BACK","BACK",vbl);
   addBottomButton("QUIT","QUIT",vbl);
   visnext_button = addBottomButton("NEXT","NEXT",vbl);
   visnext_button.setEnabled(false);
   addBottomButtons();

   checkVisualization();
}



/********************************************************************************/
/*										*/
/*	Methods to handle setup after visual is ready				*/
/*										*/
/********************************************************************************/

private void checkVisualization()
{
   if (visnext_button != null) visnext_button.setEnabled(view_model.isVisualReady());
}



/********************************************************************************/
/*										*/
/*	Methods to check if computation is needed				*/
/*										*/
/********************************************************************************/

private boolean checkIfVisualizationSetup()
{
   String key = getVisualKey();
   Date d1 = view_model.getUpdateTime("DYNAMIC",view_model.getStartClass());
   Date d2 = view_model.getUpdateTime("VISUALSETUP",key);
   Date d3 = view_model.getUpdateTime("STATIC",null);
   if (d1 == null || d2 == null || d3 == null ||
	  d2.compareTo(d1) < 0 || d2.compareTo(d3) < 0)
      return false;

   return true;
}



private void updateSetupTime()
{
   String key = getVisualKey();

   String u0 = "DELETE FROM UpdateTimes WHERE what = 'VISUALSETUP'" +
      " AND main LIKE '" + view_model.getStartClass() + "@%'";
   view_model.executeSql(u0);

   if (key != null) view_model.updateTime("VISUALSETUP",key);
}



private String getVisualKey()
{
   MessageDigest md;

   try {
      md = MessageDigest.getInstance("MD5");
    }
   catch (NoSuchAlgorithmException e) {
      return null;
    }

   DyviewVisual dv = view_model.getVisual();
   Map<String,Object> m = dv.getParameters();
   for (Map.Entry<String,Object> ent : m.entrySet()) {
      md.update(ent.getKey().getBytes());
      Object v = ent.getValue();
      if (v instanceof Collection<?>) {
	 Object [] vals = ((Collection<?>) v).toArray();
	 for (int i = 0; i < vals.length; ++i) {
	    String s = vals[i].toString();
	    md.update(s.getBytes());
	  }
       }
      else {
	 md.update(v.toString().getBytes());
       }
    }

   byte[] d = md.digest();
   long vl = 0;
   for (int i = 0; i < 4; ++i) {
      long vb = d[i]&0xff;
      vl = (vl << 8) + vb;
    }
   String key = view_model.getStartClass() + "@" + vl;

   return key;
}



/********************************************************************************/
/*										*/
/*	Methods to compute relations need for visual				*/
/*										*/
/********************************************************************************/

private boolean setupVisualization()
{
   Object [] opts = new Object [] { "CANCEL" };
   StringBuffer buf = new StringBuffer();
   buf.append("<html><p>Setting up visualization.");

   JOptionPane opt = new JOptionPane(buf.toString(),
					JOptionPane.INFORMATION_MESSAGE,
					JOptionPane.DEFAULT_OPTION,
					null,opts);

   JDialog dlg = opt.createDialog(this,"Visualization Setup Waiter");
   dlg.setModal(true);

   VisualUpdater vup = new VisualUpdater(dlg);

   vup.start();
   dlg.setVisible(true);

   return vup.getStatus();
}




private class VisualUpdater extends Thread {

   private JDialog wait_dialog;
   private boolean result_status;
   private DycompMain comp_main;

   VisualUpdater(JDialog dlg) {
      super("Visual Setup");
      wait_dialog = dlg;
      result_status = false;
      comp_main = new DycompMain(view_model.getProject());
    }

   boolean getStatus()				{ return result_status; }

   public void run() {
      while (wait_dialog != null && !wait_dialog.isActive()) {
	 try {
	    Thread.sleep(1);
	  }
	 catch (InterruptedException e) { }
       }

      // String key = getVisualKey();
      boolean sts = true;
      if (!checkIfVisualizationSetup()) {
	 String comp = view_model.getVisual().setupRelations();
	 if (comp != null) {
	    sts = comp_main.processXml(comp,view_model.getStartClass());
	  }
       }
      String pm = view_model.getVisual().getPatchModel();
      if (pm != null && sts) {
	 DynamoPatchSetup dps = new DynamoPatchSetup(view_model.getProject(),pm);
	 String rslt = dps.setupPatchModel();
	 if (rslt == null) sts = false;
	 view_model.setPatchModel(IvyXml.convertStringToXml(rslt));
       }
      result_status = sts;

      if (wait_dialog != null) {
	 wait_dialog.setVisible(false);
	 wait_dialog.dispose();
	 updateSetupTime();
       }
    }

}	// end of inner class VisualUpdater





/********************************************************************************/
/*										*/
/*	Model change methods							*/
/*										*/
/********************************************************************************/

void handleReadyChanged()
{
   checkVisualization();
}



/********************************************************************************/
/*										*/
/*	Visual button handlers							*/
/*										*/
/********************************************************************************/

private class VisualButtonListener implements ActionListener {

   public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      if (cmd.equals("QUIT")) {
	 System.exit(0);
       }
      else if (cmd.equals("BACK")) {
	 for_window.gotoSetupPanel();
       }
      else if (cmd.equals("NEXT")) {
	 if (setupVisualization()) for_window.gotoDisplayPanel();
       }
    }

}	// end of inner class VisualButtonListener




}	// end of class DyviewVisualPanel




/* end of DyviewVisualPanel.java */
