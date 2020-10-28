/********************************************************************************/
/*										*/
/*		DyviewClassPanel.java						*/
/*										*/
/*	DYname VIEW panel for choosing classes					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyview/DyviewClassPanel.java,v 1.7 2013-08-05 12:03:41 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyviewClassPanel.java,v $
 * Revision 1.7  2013-08-05 12:03:41  spr
 * Updates; use dypatchasm.
 *
 * Revision 1.6  2013-06-03 13:02:58  spr
 * Minor bug fixes
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
 * Revision 1.2  2009-09-27 23:57:44  spr
 * Updates for mac os/x.
 *
 * Revision 1.1  2009-09-19 00:14:23  spr
 * Viewer with dialog based front end.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dyview;


import edu.brown.cs.dyvise.dyvise.DyviseConstants;
import edu.brown.cs.dyvise.dyvise.DyviseDatabase;
import edu.brown.cs.ivy.swing.*;

import javax.swing.*;
import javax.swing.event.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;



public class DyviewClassPanel extends SwingGridPanel implements ActionListener,
	ListSelectionListener, DyviewConstants, DyviseConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private DyviewModel	view_model;

private String			panel_name;
private SwingListSet<String>	super_classes;
private SwingListSet<String>	chosen_classes;
private SwingListSet<String>	candidate_classes;
private SwingListSet<ClassPat>	class_filters;
private List<String>		default_classes;
private List<ActionListener>	callback_rtns;
private List<String>		user_supers;
private Set<String>		super_candidates;

private boolean 	user_class;

private boolean 	allow_multiple;

private JList<String>	candidate_list;
private JList<String>	chosen_list;
private JTextField	chosen_box;
private JButton 	add_button;
private JButton 	remove_button;
private JButton 	filter_button;
private JButton 	new_button;


private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public DyviewClassPanel(DyviewModel mdl,String name,boolean mult)
{
   view_model = mdl;
   allow_multiple = mult;
   panel_name = name;

   user_class = true;
   super_classes = new SwingListSet<String>(true);
   chosen_classes = new SwingListSet<String>(true);
   candidate_classes = new SwingListSet<String>(true);
   default_classes = new ArrayList<String>();
   class_filters = new SwingListSet<ClassPat>(false);
   callback_rtns = new ArrayList<ActionListener>();
   user_supers = new ArrayList<String>();
   super_candidates = new TreeSet<String>();

   super_classes.addListDataListener(new FilterUpdated());
   class_filters.addListDataListener(new FilterUpdated());
   chosen_classes.addListDataListener(new ResultUpdated());

   computeCandidates();
   computeSuperCandidates();

   setupPanel();
}




/********************************************************************************/
/*										*/
/*	Callback setup methods							*/
/*										*/
/********************************************************************************/

public void addActionListener(ActionListener al)
{
   callback_rtns.add(al);
}



private void invokeCallbacks()
{
   ActionEvent evt = new ActionEvent(this,ActionEvent.ACTION_PERFORMED,panel_name);

   for (ActionListener al : callback_rtns) {
      al.actionPerformed(evt);
    }
}



private class ResultUpdated implements ListDataListener, ActionListener {


   public void actionPerformed(ActionEvent e) {
      JTextField tf = (JTextField) e.getSource();
      String txt = tf.getText();
      chosen_classes.removeAll();
      chosen_classes.addElement(txt);
    }

   public void contentsChanged(ListDataEvent e) {
      invokeCallbacks();
    }

   public void intervalAdded(ListDataEvent e) {
      invokeCallbacks();
    }

   public void intervalRemoved(ListDataEvent e) {
      invokeCallbacks();
    }

}	// end of inner class FilterUpdated





/********************************************************************************/
/*										*/
/*	Access and setup methods						*/
/*										*/
/********************************************************************************/

public void requireUserClass(boolean fg)
{
   user_class = fg;
}



public void addSuperClass(String cls)
{
   user_supers.add(cls);
   super_classes.addElement(cls);
   computeSuperCandidates();
}



public void removeSuperClass(String cls)
{
   user_supers.remove(cls);
   super_classes.removeElement(cls);
   computeSuperCandidates();
}



public void addChosenClass(String cls)
{
   if (allow_multiple) chosen_classes.addElement(cls);
   else {
      chosen_classes.removeAll();
      chosen_classes.addElement(cls);
      if (chosen_box != null) chosen_box.setText(cls);
    }
}


