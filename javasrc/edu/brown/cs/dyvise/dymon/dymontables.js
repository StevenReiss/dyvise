/********************************************************************************/
/*										*/
/*		dymontables.js							*/
/*										*/
/*	Code to create tables from dynamic data using flapjax			*/
/*										*/
/********************************************************************************/
/*	Copyright 2007 Brown University -- Steven P. Reiss		      */
/*	Copyright 2007 Brown University -- Jason Baskin 		      */
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


/********************************************************************************/
/*										*/
/*	Process tables								*/
/*										*/
/********************************************************************************/

function loadProcTable()
{
   var flapjax = flapjaxInit();
   initFlapjaxWebServiceAPIs(flapjax);

   var addrRequestE = timer_e(2000).constant_e(
      { url: 'dymonproctable.php',
	fields: {},
	serviceType: 'xml',
	returnType: 'xml',
	request: 'get' });

   var addrReturnE = getWebServiceObject_e(addrRequestE);

   var tableRowsE = addrReturnE.filter_e(function(xdoc) {
	if (xdoc == null) return false;
	var alist = xdoc.firstChild;
	if (alist.nodeName == 'PROCESSES') return true;
	return false; }).transform_e(function(xdoc) {
	var alist = xdoc.firstChild;
	return map(function(address) {
	  return [
	     getTextElement(address,'ID'),
	     getTextElement(address,'START'),
	     getTextElement(address,'SHOWARGS'),
	     getTextElement(address,'MONITOR'),
	     getTextElement(address,'ATTACHED') ]; },
	   alist.getElementsByTagName('PROCESS'));
      });

   var tableRowsB = tableRowsE.hold([]);

   var procsB = makeProcTable(['ID','Main Class','Arguments','Monitored','Attached'],
			     ['string','string','string','string','string'],tableRowsB,
				    {border: '1', bordercolor: '#0000ff', id: 'processtable' },
			      makeProcLink);
   insertDomB(procsB,'plist');
}



