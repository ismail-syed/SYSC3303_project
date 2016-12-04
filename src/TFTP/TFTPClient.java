package TFTP;

import Exceptions.InvalidBlockNumberException;
import Exceptions.MalformedPacketException;
import Exceptions.PacketOverflowException;
import FileIO.TFTPReader;
import FileIO.TFTPWriter;
import TFTPPackets.*;
import TFTPPackets.ErrorPacket.ErrorCode;
import TFTPPackets.TFTPPacket.Opcode;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.file.AccessDeniedException;
import java.util.Arrays;
import java.util.Scanner;
import static TFTPPackets.TFTPPacket.MAX_SIZE;

/*
 * @author Mohamed Zalat
 * @author Kunall Banerjee
 */
public class TFTPClient {
	private static final int SOCKET_TIMEOUT_MS = 1000;
	private DatagramPacket sendPacket, receivePacket;
	private DatagramSocket sendReceiveSocket;
	private int counter;
	private int previousBlockNumber;
	private TFTPPacket lastRequest;
	private static DataPacket lastDataPacketSent;
	private static String filePath;
	private TFTPReader tftpReader;
	private TFTPWriter tftpWriter;
	private static Mode run;
	private static boolean firstTime;
	private boolean firstReceive;
	private static boolean verbose;
	private int sendPort;
	private InetSocketAddress serverInetSocketAddress;
	private static boolean lastAckSent;
	private static InetAddress ip;
	private boolean invalidTID = false;

	public static enum Mode {
		NORMAL, TEST
	};

