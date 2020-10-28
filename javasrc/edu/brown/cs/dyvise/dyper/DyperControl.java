/********************************************************************************/
/*										*/
/*		DyperControl.java						*/
/*										*/
/*	Controller for DYnamic Performance Evaluation Runner			*/
/*										*/
/********************************************************************************/
/*	Copyright 2007 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2007, Brown University, Providence, RI.				 *
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyper/DyperControl.java,v 1.14 2016/11/02 18:59:20 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyperControl.java,v $
 * Revision 1.14  2016/11/02 18:59:20  spr
 * Move to asm5
 *
 * Revision 1.13  2012-10-05 00:53:01  spr
 * Code clean up.
 *
 * Revision 1.12  2011-04-01 23:09:11  spr
 * Code clean up.
 *
 * Revision 1.11  2011-03-19 20:34:28  spr
 * Code cleanup
 *
 * Revision 1.10  2011-03-10 02:27:40  spr
 * Code fixups.
 *
 * Revision 1.9  2010-06-01 19:26:24  spr
 * Upgrades to make dyview work on the mac
 *
 * Revision 1.8  2010-03-30 16:19:22  spr
 * Remove debugging. Add xjar to Make.data.
 *
 * Revision 1.7  2009-10-07 01:00:17  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.6  2009-09-19 00:13:28  spr
 * Update agents for dynamic insertion and removal.
 *
 * Revision 1.5  2009-04-11 23:47:08  spr
 * Check to avoid multiple adds to boot path.
 *
 * Revision 1.4  2009-03-20 02:08:21  spr
 * Code cleanup; output information for incremental time-based display.
 *
 * Revision 1.3  2008-12-04 01:11:12  spr
 * Clean up memory agent.
 *
 * Revision 1.2  2008-11-12 14:11:10  spr
 * Handle continuous memory tracing.  Other minor cleanups.
 *
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dyper;




import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;

import org.xml.sax.InputSource;

import javax.xml.parsers.*;
import java.io.*;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.util.*;

import java.net.InetAddress;
import java.net.Socket;

public class DyperControl implements DyperConstants {




/********************************************************************************/
/*										*/
/*	Agent entry points							*/
/*										*/
/********************************************************************************/

public static void premain(String args,Instrumentation inst)
{
   the_control = new DyperControl(args,inst);
}



public static void agentmain(String args,Instrumentation inst)
{
   if (the_control == null) the_control = new DyperControl(args,inst);
   else the_control.restart();
}


public static void dymtimain()
{
   if (the_control != null) the_control.attach();
}



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private Instrumentation 	class_inst;
private DyperMonitor		stack_monitor;
private String			process_id;
private Map<String,ClassType>	class_map;
private Map<String,ClassType>	package_map;
private DyperInstrumenter	dyper_inst;
private Set<String>		installed_agents;
private Map<String,Class<?>>	known_classes;
private long			start_time;
private Thread			monitor_thread;
private String			classpath_add;
private boolean 		no_attach;
private BitSet			ignore_thread;
private BitSet			use_thread;
private boolean 		thread_default;
private int			num_special;
private String			start_class;

private String			server_host;
private int			server_port;
private SocketClient		socket_client;
private CmdHandler		cmd_handler;

private long			init_overhead;

private boolean 		is_waiting;

private static boolean		is_attached = false;

private static DyperControl	the_control = null;

private static boolean		use_transform = true;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private DyperControl(String args,Instrumentation inst)
{
   start_time = System.currentTimeMillis();

   System.err.println("DYPER: Dyper started");

   class_inst = inst;

   String host = getHostName();
   process_id = getProcessId();
   server_host = null;
   server_port = 0;

   prescanArgs(args);

   if (process_id == null) {
      Random r = new Random();
      process_id = "D_" + r.nextInt(1024*1024);
    }
   if (host != null) process_id += "@" + host;

   installed_agents = new HashSet<String>();

   package_map = new HashMap<String,ClassType>();
   class_map = new HashMap<String,ClassType>();
   package_map.put("java.",ClassType.SYSTEM);
   package_map.put("javax.",ClassType.SYSTEM);
   package_map.put("sun.",ClassType.SYSTEM);
   package_map.put("org.w3c.",ClassType.SYSTEM);
   package_map.put("org.omg.",ClassType.SYSTEM);
   package_map.put("org.xml.",ClassType.SYSTEM);
   package_map.put("edu.brown.cs.dyvise.dyper.",ClassType.SYSTEM);
   package_map.put("com.ibm.",ClassType.SYSTEM);
   package_map.put("com.sun.",ClassType.SYSTEM);
   package_map.put("org.postgresql.",ClassType.SYSTEM);
   package_map.put("jrockit.",ClassType.SYSTEM);

   package_map.put("java.io.",ClassType.SYSTEM_IO);
   package_map.put("java.net.",ClassType.SYSTEM_IO);
   package_map.put("javax.net.",ClassType.SYSTEM_IO);
   package_map.put("java.nio.",ClassType.SYSTEM_IO);
   package_map.put("sun.nio.",ClassType.SYSTEM_IO);
   package_map.put("sun.net.",ClassType.SYSTEM_IO);
   package_map.put("jrockit.net.",ClassType.SYSTEM_IO);
   package_map.put("jrockit.io.",ClassType.SYSTEM_IO);

   class_map.put("java.util.AbstractCollection",ClassType.SYSTEM_COLLECTION);
   class_map.put("java.util.AbstractList",ClassType.SYSTEM_COLLECTION);
   class_map.put("java.util.AbstractQueue",ClassType.SYSTEM_COLLECTION);
   class_map.put("java.util.AbstractSequentialList",ClassType.SYSTEM_COLLECTION);
   class_map.put("java.util.AbstractSet",ClassType.SYSTEM_COLLECTION);
   class_map.put("java.util.ArrayList",ClassType.SYSTEM_COLLECTION);
   class_map.put("java.util.EnumSet",ClassType.SYSTEM_COLLECTION);
   class_map.put("java.util.HashSet",ClassType.SYSTEM_COLLECTION);
   class_map.put("java.util.LinkedHashSet",ClassType.SYSTEM_COLLECTION);
   class_map.put("java.util.LinkedList",ClassType.SYSTEM_COLLECTION);
   class_map.put("java.util.Stack",ClassType.SYSTEM_COLLECTION);
   class_map.put("java.util.TreeSet",ClassType.SYSTEM_COLLECTION);
   class_map.put("java.util.Vector",ClassType.SYSTEM_COLLECTION);
   class_map.put("java.util.AbstractMap",ClassType.SYSTEM_COLLECTION);
   class_map.put("java.util.EnumMap",ClassType.SYSTEM_COLLECTION);
   class_map.put("java.util.HashMap",ClassType.SYSTEM_COLLECTION);
   class_map.put("java.util.Hashtable",ClassType.SYSTEM_COLLECTION);
   class_map.put("java.util.IdentityHashMap",ClassType.SYSTEM_COLLECTION);
   class_map.put("java.util.LinkedHashMap",ClassType.SYSTEM_COLLECTION);
   class_map.put("java.util.TreeMap",ClassType.SYSTEM_COLLECTION);
   class_map.put("java.util.WeakHashMap",ClassType.SYSTEM_COLLECTION);

   known_classes = new HashMap<String,Class<?>>();

   classpath_add = null;

   stack_monitor = new DyperMonitor(this);

   no_attach = false;

   scanArgs(args);

   num_special = 0;
   ignore_thread = new BitSet(DYPER_MAX_THREADS);
   use_thread = new BitSet(DYPER_MAX_THREADS);
   thread_default = true;				// use all unless otherwise stated

   is_waiting = false;

   if (!no_attach) attach();

   init_overhead = System.currentTimeMillis() - start_time;
}



