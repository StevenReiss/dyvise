/********************************************************************************/
/*										*/
/*		DymemViewCommon.java						*/
/*										*/
/*	Common abstract class for DYMEM viewers 				*/
/*										*/
/********************************************************************************/
/*	Copyright 2005 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2005, Brown University, Providence, RI.				 *
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymem/DymemViewCommon.java,v 1.5 2010-03-30 16:21:56 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymemViewCommon.java,v $
 * Revision 1.5  2010-03-30 16:21:56  spr
 * Bug fixes and feature enhancements.  Start of interface for cycle elimination.
 *
 * Revision 1.4  2009-10-07 01:00:02  spr
 * Code cleanup for eclipse. Add support for standalone version.
 *
 * Revision 1.3  2009-06-04 18:53:35  spr
 * Clean up memory display.
 *
 * Revision 1.2  2009-04-28 18:00:57  spr
 * Update visualization with data panel.
 *
 * Revision 1.1  2009-04-11 23:45:29  spr
 * Provide updated visualizations with time line, hashing, etc.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymem;


import edu.brown.cs.dyvise.dyvise.*;

import edu.brown.cs.ivy.file.IvyFormat;

import org.w3c.dom.Element;

import javax.swing.JFrame;
import javax.swing.JPanel;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;



public abstract class DymemViewCommon extends JPanel implements DymemConstants,
	DymemConstants.ParameterListener, DymemConstants.HeapListener,
	DyviseConstants.TimeListener
{




/********************************************************************************/
/*										*/
/*	Local Storage								*/
/*										*/
/********************************************************************************/

protected String		process_id;
protected DymemParameters	param_values;
protected DymemHistory		file_history;
private DymemTimeLine		time_line;
private boolean 		is_vertical;
private DyviseTimeManager	time_manager;

private List<ViewListener>	view_listeners;

protected DymemGraph		current_graph;
protected GraphNode		current_root;
protected long			current_time;

protected HistoryNode		current_history;

private final static Stroke HISTORY_STROKE = new BasicStroke(0f);
private final static Stroke POINTER_STROKE = new BasicStroke(2f);

private final static double	GRAPH_SPACE = 0.050;

private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public DymemViewCommon(String pid) throws DymemException
{
   process_id = pid;

   view_listeners = new ArrayList<ViewListener>();

   param_values = new DymemParameters(pid);
   param_values.addParameterListener(this);

   time_manager = DyviseTimeManager.getTimeManager(pid);
   time_manager.addTimeListener(this);

   is_vertical = param_values.getShowVertical();

   file_history = new DymemHistory(pid,param_values,this);
   time_line = new DymemTimeLine(pid,param_values);

   current_graph = null;
   current_root = null;
   current_history = new HistoryNode();
   current_time = time_manager.getCurrentTime();

   addMouseListener(new Mouser());
}




/********************************************************************************/
/*										*/
/*	Callback methods							*/
/*										*/
/********************************************************************************/

public void addViewListener(ViewListener v)
{
   view_listeners.add(v);
}


public Orientation getOrientation()
{
   return (is_vertical ? Orientation.VERTICAL : Orientation.HORIZONTAL);
}



/********************************************************************************/
/*										*/
/*	Time line methods							*/
/*										*/
/********************************************************************************/

public DymemTimeLine getTimeLine(TimeLineDirection dir)
{
   time_line.setVertical(dir);

   return time_line;
}



/********************************************************************************/
/*										*/
/*	Methods to read heap file as it is updated				*/
/*										*/
/********************************************************************************/

public void heapUpdated(long when)
{
   if (current_time == 0 || current_graph == null) setCurrentEntry();
}


public void heapDumpTime(long when)
{
   time_line.noteDump(when);
}



public void memoryUsageUpdate(Element e)
{
   time_line.updateUsage(e);
}




/********************************************************************************/
/*										*/
/*	Methods to maintain current entry					*/
/*										*/
/********************************************************************************/

public void handleMark(long when,String what)			{ }

public void handleTimeSet(long when)
{
   current_time = when;
   setCurrentEntry();
}


private void setCurrentEntry()
{
   DymemGraph ng = file_history.getCurrentGraph(current_time);
   if (ng == current_graph) return;

   current_graph = ng;
   updateCurrentGraph();

   /***************
   if (current_graph == null)
      System.err.println("DYMEM: Set current entry to null");
   else
      System.err.println("DYMEM: Set current entry " + current_graph.getAtTime());
   **************/

   repaint();
}



protected void updateCurrentGraph()			{ }



/********************************************************************************/
/*										*/
/*	Methods for getting/setting current root				*/
/*										*/
/********************************************************************************/

