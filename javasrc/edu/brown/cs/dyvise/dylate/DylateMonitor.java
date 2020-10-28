/********************************************************************************/
/*										*/
/*		DylateMonitor.java						*/
/*										*/
/*	Monitor entry points for DYnamic Lock UTilization Experiencer		*/
/*										*/
/********************************************************************************/
/*	Copyright 2007 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2007, Brown University, Providence, RI.				 *
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylate/DylateMonitor.java,v 1.2 2013-05-09 12:28:58 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylateMonitor.java,v $
 * Revision 1.2  2013-05-09 12:28:58  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.1  2012-10-05 00:52:26  spr
 * New lock tracer
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dylate;

// NOTE: This should only use system libraries and libraries that are explicitly
//    in this package.	Using anything else might cause conflicts if the application
//    being monitored uses the same library.

import java.io.*;
import java.util.*;
import java.lang.ref.*;
import java.util.concurrent.atomic.AtomicInteger;



public class DylateMonitor implements DylateConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

enum LockMode { IGNORE, USE, RESET, STOP };

private static final int LAST_CT = 2;



private long		start_time;
private TraceSet []	trace_sets;
private int		current_set;
private int		max_size;
private Writer		output_writer;
private Writer		info_writer;
private Reporter	trace_thread;
private boolean 	force_end;
private boolean 	full_trace;

private ItemMap<Object,LockData> lock_items;
private ThreadLocal<ThreadData> thread_data;

private static DylateMonitor	the_monitor = new DylateMonitor();

private static AtomicInteger lock_counter = new AtomicInteger();


private static final int	NUM_SETS = 10;
private static final int	ENTRY_COUNT = 1024;
private static final long	TIME_DELAY = 100;
private static final long	RESET_TIME = 3000000000L;	// 3 second(s)
private static final int	SAVE_SIZE = 3;

private static Set<String> ignore_set;

static {
   ignore_set = new HashSet<String>();
   ignore_set.add("java.lang.ref.ReferenceQueue$Lock");
   ignore_set.add("java.awt.EventQueue");
   ignore_set.add("java.awt.EventQueue$1AWTInvocationLock");
   ignore_set.add("edu.brown.cs.bubbles.buda.BudaRoot$MouseEventQueue");
   ignore_set.add("sun.misc.Launcher$AppClassLoader");
   ignore_set.add("java.util.TaskQueue");
   ignore_set.add("javax.swing.TimerQueue");
   ignore_set.add("sun.awt.AWTAutoShutdown");
   ignore_set.add("sun.awt.X11GraphicsEnvironment");
}


enum Command {
   THREAD,
   DONE,
   TIME,
   ENTER,
   ENTERED,
   NOTIFY,
   WAIT,
   WAITED,
   UNLOCK,
   NOLOCK,
   RESET,
   PREJOIN,
   NOJOIN,
   JOIN
}




/********************************************************************************/
/*										*/
/*	Static Entry points							*/
/*										*/
/********************************************************************************/

public static void monEnter(Object mon,int loc)
{
   the_monitor.addEntry(Command.ENTER,mon,loc);
}


public static void monEntered(Object mon,int loc)
{
   the_monitor.addEntry(Command.ENTERED,mon,loc);
}



public static void monExit(Object mon)
{
   the_monitor.addEntry(Command.UNLOCK,mon,0);
}



public static void monNotify(Object mon,int loc)
{
   the_monitor.addEntry(Command.NOTIFY,mon,loc);
}



public static void monWait(Object mon,int loc)
{
   the_monitor.addEntry(Command.WAIT,mon,loc);
}



public static void monWaited(Object mon)
{
   the_monitor.addEntry(Command.WAITED,mon,0);
}


public static  void monWaitTimed(Object mon,long t0,int t1,int loc) throws InterruptedException
{
   monWait(mon,loc);
   try {
      mon.wait(t0,t1);
    }
   finally {
      monWaited(mon);
    }
}


