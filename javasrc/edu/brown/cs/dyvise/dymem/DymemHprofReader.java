/********************************************************************************/
/*										*/
/*		DymemHprofReader.java						*/
/*										*/
/*	Module to read in a hprof file and generate summary xml 		*/
/*										*/
/********************************************************************************/
/*	Copyright 2005 Brown University -- Steven P. Reiss			*/
/*	Copyright 1997-2008 Sun Microsystems, Inc.  All rights reserved.	*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dymem/DymemHprofReader.java,v 1.3 2011-03-10 02:33:07 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DymemHprofReader.java,v $
 * Revision 1.3  2011-03-10 02:33:07  spr
 * Code cleanup.
 *
 * Revision 1.2  2009-10-07 22:39:49  spr
 * Eclipse code cleanup.
 *
 * Revision 1.1  2009-09-19 00:09:27  spr
 * Update dymem with some bug fixes; initial support for reading dump files.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dymem;




import java.io.*;
import java.util.*;



public class DymemHprofReader {




/********************************************************************************/
/*										*/
/*	Constants defining the hprof file format				*/
/*										*/
/********************************************************************************/

private static final int HPROF_UTF8	     = 0x01;
private static final int HPROF_LOAD_CLASS    = 0x02;
private static final int HPROF_UNLOAD_CLASS  = 0x03;
private static final int HPROF_FRAME	     = 0x04;
private static final int HPROF_TRACE	     = 0x05;
private static final int HPROF_ALLOC_SITES   = 0x06;
private static final int HPROF_HEAP_SUMMARY  = 0x07;

private static final int HPROF_START_THREAD  = 0x0a;
private static final int HPROF_END_THREAD    = 0x0b;

private static final int HPROF_HEAP_DUMP     = 0x0c;

private static final int HPROF_CPU_SAMPLES   = 0x0d;
private static final int HPROF_CONTROL_SETTINGS = 0x0e;
private static final int HPROF_LOCKSTATS_WAIT_TIME = 0x10;
private static final int HPROF_LOCKSTATS_HOLD_TIME = 0x11;

private static final int HPROF_GC_ROOT_UNKNOWN	     = 0xff;
private static final int HPROF_GC_ROOT_JNI_GLOBAL    = 0x01;
private static final int HPROF_GC_ROOT_JNI_LOCAL     = 0x02;
private static final int HPROF_GC_ROOT_JAVA_FRAME    = 0x03;
private static final int HPROF_GC_ROOT_NATIVE_STACK  = 0x04;
private static final int HPROF_GC_ROOT_STICKY_CLASS  = 0x05;
private static final int HPROF_GC_ROOT_THREAD_BLOCK  = 0x06;
private static final int HPROF_GC_ROOT_MONITOR_USED  = 0x07;
private static final int HPROF_GC_ROOT_THREAD_OBJ    = 0x08;

private static final int HPROF_GC_CLASS_DUMP	     = 0x20;
private static final int HPROF_GC_INSTANCE_DUMP      = 0x21;
private static final int HPROF_GC_OBJ_ARRAY_DUMP	 = 0x22;
private static final int HPROF_GC_PRIM_ARRAY_DUMP	  = 0x23;

private static final int HPROF_HEAP_DUMP_SEGMENT     = 0x1c;
private static final int HPROF_HEAP_DUMP_END	     = 0x2c;


private final static int T_CLASS = 2;


// array type codes
private static final int T_BOOLEAN = 4;
private static final int T_CHAR    = 5;
private static final int T_FLOAT   = 6;
private static final int T_DOUBLE  = 7;
private static final int T_BYTE    = 8;
private static final int T_SHORT   = 9;
private static final int T_INT	   = 10;
private static final int T_LONG    = 11;



/********************************************************************************/
/*										*/
/*	Constants for the reference output					*/
/*										*/
/********************************************************************************/

