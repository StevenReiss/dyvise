/********************************************************************************/
/*										*/
/*		DymonAgentCollections.java					*/
/*										*/
/*	DYPER monitor agent for Coillections monitoring 			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymon/DymonAgentCollections.java,v 1.5 2009-09-19 00:09:46 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymonAgentCollections.java,v $
 * Revision 1.5  2009-09-19 00:09:46  spr
 * Update dymon for seletive agent insertion and removal.
 *
 * Revision 1.4  2009-06-04 18:53:50  spr
 * Set up for binary distribution.
 *
 * Revision 1.3  2009-05-01 23:15:12  spr
 * Fix up state computation.  Clean up code.
 *
 * Revision 1.2  2009-04-11 23:46:18  spr
 * Use IvyFormat rather than local code.
 *
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymon;


import edu.brown.cs.ivy.file.IvyFormat;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.util.*;



class DymonAgentCollections extends DymonAgent {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private long		total_samples;
private long		total_time;
private Map<String,CollectionData> coll_map;

private double		report_threshold = 0.0;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DymonAgentCollections(DymonMain dm,DymonProcess p)
{
   super(dm,p);

   total_samples = 0;
   total_time = 0;
   coll_map = new HashMap<String,CollectionData>();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getName()			{ return "COLLECTIONS"; }


@Override public String getDyperAgentName()		{ return "COLLECTIONS"; }


@Override protected String getMonitorClass()
{
   return "edu.brown.cs.dyvise.dyper.DyperAgentCollections";
}



/********************************************************************************/
/*										*/
/*	Report handling methods 						*/
/*										*/
/********************************************************************************/

@Override public void handleReport(Element r)
{
   Element ce = IvyXml.getElementByTag(r,"COLLECTIONS");

   long montime = IvyXml.getAttrLong(ce,"MONTIME");

   total_time = montime;
   total_samples = IvyXml.getAttrLong(ce,"SAMPLES");

   for (Element te : IvyXml.elementsByTag(ce,"ITEM")) {
      String nm = IvyXml.getAttrString(te,"NAME");
      CollectionData id = coll_map.get(nm);
      if (id == null) {
	 id = new CollectionData(nm);
	 coll_map.put(nm,id);
       }
      id.update(te);
    }
}




/********************************************************************************/
/*										*/
/*	Analysis methods							*/
/*										*/
/********************************************************************************/

@Override public void outputAnalysis(IvyXmlWriter xw)
{
/*****************
   xw.begin("COLLECTIONS");

   xw.field("TOTTIME",IvyFormat.formatTime(total_time));
   xw.field("TOTSAMP",total_samples);
   xw.field("TOTCOLL",total_coll);
   xw.field("COLLTIME",IvyFormat.formatTime(((double) total_coll)/total_samples*total_time));

   for (CollectionData id : coll_map.values()) {
      id.output(xw);
    }

   xw.end("COLLECTIONS");
******************/
}




/********************************************************************************/
/*										*/
/*	Summary methods 							*/
/*										*/
/********************************************************************************/

@Override public double getSummaryValue()	{ return -1; }




/********************************************************************************/
/*										*/
/*	Detailing methods							*/
/*										*/
/********************************************************************************/

public Collection<DymonDetailing> getDetailings()	{ return null; }



/********************************************************************************/
/*										*/
/*	Class to hold I/O information for a method				*/
/*										*/
/********************************************************************************/

private class CollectionData {

   private String class_name;
   private String method_name;
   private double total_count;
   private Map<String,long []> thread_counts;

   CollectionData(String nm) {
      int idx = nm.indexOf('@');
      class_name = nm.substring(0,idx);
      method_name = nm.substring(idx+1);
      total_count = 0;
      thread_counts = new HashMap<String,long[]>();
    }

   void update(Element e) {
      total_count = IvyXml.getAttrDouble(e,"COUNT");
      for (Element te : IvyXml.elementsByTag(e,"THREAD")) {
	 String nm = IvyXml.getAttrString(te,"NAME");
	 long [] val = thread_counts.get(nm);
	 if (val == null) {
	    val = new long[1];
	    thread_counts.put(nm,val);
	  }
	 val[0] = IvyXml.getAttrLong(te,"COUNT");
       }
    }

   @SuppressWarnings("unused")
   void output(IvyXmlWriter xw) {
      if (total_count/total_samples < report_threshold) return;

      xw.begin("COLLCOUNT");
      xw.field("CLASS",class_name);
      xw.field("METHOD",method_name);
      xw.field("COUNT",IvyFormat.formatCount(total_count));
      xw.field("PCT",IvyFormat.formatPercent(total_count/total_samples));
      xw.field("TIME",IvyFormat.formatTime(total_count/total_samples * total_time));
      for (Map.Entry<String,long []> ent : thread_counts.entrySet()) {
	 xw.begin("THREAD");
	 xw.field("NAME",ent.getKey());
	 double ct = ent.getValue()[0];
	 xw.field("COUNT",IvyFormat.formatCount(ct));
	 xw.field("PCT",IvyFormat.formatPercent(ct/total_count));
	 xw.field("TIME",IvyFormat.formatTime(ct/total_samples * total_time));
	 xw.end("THREAD");
       }
      xw.end("COLLCOUNT");
    }

}	// end of subclass CollectionData



}	// end of class DymonAgentCollections




/* end of DymonAgentCollections.java */


