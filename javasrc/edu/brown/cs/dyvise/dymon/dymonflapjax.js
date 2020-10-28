/********************************************************************************/
/*										*/
/*		dymonflapjax.js 						*/
/*										*/
/*	Flap-jax code that has to be included for dymon displays		*/
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


function initFlapjaxWebServiceAPIs (flapjax) {
  var version = 3;
	if (flapjax.util.version != version) {
		throw 'fjws.js (v' + version + ') out of sync with flapjax.js (v' + flapjax.util.version + ')';
	}

	/*<annotation>
	   <function>
	   <variation><name>$___defs</name></variation>
	   <contract>Array ( Annotation * String * Object)</contract>
	   <description>
		<p>Internal library function used for labeling functions for construction of the library.</p>
		<p>An Annotation is an object literal:</p>

<p><pre>	Object (
		[np: Boolean] - not public - do not add function to flapjax.pub, default false
		[b: Boolean] - behaviour - add function to behaviour prototype, default false
		[e: Boolean] - event - add function to event prototype, default false
		[c: Boolean] - context - first parameter to function is context, default false
		[a: Number] - argument - parameter position to replace with object when used in prototype, default: don't do it
		[l: Boolean ] - lifted - default true
	)</pre></p>
		</description>
		<examples>
			<example>
				<explanation>Add the identity function to the top level</explanation>
				<code>$___defs.push([{}, 'identity', function (v} { return v;}]);</code>
			</example>
		</examples>
		</function>
	</annotation>*/
	var $___a =[];

	//$$___a: {[np: boolean] [b: boolean] [e: boolean] [c: boolean] [a: Number] }
	//					* String fnName * fn ->
	var $$___a = function (props, n, fn) { $___a.push(arguments); };

	/**
	* Create a new Exception object.
	* name: The name of the exception.
	* message: The exception message.
	*/
	var Exception = function(name, message)
	{
			if (name) { this.name = name; }
			if (message) { this.message = message; }
	};

	/**
	* Set the name of the exception.
	*/
	Exception.prototype.setName = function(name)
	{
			this.name = name;
	};

	/**
	* Get the exception's name.
	*/
	Exception.prototype.getName = function()
	{
			return this.name;
	};

	/**
	* Set a message on the exception.
	*/
	Exception.prototype.setMessage = function(msg)
	{
			this.message = msg;
	};

	/**
	* Get the exception message.
	*/
	Exception.prototype.getMessage = function()
	{
			return this.message;
	};

	var $ = function() {
		var elements = [];

		for (var i = 0; i < arguments.length; i++) {
			var element = arguments[i];
			if (typeof element == 'string') {
				element = document.getElementById(element);
			}
			if (arguments.length == 1) {
				return element;
			}

			elements.push(element);
		}

		return elements;
	}

	var _getWebServiceObject_e = function(maybeKeyNode, objE) {
		var genRequest = function(f) {
			var req = '';
			var first = true;
			for (var key in f) {
				if ((typeof f[key] != 'function') &&
						(!f.prototype ||
						!f.prototype.key)) {
					if (!first) {
						req += "&";
					} else {
						first = false;
					}
					req += key += "=" + escape(f[key]);
				}
			}
			return req;
		};
		var ws_e = receiver_e();
		objE.transform_e(
			function (obj) {
						var body = '';
						var method = 'get';
						var url = obj.url;
						var reqType = obj.request ? obj.request :
							obj.fields ? 'post' : 'get';
						// TODO add support for 'xmlRpc' and 'jsonRpc'
						if (obj.request == 'get') {
							url += "?" + genRequest(obj.fields);
							body = '';
							method = 'GET';
						} else if (obj.request == 'post') {
							body = genRequest(obj.fields);
							method = 'POST';
						} else if (obj.request == 'rawPost') {
							body = obj.body;
							method = 'POST';
						}
						var xhr = new XMLHttpRequest();
						if(obj.serviceType == 'jsonLiteral')
							xhr.onload = function() {ws_e.sendEvent(this.responseText.parseJSON());};
						else if(obj.serviceType == 'xml')
							xhr.onload = function() {ws_e.sendEvent(this.responseXML);};
						xhr.open(method,url,obj.asynchronous);
						xhr.send(body);
				});
				return ws_e;
				};
	$$___a({l:false,c:true},'getWebServiceObject_e',_getWebServiceObject_e);
	$$___a({l:false,c:true,e:true,a:0},'getWebServiceObject',_getWebServiceObject_e);

	/////////////////////////////////////////////////////
	// Process annotations & export
	/////////////////////////////////////////////////////

	var fjws = {};
	fjws.pub = {};
	var explicitContext = false;
	var makeTopLevel = true;
	var context = flapjax.maybeEmpty;

	var annotations =
	{
		/* add context check */
		checkForContextFn:
			function (fnName, fn) {
				return function (maybeKeyNode /* . rest */) {
						if (!(maybeKeyNode instanceof flapjax.Maybe)) {
							throw fnName + ': expected Maybe Event as first argument';
						}
						return fn.apply(this, arguments);
					};
			},

		/* add to prototype */
		//when chaining function, thread parent is 'a'th argument to function
		addTupleToProtoMaker:
			function (proto) {
				return function (tuple) {
					proto[tuple[1]] =
						function () {

							var args;
							if (tuple[0].c && explicitContext !== true && (tuple[0].a !== undefined)) {
								args = [];
								//remember, context already removed
								for (var i = 0; i < tuple[0].a - 1; i++) {
									args.push(arguments[i]);
								}
								args.push(this);
								args = args.concat(flapjax.slice(arguments, tuple[0].a - 1));
							} else if ( tuple[0].a !== undefined ) {
								args = [];
								for (var jj = 0; jj < tuple[0].a; jj++) {
									args.push(arguments[jj]);
								}
								args.push(this);
								args = args.concat(flapjax.slice(arguments, tuple[0].a));
							} else {
								args = arguments;
							}

							return tuple[2].apply(this, args);
						};
				};
			},

		makeImplicitContextFn:
			function (fn) {
				return function () {
					//BUG 8/17/2006 - leo - concat(arguments) doesn't work, make copy
					var args = [context].concat(flapjax.slice(arguments, 0));
					return fn.apply(this, args);
				};
			}
	};

	//////////////// APPLY ANNOTATIONS /////////////
	for (var k = 0; k < $___a.length; k++) {

//		if ($___a[k][2] == currChunk[2]) { $___a[k][3] = currChunk; }
//		else { currChunk = $___a[k]; }

		//tuple: [{attributes}, String name, Function]

		if ($___a[k][0].c) {

			///////// 1. CONTEXT CHECK
			$___a[k][2] = annotations.checkForContextFn($___a[k][1], $___a[k][2]);

			///////// 2. MAKE CONTEXT EXPLICIT
			if (explicitContext !== true) {
				$___a[k][2] = annotations.makeImplicitContextFn($___a[k][2]);
			}

		}

		///////// 3.5 MIXED LIFT ANNOTATION
		/* if l is true, then the function should be lifted,
		  so we set ___doNotLift to false.  if l is false, then
		  the function shouldn't be lifted, so we set ___doNotLift
		  to true.  the default value for ___doNotLift is false,
		  which is to say that functions are lifted by default. */
		$___a[k][2].___doNotLift =
			($___a[k][0].l === false) ? true :
			($___a[k][0].l === true) ? false :
			false; // default

		///////// 4. PACKAGE FUNCTIONS
		if ($___a[k][0].np !== true) { fjws.pub[$___a[k][1]] = $___a[k][2]; }
	}

	///////// 5. OPTIONALLY EXPORT PUBLIC FUNCTIONS TO GLOBAL NAMESPACE
	if (makeTopLevel !== false) {
		for (var zz in fjws.pub) {
			eval(zz + " = fjws.pub." + zz + ";");
		}
	}


	return fjws.pub;

}

