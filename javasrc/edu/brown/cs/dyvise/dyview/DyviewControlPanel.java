/********************************************************************************/
/*										*/
/*		DyviewControlPanel.java 					*/
/*										*/
/*	DYnamic VIEW control panel for setting up visualization 		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyview/DyviewControlPanel.java,v 1.6 2013-05-09 12:29:05 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyviewControlPanel.java,v $
 * Revision 1.6  2013-05-09 12:29:05  spr
 * Dylock update.	Last 1.6 update
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
import edu.brown.cs.dyvise.dyvise.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.*;
import javax.swing.Timer;

class DyviewControlPanel extends DyviewAbstractPanel implements DyviewConstants, DyviseConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private JButton 	run_button;
private JButton 	trace_button;
private DyviewRunner	current_process;
private List<DyviewGraphicsFrame> active_views;
private JFileChooser	save_chooser;
private JFileChooser	trace_chooser;

private static final int CHECK_INTERVAL = 10000;

private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DyviewControlPanel(DyviewWindow dw,DyviewModel mdl)
{
   super(dw,mdl);

   current_process = null;
   active_views = new ArrayList<DyviewGraphicsFrame>();

   setupPanel();

   save_chooser = new JFileChooser();
   FileNameExtensionFilter filter = new FileNameExtensionFilter("Dyview Files","dyview");
   save_chooser.setFileFilter(filter);
   save_chooser.setDialogTitle("Enter File to Save Settings In");

   trace_chooser = new JFileChooser();
   FileNameExtensionFilter f1 = new FileNameExtensionFilter("Dyview Trace Files","dytrace");
   trace_chooser.setFileFilter(f1);
   trace_chooser.setDialogTitle("Dyview Trace File");

   new Timer(CHECK_INTERVAL,new ActiveChecker());
}




/********************************************************************************/
/*										*/
/*	Panel setup methods							*/
/*										*/
/********************************************************************************/

private void setupPanel()
{
   beginLayout();
   addBannerLabel(view_model.getVisual().toString() + " Visualization Control");
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

   DygraphControl dg = view_model.getGraphics();
   for (DygraphView dv : dg.getViews()) {
      if (view_model.isGraphViewEnabled(dv)) {
	 addSectionLabel(dv.getName());
	 addLabellessRawComponent(dv.getName(),dv.getControlPanel());
	 createVisualizer(dv);
	 addSeparator();
       }
    }

   ControlButtonListener dbl = new ControlButtonListener();
   addBottomButton("SAVE","SAVE",dbl);
   addBottomButton("QUIT","QUIT",dbl);
   trace_button = addBottomButton("SAVE TRACE","SAVE TRACE",dbl);
   trace_button.setEnabled(false);
   addBottomButton("LOAD TRACE","LOAD TRACE",dbl);
   addBottomButton("CLEAR","CLEAR",dbl);
   run_button = addBottomButton("ATTACH","ATTACH",dbl);
   addBottomButton("SHOW","SHOW",dbl);
   addBottomButtons();
}



/********************************************************************************/
/*										*/
/*	Manage the actual visualization windows 				*/
/*										*/
/********************************************************************************/

private void createVisualizer(DygraphView dv)
{
   DyviewGraphicsFrame frm = new DyviewGraphicsFrame(view_model,dv);

   active_views.add(frm);

   // user should use the show button to create views when ready
   // frm.setVisible(true);
}



/********************************************************************************/
/*										*/
/*	Methods for handling run						*/
/*										*/
/********************************************************************************/

private void runProcess()
{
   DyviseProcessChooser pc = new DyviseProcessChooser(view_model.getDymonHandle(),
							 view_model.getStartClass(),
							 "ATTACH");
   String pid = pc.requestProcess(for_window);
   if (pid == null) return;

   run_button.setEnabled(false);
   trace_button.setEnabled(false);

   current_process = new DyviewRunner(view_model,pid);

   for (DyviewGraphicsFrame dgf : active_views) dgf.setProcess(pid);

   StringBuffer buf = new StringBuffer();
   buf.append("<html><p>Attaching to and Patching Process " + pid);

   JOptionPane opt = new JOptionPane(buf.toString(),
					JOptionPane.INFORMATION_MESSAGE,
					JOptionPane.DEFAULT_OPTION,
					null,null);

   JDialog dlg = opt.createDialog(this,"Process Setup Waiter");
   dlg.setModal(true);

   ProcessSetup ps = new ProcessSetup(dlg,current_process);

   ps.start();
   dlg.setVisible(true);

   dlg.setVisible(false);
   dlg.dispose();

   run_button.setText("STOP");
   run_button.setActionCommand("STOP");
   run_button.setEnabled(true);
}




