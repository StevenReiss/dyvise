/********************************************************************************/
/*										*/
/*		DyviseTimeManager.java						*/
/*										*/
/*	Global time manager for various time windows and users			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyvise/DyviseTimeManager.java,v 1.1 2009-10-07 01:38:30 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyviseTimeManager.java,v $
 * Revision 1.1  2009-10-07 01:38:30  spr
 * Add time manager for common maintenacne of user time.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dyvise;


import edu.brown.cs.dyvise.dymon.DymonRemote;

import edu.brown.cs.ivy.mint.*;
import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.Element;
import java.util.*;


public class DyviseTimeManager implements DyviseConstants {




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private long			current_time;
private long			last_time;
private List<TimeListener>	time_listeners;
private List<TimeMark>		time_marks;
private MintControl		mint_control;
private String			process_id;


private static Map<String,DyviseTimeManager> time_map = new HashMap<String,DyviseTimeManager>();



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public synchronized static DyviseTimeManager getTimeManager(String pid)
{
   DyviseTimeManager tm = time_map.get(pid);
   if (tm == null) {
      tm = new DyviseTimeManager(pid);
      time_map.put(pid,tm);
    }

   return tm;
}



private DyviseTimeManager(String pid)
{
   time_listeners = new ArrayList<TimeListener>();
   time_marks = new ArrayList<TimeMark>();
   current_time = 0;
   last_time = 0;
   process_id = pid;
   mint_control = DymonRemote.getMintControl();
   mint_control.register("<DYMON PID='" + process_id + "' COMMAND='MARK'><_VAR_0/></DYMON>",
			    new MarkHandler());
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public synchronized void addTimeListener(TimeListener tl)
{
   time_listeners.add(tl);
}


public synchronized void removeTimeListener(TimeListener tl)
{
   time_listeners.remove(tl);
}



public void setCurrentTime(long when)
{
   current_time = when;

   List<TimeListener> ltl;
   synchronized (this) {
      ltl = new ArrayList<TimeListener>(time_listeners);
    }

   for (TimeListener tl : ltl) {
      tl.handleTimeSet(current_time);
    }
}



public long getCurrentTime()			{ return current_time; }



public long getLastTime()			{ return last_time; }
public void setLastTime(long t) 		{ last_time = t; }



/********************************************************************************/
/*										*/
/*	Mark methods								*/
/*										*/
/********************************************************************************/

public void createUserMark(long when,String what)
{
   // setupUserMark(when,what); 	// handling the message should set the mark

   String msg = "<DYMON PID='" + process_id + "' COMMAND='MARK'>" +
      "<MARK TYPE='" + what + "' WHEN='" + when + "' /></DYMON>";

   mint_control.send(msg);
}



private void setupUserMark(long when,String what)
{
   if (when == 0) {
      if (current_time != 0) when = current_time;
      else if (last_time != 0) when = last_time;
      else return;
    }

   if (what == null) return;

   List<TimeListener> ltl;
   synchronized (this) {
      ltl = new ArrayList<TimeListener>(time_listeners);
      time_marks.add(new TimeMark(when,what));
    }

   for (TimeListener tl : ltl) {
      tl.handleMark(when,what);
    }
}



public Map<Long,String> getTimeMarks()
{
   Map<Long,String> rslt = new TreeMap<Long,String>();

   synchronized (this) {
      for (TimeMark tm : time_marks) {
	 long w = tm.getWhen();
	 String s0 = rslt.get(w);
	 if (s0 == null) rslt.put(w,tm.getType());
	 else rslt.put(w,s0 + "," + tm.getType());
       }
    }

   return rslt;
}



private static class TimeMark {

   private long mark_when;
   private String mark_type;

   TimeMark(long when,String what) {
      mark_when = when;
      mark_type = what;
    }

   long getWhen()			{ return mark_when; }
   String getType()			{ return mark_type; }

}	// end of inner class TimeMark



/********************************************************************************/
/*										*/
/*	Messaging code								*/
/*										*/
/********************************************************************************/

private class MarkHandler implements MintHandler {

   public void receive(MintMessage msg,MintArguments args) {
      Element e = args.getXmlArgument(0);
      if (e != null) {
	 String typ = IvyXml.getAttrString(e,"TYPE");
	 long when = IvyXml.getAttrLong(e,"WHEN");
	 setupUserMark(when,typ);
       }
      msg.replyTo();
    }

}	// end of inner class MarkHandler




}	// end of class DyviseTimeManager




/* end of DyviseTimeManager.java */
