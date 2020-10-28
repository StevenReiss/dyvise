/********************************************************************************/
/*										*/
/*		DymonPatchTracker.java						*/
/*										*/
/*	Monitor that handles instrumentation					*/
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
 PROCESSWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymon/DymonPatchTracker.java,v 1.8 2010-03-30 16:22:05 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymonPatchTracker.java,v $
 * Revision 1.8  2010-03-30 16:22:05  spr
 * Clean up the code.
 *
 * Revision 1.7  2009-09-19 00:09:46  spr
 * Update dymon for seletive agent insertion and removal.
 *
 * Revision 1.6  2009-05-01 23:15:12  spr
 * Fix up state computation.  Clean up code.
 *
 * Revision 1.5  2009-04-11 23:46:18  spr
 * Use IvyFormat rather than local code.
 *
 * Revision 1.4  2009-03-20 02:06:51  spr
 * Update dymon: add time-based information, updated memory, etc.
 *
 * Revision 1.3  2008-12-04 01:11:00  spr
 * Update output and fix phaser summary.
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


import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintDefaultReply;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.util.*;


class DymonPatchTracker implements DymonConstants, MintConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private DymonProcess	for_process;
private Queue<DymonPatchRequest> request_queue;
private Timer request_timer;
private boolean is_enabled;

private Collection<DymonPatchRequest> active_requests;
private Map<DymonPatchRequest,RemoveRequest> remove_handlers;
private Collection<DymonPatchRequest> remove_pending;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DymonPatchTracker(DymonProcess dp)
{
   for_process = dp;
   request_queue = new PriorityQueue<DymonPatchRequest>();
   active_requests = new LinkedList<DymonPatchRequest>();
   remove_handlers = new HashMap<DymonPatchRequest,RemoveRequest>();
   remove_pending = new LinkedList<DymonPatchRequest>();
   request_timer = null;
   is_enabled = true;
}



/********************************************************************************/
/*										*/
/*	Enqueuing methods							*/
/*										*/
/********************************************************************************/

void addRequest(DymonPatchRequest pr)
{
   if (pr == null) return;

   System.err.println("ADD PATCH REQUEST " + pr.getModelName());

   synchronized (this) {
      if (active_requests.contains(pr)) return;
      if (request_queue.contains(pr)) return;

      if (!is_enabled) {
	 request_queue.offer(pr);
	 return;
       }
      if (checkConflicts(pr,active_requests)) {
	 request_queue.offer(pr);
	 for (DymonPatchRequest ar : active_requests) {
	    if (pr.getPriority() < ar.getPriority()) removeActive(ar);
	  }
	 return;
       }

      active_requests.add(pr);
    }

   if (instrument(pr,true)) {
      long ivl = pr.getDuration();
      if (ivl != 0) {
	 RemoveRequest rr = new RemoveRequest(pr);
	 remove_handlers.put(pr,rr);
	 synchronized (this) {
	    if (request_timer == null) request_timer = new Timer("DymonPatchTrackTimer");
	    request_timer.schedule(rr,ivl);
	  }
       }
    }
   else {
      removeRequest(pr,false);
    }
}



void removeRequest(DymonPatchRequest pr)
{
   removeRequest(pr,true);
}



private void removeRequest(DymonPatchRequest pr,boolean run)
{
   synchronized (this) {
      remove_handlers.remove(pr);
      if (remove_pending.contains(pr)) return;
      remove_pending.add(pr);
    }

   if (active_requests.contains(pr) && run) {
      instrument(pr,false);
    }

   synchronized (this) {
      remove_pending.remove(pr);
      active_requests.remove(pr);
    }

   handleQueuedTasks();
}




private void handleQueuedTasks()
{
   if (!is_enabled) return;

   Collection<DymonPatchRequest> starts = new LinkedList<DymonPatchRequest>();
   Collection<DymonPatchRequest> requeue = new LinkedList<DymonPatchRequest>();

   synchronized (this) {
      while (!request_queue.isEmpty()) {
	 DymonPatchRequest npr = request_queue.peek();
	 request_queue.remove(npr);
	 if (!checkConflicts(npr,active_requests) && !checkConflicts(npr,starts)) {
	    starts.add(npr);
	  }
	 else requeue.add(npr);
       }
      for (DymonPatchRequest npr : requeue) {
	 request_queue.add(npr);
       }
    }

   for (DymonPatchRequest sr : starts) {
      System.err.println("DELAYED ADD REQUEST FOR " + sr.getModelName());
      sr.raisePriority();
      addRequest(sr);
    }
}




