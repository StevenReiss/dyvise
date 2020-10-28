/********************************************************************************/
/*										*/
/*		DyviewModel.java						*/
/*										*/
/*	DYname VIEW model of visualization information				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyview/DyviewModel.java,v 1.5 2013/09/04 18:36:36 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyviewModel.java,v $
 * Revision 1.5  2013/09/04 18:36:36  spr
 * Minor bug fixes.
 *
 * Revision 1.4  2011-04-01 23:09:22  spr
 * Code clean up.
 *
 * Revision 1.3  2010-03-30 16:23:25  spr
 * Upgrades to the visualizer to handle new programs and provide better views.
 *
 * Revision 1.2  2009-10-07 01:00:21  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.1  2009-09-19 00:14:23  spr
 * Viewer with dialog based front end.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dyview;


import edu.brown.cs.dyvise.dygraph.DygraphControl;
import edu.brown.cs.dyvise.dygraph.DygraphView;
import edu.brown.cs.dyvise.dymon.DymonRemote;
import edu.brown.cs.dyvise.dystore.DystoreControl;
import edu.brown.cs.dyvise.dyvise.*;

import edu.brown.cs.ivy.mint.*;
import edu.brown.cs.ivy.project.IvyProject;
import edu.brown.cs.ivy.project.IvyProjectManager;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.io.*;
import java.sql.ResultSet;
import java.util.*;



public class DyviewModel implements DyviewConstants, DyviseConstants, MintConstants
{




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private IvyProjectManager project_manager;
private IvyProject	user_project;
private String		start_class;
private DyviseDatabase	using_database;
private List<ModelListener> model_listeners;
private DyviewVisual	current_visual;
private boolean 	visual_ready;
private DymonRemote	dymon_remote;
private Element 	tuple_model;
private Element 	graph_model;
private Element 	patch_model;
private Element 	event_model;
private DystoreControl	tuple_store;
private DyviewTupleBuilder tuple_builder;
private DygraphControl	graph_control;
private Map<DygraphView,Boolean> graph_enabled;
private Element 	graph_params;
private MintControl	mint_control;

private Set<DyviewVisual> all_visuals;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DyviewModel()
{
   project_manager = IvyProjectManager.getManager();
   user_project = null;
   start_class = null;
   new HashMap<String,Collection<String>>();
   using_database = new DyviseDatabase();
   model_listeners = new ArrayList<ModelListener>();
   dymon_remote = null;
   tuple_model = null;
   graph_model = null;
   patch_model = null;
   event_model = null;
   tuple_store = null;
   graph_control = null;
   graph_params = null;
   graph_enabled = new HashMap<DygraphView,Boolean>();

   current_visual = null;
   visual_ready = false;
   all_visuals = new TreeSet<DyviewVisual>();
   all_visuals.add(new DyviewVisualThreadState(this));

   // do this early to ensure right type of synchronization
   mint_control = DymonRemote.getMintControl(MintSyncMode.SINGLE);
}




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

void setProjectDirectory(String pd)
{
   IvyProjectManager npm = IvyProjectManager.getManager(pd);

   if (npm == project_manager) return;

   project_manager = npm;

   if (user_project != null) {
      IvyProject ip = project_manager.findProject(user_project.getName());
      setProject(ip);
    }
}



void setProject(String pnm) throws DyviseException
{
   IvyProject ip = project_manager.findProject(pnm);
   if (ip == null) throw new DyviseException("Project " + pnm + " not found");

   setProject(ip);
}



void setProject(IvyProject ip)
{
   if (user_project == ip) return;
   user_project = ip;

   setupDatabase();

   for (ModelListener ml : model_listeners) ml.projectChanged();
}


void setStartClass(String cls)
{
   start_class = cls;

   for (ModelListener ml : model_listeners) ml.startClassChanged();
}



void addModelListener(ModelListener ml)
{
   model_listeners.add(ml);
}



void setVisual(DyviewVisual dv)
{
   current_visual = dv;

   for (ModelListener ml : model_listeners) ml.visualChanged();

   if (dv == null) setVisualReady(false);
   else setVisualReady(dv.isReady());
}


DyviewVisual setVisual(String nm)
{
   DyviewVisual newdv = null;

   for (DyviewVisual dv : all_visuals) {
      if (dv.getIdName().equalsIgnoreCase(nm) || dv.toString().equalsIgnoreCase(nm)) {
	 newdv = dv;
	 break;
       }
    }

   setVisual(newdv);

   return newdv;
}



void setVisualReady(boolean fg)
{
   if (fg == visual_ready) return;

   visual_ready = fg;

   for (ModelListener ml : model_listeners) ml.visualReadyChanged();
}



void setPatchModel(Element mdl)
{
   System.err.println("DYVIEW: SET PATCH " + IvyXml.convertXmlToString(mdl));

   tuple_model = null;
   graph_model = null;
   event_model = null;
   patch_model = null;

   if (mdl != null) {
      tuple_model = IvyXml.getChild(mdl,"TUPLEMODEL");
      graph_model = IvyXml.getChild(mdl,"GRAPHMODEL");
      event_model = IvyXml.getChild(mdl,"EVENTMODEL");
      patch_model = IvyXml.getChild(mdl,"PATCHMODEL");
      tuple_store = new DystoreControl(tuple_model);
      tuple_builder = new DyviewTupleBuilder(event_model,tuple_store);
      graph_control = new DygraphControl(graph_model,tuple_store);
      graph_enabled.clear();
      for (DygraphView dv : graph_control.getViews()) {
	 graph_enabled.put(dv,true);
       }

      if (graph_params != null) {
	 for (Element ve : IvyXml.children(graph_params,"VIEW")) {
	    for (DygraphView dv : graph_control.getViews()) {
	       if (dv.getName().equals(IvyXml.getAttrString(ve,"NAME"))) {
		  graph_enabled.put(dv,IvyXml.getAttrBool(ve,"ENABLED",true));
		  dv.loadValues(ve);
		  break;
		}
	     }
	  }
       }
    }
}


void enableGraphView(DygraphView dv,boolean fg)
{
   graph_enabled.put(dv,fg);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public File getProjectDirectory()
{
   return project_manager.getProjectDirectory();
}


public IvyProject getProject()			{ return user_project; }


public IvyProjectManager getProjectManager()	{ return project_manager; }


Collection<DyviewVisual> getAllVisuals()
{
   return new ArrayList<DyviewVisual>(all_visuals);
}



public DyviewVisual getVisual() 		{ return current_visual; }

public String getStartClass()			{ return start_class; }

public boolean isVisualReady()			{ return visual_ready; }

public DygraphControl getGraphics()		{ return graph_control; }

public boolean isGraphViewEnabled(DygraphView dv)
{
   if (graph_enabled.containsKey(dv)) return graph_enabled.get(dv);

   return false;
}


public Element getPatchModel()			{ return patch_model; }

public DystoreControl getStore()		{ return tuple_store; }
public DyviewTupleBuilder getBuilder()		{ return tuple_builder; }



/********************************************************************************/
/*										*/
/*	Database methods							*/
/*										*/
/********************************************************************************/

