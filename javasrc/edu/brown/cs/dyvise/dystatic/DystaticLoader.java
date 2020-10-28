/********************************************************************************/
/*										*/
/*		DystaticLoader.java						*/
/*										*/
/*	DYVISE static analysis ASM to database loader				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dystatic/DystaticLoader.java,v 1.7 2011-03-10 02:33:17 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DystaticLoader.java,v $
 * Revision 1.7  2011-03-10 02:33:17  spr
 * Code cleanup.
 *
 * Revision 1.6  2010-08-27 17:00:41  spr
 * Remove asm-based loader (use the bt one)
 *
 * Revision 1.5  2010-06-01 15:46:37  spr
 * Write booleans to file correctly for MYSQL
 *
 * Revision 1.4  2010-06-01 15:35:00  spr
 * Updates for mysql.
 *
 * Revision 1.3  2010-03-30 16:22:43  spr
 * Minor bug fixes and clean up.
 *
 * Revision 1.2  2009-10-07 01:00:19  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.1  2009-09-19 00:13:48  spr
 * Static analyzer storing info in the database.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dystatic;

import edu.brown.cs.dyvise.dyvise.DyviseDatabase;
import edu.brown.cs.ivy.project.IvyProject;
import edu.brown.cs.ivy.file.IvyDatabase;

import java.io.*;
import java.lang.reflect.Modifier;
import java.util.*;



public abstract class DystaticLoader implements DystaticConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private   DystaticMain	  for_main;
private   IvyProject	  user_project;
private   DyviseDatabase  sql_database;
private   int		  id_counter;
private   boolean	  use_files;
private   Random	  random_gen;
private   Map<String,PrintWriter> file_writers;
private   Map<String,File> file_names;


private static String []  table_names = new String [] {
   "SrcClass",
   "SrcInterface",
   "SrcField",
   "SrcMethod",
   "SrcMethodParam",
   "SrcCall",
   "SrcLines",
   "SrcAlloc"
};



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DystaticLoader(DystaticMain dm)
{
   for_main = dm;
   user_project = for_main.getProject();

   sql_database = new DyviseDatabase();
   sql_database.connect(for_main.getDatabaseName());

   sql_database.updateTime(DYSTATIC_UPDATE_LABEL,null);

   use_files = sql_database.getSupportsFiles();

   random_gen = new Random();
   file_writers = null;
   file_names = null;

   id_counter = 0;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

protected IvyProject getProject()		{ return user_project; }

protected File getSourceFile(String cls)
{
   return user_project.findSourceFile(cls);
}


protected String getAlternateClass(String cls)
{
   return for_main.getMappedClass(cls);
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

abstract void processLoad();

protected void startLoad()
{
   clearTables();

   if (use_files) {
      file_writers = null;
      file_names = null;
      File f = new File(DYSTATIC_FILE_DIR);
      File f1 = null;
      if (!f.exists()) f.mkdirs();
      for (int i = 0; i < 100; ++i) {
	 int rn = random_gen.nextInt(100000);
	 f1 = new File(f,"DS_" + rn);
	 if (!f1.exists()) {
	    if (f1.mkdir()) break;
	    f1 = null;
	  }
       }
      if (f1 != null) {
	 f1.deleteOnExit();
	 file_writers = new HashMap<String,PrintWriter>();
	 file_names = new HashMap<String,File>();
	 file_names.put("DIRECTORY",f1);
	 for (String tn : table_names) {
	    try {
	       File f2 = new File(f1,tn);
	       PrintWriter pw = new PrintWriter(new FileWriter(f2));
	       f2.deleteOnExit();
	       file_names.put(tn,f2);
	       file_writers.put(tn,pw);
	     }
	    catch (IOException e) {
	       System.err.println("DYSTATIC: Problem creating data file: " + e);
	     }
	  }
	 if (file_writers.size() != table_names.length) file_writers = null;
       }
      if (file_writers == null) use_files = false;
    }
}



protected void finishLoad()
{
   if (use_files) {
      for (String tn : table_names) {
	 PrintWriter pw = file_writers.get(tn);
	 pw.close();
	 File f = file_names.get(tn);
	 sql_database.loadTableFromFile(tn,f.getPath());
	 f.delete();
       }
      File f1 = file_names.get("DIRECTORY");
      f1.delete();
    }

   sql_database.runSql();
}




/********************************************************************************/
/*										*/
/*	Id routines								*/
/*										*/
/********************************************************************************/

protected String getNewId(String pfx)
{
   return pfx + (++id_counter);
}



/********************************************************************************/
/*										*/
/*	Database routines							*/
/*										*/
/********************************************************************************/

protected void clearTables()
{
   for (String tn : table_names) {
      sql_database.deleteAll(tn);
    }

   sql_database.runSql();
}



protected void addClassEntry(String name,String typ,String sign,String sup,int acc,boolean ufg)
{
   if (name != null) name = name.replace('/','.');
   if (sup != null) sup = sup.replace('/','.');

   File srcf = user_project.findSourceFile(name);
   String src = null;
   if (srcf != null) src = srcf.getPath();

   Updater upd = new Updater("SrcClass");
   upd.addValue(name);
   upd.addValue(typ);
   upd.addValue(sup);
   upd.addValue(sign);
   upd.addValue(src);
   upd.addValue(acc);
   upd.addFlag(acc,Modifier.ABSTRACT);
   upd.addFlag(acc,0x4000);				// ENUM
   upd.addFlag(acc,Modifier.FINAL);
   upd.addFlag(acc,Modifier.INTERFACE);
   upd.addFlag(acc,Modifier.PRIVATE);
   upd.addFlag(acc,Modifier.PROTECTED);
   upd.addFlag(acc,Modifier.PUBLIC);
   upd.addFlag(acc,Modifier.STATIC);
   upd.addValue(ufg);
   upd.finish();
}



