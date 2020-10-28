/********************************************************************************/
/*										*/
/*		DymemParameters.java						*/
/*										*/
/*	Parameter management for dynamic memory visualizer			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymem/DymemParameters.java,v 1.6 2013-06-03 13:02:57 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymemParameters.java,v $
 * Revision 1.6  2013-06-03 13:02:57  spr
 * Minor bug fixes
 *
 * Revision 1.5  2010-03-30 16:21:56  spr
 * Bug fixes and feature enhancements.  Start of interface for cycle elimination.
 *
 * Revision 1.4  2009-10-07 01:00:02  spr
 * Code cleanup for eclipse. Add support for standalone version.
 *
 * Revision 1.3  2009-06-04 18:53:35  spr
 * Clean up memory display.
 *
 * Revision 1.2  2009-04-11 23:45:29  spr
 * Provide updated visualizations with time line, hashing, etc.
 *
 * Revision 1.1  2009-03-20 02:10:12  spr
 * Add memory graph computation and display.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymem;


import edu.brown.cs.ivy.swing.*;

import javax.swing.*;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;


public class DymemParameters implements DymemConstants, ActionListener {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private double cutoff_value;
private OutputCompareBy compare_by;

private double error_fraction;
private double min_fraction;
private double min_reference;
private int    min_refs;
private int    min_total_refs;
private boolean fix_root_links;
private double root_cutoff;
private boolean use_system_node;
private boolean use_class_nodes;
private boolean use_thread_nodes;
private OutputCompareBy show_down;
private OutputCompareBy show_up;
private boolean show_vertical;
private OutputCompareBy show_history;
private boolean all_history;
private PrintWriter stat_output;

private List<IgnoreArc> ignore_arcs;

private JFrame	param_frame;
private SwingGridPanel param_panel;

private Set<ParameterListener> change_callbacks;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public DymemParameters(String pid)
{
   cutoff_value = 0.0050;
   compare_by = OutputCompareBy.TOTAL_SIZE;

   error_fraction = 0.25;
   min_fraction = 0.01;
   min_reference = 0.01;
   min_refs = 10;
   min_total_refs = 100;
   fix_root_links = true;
   root_cutoff = 0.5;
   use_system_node = true;
   use_class_nodes = true;
   use_thread_nodes = false;
   show_up = OutputCompareBy.LOCAL_SIZE;
   show_down = OutputCompareBy.LOCAL_NEW;
   show_history = OutputCompareBy.TOTAL_SIZE;
   all_history = false;
   show_vertical = false;

   ignore_arcs = new ArrayList<IgnoreArc>();
   ignore_arcs.add(new IgnoreArc("Lsun/misc/Launcher$ExtClassLoader;",null));
   ignore_arcs.add(new IgnoreArc(":Lsun/misc/Launcher$ExtClassLoader;",null));
   ignore_arcs.add(new IgnoreArc("Lsun/misc/Launcher$AppClassLoader;",null));
   ignore_arcs.add(new IgnoreArc(":Lsun/misc/Launcher$AppClassLoader;",null));
   ignore_arcs.add(new IgnoreArc("Ljava/lang/Thread;",null));
   ignore_arcs.add(new IgnoreArc("Ljava/lang/ClassLoader;",null));
   ignore_arcs.add(new IgnoreArc(":Ljava/lang/ClassLoader;",null));
   ignore_arcs.add(new IgnoreArc("Ljava/lang/ref/SoftReference;",null));
   ignore_arcs.add(new IgnoreArc("Ljava/lang/ref/Reference$ReferenceHandler;",null));
   ignore_arcs.add(new IgnoreArc("Lsun/net/www/http/ChunkedInputStream;",null));
   ignore_arcs.add(new IgnoreArc("Lsun/nio/cs/StreamEncoder;",null));
   ignore_arcs.add(new IgnoreArc("Lsun/net/www/protocol/http/HttpURLConnection$HttpInputStream;",null));

   stat_output = null;
   try {
      File f = new File(DYMEM_HEAP_PREFIX + pid + ".stats");
      stat_output = new PrintWriter(new FileWriter(f));
    }
   catch (IOException e) {
      System.err.println("DYMEM: Couldn't create stats file: " + e);
    }

   change_callbacks = new HashSet<ParameterListener>();
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

double getViewerCutoff()			{ return cutoff_value; }

OutputCompareBy getViewerCompareBy()		{ return compare_by; }


Color getViewerColor(double v,double scale)
{
   float h = (float)((1.0 - v) / 3.0);
   float s = (float)(scale * 0.6 + 0.4);

   int icol = Color.HSBtoRGB(h,s,1f);

   return new Color(icol);
}



double getPredictionError()			{ return error_fraction; }

double getMinimumFraction()			{ return min_fraction; }

double getMinimumReferenceFraction()		{ return min_reference; }

double getMinRefCount() 			{ return min_refs; }

double getMinTotalRefs()			{ return min_total_refs; }

boolean getFixRootLinks()			{ return fix_root_links; }

double getRootCutoff()				{ return root_cutoff; }

boolean getUseSystemNode()			{ return use_system_node; }

boolean getUseClassNodes()			{ return use_class_nodes; }

boolean getUseThreadNodes()			{ return use_thread_nodes; }

OutputCompareBy getShowDown()			{ return show_down; }
OutputCompareBy getShowUp()			{ return show_up; }

OutputCompareBy getShowHistory()		{ return show_history; }
boolean getShowAllHistory()			{ return all_history; }

boolean getShowVertical()			{ return show_vertical; }

PrintWriter getStatWriter()			{ return stat_output; }


boolean ignoreArc(String f,String t)
{
   for (IgnoreArc ia : ignore_arcs) {
      if (ia.ignore(f,t)) return true;
    }

   return false;
}



/********************************************************************************/
/*										*/
/*	Methods to handle changes						*/
/*										*/
/********************************************************************************/

