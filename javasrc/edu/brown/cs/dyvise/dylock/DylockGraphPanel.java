/********************************************************************************/
/*										*/
/*		DylockGraphPanel.java						*/
/*										*/
/*	DYVISE lock analysis lock graph showing lock relationships		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylock/DylockGraphPanel.java,v 1.2 2011-04-01 23:09:02 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylockGraphPanel.java,v $
 * Revision 1.2  2011-04-01 23:09:02  spr
 * Bug clean up.
 *
 * Revision 1.1  2011-03-10 02:25:00  spr
 * New version of dylock to produce visualizations.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dylock;

import edu.brown.cs.ivy.petal.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import java.util.*;
import java.util.List;


class DylockGraphPanel extends JPanel implements DylockConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private List<DylockLockData> lock_data;
private GraphDisplay	graph_display;
private Map<DylockLockData,LockNode> node_map;
private double		user_scale;
private double		prior_scale;

private final double SCALE_FACTOR = 1.125;

private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DylockGraphPanel(DylockViewer dv,Collection<DylockLockData> locks)
{
   super(new BorderLayout());

   user_scale = 1.0;
   prior_scale = 1.0;

   Set<DylockLockData> lks = new TreeSet<DylockLockData>(new LockComparator());
   lks.addAll(locks);
   lock_data = new ArrayList<DylockLockData>(lks);

   LockModel mdl = setupGraph();

   Dimension sz = new Dimension(300,300);
   graph_display = new GraphDisplay(mdl);
   graph_display.setSize(sz);
   graph_display.setPreferredSize(sz);

   addComponentListener(new Sizer());

   add(new JScrollPane(graph_display),BorderLayout.CENTER);
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
/*	Methods for handling sizing						*/
/*										*/
/********************************************************************************/

private class Sizer extends ComponentAdapter {

   @Override public void componentResized(ComponentEvent e) {
      updateGraph();
    }

}	// end of inner class Sizer



private class Wheeler extends MouseAdapter {

   @Override public void mouseWheelMoved(MouseWheelEvent e) {
      int mods = e.getModifiersEx();
      if ((mods & MouseEvent.CTRL_DOWN_MASK) == 0) return;
      int ct = e.getWheelRotation();
      zoom(ct);
      e.consume();
   }

}	// end of inner class Wheeler



/********************************************************************************/
/*										*/
/*	Graph setup methods							*/
/*										*/
/********************************************************************************/

private LockModel setupGraph()
{
   Set<DylockLockData> used = new HashSet<DylockLockData>();
   for (DylockLockData vd : lock_data) {
      for (DylockLockData pd : vd.getPriorLocks()) {
	 used.add(pd);
	 used.add(vd);
       }
    }

   LockModel mdl = new LockModel();
   node_map = new HashMap<DylockLockData,LockNode>();

   for (DylockLockData vd : lock_data) {
      if (used.contains(vd)) {
	 LockNode ln = new LockNode(vd);
	 node_map.put(vd,ln);
	 mdl.addNode(ln);
       }
    }

   for (DylockLockData vd : lock_data) {
      for (DylockLockData pd : vd.getPriorLocks()) {
	 LockArc la = new LockArc(pd,vd);
	 mdl.addArc(la);
       }
    }

   return mdl;
}


private void updateGraph()
{
   if (graph_display != null) {
      Dimension d1 = graph_display.getPreferredSize();
      Dimension d2 = getSize();
      if (d1.width != 0 && d2.width != 0 && d1.height != 0 && d2.height != 0) {
	 double dx = d2.getWidth() / d1.getWidth();
	 double dy = d2.getHeight() / d1.getHeight();
	 double da = Math.min(dx,dy);
	 double db = graph_display.getScaleFactor() / prior_scale;
	 graph_display.setScaleFactor(da*db*0.95*user_scale);
	 prior_scale = user_scale;
       }
      graph_display.repaint();
   }
}



void zoom(int amt)
{
   for (int i = 0; i < Math.abs(amt); ++i) {
      if (amt < 0) user_scale /= SCALE_FACTOR;
      else user_scale *= SCALE_FACTOR;
   }
   if (Math.abs(user_scale - 1.0) < 0.001) user_scale = 1;
   if (user_scale < 1/128.0) user_scale = 1/128.0;
   // if (user_scale > 2048) user_scale = 2048;

   if (graph_display != null) {
      double sf = graph_display.getScaleFactor();
      if (sf * user_scale / prior_scale > 2) {
	 user_scale = 2 * prior_scale / sf;
      }
      sf = sf * user_scale / prior_scale;
      graph_display.setScaleFactor(sf);
      prior_scale = user_scale;
      graph_display.repaint();
   }
}






/********************************************************************************/
/*										*/
/*	LockModel -- graph model						*/
/*										*/
/********************************************************************************/

private class LockModel extends PetalModelDefault {

   LockModel()				{ }

}	// end of inner class LockModel




/********************************************************************************/
/*										*/
/*	LockNode -- Petal interface for nodes					*/
/*										*/
/********************************************************************************/

private class LockNode extends PetalNodeDefault {

   private DylockLockData xlock_data;

   private static final long serialVersionUID = 1;

   LockNode(DylockLockData l) {
      super(l.getGraphString());
      xlock_data = l;
    }



   @Override public String getToolTip(Point at) {
      return xlock_data.getOverviewToolTip();
    }

}	// end of inner class LockNode



/********************************************************************************/
/*										*/
/*	LockArc -- representation of an lock relationship			*/
/*										*/
/********************************************************************************/

private class LockArc extends PetalArcDefault {

   private static final long serialVersionUID = 1;

   LockArc(DylockLockData frm,DylockLockData to) {
      super(node_map.get(frm),node_map.get(to));
      setTargetEnd(new PetalArcEndDefault(PetalConstants.PETAL_ARC_END_ARROW));
    }

}	// end of inner class LockArc



/********************************************************************************/
/*										*/
/*	Petal editor								*/
/*										*/
/********************************************************************************/

private class GraphDisplay extends PetalEditor {

   private static final long serialVersionUID = 1;

   GraphDisplay(LockModel mdl) {
      super(mdl);
      PetalLevelLayout pll = new PetalLevelLayout(this);
      pll.setOptimizeLevels(true);
      commandLayout(pll);
      addMouseWheelListener(new Wheeler());
    }

}	// end of inner class GraphDisplay




}	// end of class DylockGraphPanel



/* end of DylockGraphPanel.java */
