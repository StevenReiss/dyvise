/********************************************************************************/
/*										*/
/*		DymemConstants.java						*/
/*										*/
/*	Constants for memory display and analysis				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymem/DymemConstants.java,v 1.4 2009-10-07 01:00:02 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymemConstants.java,v $
 * Revision 1.4  2009-10-07 01:00:02  spr
 * Code cleanup for eclipse. Add support for standalone version.
 *
 * Revision 1.3  2009-06-04 18:53:35  spr
 * Clean up memory display.
 *
 * Revision 1.2  2009-04-11 23:45:29  spr
 * Provide updated visualizations with time line, hashing, etc.
 *
 * Revision 1.1  2009-03-20 02:10:12  spr
 * Add memory graph computation and display.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymem;


import edu.brown.cs.dyvise.dymon.DymonConstants;
import edu.brown.cs.dyvise.dyvise.DyviseConstants;

import org.w3c.dom.Element;

import java.util.List;


public interface DymemConstants extends DymonConstants, DyviseConstants {



/********************************************************************************/
/*										*/
/*	File locations								*/
/*										*/
/********************************************************************************/

String DYMEM_HEAP_PREFIX = DYMON_HEAP_PREFIX;



/********************************************************************************/
/*										*/
/*	Timing constants							*/
/*										*/
/********************************************************************************/

int DYMEM_CHECK_EVERY = 2000;
int DYMEM_UPDATE_TIME = 2000;



/********************************************************************************/
/*										*/
/*	Comparison constants							*/
/*										*/
/********************************************************************************/

enum OutputCompareBy {
   NONE,
   LOCAL_SIZE,
   TOTAL_SIZE,
   LOCAL_COUNT,
   TOTAL_COUNT,
   LOCAL_NEW,
   TOTAL_NEW
}


enum Orientation {
   HORIZONTAL,
   VERTICAL
};



/********************************************************************************/
/*										*/
/*	Names									*/
/*										*/
/********************************************************************************/

String ROOT_NAME = "*ROOT*";
String SYSTEM_NAME = "*SYSTEM*";
String CLASS_PREFIX = "CLASS*";
String THREAD_PREFIX = "THREAD*";
String FINALIZER_NAME = "Ljava/lang/ref/Finalizer;";




/********************************************************************************/
/*										*/
/*	Interface for a Memory Element						*/
/*										*/
/********************************************************************************/

interface GraphNode {

   String getName();
   String getCycleName();
   int getIndex();
   long getLocalSize();
   long getLocalCount();
   long getLocalNewCount();
   long getTotalSize();
   long getTotalCount();
   long getTotalNewCount();
   boolean isCycle();
   boolean isSelfCycle();
   long getOutRefs();
   long getInRefs();
   long getValue(OutputCompareBy cb);

   List<GraphLink> getSortedInLinks(OutputCompareBy cb);
   List<GraphLink> getSortedOutLinks(OutputCompareBy cb);

}	// end of subinterface GraphNode


interface GraphLink {

   long getNumRefs();
   double getRefPercent();
   double getSizePercent();
   GraphNode getFromNode();
   GraphNode getToNode();

}	// end of subinterface GraphLink



/********************************************************************************/
/*										*/
/*	Interface for parameter change callbacks				*/
/*										*/
/********************************************************************************/

interface ParameterListener {

   void valuesChanged();

}	// end of subinterface ParameterListener


/********************************************************************************/
/*										*/
/*	Interface for abstract items						*/
/*										*/
/********************************************************************************/

interface GraphItem {

   String getName();
   long getSize();

}	// end of subinterface GraphItem



/********************************************************************************/
/*										*/
/*	Interface for heap update callbacks					*/
/*										*/
/********************************************************************************/

interface HeapListener {

   void heapUpdated(long when);

   void heapDumpTime(long when);
   void memoryUsageUpdate(Element e);

}	// end of subinterface HeapUpdater



/********************************************************************************/
/*										*/
/*	Interface for visualization change callbacks				*/
/*										*/
/********************************************************************************/

public interface ViewListener {

   void changeOrientation(Orientation o);

}	// end of subinterface ViewListener



/********************************************************************************/
/*										*/
/*	Time line definitions							*/
/*										*/
/********************************************************************************/

enum TimeLineDirection {
   VERTICAL_UP,
   VERTICAL_DOWN,
   HORIZONTAL
}


/********************************************************************************/
/*										*/
/*	Dymon information							*/
/*										*/
/********************************************************************************/

String DYMEM_PATCH_AGENT = "HEAP";
double DYMEM_PATCH_OVERHEAD = 0.005;



}	// end of interface DymemConstants




/* end of DymemConstants.java */
