/********************************************************************************/
/*										*/
/*		dyltimonitor.C							*/
/*										*/
/*	JVMTI monitor code for lock monitoring					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/dylti/src/dyltimonitor.C,v 1.3 2012-10-05 00:51:57 spr Exp $ */


/*********************************************************************************
 *
 * $Log: dyltimonitor.C,v $
 * Revision 1.3  2012-10-05 00:51:57  spr
 * Clean up to support new dylock.  (Actually this should be removed since it
 * is no longer used).
 *
 * Revision 1.2  2011-09-12 19:39:04  spr
 * Clean up
 *
 * Revision 1.1  2011-09-12 18:29:47  spr
 * Add dylti to cvs
 *
 *
 ********************************************************************************/

#include "dylti_local.H"




/********************************************************************************/
/*										*/
/*	Local function definitions						*/
/*										*/
/********************************************************************************/

static void handleMonitorContendedEnter(DyltiEnv,DyltiJni,jthread th,jobject obj);
static void handleMonitorContendedEntered(DyltiEnv,DyltiJni,jthread th,jobject obj);
static void handleMonitorWait(DyltiEnv,DyltiJni,jthread th,jobject obj,jlong timeout);
static void handleMonitorWaited(DyltiEnv,DyltiJni,jthread th,jobject obj,jboolean timedout);
static void handleVMDeath(DyltiEnv,DyltiJni);



/********************************************************************************/
/*										*/
/*	Local Storage								*/
/*										*/
/********************************************************************************/

static DyltiMonitor		the_monitor = NULL;



static class IgnoreSet {

private:
   HashMap<StdString,bool> ignore_set;

public:
   IgnoreSet() {
      ignore_set["Ljava/lang/ref/ReferenceQueue$Lock;"] = true;
      ignore_set["Ljava/awt/EventQueue;"] = true;
      ignore_set["Ljava/awt/EventQueue$1AWTInvocationLock"] = true;
      ignore_set["Ledu/brown/cs/bubbles/buda/BudaRoot$MouseEventQueue;"] = true;
      ignore_set["Lsun/misc/Launcher$AppClassLoader;"] = true;
      ignore_set["Ljava/util/TaskQueue;"] = true;
      ignore_set["Ljavax/swing/TimerQueue;"] = true;
      ignore_set["Lsun/awt/AWTAutoShutdown;"] = true;
    }

   const bool ignore(CStdString s)	{ return ignore_set[s]; }

}	IGNORE_SET;



static HashMap<jmethodID,bool>		wait_set;
static bool				wait_setup = false;
static int				id_counter = 0;




/********************************************************************************/
/*										*/
/*	Constructors/destructors						*/
/*										*/
/********************************************************************************/

DyltiMonitorInfo::DyltiMonitorInfo(DyltiMain m,JavaVM * jvm,DyltiEnv env)
{
   for_main = m;
   java_vm = jvm;
   jvmti_env = env;
   is_attached = false;
   bpts_set = false;
   lock_mutex.createThread();
   lock_logger = NULL;
   base_time = currentTimeNanos();

   CStdString logname = m->getOption("LOG");
   if (logname != "") {
      ostream * ostp = new ofstream(logname.c_str());
      if (*ostp) {
	 lock_logger = new DyltiLockLoggerInfo(this,*ostp);
       }
    }

   the_monitor = this;
}




DyltiMonitorInfo::~DyltiMonitorInfo()
{ }



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

void
DyltiMonitorInfo::initialSetup(bool attach)
{
   if (attach && !is_attached) {
      is_attached = true;
      initialAttach();
    }
}



