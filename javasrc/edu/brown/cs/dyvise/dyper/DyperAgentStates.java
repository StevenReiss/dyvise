/********************************************************************************/
/*										*/
/*		DyperAgentStates.java						*/
/*										*/
/*	Monitor agent that finds program states 				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyper/DyperAgentStates.java,v 1.6 2016/11/02 18:59:18 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyperAgentStates.java,v $
 * Revision 1.6  2016/11/02 18:59:18  spr
 * Move to asm5
 *
 * Revision 1.5  2012-10-05 00:53:01  spr
 * Code clean up.
 *
 * Revision 1.4  2010-03-30 16:19:22  spr
 * Remove debugging. Add xjar to Make.data.
 *
 * Revision 1.3  2009-09-19 00:13:28  spr
 * Update agents for dynamic insertion and removal.
 *
 * Revision 1.2  2009-04-28 18:01:19  spr
 * Update state information to produce state output.
 *
 * Revision 1.1  2009-03-20 02:11:53  spr
 * Add thread state computation agent.
 *
 *
 ********************************************************************************/


package edu.brown.cs.dyvise.dyper;


import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.List;




public class DyperAgentStates extends DyperAgent {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private long		last_sample;
private long		sample_count;
private long		tsample_count;
private TrieNode	root_node;
private int		max_level;
private long		total_count;

private static final int	MAX_LEVEL = 15;
private static final boolean	use_lines = true;
private static final double	NODE_CUTOFF = 0.0005;
private static final double	CHILD_MAX = 0.80;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public DyperAgentStates(DyperControl dc)
{
   super(dc,"STATES");

   last_sample = 0;
   sample_count = 0;
   tsample_count = 0;
   root_node = new TrieNode();
   max_level = MAX_LEVEL;
   total_count = 0;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public boolean setParameter(String s,String v)
{
   if (s.equals("STATES_MAX_LEVEL")) {
      max_level = Integer.parseInt(v);
    }
   else return false;

   return true;
}



public String getParameter(String nm)
{
   String rslt = null;

   if (nm.equals("STATES_MAX_LEVEL")) {
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

   switch (ti.getThreadState()) {
      case NEW :
      case TERMINATED :
      // case WAITING :
      // case TIMED_WAITING :
	 return;
      default :
	 break;
    }
   if (trc.length == 0) return;
   String tnm = ti.getThreadName();
   if (tnm != null) {
      if (tnm.equals("Reference Handler")) return;
      if (tnm.equals("Finalizer")) return;
    }

   int sysend = -1;
   ++tsample_count;
   for (int j = 0; j < trc.length; ++j) {
      StackTraceElement te = trc[j];
      String nm = te.getClassName();
      if (!the_control.isSystemClass(nm)) {
	 sysend = j;
	 break;
       }
    }

   if (sysend == -1) {
      sysend = trc.length-1;
      return;				// ignore system-only stacks for now
    }

   root_node.insert(trc,sysend);
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
   root_node = new TrieNode();
   total_count = 0;
}



/********************************************************************************/
/*										*/
/*	Reporting methods							*/
/*										*/
/********************************************************************************/

public void generateReport(ReportType rt,DyperXmlWriter xw,long now)
{
   xw.begin("STATES");
   xw.field("MONTIME",getMonitoredTime(now));
   xw.field("SAMPLES",sample_count);
   xw.field("TSAMPLES",tsample_count);
   xw.field("LAST",last_sample);
   xw.field("TOTAL",total_count);

   double cutoff = total_count * NODE_CUTOFF;

   xw.begin("TRIE");
   root_node.report(xw,cutoff,null);
   xw.end();

   xw.end("STATES");
}



/********************************************************************************/
/*										*/
/*	Methods for maintaining a method-trie					*/
/*										*/
/********************************************************************************/

private class TrieNode {

   private String class_name;
   private String method_name;
   private int line_number;
   private long num_active;
   private long tot_active;
   private List<TrieNode> next_nodes;
   private boolean is_system;

   TrieNode() {
      method_name = null;
      class_name = null;
      line_number = 0;
      num_active = 0;
      tot_active = 0;
      next_nodes = null;
      is_system = true;
    }

   TrieNode(StackTraceElement trc) {
      class_name = trc.getClassName();
      method_name = trc.getMethodName();
      line_number = trc.getLineNumber();
      num_active = 0;
      tot_active = 0;
      next_nodes = null;
      is_system = the_control.isSystemClass(class_name);
    }

   private void count() {
      ++num_active;
      ++tot_active;
      ++total_count;
    }

   void insert(StackTraceElement [] trc,int idx) {
      insertItem(trc,trc.length-1,idx,0);
    }


   private boolean insertItem(StackTraceElement [] trc,int idx,int base,int lvl) {
      if (idx < base || lvl >= max_level) {
	 if (class_name == null) return false;
	 count();
	 return true;
       }

      if (next_nodes != null) {
	 for (TrieNode tn : next_nodes) {
	    if (tn.matches(trc[idx])) {
	       boolean fg = tn.insertItem(trc,idx-1,base,lvl+1);
	       if (fg) ++tot_active;
	       return fg;
	     }
	  }
       }

      base = idx-1;

      TrieNode tn = createTrieNode(trc,idx,base,lvl+1);
      if (tn == null) count();
      else {
	 if (next_nodes == null) next_nodes = new ArrayList<TrieNode>();
	 next_nodes.add(tn);
	 ++tot_active;
       }

      return true;
    }

   private TrieNode createTrieNode(StackTraceElement [] trc,int idx,int base,int lvl) {
      if (idx < base || lvl >= max_level || idx < 0) return null;
      TrieNode tn = new TrieNode(trc[idx]);
      TrieNode cn = createTrieNode(trc,idx-1,base,lvl+1);
      if (cn == null) {
	 tn.count();
       }
      else {
	 if (tn.next_nodes == null) tn.next_nodes = new ArrayList<TrieNode>();
	 tn.next_nodes.add(cn);
	 tn.tot_active++;
       }
      return tn;
    }

   private boolean matches(StackTraceElement trc) {
      if (class_name == null) return false;
      if (use_lines && line_number != trc.getLineNumber()) return false;

      return class_name.equals(trc.getClassName()) && method_name.equals(trc.getMethodName());
    }

   void report(DyperXmlWriter xw,double cutoff,TrieNode par) {
      if (!checkOutput(cutoff,par)) return;
   
      if (class_name != null) {
         xw.begin("TRIENODE");
         xw.field("CLASS",class_name);
         xw.field("METHOD",method_name);
         if (is_system) xw.field("SYSTEM",true);
         if (use_lines) xw.field("LINE",line_number);
       }
   
      xw.field("ACTIVE",num_active);
      xw.field("TOTAL",tot_active);
   
      if (next_nodes != null) {
         for (TrieNode tn : next_nodes) {
            tn.report(xw,cutoff,this);
          }
       }
      if (class_name != null) xw.end();
    }

   boolean checkOutput(double cutoff,TrieNode par) {
      if (par == null) return true;
      if (tot_active < cutoff) return false;
      if (par.is_system && !is_system) return true;
      if (tot_active < par.tot_active * CHILD_MAX) return true;
      if (next_nodes != null) {
	 for (TrieNode tn : next_nodes) {
	    if (tn.checkOutput(cutoff,this)) return true;
	  }
       }
      return false;
    }

}	// end of subclass TrieNode




}	// end of class DyperAgentStates




/* end of DyperAgentStates.java */
