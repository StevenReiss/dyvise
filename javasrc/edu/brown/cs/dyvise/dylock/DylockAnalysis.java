/********************************************************************************/
/*										*/
/*		DylockAnalysis.java						*/
/*										*/
/*	DYVISE lock analysis analyzer						*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylock/DylockAnalysis.java,v 1.2 2013-05-09 12:28:59 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylockAnalysis.java,v $
 * Revision 1.2  2013-05-09 12:28:59  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.1  2011-03-10 02:24:59  spr
 * New version of dylock to produce visualizations.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dylock;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.util.*;


class DylockAnalysis implements DylockConstants
{




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private List<DylockChecker>	allowed_types;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DylockAnalysis(TraceLockData ld)
{
   allowed_types = new ArrayList<DylockChecker>();
   allowed_types.add(new DylockCheckerReadWrite(ld));
   allowed_types.add(new DylockCheckerSemaphore(ld));
   allowed_types.add(new DylockCheckerLatch(ld));
   allowed_types.add(new DylockCheckerJoin());
   allowed_types.add(new DylockCheckerDelay());
   allowed_types.add(new DylockCheckerCondition());
   allowed_types.add(new DylockCheckerMutex());
}



/********************************************************************************/
/*										*/
/*	Checking methods							*/
/*										*/
/********************************************************************************/

void check(List<TraceLockEntry> seq)
{
   for (Iterator<DylockChecker> it = allowed_types.iterator(); it.hasNext(); ) {
      DylockChecker dc = it.next();
      if (!dc.isConsistent(seq)) it.remove();
    }
}



DylockChecker finishChecks()
{
   for (Iterator<DylockChecker> it = allowed_types.iterator(); it.hasNext(); ) {
      DylockChecker dc = it.next();
      if (!dc.isValidated()) it.remove();
    }

   return null;
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(IvyXmlWriter xw)
{
   List<TraceLockLocation> used = new ArrayList<TraceLockLocation>();

   xw.begin("ANALYSIS");
   for (DylockChecker dc : allowed_types) {
      dc.generateReport(xw,null,used);
    }
   xw.end("ANALYSIS");
}



}	// end of class DylockAnalysis




/* end of DylockAnalysis.java */
