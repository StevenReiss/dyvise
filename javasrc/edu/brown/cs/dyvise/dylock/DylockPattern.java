/********************************************************************************/
/*										*/
/*		DylockPattern.java						*/
/*										*/
/*	Holder of a found locking pattern					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylock/DylockPattern.java,v 1.1 2013-05-09 12:29:00 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylockPattern.java,v $
 * Revision 1.1  2013-05-09 12:29:00  spr
 * Dylock update.	Last 1.6 update
 *
 *
 ********************************************************************************/




package edu.brown.cs.dyvise.dylock;


import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.Element;

import java.util.*;
import java.io.*;


class DylockPattern implements DylockConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private List<PatternEvent>	exemplar_set;
private int			instance_count;
private String			pattern_text;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DylockPattern(List<PatternEvent> evts)
{
   exemplar_set = evts;
   instance_count = 1;
   pattern_text = null;

   computeText();
}



DylockPattern(DylockLockManager mgr,Element xml) throws IOException
{
   instance_count = IvyXml.getAttrInt(xml,"INSTANCES");
   pattern_text = IvyXml.getAttrString(xml,"KEY");
   exemplar_set = new ArrayList<PatternEvent>();
   for (Element ee : IvyXml.children(xml,"EVENT")) {
      DylockPatternEvent pe = new DylockPatternEvent(mgr,ee);
      exemplar_set.add(pe);
    }
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getKey()
{
   return pattern_text;
}

List<PatternEvent> getEvents()		{ return exemplar_set; }


void addInstance(DylockPattern dup)
{
   ++instance_count;
}



/********************************************************************************/
/*										*/
/*	Methods to compute normalized pattern information			*/
/*										*/
/********************************************************************************/

private void computeText()
{
   Map<Integer,Integer> threads = new HashMap<Integer,Integer>();
   Map<DylockLockData,Integer> locks = new HashMap<DylockLockData,Integer>();

   StringBuffer buf = new StringBuffer();
   for (PatternEvent evt : exemplar_set) {
      buf.append("(");
      buf.append(evt.getType().toString());
      buf.append(",");
      int tid = evt.getThreadId();
      Integer tvl = threads.get(tid);
      if (tvl == null) {
	 tvl = threads.size() + 1;
	 threads.put(tid,tvl);
       }
      buf.append(tvl.toString());
      buf.append(",");
      DylockLockData ld = evt.getLock();
      Integer lvl = locks.get(ld);
      if (lvl == null) {
	 lvl = locks.size() + 1;
	 locks.put(ld,lvl);
       }
      buf.append(lvl.toString());
      buf.append(")");
    }

   pattern_text = buf.toString();
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void output(IvyXmlWriter xw)
{
   xw.begin("PATTERN");
   xw.field("INSTANCES",instance_count);
   xw.field("COUNT",exemplar_set.size());
   xw.field("KEY",pattern_text);

   Map<Integer,Integer> threads = new HashMap<Integer,Integer>();
   Map<DylockLockData,Integer> locks = new HashMap<DylockLockData,Integer>();

   for (PatternEvent evt : exemplar_set) {
      xw.begin("EVENT");
      xw.field("TYPE",evt.getType());
      xw.field("LEVEL",evt.getLevel());

      int tid = evt.getThreadId();
      Integer tvl = threads.get(tid);
      if (tvl == null) {
	 tvl = threads.size() + 1;
	 threads.put(tid,tvl);
       }
      xw.field("THREAD",tvl.intValue());

      DylockLockData ld = evt.getLock();
      Integer lvl = locks.get(ld);
      if (lvl == null) {
	 lvl = locks.size() + 1;
	 locks.put(ld,lvl);
       }
      xw.field("LOCK",lvl.intValue());
      evt.outputXml(xw);
      xw.end("EVENT");
    }

   xw.end("PATTERN");
}



/********************************************************************************/
/*										*/
/*	Display methods 							*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   PatternEvent evt = exemplar_set.get(0);
   DylockLockData ld = evt.getLock();
   TraceLockLocation loc = evt.getLocation();
   String s1 = ld.getClassString();
   String s2 = loc.getMethodName();

   return s1 + "@" + s2 + " [" + exemplar_set.size() + "] (" + instance_count + ")";
}


int compareOrder(DylockPattern p2)
{
   PatternEvent evt1 = exemplar_set.get(0);
   DylockLockData ld1 = evt1.getLock();
   TraceLockLocation loc1 = evt1.getLocation();
   PatternEvent evt2 = p2.exemplar_set.get(0);
   DylockLockData ld2 = evt2.getLock();
   TraceLockLocation loc2 = evt2.getLocation();

   if (ld1 == null) return -1;
   else if (ld2 == null) return 1;


   String s1 = ld1.getClassString();
   String s2 = ld2.getClassString();
   int c1 = 0;
   if (s1 == null && s2 == null) c1 = 0;
   else if (s1 == null) c1 = -1;
   else if (s2 == null) c1 = 1;
   else c1 = ld1.getClassString().compareTo(ld2.getClassString());

   if (c1 == 0) {
      if (loc1.getMethodName() == null) c1 = -1;
      else c1 = loc1.getMethodName().compareTo(loc2.getMethodName());
    }
   if (c1 == 0) {
      c1 = p2.instance_count - instance_count;
    }
   if (c1 == 0) {
      c1 = exemplar_set.size() - p2.exemplar_set.size();
    }

   return c1;
}



}	// end of class DylockPattern



/* end of DylockPattern.java */










