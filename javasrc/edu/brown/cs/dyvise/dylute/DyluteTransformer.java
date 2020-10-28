/********************************************************************************/
/*										*/
/*		DyluteTransformer.java						*/
/*										*/
/*	CLass Instrumentation for DYnamic Lock UTilization Experiencer		*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/dylute/src/DyluteTransformer.java,v 1.4 2016/11/02 18:59:05 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyluteTransformer.java,v $
 * Revision 1.4  2016/11/02 18:59:05  spr
 * Move to asm5
 *
 * Revision 1.3  2013-06-03 13:02:49  spr
 * Minor bug fixes
 *
 * Revision 1.2  2013-05-09 12:28:55  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.1  2011-09-12 19:37:26  spr
 * Add dylute files to repository
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dylute;

// NOTE: This should only use system libraries and libraries that are explicitly
//    in this package.	Using anything else might cause conflicts if the application
//    being monitored uses the same library.

import edu.brown.cs.dyvise.dylute.org.objectweb.asm.*;
import edu.brown.cs.dyvise.dylute.org.objectweb.asm.commons.*;
import edu.brown.cs.dyvise.dylute.org.objectweb.asm.util.*;


import java.util.*;
import java.lang.reflect.Modifier;



class DyluteTransformer extends ClassVisitor implements DyluteConstants {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private List<Location>	lock_set;
private String		class_name;
private LocalVariablesSorter local_vars;
private CodeSizeEvaluator size_eval;

private static boolean	do_debug = false;

private static int	OFFSET_DELTA = 0;

private static int ASM_API = Opcodes.ASM5;




/********************************************************************************/
/*										*/
/*	Constructores								*/
/*										*/
/********************************************************************************/

DyluteTransformer(Class<?> cls,String name,List<Location> locks,ClassVisitor cvx)
{
   super(ASM_API,cvx);
   class_name = name;
   lock_set = locks;
}



/********************************************************************************/
/*										*/
/*	Claas Member Visitor for methods					*/
/*										*/
/********************************************************************************/

@Override public MethodVisitor visitMethod(int acc,String name,String desc,String sgn,String [] exc)
{
   List<Location> mlocks = new ArrayList<Location>();
   boolean fixsynch = false;
   for (Location l : lock_set) {
      if (l.getMethodName().equals(name) &&
	     l.getMethodSignature().equals(desc)) {
	 mlocks.add(l);
	 if (l.getMethodOffset() == 0 && Modifier.isSynchronized(acc)) fixsynch = true;
       }
    }

   if (fixsynch) {
      acc &= ~ Modifier.SYNCHRONIZED;
    }

   MethodVisitor mv = super.visitMethod(acc,name,desc,sgn,exc);

   if (do_debug) mv = new TraceMethodVisitor(mv,new Textifier());

   if (mlocks.size() > 0) mv = new RegionFixer(mv,mlocks);
   mv = new WaitNotifyFixer(mv);

   if (fixsynch) {
      if (Modifier.isStatic(acc)) mv = new SynchFixer(mv,class_name);
      else mv = new SynchFixer(mv,null);
    }

   local_vars = new LocalVariablesSorter(acc,desc,mv);
   mv = local_vars;

   size_eval = new CodeSizeEvaluator(mv);
   mv = size_eval;

   return mv;
}



/********************************************************************************/
/*										*/
/*	Method Visitor to handle removing synchronized modifier 		*/
/*										*/
/********************************************************************************/

// If constructors can be syncronized, this won't work
//    here the monitorenter has to occur after the super call

private class SynchFixer extends MethodVisitor {

   private Type static_type;
   private Label start_label;
   private Label catch_label;
   private Label end_label;
   private boolean done_catch;
   private int	 synch_local;

   SynchFixer(MethodVisitor xmv,String sty) {
      super(ASM_API,xmv);
      if (sty == null) static_type = null;
      else static_type = Type.getType(sty);
      start_label = new Label();
      end_label = new Label();
      catch_label = new Label();
      done_catch = false;
    }

   @Override public void visitCode() {
      super.visitCode();
      synch_local = local_vars.newLocal(Type.getObjectType("java/lang/Object"));
      if (do_debug) System.err.println("DYLUTE: LOCAL VARIABLE " + synch_local);
      Label vstart = new Label();
      // super.visitLocalVariable("$sync","Ljava/lang/Object;",null,vstart,end_label,synch_local);
      loadThis();
      super.visitInsn(Opcodes.DUP);
      super.visitLabel(vstart);
      super.visitVarInsn(Opcodes.ASTORE,synch_local);
      super.visitInsn(Opcodes.MONITORENTER);
      super.visitLabel(start_label);
    }

   @Override public void visitInsn(int opc) {
      switch (opc) {
	 case Opcodes.IRETURN :
	 case Opcodes.LRETURN :
	 case Opcodes.FRETURN :
	 case Opcodes.DRETURN :
	 case Opcodes.ARETURN :
	 case Opcodes.RETURN :
	    super.visitVarInsn(Opcodes.ALOAD,synch_local);
	    super.visitInsn(Opcodes.MONITOREXIT);
	    Label next = new Label();
	    super.visitLabel(next);
	    super.visitTryCatchBlock(start_label,next,catch_label,"java/lang/Throwable");
	    super.visitInsn(opc);
	    start_label = new Label();
	    super.visitLabel(start_label);
	    return;
       }
      super.visitInsn(opc);
    }

   @Override public void visitMaxs(int maxs,int maxl) {
      generateTail();
      super.visitMaxs(maxs,maxl);
    }

