/********************************************************************************/
/*										*/
/*		DypatchMain.java						*/
/*										*/
/*	Main program for DYPER patcher						*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dypatchasm/DypatchMain.java,v 1.2 2013/09/04 18:36:34 spr Exp $ */

/*********************************************************************************
 *
 * $Log: DypatchMain.java,v $
 * Revision 1.2  2013/09/04 18:36:34  spr
 * Minor bug fixes.
 *
 *
 * Revision 2.0 2013-08-19 16:26:07 zolstein
 * Ported to ASM
 *
 * Revision 1.7  2012-10-05 00:52:59  spr
 * Code clean up.
 *
 * Revision 1.6  2010-06-01 19:26:24  spr
 * Upgrades to make dyview work on the mac
 *
 * Revision 1.5  2010-03-30 16:22:33  spr
 * Change mint mode.
 *
 * Revision 1.4  2009-09-19 00:13:15  spr
 * Update patcher to handle line number patching.
 *
 * Revision 1.3  2009-03-20 02:07:32  spr
 * Fix imports.
 *
 * Revision 1.2  2008-11-12 14:10:56  spr
 * Change patcher to handle overlapping requests.
 *
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/

package edu.brown.cs.dyvise.dypatchasm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import edu.brown.cs.ivy.mint.MintArguments;
import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintDefaultReply;
import edu.brown.cs.ivy.mint.MintHandler;
import edu.brown.cs.ivy.mint.MintMessage;
import edu.brown.cs.ivy.mint.MintReply;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;


public class DypatchMain implements DypatchConstants, MintConstants {

/********************************************************************************/
/*										*/
/* Main program */
/*										*/
/********************************************************************************/

public static void main(String[] args)
{
   System.err.println("Setup DypatchMain");
   DypatchMain dm = new DypatchMain(args);

   if (!test_flag) dm.process();

   if (test_flag) {
      for (int i : test_list) {
	 current_test = i;
	 switch (i) {
	    case 0:
	       System.err.println("DYPATCH: Do test");
	       dm.test0();
	       break;
	    case 1:
	       System.err.println("DYPATCH: Do test1");
	       dm.test1();
	       break;
	    case 2:
	       System.err.println("DYPATCH: Do test2");
	       dm.test2();
	       break;
	    default:
	       System.err.println("DYPATCH: No test" + i);
	       break;
	 }
      }
   }


   if (test_flag) {}

}

/********************************************************************************/
/*										*/
/* Private Storage */
/*										*/
/********************************************************************************/

private String			       process_id;
private MintControl		       mint_control;

private String			       class_path;
private String			       boot_path;
private Map<String, ClassType>	       package_map;
private Map<String, ClassType>	       class_map;
private HashSet<String> 	       known_classes;
private long			       last_command;
private Timer			       dypatch_timer;
private Collection<Element>	       active_models;
private Collection<String>	       active_classes;
private Map<String, Element>	       patch_models;
private Map<String, Element>	       mode_map;
private Map<String, Integer>	       id_table;
private Map<String, Map<String, File>> class_files;
private DynamicURLClassLoader	       class_loader;

private static File		       patch_directory;

private static int		       debug_flag = 0;
private static boolean		       test_flag  = false;
private static boolean		       name_flag  = false;

private static int[]		       test_list;
private static int		       current_test;

static {
   patch_directory = new File(DYPER_PATCH_DIRECTORY);
   if (!patch_directory.exists()) patch_directory.mkdirs();
}

/********************************************************************************/
/*										*/
/* Constructors */
/*										*/
/********************************************************************************/
private DypatchMain(String[] args)
{
   process_id = null;
   mint_control = null;
   last_command = System.currentTimeMillis();
   active_models = new HashSet<Element>();
   active_classes = new HashSet<String>();
   class_files = new HashMap<String, Map<String, File>>();

   scanArgs(args);

   class_path = null;
   boot_path = null;

   package_map = new HashMap<String, ClassType>();
   class_map = new HashMap<String, ClassType>();
   known_classes = new HashSet<String>();
   patch_models = new HashMap<String, Element>();
   id_table = new HashMap<String, Integer>();

   if (ClassLoader.getSystemClassLoader() instanceof URLClassLoader) {
      class_loader = new DynamicURLClassLoader(
	 (URLClassLoader) ClassLoader.getSystemClassLoader());
    }
   else {
      class_loader = new DynamicURLClassLoader();
    }

   mode_map = loadModes();

   dypatch_timer = new Timer("DypatchExitTimer");
   setupExit();
}

