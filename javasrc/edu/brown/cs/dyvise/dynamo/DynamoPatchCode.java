/********************************************************************************/
/*										*/
/*		DynamoPatchCode.java						*/
/*										*/
/*	DYVISE dyanmic analysis patch generic handler				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dynamo/DynamoPatchCode.java,v 1.5 2013-08-05 12:03:37 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DynamoPatchCode.java,v $
 * Revision 1.5  2013-08-05 12:03:37  spr
 * Updates; use dypatchasm.
 *
 * Revision 1.4  2011-03-10 02:33:09  spr
 * Code cleanup.
 *
 * Revision 1.3  2010-03-30 16:22:15  spr
 * Fix up patching problems.
 *
 * Revision 1.2  2009-10-07 01:00:15  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.1  2009-09-19 00:10:00  spr
 * Module to generate model for visualization.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dynamo;


import edu.brown.cs.dyvise.dyvise.*;
import edu.brown.cs.ivy.limbo.LimboFactory;
import edu.brown.cs.ivy.limbo.LimboLine;
import edu.brown.cs.ivy.project.IvyProject;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;



abstract class DynamoPatchCode implements DynamoConstants, DyviseConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private List<DyviseEvent> event_list;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected DynamoPatchCode()
{
   event_list = new ArrayList<DyviseEvent>();
}



/********************************************************************************/
/*										*/
/*	Generic methods 							*/
/*										*/
/********************************************************************************/

abstract void buildPatch(IvyXmlWriter patchxw,DynamoPatchDescriptor pd,DyviseDatabase db);




/********************************************************************************/
/*										*/
/*	Methods for handling generic patch setups				*/
/*										*/
/********************************************************************************/

protected void beginModel(IvyXmlWriter xw,String name)
{
   xw.begin("PATCHMODEL");
   xw.field("NAME",name);
}



protected void endModel(IvyXmlWriter xw)
{
   xw.end("PATCHMODEL");
}



protected void beginMethod(IvyXmlWriter xw,String cls,String mthd)
{
   xw.begin("FOR");
   xw.field("CLASS",cls);
   xw.field("METHOD",mthd);
}



protected void endMethod(IvyXmlWriter xw)
{
   xw.end("FOR");
}




/********************************************************************************/
/*										*/
/*	Tuple description methods						*/
/*										*/
/********************************************************************************/

protected void beginTupleModel(IvyXmlWriter xw)
{
   xw.begin("TUPLEMODEL");
}


protected void beginTuple(IvyXmlWriter xw,String id,int idx)
{
   xw.begin("TUPLE");
   xw.field("NAME",id);
   xw.field("INDEX",idx);
}


protected void tupleField(IvyXmlWriter xw,String name,String type)
{
   xw.begin("FIELD");
   xw.field("NAME",name);
   xw.field("TYPE",type);
   xw.end("FIELD");
}

protected void endTuple(IvyXmlWriter xw)
{
   xw.end("TUPLE");
}



protected void endTupleModel(IvyXmlWriter xw)
{
   xw.end("TUPLEMODEL");
}



/********************************************************************************/
/*										*/
/*	Methods for inserting events						*/
/*										*/
/********************************************************************************/

protected void handleStartEvent(IvyXmlWriter xw,int eid,String p1,String p2,String p3)
{
   xw.begin("PATCH");
   xw.field("WHAT","ENTER");
   xw.begin("MODE");
   xw.field("POST",false);
   generateEventCall(xw,eid,p1,p2,p3);
   xw.end("MODE");
   xw.end("PATCH");
}



protected void handleExitEvent(IvyXmlWriter xw,int eid,String p1,String p2,String p3)
{
   xw.begin("PATCH");
   xw.field("WHAT","EXIT");
   xw.begin("MODE");
   generateEventCall(xw,eid,p1,p2,p3);
   xw.end("MODE");
   xw.end("PATCH");
}



protected void handleLineEvent(IvyXmlWriter xw,int eid,int line,int addr,String p1,String p2,String p3)
{
   xw.begin("PATCH");
   xw.field("WHAT","LINE");
   xw.begin("MODE");
   generateEventCall(xw,eid,p1,p2,p3);
   xw.end("MODE");
   xw.begin("WHEN");
   xw.field("CASE","LINE");
   xw.field("ARG",line);
   xw.end("WHEN");
   xw.end("PATCH");
}


