/********************************************************************************/
/*										*/
/*		DyluteMonitor.java						*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/dylute/src/DyluteMonitor.java,v 1.1 2011-09-12 19:37:25 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyluteMonitor.java,v $
 * Revision 1.1  2011-09-12 19:37:25  spr
 * Add dylute files to repository
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dylute;

// NOTE: This should only use system libraries and libraries that are explicitly
//    in this package.	Using anything else might cause conflicts if the application
//    being monitored uses the same library.

import java.io.*;
import java.util.*;



public class DyluteMonitor implements DyluteConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private long		start_time;
private long		last_output;
private TraceSet []	trace_sets;
private int		current_set;
private int		max_size;
private Writer		output_writer;
private Reporter	trace_thread;

private static DyluteMonitor	the_monitor = new DyluteMonitor();

private static final int	NUM_SETS = 3;
private static final int	ENTRY_COUNT = 1024;
private static final long	TIME_DELAY = 100;
private static final long	NULL_DELAY = 1000;

enum Command {
   THREAD,
   DONE,
   TIME,
   ENTER,
   ENTERED,
   NOTIFY,
   WAIT,
   WAITED,
   UNLOCK
}




/********************************************************************************/
/*										*/
/*	Static Entry points							*/
/*										*/
/********************************************************************************/

public static void monEnter(Object mon,int lid)
{
   the_monitor.addEntry(Command.ENTER,mon,lid,mon.getClass().getName());
}


public static void monEntered(Object mon)
{
   the_monitor.addEntry(Command.ENTERED,mon);
}



public static void monExit(Object mon)
{
   the_monitor.addEntry(Command.UNLOCK,mon);
}



public static void monNotify(Object mon)
{
   the_monitor.addEntry(Command.NOTIFY,mon);
}



public static void monWait(Object mon)
{
   the_monitor.addEntry(Command.WAIT,mon);
}



public static void monWaited(Object mon)
{
   the_monitor.addEntry(Command.WAITED,mon);
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/


static DyluteMonitor getMonitor()		{ return the_monitor; }



private DyluteMonitor()
{
   start_time = System.nanoTime();
   last_output = 0;
   max_size = ENTRY_COUNT * 32;

   trace_sets = new TraceSet[NUM_SETS];
   for (int i = 0; i < NUM_SETS; ++i) trace_sets[i] = new TraceSet();
   current_set = 0;
   output_writer = null;
   trace_thread = null;
}


/********************************************************************************/
/*										*/
/*	Access Methods								*/
/*										*/
/********************************************************************************/

synchronized void setOutputWriter(Writer w)
{
   if (trace_thread == null) {
      trace_thread = new Reporter();
      trace_thread.start();
      Runtime.getRuntime().addShutdownHook(new Finisher());
    }

   output_writer = w;
}




/********************************************************************************/
/*										*/
/*	Trace entry points							*/
/*										*/
/********************************************************************************/

private void addEntry(Command cmd,Object mon)
{
   long time = System.nanoTime() - start_time;
   Thread th = Thread.currentThread();
   int mid = System.identityHashCode(mon);

   trace_sets[current_set].addEntry(th,cmd,mid,0,null,time);
}



private void addEntry(Command cmd,Object mon,int id,String xid)
{
   long time = System.nanoTime() - start_time;
   Thread th = Thread.currentThread();
   int mid = System.identityHashCode(mon);

   trace_sets[current_set].addEntry(th,cmd,mid,id,xid,time);
}



private void collectStatistics()
{
   long now = System.nanoTime() - start_time;
   int next = (current_set + 1) % NUM_SETS;
   int prior = (current_set + NUM_SETS - 1) % NUM_SETS;
   trace_sets[next].reset();
   TraceSet eval = trace_sets[prior];
   current_set = next;

   try {
      if (output_writer != null) {
	 boolean out = eval.output();

	 if (out || now - last_output > NULL_DELAY * 1000000) {
	    output_writer.append("DONE ");
	    output_writer.append(Long.toString(now));
	    output_writer.append("\n");
	    last_output = now;
	  }
       }
      output_writer.flush();
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
}



/********************************************************************************/
/*										*/
/*	Trace Set -- top level holder of current trace information		*/
/*										*/
/********************************************************************************/

private class TraceSet {

   private WeakHashMap<Thread,ThreadSet>	thread_map;

   TraceSet() {
      thread_map = new WeakHashMap<Thread,ThreadSet>();
    }

   void addEntry(Thread th,Command cmd,int mid,int id,String xid,long time) {
      if (output_writer == null) return;
      StringBuilder buf = new StringBuilder();
      buf.append(cmd);
      buf.append(" ");
      buf.append(mid);
      buf.append(" ");
      buf.append(time);
      if (id > 0) {
	 buf.append(" ");
	 buf.append(id);
       }
      if (xid != null) {
	 buf.append(" ");
	 buf.append(xid);
       }
      getThreadSet(th).addEntry(buf);
    }

   void reset() {
      for (Map.Entry<Thread,ThreadSet> ent : thread_map.entrySet()) {
	 ThreadSet ts = ent.getValue();
	 ts.reset();
       }
    }

   boolean output() throws IOException {
      boolean fg = false;
      for (Map.Entry<Thread,ThreadSet> ent : thread_map.entrySet()) {
	 ThreadSet ts = ent.getValue();
	 if (ts.containsData()) {
	    Thread th = ent.getKey();
	    output_writer.append("THREAD " + th.hashCode() + " " +
				    ent.getKey().getId() + " " +
				    ent.getKey().getName() + "\n");
	    fg = true;
	    ts.output();
	  }
       }
      return fg;
    }

   ThreadSet getThreadSet(Thread th) {
      ThreadSet ts = thread_map.get(th);
      if (ts == null) {
	 ts = new ThreadSet();
	 synchronized (this) {
	    thread_map.put(th,ts);
	  }
       }
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

   void addEntry(CharSequence s) {
      output_data.append(s);
      output_data.append('\n');
    }

   boolean containsData() {
      return output_data.length() > 0;
    }

   void reset() {
      int ln = output_data.length();
      output_data.delete(0,ln);
      if (ln+10 > max_size) max_size = ln+10;
    }

   void output() throws IOException {
      output_writer.append(output_data);
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
      super("DyluteReporter");
      setPriority(Thread.MAX_PRIORITY);
      time_delay = TIME_DELAY;
      setDaemon(true);
    }

   @Override public void run() {
      try {
	 for ( ; ; ) {
	    try {
	       sleep(time_delay);
	       setPriority(Thread.MAX_PRIORITY);
	     }
	    catch (InterruptedException e) {
	       break;
	     }
	    collectStatistics();
	  }
       }
      catch (Throwable t) {
	 System.err.println("DYLUTE: Reporter aborted: " + t);
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
    }

}	// end of inner class Finisher



}	// end of class DyluteMonitor




/* end of DyluteMonitor.java */
