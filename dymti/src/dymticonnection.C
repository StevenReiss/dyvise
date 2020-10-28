/********************************************************************************/
/*										*/
/*		dymticonnection.C						*/
/*										*/
/*	Code to connect the monitor to DYPER					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/dymti/src/dymticonnection.C,v 1.4 2016/11/02 18:59:06 spr Exp $ */


/*********************************************************************************
 *
 * $Log: dymticonnection.C,v $
 * Revision 1.4  2016/11/02 18:59:06  spr
 * Move to asm5
 *
 * Revision 1.3  2009-04-11 23:45:16  spr
 * Clean up monitoring.
 *
 * Revision 1.2  2009-03-20 02:05:20  spr
 * Add memory reference dumping.
 *
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/

#include "dymti_local.H"



/********************************************************************************/
/*										*/
/*	Constructors/destructors						*/
/*										*/
/********************************************************************************/

DymtiConnectionInfo::DymtiConnectionInfo(DymtiMain vm)
{
   for_main = vm;
   mince_control = NULL;

   CStdString mince = vm->getOption("MID");
   system_id = vm->getOption("PID");

   mince_control = MinceThreadControlInfo::create(mince,MINCE_SYNC_REPLIES);

   StringBuffer buf;
   buf << "<DYMTI PID='" << system_id << "' COMMAND='_VAR_0'><_VAR_1/></DYMTI>";

   if (mince_control != NULL) mince_control->addPattern(buf.getString(),this);
}



DymtiConnectionInfo::~DymtiConnectionInfo()
{ }



/********************************************************************************/
/*										*/
/*	Incoming message routines						*/
/*										*/
/********************************************************************************/

void
DymtiConnectionInfo::receive(MinceMessage origmsg,MinceArguments args)
{
   StdString cmd = args->getArgument(0);
   IvyXmlNode msg = args->getXmlArgument(1);

   cerr << "DYMTI: Command " << cmd << endl;

   if (cmd == "PING") {
      origmsg->replyTo("<PONG/>");
    }
   else if (cmd == "OBJDUMP") {
      IvyXmlStringWriter xw;
      for_main->buildMemoryModel(xw);
      origmsg->replyTo(xw.str());
    }
   else if (cmd == "ALLOCMON") {
      for_main->allocMonitor(msg);
      origmsg->replyTo();
    }
   else if (cmd == "DUMPMEMORY") {
      IvyXmlFileWriter xw("memory.out");
      for_main->buildMemoryModel(xw);
      xw.close();
      origmsg->replyTo("DYMTIOK");
    }
   else if (cmd == "FINDPROC") {
      StdString s = for_main->findProcess();
      origmsg->replyTo(s);
    }
   else if (cmd == "ATTACH") {
      origmsg->replyTo();
    }
   else if (cmd == "SET") {
      if (msg.isElement() && msg.getNodeName() == "PARAM") {
	 StdString nm = msg.getAttrString("NAME");
	 StdString val = msg.getAttrString("VALUE");
	 for_main->setParameter(nm,val);
       }
      origmsg->replyTo();
    }
   else {
      cerr << "DYMTI: Unknown message " << msg.getNodeName() << endl;
      origmsg->replyTo();
    }
}



/* end of dymticonnection.C */
