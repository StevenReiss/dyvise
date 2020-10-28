/********************************************************************************/
/*										*/
/*		DyvisionTime.java						*/
/*										*/
/*	Time view for dyper performance evaluation interface			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyvision/DyvisionTime.java,v 1.9 2011-09-12 18:30:10 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyvisionTime.java,v $
 * Revision 1.9  2011-09-12 18:30:10  spr
 * Code cleanup
 *
 * Revision 1.8  2009-10-07 01:00:24  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.7  2009-06-04 18:55:11  spr
 * Handle bad trace directory.
 *
 * Revision 1.6  2009-05-01 23:15:30  spr
 * Handle scaling graphs in time view.
 *
 * Revision 1.5  2009-04-28 18:01:26  spr
 * Add graphs to time lines.
 *
 * Revision 1.4  2009-04-11 23:47:31  spr
 * Handle formating using IvyFormat.
 *
 * Revision 1.3  2009-03-20 02:09:09  spr
 * Add memory view and related controls. Clean code.
 *
 * Revision 1.2  2008-12-04 01:11:27  spr
 * Fix up time display.  Add termination.
 *
 * Revision 1.1  2008-11-24 23:40:04  spr
 * Add time line view.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dyvision;


import edu.brown.cs.dyvise.dyvise.*;

import edu.brown.cs.ivy.swing.*;

import org.w3c.dom.Element;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.*;
import java.util.List;


class DyvisionTime extends JFrame implements DyvisionConstants, DyviseConstants.TimeListener
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private DyviseTimeManager time_manager;
private String		for_process;

private DyvisionTimeData time_data;

private TimePanel	time_panel;
private JPanel		graph_view;
private JPanel		time_bar;
private Stroke		cursor_stroke;
private Stroke		mark_stroke;
private JFrame		graph_panel;

private List<GraphData> graph_items;


private int		mouse_position;

private double		base_height = 0;
private double		bar_height = 20;

private static float [] DOTS = { 2.0f, 2.0f };
private static float [] MARK = { 1.0f, 2.0f };

private static Map<String,Color> mark_colors;

private static final long serialVersionUID = 1;


static {
   mark_colors = new HashMap<String,Color>();
   mark_colors.put("USER",Color.BLUE);
   mark_colors.put("HEAP",Color.CYAN);
   mark_colors.put("*",Color.GRAY);
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DyvisionTime(String pid)
{
   for_process = pid;

   time_manager = DyviseTimeManager.getTimeManager(pid);
   time_manager.addTimeListener(this);

   time_data = new DyvisionTimeData();
   graph_panel = null;

   cursor_stroke = new BasicStroke(1.0f,BasicStroke.CAP_SQUARE,BasicStroke.JOIN_BEVEL,1.0f,
				      DOTS,0f);
   mark_stroke = new BasicStroke(1.0f,BasicStroke.CAP_SQUARE,BasicStroke.JOIN_BEVEL,1.0f,
				      MARK,0f);

   mouse_position = -1;

   graph_items = new ArrayList<GraphData>();
   setupGraphs();

   setupWindow();
}




/********************************************************************************/
/*										*/
/*	Window setup methods							*/
/*										*/
/********************************************************************************/

void setupWindow()
{
   time_panel = new TimePanel();
   time_panel.setInsets(0);

   setDefaultCloseOperation(HIDE_ON_CLOSE);
   setTitle(for_process + " DYVISE Time View");

   MenuBar mb = new MenuBar();
   setJMenuBar(mb);

   graph_view = new JPanel();
   graph_view.setPreferredSize(new Dimension(400,200));
   time_panel.addGBComponent(graph_view,0,0,0,1,10,10);

   time_bar = new JPanel();
   time_bar.setPreferredSize(new Dimension(400,10));
   time_bar.setMinimumSize(new Dimension(100,10));
   time_bar.setOpaque(false);
   Mouser m = new Mouser();
   time_panel.addMouseListener(m);
   time_panel.addMouseMotionListener(m);
   time_panel.addGBComponent(time_bar,0,1,0,1,10,0);

   setContentPane(time_panel);

   pack();

   graph_panel = new GraphPanel();
}



void setupGraphs()
{
   new GraphData("CPU Load","CPU LOAD",Color.BLUE);
   new GraphData("Memory Used","MEMORY USED",Color.RED);
   new GraphData("Object Count","OBJECT COUNT",Color.MAGENTA);
   new GraphData("GCs / minute","GCs PER Minute",Color.BLACK);
   new GraphData("Allocs / minute","ALLOCS PER Minute",Color.CYAN);
   new GraphData("Event %","Event Percentage",Color.GREEN);
   new GraphData("Thread Block","% BLOCK",Color.YELLOW);
}



