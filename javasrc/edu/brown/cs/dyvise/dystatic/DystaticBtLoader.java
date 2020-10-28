/********************************************************************************/
/*										*/
/*		DystaticBtLoader.java						*/
/*										*/
/*	DYVISE static analysis JikesBt to database loader			*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dystatic/DystaticBtLoader.java,v 1.5 2012-10-05 00:53:02 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DystaticBtLoader.java,v $
 * Revision 1.5  2012-10-05 00:53:02  spr
 * Code clean up.
 *
 * Revision 1.4  2011-03-10 02:33:15  spr
 * Code cleanup.
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


import edu.brown.cs.ivy.cinder.CinderManager;

import com.ibm.jikesbt.*;

import java.io.File;
import java.util.*;


public class DystaticBtLoader extends DystaticLoader implements DystaticConstants, BT_Opcodes
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private CinderManager	cinder_manager;
private BT_Ins		start_monitor;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DystaticBtLoader(DystaticMain dm)
{
   super(dm);

   String bp = CinderManager.computeBasePath();
   String cp = null;
   for (String s : getProject().getClassPath()) {
      if (cp == null) cp = s;
      else cp = cp + File.pathSeparator + s;
    }
   // System.err.println("CLASS PATH: " + cp);
   CinderManager.setClassPath(bp,cp);

   cinder_manager = new CinderManager();
   cinder_manager.setPatchAll(true);
   // cinder_manager.do_debug = true;
   start_monitor = null;
}




/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

void processLoad()
{
   startLoad();

   Set<BT_Class> done = new HashSet<BT_Class>();

   for (String s : getProject().getStartClasses()) {
      CinderManager.checkIfClassExists(s);
    }

   boolean chng = true;
   while (chng) {
      chng = false;
      for (BT_Class bc : cinder_manager.getAllClassSet()) {
	 if (done.contains(bc)) continue;
	 done.add(bc);
	 if (bc.isStub()) continue;
	 chng = true;
	 processClass(bc);
	 String ac = getAlternateClass(bc.getName());
	 if (ac != null) {
	    CinderManager.checkIfClassExists(ac);
	  }
       }
    }

   finishLoad();
}




private void processClass(BT_Class bc)
{
   String typ = typeName(bc);
   boolean ufg = getProject().isUserClass(bc.fullName());

   addClassEntry(bc.fullName(),typ,getSignature(bc),bc.getSuperClassName(),bc.flags,ufg);

   for (Enumeration<?> e = bc.getParents().elements(); e.hasMoreElements(); ) {
      BT_Class pc = (BT_Class) e.nextElement();
      addInterfaceEntry(bc.fullName(),pc.fullName(),pc.isInterface());
    }

   for (Enumeration<?> e = bc.getFields().elements(); e.hasMoreElements(); ) {
      BT_Field bf = (BT_Field) e.nextElement();
      processField(bf);
    }

   for (Enumeration<?> e = bc.getMethods().elements(); e.hasMoreElements(); ) {
      BT_Method bm = (BT_Method) e.nextElement();
      processMethod(bm);
    }
}



private void processField(BT_Field bf)
{
   String id = getNewId("F");

   addFieldEntry(id,bf.getDeclaringClass().fullName(),bf.getName(),bf.flags,
		    typeName(bf.type),getSignature(bf));
}




private void processMethod(BT_Method bm)
{
   String id = getNewId("M");

   BT_MethodSignature ms = bm.getSignature();

   addMethodEntry(id,bm.getDeclaringClass().fullName(),bm.getName(),bm.flags,
		     ms.toString(),getSignature(bm),ms.types.size(),
		     typeName(ms.returnType));

   processCode(id,bm,bm.getCode());
}



private void processCode(String mid,BT_Method bm,BT_CodeAttribute ca)
{
   if (ca == null) return;
   if (bm.isSynchronized()) {
      handleLock(ca,null,null);
    }

   BT_MethodSignature ms = bm.getSignature();

   int asz = ms.getArgsSize();
   if (!bm.isStatic()) asz += 1;

   boolean haveparms = false;
   for (Enumeration<?> e = ca.attributes.elements(); e.hasMoreElements(); ) {
      BT_Attribute ba = (BT_Attribute) e.nextElement();
      if (ba.getName().equals("LocalVariableTable")) {
	 BT_LocalVariableAttribute lva = (BT_LocalVariableAttribute) ba;
	 if (lva.localVariables.length > 0) {
	    processParameters(mid,lva,asz);
	    haveparms = true;
	  }
       }
      else if (ba.getName().equals("LineNumberTable")) {
	 BT_LineNumberAttribute lna = (BT_LineNumberAttribute) ba;
	 processLines(mid,lna);
       }
    }

   if (!haveparms) {
      int idx = 0;
      if (!bm.isStatic()) {
	 addParamEntry(mid,"this",typeName(bm.getDeclaringClass()),null,idx++);
       }
      int idn = 1;
      for (Enumeration<?> e = ms.types.elements(); e.hasMoreElements(); ) {
	 BT_Class pty = (BT_Class) e.nextElement();
	 String nm = "P" + (idn++);
	 addParamEntry(mid,nm,typeName(pty),null,idx);
	 idx += pty.getSizeForLocal();
       }
    }

   start_monitor = null;
   for (Enumeration<?> e = ca.ins.elements(); e.hasMoreElements(); ) {
      BT_Ins ins = (BT_Ins) e.nextElement();
      processInstruction(ca,mid,ins);
    }
}



private void processParameters(String mid,BT_LocalVariableAttribute lva,int asz)
{
   for (BT_LocalVariableAttribute.LV lv : lva.localVariables) {
      if (lv.localIndex < asz) {
	 addParamEntry(mid,lv.nameS,typeName(lv.descriptorC),null,
			      lv.localIndex);
       }
    }
}




private void processLines(String mid,BT_LineNumberAttribute lna)
{
   for (BT_LineNumberAttribute.PcRange pc : lna.pcRanges) {
      addLineEntry(mid,pc.lineNumber,pc.startIns.byteIndex);
    }
}



private void processInstruction(BT_CodeAttribute ca,String mid,BT_Ins ins)
{
   switch (ins.opcode) {
      default :
	 break;
      case opc_invokeinterface :
      case opc_invokespecial :
      case opc_invokestatic :
      case opc_invokevirtual :
	 BT_MethodRefIns mri = (BT_MethodRefIns) ins;
	 addCallEntry(mid,mri.target.getDeclaringClass().fullName(),mri.target.getName(),
			 mri.target.getSignature().toString());
	 break;
      case opc_new :
	 BT_NewIns nwi = (BT_NewIns) ins;
	 addAllocEntry(mid,nwi.getTarget().fullName());
	 break;
      case opc_monitorenter :
	 start_monitor = ins;
	 break;
      case opc_monitorexit :
	 handleLock(ca,start_monitor,ins);
	 start_monitor = null;
	 break;
   }
}





private String getSignature(BT_Item itm)
{
   String sgn = null;

   for (Enumeration<?> e = itm.attributes.elements(); e.hasMoreElements(); ) {
      BT_Attribute ba = (BT_Attribute) e.nextElement();
      if (ba.getName().equals("Signature")) {
	 BT_SignatureAttribute sa = (BT_SignatureAttribute) ba;
	 sgn = sa.signature;
       }
    }

   return sgn;
}



private String typeName(BT_Class bc)
{
   return BT_ConstantPool.toInternalName(bc);
}



private void handleLock(BT_CodeAttribute ca,BT_Ins start,BT_Ins end)
{
   // boolean havewait = false;
   // boolean havenotify = false;

   if (start == null) {
      start = ca.ins.elementAt(0);
    }
   int sidx = ca.ins.indexOf(start);
   int eidx = (end == null ? ca.ins.size()-1 : ca.ins.indexOf(end));

   for (int i = sidx; i <= eidx; ++i) {
      BT_Ins lins = ca.ins.elementAt(i);
      switch (lins.opcode) {
	 case opc_invokeinterface :
	 case opc_invokespecial :
	 case opc_invokestatic :
	 case opc_invokevirtual :
	    BT_MethodRefIns mri = (BT_MethodRefIns) lins;
	    if (mri.getTarget().getDeclaringClass() == BT_Class.findJavaLangObject()) {
	       if (mri.target.getName().startsWith("wait")) {
		  //  havewait = true;
		}
	       else if (mri.target.getName().startsWith("notify")) {
		  // havenotify = true;
	        }
	     }
	    break;
      }
    }
}


/***********************
private void findConditionFields(BT_CodeAttribute ca,int idx)
{
   int nop = 0;
   for (int i = idx-1l i >= 0; --i) {
      BT_Ins ins = ca.ins.elementAt(i);
      switch (ins.opcode) {
	 case opc_if_acmpeq :
	 case opc_if_acmpne :
	 case opc_if_icmpeq :
	 case opc_if_icmpge :
	 case opc_if_icmpgt :
	 case opc_if_icmple :
	 case opc_if_icmplt :
	 case opc_if_icmpne :
	    nop += 2;
	    break;
	 case opc_ifeq :
	 case opc_ifge :
	 case opc_ifgt :
	 case opc_ifle :
	 case opc_iflt :
	 case opc_ifne :
	 case opc_ifnonnull :
	 case opc_ifnull :
	    nop += 1;
	    break;
	 case opc_lookupswitch :
	 case opc_tableswitch :
	    nop += 1;
	    break;

	 case opc_iload : case opc_iload_0 : case opc_iload_1 : case opc_iload_2 : case opc_iload_3 :
	 case opc_lload : case opc_lload_0 : case opc_lload_1 : case opc_lload_2 : case opc_lload_3 :
	 case opc_aload : case opc_aload_0 : case opc_aload_1 : case opc_aload_2 : case opc_aload_3 :

	    llins = (BT_LoadLocalIns) ins;
	    where = new WhereItem(llins.localNr,fld);
	    break;
	 case opc_dup : case opc_dup_x1 : case opc_dup_x2 : case opc_dup2 : case opc_dup2_x1 :
	 case opc_dup2_x2 :
	    break;
	 case opc_getfield :
	    if (fld != null) return null;
	    frins = (BT_FieldRefIns) ins;
	    fld = frins.getFieldTarget();
	    break;
	 case opc_getstatic :
	    if (fld != null) return null;
	    frins = (BT_FieldRefIns) ins;
	    fld = frins.getFieldTarget();
	    where = new WhereItem(fld);
	    break;
	 case opc_aaload :
	    // possibly handle this
	    return null;
	 case opc_instanceof :
	    // handle instanceof at some point
	    return null;
	 case opc_istore : case opc_istore_0 : case opc_istore_1 : case opc_istore_2 : case opc_istore_3 :
	 case opc_lstore : case opc_lstore_0 : case opc_lstore_1 : case opc_lstore_2 : case opc_lstore_3 :
	 case opc_astore : case opc_astore_0 : case opc_astore_1 : case opc_astore_2 : case opc_astore_3 :
	    slins = (BT_StoreLocalIns) ins;
	    if (ino >= 1) {
	       BT_Ins pins = wq.getInstruction(ino-1);
	       switch (pins.opcode) {
		  case opc_dup : case opc_dup_x1 : case opc_dup_x2 : case opc_dup2 :
		  case opc_dup2_x1 : case opc_dup2_x2 :
		     where = new WhereItem(slins.localNr,fld);
		     break;
		  case opc_isub :
		     // check for iconst 1, dup preceding this and use value-1
		     return null;
		  default :
		     return null;
		}
	     }
	    break;
	 default :
	    return null;
       }
      --ino;
    }
}
*******************/



}	// end of class DystaticLoader




/* end of DystaticLoader.java */

