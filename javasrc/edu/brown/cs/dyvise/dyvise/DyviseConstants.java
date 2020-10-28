/********************************************************************************/
/*										*/
/*		DyviseConstants.java						*/
/*										*/
/*	DYVISE global constant definitions					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyvise/DyviseConstants.java,v 1.3 2010-03-30 16:23:41 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyviseConstants.java,v $
 * Revision 1.3  2010-03-30 16:23:41  spr
 * Minor changes to accommodate different database systems.
 *
 * Revision 1.2  2009-10-07 01:00:23  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.1  2009-09-19 00:14:44  spr
 * Common files for use throughout the system.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dyvise;


import edu.brown.cs.ivy.file.IvyFile;



public interface DyviseConstants {


/********************************************************************************/
/*										*/
/*	File locations								*/
/*										*/
/********************************************************************************/

String DYVISE_PROJECT_DIRECTORY = IvyFile.expandName("$(HOME)/.dyvise/Projects");
String DYVISE_VISUALIZATION_DIRECTORY = IvyFile.expandName("$(HOME)/.dyvise/Visualizations");

String DYVISE_CURRENT_PROJECT = ".currentproject";
String DYVISE_PROJECT_EXTENSION = ".dyproject";
String DYVISE_VISUALIZATION_EXTENSION = ".dyviz";

String DYVISE_ICON_DIRECTORY = IvyFile.expandName("$(DYVISE)/lib/images/");



/********************************************************************************/
/*										*/
/*	Database definitions							*/
/*										*/
/********************************************************************************/

String DYVISE_DATABASE_NAME = "dyview_$(USER)_$(PROJECT)";

String DYSTATIC_UPDATE_LABEL = "STATIC";

String DYMAC_THREAD_STATE_TABLE = "DynThreadStates";
String DYMAC_THREAD_STATE_FIELDS = "class text, method text, lineno int, thread text, " +
					"type text, lineinfo text";

String DYMAC_REACTION_TABLE = "DynEventRoutines";
String DYMAC_REACTION_FIELDS = "class text, method text, thread text, type text";




/********************************************************************************/
/*										*/
/*	Event parameters							*/
/*										*/
/********************************************************************************/

enum EventParam {
   NONE,
   CONST,
   TIME,
   P0,
   P1,
   P2
}



/********************************************************************************/
/*										*/
/*	Monitoring overheads							*/
/*										*/
/********************************************************************************/

double DYVISE_ANALYSIS_OVERHEAD = 0.05;
double DYVISE_RUNNING_OVERHEAD = 0.05;



/********************************************************************************/
/*										*/
/*	Time management for synchronizing views 				*/
/*										*/
/********************************************************************************/

public interface TimeListener {

   void handleTimeSet(long when);		// time has been set
   void handleMark(long when,String what);	// mark has been added

}



}	// end of interface DyviseConstants



/* end of DyviseConstants.java */



