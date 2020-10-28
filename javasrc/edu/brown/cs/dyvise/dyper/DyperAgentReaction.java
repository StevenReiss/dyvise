/********************************************************************************/
/*										*/
/*		DyperAgentReaction.java 					*/
/*										*/
/*	Monitor agent that checks interaction/reaction profiling		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyper/DyperAgentReaction.java,v 1.8 2016/11/02 18:59:18 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyperAgentReaction.java,v $
 * Revision 1.8  2016/11/02 18:59:18  spr
 * Move to asm5
 *
 * Revision 1.7  2012-10-05 00:53:01  spr
 * Code clean up.
 *
 * Revision 1.6  2010-03-30 16:19:22  spr
 * Remove debugging. Add xjar to Make.data.
 *
 * Revision 1.5  2009-09-19 00:13:28  spr
 * Update agents for dynamic insertion and removal.
 *
 * Revision 1.4  2009-05-12 22:22:53  spr
 * Handle system classes and methods.
 *
 * Revision 1.3  2009-03-20 02:08:21  spr
 * Code cleanup; output information for incremental time-based display.
 *
 * Revision 1.2  2008-11-12 14:11:10  spr
 * Handle continuous memory tracing.  Other minor cleanups.
 *
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/


package edu.brown.cs.dyvise.dyper;


import java.lang.management.ThreadInfo;
import java.util.*;




public class DyperAgentReaction extends DyperAgent {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private long		last_sample;
private long		sample_count;
private long		tsample_count;
private Map<String,CallbackData> callback_map;
private TrieNode	root_node;
private int		max_level;
private Map<Integer,EventStats> event_map;
private long		total_run;
private long		total_io;
private long		total_wait;

private static boolean get_timings = true;
private static boolean get_threads = true;

private static final int	OP_RUN = 1;
private static final int	OP_WAIT = 2;
private static final int	OP_IO = 4;

private static final int	MAX_LEVEL = 10;

private static DyperAgentReaction reaction_agent = null;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public DyperAgentReaction(DyperControl dc)
{
   super(dc,"REACTION");

   reaction_agent = this;

   last_sample = 0;
   sample_count = 0;
   tsample_count = 0;
   callback_map = new HashMap<String,CallbackData>();
   root_node = new TrieNode();
   max_level = MAX_LEVEL;
   event_map = new HashMap<Integer,EventStats>();
   total_run = 0;
   total_io = 0;
   total_wait = 0;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public boolean setParameter(String s,String v)
{
   if (s.equals("REACTION_MAX_LEVEL")) {
      max_level = Integer.parseInt(v);
    }
   else return false;

   return true;
}



public String getParameter(String nm)
{
   String rslt = null;

   if (nm.equals("REACTION_MAX_LEVEL")) {
      rslt = Integer.toString(max_level);
    }

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

public void handleThreadStack(long now,ThreadInfo ti,StackTraceElement [] trc)
{

   if (last_sample != now) {
      ++sample_count;
      last_sample = now;
    }

   StackTraceElement cbc = null;
   StackTraceElement usr = null;
   StackTraceElement sys = null;

   if (ti.getThreadState() == Thread.State.RUNNABLE) {
      ++tsample_count;
      for (int j = 0; j < trc.length; ++j) {
	 StackTraceElement te = trc[j];
	 String nm = te.getClassName();
	 if (isCallbackClass(nm)) {
	    if (usr != null) {
	       sys = te;
	       cbc = usr;
	       break;
	     }
	  }
	 else if (!the_control.isSystemClass(nm)) usr = te;
       }
    }

   if (cbc != null && sys != null) {
      StackTraceElement root = trc[trc.length-1];
      if (root.getClassName().equals("java.lang.Thread") &&
	     root.getMethodName().equals("run")) root = trc[trc.length-2];

      String src = cbc.getClassName() + "@" + cbc.getMethodName();
      CallbackData cbd = callback_map.get(src);
      if (cbd == null) {
	 String ssrc = sys.getClassName() + "@" + sys.getMethodName();
	 cbd = new CallbackData(src,ssrc);
	 callback_map.put(src,cbd);
       }
      cbd.noteThread(root);
      cbd.countStack();
    }

   // check for I/O

   StackTraceElement ioc = null;
   int startidx = -1;

   if (ti.getThreadState() == Thread.State.RUNNABLE) {
      for (int j = 0; j < trc.length; ++j) {
	 StackTraceElement te = trc[j];
	 String nm = te.getClassName();
	 if (j == 0 && the_control.isIOClass(nm)) ioc = te;
	 else if (j == 0) break;
	 else if (startidx < 0 && the_control.isSystemClass(nm)) ioc = te;
	 else if (startidx < 0) startidx = j;
       }

      if (ioc != null && startidx >= 0) {
	 addIoInstance(trc,startidx);
       }
      else {
	 int frst = -1;
	 for (int j = 0; j < trc.length; ++j) {
	    StackTraceElement te = trc[j];
	    String nm = te.getClassName();
	    if (!the_control.isSystemClass(nm)) {
	       frst = j;
	       break;
	     }
	  }
	 if (frst >= 0) checkRunInstance(trc,frst);
       }
    }

   if (ti.getThreadState() == Thread.State.WAITING ||
	  ti.getThreadState() == Thread.State.TIMED_WAITING) {
      startidx = -1;
      for (int j = 0; j < trc.length; ++j) {
	 StackTraceElement te = trc[j];
	 String nm = te.getClassName();
	 if (startidx < 0 && the_control.isSystemClass(nm)) ;
	 else if (startidx < 0) startidx = j;
       }
      if (startidx >= 0) {
	 addWaitInstance(trc,startidx);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Check for actual system classes 					*/