private static final Long ROOT_CLASS = -3L;
private static final Long SYSTEM_CLASS = -5L;



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private String file_name;
private DataInputStream in_stream;

private long curr_pos;
private int id_size;

private Map<Long,String> name_map;
private Map<Integer,ThreadObject> thread_objects;
private Map<Long,ClassObject> class_objects;
private Map<Byte,ClassObject> class_arrays;

private Map<Long,ClassObject> object_types;
private Map<Long,Object> object_refs;
private Map<Long,Object> root_refs;
private List<Long> real_roots;
private Map<Long,Integer> array_sizes;

private int		pass_number;

private boolean 	show_system;
private boolean 	show_thread;
private boolean 	show_classes;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public DymemHprofReader(String filename) throws IOException
{
   file_name = filename;
   in_stream = null;

   name_map = new HashMap<Long, String>();
   thread_objects = new HashMap<Integer, ThreadObject>(43);
   class_objects = new HashMap<Long,ClassObject>();
   class_arrays = new HashMap<Byte,ClassObject>();

   setupDump();

   pass_number = 0;

   show_classes = true;
   show_system = true;
   show_thread = true;
}




/********************************************************************************/
/*										*/
/*	Reading methods 							*/
/*										*/
/********************************************************************************/

public void readHprof() throws IOException
{
   readHprofPass(1);
}



public void readHprofPass(int pass) throws IOException
{
   in_stream = new DataInputStream(new BufferedInputStream(new FileInputStream(file_name),1024*1024));
   pass_number = pass;

   long id,nid;
   byte [] chars;

   curr_pos = 4;
   readVersionHeader();
   id_size = in_stream.readInt();
   curr_pos += 4;
   in_stream.readLong();
   curr_pos += 8;

   for ( ; ; ) {
      int type;
      try {
	 type = in_stream.readUnsignedByte();
       }
      catch (EOFException _e) {
	 break;
       }
      in_stream.readInt();	       // timestamp
      long length = in_stream.readInt() & 0xffffffffL;
      curr_pos += 9 + length;
      switch (type) {
	 case HPROF_UTF8 :
	    id = readID();
	    chars = new byte[(int)length - id_size];
	    in_stream.readFully(chars);
	    if (firstPass()) {
	       name_map.put(id,new String(chars));
	     }
	    break;
	 case HPROF_LOAD_CLASS :
	    in_stream.readInt();	// serial number
	    id = readID();
	    in_stream.readInt();	// stack trace
	    nid = readID();
	    if (firstPass()) {
	       String nm = name_map.get(nid).replace('/','.');
	       defineClass(id,nm);
	     }
	    break;
	 case HPROF_HEAP_DUMP :
	 case HPROF_HEAP_DUMP_SEGMENT :
	    try {
	       readHeapDump(length,curr_pos);
	     }
	    catch (EOFException exp) {
	       // handle eof
	     }
	    break;
	 case HPROF_HEAP_DUMP_END :
	    finishDump();
	    skipBytes(length);
	    break;
	 case HPROF_FRAME :
	 case HPROF_TRACE :
	 case HPROF_UNLOAD_CLASS:
	 case HPROF_ALLOC_SITES:
	 case HPROF_START_THREAD:
	 case HPROF_END_THREAD:
	 case HPROF_HEAP_SUMMARY:
	 case HPROF_CPU_SAMPLES:
	 case HPROF_CONTROL_SETTINGS:
	 case HPROF_LOCKSTATS_WAIT_TIME:
	 case HPROF_LOCKSTATS_HOLD_TIME:
	 default :
	    skipBytes(length);
	    break;
       }
   }

   if (pass_number == 1) {
      name_map.clear();
    }

   in_stream.close();
}



private void skipBytes(long len) throws IOException
{
   if (len == 0) return;

   in_stream.skipBytes((int) len);
}


private void readVersionHeader() throws IOException
{
   for ( ; ; ) {
      int b = in_stream.readByte();
      ++curr_pos;
      if (b == 0) break;
    }
}



