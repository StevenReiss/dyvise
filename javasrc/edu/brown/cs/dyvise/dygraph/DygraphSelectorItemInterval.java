/********************************************************************************/
/*										*/
/*		DygraphSelectorItemInterval.java				*/
/*										*/
/*	Interval used as a choice for a selector				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dygraph/DygraphSelectorItemInterval.java,v 1.5 2013-05-09 12:28:56 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DygraphSelectorItemInterval.java,v $
 * Revision 1.5  2013-05-09 12:28:56  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.4  2012-10-05 00:52:14  spr
 * Code cleanup
 *
 * Revision 1.3  2011-03-19 20:34:08  spr
 * Fix time block display and performance issues.
 *
 * Revision 1.2  2011-03-10 02:32:57  spr
 * Fixups for lock visualization.
 *
 * Revision 1.1  2010-03-31 17:41:54  spr
 * Add selection mechanism.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dygraph;

import edu.brown.cs.dyvise.dystore.*;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.util.*;



class DygraphSelectorItemInterval extends DygraphSelectorItem {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<DystoreTable,TableContext>	 context_map;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DygraphSelectorItemInterval(DygraphSelector ds,Element e)
{
   super(ds,e);

   context_map = new HashMap<DystoreTable,TableContext>();
}



/********************************************************************************/
/*										*/
/*	Data access methods							*/
/*										*/
/********************************************************************************/

@Override double getValue(DygraphValueContext ctx,DystoreRangeSet data)
{
   if (data.size() == 0) return 0;
   TableContext tctx = getTableContext(ctx);

   double t0 = ctx.getFromTime();
   double t1 = ctx.getToTime();
   if (t0 == t1) return 1.0/data.size();

   double total = 0;

   for (DystoreTuple dt : data) {
      total += tctx.getFraction(dt,t0,t1);
    }

   
   if (total > 1) {
      if (t1-t0 > 1 && total > 1.25) {
	 System.err.println("OVERFLOW: " + total + " " + t1 + " " + t0 + " " + (t1-t0));
	// /*************
	 for (DystoreTuple dt : data) {
	    double v = tctx.getFraction(dt,t0,t1);
	    System.err.println("   DATA " + v + " " + dt);
	  }
	// **********/
      }
      total = 1;
    }

   return total;
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

public String toString()			{ return "INTERVAL"; }

@Override void outputValue(IvyXmlWriter xw)
{
   xw.begin("ITEM");
   xw.field("TYPE","INTERVAL");
   xw.end("ITEM");
}



/********************************************************************************/
/*										*/
/*	Matching methods							*/
/*										*/
/********************************************************************************/

@Override boolean match(Element xml)
{
   return IvyXml.getAttrString(xml,"TYPE").equals("INTERVAL");
}




/********************************************************************************/
/*										*/
/*	Context management methods						*/
/*										*/
/********************************************************************************/

private TableContext getTableContext(DygraphValueContext ctx)
{
   DystoreTable tbl = ctx.getTable();

   TableContext tctx = context_map.get(tbl);
   if (tctx == null) {
      tctx = new TableContext(tbl);
      context_map.put(tbl,tctx);
    }

   return tctx;
}




/********************************************************************************/
/*										*/
/*	Holder of information for a table					*/
/*										*/
/********************************************************************************/

private static class TableContext {

   private DystoreField start_field;
   private DystoreField end_field;
   private DystoreField ival_field;

   TableContext(DystoreTable tbl) {
      start_field = null;
      end_field = null;
      ival_field = null;
   
      for (DystoreField df : tbl.getFields()) {
         switch (df.getType()) {
            case START_TIME :
               start_field = df;
               break;
            case END_TIME :
               end_field = df;
               break;
            case INTERVAL :
               ival_field = df;
               break;
            default :
               break;
          }
       }
    }

   double getFraction(DystoreTuple dt,double t0,double t1) {
      if (t0 == t1) return 1;
   
      double tx = t0;
      double ty = t1;
   
      if (start_field != null) {
         tx = dt.getTimeValue(start_field);
       }
      if (end_field != null) {
         ty = dt.getTimeValue(end_field);
       }
      else if (ival_field != null) {
         ty = tx + dt.getTimeValue(ival_field);
       }
   
      // if (tx == ty) ty += 0.5;			// handle round off
   
      if (tx < t0) tx = t0;
      if (ty > t1) ty = t1;
   
      return (ty-tx)/(t1-t0);
    }

}	// end of inner class TableContext




}	// end of class DygraphSelectorItemInterval




/* end of DygraphSelectorItemInterval.java */