/*										*/
/********************************************************************************/

private boolean isCallbackClass(String nm)
{
   if (!the_control.isSystemClass(nm)) return false;

   if (nm.startsWith("java.lang.reflect.")) return false;
   if (nm.equals("java.lang.Class")) return false;
   if (nm.equals("java.lang.Thread")) return false;
   if (nm.startsWith("javax.swing.text.html.parser.")) return false;

   return true;
}




/********************************************************************************/
/*										*/
/*	Clear methods								*/
/*										*/
/********************************************************************************/

@Override public void handleClear(long now)
{
   last_sample = 0;
   sample_count = 0;
   tsample_count = 0;
   callback_map.clear();
   root_node = new TrieNode();
   event_map = new HashMap<Integer,EventStats>();
   total_run = 0;
   total_io = 0;
   total_wait = 0;
}



/********************************************************************************/
/*										*/
/*	Reporting methods							*/
/*										*/
/********************************************************************************/

public void generateReport(ReportType rt,DyperXmlWriter xw,long now)
{
   xw.begin("REACTIONS");
   xw.field("MONTIME",getMonitoredTime(now));
   xw.field("SAMPLES",sample_count);
   xw.field("TSAMPLES",tsample_count);
   xw.field("LAST",last_sample);
   xw.field("TOTRUN",total_run);
   xw.field("TOTIO",total_io);
   xw.field("TOTWAIT",total_wait);

   for (CallbackData cd : callback_map.values()) {
      cd.report(xw);
    }

   xw.begin("TRIE");
   root_node.report(xw);
   xw.end();

   synchronized (event_map) {
      for (Map.Entry<Integer,EventStats> ent : event_map.entrySet()) {
	 xw.begin("EVENT");
	 xw.field("MID",ent.getKey());
	 ent.getValue().outputXml(xw);
	 xw.end("EVENT");
       }
    }

   xw.end("REACTIONS");
}



/********************************************************************************/
/*										*/
/*	CallbackData -- information about callbacks				*/
/*										*/
/********************************************************************************/

private static class CallbackData {

   private String user_method;
   private int	stack_count;
   private String system_method;
   private Set<String> used_threads;

   CallbackData(String m,String s) {
      user_method = m;
      system_method = s;
      stack_count = 0;
      used_threads = new HashSet<String>();
    }

   void countStack()			{ ++stack_count; }
   void noteThread(StackTraceElement se) {
      String s = se.getClassName() + "@" + se.getMethodName();
      used_threads.add(s);
    }

   void report(DyperXmlWriter xw) {
      xw.begin("CALLBACK");
      xw.field("USER",user_method);
      xw.field("STACK",stack_count);
      xw.field("SYSTEM",system_method);
      for (String s : used_threads) xw.textElement("THREAD",s);
      xw.end();
    }

}	// end of subclass CallbackData






/********************************************************************************/
/*										*/
/*	Stack snapshot checking for I/O and Wait processing			*/
/*										*/
/********************************************************************************/

void addIoInstance(StackTraceElement [] trc,int idx)
{
   root_node.insert(trc,idx,OP_IO);
}



void addWaitInstance(StackTraceElement [] trc,int idx)
{
   root_node.insert(trc,idx,OP_WAIT);
}



void checkRunInstance(StackTraceElement [] stk,int idx)
{
   root_node.insert(stk,idx,OP_RUN);
}





/********************************************************************************/
/*										*/
/*	Methods for maintaining a method-trie					*/
/*										*/
/********************************************************************************/

private class TrieNode {

   private String class_name;
   private String method_name;
   private int num_run;
   private int num_io;
   private int num_wait;
   private List<TrieNode> next_nodes;
   private boolean is_system;

   TrieNode() {
      method_name = null;
      class_name = null;
      num_run = 0;
      num_io = 0;
      num_wait = 0;
      next_nodes = new Vector<TrieNode>();
      is_system = true;
    }

