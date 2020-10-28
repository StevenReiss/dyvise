/********************************************************************************/
/*										*/
/*		DylockLockManager.java						*/
/*										*/
/*	Manage locks for later processing					*/
/*										*/
/********************************************************************************/



package edu.brown.cs.dyvise.dylock;

import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.Element;

import java.util.*;
import java.io.File;


abstract class DylockLockManager implements DylockConstants, DylockConstants.DylockLockDataManager
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<Integer,DylockLockData>	lock_map;
private DylockViewRef			view_reference;
private Map<Integer,EntryLock>		entry_map;
private Map<Integer,DylockThreadData>	thread_map;
private double				max_time;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected DylockLockManager()
{
   lock_map = new HashMap<Integer,DylockLockData>();
   view_reference = new DylockViewRef();
   entry_map = new HashMap<Integer,EntryLock>();
   thread_map = new HashMap<Integer,DylockThreadData>();
   max_time = 0;
}



/********************************************************************************/
/*										*/
/*	Lock input methods							*/
/*										*/
/********************************************************************************/

protected void readLockData(File lockfile)
{
   Element eld = IvyXml.loadXmlFromFile(lockfile);
   if (eld == null) {
      System.err.println("DYLOCKPATTERN: Bad lock data file");
      System.exit(1);
    }
   max_time = IvyXml.getAttrDouble(eld,"TIME");

   for (Element te : IvyXml.children(eld,"THREAD")) {
      DylockThreadData td = new DylockThreadData(te);
      thread_map.put(td.getId(),td);
    }
   for (Element ed : IvyXml.children(eld,"LOCK")) {
      DylockLockData ld = new DylockLockData(view_reference,ed);
      for (DylockViewType dvt : ld.getViewTypes()) {
	 EntryLock el = new EntryLock(ld,dvt);
	 for (TraceLockLocation dll : dvt.getLocations()) {
	    int lid = dll.getId();
	    entry_map.put(lid,el);
	  }
       }
      lock_map.put(ld.getLockId(),ld);

      Element de = IvyXml.getChild(ed,"DATA");
      String eqv = IvyXml.getTextElement(de,"EQUIV");
      if (eqv != null) {
	 StringTokenizer tok = new StringTokenizer(eqv,",");
	 while (tok.hasMoreTokens()) {
	    String sid = tok.nextToken();
	    int iv = Integer.parseInt(sid);
	    lock_map.put(iv,ld);
	  }
       }
    }

   for (DylockLockData vd : lock_map.values()) {
      vd.fixup(this);
    }
   
   view_reference.finishLoad();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public double getMaxTime()	{ return max_time; }



/********************************************************************************/
/*										*/
/*	Location methods							*/
/*										*/
/********************************************************************************/

DylockLockLocation getLocation(Element xml)
{
   return view_reference.getLocation(xml);
}

@Override public DylockLockLocation findLocation(int id)
{
   return view_reference.findLocation(id);
}

@Override public DylockThreadData findThread(int id)
{
   return thread_map.get(id);
}




/********************************************************************************/
/*										*/
/*	Lock Access methods							*/
/*										*/
/********************************************************************************/

@Override public DylockLockData findLock(int id)
{
   return lock_map.get(id);
}


Collection<DylockLockData> getAllLocks()
{
   return lock_map.values();
}

EntryLock findEntryForLocation(int lid)
{
   return entry_map.get(lid);
}



/********************************************************************************/
/*										*/
/*	Entry lock information							*/
/*										*/
/********************************************************************************/

protected static class EntryLock {

   private DylockLockData for_lock;
   private DylockViewType for_type;

   EntryLock(DylockLockData ld,DylockViewType vt) {
      for_lock = ld;
      for_type = vt;
    }

   DylockLockData getLock()		{ return for_lock; }
   DylockViewType getViewType() 	{ return for_type; }

}	// end of inner class EntryLock


}	// end of class DylockLockManager




/* end of DylockLockManager.java */
