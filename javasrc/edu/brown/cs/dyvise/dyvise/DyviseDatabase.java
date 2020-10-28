/********************************************************************************/
/*										*/
/*		DyviseDatabase.java						*/
/*										*/
/*	DYVISE common methods for database access				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyvise/DyviseDatabase.java,v 1.8 2013/09/04 18:36:37 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyviseDatabase.java,v $
 * Revision 1.8  2013/09/04 18:36:37  spr
 * Minor bug fixes.
 *
 * Revision 1.7  2011-04-01 23:09:23  spr
 * Code clean up.
 *
 * Revision 1.6  2010-06-01 15:40:28  spr
 * Handle booleans in mysql correctly.
 *
 * Revision 1.5  2010-06-01 02:46:05  spr
 * Minor bug fixes.
 *
 * Revision 1.4  2010-03-30 16:23:41  spr
 * Minor changes to accommodate different database systems.
 *
 * Revision 1.3  2009-10-07 01:00:23  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.2  2009-09-27 23:57:44  spr
 * Updates for mac os/x.
 *
 * Revision 1.1  2009-09-19 00:14:44  spr
 * Common files for use throughout the system.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dyvise;


import edu.brown.cs.ivy.file.IvyDatabase;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.project.IvyProject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;



public class DyviseDatabase implements DyviseConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Connection	sql_conn;
private Statement	cur_batch;

private static MessageDigest msg_digest;


static {
   try {
      msg_digest = MessageDigest.getInstance("MD5");
    }
   catch (NoSuchAlgorithmException e) {
      System.err.println("DYVISE: Can't create MD5 message digest");
      System.exit(1);
    }
}


/********************************************************************************/
/*										*/
/*	Constructor								*/
/*										*/
/********************************************************************************/

public DyviseDatabase()
{
   sql_conn = null;
   cur_batch = null;
}



/********************************************************************************/
/*										*/
/*	Methods to open the database						*/
/*										*/
/********************************************************************************/

public void connect(String dbnm)
{
   try {
      sql_conn = IvyDatabase.openDatabase(dbnm);
    }
   catch (SQLException e) {
      System.err.println("DYVISE: Problem opening database " + dbnm + ": " + e);
      e.printStackTrace();
      System.exit(1);
    }
}



public void connectDefault()
{
   try {
      sql_conn = IvyDatabase.openDefaultDatabase();
    }
   catch (SQLException e) {
      System.err.println("DYVISE: Problem opening default database: " + e);
      e.printStackTrace();
      System.exit(1);
    }
}



public boolean testConnect(String dbnm)
{
   try {
      sql_conn = IvyDatabase.openDatabase(dbnm);
    }
   catch (SQLException e) {
      return false;
    }

   return true;
}




public void close()
{
   if (sql_conn != null) {
      try {
	 sql_conn.close();
       }
      catch (SQLException e) { }
      sql_conn = null;
    }
}




/********************************************************************************/
/*										*/
/*	Batch execution commands						*/
/*										*/
/********************************************************************************/

public void addSql(String s)
{
   try {
      if (cur_batch == null) {
	 cur_batch = sql_conn.createStatement();
	 cur_batch.clearBatch();
       }
      cur_batch.addBatch(s);
    }
   catch (SQLException e) {
      System.err.println("DYVISE: Problem with SQL command: " + s + ": " + e);
      e.printStackTrace();
      for (SQLException ne = e.getNextException(); ne != null; ne = ne.getNextException()) {
	 System.err.println("DYVISE: Cause: " + ne);
       }
      System.exit(1);
    }
}



public void runSql()
{
   if (cur_batch == null) return;

   try {
      cur_batch.executeBatch();
      cur_batch.close();
    }
   catch (SQLException e) {
      System.err.println("DYVISE: Problem executing SQL commands: " + e);
      e.printStackTrace();
      for (SQLException ne = e.getNextException(); ne != null; ne = ne.getNextException()) {
	 System.err.println("DYVISE: Cause: " + ne);
       }
      System.exit(1);
    }

   cur_batch = null;
}



