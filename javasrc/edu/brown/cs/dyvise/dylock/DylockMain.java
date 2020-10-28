/********************************************************************************/
/*										*/
/*		DylockMain.java 						*/
/*										*/
/*	DYVISE lock analysis main program					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylock/DylockMain.java,v 1.8 2013/09/04 18:36:28 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylockMain.java,v $
 * Revision 1.8  2013/09/04 18:36:28  spr
 * Minor bug fixes.
 *
 * Revision 1.7  2013-05-09 12:29:00  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.6  2012-10-05 00:52:41  spr
 * Update lock analysis.  Prepare for pattern display.
 *
 * Revision 1.5  2011-09-12 19:39:08  spr
 * Clean up
 *
 * Revision 1.4  2011-09-12 18:29:53  spr
 * Update locking main program.
 *
 * Revision 1.3  2011-04-01 23:09:02  spr
 * Bug clean up.
 *
 * Revision 1.2  2011-03-19 20:34:18  spr
 * Clean up and fix bugs in dylock.
 *
 * Revision 1.1  2011-03-10 02:25:00  spr
 * New version of dylock to produce visualizations.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dylock;

import edu.brown.cs.ivy.xml.*;
import edu.brown.cs.ivy.exec.*;

import java.util.*;
import java.io.*;


public class DylockMain implements DylockConstants, DylockConstants.DylockExec,
	DylockConstants.DylockLockDataManager
{



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   DylockExec execable = null;

   if (args.length > 0) {
      if (args[0].startsWith("-c")) execable = new DylockCollector(args);
      else if (args[0].startsWith("-r")) execable = new DylockRunner(args);
      else if (args[0].startsWith("-v")) execable = new DylockViewer(args);
      else if (args[0].startsWith("-PV")) execable = new DylockPatternViewer(args);
      else if (args[0].startsWith("-P")) execable = new DylockPatternAnalyzer(args);
    }

   if (execable == null) execable = new DylockMain(args);

   execable.process();
}



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		info_data;
private String		lock_logs;
private Map<Integer,DylockLockData> lock_map;
private String		start_class;
private Map<String,String> name_map;
private Map<Integer,DylockLockLocation> location_map;
private RandomAccessFile lock_file;
private Map<Integer,Long> position_map;
private Map<Integer,DylockThreadData> thread_map;
private int		max_depth;
private Map<DylockLockData,DylockAnalysis> lock_analysis;
private File		output_file;
private boolean 	merge_priors;
private double		max_time;

private static int	MAX_SINGLE = 10000000;
private static int	MAX_GROUP  = 100000000;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private DylockMain(String [] args)
{
   info_data = null;
   lock_logs = null;
   lock_map = new HashMap<Integer,DylockLockData>();
   start_class = null;
   name_map = new HashMap<String,String>();
   lock_file = null;
   location_map = new HashMap<Integer,DylockLockLocation>();
   max_depth = 0;
   output_file = null;
   lock_analysis = new HashMap<DylockLockData,DylockAnalysis>();
   thread_map = new HashMap<Integer,DylockThreadData>();
   merge_priors = false;

   scanArgs(args);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

DylockLockData findLock(Integer id)			{ return lock_map.get(id); }
@Override public DylockLockData findLock(int id)	{ return lock_map.get(id); }


@Override public DylockLockLocation findLocation(int key)	{ return location_map.get(key); }
@Override public DylockThreadData findThread(int id)		{ return thread_map.get(id); }
@Override public double getMaxTime()				{ return max_time; }




boolean getMergePriors()				{ return merge_priors; }



/********************************************************************************/
/*										*/
/*	Argument scanning methods						*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   int start = 0;
   if (args.length > 0 && args[0].startsWith("-a")) start = 1;

   for (int i = start; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
	 if (args[i].startsWith("-d") && i+1 < args.length) {           // -d <data>
	    info_data = args[++i];
	  }
	 else if (args[i].startsWith("-l") && i+1 < args.length) {      // -l <log>
	    lock_logs = args[++i];
	  }
	 else if (args[i].startsWith("-i") && i+1 < args.length) {      // -i <input>
	    String root = args[++i];
	    info_data = root + ".info";
	    lock_logs = root + ".csv";
	    output_file = new File(root + ".out");
	    name_map.put("OUTPUT",output_file.getPath());
	  }
	 else if (args[i].startsWith("-o") && i+1 < args.length) {      // -o <xml output>
	    output_file = new File(args[++i]);
	    name_map.put("OUTPUT",output_file.getPath());
	  }
	 else if (args[i].startsWith("-s") && i+1 < args.length) {      // -s <start class>
	    start_class = args[++i];
	    name_map.put("START",start_class);
	  }
	 else if (args[i].startsWith("-m")) {                           // -mergeprior
	    merge_priors = true;
	  }
	 else badArgs();
       }
      else {
	 badArgs();
       }
    }

   if (info_data == null || lock_logs == null) badArgs();
}



private void badArgs()
{
   System.err.println("DYLOCK: dylock -a -d <data file> -l <log file>");
   System.exit(1);
}



/********************************************************************************/
/*										*/
/*	Main processing methods 						*/
/*										*/
/********************************************************************************/

