/********************************************************************************/
/*										*/
/*		DymemViewSimple.java						*/
/*										*/
/*	JPanel for viewing memory usage 					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymem/DymemViewSimple.java,v 1.3 2009-10-07 01:00:02 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymemViewSimple.java,v $
 * Revision 1.3  2009-10-07 01:00:02  spr
 * Code cleanup for eclipse. Add support for standalone version.
 *
 * Revision 1.2  2009-06-04 18:53:35  spr
 * Clean up memory display.
 *
 * Revision 1.1  2009-04-11 23:45:29  spr
 * Provide updated visualizations with time line, hashing, etc.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymem;


import edu.brown.cs.dyvise.dyvise.*;

import edu.brown.cs.ivy.file.IvyFormat;
import edu.brown.cs.ivy.swing.SwingText;

import javax.swing.JFrame;
import javax.swing.ToolTipManager;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.List;



public class DymemViewSimple extends DymemViewCommon implements DymemConstants,
	DymemConstants.ParameterListener, DymemConstants.HeapListener
{




/********************************************************************************/
/*										*/
/*	Local Storage								*/
/*										*/
/********************************************************************************/


protected double	scale_x;
protected double	scale_y;

protected double	total_size;
protected double	full_size;
protected double	max_local;
protected double	max_levels;
protected int		max_drawn;

private static final Stroke OUTLINE_STROKE = new BasicStroke(0f);

private static final int MIN_LEVELS = 10;

private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public DymemViewSimple(String pid) throws DymemException
{
   this(pid,true);
}




protected DymemViewSimple(String pid,boolean fg) throws DymemException
{
   super(pid);

   max_levels = 20;
   scale_x = 1;
   scale_y = 1;

   setPreferredSize(new Dimension(400,400));
   setToolTipText("Memory visualizer for " + process_id);

   if (fg) file_history.start();
}





/********************************************************************************/
/*										*/
/*	Painting methods							*/
/*										*/
/********************************************************************************/

protected void paintComponent(Graphics g0)
{
   Graphics2D g = (Graphics2D) g0.create();

   String rootname = ROOT_NAME;
   if (current_history != null) rootname = current_history.getCurrentName();
   current_root = null;

   Dimension d0 = getSize();
   g.clearRect(0,0,d0.width,d0.height);

   if (current_graph == null) return;

   List<GraphNode> nodes = current_graph.getNodeList(OutputCompareBy.TOTAL_SIZE);

   GraphNode droot = null;
   for (GraphNode gn : nodes) {
      if (gn.getName().equals(rootname)) {
	 current_root = gn;
	 if (droot != null) break;
       }
      if (gn.getName().equals(ROOT_NAME)) {
	 droot = gn;
	 if (current_root != null) break;
       }
    }
   if (current_root == null) current_root = droot;
   if (current_root == null) current_root = nodes.get(1);

   GraphNode tot = nodes.get(0);
   full_size = tot.getLocalSize();
   if (current_root == droot) {
      total_size = tot.getLocalSize();
    }
   else {
      total_size = current_root.getTotalSize();
    }
   max_local = 0;
   for (GraphNode n : nodes) {
      if (n.getLocalSize() > max_local && !n.getName().startsWith("*")) {
	 max_local = n.getLocalSize();
       }
    }

   if (param_values.getShowVertical()) {
      scale_y = 1.0/max_levels*d0.width;
      scale_x = 1.0/total_size*d0.height;
    }
   else {
      scale_x = 1.0/max_levels*d0.width;
      scale_y = 1.0/total_size*d0.height;
    }

   current_history.update(droot,current_root);

   max_drawn = 0;
   displayNode(g,current_root,0,0,1.0);

   if (max_drawn > max_levels) {
      max_levels *= 2;
      repaint();
    }
   else if (max_drawn > 1 && max_drawn < max_levels / 2 && max_levels > MIN_LEVELS) {
      max_levels /= 2;
      if (max_levels < MIN_LEVELS) max_levels = MIN_LEVELS;
      repaint();
    }
}



