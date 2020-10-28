/********************************************************************************/
/*										*/
/*		DylateMain.java 						*/
/*										*/
/*	Controller for DYnamic Lock UTilization Experiencer			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylate/DylateMain.java,v 1.3 2013-06-03 13:02:55 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylateMain.java,v $
 * Revision 1.3  2013-06-03 13:02:55  spr
 * Minor bug fixes
 *
 * Revision 1.2  2013-05-09 12:28:58  spr
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


import javax.xml.stream.*;
import javax.xml.stream.events.*;
import javax.xml.namespace.*;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.util.*;
import java.net.*;


public class DylateMain implements DylateConstants {




/********************************************************************************/
/*										*/
/*	Agent entry points							*/
/*										*/
/********************************************************************************/

public static void premain(String args,Instrumentation inst)
{
   the_control = new DylateMain(args,inst);
}



public static void agentmain(String args,Instrumentation inst)
{
   if (the_control == null) the_control = new DylateMain(args,inst);
   else the_control.restart();
}



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private Instrumentation 	class_inst;
private DylateInstrumenter	our_instrumenter;
private Writer			info_writer;
private List<Location>		lock_locations;

private static DylateMain	the_control = null;



/********************************************************************************/
/*										*/
/*	Class to maintain state information while reading locks 		*/
/*										*/
/********************************************************************************/

private static final QName class_attr = new QName("CLASS");
private static final QName method_attr = new QName("METHOD");
private static final QName signature_attr = new QName("SIGNATURE");
private static final QName offset_attr = new QName("OFFSET");
private static final QName id_attr = new QName("ID");





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private DylateMain(String args,Instrumentation inst)
{
   class_inst = inst;
   info_writer = null;
   lock_locations = null;

   scanArgs(args);

   if (class_inst != null) {
      our_instrumenter = new DylateInstrumenter(info_writer,lock_locations);
      class_inst.addTransformer(our_instrumenter,true);
    }
}



private void restart()
{
   System.err.println("DYLATE: RESTART");
}



/********************************************************************************/
/*										*/
/*	Method to scan arguments						*/
/*										*/
/********************************************************************************/

private void scanArgs(String args)
{
   if (args == null) return;

   StringTokenizer tok = new StringTokenizer(args,":;,");
   while (tok.hasMoreTokens()) {
      String arg = tok.nextToken();
      String val = null;
      int idx = arg.indexOf('=');
      if (idx >= 0) {
	 val = arg.substring(idx+1);
	 arg = arg.substring(0,idx);
       }
      if (arg.equals("OUTPUT")) {
	 setOutput(val);
       }
      else if (arg.equals("FULL")) {
	 DylateMonitor.getMonitor().setFull(true);
       }
      else if (arg.equals("LOCK")) {
	 readLocks(val);
       }
      else {
	 System.err.println("DYLATE: Invalid argument: " + arg);
       }
    }
}




private void setOutput(String tgt)
{
   if (tgt == null) return;

   int idx = tgt.indexOf("@");
   Writer w = null;
   Writer sw = null;

   if (idx > 0) {
      String host = tgt.substring(idx+1);
      int idx1 = tgt.indexOf("+");
      int port1 = -1;
      int port2 = -1;
      try {
	 port1 = Integer.parseInt(tgt.substring(0,idx1));
	 port2 = Integer.parseInt(tgt.substring(idx1+1,idx));
       }
      catch (NumberFormatException e) {
	 System.err.println("DYLATE: Invalid port,port@host specifiation");
	 return;
       }
      try {
         @SuppressWarnings("resource")
	 Socket s1 = new Socket(host,port1);
	 w = new OutputStreamWriter(s1.getOutputStream());
         @SuppressWarnings("resource")
	 Socket s2 = new Socket(host,port2);
	 sw = new OutputStreamWriter(s2.getOutputStream());
       }
      catch (IOException e) {
	 System.err.println("DYLUTE: Unable to connect to socket: " + e);
	 System.exit(1);
       }
    }
   if (w == null) {
      try {
	 w = new FileWriter(tgt + ".csv");
	 sw = new FileWriter(tgt + ".info");
       }
      catch (IOException e) {
	 System.err.println("DYLATE: Can't open output file " + tgt + ": " + e);
       }
    }

   if (w == null || sw == null) System.exit(1);

   w = new BufferedWriter(w);
   info_writer = new BufferedWriter(sw);
   DylateMonitor.getMonitor().setOutputWriter(w,info_writer);

}




/********************************************************************************/
/*										*/
/*	Scan lock file								*/
/*										*/
/********************************************************************************/

