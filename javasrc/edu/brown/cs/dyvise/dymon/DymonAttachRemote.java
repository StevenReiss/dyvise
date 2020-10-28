/********************************************************************************/
/*										*/
/*		DymonAttachRemote.java						*/
/*										*/
/*	Process to attach a Java MTI library to a remote java process		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymon/DymonAttachRemote.java,v 1.3 2012-10-05 00:52:56 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymonAttachRemote.java,v $
 * Revision 1.3  2012-10-05 00:52:56  spr
 * Code clean up.
 *
 * Revision 1.2  2010-03-30 16:22:05  spr
 * Clean up the code.
 *
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymon;


import edu.brown.cs.ivy.exec.IvyExecQuery;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.io.IOException;
import java.util.List;




public class DymonAttachRemote implements DymonConstants
{



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   DymonAttachRemote ar = new DymonAttachRemote(args);

   ar.process();

   return;
}




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

enum AttachType {
   NONE, AGENT, LIBRARY, PATH, QUERY
}


private String process_id;
private AttachType attach_type;
private String attach_name;
private String attach_args;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DymonAttachRemote(String [] args)
{
   process_id = null;
   attach_type = AttachType.NONE;
   attach_name = null;
   attach_args = null;

   scanArgs(args);
}




/********************************************************************************/
/*										*/
/*	Argument processing							*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   String proc = null;
   String forhost = null;

   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
	 if (args[i].startsWith("-p") && i+1 < args.length) {   // -p process@host
	    proc = args[++i];
	  }
	 else if (args[i].startsWith("-a")) {                   // -agent
	    attach_type = AttachType.AGENT;
	  }
	 else if (args[i].startsWith("-l")) {                   // -library
	    attach_type = AttachType.LIBRARY;
	  }
	 else if (args[i].startsWith("-P")) {                   // -Path
	    attach_type = AttachType.PATH;
	  }
	 else if (args[i].startsWith("-q")) {                   // -query
	    attach_type = AttachType.QUERY;
	  }
	 else if (args[i].startsWith("-h") && i+1 < args.length) {      // -host <hid>
	    forhost = args[++i];
	  }
	 else badArgs();
       }
      else if (attach_name == null) attach_name = args[i];
      else if (attach_args == null) attach_args = args[i];
      else badArgs();
    }

   if (attach_args == null) attach_args = DYMON_ATTACH_ARGS;
   else attach_args += ":" + DYMON_ATTACH_ARGS;

   if (attach_name == null && attach_type != AttachType.QUERY) badArgs();

   if (proc != null) {
      int idx = proc.indexOf("@");
      if (idx < 0) process_id = proc;
      else {
	 process_id = proc.substring(0,idx);
	 if (forhost == null) forhost = proc.substring(idx+1);
       }
    }

   if (process_id == null && attach_type != AttachType.QUERY) badArgs();

   if (attach_type == AttachType.NONE) {
      if (attach_name.endsWith(".jar")) attach_type = AttachType.AGENT;
      else if (attach_name.startsWith("/") ||
		  attach_name.startsWith("\\")) attach_type = AttachType.PATH;
      else attach_type = AttachType.LIBRARY;
    }

   if (forhost != null && !forhost.equals(IvyExecQuery.getHostName())) {
      System.err.println("DYMONATTACH: Illegal host name " + forhost + " :: " +
			    IvyExecQuery.getHostName());
      System.exit(1);
    }
}



private static void badArgs()
{
   System.err.println("DYMONATTACH: attach [-p proc] [-a|-l|-p] agent [args]");
   System.err.println("DYMONATTACH: attach -query");
   System.exit(2);
}




/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

private void process()
{
   if (attach_type == AttachType.QUERY) {
      handleQuery();
      return;
    }

   VirtualMachine vm = null;

   try {
      vm = VirtualMachine.attach(process_id);

      switch (attach_type) {
	 case NONE :
	    break;
	 case AGENT :
	    vm.loadAgent(attach_name,attach_args);
	    break;
	 case PATH :
	    vm.loadAgentPath(attach_name,attach_args);
	    break;
	 case LIBRARY :
	    vm.loadAgentLibrary(attach_name,attach_args);
	    break;
         default :
            break;
       }
    }
   catch (Exception e) {
      System.err.println("DYMONATTACH: Attach failure: " + e);
      System.exit(3);
    }
   finally {
      try {
	 if (vm != null) vm.detach();
       }
      catch (IOException e) { }
    }
}




/********************************************************************************/
/*										*/
/*	Query methods								*/
/*										*/
/********************************************************************************/

private void handleQuery()
{
   List<VirtualMachineDescriptor> active = VirtualMachine.list();
   String cid = IvyExecQuery.getProcessId();
   String host = IvyExecQuery.getHostName();

   for (VirtualMachineDescriptor vmd : active) {
      String pid = vmd.id();
      if (pid.equals(cid)) continue;
      String cmdargs = vmd.displayName();
      String procid = pid + "@" + host;
      System.out.println("ATTACH\t" + procid + "\t" + cmdargs);
    }
}



}	// end of class DymonAttachRemote




/* end of DymonAttachRemove.java */
