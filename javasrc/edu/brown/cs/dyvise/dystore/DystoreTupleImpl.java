/********************************************************************************/
/*										*/
/*		DystoreTupleImpl.java						*/
/*										*/
/*	DYVISE implementation of a data tuple					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dystore/DystoreTupleImpl.java,v 1.3 2013-05-09 12:29:04 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DystoreTupleImpl.java,v $
 * Revision 1.3  2013-05-09 12:29:04  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.2  2009-09-21 19:34:28  spr
 * Fix size of long data.
 *
 * Revision 1.1  2009-09-19 00:14:00  spr
 * In memory tuple store for visualization.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dystore;



class DystoreTupleImpl implements DystoreTuple, DystoreConstants {


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String []	string_data;
private double []       double_data;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DystoreTupleImpl(int nobj,int ndbl)
{
   if (nobj == 0) string_data = null;
   else string_data = new String [nobj];
   if (ndbl == 0) double_data = null;
   else double_data = new double[ndbl];
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public String getValue(DystoreField fld)
{
   DystoreFieldImpl df = (DystoreFieldImpl) fld;

   if (df == null) return null;

   int oidx = df.getStringIndex();
   if (oidx >= 0) return string_data[oidx];
   oidx = df.getDoubleIndex();
   if (oidx >= 0) return Double.toString(double_data[oidx]);

   return null;
}



public double getTimeValue(DystoreField fld)
{
   DystoreFieldImpl df = (DystoreFieldImpl) fld;

   int oidx = df.getDoubleIndex();
   if (oidx < 0) return 0;

   return double_data[oidx];
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

void setValue(int idx,String v)
{
   if (v != null) v = v.intern();
   string_data[idx] = v;
}



void setValue(int idx,double v)
{
   double_data[idx] = v;
}


/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

public String toString()
{
   StringBuffer buf = new StringBuffer();
   buf.append("[");
   int ct = 0;
   if (string_data != null) {
      for (int i = 0; i < string_data.length; ++i) {
	 if (ct++ > 0) buf.append(",");
	 buf.append(string_data[i]);
       }
    }
   if (double_data != null) {
      for (int i = 0; i < double_data.length; ++i) {
	 if (ct++ > 0) buf.append(",");
	 buf.append(double_data[i]);
       }
    }
   buf.append("]");
   return buf.toString();
}



}	// end of class DystoreTupleImpl




/* end of DystoreTupleImpl.java */