/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

void update(Element e)
{
   if (e == null) return;

   time_data.addData(e);

   if (isVisible()) repaint();
}



public void handleMark(long when,String type)
{
   if (time_data == null || when <= 0 || type == null) return;

   time_data.addMark(when,type);

   if (isVisible()) repaint();
}



public void handleTimeSet(long when)
{
   if (isVisible()) repaint();
}




/********************************************************************************/
/*										*/
/*	TimeLine painting methods						*/
/*										*/
/********************************************************************************/

private void paintTimeLine(Graphics2D g2)
{
   long stime = time_data.getStartTime();
   long etime = time_data.getEndTime();

   if (base_height == 0) {
      double ct = time_data.getNumValues();
      if (ct == 0) return;
      base_height = bar_height / ct;
    }

   double t = etime - stime;
   Dimension sz = time_panel.getSize();
   double w = sz.width;
   double m = mouse_position;
   long tt = time_manager.getCurrentTime();

   float [] vals  = null;
   Line2D line = new Line2D.Double();

   double d = 0;
   if (mouse_position >= 0) {
      d = findDistance(m,w,t);
    }

   long v0 = stime;
   for (int i = 0; i < sz.width; ++i) {
      double k = findHeight(m,w,i);
      double x0 = findPosition(d,m,w,t,i+1);
      double pos = x0 + stime;
      long v1 = (long) (pos + 0.5);
      vals = time_data.getIntervalData(v0,v1,vals);
      for (int j = 0; j < vals.length; ++j) {
	 Color c = getColorValue(vals[j],1.0);
	 g2.setColor(c);
	 line.setLine(i,sz.height-j*k,i,sz.height-(j+1)*k);
	 g2.fill(line);
	 g2.draw(line);
       }
      Collection<String> marks = time_data.getMarks(v0,v1);
      for (String mtyp : marks) {
	 line.setLine(i,sz.height,i,sz.height-(vals.length)*k);
	 drawMark(g2,mtyp,line);
       }

      if (tt > 0 && tt >= v0 && tt <= v1) {
	 line.setLine(i,sz.height,i,sz.height-(vals.length)*k);
	 Stroke s = g2.getStroke();
	 g2.setColor(Color.BLACK);
	 g2.setStroke(cursor_stroke);
	 g2.draw(line);
	 g2.setStroke(s);
       }
      v0 = v1;
    }
}



private double findDistance(double m,double w,double t)
{
   double g = t / time_data.getNumIntervals();

   if (g*w > t) return 0;		// no scaling needed

   double d0 = 0;
   double d1 = t;
   int mx = (int)(Math.log(t)/Math.log(2));

   for (int i = 0; i < mx; ++i) {
      double d = (d0+d1)/2.0;
      double a1 = Math.atan2(g,d);
      double a2 = Math.atan2(m/w*t,d);
      double a3 = Math.atan2((w-m)/w*t,d);
      double x = a1 / (a2 + a3);
      if (x > 1/w) d0 = d;
      else d1 = d;
    }
   return (d0 + d1) / 2.0;
}




private double findPosition(double d,double m,double w,double t,double i)
{
   double v = t * m/w;

   double x = 0;
   double a4,a5;

   if (m < 0 || d == 0) return i * t/w;

   if (i <= m) {
      double a2 = Math.atan2(m/w*t,d);
      if (m == 0) a4 = 0;
      else a4 = (m-i)/m;
      a5 = a4*a2;
      x = v-d*Math.tan(a5);
    }
   else {
      double a3 = Math.atan2((w-m)/w*t,d);
      if (w == m) a4 = 0;
      else a4 = (i-m)/(w-m);
      a5 = a4*a3;
      x = d*Math.tan(a5) + v;
    }

   if (x < 0) x = 0;

   return x;
}




private double findHeight(double m,double w,double i)
{
   double k = base_height;
   if (m >= 0) {
      double dx = i - m;
      dx /= w/8;
      if (Math.abs(dx) < 1) {
	 double y = -2 * dx*dx + 3;
	 k *= y;
       }
    }
   return k;
}




private Color getColorValue(double v,double cv)
{
   float h = (float)((1.0 - v) / 3.0);
   float s = (float)(cv * 0.8 + 0.2);

   int icol = Color.HSBtoRGB(h,s,1f);

   return new Color(icol);
}




