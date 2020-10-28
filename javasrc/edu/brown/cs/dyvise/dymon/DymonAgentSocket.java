/********************************************************************************/
/*										*/
/*		DymonAgentSocket.java						*/
/*										*/
/*	DYPER monitor agent for socket monitoring				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymon/DymonAgentSocket.java,v 1.5 2009-09-19 00:09:46 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymonAgentSocket.java,v $
 * Revision 1.5  2009-09-19 00:09:46  spr
 * Update dymon for seletive agent insertion and removal.
 *
 * Revision 1.4  2009-06-04 18:53:50  spr
 * Set up for binary distribution.
 *
 * Revision 1.3  2009-04-11 23:46:18  spr
 * Use IvyFormat rather than local code.
 *
 * Revision 1.2  2008-11-12 14:10:44  spr
 * Various efficiency and bug fixups.  Readiness for immediate output.
 *
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymon;


import edu.brown.cs.ivy.file.IvyFormat;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.util.*;



class DymonAgentSocket extends DymonAgent {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private long		total_samples;
private long		total_socket;
private long		total_time;
private Map<String,SocketData> socket_map;
private Map<String,SocketObject> object_map;
private long		max_ops;
private PatchRequest	patch_request;

private long		object_time;

private Detailing	socket_detailing;

private static final int	MIN_SAMPLES = 100;

private static double	report_threshold = 0.0;
private static double	object_threshold = 0.25;

private static final long	INSTRUMENT_TIME = 60000;
private static final long	OVERHEAD_FIXED_TIME = 2000;
private static final double	OVERHEAD_SLOWDOWN = 0.01;

private static final String NAME = "SOCKETS";

private static Collection<PatchInfo> patch_methods;


static {
   patch_methods = new ArrayList<PatchInfo>();
   patch_methods.add(new PatchInfo("java.net.SocketInputStream","read([BII)I","READ"));
   patch_methods.add(new PatchInfo("java.net.SocketInputStream","close","CLOSE"));
   patch_methods.add(new PatchInfo("java.net.SocketOutputStream","socketWrite","WRITE"));
   patch_methods.add(new PatchInfo("java.net.SocketOutputStream","close","CLOSE"));
   // patch_methods.add(new PatchInfo("java.net.Socket","close","CLOSE1"));
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DymonAgentSocket(DymonMain dm,DymonProcess p)
{
   super(dm,p);

   total_samples = 0;
   total_socket = 0;
   total_time = 0;
   max_ops = 0;
   object_time = 0;
   patch_request = new PatchRequest();

   socket_map = new HashMap<String,SocketData>();
   object_map = new HashMap<String,SocketObject>();

   socket_detailing = new Detailing();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getName()			{ return NAME; }


@Override public String getDyperAgentName()		{ return "SOCKETS"; }


@Override protected String getMonitorClass()
{
   return "edu.brown.cs.dyvise.dyper.DyperAgentSocket";
}



/********************************************************************************/
/*										*/
/*	Clear methods								*/
/*										*/
/********************************************************************************/

@Override public void doClear()
{
   total_samples = 0;
   total_socket = 0;
   total_time = 0;
   max_ops = 0;
   object_time = 0;

   socket_map.clear();
   object_map.clear();

   socket_detailing.doClear();
}




/********************************************************************************/
/*										*/
/*	Report handling methods 						*/
/*										*/
/********************************************************************************/

@Override public void handleReport(Element r)
{
   Element ce = IvyXml.getElementByTag(r,"SOCKETS");

   long montime = IvyXml.getAttrLong(ce,"MONTIME");

   total_time = montime;
   total_samples = IvyXml.getAttrLong(ce,"SAMPLES");
   total_socket = IvyXml.getAttrLong(ce,"TOTAL");

   for (Element te : IvyXml.elementsByTag(ce,"ITEM")) {
      String nm = IvyXml.getAttrString(te,"NAME");
      synchronized (socket_map) {
	 SocketData id = socket_map.get(nm);
	 if (id == null) {
	    id = new SocketData(nm);
	    socket_map.put(nm,id);
	  }
	 id.update(te);
       }
    }

   synchronized (object_map) {
      ++object_time;
      for (Element te : IvyXml.elementsByTag(ce,"OBJECT")) {
	 String nm = IvyXml.getAttrString(te,"KEY");
	 SocketObject so = object_map.get(nm);
	 if (so == null) {
	    so = new SocketObject(te);
	    object_map.put(nm,so);
	  }
	 so.update(te);
       }
    }
}




/********************************************************************************/
/*										*/
/*	Analysis methods							*/
/*										*/
/********************************************************************************/

