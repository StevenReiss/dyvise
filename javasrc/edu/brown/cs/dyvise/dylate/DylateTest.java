package edu.brown.cs.dyvise.dylate;

import java.util.*;


public class DylateTest {


   private static int count_value;
   private Map<Thread,int []> count_map;


   public static void main(String [] args) { }


   DylateTest() {
      count_value = 0;
      count_map = new HashMap<Thread,int []>();
    }

   int get() {
      if (count_value == 0 && count_map.size() > 0) {
	 for (int [] v : count_map.values()) {
	    count_value += v[0];
	  }
       }
      return count_value;
    }

   void increment()	{ count_value++; }

   synchronized void syncrement()    { count_value++; notify(); }

   void thincrement()	{ Thread.currentThread(); ++count_value; }

   void thincrement0() {
      Thread t = Thread.currentThread();
      int [] v = count_map.get(t);
      if (v == null) {
	 synchronized(count_map) {
	    v = new int[1];
	    count_map.put(t,v);
	    try {
	       count_map.wait();
	     }
	    catch (InterruptedException e) { }
	  }
       }
      v[0]++;
    }

}	// end of class DylateTest




/* end of DylateTest.java */