/********************************************************************************/
/*										*/
/* Argument processing */
/*										*/
/********************************************************************************/

private void scanArgs(String[] args)
{
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
	 if (args[i].startsWith("-X")) { // -X (debug)
	    ++debug_flag;
	 }
	 else if (args[i].startsWith("-T")) { // -Test
	    test_flag = true;
	    if (args.length - 1 <= i || args[i + 1].startsWith("-")) {
	       test_list = new int[] { 0 };
	    }
	    else {
	       char[] tests = args[++i].toCharArray();
	       test_list = new int[tests.length];
	       for (int j = 0; j < test_list.length; j++) {
		  test_list[j] = tests[j] - '0';
	       }
	    }
	 }
	 else if (args[i].startsWith("-N")) { // -Named
	    name_flag = true;
	 }
	 else if (args[i].startsWith("-d") && i + 1 < args.length) { // -d
	    // <directory>
	    patch_directory = new File(args[++i]);
	 }
	 else if (args[i].startsWith("-P") && i + 1 < args.length) { // -P
	    // <processid>
	    process_id = args[++i];
	 }
	 else badArgs();
      }
      else badArgs();
   }

   if (process_id == null) badArgs();
}

private void badArgs()
{
   System.err.println("DYPATCH: dypatch -P <master> [-d <directory>]");
   System.exit(1);
}

// Loads modeMap from MODE_FILE.
public static Map<String, Element> loadModes()
{
   Map<String, Element> modeMap = new HashMap<String, Element>();

   File f = new File(MODE_FILE);
   if (f == null || !f.exists()) {
      f = new File(ALT_MODE_FILE);
   }

   Element xml = IvyXml.loadXmlFromFile(f);
   if (xml == null) return modeMap;

   for (Element e : IvyXml.elementsByTag(xml, "MODE")) {
      try {
	 modeMap.put(IvyXml.getAttrString(e, "NAME"), e);
      }
      catch (Throwable t) {
	 System.err.println("DYPATCH: Problem loading patch mode "
		  + IvyXml.getAttrString(e, "NAME") + ": " + t);
	 t.printStackTrace();
      }
   }
   return modeMap;
}


/********************************************************************************/
/*										*/
/* Processing methods */
/*										*/
/********************************************************************************/

private void process()
{
   mint_control = MintControl.create(DYPER_MESSAGE_BUS, MintSyncMode.SINGLE);

   mint_control.register("<DYPATCH PID='" + process_id
	    + "' COMMAND='_VAR_0'><_VAR_1/></DYPATCH>", new PatchCommand());
}

/********************************************************************************/
/*										*/
/* Message routines */
/*										*/
/********************************************************************************/

void sendDyperMessage(String cmd,String body,MintReply hdlr,int flags)
{
   if (test_flag) {
      return;
   }
   String s = "<DYPER PID='" + process_id + "'";
   if (cmd != null) s += " COMMAND='" + cmd + "'";
   s += ">";
   if (body != null) s += body;
   s += "</DYPER>";

   if (mint_control != null) {
      if (hdlr == null) mint_control.send(s);
      else mint_control.send(s, hdlr, flags);
   }
   else {
      System.err.println("DYPATCH: MINT MESSAGE: " + s);
   }
}

void sendReply(MintMessage msg,String r)
{
   String s = "<DYPATCH_REPLY ID='" + process_id + "'>";
   if (r != null) s += r;
   s += "</DYPATCH_REPLY>";

   msg.replyTo(s);
}

/********************************************************************************/
/*										*/
/* Access methods */
/*										*/
/********************************************************************************/

String getProcessId()
{
   return process_id;
}

static boolean doDebug()
{
   return debug_flag > 0;
}

/********************************************************************************/
/*										*/
/* File output methods */
/*										*/
/********************************************************************************/

static File newOutputFile()
{
   File f = null;

   try {
      f = File.createTempFile("dypatch", ".class", patch_directory);
   }
   catch (IOException e) {
      System.err.println("DYPATCH: Problem creating temp file: " + e);
      return null;
   }

   f.deleteOnExit();

   return f;
}

