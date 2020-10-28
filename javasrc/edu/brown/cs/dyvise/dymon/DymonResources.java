/********************************************************************************/
/*										*/
/*		DymonResources.java						*/
/*										*/
/*	DYPER monitor interface and agents constant resource store		*/
/*										*/
/********************************************************************************/
/*	Copyright 2005 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2005, Brown University, Providence, RI.				 *
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymon/DymonResources.java,v 1.6 2012-10-05 00:52:56 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymonResources.java,v $
 * Revision 1.6  2012-10-05 00:52:56  spr
 * Code clean up.
 *
 * Revision 1.5  2009-06-04 18:53:51  spr
 * Set up for binary distribution.
 *
 * Revision 1.4  2009-04-20 23:23:30  spr
 * Updates to make things work on the mac.  Fix bug in dymti.
 *
 * Revision 1.3  2009-03-20 02:06:51  spr
 * Update dymon: add time-based information, updated memory, etc.
 *
 * Revision 1.2  2008-11-24 23:59:42  spr
 * Update remote error handling.  Use address rather than name for hosts.
 *
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymon;


import edu.brown.cs.dyvise.dyper.DyperConstants;
import edu.brown.cs.ivy.exec.IvyExecQuery;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;



class DymonResources implements DyperConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Set<String> file_set;
private long last_check;
private Map<String,Map<String,String>> resource_map;
private Map<String,String> resource_default;
private Map<String,Map<String,String>> class_map;
private Map<String,String> class_default;
private Map<String,Namer> namer_map;
private Collection<String> host_names;
private Namer default_namer;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DymonResources()
{
   file_set = new HashSet<String>();
   last_check = System.currentTimeMillis();

   resource_map = new HashMap<String,Map<String,String>>();
   resource_default = new HashMap<String,String>();
   class_map = new HashMap<String,Map<String,String>>();
   class_default = new HashMap<String,String>();
   host_names = new HashSet<String>();
   namer_map = new HashMap<String,Namer>();

   host_names.add(IvyExecQuery.getHostName());

   default_namer = new Namer();

   resource_default.put("CHECKTIME","100");
   resource_default.put("DISABLETIME","10000");
   resource_default.put("REPORTTIME","10000");
   resource_default.put("MONITOR","TRUE");
   resource_default.put("SHOWSTACK","FALSE");
   resource_default.put("AGENTS","*");
   resource_default.put("CONTENTIONCHECK","FALSE");
   resource_default.put("TIMINGCHECK","FALSE");
}




/********************************************************************************/
/*										*/
/*	Setting methods 							*/
/*										*/
/********************************************************************************/

