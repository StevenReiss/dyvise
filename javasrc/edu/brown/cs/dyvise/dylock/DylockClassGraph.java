/********************************************************************************/
/*                                                                              */
/*              DylockClassGraph.java                                           */
/*                                                                              */
/*      Lock analysis graph showing locks and classes                           */
/*                                                                              */
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylock/DylockClassGraph.java,v 1.1 2013-05-09 12:28:59 spr Exp $ */

/*********************************************************************************
 *
 * $Log $
 *
 ********************************************************************************/




package edu.brown.cs.dyvise.dylock;

import edu.brown.cs.ivy.petal.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import java.util.*;
import java.util.List;



class DylockClassGraph extends JPanel implements DylockConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private List<DylockLockData>    lock_data;
private GraphDisplay            graph_display;
private Map<DylockLockData,LockNode> lock_nodes;
private Map<String,ClassNode>   class_nodes;
private double                  user_scale;
private double                  prior_scale;

private final double SCALE_FACTOR = 1.125;

private static final long serialVersionUID = 1;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

DylockClassGraph(Collection<DylockLockData> locks)
{
   super(new BorderLayout());
    
   user_scale = 1.0;
   prior_scale = 1.0;
   
   lock_data = new ArrayList<DylockLockData>(locks);
   
   GraphModel mdl = setupGraph();
   
   Dimension sz = new Dimension(500,400);
   graph_display = new GraphDisplay(mdl);
   graph_display.setSize(sz);
   graph_display.setPreferredSize(sz);
   
   addComponentListener(new Sizer());
   
   add(new JScrollPane(graph_display),BorderLayout.CENTER);
}



/********************************************************************************/
/*                                                                              */
/*      Methods to handle sizing                                                */
/*                                                                              */
/********************************************************************************/

private class Sizer extends ComponentAdapter {
   
   @Override public void componentResized(ComponentEvent e) {
      updateGraphSize();
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



private void updateGraphSize()
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




/********************************************************************************/
/*                                                                              */
/*      Methods to setup the graph                                              */
/*                                                                              */
/********************************************************************************/

private GraphModel setupGraph()
{
   GraphModel mdl = new GraphModel();
   lock_nodes = new HashMap<DylockLockData,LockNode>();
   class_nodes = new HashMap<String,ClassNode>();
   Map<String,LockNode> nodes = new HashMap<String,LockNode>();
   Map<LockNode,Set<String>> links = new HashMap<LockNode,Set<String>>();
   
   for (DylockLockData vd : lock_data) {
      String s1 = vd.getGraphString();
      LockNode ln = nodes.get(s1);
      if (ln == null) {
         ln = new LockNode(vd);
         lock_nodes.put(vd,ln);
         nodes.put(s1,ln);
       }
      lock_nodes.put(vd,ln);
      mdl.addNode(ln);
      Set<String> done = links.get(ln);
      if (done == null) {
         done = new HashSet<String>();
         links.put(ln,done);
       }
      for (TraceLockLocation loc : vd.getLocations()) {
         String cnm = loc.getClassName();
         if (done.contains(cnm)) continue;
         done.add(cnm);
         ClassNode cn = class_nodes.get(cnm);
         if (cn == null) {
            cn = new ClassNode(cnm);
            class_nodes.put(cnm,cn);
          }
         mdl.addNode(cn);
         UsageArc ua = new UsageArc(cn,ln);
         mdl.addArc(ua);
       }
    }
   
   return mdl;
}




/********************************************************************************/
/*										*/
/*	LockModel -- graph model						*/
/*										*/
/********************************************************************************/

private class GraphModel extends PetalModelDefault {
   
   GraphModel()				{ }
   
}	// end of inner class GraphModel




/********************************************************************************/
/*										*/
/*	LockNode and ClassNode -- Petal interface for nodes		        */
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




private class ClassNode extends PetalNodeDefault {
   
   private String class_name;
   private ClassIcon class_icon;
   
   private static final long serialVersionUID = 1;
   
   ClassNode(String cls) {
      class_name = cls;
      String xnm = cls;
      int idx = xnm.lastIndexOf(".");
      if (idx < 0) idx = xnm.lastIndexOf("/");
      if (idx >= 0) xnm = xnm.substring(idx+1);
      xnm = xnm.replaceAll("$",".");
      if (xnm.endsWith(".")) xnm = xnm.substring(0,xnm.length()-1);
      class_icon = new ClassIcon(xnm);
      JLabel lbl = new JLabel(xnm,class_icon,SwingConstants.CENTER);
      lbl.setHorizontalTextPosition(SwingConstants.CENTER);
      Dimension d = lbl.getPreferredSize();
      lbl.setSize(d);
      lbl.setMinimumSize(d);
      lbl.setBackground(Color.YELLOW);
      setComponent(lbl);     
    }
   
   @Override public Point findPortPoint(Point at,Point from) {
      return PetalHelper.findOvalPortPoint(getComponent().getBounds(),at,from);
    }
   
   @Override public String getToolTip(Point at) {
      return class_name;
    }
   
}	// end of inner class LockNode




private class ClassIcon implements Icon {
  
   private String icon_name;
   private Color fill_color;
   private int border_width;
   private int icon_width;
   private int icon_height;
   
   ClassIcon(String name) {
      icon_name = name;
      fill_color = Color.white;
      border_width = 1;
      icon_width = 20;
      icon_height = 20;
      reset();
    }
   
   public int getIconWidth()				{ return icon_width; }
   public int getIconHeight()				{ return icon_height; }
   
   public void paintIcon(Component _c,Graphics g,int x,int y) {
      Color oc = g.getColor();
      
      g.setColor(fill_color);
      g.fillOval(x,y,icon_width,icon_height);
      
      g.setColor(Color.BLACK);
      for (int i = 0; i < border_width; ++i) {
	 g.drawOval(x+i,y+i,icon_width-2*i-1,icon_height-2*i-1);
       }
      
      g.setColor(oc);
    }
   
   void reset() {
      fill_color = Color.WHITE;
      border_width = 1;
      
      JLabel tlbl = new JLabel(icon_name);
      Dimension sz = tlbl.getPreferredSize();
      int delta = (border_width - 1)*2;
      icon_width = sz.width + 6 + delta;
      icon_height = sz.height*2 + 5;
      
      if (icon_width > 1000) {
	 System.err.println("SIZE " + icon_width + " " + icon_height);
	 System.err.println(tlbl.getPreferredSize() + " " + sz + " " + border_width + " " + delta);
       }
    }
   
}	// end of subclass StateIcon




/********************************************************************************/
/*										*/
/*	LockArc -- representation of an lock relationship			*/
/*										*/
/********************************************************************************/

private class UsageArc extends PetalArcDefault {
   
   private static final long serialVersionUID = 1;
   
   UsageArc(ClassNode frm,LockNode to) {
      super(frm,to);
      setTargetEnd(new PetalArcEndDefault(PetalConstants.PETAL_ARC_END_ARROW));
    }
   
}	// end of inner class UsageArc



/********************************************************************************/
/*										*/
/*	Petal editor								*/
/*										*/
/********************************************************************************/

private class GraphDisplay extends PetalEditor {
   
   private static final long serialVersionUID = 1;
   
   GraphDisplay(GraphModel mdl) {
      super(mdl);
      PetalLevelLayout pll = new PetalLevelLayout(this);
      pll.setOptimizeLevels(true);
      commandLayout(pll);
      addMouseWheelListener(new Wheeler());
    }
   
}	// end of inner class GraphDisplay

}       // end of class DylockClassGraph




/* end of DylockClassGraph.java */
