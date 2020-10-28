/********************************************************************************/
/*										*/
/*		DycompAnalysisAllocType.java					*/
/*										*/
/*	DYVISE computed relations analysis for allocations for given types	*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dycomp/DycompAnalysisAllocType.java,v 1.3 2009-10-07 00:59:39 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DycompAnalysisAllocType.java,v $
 * Revision 1.3  2009-10-07 00:59:39  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.2  2009-09-21 19:33:58  spr
 * Add which to xml definitions and use.
 *
 * Revision 1.1  2009-09-19 00:08:21  spr
 * Code to compute relations and store them in the database.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dycomp;


import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;



class DycompAnalysisAllocType extends DycompAnalysis implements DycompConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Collection<String> type_names;
private Map<String,Integer> type_which;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DycompAnalysisAllocType(DycompMain dm)
{
   super(dm);
}



DycompAnalysisAllocType(DycompAnalysisAllocType proto,Element xml)
{
   super(proto,xml);

   type_names = new ArrayList<String>();
   type_which = new HashMap<String,Integer>();
   for (Element e : IvyXml.children(xml,"TARGET")) {
      String s = IvyXml.getText(e);
      int idx = IvyXml.getAttrInt(e,"WHICH",1);
      type_names.add(s);
      type_which.put(s,idx);
    }
   String s = IvyXml.getAttrString(xml,"TARGET");
   if (s != null) {
      int idx = IvyXml.getAttrInt(xml,"WHICH",1);
      type_names.add(s);
      type_which.put(s,idx);
    }
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

DycompAnalysis createNew(Element xml)
{
   DycompAnalysisAllocType da = new DycompAnalysisAllocType(this,xml);

   return da;
}



/********************************************************************************/
/*										*/
/*	Work methods								*/
/*										*/
/********************************************************************************/

void compute()
{
   comp_main.createTable(output_table,"class text, method text, alloc text, which int");

   for (String s : type_names) {
      String q = "SELECT M.class, M.name, A.class" +
	 " FROM SrcMethod m, SrcAlloc A, CompClassHierarchy C" +
	 " WHERE m.id = a.methodid AND A.class = C.subtype" +
	 " AND C.supertype = '" + s + "'";

      try {
	 ResultSet rs = comp_main.query(q);

	 while (rs.next()) {
	    String cls = rs.getString(1);
	    String mthd = rs.getString(2);
	    String tgt = rs.getString(3);
	    int idx = type_which.get(s);

	    String cmd = comp_main.beginInsert(output_table);
	    cmd = comp_main.addValue(cmd,cls);
	    cmd = comp_main.addValue(cmd,mthd);
	    cmd = comp_main.addValue(cmd,tgt);
	    cmd = comp_main.addValue(cmd,idx);
	    comp_main.endInsert(cmd);
	  }
       }
      catch (SQLException e) {
	 System.err.println("DYCOMP: Problem accessing data: " + e);
	 e.printStackTrace();
       }
    }

   comp_main.runSql();
}




}	// end of class DycompAnalysisAllocType




/* end of DycompAnalysisAllocType.java */






































































































































