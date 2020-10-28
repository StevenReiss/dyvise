/********************************************************************************/
/*										*/
/*		DyviewSetupPanel.java						*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyview/DyviewSetupPanel.java,v 1.6 2013-06-03 13:02:59 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyviewSetupPanel.java,v $
 * Revision 1.6  2013-06-03 13:02:59  spr
 * Minor bug fixes
 *
 * Revision 1.5  2012-10-05 00:53:04  spr
 * Code clean up.
 *
 * Revision 1.4  2010-06-01 02:46:04  spr
 * Minor bug fixes.
 *
 * Revision 1.3  2010-03-30 16:23:26  spr
 * Upgrades to the visualizer to handle new programs and provide better views.
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
import edu.brown.cs.dyvise.dymac.DymacMain;
import edu.brown.cs.dyvise.dymon.DymonRemote;
import edu.brown.cs.dyvise.dyvise.*;
import edu.brown.cs.ivy.project.IvyProject;
import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.*;


class DyviewSetupPanel extends DyviewAbstractPanel implements DyviewConstants, DyviseConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private JComboBox<IvyProject> project_selector;
private DyviewStartPanel start_panel;
private JLabel		last_static;
private JLabel		last_dynamic;
private JButton 	dynamic_button;
private JButton 	next_button;
private boolean 	dynamic_ready;

private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DyviewSetupPanel(DyviewWindow dw,DyviewModel mdl)
{
   super(dw,mdl);

   dynamic_ready = false;

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

   addBannerLabel("DYnamic VIEWer");
   addSeparator();

   addSectionLabel("Project Settings");

   Vector<IvyProject> projs = new Vector<IvyProject>(view_model.getProjectManager().getAllProjects());
   Collections.sort(projs,new ProjectSorter());
   project_selector = new JComboBox<IvyProject>(projs);
   project_selector.addActionListener(new ProjectListener());
   project_selector.setSelectedItem(view_model.getProject());
   Box bx = Box.createHorizontalBox();
   bx.add(project_selector);
   bx.add(Box.createHorizontalStrut(20));
   JButton btn = new JButton("Advanced");
   btn.addActionListener(new AdvProjectListener());
   bx.add(btn);
   addRawComponent("Project",bx);

   last_static = new JLabel("No Project");
   bx = Box.createHorizontalBox();
   bx.add(last_static);
   bx.add(Box.createHorizontalStrut(20));
   addRawComponent("Last Analysis",bx);

   addSeparator();

   addSectionLabel("Program Settings");

   start_panel = new DyviewStartPanel(view_model,view_model.getStartClass());
   start_panel.addActionListener(new StartListener());
   addRawComponent("Start Class",start_panel);

   addSeparator();

   addSectionLabel("Visualization Choices");

   Collection<DyviewVisual> vis = view_model.getAllVisuals();
   addChoice("Visualization",vis,view_model.getVisual(),new VisualHandler());

   last_dynamic = new JLabel();
   bx = Box.createHorizontalBox();
   bx.add(last_dynamic);
   bx.add(Box.createHorizontalStrut(20));
   dynamic_button = new JButton("Update Dynamic Analysis Now");
   dynamic_button.addActionListener(new DynamicUpdateListener());
   bx.add(dynamic_button);
   addRawComponent("Last Analysis",bx);

   addSeparator();

   SetupButtonListener sbl = new SetupButtonListener();
   addBottomButton("QUIT","QUIT",sbl);
   next_button = addBottomButton("NEXT","NEXT",sbl);
   next_button.setEnabled(false);
   addBottomButtons();

   if (view_model.getProject() != null) setupProject();
   setupDynamics();
}



/********************************************************************************/
/*										*/
/*	Model callback methods							*/
/*										*/
/********************************************************************************/

void handleProjectChanged()
{
   setupProject();
   setupDynamics();
}


void handleStartClassChanged()
{
   start_panel.setStartClass(view_model.getStartClass());
   setupDynamics();
}


void handleVisualChanged()
{
   setupDynamics();
}




/********************************************************************************/
/*										*/
/*	Project management methods						*/
/*										*/
/********************************************************************************/

private void updateProjectList()
{
   Set<IvyProject> projs = new TreeSet<IvyProject>(new ProjectSorter());
   projs.addAll(view_model.getProjectManager().getAllProjects());

   DefaultComboBoxModel<IvyProject> mdl = (DefaultComboBoxModel<IvyProject>) project_selector.getModel();
   mdl.removeAllElements();
   for (IvyProject ip : projs) mdl.addElement(ip);
   mdl.setSelectedItem(view_model.getProject());
}



private static class ProjectSorter implements Comparator<IvyProject> {

