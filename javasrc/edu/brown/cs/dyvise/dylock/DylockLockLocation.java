/********************************************************************************/
/*										*/
/*	`       DylockLockLocation.java                                         */
/*										*/
/*	DYVISE lock analysis lock location information holder			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylock/DylockLockLocation.java,v 1.4 2013-05-09 12:29:00 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylockLockLocation.java,v $
 * Revision 1.4  2013-05-09 12:29:00  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.3  2012-10-05 00:52:41  spr
 * Update lock analysis.  Prepare for pattern display.
 *
 * Revision 1.2  2011-04-01 23:09:02  spr
 * Bug clean up.
 *
 * Revision 1.1  2011-03-10 02:25:00  spr
 * New version of dylock to produce visualizations.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dylock;

import edu.brown.cs.ivy.xml.*;
import edu.brown.cs.ivy.file.*;

import org.w3c.dom.Element;

import java.util.*;



class DylockLockLocation implements DylockConstants, DylockConstants.TraceLockLocation,
		Comparable<DylockLockLocation>
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String class_name;
private String method_name;
private String method_signature;
private String location_output;
private int method_offset;
private boolean does_wait;
private boolean does_notify;
private int	location_id;
private boolean is_used;
private Set<DylockLockLocation> alias_locations;
private String	alias_ids;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DylockLockLocation(String id,String cls,String mthd,String sgn,String offset)
{
   location_id = Integer.parseInt(id);
   method_offset = Integer.parseInt(offset);
   method_name = mthd;
   method_signature = sgn;
   class_name = cls;
   does_wait = false;
   does_notify = false;
   location_output = null;
   is_used = false;
   alias_locations = null;
}





DylockLockLocation(Element xml,boolean view)
{
   class_name = IvyXml.getAttrString(xml,"CLASS");
   method_name = IvyXml.getAttrString(xml,"METHOD");
   method_signature = IvyXml.getAttrString(xml,"SIGNATURE");
   method_offset = IvyXml.getAttrInt(xml,"OFFSET");
   does_wait = IvyXml.getAttrBool(xml,"DOESWAIT");
   does_notify = IvyXml.getAttrBool(xml,"DOESNOTIFY");
   location_id = IvyXml.getAttrInt(xml,"ID");
   alias_ids = IvyXml.getAttrString(xml,"ALIASES");
   is_used = true;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void setDoesWait()				{ does_wait = true; }
void setDoesNotify()				{ does_notify = true; }
void setUsed()					{ is_used = true; }

@Override public String getClassName()		{ return class_name; }
@Override public String getMethodName() 	{ return method_name; }
@Override public String getMethodSignature()	{ return method_signature; }


@Override public boolean doesWait()		{ return does_wait; }
@Override public boolean doesNotify()		{ return does_notify; }

@Override public int getId()			{ return location_id; }
boolean isUsed()				{ return is_used; }

void addAlias(DylockLockLocation loc)
{
   if (alias_locations == null) alias_locations = new HashSet<DylockLockLocation>();
   alias_locations.add(loc);
}


@Override public void finishLoad(DylockLocationManager vr)
{
   if (alias_ids == null) return;

   for (StringTokenizer tok = new StringTokenizer(alias_ids,", "); tok.hasMoreTokens(); ) {
      String s = tok.nextToken();
      int idv = Integer.parseInt(s);
      DylockLockLocation dll = vr.findLocation(idv);
      if (dll != null) addAlias(dll);
    }

   alias_ids = null;
}


@Override public Collection<TraceLockLocation> getAliases()
{
   Collection<TraceLockLocation> locks = new ArrayList<TraceLockLocation>();
   if (alias_locations != null) locks.addAll(alias_locations);
   return locks;
}


@Override public String getKey()
{
   String key = "";
   if (class_name != null) key = class_name + "@";
   key += method_name;
   if (method_signature != null) key += method_signature;
   if (method_offset >= 0) key += "@" + method_offset;
   return key;
}



String getOutput()
{
   if (location_output == null) {
      StringBuffer buf = new StringBuffer();
      String cnm = IvyFormat.formatTypeName("L" + class_name + ";");
      buf.append(cnm);
      buf.append(".");
      buf.append(method_name);
      int idx = method_signature.lastIndexOf(")");
      String sg = method_signature.substring(1,idx);
      buf.append("(");
      if (sg.length() > 0) {
	 String sgn = IvyFormat.formatTypeNames(sg,"@");
	 StringTokenizer tok = new StringTokenizer(sgn,"@");
	 int ct = 0;
	 while (tok.hasMoreTokens()) {
	    String a0 = tok.nextToken();
	    int idx1 = a0.indexOf("<");
	    if (idx1 < 0) idx1 = a0.lastIndexOf(".");
	    else idx1 = a0.lastIndexOf(".",idx1);
	    if (idx1 > 0) a0 = a0.substring(idx1+1);
	    if (ct++ > 0) buf.append(",");
	    buf.append(a0);
	  }
       }
      buf.append(")");
      if (method_offset > 0) {
	 buf.append(" @");
	 buf.append(method_offset);
       }
      location_output = buf.toString();
    }

   return location_output;
}



/********************************************************************************/
/*										*/
/*	Comparison methods							*/
/*										*/
/********************************************************************************/

@Override public int compareTo(DylockLockLocation dll)
{
   return getOutput().compareTo(dll.getOutput());
}



@Override public boolean equals(Object o)
{
   if (o instanceof DylockLockLocation) {
      DylockLockLocation ll = (DylockLockLocation) o;
      if (class_name == ll.class_name && method_name == ll.method_name &&
	     method_signature == ll.method_signature &&
	     method_offset == ll.method_offset) return true;
    }
   return false;
}



@Override public int hashCode()
{
   int hc = 0;
   if (class_name != null) hc += class_name.hashCode();
   if (method_name != null) hc += method_name.hashCode();
   if (method_signature != null) hc += method_signature.hashCode();
   hc += method_offset;
   return hc;
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public void outputXml(IvyXmlWriter xw,boolean alias)
{
   xw.begin("LOCATION");
   if (class_name != null) xw.field("CLASS",class_name);
   if (method_name != null) xw.field("METHOD",method_name);
   if (method_signature != null) xw.field("SIGNATURE",method_signature);
   if (method_offset >= 0) xw.field("OFFSET",method_offset);
   xw.field("ID",location_id);
   xw.field("DOESWAIT",does_wait);
   xw.field("DOESNOTIFY",does_notify);
   if (alias) xw.field("ISALIAS",true);
   if (alias_locations != null && !alias_locations.isEmpty()) {
      StringBuffer buf = new StringBuffer();
      int ct = 0;
      for (DylockLockLocation dll : alias_locations) {
	 if (ct++ > 0) buf.append(",");
	 buf.append(dll.getId());
       }
      xw.field("ALIASES",buf);
    }
   xw.end();
}



@Override public void outputBuffer(StringBuffer buf)
{
   buf.append(getOutput());
}


@Override public String getDisplayName()
{
   return class_name + "." + method_name + "@" + method_offset;
}



/********************************************************************************/
/*										*/
/*	Debugging methods							*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   return class_name + "." + method_name + "@" + method_offset;
}



}	// end of class DylockLockLocation



/* end of DylockLockLocation.java */