private boolean firstPass()
{
   return pass_number != 2;
}


private boolean secondPass()
{
   return pass_number != 1;
}


/********************************************************************************/
/*										*/
/*	Heap Dump reader							*/
/*										*/
/********************************************************************************/

private void readHeapDump(long bytesleft,long endpos) throws IOException
{
   long id;
   int seq0;
   ThreadObject to;

   while (bytesleft > 0) {
      int type = in_stream.readUnsignedByte();
      --bytesleft;
      switch (type) {
	 case HPROF_GC_ROOT_UNKNOWN :
	    id = readID();
	    bytesleft -= id_size;
	    if (secondPass()) {
	       if (show_system) noteRoot(SYSTEM_CLASS,id);
	       else noteRoot(id);
	     }
	    break;
	 case HPROF_GC_ROOT_THREAD_OBJ :
	    id = readID();
	    seq0 = in_stream.readInt(); 	// thread sequence
	    in_stream.readInt();		// stack sequence
	    bytesleft -= id_size + 8;
	    if (firstPass()) thread_objects.put(seq0,new ThreadObject(id));
	    break;
	 case HPROF_GC_ROOT_JNI_GLOBAL :
	    id = readID();
	    readID();				// global ref
	    bytesleft -= 2*id_size;
	    if (secondPass()) noteRoot(ROOT_CLASS,id);
	    break;
	 case HPROF_GC_ROOT_JNI_LOCAL :
	    id = readID();
	    seq0 = in_stream.readInt();
	    in_stream.readInt();		// depth
	    bytesleft -= id_size + 8;
	    if (secondPass()) {
	       if (show_thread) {
		  to = thread_objects.get(seq0);
		  noteRoot(to.getId(),id);
		}
	       else noteRoot(id);
	     }
	    break;
	 case HPROF_GC_ROOT_JAVA_FRAME :
	    id = readID();
	    seq0 = in_stream.readInt();
	    in_stream.readInt();		// depth
	    bytesleft -= id_size + 8;
	    if (secondPass()) {
	       if (show_thread) {
		  to = thread_objects.get(seq0);
		  noteRoot(to.getId(),id);
		}
	       else noteRoot(id);
	     }
	    break;
	 case HPROF_GC_ROOT_NATIVE_STACK :
	    id = readID();
	    seq0 = in_stream.readInt();
	    bytesleft -= id_size + 4;
	    if (secondPass()) {
	       if (show_thread) {
		  to = thread_objects.get(seq0);
		  noteRoot(to.getId(),id);
		}
	       else noteRoot(id);
	     }
	    break;
	 case HPROF_GC_ROOT_STICKY_CLASS :
	    id = readID();
	    bytesleft -= id_size;
	    if (secondPass()) {
	       if (show_system) noteRoot(SYSTEM_CLASS,id);
	       else noteRoot(id);
	     }
	    break;
	 case HPROF_GC_ROOT_THREAD_BLOCK :
	    id = readID();
	    seq0 = in_stream.readInt();
	    bytesleft -= id_size + 4;
	    if (secondPass()) {
	       if (show_thread) {
		  to = thread_objects.get(seq0);
		  noteRoot(SYSTEM_CLASS,id);
		}
	       else noteRoot(id);
	     }
	    break;
	 case HPROF_GC_ROOT_MONITOR_USED :
	    id = readID();
	    bytesleft -= id_size;
	    if (secondPass()) {
	       if (show_system) noteRoot(SYSTEM_CLASS,id);
	       else noteRoot(id);
	     }
	    break;
	 case HPROF_GC_CLASS_DUMP :
	    seq0 = readClass();
	    bytesleft -= seq0;
	    break;
	 case HPROF_GC_INSTANCE_DUMP :
	    seq0 = readInstance();
	    bytesleft -= seq0;
	    break;
	 case HPROF_GC_OBJ_ARRAY_DUMP :
	    seq0 = readArray();
	    bytesleft -= seq0;
	    break;
	 case HPROF_GC_PRIM_ARRAY_DUMP :
	    seq0 = readPrimArray();
	    bytesleft -= seq0;
	    break;
	 default :
	    throw new IOException("DYMEMHPROF: Unknown record subtype: " + type);
       }
    }
}




