/********************************************************************************/
/*										*/
/*		DylockConstants.java						*/
/*										*/
/*	DYVISE lock analysis constants						*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylock/DylockConstants.java,v 1.3 2013-05-09 12:28:59 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylockConstants.java,v $
 * Revision 1.3  2013-05-09 12:28:59  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.2  2012-10-05 00:52:41  spr
 * Update lock analysis.  Prepare for pattern display.
 *
 * Revision 1.1  2011-03-10 02:24:59  spr
 * New version of dylock to produce visualizations.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dylock;


import edu.brown.cs.dyvise.dyvise.*;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.util.*;


interface DylockConstants extends DyviseConstants {


/********************************************************************************/
/*										*/
/*	Lock Trace entry types							*/
/*										*/
/********************************************************************************/

enum TraceEntryType {
   ENTER,
   ENTERED,
   WAIT,
   WAITED,
   WAITTIME,
   UNLOCK,
   NOTIFY,
   NOLOCK,
   RESET,
   PREJOIN,
   NOJOIN,
   JOIN
}



/********************************************************************************/
/*										*/
/*	Reporting constants							*/
/*										*/
/********************************************************************************/

double TIME_SHIFT = 1000;	// nano to micro



/********************************************************************************/
/*										*/
/*	Lock Entry information							*/
/*										*/
/********************************************************************************/

interface TraceLockData {

   Iterable<TraceLockLocation> getLockLocations();

}



interface TraceLockEntry {

   int getLockId();
   DylockLockData getLock();
   double getTime();
   int getThreadId();
   String getThreadName();
   DylockThreadData getThread();
   TraceEntryType getEntryType();
   TraceLockLocation getLocation();
   int getLocationId();
   int getNestedDepth();
   int getThreadDepth();

}


interface TraceLockLocation {
   String getClassName();
   String getMethodName();
   String getMethodSignature();
   boolean doesWait();
   boolean doesNotify();
   int getId();
   String getKey();
   Collection<TraceLockLocation> getAliases();

   String getDisplayName();

   void finishLoad(DylockLocationManager mgr);
   void outputXml(IvyXmlWriter xw,boolean alias);
   void outputBuffer(StringBuffer buf);
}




/********************************************************************************/
/*										*/
/*	Trace entry information 						*/
/*										*/
/********************************************************************************/

interface RunningEntry {

   TraceEntryType getEntryType();
   int getThreadId();
   String getThreadName();
   double getTime();
   int getLocationId();

}	// end of inner interface RunningEntry



/********************************************************************************/
/*										*/
/*	Pattern event information						*/
/*										*/
/********************************************************************************/

enum PatternEventType {
   ENTER,
   ENTERED,
   EXIT,
   WAIT,
   WAITED,
   NOTIFY,
   READLOCK,
   READUNLOCK,
   WRITELOCK,
   WRITEUNLOCK,
}


interface PatternEvent {
   DylockLockData getLock();
   DylockViewType getView();
   PatternEventType getType();
   int getThreadId();
   DylockThreadData getThread();
   int getLevel();
   double getTime();
   DylockLockEntry getLockEntry();
   TraceLockLocation getLocation();
   void outputXml(IvyXmlWriter xw);
}



interface DylockEventSetBuilder {

   void addEvent(PatternEvent pe);
   DylockLockDataManager getManager();
   boolean missingWaitForWaited(DylockLockEntry ent);
   double getLockTime(DylockLockData ld,double t0);

}	// end of interface DylockEventSetBuilder



/********************************************************************************/
/*										*/
/*	Analyzed lock types							*/
/*										*/
/********************************************************************************/

interface DylockChecker {

   boolean isConsistent(List<TraceLockEntry> seq);
   boolean isValidated();
   void generateReport(IvyXmlWriter xw,DyviseDatabase db,List<TraceLockLocation> used);

}


/********************************************************************************/
/*										*/
/*	Main program option							*/
/*										*/
/********************************************************************************/

interface DylockExec {

   void process();

}

interface DylockLocationManager {
   DylockLockLocation findLocation(int id);
}




interface DylockLockDataManager extends DylockLocationManager {
   DylockThreadData findThread(int id);
   double getMaxTime();
   DylockLockData findLock(int id);
}



interface DylockPatternAccess {

   List<DylockPattern> getPatterns();
   void setCurrentPattern(DylockPattern pat);

}	// end of interface DylockPatternAccess




/********************************************************************************/
/*										*/
/*	Viewing definitions							*/
/*										*/
/********************************************************************************/

int VIEW_TYPE_MUTEX = 0;
int VIEW_TYPE_R = 1;
int VIEW_TYPE_W = 2;
int NUM_VIEW_TYPES = 3;
String [] VIEW_TYPE_NAMES = new String [] { "MUTEX", "READ", "WRITE" };

double	VIEW_FORCE_TIME = 5000000000.0;



}	// end of interface DylockConstants




/* end of DylockConstants.java */

