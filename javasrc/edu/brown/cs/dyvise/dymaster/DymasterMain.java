/********************************************************************************/
/*										*/
/*		DymasterMain.java						*/
/*										*/
/*	Main program for dyper performance evaluation master interface		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymaster/DymasterMain.java,v 1.7 2013-08-05 12:03:35 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymasterMain.java,v $
 * Revision 1.7  2013-08-05 12:03:35  spr
 * Updates; use dypatchasm.
 *
 * Revision 1.6  2010-03-30 16:21:37  spr
 * Change mint mode; fix up process tracking.
 *
 * Revision 1.5  2009-09-19 00:09:08  spr
 * Update master to use new databse.
 *
 * Revision 1.4  2009-06-04 18:53:21  spr
 * Use dyviseJava call if needed.
 *
 * Revision 1.3  2009-04-28 18:00:44  spr
 * Better syncrhonization.
 *
 * Revision 1.2  2009-03-20 02:06:10  spr
 * Add options before enabling for selective enable.
 *
 * Revision 1.1  2008-11-24 23:39:54  spr
 * Master control for dyvise now here.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymaster;


import edu.brown.cs.dyvise.dymon.DymonRemote;
import edu.brown.cs.ivy.exec.IvyExecQuery;
import edu.brown.cs.ivy.mint.*;
import edu.brown.cs.ivy.swing.SwingMenuBar;
import edu.brown.cs.ivy.swing.SwingSetup;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.*;
import javax.swing.table.*;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;
import java.util.List;



public class DymasterMain implements DymasterConstants, MintConstants {



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   DymasterMain dm = new DymasterMain(args);

   dm.process();
}




/********************************************************************************/
/*										*/
/*	Local Storage								*/
/*										*/
/********************************************************************************/

private DymonRemote	dymon_remote;
private JFrame		master_frame;
private ProcessTable	proc_table;
private ProcessAction	proc_action;
private UnattachedPopup unattached_menu;
private String		default_agents;

private MintControl	mint_control;

private List<Process>	active_processes;
private Map<String,Process> process_map;
private Set<Process>	show_pending;



/********************************************************************************/
/*										*/
/*	Table definitions							*/
/*										*/
/********************************************************************************/

private static String [] col_names = new String[] {
   "Host", "Process ID", "Main Class", "Attached" };

private static Class<?> [] col_types = new Class<?>[] {
   String.class, Integer.class, String.class, Boolean.class };

private static int [] col_sizes = new int [] {
   60, 75, 200, 75
};

