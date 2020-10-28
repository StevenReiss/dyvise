/********************************************************************************/
/*										*/
/*		DymemGraph.java 						*/
/*										*/
/*	Build a gprof-like graph from memory reference information		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymem/DymemGraph.java,v 1.7 2012-10-05 00:52:49 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymemGraph.java,v $
 * Revision 1.7  2012-10-05 00:52:49  spr
 * Code clean up.
 *
 * Revision 1.6  2010-03-30 16:21:56  spr
 * Bug fixes and feature enhancements.  Start of interface for cycle elimination.
 *
 * Revision 1.5  2009-10-07 01:00:02  spr
 * Code cleanup for eclipse. Add support for standalone version.
 *
 * Revision 1.4  2009-09-19 00:09:27  spr
 * Update dymem with some bug fixes; initial support for reading dump files.
 *
 * Revision 1.3  2009-05-01 23:15:00  spr
 * Fix up data panel graphs, clean up unnecessary code.
 *
 * Revision 1.2  2009-04-11 23:45:29  spr
 * Provide updated visualizations with time line, hashing, etc.
 *
 * Revision 1.1  2009-03-20 02:10:12  spr
 * Add memory graph computation and display.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymem;


import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.*;

import org.w3c.dom.Element;

import quadprogj.QuadProgJ;

import java.io.PrintWriter;
import java.util.*;


class DymemGraph implements DymemConstants {




/********************************************************************************/
/*										*/
/*	Local Storage								*/
/*										*/
/********************************************************************************/

private DymemStats			stat_values;
private DymemParameters 		param_values;
private DymemCycleNamer 		cycle_namer;

private Map<String,ClassItem>		class_map;
private Map<MemoryItem,List<Reference>> reference_set;
private List<CycleItem> 		all_cycles;
private Set<MemoryItem> 		root_items;
private OutputCompareBy 		result_compareby;
private List<GraphNode> 		result_nodes;

private LinkedList<MemoryItem>		work_list;
private Map<MemoryItem,CycleItem>	cycle_map;
private int				mark_counter;
private int				cycle_count;
private Collection<CycleItem>		cycles_found;
private LinkedList<MemoryItem>		cycle_stack;
private long				graph_time;


private static final int		METHOD_COUNTS = 0;
private static final int		METHOD_REFS = 1;
private static final int		METHOD_PTRS = 2;

private static final int		NUM_METHODS = 3;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DymemGraph(Element node,DymemStats stats,DymemParameters params,DymemCycleNamer namer)
{
   stat_values = stats;
   param_values = params;
   cycle_namer = namer;

   class_map = new HashMap<String,ClassItem>();
   reference_set = new HashMap<MemoryItem,List<Reference>>();
   all_cycles = new ArrayList<CycleItem>();
   root_items = new HashSet<MemoryItem>();

   result_compareby = null;
   result_nodes = null;

   graph_time = IvyXml.getAttrLong(node,"NOW");

   setup(node);

   if (param_values.getFixRootLinks()) removeSpurriousLinks();

   computeCycles();
}





/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

List<GraphNode> getNodeList(OutputCompareBy ocb)
{
   if (result_compareby == ocb && result_nodes != null) return result_nodes;

   List<OutputItem> rslt = new ArrayList<OutputItem>(setupOutput());
   OutputComparator oc = new OutputComparator(ocb);

   long totcnt = 0;
   long totsize = 0;
   long totnew = 0;
   for (ClassItem ci : class_map.values()) {
      totcnt += ci.getObjectCount();
      totsize += ci.getSize();
      totnew += ci.getNewCount();
    }
   OutputItem totitm = new OutputItem("*TOTALS*",null,false,totcnt,totsize,totnew);
   totitm.setTotals(totcnt,totsize,totnew);

   Collections.sort(rslt,oc);
   for (int i = 0; i < rslt.size(); ++i) {
      OutputItem oi = rslt.get(i);
      oi.setIndex(i+1);
    }

   result_nodes = new ArrayList<GraphNode>();
   result_nodes.add(totitm);
   result_nodes.addAll(rslt);
   result_compareby = ocb;

   return result_nodes;
}



void clear()
{
   result_nodes = null;
}



long getAtTime()				{ return graph_time; }

DymemStats getStatistics()			{ return stat_values; }




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void generateOutput(IvyXmlWriter xw)
{
   OutputCompareBy ocb = OutputCompareBy.TOTAL_SIZE;

   List<OutputItem> rslt = new ArrayList<OutputItem>(setupOutput());
   OutputComparator oc = new OutputComparator(ocb);
   InComparator icmp = new InComparator(oc);
   OutComparator ocmp = new OutComparator(oc);

   Collections.sort(rslt,oc);
   for (int i = 0; i < rslt.size(); ++i) {
      OutputItem oi = rslt.get(i);
      oi.setIndex(i+1);
    }

   xw.begin("MEMORY");
   for (OutputItem oi : rslt) oi.output(xw,icmp,ocmp);
   xw.end("MEMORY");
}



/********************************************************************************/
/*										*/
/*	Cycle management methods						*/
/*										*/
/********************************************************************************/

void manageCycle(String nm)
{
   for (CycleItem ci : all_cycles) {
      if (ci.getName().equals(nm)) {
	 manageCycle(ci);
	 break;
       }
    }
}


private void manageCycle(CycleItem ci)
{
   DymemCycleCutter cc = new DymemCycleCutter(ci.getName());
   Set<MemoryItem> itms = new HashSet<MemoryItem>();
   for (MemoryItem mi : ci.getItems()) itms.add(mi);

   for (MemoryItem mi : ci.getItems()) {
      cc.addNode(mi.getName(),mi.getObjectCount());
      List<Reference> lr = reference_set.get(mi);
      if (lr != null) {
	 for (Reference r : lr) {
	    if (itms.contains(r.getToItem())) {
	       cc.addEdge(r.getFromItem().getName(),r.getToItem().getName(),r.getCount());
	     }
	  }
       }
    }

   cc.findCuts();
}


/********************************************************************************/
/*										*/
/*	Initial setup and cleanup methods					*/
/*										*/
/********************************************************************************/

