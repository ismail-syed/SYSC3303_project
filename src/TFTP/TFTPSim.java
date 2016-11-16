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

	// default simulator mode
	private static ErrorSimState errorSimMode = ErrorSimState.NORMAL;
	private static TFTPErrorSimMode simMode;

	private boolean listenOnClient;
	private byte[] data;

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
					System.out.println("Simulator running in " + errorSimMode.toString().toLowerCase() + " mode...\n");
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

		listenOnClient = true;
		for (;;) {
			if (listenOnClient) {
				handleClientSideCommunication();
			}
			else {
				handleServerSideCommunication();
			}
		}
	}

	/**
	 * Some helper methods
	 * 
	 * @author Ismail Syed
	 */

	/**
	 * Handle all the Client side communications
	 */
	public void handleClientSideCommunication() {
		// Receive packet from client
		System.out.println("\nSimulator: Waiting for packet from client...\n");
		data = new byte[516];
		receivePacket = new DatagramPacket(data, data.length);
		waitTillPacketReceived(receiveSocket, receivePacket);

		// Update the clientPort since to where the receivePacket came from
		clientPort = receivePacket.getPort();
		
		// Check if we should be producing any error
		if (checkPacketToCreateError(ErrorSimState.LOST_PACKET, simMode, receivePacket)) {
			// Simulate a lost by not setting sendPacket
			System.out.print("LOST PACKET: ");
			printErrorMessage(simMode, receivePacket);
			
			// Since we dropped the first RRQ or WRQ request, we need wait for the second
			Opcode currentOpCode = Opcode.asEnum((receivePacket.getData()[1]));
			if (currentOpCode == Opcode.READ || currentOpCode == Opcode.WRITE) {
				// Since we just 'dropped' a packet, we need to listen on the client
				waitTillPacketReceived(receiveSocket, receivePacket);
			} 
			else if(currentOpCode == Opcode.ACK || currentOpCode == Opcode.DATA){
				data = new byte[516];
				receivePacket = new DatagramPacket(data, data.length);
				waitTillPacketReceived(receiveSocket, receivePacket);
			}
		} 
		
		// Send packet to the server
		sendPacket = new DatagramPacket(data, receivePacket.getLength(), receivePacket.getAddress(), serverPort);
		sendPacketThroughSocket(sendReceiveSocket, sendPacket);
	
		// Start handling server side communications
		listenOnClient = false;
	}

	/**
	 * Handle all the Server side communications
	 */
	public void handleServerSideCommunication() {
		// Receive packet from server
		System.out.println("Simulator: Waiting for packet from server...");
		data = new byte[516];
		receivePacket = new DatagramPacket(data, data.length);
		waitTillPacketReceived(sendReceiveSocket, receivePacket);

		// Update the server port since to where the receivePacket came from
		serverPort = receivePacket.getPort();

		// Send packet to client via a new socket
		try {
			sendSocket = new DatagramSocket();
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}

		// Send the packet
		sendPacket = new DatagramPacket(data, receivePacket.getLength(), receivePacket.getAddress(), clientPort);
		sendPacketThroughSocket(sendSocket, sendPacket);

		// Go back to handling client side communication
		listenOnClient = true;
	}

	/**
	 * A helper method to print out the details associated with receiving packet
	 * info
	 * 
	 * @param packet
	 * @param data
	 */
	private void printReceivePacketInfo(DatagramPacket packet, byte[] data) {
		System.out.println("Simulator: Packet received");
		System.out.println("From host: " + packet.getAddress());
		System.out.println("Length: " + packet.getLength());
		System.out.println("Containing: ");
		System.out
				.println("Byte Array: " + TFTPPacket.toString(Arrays.copyOfRange(data, 0, packet.getLength())) + "\n");
	}

	/**
	 * A helper method to print out the details associated with sending packet
	 * info
	 * 
	 * @param packet
	 * @param data
	 */
	private void printSendPacketInfo(DatagramPacket packet, byte[] data) {
		System.out.println("Simulator: Sent packet.");
		System.out.println("To host: " + packet.getAddress());
		System.out.println("Destination host port: " + packet.getPort());
		System.out.println("Length: " + packet.getLength());
		System.out.println("Containing Byte Array: "
				+ TFTPPacket.toString(Arrays.copyOfRange(data, 0, packet.getLength())) + "\n");
	}

	/**
	 * Helper method to wait till a DatagramPacket is received through a
	 * DatagramSocket
	 * 
	 */
	protected void waitTillPacketReceived(DatagramSocket socket, DatagramPacket packet) {
		try {
			socket.receive(packet);
			printReceivePacketInfo(receivePacket, data);
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
		try {
			socket.send(packet);
			printSendPacketInfo(packet, data);
		} catch (IOException e) {
			System.out.println("IO exception while attempting to send packet");
		}
	}

	/**
	 * Helper method to check if the simStateToCheck and packet meet the requirements for
	 * the properties saved in the errorSimMode
	 * 
	 * @param simStateToCheck
	 * @param errorSimMode
	 * @param packet
	 * @return
	 */
	private boolean checkPacketToCreateError(ErrorSimState simStateToCheck, TFTPErrorSimMode errorSimMode, DatagramPacket packet) {
		Opcode currentOpCode = Opcode.asEnum((packet.getData()[1]));
		
		if(Opcode.asEnum((packet.getData()[1])) == Opcode.DATA){
			System.out.println("DATA Block Number --> " + Integer.parseInt((packet.getData()[2] + "" + packet.getData()[3])) + "\n");
		} else if(Opcode.asEnum((packet.getData()[1])) == Opcode.ACK){
			System.out.println("ACK Block Number -->" + Integer.parseInt((packet.getData()[2] + "" + packet.getData()[3])) + "\n");
		}
		
		if (errorSimMode.getSimState() == simStateToCheck && errorSimMode.getPacketType() == currentOpCode) {
			if (currentOpCode == Opcode.READ || currentOpCode == Opcode.WRITE)
				return true;

			// Get the current block number by concating the two byte values and parsing that String into an Int 
			int currentBlockNumber = Integer.parseInt((packet.getData()[2] + "" + packet.getData()[3])); 
			if(errorSimMode.getPacketNumer() == currentBlockNumber){
				return currentBlockNumber == errorSimMode.getPacketNumer(); 
			}
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
	private static void printErrorMessage(TFTPErrorSimMode mode, DatagramPacket packet) {
		int currentBlockNumber = Integer.parseInt((packet.getData()[2] + "" + packet.getData()[3])); 
		if (mode.getPacketType() == Opcode.ACK) {
			System.out.println("On ACK packet #" + currentBlockNumber + "\n");
		} else if (mode.getPacketType() == Opcode.DATA) {
			System.out.println("On DATA packet #" + currentBlockNumber + "\n");
		} else if (mode.getPacketType() == Opcode.READ) {
			System.out.println("On RRQ \n");
		} else if (mode.getPacketType() == Opcode.WRITE) {
			System.out.println("On WRQ \n");
		}
	}
}