/********************************************************************************/
/*										*/
/*		DyperConstants.java						*/
/*										*/
/*	Constants for dyper performance evaluator				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyper/DyperConstants.java,v 1.3 2016/11/02 18:59:19 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyperConstants.java,v $
 * Revision 1.3  2016/11/02 18:59:19  spr
 * Move to asm5
 *
 * Revision 1.2  2009-09-19 00:13:28  spr
 * Update agents for dynamic insertion and removal.
 *
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dyper;



public interface DyperConstants {


/********************************************************************************/
/*										*/
/*	Messaging constants							*/
/*										*/
/********************************************************************************/

String		DYPER_MESSAGE_BUS = "DYVISE_" + System.getProperty("user.name");

String		DYPER_PATCH_DIRECTORY = PathComputer.dyvise() + "/tmp/dyper";
String		DYPER_PATCH_JAR = PathComputer.dyvise() + "/lib/dyper.jar";

String		DYPER_TRAILER = "ENDDYPERMSG";
String		DYPER_REPLY_TRAILER = "ENDDYPERREPLY";



/********************************************************************************/
/*										*/
/*	Timer constants 							*/
/*										*/
/********************************************************************************/

long		DYPER_CHECK_TIME = 10;
long		DYPER_DISABLE_TIME = 5000;
long		DYPER_REPORT_TIME = 0;

long		DYPER_MAX_DELAY = 10000;



/********************************************************************************/
/*										*/
/*	Thresholds								*/
/*										*/
/********************************************************************************/

double		CLASS_DETAIL_MIN_TOTAL_INIT = 250.0;
double		CLASS_DETAIL_MINIMUM_INIT = 10.0;
double		CLASS_DETAIL_THRESHOLD_INIT = 0.025;
double		CLASS_DETAIL_STOP_THRESHOLD_INIT = 0.005;


double		METHOD_DETAIL_MIN_TOTAL_INIT = 250.0;
double		METHOD_DETAIL_MINIMUM_INIT = 10.0;
double		METHOD_DETAIL_THRESHOLD_INIT = 0.025;
double		METHOD_DETAIL_STOP_THRESHOLD_INIT = 0.005;



enum CounterParameter {
   DETAIL_ALL,
   CLASS_DETAIL_MIN_TOTAL,
   CLASS_DETAIL_MINIMUM,
   CLASS_DETAIL_THRESHOLD,
   CLASS_DETAIL_STOP_THRESHOLD,
   METHOD_DETAIL_MIN_TOTAL,
   METHOD_DETAIL_MINIMUM,
   METHOD_DETAIL_THRESHOLD,
   METHOD_DETAIL_STOP_THRESHOLD,
   MIN_COUNTER
}



/********************************************************************************/
/*										*/
/*	Class types								*/
/*										*/
/********************************************************************************/

enum ClassType {
   NORMAL,
   SYSTEM,
   IO,
   SYSTEM_IO,
   COLLECTION,
   SYSTEM_COLLECTION;

   public boolean isIO() {
      return this == IO || this == SYSTEM_IO;
    }

   public boolean isCOLLECTION() {
      return this == IO || this == SYSTEM_COLLECTION;
    }

   public boolean isSYSTEM() {
      return this == SYSTEM || this == SYSTEM_IO || this == SYSTEM_COLLECTION;
    }

}	// end of enum ClassType



/********************************************************************************/
/*										*/
/*	Report types								*/
/*										*/
/********************************************************************************/

enum ReportType {
   ALL
};



/********************************************************************************/
/*										*/
/*	Instrumentation constants						*/
/*										*/
/********************************************************************************/

int DYPER_MAX_THREADS = 32768;		// max thread ID is one less than this
int DYPER_MAX_SOURCES = 64;		// max sources for allocation

int DYPER_MAX_DEPTH = 128;


class PathComputer {

   static String dyvise() {
      String s = System.getProperty("edu.brown.cs.dyvise.DYVISE");
      if (s != null) return s;
      s = System.getenv("BROWN_DYVISE_DYVISE");
      if (s != null) return s;
      return ".";
    }
}


}	// end of interface DyperConstants



/* end of DyperConstants.java */

