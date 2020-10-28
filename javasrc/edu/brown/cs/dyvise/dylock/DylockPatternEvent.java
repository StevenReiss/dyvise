/********************************************************************************/
/*										*/
/*		DylockPatternEvent.java 					*/
/*										*/
/*	Event that is part of a pattern 					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylock/DylockPatternEvent.java,v 1.1 2013-05-09 12:29:00 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylockPatternEvent.java,v $
 * Revision 1.1  2013-05-09 12:29:00  spr
 * Dylock update.	Last 1.6 update
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dylock;

import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.Element;

import java.io.*;


class DylockPatternEvent implements DylockConstants, DylockConstants.PatternEvent
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/


private DylockLockEntry 	lock_entry;
private DylockViewType		view_type;
private PatternEventType	event_type;
private int			lock_level;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DylockPatternEvent(DylockLockEntry ent,DylockViewType vt,PatternEventType typ,int lvl)
{
   lock_entry = ent;
   event_type = typ;
   view_type = vt;
   lock_level = lvl;
}



DylockPatternEvent(DylockLockManager mgr,Element exml) throws IOException
{
   StringBuilder buf = new StringBuilder();

   event_type = IvyXml.getAttrEnum(exml,"TYPE",PatternEventType.ENTER);
   lock_level = IvyXml.getAttrInt(exml,"LEVEL");

   Element xml = IvyXml.getChild(exml,"ENTRY");

   int vtid = IvyXml.getAttrInt(xml,"VIEW");
   view_type = DylockViewType.findViewType(vtid);

   buf.append(IvyXml.getAttrString(xml,"TYPE"));
   buf.append("|");
   buf.append(IvyXml.getAttrInt(xml,"THREAD"));
   buf.append("|");
   buf.append(IvyXml.getAttrInt(xml,"LOCATION"));
   buf.append("|");
   buf.append(IvyXml.getAttrDouble(xml,"TIME"));
   buf.append("|");
   buf.append(IvyXml.getAttrInt(xml,"LOCK"));
   buf.append("|");
   buf.append(IvyXml.getAttrInt(xml,"NESTDEPTH"));
   buf.append("|");
   buf.append(IvyXml.getAttrInt(xml,"THREADDEPTH"));

   lock_entry = new DylockLockEntry(mgr,buf.toString());
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public DylockLockData getLock()
{
   return lock_entry.getLock();
}


@Override public DylockViewType getView()		{ return view_type; }
@Override public PatternEventType getType()		{ return event_type; }
@Override public int getLevel() 			{ return lock_level; }
@Override public DylockLockEntry getLockEntry() 	{ return lock_entry; }

@Override public double getTime()	{ return lock_entry.getTime(); }
@Override public TraceLockLocation getLocation()	{ return lock_entry.getLocation(); }

@Override public int getThreadId()
{
   return lock_entry.getThreadId();
}
@Override public DylockThreadData getThread()
{
   return lock_entry.getThread();
}


@Override public String toString() {
   return event_type.toString() + ": " + lock_entry.toString() + " " + lock_level;
}


/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public void outputXml(IvyXmlWriter xw)
{
   xw.begin("ENTRY");
   xw.field("LOCK",lock_entry.getLockId());
   xw.field("THREAD",lock_entry.getThreadId());
   xw.field("TIME",lock_entry.getTime());
   xw.field("TYPE",lock_entry.getEntryType());
   xw.field("LOCATION",lock_entry.getLocationId());
   xw.field("NESTDEPTH",lock_entry.getNestedDepth());
   xw.field("THREADDEPTH",lock_entry.getThreadDepth());
   xw.field("VIEW",view_type.getTypeId());
   xw.field("LEVEL",getLevel());

   xw.end("ENTRY");
}




}	// end of class DylockPatternEvent




/* end of DylockPatternEvent.java */
