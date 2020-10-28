/********************************************************************************/
/*										*/
/*		DycompAnalysisClosure.java					*/
/*										*/
/*	DYVISE computed relations analysis for transitive closure		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dycomp/DycompAnalysisClosure.java,v 1.2 2009-10-07 00:59:39 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DycompAnalysisClosure.java,v $
 * Revision 1.2  2009-10-07 00:59:39  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.1  2009-09-19 00:08:21  spr
 * Code to compute relations and store them in the database.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dycomp;


import edu.brown.cs.dyvise.dyvise.DyviseException;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;



class DycompAnalysisClosure extends DycompAnalysis implements DycompConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<String,Target> target_items;
private List<Target>	target_list;
private List<Source>	source_items;
private List<Compute>	compute_items;
private Target		from_field;
private Target		to_field;
private boolean 	do_self;
private boolean 	is_undirected;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DycompAnalysisClosure(DycompMain dm)
{
   super(dm);
}



DycompAnalysisClosure(DycompAnalysisClosure proto,Element xml) throws DyviseException
{
   super(proto,xml);

   source_items = new ArrayList<Source>();
   compute_items = new ArrayList<Compute>();
   target_list = new ArrayList<Target>();
   target_items = new HashMap<String,Target>();
   for (Element c : IvyXml.children(xml,"TARGET")) {
      Target t = new Target(c);
      target_list.add(t);
      target_items.put(t.getName(),t);
    }

   String fnm = IvyXml.getTextElement(xml,"FROM");
   from_field = target_items.get(fnm);
   String tnm = IvyXml.getTextElement(xml,"TO");
   to_field = target_items.get(tnm);
   if (from_field == null || to_field == null) throw new DyviseException("Missing FROM or TO");

   for (Element c : IvyXml.children(xml,"SOURCE")) {
      source_items.add(new Source(c));
    }

   for (Element c : IvyXml.children(xml,"EVAL")) {
      compute_items.add(new Compute(c));
    }

   do_self = IvyXml.getAttrBool(xml,"REFLEXIVE");
   is_undirected = IvyXml.getAttrBool(xml,"UNDIRECTED");
}




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

DycompAnalysis createNew(Element xml) throws DyviseException
{
   DycompAnalysisClosure da = new DycompAnalysisClosure(this,xml);

   return da;
}



/********************************************************************************/
/*										*/
/*	Work methods								*/
/*										*/
/********************************************************************************/

void compute() throws DyviseException
{
   List<TupleData> data = new ArrayList<TupleData>();
   for (Source s : source_items) loadData(s,data);

   computeClosure(data);
   insertData(data);
}



private void loadData(Source s,List<TupleData> rslt) throws DyviseException
{
   String q = s.getQuery();

   ResultSet rs = comp_main.query(q);

   if (rs == null) return;

   try {
      while (rs.next()) {
	 TupleData td = new TupleData();

	 for (Target t : target_list) {
	    String fvl = rs.getString(t.getName());
	    td.setField(t,fvl);
	  }

	 rslt.add(td);
       }

      rs.close();
    }
   catch (SQLException e) {
      throw new DyviseException("SQL error: " + e.getMessage());
    }

   return;
}



private void insertData(List<TupleData> rslt) throws DyviseException
{
   comp_main.deleteAll(output_table);
   
   for (TupleData td : rslt) {
      String cmd = comp_main.beginInsert(output_table);
      for (Target t : target_list) {
         String x = t.getType();
         String v = td.getField(t);
         if (v == null) cmd = comp_main.addNull(cmd);
         else {
            if (x.equals("INT")) {
               Integer ivl = null;
               try {
                  ivl = Integer.valueOf(v);
                }
               catch (NumberFormatException e) { }
               if (ivl == null) cmd = comp_main.addNull(cmd);
               else cmd = comp_main.addValue(cmd,ivl.intValue());
             }
            else if (x.equals("BOOLEAN")) {
               boolean bv = false;
               if (v.length() > 0 &&
                     (v.charAt(0) == 'T' || v.charAt(0) == 't' || v.charAt(0) == '1'))
                  bv = true;
               cmd = comp_main.addValue(cmd,bv);
             }
            else {
               cmd = comp_main.addValue(cmd,v);
             }
          }
       }
      comp_main.endInsert(cmd);
    }
   
   comp_main.runSql();
}





/********************************************************************************/
/*										*/
/*	Transitive closure method						*/
/*										*/
/********************************************************************************/

