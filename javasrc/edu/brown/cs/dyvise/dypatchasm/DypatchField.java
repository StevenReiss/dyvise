/********************************************************************************/
/*                                                                              */
/*              DypatchField.java                                               */
/*                                                                              */
/*      Representation of a Field                                               */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2005 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2005, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of Brown University not be used in          *
 *  advertising or publicity pertaining to distribution of the software          *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS                *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY      *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
 ********************************************************************************/

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dypatchasm/DypatchField.java,v 1.2 2013/09/04 18:36:34 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DypatchField.java,v $
 * Revision 1.2  2013/09/04 18:36:34  spr
 * Minor bug fixes.
 *
 * Revision 1.0 2013-08-19 16:26:07 zolstein
 * Original version
 * 
 ********************************************************************************/

package edu.brown.cs.dyvise.dypatchasm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class DypatchField {

/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private int    field_access;
private String field_name;
private Type   field_type;

/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public DypatchField(int access, String name, String desc/* , Object value */)
{
    field_access = access;
    field_name = name;
    field_type = Type.getType(desc);
}

/********************************************************************************/
/*                                                                              */
/*      Getters/Setters                                                         */
/*                                                                              */
/********************************************************************************/

public int getAccess()
{
    return field_access;
}

public String getName()
{
    return field_name;
}

public Type getType()
{
    return field_type;
}

public boolean isStatic()
{
    return (field_access & Opcodes.ACC_STATIC) != 0;
}
}
