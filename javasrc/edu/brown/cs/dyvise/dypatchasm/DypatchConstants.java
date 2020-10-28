/********************************************************************************/
/*										*/
/*		DypatchConstants.java						*/
/*										*/
/*	Constants for Dyper patcher						*/
/*										*/
/********************************************************************************/
/*	Copyright 2005 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2005, Brown University, Providence, RI.				 *
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dypatchasm/DypatchConstants.java,v 1.2 2013/09/04 18:36:34 spr Exp $ */

/*********************************************************************************
 *
 * $Log: DypatchConstants.java,v $
 * Revision 1.2  2013/09/04 18:36:34  spr
 * Minor bug fixes.
 *
 * 
 * Revision 1.5  2013-08-19 16:26:07  zolstein
 * Update to support new ASM patcher
 * 
 * Revision 1.4  2009-09-19 00:13:15  spr
 * Update patcher to handle line number patching.
 *
 * Revision 1.3  2009-03-20 02:07:32  spr
 * Fix imports.
 *
 * Revision 1.2  2008-11-12 14:10:56  spr
 * Change patcher to handle overlapping requests.
 *
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/

package edu.brown.cs.dyvise.dypatchasm;

import org.objectweb.asm.Opcodes;

import edu.brown.cs.dyvise.dyper.DyperConstants;
import edu.brown.cs.ivy.file.IvyFile;

public interface DypatchConstants extends DyperConstants {

/********************************************************************************/
/*										*/
/* Patch Options */
/*										*/
/********************************************************************************/

enum PatchOption
{
    NONE, ENTER, EXIT, BLOCK, SYNC_ENTER, SYNC_EXIT, STATIC_ACCESS, FIELD_ACCESS, STATIC_WRITE, FIELD_WRITE, OBJECT_ALLOC, ARRAY_ALLOC, BASE_ARRAY_ALLOC, CALL, LINE
}

/********************************************************************************/
/*										*/
/* Argument types */
/*										*/
/********************************************************************************/

enum ArgumentType
{
    THIS, INTEGER, CURTHREAD, CURMETHOD, CURCLASS, FIELD, RETURN, LOCAL, INSNO, BLOCKID, METHODID, CLASSID, REFMETHODID, REFCLASSID, REFBLOCKID, ARG, CONTEXT, NULL, NEWLOCAL, STACK, MULTIPLE
}

enum AccessType
{
    NONE, LOCAL, STATICFIELD, FIELD, STATICMETHOD0, METHOD0
}

/********************************************************************************/
/*										*/
/* Activate types */
/*										*/
/********************************************************************************/

enum ActionType
{
    NONE, ADD, REMOVE
}

enum PatchMode
{
    NONE, PATCH, COUNTERS
}

/********************************************************************************/
/*										*/
/* Files */
/*										*/
/********************************************************************************/

String MODE_FILE = IvyFile.expandName("$(DYVISE)/lib/dypatch_modes.xml");
String ALT_MODE_FILE = "dyvise/edu/brown/cs/dyvise/dypatch/dypatch_modes.xml";

/********************************************************************************/
/*										*/
/* Other constants */
/*										*/
/********************************************************************************/

long   EXIT_DELAY  = 300000;
int    ASM_VERSION = Opcodes.ASM4;

String TEST0_CLASSPATH = "<CLASSMODEL CLASSPATH = '' BOOTPATH = ''/>";

String TEST1_CLASSPATH = "<CLASSMODEL CLASSPATH = '/pro/s6/java' BOOTPATH = ''>"
         + "<CLASS NAME = 'edu.brown.cs.s6.engine.EngineMain$DependWorker' />"
         + "<CLASS NAME = 'edu.brown.cs.s6.engine.EngineMain$TestWorker' />"
         + "<CLASS NAME = 'edu.brown.cs.s6.engine.EngineMain$TransformSolution' />"
         + "<CLASS NAME = 'edu.brown.cs.s6.engine.EngineMain' />"
         + "<CLASS NAME = 'edu.brown.cs.s6.engine.EngineMain' />"
         + "<CLASS NAME = 'edu.brown.cs.s6.engine.EnginePool$Worker' />"
         + "</CLASSMODEL>";

String TEST2_CLASSPATH_FILE = "dyvise/edu/brown/cs/dyvise/dypatchasm/test2_classpath_new.xml";

} // end of interface DypatchConstants

/* end of DypatchConstants.java */

