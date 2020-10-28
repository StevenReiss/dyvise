/********************************************************************************/
/*										*/
/*		DpinMain.java							*/
/*										*/
/*	Main program for dyper programatic interface				*/
/*										*/
/********************************************************************************/
/*	Copyright 2007 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2007, Brown University, Providence, RI.				 *
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dpin/DpinMain.java,v 1.2 2010-03-30 16:20:23 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DpinMain.java,v $
 * Revision 1.2  2010-03-30 16:20:23  spr
 * Change mint sync mode.
 *
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dpin;


import edu.brown.cs.ivy.mint.*;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.util.*;


public class DpinMain implements DpinConstants, MintConstants {



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   DpinMain dm = new DpinMain(args);
   dm.process();
}





/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<String,DpinProcess> process_set;
private MintControl mint_control;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private DpinMain(String [] args)
{
   scanArgs(args);

   process_set = new HashMap<String,DpinProcess>();
   mint_control = MintControl.create(DYPER_MESSAGE_BUS,MintSyncMode.MULTIPLE);

   mint_control.register("<DYPER START='_VAR_0' />",new StartHandler());
   mint_control.register("<DYPER END='_VAR_0' />",new EndHandler());

   WhoHandler wh = new WhoHandler();
   sendMessage(null,"WHO",wh,MINT_MSG_ALL_REPLIES);
   List<String> pl = wh.waitForDone();

   for (String id : pl) {
      DpinProcess dp = new DpinProcess(id,this);
      addProcess(dp);
    }

   dumpProcesses();
}




/********************************************************************************/
/*										*/
/*	Argument processing							*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
	 badArgs();
       }
      else badArgs();
    }
}



private void badArgs()
{
   System.err.println("DPIN: dpin");
   System.exit(1);
}




/********************************************************************************/
/*										*/
/*	Main processing loop							*/
/*										*/
/********************************************************************************/

private synchronized void process()
{
   boolean waiter = process_set.isEmpty();

   while (waiter || process_set.size() > 0) {
      try {
	 wait(0);
       }
      catch (InterruptedException e) { }
      if (process_set.size() > 0) waiter = false;
      dumpProcesses();
    }
}




/********************************************************************************/
/*										*/
/*	Process set maintenance 						*/
/*										*/
/********************************************************************************/

synchronized void addProcess(DpinProcess dp)
{
   if (dp != null) process_set.put(dp.getId(),dp);
   notifyAll();
}



synchronized void removeProcess(DpinProcess dp)
{
   if (dp != null) process_set.remove(dp.getId());
   notifyAll();
}


synchronized DpinProcess getProcess(String id)
{
   return process_set.get(id);
}



/********************************************************************************/
/*										*/
/*	Messaing methods							*/
/*										*/
/********************************************************************************/

void sendMessage(String pid,String cmd,MintReply mr,int flags)
{
   sendMessage(pid,cmd,null,mr,flags);
}


void sendMessage(String pid,String cmd,String cnts,MintReply mr,int flags)
{
   StringBuffer buf = new StringBuffer();
   buf.append("<DYPER");
   if (pid != null) {
      buf.append(" PID='");
      buf.append(pid);
      buf.append("'");
    }
   buf.append(" COMMAND='");
   buf.append(cmd);
   buf.append("'");
   buf.append(">");
   if (cnts != null) buf.append(cnts);
   buf.append("</DYPER>");

   mint_control.send(buf.toString(),mr,flags);
}



/********************************************************************************/
/*										*/
/*	Handler for currently running processes 				*/
/*										*/
/********************************************************************************/

private static class WhoHandler implements MintReply {

   private boolean all_done;
   private List<String> id_list;

   WhoHandler() {
      all_done = false;
      id_list = new ArrayList<String>();
    }

   public synchronized void handleReply(MintMessage msg,MintMessage reply) {
      if (reply == null) return;
      Element e = reply.getXml();
      if (e != null) {
	 String id = IvyXml.getAttrString(e,"ID");
	 id_list.add(id);
       }
    }

   public synchronized void handleReplyDone(MintMessage msg) {
      all_done = true;
      notifyAll();
    }

   synchronized List<String> waitForDone() {
      while (!all_done) {
	 try {
	    wait();
	  }
	 catch (InterruptedException e) { }
       }

      return id_list;
    }

}	// end of subclass WhoHandler



/********************************************************************************/
/*										*/
/*	Handler for new processes						*/
/*										*/
/********************************************************************************/

private class StartHandler implements MintHandler {

   public void receive(MintMessage msg,MintArguments args) {
      String id = args.getArgument(0);
      DpinProcess dp = new DpinProcess(id,DpinMain.this);
      addProcess(dp);
      msg.replyTo();
    }

}	// end of subclass StartHandler



private class EndHandler implements MintHandler {

   public void receive(MintMessage msg,MintArguments args) {
      String id = args.getArgument(0);
      DpinProcess dp = getProcess(id);
      removeProcess(dp);
      msg.replyTo();
    }

}	// end of subclass StartHandler



/********************************************************************************/
/*										*/
/*	Debugging methods							*/
/*										*/
/********************************************************************************/

private void dumpProcesses()
{
   System.err.println("\n\nDumping Processes");
   for (DpinProcess dp : process_set.values()) {
      dp.dump();
    }
}




}	// end of class DpinMain




/* end of DpinMain.java */