@Override public void outputAnalysis(IvyXmlWriter xw)
{

   xw.begin("SOCKETS");

   xw.field("TOTTIME",IvyFormat.formatTime(total_time));
   xw.field("TOTSAMP",total_samples);
   xw.field("TOTCOLL",total_socket);
   xw.field("COLLTIME",IvyFormat.formatTime(((double) total_socket)/total_samples*total_time));

   Collection<SocketObject> soset;
   synchronized (object_map) {
      soset = new TreeSet<SocketObject>(object_map.values());
      for (SocketObject so : soset) so.checkClosed(object_time);
    }

   double dtime = socket_detailing.getActiveTime();
   if (dtime > 0) {
      double totread = 0;
      double totwrite = 0;
      double totopen = 0;
      double totclosed = 0;

      for (SocketObject so : soset) {
	 totopen += 1;
	 if (so.isClosed()) totclosed += 1;
	 totread += so.getReads();
	 totwrite += so.getWrites();
       }

      totread *= total_time / dtime;
      totwrite *= total_time / dtime;
      totopen *= total_time /dtime;
      totclosed *= total_time /dtime;

      xw.begin("TOTALS");
      xw.field("OPEN",IvyFormat.formatCount(totopen));
      xw.field("READ",IvyFormat.formatCount(totread));
      xw.field("WRITE",IvyFormat.formatCount(totwrite));
      xw.field("CLOSE",IvyFormat.formatCount(totclosed));
      xw.end("TOTALS");
    }

   synchronized (socket_map) {
      for (SocketData id : socket_map.values()) {
	 id.output(xw);
       }
    }

   for (SocketObject so : soset) {
      so.output(xw);
    }

   xw.end("SOCKETS");
}




/********************************************************************************/
/*										*/
/*	Summary methods 							*/
/*										*/
/********************************************************************************/

@Override public double getSummaryValue()	{ return getAgentPriority(); }



@Override public double getConfidence()
{
   if (total_samples < MIN_SAMPLES) return 0;

   double conf = (socket_detailing.getNumDetailing() > 0 ? 1 : 0.5);

   return conf;
}



@Override public void outputSummary(IvyXmlWriter xw)
{
   if (total_samples == 0) return;

   double p = ((double) total_socket)/total_samples;
   xw.begin("METER");
   xw.field("NAME","SOCKET %");
   xw.field("VALUE",p);
   xw.field("TYPE","PERCENT");
   xw.field("MIN",0);
   xw.field("MAX",1.0);
   xw.end();
}




/********************************************************************************/
/*										*/
/*	Detailing methods							*/
/*										*/
/********************************************************************************/

public Collection<DymonDetailing> getDetailings()
{
   Collection<DymonDetailing> r = new ArrayList<DymonDetailing>();
   r.add(socket_detailing);
   return r;
}



private double getAgentPriority()
{
   if (total_samples < MIN_SAMPLES) return 0;

   double p1 = ((double) total_socket)/total_samples;
   if (p1 > 1) p1 = 1;

   return p1;
}



private DymonPatchRequest getAgentPatchRequest(long interval,int prior)
{
   patch_request.reset(getMethodList(),interval,prior);
   if (patch_request.isEmpty()) return null;

   return patch_request;
}




private class Detailing extends DymonDetailing {

   Detailing()						{ super(for_process); }

   @Override public String getDetailName()		{ return getName(); }
   @Override protected double getLocalPriority()	{ return getAgentPriority(); }
   @Override public long getDetailInterval()		{ return INSTRUMENT_TIME; }
   @Override public long getDetailOverhead()		{ return OVERHEAD_FIXED_TIME; }
   @Override public double getDetailSlowdown()		{ return OVERHEAD_SLOWDOWN; }

   @Override protected DymonPatchRequest getPatchRequest(int p) {
      return getAgentPatchRequest(getDetailInterval(),p);
    }

}	// end of subclass Detailing





/********************************************************************************/
/*										*/
/*	Class to hold information for a socket method				*/
/*										*/
/********************************************************************************/

private class SocketData {

   private String class_name;
   private String method_name;
   private double total_count;
   private Map<String,long []> thread_counts;

   SocketData(String nm) {
      int idx = nm.indexOf('@');
      class_name = nm.substring(0,idx);
      method_name = nm.substring(idx+1);
      total_count = 0;
      thread_counts = new HashMap<String,long[]>();
    }

   void update(Element e) {
      total_count = IvyXml.getAttrDouble(e,"COUNT");
      for (Element te : IvyXml.elementsByTag(e,"THREAD")) {
	 String nm = IvyXml.getAttrString(te,"NAME");
	 long [] val = thread_counts.get(nm);
	 if (val == null) {
	    val = new long[1];
	    thread_counts.put(nm,val);
	  }
	 val[0] = IvyXml.getAttrLong(te,"COUNT");
       }
    }

