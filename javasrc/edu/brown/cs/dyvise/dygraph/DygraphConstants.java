/********************************************************************************/
/*										*/
/*		DygraphConstants.java						*/
/*										*/
/*	DYVISE graphics (visualization) definitions				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dygraph/DygraphConstants.java,v 1.3 2013-05-09 12:28:56 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DygraphConstants.java,v $
 * Revision 1.3  2013-05-09 12:28:56  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.2  2010-03-30 16:20:44  spr
 * Fix bugs and features in graphical output.
 *
 * Revision 1.1  2009-09-19 00:08:37  spr
 * Module to draw various types of displays.  Only time rows implemented for now.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dygraph;



public interface DygraphConstants {



/********************************************************************************/
/*										*/
/*	Special names								*/
/*										*/
/********************************************************************************/

String	DYGRAPH_ALL = "*";



/********************************************************************************/
/*										*/
/*	Selector types								*/
/*										*/
/********************************************************************************/

enum SelectorType {
   GENERIC,
   TABLE,				// selection of a table
   STRING,				// string value
   SET, 				// set w/ arbitrary sort order
   SORTED_SET,				// selection of a set w/ sort order
   VALUE,				// value between 0 and 1
   BOOLEAN				// boolean value
}



enum SelectorOp {
   NONE,
   AVERAGE,			// average tuple values
   MAX, 				// use max tuple value
   MIN, 				// use min tuple value
   COUNT,				// use # of tuple values
   INTERVAL,			// total time as fraction of interval
   MODE
}


enum SelectorSortOp {
   SORT_NAME,				// sort results alphabetically
   SORT_TIME				// sort results by creation order
}



}	// end of interface DygraphConstants




/* end of DygraphConstants.java */



