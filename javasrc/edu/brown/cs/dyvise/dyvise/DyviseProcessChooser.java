/********************************************************************************/
/*										*/
/*		DyviseProcessChooser.java					*/
/*										*/
/*	Common code for choosing a user process 				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyvise/DyviseProcessChooser.java,v 1.1 2009-10-07 01:41:19 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyviseProcessChooser.java,v $
 * Revision 1.1  2009-10-07 01:41:19  spr
 * Add common implementation of process chooser (moved from dyview).
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dyvise;


import edu.brown.cs.dyvise.dymon.DymonRemote;

import edu.brown.cs.ivy.swing.SwingGridPanel;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;



public class DyviseProcessChooser implements DyviseConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String	start_class;
private String	action_name;
private ProcTable proc_table;
private ProcModel table_model;
private String	process_id;
private DymonRemote dymon_remote;
private DymonRemote.ProcessManager process_manager;
private List<String> process_set;
private JButton ok_button;
private boolean value_ok;

private static int	UPDATE_TIME = 5000;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public DyviseProcessChooser(DymonRemote dr,String start,String action)
{
   dymon_remote = dr;
   start_class = start;
   action_name = action;
   process_id = null;
   process_set = new ArrayList<String>();
   process_manager = null;
   value_ok = false;

   proc_table = null;
   table_model = new ProcModel();
}




/********************************************************************************/
/*										*/
/*	Dialog methods								*/
/*										*/
/********************************************************************************/

public String requestProcess(JFrame owner)
{
   process_id = null;

   JDialog dlg = new JDialog(owner,"Process Selection",true);
   SwingGridPanel pnl = new SwingGridPanel();
   pnl.setInsets(4);

   pnl.beginLayout();
   if (start_class != null) pnl.addBannerLabel("Select " + start_class + " Process");
   else pnl.addBannerLabel("Select Process");
   pnl.addSeparator();

   proc_table = new ProcTable();
   updateProcesses();
   pnl.addLabellessRawComponent("PTABLE",new JScrollPane(proc_table));
   pnl.addSeparator();

   ButtonHandler bh = new ButtonHandler(dlg);

   pnl.addBottomButton("Update","UPDATE",bh);
   pnl.addBottomButton("Cancel","CANCEL",bh);
   ok_button = pnl.addBottomButton(action_name,"OK",false,bh);
   pnl.addBottomButtons();

   dlg.setContentPane(pnl);
   dlg.pack();

   Timer t = new Timer(UPDATE_TIME,new UpdateHandler());
   t.setRepeats(true);
   t.start();

   dlg.setVisible(true);

   t.stop();

   if (!value_ok) process_id = null;

   return process_id;
}



/********************************************************************************/
/*										*/
/*	Methods for maintaining process set					*/
/*										*/
/********************************************************************************/

private void updateProcesses()
{
   if (process_manager == null) {
      process_manager = dymon_remote.getProcessManager();
    }

   synchronized (process_set) {
      process_set = process_manager.findProcess(start_class);
      Collections.sort(process_set);
    }

   table_model.update();

   if (process_id != null) {
      int idx = process_set.indexOf(process_id);
      int vidx = proc_table.convertRowIndexToView(idx);
      if (vidx >= 0) proc_table.addRowSelectionInterval(vidx,vidx);
    }
}



private class UpdateHandler implements ActionListener {

   public void actionPerformed(ActionEvent evt) {
      updateProcesses();
    }

}	// end of inner class UpdateHandler




/********************************************************************************/
/*										*/
/*	Button handler								*/
/*										*/
/********************************************************************************/

private class ButtonHandler implements ActionListener {

   private JDialog for_dialog;

   ButtonHandler(JDialog dlg) {
      for_dialog = dlg;
    }

   public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      if (cmd.equals("UPDATE")) {
	 if (process_manager != null) process_manager.forceUpdate();
	 updateProcesses();
       }
      else if (cmd.equals("CANCEL")) {
	 process_id = null;
	 for_dialog.setVisible(false);
       }
      else if (cmd.equals("OK")) {
	 value_ok = true;
	 for_dialog.setVisible(false);
       }
      else {
	 System.err.println("UNKNOWN BUTTON: " + cmd);
       }
    }

}	// end of class ButtonHandler




/********************************************************************************/
/*										*/
/*	Table class								*/
/*										*/
/********************************************************************************/

private class ProcTable extends JTable {

   private static final long serialVersionUID = 1;

   ProcTable() {
      super(table_model);
      setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      setAutoCreateRowSorter(true);
      // setColumnSelectionAllowed(false);
      Dimension d = getPreferredSize();
      if (start_class == null) d.height = getRowHeight() * 10;
      else d.height = getRowHeight() * 4;
      setPreferredScrollableViewportSize(d);
      ListSelectionModel smdl = getSelectionModel();
      smdl.addListSelectionListener(new Selector(this));
    }

}	// end of inner class ProcTable




private class Selector implements ListSelectionListener {

   private ProcTable for_table;

   Selector(ProcTable tbl) {
      for_table = tbl;
    }

   public void valueChanged(ListSelectionEvent e) {
      int idx = for_table.getSelectedRow();
      if (idx < 0) ok_button.setEnabled(false);
      else {
	 ok_button.setEnabled(true);
	 int nidx = for_table.convertRowIndexToModel(idx);
	 process_id = process_set.get(nidx);
       }
    }

}	// end of inner class Selector


/********************************************************************************/
/*										*/
/*	Table Model class							*/
/*										*/
/********************************************************************************/

private class ProcModel extends AbstractTableModel {

   private static final long serialVersionUID = 1;

   private boolean use_start;

   ProcModel() {
      use_start = (start_class == null);
    }

   public int getColumnCount()				{ return (use_start ? 3 : 2); }
   public int getRowCount()				{ return process_set.size(); }

   public Object getValueAt(int row,int col) {
      if (row < 0 || row >= process_set.size()) return null;
      String pid = process_set.get(row);
      switch (col) {
	 case 0 :
	    return pid;
	 case 1 :
	    if (use_start) return process_manager.getName(pid);
	    else return process_manager.getArgs(pid);
	 case 2 :
	    return process_manager.getArgs(pid);
       }
      return null;
    }

   public String getColumnName(int col) {
      String nm = "???";
      switch (col) {
	 case 0 :
	    nm = "ID";
	    break;
	 case 1 :
	    nm = (use_start ? "Start Class" : "Arguments");
	    break;
	 case 2 :
	    nm = "Arguments";
	    break;
       }
      return nm;
    }

   void update() {
      fireTableDataChanged();
    }

}	// end of inner class ProcModel




}	// end of class DyviewProcessChooser




/* end of DyviewProcessChooser.java */

