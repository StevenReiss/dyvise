/********************************************************************************/
/*										*/
/*		DyvisionConstants.java						*/
/*										*/
/*	Constants for dyper performance evaluation interface			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyvision/DyvisionConstants.java,v 1.6 2009-10-07 01:00:24 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyvisionConstants.java,v $
 * Revision 1.6  2009-10-07 01:00:24  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.5  2009-09-19 00:14:57  spr
 * UPdate front end to clean up tables.
 *
 * Revision 1.4  2009-06-04 18:55:11  spr
 * Handle bad trace directory.
 *
 * Revision 1.3  2009-03-20 02:09:09  spr
 * Add memory view and related controls. Clean code.
 *
 * Revision 1.2  2008-11-12 14:11:20  spr
 * Clean up the output and bug fixes.
 *
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dyvision;


import edu.brown.cs.dyvise.dyvise.DyviseConstants;
import edu.brown.cs.dyvise.dymon.DymonConstants;
import edu.brown.cs.ivy.file.IvyFile;



public interface DyvisionConstants extends DymonConstants, DyviseConstants {


/********************************************************************************/
/*										*/
/*	File locations								*/
/*										*/
/********************************************************************************/

String DYVISION_TABLE_FILE = IvyFile.expandName("$(DYVISE)/lib/dyvisiontables.xml");

String DYVISION_TRACE_FILE = IvyFile.expandName("$(TRACE_DIR)/DYVIS_");




/********************************************************************************/
/*										*/
/*	Timing constants							*/
/*										*/
/********************************************************************************/

long	DYVISION_PROCESS_UPDATE = 2500L;

long	DYVISION_VIEW_UPDATE = 2000L;

long	DYVISION_MAX_FAIL = 10;



/********************************************************************************/
/*										*/
/*	Value types for meters, graphs and tables				*/
/*										*/
/********************************************************************************/

enum ValueType {
   NUMBER,
   STRING,
   PERCENT,
   TIME,			// seconds
   MSTIME,			// milliseconds
   USTIME,			// microseconds
   MEMORY,
   COUNT,
   INTERVAL,			// seconds
   MSINTERVAL,			// milliseconds
   USINTERVAL,			// microseconds
   LINE,			// line number
   BOOLEAN
};



}	// end of interface DyvisionConstants




/* end of DyvisionConstants.java */