public ResultSet queryDatabase(String q)
{
   if (user_project == null) return null;

   return using_database.query(q);
}



public boolean executeSql(String q)
{
   if (user_project == null) return false;

   return using_database.trySql(q);
}



public Date getUpdateTime(String what,String id)
{
   if (user_project == null) return null;

   return using_database.getTime(what,id);
}



public void updateTime(String what,String id)
{
   using_database.updateTime(what,id);
}



private void setupDatabase()
{
   using_database.close();

   if (user_project == null) return;

   String dbnm = DyviseDatabase.getDatabaseName(user_project.getName());
   if (!using_database.testConnect(dbnm)) {
      createDatabase(dbnm);
    }
}



private void createDatabase(String nm)
{
   System.err.println("DYVIEW: Creating database " + nm);

   using_database.connectDefault();
   using_database.trySql("DROP DATABASE " + nm);
   using_database.runSql("CREATE DATABASE " + nm);
   using_database.close();

   using_database.connect(nm);

   try (BufferedReader br = new BufferedReader(new FileReader(DYVIEW_DATABASE_SETUP))) {
      StringBuffer buf = new StringBuffer();
      for ( ; ; ) {
	 String ln = br.readLine();
	 if (ln == null) break;
	 buf.append(ln);
	 buf.append(" ");
       }

      StringTokenizer tok = new StringTokenizer(buf.toString(),";");
      while (tok.hasMoreTokens()) {
	 String cmd = tok.nextToken().trim();
	 if (cmd.length() == 0) continue;
	 using_database.addSql(cmd);
       }
      using_database.runSql();
    }
   catch (IOException e) {
      System.err.println("DYVIEW: Problem creating database: " + e);
      System.exit(4);
    }
}




