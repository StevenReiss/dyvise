/********************************************************************************/
/*										*/
/*		DymonAgentStates.java						*/
/*										*/
/*	DYPER monitor agent for state determination				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymon/DymonAgentStates.java,v 1.6 2010-03-30 16:22:05 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymonAgentStates.java,v $
 * Revision 1.6  2010-03-30 16:22:05  spr
 * Clean up the code.
 *
 * Revision 1.5  2009-09-19 00:09:46  spr
 * Update dymon for seletive agent insertion and removal.
 *
 * Revision 1.4  2009-05-01 23:15:12  spr
 * Fix up state computation.  Clean up code.
 *
 * Revision 1.3  2009-04-28 18:01:15  spr
 * Update state information to produce state output.
 *
 * Revision 1.2  2009-04-11 23:46:18  spr
 * Use IvyFormat rather than local code.
 *
 * Revision 1.1  2009-03-20 02:15:54  spr
 * Add thread state agent controller.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymon;


import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.util.*;



class DymonAgentStates extends DymonAgent {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private TrieNode	root_node;

private static double child_cutoff = 0.05;
private static double state_cutoff = 0.01;
private static double state_min = 0.001;


private static final String	NAME = "STATES";


enum StateType {
   NONE,
   CALL,			// line with call(s)
   EXEC,			// line where execution occurs
   PARENT			// parent with substates
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DymonAgentStates(DymonMain dm,DymonProcess p)
{
   super(dm,p);

   root_node = new TrieNode();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getName()			{ return NAME; }


@Override public String getDyperAgentName()		{ return "STATES"; }


@Override protected String getMonitorClass()
{
   return "edu.brown.cs.dyvise.dyper.DyperAgentStates";
}



/********************************************************************************/
/*										*/
/*	Clear methods								*/
/*										*/
/********************************************************************************/

@Override public void doClear()
{
   root_node = new TrieNode();
}



/********************************************************************************/
/*										*/
/*	Summary methods 							*/
/*										*/
/********************************************************************************/

@Override public double getSummaryValue()	{ return -1; }




/********************************************************************************/
/*										*/
/*	Detailing methods							*/
/*										*/
/********************************************************************************/

public Collection<DymonDetailing> getDetailings()	{ return null; }



/********************************************************************************/
/*										*/
/*	Report handling methods 						*/
/*										*/
/********************************************************************************/

@Override public void handleReport(Element r)
{
   Element ce = IvyXml.getElementByTag(r,"STATES");

   IvyXml.getAttrLong(ce,"SAMPLES");
   IvyXml.getAttrLong(ce,"TSAMPLES");

   Element trie = IvyXml.getElementByTag(ce,"TRIE");
   setupTrie(trie,root_node);
}