private void readLocks(String fnm)
{
   LockReader lr = new LockReader();
   if (lock_locations ==  null) lock_locations = new ArrayList<Location>();

   try {
      XMLInputFactory xf = XMLInputFactory.newInstance();
      xf.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE,Boolean.FALSE);
      xf.setProperty(XMLInputFactory.IS_VALIDATING,Boolean.FALSE);
      xf.setProperty(XMLInputFactory.SUPPORT_DTD,Boolean.FALSE);

      XMLEventReader rdr = xf.createXMLEventReader(new FileInputStream(fnm));
      while (rdr.hasNext()) {
	 XMLEvent evt = rdr.nextEvent();

	 lr.processEvent(evt);
       }
      rdr.close();
    }
   catch (IOException e) {
      System.err.println("DYLUTE: Problem reading lock description file: " + e);
    }
   catch (XMLStreamException e) {
      System.err.println("DYLUTE: Problem with XML in lock description file: " + e);
    }
}



private class LockReader {

   private int	lock_id;

   LockReader() {
      lock_id = 0;
    }

   void processEvent(XMLEvent evt) {
      if (evt.isStartElement()) {
	 StartElement se = evt.asStartElement();
	 String enm = se.getName().getLocalPart();
	 if (enm.equals("DYLUTE")) ;
	 else if (enm.equals("LOCK")) {
	    lock_id = getIntAttr(se,id_attr,0);
	  }
	 else if (enm.equals("LOCATION")) {
	    String cnm = getStringAttr(se,class_attr);
	    String mnm = getStringAttr(se,method_attr);
	    String msg = getStringAttr(se,signature_attr);
	    int offset = getIntAttr(se,offset_attr,0);
	    int lid = getIntAttr(se,id_attr,0);
	    Location loc = new LocationImpl(cnm,mnm,msg,offset,lid,lock_id);
	    lock_locations.add(loc);
	  }
       }
    }

   private String getStringAttr(StartElement se,QName attr) {
      Attribute at = se.getAttributeByName(attr);
      if (at == null) return null;
      return at.getValue();
    }

   private int getIntAttr(StartElement se,QName attr,int dflt) {
      Attribute at = se.getAttributeByName(attr);
      if (at == null) return dflt;
      try {
	 return Integer.parseInt(at.getValue());
       }
      catch (NumberFormatException e) { }
      return dflt;
    }

}	// end of inner class LockReader



private static class LocationImpl implements Location {

   private String class_name;
   private String method_name;
   private String method_signature;
   private int method_offset;
   private int location_id;
   private int lock_id;

   LocationImpl(String cnm,String mnm,String msg,int off,int lid,int lock) {
      if (cnm.startsWith("L") && cnm.endsWith(";")) {
	 int ln = cnm.length();
	 cnm = cnm.substring(1,ln-1);
       }

      class_name = cnm;
      method_name = mnm;
      method_signature = msg;
      method_offset = off;
      location_id = lid;
      lock_id = lock;
    }

   @Override public String getClassName()		{ return class_name; }
   @Override public String getMethodName()		{ return method_name; }
   @Override public String getMethodSignature() 	{ return method_signature; }
   @Override public int getMethodOffset()		{ return method_offset; }
   @Override public int getLocationId() 		{ return location_id; }
   @Override public int getLockId()			{ return lock_id; }

   @Override public String toString() {
      return class_name + "." + method_name + method_signature + "@" + method_offset;
    }

}	// end of inner class LocationImpl




/********************************************************************************/
/*										*/
/*	Test program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   long [] times = new long[100];
   long start = System.nanoTime();
   for (int i = 0; i < 100; ++i) times[i] = System.nanoTime() - start;
   for (int i = 0; i < 100; ++i) System.err.println(times[i]);

   File f1;
   String cnm;
   f1 = new File("/research/dyvise/java/edu/brown/cs/dyvise/dylate/DylateTest.class");
   cnm = "edu/brown/cs/dyvise/dylate/DylateTest";
   f1 = new File("/research/ivy/java/edu/brown/cs/ivy/mint/client/MintClient.class");
   cnm = "edu/brown/cs/ivy/mint/client/MintClient";
   f1 = new File("/u/spr/temp/javax/media/opengl/glu/GLU.class");
   cnm = "javax/media/opengl/glu/GLU";

   int ln = (int) f1.length();
   byte [] ind = new byte[ln];
   try {
      FileInputStream fis = new FileInputStream(f1);
      int off = 0;
      while (ln > 0) {
	 int xln = fis.read(ind,off,ln);
	 ln -= xln;
	 off += xln;
       }
      fis.close();
    }
   catch (IOException e) {
      System.err.println("DYLATE TEST: Problem reading input file: " + e);
    }

   File f2 = new File("/research/dyvise/java/edu/brown/cs/dyvise/dylate/DylateTest.out");
   f2 = new File("/u/spr/temp/javax/media/opengl/glu/GLU.out");

   DylateInstrumenter dis = new DylateInstrumenter(null,null);
   byte [] rslt;
   rslt = dis.transform(null,cnm,null,null,ind);

   if (rslt == null) {
      System.err.println("DYLATE TEST: No transformation done");
      return;
    }
   try {
      FileOutputStream fos = new FileOutputStream(f2);
      fos.write(rslt,0,rslt.length);
      fos.close();
    }
   catch (IOException e) {
      System.err.println("DYLATE TEST: Problem writing output file: " + e);
    }
}




}	// end of class DylateMain



/* end of DylateMain.java */








