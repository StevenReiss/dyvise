/********************************************************************************/
/*										*/
/*		DylockLockRunData.java						*/
/*										*/
/*	Representation of data for visualization				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylock/DylockLockRunData.java,v 1.4 2016/11/02 18:59:10 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylockLockRunData.java,v $
 * Revision 1.4  2016/11/02 18:59:10  spr
 * Move to asm5
 *
 * Revision 1.3  2013/09/04 18:36:28  spr
 * Minor bug fixes.
 *
 * Revision 1.2  2013-05-09 12:29:00  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.1  2012-10-05 00:52:41  spr
 * Update lock analysis.  Prepare for pattern display.
 *
 *
 ********************************************************************************/


package edu.brown.cs.dyvise.dylock;


import edu.brown.cs.dyvise.dystore.DystoreStore;

import java.util.*;


class DylockLockRunData implements DylockConstants
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private String lock_id;
private double last_time;
private int event_counter;
private Set<LockRunEntry> wait_set;
private Map<String,LockRunEntry> current_waits;
private Map<String,LockRunEntry> thread_locks;
private Queue<LockRunEntry> active_set;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DylockLockRunData(String lid,double when)
{
   lock_id = lid;
   last_time = when;
   event_counter = 1;
   wait_set = new TreeSet<LockRunEntry>();
   active_set = new LinkedList<LockRunEntry>();
   current_waits = new HashMap<String,LockRunEntry>();
   thread_locks = new HashMap<String,LockRunEntry>();
}



/********************************************************************************/
/*										*/
/*	Lock management 							*/
/*										*/
/********************************************************************************/

void clear()
{
   wait_set.clear();
   active_set.clear();
   current_waits.clear();
   thread_locks.clear();
}



void doneP(double now,String th,DystoreStore ds)
{
   LockRunEntry ent = current_waits.remove(th);
   if (ent != null) ent.setActive(true);
   if (ent == null) {
      if (active_set.size() > 0) {
	 ent = active_set.peek();
	 ent.setEndTime(now);
	 outputTuples(now,ds);
	 active_set.remove(ent);
       }
    }
   else {
      if (wait_set.contains(ent)) {
	 ent.setEndTime(now);
	 outputTuples(now,ds);
	 wait_set.remove(ent);
       }
    }
}



void doneV(double now,String th,DystoreStore ds)
{
   if (wait_set.size() == 0) {
      LockRunEntry ent = new LockRunEntry(th,event_counter++,VIEW_TYPE_MUTEX,"V",now);
      active_set.add(ent);
    }
}



void acquireLock(double now,String th,DystoreStore ds,int type)
{
   // first handle the case where this lock was previously waiting
   LockRunEntry ent = current_waits.remove(th);
   if (ent != null) {
      ent.setActive(true);
      ent.setEndTime(now);
      outputTuples(now,ds);
      wait_set.remove(ent);
    }

   // ent = thread_locks.get(th);
   // if (ent != null) {
      // ent.noteLock(type);
      // return;
    // }

   ent = new LockRunEntry(th,event_counter++,type,"LOCK",now);
   thread_locks.put(th,ent);

   active_set.add(ent);
}




void releaseLock(double now,String th,DystoreStore ds,int type)
{
   LockRunEntry ent = thread_locks.get(th);
   if (ent == null) return;
   // if (!ent.noteUnlock(type)) return;
   ent.setEndTime(now);
   outputTuples(now,ds);
   active_set.remove(ent);
   LockRunEntry fnd = null;
   for (LockRunEntry lre : active_set) {
      if (lre.getThread().equals(th)) fnd = lre;
    }
   if (fnd == null) thread_locks.remove(th);
   else thread_locks.put(th,fnd);
}