static File newNamedOutputFile(byte[] data,String name)
{
   File f = new File(name + ".class");
   try {
      f.getParentFile().mkdirs();
      f.createNewFile();
   }
   catch (IOException e) {
      System.err.println("DYPATCH: Problem creating temp file: " + e);
      return null;
   }

   try {
      FileOutputStream fos = new FileOutputStream(f);
      fos.write(data);
      fos.close();
   }
   catch (IOException e) {
      System.err.println("DYPATCH: Problem outputing temp file: " + e);
      f = null;
   }

   f.deleteOnExit();
   return f;
}

static File newOutputFile(byte[] data)
{
   File f = newOutputFile();
   if (f == null) return null;

   try {
      FileOutputStream fos = new FileOutputStream(f);
      fos.write(data);
      fos.close();
   }
   catch (IOException e) {
      System.err.println("DYPATCH: Problem outputing temp file: " + e);
      f = null;
   }

   return f;
}

/********************************************************************************/
/*										*/
/* Patching setup methods */
/*										*/
/**********************************************/

void initializeClassModel()
{
   if (known_classes.size() == 0) setupClassModel(null);
}

void initializeClassModel(Collection<String> clss)
{
   boolean rq = false;
   if (known_classes.size() == 0) rq = true;
   if (clss != null) {
      for (String s : clss) {
	 if (!known_classes.contains(s)) rq = true;
      }
   }

   if (rq) setupClassModel(clss);
}

void setupClassModel(Collection<String> clsset)
{
   String clslist = null;
   if (clsset != null) {
      IvyXmlWriter xw = new IvyXmlWriter();
      xw.begin("CLASSES");
      for (String s : clsset)
	 xw.textElement("CLASS", s);
      xw.end("CLASSES");
      clslist = xw.toString();
      xw.close();
   }

   if (test_flag) {
      testSetupClasses(current_test);
      return;
   }

   MintDefaultReply mdr = new MintDefaultReply();
   sendDyperMessage("GETCLASSES", clslist, mdr, MINT_MSG_FIRST_NON_NULL);
   Element e = mdr.waitForXml();
   if (e == null) {
      System.err.println("DYPATCH: Process didn't respond for GETCLASSES");
      System.exit(0);
   }
   e = IvyXml.getElementByTag(e, "CLASSES");
   setupSystemClasses(e);

   mdr = new MintDefaultReply();
   sendDyperMessage("CLASSMODEL", null, mdr, MINT_MSG_FIRST_NON_NULL);
   e = mdr.waitForXml();
   if (e == null) {
      System.err.println("DYPATCH: dyper process didn't respond for classmodel");
      System.exit(0);
   }
   if (debug_flag > 0) System.err.println("DYPATCH: Received model "
	    + IvyXml.convertXmlToString(e));
   
   e = IvyXml.getElementByTag(e, "CLASSMODEL");
   setupClasses(e);
}

private void setupSystemClasses(Element e)
{
   package_map.clear();
   class_map.clear();

   for (Element p : IvyXml.elementsByTag(e, "PACKAGE")) {
      String nm = IvyXml.getAttrString(p, "NAME");
      ClassType typ = IvyXml.getAttrEnum(p, "TYPE", ClassType.NORMAL);
      package_map.put(nm, typ);
   }
   for (Element p : IvyXml.elementsByTag(e, "CLASS")) {
      String nm = IvyXml.getAttrString(p, "NAME");
      ClassType typ = IvyXml.getAttrEnum(p, "TYPE", ClassType.NORMAL);
      class_map.put(nm, typ);
   }
}

private synchronized void setupClasses(Element xml)
{
   class_path = IvyXml.getTextElement(xml, "CLASSPATH");
   boot_path = IvyXml.getTextElement(xml, "BOOTPATH");
   String cwd = IvyXml.getTextElement(xml, "CWD");
   if (cwd != null) {
      class_path = fixupPaths(cwd, class_path);
      boot_path = fixupPaths(cwd, boot_path);
   }
   class_loader.addClassPath(class_path);
   if (boot_path != null) class_loader.addClassPath(boot_path);

   for (Element ce : IvyXml.elementsByTag(xml, "CLASS")) {
      String nm = IvyXml.getAttrString(ce, "NAME");
      known_classes.add(nm);
      if (!checkIfClassExists(nm)) {
	 if (debug_flag > 0) System.err.println("DYPATCH: Can't find class " + nm);
      }
   }
}

