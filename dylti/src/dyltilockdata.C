/********************************************************************************/
/*										*/
/*		dyltilockdata.C 						*/
/*										*/
/*	JVMTI lock data process for lock monitoring				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/dylti/src/dyltilockdata.C,v 1.1 2011-09-12 18:29:46 spr Exp $ */


/*********************************************************************************
 *
 * $Log: dyltilockdata.C,v $
 * Revision 1.1  2011-09-12 18:29:46  spr
 * Add dylti to cvs
 *
 *
 ********************************************************************************/

#include "dylti_local.H"




/********************************************************************************/
/*										*/
/*	DyltiLockData constructors/destructors					*/
/*										*/
/********************************************************************************/

DyltiLockDataInfo::DyltiLockDataInfo(DyltiMonitor dm,const char * cnm,int id)
{
   using_monitor = dm;
   wait_count = 0;
   enter_count = 0;
   timed_count = 0;
   waited_count = 0;
   lock_mutex.createThread();
   lock_class = cnm;
   lock_id = id;
   is_unnested = false;
   prior_items = NULL;
}




DyltiLockDataInfo::~DyltiLockDataInfo()
{ }




/********************************************************************************/
/*										*/
/*	Event handling methods							*/
/*										*/
/********************************************************************************/

void
DyltiLockDataInfo::contendedEnter(jthread th,DyltiLocation& loc)
{
   addLocation(loc);
   ++enter_count;
}



void
DyltiLockDataInfo::contendedEntered(jthread th,DyltiLocation& loc)
{ }



void
DyltiLockDataInfo::monitorWait(jthread th,DyltiLocation& loc)
{
   addLocation(loc);
   ++wait_count;
}



void
DyltiLockDataInfo::monitorWaited(jthread th,DyltiLocation& loc,bool timed)
{
   if (timed) ++timed_count;
   else ++waited_count;
}


/********************************************************************************/
/*										*/
/*	Nest handling methods							*/
/*										*/
/********************************************************************************/

void
DyltiLockDataInfo::addPriorLock(DyltiLockData ld)
{
   if (prior_items == NULL || prior_items->count(ld) == 0) {
      lock_mutex.lock();
      if (prior_items == NULL) prior_items = new DyltiLockSet;
      prior_items->insert(ld);
      lock_mutex.unlock();
    }
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void
DyltiLockDataInfo::outputXml(DyltiMonitor dm,IvyXmlWriter& xw)
{
   xw.begin("LOCK");
   xw.field("CLASS",lock_class);
   xw.field("WAIT",wait_count);
   xw.field("ENTER",enter_count);
   xw.field("TIMED",timed_count);
   xw.field("WAITED",waited_count);
   xw.field("ID",lock_id);
   if (is_unnested) xw.field("UNNEST",true);

   for (DyltiLocationListIter it = location_set.begin(); it != location_set.end(); ++it) {
      DyltiLocation loc = *it;
      StdString txt = method_map[loc.getMethodId()];
      xw.begin("LOCATION");
      xw.field("METHOD",txt);
      xw.field("OFFSET",loc.getLocation());
      xw.end();
    }

   if (prior_items != NULL) {
      for (DyltiLockSetIter it = prior_items->begin(); it != prior_items->end(); ++it) {
	 DyltiLockData ld = *it;
	 xw.begin("PRIOR");
	 xw.field("LOCK",ld->getLockId());
	 xw.end();
       }
    }

   xw.end();
}



/********************************************************************************/
/*										*/
/*	Location management							*/
/*										*/
/********************************************************************************/

void
DyltiLockDataInfo::addLocation(DyltiLocation& loc)
{
   if (loc.getMethodId() == 0) return;

   for (DyltiLocationListIter it = location_set.begin(); it != location_set.end(); ++it) {
      if ((*it).equals(loc)) return;
    }

   lock_mutex.lock();

   for (DyltiLocationListIter it = location_set.begin(); it != location_set.end(); ++it) {
      if ((*it).equals(loc)) {
	 lock_mutex.unlock();
	 return;
       }
    }

   CStdString str = using_monitor->getLocationText(loc.getMethodId());
   method_map[loc.getMethodId()] = str;

   // cerr << "ADD LOCATION " << this << " " << loc.getMethodId() << " " << loc.getLocation() << " " << str << endl;

   location_set.pushBack(loc);

   lock_mutex.unlock();
}




/* end of dyltilockdata.C */
