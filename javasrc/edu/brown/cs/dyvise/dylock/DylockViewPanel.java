/********************************************************************************/
/*										*/
/*		DylockViewPanel.java						*/
/*										*/
/*	DYVISE lock analysis lock viewer panel for lock understanding		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylock/DylockViewPanel.java,v 1.3 2013-05-09 12:29:01 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylockViewPanel.java,v $
 * Revision 1.3  2013-05-09 12:29:01  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.2  2011-04-18 19:24:29  spr
 * Bug fixes in dylock.
 *
 * Revision 1.1  2011-03-10 02:25:00  spr
 * New version of dylock to produce visualizations.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dylock;

import edu.brown.cs.ivy.swing.*;
import edu.brown.cs.ivy.xml.*;
import edu.brown.cs.ivy.file.*;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;

import java.util.*;
import java.util.List;
import java.io.*;


class DylockViewPanel extends SwingGridPanel implements DylockConstants, ActionListener
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private DylockViewer for_viewer;
private LockModel    table_model;
private List<DylockLockData> lock_data;
private JButton 	vis_button;
private JButton 	save_button;
private File		save_directory;

private static final int COL_ID = 0;
private static final int COL_TYPE = 1;
private static final int COL_LOCATION = 2;
private static final int COL_CLASS = 3;
private static final int COL_LOCK_CT = 4;
private static final int COL_ENTER_CT = 5;
private static final int COL_DELAY_TIME = 6;
private static final int COL_WAIT_CT = 7;
private static final int COL_WAIT_TIME = 8;
private static final int COL_MONITOR = 9;

private static final String [] column_names = new String [] {
   "ID", "Types", "Locations", "Class", "# Lock","# Enter", "Delay (ms)", "# Wait", "Wait (ms)", "Monitor"
};

private static final Class<?> [] column_types = new Class<?> [] {
   Integer.class, String.class, String.class, String.class, Integer.class,Integer.class,
   Long.class, Integer.class, Long.class, Boolean.class
};

private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DylockViewPanel(DylockViewer dv,Collection<DylockLockData> locks)
{
   for_viewer = dv;
   Set<DylockLockData> lks = new TreeSet<DylockLockData>(new LockComparator());
   lks.addAll(locks);
   lock_data = new ArrayList<DylockLockData>(lks);

   table_model = new LockModel();

   beginLayout();

   LockTable tbl = new LockTable(table_model);
   addGBComponent(new JScrollPane(tbl),0,0,1,1,10,10);

   addBottomButton("Select All","SelectAll",this);
   addBottomButton("Select None","SelectNone",this);
   addBottomButton("Dependencies","Dependencies",this);
   addBottomButton("Usage Graph","Usage Graph",this);
   save_button = addBottomButton("Save","Save",this);
   vis_button = addBottomButton("Visualize","Visualize",this);
   addBottomButtons(1);
   checkButtons();

   File otf = dv.getOutputFile();
   save_directory = otf.getParentFile();
}




/********************************************************************************/
/*										*/
/*	Comparator for initial ordering of locks				*/
/*										*/
/********************************************************************************/

private static class LockComparator implements Comparator<DylockLockData> {

   @Override public int compare(DylockLockData l1,DylockLockData l2) {
      return l1.getLockNumber() - l2.getLockNumber();
    }

}	// end of inner class LockComparator





/********************************************************************************/
/*										*/
/*	Button handler								*/
/*										*/
/********************************************************************************/

@Override public void actionPerformed(ActionEvent evt)
{
   String cmd = evt.getActionCommand();

   if (cmd.equals("SelectAll")) {
      for (DylockLockData lock : lock_data) lock.setMonitored(true);
      table_model.fireTableDataChanged();
      checkButtons();
    }
   else if (cmd.equals("SelectNone")) {
      for (DylockLockData lock : lock_data) lock.setMonitored(false);
      table_model.fireTableDataChanged();
      checkButtons();
    }
   else if (cmd.equals("Dependencies")) {
      for_viewer.createGraphPanel();
    }
   else if (cmd.equals("Usage Graph")) {
      for_viewer.createUsageGraph();
    }
   else if (cmd.equals("Save")) {
      JFileChooser jfc = new JFileChooser(save_directory);
      int val = jfc.showSaveDialog(this);
      if (val == JFileChooser.APPROVE_OPTION) {
	 outputVisualization(jfc.getSelectedFile());
	 save_directory = jfc.getCurrentDirectory();
       }
    }
   else if (cmd.equals("Visualize")) {
      outputVisualization(for_viewer.getOutputFile());
      // start up dylock on the visualization file
    }
}


private void checkButtons()
{
   int ct = 0;
   for (DylockLockData lock : lock_data) {
      if (lock.isMonitored()) ++ct;
    }

   if (ct == 0) {
      save_button.setEnabled(false);
      vis_button.setEnabled(false);
    }
   else {
      save_button.setEnabled(true);
      vis_button.setEnabled(true);
    }
}



/********************************************************************************/
/*										*/
/*	Routiens to handle visualization request				*/
/*										*/
/********************************************************************************/

private void outputVisualization(File vis)
{
   try {
      IvyXmlWriter xw = new IvyXmlWriter(vis);
      xw.begin("DYLATE");
      for (DylockLockData lock : lock_data) {
	 if (lock.isMonitored()) lock.outputVisualization(xw);
       }
      xw.end("DYLATE");
      xw.close();
    }
   catch (IOException e) {
      System.err.println("DYLOCKVIEW: Problem writing output visualization file: " + e);
      return;
    }
}



