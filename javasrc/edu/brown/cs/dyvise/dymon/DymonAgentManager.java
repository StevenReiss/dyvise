/********************************************************************************/
/*										*/
/*		DymonAgentManager.java						*/
/*										*/
/*	DYMON package to manage agent sets for processes			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymon/DymonAgentManager.java,v 1.4 2011-03-10 02:26:33 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymonAgentManager.java,v $
 * Revision 1.4  2011-03-10 02:26:33  spr
 * Code cleanup.
 *
 * Revision 1.3  2010-04-29 20:03:45  spr
 * Add some debugging for the mac.
 *
 * Revision 1.2  2009-10-07 01:00:13  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.1  2009-09-19 00:09:46  spr
 * Update dymon for seletive agent insertion and removal.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymon;


import edu.brown.cs.ivy.mint.MintConstants;

import java.util.*;


class DymonAgentManager implements DymonConstants, MintConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private DymonMain	dymon_main;
private DymonProcess	for_process;
private Collection<DymonAgent> active_agents;
private String		agent_set;
private Map<String,DymonAgent> agent_map;
private String		all_agents;

private Collection<DymonDetailing> active_detailings;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DymonAgentManager(DymonMain dm,DymonProcess dp)
{
   dymon_main = dm;
   for_process = dp;
   agent_set = null;
   active_agents = null;
   active_detailings = null;
   all_agents = null;

   setupInitialAgents();
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getAgentSet()
{
   setupAgentSet();
   return agent_set;
}



Collection<DymonAgent> getActiveAgents()
{
   if (active_agents == null) return new ArrayList<DymonAgent>();

   return new ArrayList<DymonAgent>(active_agents);
}


Collection<DymonDetailing> getActiveDetailings()
{
   return active_detailings;
}


DymonAgent getAgent(String nm)
{
   return agent_map.get(nm);
}


static Collection<String> getAllAgents()
{
   DymonAgentManager dm = new DymonAgentManager(null,null);

   return dm.listAllAgents();
}


Collection<String> listAllAgents()
{
   return new TreeSet<String>(agent_map.keySet());
}



/********************************************************************************/
/*										*/
/*	Set methods								*/
/*										*/
/********************************************************************************/

void setAgentSet(String agts)
{
   if (agts == null || agts.equals("*") || agts.equals("ALL")) {
      agent_set = all_agents;
      String a2 = dymon_main.getResource(for_process.getStartClass(),"ADDAGENTS");
      if (a2 != null) agent_set += "," + a2;
    }
   else if (agts.equals("DEFAULT")) {
      agent_set = null;
      setupAgentSet();
    }
   else if (agts.equals("NONE")) {
      agent_set = "";
    }
   else {
      agent_set = agts;
    }

   resetAgents();
}




/********************************************************************************/
/*										*/
/*	Initial installation methods						*/
/*										*/
/********************************************************************************/

void initialInstall()
{
   setupAgentSet();

   Set<String> active = new HashSet<String>();
   for (StringTokenizer tok = new StringTokenizer(agent_set,", "); tok.hasMoreTokens(); ) {
      active.add(tok.nextToken());
    }

   active_agents = new ArrayList<DymonAgent>();
   active_detailings = new HashSet<DymonDetailing>();

   for (DymonAgent da : agent_map.values()) {
      if (active.contains(da.getName())) {
	 addActiveAgent(da);
	 da.install();
       }
    }
}




private void resetAgents()
{
   if (active_agents == null) return;		// no install done yet

   Set<String> active = new HashSet<String>();
   for (StringTokenizer tok = new StringTokenizer(agent_set,", "); tok.hasMoreTokens(); ) {
      active.add(tok.nextToken());
    }

   // first remove active agents that are no longer wanted
   Set<String> del = null;

   for (Iterator<DymonAgent> it = active_agents.iterator(); it.hasNext(); ) {
      DymonAgent da = it.next();
      if (!active.contains(da.getName())) {
	 System.err.println("DYMON: Deactivate agent " + da.getName());
	 da.deactivate();
	 Collection<DymonDetailing> ddl = da.getDetailings();
	 if (ddl != null) active_detailings.removeAll(ddl);
	 it.remove();
	 String dyp = da.getDyperAgentName();
	 if (del == null) del = new HashSet<String>();
	 del.add(dyp);
       }
    }

   // then activate any new agents
   for (String s : active) {
      DymonAgent da = agent_map.get(s);
      if (da != null) {
	 System.err.println("DYMON: Activate agent " + da.getName());
	 switch (da.getState()) {
	    case IDLE :
	       addActiveAgent(da);
	       da.install();
	       break;
	    case ACTIVE :
	       break;
	    case PASSIVE :
	    case DEAD :
	       addActiveAgent(da);
	       da.noteActive();
	       String dyp = da.getDyperAgentName();
	       for_process.setDyperAgentStatus(dyp,true);
	       break;
	  }
       }
    }

   if (del != null) {
      for (DymonAgent da : active_agents) {
	 String dyp = da.getDyperAgentName();
	 del.remove(dyp);
       }
      for (String s : del) {
	 for_process.setDyperAgentStatus(s,false);
       }
    }
}



private void addActiveAgent(DymonAgent da)
{
   active_agents.add(da);
   Collection<DymonDetailing> ddl = da.getDetailings();
   if (ddl != null) active_detailings.addAll(ddl);
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

private void setupInitialAgents()
{
   all_agents = null;
   agent_map = new LinkedHashMap<String,DymonAgent>();

   addAgent(new DymonAgentCpu(dymon_main,for_process),true);
   addAgent(new DymonAgentThreads(dymon_main,for_process),true);
   addAgent(new DymonAgentTiming(dymon_main,for_process),true);
   addAgent(new DymonAgentMemory(dymon_main,for_process),true);
   addAgent(new DymonAgentIO(dymon_main,for_process),true);
   addAgent(new DymonAgentHeap(dymon_main,for_process),true);
   addAgent(new DymonAgentReaction(dymon_main,for_process,true),true);
   // addAgent(new DymonAgentSocket(dymon_main,for_process),true);
   addAgent(new DymonAgentPhaser(dymon_main,for_process),true);
   addAgent(new DymonAgentCollections(dymon_main,for_process),false);
   addAgent(new DymonAgentStates(dymon_main,for_process),false);
   addAgent(new DymonAgentReaction(dymon_main,for_process,false),false);
   addAgent(new DymonAgentEvents(dymon_main,for_process),false);

   if (all_agents == null) all_agents = "";
}



private void addAgent(DymonAgent da,boolean dflt)
{
   String nm = da.getName();
   agent_map.put(nm,da);

   if (dflt) {
      if (all_agents == null) all_agents = nm;
      else all_agents += "," + nm;
    }
}



private void setupAgentSet()
{
   if (agent_set != null) return;

   agent_set = dymon_main.getResource(for_process.getStartClass(),"AGENTS");
   if (agent_set == null || agent_set.equals("*") || agent_set.equals("ALL"))
      agent_set = all_agents;

   String a2 = dymon_main.getResource(for_process.getStartClass(),"ADDAGENTS");
   if (a2 != null) agent_set += "," + a2;
}




}	// end of class DymonAgentManager




/* end of DymonAgentManager.java */
