/********************************************************************************/
/*										*/
/*		DystoreFilterImpl.java						*/
/*										*/
/*	Implementation of a field filter for DYVISE				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dystore/DystoreFilterImpl.java,v 1.3 2013-05-09 12:29:03 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DystoreFilterImpl.java,v $
 * Revision 1.3  2013-05-09 12:29:03  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.2  2011-03-10 02:33:19  spr
 * Code cleanup.
 *
 * Revision 1.1  2010-03-30 21:29:08  spr
 * Add the notion of filters and separate the data map to its own class.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dystore;

import edu.brown.cs.dyvise.dygraph.DygraphConstants;
import edu.brown.cs.dyvise.dyvise.*;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.util.regex.*;



abstract class DystoreFilterImpl implements DystoreFilter, DygraphConstants {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected DystoreFilterImpl(DystoreField fld)
{
}


/********************************************************************************/
/*										*/
/*	Creation methods							*/
/*										*/
/********************************************************************************/

static DystoreFilter createPatternFilter(DystoreField fld,String pat)
	throws DyviseException
{
   return new PatternFilter(fld,pat,0,-1);
}


static DystoreFilter createPatternFilter(DystoreField fld,String pat,int fgs)
	throws DyviseException
{
   return new PatternFilter(fld,pat,fgs,-1);
}


static DystoreFilter createPatternMapper(DystoreField fld,String pat,int grp)
	throws DyviseException
{
   return new PatternFilter(fld,pat,0,grp);
}


static DystoreFilter createPatternMapper(DystoreField fld,String pat,int fgs,int grp)
	throws DyviseException
{
   return new PatternFilter(fld,pat,fgs,grp);
}


static DystoreFilter createRangeFilter(DystoreField fld,double min,double max)
	throws DyviseException
{
   return new RangeFilter(fld,min,max);
}




/********************************************************************************/
/*										*/
/*	Default filters 							*/
/*										*/
/********************************************************************************/

public String filter(String v)			{ return v; }

public boolean filter(double v)	        { return true; }



/********************************************************************************/
/*										*/
/*	Pattern-based filters							*/
/*										*/
/********************************************************************************/

private static class PatternFilter extends DystoreFilterImpl {

   private Pattern match_pattern;
   private int match_group;

   PatternFilter(DystoreField df,String pat,int flgs,int grp) throws DyviseException {
      super(df);
      if (!df.isStringField())
	 throw new DyviseException("Pattern Filters only apply to string fields");
      try {
	 match_pattern = Pattern.compile(pat,flgs);
       }
      catch (Exception e) {
	 throw new DyviseException("Illegal pattern or flags",e);
       }
      match_group = grp;
    }

   public String filter(String v) {
      Matcher m = match_pattern.matcher(v);
      if (!m.find()) return null;
      if (match_group < 0) return v;
      return m.group(match_group);
    }

   public void output(IvyXmlWriter xw) {
      xw.begin("FILTER");
      xw.field("TYPE","PATTERN");
      xw.field("GROUP",match_group);
      xw.field("FLAGS",match_pattern.flags());
      xw.textElement("PATTERN",match_pattern.toString());
      xw.end("FILTER");
    }

}	// end of inner class PatternFilter



/********************************************************************************/
/*										*/
/*	Range filters								*/
/*										*/
/********************************************************************************/

private static class RangeFilter extends DystoreFilterImpl {

   private double min_value;
   private double max_value;

   RangeFilter(DystoreField df,double minv,double maxv) throws DyviseException {
      super(df);
      if (!df.isDoubleField())
         throw new DyviseException("Range Filters only apply to numeric fields");
      min_value = minv;
      max_value = maxv;
    }

   public boolean filter(double v) {
      if (v < min_value || v > max_value) return false;
      return true;
    }

   public void output(IvyXmlWriter xw) {
      xw.begin("FILTER");
      xw.field("TYPE","RANGE");
      xw.field("MIN",min_value);
      xw.field("MAX",max_value);
      xw.end("FILTER");
    }

}	// end of subclass RangeFilter



}	// end of class DystoreFilterImpl




/* end of DystoreFilterImpl.java */


