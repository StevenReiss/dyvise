/********************************************************************************/
/*										*/
/*		DyvisionSummary.java						*/
/*										*/
/*	Summary view for dyper performance evaluation interface 		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyvision/DyvisionSummary.java,v 1.8 2009-10-07 01:00:24 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyvisionSummary.java,v $
 * Revision 1.8  2009-10-07 01:00:24  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.7  2009-04-28 18:01:26  spr
 * Add graphs to time lines.
 *
 * Revision 1.6  2009-03-20 02:09:09  spr
 * Add memory view and related controls. Clean code.
 *
 * Revision 1.5  2008-12-04 19:13:22  spr
 * Remove extra print statements
 *
 * Revision 1.4  2008-12-04 19:12:09  spr
 * Add debugging
 *
 * Revision 1.3  2008-12-04 01:11:27  spr
 * Fix up time display.  Add termination.
 *
 * Revision 1.2  2008-11-24 23:38:15  spr
 * Move to dymaster.  Update views.
 *
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dyvision;

import edu.brown.cs.dyvise.dyvise.*;

import edu.brown.cs.ivy.swing.SwingText;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;




class DyvisionSummary extends JPanel implements DyvisionConstants {




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private DyvisionMain	for_main;
private DyvisionView	for_view;
private String		for_process;
private Map<String,SummaryData> value_map;
private boolean 	is_alive;
private double		cur_state;		// 0: normal, 1: highlight
private SummaryData	selected_value;
private int		selected_index;
private int		num_analysis;
private Shape		center_dot;
private Shape		analysis_dot;
private Animator	anim_timer;
private Stroke		simple_stroke;
private Stroke		graph_stroke;
private DyviseTimeManager time_manager;

private boolean 	show_detail;
private int		last_height;

private static RenderingHints rendering_hints;


private static final double CENTER_RADIUS = 0.25;
private static final double ANALYSIS_CENTER = (1.0-CENTER_RADIUS)/2.0 + CENTER_RADIUS;
private static final double ANALYSIS_RADIUS = 0.20;
private static final double SPACE_RADIUS = 0.10;

private static final int ANIMATE_DELAY = 30;
private static final double ANIMATE_LENGTH = 1000;

private static final double GRAPH_SIZE = 1.0;
private static final double GRAPH_SPACE = 0.25;
private static final double LABEL_HEIGHT = 0.15;
private static final double SUBLABEL_HEIGHT = 0.10;



static {
   rendering_hints = new RenderingHints(null);
   rendering_hints.put(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
   rendering_hints.put(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_DEFAULT);
}



private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DyvisionSummary(DyvisionMain dm,String pid,DyvisionView dv)
{
   for_main = dm;
   for_process = pid;
   for_view = dv;
   is_alive = true;
   cur_state = 0;
   selected_value = null;
   selected_index = -1;
   num_analysis = 0;
   center_dot = new Ellipse2D.Double(-CENTER_RADIUS,-CENTER_RADIUS,2*CENTER_RADIUS,2*CENTER_RADIUS);
   analysis_dot = null;
   simple_stroke = new BasicStroke(0.04f);			 // About a width of 2 in center area
   graph_stroke = new BasicStroke(0.02f);			 // About a width of 2 in graph area
   show_detail = false;
   last_height = 0;

   time_manager = DyviseTimeManager.getTimeManager(pid);

   value_map = new TreeMap<String,SummaryData>();

   anim_timer = new Animator();

   setPreferredSize(new Dimension(64,64));
   setMinimumSize(new Dimension(32,32));
   setToolTipText("Summary for " + for_process);
   addMouseListener(new Mouser());
   addComponentListener(new Resizer());
}



/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

void update(Element xml)
{
   if (xml == null) {
      is_alive = false;
    }
   else {
      is_alive = true;

      Element se = IvyXml.getElementByTag(xml,"SUMMARIES");
      for (Element e : IvyXml.elementsByTag(se,"SUMMARY")) {
	 String id = IvyXml.getAttrString(e,"NAME");
	 SummaryData sd = value_map.get(id);
	 if (sd == null) {
	    sd = new SummaryData(id);
	    value_map.put(id,sd);
	  }
	 sd.update(e);
       }
      if (num_analysis != value_map.size()) {
	 num_analysis = value_map.size();
	 double theta = Math.PI / num_analysis;
	 double sz = Math.tan(theta/2.0) * ANALYSIS_CENTER;
	 sz *= 0.90;
	 if (sz > ANALYSIS_RADIUS) sz = ANALYSIS_RADIUS;
	 analysis_dot = new Ellipse2D.Double(-sz,-sz,2*sz,2*sz);
       }
    }

   repaint();
}



/********************************************************************************/
/*										*/
/*	Painting methods							*/
/*										*/
/********************************************************************************/

