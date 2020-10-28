/********************************************************************************/
/*										*/
/*		DymemTimeLine.java						*/
/*										*/
/*	JPanel for viewing memory usage time line				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymem/DymemTimeLine.java,v 1.6 2009-10-07 22:39:49 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymemTimeLine.java,v $
 * Revision 1.6  2009-10-07 22:39:49  spr
 * Eclipse code cleanup.
 *
 * Revision 1.5  2009-10-07 01:00:02  spr
 * Code cleanup for eclipse. Add support for standalone version.
 *
 * Revision 1.4  2009-06-04 18:53:35  spr
 * Clean up memory display.
 *
 * Revision 1.3  2009-04-28 18:00:57  spr
 * Update visualization with data panel.
 *
 * Revision 1.2  2009-04-14 22:23:00  spr
 * Bug fit in timeline to avoid too few colors.
 *
 * Revision 1.1  2009-04-11 23:45:29  spr
 * Provide updated visualizations with time line, hashing, etc.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymem;


import edu.brown.cs.dyvise.dyvise.*;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.JPanel;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;



public class DymemTimeLine extends JPanel implements DymemConstants, DyviseConstants.TimeListener
{



/********************************************************************************/
/*										*/
/*	Local Storage								*/
/*										*/
/********************************************************************************/

private String	process_id;

private SortedMap<Long,Long> memory_map;
private SortedSet<Long> dump_times;
private boolean is_vertical;
private boolean flip_dir;
private double max_mem;
private double min_mem;
private long min_time;
private long max_time;
private long cur_time;
private DyviseTimeManager time_manager;

private final static Color LOW_COLOR = Color.WHITE;
private final static Color HIGH_COLOR = Color.BLUE;

private final static float [] LOW_COLOR_RGB;
private final static float [] HIGH_COLOR_RGB;


private final static Color DUMP_COLOR = Color.MAGENTA;
private final static Stroke DUMP_STROKE = new BasicStroke(2.0f);

private final static Color TIME_COLOR = Color.YELLOW;
private final static Stroke TIME_STROKE = new BasicStroke(4.0f);

private final static SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("hh:mm:ss.SSS");
private static final DecimalFormat MEMORY_FORMAT = new DecimalFormat("0.00");
private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.0");


private static final long serialVersionUID = 1;


