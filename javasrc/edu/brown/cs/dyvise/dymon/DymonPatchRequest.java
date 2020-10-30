/********************************************************************************/
/*										*/
/*		DymonPatchRequest.java						*/
/*										*/
/*	DYPER monitor patch request holder					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymon/DymonPatchRequest.java,v 1.7 2013/09/04 18:36:32 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymonPatchRequest.java,v $
 * Revision 1.7  2013/09/04 18:36:32  spr
 * Minor bug fixes.
 *
 * Revision 1.6  2012-10-05 00:52:56  spr
 * Code clean up.
 *
 * Revision 1.5  2009-09-19 00:09:46  spr
 * Update dymon for seletive agent insertion and removal.
 *
 * Revision 1.4  2009-03-20 02:06:50  spr
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



public abstract class DymonPatchRequest implements Comparable<DymonPatchRequest>,
		DymonConstants, MintConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private DymonProcess for_process;
private DymonDetailing for_detailing;
private String	patch_model;
private long	patch_duration;
private Map<String,String> patch_names;
private Set<String> patch_change;
private Set<String>	method_set;
private Collection<Integer> patch_items;
private int	patch_priority;
private boolean is_done;
private Object	doing_prepare;
private boolean model_changed;

protected static int	  min_counter = -1;

private static Map<DymonProcess,Object> lock_map = new WeakHashMap<DymonProcess,Object>();




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected DymonPatchRequest(DymonProcess dp,String mdl)
{
   for_process = dp;
   for_detailing = null;
   patch_model = mdl;
   patch_duration = 0;
   patch_names = new HashMap<String,String>();
   patch_change = new HashSet<String>();
   method_set = new HashSet<String>();
   patch_items = new HashSet<Integer>();
   patch_priority = PATCH_PRIORITY_NORMAL;
   is_done = false;
   model_changed = true;

   synchronized (lock_map) {
      doing_prepare = lock_map.get(dp);
      if (doing_prepare == null) {
	 doing_prepare = new Object();
	 lock_map.put(dp,doing_prepare);
       }
    }

   setMethods(null);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void setDetailing(DymonDetailing dd)		{ for_detailing = dd; }


List<DymonProcess> getProcesses()
{
   List<DymonProcess> r = new ArrayList<DymonProcess>();
   r.add(for_process);
   return r;
}


long getDuration()				{ return patch_duration; }
void setDuration(long v)			{ patch_duration = v; }

String getModelName()				{ return patch_model; }
abstract String getRequestName();


Iterable<String> getClasses()
{
   return new ArrayList<String>(patch_names.keySet());
}


String getPatchFile(String cls) 		{ return patch_names.get(cls); }
boolean isPatchChanged(String cls)		{ return patch_change.contains(cls); }

boolean isEmpty()				{ return method_set.isEmpty(); }

boolean allowEmptyPatch()			{ return false; }

int getPriority()				{ return patch_priority; }
void setPriority(int p) 			{ patch_priority = p; }
void raisePriority()
{
   if (--patch_priority < 0) patch_priority = 0;
}


boolean isDone()				{ return is_done; }


public int compareTo(DymonPatchRequest pr)
{
   if (patch_priority < pr.patch_priority) return -1;
   if (patch_priority > pr.patch_priority) return 1;
   return 0;
}


protected String getVjmtiAgent()		{ return null; }
protected String getVjmtiArguments()		{ return null; }
protected String getVjmtiMintName()		{ return null; }



public PatchOverlap getPatchOverlap()		{ return PatchOverlap.NONE; }
public boolean excludeOverlap(String id)	{ return false; }

void setMethods(Collection<String> methods)
{
   if (methods == null) {
      if (!method_set.isEmpty()) {
	 method_set.clear();
	 model_changed = true;
       }
      return;
    }

   Set<String> nmthd = new HashSet<String>();

   for (String s : methods) {
      int idx = s.indexOf("@");
      if (idx < 0) break;
      String c = s.substring(0,idx);
      String m = s.substring(idx+1);
      idx = m.indexOf("@");
      if (idx > 0) m = m.substring(0,idx);
      String cm = c + "@" + m;
      // System.err.println("DYMON: ADD " + cm);
      nmthd.add(cm);
    }

   if (method_set.equals(nmthd)) return;

   method_set = nmthd;
   model_changed = true;
}



void reset(Collection<String> methods,long duration,int priority)
{
   setMethods(methods);
   setDuration(duration);
   setPriority(priority);
}



/********************************************************************************/
/*										*/
/*	Methods to handle actual patching					*/
/*										*/
/********************************************************************************/

