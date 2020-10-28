/********************************************************************************/
/*										*/
/*		DyperInstrumenter.java						*/
/*										*/
/*	Class file transformer for DYPER					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyper/DyperInstrumenter.java,v 1.6 2012-10-05 00:53:01 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyperInstrumenter.java,v $
 * Revision 1.6  2012-10-05 00:53:01  spr
 * Code clean up.
 *
 * Revision 1.5  2011-03-10 02:27:40  spr
 * Code fixups.
 *
 * Revision 1.4  2010-03-30 16:19:22  spr
 * Remove debugging. Add xjar to Make.data.
 *
 * Revision 1.3  2009-03-20 02:08:21  spr
 * Code cleanup; output information for incremental time-based display.
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


import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.*;



class DyperInstrumenter implements DyperConstants, ClassFileTransformer {




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private Map<Class<?>,ClassData> class_data;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DyperInstrumenter()
{
   class_data = new HashMap<Class<?>,ClassData>();
}




/********************************************************************************/
/*										*/
/*	Transformation method							*/
/*										*/
/********************************************************************************/

public byte [] transform(ClassLoader ldr,String name,Class<?> cls,
			    ProtectionDomain dom,byte [] buf)
{
   if (cls == null) return null;

   if (name.startsWith("edu/brown/cs/dyvise/dyper/")) return null;

   ClassData cd = class_data.get(cls);
   if (cd == null) {
      // handle informing dypatch of new classes here
      cd = new ClassData(cls);
      class_data.put(cls,cd);
      cd.setData(buf);
    }
   else if (!cd.hasData()) cd.setData(buf);	// first time should be original data

   // System.err.println("DYPER: TRANSFORM " + name + " " + cd.getPatchData().length + " " + buf.length + " " + cd.getPatchFile());

   return cd.getPatchData();
}



/********************************************************************************/
/*										*/
/*	Class to setup instrumentation request					*/
/*										*/
/********************************************************************************/

List<Class<?>> setupTransform(Map<Class<?>,File> entries,Set<Class<?>> chng)
{
   List<Class<?>> rslt = new ArrayList<Class<?>>();

   for (Map.Entry<Class<?>,File> ent : entries.entrySet()) {
      try {
	 Class<?> cls = ent.getKey();
	 ClassData cd = class_data.get(cls);
	 if (cd == null) {
	    cd = new ClassData(cls);
	    class_data.put(cls,cd);
	  }
	 rslt.add(cls);
	 if (!chng.contains(cls)) {
	    cd.setPatchData(null,null);
	  }
	 else {
	    byte [] buf = readFile(ent.getValue());
	    // System.err.println("DYPER: SETUP " + ent.getValue() + " " + buf.length);
	    cd.setPatchData(buf,ent.getValue());
	  }
       }
      catch (Throwable t) {
	 System.err.println("DYPER: Problem loading instrumentation: " + t);
	 t.printStackTrace();
       }
    }

   return rslt;
}



private byte [] readFile(File f) throws IOException
{
   FileInputStream fis = new FileInputStream(f);
   int ln = (int) f.length();
   byte [] buf = new byte[ln];
   int ct = 0;
   while (ct < ln) {
      int i = fis.read(buf,ct,ln-ct);
      if (i < 0) {
	 fis.close();
	 throw new IOException("Unexpected end of file");
      }
      ct += i;
    }
   fis.close();

   return buf;
}




/********************************************************************************/
/*										*/
/*	Information about a class						*/
/*										*/
/********************************************************************************/

private static class ClassData {

   private byte [] orig_data;
   private byte [] patch_data;

   ClassData(Class<?> c) {
      orig_data = null;
      patch_data = null;
    }

   boolean hasData()				{ return orig_data != null; }
   void setData(byte [] data) {
      orig_data = new byte[data.length];
      System.arraycopy(data,0,orig_data,0,data.length);
    }

   void setPatchData(byte [] data,File f) {
      patch_data = data;
    }

   byte [] getPatchData() {
      if (patch_data != null) {
	 return patch_data;
       }
      return orig_data;
    }

}	// end of subclass ClassData



}	// end of class DyperInstrumenter




/* end of DyperInstrumenter.java */

