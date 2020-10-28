/********************************************************************************/
/*										*/
/*		dymtimonitor.C							*/
/*										*/
/*	JVMTI monitor code for DYPER						*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/dymti/src/dymtimonitor.C,v 1.9 2011-03-10 02:32:36 spr Exp $ */


/*********************************************************************************
 *
 * $Log: dymtimonitor.C,v $
 * Revision 1.9  2011-03-10 02:32:36  spr
 * Clean up code
 *
 * Revision 1.8  2009-10-07 23:08:46  spr
 * Handle case where jlong = long long
 *
 * Revision 1.7  2009-06-04 18:52:46  spr
 * Fix up debugging output.
 *
 * Revision 1.6  2009-05-12 22:22:23  spr
 * Fix soft pointer classes.
 *
 * Revision 1.5  2009-04-20 23:23:30  spr
 * Updates to make things work on the mac.  Fix bug in dymti.
 *
 * Revision 1.4  2009-04-11 23:45:16  spr
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

#include "dymti_local.H"




/********************************************************************************/
/*										*/
/*	Local function definitions						*/
/*										*/
/********************************************************************************/

static void handleBreakpoint(jvmtiEnv *,JNIEnv *,jthread th,jmethodID m,jlocation l);



/********************************************************************************/
/*										*/
/*	Local Storage								*/
/*										*/
/********************************************************************************/

static DymtiMonitor		the_monitor = NULL;



/********************************************************************************/
/*										*/
/*	Constructors/destructors						*/
/*										*/
/********************************************************************************/

DymtiMonitorInfo::DymtiMonitorInfo(DymtiMain m,JavaVM * jvm,DymtiEnv env)
  : class_count(1024), tag_class(1024), class_tag(1024), ref_count(4096)
{
   for_main = m;
   java_vm = jvm;
   jvmti_env = env;
   suspended_threads = NULL;
   suspend_count = 0;
   is_attached = false;
   total_count = 0;
   total_size = 0;
   total_refs = 0;
   next_tag = 1;
   class_size = 0;
   root_tag = tagClass(DYMTI_ROOT_CLASS);
   tagClass(DYMTI_SYSTEM_CLASS);
   tagClass(DYMTI_OTHER_CLASS);
   dump_data = new DymtiDumpInfo(this);

   the_monitor = this;
}




DymtiMonitorInfo::~DymtiMonitorInfo()
{ }



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

void
DymtiMonitorInfo::initialSetup(bool attach)
{
   if (attach && !is_attached) {
      is_attached = true;
      initialAttach();
    }
}



void
DymtiMonitorInfo::initialAttach()
{
   jvmtiEventCallbacks cbs;
   jvmtiCapabilities cap;
   jvmtiError e;

   e = jvmti_env->GetPotentialCapabilities(&cap);

   e = jvmti_env->GetCapabilities(&cap);

   cap.can_tag_objects = 1;			// needed to iterate over heap
   cap.can_generate_field_modification_events = 0;
   cap.can_generate_field_access_events = 0;
   cap.can_get_bytecodes = 0;
   cap.can_get_synthetic_attribute = 0;
   cap.can_get_owned_monitor_info = 0;
   cap.can_get_current_contended_monitor = 0;
   cap.can_get_monitor_info = 0;
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
   cap.can_generate_breakpoint_events = 0;
// cap.can_suspend = 1;
   cap.can_suspend = 0;
   cap.can_redefine_any_class = 0;
   cap.can_get_current_thread_cpu_time = 0;
   cap.can_get_thread_cpu_time = 0;
   cap.can_generate_method_entry_events = 0;
   cap.can_generate_method_exit_events = 0;
   cap.can_generate_all_class_hook_events = 0;
   cap.can_generate_compiled_method_load_events = 0;
   cap.can_generate_monitor_events = 0;
   cap.can_generate_vm_object_alloc_events = 0;
   cap.can_generate_native_method_bind_events = 0;
   cap.can_generate_garbage_collection_events = 0;
   cap.can_generate_object_free_events = 0;

   e = jvmti_env->AddCapabilities(&cap);
   if (e != 0) {
      cerr << "DYMTI: Problem setting capabilities: " << e << endl;
      // this can fail if sharing is enabled -- won't be able to iterate over heap
    }

   cbs.VMInit = NULL;
   cbs.VMDeath = NULL;
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
   cbs.Breakpoint = handleBreakpoint;
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
   cbs.MonitorWait = NULL;
   cbs.MonitorWaited = NULL;
   cbs.MonitorContendedEnter = NULL;
   cbs.MonitorContendedEntered = NULL;
   cbs.reserved77 = NULL;
   cbs.reserved78 = NULL;
   cbs.reserved79 = NULL;
   // cbs.ResourceExhausted = NULL;
   cbs.GarbageCollectionStart = NULL;
   cbs.GarbageCollectionFinish = NULL;
   cbs.ObjectFree = NULL;
   cbs.VMObjectAlloc = NULL;

   e = jvmti_env->SetEventCallbacks(&cbs,sizeof(cbs));

   // jvmti_env->SetVerboseFlag(JVMTI_VERBOSE_CLASS,true);
   // jvmti_env->SetVerboseFlag(JVMTI_VERBOSE_GC,true);

   cerr << "DYMTI: ATTACHED" << endl;
}




