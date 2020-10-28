/********************************************************************************/
/*										*/
/*		DystaticMain.java						*/
/*										*/
/*	DYVISE static analysis controller					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dystatic/DystaticMain.java,v 1.4 2011-04-01 23:09:20 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DystaticMain.java,v $
 * Revision 1.4  2011-04-01 23:09:20  spr
 * Code clean up.
 *
 * Revision 1.3  2011-03-10 02:33:17  spr
 * Code cleanup.
 *
 * Revision 1.2  2009-10-07 01:00:19  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.1  2009-09-19 00:13:48  spr
 * Static analyzer storing info in the database.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dystatic;


import edu.brown.cs.dyvise.dyvise.DyviseConstants;
import edu.brown.cs.dyvise.dyvise.DyviseDatabase;
import edu.brown.cs.ivy.jflow.*;
import edu.brown.cs.ivy.project.*;
import edu.brown.cs.ivy.xml.*;

import com.ibm.jikesbt.*;

import org.w3c.dom.Element;

import java.util.*;


public class DystaticMain implements DystaticConstants, DyviseConstants, JflowControl
{


/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   DystaticMain dm = new DystaticMain(args);

   dm.process();
}



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IvyProject	user_project;
private List<DystaticStatlet> active_statlets;
private JflowMaster	flow_master;
private List<String>	jflow_files;
private boolean 	run_jflow;
private Map<String,String> name_map;
private String		map_file;
private Map<String,String> class_map;

private boolean 	do_debug;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private DystaticMain(String [] args)
{
   user_project = null;
   active_statlets = new ArrayList<DystaticStatlet>();
   jflow_files = new ArrayList<String>();
   do_debug = false;
   run_jflow = false;
   name_map = new HashMap<String,String>();
   map_file = DYSTATIC_MAP_FILE;
   class_map = new HashMap<String,String>();

   scanArgs(args);
}



public DystaticMain(IvyProject proj)
{
   user_project = proj;

   active_statlets = new ArrayList<DystaticStatlet>();
   jflow_files = new ArrayList<String>();
   do_debug = false;
   run_jflow = false;
   name_map = new HashMap<String,String>();
   map_file = DYSTATIC_MAP_FILE;
   class_map = new HashMap<String,String>();

   name_map.put("PROJECT",proj.getName());
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
   List<String> statlets = new ArrayList<String>();

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
	 else if (args[i].startsWith("-D")) {                           // -DEBUG
	    do_debug = true;
	  }
	 else if (args[i].startsWith("-j") && i+1 < args.length) {      // -j <jflow file>
	    jflow_files.add(args[++i]);
	  }
	 else if (args[i].startsWith("-J")) {                           // -JFLOW
	    run_jflow = true;
	  }
	 else if (args[i].startsWith("-i") && i+1 < args.length) {      // -id <db id>
	    name_map.put("DBID",args[++i]);
	  }
	 else if (args[i].startsWith("-m") && i+1 < args.length) {      // -map <mapfile>
	    map_file = args[++i];
	  }
	 else badArgs();
       }
      else {
	 run_jflow = true;
	 statlets.add(args[i]);
       }
    }

   if (projnm == null) badArgs();

   IvyProjectManager pm = IvyProjectManager.getManager(projdir);
   user_project = pm.findProject(projnm);

   loadMapFile();

   // TODO: load statlets
}



private void badArgs()
{
   System.err.println("DYSTATIC: dystatic -p <project> -i <databaseid> [-d <projectdir>] statlet ...");
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
   return DyviseDatabase.getDatabaseName(name_map);
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

public void defaultAnalysis()
{
   DystaticLoader dl = new DystaticBtLoader(this);
   dl.processLoad();
}




private void process()
{
   if (run_jflow) runJflow();

   // DystaticLoader dl = new DystaticAsmLoader(this);
   DystaticLoader dl = new DystaticBtLoader(this);
   dl.processLoad();
}




private void runJflow()
{
   flow_master = JflowFactory.createFlowMaster(this);
   if (do_debug) flow_master.setOption(FlowOption.DO_DEBUG,true);
   flow_master.addDefaultDescriptionFile();
   for (String f : jflow_files) {
      flow_master.addDescriptionFile(f);
    }

   flow_master.setClassPath(null);
   for (String cp : user_project.getClassPath()) {
      flow_master.addToClassPath(cp);
    }
   for (String sc : user_project.getStartClasses()) {
      flow_master.addClass(sc);
    }

   System.err.println("DYSTATIC: Starting flow analysis for " + user_project.getName());

   try {
      flow_master.analyze();
    }
   catch (JflowException e) {
      System.err.println("DYSTATIC: Problem doing analysis: " + e);
      e.printStackTrace();
    }

   System.err.println("DYSTATIC: Finished flow analysis for " + user_project.getName());

   // TODO: save results
}




/********************************************************************************/
/*										*/
/*	JflowControl methods							*/
/*										*/
/********************************************************************************/

public JflowModelSource createModelSource(JflowMethod jm,int ino,BT_Ins ins,JflowValue base)
{
   for (DystaticStatlet ds : active_statlets) {
      JflowModelSource ms = ds.createModelSource(jm,ino,ins,base);
      if (ms != null) return ms;
    }

   return null;
}



public JflowMethodData createMethodData(JflowMethod jm)
{
   return new DystaticMethodData(this,jm);
}



/********************************************************************************/
/*										*/
/*	Model building methods (not used)					*/
/*										*/
/********************************************************************************/

public boolean checkUseMethod(JflowMethod m)			{ return false; }

public JflowEvent findEvent(JflowModel jm,JflowMethod m,BT_Ins ins,boolean start,List<Object> vals)
{
   return null;
}

public Collection<JflowEvent> getRequiredEvents()		{ return null; }

public boolean isFieldTracked(BT_Field fld)			{ return false; }

public boolean checkUseCall(JflowMethod from,BT_Method to)	{ return true; }




/********************************************************************************/
/*										*/
/*	Map file methods							*/
/*										*/
/********************************************************************************/

private void loadMapFile()
{
   Element e = IvyXml.loadXmlFromFile(map_file);
   if (e != null) {
      for (Element m : IvyXml.children(e,"MAP")) {
	 String f = IvyXml.getAttrString(m,"FROM");
	 String t = IvyXml.getAttrString(m,"TO");
	 class_map.put(f,t);
       }
    }
}


String getMappedClass(String cls)
{
   if (cls == null) return null;

   return class_map.get(cls);
}



}	// end of class DystaticMain



/* end of DystaticMain.java */
