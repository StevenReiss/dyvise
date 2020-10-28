/********************************************************************************/
/*										*/
/*		DygraphDisplayTimed.java					*/
/*										*/
/*	DYVISE graphics (visualization) abstract time-based visualization	*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dygraph/DygraphDisplayTimed.java,v 1.1 2013/09/04 18:48:10 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DygraphDisplayTimed.java,v $
 * Revision 1.1  2013/09/04 18:48:10  spr
 * Add new common file.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dygraph;

import edu.brown.cs.dyvise.dystore.*;

import edu.brown.cs.ivy.swing.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.border.*;
import javax.swing.*;
import java.util.*;
import java.util.List;



abstract class DygraphDisplayTimed extends DygraphDisplay {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Color		border_color = Color.BLACK;
private int		border_thickness = 2;
private Border		panel_border = BorderFactory.createLineBorder(border_color,border_thickness);

private double		min_step = 0.1; 	// microseconds
private double		block_step = 1.0;	// microseconds

private double		cur_mouse;
private double		cur_delta;
private double		cur_width;
private double		cur_tdelta;
private double		cur_start;

private BlockSet	block_set;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected DygraphDisplayTimed(Class<? extends Enum<?>> cls)
{
   super(cls);
   cur_mouse = -1;
   cur_delta = 0;
   cur_tdelta = 0;
   block_set = null;
}




protected DygraphDisplayTimed(DygraphDisplay dd,DygraphDisplayHandler ddh)
{
   super(dd,ddh);
   cur_mouse = -1;
   cur_delta = 0;
   cur_tdelta = 0;
   block_set = null;
}



/********************************************************************************/
/*										*/
/*	Time slice methods							*/
/*										*/
/********************************************************************************/

protected double getTimeAtPosition(double pos)
{
   double tdelta = display_handler.getTimeSpan();
   double w = (cur_width == 0 ? 1 : cur_width);

   if (block_set == null) {
      computeBlockSet();
    }

   if (cur_mouse < 0) {
      if (block_set == null || block_set.isEmpty()) {
	 return display_handler.getTimeAtDelta(pos*tdelta/w);
       }
      else {
	 return block_set.getSkippedTime(pos,w);
       }
    }

   tdelta -= block_set.getTotalSkipped();

   if (cur_delta < 0 || cur_tdelta != tdelta) {
      cur_tdelta = tdelta;
      cur_delta = findDistance(cur_mouse,w,tdelta);
      cur_start = display_handler.getStartTime();
      // System.err.println("SET MOUSE " + cur_mouse + " " + cur_delta + " " + cur_tdelta);
    }

   double x = findPosition(cur_delta,cur_mouse,w,tdelta,pos);
   if (block_set != null && !block_set.isEmpty()) x = block_set.getActualTime(x);

   return cur_start + x;
}