/********************************************************************************/
/*										*/
/*	Parameter methods							*/
/*										*/
/********************************************************************************/

void
DymtiMonitorInfo::setParameter(CStdString nm,CStdString val)
{
}



/********************************************************************************/
/*										*/
/*	Attach methods								*/
/*										*/
/********************************************************************************/

void
DymtiMonitorInfo::attach()
{
   JNIEnv * jni;

   if (!is_attached) {
      is_attached = true;

      int sts0 = java_vm->AttachCurrentThread((void **) &jni,NULL);
      jclass cls = jni->FindClass("edu/brown/cs/dyvise/dyper/DyperControl");
      jmethodID mid = jni->GetStaticMethodID(cls,"dymtimain","()V");
      jni->CallStaticVoidMethod(cls,mid);

      initialAttach();
    }
}



/********************************************************************************/
/*										*/
/*	Heap Checking methods							*/
/*										*/
/********************************************************************************/

void
DymtiMonitorInfo::countItem(jlong tag,jlong sz,bool isnew)
{
   if (tag != 0) {
      DymtiClassData * cd = &class_count[tag];
      cd->num_object++;
      cd->total_size += sz;
      if (isnew) cd->num_new++;
    }
   ++total_count;
   total_size += sz;
}


void
DymtiMonitorInfo::countRef(jlong ftag,jlong ttag)
{
   if (ftag != 0 && ttag != 0) {
      ref_count[DymtiRef(ftag,ttag)]++;
      class_count[ttag].num_refs++;
      class_count[ftag].num_ptrs++;
    }
   ++total_refs;
}




/********************************************************************************/
/*										*/
/*	Methods to handle allocation monitoring 				*/
/*										*/
/********************************************************************************/

void
DymtiMonitorInfo::allocMonitor(IvyXmlNode msg)
{
}



/********************************************************************************/
/*										*/
/*	Methods to handle breakpoints						*/
/*										*/
/********************************************************************************/

static void
handleBreakpoint(jvmtiEnv * env,JNIEnv * jni,jthread th,jmethodID m,jlocation l)
{
}



/********************************************************************************/
/*										*/
/*	Methods to handle stop/resume threads					*/
/*										*/
/********************************************************************************/

void
DymtiMonitorInfo::stopThreads()
{
   jvmtiError sts;
   jint ctr = 0;
   jthread * thrds;
   jvmtiError * errs;

   sts = jvmti_env->GetAllThreads(&ctr,&thrds);

   if (sts != 0 || ctr == 0) {
      cerr << "DYMTI: Problem getting threads: " << sts << " " << ctr << endl;
      return;
    }

   fixCurrentThread(ctr,thrds);

   errs = new jvmtiError[ctr-1];

   sts = jvmti_env->SuspendThreadList(ctr-1,thrds,errs);
   if (sts != 0) {
      cerr << "DYMTI: Problem suspending threads: " << sts << endl;
      return;
    }
   for (int i = 0; i < ctr-1; ++i) {
      if (errs[i] != 0) {
	 cerr << "DYMTI: Problem suspending thread " << thrds[i] << ": " << errs[i] << endl;
       }
    }

   delete [] errs;

   suspended_threads = thrds;
   suspend_count = ctr;
}



void
DymtiMonitorInfo::resumeThreads()
{
   if (suspended_threads == NULL) return;

   jvmtiError sts;
   jvmtiError * errs = new jvmtiError[suspend_count-1];

   sts = jvmti_env->ResumeThreadList(suspend_count-1,suspended_threads,errs);

   if (sts != 0) {
      cerr << "DYMTI: Problem resuming threads: " << sts << endl;
      return;
    }
   for (int i = 0; i < suspend_count-1; ++i) {
      if (errs[i] != 0) {
	 cerr << "DYMTI: Problem resuming thread " << suspended_threads[i] << ": " << errs[i] << endl;
       }
    }
   delete [] errs;

   // free suspended_threads array

   suspended_threads = NULL;
   suspend_count = 0;
}




