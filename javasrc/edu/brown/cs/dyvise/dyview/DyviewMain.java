/********************************************************************************/
/*										*/
/*		DyviewMain.java 						*/
/*										*/
/*	DYname VIEW visualization system main program				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyview/DyviewMain.java,v 1.3 2010-03-30 16:23:25 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyviewMain.java,v $
 * Revision 1.3  2010-03-30 16:23:25  spr
 * Upgrades to the visualizer to handle new programs and provide better views.
 *
 * Revision 1.2  2009-10-07 01:00:21  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.1  2009-09-19 00:14:23  spr
 * Viewer with dialog based front end.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dyview;


import edu.brown.cs.dyvise.dyvise.DyviseConstants;
import edu.brown.cs.dyvise.dyvise.DyviseException;
import edu.brown.cs.ivy.swing.SwingSetup;

import java.io.File;


public class DyviewMain implements DyviewConstants, DyviseConstants
{




/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   DyviewMain dm = new DyviewMain(args);

   dm.process();
}




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private DyviewModel	view_model;
private boolean 	show_control;
private String		dyview_file;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private DyviewMain(String [] args)
{
   new SwingSetup();

   view_model = new DyviewModel();
   show_control = true;
   dyview_file = null;

   scanArgs(args);
}



/********************************************************************************/
/*										*/
/*	Argument scanning methods						*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
	 if (args[i].startsWith("-d") && i+1 < args.length) {           // -d <project directory>
	    view_model.setProjectDirectory(args[++i]);
	  }
	 else if (args[i].startsWith("-p") && i+1 < args.length) {      // -p <project>
	    String pnm = args[++i];
	    try {
	       view_model.setProject(pnm);
	     }
	    catch (DyviseException e) {
	       System.err.println("DYVIEW: project " + pnm + " does not exist");
	       System.exit(2);
	     }
	  }
	 else if (args[i].startsWith("-s") && i+1 < args.length) {      // -s <start class>
	    view_model.setStartClass(args[++i]);
	  }
	 else if (args[i].startsWith("-v") && i+1 < args.length) {      // -visual <name>
	    DyviewVisual dv = view_model.setVisual(args[++i]);
	    if (dv == null) {
	       System.err.println("DYVIEW: invalid visual name " + args[i]);
	     }
	  }
	 else badArgs();
       }
      else if (dyview_file == null) {
	 dyview_file = args[i];
	 try {
	    view_model.loadFrom(new File(dyview_file));
	  }
	 catch (DyviseException e) {
	    System.err.println("DYVIEW: invalid dyview file " + dyview_file);
	    System.exit(3);
	  }
       }
      else badArgs();
    }
}




private void badArgs()
{
   System.err.println("DYVIEW: dyview [-d projdir] [-p project] [-s startclass] [-v visual] [ input_file ]");
   System.exit(1);
}




/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

private void process()
{
   if (show_control) {
      DyviewWindow dw = new DyviewWindow(view_model);
      dw.setVisible(true);
    }
   else {
      // if ready, then do visualization, else print error
    }
}



}	// end of class DyviewMain



/* end of DyviewMain.java */
