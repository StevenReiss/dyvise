/********************************************************************************/
/*										*/
/*		DymacProflet.java						*/
/*										*/
/*	DYVISE dynamic analysis analysis agent interface			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymac/DymacProflet.java,v 1.3 2010-03-30 16:21:04 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymacProflet.java,v $
 * Revision 1.3  2010-03-30 16:21:04  spr
 * Bug fixes in dynamic analysis.
 *
 * Revision 1.2  2009-10-07 00:59:45  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.1  2009-09-19 00:09:00  spr
 * Module to collect dynamic information from dymon about an applcation and store in database.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymac;


import edu.brown.cs.dyvise.dyvise.DyviseConstants;
import edu.brown.cs.dyvise.dyvise.DyviseDatabase;

import org.w3c.dom.Element;


interface DymacProflet extends DyviseConstants {



String getName();

String getAgentName();

boolean processData(Element xml);

boolean verifyResults(String start);

void saveData(DyviseDatabase db,String start);


}	// end of interface DymacProflet




/* end of DymacProflet.java */


