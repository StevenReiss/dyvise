/********************************************************************************/
/*										*/
/*		dymti_local.H							*/
/*										*/
/*	Java monitor using JVMTI for handling special events for DYPER		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/dymti/src/dymti_local.H,v 1.6 2011-03-10 02:32:36 spr Exp $ */


/*********************************************************************************
 *
 * $Log: dymti_local.H,v $
 * Revision 1.6  2011-03-10 02:32:36  spr
 * Clean up code
 *
 * Revision 1.5  2009-04-20 23:23:30  spr
 * Updates to make things work on the mac.  Fix bug in dymti.
 *
 * Revision 1.4  2009-04-11 23:45:15  spr
 * Clean up monitoring.
 *
 * Revision 1.3  2009-03-20 02:05:20  spr
 * Add memory reference dumping.
 *
 * Revision 1.2  2008-11-12 14:10:24  spr
 * Keep object totals.
 *
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/


#ifndef DYMTI_LOCAL_ALREADY_INCLUDED
#define DYMTI_LOCAL_ALREADY_INCLUDED


#include "dymti.H"

#include <ivy_datatypes.H>
#include <ivy_hash.H>
#include <ivy_file.H>
#include <ivy_fileio.H>
#include <ivy_string.H>
#include <ivy_xml.H>
#include <ivy_socket.H>

#include <mince.H>
#include <mince_thread.H>

#include <iostream>
#include <fstream>

#include <sys/types.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <time.h>
#include <fcntl.h>

#include <jni.h>
#include <jvmti.h>


using namespace Ivy;
using namespace Ivy::Mince;



namespace Dyvise {

namespace Dymti {



/********************************************************************************/
/*										*/
/*	Constants								*/
/*										*/
/********************************************************************************/

const char * const	DYMTI_START_CLASS = "START_CLASS";
const char * const	DYMTI_ROOT_CLASS = "*ROOT*";
const char * const	DYMTI_SYSTEM_CLASS = "*SYSTEM*";
const char * const	DYMTI_OTHER_CLASS = "*OTHER*";



/********************************************************************************/
/*										*/
/*	Local Structures							*/
/*										*/
/********************************************************************************/

typedef struct DymtiClassData {

   long total_size;
   int num_refs;
   int num_ptrs;
   int num_object;
   int num_new;

} DymtiClassData;




/********************************************************************************/
/*										*/
/*	Forward Type Definitions						*/
/*										*/
/********************************************************************************/

typedef class DymtiMainInfo *		DymtiMain;
typedef class DymtiMonitorInfo *	DymtiMonitor;
typedef class DymtiConnectionInfo *	DymtiConnection;
typedef class DymtiNativeInfo * 	DymtiNative;
typedef class DymtiReplyHandlerInfo *	DymtiReplyHandler;
typedef class DymtiDumpInfo *		DymtiDump;

typedef jvmtiEnv *			DymtiEnv;
typedef JNIEnv *			DymtiJni;

typedef HashMap<StdString,StdString>	DymtiEnvHash;

typedef HashMap<jlong,StdString>	DymtiTagClassHash;
typedef DymtiTagClassHash::iterator	DymtiTagClassHashIter;
typedef HashMap<StdString,jlong>	DymtiClassTagHash;

typedef HashMap<jlong,DymtiClassData>	DymtiClassCount;
typedef DymtiClassCount::iterator	DymtiClassCountIter;
typedef HashMap<jlong,bool>		DymtiTagSet;





/********************************************************************************/
/*										*/
/*	Reference pair information						*/
/*										*/
/********************************************************************************/

class DymtiRef {

private:
   jlong from_index;
   jlong to_index;

public:
   DymtiRef(jlong f = 0,jlong t = 0) {
      from_index = f;
      to_index = t;
    }

   jlong getFromIndex() const		{ return from_index; }
   jlong getToIndex() const		{ return to_index; }

   unsigned long hash() const		{ return from_index * 31 + to_index; }

