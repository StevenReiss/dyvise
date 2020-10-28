/********************************************************************************/
/*										*/
/*		DyviewVisual.java						*/
/*										*/
/*	DYname VIEW controller for a particular type of visualization		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyview/DyviewVisual.java,v 1.2 2009-10-07 01:00:21 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyviewVisual.java,v $
 * Revision 1.2  2009-10-07 01:00:21  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.1  2009-09-19 00:14:23  spr
 * Viewer with dialog based front end.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dyview;


import edu.brown.cs.dyvise.dyvise.DyviseConstants;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import javax.swing.JPanel;

import java.util.*;


abstract class DyviewVisual implements DyviewConstants, DyviseConstants,
	Comparable<DyviewVisual>
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

protected DyviewModel	view_model;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected DyviewVisual(DyviewModel vm)
{
   view_model = vm;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public Collection<String> getProflets()
{
   return new ArrayList<String>();
}



abstract public String getIdName();



protected DyviewModel getViewModel()		{ return view_model; }




/********************************************************************************/
/*										*/
/*	Comparison methods							*/
/*										*/
/********************************************************************************/

public int compareTo(DyviewVisual dv)
{
   return toString().compareTo(dv.toString());
}


/********************************************************************************/
/*										*/
/*	Methods for creating appropriate dialogs				*/
/*										*/
/********************************************************************************/

abstract public JPanel setupPanel(DyviewModel mdl);


/********************************************************************************/
/*										*/
/*	Methods for doing computations prior to visualization			*/
/*										*/
/********************************************************************************/

public String setupRelations()			{ return null; }
public String getPatchModel()			{ return null; }


abstract public boolean isReady();

abstract public Map<String,Object> getParameters();



/********************************************************************************/
/*										*/
/*	Save/load methods							*/
/*										*/
/********************************************************************************/

public void outputXml(IvyXmlWriter xw)		{ }
public void loadXml(Element xml)		{ }


}	// end of class DyviewVisual




/* end of DyviewVisual.java */