void handleAddClass(Element xml)
{
   for (Element ce : IvyXml.elementsByTag(xml, "CLASS")) {
      String nm = IvyXml.getAttrString(ce, "NAME");
      known_classes.add(nm);
      if (!checkIfClassExists(nm)) {
	 if (debug_flag > 0) System.err.println("DYPATCH: Can't find class " + nm);
      }

   }
}

private static boolean checkIfClassExists(String nm)
{
   try {
      @SuppressWarnings("unused") ClassReader cr = new ClassReader(nm);
      return true;
   }
   catch (IOException e) {
      return false;
   }
}

boolean isClassKnown(String nm)
{
   return known_classes.contains(nm);
}

private static int numOfChar(String s,char ch)
{
   int n = 0;
   for (char c : s.toCharArray()) {
      if (c == ch) {
	 ++n;
      }
   }
   return n;
}

private String fixupPaths(String wd,String paths)
{
   if (paths == null || wd == null) return null;

   StringTokenizer tok = new StringTokenizer(paths,File.pathSeparator);
   StringBuffer buf = new StringBuffer();
   while (tok.hasMoreTokens()) {
      String p = tok.nextToken();
      File f = new File(p);
      if (!f.isAbsolute()) p = wd + File.separator + p;
      if (buf.length() > 0) buf.append(File.pathSeparator);
      buf.append(p);
   }

   return buf.toString();
}

// Visit class by name with given ClassVisitor
private void visitClass(String name,ClassVisitor cv) throws IOException
{
   String res = name.replace('.', '/') + ".class";
   InputStream is = class_loader.getResourceAsStream(res);
   if (is == null) {
      ClassLoader cl1 = ClassLoader.getSystemClassLoader();
      is = cl1.getResourceAsStream(res);
    }
   if (is == null) {
      System.err.println("DYPATCH: Can't find " + name);
      return;
    }
   ClassReader cr = new ClassReader(is);
   cr.accept(cv, ClassReader.SKIP_FRAMES);
}




/********************************************************************************/
/*										*/
/* Model management commands */
/*										*/
/********************************************************************************/

private synchronized String handleModelCommand(Element xml)
{
   if (xml == null) return null;

   String nm = IvyXml.getAttrString(xml, "NAME");
   if (nm == null) return null;

   patch_models.put(nm, xml);

   Collection<String> clss = new HashSet<String>();
   for (Element e : IvyXml.children(xml, "CLASS")) {
      String cnm = IvyXml.getAttrString(e, "NAME");
      clss.add(cnm);
   }
   for (Element e : IvyXml.children(xml, "FOR")) {
      String cnm = IvyXml.getAttrString(e, "CLASS");
      if (cnm != null && !cnm.equals("*")) clss.add(cnm);
   }
   initializeClassModel(clss);

   return setupCounters(xml);
}

private String setupCounters(Element xml)
{
   Set<String> counted = new HashSet<String>();
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("COUNTERS");
   xw.begin("COUNTERDATA");
   DypatchCounter counter = new DypatchCounter(ASM_VERSION,xw,class_loader);
   for (Element e : IvyXml.children(xml)) {
      if (IvyXml.isElement(e, "CLASS") || IvyXml.isElement(e, "FOR")) {
	 String name = IvyXml.getAttrString(e, "NAME");
	 if (name == null) name = IvyXml.getAttrString(e, "CLASS");
	 if (counted.contains(name) || name == null) continue;
	 System.err.println("DYPATCH: Counting class: " + name);
	 doCounting(name, counter);
	 counted.add(name);
      }
   }
   xw.end("COUNTERDATA");
   id_table.putAll(counter.getIds());

   dumpCounters(counter, xw);
   xw.end("COUNTERS");
   return xw.toString();
}

private void doCounting(String name,DypatchCounter counter)
{
   if (name == null || name.equals("*")) return;
   try {
      visitClass(name, counter);
   }
   catch (IOException ioe) {
      ioe.printStackTrace();
      System.err.println("DYPATCH: An error occured while patching " + name);
   }
}

