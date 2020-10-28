/********************************************************************************/
/*										*/
/*		DymonAgent.java 						*/
/*										*/
/*	DYPER monitor agent interface						*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymon/DymonAgent.java,v 1.5 2009-09-19 00:09:46 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymonAgent.java,v $
 * Revision 1.5  2009-09-19 00:09:46  spr
 * Update dymon for seletive agent insertion and removal.
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


import edu.brown.cs.dyvise.dyper.DyperConstants;
import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintDefaultReply;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.util.*;



public abstract class DymonAgent implements DyperConstants, DymonConstants, MintConstants {



/********************************************************************************/
/*										*/
/*	private storage 							*/
/*										*/
/********************************************************************************/

protected DymonMain dymon_main;
protected DymonProcess for_process;

private AgentState	current_state;

private long [] 	recent_count;
private long [] 	recent_total;
private int		recent_index;

private LinkedList<DeltaData> delta_info;

private static final int RECENT_SIZE = 40;

private static final int MAX_DELTAS = 1000;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected DymonAgent(DymonMain dm,DymonProcess dp)
{
   dymon_main = dm;
   for_process = dp;

   current_state = AgentState.IDLE;

   delta_info = new LinkedList<DeltaData>();

   recent_index = 0;
   recent_count = new long[RECENT_SIZE];
   recent_total = new long[RECENT_SIZE];
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public abstract String getName();

protected DymonMain getMonitor()		{ return dymon_main; }

AgentState getState()				{ return current_state; }




/********************************************************************************/
/*										*/
/*	Installation methods							*/
/*										*/
/********************************************************************************/

final void install()
{
   String s = getMonitorClass();
   if (s != null) {
      String msg = "<AGENT CLASS='" + s + "' />";
      MintDefaultReply mr = new MintDefaultReply();
      for_process.sendDyperMessage("AGENT",msg,mr,MINT_MSG_FIRST_REPLY);
      mr.waitFor();
    }

   current_state = AgentState.ACTIVE;

   handleStart(for_process);
}


final void deactivate()
{
   current_state = AgentState.PASSIVE;
}


final void activate()
{
   current_state = AgentState.ACTIVE;
}



protected String getMonitorClass()		{ return null; }


protected void handleStart(DymonProcess dp)	{ }


public void detach()				{ }

public void reattach()				{ }

public void noteDead()				{ current_state = AgentState.DEAD; }

public void noteActive()			{ }


public String getDyperAgentName()		{ return null; }



/********************************************************************************/
/*										*/
/*	Methods to handle data							*/
/*										*/
/********************************************************************************/

public void handleReport(Element xml)		{ }

public void handleDump(Element xml)		{ }



/********************************************************************************/
/*										*/
/*	Methods to change instrumentation					*/
/*										*/
/********************************************************************************/

protected abstract Collection<DymonDetailing> getDetailings();




/********************************************************************************/
/*										*/
/*	Analysis methods							*/
/*										*/
/********************************************************************************/

public void outputAnalysis(IvyXmlWriter xw)	{ }


public void doClear()				{ }


public double getSummaryValue() 		{ return -1; }
public double getConfidence()			{ return 1; }
public void outputSummary(IvyXmlWriter xw)	{ }

public void outputImmediate(IvyXmlWriter xw)	{ }




/********************************************************************************/
/*										*/
/*	Query methods								*/
/*										*/
/********************************************************************************/

Map<String,Number> handleSimpleQuery(String id)
{
   return null;
}




/********************************************************************************/
/*										*/
/*	Count methods								*/
/*										*/
/********************************************************************************/

protected void clearRecentCounts()
{
   synchronized (delta_info) {
      delta_info.clear();
    }

   synchronized (recent_count) {
      recent_index = 0;
      recent_count = new long[RECENT_SIZE];
      recent_total = new long[RECENT_SIZE];
    }
}



protected void updateRecentCounts(long ct,long tot)
{
   synchronized (recent_count) {
      recent_index = (recent_index + 1) % RECENT_SIZE;
      recent_count[recent_index] = ct;
      recent_total[recent_index] = tot;
    }
}



protected double getRecentRatio()
{
   synchronized (recent_count) {
      if (recent_count[recent_index] == 0) return 0;
      double pct = recent_count[recent_index];
      pct /= recent_total[recent_index];

      for (int i = RECENT_SIZE-1; i > 0; --i) {
	 int j = (recent_index + i) % RECENT_SIZE;
	 if (recent_total[j] != 0) {
	    double dct = recent_count[recent_index] - recent_count[j];
	    double tct = recent_total[recent_index] - recent_total[j];
	    pct = dct/tct;
	    break;
	  }
       }

      return pct;
    }
}



protected long getRecentTotal()
{
   synchronized (recent_count) {
      return recent_total[recent_index];
    }
}



protected long getRecentCount()
{
   synchronized (recent_count) {
      return recent_count[recent_index];
    }
}




/********************************************************************************/
/*										*/
/*	Delta information maintenance						*/
/*										*/
/********************************************************************************/

protected void addDelta(DeltaData dd)
{
   synchronized (delta_info) {
      delta_info.add(dd);
      if (delta_info.size() > MAX_DELTAS) delta_info.removeFirst();
    }
}



protected void processDeltas(IvyXmlWriter xw)
{
   LinkedList<?> dlst = null;
   synchronized (delta_info) {
      if (delta_info.size() > 1) {
	 dlst = (LinkedList<?>) delta_info.clone();
	 DeltaData dd = delta_info.getLast();
	 delta_info.clear();
	 delta_info.add(dd);
       }
    }

   if (dlst != null) {
      DeltaData prev = null;
      for (Iterator<?> it = dlst.iterator(); it.hasNext(); ) {
	 DeltaData nxt = (DeltaData) it.next();
	 if (prev != null) nxt.outputDelta(xw,prev);
	 prev = nxt;
       }
    }
}



/********************************************************************************/
/*										*/
/*	Subclasses for comparisons						*/
/*										*/
/********************************************************************************/

protected static class SourceCompare implements Comparator<Map.Entry<StackTraceElement,Long>> {

   public int compare(Map.Entry<StackTraceElement,Long> e1,
			 Map.Entry<StackTraceElement,Long> e2) {
      int v1 = e1.getValue().compareTo(e2.getValue());
      if (v1 != 0) return -v1;
      v1 = e1.getKey().getClassName().compareTo(e2.getKey().getClassName());
      if (v1 != 0) return v1;
      v1 = e1.getKey().getMethodName().compareTo(e2.getKey().getMethodName());
      if (v1 != 0) return v1;
      v1 = e1.getKey().getLineNumber() - e2.getKey().getLineNumber();
      if (v1 < 0) return -1;
      if (v1 > 0) return 1;
      return 0;
    }

}	// end of subclass SourceCompare



protected static class ThreadCompare implements Comparator<Map.Entry<String,long []>> {

   public int compare(Map.Entry<String,long []> e1,Map.Entry<String,long []> e2) {
      long v1 = e1.getValue()[0] - e2.getValue()[0];
      if (v1 < 0) return 1;
      if (v1 > 0) return -1;
      return e1.getKey().compareTo(e2.getKey());
    }

}	// end of subclass ThreadCompare



}	// end of abstract class DymonAgent




/* end of DymonAgent.java */
