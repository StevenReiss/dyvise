/********************************************************************************/
/*										*/
/*		DymonSetup.java 						*/
/*										*/
/*	Class for setting up a remote DYVISE distribution			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymon/DymonSetup.java,v 1.4 2011-03-10 02:26:37 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymonSetup.java,v $
 * Revision 1.4  2011-03-10 02:26:37  spr
 * Code cleanup.
 *
 * Revision 1.3  2010-04-29 18:47:40  spr
 * Clean up setup code
 *
 * Revision 1.2  2009-09-19 00:09:46  spr
 * Update dymon for seletive agent insertion and removal.
 *
 * Revision 1.1  2009-06-04 18:53:51  spr
 * Set up for binary distribution.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymon;


import edu.brown.cs.ivy.exec.IvySetup;


import java.io.*;
import java.util.Date;
import java.util.Properties;



public class DymonSetup implements DymonConstants {



/********************************************************************************/
/*										*/
/*	Local Storage								*/
/*										*/
/********************************************************************************/

private static String [] dyvise_props = new String [] {
   "IVY","DYVISE"
};




/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   IvySetup.main(args);

   String dir = System.getProperty("user.dir");

   if (args.length > 0) {
      File dirf = new File(args[0]);
      if (dirf.isDirectory()) dir = dirf.getAbsolutePath();
    }

   File jarf = new File(dir,"dyvisefull.jar");
   if (!jarf.exists()) {
      System.err.println("DYMONSETUP: Please run in the installation directory");
      System.exit(1);
    }

   File dyv = new File(System.getProperty("user.home"),".dyvise");
   if (!dyv.exists()) {
      if (!dyv.mkdir()) {
	 System.err.println("DYMONSETUP: Can't create dyvise directory " + dyv);
	 System.exit(1);
       }
    }
   File pf = new File(dyv,"Props");

   Properties p = new Properties();

   if (pf.exists()) {
      try {
	 FileInputStream fis = new FileInputStream(pf);
	 p.loadFromXML(fis);
	 fis.close();
       }
      catch (IOException e) {
	 System.err.println("DYMONSETUP: Problem loading old properties: " + e);
       }
    }

   p.setProperty("BROWN_DYVISE_IVY",dir);
   p.setProperty("edu.brown.cs.dyvise.IVY",dir);
   p.setProperty("BROWN_DYVISE_DYVISE",dir);
   p.setProperty("edu.brown.cs.dyvise.DYVISE",dir);
   p.setProperty("BROWN_DYVISE_DYVISEPATH",jarf.getAbsolutePath());
   p.setProperty("edu.brown.cs.dyvise.DYVISEPATH",jarf.getAbsolutePath());

   try {
      FileOutputStream os = new FileOutputStream(pf);
      p.storeToXML(os,"SETUP on " + new Date().toString());
      os.close();
    }
   catch (IOException e) {
      System.err.println("DYMONSETUP: Problem writing property file: " + e);
      System.exit(1);
    }

   System.err.println("DYMONSETUP: Setup complete");
}


/********************************************************************************/
/*										*/
/*	Callable setup method							*/
/*										*/
/********************************************************************************/

public static void doSetup()
{
   if (checkSetup()) return;

   String dir = System.getProperty("user.dir");

   File jarf = new File(dir,"dyvisefull.jar");
   if (!jarf.exists()) {
      System.err.println("DYMONSETUP: Please run in the installation directory");
      System.exit(1);
    }

   File dyv = new File(System.getProperty("user.home"),".dyvise");
   if (!dyv.exists()) {
      if (!dyv.mkdir()) {
	 System.err.println("DYMONSETUP: Can't create dyvise directory " + dyv);
	 System.exit(1);
       }
    }
   File pf = new File(dyv,"Props");

   Properties p = new Properties();

   if (pf.exists()) {
      try {
	 FileInputStream fis = new FileInputStream(pf);
	 p.loadFromXML(fis);
	 fis.close();
       }
      catch (IOException e) {
	 System.err.println("DYMONSETUP: Problem loading old properties: " + e);
       }
    }

   p.setProperty("BROWN_DYVISE_IVY",dir);
   p.setProperty("edu.brown.cs.dyvise.IVY",dir);
   p.setProperty("BROWN_DYVISE_DYVISE",dir);
   p.setProperty("edu.brown.cs.dyvise.DYVISE",dir);
   p.setProperty("BROWN_DYVISE_DYVISEPATH",jarf.getAbsolutePath());
   p.setProperty("edu.brown.cs.dyvise.DYVISEPATH",jarf.getAbsolutePath());

   try {
      FileOutputStream os = new FileOutputStream(pf);
      p.storeToXML(os,"SETUP on " + new Date().toString());
      os.close();
    }
   catch (IOException e) {
      System.err.println("DYMONSETUP: Problem writing property file: " + e);
      System.exit(1);
    }

   System.err.println("DYMONSETUP: Setup complete");
}



/********************************************************************************/
/*										*/
/*	Check if we are already setup						*/
/*										*/
/********************************************************************************/

private static boolean checkSetup()
{
   boolean ok = true;
   for (int i = 0; i < dyvise_props.length; ++i) {
      String s = "BROWN_DYVISE_" + dyvise_props[i];
      String s1 = "edu.brown.cs.dyvise." + dyvise_props[i];
      if (System.getProperty(s) == null && System.getenv(s) == null &&
	     System.getProperty(s1) == null)
	 ok = false;
    }
   if (ok) return true;

   File dyv = new File(System.getProperty("user.home"),".dyvise");
   if (!dyv.exists()) return false;
   File pf = new File(dyv,"Props");
   if (!pf.exists()) return false;
   Properties p = new Properties();
   try {
      FileInputStream fis = new FileInputStream(pf);
      p.loadFromXML(fis);
      fis.close();
    }
   catch (IOException e) {
      return false;
    }

   ok = true;
   for (int i = 0; i < dyvise_props.length; ++i) {
      String s = "BROWN_DYVISE_" + dyvise_props[i];
      if (p.getProperty(s) == null) ok = false;
    }

   return ok;
}




}	// end of class DymonSetup



/* end of DymonSetup.java */