private void setup(Element root)
{
   for (Element c : IvyXml.children(root,"CLASS")) {
      ClassItem ci = new ClassItem(IvyXml.getAttrString(c,"NAME"),
				      IvyXml.getAttrLong(c,"COUNT"),
				      IvyXml.getAttrLong(c,"SIZE"),
				      IvyXml.getAttrLong(c,"NEW",0));
      class_map.put(ci.getName(),ci);
    }

   for (Element r : IvyXml.children(root,"REF")) {
      String fnm = IvyXml.getAttrString(r,"FROM");
      String tnm = IvyXml.getAttrString(r,"TO");
      if (param_values.ignoreArc(fnm,tnm)) continue;
      ClassItem fi = findItem(fnm);
      ClassItem ti = findItem(tnm);
      if (fi == ti) {
	 fi.setSelfLoop();
	 continue;
       }

      Reference ri = new Reference(fi,ti,IvyXml.getAttrLong(r,"COUNT"));
      List<Reference> lr = reference_set.get(fi);
      if (lr == null) {
	 lr = new ArrayList<Reference>();
	 reference_set.put(fi,lr);
       }
      lr.add(ri);
      ti.addCount(ri.getCount());
    }
}



private ClassItem findItem(String name)
{
   ClassItem ci = class_map.get(name);
   if (ci == null) {
      ci = new ClassItem(name,1,0,0);
      class_map.put(name,ci);
    }
   return ci;
}




/********************************************************************************/
/*										*/
/*	Methods to compute the cycles in the graph				*/
/*										*/
/********************************************************************************/

private void computeCycles()
{
   work_list = new LinkedList<MemoryItem>();
   cycle_map = new HashMap<MemoryItem,CycleItem>();
   mark_counter = 1;

   for (ClassItem ci : class_map.values()) {
      if (ci.getCount() == 0) work_list.add(ci);
    }

   root_items.addAll(work_list);

   while (!work_list.isEmpty()) {
      // first remove all nodes using topological sort
      topSort();
      // then handle cycles
      findCycles();
    }

   work_list = null;

   /**************
   for (ClassItem ci : class_map.values()) {
      if (ci.getCount() > 0) {
	 System.err.println("MISSING: " + ci + " = " + ci.getCount());
       }
    }
   *************/
}



private void topSort()
{
   while (!work_list.isEmpty()) {
      MemoryItem mi = work_list.removeFirst();
      List<Reference> lr = reference_set.get(mi);
      // System.err.println("REMOVE " + mi);
      if (lr != null) {
	 for (Reference r : lr) {
	    MemoryItem fi = r.getFromItem();
	    MemoryItem ti = r.getToItem();
	    if (fi != ti) {
	       if (cycle_map.get(fi) == null || cycle_map.get(fi) != cycle_map.get(ti)) {
		  ti.decrementCount(r.getCount());
		  if (ti.getCount() == 0) work_list.add(ti);
		}
	     }
	  }
       }
    }
}



private void findCycles()
{
   ++mark_counter;
   cycle_count = 1;
   cycles_found = new ArrayList<CycleItem>();

   for (ClassItem ci : class_map.values()) {
      if (ci.getCount() != 0) {
	 cycle_stack = new LinkedList<MemoryItem>();
	 // System.err.println("BEGIN " + ci);
	 cycleSearch(ci);
	 cycle_stack = null;
       }
    }

   for (CycleItem cy : cycles_found) {
      setupCycle(cy);
    }

   cycles_found = null;
}



private void cycleSearch(MemoryItem ci)
{
   if (ci.getMark() == mark_counter || cycle_map.get(ci) != null) return;

   // System.err.println("SEARCH " + ci);
   ci.setMark(mark_counter);
   ci.setDfn(cycle_count);
   ci.setLowDfn(cycle_count);
   ++cycle_count;
   cycle_stack.add(ci);

   List<Reference> lr = reference_set.get(ci);
   int selfcount = 0;
   if (lr != null) {
      for (Reference r : lr) {
	 MemoryItem ti = r.getToItem();
	 if (ti == ci) selfcount += r.getCount();
	 int v = cycleNext(ti,ci.getDfn());
	 ci.setLowDfn(v);
       }
    }

   if (ci.getLowDfn() == ci.getDfn()) { 		// this is cycle head
      if (ci == cycle_stack.peekLast()) {
	// System.err.println("DISCARD " + ci + " " + selfcount);
	 cycle_stack.removeLast();
	 if (selfcount > 0) {
	    // This code doesn't always work, hence it isn't used
	    // self loops are removed by never creating the approrpriate links
	    ci.setSelfLoop();
	    ci.decrementCount(selfcount);
	    if (ci.getCount() == 0) work_list.add(ci);
	  }
       }
      else {
	 Collection<MemoryItem> ncyc = new ArrayList<MemoryItem>();
	 int idx = cycle_stack.indexOf(ci);
	 for (ListIterator<MemoryItem> li = cycle_stack.listIterator(idx); li.hasNext(); ) {
	    MemoryItem mi = li.next();
	    li.remove();
	    ncyc.add(mi);
	  }
	 if (checkCycleBase()) {
	    CycleItem nci = new CycleItem(ncyc);
	    setCycleName(nci);
	    cycles_found.add(nci);
	    // System.err.println("ADD CYCLE " + ncyc);
	  }
	 else {
	    // System.err.println("BAD CYCLE " + ncyc);
	    CycleItem nci = new CycleItem(ncyc);	// use the cycle anyway
	    setCycleName(nci);
	    cycles_found.add(nci);
	  }
       }
    }
}



private int cycleNext(MemoryItem mi,int dfn)
{
   if (mi.getMark() == mark_counter && mi.getDfn() < dfn) {
      if (cycle_stack.contains(mi)) return mi.getDfn();
    }
   else {
      cycleSearch(mi);
      return mi.getLowDfn();
    }

   return -1;
}



private boolean checkCycleBase()
{
   for (MemoryItem mi : cycle_stack) {
      List<Reference> lr = reference_set.get(mi);
      if (lr != null) {
	 for (Reference r : lr) {
	    MemoryItem ti = r.getToItem();
	    if (ti.getCount() > 0 && !cycle_stack.contains(ti)) {
	       // System.err.println("CYCLE CHECK FAIL " + mi + " => " + ti);
	       return false;
	     }
	  }
       }
    }

   return true;
}



