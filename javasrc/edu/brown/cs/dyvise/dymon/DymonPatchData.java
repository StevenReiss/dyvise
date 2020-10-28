/********************************************************************************/
/*										*/
/*		DymonPatchData.java						*/
/*										*/
/*	DYPER monitor patch counter information holder				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymon/DymonPatchData.java,v 1.1.1.1 2008-10-22 13:16:47 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymonPatchData.java,v $
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymon;


import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.util.*;



class DymonPatchData implements DymonConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<Integer,PatchData>	id_table;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DymonPatchData()
{
   id_table = new HashMap<Integer,PatchData>();
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

CounterData getCounterData(int id)
{
   return id_table.get(id);
}



/********************************************************************************/
/*										*/
/*	Method to add elements							*/
/*										*/
/********************************************************************************/

void addClassData(Element e)
{
   ClassData pd = new ClassData(e);
   id_table.put(pd.getIndex(),pd);
}



void addMethodData(Element e)
{
   MethodData md = new MethodData(e);
   id_table.put(md.getIndex(),md);

   for (Element be : IvyXml.elementsByTag(e,"BLOCK")) {
      BlockData bd = new BlockData(be,md);
      id_table.put(bd.getIndex(),bd);
    }
}



/********************************************************************************/
/*										*/
/*	Methods to note which counters are active				*/
/*										*/
/********************************************************************************/

void manageActiveData(Collection<Integer> ids,long when,long totsam,long actsam,
			 boolean start,String forwhom)
{
   for (Integer id : ids) {
      PatchData pd = id_table.get(id);
      if (pd != null) pd.noteActive(when,totsam,actsam,start,forwhom);
    }
}




/********************************************************************************/
/*										*/
/*	PatchData generic definitions						*/
/*										*/
/********************************************************************************/

private static abstract class PatchData implements CounterData {

   private final static int START_ACTIVE = 0;
   private final static int TOTAL_ACTIVE = 1;
   private final static int TIMES_ACTIVE = 2;
   private final static int TOTAL_TOT = 3;
   private final static int TOTAL_ACT = 4;
   private final static int START_TOT = 5;
   private final static int START_ACT = 6;

   private int	data_index;
   private Map<String,long []> active_data;

   protected PatchData(Element e) {
      data_index = IvyXml.getAttrInt(e,"INDEX");
      active_data = new HashMap<String,long []>();
    }

   int getIndex()				{ return data_index; }

   void noteActive(long when,long totsam,long actsam,boolean start,String forwhom) {
      long [] data = active_data.get(forwhom);
      if (data == null) {
	 data = new long[7];
	 active_data.put(forwhom,data);
       }
      if (start && data[START_ACTIVE] == 0) {
	 data[START_ACTIVE] = when;
	 data[START_TOT] = totsam;
	 data[START_ACT] = actsam;
	 ++data[TIMES_ACTIVE];
       }
      else if (data[START_ACTIVE] != 0) {
	 data[TOTAL_ACTIVE] += when - data[START_ACTIVE];
	 data[TOTAL_TOT] += totsam - data[START_TOT];
	 data[TOTAL_ACT] += actsam - data[START_ACT];
	 data[START_ACTIVE] = 0;
	 data[START_TOT] = 0;
	 data[START_ACT] = 0;
       }
    }

   public String getName()			{ return null; }
   public String getName(int idx)		{ return null; }

   public boolean isRange()			{ return false; }

   public long getActiveTime(long now,String whom) {
      long [] data = active_data.get(whom);
      if (data == null) return 0;
      long tot = data[TOTAL_ACTIVE];
      if (data[START_ACTIVE] > 0) tot += now - data[START_ACTIVE];
      return tot;
    }

   public double getActiveFraction(long totsam,long actsam,String whom) {
      long [] data = active_data.get(whom);
      if (data == null) return 0;
      double tot = data[TOTAL_TOT];
      double act = data[TOTAL_ACT];
      if (data[START_TOT] > 0) tot += totsam - data[START_TOT];
      if (data[START_ACT] > 0) act += actsam - data[START_ACT];
      if (tot == 0) return 1;
      return act / tot;
    }

   public long getActiveRunTime(long now,long totsam,long actsam,String whom) {
      return (long)(getActiveTime(now,whom) * getActiveFraction(totsam,actsam,whom) + 0.5);
    }

   public long getTimesActive(String whom) {
      long [] data = active_data.get(whom);
      if (data == null) return 0;
      return data[TIMES_ACTIVE];
    }

   public boolean isActive(String whom) {
      long [] data = active_data.get(whom);
      if (data == null) return false;
      return data[START_ACTIVE] != 0;
    }




}	// end of subclass PatchData




/********************************************************************************/
/*										*/
/*	Class data definitions							*/
/*										*/
/********************************************************************************/

private static class ClassData extends PatchData {

   private String class_name;

   ClassData(Element e) {
      super(e);
      class_name = IvyXml.getAttrString(e,"NAME");
    }

   public String getName() {
      return class_name;
    }

}	// end of subclass ClassData




private static class MethodData extends PatchData {

   private String dymon_name;
   private String method_name;

   MethodData(Element e) {
      super(e);
      method_name = IvyXml.getAttrString(e,"NAME");

      int idx = method_name.lastIndexOf(".");
      if (idx < 0) dymon_name = method_name;
      else {
	 dymon_name = method_name.substring(0,idx) + "@" + method_name.substring(idx+1);
       }
    }

   public String getName() {
      return dymon_name;
    }

}	// end of subclass MethodData




private static class BlockData extends PatchData {

   private MethodData for_method;
   private int start_line;
   private int end_line;
   private String dymon_name;

   BlockData(Element e,MethodData md) {
      super(e);
      for_method = md;
      start_line = IvyXml.getAttrInt(e,"START");
      end_line = IvyXml.getAttrInt(e,"END");
      dymon_name = for_method.getName() + "@" + start_line;
      if (end_line != start_line) dymon_name += "-" + end_line;
    }

   public String getName() {
      return dymon_name;
    }

   public boolean isRange() {
      return start_line != end_line;
    }

   public String getName(int idx) {
      int ln = start_line + idx;
      if (ln > end_line) return null;
      return for_method.getName() + "@" + ln;
    }

}	// end of subclass BlockData





}	// end of class DymonPatchData




/* end of DymonPatchData.java */