@Override protected void paintComponent(Graphics g0)
{
   Dimension sz = getSize();

   Graphics2D g = (Graphics2D) g0.create();

   g.setRenderingHints(rendering_hints);
   g.setBackground(new Color(1f,1f,1f,1f));
   g.clearRect(0,0,sz.width,sz.height);
   g.setTransform(getTransform());

   if (!is_alive) {
      g.setStroke(simple_stroke);
      g.draw(center_dot);
    }
   else {
      g.setColor(Color.BLACK);
      g.fill(center_dot);
    }

   if (analysis_dot == null) return;

   int i = 0;
   for (SummaryData sd : value_map.values()) {
      double angle = computeAngle(i);
      double cx = Math.cos(angle) * ANALYSIS_CENTER;
      double cy = Math.sin(angle) * ANALYSIS_CENTER;
      g.translate(cx,cy);
      g.setColor(getColorValue(sd));
      g.fill(analysis_dot);
      g.translate(-cx,-cy);
      ++i;
    }

   if (cur_state >= 0.5 && selected_value != null) {
      drawLabels(g);
      for (int j = 0; j < selected_value.getNumGraphs(); ++j) {
	 // draw label for graph
	 g.setTransform(getGraphTransform(j));
	 g.setColor(Color.BLACK);
	 g.setStroke(graph_stroke);
	 selected_value.drawGraph(j,g);
       }
    }
}



private AffineTransform getTransform()
{
   Dimension sz = getSize();

   double c0 = 0.5;

   if (cur_state > 0 && selected_value != null) {
      double c2 = 1.0 + cur_state * (GRAPH_SPACE + selected_value.getNumGraphs() * GRAPH_SIZE);
      c0 = 0.5 / c2;
    }

   int cw = (int)(sz.getWidth() * c0 + 0.5);

   AffineTransform at;
   at = AffineTransform.getTranslateInstance(cw,sz.getHeight()/2.0);   // translate to center
   at.scale(cw,-sz.getHeight()/2.0);	  // -1 to +n, plus on top

   return at;
}




private Color getColorValue(SummaryData sd)
{
   double v = sd.getSummaryValue();
   double cv = sd.getConfidence();

   float h = (float)((1.0 - v) / 3.0);
   float s = (float)(cv * 0.8 + 0.2);

   int icol = Color.HSBtoRGB(h,s,1f);

   return new Color(icol);
}