private void setupCycle(CycleItem cy)
{
   all_cycles.add(cy);

   // System.err.println("SETUP " + cy);

   for (MemoryItem mi : cy.getItems()) cycle_map.put(mi,cy);

   for (MemoryItem mi : cy.getItems()) {
      List<Reference> lr = reference_set.get(mi);
      if (lr != null) {
	 for (Reference r : lr) {
	    MemoryItem ti = r.getToItem();
	    if (cycle_map.get(ti) == cy) {
	       ti.decrementCount(r.getCount());
	       if (ti.getCount() == 0) work_list.add(ti);
	     }
	  }
       }
    }
}



private void setCycleName(CycleItem ci)
{
   List<GraphItem> lgi = new ArrayList<GraphItem>(ci.getItems());
   String name = cycle_namer.getCycleName(lgi);
   ci.setName(name);
}




/********************************************************************************/
/*										*/
/*	Methods to remove spurrious root links					*/
/*										*/
/********************************************************************************/

private void removeSpurriousLinks()
{
   MemoryItem ri = class_map.get(ROOT_NAME);
   if (ri == null) {
      System.err.println("DYMEM: Root item not found");
      return;
    }

   List<Reference> lr = reference_set.get(ri);
   if (lr != null) {
      for (Iterator<Reference> it = lr.iterator(); it.hasNext(); ) {
	 Reference r = it.next();
	 MemoryItem ti = r.getToItem();
	 double cnt = ti.getCount();
	 double rcnt = r.getCount();
	 if (rcnt <= cnt*param_values.getRootCutoff() && rcnt != cnt) {
	    it.remove();
	    ti.decrementCount(r.getCount());
	  }
       }
    }

   MemoryItem fi = class_map.get(FINALIZER_NAME);
   if (fi != null) {
      lr = reference_set.get(fi);
      if (lr != null) {
	 for (Iterator<Reference> it = lr.iterator(); it.hasNext(); ) {
	    Reference r = it.next();
	    MemoryItem ti = r.getToItem();
	    double cnt = ti.getCount();
	    double rcnt = r.getCount();
	    if (rcnt != cnt) {
	       it.remove();
	       ti.decrementCount(r.getCount());
	     }
	  }
       }
    }

   // TODO: remove WEAK links here
}




private void fixRootLinks(Map<MemoryItem,OutputItem> outmap)
{
   OutputItem root = null;

   for (OutputItem itm : outmap.values()) {
      if (itm.getName().equals(ROOT_NAME)) {
	 root = itm;
	 break;
       }
    }

   if (root == null) return;

   List<OutputLink> removes = new ArrayList<OutputLink>();

   for (OutputLink ol : root.getOutLinks()) {
      OutputItem ti = (OutputItem) ol.getToNode();
      boolean havemult = false;
      for (OutputLink tol : ti.getInLinks()) {
	 if (tol.getFromNode() != root) {
	    havemult = true;
	    break;
	  }
       }
      if (havemult) {
	 double lcnt = ol.getNumRefs();
	 double tcnt = ti.getInRefs();
	 if (lcnt < tcnt*param_values.getRootCutoff() && lcnt != tcnt) {
	    removes.add(ol);
	  }
       }
    }

   for (OutputLink ol : removes) {
      ol.removeLink();
    }
}



private void removeArtificalNodes(Map<MemoryItem,OutputItem> outmap)
{
   OutputItem root = null;

   for (OutputItem itm : outmap.values()) {
      if (itm.getName().equals(ROOT_NAME)) {
	 root = itm;
	 break;
       }
    }

   if (root == null) return;

   Set<OutputItem> removes = new HashSet<OutputItem>();
   List<OutputLink> newlinks = new ArrayList<OutputLink>();

   for (Iterator<OutputLink> it = root.getOutLinks().iterator(); it.hasNext(); ) {
      OutputLink ol = it.next();
      OutputItem ti = (OutputItem) ol.getToNode();
      if (testRemoveNode(ti)) {
	 it.remove();
	 for (OutputLink tol : ti.getOutLinks()) {
	    tol.resetFromItem(root);
	    newlinks.add(tol);
	    removes.add(ti);
	  }

       }
    }

   if (removes.isEmpty()) return;

   for (OutputLink nol : newlinks) {
      boolean fnd = false;
      for (OutputLink ool : root.getOutLinks()) {
	 if (nol.getToNode() == ool.getToNode()) {
	    ool.mergeWith(nol);
	    fnd = true;
	    break;
	  }
       }
      if (!fnd) root.getOutLinks().add(nol);
    }

   for (Iterator<OutputItem> it = outmap.values().iterator(); it.hasNext(); ) {
      OutputItem oi = it.next();
      if (removes.contains(oi)) it.remove();
    }
}



private boolean testRemoveNode(OutputItem oi)
{
   if (!param_values.getUseSystemNode()) {
      if (oi.getName().equals(SYSTEM_NAME)) return true;
    }
   if (!param_values.getUseClassNodes()) {
      if (oi.getName().startsWith(CLASS_PREFIX)) return true;
    }
   if (!param_values.getUseThreadNodes()) {
      if (oi.getName().startsWith(THREAD_PREFIX)) return true;
    }

   return false;
}



/********************************************************************************/
/*										*/
/*	Methods to setup the output						*/
/*										*/
/********************************************************************************/

private Collection<OutputItem> setupOutput()
{
   Map<MemoryItem,OutputItem> outmap = new HashMap<MemoryItem,OutputItem>();

   defineOutputNodes(outmap);
   defineOutputLinks(outmap);

   assignSizeValues(outmap);

   // assignValues(outmap);

   removeArtificalNodes(outmap);

   // assignValues(outmap);

   if (param_values.getFixRootLinks()) {
      fixRootLinks(outmap);
    }

   assignValues(outmap);

   return outmap.values();
}



