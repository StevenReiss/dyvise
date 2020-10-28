/********************************************************************************/
/*										*/
/*		DynamicURLClassLoader.java					*/
/*										*/
/*	    ClassLoader capable of easily adding classes at runtime		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dypatchasm/DynamicURLClassLoader.java,v 1.2 2013/09/04 18:36:33 spr Exp $ */

/*********************************************************************************
 *
 * $Log: DynamicURLClassLoader.java,v $
 * Revision 1.2  2013/09/04 18:36:33  spr
 * Minor bug fixes.
 *
 *
 * Revision 1.0 2013-08-19 16:26:07 zolstein
 * Original version
 *
 ********************************************************************************/


package edu.brown.cs.dyvise.dypatchasm;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

import java.net.URLClassLoader;

class DynamicURLClassLoader extends URLClassLoader {

/********************************************************************************/
/*										*/
/* Constructors */
/*										*/
/********************************************************************************/


public DynamicURLClassLoader(URLClassLoader classLoader)
{
   super(classLoader.getURLs());
}



public DynamicURLClassLoader(ClassLoader par)
{
   super(new URL[0],par);
}



public DynamicURLClassLoader()
{
   super(new URL [0]);
}




/********************************************************************************/
/*										*/
/* Methods To Add Classes */
/*										*/
/********************************************************************************/


public void addURLs(URL[] urls)
{
   for (URL url : urls) {
      addURL(url);
   }
}

public void addURLs(Collection<URL> urls)
{
   addURLs(urls.toArray(new URL[0]));
}

public void addFiles(String[] paths)
{
   for (String s : paths) {
      File f = new File(s);
      if (f.exists()) {
	 try {
	    URL url = f.toURI().toURL();
	    addURL(url);
	 }
	 catch (MalformedURLException e) {
	    System.err.println("Bad path: " + s);
	    e.printStackTrace();
	 }
      }
   }
}

public void addClassPath(String classpath)
{
   if (classpath == null) return;
   
   addFiles(classpath.split(File.pathSeparator));
}


}	// end of class DynamicURLClassLoader




/* end of DynamicURLClassLoader.java */


























































































