static void dumpCounters(DypatchCounter counter,IvyXmlWriter xw)
{
   Map<String, Integer> counters = counter.getIds();
   if (counters == null || counters.size() == 0) return;

   xw.begin("COUNTERS");
   for (Map.Entry<String, Integer> e : counters.entrySet()) {
      String key = e.getKey();
      Integer val = e.getValue();
      int keyLen = numOfChar(key, '%');
      String type = null;
      switch (keyLen) {
	 case 0:
	    type = "CLASS";
	    break;
	 case 1:
	    type = "METHOD";
	    break;
	 case 2:
	    type = "BLOCK";
	    break;
	 default:
	    System.err.println("DYPATCH: Error processing counter: " + key);
	    continue;
      }
      xw.begin("USE");
      xw.field("ID", val.intValue());
      xw.field("TYPE", type);
      xw.end("USE");
   }
   xw.end("COUNTERS");
}

/********************************************************************************/
/*										*/
/* Interface for handling model add and remove */
/*										*/
/********************************************************************************/

private synchronized String handleActivateCommand(Element xml)
{
   if (xml == null) return null;

   Collection<String> addcls = new HashSet<String>();
   Collection<String> delcls = new HashSet<String>();

   for (Element e : IvyXml.children(xml, "PATCHMODEL")) {
      String nm = IvyXml.getAttrString(e, "NAME");
      Element model = patch_models.get(nm);
      if (model == null) {
	 System.err.println("DYPATCH: Model not found");
	 continue;
      }
      ActionType atyp = IvyXml.getAttrEnum(e, "ACTION", ActionType.NONE);
      switch (atyp) {
	 case NONE:
	    System.err.println("ActionType == NONE");
	    break;
	 case ADD:
	    if (active_models.add(model)) addActiveClasses(model, addcls);
	    break;
	 case REMOVE:
	    if (active_models.remove(model)) addActiveClasses(model, delcls);
	    break;
      }
   }

   Collection<String> clsset = new HashSet<String>(active_classes);
   clsset.addAll(addcls);
   clsset.addAll(delcls);

   active_classes.removeAll(delcls);
   active_classes.addAll(addcls);

   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("PATCH");

   patchActiveClasses(clsset, xw);

   xw.end("PATCH");

   return xw.toString();
}

private void patchActiveClasses(Collection<String> clsset,IvyXmlWriter xw)
{
   Map<String, Collection<Element>> classMap = patchesByClass();

   for (String name : clsset) {
      Collection<Element> patches = classMap.get(name);
      Map<String, File> files = class_files.get(name);
      if (files == null) {
	 files = new HashMap<String, File>();
	 class_files.put(name, files);
      }
      ClassWriter cw = new DypatchClassWriter(ClassWriter.COMPUTE_MAXS
	       | ClassWriter.COMPUTE_FRAMES,class_loader);

      File orig = files.get("%ORIGINAL%");
      if (orig == null) {
	 doPatching(name, cw);
	 if (name_flag) {
	    orig = newNamedOutputFile(cw.toByteArray(),
		     "/tmp/dyper/orig/" + name.replace('.', '/'));
	 }
	 else {
	    orig = newOutputFile(cw.toByteArray());
	 }
	 files.put("%ORIGINAL%", orig);
	 cw = new DypatchClassWriter(ClassWriter.COMPUTE_MAXS
		  | ClassWriter.COMPUTE_FRAMES,class_loader);
      }
      File patch;
      if (!(patches == null) && !patches.isEmpty()) {
	 String patchString = computePatchString(patches);
	 patch = files.get(patchString);
	 if (patch == null) {
	    ClassVisitor cv = new DypatchClassVisitor(ASM_VERSION,cw,patches,mode_map,
		     id_table);
	    doPatching(name, cv);
	    if (name_flag) {
	       patch = newNamedOutputFile(cw.toByteArray(),
			"/tmp/dyper/patched/" + name.replace('.', '/'));
	       files.put(patchString, patch);
	    }
	    else {
	       patch = newOutputFile(cw.toByteArray());
	       files.put(patchString, patch);
	    }
	 }
      }
      else {
	 patch = orig;
      }

      xw.begin("CLASS");
      xw.field("NAME", name);
      xw.field("FILE", patch);
      xw.field("ORIGINAL", patch == orig ? "TRUE" : "FALSE");
      xw.end("CLASS");
   }
}

