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
			System.out.println("Simulator: Waiting for packet from client...\n");
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
	            try {
					tftpErrorPacket =  new ErrorPacket(ErrorPacket.ErrorCode.LOST_PACKET_RRQ, "Lost packet on WRQ going to the server.");
				} catch (IllegalArgumentException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				};
				sendPacket = new DatagramPacket(tftpErrorPacket.getByteArray(), tftpErrorPacket.getByteArray().length, receivePacket.getAddress(), serverPort);	
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
			try {
				sendReceiveSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
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
	            try {
					tftpErrorPacket =  new ErrorPacket(ErrorPacket.ErrorCode.LOST_PACKET_RRQ, "Lost packet on RRQ going to the client.");
				} catch (IllegalArgumentException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				};
				sendPacket = new DatagramPacket(tftpErrorPacket.getByteArray(), tftpErrorPacket.getByteArray().length, receivePacket.getAddress(), clientPort);	
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

			try {
				sendSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
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
	
	
	
	public static void main(String args[]) {
		TFTPSim s = new TFTPSim();
		
		// Not utilized yet. just a placeholder for now
		// This is where we will specify the error sim behavior which will be consumed by passOnTFTP()
		TFTPErrorSimMode simMode = new TFTPErrorSimMode(ErrorSimState.DELAY_PACKET, ErrorSimTransferMode.WRQ, 2, 0); 
		
		s.passOnTFTP(simMode);
	}
}