private static boolean [] col_resize = new boolean [] {
   true, false, true, false
};




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private DymasterMain(String [] args)
{
   new SwingSetup();

   default_agents = null;

   scanArgs(args);

   mint_control = MintControl.create(DYPER_MESSAGE_BUS,MintSyncMode.SINGLE);

   mint_control.register("<DYMASTER COMMAND='_VAR_0'/>",new CommandHandler());

   dymon_remote = new DymonRemote();

   show_pending = new HashSet<Process>();
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

private void process()
{
   unattached_menu = new UnattachedPopup();

   master_frame = new JFrame();
   master_frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
   master_frame.setTitle("DYVISE Master Monitor");

   MenuBar mb = new MenuBar();
   master_frame.setJMenuBar(mb);

   active_processes = new ArrayList<Process>();
   process_map = new HashMap<String,Process>();

   proc_table = new ProcessTable();

   proc_action = new ProcessAction();
   proc_action.run();
   dymon_remote.scheduleEvery(proc_action,DYMASTER_PROCESS_UPDATE);

   // proc_table.updateSize();

   master_frame.getContentPane().add(new JScrollPane(proc_table),BorderLayout.CENTER);
   master_frame.pack();
   master_frame.setVisible(true);
}




/********************************************************************************/
/*										*/
/*	Argument scanning methods						*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
	 if (args[i].startsWith("-H")) {                        // -HEAP
	    default_agents = "HEAP";
	  }
	 else badArgs();
       }
      else badArgs();
    }
}



private void badArgs()
{
   System.err.println("DYMASTER: dymaster");
   System.exit(1);
}



/********************************************************************************/
/*										*/
/*	Update action								*/
/*										*/
/********************************************************************************/

private synchronized void updateProcesses(Element xml)
{
   boolean chng = false;

   Set<Process> found = new HashSet<Process>();

   for (Element e : IvyXml.elementsByTag(xml,"PROCESS")) {
      String id = IvyXml.getTextElement(e,"ID");
      Process p = process_map.get(id);
      if (p == null) {
	 p = new Process(e);
	 active_processes.add(p);
	 process_map.put(id,p);
	 chng = true;
       }
      else {
	 chng |= p.update(e);
       }

      found.add(p);
    }

   for (Iterator<Process> it = active_processes.iterator(); it.hasNext(); ) {
      Process p = it.next();
      if (!found.contains(p)) {
	 System.err.println("DYMASTER: REMOVE PROCESS " + p.getId());
	 // System.err.println("UPDATE: " + IvyXml.convertXmlToString(xml));
	 it.remove();
	 show_pending.remove(p);
	 process_map.remove(p.getId());
	 chng = true;
       }
    }

   if (chng && proc_table != null) {
      proc_table.updateSize();
      proc_table.modelUpdated();
    }
}



private synchronized Process getProcess(int idx)
{
   return active_processes.get(idx);
}


private synchronized Process getActualProcess(int idx)
{
   if (proc_table != null) {
      RowSorter<?> rs = proc_table.getRowSorter();
      if (rs != null) {
	 idx = rs.convertRowIndexToModel(idx);
       }
    }

   return getProcess(idx);
}


private synchronized int getNumProcesses()
{
   return active_processes.size();
}


private class ProcessAction extends TimerTask {

   private long last_id;
   private int time_count;

   ProcessAction() {
      last_id = -1;
      time_count = 0;
    }

   public void run() {
      String cmd = "PTABLE";
      if (time_count++ == 24) {
	 time_count = 0;
       }
      else if (last_id > 0) cmd += " " + last_id;
      String rslt = dymon_remote.dymonCommand(cmd);
      if (rslt == null) return;
      Element e = IvyXml.convertStringToXml(rslt);
      last_id = IvyXml.getAttrLong(e,"COUNT");
      updateProcesses(e);

      checkPending();
    }

}	// end of subclass ProcessAction




/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

private void showViewer(Process p)
{
   if (p == null || !p.isAttached()) return;
   if (show_pending.contains(p)) return;

   MintDefaultReply mr = new MintDefaultReply();
   String msg = "<DYVISION PID='" + p.getProcessId() + "' COMMAND='SHOW'></DYVISION>";
   mint_control.send(msg,mr,MINT_MSG_FIRST_NON_NULL);
   String rslt = mr.waitForString(60000);
   if (rslt != null) return;
   synchronized (this) {
      show_pending.add(p);
    }

   try {
      DymonRemote.dyviseJava(DYMASTER_DYVISION_CLASS,DYMASTER_DYVISION_JARGS,p.getProcessId());
    }
   catch (IOException e) {
      System.err.println("DYMASTER: Problem starting dyvision: " + e);
      synchronized (this) {
	 show_pending.remove(p);
       }
    }
}


private void checkPending()
{
   Collection<Process> tocheck;
   synchronized (this) {
      tocheck = new ArrayList<Process>(show_pending);
    }

   for (Process p : tocheck) {
      MintDefaultReply mr = new MintDefaultReply();
      String msg = "<DYVISION PID='" + p.getProcessId() + "' COMMAND='PING'></DYVISION>";
      mint_control.send(msg,mr,MINT_MSG_FIRST_NON_NULL);
      String rslt = mr.waitForString(100);
      if (rslt != null) {
	 synchronized (this) {
	    show_pending.remove(p);
	  }
       }
    }
}




/********************************************************************************/
/*										*/
/*	Host maintenance methods						*/
/*										*/
/********************************************************************************/

private void addRemoteHost()
{
   String host = JOptionPane.showInputDialog(master_frame,"Enter host name","Add Remote Host",
						JOptionPane.QUESTION_MESSAGE);
   if (host == null || host.length() == 0) return;

   dymon_remote.dymonCommand("HOST ADD " + host);
}



private void addLocalHost()
{
   dymon_remote.dymonCommand("HOST ADD " + IvyExecQuery.getHostName());
}



private void removeHost()
{
   String hostl = dymon_remote.dymonCommand("HOST LIST");
   if (hostl == null) return;
   StringTokenizer tok = new StringTokenizer(hostl,", \t");
   int ntok = tok.countTokens();
   String [] hosts = new String[ntok];
   int i = 0;
   while (tok.hasMoreTokens()) {
      hosts[i++] = tok.nextToken();
    }

   String rhost = (String) JOptionPane.showInputDialog(master_frame,"Select Host to Remove",
							  "Remove Host",
							  JOptionPane.QUESTION_MESSAGE,
							  null,hosts,hosts[0]);

   if (rhost == null) return;

   dymon_remote.dymonCommand("HOST REMOVE " + rhost);
}




private void removeRemoteHosts()
{
   String lcl = IvyExecQuery.getHostName();
   String hostl = dymon_remote.dymonCommand("HOST LIST");
   if (hostl == null) return;
   StringTokenizer tok = new StringTokenizer(hostl,", \t");
   while (tok.hasMoreTokens()) {
      String s = tok.nextToken();
      if (s.equals(lcl)) continue;
      dymon_remote.dymonCommand("HOST REMOVE " + s);
    }
}




/********************************************************************************/
/*										*/
/*	Menu bar								*/
/*										*/
/********************************************************************************/

private class MenuBar extends SwingMenuBar {

   private static final long serialVersionUID = 1;


   MenuBar() {
      JMenu m = new JMenu("File");
      addButton(m,"Quit","Exit from Dymaster");
      add(m);

      m = new JMenu("Process");
      addButton(m,"Add Local Host","Add local host as a source for processes");
      addButton(m,"Add Host ...","Add other nodes as hosts for processes");
      addButton(m,"Remove Host ...","Remove host as a source for processes");
      addButton(m,"Remove Remote Hosts","Remove all remote hosts");
      add(m);
    }

   public void actionPerformed(ActionEvent e) {
      String btn = e.getActionCommand();

      if (btn == null) ;
      else if (btn.equals("Quit")) {
	 System.exit(0);
       }
      else if (btn.equals("Find ...")) {
       }
      else if (btn.equals("Add Local Host")) {
	 addLocalHost();
       }
      else if (btn.equals("Add Host ...")) {
	 addRemoteHost();
       }
      else if (btn.equals("Remove Host ...")) {
	 removeHost();
       }
      else if (btn.equals("Remove Remote Hosts")) {
	 removeRemoteHosts();
       }
    }

}	// end of subclass MenuBar




/********************************************************************************/
/*										*/
/*	Unattached process popup menu						*/
/*										*/
/********************************************************************************/

private abstract class Popup extends JPopupMenu implements ActionListener {

   private static final long serialVersionUID = 1L;

   protected JMenuItem addButton(String id,String tt) {
      JMenuItem itm = new JMenuItem(id);
      itm.addActionListener(this);
      itm.setToolTipText(tt);
      if (tt != null) ToolTipManager.sharedInstance().registerComponent(itm);
      add(itm);
      return itm;
    }

   protected JCheckBoxMenuItem addCheckButton(String id,boolean fg,String tt) {
      JCheckBoxMenuItem itm = new JCheckBoxMenuItem(id,fg);
      itm.addActionListener(this);
      itm.setToolTipText(tt);
      if (tt != null) ToolTipManager.sharedInstance().registerComponent(itm);
      add(itm);
      return itm;
    }

}	// end of abstract class Popup




private class UnattachedPopup extends Popup implements ActionListener {

   private Process for_process;
   private JCheckBoxMenuItem all_button;
   private JCheckBoxMenuItem heap_button;
   private JCheckBoxMenuItem enable_button;

   private static final long serialVersionUID = 1;

   UnattachedPopup() {
      for_process = null;
      all_button = addCheckButton("All Agents",false,"Enable all agents for process");
      heap_button = addCheckButton("Heap Only",false,"Enable only heap display for process");
      addButton("Select Agents ...","Selectively enable agents for process");
      enable_button = addCheckButton("Enable",true,"Enable monitoring once attached");
      addButton("Attach","Begin monitoring process with selected agents");
    }

   void show(Process p,MouseEvent e) {
      for_process = p;
      if (p == null) return;
      String agts = p.getAgentSet();
      if (agts == null || agts.indexOf("ALL") >= 0 || agts.indexOf("*") >= 0) all_button.setState(true);
      else all_button.setState(false);
      if (agts != null && agts.equals("HEAP")) heap_button.setState(true);
      else heap_button.setState(false);
      enable_button.setState(p.isMonitored());
      show(e.getComponent(),e.getX(),e.getY());
    }

   public void actionPerformed(ActionEvent e) {
      String cmd = e.getActionCommand();
      if (cmd.equals("All Agents")) for_process.setAgentSet("*");
      else if (cmd.equals("Heap Only")) for_process.setAgentSet("HEAP");
      else if (cmd.equals("Select Agents ...")) ;
      else if (cmd.equals("Enable")) for_process.setEnabled(!for_process.isMonitored());
      else if (cmd.equals("Attach")) for_process.setAttached(true);
    }

}	// end of subclass UnattachedPopup




/********************************************************************************/
/*										*/
/*	Display table								*/
/*										*/
/********************************************************************************/

private class ProcessTable extends JTable implements MouseListener {

   private static final long serialVersionUID = 1;

   ProcessTable() {
      super(new ProcessModel());
      setAutoCreateRowSorter(true);
      fixColumnSizes();
      setIntercellSpacing(new Dimension(10,1));
      setToolTipText("");
      addMouseListener(this);
      setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      getColumnModel().getColumn(2).setCellRenderer(new ActiveRenderer());
      for (Enumeration<TableColumn> e = getColumnModel().getColumns(); e.hasMoreElements(); ) {
	 TableColumn tc = e.nextElement();
	 tc.setHeaderRenderer(new HeaderRenderer(getTableHeader().getDefaultRenderer()));
       }
      DefaultRowSorter<?,?> rs = (DefaultRowSorter<?,?>) getRowSorter();
      rs.setComparator(2,new ProcessComparator());
    }

   void modelUpdated() {
      ProcessModel pm = (ProcessModel) getModel();
      pm.fireTableDataChanged();
      // doLayout();
    }

   void updateSize() {
      Dimension d = getPreferredSize();
      d.width += 100;
      d.height += 16;
      setPreferredScrollableViewportSize(d);
    }

   private void fixColumnSizes() {
      TableColumnModel tcm = getColumnModel();
      for (int i = 0; i < 4; ++i) {
	 TableColumn tc = tcm.getColumn(i);
	 tc.setPreferredWidth(col_sizes[i]);
	 if (!col_resize[i]) {
	    tc.setMaxWidth(col_sizes[i]);
	    tc.setMinWidth(col_sizes[i]);
	  }
       }
    }

   public void mouseClicked(MouseEvent e) {
      int row = rowAtPoint(e.getPoint());
      int srow = getSelectedRow();
      if (srow != row || srow < 0) return;
      Process p = getActualProcess(row);
      if (p == null) return;

      if (e.getClickCount() >= 2) {
	 showViewer(p);
       }
    }

   public void mouseEntered(MouseEvent _e)				{ }
   public void mouseExited(MouseEvent _e)				{ }
   public void mouseReleased(MouseEvent e) {
      checkPopup(e);
    }
   public void mousePressed(MouseEvent e) {
      checkPopup(e);
    }

   private void checkPopup(MouseEvent e) {
      int row = rowAtPoint(e.getPoint());
      int srow = getSelectedRow();
      if (srow != row || srow < 0) return;
      Process p = getActualProcess(row);
      if (p == null) return;

      if (e.isPopupTrigger()) {
	 if (p.isAttached()) return;
	 else unattached_menu.show(p,e);
       }
    }

   public String getToolTipText(MouseEvent e) {
      int r = rowAtPoint(e.getPoint());
      if (r < 0) return null;
      Process p = getActualProcess(r);
      return p.getToolTip();
    }

   protected void paintComponent(Graphics g) {
      synchronized (DymasterMain.this) {
	 super.paintComponent(g);
       }
    }

}



/********************************************************************************/
/*										*/
/*	Table model for processes						*/
/*										*/
/********************************************************************************/

private class ProcessModel extends AbstractTableModel {

   private static final long serialVersionUID = 1;


   ProcessModel() { }

   @Override public int getColumnCount()		{ return col_names.length; }

   @Override public String getColumnName(int idx) {
      return col_names[idx];
    }

   @Override public Class<?> getColumnClass(int idx) {
      return col_types[idx];
    }

   @Override public boolean isCellEditable(int row,int col) {
      if (col == 3) return true;
      return false;
    }

   @Override public int getRowCount() {
      return getNumProcesses();
    }

   @Override public Object getValueAt(int row,int col) {
      Process p = getProcess(row);
      switch (col) {
	 case 0 :
	    return p.getHost();
	 case 1 :
	    return Integer.valueOf(p.getPid());
	 case 2 :
	    return p;
	 case 3 :
	    return Boolean.valueOf(p.isAttached());
       }

      return null;
    }

   @Override public void setValueAt(Object val,int row,int col) {
      if (col != 3) return;
      Process p = getProcess(row);
      p.setAttached(((Boolean) val).booleanValue());
    }

}	// end of ProcessModel




/********************************************************************************/
/*										*/
/*	Renderers for master table						*/
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




private class ActiveRenderer implements TableCellRenderer {

   private JLabel use_label;
   private Font   bold_font;
   private Font   norm_font;

   ActiveRenderer() {
      use_label = new JLabel();
      Font f = use_label.getFont();
      bold_font = f;
      f = f.deriveFont(Font.PLAIN);
      norm_font = f;
    }

   public Component getTableCellRendererComponent(JTable _t,Object v,boolean sel,
						     boolean foc,int r,int c) {
      use_label.setText(v.toString());

      Process p = getActualProcess(r);
      if (p.isMonitored()) {
	 use_label.setFont(bold_font);
	 use_label.setForeground(Color.BLACK);
       }
      else {
	 use_label.setFont(norm_font);
	 use_label.setForeground(Color.BLACK);
       }

      return use_label;
    }

}	// end of subclass ActiveRenderer




/********************************************************************************/
/*										*/
/*	Process information							*/
/*										*/
/********************************************************************************/

private class Process {

   private String full_id;
   private String host_name;
   private int	  process_id;
   private String full_start;
   private String show_start;
   private String start_jar;
   private String proc_args;
   private boolean is_monitored;
   private boolean is_attached;
   private String agent_set;
   private String local_agents;

   Process(Element xml) {
      full_id = IvyXml.getTextElement(xml,"ID");
      int idx = full_id.indexOf("@");
      host_name = full_id.substring(idx+1);
      process_id = Integer.parseInt(full_id.substring(0,idx));
      full_start = IvyXml.getTextElement(xml,"START");
      show_start = IvyXml.getTextElement(xml,"NAME");
      if (show_start == null) {
	 idx = full_start.lastIndexOf(".");
	 if (idx > 0) show_start = full_start.substring(idx+1);
	 else show_start = full_start;
       }
      start_jar = IvyXml.getTextElement(xml,"JAR");
      proc_args = IvyXml.getTextElement(xml,"SHOWARGS");
      agent_set = IvyXml.getTextElement(xml,"AGENTSET");
      is_monitored = Boolean.parseBoolean(IvyXml.getTextElement(xml,"MONITOR"));
      is_attached = Boolean.parseBoolean(IvyXml.getTextElement(xml,"ATTACHED"));
      if (!is_attached) local_agents = default_agents;
      else local_agents = null;
    }

   String getId()				{ return full_id; }
   String getHost()				{ return host_name; }
   int getPid() 				{ return process_id; }
   String getMainClass()			{ return show_start; }
   boolean isAttached() 			{ return is_attached; }
   boolean isMonitored()			{ return is_monitored; }
   String getProcessId()			{ return full_id; }
   String getAgentSet() {
      if (local_agents != null) return local_agents;
      return agent_set;
    }

   boolean update(Element xml) {
      boolean chng = false;
      boolean mfg = Boolean.parseBoolean(IvyXml.getTextElement(xml,"MONITOR"));
      if (!mfg == is_monitored) {
	 is_monitored = mfg;
	 chng = true;
       }
      mfg = Boolean.parseBoolean(IvyXml.getTextElement(xml,"ATTACHED"));
      if (!mfg == is_attached) {
	 is_attached = mfg;
	 chng = true;
       }
      String agts = IvyXml.getTextElement(xml,"AGENTSET");
      if (agts != null) {
	 if (!agts.equals(agent_set)) {
	    agent_set = agts;
	    chng = true;
	  }
       }
      return chng;
    }

   void setAttached(boolean fg) {
      if (local_agents != null) {
	 dymon_remote.dymonCommand("AGENTS " + full_id + " " + local_agents);
       }
      dymon_remote.dymonCommand("ATTACH " + full_id + " " + fg);
      local_agents = null;
    }

   void setAgentSet(String agts) {
      if (!is_attached) local_agents = agts;
      else dymon_remote.dymonCommand("AGENTS " + full_id + " " + agts);
    }

   void setEnabled(boolean fg) {
      dymon_remote.dymonCommand("ENABLE " + full_id + " " + fg);
    }

   String getToolTip() {
      StringBuffer buf = new StringBuffer();
      buf.append("<html>");
      buf.append("<div align='center'><b>Process " + full_id + "</b></div>");
      buf.append("Start Class: " + full_start);
      if (start_jar != null) buf.append("<br>Jar File:  " + start_jar);
      if (proc_args != null) buf.append("<br>Arguments: " + proc_args);
      buf.append("</html>");

      return buf.toString();
    }


   public String toString()			{ return getMainClass(); }

}	// end of subclass Process



private static class ProcessComparator implements Comparator<Process> {

   public int compare(Process p1,Process p2) {
      return p1.toString().compareTo(p2.toString());
    }

}	// end of subclass ProcessComparator




/********************************************************************************/
/*										*/
/*	Handle message-based commands						*/
/*										*/
/********************************************************************************/

private class CommandHandler implements MintHandler {

   public void receive(MintMessage msg,MintArguments args) {
      String rslt = null;
      String cmd = args.getArgument(0);
      if (cmd == null) return;
      if (cmd.equals("PING")) rslt = "PONG";
      String s = "<DYMASTER_REPLY>";
      if (rslt != null) s += rslt;
      s += "</DYMASTER_REPLY>";
      msg.replyTo(s);
    }

}	// end of subclass Command Handler




}	// end of class DymasterMain




/* end of DymasterMain.java */
