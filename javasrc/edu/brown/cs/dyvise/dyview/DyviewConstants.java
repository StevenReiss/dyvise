/********************************************************************************/
/*										*/
/*		DyviewConstants.java						*/
/*										*/
/*	DYVISE viewer constant definitions					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyview/DyviewConstants.java,v 1.2 2010-03-30 16:23:25 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyviewConstants.java,v $
 * Revision 1.2  2010-03-30 16:23:25  spr
 * Upgrades to the visualizer to handle new programs and provide better views.
 *
 * Revision 1.1  2009-09-19 00:14:23  spr
 * Viewer with dialog based front end.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dyview;


import edu.brown.cs.dyvise.dyvise.DyviseConstants;
import edu.brown.cs.ivy.file.IvyFile;


interface DyviewConstants extends DyviseConstants {



/********************************************************************************/
/*										*/
/*	Patching names								*/
/*										*/
/********************************************************************************/

String DYVIEW_PATCH_AGENT = "EVENTS";



/********************************************************************************/
/*										*/
/*	Files									*/
/*										*/
/********************************************************************************/

String DYVIEW_DATABASE_SETUP = IvyFile.expandName("$(DYVISE)/lib/dyviewcreatedb.sql");

String DYVIEW_STATIC_COMPUTE = IvyFile.expandName("$(DYVISE)/lib/dyviewstatic.dycomp");



/********************************************************************************/
/*										*/
/*	Timings 								*/
/*										*/
/********************************************************************************/

double DYVIEW_OVERHEAD = 0.05;




/********************************************************************************/
/*										*/
/*	Callbacks								*/
/*										*/
/********************************************************************************/

interface ModelListener {

   void projectChanged();

   void startClassChanged();

   void visualChanged();

   void visualReadyChanged();

   void dataUpdated();

}


}	// end of interface DyviewConstants




/* end of DyviewConstants.java */

