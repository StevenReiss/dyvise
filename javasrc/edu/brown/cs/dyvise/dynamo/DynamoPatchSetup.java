/********************************************************************************/
/*										*/
/*		DynamoPatchSetup.java						*/
/*										*/
/*	DYVISE dyanmic analysis patch setup from database information		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dynamo/DynamoPatchSetup.java,v 1.2 2009-10-07 01:00:16 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DynamoPatchSetup.java,v $
 * Revision 1.2  2009-10-07 01:00:16  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.1  2009-09-19 00:10:00  spr
 * Module to generate model for visualization.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dynamo;


import edu.brown.cs.dyvise.dyvise.DyviseConstants;
import edu.brown.cs.dyvise.dyvise.DyviseDatabase;
import edu.brown.cs.ivy.project.IvyProject;
import edu.brown.cs.ivy.project.IvyProjectManager;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.io.*;
import java.util.HashMap;
import java.util.Map;



public class DynamoPatchSetup implements DynamoConstants, DyviseConstants
{


/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   DynamoPatchSetup dm = new DynamoPatchSetup(args);

   dm.setupPatchModel();
}



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IvyProject	user_project;

private DynamoPatchDescriptor patch_data;
private String		output_file;

private DyviseDatabase	sql_database;

private static Map<String,DynamoPatchCode> patch_table;


static {
   patch_table = new HashMap<String,DynamoPatchCode>();
   patch_table.put("EVENTSTATE",new DynamoPatchCodeEventState());
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public DynamoPatchSetup(IvyProject p,Element xml)
{
   output_file = null;
   user_project = p;
   patch_data = new DynamoPatchDescriptor(p,xml);
}



public DynamoPatchSetup(IvyProject p,String xml)
{
   output_file = null;
   user_project = p;
   patch_data = new DynamoPatchDescriptor(p,IvyXml.convertStringToXml(xml));
}



private DynamoPatchSetup(String [] args)
{
   user_project = null;
   patch_data = null;
   output_file = null;

   scanArgs(args);
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
   String pdata = null;

   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
	 if (args[i].startsWith("-p") && i+1 < args.length) {           // -p <project>
	    projnm = args[++i];
	  }
	 else if (args[i].startsWith("-d") && i+1 < args.length) {      // -d <project dir>
	    projdir = args[++i];
	  }
	 else if (args[i].startsWith("-o") && i+1 < args.length) {      // -o output
	    output_file = args[++i];
	  }
	 else badArgs();
       }
      else {
	 if (pdata != null) badArgs();
	 pdata = args[i];
       }
    }

   if (pdata == null) badArgs();

   if (projnm != null) {
      IvyProjectManager pm = IvyProjectManager.getManager(projdir);
      user_project = pm.findProject(projnm);
    }

   patch_data = new DynamoPatchDescriptor(user_project,pdata);
}



private void badArgs()
{
   System.err.println("DYNAMOPATCH: dynamopatch [-p <project>] -i <databaseid> [-d <projectdir>] method");
   System.exit(1);
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

public String setupPatchModel()
{
   DynamoPatchCode pc = patch_table.get(patch_data.getMethod());
   if (pc == null) {
      System.err.println("DYNAMOPATCH: method " + patch_data.getMethod() + " not found");
      System.exit(1);
      return null;
    }

   String dbnm = DyviseDatabase.getDatabaseName(user_project);
   sql_database = new DyviseDatabase();
   sql_database.connect(dbnm);

   IvyXmlWriter xw = new IvyXmlWriter();		// patch information
   xw.begin("DYNAMO");
   if (user_project != null) {
      xw.field("PROJECT",user_project.getName());
      xw.field("PROJECTDIR",user_project.getDirectory());
    }

   pc.buildPatch(xw,patch_data,sql_database);

   xw.begin("EVENTMODEL");
   pc.outputEvents(xw);
   xw.end("EVENTMODEL");

   xw.end("DYNAMO");

   String rslt = null;

   if (output_file != null) {
      try {
	 PrintWriter pw = new PrintWriter(new FileWriter(output_file));
	 pw.println(xw.toString());
	 pw.close();
       }
      catch (IOException e) {
	 System.err.println("DYNAMOPATCH: Problem writing output: " + e);
	 System.exit(1);
       }
    }
   else {
      rslt = xw.toString();
    }

   return rslt;
}





}	// end of class DynamoPatchSetup



/* end of DynamoPatchSetup.java */