public static void monAwaitTimed(java.util.concurrent.locks.Condition mon,long v,java.util.concurrent.TimeUnit u,int loc)
	throws InterruptedException
{
   monWait(mon,loc);
   try {
      mon.await(v,u);
    }
   finally {
      monWaited(mon);
    }
}


public static void monAwaitBarrierTimed(java.util.concurrent.CyclicBarrier mon,long v,java.util.concurrent.TimeUnit u,int loc)
	throws java.util.concurrent.TimeoutException, java.util.concurrent.BrokenBarrierException,
	InterruptedException
{
   monWait(mon,loc);
   try {
      mon.await(v,u);
    }
   finally {
      monWaited(mon);
    }
}


public static void monAwaitLatchTimed(java.util.concurrent.CountDownLatch mon,long v,java.util.concurrent.TimeUnit u,int loc)
throws InterruptedException
{
   monWait(mon,loc);
   try {
      mon.await(v,u);
    }
   finally {
      monWaited(mon);
    }
}

public static long monAwaitNanos(java.util.concurrent.locks.Condition mon,long v,int loc)
	throws InterruptedException
{
   monWait(mon,loc);
   try {
      return mon.awaitNanos(v);
    }
   finally {
      monWaited(mon);
    }
}

public static void monPreLock(Object mon,int loc)
{
   the_monitor.addEntry(Command.ENTER,mon,loc);
}


public static void monLock(Object mon,boolean succ,int loc)
{
   if (succ)
      the_monitor.addEntry(Command.ENTERED,mon,loc);
   else
      the_monitor.addEntry(Command.NOLOCK,mon,loc);
}


public static boolean monTry(java.util.concurrent.locks.Lock mon,int loc)
{
   monEnter(mon,loc);

   boolean sts = mon.tryLock();

   monLock(mon,sts,loc);

   return sts;
}


public static boolean monTryTimed(java.util.concurrent.locks.Lock mon,long v,
				     java.util.concurrent.TimeUnit u,int loc)
	throws InterruptedException
{
   monEnter(mon,loc);
   boolean sts = false;

   try {
      sts = mon.tryLock(v,u);
      return sts;
    }
   finally {
      monLock(mon,sts,loc);
    }
}



public static void monUnlock(Object mon,int loc)
{
   the_monitor.addEntry(Command.UNLOCK,mon,loc);
}


public static void monAssoc(Object l1,Object l2)
{
   the_monitor.assocLocks(l1,l2);
}



