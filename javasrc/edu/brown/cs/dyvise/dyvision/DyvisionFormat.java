/********************************************************************************/
/*										*/
/*		DyvisionFormat.java						*/
/*										*/
/*	Methods for formatting values for output appropriately			*/
/*										*/
/********************************************************************************/
/*	Copyright 2007 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2007, Brown University, Providence, RI.				 *
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyvision/DyvisionFormat.java,v 1.4 2009-09-19 00:14:57 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyvisionFormat.java,v $
 * Revision 1.4  2009-09-19 00:14:57  spr
 * UPdate front end to clean up tables.
 *
 * Revision 1.3  2009-04-28 18:01:26  spr
 * Add graphs to time lines.
 *
 * Revision 1.2  2009-04-11 23:47:31  spr
 * Handle formating using IvyFormat.
 *
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dyvision;


import edu.brown.cs.ivy.file.IvyFormat;



class DyvisionFormat implements DyvisionConstants {




/********************************************************************************/
/*										*/
/*	General formatting methods						*/
/*										*/
/********************************************************************************/

static String outputValue(double v,ValueType vt)
{
   String r;

   switch (vt) {
      case NUMBER :
	 r = IvyFormat.formatNumber(v);
	 break;
      default :
      case STRING :
	 r = Double.toString(v);
	 break;
      case PERCENT :
	 r = IvyFormat.formatPercent(v);
	 break;
      case TIME :
	 r = IvyFormat.formatTime(v);
	 break;
      case MSTIME :
	 r = IvyFormat.formatTime(v*1000.0);
	 break;
      case USTIME :
	 r = IvyFormat.formatTime(v*1000000.0);
	 break;
      case MEMORY :
	 r = IvyFormat.formatMemory(v);
	 break;
      case COUNT :
	 r = IvyFormat.formatCount(v);
	 break;
      case LINE :
	 r = Integer.toString((int) v);
	 break;
      case INTERVAL :
	 r = IvyFormat.formatInterval(v/1000.0);
	 break;
      case MSINTERVAL :
	 r = IvyFormat.formatInterval(v);
	 break;
      case USINTERVAL :
	 r = IvyFormat.formatInterval(v*1000.0);
	 break;
    }

   return r;
}



}	// end of class DyvisionFormat




/* end of DyvisionFormat.java */