   public int compare(IvyProject p1,IvyProject p2) {
      return p1.toString().compareTo(p2.toString());
    }

}	// end of subclass ProjectSorter



private class ProjectListener implements ActionListener {

   public void actionPerformed(ActionEvent evt) {
      if (project_selector == null) return;
      IvyProject ip = (IvyProject) project_selector.getSelectedItem();
      if (ip != null) ip.update();
      view_model.setProject(ip);
    }

}	// end of innerclass ProjectListener



/********************************************************************************/
/*										*/
/*	Start class updater							*/
/*										*/
/********************************************************************************/

private class StartListener implements ActionListener {

   public void actionPerformed(ActionEvent evt) {
      String s = start_panel.getStartClass();
      view_model.setStartClass(s);
    }

}	// end of inner class StartListener




/********************************************************************************/
/*										*/
/*	Advanced project selection panel					*/
/*										*/
/********************************************************************************/

private void advancedProjectPanel()
{
   File edir = null;
   for (IvyProject ip : view_model.getProjectManager().getAllProjects()) {
      edir = ip.getWorkspace();
      if (edir != null) break;
    }
   File xdir = view_model.getProjectDirectory();

   SwingGridPanel pnl = new SwingGridPanel();

   pnl.beginLayout();

   pnl.addBannerLabel("Advanced Project Options");
   pnl.addSeparator();

   JTextField pdirfld = pnl.addFileField("Project Directory",xdir,
					    JFileChooser.DIRECTORIES_ONLY,
					    null,null);

   JTextField edirfld = pnl.addFileField("Eclipse Workspace",edir,
					    JFileChooser.DIRECTORIES_ONLY,
					    null,null);

   int sts = JOptionPane.showOptionDialog(this,pnl,"Advanced Project Options",
					     JOptionPane.OK_CANCEL_OPTION,
					     JOptionPane.PLAIN_MESSAGE,
					     null,null,null);

   if (sts == JOptionPane.OK_OPTION) {
      String pdir = pdirfld.getText();
      File fdir = new File(pdir);
      if (fdir.exists() && fdir.isDirectory() && xdir != fdir) {
	 view_model.setProjectDirectory(pdir);
       }
      pdir = edirfld.getText();
      fdir = new File(pdir);
      if (fdir.exists() && fdir.isDirectory()) {
	 view_model.getProjectManager().defineEclipseProjects(fdir);
       }
      updateProjectList();
    }
}




private class AdvProjectListener implements ActionListener {

   public void actionPerformed(ActionEvent evt) {
      advancedProjectPanel();
    }

}


/********************************************************************************/
/*										*/
/*	Project selection actions						*/
/*										*/
/********************************************************************************/

private void setupProject()
{
   if (view_model.getProject() == null) {
      last_static.setText("No project");
      dynamic_button.setEnabled(false);
      dynamic_button.setText("No project");
      return;
    }

   start_panel.recompute();
}





/********************************************************************************/
/*										*/
/*	Methods to handle visualization choices 				*/
/*										*/
/********************************************************************************/

private void setupDynamics()
{
   if (dynamic_button == null) return;

   dynamic_button.setEnabled(false);

   if (view_model.getProject() == null) {
      setLastDynamic("No Project");
    }
   else if (view_model.getStartClass() == null) {
      setLastDynamic("No Start Class");
    }
   else if (view_model.getVisual() == null) {
      setLastDynamic("No Visualization");
    }
   else if (view_model.getVisual().getProflets().size() == 0) {
      setLastDynamic("Not Required");
    }
   else {
      Date d0 = getDynamicDate();
      if (d0 == null) {
	 setLastDynamic("Required");
	 dynamic_button.setEnabled(true);
       }
      else {
	 setLastDynamic(d0.toString());
	 dynamic_ready = true;
	 dynamic_button.setEnabled(true);
       }
    }

   next_button.setEnabled(dynamic_ready);
}


private void setLastDynamic(String txt)
{
   StringBuffer buf = new StringBuffer();
   buf.append(txt);
   int ln = (30 - buf.length())*2;
   for (int i = 0; i < ln; ++i) buf.append(" ");

   last_dynamic.setText(buf.toString());
}