private double computeAngle(int idx)
{
   double a0 = Math.PI / 2.0;
   double da = Math.PI * 2.0 / num_analysis;

   double a1 = a0 - da * idx;
   if (a1 < 0) a1 += Math.PI * 2;
   if (a1 > Math.PI * 2) a1 -= Math.PI * 2;

   if (cur_state != 0) {
      double at;
      double a3 = a0 - da * selected_index;
      if (a3 < 0) a3 += Math.PI * 2;
      if (a3 > Math.PI * 2) a3 -= Math.PI * 2;
      double dir = (a3 <= Math.PI ? -1 : 1);

      if (selected_index == idx) at = 0.0;		// selected on right
      else {
	 int d0 = (selected_index - idx);
	 if (d0 < 0) d0 += num_analysis;
	 double a2 = Math.PI / num_analysis;
	 at = Math.PI * 0.5 + a2 * d0;
       }

      if (dir < 0 && at > a1 && a3 < a1) dir = 1;
      if (dir > 0 && at < a1 && a3 > a1) dir = -1;

      if (dir > 0 && at < a1) at += Math.PI * 2;
      else if (dir < 0 && at > a1) a1 += Math.PI * 2;

      a1 = a1 + (at-a1)*cur_state;
    }

   if (a1 > Math.PI) a1 -= Math.PI * 2.0;

   return a1;
}


private AffineTransform getGraphTransform(int i)
{
   int n = 0;
   if (selected_value != null) n = selected_value.getNumGraphs();

   Dimension sz = getSize();

   double xsp = sz.getWidth() / (n+1+GRAPH_SPACE);
   double ysp = last_height;
   if (ysp == 0) {
      ysp = sz.getHeight();
      last_height = sz.height;
    }

   AffineTransform at = AffineTransform.getTranslateInstance(xsp*(i+1+GRAPH_SPACE),0);
   at.scale(xsp,ysp);

   return at;
}



private void drawLabels(Graphics2D g)
{
   if (selected_value == null) return;

   String lbl = selected_value.getName();

   g.setTransform(new AffineTransform());
   Dimension sz = getSize();
   double x0 = 1 + (GRAPH_SPACE + selected_value.getNumGraphs()) * cur_state;
   double x1 = 1 + GRAPH_SPACE * cur_state;
   double xstart = x1/x0*sz.getWidth();
   double xend = sz.getWidth();
   double ystart = 0;
   double yend = sz.getHeight() * LABEL_HEIGHT;

   g.setColor(Color.BLACK);
   SwingText.drawText(lbl,g,xstart,ystart,xend-xstart,yend-ystart);

   for (int i = 0; i < selected_value.getNumGraphs(); ++i) {
      lbl = selected_value.getGraphName(i);
      if (lbl == null) continue;
      x1 = 1 + (GRAPH_SPACE + i)*cur_state;
      xstart = x1/x0*sz.getWidth();
      double x2 = 1 + (GRAPH_SPACE + i + 1)*cur_state;
      xend = x2/x0*sz.getWidth();
      yend = sz.getHeight();
      ystart = yend - sz.getHeight() * SUBLABEL_HEIGHT;
      SwingText.drawText(lbl,g,xstart,ystart,xend-xstart,yend-ystart);
    }
}


/********************************************************************************/
/*										*/
/*	Point location methods							*/
/*										*/
/********************************************************************************/

private int locateItem(int x,int y)
{
   getSize();

   AffineTransform at = getTransform();
   Point2D p2 = new Point2D.Double(x,y);
   try {
      at.inverseTransform(p2,p2);
    }
   catch (NoninvertibleTransformException e) {
      return -2;
    }

   double d0 = Math.hypot(p2.getX(),p2.getY());
   if (d0 < CENTER_RADIUS) return -1;
   if (d0 > 1.0 - SPACE_RADIUS) return -3;
   if (d0 < CENTER_RADIUS + SPACE_RADIUS) return -2;

   double a1 = Math.atan2(p2.getY(),p2.getX());

   for (int i = 0; i < num_analysis; ++i) {
      double a = computeAngle(i);
      double a2 = a1 - a;

      double a3 = computeAngle((i+num_analysis-1) % num_analysis);
      double a4 = computeAngle((i+1) % num_analysis);
      double a5 = Math.abs(a-a3);
      double a6 = Math.abs(a-a4);
      if (a5 > a6) a5 = a6;
      double da = a5 / 2.0;

      if (a2 < Math.PI) a2 += Math.PI * 2;
      if (a2 > Math.PI) a2 -= Math.PI * 2;
      a2 = Math.abs(a2);
      if (a2 < da) return i;
    }

   return -2;
}



