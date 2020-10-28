/********************************************************************************/
/*										*/
/*		DyperAgentSocket.java						*/
/*										*/
/*	Monitor agent that does Socket analysis 				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyper/DyperAgentSocket.java,v 1.2 2016/11/02 18:59:18 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyperAgentSocket.java,v $
 * Revision 1.2  2016/11/02 18:59:18  spr
 * Move to asm5
 *
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/


package edu.brown.cs.dyvise.dyper;


import java.lang.management.ThreadInfo;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.util.*;


public class DyperAgentSocket extends DyperAgent {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private long	last_sample;
private long	sample_count;
private Map<String,SocketCount> count_map;
private long	total_count;
private Map<Object,SocketItem> item_map;
private List<SocketItem> item_set;

private static int socket_counter = 0;
private static boolean [] in_mon = new boolean[DYPER_MAX_THREADS];
private static DyperAgentSocket socket_agent = null;

private static final int     OP_READ = 0;
private static final int     OP_WRITE = 1;
private static final int     OP_CLOSE = 2;

private static final int     NUM_OPS = 2;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public DyperAgentSocket(DyperControl dc)
{
   super(dc,"SOCKETS");

   socket_agent = this;

   last_sample = 0;
   sample_count = 0;
   count_map = new HashMap<String,SocketCount>();
   total_count = 0;
   item_map = new WeakHashMap<Object,SocketItem>();
   item_set = new ArrayList<SocketItem>();

   long mid = dc.getMonitorThreadId();
   in_mon[(int)mid] = true;
}




/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

public void handleThreadStack(long now,ThreadInfo ti,StackTraceElement [] trc)
{
   if (ti.getThreadState() != Thread.State.RUNNABLE) return;

   if (last_sample != now) {
      ++sample_count;
      last_sample = now;
    }

   StackTraceElement rpt = null;

   for (int i = 0; i < trc.length; ++i) {
      StackTraceElement te = trc[i];
      String nm = te.getClassName();
      if (nm.equals("java.net.SocketInputStream") || nm.equals("java.net.SocketOutputStream"))
	 rpt = te;
    }

   if (rpt != null) {
      ++total_count;
      String km = rpt.getClassName() + "@" + rpt.getMethodName();
      SocketCount ic = count_map.get(km);
      if (ic == null) {
	 ic = new SocketCount(km);
	 count_map.put(km,ic);
       }
      ic.accumulate(ti);
    }
}




/********************************************************************************/
/*										*/
/*	Clear methods								*/
/*										*/
/********************************************************************************/

public void handleClear(long now)
{
   last_sample = 0;
   sample_count = 0;
   count_map = new HashMap<String,SocketCount>();
   total_count = 0;
}




/********************************************************************************/
/*										*/
/*	Reporting methods							*/
/*										*/
/********************************************************************************/

public void generateReport(ReportType rt,DyperXmlWriter xw,long now)
{
   xw.begin("SOCKETS");
   xw.field("MONTIME",getMonitoredTime(now));
   xw.field("SAMPLES",sample_count);
   xw.field("LAST",last_sample);
   xw.field("TOTAL",total_count);

   for (SocketCount ic : count_map.values()) {
      ic.report(xw);
    }

   synchronized (this) {
      for (Iterator<SocketItem> it = item_set.iterator(); it.hasNext(); ) {
	 SocketItem ci = it.next();
	 ci.report(xw);
	 if (ci.isClosed()) it.remove();
       }
    }

   xw.end("SOCKETS");
}



/********************************************************************************/
/*										*/
/*	Run time entries							*/
/*										*/
/********************************************************************************/

public static void monSocketOp(Object c,int op)
{
   Thread t = Thread.currentThread();
   int tid = (int) t.getId();

   SocketItem ci = null;

   synchronized (socket_agent) {
      while (tid >= in_mon.length) {
	 boolean [] nmon = new boolean[in_mon.length*2];
	 System.arraycopy(in_mon,0,nmon,0,in_mon.length);
	 in_mon = nmon;
       }

      if (in_mon[tid] || !socket_agent.the_control.useThread(tid)) return;
      in_mon[tid] = true;

      ci = socket_agent.item_map.get(c);
      if (ci == null && op == OP_CLOSE) return;
      if (ci == null) {
	 ci = new SocketItem(c,++socket_counter);
	 socket_agent.item_map.put(c,ci);
	 socket_agent.item_set.add(ci);
       }
    }

   ci.record(op);

   in_mon[tid] = false;
}




/********************************************************************************/
/*										*/
/*	Class to hold counter information					*/
/*										*/
/********************************************************************************/

private static class SocketCount {

   private String source_item;
   private long total_count;
   private Map<String,long []> thread_counts;

   SocketCount(String itm) {
      source_item = itm;
      total_count = 0;
      thread_counts = new HashMap<String,long[]>();
    }

   void accumulate(ThreadInfo ti) {
      ++total_count;
      String tnm = ti.getThreadName();
      long [] val = thread_counts.get(tnm);
      if (val == null) {
	 val = new long[1];
	 thread_counts.put(tnm,val);
       }
      val[0]++;
    }

   void report(DyperXmlWriter xw) {
      xw.begin("ITEM");
      xw.field("NAME",source_item);
      xw.field("COUNT",total_count);
      for (Map.Entry<String,long[]> ent : thread_counts.entrySet()) {
         xw.begin("THREAD");
         xw.field("NAME",ent.getKey());
         xw.field("COUNT",ent.getValue()[0]);
         xw.end("THREAD");
       }
      xw.end("ITEM");
    }

}	// end of subclass SocketCount



/********************************************************************************/
/*										*/
/*	SocketItem -- information about a particular socket			*/
/*										*/
/********************************************************************************/

private static class SocketItem {

   private String socket_id;
   private String local_address;
   private String remote_address;
   private int [] op_counts;
   private boolean is_closed;
   private WeakReference<Socket> for_socket;

   SocketItem(Object o,int id) {
      Socket s = (Socket) o;
      socket_id = "S_" + id;
      if (s != null) {
	 local_address = s.getLocalSocketAddress().toString();
	 remote_address = s.getRemoteSocketAddress().toString();
       }
      op_counts = new int[NUM_OPS];
      is_closed = false;
      for_socket = new WeakReference<Socket>(s);
    }

   void record(int op) {
      if (op == OP_CLOSE) is_closed = true;
      else op_counts[op]++;
    }

   void report(DyperXmlWriter xw) {
      if (for_socket.get() == null) is_closed = true;
      xw.begin("OBJECT");
      xw.field("KEY",socket_id);
      if (local_address != null) {
         xw.field("LOCAL",local_address);
         xw.field("REMOTE",remote_address);
       }
      xw.field("READS",op_counts[OP_READ]);
      xw.field("WRITES",op_counts[OP_WRITE]);
      if (is_closed) xw.field("CLOSED",is_closed);
      xw.end("OBJECT");
    }

   boolean isClosed()				{ return is_closed; }

}	// end of subclass SocketItem




}	// end of class DyperAgentSocket




/* end of DyperAgentSocket.java */