   void output(IvyXmlWriter xw) {
      if (total_count/total_samples < report_threshold) return;

      xw.begin("SOCKETCOUNT");
      xw.field("CLASS",class_name);
      xw.field("METHOD",method_name);
      xw.field("COUNT",IvyFormat.formatCount(total_count));
      xw.field("PCT",IvyFormat.formatPercent(total_count/total_samples));
      xw.field("TIME",IvyFormat.formatTime(total_count/total_samples * total_time));
      for (Map.Entry<String,long []> ent : thread_counts.entrySet()) {
	 xw.begin("THREAD");
	 xw.field("NAME",ent.getKey());
	 double ct = ent.getValue()[0];
	 xw.field("COUNT",IvyFormat.formatCount(ct));
	 xw.field("PCT",IvyFormat.formatPercent(ct/total_count));
	 xw.field("TIME",IvyFormat.formatTime(ct/total_samples * total_time));
	 xw.end("THREAD");
       }
      xw.end("SOCKETCOUNT");
    }

}	// end of subclass SocketData


/********************************************************************************/
/*										*/
/*	Class to hold information about a particular socket			*/
/*										*/
/********************************************************************************/

private class SocketObject implements Comparable<SocketObject> {

   private String local_address;
   private String remote_address;
   private long num_read;
   private long num_write;
   private boolean is_closed;
   private long last_update;

   SocketObject(Element e) {
      local_address = IvyXml.getAttrString(e,"LOCAL");
      remote_address = IvyXml.getAttrString(e,"REMOTE");
    }

   void update(Element e) {
      num_read = IvyXml.getAttrLong(e,"READS");
      num_write = IvyXml.getAttrLong(e,"WRITES");
      if (num_read + num_write > max_ops) max_ops = num_read+num_write;
      is_closed = IvyXml.getAttrBool(e,"CLOSED");
      last_update = object_time;
    }

   void checkClosed(long when) {
      if (!is_closed && last_update < when) {
	 // System.err.println("DYMON:SOCKET: IMPLICIT CLOSE: " + local_address + " -> " + remote_address);
	 is_closed = true;
       }
    }

   long getReads()			{ return num_read; }
   long getWrites()			{ return num_write; }
   boolean isClosed()			{ return is_closed; }

   void output(IvyXmlWriter xw) {
      double ct = num_read+num_write;
      if (ct / max_ops < object_threshold) return;
      xw.begin("SOCKET");
      xw.field("READ",num_read);
      xw.field("WRITE",num_write);
      xw.field("CLOSED",is_closed);
      xw.textElement("LOCAL",local_address);
      xw.textElement("REMOTE",remote_address);
      xw.end("SOCKET");
    }

   public int compareTo(SocketObject so) {
      long v0 = num_read + num_write - so.num_read - so.num_write;
      if (v0 < 0) return -1;
      if (v0 > 0) return 1;
      if (is_closed && !so.is_closed) return -1;
      if (!is_closed && so.is_closed) return 1;
      if (remote_address == null && so.remote_address == null) return 0;
      if (remote_address == null) return -1;
      return remote_address.compareTo(so.remote_address);
    }

}	// end of subclass SocketObject



/********************************************************************************/
/*										*/
/*	Patch description class 						*/
/*										*/
/********************************************************************************/

private Collection<String> getMethodList()
{
   Collection<String> r = new ArrayList<String>();
   for (PatchInfo pi : patch_methods) {
      r.add(pi.getFullName());
    }
   return r;
}


private PatchInfo getPatchInfo(String c,String m)
{
   for (PatchInfo pi : patch_methods) {
      if (pi.match(c,m)) return pi;
    }
   return null;
}



private static class PatchInfo {

   private String patch_class;
   private String patch_method;
   private String method_name;
   private String patch_mode;

   PatchInfo(String c,String m,String md) {
      patch_class = c;
      patch_method = m;
      patch_mode = md;
      int idx = m.indexOf("(");
      if (idx > 0) method_name = m.substring(0,idx);
      else method_name = m;
    }

   String getFullName() 		{ return patch_class + "@" + patch_method; }
   String getMode()			{ return patch_mode; }

   boolean match(String c,String m) {
      if (!patch_class.equals(c)) return false;
      return method_name.equals(m) || patch_method.equals(m);
    }

}	// end of subclass PatchInfo




/********************************************************************************/
/*										*/
/*	Patch request class							*/
/*										*/
/********************************************************************************/

private class PatchRequest extends DymonPatchRequest {

   PatchRequest() {
      super(for_process,"SOCKETAGENT");
    }

   @Override protected void addPatchInfo(IvyXmlWriter xw,String c,String m) {
      PatchInfo pi = getPatchInfo(c,m);
      if (pi != null) {
	 xw.begin("PATCH");
	 xw.field("WHAT","ENTER");
	 xw.field("MODE","SOCKETAGENT_" + pi.getMode());
	 xw.end("PATCH");
       }
      else {
	 System.err.println("DYMON:SOCKET: Can't find patch information for " + c + " @ " + m);
       }
    }

   @Override String getRequestName()			{ return getName(); }

   @Override public PatchOverlap getPatchOverlap()	{ return PatchOverlap.CLASS; }

}	// end of subclass PatchRequest



}	// end of class DymonAgentSocket




/* end of DymonAgentSocket.java */


