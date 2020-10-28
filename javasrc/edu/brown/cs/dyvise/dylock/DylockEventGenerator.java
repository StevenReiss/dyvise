/********************************************************************************/
/*										*/
/*		DylockEventGenerator.java					*/
/*										*/
/*	Generate dystore events based on trace					*/
/*										*/
/********************************************************************************/



package edu.brown.cs.dyvise.dylock;

import edu.brown.cs.dyvise.dystore.*;

import java.util.*;


class DylockEventGenerator implements DylockConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private DystoreControl	data_store;
private Map<DylockLockData,UserLock> user_locks;





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DylockEventGenerator(DystoreControl st)
{
   data_store = st;
   user_locks = new HashMap<DylockLockData,UserLock>();
}


/********************************************************************************/
/*										*/
/*	Editing methods 							*/
/*										*/
/********************************************************************************/

void clearAll()
{
   data_store.clear(false);
}


void add(PatternEvent en)
{
   DylockLockData dld = en.getLock();

   UserLock lk = user_locks.get(dld);
   if (lk == null) {
      lk = new UserLock(dld);
      user_locks.put(dld,lk);
    }

   generateThreadView(en);
   generateNotifyView(en);
   generateLockView(en);
}



/********************************************************************************/
/*										*/
/*	Handle thread view							*/
/*										*/
/********************************************************************************/

private void generateThreadView(PatternEvent en)
{
   DylockLockData dld = en.getLock();
   UserLock lk = user_locks.get(dld);
   DylockThreadData th = en.getThread();
   PatternEvent prior = lk.getEntry(th);

   // System.err.println("HANDLE EVENT " + en + " " + prior);

   switch (en.getType()) {
      case ENTER :
	 lk.pushEntry(th,en);
	 break;
      case ENTERED :
	 if (en.getLevel() <= 1) addThreadEntry(prior,en,"BLOCK");
	 if (prior == null) lk.pushEntry(th,en);
	 else lk.setEntry(th,en);
	 break;
      case READLOCK :
	 addThreadEntry(prior,en,"READBLOCK");
	 if (prior == null) lk.pushEntry(th,en);
	 else lk.setEntry(th,en);
	 break;
      case WRITELOCK :
	 addThreadEntry(prior,en,"WRITEBLOCK");
	 if (prior == null) lk.pushEntry(th,en);
	 else lk.setEntry(th,en);
	 break;
      case EXIT :
	 addThreadEntry(prior,en,"LOCKED");
	 lk.popEntry(th);
	 break;
      case READUNLOCK :
	 addThreadEntry(prior,en,"READUNLOCKED");
	 lk.popEntry(th);
	 break;
      case WRITEUNLOCK :
	 addThreadEntry(prior,en,"WRITEUNLOCKED");
	 lk.popEntry(th);
	 break;
      case WAIT :
	 addThreadEntry(prior,en,"LOCKED");
	 lk.setEntry(th,en);
	 break;
      case WAITED :
	 addThreadEntry(prior,en,"WAITING");
	 lk.setEntry(th,en);
	 break;
      default :
	 break;
    }
}



private void addThreadEntry(PatternEvent prior,PatternEvent en,String state)
{
   if (prior == null) return;

   DylockLockData dld = en.getLock();
   DylockThreadData th = en.getThread();

   Map<String,Object> tup = new HashMap<String,Object>();
   tup.put("THREAD",th.getDisplayName());
   tup.put("LOCK",dld.getDisplayName());
   tup.put("STATE",state);
   tup.put("START",prior.getTime()/TIME_SHIFT);
   tup.put("END",en.getTime()/TIME_SHIFT);
   tup.put("LOCATION",en.getLocation().getDisplayName());

   DystoreStore dat = data_store.getStore("THREADLOCK");
   dat.addTuple(tup);
}



/********************************************************************************/
/*										*/
/*	Handle notification view						*/
/*										*/
/********************************************************************************/