void addParameterListener(ParameterListener pl)
{
   change_callbacks.add(pl);
}


private void triggerChange()
{
   for (ParameterListener pl : change_callbacks) {
      pl.valuesChanged();
    }
}



/********************************************************************************/
/*										*/
/*	Methods to put up a parameter dialog box				*/
/*										*/
/********************************************************************************/

synchronized void showParameters()
{
   if (param_frame != null) {
      setDialogValues();
      param_frame.setVisible(true);
      return;
    }

   param_frame = new JFrame();
   param_panel = new SwingGridPanel();

   param_panel.beginLayout();
   param_panel.addBannerLabel("Memory Visualization Parameters");
   param_panel.addSeparator();
   param_panel.addNumericField("Cutoff Value",0.0001,0.1000,cutoff_value,this);
   param_panel.addChoice("Compare By",compare_by,this);
   param_panel.addBoolean("Vertical Display",show_vertical,this);
   param_panel.addNumericField("Error Fraction",0.01,0.50,error_fraction,this);
   param_panel.addNumericField("Min Fraction",0.001,0.10,min_fraction,this);
   param_panel.addNumericField("Reference Fraction",0.001,0.10,min_reference,this);
   param_panel.addNumericField("Min References",0,100,min_refs,this);
   param_panel.addNumericField("Min Total Refs",0,1000,min_total_refs,this);
   param_panel.addBoolean("Fix Root Links",fix_root_links,this);
   param_panel.addNumericField("Root Elimination",0.0,1.0,root_cutoff,this);
   param_panel.addBoolean("Use System Node",use_system_node,this);
   param_panel.addBoolean("Use Class Nodes",use_class_nodes,this);
   param_panel.addBoolean("Use Thread Nodes",use_thread_nodes,this);
   param_panel.addChoice("Show Down",show_down,this);
   param_panel.addChoice("Show Up",show_up,this);
   param_panel.addChoice("Show History",show_history,this);
   param_panel.addBoolean("Show All History",all_history,this);

   param_panel.addBottomButton("Dismiss","Dismiss",this);
   param_panel.addBottomButtons();
   param_frame.setContentPane(param_panel);
   param_frame.pack();

   param_frame.setVisible(true);
}



private void setDialogValues()
{
   SwingNumericField nf = (SwingNumericField) param_panel.getComponentForLabel("Cutoff Value");
   nf.setValue(cutoff_value);
   SwingComboBox<?> cbx = (SwingComboBox<?>) param_panel.getComponentForLabel("Compare By");
   cbx.setSelectedItem(compare_by);
   nf = (SwingNumericField) param_panel.getComponentForLabel("Error Fraction");
   nf.setValue(error_fraction);
   nf = (SwingNumericField) param_panel.getComponentForLabel("Min Fraction");
   nf.setValue(min_fraction);
   nf = (SwingNumericField) param_panel.getComponentForLabel("Reference Fraction");
   nf.setValue(min_reference);
   nf = (SwingNumericField) param_panel.getComponentForLabel("Min References");
   nf.setValue(min_refs);
   nf = (SwingNumericField) param_panel.getComponentForLabel("Min Total Refs");
   nf.setValue(min_total_refs);
   JCheckBox xbx = (JCheckBox) param_panel.getComponentForLabel("Fix Root Links");
   xbx.setSelected(fix_root_links);
   nf = (SwingNumericField) param_panel.getComponentForLabel("Root Elimination");
   nf.setValue(root_cutoff);
   xbx = (JCheckBox) param_panel.getComponentForLabel("Use System Node");
   xbx.setSelected(use_system_node);
   xbx = (JCheckBox) param_panel.getComponentForLabel("Use Class Nodes");
   xbx.setSelected(use_class_nodes);
   xbx = (JCheckBox) param_panel.getComponentForLabel("Use Thread Nodes");
   xbx.setSelected(use_thread_nodes);
   cbx = (SwingComboBox<?>) param_panel.getComponentForLabel("Show Down");
   cbx.setSelectedItem(show_down);
   cbx = (SwingComboBox<?>) param_panel.getComponentForLabel("Show Up");
   cbx.setSelectedItem(show_up);
   cbx = (SwingComboBox<?>) param_panel.getComponentForLabel("Show History");
   cbx.setSelectedItem(show_history);
   xbx = (JCheckBox) param_panel.getComponentForLabel("Show All History");
   xbx.setSelected(all_history);
   xbx = (JCheckBox) param_panel.getComponentForLabel("Vertical Display");
   xbx.setSelected(show_vertical);
}