static {
   LOW_COLOR_RGB = LOW_COLOR.getRGBColorComponents(null);
   HIGH_COLOR_RGB = HIGH_COLOR.getRGBColorComponents(null);
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DymemTimeLine(String pid,DymemParameters dp)
{
   process_id = pid;

   memory_map = new TreeMap<Long,Long>();
   dump_times = new TreeSet<Long>();
   time_manager = DyviseTimeManager.getTimeManager(pid);
   time_manager.addTimeListener(this);

   max_mem = 0;
   min_mem = -1;
   min_time = -1;
   max_time = 0;
   cur_time = 0;

   setVertical(TimeLineDirection.VERTICAL_UP);

   setToolTipText("Memory Usage Time Line for " + process_id);
   addMouseListener(new Mouser());
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

void setVertical(TimeLineDirection dir)
{
   switch (dir) {
      case VERTICAL_UP :
	 is_vertical = true;
	 flip_dir = true;
	 break;
      case VERTICAL_DOWN :
	 is_vertical = true;
	 flip_dir = false;
	 break;
      case HORIZONTAL :
	 is_vertical = false;
	 flip_dir = false;
	 break;
    }

   setPreferredSize(new Dimension(16,16));
}



void updateUsage(Element e)
{
   long when = IvyXml.getAttrLong(e,"WHEN");
   long tmem = IvyXml.getAttrLong(e,"MEMORY");

   // System.err.println("DYMEM: NOTE USAGE AT " + when);

   synchronized (memory_map) {
      if (when > max_time) max_time = when;
      if (when < min_time || min_time < 0) min_time = when;
      if (tmem > max_mem) max_mem = tmem;
      if (tmem < min_mem || min_mem < 0) min_mem = tmem;
      memory_map.put(when,tmem);
    }

   repaint();
}



void noteDump(long when)
{
   synchronized (memory_map) {
      if (when > max_time) max_time = when;
      if (when < min_time || min_time < 0) min_time = when;
    }

   synchronized (dump_times) {
      dump_times.add(when);
    }

   repaint();
}



public void handleMark(long when,String what)			{ }

public void handleTimeSet(long when)
{
   if (cur_time != when) {
      cur_time = when;
      repaint();
    }
}




/********************************************************************************/
/*										*/
/*	Drawing methods 							*/
/*										*/
/********************************************************************************/

protected void paintComponent(Graphics g0)
{
   Graphics2D g = (Graphics2D) g0.create();

   Dimension d0 = getSize();
   g.clearRect(0,0,d0.width,d0.height);

   if (memory_map.size() < 4) return;

   List<Color> cols = new ArrayList<Color>();
   List<Float> pcts = new ArrayList<Float>();

   synchronized (memory_map) {
      float prev = 0;
      for (Map.Entry<Long,Long> ent : memory_map.entrySet()) {
	 long when = ent.getKey();
	 float memv = ent.getValue();
	 float v0 = ((float)(((double)(when-min_time))/(max_time - min_time)));
	 float v1 = ((float)((memv-min_mem)/(max_mem - min_mem)));
	 if (v1 > 1) v1 = 1;
	 if (v1 < 0) v1 = 0;
	 if (v0 > 1) v0 = 1;
	 if (v0 < 0) v0 = 0;
	 if (prev < v0) {
	    Color c0 = new Color ((1-v1)*LOW_COLOR_RGB[0] + v1 * HIGH_COLOR_RGB[0],
				     (1-v1)*LOW_COLOR_RGB[1] + v1 * HIGH_COLOR_RGB[1],
				     (1-v1)*LOW_COLOR_RGB[2] + v1 * HIGH_COLOR_RGB[2]);
	    cols.add(c0);
	    pcts.add(v0);
	    prev = v0;
	  }
       }
    }

   if (cols.size() < 2) return;

   Color [] cola = new Color[cols.size()];
   float [] pcta = new float[pcts.size()];
   for (int i = 0; i < cola.length; ++i) {
      cola[i] = cols.get(i);
      pcta[i] = pcts.get(i);
    }

   Point2D s0,s1;
   if (is_vertical && flip_dir) {
      s1 = new Point2D.Double(d0.width/2.0,d0.height);
      s0 = new Point2D.Double(d0.width/2.0,0);
    }
   if (is_vertical) {
      s0 = new Point2D.Double(d0.width/2.0,d0.height);
      s1 = new Point2D.Double(d0.width/2.0,0);
    }
   else {
      s0 = new Point2D.Double(0,d0.height/2.0);
      s1 = new Point2D.Double(d0.width,d0.height/2.0);
    }

   LinearGradientPaint lpg = new LinearGradientPaint(s0,s1,pcta,cola);
   g.setPaint(lpg);
   Rectangle2D r0 = new Rectangle2D.Double(0,0,d0.width,d0.height);
   g.fill(r0);

   g.setColor(DUMP_COLOR);
   g.setStroke(DUMP_STROKE);
   Line2D.Double l0 = new Line2D.Double();

   synchronized (dump_times) {
      for (Long lv : dump_times) {
	 double v0 = (lv.doubleValue() - min_time) / (max_time - min_time);
	 // if (is_vertical) v0 = 1.0 - v0;
	 if (v0 < 0 || v0 > 1) continue;
	 if (is_vertical) l0.setLine(0,(d0.height-1)*(1-v0),d0.width,(d0.height-1)*(1-v0));
	 else l0.setLine((d0.width-1)*v0,0,(d0.width-1)*v0,d0.height);
	 g.draw(l0);
       }
    }

   if (cur_time != 0) {
      double v0 = ((double)(cur_time - min_time)) / (max_time - min_time);
      // if (!flip_dir) v0 = 1.0 - v0;
      g.setColor(TIME_COLOR);
      g.setStroke(TIME_STROKE);
      if (is_vertical) l0.setLine(0,d0.height*(1-v0),d0.width,d0.height*(1-v0));
      else l0.setLine(d0.width*v0,0,d0.width*v0,d0.height);
      g.draw(l0);
    }
}



/********************************************************************************/
/*										*/
/*	Interaction handling							*/
/*										*/
/********************************************************************************/

private void handleMouseClick(MouseEvent e)
{
   if (e.getButton() == MouseEvent.BUTTON1) {
      long t0 = getTime(e);
      time_manager.setCurrentTime(t0);
    }
   else if (e.getButton() == MouseEvent.BUTTON3) {
      time_manager.setCurrentTime(0);
    }
}



private class Mouser extends MouseAdapter {

   public void mouseClicked(MouseEvent e) {
      handleMouseClick(e);
    }

}	// end of subclass Mouser



private long getTime(MouseEvent e)
{
   double v;

   Dimension sz = getSize();

   if (is_vertical) {
      v = e.getY();
      v /= sz.height;
    }
   else {
      v = e.getX();
      v /= sz.width;
    }

   if (flip_dir) v = 1.0-v;

   long t = min_time + (long)((max_time - min_time) * v);

   return t;
}




/********************************************************************************/
/*										*/
/*	Tool tip methods							*/
/*										*/
/********************************************************************************/

@Override public String getToolTipText(MouseEvent e)
{
   long t0 = getTime(e);
   Date d = new Date(t0);

   StringBuffer rslt = new StringBuffer();
   rslt.append("<html>");
   rslt.append("Memory Usage Time Line for " + process_id);
   rslt.append("<br>Time: " + TIME_FORMAT.format(d));

   synchronized (memory_map) {
      SortedMap<Long,Long> hm = memory_map.headMap(t0);
      SortedMap<Long,Long> tm = memory_map.tailMap(t0);
      if (hm.size() != 0) {
	 Date d1 = new Date(hm.lastKey());
	 rslt.append("<br>Usage: " + outputSize(hm.get(hm.lastKey()),0) + " at " +
			TIME_FORMAT.format(d1));
       }
      if (tm.size() != 0) {
	 Date d2 = new Date(tm.firstKey());
	 rslt.append("<br>Usage: " + outputSize(tm.get(tm.firstKey()),0) + " at " +
			TIME_FORMAT.format(d2));
       }
    }

   rslt.append("</html>");

   return rslt.toString();
}



protected String outputSize(double v0,double max)
{
   String tail = "";

   double v = v0;

   if (v > 1024*1024*1024) {
      v /= 1024*1024*1024;
      tail = "G";
    }
   else if (v > 1024*1024) {
      v /= 1024*1024;
      tail = "M";
    }
   else if (v > 1024) {
      v /= 1024;
      tail = "K";
    }

   String s = MEMORY_FORMAT.format(v) + tail;

   if (max != 0) {
      s += " (" + outputPercent(v0/max) + "%)";
    }

   return s;
}




protected String outputPercent(double v)
{
   v *= 100.0;

   return PERCENT_FORMAT.format(v);
}






}	// end of DymemTimeLine




/* end of DymemTimeLine.java */
