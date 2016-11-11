package TFTP;

/**
 * The {@link TFTPSim} class represents a TFTP Error Simulator
 *
 * @author Team 3000000
 * @version 3.0
 * @since 1.0
 */
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Scanner;

import TFTP.TFTPErrorSimMode.ErrorSimDuplicatePacketType;
import TFTP.TFTPErrorSimMode.ErrorSimState;
import TFTP.TFTPErrorSimMode.ErrorSimTransferMode;
import TFTPPackets.TFTPPacket;

public class TFTPSim {

	// UDP datagram packets and sockets used to send / receive
	private DatagramPacket sendPacket, receivePacket;
	private DatagramSocket receiveSocket, sendSocket, sendReceiveSocket;
	private int serverPort = 69;
	// used to distinguish between read and write
	private int requestOpCode;
	private boolean firstTime = true;
	private int currentPacketNumber;
	// helps determine when we should drop packets
	private boolean dropPacket = false;
	// default simulator mode
	private static ErrorSimState errorSimMode = ErrorSimState.NORMAL;

	/**
	 * Constructor
	 */
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

	/**
	 * 
	 * Default mode is {@link ErrorSimState#NORMAL}
	 * 
	 * @param errorSimMode
	 * @see {@link ErrorSimState} for the list of possible error modes
	 */
	public void passOnTFTP(TFTPErrorSimMode mode) {

		byte[] data;

		int clientPort, fromServerLen, fromClientLen, endOfWriteDataSize;
		currentPacketNumber = 0;

		PacketType packetType = null;

		// loop forever
		for (;;) {
			// Construct a DatagramPacket for receiving packets.
			// 516 bytes long, the length of the byte array.
			data = new byte[516];
			receivePacket = new DatagramPacket(data, data.length);

			// DUPLICATE PACKET
			// DO NOT REMOVE THIS
			// // if user wants to duplicate a packet
			// if (mode.getSimState() == ErrorSimState.DUPLICATE_PACKET) {
			// if (packetType == PacketType.ACK) {
			// } else if (packetType == PacketType.DATA) {
			// }
			// }

			/**
			 * RECEIVE PACKET FROM CLIENT
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
			 * SEND DATAPACKET TO THE SERVER Construct a DatagramPacket that is
			 * to be sent to the server on serverPort.
			 **/
			// Check if we're in the LOST_PACKET mode and handle requests
			// appropriately
			if (checkToGenerateLostPacketWRQ(mode, currentPacketNumber)) {
				// Simulate a lost by not setting sendPacket
				dropPacket = true;
				System.out.println("LOST PACKET: On WRQ for packet number #" + currentPacketNumber + "\n");
			} else if (checkToDelayPacketOnWRQ(mode, currentPacketNumber)) {
				System.out.println(
						"\nDELAY: Delaying packing on WRQ to the server by " + mode.getDelayLength() + " ms \n");
				try {
					Thread.sleep(mode.getDelayLength());
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			} else {
				sendPacket = new DatagramPacket(data, fromClientLen, receivePacket.getAddress(), serverPort);
			}

			// If were dropping this packet, we don't want to print any info
			if(!dropPacket){
				System.out.println("Simulator: Sending packet to server.");
				System.out.println("To host: " + sendPacket.getAddress());
				System.out.println("Destination host port: " + sendPacket.getPort());
				// fromClientLen = sendPacket.getLength();
				System.out.println("Length: " + fromClientLen);
				System.out.println("Containing: ");
				System.out.println("Byte Array: " + TFTPPacket.toString(Arrays.copyOfRange(data, 0, fromClientLen)) + "\n");
			}
			
			// Send the datagram packet to the server via the send/receivesocket.
			if (!dropPacket) {
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
			 * RECEIVE PACKET FROM SERVER
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

			// keep track of the packet numbers being sent back to the client
			// from the server
			currentPacketNumber++;

			if (data[1] == (byte) 5)
				firstTime = true;

			// Process the received datagram.
			System.out.println("Simulator: Packet " + currentPacketNumber + " received from server.");
			System.out.println("From host: " + receivePacket.getAddress());
			serverPort = receivePacket.getPort();
			System.out.println("Host port: " + serverPort);
			fromServerLen = receivePacket.getLength();
			System.out.println("Length: " + fromServerLen);
			System.out.println("Containing: ");
			System.out.println("Byte Array: " + TFTPPacket.toString(Arrays.copyOfRange(data, 0, fromServerLen)) + "\n");

			/**
			 * SEND PACKET TO THE CLIENT Construct a DatagramPacket that is to
			 * be sent to the client on serverPort.
			 **/
			// Check if in the LOST_PACKET mode and handle requests
			// appropriately
			if (checkToGenerateLostPacketOnRRQ(mode, currentPacketNumber)) {
				// enable flag to know if we should drop this packet
				dropPacket = true;
				System.out.println("LOST PACKET: On RRQ for packet number #" + currentPacketNumber + "\n");
			} else if (checkToDelayPacketOnRRQ(mode, currentPacketNumber)) {
				System.out.println(
						"\nDELAY: Delaying packing on RRQ to the client by " + mode.getDelayLength() + " ms \n");
				try {
					Thread.sleep(mode.getDelayLength());
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			} else {
				sendPacket = new DatagramPacket(data, receivePacket.getLength(), receivePacket.getAddress(),
						clientPort);
			}

			// If were dropping this packet, we don't want to print any info
			if(!dropPacket){
				System.out.println("Simulator: Sending packet " + currentPacketNumber + " to client.");
				System.out.println("To host: " + sendPacket.getAddress());
				System.out.println("Destination host port: " + sendPacket.getPort());
				// fromServerLen = sendPacket.getLength();
				System.out.println("Length: " + fromServerLen);
				System.out.println("Containing: ");
				System.out.println("Byte Array: " + TFTPPacket.toString(Arrays.copyOfRange(data, 0, fromServerLen)) + "\n");
			}
			
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

			if (!dropPacket) {
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

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TFTPSim s = new TFTPSim();

		Scanner sc = new Scanner(System.in);
		ErrorSimTransferMode errorSimTransferMode = null;
		ErrorSimDuplicatePacketType errorSimDuplicatePacketType = null;

		int packetNumber = 0, delayLength = 0, packetType = 0, inp, transferMode;

		boolean errorSimModeSelected = false, errorSimTransferModeSelected = false;

		// Get error sim mode console input
		do {
			System.out.println("Select mode: \n(0)Normal mode\n(1)Packet loss\n(2)Packet delay\n(3)Duplicate packet");
			if (sc.hasNextInt())
				inp = sc.nextInt();
	        else {
	            sc.next();
	            continue;
	        }
			
			// Set error sim mode
			if (isValidErrorSimMode(inp)) {
				if (inp == ErrorSimState.NORMAL.ordinal()) {
					errorSimMode = ErrorSimState.NORMAL;
				} else if (inp == ErrorSimState.LOST_PACKET.ordinal()) {
					errorSimMode = ErrorSimState.LOST_PACKET;
				} else if (inp == ErrorSimState.DELAY_PACKET.ordinal()) {
					errorSimMode = ErrorSimState.DELAY_PACKET;
				} else if (inp == ErrorSimState.DUPLICATE_PACKET.ordinal()) {
					errorSimMode = ErrorSimState.DUPLICATE_PACKET;
					while(true){
						System.out.println("What kind of packet would you like to duplicate?\n(0)Data Packet\n(1)ACK Packet\n");
						if (sc.hasNextInt()){
							inp = sc.nextInt();
							if(!isValidPacketType(inp)){
								continue;
							}
							packetType = inp;
							break;
						}
				        else {
				            sc.next();
				            continue;
				        }	
					}
					
				}
				// we have a valid input now
				errorSimModeSelected = true;
			} else {
				System.out.println("Please enter a valid simulator mode\n");
			}
		} while (!errorSimModeSelected);
		
		if(!(errorSimMode == ErrorSimState.NORMAL)){
			// Get error sim transfer mode
			do {
				System.out.println("Select type of transfer: \n(0)RRQ\n(1)WRQ");
				if (sc.hasNextInt()){
					transferMode = sc.nextInt();
				}
		        else {
		            sc.next();
		            continue;
		        }

				if (isValidErrorSimTransferMode(transferMode)) {
					if (transferMode == ErrorSimTransferMode.RRQ.ordinal()) {
						errorSimTransferMode = ErrorSimTransferMode.RRQ;
					} else if (transferMode == ErrorSimTransferMode.WRQ.ordinal()) {
						errorSimTransferMode = ErrorSimTransferMode.WRQ;
					}
					errorSimTransferModeSelected = true;
				} else {
					System.out.println("Please enter a valid transfer mode\n");
				}

			} while (!errorSimTransferModeSelected);

			// Get packetNumber
			while(true){
				System.out.println("Enter packet number: ");
				if (sc.hasNextInt()){
					packetNumber = sc.nextInt();
					break;
				}
		        else {
		            sc.next();
		            continue;
		        }
		
			}
		
			// Get delay length
			if (errorSimMode == ErrorSimState.DELAY_PACKET || errorSimMode == ErrorSimState.DUPLICATE_PACKET) {
				while(true){
					System.out.println("Enter delay length: ");
					if (sc.hasNextInt()){
						delayLength = sc.nextInt();	
						break;
					}
			        else {
			            sc.next();
			            continue;
			        }
				}
			}
		}

		s.passOnTFTP(new TFTPErrorSimMode(errorSimMode, errorSimTransferMode, packetNumber, delayLength, errorSimDuplicatePacketType));
		sc.close();
	}

	private enum PacketType {
		DATA, ACK, REQUEST;
	}

	/**
	 * Some helper methods
	 */

	/**
	 * 
	 * Helper method to check if we are in the appropriate errorSimMode to
	 * return a lost packet response on a RRQ to the client
	 * 
	 * @param errorSimMode
	 * @param currentPacketNum
	 * @return
	 */
	public boolean checkToGenerateLostPacketOnRRQ(TFTPErrorSimMode errorSimMode, int currentPacketNum) {
		return lostPacketCheck(errorSimMode, currentPacketNum)
				&& errorSimMode.getTransferMode() == ErrorSimTransferMode.RRQ;
	}

	/**
	 * 
	 * Helper method to check if we are in the appropriate errorSimMode to
	 * return a lost packet response on a WRQ to the client
	 * 
	 * @param errorSimMode
	 * @param currentPacketNum
	 * @return
	 */
	public boolean checkToGenerateLostPacketWRQ(TFTPErrorSimMode errorSimMode, int currentPacketNum) {
		return lostPacketCheck(errorSimMode, currentPacketNum)
				&& errorSimMode.getTransferMode() == ErrorSimTransferMode.WRQ;
	}

	/**
	 * 
	 * Helper method to check if currentPacketNum matches the packet number
	 * specified by the errorSimMode properties
	 * 
	 * @param errorSimMode
	 * @param currentPacketNum
	 * @return
	 */
	public boolean lostPacketCheck(TFTPErrorSimMode errorSimMode, int currentPacketNum) {
		return errorSimMode.getSimState() == ErrorSimState.LOST_PACKET
				&& errorSimMode.getPacketNumer() == currentPacketNum;
	}

	/**
	 * 
	 * Helper method to check if we are in the appropriate errorSimMode to delay
	 * a packet response on a RRQ to the client
	 * 
	 * @param errorSimMode
	 * @param currentPacketNum
	 * @return
	 */
	public boolean checkToDelayPacketOnRRQ(TFTPErrorSimMode errorSimMode, int currentPacketNum) {
		return delayPacketCheck(errorSimMode, currentPacketNum)
				&& errorSimMode.getTransferMode() == ErrorSimTransferMode.RRQ;
	}

	/**
	 * 
	 * Helper method to check if we are in the appropriate errorSimMode to delay
	 * a packet response on a WRQ to the client
	 * 
	 * @param errorSimMode
	 * @param currentPacketNum
	 * @return
	 */
	public boolean checkToDelayPacketOnWRQ(TFTPErrorSimMode errorSimMode, int currentPacketNum) {
		return delayPacketCheck(errorSimMode, currentPacketNum)
				&& errorSimMode.getTransferMode() == ErrorSimTransferMode.WRQ;
	}

	/**
	 * 
	 * Helper method to check if currentPacketNum matches the packet number
	 * specified by the errorSimMode properties
	 * 
	 * @param errorSimMode
	 * @param currentPacketNum
	 * @return
	 */
	public boolean delayPacketCheck(TFTPErrorSimMode errorSimMode, int currentPacketNum) {
		return errorSimMode.getSimState() == ErrorSimState.DELAY_PACKET
				&& errorSimMode.getPacketNumer() == currentPacketNum;
	}

	/**
	 * Helper method to validate error sim mode inputted through console
	 * 
	 * @param mode
	 * @return True if mode is in the error mode set
	 */
	private static boolean isValidErrorSimMode(int mode) {
		return mode == 0 || mode == 1 || mode == 2 || mode == 3;
	}

	/**
	 * 
	 * Helper method to validate error sim transfer mode inputted through
	 * console
	 * 
	 * @param mode
	 * @return True if mode is in the transfer mode set
	 */
	private static boolean isValidErrorSimTransferMode(int mode) {
		return mode == 0 || mode == 1;
	}
	
	/**
	 * 
	 * Helper method to validate error sim  packet type for duplicate packet
	 * error sim mode
	 * 
	 * @param mode
	 * @return True if mode is in the packet mode
	 */
	private static boolean isValidPacketType(int mode) {
		return mode == 0 || mode == 1;
	}
}