/********************************************************************************/
/*										*/
/*		DymasterConstants.java						*/
/*										*/
/*	Constants for dyper performance evaluation master interface		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymaster/DymasterConstants.java,v 1.4 2010-03-30 16:21:37 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymasterConstants.java,v $
 * Revision 1.4  2010-03-30 16:21:37  spr
 * Change mint mode; fix up process tracking.
 *
 * Revision 1.3  2009-06-04 18:53:21  spr
 * Use dyviseJava call if needed.
 *
 * Revision 1.2  2009-03-20 02:06:10  spr
 * Add options before enabling for selective enable.
 *
 * Revision 1.1  2008-11-24 23:39:54  spr
 * Master control for dyvise now here.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymaster;


import edu.brown.cs.dyvise.dymon.DymonConstants;
import edu.brown.cs.ivy.file.IvyFile;



public interface DymasterConstants extends DymonConstants {


/********************************************************************************/
/*										*/
/*	Commands for running dymon						*/
/*										*/
/********************************************************************************/

String DYMASTER_DYVISION_COMMAND = IvyFile.expandName("$(DYVISE)/bin/startdyvision");
String DYMASTER_DYVISION_CLASS = "edu.brown.cs.dyvise.dyvision.DyvisionMain";
String DYMASTER_DYVISION_JARGS = "-Xmx1500m";



/********************************************************************************/
/*										*/
/*	Timing constants							*/
/*										*/
/********************************************************************************/

long	DYMASTER_PROCESS_UPDATE = 1000L;




}	// end of interface DymasterConstants




/* end of DymasterConstants.java */