public void addDefaultClass(String cls)
{
   default_classes.add(cls);
   computeCandidates();
   computeSuperCandidates();
}


public void addDefaultClass(Collection<String> clss)
{
   default_classes.addAll(clss);
   computeCandidates();
   computeSuperCandidates();
}



public Collection<String> getClasses()
{
   ArrayList<String> rslt = new ArrayList<String>();

   for (String s : chosen_classes) rslt.add(s);

   return rslt;
}



public boolean isEmpty()
{
   return chosen_classes.getSize() == 0;
}



/********************************************************************************/
/*										*/
/*	Panel methods								*/
/*										*/
/********************************************************************************/

private void setupPanel()
{
   setInsets(3);

   candidate_list = new JList<String>(candidate_classes);
   candidate_list.addListSelectionListener(this);
   candidate_list.setVisibleRowCount(4);

   addGBComponent(new JScrollPane(candidate_list),0,0,1,1,10,10);

   add_button = new JButton(allow_multiple ? "Add" : "Set");
   add_button.setEnabled(false);
   add_button.addActionListener(this);
   if (allow_multiple) {
      remove_button = new JButton("Remove");
      remove_button.addActionListener(this);
      remove_button.setEnabled(false);
      new_button = new JButton("New");
      new_button.addActionListener(this);
    }
   else {
      remove_button = null;
      new_button = null;
      candidate_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }
   filter_button = new JButton("Filter");
   filter_button.addActionListener(this);

   Box b1 = Box.createVerticalBox();
   b1.add(Box.createVerticalGlue());
   b1.add(add_button);
   if (remove_button != null) {
      b1.add(Box.createVerticalGlue());
      b1.add(remove_button);
    }
   b1.add(Box.createVerticalGlue());
   b1.add(filter_button);
   if (new_button != null) {
      b1.add(Box.createVerticalGlue());
      b1.add(new_button);
    }
   b1.add(Box.createVerticalGlue());
   addGBComponent(b1,1,0,1,1,0,10);

   chosen_list = null;
   chosen_box = null;
   if (allow_multiple) {
      chosen_list = new JList<String>(chosen_classes);
      chosen_list.addListSelectionListener(this);
      addGBComponent(new JScrollPane(chosen_list),2,0,1,1,10,10);
    }
   else {
      chosen_box = new JTextField(24);
      chosen_box.addActionListener(new ResultUpdated());
      SwingGridPanel p2 = new SwingGridPanel();
      p2.addGBComponent(new JLabel(),0,0,1,1,10,10);
      p2.addGBComponent(chosen_box,0,1,1,1,10,0);
      p2.addGBComponent(new JLabel(),0,2,1,1,10,10);
      addGBComponent(p2,2,0,1,1,10,10);
    }
}



private SwingGridPanel setupFilterPanel()
{
   SwingGridPanel pnl = new SwingGridPanel();
   pnl.beginLayout();
   pnl.addBannerLabel(panel_name + " Filters");
   pnl.addSeparator();
   pnl.addSectionLabel("Filter by Superclass or Interface");
   pnl.addLabellessRawComponent("Super",new SuperClassPanel());
   pnl.addSeparator();
   pnl.addSectionLabel("Filter by Pattern");
   pnl.addLabellessRawComponent("Pattern",new PatternPanel());
   pnl.addSeparator();
   pnl.addBoolean("Require Project Class",user_class,new ProjClassUpdater());

   return pnl;
}





/********************************************************************************/
/*										*/
/*	Handle button presses							*/
/*										*/
/********************************************************************************/

@Override public void actionPerformed(ActionEvent evt)
{
   String cmd = evt.getActionCommand();
   if (cmd.equals("Add")) {
      List<String> sels = candidate_list.getSelectedValuesList();
      for (String os : sels) {
	 addChosenClass(os);
       }
    }
   else if (cmd.equals("Set")) {
      String os = candidate_list.getSelectedValue();
      addChosenClass(os);
    }
   else if (cmd.equals("Remove")) {
      List<String> sels = chosen_list.getSelectedValuesList();
      for (String os : sels) {
	 chosen_classes.removeElement(os);
       }
    }
   else if (cmd.equals("Filter")) {
      createFilterDialog();
    }
   else if (cmd.equals("New")) {
      String rslt = JOptionPane.showInputDialog(this,"Enter class to add");
      if (rslt != null) {
	 rslt = rslt.trim();
	 if (rslt.length() > 0) addChosenClass(rslt);
       }
    }
}



