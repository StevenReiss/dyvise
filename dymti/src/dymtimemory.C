/********************************************************************************/
/*										*/
/*		dymtimemory.C							*/
/*										*/
/*	JVMTI monitor code for DYPER for memory modeling			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/dymti/src/dymtimemory.C,v 1.8 2013/09/04 18:35:05 spr Exp $ */


/*********************************************************************************
 *
 * $Log: dymtimemory.C,v $
 * Revision 1.8  2013/09/04 18:35:05  spr
 * Code cleanup for new Gcc
 *
 * Revision 1.7  2010-03-30 16:20:02  spr
 * Additional collections classes added.
 *
 * Revision 1.6  2009-09-19 00:07:57  spr
 * Bug fix in memory caching.
 *
 * Revision 1.5  2009-06-04 18:52:46  spr
 * Fix up debugging output.
 *
 * Revision 1.4  2009-05-12 22:22:23  spr
 * Fix soft pointer classes.
 *
 * Revision 1.3  2009-05-08 12:32:19  spr
 * Add softref as indirect class.
 *
 * Revision 1.2  2009-04-11 23:45:16  spr
 * Clean up monitoring.
 *
 * Revision 1.1  2009-03-20 02:10:05  spr
 * Add memory reference counting code.
 *
 *
 ********************************************************************************/

#include "dymti_local.H"




/********************************************************************************/
/*										*/
/*	Local Function References						*/
/*										*/
/********************************************************************************/

static jint heapReferenceCallback(jvmtiHeapReferenceKind,
				     const jvmtiHeapReferenceInfo *,
				     jlong,jlong,jlong,
				     jlong*,jlong*,jint,void *);

static jint heapStringCountCallback(jlong,jlong,jlong *,const jchar *,jint,void *);
static jint heapObjectCallback(jlong,jlong,jlong *,jint,void *);

static bool debug_print = false;
static bool use_two_passes = true;
static bool use_total_pass = false;
static bool show_classes = true;
static bool show_system = true;
static bool show_thread = true;
static bool show_other = false;
static bool ignore_dymon = true;
static bool try_fork = false;		// This doesn't work: java hangs in the forked process




/********************************************************************************/
/*										*/
/*	Top level method to buiild a memory model				*/
/*										*/
/********************************************************************************/

