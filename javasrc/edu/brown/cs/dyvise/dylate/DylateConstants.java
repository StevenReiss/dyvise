/********************************************************************************/
/*										*/
/*		DylateConstants.java						*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylate/DylateConstants.java,v 1.2 2013-05-09 12:28:57 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylateConstants.java,v $
 * Revision 1.2  2013-05-09 12:28:57  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.1  2012-10-05 00:52:26  spr
 * New lock tracer
 *
 *
 ********************************************************************************/


package edu.brown.cs.dyvise.dylate;

// NOTE: This should only use system libraries and libraries that are explicitly
//    in this package.	Using anything else might cause conflicts if the application
//    being monitored uses the same library.


interface DylateConstants {



/********************************************************************************/
/*										*/
/*	Monitoring routine definitions						*/
/*										*/
/********************************************************************************/

String		MONITOR_CLASS = "edu/brown/cs/dyvise/dylate/DylateMonitor";
String		MONITOR_ENTER = "monEnter";
String		MONITOR_ENTER_ARGS = "(Ljava/lang/Object;I)V";
String		MONITOR_ENTERED = "monEntered";
String		MONITOR_ENTERED_ARGS = "(Ljava/lang/Object;I)V";
String		MONITOR_EXIT = "monExit";
String		MONITOR_EXIT_ARGS = "(Ljava/lang/Object;)V";
String		MONITOR_NOTIFY = "monNotify";
String		MONITOR_NOTIFY_ARGS = "(Ljava/lang/Object;I)V";
String		MONITOR_WAIT = "monWait";
String		MONITOR_WAIT_ARGS = "(Ljava/lang/Object;I)V";
String		MONITOR_WAITED = "monWaited";
String		MONITOR_WAITED_ARGS = "(Ljava/lang/Object;)V";
String		MONITOR_WAIT_TIMED = "monWaitTimed";
String		MONITOR_WAIT_TIMED_ARGS = "(Ljava/lang/Object;JII)V";
String		MONITOR_TRY = "monTryTimed";
String		MONITOR_TRY_ARGS = "(Ljava/util/concurrent/locks/Lock;I)V";
String		MONITOR_TRY_TIMED = "monTryTimed";
String		MONITOR_TRY_TIMED_ARGS = "(Ljava/util/concurrent/locks/Lock;JLjava/util/concurrent/TimeUnit;I)V";
String		MONITOR_AWAIT_TIMED = "monAwaitTimed";
String		MONITOR_AWAIT_TIMED_ARGS = "(Ljava/util/concurrent/locks/Condition;JLjava/util/concurrent/TimeUnit;I)V";
String		MONITOR_AWAIT_NANOS = "monAwaitNanos";
String		MONITOR_AWAIT_NANOS_ARGS = "(Ljava/util/concurrent/locks/Condition;JI)J";
String		MONITOR_AWAIT_TIMED_BARRIER = "monAwaitBarrierTimed";
String		MONITOR_AWAIT_TIMED_BARRIER_ARGS = "(Ljava/util/concurrent/CyclicBarrier;JLjava/util/concurrent/TimeUnit;I)V";
String		MONITOR_AWAIT_TIMED_LATCH = "monAwaitLatchTimed";
String		MONITOR_AWAIT_TIMED_LATCH_ARGS = "(Ljava/util/concurrent/CountDownLatch;JLjava/util/concurrent/TimeUnit;I)V";
String		MONITOR_PRELOCK = "monPreLock";
String		MONITOR_PRELOCK_ARGS = "(Ljava/lang/Object;I)V";
String		MONITOR_LOCK = "monLock";
String		MONITOR_LOCK_ARGS = "(Ljava/lang/Object;ZI)V";
String		MONITOR_UNLOCK = "monUnlock";
String		MONITOR_UNLOCK_ARGS = "(Ljava/lang/Object;I)V";
String		MONITOR_ASSOC = "monAssoc";
String		MONITOR_ASSOC_ARGS = "(Ljava/lang/Object;Ljava/lang/Object;)V";
String		MONITOR_JOIN = "monJoin";
String		MONITOR_JOIN_ARGS = "(Ljava/lang/Thread;JII)V";



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
   int getLocationId();
   int getLockId();

}

}	// end of interface DylateConstants




/* end of DylateConstants.java */
