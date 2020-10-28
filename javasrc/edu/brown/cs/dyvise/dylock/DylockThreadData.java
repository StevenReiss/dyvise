/********************************************************************************/
/*										*/
/*		DylockThreadData.java						*/
/*										*/
/*	Information for a thread						*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylock/DylockThreadData.java,v 1.1 2013-05-09 12:29:01 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylockThreadData.java,v $
 * Revision 1.1  2013-05-09 12:29:01  spr
 * Dylock update.	Last 1.6 update
 *
 *
 ********************************************************************************/





package edu.brown.cs.dyvise.dylock;

import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.Element;



class DylockThreadData implements DylockConstants
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private int	dylock_id;
private String	thread_name;
private String	thread_class;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DylockThreadData(int id,String cls,int tid,String tnm)
{
   dylock_id = id;
   thread_name = tnm;
   thread_class = cls;
}


DylockThreadData(Element e)
{
   dylock_id = IvyXml.getAttrInt(e,"ID");
   thread_name = IvyXml.getAttrString(e,"NAME");
   thread_class = IvyXml.getAttrString(e,"CLASS");
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

int getId()				{ return dylock_id; }
String getName()			{ return thread_name; }
String getDisplayName()
{
   return thread_name;
}



/********************************************************************************/
/*										*/
/*	Output Methods								*/
/*										*/
/********************************************************************************/

void outputXml(IvyXmlWriter xw)
{
   xw.begin("THREAD");
   xw.field("ID",dylock_id);
   xw.field("NAME",thread_name);
   xw.field("CLASS",thread_class);
   xw.end("THREAD");
}



@Override public String toString()
{
   return getDisplayName(); 
}



}	// end of class DylockThreadData




/* end of DylockThreadData.java */