/********************************************************************************/
/*										*/
/*	File methods								*/
/*										*/
/********************************************************************************/

void saveTo(File file) throws IOException
{
   IvyXmlWriter xw = new IvyXmlWriter(file);

   xw.begin("DYVIEW");

   if (project_manager != null) xw.textElement("PROJECT_DIR",project_manager.getProjectDirectory());
   if (user_project != null) xw.textElement("PROJECT_NAME",user_project.getName());
   if (start_class != null) xw.textElement("START_CLASS",start_class);
   if (current_visual != null) {
      xw.begin("VISUAL");
      xw.field("NAME",current_visual.getIdName());
      current_visual.outputXml(xw);
      xw.end("VISUAL");
    }
   if (tuple_model != null) xw.writeXml(tuple_model);
   if (graph_model != null) xw.writeXml(graph_model);
   if (patch_model != null) xw.writeXml(patch_model);
   if (event_model != null) xw.writeXml(event_model);

   if (graph_control != null) {
      xw.begin("GRAPHICS");
      for (DygraphView dv : graph_control.getViews()) {
	 xw.begin("VIEW");
	 xw.field("NAME",dv.getName());
	 xw.field("ENABLED",graph_enabled.get(dv));
	 dv.outputXml(xw);
	 xw.end("VIEW");
       }
      xw.end("GRAPHICS");
    }

   xw.end("DYVIEW");

   xw.close();
}



void loadFrom(File file) throws DyviseException
{
   Element xml = IvyXml.loadXmlFromFile(file);
   if (xml == null) throw new DyviseException("Invalid dyview file " + file);

   String pdir = IvyXml.getTextElement(xml,"PROJECT_DIR");
   project_manager = IvyProjectManager.getManager(pdir);
   String pnm = IvyXml.getTextElement(xml,"PROJECT_NAME");
   setProject(pnm);

   start_class = IvyXml.getTextElement(xml,"START_CLASS");

   Element vis = IvyXml.getChild(xml,"VISUAL");
   if (vis != null) {
      setVisual(IvyXml.getAttrString(vis,"NAME"));
      if (current_visual != null) current_visual.loadXml(vis);
    }

   graph_params = IvyXml.getChild(xml,"GRAPHICS");

   setPatchModel(xml);
}





/********************************************************************************/
/*										*/
/*	Data processing methods 						*/
/*										*/
/********************************************************************************/

void processEvents(Element e)
{
   tuple_builder.processEvents(e);
   graph_control.dataUpdated();

   for (ModelListener ml : model_listeners) ml.dataUpdated();
}




/********************************************************************************/
/*										*/
/*	Dymon methods								*/
/*										*/
/********************************************************************************/

synchronized DymonRemote getDymonHandle()
{
   if (dymon_remote == null) {
      dymon_remote = new DymonRemote();
    }

   return dymon_remote;
}


DymonRemote.ProcessManager getProcessManager()
{
   return getDymonHandle().getProcessManager();
}


MintControl getMintControl()
{
   return mint_control;
}



}	// end of class DyviewModel




/* end of DyviewModel.java */