void addWaiting(double now,String th,DystoreStore ds,int type)
{
   LockRunEntry e1 = thread_locks.get(th);
   if (e1 != null) return;

   LockRunEntry ent = current_waits.get(th);
   if (ent == null) {
      outputTuples(now,ds);
      ent = new LockRunEntry(th,event_counter++,type,"WAIT",now);
      current_waits.put(th,ent);
      wait_set.add(ent);
    }
   ent.setActive(true);
}

void doneWaiting(double now,String th,DystoreStore ds)
{
   LockRunEntry e1 = thread_locks.get(th);
   if (e1 != null) return;

   LockRunEntry ent = current_waits.get(th);
   if (ent != null) ent.setActive(false);
}



private void outputTuples(double to,DystoreStore ds)
{
   if (last_time == 0) last_time = to;
   if (last_time == to) return;
   int lvl = 0;

   for (LockRunEntry ent : wait_set) {
      if (ent.isActive()) {
	 ++lvl;
	 double end = ent.getEndTime();
	 if (end == 0 && to - ent.getStartTime() > VIEW_FORCE_TIME) {
	    end = to;
	  }
	 if (end > 0) {
	    Map<String,Object> tup = new HashMap<String,Object>();
	    tup.put("START",ent.getStartTime()/TIME_SHIFT);
	    tup.put("END",end/TIME_SHIFT);
	    tup.put("LEVEL",lvl);
	    tup.put("THREAD",ent.getThread());
	    tup.put("LOCK",lock_id);
	    tup.put("TYPE",ent.getType());
	    tup.put("TYPENAME",VIEW_TYPE_NAMES[ent.getType()]);
	    tup.put("EVENT",ent.getEntryType());
	    ds.addTuple(tup);
	    // System.err.println("ADD TUPLE " + tup);
	    ent.setStartTime(to);
	  }
       }
    }

   lvl = -1;
   for (LockRunEntry ent : active_set) {
      double end = ent.getEndTime();
      if (end == 0 && to - ent.getStartTime() > VIEW_FORCE_TIME) {
	 end = to;
       }
      if (end > 0) {
	 Map<String,Object> tup = new HashMap<String,Object>();
	 tup.put("START",ent.getStartTime()/TIME_SHIFT);
	 tup.put("END",end/TIME_SHIFT);
	 tup.put("LEVEL",lvl);
	 tup.put("THREAD",ent.getThread());
	 tup.put("LOCK",lock_id);
	 tup.put("TYPE",ent.getType());
	 tup.put("TYPENAME",VIEW_TYPE_NAMES[ent.getType()]);
	 tup.put("EVENT",ent.getEntryType());
	 ds.addTuple(tup);
	 ent.setStartTime(to);
       }
      --lvl;
    }
   last_time = to;
}




/********************************************************************************/
/*										*/
/*	Lock Entry holder							*/
/*										*/
/********************************************************************************/

private static class LockRunEntry implements Comparable<LockRunEntry> {

   private String thread_name;
   private int start_id;
   private int lock_type;
   private boolean is_active;
   private int [] lock_level;
   private double start_time;
   private double end_time;
   private String entry_type;

   LockRunEntry(String th,int id,int type,String etyp,double now) {
      thread_name = th;
      start_id = id;
      is_active = true;
      lock_type = type;
      lock_level = new int[NUM_VIEW_TYPES];
      lock_level[type]++;
      entry_type = etyp;
      start_time = now;
      end_time = 0;
    }

   void setActive(boolean fg)		{ is_active = fg; }
   boolean isActive()			{ return is_active; }
   String getThread()			{ return thread_name; }
   int getType()			{ return lock_type; }

   void setEndTime(double t)		{ end_time = t; }
   double getEndTime()			{ return end_time; }
   void setStartTime(double t)		{ start_time = t; }
   double getStartTime()		{ return start_time; }
   String getEntryType()		{ return entry_type; }

   

   

   @Override public int compareTo(LockRunEntry e) {
      return start_id - e.start_id;
    }

}	// end of inner class LockRunEntry



}	// end of class DylockLockRunData




/* end of DylockLockRunData.java */
