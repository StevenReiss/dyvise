/********************************************************************************/
/*										*/
/*		DyperAgentEvents.java						*/
/*										*/
/*	Monitor agent that handles event calls for visualization		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyper/DyperAgentEvents.java,v 1.3 2016/11/02 18:59:18 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyperAgentEvents.java,v $
 * Revision 1.3  2016/11/02 18:59:18  spr
 * Move to asm5
 *
 * Revision 1.2  2010-03-30 16:19:21  spr
 * Remove debugging. Add xjar to Make.data.
 *
 * Revision 1.1  2009-09-19 00:13:28  spr
 * Update agents for dynamic insertion and removal.
 *
 *
 ********************************************************************************/


package edu.brown.cs.dyvise.dyper;


import java.lang.management.ThreadInfo;
import java.util.*;




public class DyperAgentEvents extends DyperAgent {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private List<Event>	event_list;
private List<Event>	event_next;
private LinkedList<Event> event_free;

private Map<Object,Boolean> object_map;
private List<Object>	 report_list;
private List<Object>	 report_next;

private static long	base_time = System.currentTimeMillis();

private long		monitor_start;

private static	DyperAgentEvents	event_agent = null;

private final static int	INITIAL_SIZE = 128;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public DyperAgentEvents(DyperControl dc)
{
   super(dc,"EVENTS");

   event_agent = this;

   event_list = new ArrayList<Event>(INITIAL_SIZE);
   event_next = new ArrayList<Event>(INITIAL_SIZE);
   event_free = new LinkedList<Event>();

   object_map = new WeakHashMap<Object,Boolean>();
   report_list = new ArrayList<Object>();
   report_next = new ArrayList<Object>();

   monitor_start = 0;
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

public void handleThreadStack(long now,ThreadInfo ti,StackTraceElement [] trc)
{
   // might want to record timings inside states here
   return;
}



/********************************************************************************/
/*										*/
/*	Clear methods								*/
/*										*/
/********************************************************************************/

@Override public void handleClear(long now)
{
   // System.err.println("DYPER: EVENTS CLEARED");

   synchronized (object_map) {
      object_map.clear();
    }
}


@Override public void handleMonitorStart(long now)
{
   monitor_start = now;
}


@Override public void handleMonitorStop(long now)
{
   monitor_start = 0;
   synchronized (event_list) {
      event_list.clear();
      event_next.clear();
      event_free.clear();
    }
}




/********************************************************************************/
/*										*/
/*	Reporting methods							*/
/*										*/
/********************************************************************************/

public void generateReport(ReportType rt,DyperXmlWriter xw,long now)
{
   // System.err.println("DYPER: EVENTS " + monitor_start + " " + event_list.size() + " " + object_map.size() + " " + event_free.size());

   if (monitor_start == 0) return;

   xw.begin("EVENTS");
   xw.field("BASE",base_time);
   xw.field("NOW",now);

   synchronized (event_list) {
      List<Event> evt = event_list;
      event_list = event_next;
      event_next = evt;
    }

   synchronized (object_map) {
      List<Object> rpt = report_list;
      report_list = report_next;
      report_next = rpt;
    }

   // report_next is safe for synchronziation since generateReport is only used in
   // the dyper thread and hence the next will be cleared before it is needed again

   for (Object o : report_next) {
      xw.begin("ID");
      xw.field("VALUE",System.identityHashCode(o));
      xw.field("TYPE",o.getClass().getName());
      xw.text(o.toString());
      xw.end("ID");
    }
   report_next.clear();

   for (Event e : event_next) {
      e.output(xw);
      e.clear();
    }

   synchronized (event_free) {
      event_free.addAll(event_next);
    }
   event_next.clear();

   xw.end("EVENTS");
}



/********************************************************************************/
/*										*/
/*	Main entry points for instrumented code 				*/
/*										*/
/********************************************************************************/

public static void newEvent(int eid)
{
   event_agent.addEvent(eid,null,null,null);
}




public static void newEvent(int eid,Object p0)
{
   event_agent.addEvent(eid,p0,null,null);
}




public static void newEvent(int eid,Object p0,Object p1)
{
   event_agent.addEvent(eid,p0,p1,null);
}




public static void newEvent(int eid,Object p0,Object p1,Object p2)
{
   event_agent.addEvent(eid,p0,p1,p2);
}




/********************************************************************************/
/*										*/
/*	Event management							*/
/*										*/
/********************************************************************************/

private void addEvent(int id,Object p0,Object p1,Object p2)
{
   if (monitor_start == 0) return;

   // System.err.println("DYPER EVENT " + id + " " + p0 + " " + p1 + " " + p2);

   Event e = getNextEvent();
   e.set(id,getId(p0),getId(p1),getId(p2));

   synchronized (event_list) {
      event_list.add(e);
    }
}



private Event getNextEvent()
{
   Event evt = null;

   if (event_free.isEmpty()) {
      evt = new Event();
    }
   else {
      synchronized (event_free) {
	 if (!event_free.isEmpty()) evt = event_free.removeFirst();
       }
      if (evt == null) evt = new Event();
    }

   return evt;
}



/********************************************************************************/
/*										*/
/*	Object management							*/
/*										*/
/********************************************************************************/

int getId(Object o)
{
   if (o == null) return 0;

   int v = System.identityHashCode(o);

   if (object_map.containsKey(o)) return v;

   synchronized (object_map) {
      if (object_map.put(o,Boolean.TRUE) == null) {
	 report_list.add(o);
       }
    }

   return v;
}



/********************************************************************************/
/*										*/
/*	Event holder								*/
/*										*/
/********************************************************************************/

private static class Event {

   int event_id;
   int event_p0;
   int event_p1;
   int event_p2;
   long event_when;

   Event() {
      event_id = -1;
      event_p0 = event_p1 = event_p2 = 0;
      event_when = 0;
    }

   void set(int id,int p0,int p1,int p2) {
      event_id = id;
      event_p0 = p0;
      event_p1 = p1;
      event_p2 = p2;
      event_when = System.currentTimeMillis() - base_time;
    }


   void clear() 			{ event_id = -1; }

   void output(DyperXmlWriter xw) {
      if (event_id < 0) return;
    
      String p = Integer.toString(event_p0) + "," + event_p1 + "," + event_p2;
      xw.begin("E");
      xw.field("I",event_id);
      xw.field("P",p);
      xw.field("T",event_when);
      xw.end("E");
    }

}	// end of innerclass Event



}	// end of class DyperAgentEvents




/* end of DyperAgentEvents.java */
