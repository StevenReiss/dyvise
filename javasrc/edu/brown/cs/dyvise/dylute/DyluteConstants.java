/********************************************************************************/
/*										*/
/*		DyluteConstants.java						*/
/*										*/
/*	Constants for DYnamic Lock UTilization Experiencer			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/dylute/src/DyluteConstants.java,v 1.1 2011-09-12 19:37:25 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyluteConstants.java,v $
 * Revision 1.1  2011-09-12 19:37:25  spr
 * Add dylute files to repository
 *
 *
 ********************************************************************************/


package edu.brown.cs.dyvise.dylute;

// NOTE: This should only use system libraries and libraries that are explicitly
//    in this package.	Using anything else might cause conflicts if the application
//    being monitored uses the same library.


interface DyluteConstants {


/********************************************************************************/
/*										*/
/*	Location representation 						*/
/*										*/
/********************************************************************************/

interface Location {

   String getClassName();
   String getMethodName();
   String getMethodSignature();
   int getMethodOffset();
   int getLockId();

}


/********************************************************************************/
/*										*/
/*	Monitoring routine definitions						*/
/*										*/
/********************************************************************************/

String		MONITOR_CLASS = "edu/brown/cs/dyvise/dylute/DyluteMonitor";
String		MONITOR_ENTER = "monEnter";
String		MONITOR_ENTER_ARGS = "(Ljava/lang/Object;I)V";
String		MONITOR_ENTERED = "monEntered";
String		MONITOR_ENTERED_ARGS = "(Ljava/lang/Object;)V";
String		MONITOR_EXIT = "monExit";
String		MONITOR_EXIT_ARGS = "(Ljava/lang/Object;)V";
String		MONITOR_NOTIFY = "monNotify";
String		MONITOR_NOTIFY_ARGS = "(Ljava/lang/Object;)V";
String		MONITOR_WAIT = "monWait";
String		MONITOR_WAIT_ARGS = "(Ljava/lang/Object;)V";
String		MONITOR_WAITED = "monWaited";
String		MONITOR_WAITED_ARGS = "(Ljava/lang/Object;)V";


}	// end of interface DyluteConstants




/* end of DyluteConstants.java */