public static void monJoin(Thread t,long millis,int nanos,int loc)
	throws InterruptedException
{
   the_monitor.addEntry(Command.PREJOIN,t,loc);
   try {
      if (millis == 0 && nanos == 0) {
	 t.join();
       }
      else {
	 t.join(millis,nanos);
       }
    }
   finally {
      if (t.isAlive()) the_monitor.addEntry(Command.NOJOIN,t,loc);
      else the_monitor.addEntry(Command.JOIN,t,loc);
    }
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/


static DylateMonitor getMonitor()		{ return the_monitor; }



private DylateMonitor()
{
   start_time = System.nanoTime();
   max_size = ENTRY_COUNT * 32;
   lock_items = new ItemMap<Object,LockData>(new Object[LAST_CT],new LockData[LAST_CT]);
   thread_data = new ThreadLocal<ThreadData>();

   trace_sets = new TraceSet[NUM_SETS];
   for (int i = 0; i < NUM_SETS; ++i) trace_sets[i] = new TraceSet();
   current_set = 0;
   output_writer = null;
   trace_thread = null;
   force_end = false;
   full_trace = false;
}




/********************************************************************************/
/*										*/
/*	Access Methods								*/
/*										*/
/********************************************************************************/

synchronized void setOutputWriter(Writer w,Writer infow)
{
   output_writer = w;
   info_writer = infow;

   if (trace_thread == null) {
      trace_thread = new Reporter();
      trace_thread.start();
      Runtime.getRuntime().addShutdownHook(new Finisher());
    }
}



void setFull(boolean fg)
{
   System.err.println("DYLATE: Full trace");
   full_trace = fg;
}



/********************************************************************************/
/*										*/
/*	Trace entry points							*/
/*										*/
/********************************************************************************/

private void addEntry(Command cmd,Object mon,int loc)
{
   // System.err.println("DYLATEMON: " + cmd + " " + loc);

   try {
      ThreadData td = getThreadData();
      if (td == null) return;
      LockData ld = getLockData(mon,td);
      if (ld.isIgnored()) return;

      switch (cmd) {
	 case ENTERED :
	    LockData ld1 = td.addLock(ld,loc);
	    if (ld1 != null) {
	       String rslt = "PRIOR|" + ld.getId() + "|" + ld1.getId() + "\n";
	       writeInfo(rslt);
	     }
	    break;
	 case UNLOCK :
	    if (loc == 0) loc = td.getLocation(ld);	// need to do this before removal
	    td.removeLock(ld);
	    break;
	 default :
	    break;
       }

      long time = System.nanoTime() - start_time;
      ld.updateCounts(cmd,time);

      LockMode md = LockMode.USE;
      if (!full_trace) {
	 md = ld.setThread(td,time,cmd);
	 if (md == LockMode.IGNORE) return;
       }

      String locstr = null;
      if (loc == 0) {
	 loc = td.getLocation(ld);
	 if (loc == 0) return;
       }
      if (loc < 0) {
	 int loc0 = td.getLocation(ld);
	 if (loc0 <= 0) return;
	 locstr = Integer.toString(loc0) + Integer.toString(loc);
       }
      else locstr = Integer.toString(loc);

      if (!full_trace) {
	 switch (md) {
	    case STOP :
	       trace_sets[current_set].addEntry(td,Command.RESET,ld,locstr,time-1);
	       return;
	    case RESET :
	       trace_sets[current_set].addEntry(td,Command.RESET,ld,locstr,time-1);
	       break;
	    case IGNORE :
	       return;
	    case USE :
	       break;
	  }
       }

      trace_sets[current_set].addEntry(td,cmd,ld,locstr,time);
    }
   catch (Throwable t) {
      System.err.println("Problem tracing: " + t);
      t.printStackTrace();
    }

}



private void writeInfo(String rslt)
{
   synchronized (info_writer) {
      try {
	 info_writer.write(rslt);
	 info_writer.flush();
       }
      catch (IOException e) { }
    }
}






private synchronized void collectStatistics()
{
   int next = (current_set + 1) % NUM_SETS;
   int prior = (current_set + NUM_SETS - 1) % NUM_SETS;
   trace_sets[next].reset();
   TraceSet eval = trace_sets[prior];
   current_set = next;

   try {
      boolean fg = false;
      if (output_writer != null) {
	 fg = eval.output();
       }
      if (fg || force_end || full_trace) {
	 output_writer.append("ENDBLOCK|0|0|0|0|0|0\n");
	 output_writer.flush();
       }
      force_end = fg;
    }
   catch (IOException e) {
      output_writer = null;
    }
}



private void finishTrace()
{
   trace_thread.interrupt();
   try {
      trace_thread.join(3000);
    }
   catch (InterruptedException e) { }

   for (int i = 0; i < NUM_SETS; ++i) {
      collectStatistics();
    }

   for (LockData ld : lock_items.values()) {
      if (ld.getId() <= 0) continue;
      String st = ld.outputStats();
      if (st != null) writeInfo(st);
    }
}



/********************************************************************************/
/*										*/
/*	Trace Set -- top level holder of current trace information		*/
/*										*/
/********************************************************************************/

private class TraceSet {

   private WeakHashMap<ThreadData,ThreadSet>	buffer_map;
   private ThreadData	last_key;
   private ThreadSet	last_set;

   TraceSet() {
      buffer_map = new WeakHashMap<ThreadData,ThreadSet>();
      last_key = null;
      last_set = null;
    }

   void addEntry(ThreadData td,Command cmd,LockData ld,String loc,long time) {
      if (output_writer == null) return;
      StringBuilder buf = new StringBuilder();
      buf.append(cmd);
      buf.append("|");
      buf.append(td.getId());
      buf.append("|");
      buf.append(loc);
      buf.append("|");
      buf.append(time);
      buf.append("|");
      buf.append(ld.getId());
      buf.append("|");
      buf.append(td.getLevel(ld));
      buf.append("|");
      buf.append(td.getLevelCount());
      buf.append("\n");

      // System.err.println("ADD ENTRY: " + buf);

      getThreadSet(td).addEntry(buf);
    }

   void reset() {
      synchronized (this) {
	 for (Map.Entry<ThreadData,ThreadSet> ent : buffer_map.entrySet()) {
	    ThreadSet ts = ent.getValue();
	    ts.reset();
	  }
       }
    }

   boolean output() throws IOException {
      boolean fg = false;
      synchronized (this) {
	 for (Map.Entry<ThreadData,ThreadSet> ent : buffer_map.entrySet()) {
	    ThreadSet ts = ent.getValue();
	    if (ts.containsData()) {
	       fg = true;
	       ts.output();
	     }
	  }
       }
      return fg;
    }

   ThreadSet getThreadSet(ThreadData th) {
      if (last_key == th) return last_set;

      ThreadSet ts = buffer_map.get(th);
      if (ts == null) {
	 ts = new ThreadSet();
	 synchronized (this) {
	    buffer_map.put(th,ts);
	  }
       }
      last_key = th;
      last_set = ts;
      return ts;
    }

}	// end of inner class TraceSet




/********************************************************************************/
/*										*/
/*	Thread Set -- holder of trace entries for a thread			*/
/*										*/
/********************************************************************************/

private class ThreadSet {

   private StringBuilder output_data;

   ThreadSet() {
      output_data = new StringBuilder();
      output_data.ensureCapacity(max_size);
    }

   synchronized void addEntry(CharSequence s) {
      output_data.append(s);
    }

   boolean containsData() {
      return output_data.length() > 0;
    }

   synchronized void reset() {
      int ln = output_data.length();
      if (ln+10 > max_size) max_size = ln+10;
      // output_data.delete(0,ln);
      output_data = new StringBuilder();
    }

   synchronized void output() throws IOException {
      try {
	 output_writer.append(output_data);
       }
      catch (StringIndexOutOfBoundsException e) {
	 output_writer.append(output_data);
       }
    }

}	// end of inner class ThreadSet



/********************************************************************************/
/*										*/
/*	Reporting thread							*/
/*										*/
/********************************************************************************/

private class Reporter extends Thread {

   private long time_delay;

   Reporter() {
      super("DylateReporter");
      setPriority(Thread.MAX_PRIORITY);
      time_delay = TIME_DELAY;
      setDaemon(true);
    }

   @Override public void run() {
      thread_data.set(new ThreadData(-1));

      try {
	 for ( ; ; ) {
	    if (isInterrupted()) break;
	    try {
	       sleep(time_delay);
	       setPriority(Thread.MAX_PRIORITY);
	     }
	    catch (InterruptedException e) {
	       break;
	     }
	    // System.err.println("REPORTER: COLLECT");
	    collectStatistics();
	  }
       }
      catch (Throwable t) {
	 System.err.println("DYLATE: Reporter aborted: " + t);
	 t.printStackTrace();
       }
    }

}	// end of inner class Reporter



/********************************************************************************/
/*										*/
/*	Shutdown thread 							*/
/*										*/
/********************************************************************************/

private class Finisher extends Thread {

   @Override public void run() {
      finishTrace();
      // System.err.println("REPORTER: FINISH");
    }

}	// end of inner class Finisher




/********************************************************************************/
/*										*/
/*	Lock information							*/
/*										*/
/********************************************************************************/

private LockData getLockData(Object o,ThreadData td)
{
   LockData ld = null;
   ld = td.getLock(o);
   if (ld != null) return ld;

   ld = lock_items.get(o);
   if (ld == null) {
      LockData ld1 = null;
      synchronized (lock_items) {
	 ld1 = new LockData();
       }
      ld = lock_items.putIfAbsent(o,ld1);
      if (ld == null) {
	 ld = ld1;
	 if (ignore_set.contains(o.getClass().getName())) ld.setIgnored();
	 else {
	    String rslt = "MONITOR|" + ld.getId() + "|" + o.getClass().getName() + "|" +
	       System.identityHashCode(o) + "|" +
	       (System.nanoTime() - start_time) + "\n";
	    writeInfo(rslt);
	  }
       }
    }

   td.saveLock(o,ld);

   return ld;
}

private void assocLocks(Object o1,Object o2)
{
   ThreadData td = getThreadData();

   LockData ld = getLockData(o1,td);
   lock_items.put(o2,ld);

   // for debugging
   String rslt = "ASSOC|" + ld.getId() + "|" + System.identityHashCode(o1) + "|" +
      System.identityHashCode(o2) + "\n";

   System.err.println("DYLATE ASSOC " + rslt);

   writeInfo(rslt);
}



private static int nextId()
{
   return lock_counter.incrementAndGet();
}


private static class LockData {


   private int lock_id;
   private ThreadData last_thread;
   private long last_time;
   private long contested_time;
   private int contested_ctr;
   private Set<LockData> after_locks;
   private long wait_count;
   private long enter_count;
   private long timed_count;
   private long waited_count;
   private long block_time;
   private long wait_time;
   private long top_count;
   private ThreadLocal<Long> count_time;

   LockData() {
      lock_id = nextId();
      last_thread = null;
      last_time = 0;
      contested_time = 0;
      contested_ctr = 0;
      after_locks = new HashSet<LockData>(2);
      enter_count = 0;
      wait_count = 0;
      timed_count = 0;
      waited_count = 0;
      count_time = new ThreadLocal<Long>();
      block_time = 0;
      wait_time = 0;
      top_count = 0;
    }

   void setIgnored()			{ lock_id = -1; }
   boolean isIgnored()			{ return lock_id < 0; }

   boolean addAfter(LockData ld)	{ return after_locks.add(ld); }

   int getId()				{ return lock_id; }

   void setTopLevel()			{ ++top_count; }

   void updateCounts(Command cmd,long when)
   {
      switch (cmd) {
	 case ENTER :
	    ++enter_count;
	    count_time.set(when);
	    break;
	 case ENTERED:
	    block_time += when - getTime(when);
	    break;
	 case WAIT :
	    ++wait_count;
	    count_time.set(when);
	    break;
	 case TIME :
	    ++timed_count;
	    count_time.set(when);
	    break;
	 case WAITED :
	    ++waited_count;
	    wait_time += when - getTime(when);
	    break;
	 default :
	    break;
       }
    }

   private long getTime(long when) {
      Long l = count_time.get();
      if (l == null || l == 0) return when;
      return l;
    }

   String outputStats() {
      String rslt = "STATS|" + lock_id + "|" + wait_count + "|" + enter_count + "|" +
	 timed_count + "|" + waited_count + "|" + wait_time + "|" + block_time + "|" +
	 top_count + "\n";
      return rslt;
    }

   LockMode setThread(ThreadData th,long time,Command cmd) {
      LockMode result = LockMode.IGNORE;
      if (last_thread == null) {
	 // initial case or after an uncontested unlock
	 last_thread = th;
	 contested_time = 0;
       }
      else if (last_thread == th) {
	 if (contested_time != 0 && time - contested_time > RESET_TIME && (cmd == Command.ENTERED || cmd == Command.WAITED)) {
	    // same thread for > RESET TIME :: ignore for a while
	    contested_time = 0;
	    result = LockMode.STOP;
	  }
	 else if (contested_time != 0) {
	    // same thread, still contested
	    result = LockMode.USE;
	  }
	 else {
	    // same thread, not contested
	    if (cmd == Command.UNLOCK && contested_ctr == 0 && th.getLevel(this) == 0) {
	       // same thread, uncontested, level 0 unlock -> reset lock
	       last_thread = null;
	     }
	  }
       }
      else if (contested_time == 0) {
	 if (time - last_time > RESET_TIME) {
	    // new thread, uncontested, long time idle -> just set for now
	    last_thread = th;
	  }
	 else {
	    // new thread, short time since contested, update contested and start using
	    last_thread = th;
	    contested_time = time;
	    ++contested_ctr;
	    result = LockMode.RESET;
	  }
       }
      else {
	 contested_time = time;
	 result = LockMode.USE;
	 last_thread = th;
       }

      last_time = time;
      return result;
    }


}	// end of inner class LockData







/********************************************************************************/
/*										*/
/*	Thread management							*/
/*										*/
/********************************************************************************/

private ThreadData getThreadData()
{
   ThreadData td = thread_data.get();
   if (td != null && !td.isValid()) return null;

   if (td == null) {
      td = new ThreadData();
      thread_data.set(td);
      Thread t = Thread.currentThread();
      String rslt = "THREAD|" + td.getId() + "|" + t.getClass().getName() + "|" +
	 t.getId() + "|" + t.getName() + "\n";
      writeInfo(rslt);
    }

   return td;
}



private static class ThreadData {

   private int thread_id;
   private List<LockData> cur_locks;
   private DynamicArrayOfInt lock_locations;
   private Object [] for_object;
   private LockData [] use_lock;
   private int last_used;

   ThreadData() {
      thread_id = nextId();
      cur_locks = new ArrayList<LockData>();
      lock_locations = new DynamicArrayOfInt();
      for_object = new Object [SAVE_SIZE];
      use_lock = new LockData [SAVE_SIZE];
      last_used = -1;
    }

   ThreadData(int id) {
      thread_id = id;
      cur_locks = null;
    }

   boolean isValid()				{ return thread_id >= 0; }

   int getId()					{ return thread_id; }

   int getLevelCount()				{ return cur_locks.size(); }

   int getLevel(LockData ld) {
      return Collections.frequency(cur_locks,ld);
    }

   int getLocation(LockData ld) {
      for (int i = cur_locks.size() - 1; i >= 0; --i) {
	 LockData ld1 = cur_locks.get(i);
	 if (ld1 == ld) {
	    return lock_locations.get(i);
	  }
       }
      return 0;
    }

   LockData addLock(LockData ld,int loc) {
      cur_locks.add(ld);
      int sz = cur_locks.size();
      lock_locations.put(sz-1,loc);
      if (sz > 1) {
	 LockData ld1 = cur_locks.get(sz-2);
	 if (ld != ld1 && ld.addAfter(ld1)) {
	    // if this lock hasn't been already set
	    for (int i = 0; i < sz-1; ++i) {
	       if (cur_locks.get(i) == ld) return null;
	     }
	    // then return the previous lock as a prior
	    return ld1;
	  }
       }
      else ld.setTopLevel();
      return null;
    }

   void removeLock(LockData ld) {
      for (int i = cur_locks.size() - 1; i >= 0; --i) {
	 LockData ld1 = cur_locks.get(i);
	 if (ld1 == ld) {
	    cur_locks.remove(i);
	    lock_locations.put(i,0);
	    return;
	  }
       }
    }

   LockData getLock(Object o) {
      for (int i = 0; i < SAVE_SIZE; ++i) {
	 if (for_object[i] == o) {
	    last_used = i;
	    return use_lock[i];
	  }
       }
      return null;
    }

   void saveLock(Object o,LockData ld) {
      last_used = (last_used + 1) % SAVE_SIZE;
      for_object[last_used] = o;
      use_lock[last_used] = ld;
    }

}	// end of inner class ThreadData




private static class DynamicArrayOfInt {

   private int[] data;	// An array to hold the data.

   public DynamicArrayOfInt() {
      data = new int[10];
    }

   int get(int position) {
      if (position >= data.length)
	 return 0;
      else
	 return data[position];
    }

   void put(int position, int value) {
      if (position >= data.length) {
	 int newSize = 2 * data.length;
	 if (position >= newSize)
	    newSize = 2 * position;
	 int[] newData = new int[newSize];
	 System.arraycopy(data, 0, newData, 0, data.length);
	 data = newData;
       }
      data[position] = value;
    }

}	// end class DynamicArrayOfInt



/********************************************************************************/
/*										*/
/*	Hash table implementation						*/
/*										*/
/********************************************************************************/

private class ItemMap<K extends Object,T extends Object>
{
   private Map<Object,T>	item_map;
   private ReferenceQueue<K>	ref_queue;
   private T [] 		last_items;
   private K [] 		last_keys;
   private int			last_ctr;


   ItemMap(K[] kmap,T[] vmap) {
      item_map = new HashMap<Object,T>();
      ref_queue = new ReferenceQueue<K>();
      last_keys = kmap;
      last_items = vmap;
      for (int i = 0; i < LAST_CT; ++i){
	 last_items[i] = null;
	 last_keys[i] = null;
       }
      last_ctr = 0;
    }

   T get(K key) {
      for (int i = 0; i < LAST_CT; ++i) {
	 if (key == last_keys[i]) return last_items[i];
       }

      IdItem<K> kv = new IdItem<K>(key);
      T rslt = item_map.get(kv);

      last_keys[last_ctr] = key;
      last_items[last_ctr] = rslt;
      last_ctr = (last_ctr + 1) % LAST_CT;

      return rslt;
    }

   synchronized void put(K key,T item) {
      update();
      WeakItem<K> wi = new WeakItem<K>(key,ref_queue);
      item_map.put(wi,item);
    }

   synchronized T putIfAbsent(K key,T item) {
      IdItem<K> kv = new IdItem<K>(key);
      T v = item_map.get(kv);
      if (v == null) put(key,item);
      return v;
    }

   synchronized Collection<T> values() {
      return new ArrayList<T>(item_map.values());
    }

   private synchronized void update() {
      for ( ; ; ) {
	 Reference<?> rt = ref_queue.poll();
	 if (rt == null) break;
	 item_map.remove(rt);
       }
    }

}	// end of inner class ItemMap


private static class IdItem<T extends Object>
{
   private Object base_item;
   private int hash_code;

   IdItem(T x) {
      base_item = x;
      hash_code = System.identityHashCode(base_item);
    }

   Object get() 					{ return base_item; }

   @Override public int hashCode()			{ return hash_code; }

   @Override public boolean equals(Object o) {
      if (o == this || o == base_item) return true;
      if (o == null) return false;
      if (o instanceof IdItem) {
	 IdItem<?> id = (IdItem<?>) o;
	 return id.base_item == this.base_item;
       }
      if (o instanceof WeakItem) {
	 return o.equals(this);
       }
      return false;
    }

}	// end of inner class IdItem



private static class WeakItem<T extends Object> {

   private WeakReference<T> item_ref;
   private int hash_code;

   WeakItem(T o,ReferenceQueue<T> queue) {
      item_ref = new WeakReference<T>(o,queue);
      hash_code = System.identityHashCode(o);
    }

   @Override public int hashCode()			{ return hash_code; }

   @Override public boolean equals(Object o) {
      if (o instanceof WeakItem) {
	 if (o == this) return true;
	 WeakItem<?> wi = (WeakItem<?>) o;
	 Object o1 = item_ref.get();
	 if (o1 != null && o1.equals(wi.item_ref.get())) return true;
       }
      else if (o instanceof IdItem) {
	 IdItem<?> bi = (IdItem<?>) o;
	 o = bi.get();
	 if (o != null && o == item_ref.get()) return true;
       }
      else {
	 if (o != null && o.equals(item_ref.get())) return true;
       }
      return false;
    }

}	// end of inner class WeakItem




}	// end of class DylateMonitor




/* end of DylateMonitor.java */