protected void addInterfaceEntry(String cls,String ifc,boolean sup)
{
   if (cls != null) cls = cls.replace('/','.');
   if (ifc != null) ifc = ifc.replace('/','.');

   Updater upd = new Updater("SrcInterface");
   upd.addValue(cls);
   upd.addValue(ifc);
   upd.addValue(sup);
   upd.finish();
}



protected void addFieldEntry(String id,String cls,String nm,int acc,String desc,String sgn)
{
   if (cls != null) cls = cls.replace('/','.');

   Updater upd = new Updater("SrcField");
   upd.addValue(id);
   upd.addValue(nm);
   upd.addValue(cls);
   upd.addValue(desc);
   upd.addValue(sgn);
   upd.addValue(acc);
   upd.addFlag(acc,Modifier.FINAL);
   upd.addFlag(acc,Modifier.PRIVATE);
   upd.addFlag(acc,Modifier.PROTECTED);
   upd.addFlag(acc,Modifier.PUBLIC);
   upd.addFlag(acc,Modifier.STATIC);
   upd.addFlag(acc,Modifier.TRANSIENT);
   upd.addFlag(acc,Modifier.VOLATILE);
   upd.finish();
}



protected void addMethodEntry(String id,String cls,String nm,int acc,String desc,String sgn,int narg,
			       String ret)
{
   if (cls != null) cls = cls.replace('/','.');

   Updater upd = new Updater("SrcMethod");
   upd.addValue(id);
   upd.addValue(nm);
   upd.addValue(cls);
   upd.addValue(desc);
   upd.addValue(sgn);
   upd.addValue(narg);
   upd.addValue(ret);
   upd.addValue(acc);
   upd.addFlag(acc,Modifier.ABSTRACT);
   upd.addFlag(acc,Modifier.FINAL);
   upd.addFlag(acc,Modifier.NATIVE);
   upd.addFlag(acc,Modifier.PRIVATE);
   upd.addFlag(acc,Modifier.PROTECTED);
   upd.addFlag(acc,Modifier.PUBLIC);
   upd.addFlag(acc,Modifier.STATIC);
   upd.addFlag(acc,Modifier.SYNCHRONIZED);
   upd.addFlag(acc,0x0080);				// VARARGS
   upd.finish();
}



protected void addCallEntry(String frm,String cls,String nm,String desc)
{
   if (cls != null) cls = cls.replace('/','.');

   Updater upd = new Updater("SrcCall");
   upd.addValue(frm);
   upd.addValue(cls);
   upd.addValue(nm);
   upd.addValue(desc);
   upd.finish();
}



protected void addAllocEntry(String mid,String cls)
{
   if (cls != null) cls = cls.replace('/','.');

   Updater upd = new Updater("SrcAlloc");
   upd.addValue(mid);
   upd.addValue(cls);
   upd.finish();
}



protected void addParamEntry(String mid,String nam,String typ,String sgn,int idx)
{
   Updater upd = new Updater("SrcMethodParam");
   upd.addValue(mid);
   upd.addValue(nam);
   upd.addValue(typ);
   upd.addValue(idx);
   upd.finish();
}





protected void addLineEntry(String mid,int line,int idx)
{
   Updater upd = new Updater("SrcLines");
   upd.addValue(mid);
   upd.addValue(line);
   upd.addValue(idx);
   upd.finish();
}



/********************************************************************************/
/*										*/
/*	Value output and normalization methods					*/
/*										*/
/********************************************************************************/

private class Updater {

   private PrintWriter cur_writer;
   private String cur_command;
   private int value_count;

   Updater(String tbl) {
      cur_writer = null;
      cur_command = null;
      if (use_files) cur_writer = file_writers.get(tbl);
      else cur_command = sql_database.beginInsert(tbl);
      value_count = 0;
    }

   void addValue(String val) {
      if (cur_writer == null) cur_command = sql_database.addValue(cur_command,val);
      else if (val == null) addNull();
      else writeValue(safeString(val));
    }

   void addValue(int val) {
      if (cur_writer == null) cur_command = sql_database.addValue(cur_command,val);
      else writeValue(Integer.toString(val));
    }

   void addValue(boolean val) {
      if (cur_writer == null) cur_command = sql_database.addValue(cur_command,val);
      else writeValue(IvyDatabase.getBooleanValue(val));
    }

   void addNull() {
      if (cur_writer == null) cur_command = sql_database.addNull(cur_command);
      else writeValue("\\N");
    }

   void addFlag(int fgs,int fg) {
      boolean v = (fgs & fg) != 0;
      addValue(v);
    }

   void finish() {
      if (cur_writer == null) sql_database.endInsert(cur_command);
      else cur_writer.println();
    }

   private void writeValue(String val) {
      if (value_count++ > 0) cur_writer.print("\t");
      cur_writer.print(val);
    }

   private String safeString(String v) {
      v = v.replaceAll("\t"," ");
      v = v.replaceAll("\n"," ");
      return v;
    }

}	// end of inner class Updater



}	// end of class DystaticLoader




/* end of DystaticLoader.java */
