/********************************************************************************/
/*										*/
/*		DynamoPatchCodeEventState.java					*/
/*										*/
/*	Event-State patching methods						*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dynamo/DynamoPatchCodeEventState.java,v 1.4 2011-03-10 02:33:09 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DynamoPatchCodeEventState.java,v $
 * Revision 1.4  2011-03-10 02:33:09  spr
 * Code cleanup.
 *
 * Revision 1.3  2010-03-30 16:22:15  spr
 * Fix up patching problems.
 *
 * Revision 1.2  2009-10-07 01:00:16  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.1  2009-09-19 00:10:00  spr
 * Module to generate model for visualization.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dynamo;


import edu.brown.cs.dyvise.dyvise.*;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.sql.ResultSet;
import java.sql.SQLException;



class DynamoPatchCodeEventState extends DynamoPatchCode implements DynamoConstants, DyviseConstants
{




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static final EventParam THREAD = EventParam.P0;
private static final EventParam TRANSACTION = EventParam.P1;
private static final EventParam TASK = EventParam.P2;
private static final EventParam TIME = EventParam.TIME;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DynamoPatchCodeEventState()
{ }



/********************************************************************************/
/*										*/
/*	Patching methods							*/
/*										*/
/********************************************************************************/

@Override void buildPatch(IvyXmlWriter xw,DynamoPatchDescriptor pd,DyviseDatabase db)
{
   String evts = pd.getValue("EVENTS");
   String alcs = pd.getValue("ALLOCS");
   String tstatetbl = pd.getValue("THREADSTATES");
   String evtrtntbl = pd.getValue("EVENTROUTINES");

   fixLines(pd,db,tstatetbl);

   beginTupleModel(xw);
   beginTuple(xw,"EVENT",0);
   tupleField(xw,"THREAD","THREAD");
   tupleField(xw,"TRANSACTION","OBJECT");
   tupleField(xw,"TASK","OBJECT");
   tupleField(xw,"STATE","STRING");
   tupleField(xw,"START","START_TIME");
   tupleField(xw,"CPU","INTERVAL");
   tupleField(xw,"END","END_TIME");
   endTuple(xw);
   beginTuple(xw,"CREATE",1);
   tupleField(xw,"THREAD","THREAD");
   tupleField(xw,"TIME","START_TIME");
   tupleField(xw,"TRANSACTION","OBJECT");
   tupleField(xw,"TASK","OBJECT");
   endTuple(xw);
   endTupleModel(xw);

   beginGraphModel(xw);
   beginGraph(xw,"Events over Time","TIMEROWS");
   graphSelect(xw,"ROWDATA");
   graphSelectTable(xw,"EVENT",true);
   graphEndSelect(xw);
   graphSelect(xw,"ROW",10);
   graphSelectField(xw,"EVENT","THREAD","SORT_NAME",true);
   graphSelectField(xw,"EVENT","THREAD","SORT_TIME",false);
   graphSelectField(xw,"EVENT","TRANSACTION","SORT_NAME",false);
   graphSelectField(xw,"EVENT","TRANSACTION","SORT_TIME",false);
   graphEndSelect(xw);
   graphSelect(xw,"HUE");
   graphSelectConst(xw,"NONE","0.5",null,false);
   graphSelectField(xw,"EVENT","THREAD","MODE",false);
   graphSelectField(xw,"EVENT","TRANSACTION","MODE",true);
   graphSelectField(xw,"EVENT","STATE","MODE",false);
   graphEndSelect(xw);
   graphSelect(xw,"SAT");
   graphSelectConst(xw,"NONE","1.0","0.0",true);
   graphEndSelect(xw);
   graphSelect(xw,"VALUE");
   graphSelectConst(xw,"NONE","1.0",null,true);
   graphEndSelect(xw);
   graphSelect(xw,"ROWITEM",5);
   graphSelectConst(xw,"NONE","0",null,false);
   graphSelectField(xw,"EVENT","TRANSACTION",null,false);
   graphSelectField(xw,"EVENT","STATE",null,true);
   graphEndSelect(xw);
   graphSelect(xw,"WIDTH");
   graphSelectConst(xw,"NONE","1.0",null,true);
   graphEndSelect(xw);
   graphSelect(xw,"ITEMWIDTH");
   graphSelectInterval(xw,"EVENT",true);
   graphEndSelect(xw);
   graphSelect(xw,"LINKDATA");
   graphSelectTable(xw,"CREATE",true);
   graphEndSelect(xw);
   graphSelect(xw,"DATA1");
   graphSelectConst(xw,"NONE","0",null,false);
   graphSelectField(xw,"CREATE","TRANSACTION",null,true);
   graphEndSelect(xw);
   graphSelect(xw,"DATA2");
   graphSelectConst(xw,"NONE","0",null,true);
   graphSelectField(xw,"CREATE","TASK",null,false);
   graphEndSelect(xw);
   graphSelect(xw,"LHUE");
   graphSelectConst(xw,"NONE","0",null,true);
   graphSelectField(xw,"EVENT","TRANSACTION",null,false);
   graphEndSelect(xw);
   graphSelect(xw,"LSAT");
   graphSelectConst(xw,"GRAY","0",null,true);
   graphSelectConst(xw,"COLORED","1",null,true);
   graphEndSelect(xw);
   graphSelect(xw,"LVALUE");
   graphSelectConst(xw,"GRAY","0.5",null,true);
   graphEndSelect(xw);
   graphSelect(xw,"LENDHUE");
   graphSelectConst(xw,"NONE","0",null,true);
   graphSelectField(xw,"EVENT","TRANSACTION",null,false);
   graphEndSelect(xw);
   graphSelect(xw,"LENDSAT");
   graphSelectConst(xw,"GRAY","0",null,true);
   graphSelectConst(xw,"COLORED","1",null,true);
   graphEndSelect(xw);
   graphSelect(xw,"LENDVALUE");
   graphSelectConst(xw,"GRAY","0.5",null,true);
   graphEndSelect(xw);
   graphSelect(xw,"LALPHA");
   graphSelectConst(xw,"HALF","0.5",null,true);
   graphSelectConst(xw,"NONE","1.0",null,false);
   graphEndSelect(xw);
   graphSelect(xw,"SPLITROWS");
   graphSelectConst(xw,"INTERIOR","0",null,true);
   graphSelectConst(xw,"AS ROWS","1",null,false);
   graphEndSelect(xw);
   endGraph(xw);
   endGraphModel(xw);

   beginModel(xw,"EVENTSTATE");

   String q1 = "SELECT DISTINCT E.class, E.method, A.access" +
      " FROM " + evtrtntbl + " E, " + evts + " A" +
      " WHERE E.class = A.class AND E.method = A.method";

   String q2 = "SELECT A.class, A.method, A.alloc, A.which" +
      " FROM " + alcs + " A";

   String q3 = "SELECT S.class, S.method, S.lineno, S.type, L.startoffset, C.source" +
      " FROM " + tstatetbl + " S, SrcMethod M, SrcLines L, SrcClass C" +
      " WHERE M.name = S.method AND M.class = S.class AND L.methodid = M.id AND" +
      " L.lineno = S.lineno AND M.class = C.name";

   int eid = 1;
   try {
      ResultSet rs = db.query(q1);
      while (rs.next()) {
	 String cls = rs.getString(1);
	 String mthd = rs.getString(2);
	 String acc = rs.getString(3);
	 String acc1 = null;
	 int idx0 = acc.indexOf("+");
	 if (idx0 > 0) {
	    acc1 = acc.substring(idx0+1);
	    acc = acc.substring(0,idx0);
	  }

	 beginMethod(xw,cls,mthd);
	 DyviseEvent e = defineEvent(eid,"STARTEVENT " + cls + "." + mthd);
	 e.addBeginTuple(THREAD,0);
	 e.addSetField("THREAD",THREAD);
	 e.addSetField("TRANSACTION",TRANSACTION);
	 e.addSetField("TASK",TASK);
	 e.addSetField("STATE","BEGIN@" + cls + "." + mthd);
	 // e.addSetField("STATE","INITIAL");
	 e.addSetField("START",TIME);
	 e.addSetField("CPU",TIME);
	 handleStartEvent(xw,eid++,DYNAMO_ARG_THREAD,acc,acc1);
	 e = defineEvent(eid,"ENDEVENT " + cls + "." + mthd);
	 e.addFindTuple(THREAD,0);
	 e.addSetField("END",TIME);
	 e.addSetDiff("CPU",TIME);
	 e.addOutputDiscard();
	 handleExitEvent(xw,eid++,DYNAMO_ARG_THREAD,null,null);
	 endMethod(xw);
       }

      rs = db.query(q2);
      while (rs.next()) {
	 String cls = rs.getString(1);
	 String mthd = rs.getString(2);
	 String typ = rs.getString(3);
	 int which = rs.getInt(4);
	 beginMethod(xw,cls,mthd);
	 DyviseEvent e = defineEvent(eid,"ALLOC " + cls + "." + mthd + " > " + typ);
	 e.addFindNewTuple(THREAD,0);
	 e.addIfNew();
	 e.addSetField("THREAD",THREAD);
	 e.addSetField("TRANSACTION",TRANSACTION);
	 e.addSetField("TASK",TASK);
	 e.addSetField("STATE","ALLOC");
	 e.addSetField("START",TIME);
	 e.addSetField("CPU",TIME);
	 e.addEndIf();
	 e.addBeginTuple(THREAD,1);
	 e.addSetField("THREAD",THREAD);
	 e.addSetField("TRANSACTION",TRANSACTION);
	 e.addSetField("TASK",TASK);
	 e.addSetField("TIME",TIME);
	 e.addOutputDiscard();
	 String p1 = (which == 1 ? DYNAMO_ARG_STACK : DYNAMO_ARG_NULL);
	 String p2 = (which == 2 ? DYNAMO_ARG_STACK : DYNAMO_ARG_NULL);
	 handleAllocEvent(xw,eid++,typ,DYNAMO_ARG_THREAD,p1,p2);
	 endMethod(xw);
       }

      rs = db.query(q3);
      while (rs.next()) {
	 String cls = rs.getString(1);
	 String mthd = rs.getString(2);
	 int line = rs.getInt(3);
	 int addr = rs.getInt(5);
	 beginMethod(xw,cls,mthd);
	 DyviseEvent e = defineEvent(eid,"THREADSTATE " + cls + "." + mthd + " @ " + line);
	 e.addFindTuple(THREAD,0);
	 e.addSetField("END",TIME);
	 e.addSetDiff("CPU",TIME);
	 e.addOutputTuple();
	 e.addSetField("START",TIME);
	 e.addSetField("CPU",TIME);
	 e.addSetField("STATE",cls + "." + mthd + "@" + line);
	 handleLineEvent(xw,eid++,line,addr,DYNAMO_ARG_THREAD,null,null);
	 endMethod(xw);
       }
    }
   catch (SQLException e) {
      System.err.println("DYNAMO: Problem accessing data: " + e);
      System.exit(1);
    }

   endModel(xw);
}




}	// end of class DynamoPatchCodeEventState




/* end of DynamoPatchCodeEventState.java */