   @Override public void visitEnd() {
      generateTail();
      super.visitEnd();
    }

   private void generateTail() {
      if (done_catch) return;
      done_catch = true;
      super.visitLabel(catch_label);
      super.visitVarInsn(Opcodes.ALOAD,synch_local);
      super.visitInsn(Opcodes.MONITOREXIT);
      Label next = new Label();
      super.visitLabel(next);
      super.visitInsn(Opcodes.ATHROW);
      super.visitTryCatchBlock(catch_label,next,catch_label,"java/lang/Throwable");
      super.visitLabel(end_label);
    }

   private void loadThis() {
      if (static_type == null) {
	 super.visitVarInsn(Opcodes.ALOAD,0);
       }
      else {
	 super.visitLdcInsn(static_type);
       }
    }

}	// end of inner class SynchFixer



/********************************************************************************/
/*										*/
/*	Synchronized region fixup calls 					*/
/*										*/
/********************************************************************************/

private class RegionFixer extends MethodVisitor {

   private List<Location> method_locks;
   private boolean last_enter;
   private int	   last_store;
   private int	   last_load;
   private int	   mon_local;
   private Set<Integer> active_locks;

   RegionFixer(MethodVisitor mvx,List<Location> locks) {
      super(ASM_API,mvx);
      method_locks = locks;
      last_enter = false;
      last_store = -1;
      last_load = -1;
      mon_local = -1;
      active_locks = new HashSet<Integer>();
    }

   @Override public void visitLabel(Label lbl) {
      super.visitLabel(lbl);
      if (last_enter) {
         super.visitVarInsn(Opcodes.ALOAD,mon_local);
         super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_ENTERED,MONITOR_ENTERED_ARGS,false);
         last_enter = false;
       }
    }

   @Override public void visitVarInsn(int opc,int var) {
      if (opc == Opcodes.ASTORE) last_store = var;
      else if (opc == Opcodes.ALOAD) last_load = var;
      super.visitVarInsn(opc,var);
    }

   @Override public void visitInsn(int opc) {
      switch (opc) {
         case Opcodes.MONITORENTER :
            int minsz = Math.max(size_eval.getMinSize() - 1,0);
            int maxsz = size_eval.getMaxSize();
            int lid = -1;
            for (Location l : method_locks) {
               if (l.getMethodOffset() >= minsz-OFFSET_DELTA && l.getMethodOffset() <= maxsz+OFFSET_DELTA) {
                  if (do_debug) System.err.println("ENTER " + l.getMethodOffset() + " " + minsz + " " + maxsz);
                  lid = l.getLockId();
                  break;
                }
             }
            if (lid < 0) {
               super.visitInsn(opc);
               break;
             }
            super.visitInsn(Opcodes.DUP);
            super.visitLdcInsn(Integer.valueOf(lid));
            super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_ENTER,MONITOR_ENTER_ARGS,false);
            super.visitInsn(Opcodes.DUP);
            super.visitVarInsn(Opcodes.ASTORE,last_store);
            super.visitInsn(opc);
            // do the rest after the start label
            last_enter = true;
            mon_local = last_store;
            active_locks.add(last_store);
            break;
         case Opcodes.MONITOREXIT :
            if (active_locks.contains(last_load)) {
               super.visitInsn(Opcodes.DUP);
               super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_EXIT,MONITOR_EXIT_ARGS,false);
             }
            super.visitInsn(opc);
            break;
         default :
            super.visitInsn(opc);
            break;
       }
    }

}	// end of inner class RegionFixer



/********************************************************************************/
/*										*/
/*	Handle class to wait and notify in this class				*/
/*										*/
/********************************************************************************/

private class WaitNotifyFixer extends MethodVisitor {

   WaitNotifyFixer(MethodVisitor mvx) {
      super(ASM_API,mvx);
    }

   @Override public void visitMethodInsn(int opc,String cls,String mnm,String desc,boolean itf) {
      if (cls.equals("java/lang/Object")) {
         if (mnm.equals("notify") || mnm.equals("notifyAll")) {
            super.visitInsn(Opcodes.DUP);
            super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_NOTIFY,MONITOR_NOTIFY_ARGS,false);
            super.visitMethodInsn(opc,cls,mnm,desc,itf);
            return;
          }
         else if (mnm.equals("wait") && desc.equals("()V")) {
            super.visitInsn(Opcodes.DUP);
            super.visitInsn(Opcodes.DUP);
            super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_WAIT,MONITOR_WAIT_ARGS,false);
            super.visitMethodInsn(opc,cls,mnm,desc,itf);
            super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_WAITED,MONITOR_WAITED_ARGS,false);
            return;
          }
         else if (mnm.equals("wait") && desc.equals("(J)V")) {
            super.visitInsn(Opcodes.DUP2_X1);
            super.visitInsn(Opcodes.POP2);
            super.visitInsn(Opcodes.DUP);
            super.visitInsn(Opcodes.DUP2_X2);
            super.visitInsn(Opcodes.POP);
            super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_WAIT,MONITOR_WAIT_ARGS,false);
            super.visitMethodInsn(opc,cls,mnm,desc,itf);
            super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_WAITED,MONITOR_WAITED_ARGS,false);
            return;
          }
         else if (mnm.equals("wait") && desc.equals("(JI)V")) {
          }
       }
      super.visitMethodInsn(opc,cls,mnm,desc,itf);
    }

}	// end of inner class WaitNotifyFixer





}	// end of class DyluteTransformer




/* end of DyluteTransformer.java */
