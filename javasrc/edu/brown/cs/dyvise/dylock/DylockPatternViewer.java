/********************************************************************************/
/*										*/
/*		DylockPatternViewer.java					*/
/*										*/
/*	Viewer for pattern sets 						*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylock/DylockPatternViewer.java,v 1.1 2013-05-09 12:29:00 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylockPatternViewer.java,v $
 * Revision 1.1  2013-05-09 12:29:00  spr
 * Dylock update.	Last 1.6 update
 *
 *
 ********************************************************************************/




package edu.brown.cs.dyvise.dylock;

import edu.brown.cs.dyvise.dystore.*;
import edu.brown.cs.dyvise.dygraph.*;

import edu.brown.cs.ivy.xml.*;
import edu.brown.cs.ivy.file.*;

import org.w3c.dom.Element;

import java.util.*;
import java.io.*;
import javax.swing.*;
import java.util.List;


class DylockPatternViewer extends DylockLockManager implements DylockConstants, DylockConstants.DylockExec,
        DylockConstants.DylockPatternAccess
{



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   DylockPatternViewer pv = new DylockPatternViewer(args);
   pv.process();
}



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private List<DylockPattern>	all_patterns;
private String			pattern_file;
private String			lock_file;
private DystoreControl		data_store;
private DygraphControl		graph_control;
private DylockPattern		current_pattern;

private static final int MAX_TIME = 1000;


			

/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DylockPatternViewer(String [] args)
{
   lock_file = null;
   pattern_file = null;
   all_patterns = null;
   current_pattern = null;

   scanArgs(args);
}



/********************************************************************************/
/*										*/
/*	Argument scanning							*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-i") && i+1 < args.length) {              // -i <base path>
	 String base = args[++i];
	 pattern_file = base + ".pats";
	 lock_file = base + ".out";
       }
      else if (args[i].startsWith("-PV")) ;                             // -PV
      else badArgs();
    }

   if (pattern_file == null || lock_file == null) badArgs();
}



private void badArgs()
{
   System.err.println("DYLOCK: dylock -PV -i <base>");
   System.exit(1);
}



/********************************************************************************/
/*										*/
/*	Process methods 							*/
/*										*/
/********************************************************************************/

@Override public void process()
{
   readLockData(new File(lock_file));

   try {
      loadPatterns();
    }
   catch (Throwable t) {
      System.err.println("DYLOCK: Problem loading patterns: " + t);
      System.exit(2);
    }

   String tmdlf = IvyFile.expandName("$(BROWN_DYVISE_DYVISE)/lib/dylockpattern.xml");
   Element elt = IvyXml.loadXmlFromFile(tmdlf);
   Element telt = IvyXml.getChild(elt,"TUPLEMODEL");
   data_store = new DystoreControl(telt);

   Element gelt = IvyXml.getChild(elt,"GRAPHMODEL");
   graph_control = new DygraphControl(gelt,data_store);

   setupGraphics();
   setupValues();
}



/********************************************************************************/
/*										*/
/*	Loading methods 							*/
/*										*/
/********************************************************************************/

private void loadPatterns() throws IOException
{
   all_patterns = new ArrayList<DylockPattern>();

   Element xml = IvyXml.loadXmlFromFile(pattern_file);

   for (Element pe : IvyXml.children(xml,"PATTERN")) {
      DylockPattern pat = new DylockPattern(this, pe);
      all_patterns.add(pat);
    }
}



/********************************************************************************/
/*										*/
/*	Graphics setup methods							*/
/*										*/
/********************************************************************************/

private void setupGraphics()
{
   ToolTipManager ttm = ToolTipManager.sharedInstance();
   ttm.setDismissDelay(60*60*1000);
   ttm.setLightWeightPopupEnabled(false);

   JFrame disp = new DylockDisplay(graph_control,this);
   disp.setVisible(true);
}




/********************************************************************************/
/*										*/
/*	Handle presetting values						*/
/*										*/
/********************************************************************************/

private void setupValues()
{
   DystoreTable t1 = data_store.getTable("THREADLOCK");
   DystoreField fth1 = t1.getField("THREAD");
   DystoreField flk1 = t1.getField("LOCK");
   DystoreTable t2 = data_store.getTable("LOCKDATA");
   DystoreField fth2 = t2.getField("THREAD");
   DystoreField flk2 = t2.getField("LOCK");
   DystoreStore store1 = data_store.getStore(t1);
   DystoreStore store2 = data_store.getStore(t2);

   for (DylockPattern dp : all_patterns) {
      for (PatternEvent evt : dp.getEvents()) {
	 DylockThreadData td = evt.getThread();
	 store1.noteValue(fth1,td.getName());
	 store2.noteValue(fth2,td.getName());
	 DylockLockData ld = evt.getLock();
	 store1.noteValue(flk1,ld.getDisplayName());
	 store2.noteValue(flk2,ld.getDisplayName());
       }
    }
}




/********************************************************************************/
/*										*/
/*	Pattern selection methods						*/
/*										*/
/********************************************************************************/

@Override public List<DylockPattern> getPatterns()
{
   return all_patterns;
}



@Override public void setCurrentPattern(DylockPattern pat)
{
   if (pat == current_pattern) return;
   if (pat == null) return;

   DylockEventGenerator eg = new DylockEventGenerator(data_store);
   data_store.clear(false);
   for (PatternEvent pe : pat.getEvents()) {
      DylockViewType vt = pe.getView();
      vt.clear();
    }
   
   System.err.println("BEGIN NEW PATTERN");

   double t0 = 0;
   double t1 = 1000;
   for (PatternEvent pe : pat.getEvents()) {
      t1 = pe.getTime();
      if (t0 == 0 || t0 > t1) t0 = t1;
      eg.add(pe);
    }
   if (t0 == t1) {
      t0 = 0;
      t1 = MAX_TIME;
    }
   else {
      t0 = data_store.getStartTime();
      t1 = data_store.getEndTime();
    }

   graph_control.dataUpdated();
   graph_control.setTimeWindow(t0,t1,false);
}



}	// end of class DylockPatternViewer



/* end of DylockPatternViewer.java */
