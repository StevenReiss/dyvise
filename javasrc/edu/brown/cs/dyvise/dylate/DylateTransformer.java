/********************************************************************************/
/*										*/
/*		DylateTransformer.java						*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dylate/DylateTransformer.java,v 1.4 2016/11/02 18:59:07 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DylateTransformer.java,v $
 * Revision 1.4  2016/11/02 18:59:07  spr
 * Move to asm5
 *
 * Revision 1.3  2013-06-03 13:02:55  spr
 * Minor bug fixes
 *
 * Revision 1.2  2013-05-09 12:28:58  spr
 * Dylock update.	Last 1.6 update
 *
 * Revision 1.1  2012-10-05 00:52:26  spr
 * New lock tracer
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dylate;

// NOTE: This should only use system libraries and libraries that are explicitly
//    in this package.	Using anything else might cause conflicts if the application
//    being monitored uses the same library.

import edu.brown.cs.dyvise.dylute.org.objectweb.asm.*;
import edu.brown.cs.dyvise.dylute.org.objectweb.asm.commons.*;
import edu.brown.cs.dyvise.dylute.org.objectweb.asm.util.*;

import java.util.*;
import java.lang.reflect.Modifier;
import java.io.Writer;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicInteger;


class DylateTransformer extends ClassVisitor implements DylateConstants {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		class_name;
private LocalVariablesSorter local_vars;
private CodeSizeEvaluator size_eval;
private String		cur_method;
private String		cur_signature;
private Integer 	last_location;
private Writer		info_writer;
private boolean 	old_file;
private List<Location>	location_set;


private static AtomicInteger location_ctr = new AtomicInteger(0);

private static int	OFFSET_DELTA = 0;

static int		ASM_API = Opcodes.ASM6;

private static boolean	do_debug = true;





/********************************************************************************/
/*										*/
/*	Constructores								*/
/*										*/
/********************************************************************************/

DylateTransformer(Class<?> cls,String name,ClassVisitor cvx,
      List<Location> locations,Writer iw,boolean old)
{
   super(ASM_API,cvx);
   class_name = name;
   last_location = 0;
   local_vars = null;
   size_eval = null;
   info_writer = iw;
   old_file = old;
   location_set = locations;
}



/********************************************************************************/
/*										*/
/*	Claas Member Visitor for methods					*/
/*										*/
/********************************************************************************/

