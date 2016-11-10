package TFTP;

/**
 * The {@link TFTP.TFTPSim} class represents a TFTP Error Simiulator
 * (based on Assignment 1 solution)
 *
 * @author Team 3000000
 * @author Shasthra Ranasinghe, Ismail Syed (100923110)
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

import TFTP.TFTPErrorSimMode.ErrorSimState;
import TFTP.TFTPErrorSimMode.ErrorSimTransferMode;
import TFTPPackets.ErrorPacket;
import TFTPPackets.TFTPPacket;
import TFTPPackets.ErrorPacket.ErrorCode;

public class TFTPSim {

	// UDP datagram packets and sockets used to send / receive
	private DatagramPacket sendPacket, receivePacket;
	private DatagramSocket receiveSocket, sendSocket, sendReceiveSocket;
	private int serverPort = 69;
	private int requestOpCode;// used to distinguish between read and write
	private boolean firstTime = true;
	private int currentPacketNumber;
	private TFTPPacket tftpErrorPacket;
	private boolean dropPacket = false; // helps determine when we should drop packets
	// console input modes
	private static final int LOST_PACKET = 1, DELAY_PACKET = 2, DUPLICATE_PACKET = 3,
							 TRANSFER_MODE_RRQ = 1, TRANSFER_MODE_WRQ = 2; 
	
	
	public TFTPSim() {
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

	public void passOnTFTP(TFTPErrorSimMode errorSimMode) {

		byte[] data;

		int clientPort, fromServerLen, fromClientLen, endOfWriteDataSize;
		currentPacketNumber = 0;
		
		// loop forever
		for(;;) { 
			// Construct a DatagramPacket for receiving packets. 
			// 516 bytes long, the length of the byte array.
			data = new byte[516];
			receivePacket = new DatagramPacket(data, data.length);

			
			/**
				RECEIVE PACKET FROM CLIENT
			**/
			System.out.println("\nSimulator: Waiting for packet from client...\n");
			// Block until a datagram packet is received from receiveSocket.
			try {
				if (firstTime) {
					receiveSocket.receive(receivePacket);
					firstTime = false;
				} else {
					sendSocket.receive(receivePacket);
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			if (data[1] == (byte) 1)
				requestOpCode = 1;
			if (data[1] == (byte) 2)
				requestOpCode = 2;

			// Process the received datagram.
			System.out.println("Simulator: Packet received from client.");
			System.out.println("From host: " + receivePacket.getAddress());
			clientPort = receivePacket.getPort();
			System.out.println("Host port: " + clientPort);
			// if its a data block save to size to check later
			if (data[1] == (byte) 3) {
				endOfWriteDataSize = receivePacket.getLength();
			} else {
				endOfWriteDataSize = 516;
			}
			fromClientLen = receivePacket.getLength();
			System.out.println("Length: " + fromClientLen);
			System.out.println("Containing: ");
			System.out.println("Byte Array: " + TFTPPacket.toString(Arrays.copyOfRange(data, 0, fromClientLen)) + "\n");

			// Form a String from the byte array, and print the string.
			String received = new String(data, 0, fromClientLen);
			System.out.println(received);

			
			/**
				SEND DATAPACKET TO THE SERVER
			 	Construct a DatagramPacket that is to be sent to the server on serverPort.
			**/
			//Check if we're in the LOST_PACKET mode and handle requests appropriately 
			if(checkToGenerateLostPacketWRQ(errorSimMode, currentPacketNumber)){
				// Simulate a lost by not setting sendPacket
				dropPacket = true;
				System.out.println("LOST PACKET: On WRQ for packet number #" + currentPacketNumber + "\n");
			}
			else if(checkToDelayPacketOnWRQ(errorSimMode, currentPacketNumber)) {
				System.out.println("\nDELAY: Delaying packing on WRQ to the server by " +  errorSimMode.getDelayLength() + " seconds \n");
				try {
					Thread.sleep(errorSimMode.getDelayLength());
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
			else {
				sendPacket = new DatagramPacket(data, fromClientLen, receivePacket.getAddress(), serverPort);
			}

			System.out.println("Simulator: Sending packet to server.");
			System.out.println("To host: " + sendPacket.getAddress());
			System.out.println("Destination host port: " + sendPacket.getPort());
			// fromClientLen = sendPacket.getLength();
			System.out.println("Length: " + fromClientLen);
			System.out.println("Containing: ");
			System.out.println("Byte Array: " + TFTPPacket.toString(Arrays.copyOfRange(data, 0, fromClientLen)) + "\n");

			// Send the datagram packet to the server via the send/receivesocket.
			if(!dropPacket){
				try {
					sendReceiveSocket.send(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
			} else {
				dropPacket = false;
			}
			

			/**
				RECEIVE PACKET FROM SERVER
			**/
			// Construct a DatagramPacket for receiving packets up
			// to 516 bytes long (the length of the byte array).
			data = new byte[516];
			receivePacket = new DatagramPacket(data, data.length);

			System.out.println("Simulator: Waiting for packet from server ...");
			try {
				// Block until a datagram is received via sendReceiveSocket.
				sendReceiveSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			// keep track of the packet numbers being sent back to the client from the server
			currentPacketNumber++; 
			
			if (data[1] == (byte) 5)
				firstTime = true;
			
			// Process the received datagram.
			System.out.println("Simulator: Packet "+ currentPacketNumber +" received from server.");
			System.out.println("From host: " + receivePacket.getAddress());
			serverPort = receivePacket.getPort();
			System.out.println("Host port: " + serverPort);
			fromServerLen = receivePacket.getLength();
			System.out.println("Length: " + fromServerLen);
			System.out.println("Containing: ");
			System.out.println("Byte Array: " + TFTPPacket.toString(Arrays.copyOfRange(data, 0, fromServerLen)) + "\n");

			
			/**
				SEND PACKET TO THE CLIENT
			 	Construct a DatagramPacket that is to be sent to the client on serverPort.
			**/
			//Check if were in the LOST_PACKET mode and handle requests appropriately 
			if(checkToGenerateLostPacketOnRRQ(errorSimMode, currentPacketNumber)){
				// enable flag to know if we should drop this packet
				dropPacket = true;
				System.out.println("LOST PACKET: On RRQ for packet number #" + currentPacketNumber + "\n");
			}
			else if(checkToDelayPacketOnRRQ(errorSimMode, currentPacketNumber)) {
				System.out.println("\nDELAY: Delaying packing on RRQ to the client by " +  errorSimMode.getDelayLength() + " seconds \n");
				try {
					Thread.sleep(errorSimMode.getDelayLength());
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
			else {
				sendPacket = new DatagramPacket(data, receivePacket.getLength(), receivePacket.getAddress(), clientPort);	
			}

			System.out.println("Simulator: Sending packet "+ currentPacketNumber +" to client.");
			System.out.println("To host: " + sendPacket.getAddress());
			System.out.println("Destination host port: " + sendPacket.getPort());
			// fromServerLen = sendPacket.getLength();
			System.out.println("Length: " + fromServerLen);
			System.out.println("Containing: ");
			System.out.println("Byte Array: " + TFTPPacket.toString(Arrays.copyOfRange(data, 0, fromServerLen)) + "\n");

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

			if(!dropPacket){
				try {
					sendSocket.send(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}	
			} else {
				dropPacket = false;
			}
			

			// check if its the last data block
			if (requestOpCode == 1 && fromServerLen < 516) {
				data = new byte[516];
				receivePacket = new DatagramPacket(data, data.length);

				System.out.println("Simulator: Waiting for packet.");
				// Block until a datagram packet is received from receiveSocket.
				try {
					sendSocket.receive(receivePacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				System.out.println("Byte Array: "
						+ TFTPPacket.toString(Arrays.copyOfRange(data, 0, receivePacket.getLength())) + "\n");

				sendPacket = new DatagramPacket(data, receivePacket.getLength(), receivePacket.getAddress(),
						serverPort);
				try {
					sendReceiveSocket.send(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				System.out.println("Byte Array: "
						+ TFTPPacket.toString(Arrays.copyOfRange(data, 0, sendPacket.getLength())) + "\n");
				serverPort = 69;
				firstTime = true;
				currentPacketNumber = 0;
			}

			// check if its the last data block
			if (requestOpCode == 2 && endOfWriteDataSize < 516) {
				serverPort = 69;
				firstTime = true;
				currentPacketNumber = 0;
			}

			System.out.println("Simulator: packet sent to the client using port " + sendSocket.getLocalPort());
			System.out.println();

		} // end of loop

	}
	
	
	/*
	 * LOST PACKET HELPER METHODS
	**/
	// Helper method to check if we are in the appropriate errorSimMode to return a lost packet response on a RRQ to the client
	public boolean checkToGenerateLostPacketOnRRQ(TFTPErrorSimMode errorSimMode, int currentPacketNum){
		return lostPacketCheck(errorSimMode, currentPacketNum) && errorSimMode.getTransferMode() == ErrorSimTransferMode.RRQ;
	}
	
	// Helper method to check if we are in the appropriate errorSimMode to return a lost packet response on a WRQ to the client
	public boolean checkToGenerateLostPacketWRQ(TFTPErrorSimMode errorSimMode, int currentPacketNum){
		return lostPacketCheck(errorSimMode, currentPacketNum) && errorSimMode.getTransferMode() == ErrorSimTransferMode.WRQ;
	}
	
	// Helper method to check if currentPacketNum matches the packet number specified by the errorSimMode properties
	public boolean lostPacketCheck(TFTPErrorSimMode errorSimMode, int currentPacketNum){ 
		return errorSimMode.getSimState() == ErrorSimState.LOST_PACKET && errorSimMode.getPacketNumer() == currentPacketNum;
	}
	
	
	/*
	 * DELAY PACKET HELPER METHODS
	**/
	// Helper method to check if we are in the appropriate errorSimMode to delay a packet response on a RRQ to the client
	public boolean checkToDelayPacketOnRRQ(TFTPErrorSimMode errorSimMode, int currentPacketNum){
		return delayPacketCheck(errorSimMode, currentPacketNum) && errorSimMode.getTransferMode() == ErrorSimTransferMode.RRQ;
	}
	
	// Helper method to check if we are in the appropriate errorSimMode to delay a packet response on a WRQ to the client
	public boolean checkToDelayPacketOnWRQ(TFTPErrorSimMode errorSimMode, int currentPacketNum){
		return delayPacketCheck(errorSimMode, currentPacketNum) && errorSimMode.getTransferMode() == ErrorSimTransferMode.WRQ;
	}

	// Helper method to check if currentPacketNum matches the packet number specified by the errorSimMode properties
	public boolean delayPacketCheck(TFTPErrorSimMode errorSimMode, int currentPacketNum){ 
		return errorSimMode.getSimState() == ErrorSimState.DELAY_PACKET && errorSimMode.getPacketNumer() == currentPacketNum;
	}
	
	// Helper method to validate error sim mode inputed through console
	private static boolean isValidErrorSimMode(int mode){
		return mode == 1 || mode == 2 || mode == 3;
	}
	
	// Helper method to validate error sim transfer mode inputed through console
	private static boolean isValidErrorSimTransferMode(int mode){
		return mode == 1 || mode == 2 ;
	}
	
	public static void main(String args[]) {
		TFTPSim s = new TFTPSim();
		
		Scanner scanner = new Scanner(System.in);
		ErrorSimState errorSimMode = null;
		ErrorSimTransferMode errorSimtransferMode = null;
		
		int packetNumber = 0, 
			delayLength = 0, 
			inputErrorSimMode; 
		
		boolean errorSimModeSelected = false, 
				errorSimTransferModeSeleted = false;
		
		// Get error sim mode console input
		// TODO Error checking on console input
		do {
			System.out.println("Select Error Simulator mode: \n(1)Lost a packet.\n(2)Delay a packet\n(3)Duplicate a packet.");
			inputErrorSimMode = scanner.nextInt();
			
			if(isValidErrorSimMode(inputErrorSimMode)){
				// Set error sim mode
				if(inputErrorSimMode == LOST_PACKET){
					errorSimMode = ErrorSimState.LOST_PACKET;
				}else if(inputErrorSimMode == DELAY_PACKET){
					errorSimMode = ErrorSimState.DELAY_PACKET;
				}else if(inputErrorSimMode == DUPLICATE_PACKET){
					errorSimMode = ErrorSimState.DUPLICATE_PACKET;
				}
				
				// we have a valid input now
				errorSimModeSelected = true; 
			}
			
		} while(!errorSimModeSelected);
		
		
		// Get error sim transfer mode
		// TODO Error checking on console input
		do {
			System.out.println("Select Error Simulator transfer mode: \n(1)RRQ.\n(2)WRQ");
			inputErrorSimMode = scanner.nextInt();
			
			if(isValidErrorSimTransferMode(inputErrorSimMode)){
				if(inputErrorSimMode == TRANSFER_MODE_RRQ){
					errorSimtransferMode = ErrorSimTransferMode.RRQ;
				} else if(inputErrorSimMode == TRANSFER_MODE_WRQ){
					errorSimtransferMode = ErrorSimTransferMode.WRQ;
				}
				errorSimTransferModeSeleted = true;
			}
			
		} while(!errorSimTransferModeSeleted);
		
		
		// Get packetNumber
		// TODO Error checking on console input
		System.out.println("Enter packet number: ");
		packetNumber = scanner.nextInt();			
		
			
		// Get delay length
		// TODO Error checking on console input
		if(errorSimMode == ErrorSimState.DELAY_PACKET){
			System.out.println("Enter delay length: ");
			delayLength = scanner.nextInt();			
		}
							
		
		// initialize simMode
		TFTPErrorSimMode simMode = new TFTPErrorSimMode(errorSimMode, errorSimtransferMode, packetNumber, delayLength); 
		
		
		s.passOnTFTP(simMode);
	}
}
