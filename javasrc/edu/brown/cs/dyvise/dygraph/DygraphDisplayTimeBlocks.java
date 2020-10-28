/********************************************************************************/
/*										*/
/*		DygraphDisplayTimeBlocks.java					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dygraph/DygraphDisplayTimeBlocks.java,v 1.5 2013/09/04 18:35:13 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DygraphDisplayTimeBlocks.java,v $
 * Revision 1.5  2013/09/04 18:35:13  spr
 * Code cleanup.
 *
 * Revision 1.4  2013-05-09 12:28:56  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.3  2012-10-05 00:52:14  spr
 * Code cleanup
 *
 * Revision 1.2  2011-03-19 20:34:07  spr
 * Fix time block display and performance issues.
 *
 * Revision 1.1  2011-03-10 02:36:39  spr
 * Add time block display.
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
import java.awt.image.*;
import java.util.*;
import java.util.List;


class DygraphDisplayTimeBlocks extends DygraphDisplayTimed {



/********************************************************************************/
/*										*/
/*	Constants								*/
/*										*/
/********************************************************************************/

enum Selectors {
   ROWDATA,		// table to use for rows
   ROW, 		// field to define the row for a block
   HUE, 		// field to define color for a block
   SAT,
   VALUE,
   ALPHA,
   LEVEL,		// field to define min level for a block
   FILL,		// field to define fill style for a block
   CENTER		// center line location
}

private final static double ROW_SPACE = 0.2;
private final static double OVERVIEW_PART = 0.5;

private boolean 	draw_lines = true;
private Color		background_color = Color.WHITE;








/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BufferedImage	texture_image;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DygraphDisplayTimeBlocks()
{
   super(Selectors.class);

   texture_image = null;
}



private DygraphDisplayTimeBlocks(DygraphDisplayTimeBlocks proto,DygraphDisplayHandler ddh)
{
   super(proto,ddh);

   texture_image = null;
}




/********************************************************************************/
/*										*/
/*	Construction methods							*/
/*										*/
/********************************************************************************/

protected DygraphDisplay createNew(DygraphDisplayHandler ddh)
{
   return new DygraphDisplayTimeBlocks(this,ddh);
}



/********************************************************************************/
/*										*/
/*	Graphics setup methods							*/
/*										*/
/********************************************************************************/

protected JPanel createDisplayPanel()
{
   return new TimeBlockPanel();
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
      case ROW :
	 rslt = "Row items";
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
      case LEVEL :
	 rslt = "Block level";
	 break;
      case FILL :
	 rslt = "Block fill";
	 break;
      case CENTER :
	 rslt = "Center line";
	 break;
      case ALPHA :
	 rslt = "Transparency";
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
	 st = SelectorType.TABLE;
	 break;
      case ROW :
	 st = SelectorType.SORTED_SET;
	 break;
      case HUE :
      case SAT :
      case VALUE :
      case LEVEL :
      case FILL :
      case CENTER :
	 st = SelectorType.VALUE;
	 break;
      case ALPHA :
	 break;
    }

   return st;
}



/********************************************************************************/
/*										*/
/*	Drawing methods 							*/
/*										*/
/********************************************************************************/

