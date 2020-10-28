/********************************************************************************/
/*										*/
/*		DymemCycleCutter.java						*/
/*										*/
/*	Determine how or if to cut a cycle					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymem/DymemCycleCutter.java,v 1.2 2011-03-10 02:33:07 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymemCycleCutter.java,v $
 * Revision 1.2  2011-03-10 02:33:07  spr
 * Code cleanup.
 *
 * Revision 1.1  2010-03-30 21:27:23  spr
 * Add initial version of cycle cutting module.
 * DymemGraph.java,v $
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymem;


import java.util.*;


class DymemCycleCutter implements DymemConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String cycle_name;
private Map<String,CycleNode> node_map;
private List<CycleArc>	all_arcs;

private Map<CycleNode,CycleNode> merge_map;

private static boolean use_random = false;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DymemCycleCutter(String nm)
{
   cycle_name = nm;
   node_map = new TreeMap<String,CycleNode>();
   all_arcs = new ArrayList<CycleArc>();
   merge_map = new HashMap<CycleNode,CycleNode>();
}



/********************************************************************************/
/*										*/
/*	Methods to build the graph						*/
/*										*/
/********************************************************************************/

void addNode(String nm,double wt)
{
   CycleNode cn = findNode(nm);
   cn.setWeight(wt);
}



