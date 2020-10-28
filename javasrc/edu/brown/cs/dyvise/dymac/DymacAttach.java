/********************************************************************************/
/*										*/
/*		DymacAttach.java						*/
/*										*/
/*	DYVISE dyanmic analysis interface to get active process 		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymac/DymacAttach.java,v 1.3 2009-10-07 22:39:40 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymacAttach.java,v $
 * Revision 1.3  2009-10-07 22:39:40  spr
 * Clean up typo.
 *
 * Revision 1.2  2009-10-07 00:59:45  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.1  2009-09-19 00:08:59  spr
 * Module to collect dynamic information from dymon about an applcation and store in database.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymac;


import edu.brown.cs.dyvise.dyvise.DyviseConstants;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;


class DymacAttach implements DymacConstants, DyviseConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private DymacMain	dymac_main;
private String		last_id;
private List<Process>	process_list;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DymacAttach(DymacMain dm)
{
   dymac_main = dm;
   last_id = null;
   process_list = null;
}



/********************************************************************************/
/*										*/
/*	Methods to find an appropriate process					*/
/*										*/
/********************************************************************************/

String findProcess(String start)
{
   List<Process> candidates = new ArrayList<Process>();

   for (int i = 0; ; ++i) {
      getProcesses();

      for (Process p : process_list) {
	 if (start.equals(p.getStartClass()) && !p.isAttached()) {
	    candidates.add(p);
	  }
       }

      if (!candidates.isEmpty()) break;

      if (i == 0) {
	 System.err.println("DYMAC: waiting for user to start process: " + start);
       }

      try {
	 Thread.sleep(1000l);
       }
      catch (InterruptedException e) { }
    }

   if (candidates.size() > 1) {
      System.out.println("Please choose one of the following:");
      for (Process p : candidates) {
	 System.out.println("\t" + p.getId() + "\t" + p.getName());
       }
      System.exit(0);
    }

   Process p = candidates.get(0);

   return p.getId();
}




/********************************************************************************/
/*										*/
/*	Methods to check for a particular process				*/
/*										*/
/********************************************************************************/

boolean checkFor(String pid)
{
   getProcesses();

   for (Process p : process_list) {
      if (p.getId().equals(pid) && !p.isAttached()) return true;
    }

   return false;
}




/********************************************************************************/
/*										*/
/*	Methods to get processes from DYMON					*/
/*										*/
/********************************************************************************/

private void getProcesses()
{
   String cmd = "PTABLE";
   if (last_id != null) cmd += " " + last_id;
   String drslt = dymac_main.getDymon().dymonCommand(cmd);
   if (drslt == null) return;

   List<Process> rslt = new ArrayList<Process>();
   Element xml = IvyXml.convertStringToXml(drslt);
   last_id = IvyXml.getAttrString(xml,"COUNT");
   for (Element px : IvyXml.children(xml,"PROCESS")) {
      Process p = new Process(px);
      rslt.add(p);
    }

   process_list = rslt;
}



/********************************************************************************/
/*										*/
/*	Subclass holding process information					*/
/*										*/
/********************************************************************************/

private static class Process {

   private String process_id;
   private String start_class;
   private String show_name;
   private boolean is_attached;

   Process(Element xml) {
      process_id = IvyXml.getTextElement(xml,"ID");
      start_class = IvyXml.getTextElement(xml,"START");
      show_name = IvyXml.getTextElement(xml,"NAME");
      is_attached = Boolean.valueOf(IvyXml.getTextElement(xml,"ATTACHED"));
    }

   String getId()			{ return process_id; }
   String getStartClass()		{ return start_class; }
   String getName()			{ return show_name; }
   boolean isAttached() 		{ return is_attached; }

}	// end of subclass Process




}	// end of class DymacAttach



/* end of DymacAttach.java */
