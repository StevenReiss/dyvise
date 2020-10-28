/********************************************************************************/
/*										*/
/*		DymemFrame.java 						*/
/*										*/
/*	Main frame for memory visualizer					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymem/DymemFrame.java,v 1.3 2012-10-05 00:52:49 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymemFrame.java,v $
 * Revision 1.3  2012-10-05 00:52:49  spr
 * Code clean up.
 *
 * Revision 1.2  2010-03-30 16:21:56  spr
 * Bug fixes and feature enhancements.	Start of interface for cycle elimination.
 *
 * Revision 1.1  2009-10-07 01:40:01  spr
 * Add common frame class here rather than in Dyvision.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymem;


import edu.brown.cs.dyvise.dyvise.*;
import edu.brown.cs.dyvise.dymon.*;

import edu.brown.cs.ivy.swing.*;

import javax.swing.*;
import javax.swing.border.LineBorder;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;




public class DymemFrame extends JFrame implements DymemConstants, DyviseConstants,
		DymemConstants.ViewListener
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private String		for_process;
private DymonRemote	dymon_remote;

private DymemViewCommon memory_panel;

private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public DymemFrame(String pid,DymonRemote dr)
{
   for_process = pid;

   dymon_remote = dr;

   setupWindow();
}




/********************************************************************************/
/*										*/
/*	Window setup methods							*/
/*										*/
/********************************************************************************/

private void setupWindow()
{
   try {
      memory_panel = new DymemViewDelta(for_process);
    }
   catch (Exception e) {
      System.err.println("DYVISION: Problem creating memory panel: " + e);
      e.printStackTrace();
      return;
    }

   memory_panel.setBorder(new LineBorder(SwingColors.SWING_DARK_COLOR));
   memory_panel.addViewListener(this);

   setDefaultCloseOperation(HIDE_ON_CLOSE);
   setTitle(for_process + " DYVISE Memory View");

   MenuBar mb = new MenuBar();
   setJMenuBar(mb);

   JPanel pnl = new JPanel(new BorderLayout());
   setContentPane(pnl);

   ToolBar toolbar = new ToolBar();

   pnl.add(toolbar,BorderLayout.PAGE_START);
   pnl.add(memory_panel,BorderLayout.CENTER);

   DymemTimeLine timeline = setupTimeLine();
   timeline.setVisible(true);
   timeline.setBorder(new LineBorder(SwingColors.SWING_DARK_COLOR));

   pnl.add(timeline,BorderLayout.EAST);

   pack();
}



/********************************************************************************/
/*										*/
/*	View callbacks								*/
/*										*/
/********************************************************************************/

public void changeOrientation(DymemConstants.Orientation o)
{
   setupTimeLine();
}


private DymemTimeLine setupTimeLine()
{
   DymemConstants.TimeLineDirection dir;
   String where;

   switch (memory_panel.getOrientation()) {
      default :
      case HORIZONTAL :
	 dir = DymemConstants.TimeLineDirection.VERTICAL_UP;
	 where = BorderLayout.EAST;
	 break;
      case VERTICAL :
	 dir = DymemConstants.TimeLineDirection.HORIZONTAL;
	 where = BorderLayout.SOUTH;
	 break;
    }


   DymemTimeLine timeline = memory_panel.getTimeLine(dir);

   JPanel pnl = (JPanel) getContentPane();
   pnl.remove(timeline);
   pnl.add(timeline,where);
   timeline.setVisible(true);
   // timeline.setPreferredSize(sz);
   pnl.validate();
   pack();

   return timeline;
}



/********************************************************************************/
/*										*/
/*	Menu bar								*/
/*										*/
/********************************************************************************/

private class MenuBar extends SwingMenuBar {

   private static final long serialVersionUID = 1;


   MenuBar() {
      JMenu m = new JMenu("File");
      addButton(m,"Quit","Exit from Dyvision");
      add(m);

      m = new JMenu("Display");
      addButton(m,"Properties ...","Show display properties dialog");
      add(m);
    }

   public void actionPerformed(ActionEvent e) {
      String btn = e.getActionCommand();

      if (btn == null) ;
      else if (btn.equals("Quit")) {
	 System.exit(0);
       }
      else if (btn.equals("Properties ...")) {
	 memory_panel.showParameters();
       }
    }

}	// end of subclass MenuBar




/********************************************************************************/
/*										*/
/*	Tool bar								*/
/*										*/
/********************************************************************************/

private class ToolBar extends SwingToolBar implements ActionListener {

   private static final long serialVersionUID = 1;

   ToolBar() {
      addButton("BACK",DYVISE_ICON_DIRECTORY + "back.png","Move to previous display");
      addButton("FORWARD",DYVISE_ICON_DIRECTORY + "forward.png","Move to next display");
      addButton("HOME",DYVISE_ICON_DIRECTORY + "gohome.png","Go to default display");
      addButton("PARENT",DYVISE_ICON_DIRECTORY + "parent.png","Go to target parent display");
      if (dymon_remote != null) {
	 addButton("DUMP",DYVISE_ICON_DIRECTORY + "dump1.png","Generate a heap dump");
	 addButton("GC",DYVISE_ICON_DIRECTORY + "gc.png","Do a garbage collection");
       }
      addButton("MARK",DYVISE_ICON_DIRECTORY + "timemark.png","Insert a time mark");
      addButton("CYCLE",DYVISE_ICON_DIRECTORY + "redled.png","Manage cycles");
    }

   public void actionPerformed(ActionEvent e) {
      String cmd = e.getActionCommand();
      if (cmd.equals("BACK")) memory_panel.goBack();
      else if (cmd.equals("FORWARD")) memory_panel.goForward();
      else if (cmd.equals("HOME")) memory_panel.goHome();
      else if (cmd.equals("PARENT")) memory_panel.goParent();
      else if (cmd.equals("DUMP")) {
	 dymon_remote.dymonCommand("SHOWHEAP " + for_process);
       }
      else if (cmd.equals("GC")) {
	 System.err.println("Issue GC command");
	 dymon_remote.dymonCommand("GC " + for_process);
       }
      else if (cmd.equals("MARK")) {
	 String what = JOptionPane.showInputDialog(this,"Enter Mark Type","");
	 DyviseTimeManager dtm = DyviseTimeManager.getTimeManager(for_process);
	 dtm.createUserMark(0,what);
       }
      else if (cmd.equals("CYCLE")) {
	 memory_panel.manageCycle();
       }
      else System.err.println("PRESS " + cmd);
    }

}	// end of subclass ToolBar



}	// end of class DymemFrame




/* end of DymemFrame.java */