private synchronized void setCurrentNode(GraphNode gn)
{
   if (gn == current_history.getCurrentNode()) return;

   if (current_root == null) return;
   if (gn == null) {
      gn = current_history.getRootNode();
      if (gn == current_history.getCurrentNode()) return;
    }

   current_history = current_history.next();
   current_history.set(gn);

   repaint();
}



public synchronized boolean canGoBack()
{
   return current_history.getPrior() != null;
}


public synchronized boolean canGoForward()
{
   return current_history.getNext() != null;
}


public synchronized void goBack()
{
   if (canGoBack()) {
      current_history = current_history.getPrior();
      repaint();
    }
}



public synchronized void goForward()
{
   if (canGoForward()) {
      current_history = current_history.getNext();
    }
}



public synchronized void goHome()
{
   setCurrentNode(null);
}


public synchronized void goParent()
{
   GraphNode gn = current_history.getCurrentNode();
   if (gn == null) return;
   GraphNode pgn = null;

   for (GraphLink gl : gn.getSortedInLinks(OutputCompareBy.TOTAL_SIZE)) {
      pgn = gl.getFromNode();
    }

   if (pgn != null) setCurrentNode(pgn);
}




/********************************************************************************/
/*										*/
/*	Cycle management							*/
/*										*/
/********************************************************************************/

void manageCycle()
{
   if (current_root == null || !current_root.isCycle()) return;

   String nm = current_root.getName();

   current_graph.manageCycle(nm);
}



/********************************************************************************/
/*										*/
/*	Interaction handling							*/
/*										*/
/********************************************************************************/

private void handleMouseClick(MouseEvent e)
{
   if (e.getButton() == MouseEvent.BUTTON1) {
      FindResult fr = locateItem(e.getX(),e.getY());
      if (fr == null) setCurrentNode(null);
      else setCurrentNode(fr.getGraphNode());
    }
   else if (e.getButton() == MouseEvent.BUTTON2) {
      FindResult fr = locateItem(e.getX(),e.getY());
      if (fr == null) param_values.showParameters();
      else showNodeDialog(current_graph,fr.getGraphNode());
    }
   else if (e.getButton() == MouseEvent.BUTTON3) {
      FindResult fr = locateItem(e.getX(),e.getY());
      if (fr == null) return;
      if (fr.getGraphNode().isCycle()) {
	 // handle cycle display and editing
       }
      else {
	 // handle node display and editing
       }
    }
}



void showParameters()
{
   param_values.showParameters();
}



private class Mouser extends MouseAdapter {

   public void mouseClicked(MouseEvent e) {
      handleMouseClick(e);
    }

}	// end of subclass Mouser




/********************************************************************************/
/*										*/
/*	Item location methods							*/
/*										*/
/********************************************************************************/

protected abstract FindResult locateItem(double x0,double y0);


protected static class FindResult {

   private GraphNode found_node;
   private double found_scale;

   FindResult(GraphNode gn,double scl) {
      found_node = gn;
      found_scale = scl;
    }

   GraphNode getGraphNode()			{ return found_node; }
   double getScale()				{ return found_scale; }

}	// end of subclass FindResult




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

static String getNodeName(GraphNode gn)
{
   if (gn == null) return "*NONE*";

   StringBuilder buf = new StringBuilder();

   String nm = gn.getName();
   if (!gn.isCycle()) nm = getTypeName(nm,"\u2190");

   buf.append(nm);
   if (gn.isSelfCycle()) buf.append(" {R}");

   if (gn.getCycleName() != null) {
      buf.append(" " + gn.getCycleName());
    }
   buf.append(" #" + gn.getIndex());

   return buf.toString();
}



/********************************************************************************/
/*										*/
/*	Name formating methods							*/
/*										*/
/********************************************************************************/

static String getTypeName(String jty,String sep)
{
   if (jty.startsWith("*")) return jty;
   if (jty.startsWith("CLASS*")) {
      String cnm = getTypeName(jty.substring(6),sep);
      return "CLASS " + cnm;
    }
   if (jty.startsWith("THREAD*")) {
      String cnm = getTypeName(jty.substring(7),sep);
      return "THREAD " + cnm;
    }
   if (jty.startsWith("CYCLE")) return jty;

   int idx = jty.indexOf(":");
   if (idx > 0 && sep != null) {
      String pfx = jty.substring(0,idx);
      pfx = getTypeName(pfx,null);
      idx = jty.lastIndexOf(":");
      String sfx = jty.substring(idx+1);
      sfx = getTypeName(sfx,null);
      return pfx + sep + sfx;
    }
   else if (idx > 0) {
      String pfx = jty.substring(0,idx);
      pfx = getTypeName(pfx,null);
      jty = jty.substring(idx+1);
      return pfx + ":" + getTypeName(jty,sep);
    }

   return IvyFormat.formatTypeName(jty);
}




