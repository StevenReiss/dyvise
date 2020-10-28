/********************************************************************************/
/*										*/
/*	`       DylockLockEntry.java                                            */
/*										*/
/*	DYVISE lock analysis lock entry information holder			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylock/DylockLockEntry.java,v 1.4 2016/11/02 18:59:09 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylockLockEntry.java,v $
 * Revision 1.4  2016/11/02 18:59:09  spr
 * Move to asm5
 *
 * Revision 1.3  2013-05-09 12:29:00  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.2  2012-10-05 00:52:41  spr
 * Update lock analysis.  Prepare for pattern display.
 *
 * Revision 1.1  2011-03-10 02:25:00  spr
 * New version of dylock to produce visualizations.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dylock;

import java.io.*;


class DylockLockEntry implements DylockConstants, DylockConstants.TraceLockEntry,
	DylockConstants.RunningEntry
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private DylockLockDataManager lock_main;
private TraceEntryType entry_type;
private int thread_id;
private DylockLockLocation entry_location;
private double entry_time;
private double exit_time;
private double update_time;
private int lock_id;
private int nest_depth;
private int thread_depth;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DylockLockEntry(DylockLockDataManager dm,String line) throws IOException
{
   lock_main = dm;
   String [] args = DylockMain.splitLine(line);
   try {
      entry_type = TraceEntryType.valueOf(args[0]);
      thread_id = Integer.parseInt(args[1]);

      String ent = args[2];
      int e1 = 0;
      int idx = ent.indexOf("-");
      if (idx >= 0) {
	 e1 = Integer.parseInt(ent.substring(idx+1));
	 ent = ent.substring(0,idx);
       }
      int e0 = Integer.parseInt(ent);
      entry_location = lock_main.findLocation(e0);
      if (entry_location == null)
	 throw new IOException("Trace location " + args[2] + " not found");
      if (e1 > 0) {
	 DylockLockLocation ll1 = lock_main.findLocation(e1);
	 if (ll1 != null) entry_location.addAlias(ll1);
       }


      entry_time = Double.parseDouble(args[3]);
      update_time = entry_time;
      exit_time = 0;
      lock_id = Integer.parseInt(args[4]);
      nest_depth = Integer.parseInt(args[5]);
      thread_depth = Integer.parseInt(args[6]);
    }
   catch (Exception e) {
      e.printStackTrace();
      throw new IOException("Bad trace entry: " + line,e);
    }
}



DylockLockEntry(DylockLockEntry ent,TraceEntryType typ,double time)
{
   lock_main = ent.lock_main;
   entry_type = typ;
   thread_id = ent.thread_id;
   entry_location = ent.entry_location;
   entry_time = time;
   exit_time = 0;
   if (ent.exit_time != 0) {
      exit_time = ent.exit_time - ent.entry_time + time;
    }
   nest_depth = ent.nest_depth;
   thread_depth = ent.thread_depth;
   update_time = time;
   lock_id = ent.lock_id;
   if (typ == TraceEntryType.UNLOCK) ent.exit_time = time;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void setDoesWait()
{
   if (entry_location != null) entry_location.setDoesWait();
}

void setDoesNotify()
{
   if (entry_location != null) entry_location.setDoesNotify();
}



void setUpdateTime(double w)				{ update_time = Math.max(update_time,w); }
void setExitTime(double w)				{ exit_time = Math.max(exit_time,w); }

void setLocation(DylockLockLocation ll)
{
   if (ll == null) return;
   entry_location = ll;
}

@Override public int getLockId()			{ return lock_id; }
@Override public DylockLockData getLock()
{
   DylockLockData ld = lock_main.findLock(lock_id);
   if (ld == null) {
      try {
	 Thread.sleep(500);
       }
      catch (InterruptedException e) { }
      ld = lock_main.findLock(lock_id);
    }
   if (ld == null) {
      System.err.println("DYLOCK: Unknown lock id: " + lock_id);
      Thread.dumpStack();
    }

   return ld;
}

@Override public TraceLockLocation getLocation()	{ return entry_location; }
@Override public int getLocationId()
{
   if (entry_location == null) return 0;
   return entry_location.getId();
}

@Override public double getTime()			{ return entry_time; }

@Override public int getThreadId()			{ return thread_id; }
@Override public String getThreadName()
{
   DylockThreadData td = lock_main.findThread(thread_id);
   return td.getName();
}
@Override public DylockThreadData getThread()
{
   return lock_main.findThread(thread_id);
}

@Override public TraceEntryType getEntryType()		{ return entry_type; }
@Override public int getNestedDepth()			{ return nest_depth; }
@Override public int getThreadDepth()			{ return thread_depth; }

double getExitTime()					{ return exit_time; }
double getUpdateTime()					{ return update_time; }




/********************************************************************************/
/*										*/
/*	Debugging methods							*/
/*										*/
/********************************************************************************/


@Override public String toString()
{
   StringBuffer buf = new StringBuffer();
   buf.append(lock_id);
   buf.append(" ");
   buf.append(entry_type);
   buf.append(" Th=");
   buf.append(thread_id);
   buf.append(" ");
   buf.append(update_time);
   buf.append(" Lo=");
   buf.append(entry_location.getId());
   return buf.toString();
}



}	// end of class DylockLockEntry



/* end of DylockLockEntry.java */
