<?php

/********************************************************************************/
/*										*/
/*		dymonsummary.php						*/
/*										*/
/*	Output the summary analysis for a particular process			*/
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
.style2 {font-size: 24px; }
.style3 {font-size: 16px; }
.button1 {font-weight: bold }
.button2 {
	font-weight: bold;
	color: #000000;
	background-color: #FFFF00;
	border-top-width: medium;
	border-right-width: medium;
	border-bottom-width: medium;
	border-left-width: medium;
	border-top-style: groove;
	border-right-style: groove;
	border-bottom-style: groove;
	border-left-style: groove;
}
.btnenable {
	font-weight: bold;
	background-color: #FF0000;
	font-size: large;
	border: medium inset #000000;
}
-->
</style></head>


<?php
echo "<body onload='loadSummaryTables(\"$pid\")'>\n";
?>

<?php
echo "<h1 align='center' class='style1'>DYPER Summary for $pid</h1>\n";
?>
<hr />

<h2 align="left" class="style2">Process Information and Control:</h2>
<table width='100%'>
<colgroup><col width='1*'><col width='2*'>
<tr>
<td><div id='headertbl'></div></td>
<td align='center' valign='middle'>
<form name='controlform'>
<h3 align='center' class='style3'>Control Panel</h3>
<input class='btnenable' type="button" name="EnableButton" id="EnableButton"
	value="Enable Monitoring"
<?php
	echo " onclick=\"sendDymonCommand('ENABLE','$pid','*')\"";
?>
/>
<p>
<input name="ClearButton" type="button" class="button2" id="ClearButton" value="Clear Statistics"
<?php
	echo " onclick=\"sendDymonCommand('CLEAR','$pid','*')\"";
?>
/>
</p>
<p>
<label class='button1'>Impact:
  <select name="overhead" id='overhead'
<?php
     echo " onchange=\"sendDymonCommand('OVERHEAD','$pid',
			this.options[this.selectedIndex].value)\"";
?>
	>
    <option value="0.001">Very Low Impact (0.1%)</option>
    <option value="0.01">Low Impact (1%)</option>
    <option value="0.05" selected="selected">Medium Impact (5%)</option>
    <option value="0.1">High Impact (10%)</option>
    <option value="0.25">Very High Impact (25%)</option>
    </select>
  </label>
</p>
</form>
</td></tr>
</table>

<hr />

<?php
echo "<h2 align='left' class='style2'><A href='dymonprocesscpu.php?PID=$pid'>Cpu Usage</A> over 10%:</h2>\n";
?>
<div id='cputbl'></div>

<hr />

<?php
echo "<h2 align='left' class='style2'><A href='dymonprocesslock.php?PID=$pid'>Threads and Locking</A></h2>\n";
?>
<div id='threadtbl'></div>

<hr />

<?php
echo "<h2 align='left' class='style2'><A href='dymonprocessalloc.php?PID=$pid'>Allocations</A> over 10%:</h2>\n";
?>
<div id='alloctbl'></div>

<hr />

<?php
echo "<h2 align='left' class='style2'><A href='dymonprocessalloc.php?PID=$pid'>Heap Usage</A> over 10%:</h2>\n";
?>
<div id='heaptbl'></div>

<hr />

<?php
echo "<h2 align='left' class='style2'><A href='dymonprocessio.php?PID=$pid'>Significant I/O Operations</A>:</h2>\n";
?>
<div id='iotbl'></div>

<hr />

</body>
</html>




