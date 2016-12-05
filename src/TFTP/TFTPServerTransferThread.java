package TFTP;

import Exceptions.InvalidBlockNumberException;
import Exceptions.MalformedPacketException;
import Exceptions.PacketOverflowException;
import FileIO.TFTPReader;
import FileIO.TFTPWriter;
import TFTPPackets.*;
import TFTPPackets.ErrorPacket.ErrorCode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.nio.BufferUnderflowException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static TFTPPackets.TFTPPacket.MAX_SIZE;

/**
 * The {@link TFTPServerTransferThread} class represents a thread which is
 * started by the {@link TFTPServer} to send or receive files from the client
 * (based on Assignment 1 solution)
 *
 * @author Team 3000000
 * @author Aritra Sengupta
 * @author Shasthra Ranasinghe
 * @version 4.0
 */

public class TFTPServerTransferThread implements Runnable {

	private static final int SOCKET_TIMEOUT_MS = 1000;
	private static final int SOCKET_TIMEOUT_LAST_PACKET_MS = 10000;
	private static final int RETRY_LIMIT = 10;
	private final String filePath;
	private final boolean verbose; // verbose or quiet
	private int previousBlockNumber; // keeps track of the block numbers to
										// ensure blocks received are in order
	private Boolean allowTransfers;
	private volatile Boolean receivedRRQPacket;
	private volatile Boolean receivedWRQPacket;
	private ACKPacket lastAckPacketSent;
	private DataPacket lastDataPacketSent;
	private DatagramPacket packetFromClient;
	private DatagramSocket sendReceiveSocket;
	private TFTPReader tftpReader;
	private TFTPWriter tftpWriter;
	private final byte dataBuffer[] = new byte[MAX_SIZE];
	private final InetSocketAddress clientInetSocketAddress;
	private final static Lock lock = new ReentrantLock();