@Override public MethodVisitor visitMethod(int acc,String name,String desc,String sgn,String [] exc)
{
   cur_method = name;
   cur_signature = desc;

   boolean fixsynch = false;
   if (Modifier.isSynchronized(acc) && !Modifier.isNative(acc)) {
      if (!old_file || !Modifier.isStatic(acc)) {
	 fixsynch = true;
	 acc &= ~Modifier.SYNCHRONIZED;
       }
    }

   MethodVisitor mv = super.visitMethod(acc,name,desc,sgn,exc);

   if (do_debug) mv = new MethodTracer(mv,name,desc);

   mv = new RegionFixer(mv);
   mv = new WaitNotifyFixer(mv);
   mv = new ConcurrentFixer(mv);

   if (fixsynch) {
      if (Modifier.isStatic(acc)) {
	 System.err.println("CLASS NAME: " + class_name);
	 String nm = "L" + class_name.replace(".","/") + ";";
	 mv = new SynchFixer(mv,nm);
       }
      else mv = new SynchFixer(mv,null);
    }

   local_vars = new LocalVariablesSorter(acc,desc,mv);
   mv = local_vars;

   size_eval = new CodeSizeEvaluator(mv);
   mv = size_eval;

   // mv = new CheckMethodAdapter(acc,name,desc,mv,new HashMap<Label,Integer>());
   // CheckMethodAdapter cma = (CheckMethodAdapter) mv;
   // cma.version = 51;

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

   SynchFixer(MethodVisitor mvx,String sty) {
      super(ASM_API,mvx);
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
      if (do_debug) System.err.println("DYLATE: LOCAL VARIABLE " + synch_local);
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

   private boolean last_enter;
   private int	   last_store;
   private int	   last_load;
   private int	   mon_local;
   private Set<Integer> active_locks;
   private Map<Label,Label> label_map;

   RegionFixer(MethodVisitor mvx) {
      super(ASM_API,mvx);
      last_enter = false;
      last_store = -1;
      last_load = -1;
      mon_local = -1;
      active_locks = new HashSet<Integer>();
      label_map = new HashMap<Label,Label>();
    }

   @Override public void visitLabel(Label lbl) {
      super.visitLabel(lbl);
      if (last_enter) {
	 super.visitVarInsn(Opcodes.ALOAD,mon_local);
	 super.visitLdcInsn(Integer.valueOf(last_location));
	 super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_ENTERED,MONITOR_ENTERED_ARGS,false);
	 Label nlbl = new Label();
	 super.visitLabel(nlbl);
	 label_map.put(lbl,nlbl);
	 last_enter = false;
       }
    }

   @Override public void visitJumpInsn(int opc,Label lbl) {
      Label nlbl = label_map.get(lbl);
      if (nlbl != null) super.visitJumpInsn(opc,nlbl);
      else super.visitJumpInsn(opc,lbl);
    }

   @Override public void visitVarInsn(int opc,int var) {
      if (opc == Opcodes.ASTORE) last_store = var;
      else if (opc == Opcodes.ALOAD) last_load = var;
      super.visitVarInsn(opc,var);
    }

   @Override public void visitInsn(int opc) {
      switch (opc) {
	 case Opcodes.MONITORENTER :
	    last_location = getLocationId();
	    if (last_location == null) {
	       super.visitInsn(opc);
	     }
	    else {
	       super.visitInsn(Opcodes.DUP);
	       super.visitLdcInsn(last_location);
	       super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_ENTER,MONITOR_ENTER_ARGS,false);
	       super.visitInsn(Opcodes.DUP);
	       super.visitVarInsn(Opcodes.ASTORE,last_store);
	       super.visitInsn(opc);
	       // do the rest after the start label
	       last_enter = true;
	       mon_local = last_store;
	       active_locks.add(last_store);
	     }
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
	    Integer loc = getLocationId();
	    if (loc != null) {
	       loc = Integer.valueOf(-loc);
	       super.visitInsn(Opcodes.DUP);
	       super.visitLdcInsn(loc);
	       super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_NOTIFY,MONITOR_NOTIFY_ARGS,false);
	       super.visitMethodInsn(opc,cls,mnm,desc,itf);
	       return;
	     }
	  }
	 else if (mnm.equals("wait") && desc.equals("()V")) {
	    Integer loc = getLocationId();
	    if (loc != null) {
	       loc = Integer.valueOf(-loc);
	       super.visitInsn(Opcodes.DUP);
	       super.visitInsn(Opcodes.DUP);
	       super.visitLdcInsn(loc);
	       super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_WAIT,MONITOR_WAIT_ARGS,false);
	       super.visitMethodInsn(opc,cls,mnm,desc,itf);
	       super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_WAITED,MONITOR_WAITED_ARGS,false);
	       return;
	     }
	  }
	 else if (mnm.equals("wait") && desc.equals("(J)V")) {
	    Integer loc = getLocationId();
	    if (loc != null) {
	       loc = Integer.valueOf(-loc);
	       super.visitInsn(Opcodes.DUP2_X1);
	       super.visitInsn(Opcodes.POP2);
	       super.visitInsn(Opcodes.DUP);
	       super.visitInsn(Opcodes.DUP2_X2);
	       super.visitInsn(Opcodes.POP);
	       super.visitLdcInsn(loc);
	       super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_WAIT,MONITOR_WAIT_ARGS,false);
	       super.visitMethodInsn(opc,cls,mnm,desc,itf);
	       super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_WAITED,MONITOR_WAITED_ARGS,false);
	       return;
	     }
	  }
	 else if (mnm.equals("wait") && desc.equals("(JI)V")) {
	    Integer loc = getLocationId();
	    if (loc != null) {
	       loc = Integer.valueOf(-loc);
	       super.visitLdcInsn(loc);
	       super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_WAIT_TIMED,MONITOR_WAIT_TIMED_ARGS,false);
	       return;
	     }
	  }
       }
      else if (cls.equals("java/lang/Thread")) {
	 if (mnm.equals("join") && desc.equals("()V")) {
	    Integer loc = getLocationId();
	    if (loc != null) {
	       super.visitLdcInsn(Long.valueOf(0));
	       super.visitLdcInsn(Integer.valueOf(0));
	       super.visitLdcInsn(loc);
	       super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_JOIN,MONITOR_JOIN_ARGS,false);
	       return;
	     }
	  }
	 else if (mnm.equals("join") && desc.equals("(J)V")) {
	    Integer loc = getLocationId();
	    if (loc != null) {
	       super.visitLdcInsn(Integer.valueOf(0));
	       super.visitLdcInsn(loc);
	       super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_JOIN,MONITOR_JOIN_ARGS,false);
	       return;
	     }
	  }
	 else if (mnm.equals("join") && desc.equals("(JI)V")) {
	    Integer loc = getLocationId();
	    if (loc != null) {
	       super.visitLdcInsn(loc);
	       super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_JOIN,MONITOR_JOIN_ARGS,false);
	       return;
	     }
	  }
       }
      super.visitMethodInsn(opc,cls,mnm,desc,itf);
    }

}	// end of inner class WaitNotifyFixer