private Map<String, Collection<Element>> patchesByClass()
{
   Map<String, Collection<Element>> classMap = new HashMap<String, Collection<Element>>();
   for (Element e : active_models) {
      Collection<Element> patches = new HashSet<Element>();
      for (Element f : IvyXml.children(e, "FOR")) {
	 patches.add(f);
      }
      for (Element c : IvyXml.children(e, "CLASS")) {
	 String name = IvyXml.getAttrString(c, "NAME");
	 Collection<Element> classPatches = classMap.get(name);
	 if (classPatches == null) {
	    classPatches = new HashSet<Element>();
	    classMap.put(name, classPatches);
	 }
	 classPatches.addAll(patches);
      }
      for (Element c : IvyXml.children(e, "FOR")) {
	 String name = IvyXml.getAttrString(c, "CLASS");
	 Collection<Element> classPatches = classMap.get(name);
	 if (classPatches == null) {
	    classPatches = new HashSet<Element>();
	    classMap.put(name, classPatches);
	 }
	 classPatches.addAll(patches);
      }
   }
   return classMap;
}

private void doPatching(String name,ClassVisitor cv)
{
   try {
      visitClass(name, cv);
   }
   catch (IOException e) {
      System.err.println("DYPATCH: An error occurred while patching " + name);
      e.printStackTrace();
   }
}

private String computePatchString(Collection<Element> c)
{
   StringBuilder sb = new StringBuilder();

   for (Element e : c) {
      sb.append(elementToString(e));
   }

   return sb.toString();
}

private static String elementToString(Element e)
{
   try {
      return convertNodeToHtml(e);
   }
   catch (TransformerException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
   }
   return "";
}

private void addActiveClasses(Element xml,Collection<String> c)
{
   for (Element e : IvyXml.children(xml, "CLASS")) {
      c.add(IvyXml.getAttrString(e, "NAME"));
   }
   for (Element e : IvyXml.children(xml, "FOR")) {
      c.add(IvyXml.getAttrString(e, "CLASS"));
   }
}

public static String convertNodeToHtml(Node node) throws TransformerException
{
   Transformer t = TransformerFactory.newInstance().newTransformer();
   t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
   StringWriter sw = new StringWriter();
   t.transform(new DOMSource(node), new StreamResult(sw));
   return sw.toString();
}

/********************************************************************************/
/*										*/
/* Cinder interface */
/*										*/
/********************************************************************************/

public boolean isProjectClass(String cls,Object file,boolean fg)
{
   ClassType ct = class_map.get(cls);
   if (ct == null) {
      int len = 0;
      ct = ClassType.NORMAL;
      for (Map.Entry<String, ClassType> ent : package_map.entrySet()) {
	 String pn = ent.getKey();
	 if (pn.length() > len && cls.startsWith(pn)) {
	    ct = ent.getValue();
	    len = pn.length();
	 }
      }
      class_map.put(cls, ct);
   }

   // return ct == ClassType.NORMAL;

   return true; // allow anything to be patched
}

/********************************************************************************/
/*										*/
/* Message handler for commands from the DYPATCH */
/*										*/
/********************************************************************************/

private class PatchCommand implements MintHandler {

PatchCommand()
{}

public void receive(MintMessage msg,MintArguments args)
{
   String cmd = args.getArgument(0);
   String rslt = null;

   if (debug_flag > 0) System.err.println("DYPATCH: Process command " + cmd);

   last_command = System.currentTimeMillis();

   try {
      if (cmd == null) return;
      else if (cmd.equals("PING")) {
	 rslt = "PONG";
      }
      else if (cmd.equals("MODEL")) {
	 rslt = handleModelCommand(args.getXmlArgument(1));
      }
      else if (cmd.equals("ACTIVATE")) {
	 rslt = handleActivateCommand(args.getXmlArgument(1));
      }
      else if (cmd.equals("DIRECTORY")) {
	 patch_directory = new File(args.getArgument(1));
      }
      else if (cmd.equals("ADDCLASS")) {
	 handleAddClass(args.getXmlArgument(1));
      }
      else if (cmd.equals("EXIT")) {
	 System.exit(0);
      }
   }
   catch (Throwable t) {
      System.err.println("DYPATCH: Problem processing request: " + t);
      t.printStackTrace();
   }

   if (debug_flag > 0) System.err.println("DYPATCH: Send reply: " + rslt);
   sendReply(msg, rslt);
}

} // end of subclass MasterCommand

/********************************************************************************/
/*										*/
/* Exit handling methods */
/*										*/
/********************************************************************************/

private void setupExit()
{
   dypatch_timer.schedule(new CheckExit(), EXIT_DELAY);
}

