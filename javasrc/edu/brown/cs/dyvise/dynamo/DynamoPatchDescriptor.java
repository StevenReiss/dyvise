/********************************************************************************/
/*										*/
/*		DynamoPatchDescriptor.java					*/
/*										*/
/*	DYVISE dyanmic analysis patch descriptor				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dynamo/DynamoPatchDescriptor.java,v 1.2 2009-10-07 01:00:16 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DynamoPatchDescriptor.java,v $
 * Revision 1.2  2009-10-07 01:00:16  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.1  2009-09-19 00:10:00  spr
 * Module to generate model for visualization.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dynamo;


import edu.brown.cs.dyvise.dyvise.DyviseConstants;
import edu.brown.cs.ivy.project.IvyProject;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.Map;



class DynamoPatchDescriptor implements DynamoConstants, DyviseConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private IvyProject	for_project;
private String		patch_method;
private Map<String,String> patch_params;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DynamoPatchDescriptor(IvyProject dp,String f)
{
   this(dp,IvyXml.loadXmlFromFile(f));
}



DynamoPatchDescriptor(IvyProject dp,Element e)
{
   for_project = dp;

   if (e == null) {
      System.err.println("Empty XML for patch description");
      System.exit(1);
    }

   patch_method = IvyXml.getTextElement(e,"METHOD");
   if (patch_method == null) {
      System.err.println("No METHOD given for patching");
      System.exit(1);
    }

   patch_params = new HashMap<String,String>();
   for (Element set : IvyXml.children(e,"SET")) {
      patch_params.put(IvyXml.getTextElement(set,"NAME"),
			  IvyXml.getTextElement(set,"VALUE"));
    }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

IvyProject getProject() 			{ return for_project; }

String getMethod()				{ return patch_method; }

String getValue(String p)			{ return patch_params.get(p); }



}	// end of class DynamoPatchDescriptor




/* end of DynamoPatchDescriptor.java */