private void generateNotifyView(PatternEvent en)
{
   DylockLockData dld = en.getLock();
   UserLock lk = user_locks.get(dld);
   DylockThreadData th = en.getThread();

   switch (en.getType()) {
      case NOTIFY :
	 for (PatternEvent prior : lk.getWaits()) {
	    lk.setNotify(prior.getThread(),en);
	  }
	 break;
      case WAITED :
	 PatternEvent nent = lk.getNotify(th);
	 if (nent != null) {
	    Map<String,Object> tup = new HashMap<String,Object>();
	    tup.put("WHEN",nent.getTime()/TIME_SHIFT);
	    // TODO: would like to make the two table field names independent in the display
	    tup.put("THREAD",nent.getThread().getDisplayName());
	    tup.put("TO",en.getThread().getDisplayName());
	    DystoreStore dat = data_store.getStore("THREADNOTIFY");
	    dat.addTuple(tup);
	  }
	 lk.clearNotify(th);
	 break;
      case WAIT :
	 lk.clearNotify(th);
	 break;
      default :
	 break;
   }
}




/********************************************************************************/
/*										*/
/*	Lock-specific view							*/
/*										*/
/********************************************************************************/

private void generateLockView(PatternEvent en)
{
   DylockLockData dld = en.getLock();
   DylockViewType vt = en.getView();

   // System.err.println("ADD EVENT: " + en);

   if (vt == null) return;

   vt.processRunEntry(en.getLockEntry(),dld.getDisplayName(),data_store.getStore("LOCKDATA"));
}




/********************************************************************************/
/*										*/
/*	Holder of lock-specific data						*/
/*										*/
/********************************************************************************/

private static class UserLock {

   private String	display_name;
   private Map<DylockThreadData,Stack<PatternEvent>> thread_entries;
   private Map<DylockThreadData,PatternEvent> notify_entries;

   UserLock(DylockLockData lk) {
      thread_entries = new HashMap<DylockThreadData,Stack<PatternEvent>>();
      notify_entries = new HashMap<DylockThreadData,PatternEvent>();
    }

   void pushEntry(DylockThreadData th,PatternEvent e) {
      Stack<PatternEvent> stk = thread_entries.get(th);
      if (stk == null) {
	 stk = new Stack<PatternEvent>();
	 thread_entries.put(th,stk);
       }
      stk.push(e);
   }

   void popEntry(DylockThreadData th) {
      Stack<PatternEvent> stk = thread_entries.get(th);
      if (stk != null && !stk.empty()) {
	 stk.pop();
       }
    }
   void setEntry(DylockThreadData th,PatternEvent e) {
      Stack<PatternEvent> stk = thread_entries.get(th);
      if (stk == null) return;
      if (stk != null && !stk.empty()) stk.pop();
      stk.push(e);
    }

   PatternEvent getEntry(DylockThreadData th) {
      Stack<PatternEvent> stk = thread_entries.get(th);
      if (stk == null || stk.empty()) return null;
      return stk.peek();
    }

   void setNotify(DylockThreadData th,PatternEvent e)	{ notify_entries.put(th,e); }
   void clearNotify(DylockThreadData th)		{ notify_entries.remove(th); }
   PatternEvent getNotify(DylockThreadData th)		{ return notify_entries.get(th); }

   List<PatternEvent> getWaits() {
      List<PatternEvent> rslt = new ArrayList<PatternEvent>();
      for (Stack<PatternEvent> se : thread_entries.values()) {
	 if (!se.empty()) {
	    PatternEvent e = se.peek();
	    if (e.getType() == PatternEventType.WAIT) {
	       rslt.add(e);
	     }
	  }
       }
      return rslt;
    }

   @Override public String toString()		{ return display_name; }

}	// end of inner class UserLock





}	// end of class DylockEventGenerator




/* end of DylockEventGenerator.java */