void
DymtiMonitorInfo::dumpMemoryModel(IvyXmlWriter& xw)
{
   jvmtiHeapCallbacks cntrbacks;
   jvmtiHeapCallbacks objbacks;
   jvmtiHeapCallbacks callbacks;
   JNIEnv * jni;

   int sts0 = java_vm->AttachCurrentThread((void **) &jni,NULL);
   if (sts0 != 0) {
      cerr << "DYMTI: Problem attaching thread: " << sts0 << " " << errno << endl;
      // return;
    }

   if (try_fork) {
      int pid = fork();
      if (pid != 0) return;
    }

   long long time0 = currentTimeMillis();

   callbacks.heap_iteration_callback = NULL;
   callbacks.heap_reference_callback = heapReferenceCallback;
   callbacks.primitive_field_callback = NULL;
   callbacks.array_primitive_value_callback = NULL;
   callbacks.string_primitive_value_callback = heapStringCountCallback;
   callbacks.reserved5 = NULL;

   objbacks.heap_iteration_callback = heapObjectCallback;
   objbacks.heap_reference_callback = NULL;
   objbacks.primitive_field_callback = NULL;
   objbacks.array_primitive_value_callback = NULL;
   objbacks.string_primitive_value_callback = heapStringCountCallback;
   objbacks.reserved5 = NULL;

   cntrbacks.heap_iteration_callback = NULL;
   cntrbacks.heap_reference_callback = NULL;
   cntrbacks.primitive_field_callback = NULL;
   cntrbacks.array_primitive_value_callback = NULL;
   cntrbacks.string_primitive_value_callback = heapStringCountCallback;
   cntrbacks.reserved5 = NULL;

   tagAllClasses();

   long long time1 = currentTimeMillis() - time0;

   dump_data->startPass(xw);

   cerr << "DYMTI: Start model dump" << endl;

   if (use_two_passes) {
      int sts2 = jvmti_env->IterateThroughHeap(JVMTI_HEAP_FILTER_TAGGED,NULL,&cntrbacks,dump_data);
      if (sts2 != 0) {
	 cerr << "DYMTI: Problem with heap string check: " << sts2 << endl;
       }
    }

   long long time2 = currentTimeMillis() - time0;
   long long time3 = 0;

   int savedcount = 0;
   long savedsize = 0;
   if (use_total_pass) {
      class_count.clear();
      ref_count.clear();
      total_count = 0;
      total_refs = 0;
      total_size = 0;
      int sts3 = jvmti_env->IterateThroughHeap(0,NULL,&objbacks,dump_data);
      if (sts3 != 0) {
	 cerr << "DYMTI: Problem with heap object check: " << sts3 << endl;
       }
      savedsize = total_size;
      savedcount = total_count;
      time3 = currentTimeMillis() - time0 - time2;
    }

   class_count.clear();
   ref_count.clear();
   total_count = 0;
   total_refs = 0;
   total_size = 0;

   int sts1 = jvmti_env->FollowReferences(0,NULL,NULL,&callbacks,dump_data);
   if (sts1 != 0) {
      cerr << "DYMTI: Problem with heap check: " << sts1 << endl;
    }
   jlong clsid = class_tag["Ljava/lang/Class;"];
   if (class_count[clsid].num_object == 0) {
       class_count[clsid].num_object = class_tag.size();
       class_count[clsid].total_size = class_tag.size() * class_size;
     }

   long long time = currentTimeMillis() - time0 - time2 - time3;

   cerr << "DYMTI: COUNTS: " << total_count << " " << total_refs << " " << class_count.size() <<
      " " << total_size << " " << savedcount << " " << savedsize << endl;

   xw.begin("HEAP_MODEL");
   xw.field("COUNT",total_count);
   xw.field("REFS",total_refs);
   xw.field("MEMORY",total_size);
   xw.field("SIZE",class_count.size());
   xw.field("TIME",time);
   xw.field("TAGTIME",time1);
   xw.field("STRTIME",time2);
   xw.field("NOW",time0);
   if (savedcount > 0) {
      xw.field("TOTALCOUNT",savedcount);
      xw.field("TOTALSIZE",savedsize);
      xw.field("COUNTTIME",time3);
    }

   for (DymtiClassCountIter it = class_count.begin(); it != class_count.end(); ++it) {
      StdString cls = tag_class[it.key()];
      xw.begin("CLASS");
      xw.field("NAME",cls);
      xw.field("COUNT",it.data().num_object);
      xw.field("SIZE",it.data().total_size);
      xw.field("REFS",it.data().num_refs);
      xw.field("PTRS",it.data().num_ptrs);
      xw.field("NEW",it.data().num_new);
      xw.end("CLASS");
    }
   for (DymtiClassRefCountIter it = ref_count.begin(); it != ref_count.end(); ++it) {
      xw.begin("REF");
      xw.field("FROM",tag_class[it.key().getFromIndex()]);
      xw.field("TO",tag_class[it.key().getToIndex()]);
      xw.field("COUNT",it.data());
      xw.end("REF");
    }
   xw.end("HEAP_MODEL");

   class_count.clear();
   ref_count.clear();
   total_count = 0;
   total_refs = 0;
   total_size = 0;

   cerr << "DYMTI: End model dump" << endl;
}





/********************************************************************************/
/*										*/
/*	Counter callbacks :: segregate based on size				*/
/*										*/
/********************************************************************************/

jint
heapStringCountCallback(jlong ctag,jlong siz,jlong * tptr,const jchar * v,jint vln,void * ud)
{
   DymtiDump dd = (DymtiDump) ud;

   ctag = getTagClassId(ctag);

   // cerr << "STRING COUNT " << tptr << " " << ctag << " " << vln << endl;

   if (getTagClassId(*tptr) == 0) {
      dd->setReportTag(ctag,0,tptr,vln);
      // if (vln > 1000) cerr << "STRING " << vln << " " << dd->getClassName(ctag) << " " <<
      //   dd->getClassName(*tptr) << endl;
    }

   return JVMTI_VISIT_OBJECTS;
}




