/********************************************************************************/
/*                                                                              */
/*              DylockViewLatchType.java                                        */
/*                                                                              */
/*      Specifications for latch-type lock viewing                              */
/*                                                                              */
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylock/DylockViewLatchType.java,v 1.2 2013-05-09 12:29:01 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylockViewLatchType.java,v $
 * Revision 1.2  2013-05-09 12:29:01  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.1  2012-10-05 00:52:42  spr
 * Update lock analysis.  Prepare for pattern display.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dylock;

import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.Element;

import java.util.*;


class DylockViewLatchType extends DylockViewType implements DylockConstants
{

   
/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Set<TraceLockLocation> lock_set;
private Set<TraceLockLocation> unlock_set;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

DylockViewLatchType(DylockViewRef dv,Element xe) 
{
   super(dv,xe);
   lock_set = new TreeSet<TraceLockLocation>();
   unlock_set = new TreeSet<TraceLockLocation>();
   for (Element le : IvyXml.children(IvyXml.getChild(xe,"LOCKSET"),"LOCATION")) {
      DylockLockLocation vl = getLocation(le);
      lock_set.add(vl);
      addLocation(vl);
    }
   for (Element le : IvyXml.children(IvyXml.getChild(xe,"UNLOCKSET"),"LOCATION")) {
      DylockLockLocation vl = getLocation(le);
      unlock_set.add(vl);
      addLocation(vl);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

String getShortString() 
{
   return "LATCH";
}

void addToolTip(StringBuffer buf)
{
   outputTableHeader("LATCH",buf);
   outputRowStart(buf);
   outputTable("Locks",lock_set,buf);
   outputRowEnd(buf);
   outputRowStart(buf);
   outputTable("Unlocks",unlock_set,buf);
   outputRowEnd(buf);
   outputTableTrailer(buf);
}



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

void localOutputXml(IvyXmlWriter xw) 
{
   xw.field("KIND","LATCH");
   xw.begin("LOCKSET");
   for (TraceLockLocation ll : lock_set) ll.outputXml(xw,false);
   xw.end("LOCKSET");
   xw.begin("UNLOCKSET");
   for (TraceLockLocation ll : unlock_set) ll.outputXml(xw,false);
   xw.end("UNLOCKSET");
}



}       // end of class DylockViewLatchType




/* end of DylockViewLatchType.java */