public void actionPerformed(ActionEvent e)
{
   String c = e.getActionCommand();
   if (c.equals("Cutoff Value")) {
      SwingNumericField nf = (SwingNumericField) e.getSource();
      cutoff_value = nf.getValue();
    }
   else if (c.equals("Compare By")) {
      JComboBox<?> cbx = (JComboBox<?>) e.getSource();
      compare_by = (OutputCompareBy) cbx.getSelectedItem();
    }
   else if (c.equals("Error Fraction")) {
      SwingNumericField nf = (SwingNumericField) e.getSource();
      error_fraction = nf.getValue();
    }
   else if (c.equals("Min Fraction")) {
      SwingNumericField nf = (SwingNumericField) e.getSource();
      min_fraction = nf.getValue();
    }
   else if (c.equals("Reference Fraction")) {
      SwingNumericField nf = (SwingNumericField) e.getSource();
      min_reference = nf.getValue();
    }
   else if (c.equals("Min References")) {
      SwingNumericField nf = (SwingNumericField) e.getSource();
      min_refs = (int) nf.getValue();
    }
   else if (c.equals("Min Total Refs")) {
      SwingNumericField nf = (SwingNumericField) e.getSource();
      min_total_refs = (int) nf.getValue();
    }
   else if (c.equals("Fix Root Links")) {
      JCheckBox xbx = (JCheckBox) e.getSource();
      fix_root_links = xbx.isSelected();
    }
   else if (c.equals("Root Elimination")) {
      SwingNumericField nf = (SwingNumericField) e.getSource();
      root_cutoff = nf.getValue();
    }
   else if (c.equals("Use System Node")) {
      JCheckBox xbx = (JCheckBox) e.getSource();
      use_system_node = xbx.isSelected();
    }
   else if (c.equals("Use Class Nodes")) {
      JCheckBox xbx = (JCheckBox) e.getSource();
      use_class_nodes = xbx.isSelected();
    }
   else if (c.equals("Use Thread Nodes")) {
      JCheckBox xbx = (JCheckBox) e.getSource();
      use_thread_nodes = xbx.isSelected();
    }
   else if (c.equals("Dismiss")) {
      param_frame.setVisible(false);
    }
   else if (c.equals("Show Down")) {
      JComboBox<?> cbx = (JComboBox<?>) e.getSource();
      show_down = (OutputCompareBy) cbx.getSelectedItem();
    }
   else if (c.equals("Show Up")) {
      JComboBox<?> cbx = (JComboBox<?>) e.getSource();
      show_up = (OutputCompareBy) cbx.getSelectedItem();
    }
   else if (c.equals("Show History")) {
      JComboBox<?> cbx = (JComboBox<?>) e.getSource();
      show_history = (OutputCompareBy) cbx.getSelectedItem();
    }
   else if (c.equals("Show All History")) {
      JCheckBox xbx = (JCheckBox) e.getSource();
      all_history = xbx.isSelected();
    }
   else if (c.equals("Vertical Display")) {
      JCheckBox xbx = (JCheckBox) e.getSource();
      show_vertical = xbx.isSelected();
    }
   else {
      System.err.println("DYMEM: Unknown parameter action: " + c);
    }

   triggerChange();
}




/********************************************************************************/
/*										*/
/*	Class to hold arcs to ignore						*/
/*										*/
/********************************************************************************/

private static class IgnoreArc {

   private String from_node;
   private String to_node;
   private boolean from_end;
   private boolean to_end;

   IgnoreArc(String f,String t) {
      from_node = f;
      to_node = t;
      from_end = (f != null && f.startsWith(":"));
      to_end = (t != null && t.startsWith(":"));
    }

   boolean ignore(String f,String t) {
      if (from_node != null) {
	 if (from_end) {
	    if (!f.endsWith(from_node)) return false;
	  }
	 else if (!from_node.equals(f)) return false;
       }

      if (to_node != null) {
	 if (to_end) {
	    if (!t.endsWith(to_node)) return false;
	  }
	 else if (!to_node.equals(t)) return false;
       }

      return true;
    }

}	// end of inner class IgnoreArc



}	// end of class DymemParameters




/* end of DymemParameters.java */