private int locateGraph(int x,int y)
{
   if (cur_state != 1 || selected_value == null) return -1;

   Dimension sz = getSize();

   double y1 = sz.getHeight() * LABEL_HEIGHT;
   if (y <= y1) return -1;

   double x1 = 1 + GRAPH_SPACE + GRAPH_SIZE * selected_value.getNumGraphs();
   double x2 = x * x1 / sz.getWidth();
   if (x2 < 1 + GRAPH_SPACE) return -1;
   x2 -= 1 + GRAPH_SPACE;

   return (int) x2;
}


private SummaryData getItem(int idx)
{
   int i = 0;
   for (Map.Entry<String,SummaryData> ent : value_map.entrySet()) {
      if (i == idx) return ent.getValue();
      ++i;
    }

   return null;
}



/********************************************************************************/
/*										*/
/*	Tool tip methods							*/
/*										*/
/********************************************************************************/

@Override public String getToolTipText(MouseEvent e)
{
   int idx = locateItem(e.getX(),e.getY());

   if (idx == -3) {
      int gidx = locateGraph(e.getX(),e.getY());
      if (gidx >= 0 && selected_value != null) {
	 AffineTransform at = getGraphTransform(gidx);
	 Point2D p2 = new Point2D.Double(e.getX(),e.getY());
	 try {
	    p2 = at.inverseTransform(p2,p2);
	    String s = selected_value.getToolTipText(gidx,p2);
	    if (s != null) return s;
	  }
	 catch (NoninvertibleTransformException _e) { }
       }
    }

   if (idx == -1) return "Request controls (left)  or time line (right) for " + for_process;
   if (idx < 0) return "Summary for " + for_process;

   SummaryData sd = getItem(idx);
   return "Status for " + for_process + " " + sd.getName() + " = " + sd.getSummaryValue();
}




/********************************************************************************/
/*										*/
/*	Mouse methods								*/
/*										*/
/********************************************************************************/

private void setSelection(int idx)
{
   selected_index = idx;
   selected_value = getItem(idx);
}




private void handleMouseClick(MouseEvent e)
{
   int idx = locateItem(e.getX(),e.getY());

   if (idx == -1) {
      if (e.getButton() == MouseEvent.BUTTON1) for_main.showControlPanel();
      else if (e.getButton() == MouseEvent.BUTTON3) for_main.showTimeLine();
      else if (e.getButton() == MouseEvent.BUTTON2) {
	 String what = JOptionPane.showInputDialog(this,"Enter Mark Type","");
	 time_manager.createUserMark(0,what);
       }
    }
   else if (idx == -2 || idx == -3) {
      if (cur_state == 1) anim_timer.startClose();
    }
   else if (cur_state == 0) {
      setSelection(idx);
      anim_timer.startOpen();
    }
   else if (cur_state == 1 && idx != selected_index) {
      anim_timer.switchStates(idx);
    }
   else if (cur_state == 1 && idx == selected_index) {
      if (show_detail) {
	 for_view.hideDetail();
	 show_detail = false;
       }
      else {
	 if (selected_value != null) for_view.showDetail(selected_value.getName());
	 show_detail = true;
       }
    }
}



private class Mouser extends MouseAdapter {

   public void mouseClicked(MouseEvent e) {
      handleMouseClick(e);
    }

}	// end of subclass Mouser




/********************************************************************************/
/*										*/
/*	Animation methods							*/
/*										*/
/********************************************************************************/

private void setWindowSize()
{
   Dimension sz = getSize();

   double w = 1.0 + cur_state * GRAPH_SPACE;

   if (selected_value != null) {
      w = 1.0 + cur_state * (GRAPH_SPACE + selected_value.getNumGraphs() * GRAPH_SIZE);
    }

   int nw = (int) (sz.height * w);

   sz.width = nw;

   // System.err.println("SET WIN SIZE: " + cur_state + " " + getSize() + " " + sz);

   setSize(sz);
   setPreferredSize(sz);

   for_view.updateSize();
   repaint();
}