void setResource(String name,String value)
{
   if (value == null) resource_default.remove(name);
   else resource_default.put(name,value);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getSettings(String cls)
{
   Map<String,String> r = new HashMap<String,String>(resource_default);
   Map<String,String> p = resource_map.get(cls);
   if (p != null) r.putAll(p);

   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("SETTINGS");
   for (Map.Entry<String,String> ent : r.entrySet()) {
      String v = ent.getValue();
      xw.begin("VAR");
      xw.field("NAME",ent.getKey());
      if (v.length() < 64) xw.field("VALUE",ent.getValue());
      else xw.text(v);
      xw.end("VAR");
    }
   xw.end("SETTINGS");

   String rslt = xw.toString();
   xw.close();
   return rslt;
}


String getResource(String cls,String name)
{
   Map<String,String> p = resource_map.get(cls);
   if (p != null && p.containsKey(name)) return p.get(name);
   return resource_default.get(name);
}


String getClasses(String cls)
{
   Map<String,String> r = new HashMap<String,String>(class_default);
   Map<String,String> p = class_map.get(cls);
   if (p != null) r.putAll(p);

   if (r.isEmpty()) return null;

   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("CLASSES");
   for (Map.Entry<String,String> ent : r.entrySet()) {
      String k = ent.getKey();
      String v = ent.getValue();
      if (k.endsWith(".")) {
	 xw.begin("PACKAGE");
	 xw.field("NAME",k);
	 xw.field("TYPE",v);
	 xw.end("PACKAGE");
       }
    }
   for (Map.Entry<String,String> ent : r.entrySet()) {
      String k = ent.getKey();
      String v = ent.getValue();
      if (!k.endsWith(".")) {
	 xw.begin("CLASS");
	 xw.field("NAME",k);
	 xw.field("TYPE",v);
	 xw.end("CLASS");
       }
    }
   xw.end("CLASSES");

   String rslt = xw.toString();
   xw.close();
   return rslt;
}



String getName(String startclass,String startjar,List<String> args)
{
   Namer nm = namer_map.get(startclass);

   if (nm == null) nm = default_namer;

   return nm.getName(startclass,startjar,args);
}



/********************************************************************************/
/*										*/
/*	Host methdos								*/
/*										*/
/********************************************************************************/

Iterable<String> getRemoteHosts()
{
   synchronized (host_names) {
      return new ArrayList<String>(host_names);
    }
}



void addHost(String host)
{
   synchronized (host_names) {
      if (host != null) host_names.add(host);
    }
}


void removeHost(String host)
{
   synchronized (host_names) {
      if (host != null) host_names.remove(host);
    }
}




/********************************************************************************/
/*										*/
/*	Loading methods 							*/
/*										*/
/********************************************************************************/

void checkFiles()
{
   long now = System.currentTimeMillis();

   for (String s : file_set) {
      File f = new File(s);
      if (f.lastModified() > last_check) {
	 System.err.println("DYMON: Reload resource file " + f);
	 loadFile(s);
       }
    }

   last_check = now;
}



boolean loadFile(String file)
{
   file_set.add(file);

   File f = new File(file);
   if (!f.exists()) return false;

   Element xml = IvyXml.loadXmlFromFile(f);
   if (xml == null) return false;

   for (Element e : IvyXml.elementsByTag(xml,"HOSTS")) {
      for (Element he : IvyXml.elementsByTag(e,"HOST")) {
	 String h = IvyXml.getText(he);
	 if (h != null) host_names.add(h);
       }
    }

   boolean setcp = false;

   for (Element e : IvyXml.elementsByTag(xml,"PROCESS")) {
      String c = IvyXml.getAttrString(e,"CLASS");
      Map<String,String> m = resource_default;
      Map<String,String> cm = class_default;
      if (c != null) {
	 m = resource_map.get(c);
	 if (m == null) {
	    m = new HashMap<String,String>();
	    resource_map.put(c,m);
	  }
	 cm = class_map.get(c);
	 if (cm == null) {
	    cm = new HashMap<String,String>();
	    class_map.put(c,cm);
	  }
	 if (IvyXml.getAttrBool(e,"IGNORE")) m.put("IGNORE","TRUE");
       }
      for (Element s : IvyXml.elementsByTag(e,"SET")) {
	 String k = IvyXml.getAttrString(s,"NAME");
	 String v = IvyXml.getAttrString(s,"VALUE");
	 if (k != null && v != null) m.put(k,v);
       }
      for (Element s : IvyXml.elementsByTag(e,"CLASS")) {
	 String k = IvyXml.getAttrString(s,"NAME");
	 String v = IvyXml.getAttrString(s,"VALUE");
	 if (v == null) v = IvyXml.getAttrString(s,"TYPE");
	 if (k != null && v != null) cm.put(k,v);
       }
      for (Element s : IvyXml.elementsByTag(e,"PACKAGE")) {
	 String k = IvyXml.getAttrString(s,"NAME");
	 String v = IvyXml.getAttrString(s,"VALUE");
	 if (v == null) v = IvyXml.getAttrString(s,"TYPE");
	 if (k != null && !k.endsWith(".")) k += ".";
	 if (k != null && v != null) cm.put(k,v);
       }
      for (Element s : IvyXml.elementsByTag(e,"CLASSPATH")) {
	 String v = IvyXml.getText(s);
	 File fv = new File(v);
	 if (IvyXml.getAttrBool(s,"PLUGIN")) {
	    v = getEclipsePlugins(fv);
	  }
	 else {
	    if (!fv.exists()) {
	       System.err.println("DYMON: Classpath element " + v + " doesn't exist");
	     }
	  }
	 String ocp = (setcp ? m.get("CLASSPATH") : null);
	 if (ocp != null) v = ocp + File.pathSeparator + v;
	 m.put("CLASSPATH",v);
	 setcp = true;
       }
      Element ne = IvyXml.getElementByTag(e,"NAME");
      if (c != null && ne != null) {
	 namer_map.put(c,new Namer(ne));
       }
    }

   return true;
}




/********************************************************************************/
/*										*/
/*	Method to get eclipse plugins from a directory				*/
/*										*/
/********************************************************************************/

private String getEclipsePlugins(File dir)
{
   String [] files = dir.list(new JarFilter());

   if (files == null || files.length == 0) return null;

   Arrays.sort(files);

   String cp = null;
   String last = null;
   for (int i = files.length-1; i >= 0; --i) {
      String cur = files[i];
      boolean use = true;
      if (last != null) {
	 for (int j = 0; j < last.length(); ++j) {
	    if (cur.charAt(j) != last.charAt(j)) {
	       if (j > 0 && Character.isDigit(cur.charAt(j)) &&
		      cur.charAt(j-1) == '_') use = false;
	       break;
	     }
	  }
       }
      if (use) {
	 String jfil = dir + File.separator + cur;
	 if (cp == null) cp = jfil;
	 else cp += File.pathSeparator + jfil;
	 last = cur;
       }
    }

   return cp;
}


private static class JarFilter implements FilenameFilter {

   public boolean accept(File dir,String name) {
      return name.endsWith(".jar");
    }

}	// end of subclass JarFilter




/********************************************************************************/
/*										*/
/*	Naming methods								*/
/*										*/
/********************************************************************************/

private class Namer {

   private String show_name;
   private String arg_match;

   Namer() {
      show_name = null;
      arg_match = null;
    }

   Namer(Element xml) {
      show_name = IvyXml.getAttrString(xml,"SHOW");
      arg_match = IvyXml.getAttrString(xml,"ARG");
    }

   String getName(String startclass,String startjar,List<String> args) {
      if (arg_match != null) {
	 boolean usenxt = false;
	 for (String s : args) {
	    if (usenxt) return s;
	    if (s.startsWith(arg_match)) {
	       usenxt = true;
	     }
	  }
       }
      if (show_name != null) return show_name;
      if (startclass == null) return "*UNKNOWN*";

      int idx = startclass.lastIndexOf(".");
      if (idx > 0) return startclass.substring(idx+1);
      return startclass;
    }

}	// end of subclass Namer




}	// end of class DymonResources




/* end of DymonResources.java */