private void updateDynamicAnalysis()
{
   DyviseProcessChooser pc = new DyviseProcessChooser(view_model.getDymonHandle(),
							 view_model.getStartClass(),
							 "ANALYZE");
   String pid = pc.requestProcess(for_window);
   if (pid == null) {
      setupDynamics();
      return;
    }

   Object [] opts = new Object [] { "CANCEL" };

   DymonRemote.ProcessManager pm = view_model.getProcessManager();

   StringBuffer buf = new StringBuffer();
   buf.append("<html><p>Waiting for dynamic analysis to complete. (This might take a while)");
   buf.append("<br><table border='2' align='center' frame='box' rules='all'>");
   buf.append("<tr><td>Process</td><td>" + pid + "</td></tr>");
   buf.append("<tr><td>Start Class</td><td>" + pm.getName(pid) + "</td></tr>");
   buf.append("<tr><td>Arguments</td><td>" + pm.getArgs(pid) + "</td></tr>");
   buf.append("</table></html>");

   JOptionPane opt = new JOptionPane(buf.toString(),
					JOptionPane.INFORMATION_MESSAGE,
					JOptionPane.DEFAULT_OPTION,
					null,opts);

   JDialog dlg = opt.createDialog(this,"Dynamic Analysis Waiter");
   dlg.setModal(true);

   DynamicUpdater dup = new DynamicUpdater(pid,dlg);

   dup.start();
   dlg.setVisible(true);

   Object rslt = opt.getValue();
   if (rslt != null && rslt.equals("CANCEL")) {
      dup.abort();
    }
   else {
      boolean sts = dup.getStatus();
      if (sts == false) {
	 JOptionPane.showMessageDialog(this,"Dynamic Analysis not Successful.  Try Again");
       }
    }

   setupDynamics();
}



private Date getDynamicDate()
{
   if (view_model.getVisual() == null || view_model.getProject() == null) return null;

   Date d0 = null;
   for (String s : view_model.getVisual().getProflets()) {
      Date d1 = view_model.getUpdateTime(s,view_model.getStartClass());
      if (d1 == null) {
	 d0 = null;
	 break;
       }
      else if (d0 == null || d0.compareTo(d1) > 0) d0 = d1;
    }

   return d0;
}



private void doDynamicComputations()
{
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("COMPUTE");
   xw.field("TYPE","METHODCLASSES");
   xw.begin("RESULT");
   xw.field("START",true);
   xw.text("DynEventCandidates");
   xw.end("RESULT");
   xw.begin("SOURCE");
   xw.field("START",true);
   xw.text("DynEventRoutines");
   xw.end("SOURCE");
   xw.end("COMPUTE");

   DycompMain dm = new DycompMain(view_model.getProject());
   dm.processXml(xw.toString(),view_model.getStartClass());
   xw.close();
}




private void noteDynamicUpdated()
{
   view_model.updateTime("DYNAMIC",view_model.getStartClass());
}




private class VisualHandler implements ActionListener {

   public void actionPerformed(ActionEvent evt) {
      JComboBox<?> cbx = (JComboBox<?>) evt.getSource();
      view_model.setVisual((DyviewVisual) cbx.getSelectedItem());
    }

}	// end of inner class VisualHandler



private class DynamicUpdateListener implements ActionListener {

   public void actionPerformed(ActionEvent evt) {
      updateDynamicAnalysis();
    }

}	// end of inner class StaticUpdateListener




private class DynamicUpdater extends Thread {

   private JDialog wait_dialog;
   private DymacMain dymac_main;
   private String process_id;
   private boolean result_status;

   DynamicUpdater(String pid,JDialog dlg) {
      super("DynamicAnalysis for " + pid);
      process_id = pid;
      wait_dialog = dlg;
      dymac_main =  new DymacMain(view_model.getProject(),view_model.getVisual().getProflets());
      result_status = false;
    }

   void abort() {
      dymac_main.abort();
      if (wait_dialog != null) {
	 wait_dialog.setVisible(false);
	 wait_dialog.dispose();
	 wait_dialog = null;
       }
    }

   boolean getStatus()				{ return result_status; }

   public void run() {
      while (wait_dialog != null && !wait_dialog.isActive()) {
	 try {
	    Thread.sleep(1);
	  }
	 catch (InterruptedException e) { }
       }

      setLastDynamic("Updating ...");
      dynamic_button.setEnabled(false);
      result_status = dymac_main.analyze(process_id);
      if (result_status) {
	 setLastDynamic("Computing ...");
	 doDynamicComputations();
       }
      if (wait_dialog != null) {
	 wait_dialog.setVisible(false);
	 wait_dialog.dispose();
	 wait_dialog = null;
	 if (result_status) noteDynamicUpdated();
       }
    }

}	// end of inner class DynamicUpdate




/********************************************************************************/
/*										*/
/*	Setup button handlers							*/
/*										*/
/********************************************************************************/

private class SetupButtonListener implements ActionListener {

   public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      if (cmd.equals("QUIT")) {
	 System.exit(0);
       }
      else if (cmd.equals("NEXT")) {
	 for_window.gotoVisualPanel();
       }
    }

}	// end of inner class SetupButtonListener



}	// end of class DyviewSetupPanel




/* end of DyviewSetupPanel.java */