   int operator ==(const DymtiRef r) const {
      return from_index == r.from_index && to_index == r.to_index;
    }

};


class DymtiRefHash {

public:
   unsigned long operator() (const DymtiRef& key) const {
      return key.hash();
    }
};



typedef HashMap<DymtiRef,int,1024,DymtiRefHash>  DymtiClassRefCount;
typedef DymtiClassRefCount::iterator	DymtiClassRefCountIter;



/********************************************************************************/
/*										*/
/*	DymtiMain -- controller for JVMTI monitoring				*/
/*										*/
/********************************************************************************/

class DymtiMainInfo {

private:
   JavaVM * java_vm;
   DymtiEnv jvmti_env;
   DymtiMonitor using_monitor;
   DymtiConnection using_connection;
   DymtiEnvHash user_options;
   DymtiNative using_native;

public:
   DymtiMainInfo(JavaVM *,DymtiEnv,const char * opts);
   ~DymtiMainInfo();

   void initialSetup();

   CStdString getOption(CStdString p);

   void allocMonitor(IvyXmlNode);

   void stopThreads();
   void resumeThreads();

   void buildMemoryModel(IvyXmlWriter&);

   StdString findProcess();

   void setParameter(CStdString nm,CStdString val);

   void finalize();
};




/********************************************************************************/
/*										*/
/*	DymtiMonitor -- monitor to talk with the jvm				*/
/*										*/
/********************************************************************************/

class DymtiMonitorInfo {

private:
   DymtiMain for_main;
   JavaVM * java_vm;
   DymtiEnv jvmti_env;
   DymtiClassCount class_count;
   DymtiClassRefCount ref_count;
   DymtiTagClassHash tag_class;
   DymtiClassTagHash class_tag;
   jthread * suspended_threads;
   jint suspend_count;
   int total_count;
   int total_refs;
   long total_size;
   bool is_attached;
   jlong next_tag;
   jlong root_tag;
   jlong class_size;
   DymtiDump dump_data;

public:
   DymtiMonitorInfo(DymtiMain,JavaVM * jvm,DymtiEnv);
   ~DymtiMonitorInfo();

   void initialSetup(bool);

   void setParameter(CStdString nm,CStdString val);

   void attach();

   void countItem(jlong,jlong,bool isnew = false);
   void countRef(jlong,jlong);
   void countRoot(jlong tag)			{ countRef(root_tag,tag); }

   void allocMonitor(IvyXmlNode);

   void stopThreads();
   void resumeThreads();

   void dumpMemoryModel(IvyXmlWriter&);

   CStdString getClassName(jlong tag);
   jlong getClassTag(CStdString);
   jlong getMergedClassTag(jlong referer,jlong referee,jlong size);
   jlong getStaticClassTag(jlong ctag);
   jlong getThreadTag(jlong threadid);

private:
   void initialAttach();
   void tagAllClasses();
   void fixCurrentThread(int ctr,jthread *);
   jlong tagClass(CStdString);
};




/********************************************************************************/
/*										*/
/*	DymtiConnection -- connection thru message server to VELD		*/
/*										*/
/********************************************************************************/

class DymtiConnectionInfo : public virtual MinceHandlerInfo {

private:
   DymtiMain for_main;
   MinceControl mince_control;
   StdString system_id;

public:
   DymtiConnectionInfo(DymtiMain);
   ~DymtiConnectionInfo();

private:
   void receive(MinceMessage,MinceArguments);
};




/********************************************************************************/
/*										*/
/*	DymtiNative -- connection through JNI native interface			*/
/*										*/
/********************************************************************************/


class DymtiNativeInfo {

private:
   DymtiMain for_main;

public:
   DymtiNativeInfo(DymtiMain);
   ~DymtiNativeInfo();

   void checkPoint();
   void dumpHeapModel(CStdString);
};




/********************************************************************************/
/*										*/
/*	DymtiReplyHandler -- handle replies from message server 		*/
/*										*/
/********************************************************************************/