private synchronized void setupTrie(Element xml,TrieNode nd)
{
   nd.setStats(xml);

   for (Element e : IvyXml.children(xml,"TRIENODE")) {
      String cnm = IvyXml.getAttrString(e,"CLASS");
      String mnm = IvyXml.getAttrString(e,"METHOD");
      int lno = IvyXml.getAttrInt(e,"LINE");
      boolean sy = IvyXml.getAttrBool(e,"SYSTEM");
      TrieNode cn = nd.getChild(cnm,mnm,lno,sy);
      setupTrie(e,cn);
    }
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public void outputAnalysis(IvyXmlWriter xw)
{
   xw.begin("STATES");

   identifyStates();

   // double t = root_node.getTotalCount();

   // outputNode(xw,root_node,t*0.005);

   outputStates(xw,root_node);

   xw.end("STATES");
}




@SuppressWarnings("unused")
private void outputNode(IvyXmlWriter xw,TrieNode tn,double thr)
{
   if (tn.getTotalCount() < thr) return;

   xw.begin("STATENODE");
   if (tn.getName() != null) xw.field("NAME",tn.getName());
   xw.field("COUNT",tn.getCount());
   xw.field("TOTAL",tn.getTotalCount());
   xw.field("STATE",tn.getStateType());
   if (tn.hasChildren()) {
      for (TrieNode cn : tn.getChildren()) {
	 outputNode(xw,cn,thr);
       }
    }
   xw.end("STATENODE");
}



private void outputStates(IvyXmlWriter xw,TrieNode tn)
{
   if (tn.getStateType() != StateType.NONE) {
      xw.begin("STATE");
      xw.field("TYPE",tn.getStateType());
      StringTokenizer tok = new StringTokenizer(tn.getName(),"@");
      String cls = tok.nextToken();
      String method = null;
      String line = null;
      if (tok.hasMoreTokens()) method = tok.nextToken();
      if (tok.hasMoreTokens()) line = tok.nextToken();
      xw.field("CLASS",cls);
      if (method != null) xw.field("METHOD",method);
      if (line != null) xw.field("LINE",line);
      TrieNode thn = tn.getThreadRoot();
      if (thn != null) xw.field("THREAD",thn.getClassName());

      xw.end("STATE");
      if (tn.getStateType() != StateType.PARENT) return;
    }

   if (tn.hasChildren()) {
      for (TrieNode cn : tn.getChildren()) {
	 outputStates(xw,cn);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Analysis methods							*/
/*										*/
/********************************************************************************/

private void identifyStates()
{
   root_node.findEligible(0);
   root_node.findStates(0);
}




/********************************************************************************/
/*										*/
/*	TrieNode representation 						*/
/*										*/
/********************************************************************************/

private static class TrieNode {

   private TrieNode parent_node;
   private String full_name;
   private String class_name;
   private String method_name;
   private long node_count;
   private Map<String,TrieNode> child_nodes;
   private long total_count;
   private boolean is_system;
   private boolean is_eligible;
   private StateType state_type;

   TrieNode() {
      class_name = null;
      method_name = null;
      node_count = 0;
      child_nodes = null;
      total_count = 0;
      full_name = null;
      is_system = true;
      is_eligible = false;
      state_type = StateType.NONE;
    }

   TrieNode(TrieNode par,String cnm,String mnm,int lno,boolean sys) {
      this();
      parent_node = par;
      class_name = cnm;
      method_name = mnm;
      full_name = class_name + "@" + method_name;
      if (lno > 0) full_name += "@" + lno;
      is_system = sys;
      is_eligible = false;
      state_type = StateType.NONE;
    }

   void setStats(Element e) {
      node_count = IvyXml.getAttrLong(e,"ACTIVE");
      total_count = IvyXml.getAttrLong(e,"TOTAL");
    }

   TrieNode getChild(String cnm,String mnm,int lno,boolean sys) {
      String key = cnm + "@" + mnm + "@" + lno;
      if (child_nodes == null) child_nodes = new HashMap<String,TrieNode>();
      TrieNode cn = child_nodes.get(key);
      if (cn == null) {
	 cn = new TrieNode(this,cnm,mnm,lno,sys);
	 child_nodes.put(key,cn);
       }
      return cn;
    }

   String getName()				{ return full_name; }
   String getClassName()			{ return class_name; }
   long getCount()				{ return node_count; }

   boolean hasChildren()			{ return child_nodes != null; }
   Iterable<TrieNode> getChildren()		{ return child_nodes.values(); }
   boolean isEligible() 			{ return is_eligible; }
   StateType getStateType()			{ return state_type; }

   long getTotalCount() 			{ return total_count; }


   static final int HAVE_SYSTEM = 1;
   static final int HAVE_ELIGIBLE = 2;
   static final int PRECLUDE = HAVE_SYSTEM | HAVE_ELIGIBLE;

   int findEligible(double total) {
      if (total == 0) total = total_count;

      int sts = 0;
      if (is_system) {
	 is_eligible = false;
	 sts |= HAVE_SYSTEM;
       }
      else {
	 if (total_count < total * state_min) is_eligible = false;
	 else is_eligible = true;
       }

      if (hasChildren()) {
	 for (TrieNode cn : getChildren()) {
	    int csts = cn.findEligible(total);
	    if (csts == PRECLUDE) is_eligible = false;
	    sts |= csts;
	  }
       }

      if (is_eligible) sts |= HAVE_ELIGIBLE;

      return sts;
    }

   void findStates(double total) {
      if (total == 0) total = total_count;

      if (!is_eligible) {
	 if (hasChildren()) {
	    for (TrieNode cn : getChildren()) cn.findStates(total);
	  }
       }
      else {
	 if (!hasChildren()) state_type = StateType.EXEC;
	 else {
	    state_type = StateType.NONE;
	    double cutoff = getTotalCount() * child_cutoff;
	    int num = 0;
	    double rcnt = 0;
	    String rtn = null;
	    for (TrieNode cn : getChildren()) {
	       if (cn.getTotalCount() >= cutoff && cn.isEligible()) {
		  if (rtn == null) {
		     rtn = cn.getName();
		     rcnt = cn.getTotalCount();
		     ++num;
		   }
		  else if (cn.getName().equals(rtn)) {
		     if (rcnt > total*state_cutoff ||
			    cn.getTotalCount() >= total*state_cutoff) ++num;
		   }
		  else ++num;
		}
	     }
	    if (num > 1) {
	       state_type = StateType.PARENT;
	       for (TrieNode cn : getChildren()) cn.findStates(total);
	     }
	    else {
	       state_type = StateType.CALL;
	       for (TrieNode cn : getChildren()) cn.clearStates();
	     }
	  }
       }
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

   private void clearStates() {
      state_type = StateType.NONE;
      if (hasChildren()) {
	 for (TrieNode cn : getChildren()) cn.clearStates();
       }
    }

}	// end of subclass TrieNode




}	// end of class DymonAgentStates




/* end of DymonAgentStates.java */





