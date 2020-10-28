/********************************************************************************/
/*                                                                              */
/*              DylockViewMultiType.java                                        */
/*                                                                              */
/*      View type with multiple locks                                           */
/*                                                                              */
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylock/DylockViewMultiType.java,v 1.2 2013-05-09 12:29:01 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylockViewMultiType.java,v $
 * Revision 1.2  2013-05-09 12:29:01  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.1  2012-10-05 00:52:42  spr
 * Update lock analysis.  Prepare for pattern display.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dylock;

import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.Element;

import java.util.*;



class DylockViewMultiType extends DylockViewType implements DylockConstants
{

   
/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private List<DylockViewType> view_types;
      


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/


DylockViewMultiType(DylockViewType t1,DylockViewType t2) 
{
   super(t1.getViewer(),null);
   view_types = new ArrayList<DylockViewType>();
   view_types.add(t1);
   view_types.add(t2);
   addLocations(t1.getLocations());
   addLocations(t2.getLocations());
}

DylockViewMultiType(DylockViewRef dv,Element e) 
{
   super(dv,e);
   view_types = new ArrayList<DylockViewType>();
   for (Element te : IvyXml.children(e,"TYPE")) {
      DylockViewType vt = DylockViewType.createSimpleViewType(dv,te);
      if (vt != null) addType(vt);
    }
}


/********************************************************************************/
/*                                                                              */
/*      Construction methods                                                    */
/*                                                                              */
/********************************************************************************/

void addType(DylockViewType vt)
{
   view_types.add(vt);
   addLocations(vt.getLocations());
}
     


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

String getShortString()	{ return view_types.get(0).getShortString() + "+"; }
   

void addToolTip(StringBuffer buf) 
{
   for (DylockViewType vt : view_types) {
      vt.addToolTip(buf);
    }
}


List<DylockViewType> getViewTypes()
 {
   return new ArrayList<DylockViewType>(view_types);
}



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

void localOutputXml(IvyXmlWriter xw) 
{
   xw.field("KIND","MULTIPLE");
   for (DylockViewType vt : view_types) {
      vt.outputXml(xw);
    }
}




}	// end of class DylockViewMultiType




/* end of DylockViewMultiType.java */
