/********************************************************************************/
/*										*/
/*		DystoreFilter.java						*/
/*										*/
/*	Filter for selecting values from a field				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dystore/DystoreFilter.java,v 1.2 2013-05-09 12:29:03 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DystoreFilter.java,v $
 * Revision 1.2  2013-05-09 12:29:03  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.1  2010-03-30 21:29:08  spr
 * Add the notion of filters and separate the data map to its own class.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dystore;


import edu.brown.cs.dyvise.dygraph.DygraphConstants;
import edu.brown.cs.ivy.xml.IvyXmlWriter;




public interface DystoreFilter extends DygraphConstants {


/**
 *	Filter a string.  Either returns null (ignore) or a substitute string that is
 *	what should be used and displayed for the visualization.
 **/

String filter(String v);

boolean filter(double v);

void output(IvyXmlWriter xw);



}	// end of interface DystoreFilter




/* end of DystoreFilter.java */
