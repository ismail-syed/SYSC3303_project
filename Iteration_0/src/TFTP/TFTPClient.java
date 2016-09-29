package TFTP;

import static TFTPPackets.TFTPPacket.MAX_SIZE;

//TFTPClient.java
//This class is the client side for a very simple assignment based on TFTP on
//UDP/IP. The client uses one port and sends a read or write request and gets 
//the appropriate response from the server.  No actual file transfer takes place.   

import java.io.*;
import java.net.*;
import java.util.Scanner;

import Exceptions.PacketOverflowException;
import FileIO.TFTPReader;
import FileIO.TFTPWriter;
import TFTPPackets.*;
import TFTPPackets.TFTPPacket.Opcode;

/*
 * @author: Mohamed Zalat & Kunall Banerjee
 * TFTPClient
 */
public class TFTPClient {

private DatagramPacket sendPacket, receivePacket;
private DatagramSocket sendReceiveSocket;
private static String filePath;
private TFTPReader tftpReader;
private TFTPWriter tftpWriter;
private static Mode run;
private boolean firstTime;

// we can run in normal (send directly to server) or test
// (send to simulator) mode
public static enum Mode { NORMAL, TEST};

public TFTPClient()
{
	firstTime = true;
   try {
      // Construct a datagram socket and bind it to any available
      // port on the local host machine. This socket will be used to
      // send and receive UDP Datagram packets.
      sendReceiveSocket = new DatagramSocket();
   } catch (SocketException se) {   // Can't create the socket.
      se.printStackTrace();
      System.exit(1);
   }
}

public void sendRequest(Scanner sc) throws PacketOverflowException, FileNotFoundException
{
   byte[] data = new byte[100];
   String filename; // filename and mode as Strings
   int sendPort, j;
   TFTPPacket packet;
   
   // In the assignment, students are told to send to 23, so just:
   // sendPort = 23; 
   // is needed.
   // However, in the project, the following will be useful, except
   // that test vs. normal will be entered by the user.
   
   if (run==Mode.NORMAL) 
      sendPort = 69;
   else
      sendPort = 23;
   
   boolean done = false;
   while(!done){
	 System.out.println("Enter R for a read request and W for a write request");
	 String cmd = sc.nextLine();
	 
	 if(cmd.equals("W")){
      System.out.println("Client: creating WRQ packet.");

     
     // next we have a file name
 	System.out.println("Enter file name");
 	filename = sc.nextLine();
 	
 		packet = new WRQPacket(filename, RRQWRQPacketCommon.Mode.NETASCII);
 		data = packet.getByteArray();
 	done = true;
 	WRQPacket wPacket = (WRQPacket) packet;
 	tftpReader = new TFTPReader(new File(filePath + wPacket.getFilename()).getPath());
	 }
	 else if(cmd.equals("R")) {
		 System.out.println("Client: creating RRQ packet.");
		 	
		     // next we have a file name -- let's just pick one
		 	System.out.println("Enter file name");
		 	filename = sc.nextLine();
		 	
		 	
		 	packet = new RRQPacket(filename, RRQWRQPacketCommon.Mode.NETASCII);
		 	data = packet.getByteArray();
		 	done = true;
		 	RRQPacket rPacket = (RRQPacket) packet;
		 	 tftpWriter = new TFTPWriter(new File(filePath + rPacket.getFilename()).getPath(),false);
	 }
   }
     try {
        sendPacket = new DatagramPacket(data, data.length,
                            InetAddress.getLocalHost(), sendPort);
     } catch (UnknownHostException e) {
        e.printStackTrace();
        System.exit(1);
     }

     System.out.println("Client: sending packet.");
     System.out.println("To host: " + sendPacket.getAddress());
     System.out.println("Destination host port: " + sendPacket.getPort());
     System.out.println("Length: " + data.length);
     System.out.println("Containing: ");
     for (j=0;j<data.length;j++) {
         System.out.println("byte " + j + " " + data[j]);
     }
	 
     // Form a String from the byte array, and print the string.
     String sending = new String(data,0,data.length);
     System.out.println(sending);

     // Send the datagram packet to the server via the send/receive socket.

     try {
        sendReceiveSocket.send(sendPacket);
     } catch (IOException e) {
        e.printStackTrace();
        System.exit(1);
     }

     System.out.println("Client: Packet sent.");
   }

private void sendReceivePacket(Scanner sc){
    byte dataBuffer[] = new byte[MAX_SIZE];
    byte[] data = null;
    
    if(firstTime){
    	try {
			sendRequest(sc);
		} catch (Exception e) {
			e.printStackTrace();
		}
    	firstTime = false;
    }
    
    receivePacket = new DatagramPacket(dataBuffer, dataBuffer.length);
    try {
        //Receive packet
        sendReceiveSocket.receive(receivePacket);
        //Create byte array of proper size
        data = new byte[receivePacket.getLength()];
        System.arraycopy(dataBuffer, 0, data, 0, data.length);
        System.out.println("\nServer: Received Packet.");

        // Process the received datagram.
        System.out.println("Client: Packet received:");
        System.out.println("From host: " + receivePacket.getAddress());
        System.out.println("Host port: " + receivePacket.getPort());
        int len = receivePacket.getLength();
        System.out.println("Length: " + len);
        System.out.println("Containing: ");

        //Get opcode
        Opcode opcode = Opcode.asEnum((int) data[1]);

        
        if(opcode == Opcode.DATA){
            System.out.println("Opcode: DATA");
            //create/validate data
            DataPacket dataPacket = new DataPacket(data);
            //write the data you just received
            tftpWriter.writeToFile(dataPacket.getData());
            //create an ack packet from corresponding block number
            ACKPacket ackPacket = new ACKPacket(dataPacket.getBlockNumber());
            //create and send ack packet
            sendPacket = new DatagramPacket(ackPacket.getByteArray(), ackPacket.getByteArray().length,
            receivePacket.getAddress(), receivePacket.getPort());
            sendReceiveSocket.send(sendPacket);
            if(dataPacket.getData().length < 512) firstTime = true;
            
        }
        else if(opcode == Opcode.ACK){
            System.out.println("Opcode: ACK");
            ACKPacket ackPacket = new ACKPacket(data);
            //send next block of file until there are no more blocks
            if(ackPacket.getBlockNumber() < tftpReader.getNumberOfBlocks()){
                DataPacket dataPacket = new DataPacket(ackPacket.getBlockNumber() + 1, tftpReader.getFileBlock(ackPacket.getBlockNumber() + 1));
                sendPacket = new DatagramPacket(dataPacket.getByteArray(), dataPacket.getByteArray().length,
                receivePacket.getAddress(), receivePacket.getPort());
                sendReceiveSocket.send(sendPacket);
            }
            else if(ackPacket.getBlockNumber() == tftpReader.getNumberOfBlocks()) firstTime = true;
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}

public static void main(String args[]) 
{
	Scanner in = new Scanner(System.in);
	System.out.println("Enter Directory Path");
	filePath = in.nextLine();
	System.out.println("You have entered the Directory Path");
	filePath += "\\";
	boolean done = false;
	
	while(!done){
	System.out.println("Enter mode (t for test and n for normal)");
	String m = in.nextLine();
	
	if(m.equals("t")){
		run = Mode.TEST;
		done = true;
	}
	else if(m.equals("n")){ 
		run = Mode.NORMAL;
		done = true;
		
	}
	}
	TFTPClient c = new TFTPClient();
	
	while(true)
	c.sendReceivePacket(in);
}

}