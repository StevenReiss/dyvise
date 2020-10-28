/********************************************************************************/
/*										*/
/*		DyviewAbstractPanel.java					*/
/*										*/
/*	DYname VIEW control panel for setting up visualization			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyview/DyviewAbstractPanel.java,v 1.3 2009-10-07 22:39:51 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyviewAbstractPanel.java,v $
 * Revision 1.3  2009-10-07 22:39:51  spr
 * Eclipse code cleanup.
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
import edu.brown.cs.ivy.swing.SwingGridPanel;

import javax.swing.JLabel;
import javax.swing.JTextArea;

import java.awt.Color;
import java.awt.Font;
import java.util.Collection;


abstract class DyviewAbstractPanel extends SwingGridPanel implements DyviewConstants, DyviseConstants
{





/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

protected DyviewWindow	for_window;
protected DyviewModel	view_model;

private static final long serialVersionUID = 1L;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected DyviewAbstractPanel(DyviewWindow dw,DyviewModel mdl)
{
   for_window = dw;
   view_model = mdl;

   setInsets(2);
   setupLabels();
}




/********************************************************************************/
/*										*/
/*	Panel setup methods							*/
/*										*/
/********************************************************************************/

private void setupLabels()
{
   JLabel lbl = new JLabel("DUMMY");
   lbl.setForeground(Color.BLUE);
   setSectionPrototype(lbl);
   lbl = new JLabel("DUMMY");
   Font ft = lbl.getFont();
   float sz = ft.getSize();
   sz += 4;
   ft = ft.deriveFont(sz);
   lbl.setFont(ft);
   setBannerPrototype(lbl);
}



/********************************************************************************/
/*										*/
/*	Utility methods for creating fields					*/
/*										*/
/********************************************************************************/

protected void addDescription(String key,Object val)
{
   if (val instanceof Collection<?>) {
      Object [] vals = ((Collection<?>) val).toArray();
      if (vals.length == 0) {
	 addDescription(key,"");
       }
      else if (vals.length == 1) {
	 addDescription(key,vals[0].toString());
       }
      else {
	 StringBuffer buf = new StringBuffer();
	 int len = 0;
	 for (int i = 0; i < vals.length; ++i) {
	    String s = vals[i].toString();
	    if (s.length() > len) len = s.length();
	    buf.append(s);
	    if (i+1 < vals.length) buf.append("\n");
	  }
	 JTextArea jta = addTextArea(key,buf.toString(),vals.length,len,null);
	 jta.setEditable(false);
       }
    }
   else {
      addDescription(key,val.toString());
    }
}



/********************************************************************************/
/*										*/
/*	Model change methods							*/
/*										*/
/********************************************************************************/

void handleProjectChanged()			{ }

void handleStartClassChanged()			{ }

void handleVisualChanged()			{ }

void handleReadyChanged()			{ }

void handleDataUpdated()			{ }




}	// end of class DyviewAbstractPanel




/* end of DyviewAbstractPanel.java */
