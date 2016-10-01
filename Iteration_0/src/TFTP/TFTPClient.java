package TFTP;

import static TFTPPackets.TFTPPacket.MAX_SIZE;

//TFTPClient.java
//This class is the client side for a very simple assignment based on TFTP on
//UDP/IP. The client uses one port and sends a read or write request and gets 
//the appropriate response from the server.  No actual file transfer takes place.   

import java.io.*;
import java.net.*;
import java.util.Arrays;
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
private static boolean verbose;
private static final int testModeSendPort = 23;

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
      sendReceiveSocket.setSoTimeout(10000);
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
	 System.out.println("Enter R for a read request, W for a write request and Q to quit");
	 String cmd = sc.nextLine();
	 
	 if(cmd.equals("W")){
      System.out.println("Client: creating WRQ packet.");

     
     // next we have a file name
	for(;;){
 		System.out.println("Enter file name");
 		filename = sc.nextLine();
 		if(new File (filePath + "\\" + filename).isFile()){
                	//is the path was provided finish
                	System.out.println("You have entered a valid file name\n");
                	break;
            }else{
                //if the directory does not exist, ask for an input again
                System.out.println("Invalid file name\nPlease Try Again.");
            }
	}
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
	 else if(cmd.equals("Q")) {
		 System.out.println("Client: Closing socket and exiting.");
		 	
		     // next we have a file name -- let's just pick one
		 	sc.close();
		 	sendReceiveSocket.close();
		 	System.exit(0);
	 }
   }
     try {
        sendPacket = new DatagramPacket(data, data.length,
                            InetAddress.getLocalHost(), sendPort);
     } catch (UnknownHostException e) {
        e.printStackTrace();
        System.exit(1);
     }
     if(verbose){
	     System.out.println("Client: sending packet.");
	     System.out.println("To host: " + sendPacket.getAddress());
	     System.out.println("Destination host port: " + sendPacket.getPort());
	     System.out.println("Length: " + data.length);
	     System.out.println("Containing: ");
	     for (j=0;j<data.length;j++) {
	         System.out.println("byte " + j + " " + data[j]);
	     }
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

        // Process the received datagram.
       
        if(verbose){
        	System.out.println("\nClient: Packet received:");
        	System.out.println("From host: " + receivePacket.getAddress());
        	System.out.println("Host port: " + receivePacket.getPort());
        	int len = receivePacket.getLength();
        	System.out.println("Length: " + len);
        	System.out.println("Containing: ");
        	System.out.println(new String(Arrays.copyOfRange(data,0,len)));
        	System.out.println("Byte Array: " + Arrays.toString(Arrays.copyOfRange(data,0,len))+"\n");
        }

        //Get opcode
        Opcode opcode = Opcode.asEnum((int) data[1]);

        
        if(opcode == Opcode.DATA){
        	if(verbose){
        		System.out.println("Opcode: DATA");
        	}
            //create/validate data
            DataPacket dataPacket = new DataPacket(data);
            //write the data you just received
            tftpWriter.writeToFile(dataPacket.getData());
            //create an ack packet from corresponding block number
            ACKPacket ackPacket = new ACKPacket(dataPacket.getBlockNumber());
            //create and send ack packet
            if(run == Mode.NORMAL)
            sendPacket = new DatagramPacket(ackPacket.getByteArray(), ackPacket.getByteArray().length,
            receivePacket.getAddress(), receivePacket.getPort());
            else
            	sendPacket = new DatagramPacket(ackPacket.getByteArray(), ackPacket.getByteArray().length,
                        receivePacket.getAddress(), testModeSendPort);
            
            if(verbose){
            	System.out.println("Client: Sending packet");
            	System.out.println("To host: " + sendPacket.getAddress());
            	System.out.println("Destination host port: " + sendPacket.getPort());
            	int length = sendPacket.getLength();
            	System.out.println("Length: " + length);
            	System.out.println("Byte Array: " + Arrays.toString(sendPacket.getData()));
            }
            
            sendReceiveSocket.send(sendPacket);
            if(dataPacket.getData().length < 512) {
            	System.out.println("file sent.");
            	firstTime = true;
            }
            
        }
        else if(opcode == Opcode.ACK){
        	if(verbose){
        		System.out.println("Opcode: ACK");
        	}
            ACKPacket ackPacket = new ACKPacket(data);
            //send next block of file until there are no more blocks
            if(ackPacket.getBlockNumber() <= tftpReader.getNumberOfBlocks()){
                DataPacket dataPacket = new DataPacket(ackPacket.getBlockNumber() + 1, tftpReader.getFileBlock(ackPacket.getBlockNumber() + 1));
                
                if(run == Mode.NORMAL)
                	sendPacket = new DatagramPacket(dataPacket.getByteArray(), dataPacket.getByteArray().length,
                			receivePacket.getAddress(), receivePacket.getPort());
                else
                	sendPacket = new DatagramPacket(dataPacket.getByteArray(), dataPacket.getByteArray().length,
                            receivePacket.getAddress(), testModeSendPort);
                if(verbose){
                	System.out.println("Client: Sending packet");
                	System.out.println("To host: " + sendPacket.getAddress());
                	System.out.println("Destination host port: " + sendPacket.getPort());
                	int length = sendPacket.getLength();
                	System.out.println("Length: " + length);
                	System.out.println("Byte Array: " + Arrays.toString(sendPacket.getData()));
                }
                
                try{
                	sendReceiveSocket.send(sendPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else if(ackPacket.getBlockNumber() == tftpReader.getNumberOfBlocks() + 1){
            	firstTime = true;
            	System.out.println("file received.");
            }
        }
    } catch (Exception e) {
        System.exit(0);
        e.printStackTrace();
    }
}

public static void main(String args[]) 
{
	Scanner in = new Scanner(System.in);
	System.out.println("Enter the Directory Path:");
    System.out.println("Type \"DEFAULT\" to use the relative directory or Enter the filepath of the directory");
    
    for(;;){
        String userInput = in.nextLine();
        if(userInput.equals("DEFAULT")){
            //if default print the dir and finish
            System.out.println("You are now in: " + System.getProperty("user.dir") + "\\Client");
            filePath = System.getProperty("user.dir") + "\\Client" + "\\";
            break;
        }else{
            if(new File (userInput).isDirectory()){
                //if the path was provided finish
                filePath = userInput + "\\";
                System.out.println("You have entered a valid Directory Path\n");
                break;
            }else{
                //if the directory does not exist, ask for an input again
                System.out.println("Invalid Directory\nPlease Try Again.");
            }
        }
    }
    
	String userInput;
    for(;;){
        //request user for verbose or quiet mode
        System.out.println("Verbose(Y/N)?");
        userInput = in.nextLine();
        if(userInput.equals("Y")){
            verbose = true;
            System.out.println("You have chosen Verbose mode");
            break;
        }else if(userInput.equals("N")){
            verbose = false;
            System.out.println("You have chosen Quiet mode");
            break;
        }//if input is invalid, ask again
    }
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
	
	while(true) {
		try {
			c.sendReceivePacket(in);
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}

}
