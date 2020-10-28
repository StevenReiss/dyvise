/********************************************************************************/
/*										*/
/*		DycompMain.java 						*/
/*										*/
/*	DYVISE computed relations controller					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dycomp/DycompMain.java,v 1.4 2010-06-01 02:45:59 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DycompMain.java,v $
 * Revision 1.4  2010-06-01 02:45:59  spr
 * Minor bug fixes.
 *
 * Revision 1.3  2010-03-30 16:20:35  spr
 * Update analysis tables.
 *
 * Revision 1.2  2009-10-07 00:59:39  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.1  2009-09-19 00:08:21  spr
 * Code to compute relations and store them in the database.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dycomp;


import edu.brown.cs.dyvise.dyvise.DyviseDatabase;
import edu.brown.cs.dyvise.dyvise.DyviseException;
import edu.brown.cs.ivy.project.IvyProject;
import edu.brown.cs.ivy.project.IvyProjectManager;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


public class DycompMain implements DycompConstants
{


/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   DycompMain dm = new DycompMain(args);

   dm.process();
}



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IvyProject	user_project;
private Map<String,String> name_map;
private List<DycompAnalysis> analysis_set;
private Map<String,DycompAnalysis> base_analyses;
private DyviseDatabase	using_database;
private String		database_name;
private String		start_name;
private boolean 	is_aborted;

private boolean 	do_debug;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private DycompMain(String [] args)
{
   user_project = null;
   do_debug = false;
   name_map = new HashMap<String,String>();
   analysis_set = new ArrayList<DycompAnalysis>();
   using_database = new DyviseDatabase();
   database_name = null;
   start_name = null;
   is_aborted = false;

   setupBaseAnalyses();

   scanArgs(args);
}


public DycompMain(IvyProject ip)
{
   user_project = ip;
   do_debug = false;
   name_map = new HashMap<String,String>();
   name_map.put("PROJECT",ip.getName());
   analysis_set = null;
   using_database = new DyviseDatabase();
   database_name = null;
   start_name = null;

   setupBaseAnalyses();
}




private void setupBaseAnalyses()
{
   base_analyses = new HashMap<String,DycompAnalysis>();
   base_analyses.put("METHODTYPE",new DycompAnalysisMethodType(this));
   base_analyses.put("METHODCLASSES",new DycompAnalysisMethodClasses(this));
   base_analyses.put("CLOSURE",new DycompAnalysisClosure(this));
   base_analyses.put("ALLOCTYPE",new DycompAnalysisAllocType(this));
   base_analyses.put("METHODTHREAD",new DycompAnalysisMethodThread(this));
   base_analyses.put("VIEW",new DycompAnalysisView(this));
}




/********************************************************************************/
/*										*/
/*	Argument scanning methods						*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   String projnm = null;
   String projdir = null;

   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
	 if (args[i].startsWith("-p") && i+1 < args.length) {           // -p <project>
	    projnm = args[++i];
	    name_map.put("PROJECT",projnm);
	  }
	 else if (args[i].startsWith("-d") && i+1 < args.length) {      // -d <project dir>
	    projdir = args[++i];
	    name_map.put("PROJDIR",projdir);
	  }
	 else if (args[i].startsWith("-DEBUG")) {                       // -DEBUG
	    do_debug = true;
	  }
	 else if (args[i].startsWith("-i") && i+1 < args.length) {      // -id <db id>
	    name_map.put("DBID",args[++i]);
	  }
	 else if (args[i].startsWith("-s") && i+1 < args.length) {      // -s <start name>
	    start_name = args[++i];
	  }
	 else if (args[i].startsWith("-DB") && i+1 < args.length) {     // -DB <database>
	    database_name = args[++i];
	  }
	 else badArgs();
       }
      else {
	 loadAnalysis(args[i]);
       }
    }

   if (projnm == null) badArgs();

   IvyProjectManager pm = IvyProjectManager.getManager(projdir);
   user_project = pm.findProject(projnm);
}



private void badArgs()
{
   System.err.println("DYCOMP: dystatic -p <project> -i <databaseid> [-d <projectdir>] analysis_file ...");
   System.exit(1);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

boolean doDebug()				{ return do_debug; }

IvyProject getProject() 			{ return user_project; }

String getDatabaseName()
{
   if (database_name != null) return database_name;

   return DyviseDatabase.getDatabaseName(name_map);
}


String getStartName()				{ return start_name; }





/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

public boolean processFile(String file,String main)
{
   analysis_set = new ArrayList<DycompAnalysis>();
   start_name = main;

   loadAnalysis(file);

   return process();
}




public boolean processXml(String xml,String main)
{
   return processXml(IvyXml.convertStringToXml(xml),main);
}



public boolean processXml(Element xml,String main)
{
   analysis_set = new ArrayList<DycompAnalysis>();
   start_name = main;

   if (IvyXml.isElement(xml,"COMPUTE")) loadCompute("INTERNAL XML",xml);
   else {
      for (Element c : IvyXml.elementsByTag(xml,"COMPUTE")) {
	 loadCompute("INTERNAL XML",c);
       }
    }

   return process();
}





private boolean process()
{
   using_database.connect(getDatabaseName());

   for (DycompAnalysis da : analysis_set) {
      try {
	 da.compute();
       }
      catch (InterruptedException e) {
	 return false;
       }
      catch (DyviseException e) {
	 System.err.println("DYCOMP: Problem evaluating analysis: " + e);
	 e.printStackTrace();
	 System.exit(1);
       }
    }

   return true;
}



/********************************************************************************/
/*										*/
/*	Methods to load an analysis request file				*/
/*										*/
/********************************************************************************/