/********************************************************************************/
/*										*/
/*	Object callbacks :: handle objects alone				*/
/*										*/
/********************************************************************************/

jint
heapObjectCallback(jlong ctag,jlong siz,jlong * tptr,jint len,void * ud)
{
   DymtiDump dd = (DymtiDump) ud;

   ctag = getTagClassId(ctag);

   if (ignore_dymon && dd->checkIgnore(JVMTI_HEAP_REFERENCE_OTHER,ctag,0,NULL)) return 0;

   dd->countItem(ctag,siz);

   return JVMTI_VISIT_OBJECTS;
}




/********************************************************************************/
/*										*/
/*	Reference callback							*/
/*										*/
/********************************************************************************/

jint
heapReferenceCallback(jvmtiHeapReferenceKind rk,
			 const jvmtiHeapReferenceInfo * data,
			 jlong ctag,jlong rtag,jlong size,
			 jlong * tptr,jlong * rptr,jint len,void * ud)
{
   DymtiDump dd = (DymtiDump) ud;

   ctag = getTagClassId(ctag);
   rtag = getTagClassId(rtag);

   ctag = dd->getReportTag(ctag,getTagClassId(*tptr));
   if (rptr != NULL) rtag = dd->getReportTag(rtag,getTagClassId(*rptr));

   if (ignore_dymon && dd->checkIgnore(rk,ctag,rtag,rptr)) return 0;

   if (rk == JVMTI_HEAP_REFERENCE_CLASS) {
      if (show_other) dd->countOtherRoot(ctag);
    }
   else if (rk == JVMTI_HEAP_REFERENCE_CLASS_LOADER ||
	       rk == JVMTI_HEAP_REFERENCE_SIGNERS ||
	       rk == JVMTI_HEAP_REFERENCE_PROTECTION_DOMAIN ||
	       rk == JVMTI_HEAP_REFERENCE_INTERFACE ||
	       rk == JVMTI_HEAP_REFERENCE_CONSTANT_POOL ||
	       rk == JVMTI_HEAP_REFERENCE_SUPERCLASS) {
      if (show_other) dd->countOtherRoot(ctag);
    }
   else if (rk == JVMTI_HEAP_REFERENCE_STATIC_FIELD) {
      rtag = getTagClassId(*rptr);
      ctag = dd->setReportTag(ctag,rtag,tptr,len);
      if (show_classes) dd->countClassRoot(rtag,ctag);
      else dd->countRoot(ctag);
    }
   else if (rk < 20 && ctag != 0 && rtag != 0) {
      ctag = dd->setReportTag(ctag,rtag,tptr,len);
      dd->countRef(rtag,ctag);
    }
   else if (rk >= 20 && ctag != 0 && rtag == 0) {
      if (show_system &&
	     (rk == JVMTI_HEAP_REFERENCE_SYSTEM_CLASS ||
		 rk == JVMTI_HEAP_REFERENCE_MONITOR ||
		 rk == JVMTI_HEAP_REFERENCE_THREAD ||
		 rk == JVMTI_HEAP_REFERENCE_OTHER)) {
	 dd->countSystemRoot(ctag);
       }
      else if (show_thread && rk == JVMTI_HEAP_REFERENCE_STACK_LOCAL) {
	 jvmtiHeapReferenceInfoStackLocal * sli = (jvmtiHeapReferenceInfoStackLocal *) data;
	 dd->countThreadRoot(ctag,sli->thread_id);
       }
      else if (show_thread && rk == JVMTI_HEAP_REFERENCE_JNI_LOCAL) {
	 jvmtiHeapReferenceInfoJniLocal * sli = (jvmtiHeapReferenceInfoJniLocal *) data;
	 dd->countThreadRoot(ctag,sli->thread_id);
       }
      else dd->countRoot(ctag);
    }
   else {
      /******
      StdString cs = dd->getClassName(ctag);
      StdString rs = dd->getClassName(rtag);
      cerr << "DYMTI:IGNORE " << rk << " " << cs << " " << rs << " " << rtag << " " << size << " " << len << endl;
      ******/
    }

   dd->countItem(tptr,ctag,size);

   if (debug_print) {
      StdString cs = dd->getClassName(ctag);
      StdString rs = dd->getClassName(rtag);
      cerr << "HEAP   " << rk << " " << cs << " " << rs << " " << size << " " << len << endl;
    }

   return JVMTI_VISIT_OBJECTS;
}



