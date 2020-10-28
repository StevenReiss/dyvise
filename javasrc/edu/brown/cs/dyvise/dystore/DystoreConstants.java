/********************************************************************************/
/*										*/
/*		DystoreConstants.java						*/
/*										*/
/*	DYVISE storage constant definitions					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dystore/DystoreConstants.java,v 1.4 2013-05-09 12:29:03 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DystoreConstants.java,v $
 * Revision 1.4  2013-05-09 12:29:03  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.3  2011-03-10 02:33:19  spr
 * Code cleanup.
 *
 * Revision 1.2  2010-03-30 16:23:05  spr
 * Make the store efficient enough to use with the display.
 *
 * Revision 1.1  2009-09-19 00:14:00  spr
 * In memory tuple store for visualization.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dystore;



public interface DystoreConstants {



/********************************************************************************/
/*										*/
/*	Field Types								*/
/*										*/
/********************************************************************************/

enum FieldType {
   VOID,
   OBJECT,				// string repr of arbitrary object
   THREAD,				// thread from user program (string)
   STRING,
   INT, 				// long
   START_TIME,
   END_TIME,
   INTERVAL
}



/********************************************************************************/
/*										*/
/*	Range type								*/
/*										*/
/********************************************************************************/

interface ValueRange {

   double getMinValue();
   double getMaxValue();

}


/********************************************************************************/
/*										*/
/*	Callbacks for value update						*/
/*										*/
/********************************************************************************/

interface ValueCallback {

   public void valuesChanged(DystoreField f);

}


/********************************************************************************/
/*										*/
/*	Other constants 							*/
/*										*/
/********************************************************************************/

double	CURRENT_TIME =	Double.MAX_VALUE;



}	// end of interface DystoreConstants




/* end of DystoreConstants.java */


