/********************************************************************************/
/*										*/
/*		DymemViewDelta.java						*/
/*										*/
/*	JPanel for viewing memory usage showing changes 			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymem/DymemViewDelta.java,v 1.5 2009-10-07 01:00:02 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymemViewDelta.java,v $
 * Revision 1.5  2009-10-07 01:00:02  spr
 * Code cleanup for eclipse. Add support for standalone version.
 *
 * Revision 1.4  2009-06-04 18:53:35  spr
 * Clean up memory display.
 *
 * Revision 1.3  2009-05-01 23:15:00  spr
 * Fix up data panel graphs, clean up unnecessary code.
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

import edu.brown.cs.ivy.swing.SwingSetup;

import javax.swing.JFrame;
import javax.swing.ToolTipManager;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;



public class DymemViewDelta extends DymemViewSimple implements DymemConstants,
	DymemConstants.ParameterListener, DymemConstants.HeapListener
{




/********************************************************************************/
/*										*/
/*	Local Storage								*/
/*										*/
/********************************************************************************/


private DymemGraph	previous_graph;
private Map<String,GraphNode> previous_map;

private final static double SPACE = 5;
private final static Color LIGHT_BLUE = new Color(128,128,255);

private final static int MIN_HISTORY_LENGTH = 3;

private final static Stroke HASH_STROKE = new BasicStroke(0f);



private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public DymemViewDelta(String pid) throws DymemException
{
   super(pid,false);

   previous_graph = null;
   previous_map = new HashMap<String,GraphNode>();

   file_history.start();
}




/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

protected void updateCurrentGraph()
{
   previous_graph = file_history.getPreviousGraph(current_time);
   previous_map.clear();
   if (previous_graph != null) {
      for (GraphNode gn : previous_graph.getNodeList(OutputCompareBy.TOTAL_SIZE)) {
	 previous_map.put(gn.getName(),gn);
       }
    }
}




/********************************************************************************/
/*										*/
/*	Painting methods							*/
/*										*/
/********************************************************************************/

protected void showInformation(Graphics2D g,GraphNode n,Rectangle2D r)
{
   GraphNode pn = previous_map.get(n.getName());
   boolean vert = param_values.getShowVertical();

   if (pn != null) {
      g.setStroke(HASH_STROKE);
      if (vert) {
	 hashLeft(g,r,getPercent(n,pn,param_values.getShowUp()),Color.WHITE);
	 hashRight(g,r,getPercent(n,pn,param_values.getShowDown()),LIGHT_BLUE);
       }
      else {
	 hashBottom(g,r,getPercent(n,pn,param_values.getShowUp()),Color.WHITE);
	 hashTop(g,r,getPercent(n,pn,param_values.getShowDown()),LIGHT_BLUE);
       }
    }

   DymemStats stats = current_graph.getStatistics();
   double [] hvals = stats.getValues(n.getName(),param_values.getShowHistory());
   if (hvals != null && hvals.length >= MIN_HISTORY_LENGTH) {
      if (param_values.getShowAllHistory() || n == current_root) {
	 graphIndicator(current_graph,g,r,Color.WHITE,vert);
	 graphLine(current_graph,g,r,hvals,Color.BLUE,vert);
       }
    }
}



private double getPercent(GraphNode n,GraphNode pn,OutputCompareBy cb)
{
   if (cb == OutputCompareBy.NONE) return 0;

   double pct = n.getValue(cb);
   if (pct == 0) return 0;

   switch (cb) {
      case LOCAL_NEW :
	 pct = pct / n.getLocalCount();
	 break;
      case TOTAL_NEW :
	 pct = pct / n.getTotalCount();
	 break;
      default :
	 pct = (pct - pn.getValue(cb)) / pct;
	 break;
    }

   return pct;
}





/********************************************************************************/
/*										*/
/*	Methods for displaying hashed grid from various sides			*/
/*										*/
/********************************************************************************/

private void hashBottom(Graphics2D g,Rectangle2D r,double pct,Color c)
{
   if (pct <= 0) return;

   Rectangle2D r1 = new Rectangle2D.Double();
   r1.setRect(r);
   if (pct < 1) {
      double h0 = pct * r.getHeight();
      double y0 = r.getY() + r.getHeight() - h0;
      r1.setRect(r.getX(),y0,r.getWidth(),h0);
    }

   g.setColor(c);

   Rectangle r2 = g.getClipBounds();
   g.setClip(r1);
   Line2D l = new Line2D.Double();
   double x0 = r.getX();
   double x1 = r.getX() + r.getWidth();

   for (double y0 = r1.getY() + r1.getHeight() + r1.getWidth(); y0 > r1.getY(); y0 -= SPACE) {
      l.setLine(x1,y0,x0,y0-r1.getWidth());
      g.draw(l);
    }

   g.setClip(r2);
}



