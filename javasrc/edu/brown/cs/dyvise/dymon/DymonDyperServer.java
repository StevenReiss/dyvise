/********************************************************************************/
/*										*/
/*		DymonDyperServer.java						*/
/*										*/
/*	Server to allow dyper to use mint without any mint code 		*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss			*/
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 * This program and the accompanying materials are made available under the	 *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at								 *
 *	http://www.eclipse.org/legal/epl-v10.html				 *
 *										 *
 ********************************************************************************/


/* SVN: $Id: DymonDyperServer.java,v 1.1 2016/11/02 18:59:13 spr Exp $ */




package edu.brown.cs.dyvise.dymon;

import edu.brown.cs.ivy.mint.*;

import java.io.*;
import java.net.*;
import java.util.*;



public class DymonDyperServer implements MintConstants, DymonConstants
{



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   DymonDyperServer bds = new DymonDyperServer(args);
   bds.process();
}




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		mint_name;
private String		our_name;
private int		port_number;
private SocketThread	socket_thread;
private MintControl	mint_control;
private Set<Connection> active_connections;
private Map<String,Connection> named_connections;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private DymonDyperServer(String [] args)
{
   mint_name = null;
   our_name = null;
   port_number = 0;
   socket_thread = null;
   mint_control = null;
   active_connections = new HashSet<Connection>();
   named_connections = new HashMap<String,Connection>();

   scanArgs(args);
}




/********************************************************************************/
/*										*/
/*	Argument processing							*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
	 if (args[i].startsWith("-M") && i+1 < args.length) {           // -M <mint name>
	    mint_name = args[++i];
	  }
	 else badArgs();
       }
      else badArgs();
    }

   if (mint_name == null) badArgs();
}



private void badArgs()
{
   System.err.println("DymonDyperServer: dymondyperserver -M <mintname>");
   System.exit(1);
}




/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

private void process()
{
   if (mint_name == null) return;
   if (our_name == null) our_name = mint_name + "_DYPER";

   try {
      ServerSocket ss = new ServerSocket(port_number,5);
      if (!MintConnect.registerSocket(our_name,ss)) return;

      mint_control = MintControl.create(mint_name,MintSyncMode.ONLY_REPLIES);
      mint_control.register("<DYPER PID='_VAR_1'/>",new DyperHandler());
      mint_control.register("<DYPER COMMAND='WHO' />",new WhoHandler());
      mint_control.register("<DYMON CMD='_VAR_0' />",new DymonHandler());

      socket_thread = new SocketThread(ss);
      socket_thread.start();
    }
   catch (IOException e) { }
}




/********************************************************************************/
/*										*/
/*	Connection processing methods						*/
/*										*/
/********************************************************************************/

private void handleConnection(Socket s)
{
   Connection c = new Connection(s);
   active_connections.add(c);
   c.start();
}



private void removeConnection(Connection c)
{
   active_connections.remove(c);

   if (active_connections.size() == 0) {
      System.exit(0);
    }
}


/********************************************************************************/
/*										*/
/*	Thread for accepting connections from debug clients			*/
/*										*/
/********************************************************************************/

private class SocketThread extends Thread {

   private ServerSocket server_socket;

   SocketThread(ServerSocket ss) {
      super("DymonDyperServerSocketListener_" + ss);
      server_socket = ss;
    }

   String getLocation() {
      String h = server_socket.getInetAddress().getHostAddress();
      if (h.equals("0.0.0.0")) {
	try {
	    h = InetAddress.getLocalHost().getHostAddress();
	  }
	 catch (UnknownHostException e) { }
       }
      return "<SOCKET HOST='" + h + "' PORT='" + server_socket.getLocalPort() + "' />";
    }

   @Override public void run() {
      Socket s;
      while (server_socket != null) {
	 try {
	    s = server_socket.accept();
	    // System.err.println("DYMONDYPERSERVER: Process: " + s);
	    handleConnection(s);
	  }
	 catch (Throwable e) {
	    System.err.println("DYMONDYPERSERVER: I/O Error: " + e);
	  }
       }
      System.err.println("DYMONDYPERSERVER: Exiting");
    }

}	// end of inner class SocketThread




/********************************************************************************/
/*										*/
/*	Class representing a debugger connection				*/
/*										*/
/********************************************************************************/