@Override public void valueChanged(ListSelectionEvent evt)
{
   if (evt.getSource() == candidate_list) {
      List<String> sels = candidate_list.getSelectedValuesList();
      boolean upd = false;
      for (String os : sels) {
	 if (os != null && !chosen_classes.contains(os)) upd = true;
       }
      add_button.setEnabled(upd);
    }
   else if (chosen_list != null && evt.getSource() == chosen_list) {
      if (remove_button != null) {
	 if (chosen_list.isSelectionEmpty()) remove_button.setEnabled(false);
	 else remove_button.setEnabled(true);
       }
    }
}



private void createFilterDialog()
{
   SwingGridPanel pnl = setupFilterPanel();

   JOptionPane.showOptionDialog(this,pnl,"Filter Selection for " + panel_name,
					     JOptionPane.DEFAULT_OPTION,
					     JOptionPane.PLAIN_MESSAGE,
					     null,null,null);
}




/********************************************************************************/
/*										*/
/*	Methods to handle super class changing					*/
/*										*/
/********************************************************************************/

private void computeCandidates()
{
   int sz = super_classes.getSize();

   String q = "SELECT C.name FROM SrcClass C";
   if (sz != 0) q += ", CompClassHierarchy H";

   String pfx = " WHERE ";

   if (sz != 0) {
      q += pfx + "C.name = H.subtype AND ";
      pfx = " AND ";
      if (sz == 1) {
	 q += "H.supertype = " + DyviseDatabase.sqlString(super_classes.getElementAt(0));
       }
      else if (sz > 1) {
	 q += "H.supertype IN ( ";
	 for (int i = 0; i < sz; ++i) {
	    if (i > 0) q += " , ";
	    q += DyviseDatabase.sqlString(super_classes.getElementAt(i));
	  }
	 q += " )";
       }
    }

   if (default_classes.size() > 0) {
      q += pfx + "C.name IN (";
      pfx = " AND ";
      for (int i = 0; i < default_classes.size(); ++i) {
	 if (i > 0) q += " , ";
	 q += DyviseDatabase.sqlString(default_classes.get(i));
       }
      q += " )";
    }

   if (user_class) {
      q += pfx + "C.project";
      pfx = " AND ";
    }

   ResultSet rs = view_model.queryDatabase(q);

   if (rs == null) {
      candidate_classes.removeAll();
      return;
    }

   Set<String> upd = new HashSet<String>();
   try {
      while (rs.next()) {
	 String cl = rs.getString(1);
	 if (checkFilters(cl)) upd.add(cl);
       }
      rs.close();
    }
   catch (SQLException e) { }

   boolean havechng = false;
   for (Iterator<String> it = candidate_classes.iterator(); it.hasNext(); ) {
      String cl = it.next();
      if (upd.contains(cl)) upd.remove(cl);
      else {
	 it.remove();
	 havechng = true;
       }
    }
   if (upd.size() > 0) {
      for (String s : upd) candidate_classes.addElement(s);
    }
   if (havechng) candidate_classes.update();
}




private boolean checkFilters(String cls)
{
   boolean haveincl = false;

   for (ClassPat cp : class_filters) {
      if (!cp.isExclude()) haveincl = true;
      if (cp.match(cls)) {
	 if (cp.isExclude()) return false;
	 return true;
       }
    }

   if (!haveincl) return true;

   return false;
}



private class FilterUpdated implements ListDataListener {


   public void contentsChanged(ListDataEvent e) {
      computeCandidates();
    }

   public void intervalAdded(ListDataEvent e) {
      computeCandidates();
    }

   public void intervalRemoved(ListDataEvent e) {
      computeCandidates();
    }

}	// end of inner class FilterUpdated




/********************************************************************************/
/*										*/
/*	Methods to compute set of potential superclass filters			*/
/*										*/
/********************************************************************************/