private void drawTimeBlocks(Graphics2D g,Dimension sz)
{
   Color c = g.getColor();
   g.setColor(background_color);
   g.fillRect(0,0,sz.width,sz.height);
   g.setColor(c);

   DystoreDataMap data = null;
   double ystep = 0;
   double ysize = 0;

   DygraphValueContext rctx = display_handler.getContext(Selectors.ROWDATA);

   double c0 = display_handler.getValue(rctx,Selectors.CENTER,null);
   DystoreField lvlf = display_handler.getField(rctx,Selectors.LEVEL);
   DystoreConstants.ValueRange rng = display_handler.getValueRange(lvlf);
   if (rng.getMinValue() != rng.getMaxValue()) {
      c0 = (c0 - rng.getMinValue())/(rng.getMaxValue() - rng.getMinValue());
    }

   double t0 = getTimeAtPosition(0);
   for (int i = 0; i < sz.getWidth(); ++i) {
      double t1 = getTimeAtPosition(i+1);
      rctx.setTimes(t0,t1);
      data = display_handler.nextTupleSet(rctx,Selectors.ROW,true,
					     (i == 0),
					     display_handler.getYDataStart(),
					     display_handler.getYDataEnd(),data,null);
      if (ystep == 0) {
	 int nelt = data.size();
	 ystep = sz.getHeight();
	 ystep /= nelt;
	 ysize = ystep * (1.0 - ROW_SPACE);
       }
      int idx = 0;
      for (Map.Entry<String,DystoreRangeSet> ent : data.entrySet()) {
	 DystoreRangeSet tups = ent.getValue();
	 drawCellV2(g,i,ystep*idx + (ystep-ysize)/2,1,ysize,rctx,c0,tups);
	 ++idx;
       }
      // draw center line for item here
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



private void drawCellV2(Graphics2D g,double x,double y0,double xsz,double ysz,
      DygraphValueContext rctx,double ctr,DystoreRangeSet data)
{
   if (data == null || data.size() == 0) return;

   List<DystoreRangeSet> itms = display_handler.splitTuples(rctx,Selectors.LEVEL,data);
   if (itms == null || itms.size() == 0) return;

   DystoreField fillf = display_handler.getField(rctx,Selectors.FILL);
   DystoreConstants.ValueRange rng = display_handler.getValueRange(fillf);
   double mnv = rng.getMinValue();
   double mxv = rng.getMaxValue();

   double isz = ysz / itms.size();

   Rectangle2D.Double r2 = new Rectangle2D.Double(x,y0,xsz,isz);

   // background white
   g.setColor(Color.WHITE);
   g.draw(r2);

   double y1 = y0;

   r2.y = y1;
   for (DystoreRangeSet idata : itms) {
      if (idata.size() > 0) {
	 Color c = display_handler.getColor(rctx,Selectors.HUE,Selectors.SAT,Selectors.VALUE,
					       Selectors.ALPHA,idata);
	 double v0 = display_handler.getValue(rctx,Selectors.FILL,idata);
	 int fv = (int)(v0 * (mxv - mnv) + mnv + 0.5);
	 Paint p = createTexture(c,fv);
	 g.setPaint(p);
	 g.draw(r2);
       }
      r2.y  += isz;
    }

   if (ctr >= 0 && ctr <= 1) {
      double yc = y0 + ctr * ysz;
      Line2D.Double l2 = new Line2D.Double(x,yc,x+xsz,yc);
      g.setColor(Color.GRAY);
      g.draw(l2);
    }
}



private static final int IMAGE_SIZE = 8;

private Paint createTexture(Color c,int tv)
{
   if (tv == 0) return c;
   // if (tv != 0) return c;	// ignore for now

   if (texture_image == null) {
      texture_image = new BufferedImage(IMAGE_SIZE,IMAGE_SIZE,BufferedImage.TYPE_INT_ARGB);
    }

   Graphics2D g2 = texture_image.createGraphics();
   g2.setColor(c);
   g2.fillRect(0,0,IMAGE_SIZE,IMAGE_SIZE);
   g2.setColor(Color.BLACK);

   switch (tv) {
      default :
      case 0 :
	 break;
      case 1 :
	 for (int i = 0; i < IMAGE_SIZE; i += 4) {
	    g2.drawLine(0,i,IMAGE_SIZE-i,IMAGE_SIZE);
	    if (i != 0) g2.drawLine(i,0,IMAGE_SIZE,IMAGE_SIZE-i);
	  }
	 break;
      case 2 :
	 for (int i = 0; i < IMAGE_SIZE; i += 4) {
	    g2.drawLine(IMAGE_SIZE,i,i,IMAGE_SIZE);
	    if (i != 0) g2.drawLine(i,0,0,i);
	  }
	 break;
      case 3 :
	 for (int i = 0; i < IMAGE_SIZE; i += 4) {
	    g2.drawLine(0,i,IMAGE_SIZE-i,IMAGE_SIZE);
	    if (i != 0) g2.drawLine(i,0,IMAGE_SIZE,IMAGE_SIZE-i);
	    g2.drawLine(IMAGE_SIZE,i,i,IMAGE_SIZE);
	    if (i != 0) g2.drawLine(i,0,0,i);
	  }
	 break;
    }

   TexturePaint tp = new TexturePaint(texture_image,new Rectangle(0,0,IMAGE_SIZE,IMAGE_SIZE));

   return tp;
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

   double v = (y-y0)/ysize - OVERVIEW_PART/2;
   v /= (1-OVERVIEW_PART);
   if (v >= 0 && v <= 1) {
      double tot = 0;
      double fract = 1.0; // display_handler.getValue(rctx,Selectors.ITEMWIDTH,r1);
      if (fract > 0 && v >= tot && v < tot+fract) {
	 sel = r1;
       }
      else {
	 tot += fract;
       }
    }

   String fmted = display_handler.formatTuples(rctx,r1,sel);

   return fmted;
}




/********************************************************************************/
/*										*/
/*	Panel to hold the display						*/
/*										*/
/********************************************************************************/

private class TimeBlockPanel extends JPanel {

   private static final long serialVersionUID = 1;

   TimeBlockPanel() {
      setPreferredSize(new Dimension(600,400));
      setToolTipText("Time Row Display");
      setBackground(background_color);
      setOpaque(true);
    }

   public void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g;
      Dimension sz = getSize();
      drawTimeBlocks(g2,sz);
    }

   public String getToolTipText(MouseEvent evt) {
      int x = evt.getX();
      int y = evt.getY();
      return findTuple(x,y,getSize());
    }

}	// end of inner class TimeBlockPanel




}	// end of class DygraphDisplayTimeBlocks




/* end of DygraphDisplayTimeBlocks.java */
