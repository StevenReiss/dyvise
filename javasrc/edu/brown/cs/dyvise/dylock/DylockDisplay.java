/********************************************************************************/
/*										*/
/*		DylockDisplay.java						*/
/*										*/
/*	Display frame and panel for dylock visualizations			*/
/*										*/
/*	Written by spr								*/
/*										*/
/********************************************************************************/



package edu.brown.cs.dyvise.dylock;

import edu.brown.cs.dyvise.dystore.*;
import edu.brown.cs.dyvise.dygraph.*;

import edu.brown.cs.ivy.swing.*;

import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.util.List;


class DylockDisplay extends JFrame implements DylockConstants
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private MenuBar 	menu_bar;
private JFrame		control_panel;
private SwingRangeScrollBar	time_bar;
private SwingRangeScrollBar	range_bar;
private DygraphControl	graph_control;
private DylockPatternAccess pattern_access;
private Map<String,DygraphView> view_set;
private DygraphView	display_view;

private static final int MAX_TIME = 1000;
private static final int MAX_RANGE = 100;

private static Border bar_border = BorderFactory.createLineBorder(SwingColors.SWING_BORDER_COLOR,1);


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DylockDisplay(DygraphControl graph,DylockPatternAccess pats)
{
   graph_control = graph;
   pattern_access = pats;
   view_set = new TreeMap<String,DygraphView>();

   setupPanels();
}



/********************************************************************************/
/*										*/
/*	Panel Setup methods							*/
/*										*/
/********************************************************************************/

private void setupPanels()
{
   display_view = null;
   for (DygraphView dv : graph_control.getViews()) {
      view_set.put(dv.getName(),dv);
      if (display_view == null) display_view = dv;
    }

   menu_bar = new MenuBar();
   setJMenuBar(menu_bar);
   setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

   SwingGridPanel pnl = new SwingGridPanel();
   PatternArea pa = null;
   if (pattern_access != null) {
      List<DylockPattern> pats = pattern_access.getPatterns();
      if (pats != null && pats.size() > 0) {
	 pa = new PatternArea(pats);
	 pnl.addGBComponent(new JScrollPane(pa),0,0,1,2,1,10);
       }
    }

   for (DygraphView dv : view_set.values()) {
      dv.getDisplayPanel().setVisible(dv == display_view);
      pnl.addGBComponent(dv.getDisplayPanel(),1,0,1,1,10,10);
    }

   menu_bar.update();

   time_bar = new SwingRangeScrollBar(SwingRangeScrollBar.HORIZONTAL,0,MAX_TIME,0,MAX_TIME);
   time_bar.addAdjustmentListener(new TimeChanger());
   time_bar.setBorder(bar_border);
   pnl.addGBComponent(new SwingBorderPanel(bar_border,time_bar),1,1,1,1,100,0);

   range_bar = new SwingRangeScrollBar(SwingRangeScrollBar.VERTICAL,0,MAX_RANGE,0,MAX_RANGE);
   range_bar.addAdjustmentListener(new RangeChanger());
   range_bar.setBorder(bar_border);
   pnl.addGBComponent(new SwingBorderPanel(bar_border,range_bar),2,0,1,1,0,100);

   FilterArea fa = new FilterArea();
   pnl.addGBComponent(fa,0,2,0,1,10,0);

   setContentPane(pnl);
   pack();
}



/********************************************************************************/
/*										*/
/*	Pattern Area display							*/
/*										*/
/********************************************************************************/

private class PatternArea extends JList<DylockPattern> implements ListSelectionListener {

   PatternArea(Collection<DylockPattern> cpats) {
      setDragEnabled(false);
      setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      addListSelectionListener(this);
      Vector<DylockPattern> pats = new Vector<DylockPattern>(cpats);
      Collections.sort(pats,new PatternSorter());
      setListData(pats);
    }

   @Override public void valueChanged(ListSelectionEvent e) {
      DylockPattern dp = getSelectedValue();
      if (pattern_access != null) pattern_access.setCurrentPattern(dp);
    }

}	// end of inner class PatternArea



private class PatternSorter implements Comparator<DylockPattern> {

   @Override public int compare(DylockPattern p1,DylockPattern p2) {
      return p1.compareOrder(p2);
    }

}	// end of inner class PatternSorter




/********************************************************************************/
/*										*/
/*	Scroll action methods							*/
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
   
      // boolean dyn = (v1 == MAX_TIME);
      // System.err.println("SET TIME " + t0 + " " + t1 + " " + dyn);
      graph_control.setTimeWindow(t0,t1,false);
    }

}	// end of innerclass TimeChanger



private class RangeChanger implements AdjustmentListener {

   public void adjustmentValueChanged(AdjustmentEvent e) {
      double v0 = ((double)(range_bar.getLeftValue())) / MAX_RANGE;
      double v1 = ((double)(range_bar.getRightValue())) / MAX_RANGE;

      display_view.setYDataRegion(1.0-v1,1.0-v0);
    }

}	// end of innerclass RangeChanger



/********************************************************************************/
/*										*/
/*	Filter Area								*/
/*										*/
/********************************************************************************/

private class FilterArea extends SwingGridPanel {
   
   private static final long serialVersionUID = 1;
   
   FilterArea() {
    }

}	// end of inner class FilterArea




/********************************************************************************/
/*										*/
/*	Menu Bar								*/
/*										*/
/********************************************************************************/

private class MenuBar extends JMenuBar {

   private Map<DygraphView,DisplayAction> action_map;
   
   private static final long serialVersionUID = 1;
   
   MenuBar() {
      JMenu m1 = new JMenu("File");
      add(m1);
      m1.add("Quit");
      JMenu m2 = new JMenu("View");
      add(m2);
      m2.add(new ControlAction());
      action_map = new HashMap<DygraphView,DisplayAction>();
      for (Map.Entry<String,DygraphView> ent : view_set.entrySet()) {
	 DisplayAction da = new DisplayAction(ent.getKey(),ent.getValue());
	 action_map.put(ent.getValue(),da);
	 m2.add(da);
       }
    }

   void update() {
      for (Map.Entry<DygraphView,DisplayAction> ent : action_map.entrySet()) {
	 if (ent.getKey() == display_view) ent.getValue().setEnabled(false);
	 else ent.getValue().setEnabled(true);
       }
    }

}	// end of inner class MenuBar



private class ControlAction extends AbstractAction {

   private static final long serialVersionUID = 1;
   
   ControlAction() {
      super("Show Display Controller");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      if (control_panel == null) {
	 control_panel = new JFrame();
       }
      JPanel pnl = display_view.getControlPanel();
      if (pnl == null) return;
      control_panel.setContentPane(pnl);
      control_panel.pack();
      control_panel.setVisible(true);
    }

}	// end of inner class ControlAction




private class DisplayAction extends AbstractAction {

   private DygraphView show_view;
   
   private static final long serialVersionUID = 1;
   
   DisplayAction(String id,DygraphView view) {
      super("Show " + id);
      show_view = view;
    }

   @Override public void actionPerformed(ActionEvent evt) {
      if (display_view == show_view) return;
      display_view.getDisplayPanel().setVisible(false);
      display_view = show_view;
      display_view.getDisplayPanel().setVisible(true);
      menu_bar.update();
    }

}	// end of inner class DiaplayAction


}	// end of class DylockDisplay




/* end of DylockDisplay.java */
