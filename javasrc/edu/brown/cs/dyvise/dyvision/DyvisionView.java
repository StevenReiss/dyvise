/********************************************************************************/
/*										*/
/*		DyvisionView.java						*/
/*										*/
/*	Viewer for a single process for DYMON visualization			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyvision/DyvisionView.java,v 1.5 2010-03-30 16:24:34 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyvisionView.java,v $
 * Revision 1.5  2010-03-30 16:24:34  spr
 * Clean up statistic display.
 *
 * Revision 1.4  2009-03-20 02:09:09  spr
 * Add memory view and related controls. Clean code.
 *
 * Revision 1.3  2008-11-24 23:38:15  spr
 * Move to dymaster.  Update views.
 *
 * Revision 1.2  2008-11-12 14:11:20  spr
 * Clean up the output and bug fixes.
 *
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dyvision;


import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.*;

import java.awt.Container;
import java.awt.Dimension;
import java.io.IOException;
import java.net.InetAddress;



class DyvisionView extends JFrame implements DyvisionConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private DyvisionMain	for_main;
private String		for_process;
private DyvisionSummary summary_pane;
private DyvisionDetail	detail_view;
private JComponent	detail_controller;
private int		divider_size;

private static boolean	use_split = true;


private int		split_x_delta;
private int		split_y_delta;





private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DyvisionView(DyvisionMain dm,String pid)
{
   for_main = dm;
   summary_pane = null;
   detail_view = null;
   detail_controller = new JLabel(" ");
   detail_controller.setVisible(false);

   int idx = pid.indexOf("@");
   if (idx < 0) {
      try {
	 String host = InetAddress.getLocalHost().getHostName();
	 pid = pid + "@" + host;
       }
      catch (IOException e) {
	 System.err.println("DYVISION: Can't find local host: " + e);
	 System.exit(3);
       }
    }
   for_process = pid;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getId()					{ return for_process; }



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

void start()
{
   SwingGridPanel pnl = new SwingGridPanel();
   pnl.setInsets(0);

   setDefaultCloseOperation(HIDE_ON_CLOSE);
   setTitle(for_process + " DYVISE Summary");

   summary_pane = new DyvisionSummary(for_main,for_process,this);
   pnl.addGBComponent(summary_pane,0,0,1,1,1,1);
   pnl.addGBComponent(detail_controller,2,0,1,1,1,1);

   JLabel dmy = new JLabel();
   pnl.addGBComponent(dmy,1,0,1,1,10,1);

   detail_view = new DyvisionDetail(for_main,this,for_process);

   if (use_split) {
      JSplitPane spl = new JSplitPane(JSplitPane.VERTICAL_SPLIT,pnl,detail_view);
      spl.setContinuousLayout(true);
      spl.setDividerLocation(1.0);
      spl.setOneTouchExpandable(true);
      spl.setResizeWeight(1);
      divider_size = spl.getDividerSize();
      spl.setDividerSize(0);
      setContentPane(spl);
    }
   else {
      pnl.addGBComponent(detail_view,0,1,3,1,100,100);
      setContentPane(pnl);
    }

   pack();
   setVisible(true);

   Dimension d1 = getContentPane().getSize();
   Dimension d2 = pnl.getSize();

   Dimension d5 = pnl.getPreferredSize();

   split_x_delta = d1.width - d2.width;
   split_y_delta = d1.height - d2.height;

   d5.width += split_x_delta;
   d5.height += split_y_delta;

   getContentPane().setSize(d5);
}




/********************************************************************************/
/*										*/
/*	Windowing methods							*/
/*										*/
/********************************************************************************/

void showView()
{
   if (summary_pane != null) summary_pane.setVisible(true);
   setVisible(true);
}



void showDetail(String id)
{
   if (detail_view != null && !detail_view.isShown()) {
      detail_view.showView(id);
      // detail_controller.setVisible(true);
      if (!use_split) {
	 Dimension d = summary_pane.getSize();
	 summary_pane.setMinimumSize(d);
       }
      else {
	 JSplitPane spl = (JSplitPane) getContentPane();
	 spl.setResizeWeight(0);
	 spl.setDividerSize(divider_size);
	 Dimension d = detail_view.getPreferredSize();
	 Dimension d1 = summary_pane.getSize();
	 Dimension d2 = detail_controller.getPreferredSize();
	 d.height += d1.height+divider_size+20;
	 if (d.width < d1.width+d2.width+20) d.width = d1.width+d2.width+20;
	 // System.err.println("SHOW DETAIL " + d + " " + d1);
	 summary_pane.setPreferredSize(d1);
	 spl.setSize(d);
	 spl.setPreferredSize(d);
       }

      updateSize();
    }
}



void hideDetail()
{
   if (detail_view != null && detail_view.isShown()) {
      Dimension d = summary_pane.getSize();
      detail_view.hideView();
      detail_controller.setVisible(false);
      if (!use_split) {
	 summary_pane.setMinimumSize(new Dimension(32,32));
       }
      else {
	 JSplitPane spl = (JSplitPane) getContentPane();
	 spl.setResizeWeight(0);
	 spl.setDividerSize(0);
	 d.height += divider_size;
	 spl.setPreferredSize(d);
       }

      updateSize();
    }
}



void updateSize()
{
   Container pnl = getContentPane();

   if (!detail_view.isShown()) {
      Dimension d1 = summary_pane.getPreferredSize();
      d1.width += split_x_delta;
      d1.height += split_y_delta;
      pnl.setPreferredSize(d1);
    }

   Dimension d = pnl.getPreferredSize();

   // System.err.println("UPDATE " + summary_pane.getPreferredSize() + " " + d + " " + pnl.getSize());

   pnl.setSize(d);
   pack();
}



/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

void handleUpdates(Element e)
{
   try {
      summary_pane.update(e);
      if (detail_view != null) detail_view.update(e);
    }
   catch (Throwable t) {
      System.err.println("DYVISION: Error processing update: " + t);
      t.printStackTrace();
      System.err.print("DYVISION: Update = ");
      if (e == null) System.err.println("null");
      else System.err.println(IvyXml.convertXmlToString(e));
    }
}



}	// end of class DyvisionView




/* end of DyvisionView.java */