protected void handleAllocEvent(IvyXmlWriter xw,int eid,String typ,String p1,String p2,String p3)
{
   xw.begin("PATCH");
   xw.field("WHAT","CALL");
   xw.begin("MODE");
   xw.field("POST",true);
   generateEventCall(xw,eid,p1,p2,p3);
   xw.end("MODE");
   xw.begin("WHEN");
   xw.field("CASE","METHOD");
   xw.field("ARG",typ + ".<init>");
   xw.end("WHEN");
   xw.end("PATCH");
}



protected void generateEventCall(IvyXmlWriter xw,int eid,String p1,String p2,String p3)
{
   String sgn = "(int";
   if (p1 != null) sgn += ",java.lang.Object";
   if (p2 != null) sgn += ",java.lang.Object";
   if (p3 != null) sgn += ",java.lang.Object";
   sgn += ")";

   xw.begin("CODE");
   xw.field("TYPE","CALL");
   xw.field("CLASS",DYNAMO_PATCH_CLASS);
   xw.field("METHOD",DYNAMO_PATCH_METHOD);
   xw.field("SIGNATURE",sgn);
   xw.begin("ARG");
   xw.field("TYPE","INTEGER");
   xw.field("VALUE",eid);
   xw.end("ARG");
   if (p1 != null) generateArg(xw,p1);
   if (p2 != null) generateArg(xw,p2);
   if (p3 != null) generateArg(xw,p3);
   xw.end("CODE");
}




protected void generateArg(IvyXmlWriter xw,String acc)
{
   xw.begin("ARG");
   if (acc.equals(DYNAMO_ARG_THREAD)) {
      xw.field("TYPE","CURTHREAD");
    }
   else if (acc.equals(DYNAMO_ARG_THIS)) {
      xw.field("TYPE","THIS");
    }
   else if (acc.equals(DYNAMO_ARG_NULL)) {
      xw.field("TYPE","NULL");
    }
   else if (acc.equals(DYNAMO_ARG_STACK)) {
      xw.field("TYPE","STACK");
    }
   else if (acc.equals(DYNAMO_ARG_LOCAL0)) {
      xw.field("TYPE","THIS");
    }
   else {
      StringTokenizer tok = new StringTokenizer(acc,";");
      xw.field("TYPE","MULTIPLE");
      int level = 0;
      while (tok.hasMoreTokens()) {
	 xw.begin("ACCESS");
	 String id = tok.nextToken();
	 if (Character.isDigit(id.charAt(0))) { 	// # -> local
	    xw.field("TYPE","LOCAL");
	    xw.field("LOCAL",id);
	  }
	 else if (id.charAt(0) == '@') {                // @class.field>type
	    if (level == 0) xw.field("TYPE","STATICFIELD");
	    else xw.field("TYPE","FIELD");

	    int idx = id.lastIndexOf(">");
	    String rslt = id.substring(idx+1);
	    id = id.substring(1,idx);
	    idx = id.lastIndexOf(".");
	    xw.field("MEMBER",id.substring(idx+1));
	    xw.field("CLASS",id.substring(0,idx));
	    xw.field("RESULT",rslt);
	  }
	 else if (id.charAt(0) == '^') {                // ^class.method>type
	    if (level == 0) xw.field("TYPE","STATICMETHOD0");
	    else xw.field("TYPE","METHOD0");
	    int idx = id.lastIndexOf(">");
	    String rslt = id.substring(idx+1);
	    id = id.substring(1,idx);
	    idx = id.lastIndexOf(".");
	    xw.field("MEMBER",id.substring(idx+1));
	    xw.field("CLASS",id.substring(1,idx));
	    xw.field("RESULT",rslt);
	  }
	 else {
	    System.err.println("UNKNOWN ACCESS ARGUMENT: " + id);
	    System.exit(1);
	  }
	 xw.end("ACCESS");
	 ++level;
       }
    }

   xw.end("ARG");
}




/********************************************************************************/
/*										*/
/*	Event file methods							*/
/*										*/
/********************************************************************************/

void outputEvents(IvyXmlWriter xw)
{
   for (DyviseEvent e : event_list) {
      e.outputXml(xw);
    }
}




protected DyviseEvent defineEvent(int eid,String name)
{
   DyviseEvent ei = new DyviseEvent(eid,name);

   event_list.add(ei);

   return ei;
}



