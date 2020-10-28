/********************************************************************************/
/*										*/
/*		DymemDataPanel.java						*/
/*										*/
/*	JPANEL containing data display for a node				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymem/DymemDataPanel.java,v 1.6 2010-03-30 16:21:56 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymemDataPanel.java,v $
 * Revision 1.6  2010-03-30 16:21:56  spr
 * Bug fixes and feature enhancements.  Start of interface for cycle elimination.
 *
 * Revision 1.5  2009-06-04 18:53:35  spr
 * Clean up memory display.
 *
 * Revision 1.4  2009-05-12 22:22:38  spr
 * Handle variable sized lists.
 *
 * Revision 1.3  2009-05-01 23:15:00  spr
 * Fix up data panel graphs, clean up unnecessary code.
 *
 * Revision 1.2  2009-04-28 18:00:57  spr
 * Update visualization with data panel.
 *
 * Revision 1.1  2009-04-11 23:45:29  spr
 * Provide updated visualizations with time line, hashing, etc.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymem;


import edu.brown.cs.ivy.file.IvyFormat;
import edu.brown.cs.ivy.swing.SwingGridPanel;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.*;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;



public class DymemDataPanel extends SwingGridPanel implements DymemConstants
{




/********************************************************************************/
/*										*/
/*	Local Storage								*/
/*										*/
/********************************************************************************/

private DymemGraph for_graph;
private GraphNode for_node;
private GraphNode prev_node;
private GraphNode total_node;

private static final int MAX_REF_ROW = 100;
private static final int REF_MIN_WIDTH = 2;
private static final int REF_MAX_WIDTH = 400;
private static String [] REF_COLS = new String [] { "Referenced By", "%", "", "References To", "%" };

private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public DymemDataPanel(DymemGraph g,GraphNode n,GraphNode pn)
{
   for_graph = g;
   for_node = n;
   prev_node = pn;

   List<GraphNode> nodes = for_graph.getNodeList(OutputCompareBy.TOTAL_SIZE);
   total_node = nodes.get(0);

   setupPanel();
}




/********************************************************************************/
/*										*/
/*	Panel setup methods							*/
/*										*/
/********************************************************************************/

private void setupPanel()
{
   beginLayout();
   addBannerLabel(DymemViewCommon.getNodeName(for_node));

   addSeparator();
   String ni = DymemViewCommon.getNodeInformation(for_node,prev_node,total_node.getLocalSize(),0);
   ni = "<html>" + ni;
   JComponent tbl = new JLabel(ni);
   addLabellessRawComponent("TABLE",tbl,true,false);

   addSeparator();
   GraphPanel pnl = new GraphPanel(for_graph,for_node.getName(),OutputCompareBy.TOTAL_SIZE,
				      Color.BLUE);
   pnl.addDisplay(OutputCompareBy.LOCAL_SIZE,Color.RED);
   addRawComponent("Size",pnl);

   addSeparator();
   pnl = new GraphPanel(for_graph,for_node.getName(),OutputCompareBy.TOTAL_COUNT,
				      Color.BLUE);
   pnl.addDisplay(OutputCompareBy.LOCAL_COUNT,Color.RED);
   addRawComponent("Count",pnl);

   addSeparator();
   pnl = new GraphPanel(for_graph,for_node.getName(),OutputCompareBy.TOTAL_NEW,
				      Color.BLUE);
   pnl.addDisplay(OutputCompareBy.LOCAL_NEW,Color.RED);
   addRawComponent("New",pnl);

   addSeparator();
   addSectionLabel("References");
   addLabellessRawComponent("REFS",getLinkDisplay());
}




/********************************************************************************/
/*										*/
/*	Setup display of in and out information 				*/
/*										*/
/********************************************************************************/

private JComponent getLinkDisplay()
{
   List<GraphLink> inset = for_node.getSortedInLinks(OutputCompareBy.TOTAL_SIZE);
   List<GraphLink> otset = for_node.getSortedOutLinks(OutputCompareBy.TOTAL_SIZE);

   int nrow = inset.size();
   if (otset.size() > nrow) nrow = otset.size();
   if (nrow > MAX_REF_ROW) nrow = MAX_REF_ROW;

   LinkModel tmdl = new LinkModel();
   JTable tbl = new JTable(tmdl);
   int widths[] = new int[tmdl.getColumnCount()];
   for (int i = 0; i < tmdl.getColumnCount(); ++i) {
      widths[i] = REF_MIN_WIDTH;
      JLabel lbl = new JLabel(REF_COLS[i]);
      int w = lbl.getPreferredSize().width;
      if (w > widths[i]) widths[i] = w;
    }

   for (int i = 0; i < tmdl.getRowCount(); ++i) {
      for (int j = 0; j < tmdl.getColumnCount(); ++j) {
	 String s = (String) tmdl.getValueAt(i,j);
	 if (s != null) {
	    JLabel lbl = new JLabel(s);
	    int w = lbl.getPreferredSize().width + 5;
	    if (w > REF_MAX_WIDTH) w = REF_MAX_WIDTH;
	    if (w > widths[j]) widths[j] = w;
	  }
       }
    }

   TableColumnModel cmdl = tbl.getColumnModel();
   for (int i = 0; i < tmdl.getColumnCount(); ++i) {
      TableColumn col = cmdl.getColumn(i);
      col.setHeaderRenderer(new HeaderRenderer(tbl.getTableHeader().getDefaultRenderer()));
      col.setPreferredWidth(widths[i]);
      if (widths[i] < REF_MIN_WIDTH) col.setMinWidth(widths[i]);
    }

   if (tmdl.getRowCount() > 10) {
      return new JScrollPane(tbl);
    }

   JPanel pnl = new JPanel(new BorderLayout());
   pnl.add(tbl.getTableHeader(),BorderLayout.PAGE_START);
   pnl.add(tbl,BorderLayout.CENTER);
   pnl.setBorder(new LineBorder(Color.BLACK,1));

   return pnl;
}



