/********************************************************************************/
/*										*/
/*		DycompAnalysisMethodThread.java 				*/
/*										*/
/*	DYVISE computed relations analysis for finding classes assoc w/ method	*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dycomp/DycompAnalysisMethodThread.java,v 1.2 2011-03-10 02:32:39 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DycompAnalysisMethodThread.java,v $
 * Revision 1.2  2011-03-10 02:32:39  spr
 * Clean up code
 *
 * Revision 1.1  2010-03-30 21:28:29  spr
 * Add new analyses needed for better visualizations.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dycomp;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;



class DycompAnalysisMethodThread extends DycompAnalysis implements DycompConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private List<String>	method_table;
private String		source_restrict;
private List<String>	start_threads;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DycompAnalysisMethodThread(DycompMain dm)
{
   super(dm);
}



DycompAnalysisMethodThread(DycompAnalysisMethodThread proto,Element xml)
{
   super(proto,xml);

   method_table = new ArrayList<String>();
   for (Element e : IvyXml.children(xml,"SOURCE")) {
      method_table.add(getTableName(e));
    }
   source_restrict = IvyXml.getTextElement(xml,"RESTRICT");

   start_threads = new ArrayList<String>();
   for (Element e : IvyXml.children(xml,"THREAD")) {
      String tnm = IvyXml.getText(e);
      start_threads.add(tnm);
    }
}




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

DycompAnalysis createNew(Element xml)
{
   DycompAnalysisMethodThread da = new DycompAnalysisMethodThread(this,xml);

   return da;
}



/********************************************************************************/
/*										*/
/*	Work methods								*/
/*										*/
/********************************************************************************/

void compute()
{
   Set<String> usethreads = new HashSet<String>();

   if (start_threads.size() == 0) usethreads = null;
   else {
      usethreads.addAll(start_threads);

      String q = "SELECT H.subtype FROM CompClassHierarchy H WHERE H.supertype";
      if (start_threads.size() == 1) q += " = '" + start_threads.get(0) + "'";
      else {
	 q += " IN (";
	 int ct = 0;
	 for (String s : start_threads) {
	    if (ct++ > 0) q += ",";
	    q += "'" + s + "'";
	  }
	 q += ")";
       }

      try {
	 ResultSet rs = comp_main.query(q);
	 while (rs.next()) {
	    String tnm = rs.getString(1);
	    usethreads.add(tnm);
	  }
       }
      catch (SQLException e) {
	 System.err.println("DYCOMP: Problem accessing data: " + e);
	 e.printStackTrace();
       }
    }

   comp_main.createTable(output_table,"class text,method text");

   for (String mtbl : method_table) {
      String q = "SELECT M.class, M.method, M.thread FROM " + mtbl + " M";
      if (source_restrict != null) q += " WHERE " + source_restrict;

      try {
	 ResultSet rs = comp_main.query(q);

	 while (rs.next()) {
	    String cls = rs.getString(1);
	    String mthd = rs.getString(2);
	    String thrd = rs.getString(3);
	    boolean fnd = false;
	    if (usethreads == null) fnd = true;
	    else {
	       for (StringTokenizer tok = new StringTokenizer(thrd,";"); !fnd && tok.hasMoreTokens(); ) {
		  String tnm = tok.nextToken();
		  int idx = tnm.indexOf("@");
		  if (idx > 0) tnm = thrd.substring(0,idx);
		  if (usethreads.contains(tnm)) fnd = true;
		}
	     }
	    if (fnd) {
	       String cmd = comp_main.beginInsert(output_table);
	       cmd = comp_main.addValue(cmd,cls);
	       cmd = comp_main.addValue(cmd,mthd);
	       comp_main.endInsert(cmd);
	     }
	  }
       }
      catch (SQLException e) {
	 System.err.println("DYCOMP: Problem accessing data: " + e);
	 e.printStackTrace();
       }

      comp_main.runSql();
    }

}



}	// end of class DycompAnalysisMethodThread




/* end of DycompAnalysisMethodThread.java */

