/********************************************************************************/
/*										*/
/*		DystoreIdSet.java						*/
/*										*/
/*	DYVISE map from strings to integers					*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dystore/DystoreIdSet.java,v 1.2 2009-10-07 01:00:20 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DystoreIdSet.java,v $
 * Revision 1.2  2009-10-07 01:00:20  spr
 * Code cleanup for eclipse.
 *
 * Revision 1.1  2009-09-19 00:14:00  spr
 * In memory tuple store for visualization.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dystore;


import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.util.HashMap;
import java.util.Map;



class DystoreIdSet implements DystoreConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<String,Integer> id_map;
private int next_id;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

DystoreIdSet()
{
   id_map = new HashMap<String,Integer>();
   next_id = 1;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

int getId(String s)
{
   if (s == null) return 0;

   Integer iv = id_map.get(s);
   if (iv != null) return iv;

   int rs = next_id++;
   id_map.put(s,rs);

   return rs;
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(IvyXmlWriter xw)
{
   xw.begin("IDMAP");
   xw.field("SIZE",next_id);
   for (Map.Entry<String,Integer> ent : id_map.entrySet()) {
      xw.begin("I");
      xw.field("N",ent.getValue().intValue());
      xw.text(ent.getKey());
      xw.end("I");
    }
   xw.end("IDMAP");
}



}	// end of class DystoreIdSet




/* end of DystoreIdSet.java */
