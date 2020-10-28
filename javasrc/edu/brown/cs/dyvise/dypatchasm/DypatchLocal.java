/********************************************************************************/
/*										*/
/*		DypatchLocal.java						*/
/*										*/
/*	Representation of a new local variable for patching			*/
/*										*/
/********************************************************************************/
/*	Copyright 2005 Brown University -- Steven P. Reiss		        */
/*********************************************************************************
 *  Copyright 2013, Brown University, Providence, RI.				 *
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dypatchasm/DypatchLocal.java,v 1.2 2013/09/04 18:36:34 spr Exp $ */

/*********************************************************************************
 *
 * $Log: DypatchLocal.java,v $
 * Revision 1.2  2013/09/04 18:36:34  spr
 * Minor bug fixes.
 *
 * Revision 1.2 2013-08-19 16:26:07
 * Updated for use with ASM
 * 
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/

package edu.brown.cs.dyvise.dypatchasm;

import org.objectweb.asm.*;

class DypatchLocal implements DypatchConstants {

/********************************************************************************/
/*										*/
/* Private storage                                                              */
/*										*/
/********************************************************************************/

private String local_id;
private Type   local_type;
private int    local_index;

/********************************************************************************/
/*										*/
/* Constructors                                                                 */
/*										*/
/********************************************************************************/

DypatchLocal(String localId, String desc, int index, Label end)
{
    this(localId, Type.getType(desc), index, end);
}

DypatchLocal(String localId, Type type, int index, Label end)
{
    local_id = localId;
    local_type = type;
    local_index = index;
}

/********************************************************************************/
/*										*/
/* Access methods */
/*										*/
/********************************************************************************/

String getId()
{
    return local_id;
}

Type getType()
{
    return local_type;
}

int getIndex()
{
    return local_index;
}

void setIndex(int idx)
{
    local_index = idx;
}

} // end of class DypatchLocal

/* end of DypatchLocal.java */

