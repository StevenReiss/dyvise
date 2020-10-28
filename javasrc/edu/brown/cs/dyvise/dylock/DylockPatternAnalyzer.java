/********************************************************************************/
/*										*/
/*		DylockPatternAnalyzer.java					*/
/*										*/
/*	Find and build set of patterns from a trace				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylock/DylockPatternAnalyzer.java,v 1.1 2013-05-09 12:29:00 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylockPatternAnalyzer.java,v $
 * Revision 1.1  2013-05-09 12:29:00  spr
 * Dylock update.	Last 1.6 update
 *
 *
 ********************************************************************************/





package edu.brown.cs.dyvise.dylock;

import edu.brown.cs.ivy.xml.*;

import java.io.*;
import java.util.*;


public class DylockPatternAnalyzer extends DylockLockManager
	implements DylockConstants, DylockConstants.DylockExec
{


/********************************************************************************/
/*										*/
/*	Main program for testing						*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{

   DylockPatternAnalyzer dpa = new DylockPatternAnalyzer(args);
   dpa.process();
}



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private File			lock_file;
private File			input_file;
private File			output_file;
private PatternSorter		pattern_sorter;
private DylockPatternBuilder	pattern_builder;
private Map<DylockLockData,Set<Integer>> valid_map;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DylockPatternAnalyzer(String [] args)
{
   lock_file = null;
   output_file = null;
   pattern_sorter = new PatternSorter();
   pattern_builder = null;
   valid_map = new HashMap<DylockLockData,Set<Integer>>();

   scanArgs(args);
}



/********************************************************************************/
/*										*/
/*	Argument processing methods						*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   int start = 0;
   if (args.length > 0 && args[0].startsWith("-P")) start = 1;

   while (args.length >= start + 2) {
      if (args[start].startsWith("-d") && lock_file == null) {               // -d <lock data file>
	 lock_file = new File(args[start+1]);
	 start += 2;
       }
      else if (args[start].startsWith("-i") && lock_file == null) {          // -i <input>
	 lock_file = new File(args[start+1] + ".out");
	 start += 2;
       }
      else if (args[start].startsWith("-o") && output_file == null) {        // -o <output>
	 output_file = new File(args[start+1]);
       }
      else if (args[start].startsWith("-t") && input_file == null) {         // -t <trace file>
	 input_file = new File(args[start+1]);
       }
      else break;
    }
   if (args.length >= start+1) {
      if (args[start].startsWith("-r")) ++start;
    }

   // handle socket connection and dylute as in DylockRunner

   if (lock_file == null) badArgs();
   if (output_file == null) {
      String fnm = lock_file.getPath();
      int idx = fnm.lastIndexOf(".");
      if (idx >= 0) output_file = new File(fnm.substring(0,idx) + ".pats");
      else output_file = new File(fnm + ".pats");
    }
   if (input_file == null) {
      String fnm = lock_file.getPath();
      int idx = fnm.lastIndexOf(".");
      if (idx >= 0) input_file = new File(fnm.substring(0,idx) + ".csv");
      else input_file = new File(fnm + ".csv");
      if (!input_file.exists()) input_file = null;
    }
}




private void badArgs()
{
   System.err.println("DYLOCK: dylockpatterenanalyzer -d <lock file>");
   System.exit(1);
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

@Override public void process()
{
   readLockData(lock_file);

   pattern_builder = new DylockPatternBuilder(this);
   processInput();

   try {
      IvyXmlWriter xw = new IvyXmlWriter(output_file);
      xw.begin("PATTERNS");
      pattern_builder.outputPatterns(xw);
      xw.end("PATTERNS");
      xw.close();
    }
   catch (IOException e) { }
}



/********************************************************************************/
/*										*/
/*	Trace file processing							*/
/*										*/
/********************************************************************************/

private void processInput()
{
   File f1 = input_file;

   try {
      List<DylockLockEntry> ents = new ArrayList<DylockLockEntry>();
      List<DylockLockEntry> lents = new ArrayList<DylockLockEntry>();
      double ltime = 0;
      double ntime = 0;
      BufferedReader br = new BufferedReader(new FileReader(f1));
      for ( ; ; ) {
	 String ln = br.readLine();
	 if (ln == null) break;
	 if (ln.startsWith("ENDBLOCK")) {
	    processBlock(lents);
	    List<DylockLockEntry> xents = lents;
	    lents = ents;
	    ents = xents;
	    ltime = ntime;
	    ntime = 0;
	  }
	 else {
	    DylockLockEntry ent = new DylockLockEntry(this,ln);
	    double t0 = ent.getTime();
	    if (t0 <= ltime) lents.add(ent);
	    else {
	       if (t0 > ntime) ntime = t0;
	       ents.add(ent);
	     }
	  }
       }
      processBlock(ents);
      br.close();
    }
   catch (IOException e) {
      System.err.println("DYLOCK: Problem reading input file: " + e);
    }
}


private void processBlock(List<DylockLockEntry> ents)
{
   if (ents.isEmpty()) return;
   
   Collections.sort(ents,pattern_sorter);

   for (DylockLockEntry ent : ents) {
      DylockLockData ld = ent.getLock();
      int tid = ent.getThreadId();
      EntryLock el = findEntryForLocation(ent.getLocation().getId());
      if (el == null) continue;
      DylockViewType vt = el.getViewType();
      
      boolean valid = false;
      if (ent.getEntryType() == TraceEntryType.RESET) {
         valid_map.remove(ld);
       }
      else {
         Set<Integer> valids = valid_map.get(ld);
         if (valids == null) {
            valids = new HashSet<Integer>();
            valid_map.put(ld,valids);
          }
         valid = valids.contains(tid);
         if (!valid && vt.startsValidRegion(this,ent)) {
            valids.add(tid);
            valid = true;
          }
       }
      vt.processPatternEntry(pattern_builder,ent,el.getLock(),valid);
    }

   dataUpdated();

   ents.clear();
}




private void dataUpdated()
{ }



/********************************************************************************/
/*										*/
/*	Time-based event sorter 						*/
/*										*/
/********************************************************************************/

private static class PatternSorter implements Comparator<DylockLockEntry> {

   @Override public int compare(DylockLockEntry e1,DylockLockEntry e2) {
      double d1 = e1.getTime() - e2.getTime();
      if (d1 < 0) return -1;
      if (d1 > 0) return 1;
      int d2 = e1.getThreadId() - e2.getThreadId();
      if (d2 < 0) return -1;
      if (d2 > 0) return 1;
      int d3 = e1.getLockId() - e2.getLockId();
      if (d3 < 0) return -1;
      if (d3 > 0) return 1;
      int d4 = e1.getEntryType().ordinal() - e2.getEntryType().ordinal();
      return d4;
    }

}	// end of inner class PatternSorter



}	// end of class DylockPatternAnalyzer




/* end of DylockPatternAnalyzer.java */
