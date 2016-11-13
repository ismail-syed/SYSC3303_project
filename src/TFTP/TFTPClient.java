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

import Exceptions.InvalidBlockNumberException;
import Exceptions.PacketOverflowException;
import FileIO.TFTPReader;
import FileIO.TFTPWriter;
import TFTPPackets.*;
import TFTPPackets.ErrorPacket.ErrorCode;
import TFTPPackets.TFTPPacket.Opcode;

/*
 * @author: Mohamed Zalat & Kunall Banerjee
 * TFTPClient
 */
public class TFTPClient {

	private static final int SOCKET_TIMEOUT_MS = 1000;
	private DatagramPacket sendPacket, receivePacket;
	private DatagramSocket sendReceiveSocket;
	private int counter;
	private int previousBlockNumber;
	private TFTPPacket lastRequest;
	private DataPacket lastDataPacketSent;
	private static String filePath;
	private TFTPReader tftpReader;
	private TFTPWriter tftpWriter;
	private static Mode run;
	private static boolean firstTime;
	private static boolean verbose;

	// we can run in normal (send directly to server) or test
	// (send to simulator) mode
	public static enum Mode {
		NORMAL, TEST
	};

	/**
	 * This is the constructor for the client It created the required sockets
	 * and sets a timeout
	 */
	public TFTPClient() {
		lastDataPacketSent = null;
		firstTime = true;
		lastRequest = null;
		try {
			// Construct a datagram socket and bind it to any available
			// port on the local host machine. This socket will be used to
			// send and receive UDP Datagram packets.
			sendReceiveSocket = new DatagramSocket();
			sendReceiveSocket.setSoTimeout(SOCKET_TIMEOUT_MS);
		} catch (SocketException se) { // Can't create the socket.
			se.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * @param sc
	 * @throws PacketOverflowException
	 * @throws IOException
	 * 
	 */
	public void sendRequest(Scanner sc) throws PacketOverflowException, IOException {
		String filename; // filename and mode as Strings
		int sendPort;
		TFTPPacket tftpPacket = new TFTPPacket();
		counter = 0;
		// In the assignment, students are told to send to 23, so just:
		// sendPort = 23;
		// is needed.
		// However, in the project, the following will be useful, except
		// that test vs. normal will be entered by the user.

		if (run == Mode.NORMAL) {
			sendPort = 69;
		} else {
			sendPort = 23;
		}
		boolean done = false;
		while (!done) {
			System.out.println("Choose Read or Write request(R/W) or enter \"QUIT\" to close the client");
			String cmd = sc.nextLine();
			// write request
			if (cmd.equals("W")) {
				System.out.println("Client: creating WRQ packet.");

				// next we have a file name
				for (;;) {
					System.out.println("Enter file name");
					filename = sc.nextLine();
					if (new File(filePath + "\\" + filename).isFile()) {
						// is the path was provided finish
						System.out.println("You have entered a valid file name");
						break;
					} else {
						// if the directory does not exist, ask for an input
						// again
						System.out.println("Error Message: File Not Found\nPlease Try Again\n");
					}
				}
				tftpPacket = new WRQPacket(filename, RRQWRQPacketCommon.Mode.NETASCII);
				previousBlockNumber = 0;
				done = true;
				try {
					tftpReader = new TFTPReader(new File(filePath + filename).getPath());
				} catch (IOException | InvalidBlockNumberException e) {
					e.printStackTrace();
				}
			} else if (cmd.equals("R")) {// read request
				System.out.println("Client: creating RRQ packet.");

				// next we have a file name
				for (;;) {
					System.out.println("Enter file name");
					filename = sc.nextLine();
					if (!new File(filePath + "\\" + filename).isFile()) {
						// is the path was provided finish
						System.out.println("You have entered a valid file name");
						break;
					} else {
						// if the directory does not exist, ask for an input
						// again
						System.out.println("\nError Message: File Already Exists\nPlease Try Again\n");
					}
				}
				tftpPacket = new RRQPacket(filename, RRQWRQPacketCommon.Mode.NETASCII);
				previousBlockNumber = 0;
				done = true;
				try {
					tftpWriter = new TFTPWriter(new File(filePath + filename).getPath(), false);
				} catch (IOException e) {
					System.out.println("File doesnt Exist on Client");
					firstTime = true;
					tftpWriter.closeHandle();
				}
			} else if (cmd.equals("cd")) {// change directory
				System.out.println("Enter the Directory Path:");
				System.out.println(
						"Type \"DEFAULT\" to use the relative directory or Enter the filepath of the directory");

				for (;;) {
					String userInput = sc.nextLine();
					if (userInput.equals("DEFAULT")) {
						// if default print the dir and finish
						System.out.println("You are now in: " + System.getProperty("user.dir") + "\\Client");
						filePath = System.getProperty("user.dir") + "\\Client" + "\\";
						break;
					} else {
						if (new File(userInput).isDirectory()) {
							// if the path was provided finish
							filePath = userInput + "\\";
							System.out.println("You have entered a valid Directory Path\n");
							break;
						} else {
							// if the directory does not exist, ask for an input
							// again
							System.out.println("Invalid Directory\nPlease Try Again.");
						}
					}
				}
			} else if (cmd.equals("QUIT")) {// quit
				System.out.println("Client: Closing socket and exiting.");

				// close scanner, socket and exit
				sc.close();
				sendReceiveSocket.close();
				System.exit(0);
			}
		}
		try {// Send the datagram packet to the server via the send/receive
				// socket.
			lastRequest = tftpPacket;
			sendPacketToServer(tftpPacket, InetAddress.getLocalHost(), sendPort);
			System.out.println("Client: Packet sent.");

		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This function deals with the actual file transfer Data, ACK and error
	 * packets go through this function
	 */
	private void sendReceivePacket() {
		byte dataBuffer[] = new byte[MAX_SIZE];
		byte[] data = null;
		TFTPPacket tftpPacket = new TFTPPacket();

		receivePacket = new DatagramPacket(dataBuffer, dataBuffer.length);
		try {
			// Receive packet
			sendReceiveSocket.receive(receivePacket);
			lastRequest = null;
			counter = 0;
			// Create byte array of proper size
			data = new byte[receivePacket.getLength()];
			System.arraycopy(dataBuffer, 0, data, 0, data.length);

			// Process the received datagram.

			if (verbose) {
				System.out.println("\nClient: Packet received:");
				System.out.println("From host: " + receivePacket.getAddress());
				System.out.println("Host port: " + receivePacket.getPort());
				int len = receivePacket.getLength();
				System.out.println("Length: " + len);
				System.out.println("Containing: ");
				System.out.println(new String(Arrays.copyOfRange(data, 0, len)));
				System.out.println("Byte Array: " + TFTPPacket.toString(Arrays.copyOfRange(data, 0, len)) + "\n");
			}

			// Get opcode
			Opcode opcode = Opcode.asEnum((int) data[1]);

			if (opcode == Opcode.DATA) {
				if (verbose) {
					System.out.println("Opcode: DATA");
				}
				// create/validate data
				DataPacket dataPacket = new DataPacket(data);
				if (dataPacket.getBlockNumber() <= previousBlockNumber) {
					tftpPacket = new ACKPacket(dataPacket.getBlockNumber());
				} else if (dataPacket.getBlockNumber() != previousBlockNumber + 1) {
					throw new InvalidBlockNumberException("Data is out of order");
				} else if (new File(filePath).getUsableSpace() >= dataPacket.getData().length) { // check
																									// if
																									// there
																									// is
																									// enough
																									// space
																									// available
					// write the data you just received
					try {
						tftpWriter.writeToFile(dataPacket.getData());
						previousBlockNumber = dataPacket.getBlockNumber();
					} catch (IOException e) {
						String errorMessage = e.getMessage();
						switch (errorMessage) {
						case "Permission denied": // thrown when directory is
													// read only
							System.out.println("Access Violation");
							firstTime = true;
							sendPacketToServer(new ErrorPacket(ErrorCode.ACCESS_VIOLATION, "Access violation"),
									receivePacket.getAddress(), receivePacket.getPort());
							break;
						default:
							throw e;
						}
					}
					// update previous block number
					// create an ack packet from corresponding block number
					if (!firstTime) {
						tftpPacket = new ACKPacket(dataPacket.getBlockNumber());
						sendPacketToServer(tftpPacket, receivePacket.getAddress(), receivePacket.getPort());
					}
					if (dataPacket.getData().length < 512) {
						System.out.println("\nComplete File Has Been Received\n");
						firstTime = true;
						tftpWriter.closeHandle();
					}
				} else {
					System.out.println("\nError Message: Disk Full or Allocation Exceded\n");
					sendPacketToServer(new ErrorPacket(ErrorCode.DISC_FULL_OR_ALLOCATION_EXCEEDED, "Disk full"),
							receivePacket.getAddress(), receivePacket.getPort());
					firstTime = true;
				}
			} else if (opcode == Opcode.ACK) {
				if (verbose) {
					System.out.println("Opcode: ACK");
				}
				ACKPacket ackPacket = new ACKPacket(data);
				// send next block of file until there are no more blocks
				if (ackPacket.getBlockNumber() <= previousBlockNumber - 1) {
					// received duplicate ACK drop the ACK packet
					if (verbose)
						System.out.println("Dropping duplicate ACK packet");
				} else {
					if (ackPacket.getBlockNumber() != previousBlockNumber) {
						throw new InvalidBlockNumberException("Data is out of order");
					}
					previousBlockNumber = ackPacket.getBlockNumber() + 1;
					// Send next block of file until there are no more blocks
					if (ackPacket.getBlockNumber() < tftpReader.getNumberOfBlocks()) {
						System.out.println("Sending DATA with block " + (previousBlockNumber));
						lastDataPacketSent = new DataPacket(previousBlockNumber,
								tftpReader.getFileBlock(previousBlockNumber));
						sendPacketToServer(lastDataPacketSent, receivePacket.getAddress(), receivePacket.getPort());
					}
					if (ackPacket.getBlockNumber() == tftpReader.getNumberOfBlocks()) {
						System.out.println("\nFile transfer complete");
						firstTime = true;
						lastDataPacketSent = null;
					}
				}
			} else if (opcode == Opcode.ERROR) { // check for error packet and
													// print message
				ErrorPacket errorPacket = new ErrorPacket(data);
				System.out.println("\nError Message: " + errorPacket.getErrorMessage() + "\n");
				firstTime = true;
			}

		} catch (SocketTimeoutException e) {
			if (verbose)
				System.out.println("\nServer took too long to respond");
			if (lastDataPacketSent == null) {
				// This case should never happen
				if (lastRequest != null) {
					if (verbose)
						System.out.println("Resending last RQ packet");
					sendPacketToServer(lastRequest, receivePacket.getAddress(), receivePacket.getPort());
				} else {
					if (verbose)
						System.out.println("No previous DATA packet sent, waiting for ACK/DATA");
				}
				counter++;
				if (counter == 10) {
					System.out.println("Server took way too long to respond, ending transfer");
					firstTime = true;
					counter = 0;
				}
			} else {
				if (verbose)
					System.out.println("Resending last DATA packet");
				sendPacketToServer(lastDataPacketSent, receivePacket.getAddress(), receivePacket.getPort());
				counter++;
				if (counter == 10) {
					System.out.println("Server took way too long to respond, ending transfer");
					firstTime = true;
					counter = 0;
				}
			}
		} catch (Exception e) {
			System.exit(0);
			e.printStackTrace();
		}
	}

	/**
	 * @param tftpPacket
	 * @param address
	 * @param port
	 * 
	 *            This function uses the information provided to create a send
	 *            packet and send it to the error simulator or the server
	 * 
	 */
	public void sendPacketToServer(TFTPPacket tftpPacket, InetAddress address, int port) {
		// Send packet to client
		sendPacket = new DatagramPacket(tftpPacket.getByteArray(), tftpPacket.getByteArray().length, address, port);
		// printing out information about the packet
		if (verbose) {
			System.out.println("\nClient: Sending packet");
			System.out.println("To host: " + sendPacket.getAddress());
			System.out.println("Destination host port: " + sendPacket.getPort());
			int length = sendPacket.getLength();
			System.out.println("Length: " + length);
			if (firstTime) {
				System.out.println(new String(tftpPacket.getByteArray(), 0, tftpPacket.getByteArray().length));
			}
			System.out.println("Byte Array: " + TFTPPacket.toString(sendPacket.getData()));
		}
		try {
			sendReceiveSocket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 * 
	 *            The main function requests the user for a directory and asks
	 *            if the client should run in verbose mode or quiet mode After
	 *            that, run the client on a look
	 * 
	 */
	public static void main(String args[]) {
		Scanner in = new Scanner(System.in);
		System.out.println("Enter the Directory Path:");
		System.out.println("Type \"DEFAULT\" to use the relative directory or Enter the filepath of the directory");

		for (;;) {
			String userInput = in.nextLine();
			if (userInput.equals("DEFAULT")) {
				// if default print the dir and finish
				System.out.println("You are now in: " + System.getProperty("user.dir") + "\\Client");
				filePath = System.getProperty("user.dir") + "\\Client" + "\\";
				System.out.println("\nYou can change the directory at any point by typing \"cd\"\n");
				break;
			} else {
				if (new File(userInput).isDirectory()) {
					// if the path was provided finish
					filePath = userInput + "\\";
					System.out.println("You have entered a valid Directory Path\n");
					break;
				} else {
					// if the directory does not exist, ask for an input again
					System.out.println("Invalid Directory\nPlease Try Again.");
				}
			}
		}

		String userInput;
		for (;;) {
			// request user for verbose or quiet mode
			System.out.println("Verbose(Y/N)?");
			userInput = in.nextLine();
			if (userInput.equals("Y")) {
				verbose = true;
				System.out.println("You have chosen Verbose mode");
				break;
			} else if (userInput.equals("N")) {
				verbose = false;
				System.out.println("You have chosen Quiet mode");
				break;
			} // if input is invalid, ask again
		}
		boolean done = false;

		while (!done) {
			System.out.println("Enter mode (TEST for test and NORMAL for normal)");
			String m = in.nextLine();

			if (m.equals("TEST")) {
				run = Mode.TEST;
				done = true;
			} else if (m.equals("NORMAL")) {
				run = Mode.NORMAL;
				done = true;

			}
		}
		System.out.println("You can change the directory at any point by typing \"cd\"\n");
		TFTPClient c = new TFTPClient();

		while (true) {
			try {
				if (firstTime) {
					c.sendRequest(in);
					firstTime = false;
				} // if its the first time, create the RRQ/WRQ packets
				c.sendReceivePacket();
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

}