private class ProcessSetup extends Thread {

   private JDialog wait_dialog;
   private DyviewRunner for_process;

   ProcessSetup(JDialog dlg,DyviewRunner dr) {
      super("Process Setup Waiter");
      wait_dialog = dlg;
      for_process = dr;
    }

   public void run() {
      while (wait_dialog != null && !wait_dialog.isVisible()) {
	 try {
	    Thread.sleep(1);
	  }
	 catch (InterruptedException e) { }
       }

      for_process.setupProcess();
      for_process.waitUntilSetup();

      if (wait_dialog != null) {
	 wait_dialog.setVisible(false);
       }
    }

}	// end of subclass ProcessSetup




/********************************************************************************/
/*										*/
/*	Commands to stop a process						*/
/*										*/
/********************************************************************************/

private void stopProcess()
{
   if (current_process != null) current_process.removeProcess();
   current_process = null;

   run_button.setText("ATTACH");
   run_button.setActionCommand("ATTACH");
   run_button.setEnabled(true);
   trace_button.setEnabled(true);
}




/********************************************************************************/
/*										*/
/*	Dynamic update methods							*/
/*										*/
/********************************************************************************/

void handleDataUpdated()
{
   for (DyviewGraphicsFrame dgf : active_views) dgf.handleDataUpdated();
}



/********************************************************************************/
/*										*/
/*	Control button handlers 						*/
/*										*/
/********************************************************************************/

private class ControlButtonListener implements ActionListener {

   public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      if (cmd.equals("QUIT")) {
         System.exit(0);
       }
      else if (cmd.equals("SAVE")) {
         int sts = save_chooser.showSaveDialog(DyviewControlPanel.this);
         if (sts == JFileChooser.APPROVE_OPTION) {
            try {
               view_model.saveTo(save_chooser.getSelectedFile());
             }
            catch (IOException e) {
               JOptionPane.showMessageDialog(DyviewControlPanel.this,
        					"Problem saving file: " + e,
        					"Save Error",
        					JOptionPane.ERROR_MESSAGE);
             }
          }
       }
      else if (cmd.equals("ATTACH")) {
         runProcess();
       }
      else if (cmd.equals("SAVE TRACE")) {
         int sts = trace_chooser.showSaveDialog(DyviewControlPanel.this);
         if (sts == JFileChooser.APPROVE_OPTION) {
            try {
               view_model.getStore().save(trace_chooser.getSelectedFile());
             }
            catch (IOException e) {
               JOptionPane.showMessageDialog(DyviewControlPanel.this,
        					"Problem saving file: " + e,
        					"Save Error",
        					JOptionPane.ERROR_MESSAGE);
             }
          }
       }
      else if (cmd.equals("LOAD TRACE")) {
         int sts = trace_chooser.showOpenDialog(DyviewControlPanel.this);
         if (sts == JFileChooser.APPROVE_OPTION) {
            try {
               view_model.getStore().load(trace_chooser.getSelectedFile());
               view_model.getGraphics().dataUpdated();
             }
            catch (IOException e) {
               JOptionPane.showMessageDialog(DyviewControlPanel.this,
        					"Problem saving file: " + e,
        					"Save Error",
        					JOptionPane.ERROR_MESSAGE);
             }
          }
       }
      else if (cmd.equals("CLEAR")) {
         view_model.getStore().clear(true);
       }
      else if (cmd.equals("STOP")) {
         stopProcess();
       }
      else if (cmd.equals("SHOW")) {
         for (DyviewGraphicsFrame f : active_views) f.setVisible(true);
       }
    }


}	// end of inner class ControlButtonListener



/********************************************************************************/
/*										*/
/*	Methods to check if process still active				*/
/*										*/
/********************************************************************************/

private class ActiveChecker implements ActionListener {

   public void actionPerformed(ActionEvent evt) {
      DyviewRunner dr = current_process;
      if (dr == null) return;
      if (dr.checkActive()) return;
      current_process = null;
      stopProcess();
    }

}	// end of inner class ActiveChecker



}	// end of class DyviewControlPanel




/* end of DyviewControlPanel.java */