private void computeClosure(List<TupleData> data)
{
   Map<String,Map<String,TupleData>> tcmap = new HashMap<String,Map<String,TupleData>>();
   for (TupleData td : data) {
      String fvl = td.getField(from_field);
      String tvl = td.getField(to_field);
      addEntry(fvl,tvl,td,tcmap);
      if (is_undirected) addEntry(tvl,fvl,td,tcmap);
    }

   boolean chng = true;
   while (chng) {
      chng = false;
      List<TupleData> ntbl = new ArrayList<TupleData>(data);
      for (TupleData td : ntbl) {
	 String fvl = td.getField(from_field);
	 String tvl = td.getField(to_field);
	 chng |= extendEntry(data,td,fvl,tvl,tcmap);
	 if (is_undirected) chng |= extendEntry(data,td,tvl,fvl,tcmap);
       }
    }

   if (do_self) {
      for (Map.Entry<String,Map<String,TupleData>> ent : tcmap.entrySet()) {
	 String fvl = ent.getKey();
	 Map<String,TupleData> m = ent.getValue();
	 if (m.get(fvl) == null) {
	    TupleData td = buildSelfData(data,fvl);
	    m.put(fvl,td);
	  }
       }
    }
}



private void addEntry(String fvl,String tvl,TupleData td,Map<String,Map<String,TupleData>> tcmap)
{
   Map<String,TupleData> m = tcmap.get(fvl);
   if (m == null) {
      m = new HashMap<String,TupleData>();
      tcmap.put(fvl,m);
    }
   m.put(tvl,td);
}


private boolean extendEntry(List<TupleData> rslt,TupleData td,String fvl,String tvl,Map<String,Map<String,TupleData>> tcmap)
{
   boolean chng = false;

   Map<String,TupleData> fmp = tcmap.get(fvl);
   Map<String,TupleData> rvl = tcmap.get(tvl);
   if (rvl == null) return false;

   for (Map.Entry<String,TupleData> ent : rvl.entrySet()) {
      String ntvl = ent.getKey();
      if (fmp.get(ntvl) != null) continue;
      TupleData td1 = ent.getValue();
      TupleData td2 = buildNewData(rslt,td,td1);
      fmp.put(ntvl,td2);
      chng = true;
    }

   return chng;
}


private TupleData buildNewData(List<TupleData> rslt,TupleData t1,TupleData t2)
{
   TupleData td = new TupleData();
   td.setField(from_field,t1.getField(from_field));
   td.setField(to_field,t2.getField(to_field));

   for (Compute c : compute_items) {
      Target cf = c.getField();
      String vl = c.getValue(t1.getField(cf),t2.getField(cf));
      if (vl != null) td.setField(cf,vl);
    }

   rslt.add(td);

   return td;
}


private TupleData buildSelfData(List<TupleData> rslt,String vl)
{
   TupleData td = new TupleData();
   td.setField(from_field,vl);
   td.setField(to_field,vl);

   for (Compute c : compute_items) {
      Target cf = c.getField();
      String sv = c.getSelfValue();
      if (sv != null) td.setField(cf,sv);
    }

   rslt.add(td);

   return td;
}




/********************************************************************************/
/*										*/
/*	Methods for representing the target					*/
/*										*/
/********************************************************************************/

private class Target {

   private String field_name;
   private String field_type;

   Target(Element xml) {
      field_name = IvyXml.getTextElement(xml,"NAME");
      field_type = IvyXml.getTextElement(xml,"TYPE");
    }

   String getName()			{ return field_name; }
   String getType()			{ return field_type; }

}	// end of interface Target




/********************************************************************************/
/*										*/
/*	Representations for source of the closure				*/
/*										*/
/********************************************************************************/

private class Source {

   private String source_id;
   private String target_id;
   private String relation_name;
   private String query_name;
   private String query_where;
   private Map<String,SourceItem> field_mappings;

