/********************************************************************************/
/*										*/
/*		DyvisionDetail.java						*/
/*										*/
/*	Tablular view of detailed performance evaluation data			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyvision/DyvisionDetail.java,v 1.2 2010-03-30 16:24:34 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyvisionDetail.java,v $
 * Revision 1.2  2010-03-30 16:24:34  spr
 * Clean up statistic display.
 *
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dyvision;


import edu.brown.cs.ivy.swing.SwingGridPanel;

import org.w3c.dom.Element;

import javax.swing.*;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.util.*;





class DyvisionDetail extends SwingGridPanel implements DyvisionConstants {




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private DyvisionMain	for_main;
private DyvisionView	for_view;
private boolean 	is_shown;
private String		cur_detail;
private boolean 	was_visible;

private List<DyvisionTableSpec> current_specs;
private List<JTable> current_tables;

private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DyvisionDetail(DyvisionMain dm,DyvisionView dv,String pid)
{
   for_main = dm;
   for_view = dv;
   current_specs = new ArrayList<DyvisionTableSpec>();
   current_tables = new ArrayList<JTable>();
   cur_detail = null;
   is_shown = false;
   setVisible(false);
   was_visible = false;
}



/********************************************************************************/
/*										*/
/*	Methods to show tables							*/
/*										*/
/********************************************************************************/

void setupTables(Collection<DyvisionTableSpec> tbls)
{
   clearTables();
   JComponent cmp = null;

   int ct = 0;
   for (DyvisionTableSpec ts : tbls) {
      JComponent tc = setupTable(ts);
      if (tc == null) continue;
      if (cmp == null) cmp = tc;
      else {
	 JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT,true,cmp,tc);
	 sp.setResizeWeight(1.0/(ct+1));
	 cmp = sp;
       }
      ++ct;
    }

   if (cmp == null) return;
   addGBComponent(cmp,0,0,1,1,10,10);
   if (is_shown) setVisible(true);
}




private void clearTables()
{
   removeAll();
   current_specs.clear();
   current_tables.clear();
}



private JComponent setupTable(DyvisionTableSpec ts)
{
   JTable jt = ts.createTable();
   if (jt == null) return null;


   String ttl = ts.getTableTitle();
   JLabel lbl = new JLabel(ttl);
   Font f = lbl.getFont();
   f = f.deriveFont(Font.BOLD,16.0f);
   lbl.setFont(f);
   lbl.setHorizontalAlignment(SwingConstants.CENTER);

   SwingGridPanel pnl = new SwingGridPanel();
   int ct = 0;
   pnl.addGBComponent(lbl,0,ct++,1,1,10,1);
   pnl.addGBComponent(new JScrollPane(jt),0,ct++,1,1,10,10,GridBagConstraints.BOTH);

   current_tables.add(jt);
   current_specs.add(ts);

   return pnl;
}



/********************************************************************************/
/*										*/
/*	Methods to handle updates						*/
/*										*/
/********************************************************************************/

void update(Element e)
{
   boolean rsz = false;

   for (int i = 0; i < current_specs.size(); ++i) {
      rsz |= current_specs.get(i).updateTable(e,current_tables.get(i));
    }

   if (rsz) for_view.updateSize();
}


void showView(String id)
{
   if (isVisible()) was_visible = true;

   if (id != null) {
      if (cur_detail == null || !cur_detail.equals(id)) {
	 for_main.setupTables(this,id);
	 cur_detail = id;
       }
    }

   is_shown = true;
   if (!current_specs.isEmpty()) setVisible(true);
}


void hideView()
{
   is_shown = false;
   if (!was_visible) setVisible(false);
}



boolean isShown()
{
   return is_shown;
}



}	// end of class DyvisionDetail




/* end of DyvisionDetail.java */
