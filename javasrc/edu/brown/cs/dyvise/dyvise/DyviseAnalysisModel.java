/********************************************************************************/
/*										*/
/*		DyAnalysisModel.java						*/
/*										*/
/*	DYVISE abstract analysis model definition				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyvise/DyviseAnalysisModel.java,v 1.1 2009-09-19 00:14:44 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyviseAnalysisModel.java,v $
 * Revision 1.1  2009-09-19 00:14:44  spr
 * Common files for use throughout the system.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dyvise;



import java.util.List;



public interface DyviseAnalysisModel extends DyviseConstants {


void doAnalysis(DyviseInstance instance);

boolean requiresStatic();
boolean requiresDynamic();



enum AnalysisType {
   DYNAMIC,
   STATIC
}


enum ParameterType {
   VARIABLE,
   INT_CONSTANT,
   BOOL_CONSTANT,
   DOUBLE_CONTANT
}




interface Analysis {

   AnalysisType getAnalysisType();
   String getName();
   List<Parameter> getParameters();
   // List<Parameter> getOuptuts();

}	// end of subinterface Analysis



interface Parameter {

   ParameterType getParameterType();
   String getValue();
   String getType();

}	// end of subinterface Parameter



}	// end of interface DyAnalysisModel




/* end of DyAnalysisModel.java */