void
DymtiMonitorInfo::fixCurrentThread(int ctr,jthread * thrds)
{
   jvmtiError sts;
   jint fctr;
   jvmtiFrameInfo fbuf[1];
   char * nm;
   char * sg;

   jthread last = thrds[ctr-1];
   for (int i = 0; i < ctr; ++i) {
      sts = jvmti_env->GetStackTrace(thrds[i],0,1,fbuf,&fctr);
      if (fctr == 0) continue;
      jvmti_env->GetMethodName(fbuf[0].method,&nm,&sg,NULL);
      if (strcmp(nm,"nativeCheckpoint") == 0) {
	 thrds[ctr-1] = thrds[i];
	 thrds[i] = last;
	 last = NULL;
	 break;
       }
    }
   if (last != NULL) {
      cerr << "DYMTI: Current thread not found" << endl;
    }
}




/********************************************************************************/
/*										*/
/*	Tag maintenance methods 						*/
/*										*/
/********************************************************************************/

void
DymtiMonitorInfo::tagAllClasses()
{
   jint cnt;
   jclass * clss;
   char * sgn;

   clss = NULL;
   cnt = 0;

   int sts = jvmti_env->GetLoadedClasses(&cnt,&clss);
   if (sts != 0) {
      cerr << "DYMTI: Problem getting loaded classes: " << sts << endl;
      return;
    }
   if (cnt > 0 && class_size == 0) {
      int sts = jvmti_env->GetObjectSize(clss[0],&class_size);
    }

   for (int i = 0; i < cnt; ++i) {
      jvmti_env->GetClassSignature(clss[i],&sgn,NULL);
      if (sgn == NULL) continue;
      StdString s = sgn;
      jlong tag = class_tag[s];
      if (tag == 0) {
	 tag = createNewTag(tagClass(s));
	 int sts = jvmti_env->SetTag(clss[i],tag);
       }
      jvmti_env->Deallocate((unsigned char *) sgn);
    }
   jvmti_env->Deallocate((unsigned char *) clss);
}


static CStdString unknown_class = "*";


CStdString
DymtiMonitorInfo::getClassName(jlong tag)
{
   if (tag == 0) return unknown_class;

   return tag_class[tag];
}




jlong
DymtiMonitorInfo::getClassTag(CStdString cls)
{
   return class_tag[cls];
}




jlong
DymtiMonitorInfo::tagClass(CStdString cls)
{
   jlong tag = next_tag++;
   tag_class[tag] = cls;
   class_tag[cls] = tag;

   return tag;
}



static const char * end_strings [] = {
   "Ljava/util/LinkedList$Entry;:",
   "Ljava/util/TreeMap$Entry;:",
   "Ljava/util/HashMap$Entry;:",
   "Ljava/util/Hashtable$Entry;:",
   "Ljava/util/LinkedHashtable$Entry;:",
   "Ljava/util/WeakHashMap$Entry;:" };
static int num_end_strings = 5;


jlong
DymtiMonitorInfo::getMergedClassTag(jlong referer,jlong referee,jlong size)
{
   StdString newname = tag_class[referee];

   if (newname == "") {
      cerr << "DYMTI: NO CLASS NAME FOR TAG " << referee << endl;
    }

   if (referer != 0) {
      StdString ref = tag_class[referer];
      for (int i = 0; i < num_end_strings; ++i) {
	 int sz = strlen(end_strings[i]);
	 if (ref.compare(0,sz,end_strings[i],0,sz) == 0) {
	    ref = ref.substr(sz);
	    break;
	  }
       }
      newname += ":" + ref;
    }

   if (size > 0) {
      char buf[1024];
      long lsz = (long) size;
      sprintf(buf,"*%ld",lsz);
      if (newname[0] == '[') {
	 newname.insert(1,buf);
       }
      else if (newname[newname.length()-1] == ';') {
	 newname.insert(newname.length()-1,buf);
       }
      else newname += buf;
    }

   jlong tag = class_tag[newname];
   if (tag == 0) {
      tag = tagClass(newname);
    }

   return tag;
}


jlong
DymtiMonitorInfo::getStaticClassTag(jlong ctag)
{
   StdString newname = tag_class[ctag];

   if (newname.compare(0,6,"CLASS*",0,6) == 0) return ctag;

   newname = "CLASS*" + newname;

   jlong tag = class_tag[newname];
   if (tag == 0) {
      tag = tagClass(newname);
    }

   return tag;
}




jlong
DymtiMonitorInfo::getThreadTag(jlong thid)
{
   char buf[1024];
   long lth = (long) thid;
   sprintf(buf,"*%ld",lth);

   StdString newname = "THREAD";
   newname += buf;

   jlong tag = class_tag[newname];
   if (tag == 0) {
      tag = tagClass(newname);
    }

   return tag;
}




/* end of dymtimonitor.C */


