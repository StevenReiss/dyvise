/********************************************************************************/
/*										*/
/*		DyvisionTableSpec.java						*/
/*										*/
/*	Information about a particular type of table				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyvision/DyvisionTableSpec.java,v 1.8 2013-05-09 12:29:06 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyvisionTableSpec.java,v $
 * Revision 1.8  2013-05-09 12:29:06  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.7  2012-10-05 00:53:07  spr
 * Code clean up.
 *
 * Revision 1.6  2011-03-10 02:33:28  spr
 * Code cleanup.
 *
 * Revision 1.5  2009-09-19 00:14:57  spr
 * UPdate front end to clean up tables.
 *
 * Revision 1.4  2009-04-14 22:23:16  spr
 * Avoid null pointers in odd cases.
 *
 * Revision 1.3  2009-03-20 02:09:09  spr
 * Add memory view and related controls. Clean code.
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


import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.swing.*;
import javax.swing.table.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;




class DyvisionTableSpec implements DyvisionConstants {


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private String table_name;
private String table_title;
private Set<String> agent_set;
private boolean is_dynamic;
private List<Integer> key_column;
private int default_rows;

private List<ColumnSpec> column_data;

private Transformer data_transformer;

private static TransformerFactory trans_fac = TransformerFactory.newInstance();


private static final int	MAX_ROWS = 15;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DyvisionTableSpec(Element xml)
{
   table_name = IvyXml.getAttrString(xml,"NAME");
   table_title = IvyXml.getAttrString(xml,"TITLE");
   if (table_title == null) table_title = table_name;

   default_rows = IvyXml.getAttrInt(xml,"ROWS",5);

   agent_set = new HashSet<String>();
   String as = IvyXml.getAttrString(xml,"AGENT");
   StringTokenizer tok = new StringTokenizer(as," \t\n,;");
   while (tok.hasMoreTokens()) agent_set.add(tok.nextToken());

   column_data = new ArrayList<ColumnSpec>();
   key_column = new ArrayList<Integer>();
   int i = 0;
   for (Element e : IvyXml.elementsByTag(xml,"COLUMN")) {
      ColumnSpec cs = new ColumnSpec(e);
      column_data.add(cs);
      if (cs.isKey()) key_column.add(i);
      ++i;
    }
   if (i > 0) {
      ColumnSpec cs = column_data.get(i-1);
      is_dynamic = cs.isDynamic();
    }

   try {
      Element et1 = IvyXml.getElementByTag(xml,"xsl:stylesheet");
      String s = IvyXml.convertXmlToString(et1);
      StreamSource ss = new StreamSource(new StringReader(s));
      data_transformer = trans_fac.newTransformer(ss);
    }
   catch (TransformerConfigurationException e) {
      System.err.println("DYVISION: Problem constructing transformer for " + table_name + ": " + e);
     }
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getTableTitle()			{ return table_title; }


boolean useForAgent(String name)
{
   return agent_set.contains(name);
}



/********************************************************************************/
/*										*/
/*	Table creation methods							*/
/*										*/
/********************************************************************************/

JTable createTable()
{
   try {
      return new DetailTable();
    }
   catch (Throwable t) {
      System.err.println("DYVISION: Problem creating table " + table_title + ": " + t);
      t.printStackTrace();
    }

   return null;
}




/********************************************************************************/
/*										*/
/*	Table update methods							*/
/*										*/
/********************************************************************************/