void addEdge(String fm,String to,double wt)
{
   CycleNode fn = findNode(fm);
   CycleNode tn = findNode(to);
   CycleArc ca = new CycleArc(fn,tn,wt);
   fn.addFromArc(ca);
   tn.addToArc(ca);
   all_arcs.add(ca);
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

void findCuts()
{
   printCycle();

   initialMerge();

   for ( ; ; ) {
      freeMerge();
      CycleArc ca = selectArc();
      if (ca == null) break;
      CycleNode cna = findMergedNode(ca.getFromNode());
      CycleNode cnb = findMergedNode(ca.getToNode());
      System.err.println("MERGE (" + ca.getWeight() + ") " + cna.getName() + " WITH " + cnb.getName());
      mergeNodes(cna,cnb);
      printCycle();
      Set<CycleNode> nodes = new HashSet<CycleNode>();
      for (CycleNode cn : node_map.values()) {
	 CycleNode cn1 = findMergedNode(cn);
	 nodes.add(cn1);
       }
      System.err.println("NUM ITEMS = " + nodes.size());
      if (nodes.size() == 1) break;
    }
}




private void initialMerge()
{
   for (CycleArc ca : all_arcs) {
      CycleNode fn = ca.getFromNode();
      CycleNode tn = ca.getToNode();
      String fnm = fn.getName();
      String tnm = tn.getName();
      int idx = tnm.indexOf(':');
      boolean merge = false;
      if (idx > 0) merge = true;
      else if (fnm.startsWith("[") && fnm.substring(1).equals(tnm)) merge = true;

      if (merge) {
	 System.err.println("INITIAL MERGE (" + ca.getWeight() + ") " + fn.getName() + " WITH " + tn.getName());
	 fn = findMergedNode(fn);
	 tn = findMergedNode(tn);
	 mergeNodes(fn,tn);
       }
    }
}



private void freeMerge()
{
   for (CycleArc ca : all_arcs) {
      CycleNode fn = ca.getFromNode();
      CycleNode tn = ca.getToNode();
      CycleNode fn1 = findMergedNode(fn);
      CycleNode tn1 = findMergedNode(tn);
      if (fn1 == tn1) continue;
      CycleNode tgt = null;
      int ntgt = 0;
      for (CycleArc cb : all_arcs) {
	 CycleNode bfn = findMergedNode(cb.getFromNode());
	 if (bfn == tn1) {
	    CycleNode btn = findMergedNode(cb.getToNode());
	    if (tgt != btn) {
	       ++ntgt;
	       tgt = btn;
	       if (ntgt > 1) break;
	     }
	  }
       }
      if (ntgt != 1) continue;
      boolean domerge = false;
      for (CycleArc cb : all_arcs) {
	 CycleNode bfn = findMergedNode(cb.getFromNode());
	 CycleNode btn = findMergedNode(cb.getToNode());
	 if (bfn == fn1 && btn == tgt) {
	    domerge = true;
	    break;
	  }
       }
      if (domerge) {
	 System.err.println("FREE MERGE (" + ca.getWeight() + ") " + fn.getName() + " WITH " + tn.getName());
	 fn = findMergedNode(fn);
	 tn = findMergedNode(tn);
	 mergeNodes(fn,tn);
       }
    }
}



private double getTotalArcWeight()
{
   double tot = 0;
   for (CycleArc ca : all_arcs) {
      CycleNode fn = findMergedNode(ca.getFromNode());
      CycleNode tn = findMergedNode(ca.getToNode());
      if (fn != tn) tot += ca.getWeight();
    }

   return tot;
}


private double getTotalNodeCount()
{
   double tot = 0;
   for (CycleNode cn : node_map.values()) {
      tot += cn.getWeight();
    }

   return tot;
}


private CycleNode findMergedNode(CycleNode cn)
{
   CycleNode cn0 = merge_map.get(cn);
   if (cn0 == null) return cn;

   CycleNode cn1 = findMergedNode(cn0);

   if (cn1 != cn0) merge_map.put(cn,cn1);

   return cn1;
}



private CycleArc selectArc()
{
   double tgt = getTotalArcWeight();
   System.err.println("TOTAL ARC WEIGHT = " + tgt);
   System.err.println("TOTAL NODE COUNT = " + getTotalNodeCount());

   tgt *= Math.random();

   if (use_random) {
      double tot = 0;
      for (CycleArc ca : all_arcs) {
	 CycleNode fn = findMergedNode(ca.getFromNode());
	 CycleNode tn = findMergedNode(ca.getToNode());
	 if (fn != tn) {
	    tot += ca.getWeight();
	    if (tot >= tgt) return ca;
	  }
       }
    }
   else {
      Map<CycleArc,CycleArc> arcs = new HashMap<CycleArc,CycleArc>();
      for (CycleArc ca : all_arcs) {
	 CycleNode fn = findMergedNode(ca.getFromNode());
	 CycleNode tn = findMergedNode(ca.getToNode());
	 if (fn != tn) {
	    double v = ca.getWeight();
	    CycleArc ta = new CycleArc(fn,tn,v);
	    CycleArc tb = arcs.get(ta);
	    if (tb == null) arcs.put(ta,ta);
	    else tb.addWeight(v);
	  }
       }
      double bestwt = 0;
      CycleArc ba = null;
      for (CycleArc ca : arcs.keySet()) {
	 if (ca.getWeight() > bestwt) {
	    bestwt = ca.getWeight();
	    ba = ca;
	  }
       }
      return ba;
    }

   return null; 	// should never get here
}




private void printCycle()
{
   System.err.println("CYCLE: " + cycle_name);
   for (CycleNode cn : node_map.values()) {
      CycleNode cn1 = findMergedNode(cn);
      if (cn1 != cn) continue;
      double totwt = 0;
      for (CycleNode cn2 : node_map.values()) {
	 if (findMergedNode(cn2) == cn) totwt += cn2.getWeight();
       }
      System.err.println("   ITEM " + cn.getName() + " (" + totwt + "): ");
      Map<String,double []> arcs = new TreeMap<String,double[]>();
      for (CycleNode cn2 : node_map.values()) {
	 if (findMergedNode(cn2) == cn) {
	    for (CycleArc ca : cn2.getFromArcs()) {
	       CycleNode cn3 = findMergedNode(ca.getToNode());
	       if (cn3 != cn) {
		  double [] val = arcs.get(cn3.getName());
		  if (val == null) {
		     val = new double[1];
		     val[0] = 0;
		     arcs.put(cn3.getName(),val);
		   }
		  val[0] += ca.getWeight();
		}
	     }
	  }
       }
      for (Map.Entry<String,double []> ent : arcs.entrySet()) {
	 System.err.println("      LINK (" + ent.getValue()[0] + "): " + ent.getKey());
       }
    }
   System.err.println();
}




private void mergeNodes(CycleNode n1,CycleNode n2)
{
   n1 = findMergedNode(n1);
   n2 = findMergedNode(n2);
   if (n1 == n2) return;

   if (n1.getName().length() < n2.getName().length()) {
      merge_map.put(n2,n1);
    }
   else {
      merge_map.put(n1,n2);
    }
}



/********************************************************************************/
/*										*/
/*	Node access methods							*/
/*										*/
/********************************************************************************/

private CycleNode findNode(String nm)
{
   CycleNode cn = node_map.get(nm);
   if (cn == null) {
      cn = new CycleNode(nm);
      node_map.put(nm,cn);
    }
   return cn;
}




/********************************************************************************/
/*										*/
/*	Node class								*/
/*										*/
/********************************************************************************/

private static class CycleNode {

   private String node_name;
   private double node_weight;
   private List<CycleArc> from_arcs;
   private List<CycleArc> to_arcs;

   CycleNode(String nm) {
      node_name = nm;
      node_weight = 0;
      from_arcs = new ArrayList<CycleArc>();
      to_arcs = new ArrayList<CycleArc>();
    }

   void setWeight(double wt)			{ node_weight = wt; }

   void addFromArc(CycleArc ca) 		{ from_arcs.add(ca); }
   void addToArc(CycleArc ca)			{ to_arcs.add(ca); }

   String getName()				{ return node_name; }
   double getWeight()				{ return node_weight; }
   List<CycleArc> getFromArcs() 		{ return from_arcs; }

}	// end of inner class CycleNode



/********************************************************************************/
/*										*/
/*	Arc class								*/
/*										*/
/********************************************************************************/

private static class CycleArc {

   private CycleNode from_node;
   private CycleNode to_node;
   private double arc_weight;

   CycleArc(CycleNode fn,CycleNode tn,double wt) {
      from_node = fn;
      to_node = tn;
      arc_weight = wt;
    }

   CycleNode getFromNode()			{ return from_node; }
   CycleNode getToNode()			{ return to_node; }
   double getWeight()				{ return arc_weight; }

   void addWeight(double v)			{ arc_weight += v; }

   @Override public int hashCode() {
      return from_node.hashCode() + to_node.hashCode()*3;
    }

   @Override public boolean equals(Object o) {
      if (o instanceof CycleArc) {
	 CycleArc ca = (CycleArc) o;
	 if (from_node == ca.from_node && to_node == ca.to_node) return true;
       }
      return false;
    }

}	// end of inner class CycleArc



}	// end of class DymemCycleCutter




/* end of DymemCycleCutter.java */
