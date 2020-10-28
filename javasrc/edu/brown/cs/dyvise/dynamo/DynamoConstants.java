/********************************************************************************/
/*										*/
/*		DynamoConstants.java						*/
/*										*/
/*	DYVISE modeling constant definitions					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dynamo/DynamoConstants.java,v 1.1 2009-09-19 00:10:00 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DynamoConstants.java,v $
 * Revision 1.1  2009-09-19 00:10:00  spr
 * Module to generate model for visualization.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dynamo;


import edu.brown.cs.dyvise.dyvise.DyviseConstants;



interface DynamoConstants extends DyviseConstants {


/********************************************************************************/
/*										*/
/*	Special argument names							*/
/*										*/
/********************************************************************************/

String DYNAMO_ARG_THREAD = "*THREAD";
String DYNAMO_ARG_THIS = "this";
String DYNAMO_ARG_NULL = "NULL";
String DYNAMO_ARG_STACK = "*STACK";
String DYNAMO_ARG_LOCAL0 = "0";



/********************************************************************************/
/*										*/
/*	Patching names								*/
/*										*/
/********************************************************************************/

String DYNAMO_PATCH_CLASS = "edu.brown.cs.dyvise.dyper.DyperAgentEvents";
String DYNAMO_PATCH_METHOD = "newEvent";
String DYNAMO_PATCH_AGENT = "EVENTS";



}	// end of interface DynamoConstants




/* end of DynamoConstants.java */
