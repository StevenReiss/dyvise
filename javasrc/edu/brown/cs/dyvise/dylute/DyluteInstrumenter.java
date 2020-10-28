/********************************************************************************/
/*										*/
/*		DyluteInstrumenter.java 					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/dylute/src/DyluteInstrumenter.java,v 1.1 2011-09-12 19:37:25 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyluteInstrumenter.java,v $
 * Revision 1.1  2011-09-12 19:37:25  spr
 * Add dylute files to repository
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dylute;

// NOTE: This should only use system libraries and libraries that are explicitly
//    in this package.	Using anything else might cause conflicts if the application
//    being monitored uses the same library.

import edu.brown.cs.dyvise.dylute.org.objectweb.asm.*;


import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.*;
import java.io.*;



class DyluteInstrumenter implements DyluteConstants, ClassFileTransformer {




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private Map<String,List<Location>>	location_map;

private static boolean do_debug = true;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DyluteInstrumenter(Collection<Location> locks)
{
   location_map = new HashMap<String,List<Location>>();

   for (Location l : locks) {
      String cnm = l.getClassName();
      List<Location> ll = location_map.get(cnm);
      if (ll == null) {
	 ll = new ArrayList<Location>();
	 location_map.put(cnm,ll);
       }
      ll.add(l);
    }
}




/********************************************************************************/
/*										*/
/*	Transformation method							*/
/*										*/
/********************************************************************************/

public byte [] transform(ClassLoader ldr,String name,Class<?> cls,
			    ProtectionDomain dom,byte [] buf)
{
   if (isDyluteClass(name) || isSensitiveClass(name)) return null;
   if (!location_map.containsKey(name)) return null;

   System.err.println("DYLUTE: Instrument: " + name);

   return instrument(cls,name,buf);
}



private byte [] instrument(Class<?> cls,String name,byte [] buf)
{
   byte [] rsltcode;
   try {
      ClassWriter writer = createClassWriter(buf);
      ClassReader reader = new ClassReader(buf);
      ClassVisitor ins = new DyluteTransformer(cls,name,location_map.get(name),writer);
      reader.accept(ins,ClassReader.SKIP_FRAMES);
      rsltcode = writer.toByteArray();
    }
   catch (Throwable t) {
      System.err.println("DYLUTE: Problem doing instrumentation: " + t);
      t.printStackTrace();
      return null;
    }

   if (do_debug) {
      try {
	 String fnm = "/ws/volfred/dylute";
	 File f1 = new File(fnm);
	 File f2 = new File(f1,cls.getName());
	 FileOutputStream fos = new FileOutputStream(f2);
	 fos.write(rsltcode);
	 fos.close();
       }
      catch (IOException e) {
	 System.err.println("DYLUTE: Problem doing instrumentation: " + e);
       }
    }

   return rsltcode;
}



private ClassWriter createClassWriter(byte [] buf)
{
   int fgs = ClassWriter.COMPUTE_MAXS;
   // if (isJDK6(buf)) fgs |= ClassWriter.COMPUTE_FRAMES;
   return new ClassWriter(fgs);
}



/**************
private boolean isJDK6(byte [] code)
{
   int majoroffset = 4 + 2;			// 0xCAFEBABE + minor version
   int major = (((code[majoroffset] << 8) & 0xff00) | ((code[majoroffset+1]) & 0xff));
   return major >= 50;
}
****************/




/********************************************************************************/
/*										*/
/*	Class selection methods 						*/
/*										*/
/********************************************************************************/

private boolean isDyluteClass(String name)
{
   if (name.contains("DyluteTest")) return false;

   return name.startsWith("edu/brown/cs/dyvise/dylute/");
}



private boolean isSensitiveClass(String name)
{
   return name.equals("java/lang/Object") ||
      name.startsWith("java/lang/ThreadLocal") ||
      name.startsWith("sun/reflect") ||
      name.equals("sun/misc/Unsafe")  ||
      name.startsWith("sun/security/") ||
      name.equals("java/lang/VerifyError");
}




}	// end of class DyluteInstrumenter




/* end of DyluteInstrumenter.java */