public void runSql(String s)
{
   try {
      Statement st = sql_conn.createStatement();
      st.execute(s);
      st.close();
    }
   catch (SQLException e) {
      System.err.println("DYVISE: Problem executing SQL commands: " + e);
      e.printStackTrace();
      for (SQLException ne = e.getNextException(); ne != null; ne = ne.getNextException()) {
	 System.err.println("DYVISE: Cause: " + ne);
       }
      System.exit(1);
    }
}



public boolean trySql(String s)
{
   try {
      Statement st = sql_conn.createStatement();
      st.execute(s);
      st.close();
    }
   catch (SQLException e) {
      return false;
    }

   return true;
}



/********************************************************************************/
/*										*/
/*	Database naming methods 						*/
/*										*/
/********************************************************************************/

public static String getDatabaseName(Map<String,String> names)
{
   String nm = IvyFile.expandName(DYVISE_DATABASE_NAME,names);
   nm = nm.replace("-","_");

   return nm;
}


public static String getDatabaseName(String proj)
{
   Map<String,String> names = new HashMap<String,String>();
   names.put("PROJECT",proj);
   return getDatabaseName(names);
}


public static String getDatabaseName(IvyProject proj)
{
   Map<String,String> names = new HashMap<String,String>();
   names.put("PROJECT",proj.getName());
   return getDatabaseName(names);
}



/********************************************************************************/
/*										*/
/*	Methods for creating insertions 					*/
/*										*/
/********************************************************************************/

public String beginInsert(String tbl)
{
   return "INSERT INTO " + tbl + " VALUES(";
}


public String addValue(String cmd,String val)
{
   if (cmd.endsWith("(")) cmd += sqlString(val);
   else cmd += "," + sqlString(val);

   return cmd;
}


public String addValue(String cmd,int val)
{
   if (cmd.endsWith("(")) cmd += Integer.toString(val);
   else cmd += "," + Integer.toString(val);

   return cmd;
}


public String addValue(String cmd,boolean val)
{
   String v = IvyDatabase.getBooleanValue(val);

   if (cmd.endsWith("(")) cmd += v;
   else cmd += "," + v;

   return cmd;
}



public String addValue(String cmd,float val)
{
   if (cmd.endsWith("(")) cmd += Float.toString(val);
   else cmd += "," + Float.toString(val);

   return cmd;
}



public String addValue(String cmd,double val)
{
   if (cmd.endsWith("(")) cmd += Double.toString(val);
   else cmd += "," + Double.toString(val);

   return cmd;
}



public String addNull(String cmd)
{
   if (cmd.endsWith("(")) cmd += "NULL";
   else cmd += ",NULL";

   return cmd;
}




public void endInsert(String cmd)
{
   cmd = cmd + ")";

   addSql(cmd);
}



/********************************************************************************/
/*										*/
/*	Methods for creating deletions						*/
/*										*/
/********************************************************************************/

public void deleteAll(String tbl)
{
   String cmd = "DELETE FROM " + tbl;
   addSql(cmd);
}


public void dropTable(String tbl)
{
   String cmd = "DROP TABLE " + tbl;
   addSql(cmd);
}


public void createTable(String tbl,String flds)
{
   String cmd = "CREATE TABLE " + tbl + "( " + flds + ")";
   addSql(cmd);
}



public String createTable(String tbl,String start,String flds)
{
   String tnm = getTableName(tbl,start);

   try {
      ResultSet rs = testQuery("SELECT * FROM " + tnm + " LIMIT 1");
      rs.close();
      deleteAll(tnm);
      return tnm;
    }
   catch (SQLException e) { }

   createTable(tnm,flds);

   runSql();

   return tnm;
}




public void dropView(String tbl)
{
   String cmd = "DROP VIEW " + tbl;
   addSql(cmd);
}


public void createView(String tbl,String q)
{
   String cmd = "CREATE VIEW " + tbl + " AS " + q;
   addSql(cmd);
}




/********************************************************************************/
/*										*/
/*	Method to execute a query						*/
/*										*/
/********************************************************************************/

public ResultSet query(String q)
{
   try {
      Statement st = sql_conn.createStatement();

      return st.executeQuery(q);
    }
   catch (SQLException e) {
      System.err.println("DYVISE: Problem executing SQL query `" + q + "': " + e);
      e.printStackTrace();
      System.exit(1);
    }

   return null;
}