/********************************************************************************/
/*										*/
/*	Class reader								*/
/*										*/
/********************************************************************************/

private int readClass() throws IOException
{
   long id = readID();
   in_stream.readInt(); 		// stack trace
   long sid = readID(); 		// super
   readID();				// class loader
   readID();				// signers
   readID();				// protection domain
   readID();				// reserved
   readID();				// reserved
   in_stream.readInt();			// size

   ClassObject cobj = class_objects.get(id);
   if (firstPass()) {
      ClassObject sobj = class_objects.get(sid);
      cobj.setSuper(sobj);
    }

   int read = 7 * id_size + 8;

   int ncp = in_stream.readUnsignedShort();
   read += 2;
   for (int i = 0; i < ncp; ++i) {
      in_stream.readUnsignedShort();	// index
      read += 2;
      read += readValue(null);
    }

   int nst = in_stream.readUnsignedShort();
   read += 2;
   Long [] vals = new Long[1];
   for (int i = 0; i < nst; ++i) {
      readID(); 			// name id
      read += id_size;
      byte type = in_stream.readByte();
      ++read;
      vals[0] = null;
      read += readValueForType(type,vals);
      if (vals[0] != null && secondPass()) {
	 if (show_classes) noteRoot(cobj.getId(),vals[0]);
	 else noteRoot(vals[0]);
       }
    }

   int nfld = in_stream.readUnsignedShort();
   read += 2;
   if (firstPass()) cobj.setNumField(nfld);
   for (int i = 0; i < nfld; ++i) {
      readID(); 			// name id
      read += id_size;
      byte type = in_stream.readByte();
      ++read;
      if (firstPass()) {
	 type = signatureFromTypeId(type);
	 cobj.setFieldType(i,type);
       }
    }

   return read;
}



/********************************************************************************/
/*										*/
/*	Instance reading							*/
/*										*/
/********************************************************************************/

private int readInstance() throws IOException
{
   long id = readID();
   in_stream.readInt(); 		// stack trace
   long cid = readID();
   int blen = in_stream.readInt();
   int read = 2*id_size + 8 + blen;

   ClassObject cobj = class_objects.get(cid);
   Long lid = id;
   if (firstPass()) noteType(lid,cobj);

   if (cobj != null && secondPass()) {
      int fread = 0;
      Long [] vals = new Long[1];
      for (byte b : cobj.getSignatures()) {
	 // System.err.println("READ " + cobj.getName() + " " + (idx++) + " " + ((char) b));
	 vals[0] = null;
	 fread += readValueForTypeSignature(b,vals);
	 if (vals[0] != null) noteRef(lid,vals[0]);
       }
      if (fread > blen) System.err.println("LENGTH PROBLEM " + fread + " " + blen);
      else if (fread < blen) {
	 System.err.println("CLASS " + cobj.getName() +  " LENGTH " + fread + " " + blen);
	 skipBytes(blen - fread);
       }
    }
   else {
      skipBytes(blen);
    }

   return read;
}



/********************************************************************************/
/*										*/
/*	Array reading								*/
/*										*/
/********************************************************************************/

private int readPrimArray() throws IOException
{
   long id = readID();
   in_stream.readInt(); 		// stack trace
   int num = in_stream.readInt();
   int read = id_size + 8;

   byte psig = in_stream.readByte();
   ++read;
   psig = signatureFromTypeId(psig);

   if (firstPass()) {
      ClassObject cobj = class_arrays.get(psig);
      Long lid = id;
      noteSize(lid,num);
      noteType(id,cobj);
    }

   int elsize = lengthFromSignature(psig);

   int sz = elsize * num;
   read += sz;
   skipBytes(sz);

   return read;
}