private void defineOutputNodes(Map<MemoryItem,OutputItem> outmap)
{
   for (CycleItem ci : all_cycles) {
      String nm = ci.getName();
      OutputItem oi = new OutputItem(nm,null,true,ci.getObjectCount(),ci.getSize(),ci.getNewCount());
      outmap.put(ci,oi);
    }

   for (ClassItem mi : class_map.values()) {
      String cnm = null;
      CycleItem ci = cycle_map.get(mi);
      if (ci != null) cnm = outmap.get(ci).getName();
      OutputItem oi = new OutputItem(mi.getName(),cnm,false,mi.getObjectCount(),mi.getSize(),mi.getNewCount());
      if (mi.isSelfLoop()) oi.setSelfCycle();
      outmap.put(mi,oi);
    }
}



private void defineOutputLinks(Map<MemoryItem,OutputItem> outmap)
{
   for (ClassItem fi : class_map.values()) {
      OutputItem foi = outmap.get(fi);
      CycleItem fcy = cycle_map.get(fi);
      Set<CycleItem> cycs = new HashSet<CycleItem>();
      List<Reference> lr = reference_set.get(fi);
      if (lr != null) {
	 for (Reference r : lr) {
	    MemoryItem ti = r.getToItem();
	    CycleItem ci = cycle_map.get(ti);
	    if (ti == fi) continue;			// ignore self arcs
	    if (ci != null) {
	       if (ci != fcy) cycs.add(ci);
	       continue;
	     }
	    OutputItem toi = outmap.get(ti);
	    OutputLink ol = new OutputLink(foi,toi,r.getCount());
	    foi.addFromLink(ol);
	    toi.addToLink(ol);
	  }
       }
      if (!cycs.isEmpty() && lr != null) {
	 for (CycleItem ci : cycs) {
	    OutputItem toi = outmap.get(ci);
	    long rct = 0;
	    for (Reference r : lr) {
	       if (cycle_map.get(r.getToItem()) == ci) rct += r.getCount();
	     }
	    OutputLink ol = new OutputLink(foi,toi,rct);
	    foi.addFromLink(ol);
	    toi.addToLink(ol);
	  }
       }
    }

   for (CycleItem ci : all_cycles) {
      OutputItem foi = outmap.get(ci);
      for (MemoryItem mi : ci.getItems()) {
	 int rct = 0;
	 for (MemoryItem xmi : ci.getItems()) {
	    List<Reference> lr = reference_set.get(xmi);
	    if (lr != null) {
	       for (Reference r : lr) {
		  if (r.getToItem() == mi) rct += r.getCount();
		}
	     }
	  }
	 OutputItem toi = outmap.get(mi);
	 OutputLink ol = new OutputLink(foi,toi,rct);
	 ol.setRefPercentage(1.0);
	 ol.setSizePercentage(1.0);
	 foi.addFromLink(ol);
	 toi.addToLink(ol);
       }
    }
}



private void assignValues(Map<MemoryItem,OutputItem> outmap)
{
   for (OutputItem oi : outmap.values()) {
      double rtot = oi.getInRefs();
      double tpct = 0;
      double rfnd = 0;
      for (OutputLink ol : oi.getInLinks()) {
	 double rct = ol.getNumRefs();
	 double rpct = rct / rtot;
	 ol.setRefPercentage(rpct);
	 double spct = ol.getSizePercent();
	 if (spct <= 0) ol.setSizePercentage(rpct);
	 tpct += ol.getSizePercent();
	 rfnd += rct;
       }
      if (rfnd != rtot)
	 System.err.println("DYMEM: FOUND BAD TOTAL");
      if (tpct > 0 && tpct != 1.0) {
	 for (OutputLink ol : oi.getInLinks()) {
	    double spct = ol.getSizePercent() * 1.0 / tpct;
	    ol.setSizePercentage(spct);
	  }
       }
    }


   for (MemoryItem ci : root_items) {
      OutputItem oi = outmap.get(ci);
      getTotals(oi);
    }
}



private void getTotals(OutputItem oi)
{
   if (oi.getTotalSize() > 0) return;

   // System.err.println("TOTALS " + oi.getName() + " " + oi.getCycleName());

   double tob = oi.getLocalCount();
   double toz = oi.getLocalSize();
   double tnw = oi.getLocalNewCount();
   if (oi.isCycle()) {
      tob = 0;
      toz = 0;
      tnw = 0;
    }
   for (OutputLink ol : oi.getOutLinks()) {
      OutputItem toi = ol.getToItem();
      getTotals(toi);
      tob += toi.getTotalCount() * ol.getRefPercent();
      toz += toi.getTotalSize() * ol.getSizePercent();
      tnw += toi.getTotalNewCount() * ol.getSizePercent();
      // System.err.println("\t" + toi.getName() + " " + toi.getTotalSize() + " " + ol.getSizePercent());
    }
   // System.err.println("    => " + oi.getName() + " " + toz);
   oi.setTotals(((long) tob),((long) toz),((long) tnw));
}




/********************************************************************************/
/*										*/
/*	Methods to use statistics to assign size values 			*/
/*										*/
/********************************************************************************/

private void assignSizeValues(Map<MemoryItem,OutputItem> outmap)
{
   if (stat_values == null) return;

   Map<OutputItem,double []> values = new HashMap<OutputItem,double []>();

   int mx = 0;
   for (MemoryItem ci : root_items) {
      OutputItem oi = outmap.get(ci);
      mx = assignSizeValues(oi,values,mx);
    }
}




