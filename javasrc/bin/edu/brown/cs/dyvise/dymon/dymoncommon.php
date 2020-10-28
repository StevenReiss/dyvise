<?php

/********************************************************************************/
/*										*/
/*		dymoncommon.php 						*/
/*										*/
/*	Common and utility functions for dymon displays 			*/
/*										*/
/********************************************************************************/
/*	Copyright 2005 Brown University -- Steven P. Reiss		      */
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

error_reporting(15);




/********************************************************************************/
/*										*/
/*	Methods for communicating with Dymon Server				*/
/*										*/
/********************************************************************************/

$host = "NONE";
$port = 0;


function getDymonHostPort()
{
   global $host,$port;
   $inf = fopen("/web/dweb-devel/html/dyvise/dymon/.dymon_ui","r");
   fscanf($inf,"%s %d",$host,$port);
}




function readMessage($ss)
{
   $msg = "";
   while (1) {
      $txt = fgets($ss);
      if ($txt == "" || $txt == "***EOM***" || $txt == "***EOM***\n") break;
      $msg .= $txt;
    }

   return $msg;
}



/********************************************************************************/
/*										*/
/*	Methods to access the process table					*/
/*										*/
/********************************************************************************/

function getProcTable($ct)
{
   return sendDymonCommand("PTABLE $ct");
}



/********************************************************************************/
/*										*/
/*	Methods to get analysis for a given process				*/
/*										*/
/********************************************************************************/

function getAnalysis($pid)
{
   return sendDymonCommand("ANALYSIS $pid *");
}



/********************************************************************************/
/*										*/
/*	Methods to send a command to dymon					*/
/*										*/
/********************************************************************************/

function sendDymonCommand($cmd)
{
   global $host,$port;

   $socket = @fsockopen($host,$port,$errno,$errstr,30);
   if (!$socket) return NULL;

   fwrite($socket,"$cmd\n");
   fflush($socket);

   $rply = readMessage($socket);

   return $rply;
}



/********************************************************************************/
/*										*/
/*	Session management methods						*/
/*										*/
/********************************************************************************/

function handleSession()
{
   global $cnt,$host,$port;

   session_start();

   getDymonHostPort();

   if (!isset($_SESSION['COUNT'])) {
      $_SESSION['COUNT'] = 0;
      $_SESSION['HOST'] = $host;
      $_SESSION['PORT'] = $port;
    }
   else if (array_key_exists('HOST',$_SESSION) &&
	       $_SESSION['HOST'] == $host &&
	       $_SESSION['PORT'] == $port) {
      $cnt = $_SESSION['COUNT'];
    }
   else {
      $cnt = 0;
      $_SESSION['COUNT'] = 0;
      $_SESSION['HOST'] = $host;
      $_SESSION['PORT'] = $port;
    }
}



/********************************************************************************/
/*										*/
/*	Xml manipulation methods						*/
/*										*/
/********************************************************************************/

/* FROM PHP MANUAL */
/*
|
| _xml2array - another abstraction layer on xml_parse_into_struct
|	       that returns a nice nested array.
|
|      @param: $xml is a string containing a full xml document
|
|    returns: a nested php array that looks like this:
|
|	       array
|	       (
|		   [name] => the name of the tag
|		   [attributes] => an array of 'attribute'=>'value' combos
|		   [value] => the text contents of the node
|		   [children] => an array of these arrays, one for each node.
|	       )
|
|      notes: thanks to 'jeffg at activestate dot com' who inspired
|	       me to essentially re-write his example code from php.net
|
|	   me: Kieran Huggins < kieran[at]kieran[dot]ca >
|
*/

function _xml2array($xml)
{
   if (!is_array($xml)){ // init on first run
      $raw_xml = $xml;
      $p = xml_parser_create();
      xml_parser_set_option($p, XML_OPTION_CASE_FOLDING, 0);
      xml_parser_set_option($p, XML_OPTION_SKIP_WHITE, 1);
      xml_parse_into_struct($p, $raw_xml, $xml, $idx);
      xml_parser_free($p);
    }

   for ($i=0; $i<count($xml,1); $i++) {
      // set the current level
      if (!array_key_exists($i,$xml)) $xml[$i] = array('level' => 0);
      $level = $xml[$i]['level'];

      if ($level<1) break;

      // mark this level's tag in the array
      $keys[$level] = '['.$i.']';

      // if we've come down a level, sort output and destroy the upper level
      if (count($keys)>$level) unset($keys[count($keys)]);

      // ignore close tags, they're useless
      if ($xml[$i]['type']=="open" || $xml[$i]['type']=="complete") {
	 // build the evalstring
	 $e = '$output'.implode('[\'children\']',$keys);

	 // set the tag name
	 eval($e.'[\'name\'] = $xml[$i][\'tag\'];');

	 // set the attributes
	 if (array_key_exists('attributes',$xml[$i]) && $xml[$i]['attributes']){
	    eval($e.'[\'attributes\'] = $xml[$i][\'attributes\'];');
	  }

	 // set the value
	 if (array_key_exists('value',$xml[$i]) && $xml[$i]['value']){
	    eval($e.'[\'value\'] = trim($xml[$i][\'value\']);');
	  }
       }
    }

   return $output[0];
}




function getTextValue($xml,$key)
{
   if (array_key_exists('attributes',$xml)) {
      $attr = $xml['attributes'];
      if (array_key_exists($key,$attr)) return $attr[$key];
    }

   if (!array_key_exists('children',$xml)) return NULL;

   foreach ($xml['children'] as $c) {
      if ($c['name'] == $key) {
	 if (array_key_exists('value',$c)) return $c['value'];
	 else return NULL;
       }
    }

   return NULL;
}




/* end of dymoncommon.php */

?>
