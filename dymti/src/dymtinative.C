/********************************************************************************/
/*										*/
/*		dymtinative.C							*/
/*										*/
/*	Native interface to DYPER monitor calls 				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/dymti/src/dymtinative.C,v 1.2 2009-03-20 02:05:20 spr Exp $ */


/*********************************************************************************
 *
 * $Log: dymtinative.C,v $
 * Revision 1.2  2009-03-20 02:05:20  spr
 * Add memory reference dumping.
 *
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/

#include "dymti_local.H"




/********************************************************************************/
/*										*/
/*	Local storage								*/
/*										*/
/********************************************************************************/

static	DymtiNative	native_handler;

extern "C" {

   void Java_edu_brown_cs_dyvise_dyper_DyperNative_nativeCheckpoint(JNIEnv *,jclass);
   void Java_edu_brown_cs_dyvise_dyper_DyperNative_nativeDumpHeapModel(JNIEnv *,jclass,jstring);

}


/********************************************************************************/
/*										*/
/*	Constructors/destructors						*/
/*										*/
/********************************************************************************/

DymtiNativeInfo::DymtiNativeInfo(DymtiMain vm)
{
   for_main = vm;
   native_handler = this;
}



DymtiNativeInfo::~DymtiNativeInfo()
{ }




/********************************************************************************/
/*										*/
/*	Native Entry Points							*/
/*										*/
/********************************************************************************/

void
Java_edu_brown_cs_dyvise_dyper_DyperNative_nativeCheckpoint(JNIEnv * jni,jclass)
{
   native_handler->checkPoint();
}


void
Java_edu_brown_cs_dyvise_dyper_DyperNative_nativeDumpHeapModel(JNIEnv * jni,jclass,jstring file)
{
   StdString outfile = "java.checkpoint";

   if (file != NULL) {
      const char * fv = jni->GetStringUTFChars(file,NULL);
      if (fv != NULL && *fv != 0) outfile = fv;
    }

   native_handler->dumpHeapModel(outfile);
}



/********************************************************************************/
/*										*/
/*	Checkpoint methods							*/
/*										*/
/********************************************************************************/

void
DymtiNativeInfo::checkPoint()
{
   for_main->stopThreads();

   /***********
   int pid = fork();

   if (pid != 0) {
      for_main->resumeThreads();
      return;
    }
   *********/

   StdString outfile = "java.checkpoint";

   IvyXmlFileWriter ofs(outfile);
   if (!ofs) {
      cerr << "DYMTI: Unable to open checkpoint file " << outfile << endl;
    }

   for_main->resumeThreads();
   // _exit(0);
}



/********************************************************************************/
/*										*/
/*	Heap dump methods							*/
/*										*/
/********************************************************************************/

void
DymtiNativeInfo::dumpHeapModel(CStdString outfile)
{
   if (for_main == NULL) return;

   IvyXmlFileWriter ofs(outfile);
   if (!ofs) {
      cerr << "DYMTI: Unable to open model dump file " << outfile << endl;
    }
   else {
      for_main->buildMemoryModel(ofs);
      ofs.close();
    }
}




/* end of dymtinative.C */