public ResultSet testQuery(String q) throws SQLException
{
   Statement st = sql_conn.createStatement();

   return st.executeQuery(q);
}



/********************************************************************************/
/*										*/
/*	File loading methods							*/
/*										*/
/********************************************************************************/

public boolean getSupportsFiles()
{
   return IvyDatabase.getSupportsFiles();
}


public void loadTableFromFile(String tbl,String file)
{
   try {
      IvyDatabase.loadTableFromFile(sql_conn,tbl,file);
    }
   catch (SQLException e) {
      System.err.println("DYVISE: Problem loading table from file: " + e);
      System.exit(1);
    }
}


public void loadTableFromCsvFile(String tbl,String file) throws SQLException
{
   try {
      IvyDatabase.loadTableFromCsvFile(sql_conn,tbl,file);
    }
   catch (SQLException e) {
      System.err.println("DYVISE: Problem loading table from csv file: " + e);
      System.exit(1);
    }
}



/********************************************************************************/
/*										*/
/*	Time methods								*/
/*										*/
/********************************************************************************/

public void updateTime(String what,String main)
{
   SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
   String curd = sdf.format(new Date());

   String ws = sqlString(what);
   String ms = sqlString(main);

   String q0 = "SELECT lastupdated FROM UpdateTimes WHERE what = " + ws;
   if (main != null) q0 += " AND main = " + ms;

   String u0 = "INSERT INTO UpdateTimes VALUES ( " + ws + ", " + ms + ", '" + curd + "' )";
   try {
      ResultSet rs = query(q0);
      if (rs.next()) {
	 u0 = "UPDATE UpdateTimes SET lastupdated = '" + curd + "' WHERE what = '" + what + "'";
	 if (main != null) u0 += " AND main = " + ms;
       }
    }
   catch (SQLException e) {
      System.err.println("DYVISE: Problem checking update time: " + e);
    }

   addSql(u0);
   runSql();
}




public Date getTime(String what,String main)
{
   String ws = sqlString(what);
   String ms = sqlString(main);

   String q0 = "SELECT lastupdated FROM UpdateTimes WHERE what = " + ws;
   if (main != null) q0 += " AND main = " + ms;
   ResultSet rs = query(q0);

   try {
      if (rs.next()) {
	 String ts = rs.getString(1);
	 SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	 Date d0 = sdf.parse(ts);
	 return d0;
       }
    }
   catch (SQLException e) {
      System.err.println("DYVISE: Problem getting update time: " + e);
    }
   catch (ParseException e) {
      System.err.println("DYVISE: Date parsing problem: " + e);
    }

   return null;
}




/********************************************************************************/
/*										*/
/*	Formatting methods							*/
/*										*/
/********************************************************************************/

public static String sqlString(String s)
{
   if (s == null || s.length() == 0) return "NULL";

   if (s.indexOf("'") >= 0) {
      s = s.replaceAll("'","''");
    }
   s = "'" + s + "'";

   return s;
}




/********************************************************************************/
/*										*/
/*	Table naming methods							*/
/*										*/
/********************************************************************************/

public static String getTableName(String pfx,String cls)
{
   if (cls == null) return pfx;

   String rslt = pfx + "_" + getId(cls);

   // System.err.println("TABLE NAME " + pfx + " " + cls + " "+ rslt);

   return rslt;
}



public void saveId(String cls)
{
   String id = getId(cls);
   String v0 = sqlString(cls);

   String u0 = "DELETE FROM NameMaps WHERE main = " + v0;
   addSql(u0);
   String u1 = "INSERT INTO NameMaps VALUES ( " + v0 + "," + sqlString(id) + ")";
   addSql(u1);
   runSql();
}




private static String getId(String cls)
{
   byte [] d = msg_digest.digest(cls.getBytes());
   long vl = 0;
   for (int i = 0; i < 4; ++i) {
      long vb = d[i]&0xff;
      vl = (vl << 8) + vb;
    }

   StringBuffer buf = new StringBuffer();
   while (vl > 0) {
      int c = (int) (vl % 36);
      vl = vl/36;
      char ch;
      if (c < 10) ch = ((char)('0' + c));
      else ch = ((char)('a' + c - 10));
      buf.append(ch);
    }

   return buf.toString();
}





}	// end of class DyviseDatabase
