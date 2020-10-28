/********************************************************************************/
/*										*/
/*		DylockCheckerBase.java						*/
/*										*/
/*	DYVISE lock analysis generic checker					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylock/DylockCheckerBase.java,v 1.2 2012-10-05 00:52:40 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylockCheckerBase.java,v $
 * Revision 1.2  2012-10-05 00:52:40  spr
 * Update lock analysis.  Prepare for pattern display.
 *
 * Revision 1.1  2011-03-10 02:24:59  spr
 * New version of dylock to produce visualizations.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dylock;

import java.util.*;


abstract class DylockCheckerBase implements DylockConstants, DylockConstants.DylockChecker
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

protected static int	  item_counter;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected DylockCheckerBase()
{ }



/********************************************************************************/
/*										*/
/*	Helper methods								*/
/*										*/
/********************************************************************************/

public static <T> Collection<Collection<T>> powerSet(Collection<T> original)
{
   Collection<Collection<T>> sets = new HashSet<Collection<T>>();
   if (original.isEmpty()) {
      sets.add(new HashSet<T>());
      return sets;
    }

   List<T> list = new ArrayList<T>(original);
   T head = list.get(0);
   Collection<T> rest = new HashSet<T>(list.subList(1, list.size()));
   for (Collection<T> set : powerSet(rest)) {
      Collection<T> newSet = new HashSet<T>();
      newSet.add(head);
      newSet.addAll(set);
      sets.add(newSet);
      sets.add(set);
    }

   return sets;
}




}	//	end of abstract class DylockCheckerBase




/* end of DylockCheckerBase.java */