/********************************************************************************/
/*										*/
/*	DymtiDump methods							*/
/*										*/
/********************************************************************************/


static IvyXmlStringWriter	null_writer;


DymtiDumpInfo::DymtiDumpInfo(DymtiMonitor dm)
   : xml_writer(null_writer)
{
   dymti_monitor = dm;
   class_tag = dm->getClassTag("Ljava/lang/Class;");
   pass_count = 0;
}



void
DymtiDumpInfo::setupTags()
{
   // note this has to be called each time since new classes might have been loaded
   // and the previous pass will have returned class tag of 0

   merge_tags[dymti_monitor->getClassTag("Ljava/util/HashMap;")] = true;
   merge_tags[dymti_monitor->getClassTag("[Ljava/util/HashMap$Entry;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/util/HashMap$Entry;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/util/HashMap$EntrySet;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/util/HashMap$Values;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/util/HashMap$KeySet;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/util/Hashtable;")] = true;
   merge_tags[dymti_monitor->getClassTag("[Ljava/util/Hashtable$Entry;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/util/Hashtable$Entry;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/util/Hashtable$ValueCollection;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/util/Hashtable$KeySet;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/util/ArrayList;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/util/ArrayList$Itr;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/util/Vector;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/util/HashSet;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/util/LinkedHashMap;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/util/LinkedHashMap$Entry;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/util/Stack;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/util/LinkedHashSet;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/util/WeakHashMap;")] = true;
   merge_tags[dymti_monitor->getClassTag("[Ljava/util/WeakHashMap$Entry;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/util/WeakHashMap$Entry;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/util/LinkedList;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/util/LinkedList$Entry;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/util/PriorityQueue;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/util/IdentityHashTable;")] = true;
   merge_tags[dymti_monitor->getClassTag("[Ljava/lang/Object;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/util/TreeMap;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/util/TreeSet;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/util/TreeMap$Entry;")] = true;

   merge_tags[dymti_monitor->getClassTag("Ljava/util/Collections$UnmodfiableSet;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/util/Collections$UnmodfiableCollection;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/util/Collections$SynchronizedCollection;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/util/Collections$SynchronizedSet;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/util/Collections$SynchronizedMap;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/util/Collections$UnmodfiableRandomAccessList;")] = true;

   merge_tags[dymti_monitor->getClassTag("Ljava/util/concurrent/ConcurrentHashMap;")] = true;
   merge_tags[dymti_monitor->getClassTag("[Ljava/util/concurrent/ConcurrentHashMap$Segment;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/util/concurrent/ConcurrentHashMap$Segment;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/util/concurrent/locks/AbstractQueuedSynchronizer$Node;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/util/concurrent/DelayQueue;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/lang/ref/SoftReference;")] = true;
   merge_tags[dymti_monitor->getClassTag("Ljava/lang/ref/WeakReference;")] = true;

   merge_tags[dymti_monitor->getClassTag("Ljava/lang/ref/Finalizer$FinalizerThread;")] = true;
   merge_tags[dymti_monitor->getClassTag("Lsun/misc/Launcher$AppClassLoader;")] = true;

   merge_tags[0] = false;

   ignore_tags[dymti_monitor->getClassTag("Ledu/brown/cs/dyvise/dyper/DyperAgentCpu$PerfCounter;")] = true;
   ignore_tags[dymti_monitor->getClassTag("Ledu/brown/cs/dyvise/dyper/DyperAgentCpu;")] = true;
   ignore_tags[dymti_monitor->getClassTag("Ledu/brown/cs/dyvise/dyper/DyperAgentIO$IoCount;")] = true;
   ignore_tags[dymti_monitor->getClassTag("Ledu/brown/cs/dyvise/dyper/DyperAgentIO;")] = true;
   ignore_tags[dymti_monitor->getClassTag("Ledu/brown/cs/dyvise/dyper/DyperAgentMemory$GCStats;")] = true;
   ignore_tags[dymti_monitor->getClassTag("Ledu/brown/cs/dyvise/dyper/DyperAgentMemory$ItemCount;")] = true;
   ignore_tags[dymti_monitor->getClassTag("Ledu/brown/cs/dyvise/dyper/DyperAgentMemory$MemStats;")] = true;
   ignore_tags[dymti_monitor->getClassTag("Ledu/brown/cs/dyvise/dyper/DyperAgentMemory;")] = true;
   ignore_tags[dymti_monitor->getClassTag("Ledu/brown/cs/dyvise/dyper/DyperAgentReaction$CallbackData;")] = true;
   ignore_tags[dymti_monitor->getClassTag("Ledu/brown/cs/dyvise/dyper/DyperAgentReaction$TrieNode;")] = true;
   ignore_tags[dymti_monitor->getClassTag("Ledu/brown/cs/dyvise/dyper/DyperAgentReaction;")] = true;
   ignore_tags[dymti_monitor->getClassTag("Ledu/brown/cs/dyvise/dyper/DyperAgentSocket$SocketCount;")] = true;
   ignore_tags[dymti_monitor->getClassTag("Ledu/brown/cs/dyvise/dyper/DyperAgentSocket;")] = true;
   ignore_tags[dymti_monitor->getClassTag("Ledu/brown/cs/dyvise/dyper/DyperAgentStates$TrieNode;")] = true;
   ignore_tags[dymti_monitor->getClassTag("Ledu/brown/cs/dyvise/dyper/DyperAgentStates;")] = true;
   ignore_tags[dymti_monitor->getClassTag("Ledu/brown/cs/dyvise/dyper/DyperAgentThreads$LockInfo;")] = true;
   ignore_tags[dymti_monitor->getClassTag("Ledu/brown/cs/dyvise/dyper/DyperAgentThreads;")] = true;
   ignore_tags[dymti_monitor->getClassTag("Ledu/brown/cs/dyvise/dyper/DyperAgentTiming$TimeInfo;")] = true;
   ignore_tags[dymti_monitor->getClassTag("Ledu/brown/cs/dyvise/dyper/DyperAgentTiming;")] = true;
   ignore_tags[dymti_monitor->getClassTag("Ledu/brown/cs/dyvise/dyper/DyperConstants;")] = true;
   ignore_tags[dymti_monitor->getClassTag("Ledu/brown/cs/dyvise/dyper/DyperConstants$ClassType;")] = true;
   ignore_tags[dymti_monitor->getClassTag("Ledu/brown/cs/dyvise/dyper/DyperConstants$CounterParameter;")] = true;
   ignore_tags[dymti_monitor->getClassTag("Ledu/brown/cs/dyvise/dyper/DyperConstants$ReportType;")] = true;
   ignore_tags[dymti_monitor->getClassTag("Ledu/brown/cs/dyvise/dyper/DyperControl$CmdHandler;")] = true;
   ignore_tags[dymti_monitor->getClassTag("Ledu/brown/cs/dyvise/dyper/DyperControl$StartupHandler;")] = true;
   ignore_tags[dymti_monitor->getClassTag("Ledu/brown/cs/dyvise/dyper/DyperControl$Shutdown;")] = true;
   ignore_tags[dymti_monitor->getClassTag("Ledu/brown/cs/dyvise/dyper/DyperControl$WhoHandler;")] = true;
   ignore_tags[dymti_monitor->getClassTag("Ledu/brown/cs/dyvise/dyper/DyperControl;")] = true;
   ignore_tags[dymti_monitor->getClassTag("Ledu/brown/cs/dyvise/dyper/DyperInstrumenter$ClassData;")] = true;
   ignore_tags[dymti_monitor->getClassTag("Ledu/brown/cs/dyvise/dyper/DyperInstrumenter;")] = true;
   ignore_tags[dymti_monitor->getClassTag("Ledu/brown/cs/dyvise/dyper/DyperMonitor$ClassMonitor;")] = true;
   ignore_tags[dymti_monitor->getClassTag("Ledu/brown/cs/dyvise/dyper/DyperMonitor$DyperShutdown;")] = true;
   ignore_tags[dymti_monitor->getClassTag("Ledu/brown/cs/dyvise/dyper/DyperMonitor;")] = true;

   ignore_tags[0] = false;
}