/********************************************************************************/
/*										*/
/*	Methods to take care of line numbers					*/
/*										*/
/********************************************************************************/

protected void fixLines(DynamoPatchDescriptor pd,DyviseDatabase db,String tbl)
{
   IvyProject proj = pd.getProject();

   String q1 = "SELECT T.class, T.lineno, T.lineinfo FROM " + tbl + " T";

   try {
      ResultSet rs = db.testQuery(q1);

      while (rs.next()) {
	 String cnm = rs.getString(1);
	 int lno = rs.getInt(2);
	 String linfo = rs.getString(3);
	 File f = proj.findSourceFile(cnm);
	 if (f == null) continue;
	 boolean upd = false;
	 LimboLine ll = null;
	 if (linfo != null) {
	    ll = LimboFactory.createFromXml(linfo);
	    if (ll == null) continue;
	    ll.revalidate();
	    if (ll.getLine() != lno) {
	       String u1 = "UPDATE " + tbl + " SET lineno = " + ll.getLine() +
		  " WHERE class = '" + cnm + "' AND lineno = " + lno;
	       db.addSql(u1);
	       lno = ll.getLine();
	       upd = true;
	     }
	  }
	 else {
	    ll = LimboFactory.createLine(f,lno);
	    upd = true;
	  }
	 if (upd) {
	    String v = DyviseDatabase.sqlString(ll.getXml());
	    String u2 = "UPDATE " + tbl + " SET lineinfo = " + v +
		  " WHERE class = '" + cnm + "' AND lineno = " + lno;
	    db.addSql(u2);
	  }
       }
      db.runSql();
    }
   catch (SQLException e) {
      System.err.println("DYNAMO: Problem updating line numbers: " + e);
      System.err.println("DYNAMO: Table = " + tbl);
      e.printStackTrace();
    }
}




/********************************************************************************/
/*										*/
/*	Graphics model description						*/
/*										*/
/********************************************************************************/

protected void beginGraphModel(IvyXmlWriter xw)
{
   xw.begin("GRAPHMODEL");
}



protected void beginGraph(IvyXmlWriter xw,String name,String type)
{
   xw.begin("GRAPH");
   xw.field("NAME",name);
   xw.field("TYPE",type);
}



protected void graphSelect(IvyXmlWriter xw,String name)
{
   xw.begin("SELECT");
   xw.field("WHAT",name);
}



protected void graphSelect(IvyXmlWriter xw,String name,int sz)
{
   xw.begin("SELECT");
   xw.field("WHAT",name);
   xw.field("SIZE",sz);
}



protected void graphSelectTable(IvyXmlWriter xw,String tbl,boolean dflt)
{
   xw.begin("TABLE");
   xw.field("NAME",tbl);
   if (dflt) xw.field("DEFAULT",true);
   xw.end("TABLE");
}


protected void graphSelectField(IvyXmlWriter xw,String tbl,String fld,String op,boolean dflt)
{
   xw.begin("FIELD");
   xw.field("TABLE",tbl);
   xw.field("NAME",fld);
   if (dflt) xw.field("DEFAULT",true);
   if (op != null) {
      for (StringTokenizer tok = new StringTokenizer(op,", "); tok.hasMoreTokens(); ) {
	 String on = tok.nextToken();
	 if (on.startsWith("SORT_")) xw.field("SORT",on);
	 else xw.field("OP",on);
       }
    }

   xw.end("FIELD");
}



protected void graphSelectInterval(IvyXmlWriter xw,String tbl,boolean dflt)
{
   xw.begin("INTERVAL");
   xw.field("TABLE",tbl);
   if (dflt) xw.field("DEFAULT",true);
   xw.end("INTERVAL");
}



protected void graphSelectConst(IvyXmlWriter xw,String id,String val,String nval,boolean dflt)
{
   xw.begin("CONST");
   xw.field("NAME",id);
   xw.field("VALUE",val);
   if (nval != null) xw.field("NULL",nval);
   if (dflt) xw.field("DEFAULT",true);
   xw.end("CONST");
}



protected void graphEndSelect(IvyXmlWriter xw)
{
   xw.end("SELECT");
}


protected void endGraph(IvyXmlWriter xw)
{
   xw.end("GRAPH");
}



protected void endGraphModel(IvyXmlWriter xw)
{
   xw.end("GRAPHMODEL");
}




}	// end of class DynamoPatchCode




/* end of DynamoPatchCode.java */

