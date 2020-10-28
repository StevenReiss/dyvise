/********************************************************************************/
/*										*/
/*		DymonConstants.java						*/
/*										*/
/*	DYPER monitor interface and agents constant definitions 		*/
/*										*/
/********************************************************************************/
/*	Copyright 2005 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2005, Brown University, Providence, RI.				 *
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymon/DymonConstants.java,v 1.9 2016/11/02 18:59:13 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymonConstants.java,v $
 * Revision 1.9  2016/11/02 18:59:13  spr
 * Move to asm5
 *
 * Revision 1.8  2013-08-05 12:03:36  spr
 * Updates; use dypatchasm.
 *
 * Revision 1.7  2010-03-30 16:22:05  spr
 * Clean up the code.
 *
 * Revision 1.6  2009-09-19 00:09:46  spr
 * Update dymon for seletive agent insertion and removal.
 *
 * Revision 1.5  2009-06-04 18:53:50  spr
 * Set up for binary distribution.
 *
 * Revision 1.4  2009-03-20 02:06:50  spr
 * Update dymon: add time-based information, updated memory, etc.
 *
 * Revision 1.3  2008-11-24 23:38:03  spr
 * Update phaser to have summary.
 *
 * Revision 1.2  2008-11-12 14:10:44  spr
 * Various efficiency and bug fixups.  Readiness for immediate output.
 *
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymon;


import edu.brown.cs.dyvise.dyper.DyperConstants;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.io.File;


public interface DymonConstants extends DyperConstants {



/********************************************************************************/
/*										*/
/*	File Locations								*/
/*										*/
/********************************************************************************/

String DYMON_UI_SERVER_SOCKET = IvyFile.expandName("$(HOME)/.dymon_ui");
String DYMON_UI_WEB_SERVER_SOCKET = IvyFile.expandName("$(BROWN_DYMON_WEB)/.dymon_ui");
String DYMON_UI_EOM = "***EOM***";

String DYMON_RESOURCE_FILE = IvyFile.expandName("$(HOME)/.dyperrc");
String DYMON_GLOBAL_RESOURCE_FILE = IvyFile.expandName("$(DYVISE)/lib/dymonrc.xml");
String DYMON_RESOURCE_FILE_NAME = IvyFile.expandName(".dyperrc");

String DYMON_PATCH_COMMAND = IvyFile.expandName("$(DYVISE)/bin/startdypatch");
String DYMON_PATCH_CLASS = "edu.brown.cs.dyvise.dypatchasm.DypatchMain";


String DYMON_LOCK_FILE = IvyFile.expandName(DYPER_PATCH_DIRECTORY + File.separator + "dymon_$(USER).lock");

String DYMON_REMOTE_ATTACH = IvyFile.expandName("$(DYVISE)/bin/dymonremoteattach");

String DYMON_REMOTE_COMMAND = IvyFile.expandName("$(DYVISE)/bin/startdymon");
String DYMON_REMOTE_CLASS = "edu.brown.cs.dyvise.dymon.DymonMain";

String DYMON_TRACE_PREFIX = IvyFile.expandName("$(TRACE_DIR)/DYMON_");
String DYMON_HEAP_PREFIX = IvyFile.expandName("$(TRACE_DIR)/DYHEAP_");

String DYMON_ATTACH_ARGS = IvyFile.expandName("DYVISE=$(DYVISE):IVY=$(IVY):DJARCH=$(BROWN_JAVA_ARCH)");

String DYMON_DYPER_TRAILER = DyperConstants.DYPER_TRAILER;
String DYMON_DYPER_REPLY_TRAILER = DyperConstants.DYPER_REPLY_TRAILER;





/********************************************************************************/
/*										*/
/*	Timing Constants							*/
/*										*/
/********************************************************************************/

long  DYMON_CHECK_PROCS_LOCAL = 	10*1000;
long  DYMON_CHECK_PROCS_REMOTE =	60*1000;

long  DYMON_CHECK_PROCS_EVERY = 	60*1000;
long  DYMON_CHECK_PROCS_FIRST = 	1*1000;




/********************************************************************************/
/*										*/
/*	Constants for managing overhead 					*/
/*										*/
/********************************************************************************/

double DYMON_DEFAULT_OVERHEAD = 	0.10;		// 0.01 when fully working

int    DYMON_REPORT_CHECK_EVERY =	15000;
int    DYMON_REPORT_CHECK_FIRST =	5000;

long	DYMON_MIN_CHECK_TIME =		10;
long	DYMON_MIN_REPORT_TIME = 	200;

double DYMON_REPORTING_FRACTION = 0.1;		// report vs check




/********************************************************************************/
/*										*/
/*	Agent states								*/
/*										*/
/********************************************************************************/

enum AgentState {
   IDLE,			// not installed
   ACTIVE,			// installed, being used
   PASSIVE,			// installed, not used
   DEAD
}



/********************************************************************************/
/*										*/
/*	Detail/patching definitions						*/
/*										*/
/********************************************************************************/

int	PATCH_PRIORITY_HIGH = 0;
int	PATCH_PRIORITY_NORMAL = 10;
int	PATCH_PRIORITY_LOW = 20;



enum PatchOverlap {
   NONE,			// no overlap permitted
   CLASS,			// overlap if no class conflicts
   ANY				// always allow overlap
}


interface DetailDescriptor {

   double getPriority();
   long getInterval();
   long getOverhead();
   double getSlowdown();

   DymonPatchRequest getPatchRequest(int priority);

}	// end of subinterface DetailDescriptor



/********************************************************************************/
/*										*/
/*	Counter-based Data information						*/
/*										*/
/********************************************************************************/

interface CounterData {

   String getName();			// get dymon-based name
   String getName(int idx);
   long getActiveTime(long now,String whom);
   double getActiveFraction(long totsam,long actsam,String whom);
   long getActiveRunTime(long now,long totsam,long actsam,String whom);
   long getTimesActive(String whom);
   boolean isActive(String whom);
   boolean isRange();

}	// end of subinterface CounterData



/********************************************************************************/
/*										*/
/*	Delta-based Information 						*/
/*										*/
/********************************************************************************/

interface DeltaData {

   void outputDelta(IvyXmlWriter xw,DeltaData prev);

}	// end of subinterface DeltaData




}	// end of interface DymonConstants




/* end of DymonConstants.java */