   TrieNode(StackTraceElement trc) {
      class_name = trc.getClassName();
      method_name = trc.getMethodName();
      num_run = 0;
      num_io = 0;
      num_wait = 0;
      next_nodes = new Vector<TrieNode>();
      is_system = the_control.isSystemClass(class_name);
    }

   private void count(int op) {
      if ((op & OP_RUN) != 0) {
	 ++num_run;
	 ++total_run;
       }
      if ((op & OP_WAIT) != 0) {
	 ++num_wait;
	 ++total_wait;
       }
      if ((op & OP_IO) != 0) {
	 ++num_io;
	 ++total_io;
       }
    }

   void insert(StackTraceElement [] trc,int idx,int op) {
      insertItem(trc,trc.length-1,idx,op,0);
    }


   private void insertItem(StackTraceElement [] trc,int idx,int base,int op,int lvl) {
      if (idx < base || lvl >= max_level) {
	 if (class_name != null) count(op);
	 return;
       }

      for (TrieNode tn : next_nodes) {
	 if (tn.matches(trc[idx])) {
	    tn.insertItem(trc,idx-1,base,op,lvl+1);
	    return;
	  }
       }

      if (op == OP_RUN && base < idx-1) base = idx-1;

      TrieNode tn = createTrieNode(trc,idx,base,op,lvl+1);
      if (tn == null) count(op);
      else next_nodes.add(tn);
    }

   private TrieNode createTrieNode(StackTraceElement [] trc,int idx,int base,int op,int lvl) {
      if (idx < base || lvl >= max_level) return null;
      TrieNode cn = createTrieNode(trc,idx-1,base,op,lvl+1);
      TrieNode tn = new TrieNode(trc[idx]);
      if (cn == null) {
	 tn.count(op);
       }
      else {
	 tn.next_nodes.add(cn);
       }
      return tn;
    }

   private boolean matches(StackTraceElement trc) {
      if (class_name == null) return false;
      return class_name.equals(trc.getClassName()) && method_name.equals(trc.getMethodName());
    }

   void report(DyperXmlWriter xw) {
      if (class_name != null) {
         xw.begin("TRIENODE");
         xw.field("CLASS",class_name);
         xw.field("METHOD",method_name);
         xw.field("RUN",num_run);
         xw.field("IO",num_io);
         xw.field("WAIT",num_wait);
         if (is_system) xw.field("SYS",true);
       }
      for (TrieNode tn : next_nodes) {
         tn.report(xw);
       }
      if (class_name != null) xw.end();
    }

}	// end of subclass TrieNode




/********************************************************************************/
/*										*/
/*	Run time enter/exit methods						*/
/*										*/
/********************************************************************************/

public static long monMethodEnter(int no)
{
   if (get_timings) return System.nanoTime();

   return 0;
}



public static void monMethodExit(int no,long start)
{
   if (get_timings) {
      long now = System.nanoTime();
      long delta = now - start;
      if (delta == 0) delta = 500;		   // < 1 us -- give average

      reaction_agent.increment(no,delta);
    }
   else {
      reaction_agent.increment(no,0);
    }
}



/********************************************************************************/
/*										*/
/*	Statistics accumulation 						*/
/*										*/
/********************************************************************************/

private void increment(int itm,long delta)
{
   if (!the_control.useThread()) return;

   String th = null;
   if (get_threads) th = Thread.currentThread().getName();

   EventStats es = event_map.get(itm);
   if (es == null) {
      synchronized (event_map) {
	 es = event_map.get(itm);
	 if (es == null) {
	    es = new EventStats();
	    event_map.put(itm,es);
	  }
       }
    }

   es.update(delta,th);
}



private static class EventStats {

   private long occur_count;
   private long total_time;
   private Map<String,long[]> thread_stats;

   EventStats() {
      occur_count = 0;
      total_time = 0;
      if (get_threads) thread_stats = new HashMap<String,long[]>();
    }

   synchronized void update(long tim,String th) {
      occur_count++;
      total_time += tim;
      if (th != null) {
	 long [] v = thread_stats.get(th);
	 if (v == null) {
	    v = new long[3];
	    thread_stats.put(th,v);
	  }
	 v[0]++;
	 v[1] += tim;
	 v[2] += tim*tim;
       }
    }

   void outputXml(DyperXmlWriter xw) {
      xw.field("COUNT",occur_count);
      xw.field("TIME",total_time);
      // xw.field("TIME2",total_time2);
      if (thread_stats != null) {
         for (Map.Entry<String,long[]> ent : thread_stats.entrySet()) {
            xw.begin("THREAD");
            xw.field("NAME",ent.getKey());
            xw.field("COUNT",ent.getValue()[0]);
            xw.field("TIME",ent.getValue()[1]);
            // xw.field("TIME2",ent.getValue()[2]);
            xw.end("THREAD");
          }
       }
    }

}	// end of subclass EventStats


}	// end of class DyperAgentReaction




/* end of DyperAgentReaction.java */