boolean updateTable(Element xml,JTable t)
{
   ByteArrayOutputStream bas = new ByteArrayOutputStream();
   StreamResult sr = new StreamResult(bas);

   Document d = null;
   for (Node n = xml; n != null && d == null; n = n.getParentNode()) {
      if (n instanceof Document) d = (Document) n;
    }

   Source src = new DOMSource(d);

   try {
      data_transformer.transform(src,sr);
    }
   catch (TransformerException e) {
      System.err.println("DYVISION: Problem converting xml data for " + table_name +": " + e);
      return false;
    }

   DetailTable dt = (DetailTable) t;
   DetailModel dm = (DetailModel) t.getModel();
   dm.beginUpdate();

   StringTokenizer tok = new StringTokenizer(bas.toString(),";");
   List<String> row = new ArrayList<String>();
   while (tok.hasMoreTokens()) {
      String s = tok.nextToken().trim();
      if (s.equals("***EOL***")) {
	 dm.update(row);
	 row.clear();
       }
      else row.add(s);
    }

   if (dm.endUpdate()) {
      dt.updateSize();
      // return true;		Changing the window size is too disruptive here
    }

   return false;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

private String getKey(List<?> data)
{
   if (key_column == null) return null;

   String r = null;
   for (Integer idx : key_column) {
      int i = idx;
      String s = data.get(i).toString();
      if (r == null) r = s;
      else r += ";" + s;
    }

   return r;
}




/********************************************************************************/
/*										*/
/*	Column Information holder						*/
/*										*/
/********************************************************************************/

private class ColumnSpec {

   private String column_name;
   private ValueType column_type;
   private int column_size;
   private boolean can_resize;
   private boolean is_key;
   private boolean col_dynamic;
   private String column_units;
   private String default_value;

   ColumnSpec(Element e) {
      column_name = IvyXml.getAttrString(e,"NAME");
      column_type = IvyXml.getAttrEnum(e,"TYPE",ValueType.STRING);
      column_size = IvyXml.getAttrInt(e,"SIZE");
      column_units = IvyXml.getAttrString(e,"UNITS");
      default_value = IvyXml.getAttrString(e,"DEFAULT");
      if (column_units == null) {
	 switch (column_type) {
	    case PERCENT :
	       column_units = "%";
	       break;
	    default :
	       break;
	  }
       }
      can_resize = IvyXml.getAttrBool(e,"RESIZE",true);
      is_key = IvyXml.getAttrBool(e,"KEY");
      col_dynamic = IvyXml.getAttrBool(e,"DYNAMIC");
    }

   String getName()			{ return column_name; }
   int getSize()			{ return column_size; }
   boolean canResize()			{ return can_resize; }
   boolean isKey()			{ return is_key; }
   boolean isDynamic()			{ return col_dynamic; }
   String getUnits()			{ return column_units; }
   String getDefault()			{ return default_value; }

   Class<?> getDataClass() {
      switch (column_type) {
	 default :
	 case STRING :
	 case BOOLEAN :
	    return String.class;
	 case NUMBER :
	 case PERCENT :
	 case TIME :
	 case MSTIME :
	 case USTIME :
	 case MEMORY :
	 case INTERVAL :
	 case MSINTERVAL :
	 case USINTERVAL :
	    return Double.class;
	 case COUNT :
	 case LINE :
	    return Long.class;
       }
    }

   Object convertData(String s) {
      switch (column_type) {
         default :
         case STRING :
            return s;
         case NUMBER :
         case TIME :
         case MSTIME :
         case USTIME :
         case MEMORY :
            if (s == null || s.equals("") || s.equals("?")) return Double.valueOf(0);
            double scale = 1;
            if (s.endsWith("K")) scale = 1024;
            else if (s.endsWith("M")) scale = 1024*1024;
            else if (s.endsWith("G")) scale = 1024*1024*1024;
            if (scale != 1) s = s.substring(0,s.length()-1);
            try {
               return Double.valueOf(Double.parseDouble(s)*scale);
             }
            catch (NumberFormatException e) {
               System.err.println("DYVISION: Illegal numeric value: " + s + " " + column_type);
             }
            return Double.valueOf(0);
         case PERCENT :
            if (s == null || s.equals("") || s.equals("?")) return Double.valueOf(0);
            try {
               return Double.valueOf(Double.parseDouble(s)/100.0);
             }
            catch (NumberFormatException e) {
               System.err.println("DYVISION: Illegal numeric value: " + s + " " + column_type);
             }
            return Double.valueOf(0);
         case INTERVAL :
         case MSINTERVAL :
         case USINTERVAL :
            if (s == null || s.equals("") || s.equals("?")) return Double.valueOf(0);
            try {
               return Double.valueOf(s);
             }
            catch (NumberFormatException e) {
               System.err.println("DYVISION: Illegal numeric value: " + s + " " + column_type);
             }
            return Double.valueOf(0);
         case COUNT :
         case LINE :
            if (s == null || s.equals("") || s.equals("?")) return Long.valueOf(0);
            scale = 1;
            if (s.endsWith("K")) scale = 1000;
            else if (s.endsWith("M")) scale = 1000*1000;
            else if (s.endsWith("G")) scale = 1000*1000*1000;
            if (scale != 1) s = s.substring(0,s.length()-1);
            try {
               long l = (long)(0.5 + Double.parseDouble(s)*scale);
               return Long.valueOf(l);
             }
            catch (NumberFormatException e) {
               System.err.println("DYVISION: Illegal numeric value: " + s + " " + column_type);
             }
            return Long.valueOf(0);
       }
    }

   String convertOutput(Object o) {
      if (o == null) return null;

      if (o instanceof Number) {
	 double d = ((Number) o).doubleValue();
	 return DyvisionFormat.outputValue(d,column_type);
       }

      return o.toString();
    }

   int getHorizontalAlignment() {
      switch (column_type) {
	 default :
	 case STRING :
	 case BOOLEAN :
	    return SwingConstants.LEADING;
	 case NUMBER :
	 case PERCENT :
	 case TIME :
	 case MSTIME :
	 case USTIME :
	 case MEMORY :
	 case INTERVAL :
	 case MSINTERVAL :
	 case USINTERVAL :
	 case COUNT :
	 case LINE :
	    return SwingConstants.RIGHT;
       }
    }


}	// end of subclass ColumnSpec




/********************************************************************************/
/*										*/
/*	Detail table class							*/
/*										*/
/********************************************************************************/

private class DetailTable extends JTable {

   private static final long serialVersionUID = 1;

   DetailTable() {
      super(new DetailModel());
      setAutoCreateRowSorter(true);
      setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
      setFillsViewportHeight(true);
      fixColumnSizes();
      setIntercellSpacing(new Dimension(10,1));
      for (Enumeration<TableColumn> e = getColumnModel().getColumns(); e.hasMoreElements(); ) {
	 TableColumn tc = e.nextElement();
	 tc.setHeaderRenderer(new HeaderRenderer(getTableHeader().getDefaultRenderer()));
       }

      int cct = getModel().getColumnCount();
      int i = 0;
      for (ColumnSpec cs : column_data) {
	 if (i >= cct) break;
	 getColumnModel().getColumn(i).setCellRenderer(new CellRenderer(cs));
	 ++i;
       }

      setToolTipText(table_name);

      updateSize();
    }

   void updateSize() {
      Dimension d = getPreferredSize();
      d.width += 100;

      int hdrht = getTableHeader().getHeaderRect(0).height;
      int mht = getRowHeight() * default_rows;
      // if (d.height < mht+hdrht) d.height = mht+hdrht;
      d.height = mht + hdrht;
      int rct = getModel().getRowCount();
      if (rct > MAX_ROWS) rct = MAX_ROWS;
      if (d.height < hdrht+rct*getRowHeight()) d.height = hdrht+rct*getRowHeight();

      mht = MAX_ROWS*getRowHeight()+hdrht;
      if (d.height > mht) d.height = mht;

      setPreferredScrollableViewportSize(d);
    }

   private void fixColumnSizes() {
      TableColumnModel tcm = getColumnModel();
      for (int i = 0; i < tcm.getColumnCount(); ++i) {
	 ColumnSpec cs = column_data.get(i);
	 TableColumn tc = tcm.getColumn(i);
	 tc.setPreferredWidth(cs.getSize());
	 if (!cs.canResize()) {
	    tc.setMaxWidth(cs.getSize());
	    tc.setMinWidth(cs.getSize());
	  }
       }
    }

   public String getToolTipText(MouseEvent evt) {
      int col = columnAtPoint(evt.getPoint());
      if (col < 0) return null;
      col = convertColumnIndexToModel(col);
      int row = rowAtPoint(evt.getPoint());
      if (row < 0) return null;
      row = convertRowIndexToModel(row);
      if (row < 0 || col < 0) return null;
      int cols = col;
      if (col >= column_data.size()) cols = column_data.size() - 1;

      DetailModel dm = (DetailModel) getModel();
      Object val = dm.getValueAt(row,col);
      ColumnSpec cs = column_data.get(cols);
      String vals = cs.convertOutput(val);
      String units = cs.getUnits();
      if (units != null) vals += " " + units;
      return vals;
    }

}	// end of subclass DetailTable




/********************************************************************************/
/*										*/
/*	Table model								*/
/*										*/
/********************************************************************************/

private class DetailModel extends AbstractTableModel {

   private List<List<Object>> model_data;
   private Map<String,Integer> key_map;
   private boolean [] update_flags;
   private SortedMap<String,Integer> dynamic_map;

   private static final long serialVersionUID = 1;


   DetailModel() {
      model_data = new ArrayList<List<Object>>();
      key_map = new HashMap<String,Integer>();
      dynamic_map = null;
      update_flags = null;
    }

   @Override public int getColumnCount() {
      int ct = column_data.size();
      if (is_dynamic) {
	 if (dynamic_map != null) ct += dynamic_map.size() - 1;
	 else ct -= 1;
       }
      return ct;
    }

   @Override public String getColumnName(int i) {
      if (!is_dynamic || i < column_data.size()-1) return column_data.get(i).getName();
      int ct = i-column_data.size()+1;
      for (String s : dynamic_map.keySet()) {
	 if (ct-- == 0) return s;
       }
      return null;
    }

   @Override public Class<?> getColumnClass(int i) {
      if (i >= column_data.size()) i = column_data.size()-1;
      return column_data.get(i).getDataClass();
    }

   @Override public int getRowCount()		{ return model_data.size(); }

   @Override public Object getValueAt(int r,int c) {
      try {
	 return model_data.get(r).get(c);
       }
      catch (Throwable t) {
	 System.err.println("DYVISION: Table error for " + table_name + ": " + t);
	 t.printStackTrace();
       }
      return null;
    }

   void beginUpdate() {
      update_flags = new boolean[model_data.size()];
      key_map.clear();
      int i = 0;
      for (List<Object> r : model_data) {
	 String s0 = getKey(r);
	 key_map.put(s0,i);
	 ++i;
       }
    }

   void update(List<String> data) {
      if (is_dynamic) updateDynamicColumns(data);

      String s0 = getKey(data);
      List<Object> rslt;
      if (key_map.containsKey(s0)) {
	 int idx = key_map.get(s0);
	 update_flags[idx] = true;
	 rslt = model_data.get(idx);
       }
      else {
	 rslt = new ArrayList<Object>();
	 model_data.add(rslt);
       }

      updateData(data,rslt);
    }

   boolean endUpdate() {
      boolean chng = false;

      for (int i = update_flags.length-1; i >= 0; --i) {
	 if (!update_flags[i]) {
	    model_data.remove(i);
	  }
       }

      if (model_data.size() > update_flags.length) {	// new rows have been added
	 if (update_flags.length < MAX_ROWS) chng = true;
       }

      update_flags = null;
      fireTableDataChanged();

      return chng;
    }

   private void updateData(List<String> data,List<Object> r) {
      int ct = column_data.size();
      int i = 0;
      int col = 0;
      boolean havedyn = false;
      for (String s : data) {
	 ColumnSpec cs = column_data.get(i);
	 if (i == ct-1 && cs.isDynamic()) {
	    if (!havedyn) {
	       havedyn = true;
	       String dflt = cs.getDefault();
	       for (int j = 0; j < dynamic_map.size(); ++j) {
		  if (r.size() <= ct-1+j) r.add(dflt);
		  else r.set(ct-1+j,dflt);
		}
	     }
	    int idx = s.indexOf('=');
	    if (idx >= 0) {
	       String key = s.substring(0,idx).trim();
	       s = s.substring(idx+1).trim();
	       col = dynamic_map.get(key);
	     }
	  }
	 Object o = cs.convertData(s);
	 while (r.size() <= col) r.add(cs.getDefault());
	 r.set(col,o);
	 if (i < ct-1) ++i;
	 ++col;
       }
    }

   private void updateDynamicColumns(List<String> data) {
      if (dynamic_map == null) dynamic_map = new TreeMap<String,Integer>(new SmartComparator());
      boolean reset = false;
      for (int i = column_data.size()-1; i < data.size(); ++i) {
	 String s = data.get(i);
	 int idx = s.indexOf('=');
	 if (idx < 0) continue;
	 String key = s.substring(0,idx);
	 if (!dynamic_map.containsKey(key)) {
	    dynamic_map.put(key,-1);
	    reset = true;
	  }
       }
      if (reset) {
	 int i = column_data.size()-1;
	 for (Map.Entry<String,Integer> ent : dynamic_map.entrySet()) {
	    ent.setValue(i++);
	  }
	 fireTableStructureChanged();
       }
    }


}	// end of subclass DetailModel




private static class SmartComparator implements Comparator<String> {

   private Pattern number_regex;

   SmartComparator() {
      number_regex = Pattern.compile("^[0-9]+$");
    }

   public int compare(String s1,String s2) {
      if (s1 == null && s2 == null) return 0;
      if (s1 == null) return -1;
      if (s2 == null) return 1;
      if (number_regex.matcher(s1).matches() &&
	     number_regex.matcher(s2).matches()) {
	 int i1 = Integer.parseInt(s1);
	 int i2 = Integer.parseInt(s2);
	 if (i1 < i2) return -1;
	 if (i1 > i2) return 1;
	 return 0;
       }
      return s1.compareTo(s2);
    }

}	// end of subclass SmartComparator




/********************************************************************************/
/*										*/
/*	Renderers								*/
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




private class CellRenderer extends DefaultTableCellRenderer {

   private ColumnSpec for_column;
   private static final long serialVersionUID = 1;


   CellRenderer(ColumnSpec cs) {
      for_column = cs;
    }

   public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,
						     boolean foc,int r,int c) {
      JLabel lbl = (JLabel) super.getTableCellRendererComponent(t,for_column.convertOutput(v),
								   sel,foc,r,c);
      lbl.setHorizontalAlignment(for_column.getHorizontalAlignment());
      return lbl;
    }

}	// end of subclass CellRenderer




}	// end of class DyvisionTableSpec




/* end of DyvisionTableSpec.java */