void
DymtiDumpInfo::startPass(IvyXmlWriter & xw)
{
   xml_writer = xw;
   ++pass_count;
   if (pass_count >= DYMTI_MAX_PASS) pass_count = 1;
   setupTags();
}


jlong
DymtiDumpInfo::getReportTag(jlong ctag,jlong otag)
{
   if (otag == 0 || ctag == class_tag) return ctag;

   return otag;
}



jlong
DymtiDumpInfo::setReportTag(jlong ctag,jlong rtag,jlong * tptr,jlong size)
{
   jlong xtag = getTagClassId(*tptr);

   if (xtag != 0) return xtag;

   long sz = 0;
   if (size >= MIN_ARRAY_SIZE) {
      sz = MIN_ARRAY_SIZE;
      while (size > sz * ARRAY_SCALE) sz *= ARRAY_SCALE;
    }

   if (merge_tags[ctag]) {
      if (rtag == 0) return ctag;
      else if (merge_tags[rtag]) return ctag;
    }
   else rtag = 0;

   if (rtag == 0 && sz == 0) return ctag;

   ctag = dymti_monitor->getMergedClassTag(rtag,ctag,sz);

   setTagClassId(tptr,ctag);

   return ctag;
}




bool
DymtiDumpInfo::checkIgnore(jvmtiHeapReferenceKind rk,jlong ctag,jlong rtag,jlong * rptr)
{
   if (rk >= 20) {
      if (ignore_tags[ctag]) return true;
    }
   else if (rk == JVMTI_HEAP_REFERENCE_STATIC_FIELD) {
      rtag = getTagClassId(*rptr);
      if (ignore_tags[ctag] || ignore_tags[rtag]) return true;
    }
   else if (rk == JVMTI_HEAP_REFERENCE_CLASS) {
      if (ignore_tags[rtag]) return true;
    }
   else if (ctag != 0 && rtag != 0) {
      if (ignore_tags[ctag] || ignore_tags[rtag]) return true;
    }

   return false;
}




