/********************************************************************************/
/*										*/
/*		DylateInstrumenter.java 					*/
/*										*/
/*	Code Instrumentation for DYnamic Lock UTilization Experiencer		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylate/DylateInstrumenter.java,v 1.3 2013-06-03 13:02:55 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylateInstrumenter.java,v $
 * Revision 1.3  2013-06-03 13:02:55  spr
 * Minor bug fixes
 *
 * Revision 1.2  2013-05-09 12:28:57  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.1  2012-10-05 00:52:26  spr
 * New lock tracer
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dylate;

// NOTE: This should only use system libraries and libraries that are explicitly
//    in this package.	Using anything else might cause conflicts if the application
//    being monitored uses the same library.

import edu.brown.cs.dyvise.dylute.org.objectweb.asm.*;


import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.*;
import java.io.*;




class DylateInstrumenter implements DylateConstants, ClassFileTransformer {




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private Writer info_writer;
private Map<String,List<Location>>	location_map;

private static Set<String> sensitive_classes;
private static List<String> sensitive_prefix;

private static boolean do_debug = true;


static {
   sensitive_classes = new HashSet<String>();
   sensitive_classes.add("java/lang/Object");
   sensitive_classes.add("java/lang/ThreadLocal");
   sensitive_classes.add("sun/misc/Unsafe");
   sensitive_classes.add("java/lang/VerifyError");
   sensitive_classes.add("java/awt/eventQueue");
   sensitive_classes.add("java/security/Permissions");
   sensitive_classes.add("java/io/FilePermissionCollection");
   sensitive_classes.add("java/security/BasicPermissionCollection");
   sensitive_classes.add("java/util/logging/Logger");
   sensitive_classes.add("java/util/concurrent/Executors");
   sensitive_classes.add("java/util/concurrent/Executors$DefaultThreadFactory");
   sensitive_classes.add("java/util/concurrent/Executors$Worker");
   sensitive_classes.add("sun/awt/AWTAutoShutdown");
   sensitive_classes.add("sun/awt/SunToolkit");
   sensitive_classes.add("java/awt/Toolkit");

   sensitive_prefix = new ArrayList<String>();
   sensitive_prefix.add("sun/reflect/");
   sensitive_prefix.add("sun/security/");
   sensitive_prefix.add("sun/font/");
   sensitive_prefix.add("java/util/concurrent/locks/");
   sensitive_prefix.add("java/util/concurrent/Executors");
   sensitive_prefix.add("java/util/concurrent/ThreadPoolExecutor");
   sensitive_prefix.add("edu/brown/cs/dyvise/dyper/");
   sensitive_prefix.add("jogamp/");
   sensitive_prefix.add("java/awt/");
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DylateInstrumenter(Writer infow,Collection<Location> locations)
{
   if (locations == null) location_map = null;
   else {
      location_map = new HashMap<String,List<Location>>();
      for (Location l : locations) {
	 String cnm = l.getClassName();
	 List<Location> ll = location_map.get(cnm);
	 if (ll == null) {
	    ll = new ArrayList<Location>();
	    location_map.put(cnm,ll);
	  }
	 ll.add(l);
       }
    }

   info_writer = infow;
}




/********************************************************************************/
/*										*/
/*	Transformation method							*/
/*										*/
/********************************************************************************/

public byte [] transform(ClassLoader ldr,String name,Class<?> cls,
			    ProtectionDomain dom,byte [] buf)
{
   if (isDylateClass(name) || isSensitiveClass(name)) return null;
   if (location_map != null && !location_map.containsKey(name)) return null;

   System.err.println("DYLATE: Instrument: " + name + " for " + ldr);

   return instrument(cls,name,buf);
}



private byte [] instrument(Class<?> cls,String name,byte [] buf)
{
   List<Location> locs = null;
   if (location_map != null) locs = location_map.get(name);

   byte [] rsltcode;
   try {
      ClassReader reader = new ClassReader(buf);
      ClassWriter writer = new SafeClassWriter(reader,ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);
      ClassVisitor ins = new DylateTransformer(cls,name,writer,locs,info_writer,isJDKold(buf));
      reader.accept(ins,ClassReader.SKIP_FRAMES);
      rsltcode = writer.toByteArray();
    }
   catch (Throwable t) {
      System.err.println("DYLATE: Problem doing instrumentation: " + t);
      t.printStackTrace();
      System.err.println("DYLATE: Working on " + name);
      return null;
    }

   if (do_debug) {
      try {
	 String fnm = "/ws/volfred/dylate";
	 File f1 = new File(fnm);
	 File f2 = new File(f1,name.replace("/","."));
	 System.err.println("DYLATE: SAVE IN " + f2);
	 FileOutputStream fos = new FileOutputStream(f2);
	 fos.write(rsltcode);
	 fos.close();
       }
      catch (Throwable e) {
	 System.err.println("DYLUTE: Problem doing instrumentation: " + e);
	 e.printStackTrace();
       }
    }

   return rsltcode;
}



private boolean isJDKold(byte [] code)
{
   int majoroffset = 4 + 2;			// 0xCAFEBABE + minor version
   int major = (((code[majoroffset] << 8) & 0xff00) | ((code[majoroffset+1]) & 0xff));
   return major < 49;
}



/********************************************************************************/
/*										*/
/*	Class selection methods 						*/
/*										*/
/********************************************************************************/

private boolean isDylateClass(String name)
{
   if (name.contains("DylateTest")) return false;

   if (name.startsWith("edu/brown/cs/dyvise/dylate/")) return true;
   if (name.startsWith("edu/brown/cs/dyvise/dylute/")) return true;

   return false;
}



private boolean isSensitiveClass(String name)
{
   if (sensitive_classes.contains(name)) return true;

   for (String s : sensitive_prefix) {
      if (name.startsWith(s)) return true;
    }

   return false;
}



/********************************************************************************/
/*										*/
/*	Safe class writer							*/
/*										*/
/********************************************************************************/

private class SafeClassWriter extends ClassWriter {

   SafeClassWriter(ClassReader cr,int fgs) {
      super(cr,fgs);
    }

   @Override protected String getCommonSuperClass(String typ1,String typ2) {
      try {
	 return super.getCommonSuperClass(typ1,typ2);
       }
      catch (Throwable t) { }

      if (typ1.equals("java/awt/Container") && typ2.equals("javax/swing/JComponent"))
	 return typ1;
      if (typ2.equals("java/awt/Container") && typ1.equals("javax/swing/JComponent"))
	 return typ2;
      if (typ1.equals("java/awtColor") && typ2.equals("sun/swing/PrintColorUIResource"))
	 return typ1;

      System.err.println("DYLATE: COMMON SUPER " + typ1 + " " + typ2);

      return "java/lang/Object";
    }

}	// end of inner class SafeClassWriter




}	// end of class DylateInstrumenter




/* end of DylateInstrumenter.java */


