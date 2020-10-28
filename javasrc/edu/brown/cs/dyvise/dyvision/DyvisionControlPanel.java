/********************************************************************************/
/*										*/
/*		DyvisionControlPanel.java					*/
/*										*/
/*	Control panel for a dyvision display					*/
/*										*/
/********************************************************************************/
/*	Copyright 2007 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2007, Brown University, Providence, RI.				 *
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyvision/DyvisionControlPanel.java,v 1.3 2013-06-03 13:02:59 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyvisionControlPanel.java,v $
 * Revision 1.3  2013-06-03 13:02:59  spr
 * Minor bug fixes
 *
 * Revision 1.2  2011-03-10 02:33:28  spr
 * Code cleanup.
 *
 * Revision 1.1  2010-03-30 21:27:57  spr
 * Move control panel to its own file.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dyvision;


import edu.brown.cs.ivy.swing.SwingGridPanel;
import javax.swing.*;

import java.awt.event.*;
import java.util.*;


public class DyvisionControlPanel extends JDialog implements DyvisionConstants,
			ActionListener
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private DyvisionMain	for_main;
private DyvisionView	active_view;

private JLabel start_label;
private JComboBox<String> overhead_choice;
private JCheckBox enabled_btn;
private boolean doing_setup;
private Set<String> active_agents;


private static Map<Double,String> overhead_values;

static {
   overhead_values = new TreeMap<Double,String>();
   overhead_values.put(0.001,"Very Low Impact (0.1%)");
   overhead_values.put(0.005,"Very Low Impact (0.5%)");
   overhead_values.put(0.01,"Low Impact (1%)");
   overhead_values.put(0.05,"Medium Impact (5%)");
   overhead_values.put(0.10,"Medium Impact (10%)");
   overhead_values.put(0.25,"High Impact (25%)");
   overhead_values.put(0.50,"Very High Impact (50%)");
   overhead_values.put(1.00,"Very High Impact (100%)");
}



private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DyvisionControlPanel(DyvisionMain dm,DyvisionView dv)
{
   super(dv,dv.getId() + " Control Panel",false);

   for_main = dm;
   active_view = dv;

   active_agents = null;

   SwingGridPanel pnl = setupPanel();
   setContentPane(pnl);
   pack();
}




/********************************************************************************/
/*										*/
/*	Panel layout methods							*/
/*										*/
/********************************************************************************/

private SwingGridPanel setupPanel()
{
   SwingGridPanel pnl = new SwingGridPanel();

   doing_setup = true;

   pnl.beginLayout();
   pnl.addBannerLabel("Control for " + active_view.getId());
   pnl.addSeparator();
   start_label = new JLabel(for_main.getStartClass());
   pnl.addRawComponent("Start class",start_label);
   pnl.addSeparator();
   enabled_btn = pnl.addBoolean("Monitor Enabled",for_main.isEnabled(),this);

   String [] choices = new String [overhead_values.size()];
   choices = overhead_values.values().toArray(choices);
   String cur = overhead_values.get(for_main.getOverhead());
   overhead_choice = pnl.addChoice("Overhead",choices,cur,this);


   Set<String> allagts = getAgents(null);
   active_agents = getAgents(active_view.getId());

   if (allagts != null && active_agents != null) {
      pnl.addSeparator();
      pnl.addSectionLabel("Proflet Agents");
      SwingGridPanel apnl = new SwingGridPanel();
      JLabel l0 = new JLabel("   ");
      apnl.addGBComponent(l0,0,0,1,1,0,0);
      AgentCallback acb = new AgentCallback();

      int i = 0;
      int col = 1;
      for (String s : allagts) {
	 JLabel tag = new JLabel(s,SwingConstants.RIGHT);
	 apnl.addGBComponent(tag,col,i/2,1,1,10,0);
	 JCheckBox cbx = new JCheckBox();
	 cbx.setSelected(active_agents.contains(s));
	 cbx.setActionCommand(s);
	 cbx.addActionListener(acb);
	 apnl.addGBComponent(cbx,col+1,i/2,1,1,0,0);
	 ++i;
	 if (i%2 == 0) col = 1;
	 else col = 4;
       }
      apnl.addGBComponent(new JSeparator(JSeparator.VERTICAL),3,0,1,0,0,0);
      pnl.addLabellessRawComponent(null,apnl);
    }

   pnl.addSeparator();

   pnl.addBottomButton("Time View","TIMEVIEW",this);
   pnl.addBottomButton("Memory View","MEMVIEW",this);
   pnl.addBottomButtons();

   pnl.addSeparator();

   pnl.addBottomButton("Clear","CLEAR",this);
   pnl.addBottomButton("GC","GC",this);
   pnl.addBottomButton("Heap","HEAP",this);
   pnl.addBottomButton("Mark","MARK",this);
   pnl.addBottomButton("Dismiss","EXIT",this);
   pnl.addBottomButtons();

   doing_setup = false;

   return pnl;
}