/********************************************************************************/
/*										*/
/*	Handle calls to java.util.concurrent					*/
/*										*/
/********************************************************************************/

private class ConcurrentFixer extends MethodVisitor {

   ConcurrentFixer(MethodVisitor mvx) {
      super(ASM_API,mvx);
    }

   @Override public void visitMethodInsn(int opc,String cls,String mnm,String desc,boolean itf) {
      if (cls.startsWith("java/util/concurrent/locks")) {
	 if ((mnm.equals("lock") || mnm.equals("lockInterruptibly")) && desc.equals("()V")) {
	    Integer loc = getLocationId();
	    if (loc != null) {
	       super.visitInsn(Opcodes.DUP);
	       super.visitInsn(Opcodes.DUP);
	       super.visitLdcInsn(loc);
	       super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_PRELOCK,MONITOR_PRELOCK_ARGS,false);
	       super.visitMethodInsn(opc,cls,mnm,desc,itf);
	       super.visitLdcInsn(Integer.valueOf(1));
	       super.visitLdcInsn(loc);
	       super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_LOCK,MONITOR_LOCK_ARGS,false);
	       return;
	     }
	  }
	 else if (mnm.equals("tryLock") && desc.equals("()Z")) {
	    Integer loc = getLocationId();
	    if (loc != null) {
	       super.visitLdcInsn(loc);
	       super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_TRY,MONITOR_TRY_ARGS,false);
	       return;
	     }
	  }
	 else if (mnm.equals("unlock") && desc.equals("()V")) {
	    Integer loc = getLocationId();
	    if (loc != null) {
	       super.visitInsn(Opcodes.DUP);
	       loc = Integer.valueOf(-loc);
	       super.visitLdcInsn(loc);
	       super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_UNLOCK,MONITOR_UNLOCK_ARGS,false);
	     }
	    super.visitMethodInsn(opc,cls,mnm,desc,itf);
	    return;
	  }
	 else if (mnm.equals("tryLock") && desc.equals("(JLjava/util/concurrent/TimeUnit;)Z")) {
	    Integer loc = getLocationId();
	    if (loc != null) {
	       super.visitLdcInsn(loc);
	       super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_TRY_TIMED,MONITOR_TRY_TIMED_ARGS,false);
	       return;
	     }
	  }
	 else if (mnm.equals("newCondition") || mnm.equals("readLock") || mnm.equals("writeLock")) {
	    super.visitInsn(Opcodes.DUP);
	    super.visitMethodInsn(opc,cls,mnm,desc,itf);
	    super.visitInsn(Opcodes.DUP_X1);
	    super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_ASSOC,MONITOR_ASSOC_ARGS,false);
	    return;
	  }
	 else if ((mnm.equals("await") || mnm.equals("awaitUninterrupibly")) && desc.equals("()V")) {
	    Integer loc = getLocationId();
	    if (loc != null) {
	       loc = Integer.valueOf(-loc);
	       super.visitInsn(Opcodes.DUP);
	       super.visitInsn(Opcodes.DUP);
	       super.visitLdcInsn(loc);
	       super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_WAIT,MONITOR_WAIT_ARGS,false);
	       super.visitMethodInsn(opc,cls,mnm,desc,itf);
	       super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_WAITED,MONITOR_WAITED_ARGS,false);
	       return;
	     }
	  }
	 else if (mnm.equals("await") && desc.equals("(JLjava/util/concurrent/TimeUnit;)Z")) {
	    Integer loc = getLocationId();
	    if (loc != null) {
	       loc = Integer.valueOf(-loc);
	       super.visitLdcInsn(loc);
	       super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_AWAIT_TIMED,MONITOR_AWAIT_TIMED_ARGS,false);
	       return;
	     }
	  }
	 else if (mnm.equals("awaitNanos") && desc.equals("(J)J")) {
	    Integer loc = getLocationId();
	    if (loc != null) {
	       loc = Integer.valueOf(-loc);
	       super.visitLdcInsn(loc);
	       super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_AWAIT_NANOS,MONITOR_AWAIT_NANOS_ARGS,false);
	       return;
	     }
	  }
	 else if ((mnm.equals("signal") || mnm.equals("signalAll"))) {
	    Integer loc = getLocationId();
	    if (loc != null) {
	       loc = Integer.valueOf(-loc);
	       super.visitInsn(Opcodes.DUP);
	       super.visitLdcInsn(loc);
	       super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_NOTIFY,MONITOR_NOTIFY_ARGS,false);
	       super.visitMethodInsn(opc,cls,mnm,desc,itf);
	       return;
	     }
	  }
       }
      else if (cls.equals("java/util/concurrent/CyclicBarrier")) {
	 if (mnm.equals("await") && desc.equals("()V")) {
	    Integer loc = getLocationId();
	    if (loc != null) {
	       loc = Integer.valueOf(-loc);
	       super.visitInsn(Opcodes.DUP);
	       super.visitInsn(Opcodes.DUP);
	       super.visitLdcInsn(loc);
	       super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_WAIT,MONITOR_WAIT_ARGS,false);
	       super.visitMethodInsn(opc,cls,mnm,desc,itf);
	       super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_WAITED,MONITOR_WAITED_ARGS,false);
	       return;
	     }
	  }
	 else if (mnm.equals("await") && desc.equals("(JLjava/util/concurrent/TimeUnit;)Z")) {
	    Integer loc = getLocationId();
	    if (loc != null) {
	       loc = Integer.valueOf(-loc);
	       super.visitLdcInsn(loc);
	       super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_AWAIT_TIMED_BARRIER,
		     MONITOR_AWAIT_TIMED_BARRIER_ARGS,false);
	       return;
	     }
	  }
       }
      else if (cls.equals("java/util/concurrent/CountDownLatch")) {
	 if (mnm.equals("await") && desc.equals("()V")) {
	    Integer loc = getLocationId();
	    if (loc != null) {
	       loc = Integer.valueOf(-loc);
	       super.visitInsn(Opcodes.DUP);
	       super.visitInsn(Opcodes.DUP);
	       super.visitLdcInsn(loc);
	       super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_WAIT,MONITOR_WAIT_ARGS,false);
	       super.visitMethodInsn(opc,cls,mnm,desc,itf);
	       super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_WAITED,MONITOR_WAITED_ARGS,false);
	       return;
	     }
	  }
	 else if (mnm.equals("await") && desc.equals("(JLjava/util/concurrent/TimeUnit;)Z")) {
	    Integer loc = getLocationId();
	    if (loc != null) {
	       loc = Integer.valueOf(-loc);
	       super.visitLdcInsn(loc);
	       super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_AWAIT_TIMED_LATCH,
		     MONITOR_AWAIT_TIMED_LATCH_ARGS,false);
	       return;
	     }
	  }
	 else if (mnm.equals("countDown")) {
	    Integer loc = getLocationId();
	    if (loc != null) {
	       loc = Integer.valueOf(-loc);
	       super.visitInsn(Opcodes.DUP);
	       super.visitLdcInsn(loc);
	       super.visitMethodInsn(Opcodes.INVOKESTATIC,MONITOR_CLASS,MONITOR_NOTIFY,MONITOR_NOTIFY_ARGS,false);
	       super.visitMethodInsn(opc,cls,mnm,desc,itf);
	       return;
	     }
	  }
       }
      super.visitMethodInsn(opc,cls,mnm,desc,itf);
    }

}	// end of inner class ConcurrentFixer





