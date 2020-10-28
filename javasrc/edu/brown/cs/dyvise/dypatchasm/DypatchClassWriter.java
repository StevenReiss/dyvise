/********************************************************************************/
/*                                                                              */
/*              DypatchClassWriter.java                                         */
/*                                                                              */
/*      ClassWriter with support for arbitrary ClassLoaders                     */
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dypatchasm/DypatchClassWriter.java,v 1.0 2013-08-19 16:26:07 zolstein Exp $ */


/*********************************************************************************
 *
 * $Log: DypatchField.java,v $
 * Revision 1.0 2013-08-19 16:26:07 zolstein
 * Original version
 * 
 ********************************************************************************/


package edu.brown.cs.dyvise.dypatchasm;

import org.objectweb.asm.ClassWriter;

public class DypatchClassWriter extends ClassWriter {

private ClassLoader class_loader;   

/********************************************************************************/
/*                                                                              */
/* Constructors */
/*                                                                              */
/********************************************************************************/

public DypatchClassWriter(int flags, ClassLoader classLoader)
{
   super(flags);
   class_loader = classLoader; 
}

/********************************************************************************/
/*                                                                              */
/* Overridden methods */
/*                                                                              */
/********************************************************************************/

@Override
protected String getCommonSuperClass(final String type1, final String type2) {
   Class<?> c, d;
   ClassLoader classLoader = class_loader;
   try {
       c = Class.forName(type1.replace('/', '.'), false, classLoader);
       d = Class.forName(type2.replace('/', '.'), false, classLoader);
   } catch (Exception e) {
       throw new RuntimeException(e.toString());
   }
   if (c.isAssignableFrom(d)) {
       return type1;
   }
   if (d.isAssignableFrom(c)) {
       return type2;
   }
   if (c.isInterface() || d.isInterface()) {
       return "java/lang/Object";
   } else {
       do {
           c = c.getSuperclass();
       } while (!c.isAssignableFrom(d));
       return c.getName().replace('.', '/');
   }
}


   
}