/********************************************************************************/
/*										*/
/*	Methods to generate information about a node				*/
/*										*/
/********************************************************************************/

static String getNodeInformation(GraphNode gn,GraphNode pn,double totalmem,double scale)
{
   double delta;

   StringBuffer buf = new StringBuffer();
   buf.append("<table frame='all' border='1' >");
   // buf.append("<tr><td>" + IvyFormat.formatPercent(scale) + "%</td>");
   buf.append("<tr><td></td>");
   buf.append("<th>SIZE</th><th>COUNT</th><th>NEW</th></tr>");

   buf.append("<tr><th>TOTAL</th>");
   buf.append("<td><b>" + IvyFormat.formatMemory(gn.getTotalSize(),totalmem) + "</b></td>");
   buf.append("<td><b>" + IvyFormat.formatCount(gn.getTotalCount()) + "</b></td>");
   buf.append("<td><b>" + IvyFormat.formatCount(gn.getTotalNewCount()) + "</b></td>");
   buf.append("</tr>");

   if (pn != null) {
      buf.append("<tr><th>&Delta;TOTAL</th>");
      delta = gn.getTotalSize() - pn.getTotalSize();
      buf.append("<td>" + IvyFormat.formatMemory(delta,gn.getTotalSize()) + "</td>");
      delta = gn.getTotalCount() - pn.getTotalCount();
      buf.append("<td>" + IvyFormat.formatCount(delta) + "</td>");
      delta = gn.getTotalNewCount() - pn.getTotalNewCount();
      buf.append("<td>" + IvyFormat.formatCount(delta) + "</td>");
      buf.append("</tr>");
    }

   buf.append("<tr><th>LOCAL</th>");
   buf.append("<td><b>" + IvyFormat.formatMemory(gn.getLocalSize(),totalmem) + "</b></td>");
   buf.append("<td><b>" + IvyFormat.formatCount(gn.getLocalCount()) + "</b></td>");
   buf.append("<td><b>" + IvyFormat.formatCount(gn.getLocalNewCount()) + "</b></td>");
   buf.append("</tr>");

   if (pn != null) {
      buf.append("<tr><th>&Delta;LOCAL</th>");
      delta = gn.getLocalSize() - pn.getLocalSize();
      buf.append("<td>" + IvyFormat.formatMemory(delta,gn.getLocalSize()) + "</td>");
      delta = gn.getLocalCount() - pn.getLocalCount();
      buf.append("<td>" + IvyFormat.formatCount(delta) + "</td>");
      delta = gn.getLocalNewCount() - pn.getLocalNewCount();
      buf.append("<td>" + IvyFormat.formatCount(delta) + "</td>");
      buf.append("</tr>");
    }

   if (scale > 0) {
      buf.append("<tr><th>FRACTION</th>");
      buf.append("<td>" + IvyFormat.formatPercent(scale) + "%</td>");
      buf.append("</tr>");
    }

   buf.append("</table>");

   return buf.toString();
}




/********************************************************************************/
/*										*/
/*	Methods to graph information for a node 				*/
/*										*/
/********************************************************************************/

static void graphLine(DymemGraph curgraph,Graphics2D g,Rectangle2D r,double [] data,
			 Color c,boolean vert)
{
   double max = 0;
   for (int i = 0; i < data.length; ++i) {
      if (max < data[i]) max = data[i];
    }
   double delta = GRAPH_SPACE;
   max *= 1.0 + 2*delta;			// save some space at the top

   DymemStats stats = curgraph.getStatistics();
   long [] ivls = stats.getIntervals();
   long ivl0 = ivls[0];
   double divl = ivls[ivls.length-1] - ivl0;

   Point2D.Double p0 = new Point2D.Double();
   Point2D.Double p1 = new Point2D.Double();
   Line2D.Double l2 = new Line2D.Double();

   long time = curgraph.getAtTime();
   double td = divl / ivls.length / 2;
   double t0 = ((time - td) - ivl0) / divl;
   double t1 = ((time + td) - ivl0) / divl;
   double t2 = (time - ivl0) / divl;
   if (t0 < 0) t0 = 0;
   if (t1 > 1) t1 = 1;
   if (t2 > 1) t2 = 1;

   g.setStroke(HISTORY_STROKE);
   g.setColor(c);
   for (int i = 0; i < data.length; ++i) {
      double v0 = data[i]/max + delta;
      double v1 = (ivls[i] - ivl0)/divl;
      if (vert) {
	 p1.setLocation(r.getX()+v1*r.getWidth(),r.getY()+(1-v0)*r.getHeight());
       }
      else {
	 p1.setLocation(r.getX() + (1-v0)*r.getWidth(),r.getY()+(1-v1)*r.getHeight());
       }
      if (i != 0) {
	 l2.setLine(p0,p1);
	 g.draw(l2);
       }
      p0.setLocation(p1);
    }
}



