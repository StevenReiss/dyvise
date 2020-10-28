/********************************************************************************/
/*										*/
/*		DycompAnalysisMethodType.java					*/
/*										*/
/*	DYVISE computed relations analysis for finding type assoc with method	*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dycomp/DycompAnalysisMethodType.java,v 1.6 2013-08-05 12:03:32 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DycompAnalysisMethodType.java,v $
 * Revision 1.6  2013-08-05 12:03:32  spr
 * Updates; use dypatchasm.
 *
 * Revision 1.5  2012-10-05 00:52:02  spr
 *
 * is no longer used).
 *
 * Revision 1.4  2010-03-30 16:20:35  spr
 * Update analysis tables.
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


import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;



class DycompAnalysisMethodType extends DycompAnalysis implements DycompConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		method_table;
private List<String>	type_names;
private List<String>	task_names;
private String		source_restrict;
private int		best_level;
private String		best_candidate;

private static final int	MAX_LEVEL = 10;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DycompAnalysisMethodType(DycompMain dm)
{
   super(dm);
}



DycompAnalysisMethodType(DycompAnalysisMethodType proto,Element xml)
{
   super(proto,xml);

   method_table = getTableName(xml,"SOURCE");
   source_restrict = IvyXml.getTextElement(xml,"RESTRICT");

   type_names = new ArrayList<String>();
   for (Element e : IvyXml.children(xml,"TARGET")) {
      String s = IvyXml.getText(e);
      type_names.add(s);
    }
   String t = IvyXml.getAttrString(xml,"TARGET");
   if (t != null) type_names.add(t);

   task_names = new ArrayList<String>();
   for (Element e : IvyXml.children(xml,"TASK")) {
      String s = IvyXml.getText(e);
      task_names.add(s);
    }
   t = IvyXml.getAttrString(xml,"TASK");
   if (t != null) task_names.add(t);
}




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

DycompAnalysis createNew(Element xml)
{
   DycompAnalysisMethodType da = new DycompAnalysisMethodType(this,xml);

   return da;
}



/********************************************************************************/
/*										*/
/*	Work methods								*/
/*										*/
/********************************************************************************/

void compute() throws InterruptedException
{
   comp_main.createTable(output_table,"class text, method text, access text");

   String q = "SELECT class, method";
   q += " FROM " + method_table + " SOURCE";
   if (source_restrict != null) q += " WHERE " + source_restrict;

   try {
      ResultSet rs = comp_main.query(q);

      while (rs.next()) {
	 comp_main.checkAbort();

	 String cls = rs.getString(1);
	 String mthd = rs.getString(2);
	 String rslt = null;
	 String fld1 = findTypeForMethod(cls,mthd,type_names);
	 String fld2 = findTypeForMethod(cls,mthd,task_names);
	 if (!type_names.isEmpty() && fld1 == null) continue;
	 if (!task_names.isEmpty() && fld2 == null) continue;

	 if (fld1 == null) fld1 = "NULL";
	 if (fld2 == null) fld2 = "NULL";

	 rslt = fld1 + "+" + fld2;

	 String cmd = comp_main.beginInsert(output_table);
	 cmd = comp_main.addValue(cmd,cls);
	 cmd = comp_main.addValue(cmd,mthd);
	 cmd = comp_main.addValue(cmd,rslt);
	 comp_main.endInsert(cmd);
       }
    }
   catch (SQLException e) {
      System.err.println("DYCOMP: Problem accessing data: " + e);
      e.printStackTrace();
    }

   comp_main.runSql();
}




/********************************************************************************/
/*										*/
/*	Method to find field with type for given method 			*/
/*										*/
/********************************************************************************/

