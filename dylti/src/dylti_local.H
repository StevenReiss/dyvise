/********************************************************************************/
/*										*/
/*		dylti_local.H							*/
/*										*/
/*	Java monitor using JVMTI for handling lock monitoring			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/dylti/src/dylti_local.H,v 1.2 2012-10-05 00:51:57 spr Exp $ */


/*********************************************************************************
 *
 * $Log: dylti_local.H,v $
 * Revision 1.2  2012-10-05 00:51:57  spr
 * Clean up to support new dylock.  (Actually this should be removed since it
 * is no longer used).
 *
 * Revision 1.1  2011-09-12 18:29:46  spr
 * Add dylti to cvs
 *
 *
 ********************************************************************************/


#ifndef DYLTI_LOCAL_ALREADY_INCLUDED
#define DYLTI_LOCAL_ALREADY_INCLUDED


#include "dylti.H"

#include <ivy_datatypes.H>
#include <ivy_hash.H>
#include <ivy_file.H>
#include <ivy_fileio.H>
#include <ivy_string.H>
#include <ivy_xml.H>
#include <ivy_socket.H>

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



namespace Dyvise {

namespace Dylti {



/********************************************************************************/
/*										*/
/*	Constants								*/
/*										*/
/********************************************************************************/



/********************************************************************************/
/*										*/
/*	Forward Type Definitions						*/
/*										*/
/********************************************************************************/

typedef class DyltiMainInfo *		DyltiMain;
typedef class DyltiMonitorInfo *	DyltiMonitor;
typedef class DyltiLockDataInfo *	DyltiLockData;
typedef class DyltiLockLoggerInfo *	DyltiLockLogger;
typedef class DyltiBreakItemInfo *	DyltiBreakItem;
class DyltiLocation;

typedef HashMap<StdString,StdString>	DyltiEnvHash;

typedef Vector<DyltiLockData>		DyltiLockList;
typedef DyltiLockList::iterator 	DyltiLockListIter;

typedef Vector<DyltiLocation>		DyltiLocationList;
typedef DyltiLocationList::iterator	DyltiLocationListIter;

typedef HashMap<jmethodID,StdString>	DyltiMethodMap;
typedef HashMap<jthread,int>		DyltiThreadMap;

typedef Set<DyltiLockData>		DyltiLockSet;
typedef DyltiLockSet::iterator		DyltiLockSetIter;

typedef jvmtiEnv *			DyltiEnv;
typedef JNIEnv *			DyltiJni;



/********************************************************************************/
/*										*/
/*	DyltiMain -- controller for JVMTI monitoring				*/
/*										*/
/********************************************************************************/

class DyltiMainInfo {

private:
   JavaVM * java_vm;
   DyltiEnv jvmti_env;
   DyltiMonitor using_monitor;
   DyltiEnvHash user_options;

public:
   DyltiMainInfo(JavaVM *,DyltiEnv,const char * opts);
   ~DyltiMainInfo();

   void initialSetup();

   CStdString getOption(CStdString p);

   void allocMonitor(IvyXmlNode);

   void finalize();

private:
   StdString findProcess();

};




/********************************************************************************/
/*										*/
/*	DyltiMonitor -- monitor to talk with the jvm				*/
/*										*/
/********************************************************************************/

class DyltiMonitorInfo {

private:
   DyltiMain for_main;
   JavaVM * java_vm;
   DyltiEnv jvmti_env;
   bool is_attached;
   bool bpts_set;
   IvyMutex lock_mutex;
   DyltiLockList active_locks;
   DyltiLockLogger lock_logger;
   LongLong base_time;

public:
   DyltiMonitorInfo(DyltiMain,JavaVM * jvm,DyltiEnv);
   ~DyltiMonitorInfo();

   void initialSetup(bool);

   void attach();

   void processContendedEnter(DyltiJni,jthread,jobject);
   void processContendedEntered(DyltiJni,jthread,jobject);
   void processMonitorWait(DyltiJni,jthread,jobject,jlong);
   void processMonitorWaited(DyltiJni,jthread,jobject,jboolean);

   StdString getLocationText(jmethodID);

   void outputXml();

private:
   void actualAttach(JNIEnv * jni);
   void initialAttach();
   void resume(jthread th);

   DyltiLockData getLockData(DyltiJni,jobject);
   int nestLevel(DyltiJni,jthread,DyltiLockData,LongLong);
   DyltiLocation getLocation(jthread);
};



/********************************************************************************/
/*										*/
/*	DyltiLocation -- hold a location					*/
/*										*/
/********************************************************************************/

class DyltiLocation {

private:
   jmethodID method_id;
   jlocation method_offset;

public:
   DyltiLocation(jmethodID mid,jlocation l) {
      method_id = mid;
      method_offset = l;
    }

   jmethodID getMethodId()			{ return method_id; }
   jlocation getLocation()			{ return method_offset; }

   bool equals(DyltiLocation& loc) {
      return loc.method_id == method_id && loc.method_offset == method_offset;
    }
};




/********************************************************************************/
/*										*/
/*	DyltiLockData -- data for a particular lock				*/
/*										*/
/********************************************************************************/

class DyltiLockDataInfo {

private:
   DyltiLocationList location_set;
   StdString lock_class;
   int wait_count;
   int enter_count;
   int waited_count;
   int timed_count;
   IvyMutex lock_mutex;
   DyltiMonitor using_monitor;
   DyltiMethodMap method_map;
   int lock_id;
   bool is_unnested;
   DyltiLockSet * prior_items;

public:
   DyltiLockDataInfo(DyltiMonitor,const char * clsnm,int id);
   ~DyltiLockDataInfo();

   void contendedEnter(jthread,DyltiLocation& loc);
   void contendedEntered(jthread,DyltiLocation& loc);
   void monitorWait(jthread,DyltiLocation& loc);
   void monitorWaited(jthread,DyltiLocation& loc,bool timeout);

   void outputXml(DyltiMonitor,IvyXmlWriter&);

   CStdString getLockClass()				{ return lock_class; }
   int getLockId()					{ return lock_id; }
   CStdString getMethodName(jmethodID mid)		{ return method_map[mid]; }

   void addPriorLock(DyltiLockData ld);
   void noteFirstLevelLock()				{ is_unnested = true; }

private:
   void addLocation(DyltiLocation& loc);
};




/********************************************************************************/
/*										*/
/*	DyltiLockLogger -- lock data logging					*/
/*										*/
/********************************************************************************/

class DyltiLockLoggerInfo {

private:
   DyltiMonitor using_monitor;
   ostream& log_file;
   IvyMutex log_mutex;
   DyltiThreadMap thread_map;

public:
   DyltiLockLoggerInfo(DyltiMonitor,ostream&);
   ~DyltiLockLoggerInfo();

   void contendedEnter(jthread,DyltiLocation& loc,LongLong now,DyltiLockData,int);
   void contendedEntered(jthread,DyltiLocation& loc,LongLong now,DyltiLockData,int);
   void monitorWait(jthread,DyltiLocation& loc,LongLong now,DyltiLockData,int);
   void monitorWaited(jthread,DyltiLocation& loc,bool timeout,LongLong now,DyltiLockData,int);

   void output();

private:
   void header();
   void entry(ConstText,jthread,DyltiLocation& loc,LongLong now,DyltiLockData,int);

};



} }	// end of namespace Dylti, Dyvise


using namespace Dyvise;
using namespace Dyvise::Dylti;




#endif



/* end of dylti_local.H */
