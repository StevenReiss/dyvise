/********************************************************************************/
/*										*/
/*		DycompAnalysis.java						*/
/*										*/
/*	DYVISE computed relations analysis representation			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dycomp/DycompAnalysis.java,v 1.2 2009-10-07 00:59:39 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DycompAnalysis.java,v $
 * Revision 1.2  2009-10-07 00:59:39  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.1  2009-09-19 00:08:21  spr
 * Code to compute relations and store them in the database.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dycomp;


import edu.brown.cs.dyvise.dyvise.DyviseDatabase;
import edu.brown.cs.dyvise.dyvise.DyviseException;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;


abstract class DycompAnalysis implements DycompConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

protected DycompMain	comp_main;
protected String	output_table;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected DycompAnalysis(DycompMain dc)
{
   comp_main = dc;
   output_table = null;
}



protected DycompAnalysis(DycompAnalysis proto,Element xml)
{
   comp_main = proto.comp_main;
   output_table = getTableName(xml,"RESULT");
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

abstract DycompAnalysis createNew(Element xml) throws DyviseException;




/********************************************************************************/
/*										*/
/*	Work methods								*/
/*										*/
/********************************************************************************/

abstract void compute() throws DyviseException, InterruptedException;



/********************************************************************************/
/*										*/
/*	Helper methods								*/
/*										*/
/********************************************************************************/

protected final String getTableName(Element rxml,String id)
{
   String rslt = null;

   Element xml = IvyXml.getChild(rxml,id);
   if (xml == null) rslt = IvyXml.getAttrString(rxml,id);
   else rslt = getTableName(xml);

   return rslt;
}



protected final String getTableName(Element xml)
{
   String rslt = null;

   rslt = IvyXml.getText(xml).trim();
   if (IvyXml.getAttrBool(xml,"START")) {
      rslt = DyviseDatabase.getTableName(rslt,comp_main.getStartName());
    }

   return rslt;
}



}	// end of abstract class DycompAnalysis




/* end of DycompAnalysis.java */