	public TFTPServerTransferThread(DatagramPacket packetFromClient, String filePath, boolean verbose) {
		this.packetFromClient = packetFromClient;
		this.filePath = filePath;
		this.verbose = verbose;
		this.allowTransfers = true;
		this.receivedRRQPacket = false;
		this.receivedWRQPacket = false;
		this.clientInetSocketAddress = (InetSocketAddress) packetFromClient.getSocketAddress();
		try {
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	private void sendAndReceive() {
		// check for unknown TID
		if (!clientInetSocketAddress.equals(packetFromClient.getSocketAddress())) {
			verboseLog("Expected TID: " + clientInetSocketAddress);
			verboseLog("Received TID: " + packetFromClient.getSocketAddress());
			verboseLog("Received unknown TID, replying with unknown TID packet (ERROR CODE 5)");
			ErrorPacket invalidTIDErrorPacket = new ErrorPacket(ErrorCode.UNKNOWN_TID, "Unknown TID");
			// Send packet to the other client
			DatagramPacket sendPacket = new DatagramPacket(invalidTIDErrorPacket.getByteArray(),
					invalidTIDErrorPacket.getByteArray().length, packetFromClient.getAddress(),
					packetFromClient.getPort());

			// printing out information about the packet
			printPacketInfo(sendPacket, true);
			try {
				sendReceiveSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
				// Create byte array of proper size
				byte[] data = new byte[packetFromClient.getLength()];
				System.arraycopy(packetFromClient.getData(), 0, data, 0, data.length);
				printPacketInfo(packetFromClient, false);
				// Get opcode
				TFTPPacket.Opcode opcode = TFTPPacket.Opcode.asEnum((packetFromClient.getData()[1]));
				switch (opcode) {
				case UNKNOWN:
					sendPacketToClient(new ErrorPacket(ErrorCode.ILLEGAL_OPERATION, "Invalid Opcode"));
					break;
				case READ:
					processReadPacket(data);
					break;
				case WRITE:
					processWritePacket(data);
					break;
				case DATA:
					processDataPacket(data);
					break;
				case ACK:
					processACKPacket(data);
					break;
				case ERROR:
					processErrorPacket(data);
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void processErrorPacket(byte[] packetData) {
		verboseLog("Opcode: ERROR");
		try {
			ErrorPacket errorPacket = new ErrorPacket(packetData);
			ErrorCode errorCode = errorPacket.getErrorCode();
			if (errorCode.equals(ErrorCode.ILLEGAL_OPERATION) || errorCode.equals(ErrorCode.UNKNOWN_TID)) {
				// end transfer on illegal operation or unknown TID
				endTransfer();
			}
		} catch (MalformedPacketException e) {
			verboseLog("Invalid Error packet");
			sendPacketToClient(new ErrorPacket(ErrorCode.ILLEGAL_OPERATION, e.getMessage()));
		} catch (PacketOverflowException | IOException e) {
			verboseLog("Invalid Error packet");
			sendPacketToClient(new ErrorPacket(ErrorCode.ILLEGAL_OPERATION, "Invalid Error packet"));
		}
	}

	private void processReadPacket(byte[] packetData) throws IOException {
		if (!receivedRRQPacket && !receivedWRQPacket) {
			RRQPacket rrqPacket = null;
			receivedRRQPacket = true;
			verboseLog("****************NEW TRANSFER****************\n");
			verboseLog("Opcode: READ");
			try {
				sendReceiveSocket.setSoTimeout(SOCKET_TIMEOUT_MS);
				// Parse RRQ packet
				rrqPacket = new RRQPacket(packetData);
				// Read from File
				tftpReader = new TFTPReader(new File(filePath + rrqPacket.getFilename()).getPath());
				// Create DATA packet with first block of file
				verboseLog("Sending block 1");
				previousBlockNumber = 1;
				sendPacketToClient(new DataPacket(1, tftpReader.getFileBlock(1)));
			} catch (NoSuchFileException | FileNotFoundException e) {
				System.out.println("File not found");
				sendPacketToClient(new ErrorPacket(ErrorPacket.ErrorCode.FILE_NOT_FOUND,
						"File " + rrqPacket.getFilename() + "not found"));
			} catch (AccessDeniedException e) {
				System.out.println("Access Violation");
				sendPacketToClient(new ErrorPacket(ErrorPacket.ErrorCode.ACCESS_VIOLATION, "Access violation"));
			} catch (PacketOverflowException e) {
				verboseLog("Invalid RRQ packet");
				sendPacketToClient(new ErrorPacket(ErrorCode.ILLEGAL_OPERATION, "Invalid RRQ packet"));
			} catch (InvalidBlockNumberException e) {
				verboseLog("File larger than max blocks");
				sendPacketToClient(new ErrorPacket(ErrorCode.ILLEGAL_OPERATION, e.getMessage()));
			} catch (MalformedPacketException e) {
				verboseLog("Invalid RRQ name");
				sendPacketToClient(new ErrorPacket(ErrorCode.ILLEGAL_OPERATION, e.getMessage()));
			} catch (BufferUnderflowException e) {
				verboseLog("Packet missing delimiting 0's");
				sendPacketToClient(new ErrorPacket(ErrorCode.ILLEGAL_OPERATION, "Packet missing delimiting 0's"));
			}
		} else {
			verboseLog("Dropping duplicate RRQ packet");
		}
	}

	private void processWritePacket(byte[] packetData) throws IOException {
		if (!receivedRRQPacket && !receivedWRQPacket) {
			receivedWRQPacket = true;
			verboseLog("****************NEW TRANSFER****************\n");
			verboseLog("Opcode: WRITE");
			try {
				sendReceiveSocket.setSoTimeout(0);
				// Parse WRQ packet
				WRQPacket wrqPacket = new WRQPacket(packetData);
				// Open file
				File file = new File(filePath + wrqPacket.getFilename());
				if (file.exists()) {
					// handle file output already exists
					throw new FileAlreadyExistsException("File already exist");
				}
				tftpWriter = new TFTPWriter(file.getPath(), false);
				// Send ACK packet with block number 0
				verboseLog("Sending ACK with block 0");
				sendPacketToClient(new ACKPacket(0));
				previousBlockNumber = 0;
			} catch (FileAlreadyExistsException e) {
				System.out.println("File already Exist");
				sendPacketToClient(new ErrorPacket(ErrorCode.FILE_ALREADY_EXISTS, "File already exists"));
			} catch (FileNotFoundException e) {
				System.out.println(e.getMessage());
				String errMessage = e.getMessage();
				if (errMessage.contains("(Access is denied)")) {
					System.out.println("Access Violation");
					sendPacketToClient(new ErrorPacket(ErrorPacket.ErrorCode.ACCESS_VIOLATION, "Access violation"));
				} else {
					throw e;
				}
			} catch (InvalidBlockNumberException | PacketOverflowException e) {
				verboseLog("Invalid WRQ packet");
				sendPacketToClient(new ErrorPacket(ErrorCode.ILLEGAL_OPERATION, "Invalid WRQ packet"));
			} catch (MalformedPacketException e) {
				verboseLog("Invalid WRQ packet");
				sendPacketToClient(new ErrorPacket(ErrorCode.ILLEGAL_OPERATION, e.getMessage()));
			} catch (BufferUnderflowException e) {
				verboseLog("Packet missing delimiting 0's");
				sendPacketToClient(new ErrorPacket(ErrorCode.ILLEGAL_OPERATION, "Packet missing delimiting 0's"));
			}
		} else {
			verboseLog("Dropping duplicate WRQ packet");
		}
	}

	private void processDataPacket(byte[] packetData) throws IOException {
		if (receivedWRQPacket) {
			verboseLog("Opcode: DATA");
			try {
				// Parse DATA packet
				DataPacket dataPacket = new DataPacket(packetData);
				// Write the data from the DATA packet only if it is the next
				// block
				if (dataPacket.getBlockNumber() == previousBlockNumber + 1) {
					// Write data to file
					tftpWriter.writeToFile(dataPacket.getData());
					// Save previous block number
					previousBlockNumber = dataPacket.getBlockNumber();
				}
				// Create an ACK packet with the same block number as the DATA
				// packet
				verboseLog("Sending ACK with block " + dataPacket.getBlockNumber());
				sendPacketToClient(new ACKPacket(dataPacket.getBlockNumber()));
				if (dataPacket.getData().length < DataPacket.MAX_DATA_SIZE) {
					// transfer finished for WRQ
					tftpWriter.closeHandle();
					System.out.println("Complete File Has Been Received");
					endTransfer();
				}
			} catch (IOException e) {
				String errorMessage = e.getMessage();
				switch (errorMessage) {
				case "There is not enough space on the disk":
					System.out.println("Disk full");
					sendPacketToClient(new ErrorPacket(ErrorCode.DISC_FULL_OR_ALLOCATION_EXCEEDED, "Disk full"));
					break;
				case "The device is not ready": // thrown when storage is
												// removed during transfer
					System.out.println("Access Violation");
					sendPacketToClient(new ErrorPacket(ErrorCode.ACCESS_VIOLATION, "Access violation"));
					break;
				default:
					throw e;
				}
			} catch (InvalidBlockNumberException | PacketOverflowException e) {
				verboseLog("Invalid DATA packet");
				sendPacketToClient(new ErrorPacket(ErrorCode.ILLEGAL_OPERATION, "Invalid DATA packet"));
			} catch (MalformedPacketException e) {
				verboseLog("Invalid DATA packet");
				sendPacketToClient(new ErrorPacket(ErrorCode.ILLEGAL_OPERATION, e.getMessage()));
			}
		} else {
			verboseLog("Received DATA packet without receiving a RRQ packet first");
			verboseLog("Dropping DATA packet");
		}
	}

	private void processACKPacket(byte[] packetData) throws IOException {
		if (receivedRRQPacket) {
			verboseLog("Opcode: ACK");
			try {
				// Parse ACK packet
				ACKPacket ackPacket = new ACKPacket(packetData);
				if (ackPacket.getBlockNumber() == previousBlockNumber - 1) {
					// received duplicate ACK drop the ACK packet
					verboseLog("Dropping duplicate ACK packet");
				} else {
					if (ackPacket.getBlockNumber() != previousBlockNumber) {
						// Ack packet does not have the right order
						// If block number is greater than the
						// previousBlockNumber send error 4
						if (ackPacket.getBlockNumber() > previousBlockNumber) {
							throw new InvalidBlockNumberException("Data is out of order");
						}
					}
					previousBlockNumber = ackPacket.getBlockNumber() + 1;
					// Send next block of file until there are no more blocks
					if (ackPacket.getBlockNumber() < tftpReader.getNumberOfBlocks()) {
						verboseLog("Sending DATA with block " + (previousBlockNumber));
						sendPacketToClient(
								new DataPacket(previousBlockNumber, tftpReader.getFileBlock(previousBlockNumber)));
					}
					if (ackPacket.getBlockNumber() == tftpReader.getNumberOfBlocks()) {
						System.out.println("Complete File Has Been Sent");
						endTransfer();
					}
				}
			} catch (MalformedPacketException e) {
				verboseLog("Invalid ACK packet");
				sendPacketToClient(new ErrorPacket(ErrorCode.ILLEGAL_OPERATION, e.getMessage()));
			} catch (PacketOverflowException | InvalidBlockNumberException e) {
				verboseLog("Invalid ACK packet");
				sendPacketToClient(new ErrorPacket(ErrorCode.ILLEGAL_OPERATION, "Invalid ACK packet"));
			} catch (BufferUnderflowException e) {
				verboseLog("Packet missing delimiting 0's");
				sendPacketToClient(new ErrorPacket(ErrorCode.ILLEGAL_OPERATION, "Packet missing delimiting 0's"));
			}
		} else {
			verboseLog("Received ACK packet without receiving a WRQ packet first");
			verboseLog("Dropping ACK packet");
		}
	}

	private void sendPacketToClient(TFTPPacket tftpPacket) {
		if (allowTransfers || tftpPacket instanceof ACKPacket) {
			// Send packet to client
			DatagramPacket sendPacket = new DatagramPacket(tftpPacket.getByteArray(), tftpPacket.getByteArray().length,
					clientInetSocketAddress.getAddress(), clientInetSocketAddress.getPort());
			// printing out information about the packet
			printPacketInfo(sendPacket, true);
			try {
				sendReceiveSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (tftpPacket instanceof ErrorPacket) {
				if (!((ErrorPacket) tftpPacket).getErrorCode().equals(ErrorCode.UNKNOWN_TID)) {
					endTransfer();
				}
			} else if (tftpPacket instanceof DataPacket) {
				lastDataPacketSent = (DataPacket) tftpPacket;
			} else if (tftpPacket instanceof ACKPacket) {
				lastAckPacketSent = (ACKPacket) tftpPacket;
			}
		}
	}

	private void endTransfer() {
		verboseLog("\n");
		verboseLog("Ending transfer");
		allowTransfers = false;
		try {
			if (tftpWriter != null) {
				tftpWriter.closeHandle();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			if (receivedWRQPacket) {
				sendReceiveSocket.setSoTimeout(SOCKET_TIMEOUT_LAST_PACKET_MS);
				receivePacketFromClient();
				// check the packet received from the client is a DATA packet
				if (packetFromClient.getData()[1] == TFTPPacket.Opcode.DATA.getOpCodeNumber()) {
					verboseLog("Resending last ACK");
					sendPacketToClient(lastAckPacketSent);
				} else {
					verboseLog("Received a packet that was not an ACK, not responding");
				}
			}

		} catch (SocketTimeoutException e) {
			verboseLog("No response from client, client must have received all the packets");
		} catch (SocketException e) {
			e.printStackTrace();
		} finally {
			closeSocket();
		}
	}

	private void closeSocket() {
		int socketNumber = sendReceiveSocket.getLocalPort();
		sendReceiveSocket.close();
		verboseLog("Closed socket: " + socketNumber);
	}

	private void verboseLog(String logMessage) {
		if (verbose) {
			if (logMessage.equals("\n")) {
				System.out.println(logMessage);
			} else {
				System.out.println(Thread.currentThread().getId() + ": " + logMessage);
			}
		}
	}

	private void printPacketInfo(DatagramPacket datagramPacket, Boolean sendingToClient) {
		lock.lock();
		if (sendingToClient) {
			// printing out information about the packet
			verboseLog("\n");
			verboseLog("Server: Sending packet");
			verboseLog("To host: " + datagramPacket.getAddress());
			verboseLog("Destination host port: " + datagramPacket.getPort());
			verboseLog("Length: " + datagramPacket.getLength());
			verboseLog("Byte Array: " + TFTPPacket.toString(datagramPacket.getData()));
		} else {
			byte[] data = new byte[datagramPacket.getLength()];
			System.arraycopy(datagramPacket.getData(), 0, data, 0, data.length);
			verboseLog("\n");
			verboseLog("Server: Packet received:");
			verboseLog("From host: " + datagramPacket.getAddress());
			verboseLog("Host port: " + datagramPacket.getPort());
			int len = datagramPacket.getLength();
			verboseLog("Length: " + len);
			verboseLog("Containing: " + new String(Arrays.copyOfRange(data, 0, len)));
			verboseLog("Byte Array: " + TFTPPacket.toString(Arrays.copyOfRange(data, 0, len)) + "\n");
		}
		lock.unlock();
	}

	private void receivePacketFromClient() throws SocketTimeoutException {
		packetFromClient = new DatagramPacket(dataBuffer, dataBuffer.length);
		try {
			sendReceiveSocket.receive(packetFromClient);
		} catch (IOException e) {
			if (e instanceof SocketTimeoutException) {
				throw new SocketTimeoutException();
			} else {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void run() {
		int numberOfRetries = 0;
		while (allowTransfers) {
			if (!(packetFromClient.getAddress() == null)) {
				sendAndReceive();
			}
			try {
				// Receive packet
				if (allowTransfers) {
					receivePacketFromClient();
				}
			} catch (SocketTimeoutException e) {
				if (numberOfRetries < RETRY_LIMIT) {
					verboseLog("\n");
					verboseLog("Client took too long to respond");
					if (lastDataPacketSent == null) {
						// This case should never happen
						verboseLog("Null DATA packet detected");
						endTransfer();
					} else {
						verboseLog("Retry attempt " + String.valueOf(++numberOfRetries));
						verboseLog("Resending last DATA packet");
						sendPacketToClient(lastDataPacketSent);
					}
				} else {
					endTransfer();
				}
			}
		}
		TFTPServer.serverInstance.removeFromTransferPortList(Thread.currentThread().getId(),
				clientInetSocketAddress.getPort());
	}
}