   Source(Element xml) throws DyviseException {
      relation_name = getTableName(xml,"RELATION");
      field_mappings = new HashMap<String,SourceItem>();
      source_id = null;
      target_id = null;
      for (Element n : IvyXml.children(xml)) {
	 if (IvyXml.isElement(n,"MAP")) {
	    String t = IvyXml.getTextElement(n,"TARGET");
	    String v = IvyXml.getTextElement(n,"VALUE");
	    String s = IvyXml.getTextElement(n,"SOURCE");
	    if (t.equals(from_field.getName())) source_id = s;
	    else if (t.equals(to_field.getName())) target_id = s;
	    if (s != null) field_mappings.put(t,new SourceField(s));
	    else if (v != null) field_mappings.put(t,new SourceValue(v));
	    else throw new DyviseException("Transitive mapping must specify source or value");
	  }
	 else if (IvyXml.isElement(n,"WHERE")) {
	    query_where = IvyXml.getText(n).trim();
	  }
       }
    }

   String getQuery() throws DyviseException {
      StringBuffer buf = new StringBuffer();
      buf.append("SELECT ");
      int ctr = 0;
      for (Target f : target_list) {
	 SourceItem itm = field_mappings.get(f.getName());
	 if (itm == null) throw new DyviseException("Field " + f.getName() + " not defined for source");
	 if (ctr++ != 0) buf.append(",");
	 buf.append(itm.getValue());
	 buf.append(" AS ");
	 buf.append(f.getName());
       }
      buf.append(" FROM ");
      buf.append(relation_name);
      if (query_name != null) buf.append(" " + query_name);

      if (source_id != null || target_id != null || query_where != null) {
	 buf.append(" WHERE ");
	 if (source_id != null) buf.append(source_id + " IS NOT NULL");
	 if (source_id != null && target_id != null) buf.append(" AND ");
	 if (target_id != null) buf.append(target_id + " IS NOT NULL");
	 if (query_where != null) {
	    if (source_id != null || target_id != null) buf.append(" AND ");
	    buf.append("( " + query_where + " )");
	  }
       }

      return buf.toString();
    }

}	// end of subclass Source




private interface SourceItem {

   String getValue();

}	// end of interface SourceItem




private class SourceField implements SourceItem {

   private String field_name;

   SourceField(String nm) {
      field_name = nm;
    }

   public String getValue()			{ return field_name; }

}	// end of subclass SourceField




private class SourceValue  implements SourceItem {

   private String value_text;

   SourceValue(String v) {
      value_text = v;
    }

   public String getValue()			{ return value_text; }

}	// end of subclass SourceValue




/********************************************************************************/
/*										*/
/*	Subclasses for handling computed fields 				*/
/*										*/
/********************************************************************************/

private class Compute {

   private Target for_field;
   private String operator_name;
   private String self_value;

   Compute(Element xml) throws DyviseException {
      String fnm = IvyXml.getTextElement(xml,"FIELD");
      operator_name = IvyXml.getTextElement(xml,"OPERATOR");
      self_value = IvyXml.getTextElement(xml,"SELF");
      if (fnm == null || operator_name == null)
	 throw new DyviseException("Field and operator must be given for a computed field");
      for_field = target_items.get(fnm);
      if (for_field == null) throw new DyviseException("Computed field must be defined");
    }

   Target getField()			  { return for_field; }

   String getValue(String v1,String v2) {
      if (operator_name.equals("AND")) {
	 boolean b = booleanValue(v1) && booleanValue(v2);
	 return (b ? "true" : "false");
       }
      else if (operator_name.equals("OR")) {
	 boolean b = booleanValue(v1) || booleanValue(v2);
	 return (b ? "true" : "false");
       }
      else if (operator_name.equals("PLUS")) {
	 int v = intValue(v1) + intValue(v2);
	 return String.valueOf(v);
       }

      return null;
    }

   String getSelfValue()				{ return self_value; }

   private boolean booleanValue(String v) {
      if (v == null) return false;
      if (v.startsWith("t") || v.startsWith("T") || v.startsWith("1")) return true;
      return false;
    }

   private int intValue(String v) {
      if (v == null) return 0;
      try {
	 return Integer.parseInt(v);
       }
      catch (NumberFormatException _e) { }
      return 0;
    }

}	// end of subclass Compute





/********************************************************************************/
/*										*/
/*	Subclass to hold data for a tuple					*/
/*										*/
/********************************************************************************/

protected static class TupleData {

   private Map<Target,String> tuple_data;

   TupleData() {
      tuple_data = new HashMap<Target,String>();
    }

   void setField(Target fnm,String fvl) 	{ tuple_data.put(fnm,fvl); }
   String getField(Target f)			{ return tuple_data.get(f); }

}	// end of subclass TupleData




}	// end of class DycompAnalysisClosure




/* end of DycompAnalysisClosure.java */