private int readArray() throws IOException
{
   long id = readID();

   in_stream.readInt(); 		// stack trace
   int num = in_stream.readInt();
   int read = id_size + 8;
   long ecid = readID();
   read += id_size;

   Long lid = id;
   if (firstPass()) {
      ClassObject cobj = class_objects.get(ecid);
      noteType(lid,cobj);
      noteSize(lid,num);
    }

   int sz = num * id_size;
   read += sz;
   if (secondPass()) {
      for (int i = 0; i < num; ++i) {
	 long rid = readID();
	 if (rid != 0) noteRef(lid,rid);
       }
    }
   else {
      skipBytes(sz);
    }

   return read;
}


private byte signatureFromTypeId(byte typeid)
{
   switch (typeid) {
      case T_CLASS:
	 return (byte) 'L';
      case T_BOOLEAN:
	 return (byte) 'Z';
      case T_CHAR:
	 return (byte) 'C';
      case T_FLOAT:
	 return (byte) 'F';
      case T_DOUBLE:
	 return (byte) 'D';
      case T_BYTE:
	 return (byte) 'B';
      case T_SHORT:
	 return (byte) 'S';
      case T_INT:
	 return (byte) 'I';
      case T_LONG:
	 return (byte) 'J';
    }

   return typeid;
}


private int lengthFromSignature(byte psig)
{
   int elsize = 0;

   switch (psig) {
      case 'Z' :
      case 'B' :
      case T_BOOLEAN :
      case T_BYTE :
	 elsize = 1;
	 break;
      case 'C' :
      case 'S' :
      case T_CHAR :
      case T_SHORT :
	 elsize = 2;
	 break;
      case 'F' :
      case 'I' :
      case T_FLOAT :
      case T_INT :
      case T_CLASS :
	 elsize = 4;
	 break;
      case 'D' :
      case 'J' :
      case T_DOUBLE :
      case T_LONG :
	 elsize = 8;
	 break;
    }

   return elsize;
}




/********************************************************************************/
/*										*/
/*	ID reading								*/
/*										*/
/********************************************************************************/

private long readID() throws IOException
{
   long id;

   if (id_size == 4) {
      id = in_stream.readInt();
      id &= 0xffffffffL;
    }
   else {
      id = in_stream.readLong();
    }

   return id;
}



/********************************************************************************/
/*										*/
/*	Value reading								*/
/*										*/
/********************************************************************************/

private int readValue(Long [] rslt) throws IOException
{
   byte type = in_stream.readByte();
   return 1 + readValueForType(type,rslt);
}



private int readValueForType(byte type,Long [] rslt) throws IOException
{
   type = signatureFromTypeId(type);
   return readValueForTypeSignature(type,rslt);
}


private int readValueForTypeSignature(byte type,Long [] rslt) throws IOException
{
   switch (type) {
      case '[' :
      case 'L' :
	 long id = readID();
	 if (rslt != null) rslt[0] = id;
	 return id_size;
      case 'Z' :
      case 'B' :
	 in_stream.readByte();
	 return 1;
      case 'S' :
	 in_stream.readShort();
	 return 2;
      case 'C' :
	 in_stream.readChar();
	 return 2;
      case 'I' :
	 in_stream.readInt();
	 return 4;
      case 'J' :
	 in_stream.readLong();
	 return 8;
      case 'F' :
	 in_stream.readFloat();
	 return 4;
      case 'D' :
	 in_stream.readDouble();
	 return 8;
    }

   throw new IOException("Unknown type signature: " + type);
}



/********************************************************************************/
/*										*/
/*	Thread Object holder							*/
/*										*/
/********************************************************************************/

private class ThreadObject {

   private Long thread_id;

   ThreadObject(long tid) {
      thread_id = tid;
    }

   Long getId() 			{ return thread_id; }

}	 // end of inner class ThreadObject



