/********************************************************************************/
/*										*/
/*		DyperAgent.java 						*/
/*										*/
/*	Generic agent for use in run time monitoring				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyper/DyperAgent.java,v 1.2 2016/11/02 18:59:17 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyperAgent.java,v $
 * Revision 1.2  2016/11/02 18:59:17  spr
 * Move to asm5
 *
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dyper;

import java.lang.management.ThreadInfo;



abstract public class DyperAgent implements DyperConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

protected DyperControl	the_control;
private long		install_time;
private String		agent_name;
private long		mon_total;
private long		mon_start;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected DyperAgent(DyperControl dc,String name)
{
   the_control = dc;
   install_time = System.currentTimeMillis();
   agent_name = name;
   mon_total = 0;
   mon_start = 0;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public String getName() 		{ return agent_name; }




/********************************************************************************/
/*										*/
/*	Parameter methods							*/
/*										*/
/********************************************************************************/

public boolean setParameter(String s,String v)
{
   return false;
}


public String getParameter(String nm)
{
   return null;
}


public void setDetailing(String nm,boolean fg)
{
   return;
}



/********************************************************************************/
/*										*/
/*	Monitoring and timing methods						*/
/*										*/
/********************************************************************************/

void enableMonitoring(boolean fg,long now)
{
   if (now == 0) now = install_time;

   if (fg) {
      mon_start = now;
      handleMonitorStart(now);
    }
   else {
      if (mon_start != 0 && now > mon_start) mon_total += now-mon_start;
      mon_start = 0;
      handleMonitorStop(now);
    }
}



void clear(long now)
{
   mon_total = 0;
   if (mon_start > 0) mon_start = now;

   handleClear(now);
}




long getMonitoredTime(long now)
{
   long r = mon_total;
   if (mon_start > 0) r += now-mon_start;

   return r;
}



/********************************************************************************/
/*										*/
/*	Reporting methods							*/
/*										*/
/********************************************************************************/

public void generateReport(ReportType rt,DyperXmlWriter xw,long now)	{ }




/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

public void handleThreadStack(long now,ThreadInfo ti,StackTraceElement [] trc)	 { }

public void handleContentionMonitoring(boolean start,long now)		{ }

public void handleCpuTimeMonitoring(boolean start,long now)		{ }

public void handleMonitorStart(long now)				{ }

public void handleMonitorStop(long now) 				{ }

public void handleClear(long now)					{ }




}	// end of abstract class DyperAgent




/* end of DyperAgent.java */