function makeProcLink(cell,row,idx)
{
   var pid = row[0];
   var rslt = "";

   if (idx == 0 || idx == 1) {
      rslt = A({href: 'dymonsummary.php?PID=' + pid, target: '_blank' },cell);
    }
   else if (idx == 2) {
      rslt = A({href: 'dymonprocess.php?PID=' + pid, target: '_blank' },cell);
    }
   else if (idx == 3) {
      rslt = A({href: 'javascript:sendDymonCommand("ENABLE","' + row[0] + '","*")'},cell);
    }
   else if (idx == 4) {
      rslt = A({href: 'javascript:sendDymonCommand("ATTACH","' + row[0] + '","*")'},cell);
    }

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Analysis tables 							*/
/*										*/
/********************************************************************************/

function loadAnalysisTables(pid)
{
   var xdocE = setupAnalysis(pid);
   if (xdocE == null) return;

   loadCpuTable(xdocE,null);
   loadThreadTable(xdocE,null);
   loadLockTable(xdocE);
   loadMemoryTable(xdocE);
   loadGCTable(xdocE);
   loadAllocTable(xdocE,null,null);
   loadHeapTable(xdocE,null);
   loadIoTable(xdocE,null);
   loadIoSourceTable(xdocE,null,null);
   loadIoThreadTable(xdocE,null,null);
   loadTimingTable(xdocE);
}



function loadCpuTables(pid)
{
   var xdocE = setupAnalysis(pid);
   if (xdocE == null) return;

   loadCpuTable(xdocE,null);
   loadThreadTable(xdocE,null);
   loadTimingTable(xdocE);
   loadIoTable(xdocE,null);
}




function loadAllocTables(pid)
{
   var xdocE = setupAnalysis(pid);
   if (xdocE == null) return;

   loadMemoryTable(xdocE);
   loadGCTable(xdocE);
   loadAllocTable(xdocE,null,null);
   loadHeapTable(xdocE,null);
}




function loadLockTables(pid)
{
   var xdocE = setupAnalysis(pid);
   if (xdocE == null) return;

   loadThreadTable(xdocE,null);
   loadLockTable(xdocE);
   loadTimingTable(xdocE);
}



function loadIOTables(pid)
{
   var xdocE = setupAnalysis(pid);
   if (xdocE == null) return;

   loadThreadTable(xdocE,null);
   loadIoTable(xdocE,null);
   loadIoSourceTable(xdocE,null,null);
   loadIoThreadTable(xdocE,null,null);
   loadTimingTable(xdocE);
}



function setupAnalysis(pid)
{
   var flapjax = flapjaxInit();
   initFlapjaxWebServiceAPIs(flapjax);

   var addrRequestE = timer_e(2000).constant_e(
      { url: 'dymonanalysis.php',
	fields: { 'PID': pid },
	serviceType: 'xml',
	returnType: 'xml',
	request: 'get' });

   var addrReturnE = getWebServiceObject_e(addrRequestE);

   var dataReturnE = addrReturnE.filter_e(
      function(xdoc) {
	 if (xdoc == null) return false;
	 var alist = xdoc.firstChild;
	 if (alist.nodeName == 'ANALYSIS') return true;
	 return false;
       }
      );

   if (dataReturnE == null) return null;
   return dataReturnE.transform_e(function(xdoc) { return xdoc.firstChild; });
}



/********************************************************************************/
/*										*/
/*	Analysis tables -- cpu time						*/
/*										*/
/********************************************************************************/

function loadCpuTable(data,f)
{
   var d1 = data.transform_e(function (xdoc) { return getChild(xdoc,'CPUTIME'); });

   var cpuTableRowsE = d1.transform_e(function(xdoc) {
	return map(function(address) {
	  return [
		address.getAttribute('NAME'),
		address.getAttribute('BASETIME'),
		address.getAttribute('BASEPCT'),
		address.getAttribute('BASEERROR'),
		address.getAttribute('TOTALTIME'),
		address.getAttribute('TOTALPCT'),
		address.getAttribute('TIMEERROR'),
		address.getAttribute('COUNT'),
		address.getAttribute('TIME'),
		address.getAttribute('TIMEPER')]; },
	   maybeCheckFilter(xdoc,'ITEM',f));
      });

   var cpuTableRowsB = cpuTableRowsE.hold([]);

   var cpudataB = makeGenericTable(['Name','Base Time','%', '+/-','Total Time', '%', '+/-','# exec','Exec time','Time/Exec (us)'],
			     ['string','num','num','num','num','num','num','num','num','num'],cpuTableRowsB,
				      {border: '1', bordercolor: '#0000ff',
				       id: 'cputable' });

   insertDomB(cpudataB,'cputbl');
}


function maybeFilter(set,f)
{
   if (f == null) return set;

   return filter(f,set);
}


function maybeCheckFilter(xdoc,itm,f)
{
   if (xdoc == null) return [];
   set = xdoc.getElementsByTagName(itm);
   if (f == null) return set;
   return filter(f,set);
}



/********************************************************************************/
/*										*/
/*	Analysis tables -- threads						*/
/*										*/
/********************************************************************************/

function loadThreadTable(data,f)
{
   var d1 = data.transform_e(function (xdoc) { return getChild(xdoc,'THREADS'); });

   var thrTableRowsE = d1.transform_e(function(xdoc) {
	return map(function(address) {
	  return [
		address.getAttribute('NAME'),
		address.getAttribute('RUNPCT'),
		address.getAttribute('WAITPCT'),
		address.getAttribute('IOPCT'),
		address.getAttribute('BLOCKPCT'),
		address.getAttribute('SLEEPPCT'),
		address.getAttribute('RUNTIME'),
		address.getAttribute('NUMWAIT'),
		address.getAttribute('NUMBLOCK'),
		address.getAttribute('WAITTIME'),
		address.getAttribute('BLOCKTIME'),
		address.getAttribute('TERMINATE')
	     ]; },
	   maybeCheckFilter(xdoc,'THREAD',f));
      });

   var thrTableRowsB = thrTableRowsE.hold([]);

   var thrdataB = makeGenericTable(['Name','% Run','% Wait','% I/O','% Block','% Sleep','Run time',
				     '# Wait', '# Block', 'Wait Time', 'Block Time','Exited'],
				     ['string','num','num','num','num','num','num','num',
				     'num','num','num','string'],thrTableRowsB,
				     {border: '1', bordercolor: '#0000ff',
				      id: 'threadtable' });

   insertDomB(thrdataB,'threadtbl');
}



/********************************************************************************/
/*										*/
/*	Analysis tables -- locks						*/
/*										*/
/********************************************************************************/

function loadLockTable(data)
{
   var d1 = data.transform_e(
      function (xdoc) {
	 return getChild(getChild(xdoc,'LOCKS'),'LOCKMAT');
       } );

   var tblE = d1.transform_e(
      function (xml) {
	 return lockTable(xml);
       } );

   var tblB = tblE.hold(TABLE());

   insertDomB(tblB,'locktbl');
}



function lockTable(lockmat)
{
   if (lockmat == null) return TABLE();
   var forthreads = lockmat.getElementsByTagName('FORTHREAD');
   var tableHead = TR(TH('Thread'),map(function(e) {return TH(e.getAttribute('ID'));},
			  lockmat.getElementsByTagName('FORTHREAD')));
   var tableRows = map(function(e) {
			  return TR(
			     TH('#'+e.getAttribute('ID')+': '+e.getAttribute('NAME')),
				map(function(bo) {
				       return TD(getCountValue(bo));
				     },
				    e.getElementsByTagName('BLOCKON')));},forthreads);

   return TABLE({border: '1', bordercolor: '#0000ff', id: 'locktbl'},
		tableHead,tableRows);
}



function getCountValue(xdoc)
{
   var v1 = xdoc.getAttribute('COUNT');
   if (v1 == 0) return '';
   return v1;
}




/********************************************************************************/
/*										*/
/*	Analysis tables -- memory						*/
/*										*/
/********************************************************************************/

function loadMemoryTable(data)
{
   var d1 = data.transform_e(
      function (xdoc) {
	 return getChild(getChild(xdoc,'MEMORY'),'REGIONS');
       } );


   var memrowsE = d1.transform_e(function(xdoc) {
	return map(function(address) {
	  return [
		address.getAttribute('NAME'),
		address.getAttribute('INIT'),
		address.getAttribute('MAX'),
		address.getAttribute('USEDAVG'),
		address.getAttribute('COMMAVG')
	     ]; },
	   maybeCheckFilter(xdoc,'USAGE',null));
      });

   var memrowsB = memrowsE.hold([]);

   var memdataB = makeGenericTable(['Name','Initial Size','Max Size','Size Used','Size Committed'],
				      ['string','num','num','num','num'],memrowsB,
				      { border: '1',bordercolor: '#0000ff', id: 'cputable' });

   insertDomB(memdataB,'memtbl');
}


function loadGCTable(data)
{
   var d1 = data.transform_e(
      function (xdoc) {
	 return getChild(getChild(xdoc,'MEMORY'),'GCS');
       } );


   var gcrowsE = d1.transform_e(function(xdoc) {
	return map(function(address) {
	  return [
		address.getAttribute('NAME'),
		address.getAttribute('COUNT'),
		address.getAttribute('TIME')
	     ]; },
	   maybeCheckFilter(xdoc,'GC',null));
      });

   var gcrowsB = gcrowsE.hold([]);

   var gcdataB = makeGenericTable(['Name','Count','Time (ms)'],
				      ['string','num','num'],gcrowsB,
				      { border: '1',bordercolor: '#0000ff', id: 'cputable' });

   insertDomB(gcdataB,'gctbl');
}


function loadAllocTable(data,f1,f2)
{
   var d1 = data.transform_e(
      function (xdoc) {
	 return getChild(getChild(xdoc,'MEMORY'),'ALLOCATIONS');
       } );

   var d2 = d1.transform_e(
      function (xdoc) {
	 return allocTable(xdoc,f1,f2);
       } );

   var alloctblB = d2.hold(TABLE());

   insertDomB(alloctblB,'alloctbl');
}



function allocTable(xdoc,f1,f2)
{
   var tr1 = map(function(adoc) { return allocRows(adoc,f2); },
		 maybeCheckFilter(xdoc,'ALLOC',f1));

   var tr2 = foldR(function(v,a) { return a.concat(v); },[],tr1);

   var trh = TR(TH('Class'),TH('# Allocated'),TH('From Class'),TH('Method'),TH('Line'),TH('Percent'));

   var trtbl = TABLE({ border: '1',bordercolor: '#0000ff', id: 'alloctable' },
			trh, tr2);

   return trtbl;
}


function allocRows(xdoc,f2)
{
   var sz = xdoc.getElementsByTagName('SOURCE').length;
   var cls = xdoc.getAttribute('NAME');
   var ct = xdoc.getAttribute('COUNT');

   if (sz == 0) {
      return [ makeAllocRow([ cls, ct, '', '', '', 100.0 ]) ];
    }
   else if (sz == 1) {
      var src = getChild(xdoc,'SOURCE');
      return [ makeAllocRow([ cls, ct,
			       src.getAttribute('CLASS'),
			       src.getAttribute('METHOD'),
			       src.getAttribute('LINE'),
			       src.getAttribute('PCT') ]) ];
    }
   else {
      var c1 = TD({rowSpan: sz},cls);
      var c2 = TD({rowSpan: sz},ct);
      var sset = map(function(src) {
			return [ TD(src.getAttribute('CLASS')),
			   TD(src.getAttribute('METHOD')),
			   TD(src.getAttribute('LINE')),
			   TD(src.getAttribute('PCT')) ]; },
		     maybeCheckFilter(xdoc,'SOURCE',f2));
      var rset = map(function(arr) {
			if (c1 != null) {
			   arr = [ c1, c2 ].concat(arr);
			   c1 = null;
			   c2 = null;
			 }
			return TR(arr); },sset);
      return rset;
    }
}



function makeAllocRow(elts)
{
   return TR(map(
		function(cell) { return TD(cell); },
		elts));
}


/********************************************************************************/
/*										*/
/*	Analysis tables -- I/O							*/
/*										*/
/********************************************************************************/

function loadIoTable(data,f)
{
   var d1 = data.transform_e(function (xdoc) { return getChild(xdoc,'IO'); });

   var ioTableRowsE = d1.transform_e(function(xdoc) {
	return map(function(address) {
	  return [
		address.getAttribute('CLASS'),
		address.getAttribute('METHOD'),
		address.getAttribute('PCT'),
		address.getAttribute('TIME'),
		address.getAttribute('CALLS')
	     ]; },
	   maybeCheckFilter(xdoc,'IOMETHOD',f));
      });

   var ioTableRowsB = ioTableRowsE.hold([]);

   var iodataB = makeGenericTable(['Class','Method','% Time','Time','Calls'],
			     ['string','string','num','num','num'],ioTableRowsB,
				      { border: '1', bordercolor: '#0000ff',
				       id: 'iotable' });

   insertDomB(iodataB,'iotbl');
}





function loadIoSourceTable(data,f1,f2)
{
   var d1 = data.transform_e(function (xdoc) { return getChild(xdoc,'IO'); });

   var d2 = d1.transform_e(
      function (xdoc) {
	 return ioSourceTable(xdoc,f1,f2);
       } );

   var iosourcetblB = d2.hold(TABLE());

   insertDomB(iosourcetblB,'iosrctbl');
}



function ioSourceTable(xdoc,f1,f2)
{
   var tr1 = map(function(adoc) { return iosrcRows(adoc,f2); },
		 maybeCheckFilter(xdoc,'IOMETHOD',f1));

   var tr2 = foldR(function(v,a) { return a.concat(v); },[],tr1);

   var trh = TR(TH('Class'),TH('Method'),TH('% Time'),TH('Time'),
		   TH('From Class'),TH('Method'),TH('Line'),TH('Percent'));

   var trtbl = TABLE({ border: '1',bordercolor: '#0000ff', id: 'alloctable' },
			trh, tr2);

   return trtbl;
}


function iosrcRows(xdoc,f2)
{
   var sz = xdoc.getElementsByTagName('SOURCE').length;
   var cls = xdoc.getAttribute('CLASS');
   var mthd = xdoc.getAttribute('METHOD');
   var pct = xdoc.getAttribute('PCT');
   var tim = xdoc.getAttribute('TIME');

   if (sz == 0) {
      return [ makeIosrcRow([ cls, mthd, pct, tim, '', '', '', 100.0 ]) ];
    }
   else if (sz == 1) {
      var src = getChild(xdoc,'SOURCE');
      return [ makeIosrcRow([ cls, mthd, pct, tim,
			       src.getAttribute('CLASS'),
			       src.getAttribute('METHOD'),
			       src.getAttribute('LINE'),
			       src.getAttribute('PCT') ]) ];
    }
   else {
      var c1 = TD({rowSpan: sz},cls);
      var c2 = TD({rowSpan: sz},mthd);
      var c3 = TD({rowSpan: sz},pct);
      var c4 = TD({rowSpan: sz},tim);
      var sset = map(function(src) {
			return [ TD(src.getAttribute('CLASS')),
			   TD(src.getAttribute('METHOD')),
			   TD(src.getAttribute('LINE')),
			   TD(src.getAttribute('PCT')) ]; },
		     maybeCheckFilter(xdoc,'SOURCE',f2));
      var rset = map(function(arr) {
			if (c1 != null) {
			   arr = [ c1, c2, c3, c4 ].concat(arr);
			   c1 = null;
			   c2 = null;
			   c3 = null;
			   c4 = null;
			 }
			return TR(arr); },sset);
      return rset;
    }
}



function makeIosrcRow(elts)
{
   return TR(map(
		function(cell) { return TD(cell); },
		elts));
}



function loadIoThreadTable(data,f1,f2)
{
   var d1 = data.transform_e(function (xdoc) { return getChild(xdoc,'IO'); });

   var d2 = d1.transform_e(
      function (xdoc) {
	 return ioThreadTable(xdoc,f1,f2);
       } );

   var iothreadtblB = d2.hold(TABLE());

   insertDomB(iothreadtblB,'iothrtbl');
}



function ioThreadTable(xdoc,f1,f2)
{
   var tr1 = map(function(adoc) { return iothrRows(adoc,f2); },
		 maybeCheckFilter(xdoc,'IOMETHOD',f1));

   var tr2 = foldR(function(v,a) { return a.concat(v); },[],tr1);

   var trh = TR(TH('Class'),TH('Method'),TH('% Time'),TH('Time'),
		   TH('From Thread'),TH('Percent'));

   var trtbl = TABLE({ border: '1',bordercolor: '#0000ff', id: 'alloctable' },
			trh, tr2);

   return trtbl;
}


function iothrRows(xdoc,f2)
{
   var sz = xdoc.getElementsByTagName('THREAD').length;
   var cls = xdoc.getAttribute('CLASS');
   var mthd = xdoc.getAttribute('METHOD');
   var pct = xdoc.getAttribute('PCT');
   var tim = xdoc.getAttribute('TIME');

   if (sz == 0) {
      return [ makeIothrRow([ cls, mthd, pct, tim, '', 100.0 ]) ];
    }
   else if (sz == 1) {
      var src = getChild(xdoc,'THREAD');
      return [ makeIothrRow([ cls, mthd, pct, tim,
			       src.getAttribute('NAME'),
			       src.getAttribute('PCT') ]) ];
    }
   else {
      var c1 = TD({rowSpan: sz},cls);
      var c2 = TD({rowSpan: sz},mthd);
      var c3 = TD({rowSpan: sz},pct);
      var c4 = TD({rowSpan: sz},tim);
      var sset = map(function(src) {
			return [ TD(src.getAttribute('NAME')),
			   TD(src.getAttribute('PCT')) ]; },
		     maybeCheckFilter(xdoc,'THREAD',f2));
      var rset = map(function(arr) {
			if (c1 != null) {
			   arr = [ c1, c2, c3, c4 ].concat(arr);
			   c1 = null;
			   c2 = null;
			   c3 = null;
			   c4 = null;
			 }
			return TR(arr); },sset);
      return rset;
    }
}



function makeIothrRow(elts)
{
   return TR(map(
		function(cell) { return TD(cell); },
		elts));
}



/********************************************************************************/
/*										*/
/*	Analysis tables -- Timing						*/
/*										*/
/********************************************************************************/

function loadTimingTable(data)
{
   var d1 = data.transform_e(function (xdoc) { return getChild(xdoc,'TIMING'); });

   var timTableRowsE = d1.transform_e(function(xdoc) {
	return map(function(address) {
	  var cpu = address.getAttribute('PCTCPU');
	  var usr = address.getAttribute('PCTUSER');
	  var sys = address.getAttribute('PCTSYS');
	  return [ address.getAttribute('NAME'), cpu, usr, sys ]; },
	   maybeCheckFilter(xdoc,'THREAD',null));
      });

   var timTableRowsB = timTableRowsE.hold([]);

   var timdataB = makeGenericTable(['Thread','% Cpu Used','% User Time','% System Time'],
			     ['string','num','num','num'],timTableRowsB,
				      { border: '1', bordercolor: '#0000ff',
				       id: 'timingtable' });

   insertDomB(timdataB,'timingtbl');
}




/********************************************************************************/
/*										*/
/*	Analysis tables -- heap 						*/
/*										*/
/********************************************************************************/

function loadHeapTable(data,f)
{
   var d1 = data.transform_e(function (xdoc) { return getChild(xdoc,'HEAP'); });

   var heapTableRowsE = d1.transform_e(function(xdoc) {
	return map(function(address) {
	  return [
		address.getAttribute('NAME'),
		address.getAttribute('COUNT'),
		address.getAttribute('SIZE'),
		address.getAttribute('PCT') ]; },
	   maybeCheckFilter(xdoc,'CLASS',f));
      });

   var heapTableRowsB = heapTableRowsE.hold([]);

   var heapdataB = makeGenericTable(['Class','# Objects','Size','% Heap'],
			     ['string','num','num','num'],heapTableRowsB,
				      { border: '1', bordercolor: '#0000ff',
				       id: 'heaptable' });

   insertDomB(heapdataB,'heaptbl');
}





/********************************************************************************/
/*										*/
/*	Summary tables -- main routine						*/
/*										*/
/********************************************************************************/

var totaltime = 0;
var totalobject = 0;

function loadSummaryTables(pid)
{
   var xdocE = setupAnalysis(pid);
   if (xdocE == null) return;

   loadHeader(xdocE);
   loadCpuTable(xdocE,cpuItemFilter);
   loadThreadTable(xdocE,threadFilter);
   loadAllocTable(xdocE,allocFilter,null);
   loadIoTable(xdocE,ioFilter);
   loadHeapTable(xdocE,heapFilter);
}




function loadHeader(data)
{
   var tblE = data.transform_e(
      function (xml) {
	 processControls(xml);
	 return headerTable(xml);
       } );

   var tblB = tblE.hold(TABLE());

   insertDomB(tblB,'headertbl');
}



function headerTable(xdoc)
{
   var start = xdoc.getAttribute('START');
   var running = xdoc.getAttribute('ACTIVE');
   var d1 = getChild(xdoc,'CPUTIME');
   if (d1 != null) {
      var tottime = d1.getAttribute('TOTTIME');
      var numsamp = d1.getAttribute('TOTSAMP');
      var pctused = d1.getAttribute('PCTUSED');
    }
   var d2 = getChild(xdoc,'TIMING');
   var numproc = '?';
   var numthrd = '?';
   if (d2 != null) {
      numproc = d2.getAttribute('PROCESSORS');
      numthrd = d2.getAttribute('NTHREAD');
    }
   var d3 = getChild(xdoc,'MEMORY');
   var memused = '?';
   var memcomm = '?';
   var numgcs = '?';
   var totalobject = '?';
   if (d3 != null) {
      memused = d3.getAttribute('USED');
      memcomm = d3.getAttribute('COMMITTED');
      numgcs = d3.getAttribute('GCS');
      var d4 = getChild(getChild(xdoc,'MEMORY'),'ALLOCATIONS');
      totalobject = d4.getAttribute('TOTAL');
    }

   totaltime = tottime;

   return TABLE({border: '1', id: 'headertbl'},
		TR(TD('Start Class'),TD(start)),
		   TR(TD('Total Time'),TD(tottime)),
		   TR(TD('# Samples'),TD(numsamp)),
		   TR(TD('% CPU Used'),TD(pctused)),
		   TR(TD('# Processors'),TD(numproc)),
		   TR(TD('# Threads'),TD(numthrd)),
		   TR(TD('Running'),TD(running)),
		   TR(TD('Memory Used'),TD(memused)),
		   TR(TD('Memory Committed'),TD(memcomm)),
		   TR(TD('# Allocations'),TD(totalobject)),
		   TR(TD('# GCs'),TD(numgcs)));
}



function cpuItemFilter(xdoc)
{
   var b = xdoc.getAttribute("BASETIME");
   var t = xdoc.getAttribute("TOTALTIME");
   if (b / totaltime > 0.10) return true;
   return false;
}



function threadFilter(xdoc)
{
   var t = xdoc.getAttribute('RUNPCT');
   var b = xdoc.getAttribute('BLOCKPCT');

   return t >= 1.0 || b >= 1.0;
}



function allocFilter(xdoc)
{
   var c = xdoc.getAttribute('COUNT');

   return c / totalobject > 0.10;
}



function ioFilter(xdoc)
{
   var p = xdoc.getAttribute('PCT');
   return p > 5;
}



function heapFilter(xdoc)
{
   var p = xdoc.getAttribute('PCT');
   return p > 10;
}



/********************************************************************************/
/*										*/
/*	Control button methods							*/
/*										*/
/********************************************************************************/

function processControls(xdoc)
{
   var en = xdoc.getAttribute("ENABLED");
   var ovhd = xdoc.getAttribute("OVERHEAD");

   var sel = document.controlform.overhead;
   var opts = sel.options;
   var idx = -1;
   for (i = 0; i < opts.length; ++i) {
      if (opts[i].value == ovhd) idx = i;
    }

   if (idx == -1) {
      // alert("Bad overhead " + ovhd + " " + opts[0] + " " + opts[1] + " " + opts[2] + " " + opts[3] + " " + opts[4]);
    }

   sel.selectedIndex = idx;

   var chk = document.controlform.EnableButton;
   if (en == "false") {
      chk.style.backgroundColor = '#FF0000';
      chk.value = 'Enable Monitoring';
      chk.checked = false;
    }
   else {
      chk.style.backgroundColor = '#00FF00';
      chk.value = 'Disable Monitoring';
      chk.checked = true;
    }
}




/********************************************************************************/
/*										*/
/*	Command methods 							*/
/*										*/
/********************************************************************************/

function sendDymonCommand(cmd,pid,arg)
{
   var xhr = new XMLHttpRequest();
   xhr.open('POST','dymoncommand.php?PID=' + pid + '&CMD=' + cmd + '&ARG=' + arg);
   xhr.send();
}



/********************************************************************************/
/*										*/
/*	Utility methods 							*/
/*										*/
/********************************************************************************/

function getTextElement(address,tag)
{
   var x = address.getElementsByTagName(tag);
   if (x.length == 0) return "";
   x = x[0];
   if (x.firstChild == null) return "";

   return x.firstChild.data;
}



function getChild(data,key)
{
   if (data == null || data.childNodes == null) return null;

   for (i = 0; i < data.childNodes.length; ++i) {
      var xdoc = data.childNodes[i];
      if (xdoc.tagName == key) return xdoc;
    }

   return null;
}





/* end of dymontables.js */

