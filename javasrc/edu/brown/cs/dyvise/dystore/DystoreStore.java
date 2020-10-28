/********************************************************************************/
/*										*/
/*		DystoreStore.java						*/
/*										*/
/*	DYVISE description of a store for tuples				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dystore/DystoreStore.java,v 1.6 2013-05-09 12:29:04 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DystoreStore.java,v $
 * Revision 1.6  2013-05-09 12:29:04  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.5  2011-03-19 20:34:43  spr
 * Code speedup by adding range class.
 *
 * Revision 1.4  2011-03-10 02:33:19  spr
 * Code cleanup.
 *
 * Revision 1.3  2010-03-30 16:23:05  spr
 * Make the store efficient enough to use with the display.
 *
 * Revision 1.2  2009-10-07 01:00:20  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.1  2009-09-19 00:14:00  spr
 * In memory tuple store for visualization.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dystore;


import java.util.Collection;
import java.util.Map;



public interface DystoreStore extends DystoreConstants {


DystoreTable getTable();

DystoreTuple addTuple(Map<String,Object> values);
void updateTuple(DystoreTuple tup,Map<String,Object> values);



Collection<DystoreTuple> getAllTuples();
DystoreRangeSet getRange(double f,double t);

DystoreRangeSet nextRange(double f,double t,boolean prune,boolean first,DystoreRangeSet rng);

DystoreAccessor getAccessor(DystoreField df);

ValueRange getValueRange(DystoreField df);

Collection<String> getValueSet(DystoreField df);

void addValueListener(ValueCallback vc);

void removeValueListener(ValueCallback vc);

void clear(boolean values);

void setUseAbsoluteTime(boolean fg);

void noteValue(DystoreField f,String value);
void noteValue(DystoreField f,double value);


}	// end of interface DystoreStore




/* end of DystoreStore.java */