private void hashTop(Graphics2D g,Rectangle2D r,double pct,Color c)
{
   if (pct <= 0) return;

   Rectangle2D r1 = new Rectangle2D.Double();
   r1.setRect(r);
   if (pct < 1) {
      double h0 = pct * r.getHeight();
      r1.setRect(r.getX(),r.getY(),r.getWidth(),h0);
    }

   g.setColor(c);

   Rectangle r2 = g.getClipBounds();
   g.setClip(r1);
   Line2D l = new Line2D.Double();
   double x0 = r.getX();
   double x1 = r.getX() + r.getWidth();

   for (double y0 = r1.getY(); y0 < r1.getY() + r1.getHeight() + r1.getWidth(); y0 += SPACE) {
      l.setLine(x0,y0,x1,y0-r1.getWidth());
      g.draw(l);
    }

   g.setClip(r2);
}



private void hashLeft(Graphics2D g,Rectangle2D r,double pct,Color c)
{
   if (pct <= 0) return;

   Rectangle2D r1 = new Rectangle2D.Double();
   r1.setRect(r);
   if (pct < 1) {
      double w0 = pct * r.getWidth();
      r1.setRect(r.getX(),r.getY(),w0,r.getHeight());
    }

   g.setColor(c);

   Rectangle r2 = g.getClipBounds();
   g.setClip(r1);
   Line2D l = new Line2D.Double();
   double y0 = r.getY();
   double y1 = r.getY() + r.getHeight();

   for (double x0 = r1.getX(); x0 < r1.getX() + r1.getWidth() + r1.getHeight(); x0 += SPACE) {
      l.setLine(x0,y0,x0-r1.getHeight(),y1);
      g.draw(l);
    }

   g.setClip(r2);
}



private void hashRight(Graphics2D g,Rectangle2D r,double pct,Color c)
{
   if (pct <= 0) return;

   Rectangle2D r1 = new Rectangle2D.Double();
   r1.setRect(r);
   if (pct < 1) {
      double w0 = pct * r.getWidth();
      double x0 = r.getX() + r.getWidth() - w0;
      r1.setRect(x0,r.getY(),w0,r.getHeight());
    }

   g.setColor(c);

   Rectangle r2 = g.getClipBounds();
   g.setClip(r1);
   Line2D l = new Line2D.Double();
   double y0 = r.getY();
   double y1 = r.getY() + r.getHeight();

   for (double x0 = r1.getX() + r1.getWidth(); x0 > r1.getX() - r1.getHeight(); x0 -= SPACE) {
      l.setLine(x0,y0,x0+r1.getHeight(),y1);
      g.draw(l);
    }

   g.setClip(r2);
}



/********************************************************************************/
/*										*/
/*	View dialog methods							*/
/*										*/
/********************************************************************************/

@Override protected void showNodeDialog(DymemGraph g,GraphNode n)
{
   GraphNode pn = previous_map.get(n.getName());

   showNodeDialog(g,n,pn);
}



/********************************************************************************/
/*										*/
/*	Tool tip methods							*/
/*										*/
/********************************************************************************/

@Override public String getToolTipText(MouseEvent e)
{
   FindResult fr = locateItem(e.getX(),e.getY());

   if (fr == null) return "Memory map for " + process_id;

   return getVerticalText(fr);
}



private String getVerticalText(FindResult fr)
{
   GraphNode gn = fr.getGraphNode();
   double scale = fr.getScale();
   GraphNode pn = previous_map.get(gn.getName());

   StringBuffer buf = new StringBuffer();
   buf.append("<html>");
   buf.append("<center><b>");
   buf.append(getNodeName(gn));
   buf.append("</b></center>");
   buf.append("<br>");
   buf.append(getNodeInformation(gn,pn,full_size,scale));;

   return buf.toString();
}




/********************************************************************************/
/*										*/
/*	Test program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   DymemViewDelta dv;

   new SwingSetup();

   try {
      dv = new DymemViewDelta(args[0]);
    }
   catch (DymemException e) {
      System.err.println("DYMEM: Problem starting viewer: " + e);
      return;
    }

   if (args.length > 1) {
      long time = Long.parseLong(args[1]);
      DyviseTimeManager.getTimeManager(args[0]).setCurrentTime(time);
    }

   ToolTipManager ttm = ToolTipManager.sharedInstance();
   ttm.setDismissDelay(60*60*1000);
   ttm.setLightWeightPopupEnabled(false);

   JFrame jf = new JFrame();
   jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
   jf.setContentPane(dv);
   jf.setTitle("Memory display for " + args[0]);
   jf.pack();
   jf.setVisible(true);
}



}	// end of class DymemViewDelta




/* end of DymemViewDelta.java */