/********************************************************************************/
/*										*/
/*	Graph panel								*/
/*										*/
/********************************************************************************/

private static class GraphPanel extends JPanel {

   private DymemGraph current_graph;
   private String node_id;
   List<GraphData> show_data;
   private static final long serialVersionUID = 1;

   GraphPanel(DymemGraph dg,String id,OutputCompareBy w,Color c) {
      current_graph = dg;
      node_id = id;
      show_data = new ArrayList<GraphData>();
      show_data.add(new GraphData(w,c));
      setBorder(new LineBorder(Color.BLACK,2));
      setPreferredSize(new Dimension(300,40));
    }

   void addDisplay(OutputCompareBy w,Color c) {
      show_data.add(new GraphData(w,c));
    }

   @Override protected void paintComponent(Graphics g0) {
      Graphics2D g = (Graphics2D) g0.create();

      Dimension d0 = getSize();
      g.clearRect(0,0,d0.width,d0.height);
      Rectangle2D r = new Rectangle2D.Double(0,0,d0.width,d0.height);

      DymemStats stats = current_graph.getStatistics();

      DymemViewCommon.graphIndicator(current_graph,g,r,Color.WHITE,true);

      for (GraphData gd : show_data) {
	 double [] hvals = stats.getValues(node_id,gd.getWhat());
	 if (hvals != null) {
	    DymemViewCommon.graphLine(current_graph,g,r,hvals,gd.getColor(),true);
	  }
       }
    }

}	// end of subclass GraphPanel



private static class GraphData {

   private OutputCompareBy show_what;
   private Color show_color;

   GraphData(OutputCompareBy w,Color c) {
      show_what = w;
      show_color = c;
    }

   OutputCompareBy getWhat()			{ return show_what; }
   Color getColor()				{ return show_color; }

}	// end of subclass GraphData



/********************************************************************************/
/*										*/
/*	Table model for links							*/
/*										*/
/********************************************************************************/

private class LinkModel extends AbstractTableModel {

   private int num_rows;
   private List<GraphLink> in_links;
   private List<GraphLink> out_links;

   private static final long serialVersionUID = 1;

   LinkModel() {
      in_links = for_node.getSortedInLinks(OutputCompareBy.TOTAL_SIZE);
      out_links = for_node.getSortedOutLinks(OutputCompareBy.TOTAL_SIZE);
      num_rows = in_links.size();
      if (out_links.size() > num_rows) num_rows = out_links.size();
      if (num_rows > MAX_REF_ROW) num_rows = MAX_REF_ROW;
    }

   @Override public int getColumnCount()		{ return REF_COLS.length; }

   @Override public String getColumnName(int idx)	{ return REF_COLS[idx]; }

   @Override public Class<?> getColumnClass(int idx)	{ return String.class; }

   @Override public boolean isCellEditable(int r,int c) { return false; }

   @Override public int getRowCount()			{ return num_rows; }

   @Override public Object getValueAt(int r,int c) {
      String rslt = null;
      switch (c) {
	 case 0 :
	    if (in_links.size() > r) {
	       GraphLink gl = in_links.get(in_links.size()-1-r);
	       GraphNode gn = gl.getFromNode();
	       rslt = DymemViewCommon.getTypeName(gn.getName(),"\u2190");
	     }
	    break;
	 case 1 :
	    if (in_links.size() > r) {
	       GraphLink gl = in_links.get(in_links.size()-1-r);
	       rslt = IvyFormat.formatPercent(gl.getSizePercent());
	     }
	    break;
	 case 3 :
	    if (out_links.size() > r) {
	       GraphLink gl = out_links.get(r);
	       GraphNode gn = gl.getToNode();
	       rslt = DymemViewCommon.getTypeName(gn.getName(),"\u2190");
	     }
	    break;
	 case 4 :
	    if (out_links.size() > r) {
	       GraphLink gl = out_links.get(r);
	       rslt = IvyFormat.formatPercent(gl.getSizePercent());
	     }
	    break;
       }
      return rslt;
    }

   @Override public void setValueAt(Object val,int r,int c) { }

}	// end of subclass LinkModel



/********************************************************************************/
/*										*/
/*	Table renderers 							*/
/*										*/
/********************************************************************************/

private class HeaderRenderer implements TableCellRenderer {

   private TableCellRenderer default_renderer;
   private Font bold_font;

   HeaderRenderer(TableCellRenderer dflt) {
      default_renderer = dflt;
      bold_font = null;
    }

   public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,
						     boolean foc,int r,int c) {
      Component cmp = default_renderer.getTableCellRendererComponent(t,v,sel,foc,r,c);
      if (bold_font == null) {
	 bold_font = cmp.getFont();
	 bold_font = bold_font.deriveFont(Font.BOLD);
       }
      cmp.setFont(bold_font);
      return cmp;
    }

}	// end of subclass HeaderRenderer




}	// end of class DymemDataPanel



/* end of DymemDataPanel.java */