class DymtiReplyHandlerInfo : public virtual MinceReplyInfo {

private:
   IvyXmlNode reply_msg;
   IvySemaphore reply_sema;

public:
   DymtiReplyHandlerInfo() {
      reply_msg = NULL;
      reply_sema.create(0);
    }

   void handleReply(MinceMessage src,MinceMessage rply) {
      reply_msg = rply->getXml();
      reply_sema.V();
    }

   IvyXmlNode waitForReply() {
      reply_sema.P();
      return reply_msg;
    }

};



/********************************************************************************/
/*										*/
/*	Definitions for heap dumping						*/
/*										*/
/********************************************************************************/

const jlong INSTANCE_BIT = 0x40000000;
const jlong OUTPUT_BIT = 0x80000000;



class DymtiDumpInfo {

private:
   const static long MIN_ARRAY_SIZE = 1000;
   const static long ARRAY_SCALE = 10;

private:
   DymtiMonitor dymti_monitor;
   IvyXmlWriter& xml_writer;
   jlong class_tag;
   DymtiTagSet merge_tags;
   DymtiTagSet ignore_tags;
   int pass_count;

public:
   DymtiDumpInfo(DymtiMonitor dm);
   DymtiDumpInfo(DymtiMonitor dm,IvyXmlWriter& xw,int pass);
   ~DymtiDumpInfo() { }

   void startPass(IvyXmlWriter&);

   IvyXmlWriter& getWriter()		{ return xml_writer; }

   CStdString getClassName(jlong tag)	{ return dymti_monitor->getClassName(tag); }

   void countItem(jlong * tptr,jlong tag,jlong size);

   void countRef(jlong ftag,jlong ttag) { dymti_monitor->countRef(ftag,ttag); }
   void countRoot(jlong tag)		{ dymti_monitor->countRoot(tag); }
   void countItem(jlong tag,jlong size) { dymti_monitor->countItem(tag,size); }
   void countClassRoot(jlong ftag,jlong ttag);
   void countSystemRoot(jlong tag);
   void countThreadRoot(jlong tag,jlong threadid);
   void countOtherRoot(jlong tag);

   jlong getReportTag(jlong ctag,jlong otag);
   jlong setReportTag(jlong ctag,jlong rtag,jlong * tptr,jlong size);
   bool checkIgnore(jvmtiHeapReferenceKind rk,jlong ctag,jlong rtag,jlong * rptr);

private:
   void setupTags();
};



/********************************************************************************/
/*										*/
/*	Functions for handling tags						*/
/*										*/
/********************************************************************************/

// Note that these are 32 bit on a 32 bit machine, but are 64 bit on a 64 bit machine

const jlong	 DYMTI_TAG_CLASS_MASK = 0x00ffffffl;
const jlong	 DYMTI_TAG_COUNT_MASK = 0x7f000000l;
const int	 DYMTI_TAG_COUNT_SHIFT = 24;
const int	 DYMTI_MAX_PASS = 8;


inline jlong
createNewTag(jlong cls)
{
   return cls & DYMTI_TAG_CLASS_MASK;
}


inline jlong
getTagClassId(jlong tag)
{
   return tag & DYMTI_TAG_CLASS_MASK;
}


inline void
setTagClassId(jlong * tptr,jlong tag)
{
   *tptr = (*tptr & DYMTI_TAG_COUNT_MASK) | (tag & DYMTI_TAG_CLASS_MASK);
}


inline int
getTagCount(jlong tag)
{
   tag = (tag & DYMTI_TAG_COUNT_MASK) >> DYMTI_TAG_COUNT_SHIFT;

   return ((int)(tag));
}



inline void
setTagCount(jlong * tptr,int ct)
{
   jlong ctl = ct;

   *tptr = (*tptr & DYMTI_TAG_CLASS_MASK) | (ctl << DYMTI_TAG_COUNT_SHIFT);
}




} }	// end of namespace Dymti, Dyvise


using namespace Dyvise;
using namespace Dyvise::Dymti;




#endif



/* end of dymti_local.H */