@Override public void process()
{
   readInfoFile();

   mergeLocks();

   initialScan();

   lockScan();

   outputLockData();
}




/********************************************************************************/
/*										*/
/*	Information reatind methods						*/
/*										*/
/********************************************************************************/

private void readInfoFile()
{
   BufferedReader br = null;
   try {
      br = new BufferedReader(new FileReader(info_data));
      for ( ; ; ) {
	 String ln = br.readLine();
	 if (ln == null) break;
	 // String [] cnts = ln.split("\\|");
	 String [] cnts = splitLine(ln);
	 if (cnts.length < 2) continue;
	 int lid = Integer.parseInt(cnts[1]);
	 if (lid < 0) continue;
	 if (cnts[0].equals("LOC")) {
	    DylockLockLocation loc = new DylockLockLocation(cnts[1],cnts[2],cnts[3],cnts[4],cnts[5]);
	    location_map.put(lid,loc);
	  }
	 else if (cnts[0].equals("THREAD")) {
	    int v1 = Integer.parseInt(cnts[1]);
	    int v3 = Integer.parseInt(cnts[3]);
	    DylockThreadData td = new DylockThreadData(v1,cnts[2],v3,cnts[4]);
	    thread_map.put(v1,td);
	  }
	 else if (cnts[0].equals("MONITOR")) {
	    DylockLockData ld = new DylockLockData(this,cnts[1],cnts[2]);
	    lock_map.put(ld.getLockId(),ld);
	    // System.err.println("FUOND MON " + ld.getLockId() + " " + cnts[1]);
	  }
	 else if (cnts[0].equals("PRIOR")) {
	    int v1 = Integer.parseInt(cnts[1]);
	    int v2 = Integer.parseInt(cnts[2]);
	    DylockLockData ld1 = lock_map.get(v1);
	    DylockLockData ld2 = lock_map.get(v2);
	    if (ld1 != null && ld2 != null) ld1.addPrior(ld2);
	  }
	 else if (cnts[0].equals("STATS")) {
	    int v = Integer.parseInt(cnts[1]);
	    DylockLockData ld1 = lock_map.get(v);
	    if (ld1 == null) {
	       System.err.println("STATS LOCK NOT FOUND FOR " + cnts[1] + " IN " + ln);
	     }
	    else ld1.setCounts(cnts[2],cnts[3],cnts[4],cnts[5],cnts[6],cnts[7],cnts[8]);
	  }
       }
    }
   catch (IOException e) {
      System.err.println("DYLOCK: Problem reading information file: " + e);
      System.exit(1);
    }
   finally {
      if (br != null) {
	 try { br.close(); } catch (IOException ex) { }
       }
    }

   br = null;
   try {
      br = new BufferedReader(new FileReader(lock_logs));
      for ( ; ; ) {
	 String ln = br.readLine();
	 if (ln == null) break;
	 if (ln.startsWith("ENDBLOCK")) continue;
	 try {
	    String [] cnts = splitLine(ln);
	    if (cnts.length < 5) {
	       System.err.println("BAD INPUT LINE: " + ln);
	       continue;
	     }
	    String locs = cnts[2];
	    int idx = locs.indexOf("-");
	    if (idx > 0) locs = locs.substring(0,idx);
	    int loc = Integer.parseInt(locs);
	    int lock = Integer.parseInt(cnts[4]);
	    DylockLockData ld = lock_map.get(lock);
	    DylockLockLocation xld = location_map.get(loc);
	    if (xld == null) {
	       System.err.println("LOCATION " + loc + " NOT FOUND for " + ln);
	       System.exit(1);
	     }
	    else if (ld == null) {
	       System.err.println("LOCK " + lock + " NOT FOUND for " + ln);
	     }
	    else {
	       xld.setUsed();
	       if (cnts[0].startsWith("WAIT")) xld.setDoesWait();
	       else if (cnts[0].equals("NOTIFY")) xld.setDoesNotify();
	       ld.addLocation(xld);
	     }
	  }
	 catch (NumberFormatException e) {
	    System.err.println("BAD INPUT LINE: " + ln);
	    continue;
	  }
       }
    }
   catch (IOException e) {
      System.err.println("DYLOCK: Problem reading data file: " + e);
      System.exit(1);
    }
   finally {
      if (br != null) {
	 try { br.close(); } catch (IOException e) { }
       }
    }

   // might want to keep locks that are priors to a used lock
   for (Iterator<DylockLockData> it = lock_map.values().iterator(); it.hasNext(); ) {
      DylockLockData ld = it.next();
      if (ld == null || ld.getLockId() <= 0 || !ld.isUsed()) it.remove();
    }
}



