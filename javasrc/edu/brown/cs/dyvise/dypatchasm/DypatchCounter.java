/********************************************************************************/
/*										*/
/*		DypatchCounter.java						*/
/*										*/
/*    Counts classes, methods, etc. for DYPER patcher				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dypatchasm/DypatchCounter.java,v 1.2 2013/09/04 18:36:34 spr Exp $ */

/*********************************************************************************
 *
 * $Log: DypatchCounter.java,v $
 * Revision 1.2  2013/09/04 18:36:34  spr
 * Minor bug fixes.
 *
 *
 * Revision 1.0 2013-08-19 16:26:07 zolstein
 * Original version
 *
 ********************************************************************************/

package edu.brown.cs.dyvise.dypatchasm;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import edu.brown.cs.ivy.xml.IvyXmlWriter;


public class DypatchCounter extends ClassVisitor implements DypatchConstants {

/********************************************************************************/
/*										*/
/* Private Storage */
/*										*/
/********************************************************************************/

private static int	     current_id_number = 1;

private String		     class_name;
private String		     super_class_name;
private String		     source_file;
private int		     class_id;
private Map<String, Integer> id_numbers;
private IvyXmlWriter	     xml_writer;
private ClassLoader	     class_loader;

/********************************************************************************/
/*										*/
/* Constructors */
/*										*/
/********************************************************************************/

public DypatchCounter(int xapi,IvyXmlWriter xw)
{
   this(xapi, xw, null);
}

public DypatchCounter(int xapi,IvyXmlWriter xw,ClassLoader cl)
{
   super(xapi);
   id_numbers = new HashMap<String, Integer>();
   xml_writer = xw;
   if (cl == null) {
      class_loader = ClassLoader.getSystemClassLoader();
   }
   else {
      class_loader = cl;
   }
}

private class MethodCounter extends MethodVisitor {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String	     method_name;
private String	     method_desc;
private int	     method_id;
private int	     ins_no;
private int	     block_no;
private int	     block_id;
private int	     block_start;
private boolean      constructing;
private int	     min_line;
private int	     current_line;
private Label	     last_label;
private IvyXmlWriter block_writer;
private IvyXmlWriter sub_elt_writer;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private MethodCounter(int xapi,String name,String desc,int id)
{
   super(xapi);
   method_name = name;
   method_desc = desc;
   method_id = id;
   ins_no = 0;
   block_no = 0;
   block_id = -1;
   block_start = -1;
   constructing = name.equals("<init>");
   min_line = -1;
   current_line = -1;
   block_writer = new IvyXmlWriter();
   sub_elt_writer = new IvyXmlWriter();
}




/********************************************************************************/
/*										*/
/*	Instruction Visitors							*/
/*										*/
/********************************************************************************/

public void visitCode()
{
   addLabel();
   if (!constructing) {
      startBlock();
   }
}

public void visitEnd()
{
   addLabel();
   dumpMethod(xml_writer);
}

public void visitFieldInsn(int opcode,String owner,String name,String desc)
{
   addLabel();
   startBlock();
   ++ins_no;
}

@Override public void visitIincInsn(int var,int increment)
{
   addLabel();
   startBlock();
   ++ins_no;
}

@Override public void visitInsn(int opcode)
{
   addLabel();
   startBlock();
   if (opcode == Opcodes.RETURN || opcode == Opcodes.IRETURN || opcode == Opcodes.LRETURN
	    || opcode == Opcodes.FRETURN || opcode == Opcodes.DRETURN
	    || opcode == Opcodes.ARETURN || opcode == Opcodes.ATHROW){
      endBlock();
   }
   ++ins_no;
}

@Override public void visitIntInsn(int opcode,int operand)
{
   addLabel();
   startBlock();
   if (opcode == Opcodes.NEWARRAY) {
      allocXml(opcodeToType(operand), 1);
   }
   ++ins_no;
}

@Override public void visitJumpInsn(int opcode,Label label)
{
   addLabel();
   startBlock();
   endBlock();
   ++ins_no;
}

@Override public void visitLabel(Label l)
{
   addLabel();
   last_label = l;
}

@Override public void visitLdcInsn(Object cst)
{
   addLabel();
   startBlock();
   ++ins_no;
}

@Override public void visitLineNumber(int line,Label start)
{
   last_label = null;
   if (min_line == -1) {
      min_line = line;
   }
   current_line = line;
}


@Override public void visitLookupSwitchInsn(Label dflt,int[] keys,Label[] labels)
{
   addLabel();
   endBlock();
   ++ins_no;
}


// Also adds IDs for called method and containing class
@Override public void visitMethodInsn(int opcode,String owner,String name,String desc,boolean ltf)
{
   addLabel();
   addClassId(owner);
   addMethodId(owner, name, desc);
   ++ins_no;
   if (constructing
	 && name.equals("<init>")
	 && (owner.equals(super_class_name)
	 || owner.equals(class_name)))
	    constructing = false;
}


@Override public void visitMultiANewArrayInsn(String desc,int dims)
{
   addLabel();
   startBlock();
   allocXml(Type.getType(desc), dims);
   ++ins_no;
}


@Override public void visitTableSwitchInsn(int min,int max,Label dflt,Label... labels)
{
   addLabel();
   endBlock();
   ++ins_no;
}

@SuppressWarnings("fallthrough")
@Override public void visitTypeInsn(int opcode,String type)
{
   addLabel();
   startBlock();
   int dims = 1;
   switch (opcode) {
      case Opcodes.NEW:
	 dims = 0;
	 //FALLTHROUGH
      case Opcodes.ANEWARRAY:
         try {
            Type t = Type.getType(type);
            allocXml(t, dims);
          }
         catch (Throwable t) {
            System.err.println("DYPATCH: can't find type " + type + " for " + opcode);
          }
	 break;
   }
   ++ins_no;
}

@Override public void visitVarInsn(int opcode,int var)
{
   addLabel();
   startBlock();
   ++ins_no;
}

//Necessary to prevent new line declarations being treated as block endings
private void addLabel()
{
   Label l = last_label;
   if (l != null) {
      endBlock();
      last_label = null;
   }
}

/********************************************************************************/
/*										*/
/*	Block Handlers								*/
/*										*/
/********************************************************************************/

private boolean inBlock(){
   return (block_start != -1 || constructing || min_line == -1);
}

// Adds block ID and starts XML if necessary.
private void startBlock()
{
   if (inBlock()) {
      return;
   }
   block_id = addBlockId();
   startBlockXml();
   block_start = ins_no;
}

// Adds ID for block if not already present.
private int addBlockId()
{
   return addBlockId(class_name, method_name, method_desc, block_no);
}

private int addBlockId(String className,String method,String sig,int blockNum)
{
   className = className.replace('/', '.');
   String key = className + "%" + method + xmlSig(sig) + "%" + blockNum;
   int id;
   if (!id_numbers.containsKey(key)) {
      id = current_id_number++;
      id_numbers.put(key, id);
   }
   else id = id_numbers.get(key);
   return id;
}

private void endBlock()
{
   if (block_start == -1) {
      return;
   }
   endBlockXml();
   block_start = -1;
   block_id = -1;
   ++block_no;
}

/********************************************************************************/
/*										*/
/*	XML Generators								*/
/*										*/
/********************************************************************************/

/*
 * Methods for making XML element to denote block. Used when writing METHOD XML
 * elements.
 */
private void startBlockXml()
{
   block_writer.begin("BLOCK");
   block_writer.field("METHOD", class_name.replace('/','.') + '.' + method_name);
   block_writer.field("INDEX", block_id);
   block_writer.field("STARTINS", ins_no);
   block_writer.field("START", current_line);
}

private void endBlockXml()
{
   block_writer.field("ENDINS", ins_no);
   block_writer.field("END", current_line);
   String subElts = sub_elt_writer.toString();
   if (!subElts.equals("")) block_writer.xmlText(subElts);
   block_writer.end();
   sub_elt_writer = new IvyXmlWriter();
}

/*
 * Creates XML element to denote memory allocation. Used when writing BLOCK XML
 * elements.
 */
private void allocXml(Type t,int dims)
{
   sub_elt_writer.begin("ALLOC");
   if (dims > 0) {
      sub_elt_writer.field("ARRAY", dims);
   }
   sub_elt_writer.field("CLASS", t.getDescriptor().replace('/', '.'));
   sub_elt_writer.end();
}

/*
 * Creates XML element to denote method.
 */
private void dumpMethod(IvyXmlWriter xw)
{

   xw.begin("METHOD");
   xw.field("NAME", class_name.replace('/','.') + "." + method_name);
   xw.field("SIGNAGURE", xmlSig(method_desc));
   xw.field("INDEX", method_id);
   xw.field("FILE", source_file);
   // START and END will be -1 for native methods
   // Do not know how to fix; probably unneeded?
   xw.field("START", min_line);
   xw.field("END", current_line);

   dumpBlocks(xw);
   xw.end("METHOD");
}

private void dumpBlocks(IvyXmlWriter xw)
{
   String text = block_writer.toString();
   if (!text.equals("")) xw.xmlText(text);
}
}

/********************************************************************************/
/*										*/
/*	Visitors								*/
/*										*/
/********************************************************************************/

@Override public void visit(int version,int access,String name,String signature,
	 String superName,String[] interfaces)
{
   class_name = name;
   super_class_name = superName;
   source_file = class_loader.getResource(name.replace('.', '/') + ".class").getPath();
   source_file = source_file.substring(source_file.indexOf('/'));
   class_id = addClassId(name);
   dumpClass(xml_writer);
   super.visit(version, access, name, signature, superName, interfaces);
}

@Override public MethodVisitor visitMethod(int access,String name,String desc,
	 String signature,String[] exceptions)
{
   int id = addMethodId(name, desc);
   MethodCounter mc = new MethodCounter(ASM_VERSION,name,desc,id);
   return mc;
}

// Adds class ID if not already present.
private int addClassId(String className)
{
   int id;
   className = className.replace('/', '.');
   if (!id_numbers.containsKey(className)) {
      id = current_id_number++;
      id_numbers.put(className, id);
   }
   else id = id_numbers.get(className);
   return id;
}

// Adds method ID if not already present.
private int addMethodId(String method,String desc)
{
   return addMethodId(class_name, method, desc);
}

private int addMethodId(String className,String method,String desc)
{
   className = className.replace('/', '.');
   String key = className + "%" + method + xmlSig(desc);
   int id;
   if (!id_numbers.containsKey(key)) {
      id = current_id_number++;
      id_numbers.put(key, id);
   }
   else id = id_numbers.get(key);
   return id;
}

/*
 * Creates XML element to denote class.
 */
private void dumpClass(IvyXmlWriter xw)
{
   xw.begin("CLASS");
   xw.field("NAME", class_name.replace('/', '.'));
   xw.field("INDEX", class_id);
   xw.field("FILE", source_file);
   xw.end();
}

/********************************************************************************/
/*										*/
/*	Getters/Setters 							*/
/*										*/
/********************************************************************************/

public String getXml()
{
   return xml_writer.toString();
}

public Map<String, Integer> getIds()
{
   return id_numbers;
}

/********************************************************************************/
/*										*/
/*	Misc. Methods								*/
/*										*/
/********************************************************************************/

private static String xmlSig(String desc)
{
   return DypatchClassVisitor.asmToXmlMethodSig(desc);
}

private static Type opcodeToType(int opcode)
{
   switch (opcode) {
      case Opcodes.T_BOOLEAN:
	 return Type.BOOLEAN_TYPE;
      case Opcodes.T_BYTE:
	 return Type.BYTE_TYPE;
      case Opcodes.T_CHAR:
	 return Type.CHAR_TYPE;
      case Opcodes.T_DOUBLE:
	 return Type.DOUBLE_TYPE;
      case Opcodes.T_FLOAT:
	 return Type.FLOAT_TYPE;
      case Opcodes.T_INT:
	 return Type.INT_TYPE;
      case Opcodes.T_LONG:
	 return Type.LONG_TYPE;
      case Opcodes.T_SHORT:
	 return Type.SHORT_TYPE;
      default:
	 return null;
   }
}



}	// end of class DypatchCounter




/* end of DypatchCounter.java */
