/********************************************************************************/
/*										*/
/*		DyVisualModel.java						*/
/*										*/
/*	DYVISE abstract visualization model definition				*/
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

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/javasrc/edu/brown/cs/dyvise/dyvise/DyviseVisualModel.java,v 1.1 2009-09-19 00:14:44 spr Exp $ */


/*********************************************************************************
 *
 * $Log: DyviseVisualModel.java,v $
 * Revision 1.1  2009-09-19 00:14:44  spr
 * Common files for use throughout the system.
 *
 *
 ********************************************************************************/



package edu.brown.cs.dyvise.dyvise;


import java.util.*;



public interface DyviseVisualModel extends DyviseConstants {


/********************************************************************************/
/*										*/
/*	Event methods and interfaces						*/
/*										*/
/********************************************************************************/

Collection<Event> getEventSet();
Event findEvent(String id);
Event findEvent(int num);
int getNumEventParameters();

Collection<CountEvent> getCountEventSet();
Set<MatchWhat> getMatchSet(EventType ety);



enum EventType {
   ALLOC, CALL, RETURN, ENTER, EXIT, FIELDSET, FREE
}


enum EventDescriptor {
   NAME, ID, NUMBER, TRIGGER, LIBRARY,
   FROMCLASS, CLASS, VIRTUAL, PARENT, METHOD, SIGNATURE, FIELD,
   FROMTHREAD, VALUE, FROMMETHOD, FROMSIGNATURE
}


interface Event {
   String getId();
   String getName();
   int getNumber();
   boolean isTrigger();
   boolean getInLibrary();
   EventType getEventType();
   List<Match> getMatchSet();			// ordered to match parameters
   List<EventAction> getActionSet();
   Object getDescriptor(EventDescriptor p);
   Object getDescriptor(String pname);
   String getDescription();
   Match getMatch(MatchWhat w);
}



/**
 * The order of these is the order in which they are passed to the event
 * handler.
 **/

enum MatchWhat {
   THIS, OBJECT, VALUE, FROMTHIS, FROMTHREAD,
   ARG1, ARG2, ARG3, ARG4, ARG5, ARG6,
   SOURCE
}


enum MatchType {
   NEW, SET, MATCH
}


interface Match {
   MatchWhat getWhat();
   String getParameter();
   MatchType getMatchType();
   boolean getUseForName();
   String getMapFunction();
}


enum EventActionType {
   CREATE, CREATENAME, FIND, SET, SETNAME, START,
   NEWCOUNT, SETCOUNT, CLEARCOUNT, REMOVE
}


interface EventAction {
   EventActionType getActionType();
   String getParameter();
}



enum CountType {
   ALLOCS, INSTRUCTIONS, ENTERS
};


interface CountEvent {
   CountType getCountType();
   boolean countIn(String cls,String mthd);
};




/********************************************************************************/
/*										*/
/*	Automata methods and interfaces 					*/
/*										*/
/********************************************************************************/

Automata getAutomata();


interface Automata {
   String getName();
   State getStartState();
   Transition getTransition(State cur,Event evt);
   Collection<State> getStateSet();
   boolean removeOnFree();
}


interface State {
   String getName();
   String getId();
   String getValue();
   int getNumber();
}



interface Transition {
   State getNextState();
   Collection<Action> getActions();
}



/********************************************************************************/
/*										*/
/*	Entity description							*/
/*										*/
/*	Note that entities are presumed to have a lifetime (e.g. to start at	*/
/*	some time and end at some time, where times are in terms of the 	*/
/*	execution).  Particular fields of the entity, may or may not vary	*/
/*	with time.								*/
/*										*/
/********************************************************************************/

enum EntityType {
   NODE, RELATION, ARCSET, NODESET
}


interface Entity {

   String getName();

   Collection<Field> getFields();

   Field findField(String nm);

   EntityType getEntityType();

}




/********************************************************************************/
/*										*/
/*	Field description							*/
/*										*/
/********************************************************************************/

interface Field {

   String getName();

   DyviseDataType getDataType();

   boolean isTimeVarying();

   boolean getShow(FieldShowBase what);

   int getStatisticIndex();		// returns -1 if not relevant
   int getStatisticCount();
}




enum FieldShowBase {

   LABEL,				// show the name as a label or tag
   VALUE,				// show the value of the field
   TIME,				// show time spent at value
   COUNT,				// show number of sets of the value
   TRANSITIONS, 			// show transitions between values
   INSTRUCTIONS,			// show instruction counts at value
   ALLOCS,				// show allocation counts at value
   CALLS				// show call counts at value
}





/********************************************************************************/
/*										*/
/*	Relation description							*/
/*										*/
/********************************************************************************/

interface Relation extends Entity {

   Entity getSource();

   Entity getTarget();

   boolean isTimeVarying();		// if false, time based on entity times

   // need to know the ARITYs

}



/********************************************************************************/
/*										*/
/*	Action description							*/
/*										*/
/********************************************************************************/

Action findAction(String id);

enum ActionType {
   CREATE, SETFIELD, REMOVE,
   GRAPH_NEWNODE, GRAPH_NEWARC, GRAPH_REMOVENODE, GRAPH_REMOVEARC
}


enum ActionValueType {
   NONE, AUTOMATA, NAME, CONST, PARAMETER
}


interface Action {
   String getActionId();

   ActionType getActionType();
   Entity getDataEntity();
   Field getDataField();

   ActionValueType getIdType();
   String getIdTag();
   ActionValueType getValueType();
   String getValueTag();
   ActionValueType getOtherType();
   String getOtherTag();

   boolean isValid();

   boolean isFieldNeeded();
   String getIdLabel();
   String getValueLabel();
   String getOtherLabel();
}




/********************************************************************************/
/*										*/
/*	View methods and classes						*/
/*										*/
/********************************************************************************/

/****************
Collection<VeldViewModel> getViews();
VeldViewModel findView(String name);

Collection<String> getVisualizationTypes();

VeldVizModel getVisualization(String typ);

*****************/



}	// end of interface DyVisualModel




/* end of DyvisionModel.java */