@SuppressWarnings("unchecked")
private int assignSizeValues(OutputItem oi,Map<OutputItem,double []> values,int mx)
{
   if (values.get(oi) != null) return mx;

   double [] sz0 = computeTotalSize(oi,values,mx);
   if (mx == 0) mx = sz0.length;

   values.put(oi,sz0);

   oi.sortInLinks(OutputCompareBy.LOCAL_COUNT);
   List<OutputLink> inlinks = new ArrayList<OutputLink>(oi.getInLinks());

   double szm = 0;			// average size (for error comparison)
   for (int i = 0; i < mx; ++i) szm += sz0[i];
   szm /= mx;

   double totrefs = 0;
   for (OutputLink ol : inlinks) totrefs += ol.getNumRefs();

   if (mx < 2) {
      for (OutputLink ol : inlinks) {
	 ol.setSizePercentage(ol.getNumRefs() / totrefs);
       }
      return mx;
    }

   // first get the set of items we are going to check
   double totused = 0;
   List<double []>[] countdata = (List<double []>[]) new List<?>[NUM_METHODS];
   countdata[METHOD_COUNTS] = new ArrayList<double []>();
   countdata[METHOD_REFS] = new ArrayList<double []>();
   countdata[METHOD_PTRS] = new ArrayList<double []>();
   int ct = 0;
   for (Iterator<OutputLink> it = inlinks.iterator(); it.hasNext(); ) {
      OutputLink ol = it.next();
      OutputItem foi = ol.getFromItem();
      double [] v1 = stat_values.getCountValues(foi.getName(),mx);
      double [] vr1 = stat_values.getRefValues(foi.getName(),mx);
      double [] vp1 = stat_values.getPtrValues(foi.getName(),mx);

      if (checkUse(ol,totrefs,v1,vr1,vp1) && ct < mx) {
	 countdata[METHOD_COUNTS].add(v1);
	 countdata[METHOD_REFS].add(vr1);
	 countdata[METHOD_PTRS].add(vp1);
	 ++ct;
       }
      else {
	 totused += ol.getNumRefs();
	 ol.setSizePercentage(ol.getNumRefs() / totrefs);
	 it.remove();
       }
    }

   boolean fail = true;
   if (inlinks.size() >= 2) {
      double [] ydata = new double[mx];
      for (int i = 0; i < mx; ++i) {
	 ydata[i] = sz0[i] * (totrefs - totused)/totrefs;
       }

      double [][] rslt = solveQuads(countdata,ydata);
      double [] errs = new double[NUM_METHODS];
      double besterr = szm*param_values.getPredictionError();	// ignore if greater than this
      int bestmeth = -1;
      for (int i = 0; i < NUM_METHODS; ++i) {
	 errs[i] = computeError(rslt[i],ydata,countdata[i]);
	 if (errs[i] > 0 && errs[i] < besterr) {
	    besterr = errs[i];
	    bestmeth = i;
	  }
       }

      if (bestmeth >= 0) {
	 assignValues(inlinks,rslt[bestmeth],bestmeth,totrefs,totused,mx);
	 fail = false;
       }
    }

   if (fail) {
      for (OutputLink ol : inlinks) {
	 double f = ol.getNumRefs() / totrefs;
	 outputStats(ol,f,totrefs,totused,mx,-1);
	 ol.setSizePercentage(f);
       }
    }

   return mx;
}



private double [] computeTotalSize(OutputItem oi,Map<OutputItem,double []> values,int mx)
{
   double [] sz0 = stat_values.getSizeValues(oi.getName(),mx);
   if (mx == 0) mx = sz0.length;

   for (OutputLink ol : oi.getOutLinks()) {
      OutputItem toi = ol.getToItem();
      assignSizeValues(toi,values,mx);
      double [] nv = values.get(toi);
      double pct = ol.getSizePercent();
      if (pct < 0) pct = 0;
      for (int i = 0; i < mx; ++i) {
	 sz0[i] += nv[i]*pct;
       }
    }

   return sz0;
}



private boolean checkUse(OutputLink ol,double totrefs,double [] v1,double [] vr1,double [] vp1)
{
   int mx = v1.length;
   double v1a = v1[0];
   double v1b = v1[0];
   int mxct = 0;
   for (int i = 1; i < mx; ++i) {
      if (v1[i] < v1a) v1a = v1[i];
      if (v1[i] > v1b) {
	 v1b = v1[i];
	 mxct = 0;
       }
      else if (v1[i] == v1b) ++mxct;
    }

   boolean use = true;
   // handle case of all the same
   // handle case of all the values small
   // handle case of little variation in the values
   if (v1b - v1a < param_values.getMinimumReferenceFraction() * totrefs) use = false;
   if (mxct > mx / 2) use = false;
   if (ol.getNumRefs() < param_values.getMinRefCount() ||
	  totrefs < param_values.getMinTotalRefs())
      use = false;

   return use;
}




private double [][] solveQuads(List<double []> [] countdata,double [] ydata)
{
   int mx = ydata.length;
   int dim = countdata[0].size();

   double [][] xdata = new double[mx][dim];
   double [][] xrdata = new double[mx][dim];
   double [][] xpdata = new double[mx][dim];
   for (int i = 0; i < dim; ++i) {
      double [] x0 = countdata[METHOD_COUNTS].get(i);
      double [] xr0 = countdata[METHOD_REFS].get(i);
      double [] xp0 = countdata[METHOD_PTRS].get(i);
      for (int j = 0; j < mx; ++j) {
	 xdata[j][i] = x0[j];
	 xrdata[j][i] = xr0[j];
	 xpdata[j][i] = xp0[j];
       }
    }

   double [][] rslt = new double[NUM_METHODS][];

   rslt[METHOD_COUNTS] = solveQuadratic(xdata,ydata);
   rslt[METHOD_REFS] = solveQuadratic(xrdata,ydata);
   rslt[METHOD_PTRS] = solveQuadratic(xpdata,ydata);

   return rslt;
}




private double computeError(double [] soln,double [] sz,List<double []> coords)
{
   if (soln == null) return Double.POSITIVE_INFINITY;

   double numeq = sz.length;
   double numvar = soln.length;

   // compute error ourselves -- otherwise too much rounding error
   double err1 = 0;
   for (int i = 0; i < numeq; ++i) {
      double tot = 0;
      for (int j = 0; j < numvar; ++j) {
	 double [] cv = coords.get(j);
	 tot += cv[i]*soln[j];
	 if (soln[j] < 0) return Double.POSITIVE_INFINITY;
       }
      double err2 = (tot - sz[i]);
      err1 += err2*err2;
    }
   err1 /= numeq;
   err1 = Math.sqrt(err1);

   return err1;
}



