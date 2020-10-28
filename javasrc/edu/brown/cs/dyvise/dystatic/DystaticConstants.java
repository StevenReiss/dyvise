/********************************************************************************/
/*										*/
/*		DystaticConstants.java						*/
/*										*/
/*	DYVISE static analysis constants					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dystatic/DystaticConstants.java,v 1.3 2011-03-10 02:33:17 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DystaticConstants.java,v $
 * Revision 1.3  2011-03-10 02:33:17  spr
 * Code cleanup.
 *
 * Revision 1.2  2010-03-30 16:22:43  spr
 * Minor bug fixes and clean up.
 *
 * Revision 1.1  2009-09-19 00:13:48  spr
 * Static analyzer storing info in the database.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dystatic;


import edu.brown.cs.dyvise.dyvise.DyviseConstants;

import edu.brown.cs.ivy.file.IvyFile;


interface DystaticConstants extends DyviseConstants {


/********************************************************************************/
/*										*/
/*	File locations								*/
/*										*/
/********************************************************************************/

String DYSTATIC_FILE_DIR = IvyFile.expandName("$(DYVISE)/tmp/dystatic");

String DYSTATIC_MAP_FILE = IvyFile.expandName("$(DYVISE)/lib/dystatic.xml");




}	// end of interface DystaticConstants




/* end of DystaticConstants.java */
