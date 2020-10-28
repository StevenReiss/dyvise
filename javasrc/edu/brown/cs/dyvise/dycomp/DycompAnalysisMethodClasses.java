/********************************************************************************/
/*										*/
/*		DycompAnalysisMethodClasses.java				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dycomp/DycompAnalysisMethodClasses.java,v 1.4 2012-10-05 00:52:02 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DycompAnalysisMethodClasses.java,v $
 * Revision 1.4  2012-10-05 00:52:02  spr
 *
 * is no longer used).
 *
 * Revision 1.3  2009-10-07 00:59:39  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.2  2009-09-27 23:57:44  spr
 * Updates for mac os/x.
 *
 * Revision 1.1  2009-09-19 00:08:21  spr
 * Code to compute relations and store them in the database.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dycomp;

import edu.brown.cs.dyvise.dyvise.DyviseDatabase;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;



class DycompAnalysisMethodClasses extends DycompAnalysis implements DycompConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		method_table;
private String		source_restrict;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DycompAnalysisMethodClasses(DycompMain dm)
{
   super(dm);
}



DycompAnalysisMethodClasses(DycompAnalysisMethodClasses proto,Element xml)
{
   super(proto,xml);

   method_table = getTableName(xml,"SOURCE");
   source_restrict = IvyXml.getTextElement(xml,"RESTRICT");
}




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

DycompAnalysis createNew(Element xml)
{
   DycompAnalysisMethodClasses da = new DycompAnalysisMethodClasses(this,xml);

   return da;
}



/********************************************************************************/
/*										*/
/*	Work methods								*/
/*										*/
/********************************************************************************/

void compute()
{
   comp_main.createTable(output_table,"class text,fraction real");

   String q = "SELECT class, method FROM " + method_table;
   if (source_restrict != null) q += " WHERE " + source_restrict;

   Map<String,Integer> totals = new HashMap<String,Integer>();
   int ctr = 0;

   try {
      ResultSet rs = comp_main.query(q);

      while (rs.next()) {
	 String cls = rs.getString(1);
	 String mthd = rs.getString(2);
	 Set<String> rslt = new HashSet<String>();
	 findTypesForMethod(cls,mthd,rslt);
	 expandResult(rslt);

	 for (String s : rslt) {
	    Integer iv = totals.get(s);
	    if (iv == null) totals.put(s,1);
	    else totals.put(s,iv+1);
	  }
	 ++ctr;
       }

      for (Map.Entry<String,Integer> ent : totals.entrySet()) {
	 String cmd = comp_main.beginInsert(output_table);
	 cmd = comp_main.addValue(cmd,ent.getKey());
	 float v = ((float) ent.getValue())/((float) ctr);
	 cmd = comp_main.addValue(cmd,v);
	 comp_main.endInsert(cmd);
       }
    }
   catch (SQLException e) {
      System.err.println("DYCOMP: Problem accessing data: " + e);
      e.printStackTrace();
    }

   comp_main.runSql();
}



private void expandResult(Set<String> rslt) throws SQLException
{
   if (rslt.size() == 0) return;

   String q1 = "SELECT H.supertype FROM CompClassHierarchy H, SrcClass C" +
      " WHERE C.name = H.supertype AND H.subtype IN (";
   int ctr = 0;
   for (String r : rslt) {
      if (ctr++ > 0) q1 += ",";
      q1 += DyviseDatabase.sqlString(r);
    }
   q1 += ")";
   ResultSet rs = comp_main.query(q1);
   while (rs.next()) {
      String cls = rs.getString(1);
      if (!rslt.contains(cls)) {
	 rslt.add(cls);
       }
    }
}




/********************************************************************************/
/*										*/
/*	Method to find field with type for given method 			*/
/*										*/
/********************************************************************************/

private void findTypesForMethod(String cls,String mthd,Set<String> rslt)
	throws SQLException
{
   // Start with method parameters
   String q = "SELECT C.name";
   q += " FROM SrcMethod M, SrcMethodParam P, SrcClass C";
   q += " WHERE M.class = '" + cls + "' AND M.name = '" + mthd + "'";
   q += " AND M.id = P.methodid AND P.type = C.type AND C.project";

   ResultSet rs = comp_main.query(q);

   while (rs.next()) {
      String pty = rs.getString(1);
      checkType(pty,cls,rslt);
    }

   // Also look at static fields
   String q1 = "SELECT C.name";
   q1 += " FROM SrcField F, SrcClass C";
   q1 += " WHERE F.class = '" + cls + "' AND F.static";
   q1 += " AND C.type = F.type AND C.project";
   rs = comp_main.query(q1);

   //TODO: restrict to avoid private fields not accessible in context

   while (rs.next()) {
      String fty = rs.getString(1);
      checkType(fty,cls,rslt);
    }

   // handle static methods ?
}



private void checkType(String cls,String origcls,Set<String> rslt) throws SQLException
{
   if (rslt.contains(cls)) return;
   rslt.add(cls);

   // TODO: restrict to avoid private inaccessible fields

   String q1 = "SELECT C.name";
   q1 += " FROM SrcField F, SrcClass C";
   q1 += " WHERE F.class = '" + cls + "'";
   q1 += " AND F.type = C.type AND C.project";
   q1 += " AND NOT F.static";
   if (!cls.equals(origcls)) q1 += " AND F.public";

   ResultSet rs1 = comp_main.query(q1);

   while (rs1.next()) {
      String fcls = rs1.getString(1);
      checkType(fcls,origcls,rslt);
    }

   // TODO: restrict to avoid private inaccessible methods

   String q2 = "SELECT C.name";
   q2 += " FROM SrcMethod M, SrcClass C";
   q2 += " WHERE M.class = '" + cls + "'";
   q2 += " AND M.numarg = 0 AND NOT M.static";
   q2 += " AND M.returns = C.type AND C.project";

   ResultSet rs2 = comp_main.query(q2);

   while (rs2.next()) {
      String rcls = rs2.getString(1);
      checkType(rcls,origcls,rslt);
    }

   String q3 = "SELECT C.name";
   q3 += " FROM SrcMethod M, SrcClass C, SrcMethodParam P, SrcClass D";
   q3 += " WHERE M.class = '" + cls + "'";
   q3 += " AND M.numarg = 1 AND M.static";
   q3 += " AND M.returns = C.type AND C.project";
   q3 += " AND P.methodid = M.id AND P.indexno = 0";
   q3 += " AND D.name = '" + cls + "' AND D.type = P.type";

   ResultSet rs3 = comp_main.query(q3);

   while (rs3.next()) {
      String rcls = rs3.getString(1);
      checkType(rcls,origcls,rslt);
    }
}



}	// end of class DycompAnalysisMethodClasses




/* end of DycompAnalysisMethodClasses.java */

