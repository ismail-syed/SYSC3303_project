package TFTP;


/**
 * The {@link TFTP.TFTPSim} class represents a TFTP Error Simiulator
 * (based on Assignment 1 solution)
 *
 * @author Team 3000000
 * @author Shasthra Ranasinghe
 * @version 1.0
 */
//TFTPSim.java
//This class is the beginnings of an error simulator for a simple TFTP server 
//based on UDP/IP. The simulator receives a read or write packet from a client and
//passes it on to the server.  Upon receiving a response, it passes it on to the 
//client.
//One socket (23) is used to receive from the client, and another to send/receive
//from the server.  A new socket is used for each communication back to the client.   

import java.io.*;
import java.net.*; 
import java.util.*;

import TFTPPackets.TFTPPacket;

public class TFTPSim {

// UDP datagram packets and sockets used to send / receive
private DatagramPacket sendPacket, receivePacket;
private DatagramSocket receiveSocket, sendSocket, sendReceiveSocket;
private int serverPort = 69;
private int requestOpCode;//used to distinguish between read and write

public TFTPSim()
{
   try {
      // Construct a datagram socket and bind it to port 23
      // on the local host machine. This socket will be used to
      // receive UDP Datagram packets from clients.
      receiveSocket = new DatagramSocket(23);
      // Construct a datagram socket and bind it to any available
      // port on the local host machine. This socket will be used to
      // send and receive UDP Datagram packets from the server.
      sendReceiveSocket = new DatagramSocket();
   } catch (SocketException se) {
      se.printStackTrace();
      System.exit(1);
   }
}

public void passOnTFTP()
{

   byte[] data;
   
   int clientPort, j=0, fromServerLen,fromClientLen,endOfWriteDataSize;

   for(;;) { // loop forever
      // Construct a DatagramPacket for receiving packets up
      // to 516 bytes long (the length of the byte array).
      
      data = new byte[516];
      receivePacket = new DatagramPacket(data, data.length);

      System.out.println("Simulator: Waiting for packet.");
      // Block until a datagram packet is received from receiveSocket.
      try {
         receiveSocket.receive(receivePacket);
      } catch (IOException e) {
         e.printStackTrace();
         System.exit(1);
      }
      
      if(data[1]==(byte)1) requestOpCode = 1;
      if(data[1]==(byte)2) requestOpCode = 2;

      // Process the received datagram.
      System.out.println("Simulator: Packet received:");
      System.out.println("From host: " + receivePacket.getAddress());
      clientPort = receivePacket.getPort();
      System.out.println("Host port: " + clientPort);
      //if its a data block save to size to check later
      if(data[1]==(byte)3){
    	  endOfWriteDataSize = receivePacket.getLength();
      }else{
    	  endOfWriteDataSize = 516;
      }
      fromClientLen = receivePacket.getLength();
      System.out.println("Length: " + fromClientLen);
      System.out.println("Containing: " );
      System.out.println("Byte Array: " + TFTPPacket.toString(Arrays.copyOfRange(data,0,fromClientLen))+"\n");

      // Form a String from the byte array, and print the string.
      String received = new String(data,0,fromClientLen);
      System.out.println(received);
      
      // Now pass it on to the server (to port 69)
      // Construct a datagram packet that is to be sent to a specified port
      // on a specified host.
      // The arguments are:
      //  msg - the message contained in the packet (the byte array)
      //  the length we care about - k+1
      //  InetAddress.getLocalHost() - the Internet address of the
      //     destination host.
      //     In this example, we want the destination to be the same as
      //     the source (i.e., we want to run the client and server on the
      //     same computer). InetAddress.getLocalHost() returns the Internet
      //     address of the local host.
      //  69 - the destination port number on the destination host.

      sendPacket = new DatagramPacket(data, fromClientLen,
                                     receivePacket.getAddress(), serverPort);
     
      System.out.println("Simulator: sending packet.");
      System.out.println("To host: " + sendPacket.getAddress());
      System.out.println("Destination host port: " + sendPacket.getPort());
      //fromClientLen = sendPacket.getLength();
      System.out.println("Length: " + fromClientLen);
      System.out.println("Containing: ");
      System.out.println("Byte Array: " + TFTPPacket.toString(Arrays.copyOfRange(data,0,fromClientLen))+"\n");

      // Send the datagram packet to the server via the send/receive socket.

      try {
         sendReceiveSocket.send(sendPacket);
      } catch (IOException e) {
         e.printStackTrace();
         System.exit(1);
      }
      
      // Construct a DatagramPacket for receiving packets up
      // to 516 bytes long (the length of the byte array).

      data = new byte[516];
      receivePacket = new DatagramPacket(data, data.length);

      System.out.println("Simulator: Waiting for packet.");
      try {
         // Block until a datagram is received via sendReceiveSocket.
         sendReceiveSocket.receive(receivePacket);
      } catch(IOException e) {
         e.printStackTrace();
         System.exit(1);
      }

      // Process the received datagram.
      System.out.println("Simulator: Packet received:");
      System.out.println("From host: " + receivePacket.getAddress());
      serverPort = receivePacket.getPort();
      System.out.println("Host port: " + serverPort);
      fromServerLen = receivePacket.getLength();
      System.out.println("Length: " + fromServerLen);
      System.out.println("Containing: ");
      System.out.println("Byte Array: " + TFTPPacket.toString(Arrays.copyOfRange(data,0,fromServerLen))+"\n");

      // Construct a datagram packet that is to be sent to a specified port
      // on a specified host.
      // The arguments are:
      //  data - the packet data (a byte array). This is the response.
      //  receivePacket.getLength() - the length of the packet data.
      //     This is the length of the msg we just created.
      //  receivePacket.getAddress() - the Internet address of the
      //     destination host. Since we want to send a packet back to the
      //     client, we extract the address of the machine where the
      //     client is running from the datagram that was sent to us by
      //     the client.
      //  receivePacket.getPort() - the destination port number on the
      //     destination host where the client is running. The client
      //     sends and receives datagrams through the same socket/port,
      //     so we extract the port that the client used to send us the
      //     datagram, and use that as the destination port for the TFTP
      //     packet.

      sendPacket = new DatagramPacket(data, receivePacket.getLength(),
                            receivePacket.getAddress(), clientPort);

      System.out.println( "Simulator: Sending packet:");
      System.out.println("To host: " + sendPacket.getAddress());
      System.out.println("Destination host port: " + sendPacket.getPort());
      //fromServerLen = sendPacket.getLength();
      System.out.println("Length: " + fromServerLen);
      System.out.println("Containing: ");
      System.out.println("Byte Array: " + TFTPPacket.toString(Arrays.copyOfRange(data,0,fromServerLen))+"\n");

      // Send the datagram packet to the client via a new socket.

      try {
         // Construct a new datagram socket and bind it to any port
         // on the local host machine. This socket will be used to
         // send UDP Datagram packets.
         sendSocket = new DatagramSocket();
      } catch (SocketException se) {
         se.printStackTrace();
         System.exit(1);
      }

      try {
         sendSocket.send(sendPacket);
      } catch (IOException e) {
         e.printStackTrace();
         System.exit(1);
      }
      
      //check if its the last data block
      if(requestOpCode == 1 && fromServerLen<516){
    	  data = new byte[516];
          receivePacket = new DatagramPacket(data, data.length);

          System.out.println("Simulator: Waiting for packet.");
          // Block until a datagram packet is received from receiveSocket.
          try {
             receiveSocket.receive(receivePacket);
          } catch (IOException e) {
             e.printStackTrace();
             System.exit(1);
          }
          System.out.println("Byte Array: " + TFTPPacket.toString(Arrays.copyOfRange(data,0,receivePacket.getLength()))+"\n");
          
          sendPacket = new DatagramPacket(data, receivePacket.getLength(),
                  receivePacket.getAddress(), serverPort);
          try {
              sendReceiveSocket.send(sendPacket);
           } catch (IOException e) {
              e.printStackTrace();
              System.exit(1);
           }
          System.out.println("Byte Array: " + TFTPPacket.toString(Arrays.copyOfRange(data,0,sendPacket.getLength()))+"\n");
          serverPort = 69;
      }
      
      //check if its the last data block
      if(requestOpCode == 2 && endOfWriteDataSize<516) serverPort = 69;

      System.out.println("Simulator: packet sent using port " + sendSocket.getLocalPort());
      System.out.println();

   } // end of loop

}

	public static void main( String args[] )
	{
	   TFTPSim s = new TFTPSim();
	   s.passOnTFTP();
	}
}


