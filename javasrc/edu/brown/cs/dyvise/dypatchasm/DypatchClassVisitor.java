/********************************************************************************/
/*										*/
/*		DypatchClassVisitor.java					*/
/*										*/
/*	    Applies patches for DYPER patcher					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dypatchasm/DypatchClassVisitor.java,v 1.3 2016/11/02 18:59:15 spr Exp $ */

/*********************************************************************************
 *
 * $Log: DypatchClassVisitor.java,v $
 * Revision 1.3  2016/11/02 18:59:15  spr
 * Move to asm5
 *
 * Revision 1.2  2013/09/04 18:36:33  spr
 * Minor bug fixes.
 *
 *
 * Revision 1.0 2013-08-19 16:26:07 zolstein
 * Original version
 *
 ********************************************************************************/

package edu.brown.cs.dyvise.dypatchasm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import edu.brown.cs.ivy.xml.IvyXml;


public class DypatchClassVisitor extends ClassVisitor implements DypatchConstants {

/********************************************************************************/
/*										*/
/* Private Storage								*/
/*										*/
/********************************************************************************/

private Collection<Element>		 xml_patches;
private String				 class_name;
private String				 super_class_name;
private Map<String, Collection<Element>> patched_methods;
private Map<String, DypatchField>	 field_map;
private Map<String, Element>		 mode_map;
private Map<String, Integer>		 id_numbers;
private int				 last_line;

/********************************************************************************/
/*										*/
/* Constructors 								*/
/*										*/
/********************************************************************************/

public DypatchClassVisitor(int xapi,ClassVisitor xcv,Collection<Element> xml,
	 Map<String, Element> modeMap,Map<String, Integer> ids)
{
   super(xapi,xcv);
   xml_patches = xml;
   patched_methods = new HashMap<String, Collection<Element>>();
   field_map = new HashMap<String, DypatchField>();
   mode_map = modeMap;
   id_numbers = ids;
   last_line = -1;
}

private class DypatchMethodVisitor extends MethodVisitor {

/********************************************************************************/
/*										*/
/* Private Storage								*/
/*										*/
/********************************************************************************/

private Collection<Element>		      xml_collection;
private int				      access_flags;
private boolean 			      constructing;
private String				      method_name;
private String				      method_desc;
private Map<PatchOption, Collection<Element>> patches;
private Map<String, Integer>		      local_names;
private Map<Integer, DypatchLocal>	      local_vars;
private Set<DypatchLocal>		      new_locals;
private Label				      local_start;
private Label				      last_label;
private Label				      current_label;
private String				      current_class;
private String				      current_member;
private String				      current_method_desc;
private int				      block_number;
private boolean 			      in_block;
private int				      curr_line_number;
private int				      prev_line_number;
private int				      ins_number;

/********************************************************************************/
/*										*/
/* Constructors 								*/
/*										*/
/********************************************************************************/

public DypatchMethodVisitor(int xapi,MethodVisitor xmv,int access,String name,String desc)
{
   super(xapi,xmv);
   method_name = name;
   method_desc = desc;
   access_flags = access;
   constructing = name.equals("<init>");
   xml_collection = patched_methods.get("*");
   if (xml_collection == null) xml_collection = new HashSet<Element>();
   String nm1 = name+desc;
   if (patched_methods.containsKey(name))
      xml_collection.addAll(patched_methods.get(name));
   if (patched_methods.containsKey(nm1))
      xml_collection.addAll(patched_methods.get(nm1));
   patches = new HashMap<PatchOption, Collection<Element>>();
   local_names = new HashMap<String, Integer>();
   local_vars = new HashMap<Integer, DypatchLocal>();
   new_locals = new HashSet<DypatchLocal>();
   local_start = new Label();
   last_label = new Label();
   current_label = null;
   current_class = null;
   current_member = null;
   current_method_desc = null;
   block_number = 0;
   in_block = false;
   curr_line_number = last_line;
   prev_line_number = curr_line_number;
   ins_number = 0;
   parseDescription(access, desc);
   populateMap();
}

// Parses method description and populates fields accordingly
@SuppressWarnings("fallthrough")
public void parseDescription(int access,String desc)
{
   int pos = 0;
   if (!isStatic(access)) {
      local_names.put("0", 0);
      addLocalVariable(Type.getType("L" + class_name + ";"), 0, "this");
      ++pos;
   }

   Type[] args = Type.getArgumentTypes(desc);
   for (int i = 0; i < args.length; i++) {
      boolean doublesize = false;
      Type t = args[i];
      String name = "" + (pos);
      switch (t.getSort()) {
	 case Type.DOUBLE:
	 case Type.LONG:
	    doublesize = true;
	    ++pos;
	    // FALLTHROUGH
	 default:
	    int index = getVarIndex(doublesize);
	    local_names.put(name, index);
	    addLocalVariable(t, index, name);
	    ++pos;
      }
   }
}

// Adds XML elements to map for easy access
private void populateMap()
{
   if (xml_collection == null) return;
   for (Element xml : xml_collection) {
      for (Element e : IvyXml.elementsByTag(xml, "PATCH")) {
	 PatchOption what = IvyXml.getAttrEnum(e, "WHAT", PatchOption.NONE);
	 if (what == PatchOption.NONE) continue;
	 Collection<Element> collection = patches.get(what);
	 if (collection == null) {
	    collection = new ArrayList<Element>();
	    patches.put(what, collection);
	 }
	 collection.add(e);
      }
   }
}

/********************************************************************************/
/*										*/
/* Instruction Visitors 							*/
/*										*/
/********************************************************************************/

@Override public void visitCode()
{
   applyLabel();
   super.visitCode();
   super.visitLabel(local_start);
   if (!constructing) applyEntryPatches();
}

@Override public void visitEnd()
{
   applyLabel();
   super.visitEnd();
   last_line = curr_line_number;
}

@Override public void visitFieldInsn(int opcode,String owner,String name,String desc)
{
   applyLabel();
   applyPreBlockPatches();
   PatchOption po = PatchOption.NONE; // Will be overwritten in switch
   switch (opcode) {
      case Opcodes.GETSTATIC:
	 po = PatchOption.STATIC_ACCESS;
	 break;
      case Opcodes.PUTSTATIC:
	 po = PatchOption.STATIC_WRITE;
	 break;
      case Opcodes.GETFIELD:
	 po = PatchOption.FIELD_ACCESS;
	 break;
      case Opcodes.PUTFIELD:
	 po = PatchOption.FIELD_WRITE;
	 break;
   }
   current_member = name;
   current_class = owner;
   applyPrePatches(po);
   super.visitFieldInsn(opcode, owner, name, desc);
   applyPostPatches(po);
   current_member = null;
   current_class = null;
   ++ins_number;
}

@Override public void visitIincInsn(int var,int increment)
{
   applyLabel();
   applyPreBlockPatches();
   var = local_names.get("" + var);
   super.visitIincInsn(var, increment);
   ++ins_number;
}

@SuppressWarnings("fallthrough")
@Override public void visitInsn(int opcode)
{
   applyLabel();
   PatchOption po = PatchOption.NONE;
   applyPreBlockPatches();
   switch (opcode) {
      case Opcodes.ATHROW:
	 applyPostBlockPatches();
	 super.visitInsn(opcode);
	 break;
      case Opcodes.RETURN:
      case Opcodes.IRETURN:
      case Opcodes.LRETURN:
      case Opcodes.FRETURN:
      case Opcodes.DRETURN:
      case Opcodes.ARETURN:
	 applyPostBlockPatches();
	 applyPrePatches(PatchOption.EXIT);
	 applyPostPatches(PatchOption.EXIT);
	 super.visitInsn(opcode);
	 break;
      case Opcodes.MONITORENTER:
	 po = PatchOption.SYNC_ENTER;
	 // FALLTHROUGH
      case Opcodes.MONITOREXIT:
	 if (po == PatchOption.NONE) po = PatchOption.SYNC_EXIT;
	 // FALLTHROUGH
      default:
	 if (po != PatchOption.NONE) applyPrePatches(po);
	 super.visitInsn(opcode);
	 if (po != PatchOption.NONE) applyPostPatches(po);
   }
   ++ins_number;
}

@Override public void visitIntInsn(int opcode,int operand)
{
   applyLabel();
   applyPreBlockPatches();
   switch (opcode) {
      case Opcodes.NEWARRAY:
	 applyPrePatches(PatchOption.BASE_ARRAY_ALLOC);
	 super.visitIntInsn(opcode, operand);
	 applyPostPatches(PatchOption.BASE_ARRAY_ALLOC);
	 break;
      default:
	 super.visitIntInsn(opcode, operand);
   }
   ++ins_number;
}

@Override public void visitJumpInsn(int opcode,Label label)
{
   applyLabel();
   applyPreBlockPatches();
   applyPostBlockPatches();
   super.visitJumpInsn(opcode, label);
   ++ins_number;
}

@Override public void visitLabel(Label l)
{
   applyLabel();
   last_label = l;
   current_label = l;
}

// Necessary to prevent new line declarations being treated as block endings
private void applyLabel()
{
   if (current_label == null) {
      return;
   }
   applyPostBlockPatches();
   super.visitLabel(current_label);
   current_label = null;
}

@Override public void visitLdcInsn(Object cst)
{
   applyLabel();
   applyPreBlockPatches();
   super.visitLdcInsn(cst);
   ++ins_number;
}

@Override public void visitLineNumber(int line,Label start)
{
   applyPostPatches(PatchOption.LINE);
   super.visitLabel(start);
   current_label = null;
   super.visitLineNumber(line, start);
   prev_line_number = curr_line_number;
   curr_line_number = line;
   applyPrePatches(PatchOption.LINE);
}

public void visitLocalVariable(String name,String desc,String signature)
{
   boolean doublesize = desc.equals("J") || desc.equals("D");
   int index = getVarIndex(doublesize);
   local_names.put(name, index);
   DypatchLocal l = addLocalVariable(Type.getType(desc), index, name);
   new_locals.add(l);
}

@Override public void visitLocalVariable(String name,String desc,String signature,
	 Label start,Label end,int index)
{
   applyLabel();
   super.visitLocalVariable(name, desc, signature, start, end, index);
}

@Override public void visitLookupSwitchInsn(Label dflt,int[] keys,Label[] labels)
{
   applyLabel();
   applyPostBlockPatches();
   super.visitLookupSwitchInsn(dflt, keys, labels);
   ++ins_number;
}

@Override public void visitMaxs(int maxStack,int maxLocals)
{
   for (DypatchLocal l : new_locals) {
      super.visitLocalVariable(l.getId(), l.getType().getDescriptor(), null, local_start,
	       last_label, l.getIndex());
   }
   new_locals.clear();
   super.visitMaxs(maxStack, maxLocals);
}

@Override public void visitMethodInsn(int opcode,String owner,String name,String desc,boolean itf)
{
   applyLabel();
   if (constructing && name.equals("<init>") && owner.equals(super_class_name)) {
      super.visitMethodInsn(opcode, owner, name, desc, itf);
      constructing = false;
      applyEntryPatches();
   }
   else {
      applyPreBlockPatches();
      current_class = owner;
      current_member = name;
      current_method_desc = desc;
      applyPrePatches(PatchOption.CALL);
      super.visitMethodInsn(opcode, owner, name, desc, itf);
      applyPostPatches(PatchOption.CALL);
      current_class = null;
      current_member = null;
      current_method_desc = null;
   }
   ++ins_number;
}

@Override public void visitMultiANewArrayInsn(String desc,int dims)
{
   applyLabel();
   applyPreBlockPatches();
   applyPrePatches(PatchOption.ARRAY_ALLOC);
   super.visitMultiANewArrayInsn(desc, dims);
   applyPostPatches(PatchOption.ARRAY_ALLOC);
   ++ins_number;
}

@Override public void visitTableSwitchInsn(int min,int max,Label dflt,Label... labels)
{
   applyLabel();
   applyPostBlockPatches();
   super.visitTableSwitchInsn(min, max, dflt, labels);
   ++ins_number;
}

@Override public void visitTypeInsn(int opcode,String type)
{
   applyLabel();
   applyPreBlockPatches();
   current_class = type;
   switch (opcode) {
      case Opcodes.NEW:
	 applyPrePatches(PatchOption.OBJECT_ALLOC);
	 super.visitTypeInsn(opcode, type);
	 applyPostPatches(PatchOption.OBJECT_ALLOC);
	 break;
      case Opcodes.ANEWARRAY:
	 applyPrePatches(PatchOption.ARRAY_ALLOC);
	 super.visitTypeInsn(opcode, type);
	 applyPostPatches(PatchOption.ARRAY_ALLOC);
	 break;
      default:
	 super.visitTypeInsn(opcode, type);
   }
   current_class = null;
   ++ins_number;
}

@Override public void visitVarInsn(int opcode,int var)
{
   applyLabel();
   visitVarInsn(opcode, "" + var);
}

public void visitVarInsn(int opcode,String name)
{
   applyPreBlockPatches();
   Integer var = local_names.get(name);
   if (isStoreInsn(opcode)) {
      if (needNewVar(opcode, var)) {
	 removeOldVar(var);
	 boolean doublesize;
	 if (opcode == Opcodes.DSTORE || opcode == Opcodes.FSTORE) doublesize = true;
	 else doublesize = false;
	 var = getVarIndex(doublesize);
	 local_names.put(name, var);
      }
      addLocalVariable(opcode, var, name);
   }
   super.visitVarInsn(opcode, var);
   ++ins_number;
}

private boolean needNewVar(int opcode,Integer var)
{
   if (var == null) return true;
   DypatchLocal l = local_vars.get(var);
   if (l == null) return true;
   return !(typeToStoreInsn(l.getType()) == opcode);

}

private void removeOldVar(Integer var)
{
   if (var == null) return;
   DypatchLocal l = local_vars.get(var);
   Type t = l.getType();
   local_vars.remove(var);
   if (t == Type.LONG_TYPE || t == Type.DOUBLE_TYPE) {
      local_vars.remove(var + 1);
   }
}

/********************************************************************************/
/*										*/
/* Patch Methods								*/
/*										*/
/********************************************************************************/

// Applies patches that come before instruction
private void applyPrePatches(PatchOption po)
{
   Collection<Element> collection = patches.get(po);
   if (collection == null) return;
   for (Element xml : collection) {
      applyPatch(xml, false);
   }
}

// Applies patches that come after instruction
private void applyPostPatches(PatchOption po)
{
   Collection<Element> collection = patches.get(po);
   if (collection == null) return;
   for (Element xml : collection) {
      applyPatch(xml, true);
   }
}

private void applyEntryPatches()
{
   applyPrePatches(PatchOption.ENTER);
   applyPostPatches(PatchOption.ENTER);
}

/*
 * Apply block patches if relevant and handle upkeep.
 */
private void applyPreBlockPatches()
{
   if (constructing) return;
   if (!in_block) {
      applyPrePatches(PatchOption.BLOCK);
      in_block = true;
   }
}

private void applyPostBlockPatches()
{
   if (in_block) {
      applyPostPatches(PatchOption.BLOCK);
      in_block = false;
      ++block_number;
   }
}

/*
 * Apply patch appropriately if conditions met.
 */
private void applyPatch(Element xml,boolean postPatch)
{
   if (constructing) return;
   Element mode = mode_map.get(IvyXml.getAttrString(xml, "MODE"));
   if (mode == null) {
      mode = IvyXml.getChild(xml, "MODE");
      if (mode == null) {
	 return;
      }
   }

   Element when = IvyXml.getElementByTag(xml, "WHEN");
   if (!checkWhen(when, postPatch)) return;

   String context = IvyXml.getAttrString(mode, "CONTEXT");
   boolean post = IvyXml.getAttrBool(mode, "POST");
   for (Element e : IvyXml.children(mode)) {
      if (!postPatch && post) return;
      String s = e.getNodeName();
      if (postPatch && !post) {
	 if (s.equals("INSTRUCTION")) {
	    post = true;
	 }
	 continue;
      }
      switch (s) {
	 case "CODE":
	    if (validCode(e, IvyXml.getAttrString(mode, "NAME"))) {
	       applyCode(e, context);
	    }
	    break;
	 case "LOCAL":
	    String type = IvyXml.getAttrString(e, "TYPE");
	    String name = IvyXml.getAttrString(e, "VALUE");
	    if (type == null || name == null) {
	       continue;
	    }
	    visitLocalVariable(name, xmlToAsmDesc(type), null);
	    break;
      }
   }
}

/*
 * Returns true if patch should be applied now according to "WHEN" element.
 * False otherwise.
 */
private boolean checkWhen(Element when,boolean postPatch)
{
   if (when != null) {
      String c = IvyXml.getAttrString(when, "CASE");
      String a = IvyXml.getAttrString(when, "ARG");
      switch (c) {
	 case "BLOCK":
	    if (in_block != postPatch || a == null || block_number != Integer.parseInt(a)) return false;
	    break;
	 case "CLASS":
	    if (current_class == null || a == null) return false;
	    else if (!current_class.equals(a.replaceAll(".", "/"))) return false;
	    break;
	 case "FIELD":
	 case "METHOD":
	    if (current_member == null || a == null) return false;
	    else if (!current_member.equals(a)
		     && !(current_class + "/" + current_member).equals(a
			      .replace('.', '/'))) return false;
	    break;
	 case "LINE":
	    if (a == null) return false;
	    int n = Integer.parseInt(a);
	    if (prev_line_number >= n || curr_line_number < n) {
	       return false;
	    }
      }
   }
   return true;
}

/*
 * Returns true if "CODE" argument is valid. False otherwise. (Currently, just
 * needs "TYPE" argument to equal "CALL")
 */
private boolean validCode(Element e,String name)
{
   String type = IvyXml.getAttrString(e, "TYPE");
   if (type == null) {
      System.err.println("DYPATCH: Mode " + name + " CODE missing type");
      return false;
   }
   else if (!type.equals("CALL")) {
      System.err.println("DYPATCH: Mode + " + name + " CODE illegal type " + type);
      return false;
   }
   return true;
}

/*
 * Checks additional conditions, pushes arguments, adds method call, handles
 * return
 */
private void applyCode(Element xml,String context)
{
   String methodClass = IvyXml.getAttrString(xml, "CLASS");
   if (methodClass == null) methodClass = context;
   if (methodClass == null) return;
   String method = IvyXml.getAttrString(xml, "METHOD");
   String signature = IvyXml.getAttrString(xml, "SIGNATURE");
   if (method == null || signature == null) return;
   if (!meetsConditions(xml)) {
      return;
   }

   pushArguments(xml, methodClass, method, signature);
   Type t = Type.VOID_TYPE;
   Element ret = IvyXml.getChild(xml, "RETURN");
   if (ret != null) {
      String s = IvyXml.getAttrString(ret, "VALUE");
      Integer index;
      DypatchLocal l = null;
      if (s != null) {
	 index = local_names.get(s);
	 if (index != null) l = local_vars.get(index);
	 if (l != null) t = l.getType();
      }
   }
   super.visitMethodInsn(Opcodes.INVOKESTATIC, methodClass.replace('.', '/'), method,
	    xmlToAsmMethodDesc(signature, t),false);
   handleReturn(ret);
}

/*
 * Checks that "CONDITION" argument is true Currently unimplemented and unused.
 */
private boolean meetsConditions(Element xml)
{
   // TODO: Check "CONDITION" condition
   // Unhandled in original Jikes code
   /*
    * for (Element e: IvyXml.elementsByTag(xml, "CONDITION")){ String type =
    * IvyXml.getAttrString(e, "TYPE"); String lValue = IvyXml.getAttrString(e,
    * "LVALUE"); String rValue = IvyXml.getAttrString(e, "RVALUE");
    * switch(type){ case "CONTEXT": break; case "FIELD": if (lValue == null ||
    * rValue == null) return; if () ; break; case "RETURN": if (rValue == null)
    * return; if () ; break; case "LOCAL": if (lValue == null || rValue ==
    * null) return; if () ; break; case "ARG": if (lValue == null || rValue ==
    * null) return; if () ; break; } }
    */
   return true;
}

/*
 * Pushes arguments to the stack for patch's method call
 */
private void pushArguments(Element xml,String methodClass,String method,String signature)
{
   String desc = xmlToAsmMethodDesc(signature, null);
   Type[] descArray = Type.getArgumentTypes(desc);
   int argIndex = 0;
   boolean stack = requiresStack(xml);
   if (stack) super.visitInsn(Opcodes.DUP);
   for (Element e : IvyXml.elementsByTag(xml, "ARG")) {
      if (argIndex > descArray.length) continue;
      ArgumentType type = IvyXml.getAttrEnum(e, "TYPE", ArgumentType.NULL);
      switch (type) {
	 case THIS:
	    if (isStatic(access_flags)) super.visitLdcInsn(0);
	    else super.visitVarInsn(Opcodes.ALOAD, 0);
	    break;
	 case INTEGER:
	    Integer v = IvyXml.getAttrInt(e, "VALUE");
	    super.visitLdcInsn(v);
	    break;
	 case CURTHREAD:
	    super.visitMethodInsn(Opcodes.INVOKESTATIC, "Ljava/lang/Thread;",
		     "currentThread", "()Ljava/lang/Thread;",false);
	    break;
	 case INSNO:
	    super.visitLdcInsn(Integer.valueOf(ins_number));
	    break;
	 case BLOCKID:
	    super.visitLdcInsn(Integer.valueOf(getBlockId()));
	    break;
	 case CURMETHOD:
	    super.visitLdcInsn(Integer.valueOf(getMethodId()));
	    break;
	 case METHODID:
	    super.visitLdcInsn(Integer.valueOf(getMethodId()));
	    break;
	 case REFMETHODID:
	    super.visitLdcInsn(Integer.valueOf(getMethodId(current_class, current_member,
		     current_method_desc)));
	    break;
	 case CURCLASS:
	    super.visitLdcInsn(Integer.valueOf(getClassId()));
	    break;
	 case CLASSID:
	    super.visitLdcInsn(Integer.valueOf(getClassId(class_name)));
	    break;
	 case REFCLASSID:
	    super.visitLdcInsn(Integer.valueOf(getClassId()));
	    break;
	 case NEWLOCAL:
	    String name = IvyXml.getAttrString(e, "VALUE");
	    DypatchLocal l = local_vars.get(local_names.get(name));
	    if (l == null) {
	       super.visitInsn(Opcodes.ACONST_NULL);
	    }
	    else {
	       super.visitVarInsn(typeToLoadInsn(l.getType()), l.getIndex());
	    }
	    break;
	 case FIELD:
	    String fname = IvyXml.getAttrString(e, "VALUE");
	    String cname = IvyXml.getAttrString(e, "VALUE2");
	    if (cname.equals(class_name)) {
	       DypatchField f = field_map.get(fname);
	       if (!f.isStatic()) {
		  super.visitVarInsn(Opcodes.ALOAD, 0);
		  super.visitFieldInsn(Opcodes.GETFIELD, cname, fname,
			   descArray[argIndex].getDescriptor());
	       }
	    }
	    super.visitFieldInsn(Opcodes.GETSTATIC, cname, fname,
		     descArray[argIndex].getDescriptor());
	    break;
	 case STACK:
	    stack = false;
	    break;
	 case MULTIPLE:
	    handleMultipleArg(e, descArray[argIndex]);
	    break;
	 // Remainder unimplemented and unused thus far.
	 case RETURN:
	 case LOCAL:
	 case REFBLOCKID:
	 case ARG:
	 case CONTEXT:
	 case NULL:
	 default:
	    visitInsn(Opcodes.ACONST_NULL);
	    break;
      }
      if (stack) super.visitInsn(Opcodes.SWAP);
      ++argIndex;
   }
}

/*
 * Returns true if patch's method requires argument on top of stack False
 * otherwise
 */
private boolean requiresStack(Element xml)
{
   for (Element e : IvyXml.elementsByTag(xml, "ARG")) {
      if (IvyXml.getAttrString(e, "TYPE").equals("STACK")) {
	 return true;
      }
   }
   return false;
}

/*
 * Handles pushing argument that requires multiple steps. (e.g. push field, call
 * method)
 */
@SuppressWarnings("fallthrough")
private void handleMultipleArg(Element xml,Type argType)
{
   for (Element e : IvyXml.children(xml, "ACCESS")) {
      int op = -1;
      AccessType at = IvyXml.getAttrEnum(e, "TYPE", AccessType.NONE);
      switch (at) {
	 default:
	 case NONE:
	    break;
	 case LOCAL:
	    int index = IvyXml.getAttrInt(e, "LOCAL");
	    DypatchLocal l = local_vars.get(index);
	    if (l == null) {
	       break;
	    }
	    super.visitVarInsn(typeToLoadInsn(l.getType()), index);
	    continue;
	 case STATICFIELD:
	    op = Opcodes.GETSTATIC;
	    //FALLTHROUGH
	 case FIELD: {
	    if (op == -1) op = Opcodes.GETFIELD;
	    String owner = IvyXml.getAttrString(e, "CLASS");
	    String type = IvyXml.getAttrString(e, "RESULT");
	    String name = IvyXml.getAttrString(e, "MEMBER");
	    if (owner == null || type == null || name == null) break;
	    super.visitFieldInsn(Opcodes.GETFIELD, xmlToAsmDesc(owner), name,
		     xmlToAsmDesc(type));
	    continue;
	 }
	 case STATICMETHOD0:
	    op = Opcodes.INVOKESTATIC;
	    //FALLTHROUGH
	 case METHOD0: {
	    if (op == -1) op = Opcodes.INVOKEVIRTUAL;
	    Type type = getReturnType(e, argType);
	    String owner = IvyXml.getAttrString(e, "CLASS");
	    String name = IvyXml.getAttrString(e, "MEMBER");
	    super.visitMethodInsn(op, xmlToAsmDesc(owner), name,
		     Type.getMethodDescriptor(type),false);
	    continue;
	 }
      }
      super.visitInsn(Opcodes.ACONST_NULL);
   }
}

/*
 * Returns return type of intermediate method call in handleMultipleArg()
 */
private Type getReturnType(Node n,Type t)
{
   Element e;
   int depth = 0;
   do {
      Node next = n.getNextSibling();
      if (next == null) {
	 return t;
      }
      if (next instanceof Element) {
	 e = (Element) next;
      }
      else {
	 n = next;
	 continue;
      }
      switch (IvyXml.getAttrEnum(next, "TYPE", AccessType.NONE)) {
	 case FIELD:
	 case METHOD0:
	    String s = IvyXml.getAttrString(e, "CLASS");
	    if (depth > 0) {
	       --depth;
	       continue;
	    }
	    if (s == null) return t;
	    s = "L" + s.replace('.', '/') + ";";
	    return Type.getType(s);
	 case NONE:
	    n = next;
	    continue;
	 default:
	    ++depth;
	    n = next;
	    continue;
      }
   }
   while (true);
}

/*
 * Stores return of patch's method call in local variable if necessary
 */
private void handleReturn(Element xml)
{
   if (xml == null) return;
   String type = IvyXml.getAttrString(xml, "TYPE");
   switch (type) {
      case "NEWLOCAL":
	 String value = IvyXml.getAttrString(xml, "VALUE");
	 int index = local_names.get(value);
	 DypatchLocal local = local_vars.get(index);
	 int op = typeToStoreInsn(local.getType());
	 super.visitVarInsn(op, index);
	 break;
   }
}

/********************************************************************************/
/*										*/
/* Misc. Methods								*/
/*										*/
/********************************************************************************/

// Get ID of current class
private int getClassId()
{
   return getClassId(class_name);
}

private int getClassId(String name)
{
   name = name.replace('/', '.');
   return id_numbers.get(name);
}

// Get ID of current method
private int getMethodId()
{
   return getMethodId(class_name, method_name, method_desc);
}

private int getMethodId(String owner,String name,String signature)
{
   owner = owner.replace('/', '.');
   String key = owner + "%" + name + asmToXmlMethodSig(signature);
   Integer i = id_numbers.get(key);
   return i;
}

// Get ID of current block
private int getBlockId()
{
   return getBlockId(class_name, method_name, method_desc, ins_number);
}

private int getBlockId(String owner,String name,String signature,int insNo)
{
   owner = owner.replace('/', '.');
   String key = owner + "%" + name + asmToXmlMethodSig(signature) + "%" + block_number;
   Integer i = id_numbers.get(key);
   return i;
}


/*
 * Returns first index of local variable array large enough to store needed
 * value
 */
private int getVarIndex(boolean doubleSize)
{
   int i;
   int cap = local_vars.size();
   for (i = 0; i < cap; i++) {
      if (!local_vars.containsKey(i)) {
	 if (doubleSize && local_vars.containsKey(i + 1)) {
	    ++i;
	    ++cap;
	    continue;
	 }
	 break;
      }
   }
   return i;
}


/*
 * Creates DypatchLocal to represent variable and adds to map of local variables
 */
@SuppressWarnings("fallthrough")
private DypatchLocal addLocalVariable(Type type,int var,String name)
{
   DypatchLocal l = new DypatchLocal(name,type,var,null);
   switch (type.getSort()) {
      case Type.METHOD:
	 break; // Prevents method being caught in default
      case Type.DOUBLE:
      case Type.LONG:
	 local_vars.put(var + 1, l);
	 //FALLTHROUGH
      default: // ARRAY, BOOLEAN, BYTE, CHAR, FLOAT, INT, OBJECT, SHORT:
	 local_vars.put(var, l);
	 break;
   }
   return l;
}

private void addLocalVariable(int opcode,int var,String name)
{
   Type t = null;
   switch (opcode) {
      case Opcodes.ISTORE:
	 t = Type.INT_TYPE;
	 break;
      case Opcodes.LSTORE:
	 t = Type.LONG_TYPE;
	 break;
      case Opcodes.FSTORE:
	 t = Type.FLOAT_TYPE;
	 break;
      case Opcodes.DSTORE:
	 t = Type.DOUBLE_TYPE;
	 break;
      case Opcodes.ASTORE:
	 t = Type.getType("Ljava/lang/Object;");
	 break;
   }
   if (t != null) addLocalVariable(t, var, name);
}
}

/********************************************************************************/
/*										*/
/* Visitors									*/
/*										*/
/********************************************************************************/

@Override public void visit(int version,int access,String name,String signature,
	 String superName,String[] interfaces)
{
   class_name = name;
   super_class_name = superName;
   for (Element e : xml_patches) {
      String forClass = IvyXml.getAttrString(e, "CLASS").replace('.', '/');
      if (forClass == null || forClass.equals(name) || forClass.equals("*")) {
	 String method = IvyXml.getAttrString(e, "METHOD");
	 Collection<Element> collection;
	 if (method == null) method = "*";
	 collection = patched_methods.get(method);
	 if (collection == null) {
	    collection = new HashSet<Element>();
	    patched_methods.put(method, collection);
	 }
	 collection.add(e);
      }
   }
   super.visit(version, access, name, signature, superName, interfaces);
}

@Override public MethodVisitor visitMethod(int access,String name,String desc,
	 String signature,String[] exceptions)
{
   MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
   String nm1 = name + desc;
   if (patched_methods.containsKey(name) || patched_methods.containsKey("*") ||
	  patched_methods.containsKey(nm1)) {
      DypatchMethodVisitor dmv = new DypatchMethodVisitor(api,mv,access,name,desc);
      return dmv;
   }
   return mv;
}

@Override public FieldVisitor visitField(int access,String name,String desc,
	 String signature,Object value)
{
   field_map.put(name, new DypatchField(access,name,desc));
   return super.visitField(access, name, desc, signature, value);
}

/********************************************************************************/
/*										*/
/* Misc. Static Methods 							*/
/*										*/
/********************************************************************************/

// e.g. "java.net.Socket[][]" -> "[[Ljava/net/Socket;"
public static String xmlToAsmDesc(String desc)
{
   StringBuilder sb = new StringBuilder();
   int braceCount = stringCount(desc, '[');
   for (int i = 0; i < braceCount; i++) {
      sb.append('[');
   }
   if (braceCount > 0) {
      desc = desc.substring(0, desc.indexOf('['));
   }
   switch (desc) {
      case "boolean":
	 sb.append('Z');
	 break;
      case "byte":
	 sb.append('B');
	 break;
      case "char":
	 sb.append('C');
	 break;
      case "double":
	 sb.append('D');
	 break;
      case "float":
	 sb.append('F');
	 break;
      case "int":
	 sb.append('I');
	 break;
      case "long":
	 sb.append('J');
	 break;
      case "short":
	 sb.append('S');
	 break;
      default:
	 sb.append('L');
	 sb.append(desc.replace('.', '/'));
	 sb.append(';');
   }
   return sb.toString();
}

public static String xmlToAsmMethodDesc(String desc,Type t)
{
   StringBuilder sb = new StringBuilder();
   int start = desc.indexOf('(');
   int end = desc.indexOf(')', start);
   if (start < 0 || end < 0) return null;
   String[] descArray = desc.substring(start + 1, end).split(",");
   sb.append('(');
   for (String s : descArray) {
      sb.append(xmlToAsmDesc(s));
   }
   sb.append(')');
   if (t != null) sb.append(t.getDescriptor());
   return sb.toString();
}

// e.g. "[[Ljava/net/Socket;" -> "java.net.Socket[][]"
public static String asmToXmlDesc(String name)
{
   StringBuilder sb = new StringBuilder();
   int arrayDepth = 0;
   while (name.charAt(arrayDepth) == '[')
      ++arrayDepth;
   switch (name.charAt(arrayDepth)) {
      case 'B':
	 sb.append("byte");
	 break;
      case 'C':
	 sb.append("char");
	 break;
      case 'D':
	 sb.append("double");
	 break;
      case 'F':
	 sb.append("float");
	 break;
      case 'I':
	 sb.append("int");
	 break;
      case 'J':
	 sb.append("long");
	 break;
      case 'S':
	 sb.append("short");
	 break;
      case 'Z':
	 sb.append("boolean");
	 break;
      case 'L':
	 String className = name.substring(arrayDepth + 1,
		  name.indexOf(';', arrayDepth + 1));
	 className = className.replace('/', '.');
	 sb.append(className);
   }
   for (int i = 0; i < arrayDepth; i++) {
      sb.append("[]");
   }
   return sb.toString();
}

public static String asmToXmlMethodSig(String desc)
{
   StringBuilder sb = new StringBuilder();
   Type[] args = Type.getArgumentTypes(desc);
   sb.append('(');
   for (Type t : args) {
      sb.append(asmToXmlDesc(t.getDescriptor()));
      sb.append(',');
   }
   if (sb.charAt(sb.length() - 1) == ',') sb.deleteCharAt(sb.length() - 1);
   sb.append(')');
   return sb.toString();
}

public static int typeToLoadInsn(Type t)
{
   return typeToInsn(t, false);
}

public static int typeToStoreInsn(Type t)
{
   return typeToInsn(t, true);
}

public static int typeToInsn(Type t,boolean store)
{
   int sort = t.getSort();
   int op = -1;
   switch (sort) {
      case Type.METHOD:
      case Type.VOID:
	 break;
      case Type.LONG:
	 if (store) op = Opcodes.LSTORE;
	 else op = Opcodes.LLOAD;
	 break;
      case Type.FLOAT:
	 if (store) op = Opcodes.FSTORE;
	 else op = Opcodes.FLOAD;
	 break;
      case Type.DOUBLE:
	 if (store) op = Opcodes.DSTORE;
	 else op = Opcodes.DLOAD;
	 break;
      case Type.ARRAY:
      case Type.OBJECT:
	 if (store) op = Opcodes.ASTORE;
	 else op = Opcodes.ALOAD;
	 break;
      default: // BOOLAN, BYTE, CHAR, INT, SHORT
	 if (store) op = Opcodes.ISTORE;
	 else op = Opcodes.ILOAD;
	 break;
   }
   return op;
}

/*
 * Returns number of instances of char c in String s;
 */
public static int stringCount(String s,char c)
{
   int count = 0;
   for (int i = 0; i < s.length(); i++) {
      if (s.charAt(i) == c) {
	 ++count;
      }
   }
   return count;
}

public static boolean isStatic(int access)
{
   return (access & Opcodes.ACC_STATIC) != 0;
}

private static boolean isStoreInsn(int opcode)
{
   if (opcode == Opcodes.ALOAD || opcode == Opcodes.DLOAD || opcode == Opcodes.FLOAD
	    || opcode == Opcodes.ILOAD || opcode == Opcodes.LLOAD) {
      return false;
   }
   return true;
}

}
