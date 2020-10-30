/********************************************************************************/
/*										*/
/*		DymemExtract.java						*/
/*										*/
/*	DYMON program for building CSV data for memory analysis 		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymem/DymemExtract.java,v 1.3 2009-10-07 01:00:02 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymemExtract.java,v $
 * Revision 1.3  2009-10-07 01:00:02  spr
 * Code cleanup for eclipse. Add support for standalone version.
 *
 * Revision 1.2  2009-09-19 00:09:27  spr
 * Update dymem with some bug fixes; initial support for reading dump files.
 *
 * Revision 1.1  2009-03-20 02:10:12  spr
 * Add memory graph computation and display.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymem;


import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.Element;

import java.io.*;
import java.util.*;


public class DymemExtract implements DymemConstants {



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   DymemExtract de = new DymemExtract(args);

   de.process();
}




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<String,List<Integer>> count_set;
private String	process_id;
private long at_time;
private DymemParameters param_values;
private DymemCycleNamer cycle_namer;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private DymemExtract(String [] args)
{
   process_id = null;
   count_set = new TreeMap<String,List<Integer>>();
   at_time = 0;
   scanArgs(args);

   param_values = new DymemParameters(process_id);
   cycle_namer = new DymemCycleNamer();
}




/********************************************************************************/
/*										*/
/*	Argument scanning							*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
	 if (args[i].startsWith("-t") && i+1 < args.length) {           // -t <time>
	    at_time = Long.parseLong(args[++i]);
	  }
	 else badArgs();
       }
      else if (process_id == null) process_id = args[i];
      else badArgs();
    }

   if (process_id == null) badArgs();
}



private void badArgs()
{
   System.err.println("DYMEM: dymemextrace pid@host");
   System.exit(1);
}




/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

private void process()
{
   File f = new File(DYMEM_HEAP_PREFIX + process_id + ".trace");
   f.deleteOnExit();
   
   IvyXmlReader xr = null;

   try {
      xr = new IvyXmlReader(new FileReader(f));
    }
   catch (IOException e) {
      System.err.println("DYMEM: Can't open trace file " + f + ": " + e);
      System.exit(1);
      return;
    }

   int idx = 0;
   while (at_time >= 0) {
      try {
	 String xmls = xr.readXml();
	 if (xmls == null) break;
	 Element xml = IvyXml.convertStringToXml(xmls);
	 if (at_time > 0) {
	    long now = IvyXml.getAttrLong(xml,"NOW");
	    if (now < at_time) continue;
	    at_time = -1;
	  }
	 for (Element e : IvyXml.children(xml,"CLASS")) {
	    String cnm = IvyXml.getAttrString(e,"NAME");
	    int cct = IvyXml.getAttrInt(e,"COUNT");
	    List<Integer> li = count_set.get(cnm);
	    if (li == null) {
	       li = new ArrayList<Integer>();
	       count_set.put(cnm,li);
	     }
	    while (li.size() < idx) li.add(0);
	    li.add(cct);
	  }
	 DymemGraph dg = new DymemGraph(xml,null,param_values,cycle_namer);
	 IvyXmlWriter xw = new IvyXmlWriter("MemoryGraph_" + idx + ".xml");
	 dg.generateOutput(xw);
	 xw.close();
       }
      catch (IOException e) {
	 System.err.println("DYMEM: Problem reading trace file: " + e);
	 break;
       }
      ++idx;
    }
   try {
      xr.close();
    }
   catch (IOException e) { }

   if (at_time < 0) return;

   PrintWriter pw = null;
   f = new File(DYMON_HEAP_PREFIX + process_id + ".csv");
   try {
      pw = new PrintWriter(new FileWriter(f));
    }
   catch (IOException e) {
      System.err.println("DYMEM: Problem creating output file " + f + ": " + e);
      System.exit(1);
      return;
    }
   for (Map.Entry<String,List<Integer>> ent : count_set.entrySet()) {
      String nm = ent.getKey();
      List<Integer> li = ent.getValue();

      int iv0 = li.get(0);
      boolean allsame = true;
      for (Integer iv : li) {
	 if (iv0 != iv) {
	    allsame = false;
	    break;
	  }
       }
      if (allsame) continue;

      pw.print("\"" + nm + "\"");
      int ct = 0;
      for (Integer iv : li) {
	 pw.print("," + iv);
	 ++ct;
       }
      while (ct < idx) {
	 pw.print(",0");
	 ++ct;
       }
      pw.println();
    }

   pw.close();
}





}	// end of class DymemExtract




/* end of DymemExtract.java */
