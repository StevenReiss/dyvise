/********************************************************************************/
/*										*/
/*		DygraphView.java						*/
/*										*/
/*	DYVISE graphics (visualization) single view definitions 		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dygraph/DygraphView.java,v 1.2 2009-10-07 00:59:44 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DygraphView.java,v $
 * Revision 1.2  2009-10-07 00:59:44  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.1  2009-09-19 00:08:37  spr
 * Module to draw various types of displays.  Only time rows implemented for now.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dygraph;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import javax.swing.JPanel;



public interface DygraphView extends DygraphConstants {

   String getName();

   JPanel getDisplayPanel();

   JPanel getControlPanel();

   void updateDisplay();

   DygraphControl getControl();

   void setXDataRegion(double start,double end);
   void setYDataRegion(double start,double end);

   void outputXml(IvyXmlWriter xw);
   void loadValues(Element xml);

}	// end of interface DygraphView





/* end of DygraphView.java */