private void assignValues(List<OutputLink> inlinks,double [] rslt,int method,
			     double totrefs,double totused,int mx)
{
   int dim = inlinks.size();
   double tot = 0;
   double [] tots = new double[dim];
   for (int i = 0; i < dim; ++i) {
      OutputLink ol = inlinks.get(i);
      OutputItem foi = ol.getFromItem();
      double ct = 0;
      switch (method) {
	 case METHOD_COUNTS :
	    ct = foi.getLocalCount();
	    break;
	 case METHOD_REFS :
	    ct = foi.getOutRefs();			// check if this is right
	    break;
	 case METHOD_PTRS :
	    ct = foi.getInRefs();
	    break;
       }
      tots[i] = rslt[i] * ct;
      tot += tots[i];
    }

   // set a lower bound on each element
   double dtot = 0;
   for (int i = 0; i < dim; ++i) {
      OutputLink ol = inlinks.get(i);
      double min = ol.getNumRefs()/totrefs;
      if (min > param_values.getMinimumFraction()) min = param_values.getMinimumFraction();
      if (tots[i] / tot < min) {
	 double delta = min*tot - tots[i];
	 dtot += delta;
	 tots[i] += delta;
       }
    }
   tot += dtot;

   double fract = (totrefs-totused)/totrefs;		// max we can allocate
   for (int i = 0; i < dim; ++i) {
      OutputLink ol = inlinks.get(i);
      double f = tots[i]/tot * fract;
      outputStats(ol,f,totrefs,totused,mx,method);
      ol.setSizePercentage(f);
    }
}




private void outputStats(OutputLink ol,double f,double totrefs,double totused,int mx,int method)
{
   PrintWriter pw = param_values.getStatWriter();
   if (pw == null) return;

   pw.print("\"" + ol.getFromItem().getName() + "\",\"" + ol.getToItem().getName() + "\",");
   pw.print(f);
   pw.print(",");
   pw.print(ol.getNumRefs()/totrefs);
   pw.print(",");
   pw.print(method);
   pw.print(",");
   pw.print(totrefs);
   pw.print(",");
   pw.print(totused);
   pw.print(",");
   pw.print(mx);
   pw.println();
}



/********************************************************************************/
/*										*/
/*	Methods for using quadratic minimization				*/
/*										*/
/********************************************************************************/

private double [] solveQuadratic(double [][] coord,double [] sz)
{
   int numeq = sz.length;
   int numvar = coord[0].length;

   double [][] trm2 = new double[numvar][numvar];
   double [] trm1 = new double[numvar];
   for (int i = 0; i < numvar; ++i) {
      trm1[i] = 0;
      for (int j = 0; j < numvar; ++j) trm2[i][j] = 0;
    }

   for (int n = 0; n < numeq; ++n) {
      for (int i = 0; i < numvar; ++i) {
	 trm2[i][i] += coord[n][i]*coord[n][i];
	 for (int j = i+1; j < numvar; ++j) {
	    trm2[i][j] += coord[n][i]*coord[n][j];
	    trm2[j][i] = trm2[i][j];
	  }
       }
      for (int i = 0; i < numvar; ++i) {
	 trm1[i] -= sz[n]*coord[n][i];
       }
    }

   DoubleMatrix2D gmat = new DenseDoubleMatrix2D(trm2);
   DoubleMatrix1D avec = new DenseDoubleMatrix1D(trm1);
   DoubleMatrix2D ceqmat = new SparseDoubleMatrix2D(numvar,0);
   DoubleMatrix1D beqvec = new SparseDoubleMatrix1D(0);
   DoubleMatrix2D ciqmat = new SparseDoubleMatrix2D(numvar,numvar);
   DoubleMatrix1D biqmat = new DenseDoubleMatrix1D(numvar);
   for (int i = 0; i < numvar; ++i) {
      ciqmat.set(i,i,1);
      biqmat.set(i,0);
    }

   try {
      QuadProgJ qp = new QuadProgJ(gmat,avec,ceqmat,beqvec,ciqmat,biqmat);
      return qp.getMinX();
    }
   catch (Throwable t) {
      // System.err.println("Problem setting up quadratic programming: " + t);
    }

   return null;
}




/********************************************************************************/
/*										*/
/*	Memory items								*/
/*										*/
/********************************************************************************/

private static abstract class MemoryItem implements GraphItem {

   private long   in_count;
   private int	  mark_value;
   private int	  depth_value;
   private int	  low_value;
   private boolean self_loop;

   protected MemoryItem() {
      in_count = 0;
      mark_value = -1;
      depth_value = 0;
      low_value = 0;
      self_loop = false;
    }

   void addCount(long c)			{ in_count += c; }
   void decrementCount(long c) { in_count -= c;
      // System.err.println("DECR " + this + " = " + in_count);
    }
   long getCount()				{ return in_count; }

   int getMark()				{ return mark_value; }
   void setMark(int v) {
      mark_value = v;
      depth_value = 0;
      low_value = 0;
    }

   void setDfn(int v)				{ depth_value = v; }
   void setLowDfn(int v) {
      if (v > 0 && (low_value == 0 || v < low_value)) low_value = v;
    }
   int getDfn() 				{ return depth_value; }
   int getLowDfn()				{ return low_value; }

   void setSelfLoop()				{ self_loop = true; }
   boolean isSelfLoop() 			{ return self_loop; }

   abstract long getObjectCount();
   abstract long getNewCount();

   public abstract long getSize();
   public abstract String getName();

}	// end of abstract subclass MemoryItem




private static class ClassItem extends MemoryItem {

   private String class_name;
   private long usage_count;
   private long total_size;
   private long new_count;

   ClassItem(String nm,long ct,long sz,long newct) {
      class_name = nm;
      usage_count = ct;
      total_size = sz;
      new_count = newct;
    }

   public String getName()			{ return class_name; }
   long getObjectCount()			{ return usage_count; }
   public long getSize()			{ return total_size; }
   public long getNewCount()			{ return new_count; }

   public String toString()			{ return class_name; }

}	// end of subclass ClassItem




private static class CycleItem extends MemoryItem {

   private Collection<MemoryItem> cycle_items;
   private String cycle_name;

   CycleItem(Collection<MemoryItem> elts) {
      cycle_items = new ArrayList<MemoryItem>(elts);
    }

   Collection<MemoryItem> getItems()		{ return cycle_items; }
   void setName(String s)			{ cycle_name = s; }
   public String getName()			{ return cycle_name; }