/********************************************************************************/
/*										*/
/*	LockModel -- table model for locks					*/
/*										*/
/********************************************************************************/

private class LockModel extends AbstractTableModel {

   private static final long serialVersionUID = 1;

   LockModel() {
    }

   @Override public int getColumnCount() {
      return column_names.length;
    }

   @Override public String getColumnName(int col) {
      return column_names[col];
    }

   @Override public Class<?> getColumnClass(int col) {
      return column_types[col];
    }

   @Override public boolean isCellEditable(int row,int col) {
      if (col == COL_MONITOR) return true;
      return false;
    }


   @Override public int getRowCount() {
       return lock_data.size();
    }

   @Override public Object getValueAt(int r,int c) {
      if (r < 0 || r >= lock_data.size()) return null;
      DylockLockData data = lock_data.get(r);
      switch (c) {
	 case COL_ID :
	    return data.getLockNumber();
	 case COL_TYPE :
	    return data.getTypeString();
	 case COL_LOCATION :
	    return data.getLocationString();
	 case COL_CLASS :
	    return data.getClassString();
	 case COL_LOCK_CT :
	    return data.getNumInstance();
	 case COL_ENTER_CT :
	    return data.getNumLock();
	 case COL_DELAY_TIME :
	    return data.getBlockTime();
	 case COL_WAIT_CT :
	    return data.getNumWait();
	 case COL_WAIT_TIME :
	    return data.getWaitTime();
	 case COL_MONITOR :
	    return data.isMonitored();
	 default :
	    return null;
       }
    }

   @Override public void setValueAt(Object val,int r,int c) {
      if (c != COL_MONITOR) return;
      DylockLockData data = lock_data.get(r);
      data.setMonitored(((Boolean) val).booleanValue());
      checkButtons();
    }


}	// end of inner class LockModel



/********************************************************************************/
/*										*/
/*	LockTable -- table implementation					*/
/*										*/
/********************************************************************************/

private class LockTable extends JTable {

   private static final long serialVersionUID = 1;

   LockTable(LockModel mdl) {
      super(mdl);
      setOpaque(false);
      setToolTipText("");
      setAutoCreateRowSorter(true);
      for (Enumeration<TableColumn> e = getColumnModel().getColumns(); e.hasMoreElements(); ) {
	 TableColumn tc = e.nextElement();
	 tc.setHeaderRenderer(new HeaderRenderer(getTableHeader().getDefaultRenderer()));
       }
      getColumnModel().getColumn(COL_DELAY_TIME).setCellRenderer(new TimeRenderer());
      getColumnModel().getColumn(COL_WAIT_TIME).setCellRenderer(new TimeRenderer());
    }

   DylockLockData getActualLock(int row) {
      RowSorter<?> rs = getRowSorter();
      if (rs != null) row = rs.convertRowIndexToModel(row);
      if (row < 0 || row >= lock_data.size()) return null;
      return lock_data.get(row);
    }

   @Override public String getToolTipText(MouseEvent e) {
      int r = rowAtPoint(e.getPoint());
      if (r < 0) return null;
      DylockLockData lock = getActualLock(r);
      if (lock == null) return null;
      int c = columnAtPoint(e.getPoint());
      if (c < 0) return null;
      switch (c) {
	 default :
	    break;
	 case COL_TYPE :
	    return lock.getTypeToolTip();
	 case COL_LOCATION :
	    return lock.getLocationToolTip();
	 case COL_CLASS :
	    return lock.getClassToolTip();
       }
      return lock.getOverviewToolTip();
    }

   @Override public TableCellRenderer getCellRenderer(int row,int col) {
      TableCellRenderer tcr = super.getCellRenderer(row,col);
      return tcr;
   }

}	// end of inner class LockTable



/********************************************************************************/
/*										*/
/*	Cell renderers								*/
/*										*/
/********************************************************************************/

private class HeaderRenderer implements TableCellRenderer {

   private TableCellRenderer default_renderer;
   private Font bold_font;

   HeaderRenderer(TableCellRenderer dflt) {
      default_renderer = dflt;
      bold_font = null;
    }

   public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,
						     boolean foc,int r,int c) {
      Component cmp = default_renderer.getTableCellRendererComponent(t,v,sel,foc,r,c);
      if (bold_font == null) {
	 bold_font = cmp.getFont();
	 bold_font = bold_font.deriveFont(Font.BOLD);
       }
      cmp.setFont(bold_font);
      return cmp;
    }

}	// end of subclass HeaderRenderer



private class TimeRenderer extends DefaultTableCellRenderer {

   private static final long serialVersionUID = 1;


   TimeRenderer() { }

   public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,
						     boolean foc,int r,int c) {
      if (v instanceof Number) {
	 Number vn = (Number) v;
	 double time = vn.doubleValue();
	 double tv = time / 1000000.0;	// nano to milli
	 String rslt = IvyFormat.formatTime(tv);
	 JLabel lbl = (JLabel) super.getTableCellRendererComponent(t,rslt,sel,foc,r,c);
	 lbl.setHorizontalAlignment(SwingConstants.RIGHT);
	 return lbl;
       }

      return super.getTableCellRendererComponent(t,v,sel,foc,r,c);
    }

}	// end of subclass TimeRenderer




}	// end of class DylockViewPanel




/* end of DylockViewPanel.java */
