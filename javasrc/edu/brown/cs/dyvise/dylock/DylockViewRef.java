/********************************************************************************/
/*										*/
/*		DylockViewRef.java						*/
/*										*/
/*	DYVISE lock analysis lock view/visualization referencer 		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylock/DylockViewRef.java,v 1.2 2013-05-09 12:29:01 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylockViewRef.java,v $
 * Revision 1.2  2013-05-09 12:29:01  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.1  2011-03-10 02:25:00  spr
 * New version of dylock to produce visualizations.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dylock;


import java.util.*;
import org.w3c.dom.Element;



class DylockViewRef implements DylockConstants.DylockLocationManager, DylockConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<DylockLockLocation,DylockLockLocation> all_locations;
private Map<Integer,DylockLockLocation> location_map;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DylockViewRef()
{
   all_locations = new HashMap<DylockLockLocation,DylockLockLocation>();
   location_map = new HashMap<Integer,DylockLockLocation>();
}




/********************************************************************************/
/*										*/
/*	Location methods							*/
/*										*/
/********************************************************************************/

DylockLockLocation getLocation(Element xml)
{
   DylockLockLocation vl = new DylockLockLocation(xml,true);
   DylockLockLocation xvl = all_locations.get(vl);
   if (xvl == null) {
      xvl = vl;
      all_locations.put(vl,xvl);
    }
   location_map.put(vl.getId(),xvl);

   return xvl;
}



@Override public DylockLockLocation findLocation(int id)
{
   return location_map.get(id);
}




/********************************************************************************/
/*                                                                              */
/*      Load cleanup methods                                                    */
/*                                                                              */
/********************************************************************************/

void finishLoad()
{
   for (DylockLockLocation dll : location_map.values()) {
      dll.finishLoad(this);
    }
}



/********************************************************************************/
/*										*/
/*	Methods for generating tool tip tables					*/
/*										*/
/********************************************************************************/

static void outputTable(String ttl,Collection<TraceLockLocation> locs,StringBuffer buf)
{
   outputTableHeader(ttl,buf);
   for (TraceLockLocation dll : locs) {
      outputRowStart(buf);
      dll.outputBuffer(buf);
      outputRowEnd(buf);
    }
   outputTableTrailer(buf);
}



static void outputTableHeader(String ttl,StringBuffer buf)
{
   buf.append("<table border=1 rules=cols><tr><td colspan='2'>");
   buf.append(ttl);
   buf.append("</td><tr>");
}



static void outputTableTrailer(StringBuffer buf)
{
   buf.append("</table>");
}


static void outputRowStart(StringBuffer buf)
{
   buf.append("<tr><td width='16'>&nbsp;</td><td>");
}

static void outputRowEnd(StringBuffer buf)
{
   buf.append("</td></tr>");
}




}	// end of class DylockViewRef




/* end of DylockViewRef.java */