protected double displayNode(Graphics2D g,GraphNode n,int lvl,double y0,double scale)
{
   double tsz = n.getTotalSize() * scale;

   Rectangle2D r;

   if (param_values.getShowVertical()) {
      r = new Rectangle2D.Double(y0*scale_x,lvl*scale_y,tsz*scale_x,1*scale_y);
    }
   else {
      r = new Rectangle2D.Double(lvl*scale_x,y0*scale_y,1*scale_x,tsz*scale_y);
    }

   Color c0 = param_values.getViewerColor(n.getLocalSize()/max_local,scale);

   g.setColor(c0);

   g.fill(r);
   g.setColor(Color.BLACK);
   g.setStroke(OUTLINE_STROKE);
   g.draw(r);

   showInformation(g,n,r);

   g.setColor(Color.BLACK);
   if (param_values.getShowVertical()) {
      SwingText.drawText(getNodeName(n),g,r);
    }
   else {
      SwingText.drawVerticalText(getNodeName(n),g,r);
    }

   if (lvl > max_drawn) max_drawn = lvl;
   if (lvl > 2*max_levels) return tsz;

   if (tsz/total_size >= param_values.getViewerCutoff()) {
      double y1 = y0;
      for (GraphLink lnk : n.getSortedOutLinks(param_values.getViewerCompareBy())) {
	 double nscl = scale * lnk.getSizePercent();
	 y1 += displayNode(g,lnk.getToNode(),lvl+1,y1,nscl);
       }
    }

   return tsz;
}


protected void showInformation(Graphics2D g,GraphNode n,Rectangle2D r)
{ }


/********************************************************************************/
/*										*/
/*	Tool tip methods							*/
/*										*/
/********************************************************************************/

@Override public String getToolTipText(MouseEvent e)
{
   FindResult fr = locateItem(e.getX(),e.getY());

   if (fr == null) return "Memory map for " + process_id;

   GraphNode gn = fr.getGraphNode();
   double scale = fr.getScale();

   StringBuffer buf = new StringBuffer();
   buf.append("<html>");
   buf.append(getNodeName(gn));

   buf.append("<br>Total size = " + IvyFormat.formatMemory(gn.getTotalSize(),full_size));
   buf.append("<br>Local size = " + IvyFormat.formatMemory(gn.getLocalSize(),full_size));
   buf.append("<br>Total count = " + IvyFormat.formatCount(gn.getTotalCount(),0));
   buf.append("<br>Local count = " + IvyFormat.formatCount(gn.getLocalCount(),0));
   buf.append("<br>Fraction used = " + IvyFormat.formatPercent(scale));

   return buf.toString();
}



protected FindResult locateItem(double x0,double y0)
{
   if (current_root == null) return null;

   Dimension sz = getSize();

   int lvl;
   double y;

   if (param_values.getShowVertical()) {
      lvl = (int)(y0/sz.height * max_levels);
      y = x0/sz.width * total_size;
    }
   else {
      lvl = (int)(x0/sz.width * max_levels);
      y = y0/sz.height * total_size;
    }

   return findNode(current_root,0,0,1,lvl,y);
}




private FindResult findNode(GraphNode n,int x0,double y0,double scale,int lvl,double findy)
{
   double tsz = n.getTotalSize() * scale;

   if (lvl == x0 && findy >= y0 && findy <= y0+tsz) return new FindResult(n,scale);
   if (findy < y0 || findy > y0+tsz) return null;

   if (tsz/total_size >= param_values.getViewerCutoff() && lvl > x0) {
      double y1 = y0;
      for (GraphLink lnk : n.getSortedOutLinks(param_values.getViewerCompareBy())) {
	 double nscl = scale * lnk.getSizePercent();
	 FindResult fr = findNode(lnk.getToNode(),x0+1,y1,nscl,lvl,findy);
	 if (fr != null) return fr;
	 y1 += lnk.getToNode().getTotalSize()*nscl;
       }
    }

   return null;
}



/********************************************************************************/
/*										*/
/*	Test program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   DymemViewSimple dv;

   try {
      dv = new DymemViewSimple(args[0]);
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



}	// end of class DymemViewSimple




/* end of DymemViewSimple.java */