   public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("CYCLE:{");
      for (MemoryItem mi: cycle_items) {
	 buf.append(mi.toString());
	 buf.append(",");
      }
      buf.append("}");
      return buf.toString();
   }

   long getObjectCount() {
      long tot = 0;
      for (MemoryItem mi : cycle_items) tot += mi.getObjectCount();
      return tot;
    }

   long getNewCount() {
      long tot = 0;
      for (MemoryItem mi : cycle_items) tot += mi.getNewCount();
      return tot;
    }

   public long getSize() {
      long tot = 0;
      for (MemoryItem mi : cycle_items) tot += mi.getSize();
      return tot;
    }
}	// end of subclass CycleItem



/********************************************************************************/
/*										*/
/*	References								*/
/*										*/
/********************************************************************************/

private static class Reference {

   private MemoryItem from_item;
   private MemoryItem to_item;
   private long ref_count;

   Reference(MemoryItem f,MemoryItem t,long ct) {
      from_item = f;
      to_item = t;
      ref_count = ct;
    }

   MemoryItem getFromItem()				{ return from_item; }
   MemoryItem getToItem()				{ return to_item; }
   long getCount()					{ return ref_count; }

}	// end of subclass Reference




/********************************************************************************/
/*										*/
/*	OutputItem :: item for output						*/
/*										*/
/********************************************************************************/

private static class OutputItem implements GraphNode {

   private String output_name;
   private String cycle_name;
   private int output_index;
   private long local_objects;
   private long local_size;
   private long local_news;
   private long total_objects;
   private long total_size;
   private long total_news;
   private long in_refs;
   private long out_refs;
   private boolean is_cycle;
   private boolean is_selfcycle;
   private List<OutputLink> from_links;
   private List<OutputLink> to_links;

   OutputItem(String nm,String cnm,boolean cyc,long lo,long lsz,long lnew) {
      output_name = nm;
      cycle_name = cnm;
      output_index = 0;
      is_cycle = cyc;
      is_selfcycle = false;
      local_objects = lo;
      local_size = lsz;
      local_news = lnew;
      total_objects = 0;
      total_size = 0;
      total_news = 0;
      in_refs = 0;
      out_refs = 0;
      from_links = new ArrayList<OutputLink>();
      to_links = new ArrayList<OutputLink>();
    }

   public String getName()			{ return output_name; }
   public String getCycleName() 		{ return cycle_name; }
   public int getIndex()			{ return output_index; }

   public long getLocalSize()			{ return local_size; }
   public long getLocalCount()			{ return local_objects; }
   public long getLocalNewCount()		{ return local_news; }
   public long getTotalSize()			{ return total_size; }
   public long getTotalCount()			{ return total_objects; }
   public long getTotalNewCount()		{ return total_news; }

   public long getValue(OutputCompareBy cmp) {
      switch (cmp) {
         case LOCAL_SIZE :
            return getLocalSize();
         case TOTAL_SIZE :
            return getTotalSize();
         case LOCAL_COUNT :
            return getLocalCount();
         case TOTAL_COUNT :
            return getTotalCount();
         case LOCAL_NEW :
            return getLocalNewCount();
         case TOTAL_NEW :
            return getTotalNewCount();
         default :
            break;
       }
      return 0;
    }

   void setTotals(long tcnt,long tsz,long ncnt) {
      total_objects = tcnt;
      total_size = tsz;
      total_news = ncnt;
    }

   public boolean isCycle()			{ return is_cycle; }
   public boolean isSelfCycle() 		{ return is_selfcycle; }
   void setSelfCycle()				{ is_selfcycle = true; }

   public long getOutRefs()			{ return out_refs; }
   public long getInRefs()			{ return in_refs; }

   List<OutputLink> getInLinks()		{ return to_links; }
   List<OutputLink> getOutLinks()		{ return from_links; }

   public List<GraphLink> getSortedInLinks(OutputCompareBy ocb) {
      OutputComparator oc = new OutputComparator(ocb);
      InComparator icmp = new InComparator(oc);
      Collections.sort(to_links,icmp);
      List<GraphLink> rslt = new ArrayList<GraphLink>(to_links);
      return rslt;
    }

   public List<GraphLink> getSortedOutLinks(OutputCompareBy ocb) {
      OutputComparator oc = new OutputComparator(ocb);
      OutComparator ocmp = new OutComparator(oc);
      Collections.sort(from_links,ocmp);
      List<GraphLink> rslt = new ArrayList<GraphLink>(from_links);
      return rslt;
    }

   void sortInLinks(OutputCompareBy ocb) {
      OutputComparator oc = new OutputComparator(ocb);
      InComparator icmp = new InComparator(oc);
      Collections.sort(to_links,icmp);
    }

   void addFromLink(OutputLink ol) {
      from_links.add(ol);
      out_refs += ol.getNumRefs();
    }

   void addToLink(OutputLink ol) {
      to_links.add(ol);
      in_refs += ol.getNumRefs();
    }

   void removeFromLink(OutputLink ol) {
      if (!from_links.contains(ol)) {
	 System.err.println("DYMEM: missing from link");
	 return;
       }
      from_links.remove(ol);
      out_refs -= ol.getNumRefs();
    }

   void removeToLink(OutputLink ol) {
      if (!to_links.contains(ol)) {
	 System.err.println("DYMEM: missing to link");
	 return;
       }
      to_links.remove(ol);
      in_refs -= ol.getNumRefs();
    }

   void updateToRefs(long delta)		{ in_refs += delta; }

   void setIndex(int i) 			{ output_index = i; }

