package TFTPSim;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Scanner;

import TFTPPackets.TFTPPacket;
import TFTPPackets.TFTPPacket.Opcode;
import TFTPSim.TFTPErrorSimMode.ErrorSimState;

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
	private boolean startNewTransfer; // true if transfer is done and new
										// transfer is to begin
	private boolean endOfWRQ;

	private static Scanner sc;

	private static Opcode packetTypeForErrorSim;
	private static int packetNumberForErrorSim, delayLengthForErrorSim;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		sc = new Scanner(System.in);
		int rootMenuInput;
		packetTypeForErrorSim = null;
		packetNumberForErrorSim = 0;

		boolean errorSimModeSelected = false;

		// Get error sim mode console input
		do {
			System.out.println("Choose between: \n(1) Normal mode\n\tThe simulator simply acts a proxy"
					+ "\n(2) Network error(s)\n\tGenerate a network error during a TFTP transfer"
					+ "\n(3) Invalidate TFTP packet(s)\n\tChoose to corrupt a section of a TFTP packet");
			if (sc.hasNextInt())
				rootMenuInput = sc.nextInt();
			else {
				sc.next();
				continue;
			}

			// Set error sim mode
			if (rootMenuInput == 1 || rootMenuInput == 2 || rootMenuInput == 3) {
				if (rootMenuInput == 1) {
					System.out.println("Simulator running in " + errorSimMode.toString().toLowerCase() + " mode");
					break;
				} else if (rootMenuInput == 2) {
					displayNetworkErrorMenu();
				} else if (rootMenuInput == 3) {
					generateErrorPacketMenu();
				}
				// we have a valid input now
				errorSimModeSelected = true;
			} else {
				System.out.println("Please enter a valid simulator mode\n");
			}
		} while (!errorSimModeSelected);

		simMode = new TFTPErrorSimMode(errorSimMode, packetTypeForErrorSim, packetNumberForErrorSim,
				delayLengthForErrorSim);
		new TFTPSim(simMode).passOnTFTP();
		sc.close();
	}

	// This is the menu prompting the user to corrupt a packet
	private static void generateErrorPacketMenu() {
		int input;
		while (true) {
			System.out.println("(1) Generate Illegal TFTP operation. \n(2) Generate Unknown transfer ID");
			if (sc.hasNextInt()) {
				input = sc.nextInt();
				if (input == 1 || input == 2) {
					break;
				}
			} else {
				sc.next();
				continue;
			}
		}

		// Generate Illegal TFTP operation
		if (input == 1) {
			// Prompt the extended menu to select the Invalid TFTP packet
			int invalidPacketMode = promptForInvalidPacketMenuSelection();

			// Set the error
			setErrorSimInvalidPacketMode(invalidPacketMode);

			// Prompt the user with DATA or ACK packet
			if (TFTPErrorSimMode.isDataOrAckPromptDependent(errorSimMode)) {
				packetTypeForErrorSim = promptForAckOrData();
				packetNumberForErrorSim = promptForPacketNumber();
			}

			// Prompt the user to select RRQ, WRQ, DATA, ACK, or ERROR
			if (TFTPErrorSimMode.isDependentOnAllTransferModes(errorSimMode)) {
				packetTypeForErrorSim = promptForSelectingTransferMode();
			}

		}

		// Generate Unknown transfer ID
		else if (input == 2) {
			// Prompt the extended menu to select the Invalid TFTP packet
			errorSimMode = ErrorSimState.INVALID_TID;
			packetTypeForErrorSim = promptForSelectingTransferMode();
			if (packetTypeForErrorSim == Opcode.DATA || packetTypeForErrorSim == Opcode.ACK) {
				packetNumberForErrorSim = promptForPacketNumber();
			}
			
		}
	}

	// This is the menu prompting the user to select LOST, DELAY, DUPLICATE
	private static void displayNetworkErrorMenu() {
		int networkErrMode, transferMode;
		boolean isValid = false;
		do {
			System.out.println(
					"Choose a network error from the following:\n(1) Lose a packet\n(2) Delay a packet\n(3) Duplicate a packet");
			networkErrMode = sc.nextInt();
			if (networkErrMode == 1) {
				errorSimMode = ErrorSimState.LOST_PACKET;
				isValid = true;
			} else if (networkErrMode == 2) {
				isValid = true;
				errorSimMode = ErrorSimState.DELAY_PACKET;
			} else if (networkErrMode == 3) {
				errorSimMode = ErrorSimState.DUPLICATE_PACKET;
				isValid = true;
			} else
				System.out.println("Please select a valid network error");
		} while (!isValid);

		// Get the packet type (DATA or ACK)
		if (errorSimMode == ErrorSimState.LOST_PACKET || errorSimMode == ErrorSimState.DELAY_PACKET
				|| errorSimMode == ErrorSimState.DUPLICATE_PACKET) {
			System.out.println(
					"Choose a packet type to manipulate:\n(1)DATA Packet\n(2)ACK Packet\n(3)RRQ Packet\n(4)WRQ Packet");
			while (true) {
				transferMode = sc.nextInt();
				if(!(transferMode == 1 || transferMode == 2 || transferMode == 3 || transferMode == 4)) {
					System.out.println("Please choose from one of the above packet types");
					continue;
				}
				switch (transferMode) {
				case 0:
					packetTypeForErrorSim = Opcode.DATA;
					break;
				case 1:
					packetTypeForErrorSim = Opcode.ACK;
					break;
				case 2:
					packetTypeForErrorSim = Opcode.READ;
					break;
				case 3:
					packetTypeForErrorSim = Opcode.WRITE;
					break;
				default:
					break;
				}
				break;
			}

			// Get packetNumber
			if (packetTypeForErrorSim == Opcode.DATA || packetTypeForErrorSim == Opcode.ACK) {
				packetNumberForErrorSim = promptForPacketNumber();
			}

			// Get delay length
			if (errorSimMode == ErrorSimState.DELAY_PACKET || errorSimMode == ErrorSimState.DUPLICATE_PACKET) {
				delayLengthForErrorSim = promptForDelayLength();
			}
		}

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
		endOfWRQ = false;
		for (;;) {
			if (listenOnClient) {
				handleClientSideCommunication();
			} else {
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
		Opcode currentOpCode = Opcode.asEnum((receivePacket.getData()[1]));

		// Check if we should be producing any error
		// LOST PACKET
		if (simMode.checkPacketToCreateError(ErrorSimState.LOST_PACKET, receivePacket)) {
			// Simulate a lost by not setting sendPacket
			System.out.print("LOST PACKET: ");
			printErrorMessage(simMode, receivePacket);

			// Since we dropped the first RRQ/WRQ request, we need wait for the
			// second RRQ/WRA
			if (currentOpCode == Opcode.READ || currentOpCode == Opcode.WRITE) {
				// Since we just 'dropped' a packet, we need to listen on the
				// client again
				System.out.println("Dropped packet: Listening to Client again...");
				waitTillPacketReceived(receiveSocket, receivePacket);
			} else if (currentOpCode == Opcode.DATA) {
				data = new byte[516];
				receivePacket = new DatagramPacket(data, data.length);
				System.out.println("Dropped packet: Listening to Client again...");
				waitTillPacketReceived(receiveSocket, receivePacket);
			} else if (currentOpCode == Opcode.ACK) {
				System.out.println("Dropped packet: Listening to Server...");
				listenOnClient = false;
				// so that next time we receive the same ack, it wont
				// destroy it will be changed back later
				simMode.setSimState(ErrorSimState.NORMAL);
				return;
			}
		}

		// DUPLICATE PACKET
		if (simMode.checkPacketToCreateError(ErrorSimState.DUPLICATE_PACKET, receivePacket)) {
			// This will send a duplicate packet after the delayed time set in
			// simMode
			System.out.print("DUPLICATING PACKET: ");
			simulateDelayedPacket(sendReceiveSocket, receivePacket, serverPort);
		}

		// Generate ERROR 4 type packet
		if (simMode.isInvalidPacketType()) {
			if(simMode.isCurrentPacketValidToGenerateInvalidPacket(receivePacket)){
				System.out.println("==> " + TFTPPacket.toString(Arrays.copyOfRange(data, 0, receivePacket.getLength())));
				data = PacketCorrupter.corruptPacket(receivePacket.getData(), simMode.getSimState());
			}
		}

		// DELAY PACKET
		if (simMode.checkPacketToCreateError(ErrorSimState.DELAY_PACKET, receivePacket)) {
			simulateDelayedPacket(sendReceiveSocket, receivePacket, serverPort);
		} else {
			// Send packet to the server
			// check if its the last data on a WRQ
			if (Opcode.asEnum((receivePacket.getData()[1])) == Opcode.DATA && receivePacket.getLength() < 516) {
				endOfWRQ = true;
			}
			// check if we are starting a new transfer
			if (Opcode.asEnum((receivePacket.getData()[1])) == Opcode.READ
					|| Opcode.asEnum((receivePacket.getData()[1])) == Opcode.WRITE) {
				startNewTransfer = false;
			}
			sendPacket = new DatagramPacket(data, receivePacket.getLength(), receivePacket.getAddress(), serverPort);
			sendPacketThroughSocket(sendReceiveSocket, sendPacket);
			
			//Generate ERROR 5
			if(simMode.checkPacketToCreateError(ErrorSimState.INVALID_TID, receivePacket)){
				//Implement Thread
				simulateInvalidTID(sendPacket, serverPort);
			}
			
		}

		// Start handling server side communications
		if (startNewTransfer) {
			// RRQ has ended here
			listenOnClient = true;
			serverPort = 69;
			simMode.setSimState(errorSimMode);// changed back here
		} else {
			listenOnClient = false;
		}
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

		// Check if we should be producing any error
		// LOST PACKET
		if (simMode.checkPacketToCreateError(ErrorSimState.LOST_PACKET, receivePacket)) {
			// Simulate a lost by not setting sendPacket
			System.out.print("LOST PACKET: ");
			printErrorMessage(simMode, receivePacket);

			Opcode currentOpCode = Opcode.asEnum((receivePacket.getData()[1]));
			if (currentOpCode == Opcode.DATA) {
				data = new byte[516];
				receivePacket = new DatagramPacket(data, data.length);

				System.out.println("Dropped packet: Listening to Server again...");
				waitTillPacketReceived(sendReceiveSocket, receivePacket);
			}
			if (currentOpCode == Opcode.ACK) {
				System.out.println("Dropped packet: Listening to Client...");
				listenOnClient = true;
				// so that next time we receive the same ACK, it wont destroy it
				// will be changed back later
				simMode.setSimState(ErrorSimState.NORMAL);
				return;
			}
		}

		// DUPLICATE PACKET
		if (simMode.checkPacketToCreateError(ErrorSimState.DUPLICATE_PACKET, receivePacket)) {
			// This will send a duplicate packet after the delayed time set in
			// simMode
			System.out.print("DUPLICATING PACKET: ");
			simulateDelayedPacket(sendReceiveSocket, receivePacket, serverPort);
		}

		// Generate ERROR 4 type packet
		if (simMode.isInvalidPacketType()) {
			if(simMode.isCurrentPacketValidToGenerateInvalidPacket(receivePacket)){
				System.out.println("==> " + TFTPPacket.toString(Arrays.copyOfRange(data, 0, receivePacket.getLength())));
				data = PacketCorrupter.corruptPacket(receivePacket.getData(), simMode.getSimState());
			}
		}
		
		// DELAY PACKET
		if (simMode.checkPacketToCreateError(ErrorSimState.DELAY_PACKET, receivePacket)) {
			simulateDelayedPacket(sendSocket, sendPacket, clientPort);
		} else {
			// check if its the last data block from client on RRQ
			if (Opcode.asEnum((receivePacket.getData()[1])) == Opcode.DATA && receivePacket.getLength() < 516) {
				startNewTransfer = true;
			}
			// Check if the server had an error, if true, get ready for a new
			// transfer
			if (Opcode.asEnum((receivePacket.getData()[1])) == Opcode.ERROR) {
				endOfWRQ = true;
			}
			// create the packet
			sendPacket = new DatagramPacket(data, receivePacket.getLength(), receivePacket.getAddress(), clientPort);
			// send the packet
			sendPacketThroughSocket(sendSocket, sendPacket);
			
			//Generate ERROR 5
			if(simMode.checkPacketToCreateError(ErrorSimState.INVALID_TID, receivePacket)){
				//Implement Thread
				simulateInvalidTID(sendPacket, clientPort);
			}

		}

		// Go back to handling client side communication
		listenOnClient = true;
		if (endOfWRQ) {
			// WRQ has ended here
			serverPort = 69;
			endOfWRQ = false;
			simMode.setSimState(errorSimMode);
		}
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
	
	private void simulateInvalidTID(DatagramPacket packet, int port){
		// Send the duplicate packet at the specified delay time
		Thread tidThread = new Thread(new InvalidTIDThread(sendPacket));
		tidThread.start();
	}

	private void simulateDelayedPacket(DatagramSocket socket, DatagramPacket packet, int port) {
		System.out.println("DELAYING PACKET for " + simMode.getDelayLength() + " ms... \n");
		sendPacket = new DatagramPacket(data, packet.getLength(), packet.getAddress(), port);

		// Send the duplicate packet at the specified delay time
		Thread delayThread = new Thread(new ErrorSimDelayThread(this, socket, sendPacket, simMode.getDelayLength()));
		delayThread.start();
	}

	/**
	 * Helper method to print out details about the simulated error message.
	 */
	private static void printErrorMessage(TFTPErrorSimMode mode, DatagramPacket packet) {
		int currentBlockNumber = TFTPPacket.getBlockNumber(packet.getData());
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

	/**
	 * Helper method to prompt the user to select the Invalid TFTP packet to
	 * corrupt
	 */
	private static int promptForInvalidPacketMenuSelection() {
		int userInput;
		while (true) {
			System.out.println("Select Invalid TFTP packet to generate: ");
			System.out.println("(1)  Invalid Opcode");
			System.out.println("(2)  Extra Data");
			System.out.println("(3)  Missing Filename");
			System.out.println("(4)  Missing First Zero on Read or Write Request");
			System.out.println("(5)  Missing Mode");
			System.out.println("(6)  Corrupted Mode");
			System.out.println("(7)  Missing 2nd Zero on Read or Write Request");
			System.out.println("(8)  Invalid Block Number");
			System.out.println("(9)  Missing Block Number");
			System.out.println("(10) Missing Data");
			System.out.println("(11) Invalid Error Code");
			System.out.println("(12) Missing Error Code");
			System.out.println("(13) Missing Error Message");
			System.out.println("(14) Missing Zero on Error Packet");

			if (sc.hasNextInt()) {
				userInput = sc.nextInt();
				if (userInput > 0 && userInput < 15) {
					break;
				}
			} else {
				sc.next();
				continue;
			}
		}
		return userInput;
	}

	/**
	 * Helper method to prompt the user for the packet number
	 */
	private static int promptForPacketNumber() {
		int packetNumber;
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
		return packetNumber;
	}

	/**
	 * Helper method to prompt the user for the delay length
	 */
	private static int promptForDelayLength() {
		int length;
		while (true) {
			System.out.println("Enter delay length(ms):");
			if (sc.hasNextInt()) {
				length = sc.nextInt();
				break;
			} else {
				sc.next();
				continue;
			}
		}
		return length;
	}

	/**
	 * Helper method to prompt the user for to select ACK or DATA
	 * 
	 */
	private static Opcode promptForAckOrData() {
		Opcode transferMode;
		int userInput;
		while (true) {
			System.out.println("Generate invalid packet on:\n(1) DATA\n(2) ACK");
			if (sc.hasNextInt()) {
				userInput = sc.nextInt();
				if (userInput == 1) {
					transferMode = Opcode.DATA;
					break;
				} else if (userInput == 2) {
					transferMode = Opcode.ACK;
					break;
				}
			} else {
				sc.next();
				continue;
			}
		}
		return transferMode;
	}

	/**
	 * Helper method to prompt the user to select the transfer mode (1) RRQ (2)
	 * WRQ (3) DATA (4) ACK (5) ERROR
	 * 
	 */
	private static Opcode promptForSelectingTransferMode() {
		Opcode transferMode;
		int userInput;
		while (true) {
			System.out.println("Generate invalid packet on:\n(1) RRQ\n(2) WRQ\n(3) DATA\n(4) ACK\n(5) ERROR");
			if (sc.hasNextInt()) {
				userInput = sc.nextInt();
				if (userInput == 1) {
					transferMode = Opcode.READ;
					break;
				} else if (userInput == 2) {
					transferMode = Opcode.WRITE;
					break;
				} else if (userInput == 3) {
					transferMode = Opcode.DATA;
					break;
				} else if (userInput == 4) {
					transferMode = Opcode.ACK;
					break;
				} else if (userInput == 5) {
					transferMode = Opcode.ERROR;
					break;
				}
			} else {
				sc.next();
				continue;
			}
		}
		return transferMode;
	}

	private static void setErrorSimInvalidPacketMode(int menuSelectionInput) {
		switch (menuSelectionInput) {
		case 1:
			errorSimMode = ErrorSimState.INVALID_OPCODE;
			break;
		case 2:
			errorSimMode = ErrorSimState.EXTRA_DATA_AT_END;
			break;
		case 3:
			errorSimMode = ErrorSimState.RQ_MISSING_FILENAME;
			break;
		case 4:
			errorSimMode = ErrorSimState.RQ_MISSING_FIRST_ZERO;
			break;
		case 5:
			errorSimMode = ErrorSimState.RQ_MISSING_MODE;
			break;
		case 6:
			errorSimMode = ErrorSimState.RQ_INVALID_MODE;
			break;
		case 7:
			errorSimMode = ErrorSimState.RQ_MISSING_SECOND_ZERO;
			break;
		case 8:
			errorSimMode = ErrorSimState.DATA_OR_ACK_INVALID_BLOCK_NUMBER;
			break;
		case 9:
			errorSimMode = ErrorSimState.DATA_OR_ACK_MISSING_BLOCK_NUMBER;
			break;
		case 10:
			errorSimMode = ErrorSimState.DATA_MISSING_DATA;
			break;
		case 11:
			errorSimMode = ErrorSimState.ERROR_INVALID_ERROR_CODE;
			break;
		case 12:
			errorSimMode = ErrorSimState.ERROR_MISSING_ERROR_CODE;
			break;
		case 13:
			errorSimMode = ErrorSimState.ERROR_MISSING_ERROR_MESSAGE;
			break;
		case 14:
			errorSimMode = ErrorSimState.ERROR_MISSING_ZERO;
			break;
		default:
			errorSimMode = ErrorSimState.NORMAL;
			break;
		}
	}
}