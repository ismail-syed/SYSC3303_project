package TFTP;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Scanner;

import TFTP.TFTPErrorSimMode.ErrorSimState;
import TFTPPackets.TFTPPacket;
import TFTPPackets.TFTPPacket.Opcode;

/**
 * The {@link TFTPSim} class represents a TFTP Error Simulator
 *
 * @author Team 3000000
 * @version 3.0
 * @since 1.0
 */
public class TFTPSim {

	// UDP datagram packets and sockets used to send / receive
	private DatagramPacket sendPacket, receivePacket;
	private DatagramSocket receiveSocket, sendSocket, sendReceiveSocket;
	
	// used to distinguish between read and write
	private int requestOpCode;
	private boolean firstTime = true;
	private int ackPacketCounter, dataPacketCounter;
	// helps determine when we should drop packets
	private boolean dropPacket = false;
	// default simulator mode
	private static ErrorSimState errorSimMode = ErrorSimState.NORMAL;
	private Opcode currentOpCode;
	private static TFTPErrorSimMode simMode; 
	
	private String sentPacketTo;
	
	// Ports
	private int serverPort = 69;
	private int clientPort; 
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		Opcode packetType = null;

		int packetNumber = 0, delayLength = 0, inp;
		boolean errorSimModeSelected = false;

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
					System.out.println("Simulator running in " + errorSimMode.toString().toLowerCase() + " mode...");
					break;
				} else if (inp == ErrorSimState.LOST_PACKET.ordinal()) {
					errorSimMode = ErrorSimState.LOST_PACKET;
				} else if (inp == ErrorSimState.DELAY_PACKET.ordinal()) {
					errorSimMode = ErrorSimState.DELAY_PACKET;
				} else if (inp == ErrorSimState.DUPLICATE_PACKET.ordinal()) {
					errorSimMode = ErrorSimState.DUPLICATE_PACKET;
				}
				// we have a valid input now
				errorSimModeSelected = true;
			} else {
				System.out.println("Please enter a valid simulator mode\n");
			}
		} while (!errorSimModeSelected);

		// Get the packet type (DATA or ACK)
		if (errorSimMode != ErrorSimState.NORMAL) {
			System.out.println(
					"Choose a packet type to manipulate:\n(0)DATA Packet\n(1)ACK Packet\n(2)RRQ Packet\n(3)WRQ Packet");
			while (true) {
				inp = sc.nextInt();
				if (!isValidPacketType(inp)) {
					System.out.println("Please choose from one of the above packet types");
					continue;
				}
				switch (inp) {
				case 0:
					packetType = Opcode.DATA;
					break;
				case 1:
					packetType = Opcode.ACK;
					break;
				case 2:
					packetType = Opcode.READ;
					break;
				case 3:
					packetType = Opcode.WRITE;
					break;
				default:
					break;
				}
				break;
			}

			// Get packetNumber
			if (packetType == Opcode.DATA || packetType == Opcode.ACK) {
				while (true) {
					System.out.println("Enter packet number:");
					if (sc.hasNextInt()) {
						packetNumber = sc.nextInt();
						break;
					} else {
						sc.next();
						continue;
					}
				}
			}
			// Get delay length
			if (errorSimMode == ErrorSimState.DELAY_PACKET || errorSimMode == ErrorSimState.DUPLICATE_PACKET) {
				while (true) {
					System.out.println("Enter delay length(ms):");
					if (sc.hasNextInt()) {
						delayLength = sc.nextInt();
						break;
					} else {
						sc.next();
						continue;
					}
				}
			}
		}
		simMode = new TFTPErrorSimMode(errorSimMode, packetType, packetNumber, delayLength);
		new TFTPSim(simMode).passOnTFTP();
		sc.close();
	}

	/**
	 * Constructor
	 */
	public TFTPSim(TFTPErrorSimMode mode) {
		try {
			// Construct a datagram socket and bind it to port 23
			receiveSocket = new DatagramSocket(23);
			
			// Construct a datagram socket and bind it to any available
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
	public void passOnTFTP() {

		byte[] data;

		int fromServerLen, fromClientLen, endOfWriteDataSize;
		ackPacketCounter = 0;
		dataPacketCounter = 0;

		// loop forever
		for (;;) {
			// Construct a DatagramPacket for receiving packets.
			// 516 bytes long, the length of the byte array.
			data = new byte[516];
			receivePacket = new DatagramPacket(data, data.length);

			/**
			 * Receive packet from client
			 */
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

			currentOpCode = Opcode.asEnum((receivePacket.getData()[1]));
			if (currentOpCode == Opcode.ACK) {
				ackPacketCounter++;
			}
			if (currentOpCode == Opcode.DATA) {
				dataPacketCounter++;
			}

			// process the received datagram
			System.out.println("Simulator: Packet received from client hosted on " + receivePacket.getAddress());
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
			 * Send packet to server
			 **/

			// Check if we currently satisfy the error sim mode and handle
			// requests appropriately
			// LOST PACKET
			if (checkPacketToCreateError(ErrorSimState.LOST_PACKET, simMode, currentOpCode, ackPacketCounter, dataPacketCounter)) {
				// Simulate a lost by not setting sendPacket
				dropPacket = true;
				System.out.print("LOST PACKET: ");
				printErrorMessage(simMode, currentOpCode, ackPacketCounter, dataPacketCounter);
				
				// Since we dropped the first RRQ or WRQ request, we need wait for the second
				if(currentOpCode == Opcode.READ || currentOpCode == Opcode.WRITE){
					waitTillPacketReceived(receiveSocket, receivePacket);
					sendPacket = new DatagramPacket(data, fromClientLen, receivePacket.getAddress(), serverPort);
					sendPacketThroughSocket(sendReceiveSocket, sendPacket);
				} else if(currentOpCode == Opcode.ACK || currentOpCode == Opcode.DATA) {
					System.out.println("=====> waiting at 1");
					waitTillPacketReceived(sendSocket, receivePacket);
					sendPacket = new DatagramPacket(data, fromClientLen, receivePacket.getAddress(), serverPort);
					sendPacketThroughSocket(sendReceiveSocket, sendPacket);
				}
			}
			// DELAY PACKET
			else if (checkPacketToCreateError(ErrorSimState.DELAY_PACKET, simMode, currentOpCode, ackPacketCounter, dataPacketCounter)) {
				System.out.println("DELAYING PACKET for " + simMode.getDelayLength() + " ms... ");
				sendPacket = new DatagramPacket(data, receivePacket.getLength(), receivePacket.getAddress(), serverPort);
				
				// Send the duplicate packet at the specified delay time
				Thread delayThread = new Thread(new ErrorSimDelayThread(this, sendReceiveSocket, sendPacket, simMode.getDelayLength()));
				delayThread.start();
				
			}
			// DUPLICATE PACKET
			else if (checkPacketToCreateError(ErrorSimState.DUPLICATE_PACKET, simMode, currentOpCode, ackPacketCounter, dataPacketCounter)) {
				// Send a packet now
				sendPacket = new DatagramPacket(data, receivePacket.getLength(), receivePacket.getAddress(), serverPort);
				sendPacketThroughSocket(sendReceiveSocket, sendPacket);
				
				// Send the duplicate packet at the specified delay time
				System.out.println("DUPLICATING PACKET after a delay of" + simMode.getDelayLength() + " ms... ");
				Thread delayThread = new Thread(new ErrorSimDelayThread(this, sendReceiveSocket, sendPacket, simMode.getDelayLength()));
				delayThread.start();
			}
			// NORMAL MODE
			else {
				sendPacket = new DatagramPacket(data, fromClientLen, receivePacket.getAddress(), serverPort);
				sendPacketThroughSocket(sendReceiveSocket, sendPacket);
			}

			// If we're dropping this packet, we don't want to print any info
			if (!dropPacket) {
				System.out.println("Simulator: Sending packet to server.");
				System.out.println("To host: " + sendPacket.getAddress());
				System.out.println("Destination host port: " + sendPacket.getPort());
				// fromClientLen = sendPacket.getLength();
				System.out.println("Length: " + fromClientLen);
				System.out.println("Containing: ");
				System.out.println("Byte Array: " + TFTPPacket.toString(Arrays.copyOfRange(data, 0, fromClientLen)) + "\n");
			}

			/**
			 * Receive packet from server
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

			// Update the acknowledge and data packet counters appropriately
			currentOpCode = Opcode.asEnum((receivePacket.getData()[1]));
			if (currentOpCode == Opcode.ACK) {
				ackPacketCounter++;
			}
			if (currentOpCode == Opcode.DATA) {
				dataPacketCounter++;
			}

			if (data[1] == (byte) 5)
				firstTime = true;

			// Process the received datagram
			System.out.println("Simulator:");
			System.out.println("=> Acknowledge packet number: " + ackPacketCounter);
			System.out.println("=> Data packet number: " + dataPacketCounter);
			System.out.println("From host: " + receivePacket.getAddress());
			serverPort = receivePacket.getPort();
			System.out.println("Host port: " + serverPort);
			fromServerLen = receivePacket.getLength();
			System.out.println("Length: " + fromServerLen);
			System.out.println("Containing: ");
			System.out.println("Byte Array: " + TFTPPacket.toString(Arrays.copyOfRange(data, 0, fromServerLen)) + "\n");

			/**
			 * Send packet to client
			 **/

			// Send the datagram packet to the client via a new socket.
			try {
				sendSocket = new DatagramSocket();
			} catch (SocketException se) {
				se.printStackTrace();
				System.exit(1);
			}
						
			// Check if we currently satisfy the error sim mode and handle requests appropriately
			// LOST PACKET
			if (checkPacketToCreateError(ErrorSimState.LOST_PACKET, simMode, currentOpCode, ackPacketCounter, dataPacketCounter)) {
				dropPacket = true;
				System.out.print("LOST PACKET: ");
				printErrorMessage(simMode, currentOpCode, ackPacketCounter, dataPacketCounter);
				
				if(currentOpCode == Opcode.ACK || currentOpCode == Opcode.DATA) {
					System.out.println("=====> waiting at 2");
					waitTillPacketReceived(sendReceiveSocket, receivePacket);
					try {
						sendSocket = new DatagramSocket();
					} catch (SocketException se) {
						se.printStackTrace();
						System.exit(1);
					}
					sendPacket = new DatagramPacket(data, receivePacket.getLength(), receivePacket.getAddress(), clientPort);
					sendPacketThroughSocket(sendSocket, sendPacket);
				}
			}
			// DELAY PACKET
			else if (checkPacketToCreateError(ErrorSimState.DELAY_PACKET, simMode, currentOpCode, ackPacketCounter, dataPacketCounter)) {
				System.out.println("DELAYING PACKET for " + simMode.getDelayLength() + " ms... ");
				sendPacket = new DatagramPacket(data, receivePacket.getLength(), receivePacket.getAddress(), clientPort);

				// Start a thread to handle the delay of this packet
				Thread delayThread = new Thread(new ErrorSimDelayThread(this, sendSocket, sendPacket, simMode.getDelayLength()));
				delayThread.start();
			}
			// DUPLICATE PACKET
			else if(checkPacketToCreateError(ErrorSimState.DUPLICATE_PACKET, simMode, currentOpCode, ackPacketCounter, dataPacketCounter)){
				// Send a packet now
				sendPacket = new DatagramPacket(data, receivePacket.getLength(), receivePacket.getAddress(), clientPort);
				sendPacketThroughSocket(sendReceiveSocket, sendPacket);
				
				// Send the duplicate packet at the specified delay time
				System.out.println("DUPLICATING PACKET after a delay of" + simMode.getDelayLength() + " ms... ");
				Thread delayThread = new Thread(new ErrorSimDelayThread(this, sendReceiveSocket, sendPacket, simMode.getDelayLength()));
				delayThread.start();
			}
			// NORMAL MODE
			else {
				sendPacket = new DatagramPacket(data, receivePacket.getLength(), receivePacket.getAddress(), clientPort);
				sendPacketThroughSocket(sendSocket, sendPacket);
			}

			// If were dropping this packet, we don't want to print any info
			if (!dropPacket) {
				System.out.println("Simulator: Sending packet to client.");
				System.out.println("To host: " + sendPacket.getAddress());
				System.out.println("Destination host port: " + sendPacket.getPort());
				// fromServerLen = sendPacket.getLength();
				System.out.println("Length: " + fromServerLen);
				System.out.println("Containing: ");
				System.out.println("Byte Array: " + TFTPPacket.toString(Arrays.copyOfRange(data, 0, fromServerLen)) + "\n");
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
				System.out.println("Byte Array: "+ TFTPPacket.toString(Arrays.copyOfRange(data, 0, receivePacket.getLength())) + "\n");

				sendPacket = new DatagramPacket(data, receivePacket.getLength(), receivePacket.getAddress(),serverPort);
				
				sendPacketThroughSocket(sendReceiveSocket, sendPacket);
				System.out.println("Byte Array: " + TFTPPacket.toString(Arrays.copyOfRange(data, 0, sendPacket.getLength())) + "\n");
				serverPort = 69;
				firstTime = true;
				ackPacketCounter = 0;
				dataPacketCounter = 0;
			}

			// check if its the last data block
			if (requestOpCode == 2 && endOfWriteDataSize < 516) {
				serverPort = 69;
				firstTime = true;
				ackPacketCounter = 0;
				dataPacketCounter = 0;
			}

			System.out.println("Simulator: packet sent to the client using port " + sendSocket.getLocalPort() + "\n");
		} // end of loop

	}
	
	
	
	/**
	 * Some helper methods
	 * 
	 * @author Ismail Syed
	 */
	
	/**
	 * Helper method to wait till a DatagramPacket is received through a DatagramSocket
	 * 
	 */
	protected void waitTillPacketReceived(DatagramSocket socket, DatagramPacket packet){
		System.out.print("Waiting for another request...\n");
		try {
			socket.receive(packet);
			System.out.println("--> packet received");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	
	/**
	 * Helper method to send a DatagramPacket through a DatagramSocket
	 * 
	 */
	protected void sendPacketThroughSocket(DatagramSocket socket, DatagramPacket packet) {
		if(packet.getPort() == serverPort){
			sentPacketTo = "server";
		}else if(packet.getPort() == clientPort){
			sentPacketTo = "client";
		}
		try {
			socket.send(packet);
			System.out.println("Simulator: Sent packet to " + sentPacketTo);
			System.out.println("To host: " + packet.getAddress());
			System.out.println("Destination host port: " + packet.getPort());
			// fromServerLen = sendPacket.getLength();
			System.out.println("Length: " + packet.getLength());
			System.out.println("Containing: ");
			System.out.println("Byte Array: " + TFTPPacket.toString(Arrays.copyOfRange(packet.getData(), 0, packet.getLength())) + "\n");

		} catch (IOException e) {
			System.out.println("IO exception while attempting to send packet");
		}
	}
	
	
	/**
	 * Helper method to check if the specified params meet the requirements for
	 * the properties saved in the errorSimMode
	 * 
	 * @param simStateToCheck
	 *            is the error sim state you would like to check
	 * @return True if the params specify the requirements of the
	 *         simStateToCheck error sim properties
	 */
	private boolean checkPacketToCreateError(ErrorSimState simStateToCheck, TFTPErrorSimMode errorSimMode,
			Opcode opcode, int ackPacketNum, int dataPacketNum) {
		if (errorSimMode.getSimState() == simStateToCheck && errorSimMode.getPacketType() == opcode) {
			if (opcode == Opcode.READ || opcode == Opcode.WRITE)
				return true;

			// get the right packet number to operate on
			int currentPacketNum = (opcode == Opcode.ACK) ? ackPacketNum : dataPacketNum;
			return currentPacketNum == errorSimMode.getPacketNumer() ? true : false;
		}
		return false;
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
	 * Helper method to validate error sim packet type
	 * 
	 * @param mode
	 * @return True if mode is in the packet mode
	 */
	private static boolean isValidPacketType(int mode) {
		return mode == 0 || mode == 1 || mode == 2 || mode == 3;
	}

	/**
	 * Helper method to print out details about the simulated error message.
	 */
	private static void printErrorMessage(TFTPErrorSimMode mode, Opcode opcode, int ackPacketCounter,
			int dataPacketCounter) {
		if (mode.getPacketType() == Opcode.ACK) {
			System.out.println("On ACK packet #" + ackPacketCounter + "\n");
		} else if (mode.getPacketType() == Opcode.DATA) {
			System.out.println("On DATA packet #" + dataPacketCounter + "\n");
		} else if (mode.getPacketType() == Opcode.READ) {
			System.out.println("On RRQ \n");
		} else if (mode.getPacketType() == Opcode.WRITE) {
			System.out.println("On WRQ \n");
		}
	}
}