/********************************************************************************/
/*										*/
/*		dyltilocklogger.C						*/
/*										*/
/*	JVMTI lock data processing for lock logging				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/dylti/src/dyltilocklogger.C,v 1.1 2011-09-12 18:29:46 spr Exp $ */


/*********************************************************************************
 *
 * $Log: dyltilocklogger.C,v $
 * Revision 1.1  2011-09-12 18:29:46  spr
 * Add dylti to cvs
 *
 *
 ********************************************************************************/

#include "dylti_local.H"




/********************************************************************************/
/*										*/
/*	DyltiLockLogger constructors/destructors				*/
/*										*/
/********************************************************************************/

DyltiLockLoggerInfo::DyltiLockLoggerInfo(DyltiMonitor dm,ostream& ost)
   : log_file(ost)
{
   using_monitor = dm;
   log_mutex.createThread();

   header();
}




DyltiLockLoggerInfo::~DyltiLockLoggerInfo()
{ }




/********************************************************************************/
/*										*/
/*	Event handling methods							*/
/*										*/
/********************************************************************************/

void
DyltiLockLoggerInfo::contendedEnter(jthread th,DyltiLocation& loc,LongLong now,DyltiLockData ld,int nest)
{
   entry("ENTER",th,loc,now,ld,nest);
}



void
DyltiLockLoggerInfo::contendedEntered(jthread th,DyltiLocation& loc,LongLong now,DyltiLockData ld,int nest)
{
   entry("ENTERED",th,loc,now,ld,nest);
}


void
DyltiLockLoggerInfo::monitorWait(jthread th,DyltiLocation& loc,LongLong now,DyltiLockData ld,int nest)
{
   entry("WAIT",th,loc,now,ld,nest);
}



void
DyltiLockLoggerInfo::monitorWaited(jthread th,DyltiLocation& loc,bool time,LongLong now,DyltiLockData ld,int nest)
{
   ConstText what = (time ? "WAITTIME" : "WAITED");

   entry(what,th,loc,now,ld,nest);
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void
DyltiLockLoggerInfo::header()
{
   log_mutex.lock();

   log_file << "WHAT|THREAD|METHOD|OFFSET|TIME|LOCK|CLASS|LEVEL" << endl;

   log_mutex.unlock();
}



void
DyltiLockLoggerInfo::entry(ConstText what,jthread th,DyltiLocation& loc,LongLong now,
			      DyltiLockData ld,int nest)
{
   CStdString lnm = ld->getMethodName(loc.getMethodId());
   if (lnm == "") return;               // why does this happen?

   log_mutex.lock();

   int tid = thread_map[th];
   if (tid <= 0) {
      tid = thread_map.size() + 1;
      thread_map[th] = tid;
    }

   log_file << what << "|" << tid << "|" << lnm << "|" << loc.getLocation() << "|" <<
      now << "|" << ld->getLockId() << "|" << ld->getLockClass() << "|" << nest << endl;

   log_mutex.unlock();
}





void
DyltiLockLoggerInfo::output()
{
   log_file.flush();
}



/* end of dyltilocklogger.C */