private void drawMark(Graphics2D g,String typ,Line2D l)
{
   Stroke ss = g.getStroke();
   Color cs = g.getColor();

   g.setStroke(mark_stroke);

   Color c = mark_colors.get(typ);
   if (c == null) c = mark_colors.get("*");

   g.setColor(c);

   g.draw(l);

   g.setStroke(ss);
   g.setColor(cs);
}



/********************************************************************************/
/*										*/
/*	Graph painting methods							*/
/*										*/
/********************************************************************************/

private void paintGraphs(Graphics2D g2)
{
   long stime = time_data.getStartTime();
   long etime = time_data.getEndTime();

   double t = etime - stime;
   Dimension sz = graph_view.getSize();
   double w = sz.width;
   double m = mouse_position;
   long tt = time_manager.getCurrentTime();

   float [] minv = null;
   float [] maxv = null;
   float [] vals  = null;
   Line2D line = new Line2D.Double();

   minv = time_data.getMeterMins(minv);
   maxv = time_data.getMeterMaxs(maxv);
   Path2D.Float [] paths = new Path2D.Float[minv.length];

   double d = 0;
   if (mouse_position >= 0) {
      d = findDistance(m,w,t);
    }

   long v0 = stime;
   for (int i = 0; i < sz.width; ++i) {
      double x0 = findPosition(d,m,w,t,i+1);
      double pos = x0 + stime;
      long v1 = (long) (pos + 0.5);
      vals = time_data.getMeterData(v0,v1,vals);
      for (int j = 0; j < vals.length; ++j) {
	 if (minv[j] == maxv[j]) continue;
	 double y = (vals[j] - minv[j])/(maxv[j] - minv[j]);
	 if (y < 0) y = 0;
	 if (y > 1) y = 1;
	 y = (1-y)*sz.height;
	 if (i == 0) {
	    paths[j] = new Path2D.Float();
	    paths[j].moveTo(i,y);
	  }
	 else paths[j].lineTo(i,y);
       }
      for (int j = 0; j < vals.length; ++j) {
	 if (paths[j] == null) continue;
	 GraphData gd = graph_items.get(j);
	 if (gd == null || !gd.isEnabled()) continue;
	 g2.setColor(gd.getColor());
	 g2.draw(paths[j]);
       }

      Collection<String> marks = time_data.getMarks(v0,v1);
      for (String mtyp : marks) {
	 line.setLine(i,0,i,sz.height);
	 drawMark(g2,mtyp,line);
       }

      if (tt > 0 && tt >= v0 && tt <= v1) {
	 line.setLine(i,0,i,sz.height);
	 Stroke s = g2.getStroke();
	 g2.setColor(Color.BLACK);
	 g2.setStroke(cursor_stroke);
	 g2.draw(line);
	 g2.setStroke(s);
       }
      v0 = v1;
    }
}




/********************************************************************************/
/*										*/
/*	Mouse methods								*/
/*										*/
/********************************************************************************/

private void setMousePosition(int pos)
{
   if (mouse_position == pos) return;

   mouse_position = pos;
   time_panel.repaint();
}



private void handleClick(MouseEvent e)
{
   if (mouse_position >= 0) {
      if (e.getButton() == MouseEvent.BUTTON3) {
	 time_manager.setCurrentTime(0);
       }
      else {
	 double etime = time_data.getEndTime();
	 double stime = time_data.getStartTime();
	 double t = etime - stime;
	 double w = time_panel.getWidth();
	 double d = findDistance(mouse_position,w,t);
	 double p = findPosition(d,mouse_position,w,t,e.getX());
	 long tp = (long)(p + stime);
	 if (e.getButton() == MouseEvent.BUTTON1) {
	    time_manager.setCurrentTime(tp);
	  }
	 else if (e.getButton() == MouseEvent.BUTTON2) {
	    String what = JOptionPane.showInputDialog(this,"Enter Mark Type","");
	    if (what != null) {
	       time_manager.createUserMark(tp,what);
	     }
	  }
       }
    }
}




/********************************************************************************/
/*										*/
/*	Panel for overall drawing						*/
/*										*/
/********************************************************************************/

private class TimePanel extends SwingGridPanel {

   private static final long serialVersionUID = 1;

   public void paint(Graphics g) {
      paintBorder(g);
      paintChildren(g);
      paintComponent(g);
    }

   protected void paintComponent(Graphics g) {
      paintGraphs((Graphics2D) g);
      paintTimeLine((Graphics2D) g);
    }

}	// end of subclass TimePanel



/********************************************************************************/
/*										*/
/*	Graph information							*/
/*										*/
/********************************************************************************/