static String [] splitLine(String txt)
{
   char [] buf = txt.toCharArray();
   int ct = 0;
   for (int i = 0; i < buf.length; ++i) {
      if (buf[i] == '|') ++ct;
    }
   String [] rslt = new String[ct+1];

   ct = 0;
   int st = 0;
   for (int i = 0; i < buf.length; ++i) {
      if (buf[i] == '|') {
	 rslt[ct++] = new String(buf,st,i-st);
	 st = i+1;
       }
    }
   rslt[ct++] = new String(buf,st,buf.length-st);

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Lock merging methods							*/
/*										*/
/********************************************************************************/

private void mergeLocks()
{
   ArrayList<DylockLockData> locks = new ArrayList<DylockLockData>(lock_map.values());

   for (int i = 0; i < locks.size(); ++i) {
      DylockLockData li = locks.get(i);
      boolean chng = true;
      while (chng) {
	 chng = false;
	 for (int j = i+1; j < locks.size(); ++j) {
	    DylockLockData lj = locks.get(j);
	    if (lj == null) continue;
	    if (li.isSameLock(lj)) {
	       li.mergeLock(lj);
	       locks.remove(j);
	       --j;
	       chng = true;
	     }
	  }
       }
    }

   // might want to add in locks that are priors to the used locks

   Set<DylockLockData> used = new HashSet<DylockLockData>(locks);

   for (DylockLockData ld : locks) {
      ld.mergePriors(used);
    }
}




/********************************************************************************/
/*										*/
/*     Methods to scan the lock trace file to find locations			*/
/*										*/
/********************************************************************************/

private void initialScan()
{
   File f1 = new File(lock_logs);
   File fdir = f1.getParentFile();
   String f1n = f1.getName();
   File f2 = new File(fdir,"sorted_" + f1n);
   String log1 = f2.getPath();

   String cmd = "sort -t| -n -k5 -n -k4 -n -k2 -o " + log1 + " " + lock_logs;
   try {
      IvyExec exc = new IvyExec(cmd);
      exc.waitFor();
    }
   catch (IOException e) {
      System.err.println("DYLOCK: Problem sorting lock trace file: " + e);
      System.exit(1);
    }
   f2.deleteOnExit();

   max_time = 0;

   try {
      lock_file = new RandomAccessFile(log1,"r");
      position_map = new HashMap<Integer,Long>();
      long flen = lock_file.length();
      int lastlock = -1;
      for ( ; ; ) {
	 long pos = lock_file.getFilePointer();
	 if (pos >= flen) break;
	 String line = lock_file.readLine();
	 if (line == null) break;
	 if (line.startsWith("ENDBLOCK")) continue;
	 DylockLockEntry le = new DylockLockEntry(this,line);
	 if (le.getNestedDepth() > max_depth) max_depth = le.getNestedDepth();
	 if (le.getLockId() != lastlock) {
	    lastlock = le.getLockId();
	    position_map.put(lastlock,pos);
	  }
	 max_time = Math.max(max_time,le.getTime());
       }
    }
   catch (IOException e) {
      System.err.println("DYLOCK: Problem reading lock trace file: " + e);
      System.exit(1);
    }
}




/********************************************************************************/
/*										*/
/*	Scan a lock								*/
/*										*/
/********************************************************************************/

private void lockScan()
{
   for (DylockLockData ld : lock_map.values()) {
      if (ld.isMerged()) continue;
      List<List<TraceLockEntry>> tocheck = new ArrayList<List<TraceLockEntry>>();

      List<TraceLockEntry> ent = buildLockList(ld);
      tocheck.add(ent);

      int totsz = 0;
      for (DylockLockData lde : ld.getEquivalents()) {
	 List<TraceLockEntry> ent1 = buildLockList(lde);
	 totsz += ent1.size();
	 tocheck.add(ent1);
	 if (totsz > MAX_GROUP && MAX_GROUP > 0) break;
       }

      DylockAnalysis anal = new DylockAnalysis(ld);
      lock_analysis.put(ld,anal);
      for (List<TraceLockEntry> lent : tocheck) {
	 anal.check(lent);
       }

      anal.finishChecks();

      System.err.println("FINISH GROUP");
    }
}




private List<TraceLockEntry> buildLockList(DylockLockData ld)
{
   List<TraceLockEntry> rslt = new ArrayList<TraceLockEntry>();

   if (!position_map.containsKey(ld.getLockId())) {
      System.err.println("NO START FOR " + ld.getLockId());
      return rslt;
    }

   int ct = 0;
   long start = position_map.get(ld.getLockId());
   try {
      lock_file.seek(start);
      for ( ; ; ) {
	 String ln = lock_file.readLine();
	 if (ln == null) break;
	 DylockLockEntry le = new DylockLockEntry(this,ln);
	 if (le.getLockId() != ld.getLockId()) break;
	 if (++ct > MAX_SINGLE && MAX_SINGLE > 0 && le.getEntryType() == TraceEntryType.RESET)
	    break;
	 rslt.add(le);
       }
    }
   catch (IOException e) {
      System.err.println("Problem scanning lock file: " + e);
      System.exit(1);
    }

   System.err.println("LIST FOR " + ld.getLockId() + " " + start + " " + rslt.size());

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

private void outputLockData()
{
   if (output_file != null) {
      try {
	 IvyXmlWriter xw = new IvyXmlWriter(output_file);
	 xw.begin("DYLOCK");
	 xw.field("TIME",max_time);
	 if (start_class != null) xw.field("START",start_class);

	 for (DylockThreadData td : thread_map.values()) {
	    td.outputXml(xw);
	  }

	 for (Map.Entry<DylockLockData,DylockAnalysis> ent : lock_analysis.entrySet()) {
	    DylockLockData ld = ent.getKey();
	    DylockAnalysis da = ent.getValue();
	    xw.begin("LOCK");
	    ld.outputXml(xw);
	    da.outputXml(xw);
	    xw.end("LOCK");
	  }

	 xw.end("DYLOCK");
	 xw.close();
       }
      catch (IOException e) {
	 System.err.println("DYLOCK: Problem generating XML output: " + e);
	 System.exit(1);
       }
    }
}




}	// end of class DylockMain



/* end of DylockMain.java */




/* end of DylockMain.java */