private void restart()
{
   System.err.println("DYPER: REATTACH ");

   sendStart();
}



private void attach()
{
   if (is_attached) return;
   is_attached = true;

   if (class_inst != null) {
      dyper_inst = new DyperInstrumenter();
      class_inst.addTransformer(dyper_inst,use_transform);
    }

   cmd_handler = new CmdHandler();
   socket_client = new SocketClient();
   socket_client.sendMessage("CONNECT " + process_id + "\n",false);

   System.err.println("DYPER: REGISTER FOR " + process_id);

   monitor_thread = stack_monitor.start();
   ignore_thread.set((int) monitor_thread.getId());

   System.err.println("DYPER: ATTACH");

   Runtime.getRuntime().addShutdownHook(new Shutdown());
}



/********************************************************************************/
/*										*/
/*	Method to scan arguments						*/
/*										*/
/********************************************************************************/

private void prescanArgs(String args)
{
   if (args == null) return;

   StringTokenizer tok = new StringTokenizer(args,":;");
   while (tok.hasMoreTokens()) {
      String arg = tok.nextToken();
      String val = null;
      int idx = arg.indexOf('=');
      if (idx >= 0) {
	 val = arg.substring(idx+1);
	 arg = arg.substring(0,idx);
       }

      if (process_id == null && arg.startsWith("id") && val != null) {
	 process_id = val;
       }
      else if (arg.startsWith("DYVISE") && val != null) {
	 System.setProperty("edu.brown.cs.override.DYVISE",val);
       }
      else if (arg.startsWith("IVY") && val != null) {
	 System.setProperty("edu.brown.cs.override.IVY",val);
       }
      else if (arg.startsWith("DJARCH") && val != null) {
	 System.setProperty("edu.brown.cs.override.BROWN_DYVISE_JAVA_ARCH",val);
       }
      else if (arg.equals("HOST") && val != null) {
	 server_host = val;
       }
      else if (arg.equals("PORT") && val != null) {
	 try {
	    server_port = Integer.parseInt(val);
	  }
	 catch (NumberFormatException e) { }
       }
    }
}




