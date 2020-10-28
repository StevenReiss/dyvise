/********************************************************************************/
/*										*/
/*		DylockPatternBuilder.java					*/
/*										*/
/*	Class to build patterns from events					*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2009, Brown University, Providence, RI.				 *
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylock/DylockPatternBuilder.java,v 1.3 2016/11/02 18:59:10 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylockPatternBuilder.java,v $
 * Revision 1.3  2016/11/02 18:59:10  spr
 * Move to asm5
 *
 * Revision 1.2  2013/09/04 18:36:28  spr
 * Minor bug fixes.
 *
 * Revision 1.1  2013-05-09 12:29:00  spr
 * Dylock update.	Last 1.6 update
 *
 *
 ********************************************************************************/




package edu.brown.cs.dyvise.dylock;

import edu.brown.cs.ivy.xml.*;

import java.util.*;


class DylockPatternBuilder implements DylockConstants, DylockConstants.DylockEventSetBuilder
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<Integer,PatternData> pattern_map;
private PatternSorter	pattern_sorter;
private Map<String,DylockPattern> all_patterns;
private DylockLockDataManager location_manager;

private static int	MAX_PATTERN_SIZE = 256;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DylockPatternBuilder(DylockLockDataManager mgr)
{
   pattern_map = new HashMap<Integer,PatternData>();
   pattern_sorter = new PatternSorter();
   all_patterns = new HashMap<String,DylockPattern>();
   location_manager = mgr;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public DylockLockDataManager getManager()	{ return location_manager; }


@Override public boolean missingWaitForWaited(DylockLockEntry ent)
{
   PatternData pd = pattern_map.get(ent.getThreadId());
   if (pd == null) return true;
   for (PatternEvent pe : pd.getEvents()) {
      if (pe.getLock() == ent.getLock()) return false;	// anything must be a wait
    }
   return true;
}


@Override public double getLockTime(DylockLockData ld,double t0)
{
   for (PatternData pd : pattern_map.values()) {
      for (PatternEvent evt : pd.getEvents()) {
	 if (evt.getLock() == ld) {
	    t0 = Math.min(t0,evt.getTime());
	  }
       }
    }
   return t0;
}





/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

@Override public void addEvent(PatternEvent evt)
{
   PatternData pd = findPatternData(evt);
   // System.err.println("ADD: " + pd + " " + evt);
   pd.addEvent(evt);
   if (pd.isStable()) {
      Set<PatternData> thrds = new HashSet<PatternData>();
      List<PatternEvent> evts = null;
      boolean useall = false;

      evts = extractSimpleLocks(pd,thrds);
      if (evts == null) {
	 useall = true;
	 thrds.clear();
	 evts = extractAllLocks(pd,thrds);
       }
      if (evts.size() < 2) return;

      if (!isStableSequence(evts)) {
         if (evts.size() > MAX_PATTERN_SIZE) {
            for (PatternData xpd : thrds) {
               xpd.clearToStable();
             }
          }
         return;
       }
      // if not stable and size > MAX_PATTERN_SIZE) then
      // we should clear each thread in thrds by finding
      // the last locally stable entry and removing it and
      // everything before it.

      Collections.sort(evts,pattern_sorter);

      if (!isStableSequenceSorted(evts)) {
	 return;
       }

      clearEvents(thrds,evts,useall);

      if (evts.size() <= MAX_PATTERN_SIZE) {
	 DylockPattern pat = new DylockPattern(evts);
	 String txt = pat.getKey();
	 DylockPattern opat = all_patterns.get(txt);
	 if (opat != null) {
	    opat.addInstance(pat);
	    // System.err.println("DUPLICATE PATTERN " + txt);
	  }
	 else {
	    all_patterns.put(txt,pat);
	    // System.err.println("OUTPUT PATTERN " + txt + " " + evts);
	  }
       }

      return;
    }
}



private List<PatternEvent> extractSimpleLocks(PatternData pd,Set<PatternData> thrds)
{
   Set<DylockLockData> locks = pd.getCurrentLocks();
   thrds.clear();

   boolean chng = true;
   while (chng) {
      chng = false;
      for (PatternData xpd : pattern_map.values()) {
	 if (thrds.contains(xpd)) continue;
	 if (xpd.usesCurrentLock(locks)) {
	    // System.err.println("ADD THREAD " + xpd.isStable() + " " + xpd);
	    if (locks.addAll(xpd.getCurrentLocks())) {
	       if (xpd == pd || !xpd.isStable()) {
		  return null;
		}
	       chng = true;
	     }
	    thrds.add(xpd);
	  }
       }
    }

   List<PatternEvent> evts = new ArrayList<PatternEvent>();
   for (PatternData xpd : thrds) {
      evts.addAll(xpd.getCurrentEvents());
    }

   return evts;
}



private List<PatternEvent> extractAllLocks(PatternData pd,Set<PatternData> thrds)
{
   Set<DylockLockData> locks = new HashSet<DylockLockData>(pd.getLocks());
   thrds.clear();

   thrds.add(pd);
   boolean chng = true;
   while (chng) {
      chng = false;
      for (PatternData xpd : pattern_map.values()) {
	 if (thrds.contains(xpd)) continue;
	 if (xpd.usesLock(locks)) {
	    // System.err.println("ADD THREAD " + xpd.isStable() + " " + xpd);
	    if (locks.addAll(xpd.getLocks())) chng = true;
	    thrds.add(xpd);
	  }
       }
    }
   List<PatternEvent> evts = new ArrayList<PatternEvent>();
   for (PatternData xpd : thrds) {
      evts.addAll(xpd.getEvents());
    }

   return evts;
}


private void clearEvents(Set<PatternData> thrds,List<PatternEvent> evts,boolean useall)
{
   // we can assume that evts is in chronological order
   // and that the events from a given thread are the
   // final events on that thread.
   
   if (useall) {
      for (PatternData xpd : thrds) {
         xpd.clear();
       }
    }
   else {
       for (PatternData xpd : thrds) {
          xpd.clear(evts);
        }
    }
}



private boolean isStableSequence(List<PatternEvent> evts)
{
   int sz = evts.size();
   if (sz == 0) return false;

   double t0 = evts.get(sz-1).getTime();
   Map<DylockLockData,PatternEvent> stable = new HashMap<DylockLockData,PatternEvent>();

   for (int i = sz-1; i >= 0; --i) {
      PatternEvent evt = evts.get(i);
      double t1 = evt.getTime();
      if (t1 > t0) t0 = t1;
      // if (t0 - t1 > 20*1000000000.0) break;

      DylockLockData ld = evt.getLock();
      PatternEvent e1 = stable.get(ld);
      if (e1 == null || e1.getTime() < t1) {
	 stable.put(ld,evt);
       }
    }
   for (PatternEvent pe : stable.values()) {
      if (pe.getLevel() != 0) return false;
    }

   return true;
}



private boolean isStableSequenceSorted(List<PatternEvent> evts)
{
   Set<DylockLockData> stable = new HashSet<DylockLockData>();
   int sz = evts.size();
   if (sz == 0) return false;
   for (int i = sz-1; i >= 0; --i) {
      PatternEvent evt = evts.get(i);
      DylockLockData ld = evt.getLock();
      if (stable.contains(ld)) continue;
      if (evt.getLevel() != 0) return false;
      stable.add(ld);
    }

   return true;
}


private PatternData findPatternData(PatternEvent evt)
{
   Integer tid = evt.getThreadId();
   PatternData pd = pattern_map.get(tid);
   if (pd == null) {
      pd = new PatternData();
      pattern_map.put(tid,pd);
    }
   return pd;
}




/********************************************************************************/
/*										*/
/*	Class to hold current pattern data					*/
/*										*/
/********************************************************************************/

private class PatternData {

   private List<PatternEvent> pattern_events;
   private Set<DylockLockData> active_locks;

   PatternData() {
      pattern_events = new ArrayList<PatternEvent>();
      active_locks = new HashSet<DylockLockData>();
    }

   void addEvent(PatternEvent evt) {
      pattern_events.add(evt);
      active_locks.add(evt.getLock());
    }

   void clear() {
      pattern_events.clear();
      active_locks.clear();
    }

   void clear(List<PatternEvent> evts) {
      int esz = evts.size();
      int psz = pattern_events.size();
      if (psz == 0) return;
      if (esz == psz) {
         clear();
         return;
       }
      
      Set<DylockLockData> find = new HashSet<DylockLockData>();
      ListIterator<PatternEvent> liev = evts.listIterator(esz);
      ListIterator<PatternEvent> lipe = pattern_events.listIterator(psz);
      if (!lipe.hasPrevious()) return;
      PatternEvent cpe = lipe.previous();
      while (liev.hasPrevious()) {
         PatternEvent xpe = liev.previous();
         if (xpe == cpe) {
            find.add(xpe.getLock());
            lipe.remove();
            if (!lipe.hasPrevious()) break;
            cpe = lipe.previous();
          }
       }
      if (find.isEmpty()) return;
      for (PatternEvent pe : pattern_events) {
         DylockLockData ld = pe.getLock();
         if (find.remove(ld)) {
            if (find.isEmpty()) return;
          }
       }
      active_locks.removeAll(find);
   }
   
   Set<DylockLockData> getLocks()	{ return active_locks; }
   List<PatternEvent> getEvents()	{ return pattern_events; }


   boolean usesLock(Set<DylockLockData> lks) {
      if (active_locks.isEmpty()) return false;
      for (DylockLockData lk : lks) {
	 if (active_locks.contains(lk)) return true;
       }
      return false;
    }

   boolean isStable() {
      int sz = pattern_events.size();
      if (sz == 0) return false;
      PatternEvent evt = pattern_events.get(sz-1);
      if (evt.getLevel() != 0) return false;
   
      return true;
    }

   Set<DylockLockData> getCurrentLocks() {
      Set<DylockLockData> rslt = new HashSet<DylockLockData>();
      int sz = pattern_events.size();
      for (int i = sz-1; i >= 0; --i) {
         PatternEvent pe = pattern_events.get(i);
         if (pe.getLevel() == 0 && i < sz-1) break;
         rslt.add(pe.getLock());
       }
      return rslt;
    }


   

   List<PatternEvent> getCurrentEvents() {
      List<PatternEvent> rslt = new ArrayList<PatternEvent>();
      int sz = pattern_events.size();
      for (int i = sz-1; i >= 0; --i) {
         PatternEvent pe = pattern_events.get(i);
         if (pe.getLevel() == 0 && i < sz-1) break;
         rslt.add(pe);
       }
      return rslt;
    }

   boolean usesCurrentLock(Set<DylockLockData> lks) {
      if (active_locks.isEmpty()) return false;
      Set<DylockLockData> clks = getCurrentLocks();
      for (DylockLockData lk : lks) {
	 if (clks.contains(lk)) return true;
       }
      return false;
    }

   void clearToStable() {
      int sz = pattern_events.size();
      ListIterator<PatternEvent> li = pattern_events.listIterator(sz);
      Set<DylockLockData> newlcks = new HashSet<DylockLockData>();
      boolean remove = false;
      while (li.hasPrevious()) {
         PatternEvent pe = li.previous();
         if (!remove && pe.getLevel() == 0) remove = true;
         if (remove) li.remove();
         else newlcks.add(pe.getLock());
       }
      active_locks = newlcks;
    }
      
}	// end of inner class PatternData



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputPatterns(IvyXmlWriter xw)
{
   for (DylockPattern dp : all_patterns.values()) {
      dp.output(xw);
    }
}



/********************************************************************************/
/*										*/
/*	Time-based event sorter 						*/
/*										*/
/********************************************************************************/

private static class PatternSorter implements Comparator<PatternEvent> {

   @Override public int compare(PatternEvent e1,PatternEvent e2) {
      double d1 = e1.getTime() - e2.getTime();
      if (d1 < 0) return -1;
      if (d1 > 0) return 1;
      int d2 = e1.getThreadId() - e2.getThreadId();
      if (d2 < 0) return -1;
      if (d2 > 0) return 1;
      int d4 = e1.getType().ordinal() - e2.getType().ordinal();
      return d4;
    }

}	// end of inner class PatternSorter





}	// end of class DylockPatternBuilder




/* end of DylockPatternBuilder.java */
