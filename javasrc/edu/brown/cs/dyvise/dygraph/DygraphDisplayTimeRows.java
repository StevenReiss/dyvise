/********************************************************************************/
/*										*/
/*		DygraphDisplayTimeRows.java					*/
/*										*/
/*	DYVISE graphics (visualization) time-row visualization			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dygraph/DygraphDisplayTimeRows.java,v 1.9 2013-05-09 12:28:56 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DygraphDisplayTimeRows.java,v $
 * Revision 1.9  2013-05-09 12:28:56  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.8  2012-10-05 00:52:14  spr
 * Code cleanup
 *
 * Revision 1.7  2011-03-19 20:34:08  spr
 * Fix time block display and performance issues.
 *
 * Revision 1.6  2011-03-10 02:32:57  spr
 * Fixups for lock visualization.
 *
 * Revision 1.5  2010-03-30 16:20:44  spr
 * Fix bugs and features in graphical output.
 *
 * Revision 1.4  2009-10-07 22:39:33  spr
 * Fix problem with initial dump.
 *
 * Revision 1.3  2009-10-07 00:59:44  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.2  2009-09-21 19:34:21  spr
 * Add debugging code.
 *
 * Revision 1.1  2009-09-19 00:08:37  spr
 * Module to draw various types of displays.  Only time rows implemented for now.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dygraph;

import edu.brown.cs.dyvise.dystore.*;

import javax.swing.JPanel;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;



class DygraphDisplayTimeRows extends DygraphDisplayTimed {


/********************************************************************************/
/*										*/
/*	Constants								*/
/*										*/
/********************************************************************************/

enum Selectors {
   ROWDATA,		// table to use for rows
   LINKDATA,		// table to use for links
   ROW, 		// field to define a row
   ROWITEM,		// field for spliting row
   HUE, 		// field to define color for set of elements
   SAT,
   VALUE,
   WIDTH,		// width of row if not split
   ITEMWIDTH,		// width of individual item as % of possible
   DATA1,		// first link data field
   DATA2,		// second link data field
   LHUE,		// field to define color for links
   LSAT,
   LVALUE,
   LENDHUE,
   LENDSAT,
   LENDVALUE,
   LALPHA,
   SPLITROWS
}

private boolean 	split_rows = false;
private boolean 	draw_lines = true;
private boolean 	draw_links = true;
private Color		background_color = Color.WHITE;





private final static double ROW_SPACE = 0.2;
private final static double OVERVIEW_PART = 0.5;




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<String,ActiveLink>	active_links;
private Line2D			link_line;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DygraphDisplayTimeRows()
{
   super(Selectors.class);

   active_links = new HashMap<String,ActiveLink>();
   link_line = new Line2D.Double();
}



private DygraphDisplayTimeRows(DygraphDisplayTimeRows proto,DygraphDisplayHandler ddh)
{
   super(proto,ddh);

   active_links = new HashMap<String,ActiveLink>();
   link_line = new Line2D.Double();
}




/********************************************************************************/
/*										*/
/*	Construction methods							*/
/*										*/
/********************************************************************************/

protected DygraphDisplay createNew(DygraphDisplayHandler ddh)
{
   return new DygraphDisplayTimeRows(this,ddh);
}



/********************************************************************************/
/*										*/
/*	Graphics setup methods							*/
/*										*/
/********************************************************************************/