void
DyltiMonitorInfo::initialAttach()
{
   jvmtiEventCallbacks cbs;
   jvmtiCapabilities cap;
   jvmtiError e;

   e = jvmti_env->GetPotentialCapabilities(&cap);

   e = jvmti_env->GetCapabilities(&cap);

   cap.can_tag_objects = 1;
   cap.can_generate_field_modification_events = 0;
   cap.can_generate_field_access_events = 0;
   cap.can_get_bytecodes = 0;
   cap.can_get_synthetic_attribute = 0;
   cap.can_get_owned_monitor_info = 1;
   cap.can_get_current_contended_monitor = 1;
   cap.can_get_monitor_info = 1;
   cap.can_pop_frame = 0;
   cap.can_redefine_classes = 0;
   cap.can_signal_thread = 0;
   cap.can_get_source_file_name = 0;
   cap.can_get_line_numbers = 0;
   cap.can_get_source_debug_extension = 0;
   cap.can_access_local_variables = 0;
   cap.can_maintain_original_method_order = 0;
   cap.can_generate_single_step_events = 0;
   cap.can_generate_exception_events = 0;
   cap.can_generate_frame_pop_events = 0;
   cap.can_generate_breakpoint_events = 1;
   cap.can_suspend = 0;
   cap.can_redefine_any_class = 0;
   cap.can_get_current_thread_cpu_time = 0;
   cap.can_get_thread_cpu_time = 0;
   cap.can_generate_method_entry_events = 0;
   cap.can_generate_method_exit_events = 0;
   cap.can_generate_all_class_hook_events = 0;
   cap.can_generate_compiled_method_load_events = 0;
   cap.can_generate_monitor_events = 1;
   cap.can_generate_vm_object_alloc_events = 0;
   cap.can_generate_native_method_bind_events = 0;
   cap.can_generate_garbage_collection_events = 0;
   cap.can_generate_object_free_events = 0;
   cap.can_force_early_return = 1;
   cap.can_get_owned_monitor_stack_depth_info = 1;
   cap.can_get_constant_pool = 0;
   cap.can_set_native_method_prefix = 0;
   cap.can_retransform_classes = 0;
   cap.can_retransform_any_class = 0;
   cap.can_generate_resource_exhaustion_heap_events = 0;
   cap.can_generate_resource_exhaustion_threads_events = 0;

   e = jvmti_env->AddCapabilities(&cap);
   if (e != 0) {
      cerr << "DYLTI: Problem setting capabilities: " << e << endl;
      // this can fail if sharing is enabled -- won't be able to iterate over heap
    }

   cbs.VMInit = NULL;
   cbs.VMDeath = handleVMDeath;
   cbs.ThreadStart = NULL;
   cbs.ThreadEnd = NULL;
   cbs.ClassFileLoadHook = NULL;
   cbs.ClassLoad = NULL;
   cbs.ClassPrepare = NULL;
   cbs.VMStart = NULL;
   cbs.Exception = NULL;
   cbs.ExceptionCatch = NULL;
   cbs.SingleStep = NULL;
   cbs.FramePop = NULL;
   cbs.Breakpoint = NULL;
   cbs.FieldAccess = NULL;
   cbs.FieldModification = NULL;
   cbs.MethodEntry = NULL;
   cbs.MethodExit = NULL;
   cbs.NativeMethodBind = NULL;
   cbs.CompiledMethodLoad = NULL;
   cbs.CompiledMethodUnload = NULL;
   cbs.DynamicCodeGenerated = NULL;
   cbs.DataDumpRequest = NULL;
   cbs.reserved72 = NULL;
   cbs.MonitorWait = handleMonitorWait;
   cbs.MonitorWaited = handleMonitorWaited;
   cbs.MonitorContendedEnter = handleMonitorContendedEnter;
   cbs.MonitorContendedEntered = handleMonitorContendedEntered;
   cbs.reserved77 = NULL;
   cbs.reserved78 = NULL;
   cbs.reserved79 = NULL;
   // cbs.ResourceExhausted = NULL;
   cbs.GarbageCollectionStart = NULL;
   cbs.GarbageCollectionFinish = NULL;
   cbs.ObjectFree = NULL;
   cbs.VMObjectAlloc = NULL;

   e = jvmti_env->SetEventCallbacks(&cbs,sizeof(cbs));
   if (e != 0) {
      cerr << "DYLTI: Problem setting callbacks: " << e << endl;
    }

   jvmti_env->SetEventNotificationMode(JVMTI_ENABLE,JVMTI_EVENT_MONITOR_CONTENDED_ENTER,NULL);
   jvmti_env->SetEventNotificationMode(JVMTI_ENABLE,JVMTI_EVENT_MONITOR_CONTENDED_ENTERED,NULL);
   jvmti_env->SetEventNotificationMode(JVMTI_ENABLE,JVMTI_EVENT_MONITOR_WAIT,NULL);
   jvmti_env->SetEventNotificationMode(JVMTI_ENABLE,JVMTI_EVENT_MONITOR_WAITED,NULL);
   jvmti_env->SetEventNotificationMode(JVMTI_ENABLE,JVMTI_EVENT_VM_DEATH,NULL);

   // cerr << "DYLTI: ATTACHED " << e << endl;
}