private class GraphData {

   private String graph_label;
   private String meter_name;
   private Color draw_color;
   private boolean is_enabled;

   GraphData(String lbl,String mnm,Color c) {
      graph_label = lbl;
      meter_name = mnm;
      draw_color = c;
      is_enabled = true;
      time_data.useMeter(mnm);		// these two lines go together so that
      graph_items.add(this);		// meter indices match graph_items indices
    }

   String getName()				{ return graph_label; }
   Color getColor()				{ return draw_color; }
   boolean isEnabled()				{ return is_enabled; }
   double getMinValue() 			{ return time_data.getMeterMin(meter_name); }
   double getMaxValue() 			{ return time_data.getMeterMax(meter_name); }

   void setEnabled(boolean fg)			{ is_enabled = fg; }
   void setColor(Color c)			{ draw_color = c; }

}	// end of subclass GraphData




/********************************************************************************/
/*										*/
/*	Mouse listener								*/
/*										*/
/********************************************************************************/

private class Mouser extends MouseAdapter {

   Dimension panel_size;

   Mouser() {
      panel_size = null;
    }

   public void mouseEntered(MouseEvent e) {
      panel_size = time_panel.getSize();
      checkMouse(e.getX(),e.getY());
    }

   public void mouseExited(MouseEvent e) {
      setMousePosition(-1);
    }

   public void mouseMoved(MouseEvent e) {
      checkMouse(e.getX(),e.getY());
    }

   public void mouseClicked(MouseEvent e) {
      checkMouse(e.getX(),e.getY());
      handleClick(e);
    }

   private void checkMouse(int x,int y) {
      if (panel_size == null) panel_size = time_panel.getSize();
      int ht = (int) findHeight(mouse_position,panel_size.width,x);
      ht *= time_data.getNumValues();
      if (y < panel_size.height - ht) {
	 setMousePosition(-1);
	 return;
       }
      setMousePosition(x);
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
      addButton(m,"Quit","Exit from Dyvision");
      add(m);

      m = new JMenu("Display");
      addButton(m,"Graphs ...","Select graphs to display");
      add(m);
    }

   public void actionPerformed(ActionEvent e) {
      String btn = e.getActionCommand();

      if (btn == null) ;
      else if (btn.equals("Graphs ...")) {
	 if (graph_panel == null) graph_panel = new GraphPanel();
	 graph_panel.setVisible(true);
       }
      else if (btn.equals("Quit")) {
	 System.exit(0);
       }
    }

}	// end of subclass MenuBar




/********************************************************************************/
/*										*/
/*	GraphPanel -- panel showing graph information				*/
/*										*/
/********************************************************************************/

private class GraphPanel extends JFrame implements ActionListener {

   private Map<Object,GraphData> panel_map;

   private static final long serialVersionUID = 1;


   GraphPanel() {
      panel_map = new HashMap<Object,GraphData>();

      SwingGridPanel pnl = new SwingGridPanel();
      pnl.beginLayout();
      pnl.addBannerLabel("Time Graph Display for " + for_process);
      pnl.addSeparator();

      for (GraphData gd : graph_items) {
	 pnl.addSectionLabel(gd.getName());
	 JCheckBox cbx = pnl.addBoolean("Enabled",gd.isEnabled(),this);
	 SwingColorButton cc = pnl.addColorField("Color",gd.getColor(),this);
	 String rval = Double.toString(gd.getMinValue()) + " to " + Double.toString(gd.getMaxValue());
	 pnl.addDescription("Range",rval);
	 pnl.addSeparator();
	 panel_map.put(cbx,gd);
	 panel_map.put(cc,gd);
       }

      pnl.addBottomButton("Dismiss","DISMISS",this);
      pnl.addBottomButtons();

      setContentPane(pnl);
      pack();
    }

   public void actionPerformed(ActionEvent e) {
      String btn = e.getActionCommand();
      Object src = e.getSource();
      GraphData gd = panel_map.get(src);

      if (btn == null) ;
      else if (btn.equals("DISMISS")) {
	 setVisible(false);
       }
      else if (btn.equals("Enabled")) {
	 JCheckBox cbx = (JCheckBox) src;
	 gd.setEnabled(cbx.isSelected());
       }
      else if (btn.equals("Color")) {
	 SwingColorButton scb = (SwingColorButton) src;
	 gd.setColor(scb.getColor());
       }
      else System.err.println("GRAPH PANEL ACTION " + btn);
   }

}	// end of subclass GraphPanel




}	// end of class DyvisionTime




/* end of DyvisionTime.java */














