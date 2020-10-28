/********************************************************************************/
/*										*/
/*		DystaticMethodData.java 					*/
/*										*/
/*	Class to hold data associated with a flow method			*/
/*										*/
/********************************************************************************/
/*	Copyright 2006 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2006, Brown University, Providence, RI.				 *
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dystatic/DystaticMethodData.java,v 1.1 2009-09-19 00:13:48 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DystaticMethodData.java,v $
 * Revision 1.1  2009-09-19 00:13:48  spr
 * Static analyzer storing info in the database.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dystatic;


import edu.brown.cs.ivy.jflow.*;

import com.ibm.jikesbt.BT_Ins;



class DystaticMethodData extends JflowDefaultMethodData implements JflowConstants, DystaticConstants {


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DystaticMethodData(DystaticMain dm,JflowMethod jm)
{
   super(jm);
}





/********************************************************************************/
/*										*/
/*	Define the associations to use						*/
/*										*/
/********************************************************************************/

protected boolean useAssociation(AssociationType typ,BT_Ins ins,JflowValue v)
{
   switch (typ) {
      case THISARG :
      case THISREF :
      case THROW :
      case SYNC :
      case ARG1 :
      case ARG2 :
      case ARG3 :
      case ARG4 :
      case ARG5 :
      case ARG6 :
      case ARG7 :
      case ARG8 :
      case ARG9 :
	 return true;
      default :
	 break;
    }

   return false;
}





}	// end of class DystaticMethodData



/* end of DystaticMethodData.java */