   void output(IvyXmlWriter xw,InComparator icmp,OutComparator ocmp) {
      xw.begin("ITEM");
      xw.field("NAME",output_name);
      if (cycle_name != null) xw.field("CYCLE",cycle_name);
      xw.field("INDEX",output_index);
      if (is_cycle) xw.field("ISCYCLE",true);
      xw.field("OBJECTS",local_objects);
      xw.field("SIZE",local_size);
      xw.field("NEW",local_news);
      xw.field("REFS",in_refs);
      xw.field("TOBJECTS",total_objects);
      xw.field("TSIZE",total_size);
      xw.field("TNEW",total_news);

      Collections.sort(to_links,icmp);

      for (OutputLink ol : to_links) {
	 xw.begin("FROM");
	 OutputItem oi = ol.getFromItem();
	 xw.field("NAME",oi.getName());
	 if (oi.getCycleName() != null) xw.field("CYCLE",oi.getCycleName());
	 xw.field("INDEX",oi.getIndex());
	 xw.field("REFS",ol.getNumRefs());
	 xw.end("FROM");
       }

      Collections.sort(from_links,ocmp);

      for (OutputLink ol : from_links) {
	 xw.begin("TO");
	 OutputItem oi = ol.getToItem();
	 xw.field("NAME",oi.getName());
	 if (oi.getCycleName() != null) xw.field("CYCLE",oi.getCycleName());
	 xw.field("INDEX",oi.getIndex());
	 xw.field("REFS",ol.getNumRefs());
	 xw.field("TOTREFS",oi.getInRefs());
	 xw.field("SPCT",ol.getSizePercent());
	 long tsz = (long)(ol.getSizePercent() * ol.getToItem().getTotalSize());
	 xw.field("TSIZE",tsz);
	 xw.end("TO");
       }
      xw.end("ITEM");
    }

   public String toString()			{ return getName(); }

}	// end of subclass OutputItem



private static class OutputLink implements GraphLink {

   private OutputItem from_item;
   private OutputItem to_item;
   private long num_refs;
   private double size_percent;
   private double ref_percent;

   OutputLink(OutputItem fitm,OutputItem titm,long ct) {
      from_item = fitm;
      to_item = titm;
      num_refs = ct;
      size_percent = -1;
      ref_percent = 0;
    }

   OutputItem getFromItem()			{ return from_item; }
   OutputItem getToItem()			{ return to_item; }

   void resetFromItem(OutputItem fitm)		{ from_item = fitm; }
   void mergeWith(OutputLink ol) {
      num_refs += ol.num_refs;
      size_percent += ol.size_percent;
      ref_percent += ol.ref_percent;
      to_item.updateToRefs(ol.num_refs);
      to_item.removeToLink(ol);
    }

   public long getNumRefs()			{ return num_refs; }
   public double getRefPercent()		{ return ref_percent; }
   public double getSizePercent()		{ return size_percent; }

   public GraphNode getFromNode()		{ return from_item; }
   public GraphNode getToNode() 		{ return to_item; }

   void setSizePercentage(double sp)		{ size_percent = sp; }
   void setRefPercentage(double rp)		{ ref_percent = rp; }

   void removeLink() {
      to_item.removeToLink(this);
      from_item.removeFromLink(this);
    }

   public String toString() {
      return from_item.getName() + "=>" + to_item.getName();
    }

}	// end of subclass OutputLink




private static class OutputComparator implements Comparator<OutputItem> {

   private OutputCompareBy compare_by;

   OutputComparator(OutputCompareBy cb) {
      compare_by = cb;
    }

   OutputCompareBy getComparison()			{ return compare_by; }

   public int compare(OutputItem i1,OutputItem i2) {
      int v = compare(i1.getValue(compare_by),i2.getValue(compare_by));
      if (v != 0) return v;
      v = compare(i1.getTotalSize(),i2.getTotalSize());
      if (v == 0) v = compare(i1.getTotalSize(),i2.getTotalSize());
      if (v == 0) v = compare(i1.getTotalCount(),i2.getTotalCount());
      if (v == 0) v = compare(i1.getLocalSize(),i2.getLocalSize());
      if (v == 0) v = compare(i1.getLocalCount(),i2.getLocalCount());
      if (v == 0) v = compare(i1.getTotalNewCount(),i2.getTotalNewCount());
      if (v == 0) v = compare(i1.getLocalNewCount(),i2.getLocalNewCount());
      if (v != 0) return v;
      if (i1.isCycle() && !i2.isCycle()) return -1;
      if (i2.isCycle() && !i1.isCycle()) return 1;
      return 0;
    }

   private int compare(long v1,long v2) {
      if (v1 > v2) return -1;
      if (v2 > v1) return 1;
      return 0;
    }

}	// end of subclass OutputComparator


private static class InComparator implements Comparator<OutputLink> {

   private OutputComparator item_compare;

   InComparator(OutputComparator oc) {
      item_compare = oc;
    }

   public int compare(OutputLink ol1,OutputLink ol2) {
      double v = 0;
      switch (item_compare.getComparison()) {
         case LOCAL_SIZE :
         case TOTAL_SIZE :
            v = ol1.getSizePercent() - ol2.getSizePercent();
            break;
         case LOCAL_COUNT :
         case TOTAL_COUNT :
            v = ol1.getNumRefs() - ol2.getNumRefs();
            break;
         case LOCAL_NEW :
         case TOTAL_NEW :
            v = ol1.getNumRefs() - ol2.getNumRefs();
            break;
         default :
            break;
       }
      if (v < 0) return -1;
      else if (v > 0) return 1;
      else return item_compare.compare(ol1.getFromItem(),ol2.getFromItem());
    }

}	// end of subclass InComparator




private static class OutComparator implements Comparator<OutputLink> {

   private OutputComparator item_compare;

   OutComparator(OutputComparator oc) {
      item_compare = oc;
    }

   public int compare(OutputLink ol1,OutputLink ol2) {
      double v = 0;
      OutputItem ot1 = ol1.getToItem();
      OutputItem ot2 = ol2.getToItem();
   
      switch (item_compare.getComparison()) {
         case LOCAL_SIZE :
         case TOTAL_SIZE :
            v = ot1.getTotalSize()*ol1.getSizePercent() - ot2.getTotalSize()*ol2.getSizePercent();
            break;
         case LOCAL_COUNT :
         case TOTAL_COUNT :
            v = ot1.getTotalCount()*ol1.getRefPercent() - ot2.getTotalCount()*ol2.getRefPercent();
            break;
         case LOCAL_NEW :
         case TOTAL_NEW :
            v = ot1.getTotalNewCount()*ol1.getRefPercent() - ot2.getTotalNewCount()*ol2.getRefPercent();
            break;
         default : 
            break;
       }
      if (v > 0) return -1;
      else if (v < 0) return 1;
      else return item_compare.compare(ot1,ot2);
    }

}	// end of subclass OutComparator




}	// end of class DymemGraph




/* end of DymemGraph.java */