private void checkExit()
{
   if (test_flag) return;
   MintDefaultReply mdr = new MintDefaultReply();
   sendDyperMessage("PING", null, mdr, MINT_MSG_FIRST_REPLY);
   String msg = mdr.waitForString(15000);
   if (msg != null) last_command = System.currentTimeMillis();

   long now = System.currentTimeMillis();
   if (now - last_command < EXIT_DELAY) {
      dypatch_timer.schedule(new CheckExit(), EXIT_DELAY);
   }
   else {
      if (debug_flag > 0) System.err.println("DYPATCH: Exiting due to timeout");
      System.exit(0);
   }
}

private class CheckExit extends TimerTask {

public void run()
{
   checkExit();
}

} // end of subclass CheckExit



/********************************************************************************/
/*										*/
/* Testing methods */
/*										*/
/********************************************************************************/

private void test0()
{
   // CinderManager.do_debug = true;

   String m1 = "<PATCHMODEL NAME='SOCKETAGENT'>"
	    + "<FOR CLASS='java.net.SocketInputStream' METHOD='close'>"
	    + "<PATCH WHAT='ENTER' MODE='SOCKETAGENT_CLOSE' />" + "</FOR>"
	    + "<FOR CLASS='java.net.SocketInputStream' METHOD='read([BII)I'>"
	    + "<PATCH WHAT='ENTER' MODE='SOCKETAGENT_READ' />" + "</FOR>"
	    + "<FOR CLASS='java.net.SocketOutputStream' METHOD='socketWrite'>"
	    + "<PATCH WHAT='ENTER' MODE='SOCKETAGENT_WRITE' />" + "</FOR>"
	    + "<FOR CLASS='java.net.SocketOutputStream' METHOD='close'>"
	    + "<PATCH WHAT='ENTER' MODE='SOCKETAGENT_CLOSE' />" + "</FOR>"
	    + "<CLASS NAME='java.net.SocketInputStream' />"
	    + "<CLASS NAME='java.net.SocketOutputStream' />"
	    + "</PATCHMODEL>";

   Element e = IvyXml.convertStringToXml(m1);
   System.err.println("COMMAND = " + IvyXml.convertXmlToString(e));
   String rslt = handleModelCommand(e);
   System.err.println("RESULT = " + rslt);

   String m2 = "<ACTIVATE><PATCHMODEL NAME='SOCKETAGENT' ACTION='ADD' /></ACTIVATE>";
   e = IvyXml.convertStringToXml(m2);
   System.err.println("COMMAND = " + IvyXml.convertXmlToString(e));
   rslt = handleActivateCommand(e);
   System.err.println("RESULT = " + rslt);
}

private void test1()
{
   Element e = IvyXml.loadXmlFromFile("/pro/dyvise/dynamo/src/test.out");
   Element mdl = IvyXml.getChild(e, "PATCHMODEL");

   String rslt = handleModelCommand(mdl);
   System.err.println("RESULT = " + rslt);

   String m2 = "<ACTIVATE><PATCHMODEL NAME='EVENTSTATE' ACTION='ADD' /></ACTIVATE>";
   e = IvyXml.convertStringToXml(m2);
   System.err.println("COMMAND = " + IvyXml.convertXmlToString(e));
   rslt = handleActivateCommand(e);
   System.err.println("RESULT = " + rslt);
}

private void test2()
{
   Element e = IvyXml
	    .loadXmlFromFile("dyvise/edu/brown/cs/dyvise/dypatchasm/test2_model.xml");
   String rslt = handleModelCommand(e);
   System.err.println("RESULT = " + rslt);

   String m2 = "<ACTIVATE><PATCHMODEL ACTION='ADD' NAME='CPUAGENT' /></ACTIVATE>";
   e = IvyXml.convertStringToXml(m2);
   System.err.println("COMMAND = " + IvyXml.convertXmlToString(e));
   rslt = handleActivateCommand(e);
   System.err.println("RESULT = " + rslt);
}

private void testSetupClasses(int testNum)
{
   Element xml = null;
   switch (testNum) {
      case 0:
	 xml = IvyXml.convertStringToXml(TEST0_CLASSPATH);
	 break;
      case 1:
	 xml = IvyXml.convertStringToXml(TEST1_CLASSPATH);
	 break;
      case 2:
	 xml = IvyXml.loadXmlFromFile(TEST2_CLASSPATH_FILE);
	 break;
   }
   setupClasses(xml);
}



} // end of class DypatchMain



/* end of DypatchMain.java */
