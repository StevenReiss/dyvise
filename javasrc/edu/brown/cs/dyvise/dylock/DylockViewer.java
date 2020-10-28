/********************************************************************************/
/*										*/
/*		DylockViewer.java						*/
/*										*/
/*	DYVISE lock analysis lock viewer main program				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylock/DylockViewer.java,v 1.3 2013-05-09 12:29:01 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylockViewer.java,v $
 * Revision 1.3  2013-05-09 12:29:01  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.2  2011-04-01 23:09:02  spr
 * Bug clean up.
 *
 * Revision 1.1  2011-03-10 02:25:00  spr
 * New version of dylock to produce visualizations.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dylock;

import edu.brown.cs.ivy.swing.*;

import java.io.*;
import javax.swing.*;



public class DylockViewer extends DylockLockManager 
        implements DylockConstants, DylockConstants.DylockExec
{



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   DylockViewer dv = new DylockViewer(args);
   dv.process();
}



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private File				lock_file;
private JFrame				graph_frame;
private JFrame                          usage_frame;
private File				output_file;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DylockViewer(String [] args)
{
   lock_file = null;
   graph_frame = null;
   usage_frame = null;

   new SwingSetup();

   ToolTipManager ttm = ToolTipManager.sharedInstance();
   ttm.setDismissDelay(60*60*1000);
   ttm.setLightWeightPopupEnabled(false);

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
	 if (args[i].startsWith("-v")) ;                                // -view
	 else if (args[i].startsWith("-d") && i+1 < args.length) {      // -d <lock data file>
	    lock_file = new File(args[++i]);
	  }
	 else if (args[i].startsWith("-i") && i+1 < args.length) {      // -i <input>
	    lock_file = new File(args[++i] + ".out");
	  }
	 else if (args[i].startsWith("-o") && i+1 < args.length) {      // -o <output>
	    output_file = new File(args[++i]);
	  }
	 else badArgs();
       }
      else {
	 if (lock_file == null) lock_file = new File(args[i]);
	 else badArgs();
       }
    }

   if (lock_file == null) badArgs();
   if (output_file == null) {
      String fnm = lock_file.getPath();
      int idx = fnm.lastIndexOf(".");
      if (idx >= 0) output_file = new File(fnm.substring(0,idx) + ".view");
      else output_file = new File(fnm + ".view");
    }

}




private void badArgs()
{
   System.err.println("DYLOCK: dylockview -d <lock file>");
   System.exit(1);
}



/********************************************************************************/
/*										*/
/*	Main processing methods 						*/
/*										*/
/********************************************************************************/

@Override public void process()
{
   readLockData(lock_file);
   createWindow();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

File getOutputFile()			{ return output_file; }



/********************************************************************************/
/*										*/
/*	Graphics methods							*/
/*										*/
/********************************************************************************/

private void createWindow()
{
   JFrame frm = new JFrame();
   frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

   DylockViewPanel pnl = new DylockViewPanel(this,getAllLocks());
   frm.setContentPane(pnl);

   frm.pack();
   frm.setVisible(true);
}



void createGraphPanel()
{
   if (graph_frame == null) {
      graph_frame = new JFrame();
      DylockGraphPanel pnl = new DylockGraphPanel(this,getAllLocks());
      graph_frame.setTitle("Lock Dependencies");
      graph_frame.setContentPane(pnl);
      graph_frame.pack();
    }

   graph_frame.setVisible(true);
}


void createUsageGraph() 
{
   if (usage_frame == null) {
      usage_frame = new JFrame();
      DylockClassGraph pnl = new DylockClassGraph(getAllLocks());
      usage_frame.setTitle("Class-Lock Usage");
      usage_frame.setContentPane(pnl);
      usage_frame.pack();
    }
   
   usage_frame.setVisible(true);
}





}	// end of class DylockViewer




/* end of DylockViewer.java */