protected JPanel createDisplayPanel()
{
   return new TimeRowPanel();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getSelectorDescription(Enum<?> s)
{
   Selectors ss = (Selectors) (Object) s;
   String rslt = null;

   switch (ss) {
      case ROWDATA :
	 rslt = "Row elements";
	 break;
      case LINKDATA :
	 rslt = "Link elements";
	 break;
      case ROW :
	 rslt = "Row items";
	 break;
      case ROWITEM :
	 rslt = "Splitting item";
	 break;
      case HUE :
	 rslt = "Hue";
	 break;
      case SAT :
	 rslt = "Saturation";
	 break;
      case VALUE :
	 rslt = "Brightness";
	 break;
      case WIDTH :
	 rslt = "Row width";
	 break;
      case ITEMWIDTH :
	 rslt = "Item width";
	 break;
      case DATA1 :
	 rslt = "Link data";
	 break;
      case DATA2 :
	 rslt = "Other link data";
	 break;
      case LHUE :
	 rslt = "Link Hue";
	 break;
      case LSAT :
	 rslt = "Link Saturation";
	 break;
      case LVALUE :
	 rslt = "Link Brightness";
	 break;
      case LENDHUE :
	 rslt = "Link End Hue";
	 break;
      case LENDSAT :
	 rslt = "Link End Saturation";
	 break;
      case LENDVALUE :
	 rslt = "Link End Brightness";
	 break;
      case LALPHA :
	 rslt = "Link Transparency";
	 break;
      case SPLITROWS :
	 rslt = "Showing States";
	 break;
    }

   return rslt;
}


SelectorType getSelectorType(Enum<?> s)
{
   Selectors ss = (Selectors)(Object) s;
   SelectorType st = SelectorType.GENERIC;

   switch (ss) {
      case ROWDATA :
      case LINKDATA :
	 st = SelectorType.TABLE;
	 break;
      case ROW :
      case ROWITEM :
	 st = SelectorType.SORTED_SET;
	 break;
      case HUE :
      case SAT :
      case VALUE :
      case WIDTH :
      case ITEMWIDTH :
      case LHUE :
      case LSAT :
      case LVALUE :
      case LENDHUE :
      case LENDSAT :
      case LENDVALUE :
      case LALPHA :
	 st = SelectorType.VALUE;
	 break;
      case SPLITROWS :
	 st = SelectorType.BOOLEAN;
	 break;
      default :
	 break;
    }

   return st;
}


/********************************************************************************/
/*										*/
/*	Drawing methods 							*/
/*										*/
/********************************************************************************/

private void drawTimeRows(Graphics2D g,Dimension sz)
{
   Color c = g.getColor();
   g.setColor(background_color);
   g.fillRect(0,0,sz.width,sz.height);
   g.setColor(c);
   active_links.clear();

   split_rows = display_handler.getBoolean(Selectors.SPLITROWS);

   DystoreDataMap data = null;
   double ystep = 0;
   double ysize = 0;

   DygraphValueContext rctx = display_handler.getContext(Selectors.ROWDATA);
   DygraphValueContext lctx = display_handler.getContext(Selectors.LINKDATA);

   DystoreDataMap ldata = null;
   DystoreField lvla = display_handler.getField(lctx,Selectors.DATA1);
   DystoreField lvlb = display_handler.getField(lctx,Selectors.DATA2);
   DystoreField rvla = display_handler.getField(rctx,Selectors.DATA1);
   DystoreField rvlb = display_handler.getField(rctx,Selectors.DATA2);

   double t0 = getTimeAtPosition(0);
   for (int i = 0; i < sz.getWidth(); ++i) {
      double t1 = getTimeAtPosition(i+1);
      rctx.setTimes(t0,t1);
      lctx.setTimes(t0,t1);
      // System.err.println("TIME ROWS " + t0 + " " + t1);
      data = display_handler.nextTupleSet(rctx,Selectors.ROW,true,
					     (i == 0),
					     display_handler.getYDataStart(),
					     display_handler.getYDataEnd(),data,null);
      if (draw_links) {
	 ldata = display_handler.nextTupleSet(lctx,Selectors.ROW,true,(i == 0),
						 display_handler.getYDataStart(),
						 display_handler.getYDataEnd(),ldata,data);
       }

      if (ystep == 0) {
	 int nelt = data.size();
	 ystep = sz.getHeight();
	 ystep /= nelt;
	 ysize = ystep * (1.0 - ROW_SPACE);
       }
      int idx = 0;
      for (Map.Entry<String,DystoreRangeSet> ent : data.entrySet()) {
	 DystoreRangeSet tups = ent.getValue();
	 if (tups.size() > 0) {
	    if (split_rows) drawCell(g,i,ystep*idx + (ystep-ysize)/2,1,ysize,rctx,tups);
	    else drawCellV2(g,i,ystep*idx + (ystep-ysize)/2,1,ysize,rctx,tups);
	  }
	 if (draw_links) {
	    // create before draw allows links with the same time
	    createLinks(i,ystep*idx + ystep/2,ent.getKey(),ldata,lvla,lvlb);
	    drawLinks(g,i,ystep*idx + ystep/2,rctx,tups,rvla,rvlb);
	  }
	 ++idx;
       }
      t0 = t1;
    }

   if (draw_lines && ystep > 0) {
      double xend = sz.getWidth();
      g.setColor(Color.BLACK);
      Line2D l2d = new Line2D.Double();
      for (double y = ystep; y < sz.getHeight(); y += ystep) {
	 l2d.setLine(0,y,xend,y);
	 g.draw(l2d);
       }
    }
}



@Override protected DygraphValueContext getTimeContext()
{
   return display_handler.getContext(Selectors.ROWDATA);
}



private void drawCell(Graphics2D g,double x,double y0,double xsz,double ysz,
			 DygraphValueContext rctx,
			 DystoreRangeSet data)
{
   if (data == null || data.size() == 0) return;
   List<DystoreRangeSet> itms = display_handler.splitTuples(rctx,Selectors.ROWITEM,data);
   if (itms == null || itms.size() == 0) return;

   double isz = ysz / itms.size();

   Rectangle2D.Double r2 = new Rectangle2D.Double(x,y0,xsz,isz);

   int idx = 0;
   for (DystoreRangeSet idata : itms) {
      if (idata != null) {
	 Color c = display_handler.getColor(rctx,Selectors.HUE,Selectors.SAT,Selectors.VALUE,idata);
	 if (itms.size() > 1) {
	    r2.y = y0 + (idx*isz);
	  }
	 else {
	    double w = display_handler.getValue(rctx,Selectors.WIDTH,idata);
	    if (w == 0) continue;
	    r2.height = isz * w;
	    r2.y = y0 + (isz * (1.0 - w)) / 2.0;
	  }
	 g.setColor(c);
	 g.draw(r2);
       }
      ++idx;
    }
}



private void drawCellV2(Graphics2D g,double x,double y0,double xsz,double ysz,
			   DygraphValueContext rctx,
			   DystoreRangeSet data)
{
   if (data == null || data.size() == 0) return;
   List<DystoreRangeSet> itms = display_handler.splitTuples(rctx,Selectors.ROWITEM,data);
   if (itms == null || itms.size() == 0) return;

   double isz = ysz / itms.size();

   Rectangle2D.Double r2 = new Rectangle2D.Double(x,y0,xsz,isz);

   // background white
   g.setColor(Color.WHITE);
   g.draw(r2);

   double y1 = y0 + ysz * OVERVIEW_PART/2;
   double y2 = y0 + ysz * (1 - OVERVIEW_PART/2);

   // draw top and bottom portions
   Color c = display_handler.getColor(rctx,Selectors.HUE,Selectors.SAT,Selectors.VALUE,data);
   g.setColor(c);
   r2.height = ysz*OVERVIEW_PART/2;
   g.draw(r2);
   r2.y = y2;
   g.draw(r2);
   
   double scale = 1.0;
   double total = 0;
   for (DystoreRangeSet idata : itms) {
      double fract = display_handler.getValue(rctx,Selectors.ITEMWIDTH,idata);
      total += fract;
    }
   if (total > 1.0) scale = 1.0/total;

   
   r2.y = y1;
   for (DystoreRangeSet idata : itms) {
      double fract = display_handler.getValue(rctx,Selectors.ITEMWIDTH,idata);
      double yd = ysz * (1-OVERVIEW_PART) * fract * scale;
      r2.height = yd;
      c = display_handler.getColor(rctx,Selectors.ROWITEM,Selectors.SAT,Selectors.VALUE,idata);
      g.setColor(c);
      g.draw(r2);
      r2.y += yd;
    }

   Line2D.Double l2 = new Line2D.Double(x,y1,x+xsz,y1);
   g.setColor(Color.GRAY);
   g.draw(l2);
   l2.y1 = y2;
   l2.y2 = y2;
   g.draw(l2);
}



/********************************************************************************/
/*										*/
/*	Correlation methods							*/
/*										*/
/********************************************************************************/

String findTuple(double x,double y,Dimension sz)
{
   double t0 = getTimeAtPosition(x);
   double t1 = getTimeAtPosition(x+1);

   DygraphValueContext rctx = display_handler.getContext(Selectors.ROWDATA);
   rctx.setTimes(t0,t1);
   DystoreDataMap data = display_handler.nextTupleSet(rctx,Selectors.ROW,true,true,
							 display_handler.getYDataStart(),
							 display_handler.getYDataEnd(),null,null);

   int nelt = data.size();
   double ystep = sz.getHeight();
   ystep /= nelt;
   double ysize = ystep * (1.0 - ROW_SPACE);
   int idx = (int)(y / ystep);
   DystoreRangeSet r1 = null;
   int i = 0;
   for (DystoreRangeSet cdt : data.values()) {
      if (i++ == idx) {
	 r1 = cdt;
	 break;
       }
    }
   if (r1 == null) return null;

   double y0 = ystep*idx + (ystep-ysize)/2;

   DystoreRangeSet sel = null;

   List<DystoreRangeSet> itms = display_handler.splitTuples(rctx,Selectors.ROWITEM,r1);
   if (itms == null) return null;

   if (split_rows) {
      double isz = ysize / itms.size();
      int idx1 = (int)((y-y0)/isz);
      if (idx1 >= 0 && idx1 < itms.size()) sel = itms.get(idx1);
    }
   else {
      double v = (y-y0)/ysize - OVERVIEW_PART/2;
      v /= (1-OVERVIEW_PART);
      if (v >= 0 && v <= 1) {
	 double tot = 0;
	 for (DystoreRangeSet idata : itms) {
	    double fract = display_handler.getValue(rctx,Selectors.ITEMWIDTH,idata);
	    if (fract > 0 && v >= tot && v < tot+fract) {
	       sel = idata;
	       break;
	     }
	    tot += fract;
	  }
       }
    }

   String fmted = display_handler.formatTuples(rctx,r1,sel);

   return fmted;
}



/********************************************************************************/
/*										*/
/*	Link management methods 						*/
/*										*/
/********************************************************************************/

private void createLinks(double x,double y,String row,DystoreDataMap ldata,
			    DystoreField lvla,DystoreField lvlb)
{
   if (ldata == null) return;

   DystoreRangeSet lnks = ldata.get(row);
   if (lnks != null && lnks.size() > 0) {
      for (DystoreTuple lnk : lnks) {
	 String tval = null;
	 if (lvla != null) tval = lnk.getValue(lvla);
	 if (tval == null && lvlb != null) tval = lnk.getValue(lvlb);
	 if (tval == null) continue;
	 createLink(x,y,tval);
       }
    }
}



private void createLink(double x,double y,String v)
{
   if (v == null) return;

   if (active_links.get(v) != null) return;		// avoid duplicate links

   // System.err.println("DYG: CREATE LINK " + x + " " + y + " " + v);

   ActiveLink al = new ActiveLink(x,y);

   active_links.put(v,al);
}



private void drawLinks(Graphics2D g,double x,double y,DygraphValueContext dctx,
			  DystoreRangeSet tups,
			  DystoreField rvla,DystoreField rvlb)
{
   if (active_links.isEmpty()) return;

   Map<String,DystoreTuple> valuemap = new HashMap<String,DystoreTuple>();

   for (DystoreTuple dt : tups) {
      if (rvla != null) {
	 String v = dt.getValue(rvla);
	 if (v != null) {
	    if (!valuemap.containsKey(v)) valuemap.put(v,dt);
	  }
       }
      if (rvlb != null) {
	 String v = dt.getValue(rvlb);
	 if (v != null) {
	    if (!valuemap.containsKey(v)) valuemap.put(v,dt);
	  }
       }
    }

   for (Map.Entry<String,DystoreTuple> ent : valuemap.entrySet()) {
      String v = ent.getKey();
      ActiveLink l = active_links.remove(v);
      if (l != null) {
	 // System.err.println("DYG: DRAW LINK TO " + v + " " + x + " " + y);
	 DystoreRangeSet stup = new DystoreRangeSet(ent.getValue());
	 Color lc = display_handler.getColor(dctx,Selectors.LHUE,Selectors.LSAT,
						Selectors.LVALUE, Selectors.LALPHA,stup);
	 Color elc = display_handler.getColor(dctx,Selectors.LENDHUE,Selectors.LENDSAT,
						 Selectors.LENDVALUE, Selectors.LALPHA,stup);
	 drawLink(g,l,x,y,lc,elc);
       }
    }
}




private void drawLink(Graphics2D g,ActiveLink lnk,double tx,double ty,Color lc,Color elc)
{
   link_line.setLine(lnk.getX(),lnk.getY(),tx,ty);

   if (elc == null || elc.equals(lc)) {
      g.setColor(lc);
    }
   else {
      Paint p = new GradientPaint(link_line.getP1(),lc,link_line.getP2(),elc);
      g.setPaint(p);
    }

   g.draw(link_line);
}



private static class ActiveLink {

   private double source_x;
   private double source_y;

   ActiveLink(double x,double y) {
      source_x = x;
      source_y = y;
    }

   double getX()			{ return source_x; }
   double getY()			{ return source_y; }

}	// end of inner class ActiveLink




/********************************************************************************/
/*										*/
/*	Panel to hold the display						*/
/*										*/
/********************************************************************************/

private class TimeRowPanel extends JPanel {

   private static final long serialVersionUID = 1;

   TimeRowPanel() {
      setPreferredSize(new Dimension(600,400));
      setToolTipText("Time Row Display");
      setBackground(background_color);
      setOpaque(true);
    }

   public void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g;
      Dimension sz = getSize();
      drawTimeRows(g2,sz);
    }

   public String getToolTipText(MouseEvent evt) {
      int x = evt.getX();
      int y = evt.getY();
      return findTuple(x,y,getSize());
    }

}	// end of inner class TimeRowPanel




}	// end of class DygraphDisplayTimeRows




/* end of DygraphDisplayTimeRows.java */