void
DyltiMonitorInfo::actualAttach(DyltiJni jni)
{
   if (!wait_setup) {
      wait_setup = true;
      jclass ocls = jni->FindClass("java/lang/Object");
      jmethodID mid = jni->GetMethodID(ocls,"wait","()V");
      wait_set[mid] = true;
      mid = jni->GetMethodID(ocls,"wait","(J)V");
      wait_set[mid] = true;
      mid = jni->GetMethodID(ocls,"wait","(JI)V");
      wait_set[mid] = true;
    }
}



/********************************************************************************/
/*										*/
/*	Attach methods								*/
/*										*/
/********************************************************************************/

void
DyltiMonitorInfo::attach()
{
   DyltiJni jni;

   if (!is_attached) {
      is_attached = true;

      int sts0 = java_vm->AttachCurrentThread((void **) &jni,NULL);

      initialAttach();
    }
}



/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

void
DyltiMonitorInfo::resume(jthread th)
{
   jvmti_env->ResumeThread(th);
}



/********************************************************************************/
/*										*/
/*	Methods to handle event callbacks					*/
/*										*/
/********************************************************************************/

static void
handleMonitorContendedEnter(DyltiEnv,DyltiJni jni,jthread th,jobject obj)
{
   the_monitor->processContendedEnter(jni,th,obj);
}


static void
handleMonitorContendedEntered(DyltiEnv,DyltiJni jni,jthread th,jobject obj)
{
   the_monitor->processContendedEntered(jni,th,obj);
}


static void
handleMonitorWait(DyltiEnv,DyltiJni jni,jthread th,jobject obj,jlong timeout)
{
   the_monitor->processMonitorWait(jni,th,obj,timeout);
}


static void
handleMonitorWaited(DyltiEnv,DyltiJni jni,jthread th,jobject obj,jboolean timedout)
{
   the_monitor->processMonitorWaited(jni,th,obj,timedout);
}