private class Animator extends javax.swing.Timer implements ActionListener {

   private boolean do_open;
   private int next_state;

   private static final long serialVersionUID = 1;


   Animator() {
      super(ANIMATE_DELAY,null);
      do_open = true;
      next_state = -1;
      addActionListener(this);
    }

   void switchStates(int idx) {
      if (cur_state == 0) {
	 setSelection(idx);
	 startOpen();
       }
      else if (selected_index == idx) return;
      else if (cur_state == 1) {
	 next_state = idx;
	 startClose();
       }
    }

   void startOpen() {
      do_open = true;
      for_view.hideDetail();
      restart();
    }

   void startClose() {
      do_open = false;
      for_view.hideDetail();
      restart();
    }

   public void actionPerformed(ActionEvent e) {
      double delta = ANIMATE_DELAY / ANIMATE_LENGTH;
      if (!do_open) delta = -delta;
      cur_state += delta;
      if (cur_state >= 1) {
	 stop();
	 cur_state = 1.0;
	 if (show_detail && selected_value != null) for_view.showDetail(selected_value.getName());
       }
      else if (cur_state <= 0) {
	 cur_state = 0.0;
	 if (next_state >= 0) {
	    setSelection(next_state);
	    next_state = -1;
	    do_open = true;
	  }
	 else stop();
       }
      setWindowSize();
    }

}	// end of subclass Opener




/********************************************************************************/
/*										*/
/*	Summary information							*/
/*										*/
/********************************************************************************/

private static class SummaryData {

   private String item_name;
   private double summary_value;
   private double confidence_value;
   private Map<String,Graph> summary_graphs;

   SummaryData(String id) {
      item_name = id;
      summary_value = 0;
      confidence_value = 0;
      summary_graphs = new LinkedHashMap<String,Graph>();
    }

   void update(Element e) {
      summary_value = IvyXml.getAttrDouble(e,"VALUE",0);
      confidence_value = IvyXml.getAttrDouble(e,"CONFIDENCE",0);
      for (Element ce : IvyXml.elementsByTag(e,"METER")) {
	 String id = IvyXml.getAttrString(ce,"NAME");
	 Graph sg = summary_graphs.get(id);
	 if (sg == null) {
	    sg = new Meter(id,ce);
	    summary_graphs.put(id,sg);
	  }
	 sg.update(ce);
       }
      for (Element ce : IvyXml.elementsByTag(e,"BARGRAPH")) {
	 String id = IvyXml.getAttrString(ce,"NAME");
	 Graph sg = summary_graphs.get(id);
	 if (sg == null) {
	    sg = new BarGraph(id,ce);
	    summary_graphs.put(id,sg);
	  }
	 sg.update(ce);
       }
    }

   void drawGraph(int idx,Graphics2D g) {
      for (Graph gr : summary_graphs.values()) {
	 if (idx == 0) {
	    gr.draw(g);
	    break;
	  }
	 --idx;
       }
    }

   String getName()				{ return item_name; }
   double getSummaryValue()			{ return summary_value; }
   double getConfidence()			{ return confidence_value; }
   int getNumGraphs()				{ return summary_graphs.size(); }

   String getGraphName(int idx) {
      for (Graph gr : summary_graphs.values()) {
	 if (idx == 0) return gr.getName();
	 --idx;
       }
      return null;
    }

   String getToolTipText(int idx,Point2D p2) {
      for (Graph gr : summary_graphs.values()) {
	 if (idx == 0) return gr.getToolTipText(p2);
	 --idx;
       }
      return null;
    }


}	// end of subclass SummaryData




/********************************************************************************/
/*										*/
/*	Classes representing different summary graphs				*/
/*										*/
/********************************************************************************/

