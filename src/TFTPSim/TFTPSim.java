package TFTPSim;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
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

	// We use this to cache the delayed packet's byte data and compare it to
	// the one we've received to ensure they are different.
	// This is important for the delay error sim mode
	private byte[] delayedPacketByteData = new byte[516];

	// Used for managing Delay on ACK Packets
	// We don't want to re-receive the same packet as before
	// so we compare with the last Data byte array received
	private byte[] lastDataReceivedFromServer = new byte[516];
	private byte[] lastDataReceivedFromClient = new byte[516];

	// Ports
	private int serverPort = 69;
	private int clientPort;
	private boolean startNewTransfer; // true if transfer is done and new
										// transfer is to begin
	private boolean endOfWRQ;
	private boolean duplicateData = false;
	private byte[] duplicateDataResponse = null;
	
	private InetAddress ClientIP;
	private static InetAddress LocalIP;

	private static Scanner sc;

	private static Opcode packetTypeForErrorSim;
	private static int packetNumberForErrorSim, delayLengthForErrorSim;

	/**
	 * @param args
	 * @throws UnknownHostException 
	 */
	public static void main(String[] args) throws UnknownHostException {
		LocalIP = InetAddress.getLocalHost();
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
			// Ask for packet type then the corresponding errors
			packetTypeForErrorSim = promptForSelectingTransferMode();

			if (packetTypeForErrorSim == Opcode.READ || packetTypeForErrorSim == Opcode.WRITE) {
				// get error type
				errorSimMode = promptForError4RequestPacketErrors();
			} else if (packetTypeForErrorSim == Opcode.DATA) {
				// get error type
				errorSimMode = promptForError4DataPacketErrors();
				// get block number
				packetNumberForErrorSim = promptForPacketNumber();
			} else if (packetTypeForErrorSim == Opcode.ACK) {
				// get error type
				errorSimMode = promptForError4AckPacketErrors();
				// get block number
				packetNumberForErrorSim = promptForPacketNumber();
			} else if (packetTypeForErrorSim == Opcode.ERROR) {
				// get error type
				errorSimMode = promptForError4ErrorPacketErrors();
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
				if (!(transferMode == 1 || transferMode == 2 || transferMode == 3 || transferMode == 4)) {
					System.out.println("Please choose from one of the above packet types");
					continue;
				}
				switch (transferMode) {
				case 1:
					packetTypeForErrorSim = Opcode.DATA;
					break;
				case 2:
					packetTypeForErrorSim = Opcode.ACK;
					break;
				case 3:
					packetTypeForErrorSim = Opcode.READ;
					break;
				case 4:
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
			if (errorSimMode == ErrorSimState.DELAY_PACKET) {
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

			// socket that is used to handle server side communication
			sendSocket = new DatagramSocket();
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

		// Keep listening until we know that the current packet received is not
		// the same as delayed packet from before
		// Important for delay error sim mode
		do {
			waitTillPacketReceived(receiveSocket, receivePacket);

			// Reset if we get a new request
			if (initialRequest(receivePacket)) {
				delayedPacketByteData = new byte[516];
				lastDataReceivedFromClient = new byte[516];
			}

		} while (Arrays.equals(delayedPacketByteData, getDataArray(data, receivePacket))
				|| Arrays.equals(lastDataReceivedFromClient, getDataArray(data, receivePacket)));

		// Update the clientPort since to where the receivePacket came from
		clientPort = receivePacket.getPort();
		ClientIP = receivePacket.getAddress();
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
			System.out.print("DUPLICATING PACKET: \n");
			sendPacket = new DatagramPacket(data, receivePacket.getLength(), LocalIP, serverPort);
			sendPacketThroughSocket(sendReceiveSocket, sendPacket);
			if (currentOpCode == Opcode.DATA) {
				duplicateData = true;
			}
		}

		// DELAY PACKET
		if (simMode.checkPacketToCreateError(ErrorSimState.DELAY_PACKET, receivePacket)) {
			// simulate the delay, create the packet, send the packet
			simulateDelayedPacket(sendReceiveSocket, receivePacket, serverPort);
			delayedPacketByteData = getDataArray(data, receivePacket);
		} else {
			// Send packet to the server
			// check if its the last data on a WRQ
			if (currentOpCode == Opcode.DATA && receivePacket.getLength() < 516) {
				endOfWRQ = true;
			}
			// check if we are starting a new transfer
			if (currentOpCode == Opcode.READ || currentOpCode == Opcode.WRITE) {
				startNewTransfer = false;
			}
			if (currentOpCode == Opcode.ERROR) {
				startNewTransfer = true;
			}

			// Generate ERROR 4 type packet
			if (simMode.isInvalidPacketType()) {
				if (isCurrentPacketValidToGenerateInvalidPacket(receivePacket)) {
					if (currentOpCode == Opcode.ERROR) {
						startNewTransfer = false;
					}
					System.out.println(
							"==> " + TFTPPacket.toString(Arrays.copyOfRange(data, 0, receivePacket.getLength())));
					data = PacketCorrupter.corruptPacket(
							Arrays.copyOfRange(receivePacket.getData(), 0, receivePacket.getLength()),
							simMode.getSimState());
					sendPacket = new DatagramPacket(data, data.length, LocalIP, serverPort);
					simMode.setSimState(ErrorSimState.NORMAL);
				} else {
					sendPacket = new DatagramPacket(data, receivePacket.getLength(), LocalIP,
							serverPort);
				}
			} else {
				sendPacket = new DatagramPacket(data, receivePacket.getLength(), LocalIP,
						serverPort);
			}

			sendPacketThroughSocket(sendReceiveSocket, sendPacket);
			lastDataReceivedFromClient = sendPacket.getData();

			if (duplicateDataResponse != null && currentOpCode == Opcode.ACK) {
				if (Arrays.equals(duplicateDataResponse, receivePacket.getData())) {
					duplicateDataResponse = null;
					if(!startNewTransfer){
						listenOnClient = true;
						return;
					}
				}
			}

			if (duplicateData && currentOpCode == Opcode.ACK) {
				duplicateDataResponse = receivePacket.getData();
				duplicateData = false;
				if(startNewTransfer){listenOnClient = true; return;}
			}

			// Generate ERROR 5
			if (simMode.getSimState() == ErrorSimState.INVALID_TID) {
				if (simMode.checkPacketToCreateError(ErrorSimState.INVALID_TID, receivePacket)) {
					// Implement Thread
					simulateInvalidTID(receivePacket, clientPort);
				}
			}

		}

		// Start handling server side communications
		if (startNewTransfer) {
			// RRQ has ended here
			listenOnClient = true;
			serverPort = 69;
			simMode.setSimState(errorSimMode);// changed back here
			duplicateData = false;
			duplicateDataResponse = null;
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

		// Keep listening until we know that the current packet received is not
		// the same as the delayed packets
		// Important for delay error sim mode
		do {
			waitTillPacketReceived(sendReceiveSocket, receivePacket);
		} while (Arrays.equals(delayedPacketByteData, getDataArray(data, receivePacket))
				|| Arrays.equals(lastDataReceivedFromServer, getDataArray(data, receivePacket)));

		// Update the server port since to where the receivePacket came from
		serverPort = receivePacket.getPort();
		Opcode currentOpCode = Opcode.asEnum((receivePacket.getData()[1]));

		// Check if we should be producing any error
		// LOST PACKET
		if (simMode.checkPacketToCreateError(ErrorSimState.LOST_PACKET, receivePacket)) {
			// Simulate a lost by not setting sendPacket
			System.out.print("LOST PACKET: ");
			printErrorMessage(simMode, receivePacket);

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
			System.out.print("DUPLICATING PACKET: \n");
			sendPacket = new DatagramPacket(data, receivePacket.getLength(), ClientIP, clientPort);
			sendPacketThroughSocket(sendSocket, sendPacket);
			if (currentOpCode == Opcode.DATA) {
				duplicateData = true;
			}
		}

		// DELAY PACKET
		if (simMode.checkPacketToCreateError(ErrorSimState.DELAY_PACKET, receivePacket)) {
			// simulate the delay, create the packet, send the packet
			simulateDelayedPacket(sendSocket, receivePacket, clientPort);
			delayedPacketByteData = getDataArray(data, receivePacket);
			System.out.println("delayedPacketByteData: " + Arrays.toString(delayedPacketByteData));
		} else {
			// check if its the last data block from client on RRQ
			if (currentOpCode == Opcode.DATA && receivePacket.getLength() < 516) {
				startNewTransfer = true;
			}
			// Check if the server had an error, if true, get ready for a new
			// transfer
			if (currentOpCode == Opcode.ERROR) {
				endOfWRQ = true;
			}

			// Generate ERROR 4 type packet
			if (simMode.isInvalidPacketType()) {
				if (isCurrentPacketValidToGenerateInvalidPacket(receivePacket)) {
					if (Opcode.asEnum((receivePacket.getData()[1])) == Opcode.ERROR) {
						endOfWRQ = false;
					}
					System.out.println(
							"==> " + TFTPPacket.toString(Arrays.copyOfRange(data, 0, receivePacket.getLength())));
					data = PacketCorrupter.corruptPacket(Arrays.copyOfRange(data, 0, receivePacket.getLength()),
							simMode.getSimState());
					sendPacket = new DatagramPacket(data, data.length, ClientIP, clientPort);
					simMode.setSimState(ErrorSimState.NORMAL);
				} else {
					sendPacket = new DatagramPacket(data, receivePacket.getLength(), ClientIP,
							clientPort);
				}
			} else {
				sendPacket = new DatagramPacket(data, receivePacket.getLength(), ClientIP,
						clientPort);
			}

			// send the packet
			sendPacketThroughSocket(sendSocket, sendPacket);
			lastDataReceivedFromServer = receivePacket.getData();

			if (duplicateDataResponse != null && currentOpCode == Opcode.ACK) {
				if (Arrays.equals(duplicateDataResponse, receivePacket.getData())) {
					duplicateDataResponse = null;
					if (!endOfWRQ) {
						listenOnClient = false;
						return;
					}
				}
			}

			if (duplicateData && currentOpCode == Opcode.ACK) {
				duplicateDataResponse = receivePacket.getData();
				duplicateData = false;
				if (endOfWRQ) {
					listenOnClient = false;
					return;
				}
			}

			// Generate ERROR 5
			if (simMode.getSimState() == ErrorSimState.INVALID_TID) {
				if (simMode.checkPacketToCreateError(ErrorSimState.INVALID_TID, receivePacket)) {
					// Implement Thread
					simulateInvalidTID(receivePacket, serverPort);
				}
			}

		}

		// Go back to handling client side communication
		listenOnClient = true;
		if (endOfWRQ) {
			// WRQ has ended here
			serverPort = 69;
			endOfWRQ = false;
			simMode.setSimState(errorSimMode);
			duplicateData = false;
			duplicateDataResponse = null;
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
		System.out
				.println("Byte Array: " + TFTPPacket.toString(Arrays.copyOfRange(data, 0, packet.getLength())) + "\n");
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

	// send packet with a different TID
	private void simulateInvalidTID(DatagramPacket packet, int port) {
		Thread tidThread = new Thread(new InvalidTIDThread(sendPacket, port));
		tidThread.start();
	}

	private void simulateDelayedPacket(DatagramSocket socket, DatagramPacket packet, int port) {
		System.out.println("DELAYING PACKET for " + simMode.getDelayLength() + " ms... \n");

		Thread delayThread = new Thread(new ErrorSimDelayThread(socket, packet, port, simMode.getDelayLength()));
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

	private static ErrorSimState promptForError4ErrorPacketErrors() {
		int userInput;
		while (true) {
			System.out.println("Select Invalid TFTP packet to generate: ");
			System.out.println("(1) Invalid Opcode");
			System.out.println("(2) Extra Data");
			System.out.println("(3) Invalid Error Code");
			System.out.println("(4) Missing Error Code");
			System.out.println("(5) Missing Error Message");
			System.out.println("(6) Missing Zero");

			if (sc.hasNextInt()) {
				userInput = sc.nextInt();
				if (userInput > 0 && userInput < 7) {
					break;
				}
			} else {
				sc.next();
				continue;
			}
		}
		switch (userInput) {
		case 1:
			return ErrorSimState.INVALID_OPCODE;
		case 2:
			return ErrorSimState.EXTRA_DATA_AT_END;
		case 3:
			return ErrorSimState.ERROR_INVALID_ERROR_CODE;
		case 4:
			return ErrorSimState.ERROR_MISSING_ERROR_CODE;
		case 5:
			return ErrorSimState.ERROR_MISSING_ERROR_MESSAGE;
		case 6:
			return ErrorSimState.ERROR_MISSING_ZERO;
		default:
			return ErrorSimState.NORMAL;
		}
	}

	private static ErrorSimState promptForError4AckPacketErrors() {
		int userInput;
		while (true) {
			System.out.println("Select Invalid TFTP packet to generate: ");
			System.out.println("(1) Invalid Opcode");
			System.out.println("(2) Extra Data");
			System.out.println("(3) Invalid Block Number");
			System.out.println("(4) Missing Block Number");

			if (sc.hasNextInt()) {
				userInput = sc.nextInt();
				if (userInput > 0 && userInput < 5) {
					break;
				}
			} else {
				sc.next();
				continue;
			}
		}
		switch (userInput) {
		case 1:
			return ErrorSimState.INVALID_OPCODE;
		case 2:
			return ErrorSimState.EXTRA_DATA_AT_END;
		case 3:
			return ErrorSimState.DATA_OR_ACK_INVALID_BLOCK_NUMBER;
		case 4:
			return ErrorSimState.DATA_OR_ACK_MISSING_BLOCK_NUMBER;
		default:
			return ErrorSimState.NORMAL;
		}
	}

	private static ErrorSimState promptForError4DataPacketErrors() {
		int userInput;
		while (true) {
			System.out.println("Select Invalid TFTP packet to generate: ");
			System.out.println("(1) Invalid Opcode");
			System.out.println("(2) Extra Data");
			System.out.println("(3) Invalid Block Number");
			System.out.println("(4) Missing Block Number");
			System.out.println("(5) Missing Data");

			if (sc.hasNextInt()) {
				userInput = sc.nextInt();
				if (userInput > 0 && userInput < 6) {
					break;
				}
			} else {
				sc.next();
				continue;
			}
		}
		switch (userInput) {
		case 1:
			return ErrorSimState.INVALID_OPCODE;
		case 2:
			return ErrorSimState.EXTRA_DATA_AT_END;
		case 3:
			return ErrorSimState.DATA_OR_ACK_INVALID_BLOCK_NUMBER;
		case 4:
			return ErrorSimState.DATA_OR_ACK_MISSING_BLOCK_NUMBER;
		case 5:
			return ErrorSimState.DATA_MISSING_DATA;
		default:
			return ErrorSimState.NORMAL;
		}
	}

	private static ErrorSimState promptForError4RequestPacketErrors() {
		int userInput;
		while (true) {
			System.out.println("Select Invalid TFTP packet to generate: ");
			System.out.println("(1) Invalid Opcode");
			System.out.println("(2) Extra Data");
			System.out.println("(3) Missing Filename");
			System.out.println("(4) Missing First Zero");
			System.out.println("(5) Missing Mode");
			System.out.println("(6) Corrupted Mode");
			System.out.println("(7) Missing 2nd Zero");

			if (sc.hasNextInt()) {
				userInput = sc.nextInt();
				if (userInput > 0 && userInput < 8) {
					break;
				}
			} else {
				sc.next();
				continue;
			}
		}
		switch (userInput) {
		case 1:
			return ErrorSimState.INVALID_OPCODE;
		case 2:
			return ErrorSimState.EXTRA_DATA_AT_END;
		case 3:
			return ErrorSimState.RQ_MISSING_FILENAME;
		case 4:
			return ErrorSimState.RQ_MISSING_FIRST_ZERO;
		case 5:
			return ErrorSimState.RQ_MISSING_MODE;
		case 6:
			return ErrorSimState.RQ_INVALID_MODE;
		case 7:
			return ErrorSimState.RQ_MISSING_SECOND_ZERO;
		default:
			return ErrorSimState.NORMAL;
		}
	}

	public boolean isCurrentPacketValidToGenerateInvalidPacket(DatagramPacket packet) {
		Opcode currentOpCode = Opcode.asEnum((packet.getData()[1]));
		if (packetTypeForErrorSim == currentOpCode) {
			if (currentOpCode == Opcode.DATA || currentOpCode == Opcode.ACK) {
				int currentBlockNumber = TFTPPacket.getBlockNumber(packet.getData());
				if (packetNumberForErrorSim == currentBlockNumber) {
					return true;
				}
			} else {
				return true;
			}
		}
		return false;
	}

	// Helper method to get the data packet array from a packet
	private byte[] getDataArray(byte[] inputData, DatagramPacket packet) {
		return Arrays.copyOfRange(inputData, 0, packet.getLength());
	}

	private boolean initialRequest(DatagramPacket packet) {
		Opcode currentOpcode = Opcode.asEnum((packet.getData()[1]));
		return (currentOpcode == Opcode.READ || currentOpcode == Opcode.WRITE) ? true : false;
	}
}