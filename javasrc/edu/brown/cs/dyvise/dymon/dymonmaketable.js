function collect_b(initval,evt,funct) {
	return evt.collect_e(initval,funct).startsWith(initval);
}

function HeaderSortWidget(headers) {
	this.dom = '';
	this.behaviors = {};
	this.events = {};

	var headerClickEvents = [];
	var tableHeadCols = map(function(col) {
		var hl = A({href:'javascript://Sort',className:'k-url',title:'Sort by '+col.text},col.text);
		headerClickEvents.push(extractEvent_e(hl,'click').constant_e(col));
		return TH({className:col.className,scope:'column'},hl);
	},headers);
	var sortInfoB = collect_b({col:headers[0],order:1},merge_e.apply(merge_e,headerClickEvents),
		function(col,prev) {if(col == prev.col) return {col:col,order:prev.order*-1}; else return {col:col,order:1};});
	this.behaviors.sortInfo = sortInfoB.transform_b(function(si) {return {order:si.order,sortFn:si.col.sortFn};});
	this.dom = THEAD(tableHeadCols);
}

/* makeGenericTable :: [String] x [('string' | 'num')] x Behavior [Row] x Props -> Behavior Dom
   Makes a dynamically sorting table.
   The first argument is an array of titles of columns; the second is an array of
   column types. The i-th element in this array should be 'string' if the i-th column
   should be sorted lexicographically, and 'num' if it should be sorted numerically.

   The third argument is a time-varying array of rows, where each row is an array of strings.

   The function will return a time-varying DOM node representing a table displaying all the data.
*/
function makeGenericTable(titles,types,dataB,props) {
	var headers = [];
	var i;
	var makeSortF = function(type, i) {
		if(type == 'string') {
			return function(a,b) {
				if (a[i] < b[i]) return -1;
				if (b[i] < a[i]) return 1;
				return 0;
			}
		} else {
			return function(a,b) {
				return parseFloat(b[i])-parseFloat(a[i]);
			}
		}
	};

	for(i=0;i<titles.length;i++) {
		headers.push({text:titles[i],className:'sorthead',sortFn:makeSortF(types[i],i)});
	};
	var sorter = new HeaderSortWidget(headers);
	var sortFnB = sorter.behaviors.sortInfo.transform_b(function(si) {
		    return function(a,b) {return si.sortFn(a,b)*si.order;};
	    });
	var sortedPapersB = lift_b(function(data,sortFn) {
		data = data.sort(sortFn);
		return map(function(row) {
			return TR(map(function(cell) {return TD(cell);},row));},data.sort(sortFn));
	},dataB,sortFnB);
	return TABLEB(props,
		      sorter.dom,TBODYB(sortedPapersB));
}



function makeProcTable(titles,types,dataB,props,linkf) {
	var headers = [];
	var i;
	var makeSortF = function(type, i) {
		if(type == 'string') {
			return function(a,b) {
				if (a[i] < b[i]) return -1;
				if (b[i] < a[i]) return 1;
				return 0;
			}
		} else {
			return function(a,b) {
				return parseFloat(b[i])-parseFloat(a[i]);
			}
		}
	};

	for(i=0;i<titles.length;i++) {
		headers.push({text:titles[i],className:'sorthead',sortFn:makeSortF(types[i],i)});
	};
	var sorter = new HeaderSortWidget(headers);
	var sortFnB = sorter.behaviors.sortInfo.transform_b(function(si) {
		    return function(a,b) {return si.sortFn(a,b)*si.order;};
	    });
	var sortedPapersB = lift_b(function(data,sortFn) {
		data = data.sort(sortFn);
		return map(function(row) {
			var itm = 0;
			return TR(map(function(cell) {
					 var x = TD(linkf(cell,row,itm));
					 ++itm;
					 return x;
				       },row));
			    },data.sort(sortFn));
	},dataB,sortFnB);
	return TABLEB(props, sorter.dom,TBODYB(sortedPapersB));
}