/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

void update()
{
   doing_setup = true;

   String s = start_label.getText();
   if (s == null || !s.equals(for_main.getStartClass()))
      start_label.setText(for_main.getStartClass());
   if (enabled_btn.isSelected() != for_main.isEnabled())
      enabled_btn.setSelected(for_main.isEnabled());
   String pvs = (String) overhead_choice.getSelectedItem();
   String cur = overhead_values.get(for_main.getOverhead());
   if (pvs == null || !pvs.equals(cur)) overhead_choice.setSelectedItem(cur);

   doing_setup = false;
}




/********************************************************************************/
/*										*/
/*	Agent methods								*/
/*										*/
/********************************************************************************/

Set<String> getAgents(String pid)
{
   String cmd = "LISTAGENTS";
   if (pid != null) cmd += " " + pid;

   String r = for_main.dymonCommand(cmd);
   if (r == null) return null;


   Set<String> rslt = new TreeSet<String>();
   StringTokenizer tok = new StringTokenizer(r,",");
   while (tok.hasMoreTokens()) {
      rslt.add(tok.nextToken());
    }

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Callback methods							*/
/*										*/
/********************************************************************************/

public void actionPerformed(ActionEvent e)
{
   if (doing_setup) return;

   String c = e.getActionCommand();

   if (c == null) return;
   else if (c.equals("Monitor Enabled")) {
      String cmd = "ENABLE " + active_view.getId();
      for_main.dymonCommand(cmd);
    }
   else if (c.equals("Overhead")) {
      double v = for_main.getOverhead();
      for (Map.Entry<Double,String> ent : overhead_values.entrySet()) {
	 if (ent.getValue() == overhead_choice.getSelectedItem()) {
	    v = ent.getKey();
	    break;
	  }
       }
      String cmd = "OVERHEAD " + active_view.getId() + " " + v;
      for_main.dymonCommand(cmd);
    }
   else if (c.equals("CLEAR")) {
      String cmd = "CLEAR " + active_view.getId();
      for_main.dymonCommand(cmd);
    }
   else if (c.equals("GC")) {
      String cmd = "GC " + active_view.getId();
      for_main.dymonCommand(cmd);
    }
   else if (c.equals("HEAP")) {
      String cmd = "SHOWHEAP " + active_view.getId();
      for_main.dymonCommand(cmd);
    }
   else if (c.equals("MARK")) {
      String what = JOptionPane.showInputDialog(active_view,"Enter Mark Type","");
      if (what != null) for_main.createUserMark(0,what);
    }
   else if (c.equals("EXIT")) {
      setVisible(false);
    }
   else if (c.equals("TIMEVIEW")) {
      for_main.showTimeLine();
    }
   else if (c.equals("MEMVIEW")) {
      for_main.showMemoryView();
    }
   else System.err.println("DYVISION CONTROL: PERFORM " + c + " " + e.getSource());
}



private class AgentCallback implements ActionListener {

   public void actionPerformed(ActionEvent evt) {
      if (active_agents == null) return;
      String agt = evt.getActionCommand();
      JCheckBox cbx = (JCheckBox) evt.getSource();
      if (cbx.isSelected()) active_agents.add(agt);
      else active_agents.remove(agt);
      StringBuffer buf = new StringBuffer();
      for (String s : active_agents) {
	 if (buf.length() > 0) buf.append(",");
	 buf.append(s);
       }
      String cmd = "AGENTS " + active_view.getId() + " " + buf.toString();
      for_main.dymonCommand(cmd);
    }

}	// end of subclass AgentCallback




}	// end of class DyvisionControlPanel




/* end of DyvisionControlPanel.java */
