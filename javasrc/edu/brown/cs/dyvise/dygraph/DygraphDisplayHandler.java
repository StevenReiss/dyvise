/********************************************************************************/
/*										*/
/*		DygraphDisplayHandler.java					*/
/*										*/
/*	DYVISE graphics (visualization) viewer callbacks			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dygraph/DygraphDisplayHandler.java,v 1.7 2013-05-09 12:28:56 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DygraphDisplayHandler.java,v $
 * Revision 1.7  2013-05-09 12:28:56  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.6  2012-10-05 00:52:14  spr
 * Code cleanup
 *
 * Revision 1.5  2011-03-19 20:34:07  spr
 * Fix time block display and performance issues.
 *
 * Revision 1.4  2011-03-10 02:32:56  spr
 * Fixups for lock visualization.
 *
 * Revision 1.3  2010-03-30 16:20:44  spr
 * Fix bugs and features in graphical output.
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

import edu.brown.cs.dyvise.dystore.*;

import java.awt.Color;
import java.util.*;



interface DygraphDisplayHandler {


DystoreTable getTable(Enum<?> tblkey);
DygraphValueContext getContext(Enum<?> tblkey);
DystoreField getField(DystoreTable tbl,Enum<?> fldkey);
DystoreField getField(DygraphValueContext ctx,Enum<?> fldkey);
boolean getBoolean(Enum<?> tblkey);

double getStartTime();
double getEndTime();
double getTimeSpan();
double getTimeAtDelta(double d);

double getXDataStart();
double getXDataEnd();
double getYDataStart();
double getYDataEnd();

DystoreDataMap nextTupleSet(DygraphValueContext ctx,Enum<?> key,
			       boolean prune,boolean first,
			       double dstart,double dend,
			       DystoreDataMap data,
			       DystoreDataMap rdata);

double getValue(DygraphValueContext ctx,Enum<?> key,DystoreRangeSet data);
boolean getBoolean(DygraphValueContext ctx,Enum<?> key,DystoreRangeSet data);

List<DystoreRangeSet> splitTuples(DygraphValueContext ctx,Enum<?> key,DystoreRangeSet data);

Color getColor(DygraphValueContext ctx,Enum<?> hkey,Enum<?> skey,Enum<?> vkey,DystoreRangeSet data);

Color getColor(DygraphValueContext ctx,Enum<?> hkey,Enum<?> skey,Enum<?> vkey,Enum<?> akey,DystoreRangeSet data);

String formatTuples(DygraphValueContext ctx,
		       DystoreRangeSet tuples,
		       DystoreRangeSet selected);

DystoreConstants.ValueRange getValueRange(DystoreField fld);
Collection<String> getValueSet(DystoreField fld);



}	// end of interface DygraphDisplayHandler



/* end of DygraphDisplayHandler.java */