/********************************************************************************/
/*										*/
/*	Class Holder								*/
/*										*/
/********************************************************************************/

private ClassObject defineClass(Long id,String nm)
{
   if (class_objects.containsKey(id)) return class_objects.get(id);

   ClassObject cobj = new ClassObject(id,nm);
   class_objects.put(id,cobj);

   if (nm.length() == 2 && nm.charAt(0) == '[') {       // prim array
      byte b = (byte) nm.charAt(1);
      class_arrays.put(b,cobj);
    }

   return cobj;
}



private class ClassObject {

   private Long class_id;
   private String class_name;
   private byte [] field_types;
   private ClassObject super_class;
   private boolean signature_set;

   ClassObject(long id,String nm) {
      class_id = id;
      class_name = nm;
      field_types = null;
      super_class = null;
      signature_set = false;
    }

   void setNumField(int nfld)		{ field_types = new byte[nfld]; }

   void setFieldType(int idx,byte t)	{ field_types[idx] = t; }

   void setSuper(ClassObject co)	{ super_class = co; }

   Long getId() 			{ return class_id; }
   String getName()			{ return class_name; }

   byte [] getSignatures() {
      if (!signature_set) {
	 if (super_class != null) {
	    byte [] supsig = super_class.getSignatures();
	    if (supsig.length > 0) {
	       int delta = supsig.length;
	       byte [] nd = new byte[field_types.length+delta];
	       for (int i = 0; i < delta; ++i) nd[i] = supsig[i];
	       for (int i = 0; i < field_types.length; ++i) nd[i+delta] = field_types[i];
	       field_types = nd;
	     }
	  }
	 signature_set = true;
       }
      return field_types;
    }

}	// end of inner class ClassObject


/********************************************************************************/
/*										*/
/*	Methods to manage reference collection					*/
/*										*/
/********************************************************************************/

private void setupDump()
{
   object_types = new HashMap<Long,ClassObject>();
   object_refs = new HashMap<Long,Object>();
   root_refs = new HashMap<Long,Object>();
   real_roots = new ArrayList<Long>();
   array_sizes = new HashMap<Long,Integer>();
}



private void noteRoot(Long id)
{
   real_roots.add(id);
}


private void noteRoot(Long which,Long id)
{
   addRef(root_refs,which,id);
}



private void noteRef(Long from,Long to)
{
   addRef(object_refs,from,to);
}


private void noteType(Long obj,ClassObject typ)
{
   object_types.put(obj,typ);
}



private void noteSize(Long arr,Integer sz)
{
   array_sizes.put(arr,sz);
}


@SuppressWarnings("unchecked")
private void addRef(Map<Long,Object> map,Long from,Long id)
{
   Object v = map.get(from);
   if (v == null) map.put(from,Long.valueOf(id));
   else if (v instanceof Long) {
      long ov = ((Long) v).longValue();
      List<Long> lv = new ArrayList<Long>(4);
      map.put(from,lv);
      lv.add(ov);
      lv.add(id);
    }
   else {
      List<Long> lv = (List<Long>) v;
      lv.add(id);
    }
}



/********************************************************************************/
/*										*/
/*	Methods to output a dump						*/
/*										*/
/********************************************************************************/

private void finishDump()
{
   // count refs
   // output xml

   setupDump(); 	// in case there is more
}




/********************************************************************************/
/*										*/
/*	Test program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   long start = System.currentTimeMillis();

   try {
      DymemHprofReader dhr = new DymemHprofReader("/ws/volfred/spr/hprof.out");
      dhr.readHprof();
    }
   catch (IOException e) {
      System.err.println("PROBLEM READING HPROF FILE: " + e);
      System.exit(1);
    }

   long delta = System.currentTimeMillis() - start;

   System.err.println("READ COMPLETED " + delta);
}




}	// end of class DymemHprofReader




/* end of DymemHprofReader.java */
