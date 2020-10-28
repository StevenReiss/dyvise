/********************************************************************************/
/*										*/
/*		DymonAgentReaction.java 					*/
/*										*/
/*	DYPER monitor agent for reaction monitoring				*/
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
r*  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymon/DymonAgentReaction.java,v 1.10 2012-10-05 00:52:56 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymonAgentReaction.java,v $
 * Revision 1.10  2012-10-05 00:52:56  spr
 * Code clean up.
 *
 * Revision 1.9  2011-03-10 02:26:33  spr
 * Code cleanup.
 *
 * Revision 1.8  2010-03-30 16:22:05  spr
 * Clean up the code.
 *
 * Revision 1.7  2009-09-19 00:09:46  spr
 * Update dymon for seletive agent insertion and removal.
 *
 * Revision 1.6  2009-06-04 18:53:50  spr
 * Set up for binary distribution.
 *
 * Revision 1.5  2009-05-12 22:22:51  spr
 * Handle system classes and methods.
 *
 * Revision 1.4  2009-04-11 23:46:18  spr
 * Use IvyFormat rather than local code.
 *
 * Revision 1.3  2009-03-20 02:06:50  spr
 * Update dymon: add time-based information, updated memory, etc.
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



class DymonAgentReaction extends DymonAgent {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private boolean 	do_detailings;
private long		total_samples;
private double		active_samples;
private long		total_coll;
private long		total_time;
private PatchRequest	patch_request;

private Map<String,CallBack>	callback_methods;
private Map<String,EventBack>	event_methods;
private TrieNode	root_node;

private Map<String,EventData>	event_data;

private Detailing	reaction_detailing;

private static final int	RUN = 0;
private static final int	WAIT = 1;
private static final int	IO = 2;
private static final int	TOTAL = 3;
private static final int	NUM_COUNT = 4;


private static final int MIN_SAMPLES = 10;
private static final double MIN_COUNT = 0.001;
private static final double WAIT_CUTOFF = 0.99;
private static final double IO_CUTOFF = 0.900; // 0.999;
private static final double RUN_CUTOFF = 0.90;

private static final double WAIT_TOTAL = 0.01;
private static final double IO_TOTAL = 0.10;

private static final long	INSTRUMENT_TIME = 15000;
private static final long	OVERHEAD_FIXED_TIME = 2000;
private static final double	OVERHEAD_SLOWDOWN = 0.30;

private static double		report_threshold = 0.005;

private static final String	NAME = "REACTION";
private static final String	SIMPLE_NAME = "FINDREACT";



enum NodeType {
   NODE_ANY,
   NODE_RUN,
   NODE_IO,
   NODE_WAIT,
   NODE_MIXED
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DymonAgentReaction(DymonMain dm,DymonProcess p,boolean doanal)
{
   super(dm,p);

   do_detailings = doanal;

   total_samples = 0;
   active_samples = 0;
   total_coll = 0;
   total_time = 0;
   patch_request = new PatchRequest();

   callback_methods = new HashMap<String,CallBack>();
   event_methods = new HashMap<String,EventBack>();
   root_node = new TrieNode();
   event_data = new HashMap<String,EventData>();

   reaction_detailing = new Detailing();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getName()
{
   return (do_detailings ? NAME : SIMPLE_NAME);
}

@Override public String getDyperAgentName()	{ return "REACTION"; }



@Override protected String getMonitorClass()
{
   return "edu.brown.cs.dyvise.dyper.DyperAgentReaction";
}



/********************************************************************************/
/*										*/
/*	Clear methods								*/
/*										*/
/********************************************************************************/

@Override public void doClear()
{
   total_samples = 0;
   active_samples = 0;
   total_coll = 0;
   total_time = 0;

   callback_methods.clear();
   event_methods.clear();
   root_node = new TrieNode();
   event_data.clear();

   clearRecentCounts();

   reaction_detailing.doClear();
}



/********************************************************************************/
/*										*/
/*	Report handling methods 						*/
/*										*/
/********************************************************************************/

@Override public void handleReport(Element r)
{
   long now = IvyXml.getAttrLong(r,"TIME");

   Element ce = IvyXml.getElementByTag(r,"REACTIONS");

   long montime = IvyXml.getAttrLong(ce,"MONTIME");

   total_time = montime;
   total_samples = IvyXml.getAttrLong(ce,"SAMPLES");
   active_samples = total_samples * reaction_detailing.getActiveFraction();
   total_coll = IvyXml.getAttrLong(ce,"TSAMPLES");

   for (Element te : IvyXml.elementsByTag(ce,"CALLBACK")) {
      String usr = IvyXml.getAttrString(te,"USER");
      long ct = IvyXml.getAttrInt(te,"STACK");
      CallBack cb = null;
      synchronized (callback_methods) {
	 cb = callback_methods.get(usr);
	 if (cb == null) {
	    cb = new CallBack(usr);
	    callback_methods.put(usr,cb);
	  }
       }
      cb.updateCount(ct);
      String name = cb.getFullName();
      EventData ed = findEventData(name);
      for (Element the : IvyXml.children(te,"THREAD")) {
	 cb.addThread(IvyXml.getText(the));
       }
      ed.updateTotalCount(ct);
    }

   Element trie = IvyXml.getElementByTag(ce,"TRIE");
   setupTrie(trie,root_node);

   long tot = for_process.getTotalSamples();
   long act = for_process.getActiveSamples();

   for (Element ee : IvyXml.elementsByTag(ce,"EVENT")) {
      int id = IvyXml.getAttrInt(ee,"MID");
      CounterData cd = for_process.getCounterData(id);
      if (cd == null) {
	 System.err.println("DYMON: Can't find reaction counter data for id = " + id);
	 continue;
       }
      String name = cd.getName();
      EventData ed = findEventData(name);
      ed.updateCounter(ee,cd.getActiveRunTime(now,tot,act,NAME),cd.getTimesActive(NAME),cd.isActive(NAME));
    }

   long totcb = 0;
   synchronized (callback_methods) {
      for (CallBack cb : callback_methods.values()) {
	 totcb += cb.getUsageCount();
       }
    }

   long totevt = 0;
   for (EventBack eb : event_methods.values()) {
      totevt += eb.getUsageCount();
    }

   long totctr = 0;
   synchronized (event_data) {
      for (EventData ed : event_data.values()) {
	 totctr += ed.getTotalCount();
       }
    }

   addDelta(new ReactionDelta(now,totctr,totcb+totevt));

   updateRecentCounts(totcb+totevt,total_coll);
}




private synchronized void setupTrie(Element xml,TrieNode nd)
{
   nd.setStats(xml);

   for (Element e : IvyXml.children(xml,"TRIENODE")) {
      String cnm = IvyXml.getAttrString(e,"CLASS");
      String mnm = IvyXml.getAttrString(e,"METHOD");
      boolean sys = IvyXml.getAttrBool(e,"SYS",false);
      TrieNode cn = nd.getChild(cnm,mnm,sys);
      setupTrie(e,cn);
    }
}



private EventData findEventData(String m)
{
   if (m == null) return null;

   EventData ed = null;

   synchronized (event_data) {
      ed = event_data.get(m);
      if (ed == null) {
	 ed = new EventData(m);
	 event_data.put(m,ed);
       }
    }

   return ed;
}




/********************************************************************************/
/*										*/
/*	Analysis methods							*/
/*										*/
/********************************************************************************/

@Override public void outputAnalysis(IvyXmlWriter xw)
{
   Map<String,EventBack> newhdlrs = new HashMap<String,EventBack>();

   computeNodeTypes(root_node);

   if (root_node.hasChildren()) {
      for (TrieNode cn : root_node.getChildren()) {
	 identifyHandlers(cn,newhdlrs);
       }
    }

   xw.begin("REACTION");

   try {
      xw.field("TOTTIME",IvyFormat.formatTime(total_time));
      xw.field("TOTSAMP",total_samples);
      xw.field("ACTSAMP",IvyFormat.formatCount(active_samples));

      double dttime = reaction_detailing.getActiveTime();
      xw.field("ACTIVE",IvyFormat.formatTime(dttime));

      Iterable<String> mthds = null;
      synchronized (callback_methods) {
	 mthds = new ArrayList<String>(callback_methods.keySet());
       }
      for (String s : mthds) {
	 xw.begin("CALLBACK");
	 xw.field("METHOD",s);
	 int idx = s.lastIndexOf("@");
	 if (idx >= 0) {
	    xw.field("CLASS",s.substring(0,idx));
	    xw.field("MNAME",s.substring(idx+1));
	  }
	 CallBack cb = callback_methods.get(s);
	 for (String t : cb.getThreads()) {
	    xw.begin("THREAD");
	    xw.field("NAME",t);
	    xw.end("THREAD");
	  }
	 xw.end();
       }
      for (Map.Entry<String,EventBack> ent : newhdlrs.entrySet()) {
	 EventBack eb = ent.getValue();
	 xw.begin("EVENT");
	 xw.field("TYPE",eb.getNodeType());
	 String s = ent.getKey();
	 xw.field("METHOD",s);
	 int idx = s.lastIndexOf("@");
	 if (idx >= 0) {
	    xw.field("CLASS",s.substring(0,idx));
	    xw.field("MNAME",s.substring(idx+1));
	  }
	 xw.field("THREAD",eb.getThreadName());
	 xw.end("EVENT");

	 EventData ed = findEventData(eb.getFullName());
	 ed.updateTotalCount(eb.getUsageCount());
       }

      event_methods = newhdlrs;

      Set<EventData> eventset;
      synchronized (event_data) {
	 eventset = new TreeSet<EventData>(event_data.values());
       }

      for (EventData ed : eventset) {
	 ed.output(xw);
       }
    }
   finally {
      xw.end("REACTION");
    }
}




/********************************************************************************/
/*										*/
/*	Methods to classify nodes by type					*/
/*										*/
/********************************************************************************/

private synchronized void computeNodeTypes(TrieNode node)
{
   NodeType nt = NodeType.NODE_ANY;

   int [] cnts = node.getCounts();
   if (cnts[TOTAL] >= MIN_SAMPLES && cnts[TOTAL] >= total_samples * MIN_COUNT) {
      nt = localNodeType(cnts);
    }

   if (nt == NodeType.NODE_ANY) {
      int [] tcnts = node.getTotalCounts();
      if (tcnts[TOTAL] >= MIN_SAMPLES*4 && tcnts[TOTAL] >= total_samples * MIN_COUNT) {
	 nt = localNodeType(tcnts);
       }
    }

   if (nt != node.getNodeType()) {
      node.setNodeType(nt);
      // System.err.println("REACTION: " + for_process.getProcessId() + ": SET TYPE " + node.getName() + " = " + nt);
    }

   if (node.hasChildren()) {
      for (TrieNode tn : node.getChildren()) {
	 computeNodeTypes(tn);
       }
    }
}



private NodeType localNodeType(int [] cnts)
{
   NodeType nt = NodeType.NODE_ANY;

   double w0 = ((double) cnts[WAIT]) / cnts[TOTAL];
   double i0 = ((double) cnts[IO]) / cnts[TOTAL];
   double r0 = ((double) cnts[RUN] + (double) cnts[IO]) / cnts[TOTAL];
   double w1 = ((double) cnts[WAIT])/total_samples;
   double i1 = ((double) cnts[IO])/total_samples;

   if (w0 >= WAIT_CUTOFF && w1 > WAIT_TOTAL) nt = NodeType.NODE_WAIT;

   if (nt == NodeType.NODE_ANY || i0 > w0) {
      if (i0 >= IO_CUTOFF && i1 > IO_TOTAL) nt = NodeType.NODE_IO;
    }

   if (nt == NodeType.NODE_ANY) {
      if (r0 >= RUN_CUTOFF) nt = NodeType.NODE_RUN;
      else nt = NodeType.NODE_MIXED;
    }

   return nt;
}




/********************************************************************************/
/*										*/
/*	Methods to actually find handlers given node classifications		*/
/*										*/
/********************************************************************************/

private void identifyHandlers(TrieNode node,Map<String,EventBack> newhdlrs)
{
   NodeType nt = node.getNodeType();
   boolean fnd = false;

   if (nt == NodeType.NODE_ANY || !node.hasChildren()) return;

   int numrun = 0;
   int numio = 0;
   int numwait = 0;
   for (TrieNode cn : node.getChildren()) {
      switch (cn.getNodeType()) {
	 case NODE_RUN :
	    ++numrun;
	    break;
	 case NODE_IO :
	    ++numio;
	    break;
	 case NODE_WAIT :
	    ++numwait;
            break;
         default :
            break;
       }
    }

   if (nt == NodeType.NODE_WAIT) {
      if (numwait == 0 && numrun + numio > 0) {
	 for (TrieNode cn : node.getChildren()) {
	    NodeType cnt = cn.getNodeType();
	    if (cnt == NodeType.NODE_IO || cnt == NodeType.NODE_RUN) {
	       addHandler(cn,nt,newhdlrs);
	     }
	  }
	 fnd = true;
       }
    }

   if (nt == NodeType.NODE_IO) {
      if (numio == 0 && numwait == 0 && numrun > 0) {
	 for (TrieNode cn : node.getChildren()) {
	    NodeType cnt = cn.getNodeType();
	    if (cnt == NodeType.NODE_RUN) {
	       addHandler(cn,nt,newhdlrs);
	     }
	  }
	 fnd = true;
       }
    }

   if (!fnd && numwait == 1 && (numrun + numio) >= 1) {
      for (TrieNode cn : node.getChildren()) {
	 NodeType cnt = cn.getNodeType();
	 if (cnt == NodeType.NODE_IO || cnt == NodeType.NODE_RUN || cnt == NodeType.NODE_ANY) {
	    addHandler(cn,NodeType.NODE_WAIT,newhdlrs);
	  }
       }
      fnd = true;
    }

   if (!fnd && numio == 1 && numrun >= 1) {
      for (TrieNode cn : node.getChildren()) {
	 NodeType cnt = cn.getNodeType();
	 if (cnt == NodeType.NODE_RUN || cnt == NodeType.NODE_ANY) {
	    addHandler(cn,NodeType.NODE_IO,newhdlrs);
	  }
       }
      fnd = true;
    }

   if (!fnd) {
      for (TrieNode cn : node.getChildren()) {
	 identifyHandlers(cn,newhdlrs);
       }
    }
}



private void addHandler(TrieNode cn,NodeType nt,Map<String,EventBack> newhdlrs)
{
   if (cn.getMethodName().startsWith("access$") || cn.isSystem()) {
      if (cn.hasChildren()) {
	 for (TrieNode ccn : cn.getChildren()) {
	    addHandler(ccn,nt,newhdlrs);
	  }
       }
      return;
    }

   newhdlrs.put(cn.getName(),new EventBack(nt,cn));
}



/********************************************************************************/
/*										*/
/*	Summary methods 							*/
/*										*/
/********************************************************************************/

@Override public double getSummaryValue()		{ return getAgentPriority(); }


@Override public double getConfidence()
{
   int csz = callback_methods.size();
   int esz = event_methods.size();

   double v = 0;

   if (csz > 0 && esz == 0) v = 0.5;
   if (csz > 0 && esz > 0) v = 1.0;

   if (reaction_detailing.getNumDetailing() == 0) v *= 0.5;

   return v;
}



@Override public void outputSummary(IvyXmlWriter xw)
{
   if (total_samples == 0) return;

   double p = getRecentRatio();

   xw.begin("METER");
   xw.field("NAME","EVENT %");
   xw.field("VALUE",p);
   xw.field("TYPE","PERCENT");
   xw.field("MIN",0);
   xw.field("MAX",1.0);
   xw.end();
}



/********************************************************************************/
/*										*/
/*	Immediate methods							*/
/*										*/
/********************************************************************************/

@Override public void outputImmediate(IvyXmlWriter xw)
{
   xw.begin("REACTIONDELTA");
   processDeltas(xw);
   xw.end("REACTIONDELTA");
}



/********************************************************************************/
/*										*/
/*	Callback representation 						*/
/*										*/
/********************************************************************************/

private static class CallBack {

   private String user_method;
   private long  usage_count;
   private Set<String> used_threads;

   CallBack(String um) {
      user_method = um;
      usage_count = 0;
      used_threads = new HashSet<String>();
    }

   void updateCount(long ct) {
      usage_count = ct;
    }

   long getUsageCount() 			{ return usage_count; }

   String getFullName() 			{ return user_method; }

   synchronized void addThread(String id) {
      int idx = id.indexOf("@");                // use only the class name
      if (idx > 0) id = id.substring(0,idx);
      used_threads.add(id);
    }

   synchronized Iterable<String> getThreads()	{ return new ArrayList<String>(used_threads); }


}	// end of subclass CallBack




/********************************************************************************/
/*										*/
/*	Event representation							*/
/*										*/
/********************************************************************************/

private static class EventBack {

   private NodeType node_type;
   private TrieNode trie_node;

   EventBack(NodeType nt,TrieNode tn) {
      node_type = nt;
      trie_node = tn;
    }

   NodeType getNodeType()			{ return node_type; }

   String getFullName() 			{ return trie_node.getName(); }
   long getUsageCount() 			{ return trie_node.getUsageCount(); }

   String getThreadName() {
      TrieNode tn = trie_node.getThreadRoot();
      return tn.getClassName();
    }

}	// end of subclass EventBack




/********************************************************************************/
/*										*/
/*	TrieNode representation 						*/
/*										*/
/********************************************************************************/

private static class TrieNode {

   private String full_name;
   private TrieNode parent_node;
   private String class_name;
   private String method_name;
   private int [] node_counts;
   private Map<String,TrieNode> child_nodes;
   private NodeType node_type;
   private int [] total_counts;
   private boolean total_valid;
   private boolean is_system;

   TrieNode() {
      parent_node = null;
      class_name = null;
      method_name = null;
      node_counts = new int[NUM_COUNT];
      for (int i = 0; i < NUM_COUNT; ++i) node_counts[i] = 0;
      child_nodes = null;
      node_type = NodeType.NODE_ANY;
      total_counts = new int[NUM_COUNT];
      total_valid = false;
      full_name = null;
      is_system = true;
    }

   TrieNode(TrieNode par,String cnm,String mnm,boolean sys) {
      this();
      parent_node = par;
      class_name = cnm;
      method_name = mnm;
      is_system = sys;
      full_name = class_name + "@" + method_name;
    }

   void setStats(Element e) {
      node_counts[RUN] = IvyXml.getAttrInt(e,"RUN");
      node_counts[IO] = IvyXml.getAttrInt(e,"IO");
      node_counts[WAIT] = IvyXml.getAttrInt(e,"WAIT");
      node_counts[TOTAL] = node_counts[RUN] + node_counts[IO] + node_counts[WAIT];
      invalidateTotals();
    }
   void setNodeType(NodeType nt)		{ node_type = nt; }

   TrieNode getChild(String cnm,String mnm,boolean sys) {
      String key = cnm + "@" + mnm;
      if (child_nodes == null) child_nodes = new HashMap<String,TrieNode>();
      TrieNode cn = child_nodes.get(key);
      if (cn == null) {
	 cn = new TrieNode(this,cnm,mnm,sys);
	 child_nodes.put(key,cn);
       }
      return cn;
    }

   String getName()				{ return full_name; }
   String getClassName()			{ return class_name; }
   String getMethodName()			{ return method_name; }
   boolean isSystem()				{ return is_system; }

   int [] getCounts()				{ return node_counts; }
   NodeType getNodeType()			{ return node_type; }

   long getUsageCount() 			{ return getTotalCounts()[TOTAL]; }

   boolean hasChildren()			{ return child_nodes != null; }
   Iterable<TrieNode> getChildren() {
      if (child_nodes == null) return new ArrayList<TrieNode>();
      return child_nodes.values();
    }
   int [] getTotalCounts() {
      if (child_nodes == null) return node_counts;
      if (!total_valid) {
	 for (int i = 0; i < NUM_COUNT; ++i) total_counts[i] = node_counts[i];
	 for (TrieNode tn : getChildren()) {
	    int [] ct = tn.getTotalCounts();
	    for (int i = 0; i < NUM_COUNT; ++i) total_counts[i] += ct[i];
	  }
	 total_valid = true;
       }
      return total_counts;
    }

   private void invalidateTotals() {
      if (!total_valid && child_nodes != null) return;
      total_valid = false;
      if (parent_node != null) parent_node.invalidateTotals();
    }

   TrieNode getThreadRoot() {
      if (parent_node == null) return this;
      TrieNode c = this;
      for ( ; c.parent_node != null; c = c.parent_node) {
	 TrieNode p = c.parent_node;
	 if (p.getName() == null) return c;
	 if (p.getName().equals("java.lang.Thread@run")) return c;
       }
      return c;
    }

}	// end of subclass TrieNode




/********************************************************************************/
/*										*/
/*	Detailing methods and classes						*/
/*										*/
/********************************************************************************/

public Collection<DymonDetailing> getDetailings()
{
   Collection<DymonDetailing> r = new ArrayList<DymonDetailing>();
   if (do_detailings) r.add(reaction_detailing);
   return r;
}



private double getAgentPriority()
{
   if (total_samples < MIN_SAMPLES) return 0;

   double v = getRecentRatio();

   if (v < 0) v = 0;
   if (v > 1) v = 1;

   return v * 0.9 + 0.1;
}



private DymonPatchRequest getAgentPatchRequest(long interval,int prior)
{
   List<String> items = new ArrayList<String>();

   synchronized (callback_methods) {
      for (CallBack cb : callback_methods.values()) {
	 items.add(cb.getFullName());
       }
    }

   for (EventBack eb : event_methods.values()) {
      items.add(eb.getFullName());
    }

   if (items.size() == 0) return null;

   patch_request.reset(items,interval,prior);
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
/*	Class to hold patch request						*/
/*										*/
/********************************************************************************/

private class PatchRequest extends DymonPatchRequest {

   PatchRequest() {
      super(for_process,"REACTIONAGENT");
    }

   @Override protected void addPatchInfo(IvyXmlWriter xw,String c,String m) {
      xw.begin("PATCH");
      xw.field("WHAT","ENTER");
      xw.field("MODE","REACTION_ENTER");
      xw.end("PATCH");
      xw.begin("PATCH");
      xw.field("WHAT","EXIT");
      xw.field("MODE","REACTION_EXIT");
      xw.end("PATCH");
    }

   @Override String getRequestName()			{ return getName(); }

}	// end of subclass PatchRequest




/********************************************************************************/
/*										*/
/*	Class to hold Event information for a method				*/
/*										*/
/********************************************************************************/

private class EventData implements Comparable<EventData> {

   private String class_name;
   private String method_name;
   private double check_count;
   private double check_time;
   private double total_stack;

   private Map<String,long []> thread_counts;
   private long count_active;
   private long num_active;


   EventData(String nm) {
      int idx = nm.indexOf('@');
      class_name = nm.substring(0,idx);
      method_name = nm.substring(idx+1);
      check_count = 0;
      check_time = 0;
      total_stack = 0;
      thread_counts = new HashMap<String,long[]>();

      count_active = 0;
      num_active = 0;
    }

   void updateTotalCount(long ct) {
      total_stack = ct;
    }
   double getTotalCount()			{ return total_stack; }

   void updateCounter(Element e,long active,long times,boolean isact) {
      count_active = active;
      if (!isact && times != num_active) {
	 num_active = times;
       }
      check_count = IvyXml.getAttrDouble(e,"COUNT");
      check_time = IvyXml.getAttrDouble(e,"TIME");

      for (Element te : IvyXml.elementsByTag(e,"THREAD")) {
	 String nm = IvyXml.getAttrString(te,"NAME");
	 long [] val = thread_counts.get(nm);
	 if (val == null) {
	    val = new long[3];
	    thread_counts.put(nm,val);
	  }
	 val[0] = IvyXml.getAttrLong(te,"COUNT");
	 val[1] = IvyXml.getAttrLong(te,"TIME");
       }
    }

   void output(IvyXmlWriter xw) {
      if (total_stack/total_samples < report_threshold) return;

      xw.begin("EVENTMETHOD");
      xw.field("CLASS",class_name);
      xw.field("METHOD",method_name);
      xw.field("COUNT",IvyFormat.formatCount(total_stack));
      xw.field("PCT",IvyFormat.formatPercent(total_stack/total_samples));
      xw.field("TIME",IvyFormat.formatTime(total_stack/total_samples * total_time));

      if (check_count > 0 && count_active > 0) {
	 double ct = check_count * total_time / count_active;
	 double tim = check_time * total_time / count_active;
	 xw.field("CALLS",IvyFormat.formatCount(ct));
	 xw.field("CALLTIME",IvyFormat.formatTime(tim));
	 xw.field("TIMEPERCALL",IvyFormat.formatInterval(tim/ct));
       }

      if (check_count > 0 && count_active > 0) {
	 Set<Map.Entry<String,long []>> tset = new TreeSet<Map.Entry<String,long []>>(
	    new ThreadCompare());
	 tset.addAll(thread_counts.entrySet());
	 for (Map.Entry<String,long []> ent : tset) {
	    xw.begin("THREAD");
	    xw.field("NAME",ent.getKey());
	    long [] vals = ent.getValue();
	    xw.field("COUNT",IvyFormat.formatCount(vals[0]*total_time/count_active));
	    xw.field("TIME",IvyFormat.formatTime(vals[1]*total_time/count_active));
	    xw.end("THREAD");
	  }
       }

      xw.end("EVENTMETHOD");
    }

   public int compareTo(EventData ev) {
      double v = check_time - ev.check_time;
      if (v > 0) return 1;
      else if (v < 0) return -1;
      v = check_count - ev.check_count;
      if (v > 0) return 1;
      else if (v < 0) return -1;
      int i = class_name.compareTo(ev.class_name);
      if (i != 0) return i;
      return method_name.compareTo(ev.method_name);
    }

}	// end of subclass EventData



/********************************************************************************/
/*										*/
/*	Class to hold immediate information					*/
/*										*/
/********************************************************************************/

private class ReactionDelta implements DeltaData {

   private long last_report;
   private long last_samples;
   private long last_active;
   private double last_count;
   private double last_total;

   ReactionDelta(long now,long ct,long tot) {
      last_report = now;
      last_samples = total_samples;
      last_active = (long) active_samples;
      last_count = ct;
      last_total = tot;
    }

   public void outputDelta(IvyXmlWriter xw,DeltaData prevd) {
      if (prevd == null) return;
      ReactionDelta prev = (ReactionDelta) prevd;
      xw.begin("DELTA");
      xw.field("NOW",last_report);
      xw.field("SAMPLE",last_samples - prev.last_samples);
      xw.field("ACTIVE",last_active - prev.last_active);
      xw.field("EVENTS",last_total - prev.last_total);
      if (prev.last_count > 0) {
	 xw.field("COUNT",last_count - prev.last_count);
       }
      xw.end("DELTA");
    }

}	// end of subclass ReactionDelta




}	// end of class DymonAgentReaction




/* end of DymonAgentReaction.java */




