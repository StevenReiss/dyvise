/********************************************************************************/
/*										*/
/*		DygraphValue.java						*/
/*										*/
/*	DYVISE graphics (visualization) value holder for field computations	*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dygraph/DygraphValue.java,v 1.2 2013-05-09 12:28:56 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DygraphValue.java,v $
 * Revision 1.2  2013-05-09 12:28:56  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.1  2010-03-31 17:41:54  spr
 * Add selection mechanism.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dygraph;

import java.util.*;


class DygraphValue implements DygraphConstants {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private SelectorOp selector_op;

private double	result_value;
private int	num_values;
private Map<String,ModeValue> count_values;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DygraphValue(SelectorOp op)
{
   selector_op = op;

   reset();
}




/********************************************************************************/
/*										*/
/*	Initial computations							*/
/*										*/
/********************************************************************************/

void reset()
{
   result_value = 0;
   num_values = 0;
   count_values = null;

   if (selector_op == SelectorOp.MODE) {
      count_values = new HashMap<String,ModeValue>();
    }
}



/********************************************************************************/
/*										*/
/*	Intermediate computations						*/
/*										*/
/********************************************************************************/

void addValue(String key,double val)
{
   switch (selector_op) {
      default :
      case INTERVAL :
         result_value += val;
         break;
      case AVERAGE :
	 result_value += val;
	 break;
      case MAX :
	 if (num_values == 0 || val > result_value) result_value = val;
	 break;
      case MIN :
	 if (num_values == 0 || val < result_value) result_value = val;
	 break;
      case COUNT :
	 // TODO: we need the maximal count over all intervals to get this
	 break;
      case MODE :
	 if (key == null) {
	    result_value += val;
	  }
	 else {
	    ModeValue mv = count_values.get(key);
	    if (mv == null) {
	       mv = new ModeValue(val);
	       count_values.put(key,mv);
	     }
	    mv.count();
	  }
	 break;
    }

   ++num_values;
}




void addNullValue(String key)
{
   ++num_values;
}



/********************************************************************************/
/*										*/
/*	Final Computations							*/
/*										*/
/********************************************************************************/

double getValue(DygraphValueContext ctx)
{
   double v = result_value;

   switch (selector_op) {
      default :
      case AVERAGE :
	 if (num_values > 0) v /= num_values;
	 break;
      case INTERVAL :
	 double dt = ctx.getToTime() - ctx.getFromTime();
	 if (dt != 0) v /= dt;
         else v = 1;
	 break;
      case MAX :
      case MIN :
	 break;
      case COUNT :
	 v = num_values;
	 break;
      case MODE :
	 if (count_values.size() == 0 && num_values > 0) v /= num_values;
	 else {
	    int maxct = 0;
	    for (ModeValue mv : count_values.values()) {
	       if (mv.getCount() > maxct) {
		  v = mv.getValue();
		  maxct = mv.getCount();
	        }
	     }
	  }
	 break;
    }

   return v;
}




/********************************************************************************/
/*										*/
/*	Holder for information for computing the mode				*/
/*										*/
/********************************************************************************/

private static class ModeValue {

   private double key_value;
   private int num_items;

   ModeValue(double v) {
      key_value = v;
      num_items = 0;
    }

   void count() 			{ num_items++; }

   int getCount()			{ return num_items; }
   double getValue()			{ return key_value; }

}	// end of inner class ModeValue




}	// end of class DygraphValue




/* end of DygraphValue.java */