	/**
	 * Constructor
	 * <p>
	 * It creates the required sockets and sets a timeout
	 */
	public TFTPClient() {
		lastDataPacketSent = null;
		firstTime = true;
		firstReceive = true;
		lastRequest = null;
		lastAckSent = false;
		try {
			// Construct a Datagram socket and bind it to any available
			// port on the local host machine. This socket will be used to
			// send and receive UDP Datagram packets
			sendReceiveSocket = new DatagramSocket();
			// sendReceiveSocket.setSoTimeout(SOCKET_TIMEOUT_MS);//TODO
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * @param sc
	 * @throws PacketOverflowException
	 * @throws IOException
	 */
	public void sendRequest(Scanner sc) throws PacketOverflowException, IOException {
		String filename;
		TFTPPacket tftpPacket = new TFTPPacket();
		counter = 0;
		if (run == Mode.NORMAL) {
			sendPort = 69;
		} else {
			sendPort = 23;
		}
		boolean done = false;
		while (!done) {
			System.out.println("Choose b/w READ or WRITE request(R/W) or enter \"QUIT\" to close the client");
			String cmd = sc.nextLine();
			// write request
			if (cmd.equals("W")) {
				System.out.println("Client: creating WRQ packet.");
				sendReceiveSocket.setSoTimeout(SOCKET_TIMEOUT_MS);// TODO
				// get file name
				for (;;) {
					System.out.println("Enter file name");
					filename = sc.nextLine();
					if (new File(filePath + "\\" + filename).isFile()) {
						System.out.println("You have entered a valid file name");
						break;
					} else {
						// if the directory does not exist, ask for an input
						// again
						System.out.println("ERROR: File not found. Please try again\n");
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
			} else if (cmd.equals("R")) {
				System.out.println("Client: creating RRQ packet.");
				sendReceiveSocket.setSoTimeout(0);// TODO
				// get file name
				for (;;) {
					System.out.println("Enter file name");
					filename = sc.nextLine();
					if (!new File(filePath + "\\" + filename).isFile()) {
						// if a path was provided, break
						System.out.println("You have entered a valid file name");
						break;
					} else {
						// if the directory does not exist, ask for an input
						// again
						System.out.println("\nERROR: File already exists. Please try again\n");
					}
				}
				tftpPacket = new RRQPacket(filename, RRQWRQPacketCommon.Mode.NETASCII);
				previousBlockNumber = 0;
				done = true;
				try {
					tftpWriter = new TFTPWriter(new File(filePath + filename).getPath(), false);
				} catch (IOException e) {
					System.out.println("File can't be written to Client");
					firstTime = true;
					return;
				}
			} else if (cmd.equals("cd")) {
				System.out.println("Enter path to file:");
				System.out.println("Type \"DEFAULT\" to use the relative directory or enter the path of the directory");
				for (;;) {
					String userInput = sc.nextLine();
					if (userInput.equals("DEFAULT")) {
						// if default, print the dir and finish
						System.out.println("You are now in: " + System.getProperty("user.dir") + "\\Client");
						filePath = System.getProperty("user.dir") + "\\Client" + "\\";
						break;
					} else {
						if (new File(userInput).isDirectory()) {
							// if a path was provided, break
							filePath = userInput + "\\";
							System.out.println("You have entered a valid path\n");
							break;
						} else {
							// if the directory does not exist, ask for an input
							// again
							System.out.println("Invalid directory. Please try again\n");
						}
					}
				}
			} else if (cmd.equals("cip")) {
				System.out.println("Type \"DEFAULT\" to use the local IP or enter the IP of the server/sim PC");
				for (;;) {
					String userInput = sc.nextLine();
					if (userInput.equals("DEFAULT")) {
						// if default, print the IP and break
						try {
							ip = InetAddress.getLocalHost();
						} catch (UnknownHostException e) {
							e.printStackTrace();
						}
						System.out.println("You are now using local host: " + ip);
						System.out.println("\nYou can change the directory at any point by typing \"cip\"\n");
						break;
					} else {
						try {
							if (userInput == null || userInput.equals("")) {
								System.out.println("Invalid IP. Please try again\n");
								continue;
							}
							if ((ip = InetAddress.getByName(userInput)) instanceof InetAddress) {
								// if the path was provided, break
								System.out.println("You have entered a valid IP\n");
								break;
							}
						} catch (UnknownHostException e) {
							System.out.println("Invalid IP. Please try again\n");
						}
					}
				}
			} else if (cmd.equals("QUIT")) {
				System.out.println("Client: Closing socket and exiting.");
				// close scanner, socket and exit
				sc.close();
				sendReceiveSocket.close();
				System.exit(0);
			}
		}
		lastRequest = tftpPacket;
		sendPacketToServer(tftpPacket, sendPort);
		System.out.println("Client: Packet sent.");
		firstReceive = true;
		firstTime = false;
	}

	/**
	 * This function deals with the actual file transfer of DATA, ACK and ERROR
	 * packets
	 * 
	 * @throws IOException
	 */
	private void sendReceivePacket() throws IOException {
		byte dataBuffer[] = new byte[MAX_SIZE];
		byte[] data = null;
		TFTPPacket tftpPacket = new TFTPPacket();
		receivePacket = new DatagramPacket(dataBuffer, dataBuffer.length);
		try {
			// Receive packet
			sendReceiveSocket.receive(receivePacket);
			if (firstReceive) {
				serverInetSocketAddress = (InetSocketAddress) receivePacket.getSocketAddress();
				firstReceive = false;
			} else {
				if (!serverInetSocketAddress.equals(receivePacket.getSocketAddress())) {
					data = new byte[receivePacket.getLength()];
					System.arraycopy(dataBuffer, 0, data, 0, data.length);
					Opcode opcode = Opcode.asEnum((int) data[1]);
					DataPacket dataPacket;
					ACKPacket ackPacket;
					invalidTID = true;
					if (opcode == Opcode.DATA)
						try {
							dataPacket = new DataPacket(data);
							if (verbose)
								System.out.println("Unknown transfer ID on Data Packet " + dataPacket.getBlockNumber());
							sendPacketToServer(
									new ErrorPacket(ErrorCode.UNKNOWN_TID,
											"Unknown transfer ID on Data Packet " + dataPacket.getBlockNumber()),
									receivePacket.getPort());
							return;
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					else if (opcode == Opcode.ACK)
						try {
							ackPacket = new ACKPacket(data);
							if (verbose)
								System.out.println("Unknown transfer ID on ACK Packet " + ackPacket.getBlockNumber());
							sendPacketToServer(
									new ErrorPacket(ErrorCode.UNKNOWN_TID,
											"Unknown transfer ID on ACK Packet " + ackPacket.getBlockNumber()),
									receivePacket.getPort());
							return;
						} catch (Exception e) {
							e.printStackTrace();
						}
				}
			}
			lastRequest = null;
			counter = 0;
			// create byte array of proper size
			data = new byte[receivePacket.getLength()];
			System.arraycopy(dataBuffer, 0, data, 0, data.length);

			// process the received Datagram
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

			// get opcode
			Opcode opcode = Opcode.asEnum((int) data[1]);

			if (opcode == Opcode.DATA) {
				if (verbose) {
					System.out.println("Opcode: DATA");
				}
				// create/validate data
				try {
					DataPacket dataPacket = new DataPacket(data);
					// ensure there is enough space available
					if (new File(filePath).getUsableSpace() >= dataPacket.getData().length) {
						// write the data you just received
						try {
							// write the data from the DATA packet
							// only if it is the next block
							if (dataPacket.getBlockNumber() == previousBlockNumber + 1) {
								tftpWriter.writeToFile(dataPacket.getData());
								previousBlockNumber = dataPacket.getBlockNumber();
							} else if (dataPacket.getBlockNumber() > previousBlockNumber + 1) {
								sendPacketToServer(
										new ErrorPacket(ErrorCode.ILLEGAL_OPERATION,
												"Corrupt Block number on Data Packet " + dataPacket.getBlockNumber()),
										receivePacket.getPort());
								if (verbose)
									System.out.println(
											"Corrupt Block number on Data Packet " + dataPacket.getBlockNumber());
								tftpWriter.closeHandle();
								firstTime = true;
								return;
							}
						} catch (AccessDeniedException e) {
							System.out.println("Access Violation");
							sendPacketToServer(
									new ErrorPacket(ErrorPacket.ErrorCode.ACCESS_VIOLATION, "Access violation"),
									receivePacket.getPort());
							tftpWriter.closeHandle();
							firstTime = true;
							return;
						} catch (IOException e) {
							System.out.println("Access Violation(2nd catch)");
							sendPacketToServer(
									new ErrorPacket(ErrorPacket.ErrorCode.ACCESS_VIOLATION, "Access violation"),
									receivePacket.getPort());
							tftpWriter.closeHandle();
							firstTime = true;
							return;
						}
					} else {
						System.out.println("\nERROR: Disk full or allocation exceeded\n");
						sendPacketToServer(new ErrorPacket(ErrorCode.DISC_FULL_OR_ALLOCATION_EXCEEDED, "Disk full"),
								receivePacket.getPort());
						tftpWriter.closeHandle();
						firstTime = true;
						return;
					}
					// update previous block number
					// create an ACK packet from corresponding block number
					if (!firstTime) {
						tftpPacket = new ACKPacket(dataPacket.getBlockNumber());
						sendPacketToServer(tftpPacket, receivePacket.getPort());
					}
					if (dataPacket.getData().length < 512) {
						System.out.println("\nComplete file has been received\n");
						lastAckSent = true;
						firstTime = true;
						sendReceiveSocket.setSoTimeout(SOCKET_TIMEOUT_MS);
						tftpWriter.closeHandle();
					}
				} catch (PacketOverflowException e1) {
					if (verbose)
						System.out.println("Large amount of data received in DATA packet");

					sendPacketToServer(new ErrorPacket(ErrorCode.ILLEGAL_OPERATION,
							"DATA packet received with data larger than 512b"), receivePacket.getPort());
					firstTime = true;

				} catch (MalformedPacketException e2) {

					sendPacketToServer(
							new ErrorPacket(ErrorCode.ILLEGAL_OPERATION, "Data packet received in an invalid format"),
							receivePacket.getPort());
					if (verbose)
						System.out.println("DATA packet received in an invalid format");
					firstTime = true;
				}
			} else if (opcode == Opcode.ACK) {
				if (verbose) {
					System.out.println("Opcode: ACK");
				}
				try {
					ACKPacket ackPacket = new ACKPacket(data);
					// send next block of file until there are no more blocks
					if (ackPacket.getBlockNumber() <= previousBlockNumber - 1) {
						// received duplicate ACK, so drop the ACK packet
						if (verbose)
							System.out.println("Dropping duplicate ACK packet");
					} else {
						if (ackPacket.getBlockNumber() != previousBlockNumber) {
							sendPacketToServer(
									new ErrorPacket(ErrorCode.ILLEGAL_OPERATION,
											"Corrupt block number on ACK packet " + ackPacket.getBlockNumber()),
									receivePacket.getPort());
							if (verbose)
								System.out.println("Corrupt block number on ACK packet " + ackPacket.getBlockNumber());
							tftpWriter.closeHandle();
							firstTime = true;
							return;
						}
						previousBlockNumber = ackPacket.getBlockNumber() + 1;
						// send next block of file until there are no more
						// blocks
						if (ackPacket.getBlockNumber() < tftpReader.getNumberOfBlocks()) {
							if (verbose) {
								System.out.println("Sending DATA with block " + (previousBlockNumber));
							}
							lastDataPacketSent = new DataPacket(previousBlockNumber,
									tftpReader.getFileBlock(previousBlockNumber));
							sendPacketToServer(lastDataPacketSent, receivePacket.getPort());
						}
						if (ackPacket.getBlockNumber() == tftpReader.getNumberOfBlocks()) {
							System.out.println("\nFile transfer complete");
							firstTime = true;
						}
					}
				} catch (MalformedPacketException e2) {

					sendPacketToServer(
							new ErrorPacket(ErrorCode.ILLEGAL_OPERATION, "ACK packet received in an invalid format"),
							receivePacket.getPort());
					if (verbose)
						System.out.println("DATA packet received in an invalid format");
					firstTime = true;
				}
			} else if (opcode == Opcode.ERROR) {
				// check for error packet and print appropriate message
				try {
					ErrorPacket errorPacket = new ErrorPacket(data);
					System.out.println("\nERROR: " + errorPacket.getErrorMessage() + "\n");
					firstTime = true;
					if (tftpWriter != null) {
						tftpWriter.closeHandle();
					}
				} catch (MalformedPacketException e1) {
					sendPacketToServer(
							new ErrorPacket(ErrorCode.ILLEGAL_OPERATION, "Error packet received in an invalid format"),
							receivePacket.getPort());
					if (verbose)
						System.out.println("Error packet received in an invalid format");
					firstTime = true;
				} catch (PacketOverflowException e2) {
					sendPacketToServer(new ErrorPacket(ErrorCode.ILLEGAL_OPERATION,
							"Error packet received that doesn't end with a 0 byte"), receivePacket.getPort());
					if (verbose)
						System.out.println("Error packet received doesn't end with a 0 byte");
					firstTime = true;
				}

			} else {
				if (verbose)
					System.out.println("Corrupt opcode on received packet");
				sendPacketToServer(new ErrorPacket(ErrorCode.ILLEGAL_OPERATION, "Corrupt opcode on received packet"),
						receivePacket.getPort());
				if (tftpWriter != null)
					tftpWriter.closeHandle();
				firstTime = true;
			}

		} catch (SocketTimeoutException e) {
			if (verbose && !lastAckSent)
				System.out.println("\nServer took too long to respond!");
			if (lastAckSent) {
				lastAckSent = false;
				System.out.println("\nTransfer succeeded!");
				return;
			}
			if (lastDataPacketSent == null) {
				// this case should never happen
				if (lastRequest != null) {
					if (verbose)
						System.out.println("Resending last RQ packet");
					sendPacketToServer(lastRequest, sendPort);
				} else {
					if (verbose)
						System.out.println("No previous DATA packet sent; waiting for ACK/DATA");
				}
				counter++;
				if (counter == 10) {
					System.out.println("Server took way too long to respond; ending transfer");
					if (tftpWriter != null)
						tftpWriter.closeHandle();
					firstTime = true;
					counter = 0;
				}
			} else {
				if (verbose)
					System.out.println("Resending last DATA/RQ packet");

				sendPacketToServer(lastDataPacketSent, receivePacket.getPort());

				counter++;
				if (counter == 10) {
					System.out.println("Server took way too long to respond; ending transfer");
					firstTime = true;
					if (tftpWriter != null)
						tftpWriter.closeHandle();
					counter = 0;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * This function uses the information provided to create a send packet and
	 * send it to the error simulator or the server
	 * 
	 * @param tftpPacket
	 * @param address
	 * @param port
	 */
	public void sendPacketToServer(TFTPPacket tftpPacket, int port) {
		// send packet to client
		if (run == Mode.TEST)
			if (invalidTID) {
				sendPacket = new DatagramPacket(tftpPacket.getByteArray(), tftpPacket.getByteArray().length, ip, port);
			} else {
				sendPacket = new DatagramPacket(tftpPacket.getByteArray(), tftpPacket.getByteArray().length, ip,
						sendPort);
			}
		else
			sendPacket = new DatagramPacket(tftpPacket.getByteArray(), tftpPacket.getByteArray().length, ip, port);
		// print info on the packet
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
			if (tftpPacket instanceof DataPacket)
				lastDataPacketSent = (DataPacket) tftpPacket;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 */
	public static void main(String args[]) {
		Scanner in = new Scanner(System.in);
		System.out.println("Enter path:");
		System.out.println("Type \"DEFAULT\" to use the relative directory or enter the path of the directory");

		for (;;) {
			String userInput = in.nextLine();
			if (userInput.equals("DEFAULT")) {
				// if default print the dir, break
				System.out.println("You are now in: " + System.getProperty("user.dir") + "\\Client");
				filePath = System.getProperty("user.dir") + "\\Client" + "\\";
				System.out.println("\nYou can change the directory at any point by typing \"cd\"\n");
				break;
			} else {
				if (new File(userInput).isDirectory()) {
					// if the path was provided, break
					filePath = userInput + "\\";
					System.out.println("You have entered a valid path\n");
					break;
				} else {
					// if the directory does not exist, ask for an input again
					System.out.println("Invalid directory. Please try again\n");
				}
			}
		}
		System.out.println("Type \"DEFAULT\" to use the local IP or enter the ip of the server/sim PC");
		for (;;) {
			String userInput = in.nextLine();
			if (userInput.equals("DEFAULT")) {
				// if default print the IP, break
				try {
					ip = InetAddress.getLocalHost();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
				System.out.println("You are now using local host: " + ip);
				System.out.println("\nYou can change the directory at any point by typing \"cip\"\n");
				break;
			} else {
				try {
					if (userInput == null || userInput.equals("")) {
						System.out.println("Invalid IP. Please try again\n");
						continue;
					}
					if ((ip = InetAddress.getByName(userInput)) instanceof InetAddress) {
						// if the path was provided finish
						System.out.println("You have entered a valid IP\n");
						break;
					}
				} catch (UnknownHostException e) {
					System.out.println("Invalid IP. Please try again\n");
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
				if (firstTime && !lastAckSent) {
					lastDataPacketSent = null;
					c.sendRequest(in);
				}
				// if it is the first time, create the RRQ/WRQ packet(s)
				if (!firstTime && !lastAckSent)
					c.sendReceivePacket();
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
}