private class Connection extends Thread {

   private Socket client_socket;
   private String client_id;
   private PrintWriter output_writer;
   private Queue<MintMessage> pending_messages;

   Connection(Socket s) {
      super("DymonDyperConnection_" + s.getRemoteSocketAddress());
      // System.err.println("DYMONDYPERSERVER: start connection " + s);
      try {
	 s.setSoTimeout(60000);
       }
      catch (SocketException e) {
	 System.err.println("DYMONDYPER: Problem with socket timeout: " + e);
       }
      client_socket = s;
      output_writer = null;
      client_id = null;
      pending_messages = new LinkedList<MintMessage>();
    }

   @Override public void run() {
      // System.err.println("DYMONDYPERSERVER: start connect");
      
      try (InputStream ins = client_socket.getInputStream();
         BufferedReader lnr = new BufferedReader(new InputStreamReader(ins))) {
         boolean done = false;
         while (!done) {
            StringBuffer body = null;
            boolean isreply = false;
            for ( ; ; ) {
               String s = "";
               try {
                  s = lnr.readLine();
                }
               catch (SocketTimeoutException e) {
                  continue;
                }
               
               // System.err.println("SERVER: READ " + s);
               if (s == null) {
                  done = true;
                  break;
                }
               if (s.equals(DYMON_DYPER_TRAILER)) break;
               if (s.equals(DYMON_DYPER_REPLY_TRAILER)) {
                  isreply = true;
                  break;
                }
               if (body == null) body = new StringBuffer(s);
               else {
                  body.append("\n");
                  body.append(s);
                }
             }
            if (isreply) {
               String reply = null;
               if (body != null) reply = body.toString();
               MintMessage msg = pending_messages.poll();
               if (msg != null) {
                  // System.err.println("SERVER: REPLY: " + reply);
                  msg.replyTo(reply);
                }
               else {
                  // System.err.println("SERVER: No pending message");
                }
               
             }
            else if (body != null) {
               String btxt = body.toString();
               if (client_id == null && btxt.startsWith("CONNECT")) {
                  client_id = btxt.substring(8).trim();
                  named_connections.put(client_id,this);
                }
               else {
                  // System.err.println("SERVER: Send to mint: " + btxt);
                  mint_control.send(btxt);
                }
             }
          }
       }
      catch (IOException e) {
         System.err.println("DYMONDYPERSERVER: Problem with socket: " + e);
         e.printStackTrace();
       }
      catch (Throwable t) {
         System.err.println("DYMONSYPERSERVER: Problem with command: " + t);
         t.printStackTrace();
       }
      
      System.err.println("DYMONDYPERSERVER: CONNECTION EXITED");
      
      try {
         if (client_socket != null) client_socket.close();
         if (output_writer != null) output_writer.close();
       }
      catch (IOException e) { }
      
      removeConnection(this);
   }

   synchronized void sendCommand(MintMessage msg) {
      pending_messages.add(msg);;
      if (output_writer == null) {
	 try {
	    output_writer = new PrintWriter(client_socket.getOutputStream());
	  }
	 catch (IOException e) { return; }
       }
      // System.err.println("SERVER: SEND: " + msg.getText());
      output_writer.println(msg.getText());
      output_writer.println(DYMON_DYPER_TRAILER);
      output_writer.flush();
    }

}	// end of inner class Connection





/********************************************************************************/
/*										*/
/*	Message handling							*/
/*										*/
/********************************************************************************/

private class DyperHandler implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      String id = args.getArgument(1);
      Connection c = named_connections.get(id);
      if (c != null) c.sendCommand(msg);
    }

}	// end of inner class DyperHandler



private class WhoHandler implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      for (Connection c : active_connections) {
	 c.sendCommand(msg);
       }
      msg.replyTo();
    }

}	// end of inner class WhoHandler



private class DymonHandler implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      String cmd = args.getArgument(0);
      String rply = null;
      if (cmd == null) ;
      else if (cmd.equals("PORT")) {
	 rply = socket_thread.getLocation();
       }

      msg.replyTo(rply);
    }

}	// end of inner class BumpHandler




}	// end of class DymonDyperServer




/* end of DymonDyperServer.java */