static void graphIndicator(DymemGraph curgraph,Graphics2D g,Rectangle2D r,Color c1,boolean vert)
{
   DymemStats stats = curgraph.getStatistics();
   long [] ivls = stats.getIntervals();
   long ivl0 = ivls[0];
   double divl = ivls[ivls.length-1] - ivl0;

   Line2D.Double l2 = new Line2D.Double();

   long time = curgraph.getAtTime();
   double td = divl / ivls.length / 2;
   double t0 = ((time - td) - ivl0) / divl;
   double t1 = ((time + td) - ivl0) / divl;
   double t2 = (time - ivl0) / divl;
   if (t0 < 0) t0 = 0;
   if (t1 > 1) t1 = 1;
   if (t2 > 1) t2 = 1;

   // first draw indicator
   g.setStroke(POINTER_STROKE);
   g.setColor(c1);
   if (vert) {
      double x0 = r.getX() + t0 * r.getWidth();
      double x1 = r.getX() + t1 * r.getWidth();
      double x2 = r.getX() + t2 * r.getWidth();
      double y0 = r.getY();
      double y1 = r.getY() + 0.33 * r.getHeight();
      double y2 = r.getY() + r.getHeight();
      l2.setLine(x0,y0,x0,y1);
      g.draw(l2);
      l2.setLine(x1,y0,x1,y1);
      g.draw(l2);
      l2.setLine(x2,y0,x2,y2);
      g.draw(l2);
      l2.setLine(x0,y1,x1,y1);
      g.draw(l2);
    }
   else {
      double y0 = r.getY() + (1-t0) * r.getHeight();
      double y1 = r.getY() + (1-t1) * r.getHeight();
      double y2 = r.getY() + (1-t2) * r.getHeight();
      double x0 = r.getX();
      double x1 = r.getX() + 0.33 * r.getWidth();
      double x2 = r.getX() + r.getWidth();
      l2.setLine(x0,y0,x1,y0);
      g.draw(l2);
      l2.setLine(x0,y1,x1,y1);
      g.draw(l2);
      l2.setLine(x0,y2,x2,y2);
      g.draw(l2);
      l2.setLine(x1,y0,x1,y1);
      g.draw(l2);
    }
}



/********************************************************************************/
/*										*/
/*	Methods to handle parameter changes					*/
/*										*/
/********************************************************************************/

public void valuesChanged()
{
   if (current_graph != null) current_graph.clear();

   if (param_values.getShowVertical() != is_vertical) {
      is_vertical = param_values.getShowVertical();
      for (ViewListener vl : view_listeners) {
	 vl.changeOrientation((is_vertical ? Orientation.VERTICAL : Orientation.HORIZONTAL));
       }
    }

   repaint();
}



/********************************************************************************/
/*										*/
/*	Methods to handle information dialogs					*/
/*										*/
/********************************************************************************/

protected void showNodeDialog(DymemGraph g,GraphNode n)
{
   showNodeDialog(g,n,null);
}



protected void showNodeDialog(DymemGraph g,GraphNode n,GraphNode p)
{
   DymemDataPanel pnl = new DymemDataPanel(g,n,p);

   JFrame frm = new JFrame(getNodeName(n));
   frm.setContentPane(pnl);
   frm.pack();
   frm.setVisible(true);
}




/********************************************************************************/
/*										*/
/*	History methods 							*/
/*										*/
/********************************************************************************/

protected static class HistoryNode {

   private GraphNode root_node;
   private GraphNode current_node;
   private String current_name;
   private HistoryNode prior_node;
   private HistoryNode next_node;

   HistoryNode() {
      root_node = null;
      current_node = null;
      current_name = ROOT_NAME;
      prior_node = null;
      next_node = null;
    }

   HistoryNode next() {
      HistoryNode next = new HistoryNode();
      next.root_node = root_node;
      next.current_node = current_node;
      next.current_name = current_name;
      next.prior_node = this;
      next.next_node = null;
      next_node = next;
      return next;
    }

   void update(GraphNode root,GraphNode gn) {
      root_node = root;
      current_node = gn;
    }

   void set(GraphNode gn) {
      current_node = gn;
      if (gn == null) current_name = ROOT_NAME;
      else current_name = gn.getName();
      next_node = null;
    }

   String getCurrentName()		{ return current_name; }
   GraphNode getCurrentNode()		{ return current_node; }
   GraphNode getRootNode()		{ return root_node; }
   HistoryNode getPrior()		{ return prior_node; }
   HistoryNode getNext()		{ return next_node; }

}	// end of subclass HistoryNode




}	// end of class DymemViewCommon




/* end of DymemViewCommon.java */

