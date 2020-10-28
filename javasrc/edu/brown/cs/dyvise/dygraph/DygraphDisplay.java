/********************************************************************************/
/*										*/
/*		DygraphDisplay.java						*/
/*										*/
/*	DYVISE graphics (visualization) visualization implementation		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dygraph/DygraphDisplay.java,v 1.4 2013-05-09 12:28:56 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DygraphDisplay.java,v $
 * Revision 1.4  2013-05-09 12:28:56  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.3  2011-03-10 02:32:56  spr
 * Fixups for lock visualization.
 *
 * Revision 1.2  2009-10-07 00:59:44  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.1  2009-09-19 00:08:37  spr
 * Module to draw various types of displays.  Only time rows implemented for now.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dygraph;

import javax.swing.JPanel;

import java.util.HashMap;
import java.util.Map;



abstract class DygraphDisplay implements DygraphConstants {



/********************************************************************************/
/*										*/
/*	Static storage								*/
/*										*/
/********************************************************************************/

private static Map<String,DygraphDisplay> display_map;

static {
   display_map = new HashMap<String,DygraphDisplay>();
   display_map.put("TIMEROWS",new DygraphDisplayTimeRows());
   display_map.put("TIMEBLOCKS",new DygraphDisplayTimeBlocks());
}



/********************************************************************************/
/*										*/
/*	Static creation methods 						*/
/*										*/
/********************************************************************************/

static DygraphDisplay getDisplay(String typ,DygraphDisplayHandler ddh)
{
   DygraphDisplay dd0 = display_map.get(typ);

   if (dd0 == null) return null;

   return dd0.createNew(ddh);
}



protected abstract DygraphDisplay createNew(DygraphDisplayHandler ddh);




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Class<? extends Enum<?>>	selector_class;
protected DygraphDisplayHandler 	display_handler;
protected JPanel			display_panel;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected DygraphDisplay(Class<? extends Enum<?>> cls)
{
   selector_class = (Class<? extends Enum<?>>)cls;
   display_handler = null;
   display_panel = null;
}


protected DygraphDisplay(DygraphDisplay dd,DygraphDisplayHandler ddh)
{
   selector_class = dd.selector_class;
   display_handler = ddh;
   display_panel = null;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

synchronized JPanel getDisplayPanel()
{
   if (display_panel == null) {
      display_panel = setupDisplayPanel();
    }
   return display_panel;
}

JPanel checkDisplayPanel()				{ return display_panel; }



protected abstract JPanel setupDisplayPanel();


Class<? extends Enum<?>> getSelectorEnums()		{ return selector_class; }

abstract String getSelectorDescription(Enum<?> s);
abstract SelectorType getSelectorType(Enum<?> s);

void handleDataUpdated()                                { }


		

}	// end of abstract class DygraphDisplay



/* end of DygraphDisplay.java */