private void scanArgs(String args)
{
   if (args == null) return;

   StringTokenizer tok = new StringTokenizer(args,":;");
   while (tok.hasMoreTokens()) {
      String arg = tok.nextToken();
      int idx = arg.indexOf('=');
      if (idx >= 0) {
	 arg.substring(idx+1);
	 arg = arg.substring(0,idx);
       }
      if (arg.startsWith("enable")) {
	 stack_monitor.enableMonitoring(true);
       }
      else if (arg.startsWith("disable")) {
	 stack_monitor.enableMonitoring(false);
       }
      else if (args.startsWith("noattach")) {
	 no_attach = true;
       }
    }
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

long getMonitorThreadId()
{
   if (monitor_thread == null) return 0;

   return monitor_thread.getId();
}



boolean useThread(long id)
{
   int idx = (int) id;
   if (ignore_thread.get(idx)) return false;
   if (use_thread.get(idx)) return true;
   return thread_default;
}


boolean useThread(int id)
{
   if (ignore_thread.get(id)) return false;
   if (use_thread.get(id)) return true;
   return thread_default;
}


boolean useThread()
{
   // this assumes it won't be called from the monitoring thread
   if (num_special == 0) return thread_default;
   Thread t = Thread.currentThread();
   return useThread(t.getId());
}



void setUseThread(long id,boolean use,boolean ignore)
{
   int iid = (int) id;
   if (ignore != ignore_thread.get(iid)) {
      ignore_thread.set(iid,ignore);
      if (ignore) ++num_special;
      else --num_special;
    }
   if (use != use_thread.get(iid)) {
      use_thread.set(iid,use);
      if (use) ++num_special;
      else --num_special;
    }
}


long getOverhead()
{
   return init_overhead;
}


private void handleUseThread(Element arg)
{
   NodeList nl = arg.getElementsByTagName("THREAD");
   for (int i = 0; i < nl.getLength(); ++i) {
      Element e = (Element) nl.item(i);
      long v0 = getAttrLong(e,"ID");
      boolean ig = getAttrBool(e,"IGNORE");
      boolean us = getAttrBool(e,"USE");
      if (v0 > 0) setUseThread(v0,us,ig);
    }
}




/********************************************************************************/
/*										*/
/*	Communications methods							*/
/*										*/
/********************************************************************************/

void handleRequests(boolean wait)
{
   if (socket_client == null) return;

   for ( ; ; ) {
      is_waiting = wait;
      try {
	 if (!cmd_handler.pollNext(wait)) break;
       }
      catch (Throwable t) {
	 System.err.println("DYPER: Problem processing command: " + t);
	 t.printStackTrace();
       }
      wait = false;
      is_waiting = false;
    }
}



void sendMessage(String xml)
{
   socket_client.sendMessage(xml,false);
}



void wakeUp()
{
   String msg = "<DUMMY/>";
   socket_client.sendMessage(msg,false);
}




/********************************************************************************/
/*										*/
/*	Command handlers							*/
/*										*/
/********************************************************************************/

private String handleWhoAreYou()
{
   long now = System.currentTimeMillis();
   long nnow = System.nanoTime();

   DyperXmlWriter xw = new DyperXmlWriter();
   // xw.begin("WHOIAM");

   int argc = 0;
   String [] args = null;
   String acts = System.getenv("DYPER_ARG_COUNT");
   if (acts != null) {
      int act = Integer.parseInt(acts);
      args = new String [act];
      for (int i = 0; i < act; ++i) {
	 String s = System.getenv("DYPER_ARG_" + i);
	 args[i] = s;
       }
    }
   if (args == null) {
      try {
	 args = getCommandLine();
	 argc = 1;			// skip java command
       }
      catch (Throwable t) {
	 // System.err.println("DYPER: PROBLEM GETTING COMMAND LINE: " + t);
	 // note that we need our native library to be a system library in this case
       }
    }

   if (args != null) {
      while (argc < args.length) {
	 if (args[argc].charAt(0) != '-') break;
	 else if (args[argc].equals("-cp") || args[argc].equals("-classpath")) {
	    xw.begin("JAVAARG");
	    xw.text(args[argc] + " " + args[argc+1]);
	    ++argc;
	    xw.end();
	  }
	 else {
	    xw.textElement("JAVAARG",args[argc]);
	  }
	 ++argc;
       }
      if (argc < args.length) {
	 start_class = args[argc++];
	 xw.textElement("START",start_class);
       }
      while (argc < args.length) {
	 xw.textElement("ARG",args[argc++]);
       }
    }

   Map<String,String> env = System.getenv();
   for (Map.Entry<String,String> ent : env.entrySet()) {
      String k = ent.getKey();
      if (k.startsWith("DYPER_ARG_")) continue;
      xw.begin("ENV");
      xw.field("KEY",k);
      xw.text(ent.getValue());
      xw.end();
    }

   // The following is a bit indirect to avoid concurrent modification exceptions
   Properties props = System.getProperties();
   for (Enumeration<?> e = props.propertyNames(); e.hasMoreElements(); ) {
      String k = (String) e.nextElement();
      String v = props.getProperty(k);
      if (v != null) {
	 xw.begin("PROPERTY");
	 xw.field("KEY",k);
	 xw.text(v);
	 xw.end();
       }
    }

   xw.begin("TIME");
   xw.field("MILLIS",now);
   xw.field("NANO",nnow);
   xw.end();


   init_overhead += System.currentTimeMillis() - now;

   String rslt = xw.toString();
   xw.close();
   return rslt;
}




private void handleSet(Element xml)
{
   if (isElement(xml,"VAR")) {
      processSet(xml);
    }
   else {
      NodeList nl =  xml.getElementsByTagName("VAR");
      for (int i = 0; i < nl.getLength(); ++i) {
	 Element v = (Element) nl.item(i);
	 processSet(v);
       }
    }
}



private String handleClear(String what)
{
   return stack_monitor.clear(what);
}




private void handleSetDetail(Element xml)
{
   if (isElement(xml,"DETAIL")) {
      processDetail(xml);
    }
   else {
      NodeList nl = xml.getElementsByTagName("DETAIL");
      for (int i = 0; i < nl.getLength(); ++i) {
	 Element v = (Element) nl.item(i);
	 processDetail(v);
       }
    }
}



private String handleClassModel(Element opts)
{
   if (opts != null) {
      NodeList nl = opts.getElementsByTagName("CLASS");
      for (int i = 0; i < nl.getLength(); ++i) {
	 Element c = (Element) nl.item(i);
	 String s = getText(c);
	 if (s != null && !s.equals("*")) {
	    loadClass(s);
	  }
       }
    }

   DyperXmlWriter xw = new DyperXmlWriter();

   xw.begin("CLASSMODEL");
   xw.field("CANREDEFINE",class_inst.isRedefineClassesSupported());
   xw.field("CLASSVERSION",System.getProperty("java.class.version"));
   xw.field("JAVAVERSION",System.getProperty("java.runtime.version"));

   String cp = System.getProperty("java.class.path");
   if (classpath_add != null) {
      if (cp == null) cp = classpath_add;
      else cp = cp + File.pathSeparator + classpath_add;
    }
   xw.textElement("CLASSPATH",cp);
   xw.textElement("JAVAHOME",System.getProperty("java.home"));
   xw.textElement("BOOTPATH",System.getProperty("sun.boot.class.path"));
   xw.textElement("CWD",System.getProperty("user.dir"));

   xw.begin("USER");
   xw.field("NAME",System.getProperty("user.name"));
   xw.end();

   Class<?> [] cset = class_inst.getAllLoadedClasses();
   for (Class<?> c : cset) {
      if (c.isArray() || c.isPrimitive()) continue;
      String nm = c.getName();
      xw.begin("CLASS");
      xw.field("NAME",c.getName());
      known_classes.put(nm,c);
      xw.end();
    }

   xw.end();

   String rslt = xw.toString();
   xw.close();
   return rslt;
}



private void handleGC()
{
   System.gc();
}



private String handleDumpHeap(String file,boolean live)
{
   return stack_monitor.dumpHeap(file,live);
}


private String handleDumpMemory(String file)
{
   return stack_monitor.dumpMemory(file);
}




/********************************************************************************/
/*										*/
/*	Instrumentation methods 						*/
/*										*/
/********************************************************************************/

private String handleInstrument(Element xml)
{
   Map<Class<?>,File> patchmap = new LinkedHashMap<Class<?>,File>();
   Set<Class<?>> changemap = new HashSet<Class<?>>();

   NodeList nl = xml.getElementsByTagName("VAR");
   for (int i = 0; i < nl.getLength(); ++i) {
      Element e = (Element) nl.item(i);
      processSet(e);
    }

   nl = xml.getElementsByTagName("DETAIL");
   for (int i = 0; i < nl.getLength(); ++i) {
      Element e = (Element) nl.item(i);
      processDetail(e);
    }

   nl = xml.getElementsByTagName("PATCH");
   for (int i = 0; i < nl.getLength(); ++i) {
      Element e = (Element) nl.item(i);
      String cnm = getAttrString(e,"CLASS");
      if (known_classes.get(cnm) == null) continue;
      String pfl = getAttrString(e,"PATCH");
      boolean chng = getAttrBool(e,"CHANGE");
      if (pfl != null) {
	 File fl = new File(pfl);
	 Class<?> c = known_classes.get(cnm);
	 if (fl.exists() && c != null) patchmap.put(c,new File(pfl));
	 if (chng) changemap.add(c);
       }
    }

   if (patchmap.size() > 0) {
      if (use_transform) {
	 List<Class<?>> clses = dyper_inst.setupTransform(patchmap,changemap);
	 for (Iterator<Class<?>> it = clses.iterator(); it.hasNext(); ) {
	    Class<?> c1 = it.next();
	    if (!class_inst.isModifiableClass(c1)) {
	       System.err.println("DYPER: Class " + c1 + " is not modifiable");
	       it.remove();
	     }
	  }
	 Class<?> [] clsa = new Class<?>[clses.size()];
	 clsa = clses.toArray(clsa);
	 try {
	    class_inst.retransformClasses(clsa);
	  }
	 catch (Throwable t) {
	    System.err.println("DYPER: Problem transforming classes: " + t);
	    for (Class<?> c1 : clsa) {
	       File f1 = patchmap.get(c1);
	       System.err.println("DYPER: Attempt to transform: " + c1 + " " + f1);
	       try {
		  class_inst.retransformClasses(c1);
		  System.err.println("DYPER: Success");
		}
	       catch (Throwable t1) {
		  System.err.println("DYPER: Failure: " + t1);
		}
	     }
	  }
	 for (Class<?> c1 : clsa) {
	    Compiler.compileClass(c1);
	  }
       }
      else {
	 ClassDefinition [] cdefs = new ClassDefinition[patchmap.size()];
	 int cdn = 0;
	 for (Map.Entry<Class<?>,File> ent : patchmap.entrySet()) {
	    try {
	       File f = ent.getValue();
	       Class<?> c = ent.getKey();
	       FileInputStream fis = new FileInputStream(f);
	       int ln = (int) f.length();
	       byte [] buf = new byte[ln];
	       int ct = 0;
	       while (ct < ln) {
		  int i = fis.read(buf,ct,ln-ct);
		  if (i < 0) throw new IOException("Unexpected end of file");
		  ct += i;
		}
	       fis.close();
	       // System.err.println("DYPER: Prepare file for " + c + " " + ent.getKey());
	       cdefs[cdn++] = new ClassDefinition(c,buf);
	     }
	    catch (Throwable t) {
	       System.err.println("DYPER: Problem loading instrumentation: " + t);
	       t.printStackTrace();
	     }
	  }

	 try {
	    class_inst.redefineClasses(cdefs);
	  }
	 catch (Throwable e) {
	    System.err.println("DYPER: Problem doing instrumentation: " + e);
	    e.printStackTrace();
	    checkErrors(cdefs);
	  }
       }
    }

   // System.err.println("DYPER: Instrumentation " + IvyXml.getAttrString(xml,"NAME") + (usenew ? " loaded" : " removed"));

   return "<PATCH TIME='" + System.currentTimeMillis() + "'/>";
}



private void checkErrors(ClassDefinition [] defs)
{
   ClassDefinition [] def1 = new ClassDefinition[1];

   for (int i = defs.length-1; i >= 0; --i) {
      def1[0] = defs[i];
      System.err.println("DYPER: CHECK " + def1[0].getDefinitionClass() + " " +
			    def1[0].getDefinitionClassFile().length);
      try {
	 class_inst.redefineClasses(def1);
       }
      catch (Throwable e) {
	 System.err.println("DYPER: REDEF FAILED: " + e);
	 e.printStackTrace();
	 break;
       }
      System.err.println("DYPER: REDEF OK");
    }
}



/********************************************************************************/
/*										*/
/*	Reporting methods							*/
/*										*/
/********************************************************************************/

private String handleReport(String typ)
{
   ReportType rt = ReportType.ALL;
   if (typ != null) {
      try {
	 rt = ReportType.valueOf(typ);
       }
      catch (IllegalArgumentException e) { }
    }

   DyperXmlWriter xw = new DyperXmlWriter();
   stack_monitor.generateReport(rt,xw);

   return xw.toString();
}



long sendReport(long now)
{
   DyperXmlWriter xw = new DyperXmlWriter();

   xw.begin("DYPER");
   xw.field("REPORT",process_id);
   xw.field("TIME",now);
   stack_monitor.generateReport(ReportType.ALL,xw);
   xw.end();

   socket_client.sendMessage(xw.toString(),false);

   long endt = System.nanoTime();

   return endt;
}



/********************************************************************************/
/*										*/
/*	Stack reporting methods 						*/
/*										*/
/********************************************************************************/

private String handleStackDump()
{
   DyperXmlWriter xw = new DyperXmlWriter();
   stack_monitor.generateStackDump(xw);

   return xw.toString();
}




void sendStackDump(long now,String cnts)
{
   DyperXmlWriter xw = new DyperXmlWriter();
   xw.begin("DYPER");
   xw.field("DUMP",process_id);
   xw.field("TIME",now);
   xw.xmlText(cnts);
   xw.end();

   socket_client.sendMessage(xw.toString(),false);
   xw.close();
}




/********************************************************************************/
/*										*/
/*	Common message routines 						*/
/*										*/
/********************************************************************************/

void sendReply(String r)
{
   String s = "<DYPER_REPLY ID='" + process_id + "'>";
   if (r != null) s += r;
   s += "</DYPER_REPLY>";
   socket_client.sendMessage(s,true);
}



void sendStart()
{
   socket_client.sendMessage("<DYPER START='" + process_id + "' />",false);
}



/********************************************************************************/
/*										*/
/*	Variable access methods 						*/
/*										*/
/********************************************************************************/

long getStartTime()			{ return start_time; }



private void processSet(Element e)
{
   String v = getAttrString(e,"VALUE");
   if (v == null) v = getText(e);

   processSet(getAttrString(e,"NAME"),v);
}



private void processSet(String nm,String val)
{
   if (nm == null) return;

   if (nm.equals("THREAD_DEFAULT")) {
      thread_default = Boolean.parseBoolean(val);
    }
   else if (nm.equals("CLASSPATH")) {
      classpath_add = val;
    }
   else {
      stack_monitor.setParameter(nm,val);
    }
}




private String processGet(String nm)
{
   if (nm == null) return null;

   String rslt = null;

   rslt = stack_monitor.getParameter(nm);

   return rslt;
}



void enableContentionMonitoring(boolean fg)
{
   stack_monitor.enableContentionMonitoring(fg);
}




/********************************************************************************/
/*										*/
/*	Detailing access methods						*/
/*										*/
/********************************************************************************/

private void processDetail(Element e)
{
   stack_monitor.setDetailing(getAttrString(e,"AGENT"),
				 getAttrString(e,"ITEM"),
				 getAttrBool(e,"VALUE"));
}



/********************************************************************************/
/*										*/
/*	Class type methods							*/
/*										*/
/********************************************************************************/

boolean isIOClass(String nm)
{
   return getClassType(nm).isIO();
}


boolean isSystemClass(String nm)
{
   return getClassType(nm).isSYSTEM();
}


boolean isCollectionsClass(String nm)
{
   return getClassType(nm).isCOLLECTION();
}



boolean checkSystemClass(String nm)
{
   ClassType ct = class_map.get(nm);

   if (ct == null) return false;

   return ct.isSYSTEM();
}



private void handleClasses(Element xml)
{
   long now = System.currentTimeMillis();

   if (getAttrBool(xml,"CLEAR")) {
      class_map.clear();
      package_map.clear();
    }
   if (getAttrBool(xml,"CLEARIO")) {
      for (Map.Entry<String,ClassType> ent : class_map.entrySet()) {
	 switch (ent.getValue()) {
	    case IO :
	       ent.setValue(ClassType.NORMAL);
	       break;
	    case SYSTEM_IO :
	       ent.setValue(ClassType.SYSTEM);
	       break;
	    default :
	       break;
	  }
       }
      for (Map.Entry<String,ClassType> ent : package_map.entrySet()) {
	 switch (ent.getValue()) {
	    case IO :
	       ent.setValue(ClassType.NORMAL);
	       break;
	    case SYSTEM_IO :
	       ent.setValue(ClassType.SYSTEM);
	       break;
	    default :
	       break;
	  }
       }
    }
   if (getAttrBool(xml,"CLEARSYSTEM")) {
      for (Map.Entry<String,ClassType> ent : class_map.entrySet()) {
	 switch (ent.getValue()) {
	    case SYSTEM :
	       ent.setValue(ClassType.NORMAL);
	       break;
	    case SYSTEM_IO :
	       ent.setValue(ClassType.IO);
	       break;
	    default :
	       break;
	  }
       }
      for (Map.Entry<String,ClassType> ent : package_map.entrySet()) {
	 switch (ent.getValue()) {
	    case SYSTEM :
	       ent.setValue(ClassType.NORMAL);
	       break;
	    case SYSTEM_IO :
	       ent.setValue(ClassType.IO);
	       break;
	    default :
	       break;
	  }
       }
    }

   NodeList nl = xml.getElementsByTagName("PACKAGE");
   for (int i = 0; i < nl.getLength(); ++i) {
      Element c = (Element) nl.item(i);
      String nm = getAttrString(c,"NAME");
      String typ = getAttrString(c,"TYPE");
      setPackage(nm,typ);
    }

   nl = xml.getElementsByTagName("CLASS");
   for (int i = 0; i < nl.getLength(); ++i) {
      Element c = (Element) nl.item(i);
      String nm = getAttrString(c,"NAME");
      String typ = getAttrString(c,"TYPE");
      setClass(nm,typ);
    }

   init_overhead += System.currentTimeMillis() - now;
}



private ClassType getClassType(String nm)
{
   ClassType ct = class_map.get(nm);

   if (ct == null) {
      ct = ClassType.NORMAL;
      int len = 0;
      for (Map.Entry<String,ClassType> ent : package_map.entrySet()) {
	 String k = ent.getKey();
	 if (k.length() > len && nm.startsWith(k)) {
	    ct = ent.getValue();
	    len = k.length();
	  }
       }
      class_map.put(nm,ct);
    }

   return ct;
}



private void setClass(String nm,String typ)
{
   setMatches(nm,typ,class_map);
}



private void setPackage(String nm,String typ)
{
   if (!nm.endsWith(".")) nm += ".";

   for (Iterator<Map.Entry<String,ClassType>> it = class_map.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<String,ClassType> ent = it.next();
      if (ent.getKey().startsWith(nm)) it.remove();
    }

   setMatches(nm,typ,package_map);
}



private void setMatches(String nm,String typ,Map<String,ClassType> map)
{
   if (nm == null || typ == null) return;
   try {
      ClassType ctyp = ClassType.valueOf(typ);
      map.put(nm,ctyp);
    }
   catch (IllegalArgumentException e) {
      System.err.println("DYPER: Illegal match type " + typ + ": " + e);
    }
}



private String handleGetClasses(Element xml)
{
   String pkg = null;
   ClassType ct = null;

   if (xml != null) {
      String typ = getAttrString(xml,"TYPE");
      if (typ != null) {
	 try {
	    ct = ClassType.valueOf(typ);
	  }
	 catch (IllegalArgumentException e) { }
       }
      pkg = getAttrString(xml,"NAME");

      NodeList nl = xml.getElementsByTagName("CLASS");
      for (int i = 0; i < nl.getLength(); ++i) {
	 Element c = (Element) nl.item(i);
	 String s = getText(c);
	 if (s != null && !s.equals("*")) {
	    loadClass(s);
	  }
       }
    }

   DyperXmlWriter xw = new DyperXmlWriter();
   xw.begin("CLASSES");
   for (Map.Entry<String,ClassType> ent : package_map.entrySet()) {
      if (matchClass(ent.getKey(),ent.getValue(),pkg,ct)) {
	 xw.begin("PACKAGE");
	 xw.field("NAME",ent.getKey());
	 xw.field("TYPE",ent.getValue().toString());
	 xw.end();
       }
    }

   for (Map.Entry<String,ClassType> ent : class_map.entrySet()) {
      String cnm = ent.getKey();
      ClassType cty = ent.getValue();
      if (matchClass(cnm,cty,pkg,ct)) {
	 boolean pfnd = false;
	 for (Map.Entry<String,ClassType> pent : package_map.entrySet()) {
	    String pnm = pent.getKey();
	    if (cnm.startsWith(pnm) && cty == pent.getValue()) {
	       pfnd = true;
	       break;
	     }
	  }
	 if (!pfnd) {
	    xw.begin("CLASS");
	    xw.field("NAME",cnm);
	    xw.field("TYPE",cty.toString());
	    xw.end();
	  }
       }
    }
   xw.end();

   String rslt = xw.toString();
   xw.close();
   return rslt;
}



private boolean matchClass(String cnm,ClassType cty,String pnm,ClassType pty)
{
   if (pnm != null && !cnm.startsWith(pnm)) return false;
   if (pty != null && cty != pty) return false;

   return true;
}




private Class<?> loadClass(String c)
{
   try {
      return Class.forName(c);
    }
   catch (ClassNotFoundException e) { }

   ClassLoader sclr = ClassLoader.getSystemClassLoader();
   try {
      return sclr.loadClass(c);
    }
   catch (ClassNotFoundException e) { }

   System.err.println("DYPER: Cannot find class " + c);
   System.err.println("DYPER: Class loader = " + c);

   return null;
}



/********************************************************************************/
/*										*/
/*	Message handlers							*/
/*										*/
/********************************************************************************/

private class CmdHandler {

   boolean pollNext(boolean wait) {
      Element e = socket_client.readCommand(wait);
      if (e == null) return false;
      receive(e);
      return true;
    }

   public void receive(Element xml) {
      long now = System.currentTimeMillis();
      String cmd = getAttrString(xml,"COMMAND");
      String rslt = null;
      System.err.println("DYPER: Command: " + cmd);
      Element cnts = null;
      for (Node n = xml.getFirstChild(); n != null; n = n.getNextSibling()) {
	 if (n.getNodeType() == Node.ELEMENT_NODE) {
	    cnts = (Element) n;
	    break;
	  }
       }

      if (cmd == null) ;
      else if (cmd.equals("WHO")) {
	 rslt = "<DYPER_REPLY ID='" + process_id +
	    "' ACTIVE='" + stack_monitor.isMonitoringEnabled() + "' />";
       }
      else if (cmd.equals("WHORU")) {
	 rslt = handleWhoAreYou();
       }
      else if (cmd.equals("PING")) {
	 rslt = "PONG " + init_overhead;
       }
      else if (cmd.equals("SET")) {
	 handleSet(cnts);
       }
      else if (cmd.equals("GET")) {
	 if (cnts != null) rslt = processGet(getAttrString(cnts,"NAME"));
       }
      else if (cmd.equals("CLEAR")) {
	 if (cnts == null) rslt = handleClear(null);
	 else rslt = handleClear(getAttrString(cnts,"AGENT"));
       }
      else if (cmd.equals("SETDETAIL")) {
	 handleSetDetail(cnts);
       }
      else if (cmd.equals("CLASSES")) {
	 handleClasses(cnts);
       }
      else if (cmd.equals("GETCLASSES")) {
	 rslt = handleGetClasses(cnts);
       }
      else if (cmd.equals("CLASSMODEL")) {
	 rslt = handleClassModel(cnts);
       }
      else if (cmd.equals("REPORT")) {
	 if (cnts == null) rslt = handleReport(null);
	 else rslt = handleReport(getAttrString(cnts,"TYPE"));
       }
      else if (cmd.equals("SHOWSTACK")) {
	 rslt = handleStackDump();
       }
      else if (cmd.equals("INSTRUMENT")) {
	 rslt = handleInstrument(cnts);
       }
      else if (cmd.equals("AGENT")) {
	 if (cnts != null) {
	    String anm = getAttrString(cnts,"CLASS");
	    if (anm != null && !installed_agents.contains(anm)) {
	       installed_agents.add(anm);
	       try {
		  String s = System.getProperty("sun.boot.class.path");
		  if (s != null && !s.contains(DYPER_PATCH_JAR)) {
		     System.setProperty("sun.boot.class.path",s + File.pathSeparator +
					   DYPER_PATCH_JAR);
		   }
		  Class<?> c = loadClass(anm);
		  if (c == null) throw new ClassNotFoundException(anm);
		  Constructor<?> con = c.getConstructor(DyperControl.class);
		  DyperAgent da = (DyperAgent) con.newInstance(DyperControl.this);
		  stack_monitor.addAgent(da);
		  rslt = "OK";
		}
	       catch (Throwable t) {
		  System.err.println("DYPER: Problem installing agent " + anm + ": " + t);
		  t.printStackTrace();
		}
	     }
	  }
       }
      else if (cmd.equals("ACTIVATE")) {
	 if (cnts != null) {
	    String anm = getAttrString(cnts,"AGENT");
	    boolean act = isElement(cnts,"ACTIVATE");
	    if (act) stack_monitor.reactivateAgent(anm);
	    else stack_monitor.deactivateAgent(anm,System.currentTimeMillis());
	  }
       }
      else if (cmd.equals("USETHREAD")) {
	 handleUseThread(cnts);
       }
      else if (cmd.equals("GC")) {
	 handleGC();
       }
      else if (cmd.equals("DUMPHEAP")) {
	 if (cnts == null) rslt = handleDumpHeap("heap.out",true);
	 else rslt = handleDumpHeap(getAttrString(cnts,"FILE"),
	       getAttrBool(cnts,"LIVE",true));
       }
      else if (cmd.equals("DUMPMEMORY")) {
	 // System.err.println("DYPER: Handle DUMP MEMORY");
	 if (cnts == null) rslt = handleDumpMemory("memory.out");
	 else rslt = handleDumpMemory(getAttrString(cnts,"FILE"));
       }
      else {
	 System.err.println("DYPER: Unknown command " + cmd);
       }

      sendReply(rslt);

      init_overhead += System.currentTimeMillis() - now;
    }

}	// end of subclass CmdHandler



/********************************************************************************/
/*										*/
/*	Reply handlers								*/
/*										*/
/********************************************************************************/





/********************************************************************************/
/*										*/
/*	Exit management 							*/
/*										*/
/********************************************************************************/

private class Shutdown extends Thread {

   Shutdown() {
      super("DyperControlShutdown");
    }

   public void run() {
      if (socket_client != null) {
	 if (!is_waiting)
	    socket_client.sendMessage("<DYPER END='" + process_id + "' />",false);
       }
    }

}	// end of subclass Shutdown



/********************************************************************************/
/*										*/
/*	Utility methods 							*/
/*										*/
/********************************************************************************/

private static String [] getCommandLine()
{
   List<String> cmds = new ArrayList<String>();

   try {
      File f = new File("/proc/self/cmdline");
      boolean fnd = false;
      if (f.exists()) {
	 FileReader fr = new FileReader(f);
	 char [] buf = new char[100000];
	 int fsz = buf.length;
	 int off = 0;
	 for ( ; ; ) {
	    int rsz = fr.read(buf,off,fsz-off);
	    if (rsz < 0) break;
	    off += rsz;
	  }
	 int sz = off;
	 fr.close();
	 if (sz != 0 && sz != 4096) {
	    int st = 0;
	    for (int i = 1; i < sz; ++i) {
	       if (buf[i] == 0) {
		  String s = new String(buf,st,i-st);
		  cmds.add(s);
		  st = i+1;
		}
	     }
	    if (sz > st) {
	       String s = new String(buf,st,sz-st);
	       cmds.add(s);
	     }
	    fnd = true;
	  }
       }
      if (!fnd) {
	 tryUsingJps(cmds);
	 if (cmds.isEmpty()) tryUsingPs(cmds);
       }
    }
   catch (Throwable t) {
      System.err.println("IVY: EXEC: Problem getting command line: " + t);
    }

   String [] r = new String[cmds.size()];
   r = cmds.toArray(r);

   return r;
}



private static void tryUsingJps(List<String> args) throws IOException
{
   String pid = getProcessId();
   if (pid == null) return;

   ProcessBuilder pb = new ProcessBuilder("jps","-ml");
   Process ex = pb.start();
   InputStreamReader isr = new InputStreamReader(ex.getInputStream());
   BufferedReader br = new BufferedReader(isr);
   for ( ; ; ) {
      String ln = br.readLine();
      if (ln == null) return;
      StringTokenizer tok = new StringTokenizer(ln);
      if (!tok.hasMoreTokens()) continue;
      String tpid = tok.nextToken();
      if (tpid.equals(pid)) {
	 args.add("java");              // implicit 'java' command
	 while (tok.hasMoreTokens()) args.add(tok.nextToken());
	 break;
       }
    }
}


private static void tryUsingPs(List<String> args) throws IOException
{
   String pid = getProcessId();
   if (pid == null) return;

   ProcessBuilder pb = new ProcessBuilder("ps","-o","command","-www");
   Process ex = pb.start();
   InputStreamReader isr = new InputStreamReader(ex.getInputStream());
   BufferedReader br = new BufferedReader(isr);
   String ln = br.readLine();
   if (ln == null) return;
   ln = br.readLine();
   if (ln == null) return;

   StringTokenizer tok = new StringTokenizer(ln);
   while (tok.hasMoreTokens()) args.add(tok.nextToken());
}



private static String getProcessId()
{
   String mpid = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
   if (mpid != null) {
      int idx = mpid.indexOf("@");
      if (idx >= 0) return mpid.substring(0,idx);
    }

   try {
      File f = new File("/proc/self/stat");
      if (f.exists()) {
	 FileReader fr = new FileReader(f);
	 int pid = 0;
	 for ( ; ; ) {
	    int c = fr.read();
	    if (c < '0' || c > '9') break;
	    pid = pid * 10 + c - '0';
	  }
	 fr.close();
	 return Integer.toString(pid);
       }
    }
   catch (Throwable t) {
      System.err.println("IVY: Problem opening /proc/self: " + t);
    }

   try {
      // TODO: Doesn't work on windows
      ProcessBuilder pb = new ProcessBuilder("perl","-e","'print getppid() . \"\n\";'");
      Process ex = pb.start();
      InputStreamReader isr = new InputStreamReader(ex.getInputStream());
      int pid = 0;
      for ( ; ; ) {
	 int c = isr.read();
	 if (c < '0' || c > '9') break;
	 pid = pid * 10 + c - '0';
       }
      isr.close();
      ex.destroy();
      return Integer.toString(pid);
    }
   catch (Throwable t) {
      System.err.println("IVY: Problem executing perl: " + t);
    }

   return null;
}



private static String getHostName()
{
   String h = System.getProperty("edu.brown.cs.dyvise.HOST");
   if (h != null) return h;
   h = System.getenv("HOST");
   if (h != null) return h;

   try {
      InetAddress lh = InetAddress.getLocalHost();
      h = lh.getCanonicalHostName();
      if (h != null) return h;
    }
   catch (IOException e ) {
      System.err.println("IVY: Problem getting host name: " + e);
    }

   return "localhost";
}



/********************************************************************************/
/*										*/
/*	XML utility functions							*/
/*										*/
/********************************************************************************/

static private String getText(Node xml)
{
   if (xml == null) return null;
   if (xml.getNodeType() == Node.TEXT_NODE) {
      String r = xml.getNodeValue();
      if (r != null) r = r.trim();
      return r;
    }
   else if (xml.getNodeType() == Node.CDATA_SECTION_NODE) {
      String r = xml.getNodeValue();
      return r;
    }
   else if (xml.getNodeType() == Node.ELEMENT_NODE) {
      NodeList nl = xml.getChildNodes();

      if (nl == null) return null;

      StringBuffer buf = new StringBuffer();

      for (int i = 0; ; ++i) {
	 Node nc = nl.item(i);
	 if (nc == null) break;
	 String s = getText(nc);
	 if (s != null) buf.append(s);
       }

      if (buf.length() == 0) return null;

      return buf.toString();
    }

   return null;
}


static private Element convertStringToXml(String s)
{
   if (s == null) return null;

   Document xdoc = null;
   DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
   dbf.setValidating(false);
   dbf.setIgnoringElementContentWhitespace(true);
   DocumentBuilder parser = null;
   try {
      parser = dbf.newDocumentBuilder();
    }
   catch (ParserConfigurationException e) {
      System.err.println("DYPER: parser problem: " + e);
      return null;
    }

   StringReader sr = new StringReader(s);
   InputSource ins = new InputSource(sr);

   try {
      xdoc = parser.parse(ins);
    }
   catch (Exception e) {
      System.err.println("DYPER: xml parse error: " + e.getMessage());
      System.err.println("DYPER: xml string: " + s);
      e.printStackTrace();
    }

   try {
      if (xdoc != null) xdoc.normalizeDocument();
    }
   catch (Throwable t) { }

   return xdoc.getDocumentElement();
}



static private boolean getAttrBool(Node frm,String id)
{
   return getAttrBool(frm,id,false);
}


static private boolean getAttrBool(Node frm,String id,boolean dflt)
{
   if (frm == null) return dflt;

   NamedNodeMap map = frm.getAttributes();
   Node n = map.getNamedItem(id);

   if (n == null) return dflt;
   if (n.getNodeType() != Node.ATTRIBUTE_NODE) return dflt;

   Attr a = (Attr) n;

   String v = a.getValue();
   if (v.length() == 0) return true;

   char c = v.charAt(0);
   if (c == 'f' || c == 'F' || c == '0' || c == 'n' || c == 'N') return false;

   return true;
}



static private String getAttrString(Node frm,String id)
{
   if (frm == null) return null;

   NamedNodeMap map = frm.getAttributes();
   Node n = map.getNamedItem(id);

   if (n == null) return null;
   if (n.getNodeType() != Node.ATTRIBUTE_NODE) return null;

   Attr a = (Attr) n;

   return a.getValue();
}



static private long getAttrLong(Node frm,String id)
{
   if (frm == null) return -1;

   NamedNodeMap map = frm.getAttributes();
   Node n = map.getNamedItem(id);

   if (n == null) return -1;
   if (n.getNodeType() != Node.ATTRIBUTE_NODE) return -1;

   Attr a = (Attr) n;

   try {
      return Long.parseLong(a.getValue());
    }
   catch (NumberFormatException e) { }

   return -1;
}



static private boolean isElement(Node xml,String id)
{
   if (xml == null) return false;
   if (xml.getNodeType() != Node.ELEMENT_NODE) return false;
   if (id == null) return true;

   String s;
   try {
      s = xml.getNodeName();
    }
   catch (Throwable t) {
      s = xml.getNodeName();
    }

   if (id.equalsIgnoreCase(s)) return true;
   if (id.equalsIgnoreCase(xml.getLocalName())) return true;
   return false;
}




/********************************************************************************/
/*										*/
/*	SocketClient to talk with MINT through server				*/
/*										*/
/********************************************************************************/

private class SocketClient {

   private OutputStream output_stream;
   private BufferedReader input_reader;
   private char [] char_trailer;
   private char [] reply_trailer;
   private byte [] byte_buffer;

   SocketClient() {
      output_stream = null;
      byte_buffer = new byte[65536];
      String eom = DYPER_TRAILER + "\n";
      char_trailer = eom.toCharArray();
      eom = DYPER_REPLY_TRAILER + "\n";
      reply_trailer = eom.toCharArray();

      try {
	 @SuppressWarnings("resource")
	 Socket cs = new Socket(server_host,server_port);
	 output_stream = cs.getOutputStream();
	 input_reader = new BufferedReader(new InputStreamReader(cs.getInputStream()));
       }
      catch (Exception e) {
	 System.err.println("DYPER: No server connection for " + server_host + "@" + server_port + ": " + e);
       }
    }



   Element readCommand(boolean wait) {
      if (input_reader == null) return null;
      StringBuffer buf = new StringBuffer();
      try {
	 for ( ; ; ) {
	    if (!wait && !input_reader.ready()) return null;
	    for ( ; ; ) {
	       String data = input_reader.readLine();
	       if (data == null) {
		  input_reader = null;
		  output_stream = null;
		  return null;
		}
	       if (data.equals(DYPER_TRAILER)) break;
	       buf.append(data);
	       buf.append("\n");
	     }
	    if (buf.length() > 0) {
	       return convertStringToXml(buf.toString());
	     }
	  }
       }
      catch (IOException e) {
	 System.err.println("DYPER: Lost server connection: " + e);
	 input_reader = null;
	 output_stream = null;
	 return null;
       }
    }

   void sendMessage(CharSequence msg,boolean rply) {
      if (output_stream == null) return;
      int slen = 0;
      int xlen = 0;
      char [] tail = (rply ? reply_trailer : char_trailer);
      if (msg != null) {
	 slen = msg.length();
	 if (msg.charAt(slen-1) != '\n') xlen = 1;
       }
      if (slen + xlen + tail.length > byte_buffer.length) {
	 byte_buffer = new byte[slen*2 + tail.length];
       }
      if (msg != null) {
	 for (int i = 0; i < slen; ++i) {
	    byte_buffer[i] = (byte) msg.charAt(i);
	  }
	 if (xlen > 0) byte_buffer[slen++] = '\n';
       }
      for (int i = 0; i < tail.length; ++i) {
	 byte_buffer[slen++] = (byte) tail[i];
       }
      try {
	 // System.err.println("DYPER: SEND TO MONITOR: " + new String(byte_buffer,0,slen) + " " + rply);
	 output_stream.write(byte_buffer,0,slen);
	 output_stream.flush();
       }
      catch (IOException e) {
	 System.err.println("DYPER: problem writing output: " + e);
	 output_stream = null;
       }
    }

}	// end of inner class SocketClient





}	// end of class DyperControl




/* end of DyperControl.java */