private static abstract class Graph {

   private String graph_id;
   private ValueType value_type;

   protected Graph(String nm,Element e) {
      graph_id = nm;
      value_type = IvyXml.getAttrEnum(e,"TYPE",ValueType.NUMBER);
    }

   String getName()			{ return graph_id; }

   abstract void update(Element e);

   abstract void draw(Graphics2D g);

   String getToolTipText(Point2D p) {
      return "Summary Data for " + getName();
    }

   String outputValue(double v) {
      return DyvisionFormat.outputValue(v,value_type);
    }

}	// end of subclass Graph



private static final double METER_INDENT = 0.15;
private static final double METER_SIZE = 1.0 - 2*METER_INDENT;
private static final double METER_PIVOT = 0.10;
private static final double METER_ARROW = 0.05;
private static final double METER_LINE = METER_SIZE - 0.02;

private static class Meter extends Graph {

   private double meter_value;
   private double min_value;
   private double max_value;
   private boolean first_time;

   Meter(String id,Element e) {
      super(id,e);
      meter_value = 0;
      min_value = 0;
      max_value = 0;
      first_time = true;
    }

   void update(Element e) {
      meter_value = IvyXml.getAttrDouble(e,"VALUE",0);
      if (first_time) {
	 min_value = meter_value;
	 max_value = meter_value;
	 first_time = false;
       }
      double mn = IvyXml.getAttrDouble(e,"MIN",min_value);
      if (mn < min_value) min_value = mn;
      double mx = IvyXml.getAttrDouble(e,"MAX",max_value);
      if (mx > max_value) max_value = mx;
      if (meter_value < min_value) min_value = meter_value;
      if (meter_value > max_value) max_value = meter_value;
    }

   void draw(Graphics2D g) {
      double hang = Math.asin(0.5);
      double maxangle = Math.toDegrees(hang);
      Arc2D a2 = new Arc2D.Double(Arc2D.OPEN);
      a2.setArcByCenter(0.5,1.0-METER_INDENT,METER_SIZE,90-maxangle,2*maxangle,Arc2D.OPEN);
      g.setColor(Color.BLACK);
      g.draw(a2);
      double a0 = 0.0;
      if (max_value != min_value) a0 = (meter_value - min_value) / (max_value - min_value);
      double a1 = Math.PI/2 + hang - 2*hang*a0;
      g.translate(0.5,1-METER_INDENT);
      g.rotate(-a1);
      g.setColor(Color.RED);
      Line2D l2 = new Line2D.Double(0,0,METER_LINE,0);
      g.draw(l2);
      Path2D p2 = new Path2D.Double();
      p2.moveTo(METER_LINE,0);
      p2.lineTo(METER_LINE-METER_ARROW,METER_ARROW);
      p2.lineTo(METER_LINE-METER_ARROW,-METER_ARROW);
      p2.closePath();
      g.fill(p2);
      g.setColor(Color.BLACK);
      Ellipse2D e2 = new Ellipse2D.Double(-METER_PIVOT/2,-METER_PIVOT/2,METER_PIVOT,METER_PIVOT);
      g.fill(e2);

    }

   String getToolTipText(Point2D p) {
      String s = "<html><center>Summary Data for " + getName();
      s += "<br>Cur Value = " + outputValue(meter_value);
      s += "<br>Max Value = " + outputValue(max_value);
      s += "<br>Min Value = " + outputValue(min_value);
      s += "</center></html>";

      return s;
    }

}	// end of subclass Meter



private static final double GRAPH_INDENT = 0.15;
private static final double GRAPH_HEIGHT = 1 - 2*GRAPH_INDENT;

private static class BarGraph extends Graph {

   private Map<String,Double> value_map;
   private Map<Double,String> output_map;
   private double min_value;
   private double max_value;
   private double total_value;