private String findTypeForMethod(String cls,String mthd,List<String> types)
	throws SQLException, InterruptedException
{
   if (types == null || types.size() == 0) return null;

   Map<String,Integer> done = new HashMap<String,Integer>();

   best_level = -1;
   best_candidate = null;

   String q0 = "SELECT C.name from compclasshierarchy H, SrcClass C";
   q0 += " WHERE C.project AND C.name = H.subtype AND H.supertype";
   if (types.size() == 1) q0 += " = '" + types.get(0) + "'";
   else {
      q0 += " IN (";
      int ct = 0;
      for (String s : types) {
	 if (ct++ > 0) q0 += ",";
	 q0 += "'" + s + "'";
       }
      q0 += ")";
    }

   Set<String> findtypes = new HashSet<String>(types);
   ResultSet rs = comp_main.query(q0);
   while (rs.next()) {
      String tnm = rs.getString(1);
      findtypes.add(tnm);
    }

   // Start with method parameters
   String q = "SELECT M.id, M.class, P.name, C.name, P.indexno";
   q += " FROM SrcMethod M, SrcMethodParam P, SrcClass C";
   q += " WHERE M.class = '" + cls + "' AND M.name = '" + mthd + "'";
   q += " AND M.id = P.methodid AND P.type = C.type AND C.project";

   rs = comp_main.query(q);

   while (rs.next()) {
      String pty = rs.getString(4);
      int pidx = rs.getInt(5);

      String nm = Integer.toString(pidx);

      checkType(pty,0,nm,done,cls,findtypes);
    }

   // Also look at static fields
   String q1 = "SELECT F.name,F.class,C.name";
   q1 += " FROM SrcField F, SrcClass C";
   q1 += " WHERE F.class = '" + cls + "' AND F.static";
   q1 += " AND C.type = F.type AND C.project";
   rs = comp_main.query(q1);

   //TODO: restrict to avoid private fields not accessible in context

   while (rs.next()) {
      String fnm = rs.getString(1);
      String fcl = rs.getString(2);
      String fty = rs.getString(3);
      String nm = "@" + fcl + "." + fnm + ">" + fty;
      checkType(fty,0,nm,done,cls,findtypes);
    }

   // handle static methods ?

   return best_candidate;
}



private void checkType(String cls,int lvl,String text,Map<String,Integer> done,
			  String origcls,Set<String> findtypes)
		throws SQLException, InterruptedException
{
   if (best_level >= 0 && lvl > best_level) return;
   if (lvl > MAX_LEVEL) return;
   comp_main.checkAbort();

   Integer v = done.get(cls);
   if (v != null && v <= lvl) return;
   done.put(cls,lvl);

   if (findtypes.contains(cls)) {
      addCandidate(lvl,text);
      return;
    }

   // TODO: restrict to avoid private inaccessible fields

   String q1 = "SELECT F.name, C.name";
   q1 += " FROM SrcField F, SrcClass C";
   q1 += " WHERE F.class = '" + cls + "'";
   q1 += " AND F.type = C.type AND C.project";
   q1 += " AND NOT F.static";
   if (!cls.equals(origcls)) q1 += " AND F.public";

   ResultSet rs1 = comp_main.query(q1);

   while (rs1.next()) {
      String fnm = rs1.getString(1);
      String fcls = rs1.getString(2);
      String nm = text + ";" + "@" + cls + "." + fnm + ">" + fcls;
      checkType(fcls,lvl+1,nm,done,origcls,findtypes);
    }

   // TODO: restrict to avoid private inaccessible methods

   String q2 = "SELECT M.name, C.name, C.type";
   q2 += " FROM SrcMethod M, SrcClass C";
   q2 += " WHERE M.class = '" + cls + "'";
   q2 += " AND M.numarg = 0 AND NOT M.static";
   q2 += " AND M.returns = C.type AND C.project";

   ResultSet rs2 = comp_main.query(q2);

   while (rs2.next()) {
      String mnm = rs2.getString(1);
      String rcls = rs2.getString(2);
      String rtyp = rs2.getString(3);
      String nm = text + ";" + "^" + cls + "." + mnm + ">" + rtyp;
      checkType(rcls,lvl+1,nm,done,origcls,findtypes);
    }

   String q3 = "SELECT M.name, C.name, C.type";
   q3 += " FROM SrcMethod M, SrcClass C, SrcMethodParam P, SrcClass D";
   q3 += " WHERE M.class = '" + cls + "'";
   q3 += " AND M.numarg = 1 AND M.static";
   q3 += " AND M.returns = C.type AND C.project";
   q3 += " AND P.methodid = M.id AND P.indexno = 0";
   q3 += " AND D.name = '" + cls + "' AND D.type = P.type";

   ResultSet rs3 = comp_main.query(q3);

   while (rs3.next()) {
      String mnm = rs3.getString(1);
      String rcls = rs3.getString(2);
      String rtyp = rs3.getString(3);
      String nm = text + ";" + "^" + cls + "." + mnm + ">" + rtyp;
      checkType(rcls,lvl+1,nm,done,origcls,findtypes);
    }
}



private void addCandidate(int lvl,String txt)
{
   if (best_level >= 0) {
      if (lvl > best_level) return;
      if (lvl == best_level && best_candidate.length() < txt.length()) return;
    }

   best_level = lvl;
   best_candidate = txt;
}



}	// end of class DycompAnalysisMethodType




/* end of DycompAnalysisMethodType.java */