/********************************************************************************/
/*										*/
/*	Location information							*/
/*										*/
/********************************************************************************/

private Integer getLocationId()
{
   if (location_set != null) {
      int minsz = Math.max(size_eval.getMinSize()-1,0);
      int maxsz = size_eval.getMaxSize();
      for (Location loc : location_set) {
	 if (loc.getMethodOffset() >= minsz - OFFSET_DELTA &&
	       loc.getMethodOffset() <= maxsz + OFFSET_DELTA) {
	    return loc.getLocationId();
	  }
       }
      return null;
    }

   int ctr = location_ctr.incrementAndGet();

   if (info_writer != null) {
      String rslt = "LOC|" + ctr + "|" + class_name + "|" + cur_method +
	  "|" + cur_signature + "|" + size_eval.getMinSize() + "\n";

      synchronized (info_writer) {
	 try {
	    info_writer.write(rslt);
	    info_writer.flush();
	  }
	 catch (IOException e) { }
       }
    }

   return ctr;
}




/********************************************************************************/
/*										*/
/*	Debug outputer								*/
/*										*/
/********************************************************************************/

private static class MethodTracer extends MethodVisitor {

   private String method_name;
   private String method_desc;

   MethodTracer(MethodVisitor xmv,String nm,String desc) {
      super(ASM_API,new TraceMethodVisitor(xmv,new Textifier()));
      method_name = nm;
      method_desc = desc;
   }

   @Override public void visitEnd() {
      TraceMethodVisitor tmv = (TraceMethodVisitor) this.mv;
      Printer p = tmv.p;
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      super.visitEnd();
      p.print(pw);
      System.err.println("METHOD OUTPUT: " + method_name + " " + method_desc + ":\n" + sw.toString());
   }
}

}	// end of class DylateTransformer




/* end of DylateTransformer.java */