private void computeSuperCandidates()
{
   int sz = user_supers.size();

   String q = "SELECT J.supertype FROM SrcClass C, CompClassHierarchy J";
   if (sz != 0) q += ", CompClassHierarchy H";
   q += " WHERE J.subtype = C.name AND J.subtype != J.supertype";

   if (sz != 0) {
      q += " AND C.name = H.subtype AND ";
      q += "H.supertype IN ( ";
      int i = 0;
      for (String s : user_supers) {
	 if (i++ > 0) q += " , ";
	 q += DyviseDatabase.sqlString(s);
       }
      q += " )";
    }

   if (default_classes.size() > 0) {
      q += " AND C.name IN (";
      for (int i = 0; i < default_classes.size(); ++i) {
	 if (i > 0) q += " , ";
	 q += DyviseDatabase.sqlString(default_classes.get(i));
       }
      q += " )";
    }

   if (user_class) {
      q += " AND C.project";
    }

   super_candidates.clear();

   ResultSet rs = view_model.queryDatabase(q);

   try {
      while (rs.next()) {
	 String cl = rs.getString(1);
	 super_candidates.add(cl);
       }
      rs.close();
    }
   catch (SQLException e) { }
}




/********************************************************************************/
/*										*/
/*	Pattern representation							*/
/*										*/
/********************************************************************************/

private static class ClassPat {

   private boolean is_exclude;
   private Pattern reg_exp;
   private String show_string;

   ClassPat(String pat,boolean excl) {
      is_exclude = excl;
      if (pat != null) reg_exp = Pattern.compile(pat);
      else reg_exp = null;
      show_string = pat;
    }

   public String toString() {
      StringBuffer buf = new StringBuffer();
      if (is_exclude) buf.append("- ");
      else buf.append("+ ");
      buf.append(show_string);
      return buf.toString();
    }

   boolean match(String s) {
      return reg_exp.matcher(s).matches();
    }

   String getPattern()				{ return show_string; }
   boolean isExclude()				{ return is_exclude; }

}	// end of inner class ClassPat



/********************************************************************************/
/*										*/
/*	Supertype panel 							*/
/*										*/
/********************************************************************************/

private class SuperClassPanel extends SwingListPanel<String> {

   private static final long serialVersionUID = 1;


   SuperClassPanel() {
      super(super_classes);
    }

   protected String createNewItem() {
      return "";
    }

   protected String editItem(Object itm) {
      if (super_candidates.size() > 0 && itm.equals("")) {
	 SwingComboBox<String> cbx = new SwingComboBox<String>(super_candidates,true);
	 int sts = JOptionPane.showOptionDialog(this,cbx,"Super Class Filter Input",
						   JOptionPane.OK_CANCEL_OPTION,
						   JOptionPane.PLAIN_MESSAGE,
						   null,null,null);
	 if (sts != JOptionPane.OK_OPTION) return null;
	 return (String) cbx.getSelectedItem();
       }

      return JOptionPane.showInputDialog(this,"Edit Class",itm);
    }

   protected String deleteItem(Object itm) {
      return (String) itm;
    }

}	// end of inner class SuperClassPanel



/********************************************************************************/
/*										*/
/*	Pattern filter panel							*/
/*										*/
/********************************************************************************/


private class PatternPanel extends SwingListPanel<ClassPat> {

   private static final long serialVersionUID = 1;


   PatternPanel() {
      super(class_filters);
    }

   protected ClassPat createNewItem() {
      return new ClassPat(null,false);
    }

   protected ClassPat editItem(Object itm) {
      return editPattern((ClassPat) itm);
    }

   protected ClassPat deleteItem(Object itm) {
      return (ClassPat) itm;
    }

   private ClassPat editPattern(ClassPat cp) {
      SwingGridPanel pnl = new SwingGridPanel();
      pnl.beginLayout();
      JTextField txt = pnl.addTextField("Pattern",cp.getPattern(),null,null);
      JCheckBox excl = pnl.addBoolean("Exclude",cp.isExclude(),null);
      int sts = JOptionPane.showOptionDialog(this,pnl,"Edit Pattern for " + panel_name,
						JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.PLAIN_MESSAGE,
						null,null,null);
      if (sts != JOptionPane.OK_OPTION) return null;
      return new ClassPat(txt.getText(),excl.isSelected());
    }

}	// end of inner class PatternPanel



/********************************************************************************/
/*										*/
/*	Project class button handler						*/
/*										*/
/********************************************************************************/

private class ProjClassUpdater implements ActionListener {

   public void actionPerformed(ActionEvent e) {
      JCheckBox cbx = (JCheckBox) e.getSource();
      boolean fg = cbx.isSelected();
      if (fg != user_class) {
	 user_class = fg;
	 computeCandidates();
       }
    }

}	// end of inner class ProjClassUpdater




}	// end of class DyviewClassPanel




/* end of DyviewClassPanel.java */
