/********************************************************************************/
/*										*/
/*		DymonDetailing.java						*/
/*										*/
/*	DYPER detailing description						*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymon/DymonDetailing.java,v 1.2 2008-11-12 14:10:44 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymonDetailing.java,v $
 * Revision 1.2  2008-11-12 14:10:44  spr
 * Various efficiency and bug fixups.  Readiness for immediate output.
 *
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymon;





public abstract class DymonDetailing implements DymonConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private DymonProcess for_process;

private double	detail_priority;
private DymonPatchRequest cur_request;
private long next_check;

private long	patch_start;
private long	patch_time;
private long	active_start;
private long	active_ticks;
private long	total_start;
private long	total_ticks;

private long	num_detail;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected DymonDetailing(DymonProcess dp)
{
   for_process = dp;
   detail_priority = 0;
   cur_request = null;
   next_check = 0;

   num_detail = 0;
   patch_start = 0;
   patch_time = 0;
   active_start = 0;
   active_ticks = 0;
}



/********************************************************************************/
/*										*/
/*	General Access methods							*/
/*										*/
/********************************************************************************/

public abstract String getDetailName();




/********************************************************************************/
/*										*/
/*	Priority management methods						*/
/*										*/
/********************************************************************************/

final double getPriority()		{ return detail_priority; }

final double computePriority()
{
   detail_priority = getLocalPriority();
   if (detail_priority < 0 || detail_priority > 1) {
      System.err.println("DYMON: Illegal priority for " + getDetailName() + " = " + detail_priority);
      detail_priority = 0.5;
    }

   return detail_priority;
}


protected abstract double getLocalPriority();




/********************************************************************************/
/*										*/
/*	Methods to get overhead and interval information for scheduling\	*/
/*										*/
/********************************************************************************/

public long getDetailInterval() 	{ return 0; }

public long getDetailOverhead() 	{ return 0; }

public double getDetailSlowdown()	{ return 0; }




/********************************************************************************/
/*										*/
/*	Methods to handle actual detailing					*/
/*										*/
/********************************************************************************/

final boolean isDetailing()
{
   if (cur_request != null && cur_request.isDone()) {
      cur_request = null;
    }

   return (cur_request != null);
}



final DymonPatchRequest setDetailing(long now,long delay)
{
   if (next_check == 0 || next_check > now + delay) next_check = now + delay;

   if (now < next_check) return null;

   cur_request = getPatchRequest(PATCH_PRIORITY_NORMAL);
   if (cur_request != null) cur_request.setDetailing(this);
   next_check = 0;

   return cur_request;
}



/********************************************************************************/
/*										*/
/*	Methods to handle continuous detailing					*/
/*										*/
/********************************************************************************/

public double getContinuousPriority()		{ return 0; }
public double getContinuousOverhead()		{ return -1; }
public void startContinuousTracing()		{ }
public void endContinuousTracing()		{ }




/********************************************************************************/
/*										*/
/*	Create a patch/detailing request					*/
/*										*/
/********************************************************************************/

protected abstract DymonPatchRequest getPatchRequest(int priority);



/********************************************************************************/
/*										*/
/*	Timing methods								*/
/*										*/
/********************************************************************************/

double getActiveTime()
{
   double tm = patch_time;
   if (patch_start != 0) tm += for_process.getReportTime() - patch_start;
   return tm;
}


double getActiveRunningTime()
{
   double tm = getActiveTime() * getActiveFraction();

   return tm;
}



double getActiveFraction()
{
   double as = active_ticks;
   if (active_start != 0) as += for_process.getActiveSamples() - active_start;

   double tt = total_ticks;
   if (total_start != 0) tt += for_process.getTotalSamples() - total_start;

   if (tt == 0) return 1.0;

   return as/tt;
}



long getNumDetailing()
{
   return num_detail;
}



void doClear()
{
   patch_start = 0;
   patch_time = 0;
   num_detail = 0;
}



void handlePatchTiming(long when,boolean start)
{
   if (start) ++num_detail;

   if (start) patch_start = when;
   else if (patch_start > 0) {
      patch_time += when - patch_start;
      patch_start = 0;
    }

   if (start) active_start = for_process.getActiveSamples();
   else if (active_start > 0) {
      active_ticks += for_process.getActiveSamples() - active_start;
      active_start = 0;
    }

   if (start) total_start = for_process.getTotalSamples();
   else if (total_start > 0) {
      total_ticks += for_process.getTotalSamples() - total_start;
      total_start = 0;
    }
}




}	// end of abstract class DymonDetailing




/* end of DymonDetailing.java */