static void
handleVMDeath(DyltiEnv,DyltiJni)
{
   the_monitor->outputXml();
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

void
DyltiMonitorInfo::processContendedEnter(DyltiJni jni,jthread th,jobject obj)
{
   LongLong now = currentTimeNanos() - base_time;

   actualAttach(jni);

   DyltiLockData ld = getLockData(jni,obj);
   if (ld == NULL) return;

   DyltiLocation loc = getLocation(th);

   ld->contendedEnter(th,loc);

   if (lock_logger != NULL) lock_logger->contendedEnter(th,loc,now,ld,nestLevel(jni,th,ld,now));
}




void
DyltiMonitorInfo::processContendedEntered(DyltiJni jni,jthread th,jobject obj)
{
   LongLong now = currentTimeNanos() - base_time;

   DyltiLockData ld = getLockData(jni,obj);
   if (ld == NULL) return;

   DyltiLocation loc = getLocation(th);

   ld->contendedEntered(th,loc);
   if (lock_logger != NULL) lock_logger->contendedEntered(th,loc,now,ld,nestLevel(jni,th,ld,now));
}




void
DyltiMonitorInfo::processMonitorWait(DyltiJni jni,jthread th,jobject obj,jlong timeout)
{
   LongLong now = currentTimeNanos() - base_time;

   actualAttach(jni);

   DyltiLockData ld = getLockData(jni,obj);
   if (ld == NULL) return;

   DyltiLocation loc = getLocation(th);

   ld->monitorWait(th,loc);
   if (lock_logger != NULL) lock_logger->monitorWait(th,loc,now,ld,nestLevel(jni,th,ld,now));
}




void
DyltiMonitorInfo::processMonitorWaited(DyltiJni jni,jthread th,jobject obj,jboolean timedout)
{
   LongLong now = currentTimeNanos() - base_time;

   DyltiLockData ld = getLockData(jni,obj);
   if (ld == NULL) return;

   DyltiLocation loc = getLocation(th);

   ld->monitorWaited(th,loc,timedout);
   if (lock_logger != NULL) lock_logger->monitorWaited(th,loc,timedout,now,ld,nestLevel(jni,th,ld,now));
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void
DyltiMonitorInfo::outputXml()
{
   IvyXmlFileWriter xw(for_main->getOption("OUTPUT"));

   xw.begin("LOCKSET");
   for (DyltiLockListIter it = active_locks.begin(); it != active_locks.end(); ++it) {
      DyltiLockData ld = (*it);
      if (ld != NULL) ld->outputXml(this,xw);
    }
   xw.end();

   xw.close();

   if (lock_logger != NULL) lock_logger->output();
}



/********************************************************************************/
/*										*/
/*	Lock access methods							*/
/*										*/
/********************************************************************************/

DyltiLockData
DyltiMonitorInfo::getLockData(DyltiJni jni,jobject obj)
{
   jint hc;
   DyltiLockData ld = NULL;

   int tsts = jvmti_env->GetTag(obj,(jlong *)&ld);

   if (ld != NULL) {
      if (ld->getLockId() < 0) return NULL;
      return ld;
    }

   lock_mutex.lock();

   jvmti_env->GetTag(obj,(jlong *)&ld);
   if (ld == NULL) {
      jclass cls = jni->GetObjectClass(obj);
      char * cnm;
      int sts = jvmti_env->GetClassSignature(cls,&cnm,NULL);
      if (sts == 0) {
	 int id = -1;
	 if (!IGNORE_SET.ignore(cnm)) id = ++id_counter;
	 ld = new DyltiLockDataInfo(this,cnm,id);
	 jvmti_env->Deallocate((unsigned char *) cnm);

	 active_locks.pushBack(ld);
	 jvmti_env->SetTag(obj,(jlong) ld);

	 // cerr << "CREATE LOCK FOR " << cnm << " " << id << " " << endl;
       }
    }

   lock_mutex.unlock();

   if (ld->getLockId() < 0) return NULL;

   return ld;
}



int
DyltiMonitorInfo::nestLevel(DyltiJni jni,jthread th,DyltiLockData ld,LongLong now)
{
   int nlck = 0;
   jvmtiMonitorStackDepthInfo * sinfo = NULL;

   int sts = jvmti_env->GetOwnedMonitorStackDepthInfo(th,&nlck,&sinfo);
   if (sts != 0) return -1;

   int cntr = 0;
   int nlvl = 0;
   DyltiLockData nested = NULL;

   for (int i = 1; i < nlck; ++i) {
      jint hc;
      DyltiLockData nld = NULL;

      jvmti_env->GetTag(sinfo[i].monitor,(jlong *)&nld);

      if (nld == ld) {
	 ++cntr;
       }
      else if (nld != NULL) {
	 nested = nld;
	 ++nlvl;
       }
    }
   if (sinfo != NULL) jvmti_env->Deallocate((unsigned char *) sinfo);

   if (nested != NULL) ld->addPriorLock(nested);
   if (cntr == 0 && nlvl == 0) ld->noteFirstLevelLock();

   return cntr;
}



DyltiLocation
DyltiMonitorInfo::getLocation(jthread th)
{
   jvmtiFrameInfo frames[5];
   jint count;

   int sts = jvmti_env->GetStackTrace(th,0,5,frames,&count);

   if (sts != 0 || count <= 0) {
      return DyltiLocation(0,0);
    }

   int lvl = 0;
   while (lvl < count) {
      if (!wait_set[frames[lvl].method]) break;
      ++lvl;
    }

   return DyltiLocation(frames[lvl].method,frames[lvl].location);
}




StdString
DyltiMonitorInfo::getLocationText(jmethodID mid)
{
   StringBuffer rslt;

   char * mthd;
   char * sgn;
   char * clsn;
   jclass cls;

   int sts = jvmti_env->GetMethodDeclaringClass(mid,&cls);
   sts = jvmti_env->GetClassSignature(cls,&clsn,NULL);
   if (sts == 0) {
      rslt << clsn << "@";
      jvmti_env->Deallocate((unsigned char *) clsn);
    }

   sts = jvmti_env->GetMethodName(mid,&mthd,&sgn,NULL);
   if (sts == 0) {
      rslt << mthd;
      jvmti_env->Deallocate((unsigned char *)mthd);
      if (sgn != NULL) {
	 rslt << sgn;
	 jvmti_env->Deallocate((unsigned char *)sgn);
       }
    }
   else {
      cerr << "METHOD NAME STATUS: " << sts << " " << mid << endl;
      rslt << "METHOD_" << mid;
    }

   return rslt.getCString();
}




/* end of dyltimonitor.C */