   BarGraph(String id,Element e) {
      super(id,e);
      value_map = new HashMap<String,Double>();
      min_value = max_value = total_value = 0;
      output_map = null;
    }

   void update(Element e) {
      min_value = IvyXml.getAttrDouble(e,"MIN",0);
      max_value = IvyXml.getAttrDouble(e,"MAX",0);
      total_value = IvyXml.getAttrDouble(e,"TOTAL",total_value);
      for (Element ie : IvyXml.elementsByTag(e,"ITEM")) {
	 String id = IvyXml.getAttrString(ie,"NAME");
	 double v = IvyXml.getAttrDouble(ie,"VALUE",0);
	 if (v != 0) {
	    if (value_map.isEmpty()) {
	       if (min_value == 0) min_value = v;
	       if (max_value == 0) max_value = v;
	     }
	    value_map.put(id,v);
	    if (v < min_value) min_value = v;
	    if (v > max_value) max_value = v;
	  }
       }
    }

   void draw(Graphics2D g) {
      if (value_map.size() == 0) return;
      output_map = new TreeMap<Double,String>(new GraphOrder());
      for (Map.Entry<String,Double> ent : value_map.entrySet()) {
	 output_map.put(ent.getValue(),ent.getKey());
       }
      int n = output_map.size();
      double isz = (1.0 - 2 * GRAPH_INDENT)/n;
      Rectangle2D r2 = new Rectangle2D.Double();
      double x0 = GRAPH_INDENT;
      double tot = total_value;
      if (tot < max_value) tot = max_value;
      if (tot == 0) return;
      for (Map.Entry<Double,String> ent : output_map.entrySet()) {
	 double y0 = (ent.getKey() - min_value) / (max_value - min_value) * GRAPH_HEIGHT;
	 double y1 = 1 - GRAPH_INDENT - y0;
	 r2.setRect(x0,y1,isz,y0);
	 float y2 = (float)(ent.getKey() / tot);
	 if (y2 < 0) y2 = 0;
	 if (y2 > 1) y2 = 1;
	 Color c = new Color(1-y2,1-y2,y2);
	 g.setColor(c);
	 g.fill(r2);
	 x0 += isz;
       }
    }

   @Override String getToolTipText(Point2D p) {
      if (output_map == null) return super.getToolTipText(p);
      int n = output_map.size();
      double isz = (1.0 - 2*GRAPH_INDENT)/n;
      double x = p.getX() - GRAPH_INDENT;
      int i = (int) (x / isz);
      if (x > 0 && i < n) {
	 for (Map.Entry<Double,String> ent : output_map.entrySet()) {
	    if (i == 0) {
	       String s = "<html><center>Summary Data for " + getName();
	       s += "<br>" + ent.getValue() + " = " + outputValue(ent.getKey());
	       s += "</center></html>";
	       return s;
	     }
	    --i;
	  }
       }
      return super.getToolTipText(p);
    }

}	// end of subclass BarGraph




private static class GraphOrder implements Comparator<Double> {

   public int compare(Double t0,Double t1) {
      return t1.compareTo(t0);
    }

}



/********************************************************************************/
/*										*/
/*	Class for handling resizing						*/
/*										*/
/********************************************************************************/

private class Resizer extends ComponentAdapter {

   public void componentResized(ComponentEvent e) {
      JPanel jp = (JPanel) e.getComponent();
      Dimension d = jp.getSize();
      if (cur_state == 0 || cur_state == 1) {
	 last_height = d.height;
	 if (d.height < 32) d.height = 32;
	 double w = 1.0;
	 if (selected_value != null) {
	    w = 1.0 + cur_state * (GRAPH_SPACE + selected_value.getNumGraphs() * GRAPH_SIZE);
	  }
	 int dw = (int)(w * d.height);
	 d.width = dw;
	 jp.setPreferredSize(d);
       }
    }

}	// end of subclass Resizer



}	// end of class DyvisionSummary




/* end of DyvisionSummary.java */