private void loadAnalysis(String file)
{
   Element e = IvyXml.loadXmlFromFile(file);
   if (e == null) {
      System.err.println("DYCOMP: File " + file + " is not a valid analysis file");
      System.exit(1);
    }

   if (IvyXml.isElement(e,"COMPUTE")) loadCompute(file,e);
   else {
      for (Element c : IvyXml.elementsByTag(e,"COMPUTE")) {
	 loadCompute(file,c);
       }
    }
}



private void loadCompute(String file,Element c)
{
   String typ = IvyXml.getAttrString(c,"TYPE");
   if (typ == null) {
      System.err.println("DYCOMP: No analysis type found in file " + file);
      System.err.println(IvyXml.convertXmlToString(c));
      System.exit(1);
    }
   DycompAnalysis proto = base_analyses.get(typ);
   if (proto == null) {
      System.err.println("DYCOMP: Analysis type " + typ + " not known in file " + file);
      System.exit(1);
      return;
    }
   try {
      DycompAnalysis na = proto.createNew(c);
      analysis_set.add(na);
    }
   catch (DyviseException e) {
      System.err.println("DYCOMP: Problem setting up analysis: " + e);
      e.printStackTrace();
      System.exit(1);
    }
}



/********************************************************************************/
/*										*/
/*	Abort methods								*/
/*										*/
/********************************************************************************/

public void abort()
{
   is_aborted = true;
}



void checkAbort() throws InterruptedException
{
   if (is_aborted) {
      is_aborted = false;
      throw new InterruptedException("User Abort");
    }
}




/********************************************************************************/
/*										*/
/*	Database methods							*/
/*										*/
/********************************************************************************/

void deleteAll(String tbl)			{ using_database.deleteAll(tbl); }


ResultSet query(String q)
{
   if (do_debug) System.err.println("DYCOMP: QUERY: " + q);
   return using_database.query(q);
}


String beginInsert(String tbl)			{ return using_database.beginInsert(tbl); }

String addValue(String cmd,String val)		{ return using_database.addValue(cmd,val); }

String addValue(String cmd,int v)		{ return using_database.addValue(cmd,v); }

String addValue(String cmd,boolean v)		{ return using_database.addValue(cmd,v); }

String addValue(String cmd,float v)		{ return using_database.addValue(cmd,v); }

String addValue(String cmd,double v)		{ return using_database.addValue(cmd,v); }

String addNull(String cmd)			{ return using_database.addNull(cmd); }

void endInsert(String cmd)			{ using_database.endInsert(cmd); }

void runSql()					{ using_database.runSql(); }


void createTable(String tbl,String cnts)
{
   try {
      ResultSet rs = using_database.testQuery("SELECT * FROM " + tbl + " LIMIT 1");
      rs.close();
      using_database.deleteAll(tbl);
      return;
    }
   catch (SQLException e) { }

   using_database.createTable(tbl,cnts);
}




void createView(String tbl,String q)
{
   try {
      ResultSet rs = using_database.testQuery("SELECT * FROM " + tbl + " LIMIT 1");
      rs.close();
      using_database.dropView(tbl);
    }
   catch (SQLException e) { }

   using_database.createView(tbl,q);
}




}	// end of class DycompMain



/* end of DycompMain.java */