private class RemoveRequest extends TimerTask {

   private DymonPatchRequest orig_request;

   RemoveRequest(DymonPatchRequest pr) {
      orig_request = pr;
    }

   public void run() {
      try {
	 removeRequest(orig_request,true);
       }
      catch (Throwable t) {
	 System.err.println("DYMON: Problem handling remove: " + t);
	 t.printStackTrace();
       }
    }

}	// end of subclass RemoveRequest



/********************************************************************************/
/*										*/
/*	Clean up methods							*/
/*										*/
/********************************************************************************/

void disable()
{
   synchronized (this) {
      is_enabled = false;
      request_queue.clear();
      for (DymonPatchRequest ar : active_requests) removeActive(ar);
      if (request_timer != null) request_timer.cancel();
      request_timer = null;
    }
}



void enable()
{
   synchronized (this) {
      is_enabled = true;
    }

   handleQueuedTasks();
}



void removeActive(DymonPatchRequest pr)
{
   System.err.println("DYMON: Removing active request " + pr.getModelName());

   synchronized (this) {
      RemoveRequest rr = remove_handlers.remove(pr);
      if (rr != null) rr.cancel();
      if (request_timer == null) request_timer = new Timer("DymonPatchTrackTimer");
      request_timer.schedule(new RemoveRequest(pr),0);
    }
}



/********************************************************************************/
/*										*/
/*	Instrumentation methods 						*/
/*										*/
/********************************************************************************/

private boolean instrument(DymonPatchRequest pr,boolean insert)
{
   if (pr == null) return false;
   if (!active_requests.contains(pr)) return false;

   pr.instrument(insert);

   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("PATCHMODEL");
   xw.field("NAME",pr.getModelName());
   xw.field("INSERT",insert);
   int ctr = 0;
   for (String s : pr.getClasses()) {
      xw.begin("PATCH");
      xw.field("CLASS",s);
      xw.field("PATCH",pr.getPatchFile(s));
      xw.field("CHANGE",pr.isPatchChanged(s));
      xw.end("PATCH");
      ++ctr;
    }
   pr.addPatchCommands(xw,insert);
   xw.end("PATCHMODEL");

   synchronized (this) {
      if (insert && !for_process.isMonitoringEnabled()) return false;
      if (ctr == 0 && !pr.allowEmptyPatch()) return false;
    }

   System.err.println("DYMON: INSTRUMENT " + pr.getModelName() + " " + insert + " " +
			 pr.getDuration() + " " + new Date());

   MintDefaultReply mr = new MintDefaultReply();
   for_process.sendDyperMessage("INSTRUMENT",xw.toString(),mr,MINT_MSG_FIRST_REPLY);
   Element e = mr.waitForXml();
   if (e == null) {
      System.err.println("DYMON: INSTRUMENT " + pr.getModelName() + " FAILED");
      return false;
    }

   Element e1 = IvyXml.getElementByTag(e,"PATCH");
   long when = IvyXml.getAttrLong(e1,"TIME");
   pr.handlePatchInsert(when,insert);

   return true;
}




/********************************************************************************/
/*										*/
/*	Methods for managing overlap						*/
/*										*/
/********************************************************************************/

private boolean checkConflicts(DymonPatchRequest r,Collection<DymonPatchRequest> active)
{
   if (active_requests.isEmpty()) return false;
   if (r.getPatchOverlap() == PatchOverlap.ANY) return false;

   for (DymonPatchRequest ar : active) {
      if (ar.getDuration() == 0 || ar.getPatchOverlap() == PatchOverlap.ANY) continue;
      if (r.excludeOverlap(ar.getRequestName())) return true;
      if (r.getPatchOverlap() == PatchOverlap.NONE) return true;
      if (ar.excludeOverlap(r.getRequestName())) return true;
      if (ar.getPatchOverlap() == PatchOverlap.NONE) return true;
      if (ar.getPatchOverlap() == PatchOverlap.CLASS ||
	     r.getPatchOverlap() == PatchOverlap.CLASS) {
	 for (String s1 : r.getClasses()) {
	    for (String s2 : ar.getClasses()) {
	       if (s1.equals(s2)) return true;
	     }
	  }
       }
    }

   return false;
}




}	// end of class DymonPatchTracker



/* end of DymonPatchTracker.java */

