/********************************************************************************/
/*										*/
/*		DycompAnalysisView.java 					*/
/*										*/
/*	DYVISE computed relations analysis for creating a restricted view	*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dycomp/DycompAnalysisView.java,v 1.2 2011-03-10 02:32:39 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DycompAnalysisView.java,v $
 * Revision 1.2  2011-03-10 02:32:39  spr
 * Clean up code
 *
 * Revision 1.1  2010-03-30 21:28:29  spr
 * Add new analyses needed for better visualizations.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dycomp;


import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;



class DycompAnalysisView extends DycompAnalysis implements DycompConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		source_relation;
private String		source_restrict;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DycompAnalysisView(DycompMain dm)
{
   super(dm);
}



DycompAnalysisView(DycompAnalysisView proto,Element xml)
{
   super(proto,xml);

   source_relation = getTableName(xml,"SOURCE");
   source_restrict = IvyXml.getTextElement(xml,"RESTRICT");
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

DycompAnalysis createNew(Element xml)
{
   DycompAnalysisView da = new DycompAnalysisView(this,xml);

   return da;
}



/********************************************************************************/
/*										*/
/*	Work methods								*/
/*										*/
/********************************************************************************/

void compute()
{
   String q = "SELECT * FROM " + source_relation + " SOURCE";
   if (source_restrict != null) {
      q += " WHERE " + source_restrict;
    }

   comp_main.createView(output_table,q);

   comp_main.runSql();
}





}	// end of class DycompAnalysisView




/* end of DycompAnalysisView.java */
