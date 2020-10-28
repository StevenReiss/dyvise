/********************************************************************************/
/*										*/
/*		DymonRun.java							*/
/*										*/
/*	Class for running DYMON/DYPER/DYMEM from a dyvise distribution		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymon/DymonRun.java,v 1.2 2009-10-07 01:00:13 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymonRun.java,v $
 * Revision 1.2  2009-10-07 01:00:13  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.1  2009-09-19 00:09:46  spr
 * Update dymon for seletive agent insertion and removal.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymon;



import java.io.IOException;



public class DymonRun implements DymonConstants {


/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   DymonRemote.dyviseSetup();

   try {
      DymonRemote.dyviseJava("edu.brown.cs.dyvise.dymaster.DymasterMain",null,null);
    }
   catch (IOException e) {
      System.err.println("DYMONRUN: Problem starting dymaster: " + e);
    }
}



}	// end of class DymonRun



/* end of DymonRun.java */