private double findDistance(double m,double w,double t)
{
   double g = min_step;

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




/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

@Override void handleDataUpdated()
{
   cur_mouse = -1;
   cur_delta = 0;
   block_set = null;
}


/********************************************************************************/
/*										*/
/*	Panel methods								*/
/*										*/
/********************************************************************************/

protected JPanel setupDisplayPanel()
{
   JPanel bpnl = createDisplayPanel();
   JPanel pnl = new SwingBorderPanel(panel_border,bpnl);
   bpnl.addComponentListener(new CompChanger());
   Mouser mm = new Mouser(bpnl);
   bpnl.addMouseListener(mm);
   bpnl.addMouseMotionListener(mm);
   bpnl.addMouseWheelListener(mm);

   cur_width = bpnl.getWidth();

   return pnl;
}


protected abstract JPanel createDisplayPanel();




/********************************************************************************/
/*										*/
/*	Panel tracking methods							*/
/*										*/
/********************************************************************************/

private class CompChanger extends ComponentAdapter {

   @Override public void componentResized(ComponentEvent e) {
      cur_mouse = -1;
      cur_delta = 0;
      cur_width = e.getComponent().getWidth();
    }

}	// end of inner class CompChanger



private class Mouser extends MouseAdapter {

   private JPanel for_panel;
   private boolean do_drag;

   Mouser(JPanel pnl) {
      for_panel = pnl;
      do_drag = false;
    }

   @Override public void mouseClicked(MouseEvent e) {
      if (e.getButton() == MouseEvent.BUTTON3) {
	 setPosition(-1);
       }
    }

   @Override public void mouseDragged(MouseEvent e) {
      if (do_drag) {
	 double x = e.getX();
	 if (x < 0) x = 0;
	 if (x >= cur_width) x = cur_width-1;
	 setPosition(x);
       }
    }

   @Override public void mousePressed(MouseEvent e) {
      if (e.getButton() == MouseEvent.BUTTON1) {
	 setPosition(e.getX());
	 do_drag = true;
       }
    }

   @Override public void mouseReleased(MouseEvent e) {
      do_drag = false;
    }

   @Override public void mouseWheelMoved(MouseWheelEvent e) { }

   private void setPosition(double v) {
      double opos = cur_mouse;
      if (v < 0 || v >= cur_width) {
	 cur_mouse = -1;
	 cur_delta = 0;
       }
      else if (cur_mouse != v) {
	 cur_mouse = v;
	 cur_delta = -1;
       }
      if (cur_mouse != opos) for_panel.repaint();
    }

}	// end of inner class Mouser



/********************************************************************************/
/*										*/
/*	BlockSet -- set of time blocks to ignore				*/
/*										*/
/********************************************************************************/

private synchronized void computeBlockSet()
{
   block_set = null;
   DygraphValueContext ctx = getTimeContext();
   if (cur_width == 0 || ctx == null) return;

   block_set = new BlockSet();

   double tdelta = display_handler.getTimeSpan();
   double w = cur_width;
   double g = block_step;
   if (g*w > tdelta) return;		// no need to find blocks
   double mindelta = 5 * g + 20 * g + 5 * g;

   DystoreTable tbl = ctx.getTable();
   DystoreField sf = tbl.getStartTimeField();
   DystoreField ef = tbl.getEndTimeField();

   DystoreDataMap data = null;
   ctx.setTimes(display_handler.getStartTime(),display_handler.getEndTime());
   data = display_handler.nextTupleSet(ctx,null,true,true,0,1,null,null);
   double lasttime = display_handler.getStartTime();
   for (DystoreRangeSet rs : data.values()) {
      for (DystoreTuple dt : rs) {
	 double start = dt.getTimeValue(sf);
	 double end = dt.getTimeValue(ef);
	 if (start - lasttime > mindelta) {
	    double gapstart = lasttime + 5 * g;
	    double gapend = start - 5 * g;
	    block_set.addSkip(gapstart,gapend);
	    // System.err.println("DYGRAPH: Create a gap from " + gapstart + " TO " + gapend);
	  }
	 lasttime = end;
       }
    }
}


protected DygraphValueContext getTimeContext()
{
   return null;
}



private class BlockSet {

   private List<BlockInterval> skip_intervals;
   private double total_skipped;

   BlockSet() {
      skip_intervals = new ArrayList<BlockInterval>();
      total_skipped = 0;
    }

   boolean isEmpty()			{ return skip_intervals.isEmpty(); }
   double getTotalSkipped()		{ return total_skipped; }

   double getSkippedTime(double pos,double w) {
      double tdelta = display_handler.getTimeSpan();
      double tt = tdelta - total_skipped;
      double t1 = pos / w * tt + display_handler.getStartTime();     // nominal position
      for (BlockInterval bi : skip_intervals) {
	 if (t1 < bi.getStartTime()) break;
	 t1 += bi.getEndTime() - bi.getStartTime();
       }
      return t1;
    }

   double getActualTime(double t0) {
      double t1 = t0 + display_handler.getStartTime();
      // should make this faster
      for (BlockInterval bi : skip_intervals) {
	 if (t1 < bi.getStartTime()) break;
	 t1 += bi.getEndTime() - bi.getStartTime();
       }
      return t1 - display_handler.getStartTime();
    }


   void addSkip(double s,double e) {
      BlockInterval bi = new BlockInterval(s,e);
      skip_intervals.add(bi);
      total_skipped += e - s;
    }

}	// end of inner class BlockSet


private static class BlockInterval {

   private double start_time;
   private double end_time;

   BlockInterval(double s,double e) {
      start_time = s;
      end_time = e;
    }

   double getStartTime()			{ return start_time; }
   double getEndTime()				{ return end_time; }

}	// end of inner class BlockInterval



}	// end of class DygraphDisplayTimed




/* end of DygraphDislayTimed.java */
