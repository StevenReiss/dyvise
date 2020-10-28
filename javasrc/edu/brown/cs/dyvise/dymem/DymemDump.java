/********************************************************************************/
/*										*/
/*		DymemDump.java							*/
/*										*/
/*	Program to generate a compact memory dump				*/
/*										*/
/********************************************************************************/
/*	Copyright 2005 Brown University -- Steven P. Reiss			*/
/*	Copyright 1997-2008 Sun Microsystems, Inc.  All rights reserved.	*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymem/DymemDump.java,v 1.2 2009-10-07 01:00:02 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymemDump.java,v $
 * Revision 1.2  2009-10-07 01:00:02  spr
 * Code cleanup for eclipse. Add support for standalone version.
 *
 * Revision 1.1  2009-09-19 00:09:27  spr
 * Update dymem with some bug fixes; initial support for reading dump files.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymem;




import com.sun.tools.attach.VirtualMachine;

import java.lang.reflect.Method;

import java.io.*;


public class DymemDump {




/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   DymemDump dd = new DymemDump(args);

   dd.process();
}




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		process_id;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private DymemDump(String [] args)
{
   process_id = null;
   scanArgs(args);
}




/********************************************************************************/
/*										*/
/*	Argument routines							*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
	 badArgs();
       }
      else if (process_id == null) process_id = args[i];
      else badArgs();
    }

   if (process_id == null) badArgs();
}



private void badArgs()
{
   System.err.println("dymemdump <process>");
}



/********************************************************************************/
/*										*/
/*	Processing routines							*/
/*										*/
/********************************************************************************/

private void process()
{
   if (process_id.matches("[0-9]+")) processLocalHprofDump();
   // else processRemote();
}



private void processLocalHprofDump()
{
   String file = "/pro/dyvise/dymem/src/dymem.dump";

   VirtualMachine vm = attach(process_id);
   // HotSpotVirtualMachine hvm = (HotSpotVirtualMachine) vm;

   try {
      file = new File(file).getCanonicalPath();
      InputStream in = null;
      try {
         Class<?> c = vm.getClass();
         Object [] args = new Object [] { file, "-live" };
         Method m = c.getDeclaredMethod("dumpHeap",args.getClass());
         in = (InputStream) m.invoke(vm,args);
       }
      catch (Throwable t) {
         System.err.println("DYMEMDUMP: Problem generating heap dump: " + t);
         throw new IOException("heap dump not generated");
       }
      byte buf [] = new byte[256];
      int n;
      for ( ; ; ) {
	 n = in.read(buf);
	 if (n <= 0) break;
	 String s = new String(buf,0,n,"UTF-8");
	 System.out.print(s);
       }
      in.close();
    }
   catch (IOException e) {
      System.err.println("DYMEMDUMP: Problem with load dump: " + e);
    }

   try {
      vm.detach();
    }
   catch (IOException e) { }
}



private VirtualMachine attach(String pid)
{
   try {
      return VirtualMachine.attach(pid);
    }
   catch (Throwable t) {
      System.err.println("DYMEMDUMP: Problem attaching to process " + pid + ": " + t);
      System.exit(1);
      return null;
    }
}



}	// end of class DymemDump




/* end of DymemDump.java */
