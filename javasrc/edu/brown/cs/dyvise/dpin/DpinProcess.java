/********************************************************************************/
/*										*/
/*		DpinProcess.java						*/
/*										*/
/*	Representation of a process for DPIN					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dpin/DpinProcess.java,v 1.1.1.1 2008-10-22 13:16:47 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DpinProcess.java,v $
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dpin;


import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintDefaultReply;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.util.*;


public class DpinProcess implements DpinConstants, MintConstants {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		process_id;
private Map<String,String> process_props;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DpinProcess(String id,DpinMain dm)
{
   process_id = id;
   process_props = new HashMap<String,String>();

   loadProperties(dm);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getId()				{ return process_id; }



/********************************************************************************/
/*										*/
/*	Methods to get properties						*/
/*										*/
/********************************************************************************/

private void loadProperties(DpinMain dm)
{
   MintDefaultReply mdr = new MintDefaultReply();
   dm.sendMessage(process_id,"WHORU",mdr,MINT_MSG_FIRST_REPLY);
   Element e = mdr.waitForXml();

   String s = IvyXml.getTextElement(e,"START");
   if (s != null) process_props.put("START_CLASS",s);

   int act = 0;
   for (Element a : IvyXml.elementsByTag(e,"ARG")) {
      s = IvyXml.getText(a);
      process_props.put("ARG_" + (++act),s);
    }

   for (Element a : IvyXml.elementsByTag(e,"ENV")) {
      String k = IvyXml.getAttrString(a,"KEY");
      String v = IvyXml.getText(a);
      process_props.put(k,v);
    }

   for (Element a : IvyXml.elementsByTag(e,"PROPERTY")) {
      String k = IvyXml.getAttrString(a,"KEY");
      String v = IvyXml.getText(a);
      process_props.put(k,v);
    }
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void dump()
{
   System.err.println("PROCESS " + process_id);
   SortedMap<String,String> sm = new TreeMap<String,String>(process_props);
   for (Map.Entry<String,String> ent : sm.entrySet()) {
      System.err.println("\t" + ent.getKey() + ":\t" + ent.getValue());
    }
   System.err.println();
   System.err.println();
}




}	// end of class DpinProcess




/* end of DpinProcess.java */