void
DymtiDumpInfo::countItem(jlong * tptr,jlong tag,jlong size)
{
   if (tptr == NULL) return;

   int tct = getTagCount(*tptr);

   if (tct == pass_count) return;

   dymti_monitor->countItem(tag,size,tct == 0);

   setTagCount(tptr,pass_count);
}



void
DymtiDumpInfo::countClassRoot(jlong ftag,jlong ttag)
{
   jlong ntag = dymti_monitor->getStaticClassTag(ftag);

   if (ntag == 0) countRoot(ttag);
   else {
      countRef(ntag,ttag);
      countRoot(ntag);
    }
}



void
DymtiDumpInfo::countSystemRoot(jlong ttag)
{
   jlong ftag = dymti_monitor->getClassTag(DYMTI_SYSTEM_CLASS);
   countRef(ftag,ttag);
   countRoot(ftag);
}



void
DymtiDumpInfo::countOtherRoot(jlong ttag)
{
   jlong ftag = dymti_monitor->getClassTag(DYMTI_OTHER_CLASS);
   countRef(ftag,ttag);
   countRoot(ftag);
}



void
DymtiDumpInfo::countThreadRoot(jlong ttag,jlong threadid)
{
   jlong ntag = dymti_monitor->getThreadTag(threadid);

   if (ntag == 0) countRoot(ttag);
   else {
      countRef(ntag,ttag);
      countRoot(ntag);
    }
}



/* end of dymtimemory.C */





