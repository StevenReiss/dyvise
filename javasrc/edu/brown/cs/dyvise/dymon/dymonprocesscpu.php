<?php

/********************************************************************************/
/*										*/
/*		dymonprocesscpu.php						*/
/*										*/
/*	Output the cpu usage analysis for a particular process			*/
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

include("dymonxml.php");

   session_start();
   $_SESSION['COUNT'] = 0;

   $pid = $_REQUEST['PID'];

?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<title>DYMON Monitor Control</title>

<script type="text/javascript" src="../flapjax/flapjax.js"></script>
<script type="text/javascript" src="../flapjax/json.js"></script>
<script type="text/javascript" src="../js/dymonmaketable.js"></script>
<script type="text/javascript" src="../js/dymonflapjax.js"></script>
<script type="text/javascript" src="../js/dymontables.js"></script>

<style type="text/css">
<!--
body {
	background-color: #FFFFCC;
}
.style1 {font-size: 48px}
.style2 {font-size: 24px; }
.style3 {font-size: 16px; }
-->
</style></head>


<?php
echo "<body onload='loadCpuTables(\"$pid\")'>\n";
echo "<h1 align='center' class='style1'>DYPER CPU Data for $pid</h1>\n";
?>
<hr />

<h2 align="left" class="style2">Cpu Usage:</h2>
<div id='cputbl'></div>

<hr />

<h2 align="left" class="style2">Threads Activity:</h2>
<div id='threadtbl'></div>

<hr />

<h2 align="left" class="style2">Threads Timing:</h2>
<div id='timingtbl'></div>

<hr />

<h2 align="left" class="style2">I/O Operations:</h2>
<div id='iotbl'></div>

<hr />

</body>
</html>