final void prepare()
{
   prepareLibraries();
   prepareClasses();
}




private void prepareLibraries()
{
   String s = getVjmtiAgent();
   if (s == null) return;
   for_process.loadVjmtiAgent(s,getVjmtiArguments());
}



private void prepareClasses()
{
   if (isEmpty()) return;

   updateModel();
}


final void instrument(boolean add)
{
   handleInstrument(add);
}



void donePrepare()						{ }

protected void addPatchInfo(IvyXmlWriter xw,String c,String m)	{ }

protected void addPatchCommands(IvyXmlWriter xw,boolean insert) { }




/********************************************************************************/
/*										*/
/*	Dypatch communication							*/
/*										*/
/********************************************************************************/

private void updateModel()
{
   if (!model_changed) return;

   Collection<String> classes = new HashSet<String>();

   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("PATCHMODEL");
   xw.field("NAME",patch_model);
   for (String s : method_set) {
      int idx = s.indexOf("@");
      String c = s.substring(0,idx);
      classes.add(c);
      patch_names.remove(c);
      patch_change.remove(c);
      String m = s.substring(idx+1);
      xw.begin("FOR");
      xw.field("CLASS",c);
      xw.field("METHOD",m);
      addPatchInfo(xw,c,m);
      xw.end("FOR");
    }
   for (String s : classes) {
      xw.begin("CLASS");
      xw.field("NAME",s);
      xw.end();
    }
   xw.end("PATCHMODEL");

   Element xml = null;
   
   System.err.println("DYMON: Setup patch request: " + xw.toString());

   synchronized (doing_prepare) {
      MintDefaultReply mr = new MintDefaultReply();
      for_process.sendPatchMessage("MODEL",xw.toString(),mr,MINT_MSG_FIRST_REPLY);
      xml = mr.waitForXml(120000);
    }

   if (xml != null) {
      Element ce = IvyXml.getElementByTag(xml,"COUNTERS");
      System.err.println("DYMON: COUNTER DATA: " + IvyXml.convertXmlToString(xml));
      if (ce != null) {
	 synchronized (patch_items) {
	    for (Element e : IvyXml.elementsByTag(ce,"USE")) {
	       int id = IvyXml.getAttrInt(e,"ID");
	       if (min_counter < 0 || id < min_counter) min_counter = id;
	       patch_items.add(id);
	     }
	  }
       }
      ce = IvyXml.getElementByTag(xml,"COUNTERDATA");
      if (ce != null) {
	 for (Element e : IvyXml.elementsByTag(ce,"CLASS")) {
	    for_process.addClassData(e);
	  }
	 for (Element e : IvyXml.elementsByTag(ce,"METHOD")) {
	    for_process.addMethodData(e);
	  }
       }
    }
}



private void handleInstrument(boolean add)
{
   patch_names.clear();
   patch_change.clear();

   if (isEmpty()) return;

   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("ACTIVATE");
   xw.begin("PATCHMODEL");
   xw.field("NAME",patch_model);
   xw.field("ACTION",(add ? "ADD" : "REMOVE"));
   xw.end("PATCHMODEL");
   xw.end("ACTIVATE");

   Element xml = null;

   synchronized (doing_prepare) {
      MintDefaultReply mr = new MintDefaultReply();
      for_process.sendPatchMessage("ACTIVATE",xw.toString(),mr,MINT_MSG_FIRST_REPLY);
      xml = mr.waitForXml(60000);
    }
   xw.close();

   System.err.println("DYPATCH RESPONSE: " + IvyXml.convertXmlToString(xml));
   if (xml != null) {
      for (Element e : IvyXml.elementsByTag(xml,"CLASS")) {
	 String c = IvyXml.getAttrString(e,"NAME");
	 String p = IvyXml.getAttrString(e,"FILE");
	 boolean orig = IvyXml.getAttrBool(e,"ORIGINAL");
	 patch_names.put(c,p);
	 if (!orig) patch_change.add(c);
       }
    }
}




/********************************************************************************/
/*										*/
/*	Item management 							*/
/*										*/
/********************************************************************************/

final void handlePatchInsert(long when,boolean start)
{
   if (for_detailing != null) for_detailing.handlePatchTiming(when,start);

   handlePatch(when,start);

   synchronized (patch_items) {
      for_process.handleItems(patch_items,when,start,getRequestName());
    }

   if (!start) is_done = true;
}


protected void handlePatch(long when,boolean start)		{ }



}	// end of class DymonPatchRequest




/* end of DymonPatchRequest.java */
