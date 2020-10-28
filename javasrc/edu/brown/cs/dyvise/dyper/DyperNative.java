/********************************************************************************/
/*										*/
/*		DyperNative.java						*/
/*										*/
/*	Native entry points for interacting with the JVMTI for DYPER		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyper/DyperNative.java,v 1.2 2009-03-20 02:08:21 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyperNative.java,v $
 * Revision 1.2  2009-03-20 02:08:21  spr
 * Code cleanup; output information for incremental time-based display.
 *
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dyper;




public class DyperNative implements DyperConstants {


static {
   // System.load("/pro/dyvise/lib/x86_64/libdymti.so");
   // System.loadLibrary("dymti");
}




/********************************************************************************/
/*										*/
/*	Setup call								*/
/*										*/
/********************************************************************************/

public static void setup(String pid,String mid)
{
   try {
      nativeSetup(pid,mid);
    }
   catch (Throwable t) { }
}



private static native void nativeSetup(String pid,String mid);




/********************************************************************************/
/*										*/
/*	Checkpoint call 							*/
/*										*/
/********************************************************************************/

public static void checkpoint()
{
   try {
      nativeCheckpoint();
    }
   catch (Throwable t) {
      System.err.println("DYPER: Checkpoint error: " + t);
    }
}


private static native void nativeCheckpoint();


/********************************************************************************/
/*										*/
/*	Heap Model Creation call						*/
/*										*/
/********************************************************************************/

public static void dumpHeapModel(String file)
{
   try {
      nativeDumpHeapModel(file);
    }
   catch (Throwable t) {
      System.err.println("DYPER: Heap Dump error: " + t);
    }
}


private static native void nativeDumpHeapModel(String file);



/********************************************************************************/
/*										*/
/*	Force load method							*/
/*										*/
/********************************************************************************/

public static void forceLoad()
{
   // note this doesn't work right because we need to load dymti as an agent
   System.err.println("DYPER: FORCE LOAD DYMTI");

   /*************
   try {
      System.load("/pro/dyvise/lib/x86_64/libdymti.so");
      return;
    }
   catch (Throwable t) {
      System.err.println("DYPER: Problem loading library: " + t);
    }

   try {
      System.loadLibrary("dymti");
      return;
    }
   catch (Throwable t) {
      System.err.println("DYPER: Problem loading library: " + t);
    }
   **************/
}




}	// end of class DyperNative




/* end of DyperNative.java */

