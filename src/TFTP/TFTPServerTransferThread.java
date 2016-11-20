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
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;

import static TFTPPackets.TFTPPacket.MAX_SIZE;

/**
 * The {@link TFTPServerTransferThread} class represents a thread which is started
 * by the {@link TFTPServer} to send or receive files from the client
 * (based on Assignment 1 solution)
 *
 * @author Team 3000000
 * @author Aritra Sengupta
 * @author Shasthra Ranasinghe
 * @version 3.0
 */

public class TFTPServerTransferThread implements Runnable {

    private static final int SOCKET_TIMEOUT_MS = 1000;
    private final String filePath;
    private final boolean verbose; //verbose or quiet
    private int previousBlockNumber; //keeps track of the block numbers to ensure blocks received are in order
    private Boolean allowTransfers;
    private volatile Boolean receivedRRQPacket;
    private volatile Boolean receivedWRQPacket;
    private DataPacket lastDataPacketSent;
    private DatagramPacket packetFromClient;
    private DatagramSocket sendReceiveSocket;
    private TFTPReader tftpReader;
    private TFTPWriter tftpWriter;
    private final byte dataBuffer[] = new byte[MAX_SIZE];
    private final InetSocketAddress clientInetSocketAddress;

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
        //check for unknown TID
        if(!clientInetSocketAddress.equals(packetFromClient.getSocketAddress())){
            verboseLog("Received unknown TID, replying with unknown TID packet (ERROR CODE 5)");
            sendPacketToClient(new ErrorPacket(ErrorCode.UNKNOWN_TID, "Unknown TID"));
        } else {
            try {
                //Create byte array of proper size
                byte[] data = new byte[packetFromClient.getLength()];
                System.arraycopy(packetFromClient.getData(), 0, data, 0, data.length);
                verboseLog("\nServer: Packet received:");
                verboseLog("From host: " + packetFromClient.getAddress());
                verboseLog("Host port: " + packetFromClient.getPort());
                int len = packetFromClient.getLength();
                verboseLog("Length: " + len);
                verboseLog("Containing: ");
                verboseLog(new String(Arrays.copyOfRange(data, 0, len)));
                verboseLog("Byte Array: " + TFTPPacket.toString(Arrays.copyOfRange(data, 0, len)) + "\n");
                //Get opcode
                TFTPPacket.Opcode opcode = TFTPPacket.Opcode.asEnum((packetFromClient.getData()[1]));
                switch (opcode) {
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
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void processReadPacket(byte[] packetData) throws IOException {
        if (!receivedRRQPacket && !receivedWRQPacket) {
            receivedRRQPacket = true;
            verboseLog("****************NEW TRANSFER****************\n");
            verboseLog("Opcode: READ");
            try {
            	sendReceiveSocket.setSoTimeout(SOCKET_TIMEOUT_MS);
                //Parse RRQ packet
                RRQPacket rrqPacket = new RRQPacket(packetData);
                //Read from File
                tftpReader = new TFTPReader(new File(filePath + rrqPacket.getFilename()).getPath());
                //Create DATA packet with first block of file
                System.out.println("Sending block 1");
                previousBlockNumber = 1;
                sendPacketToClient(new DataPacket(1, tftpReader.getFileBlock(1)));
            } catch (NoSuchFileException | FileNotFoundException e) {
                System.out.println("File not found");
                sendPacketToClient(new ErrorPacket(ErrorPacket.ErrorCode.FILE_NOT_FOUND, "File not found"));
            } catch (AccessDeniedException e) {
                System.out.println("Access Violation");
                sendPacketToClient(new ErrorPacket(ErrorPacket.ErrorCode.ACCESS_VIOLATION, "Access violation"));
            } catch (PacketOverflowException | MalformedPacketException | InvalidBlockNumberException e) {
                e.printStackTrace();
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
                //Parse WRQ packet
                WRQPacket wrqPacket = new WRQPacket(packetData);
                //Open file
                File file = new File(filePath + wrqPacket.getFilename());
                if (file.exists()) {
                    // handle file output already exists
                    throw new FileAlreadyExistsException("File already exist");
                }
                tftpWriter = new TFTPWriter(file.getPath(), false);
                //Send ACK packet with block number 0
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
            } catch (InvalidBlockNumberException | PacketOverflowException | MalformedPacketException e) {
                e.printStackTrace();
            }
        } else {
            verboseLog("Dropping duplicate WRQ packet");
        }
    }

    private void processDataPacket(byte[] packetData) throws IOException {
        if (receivedWRQPacket) {
            verboseLog("Opcode: DATA");
            try {
                //Parse DATA packet
                DataPacket dataPacket = new DataPacket(packetData);
                //Write the data from the DATA packet only if it is the next block
                if (dataPacket.getBlockNumber() == previousBlockNumber + 1) {
                    //Write data to file
                    tftpWriter.writeToFile(dataPacket.getData());
                    //Save previous block number
                    previousBlockNumber = dataPacket.getBlockNumber();
                }
                //Create an ACK packet with the same block number as the DATA packet
                verboseLog("Sending ACK with block " + dataPacket.getBlockNumber());
                sendPacketToClient(new ACKPacket(dataPacket.getBlockNumber()));
                if (dataPacket.getData().length < DataPacket.MAX_DATA_SIZE) {
                    //transfer finished for WRQ
                    tftpWriter.closeHandle();
                    endTransfer();
                }
            } catch (IOException e) {
                String errorMessage = e.getMessage();
                switch (errorMessage) {
                    case "There is not enough space on the disk":
                        System.out.println("Disk full");
                        sendPacketToClient(new ErrorPacket(ErrorCode.DISC_FULL_OR_ALLOCATION_EXCEEDED, "Disk full"));
                        break;
                    case "The device is not ready":  // thrown when storage is removed during transfer
                        System.out.println("Access Violation");
                        sendPacketToClient(new ErrorPacket(ErrorCode.ACCESS_VIOLATION, "Access violation"));
                        break;
                    default:
                        throw e;
                }
            } catch (InvalidBlockNumberException | MalformedPacketException | PacketOverflowException e) {
                e.printStackTrace();
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
                //Parse ACK packet
                ACKPacket ackPacket = new ACKPacket(packetData);
                if (ackPacket.getBlockNumber() == previousBlockNumber - 1) {
                    //received duplicate ACK drop the ACK packet
                    verboseLog("Dropping duplicate ACK packet");
                } else {
                    if (ackPacket.getBlockNumber() != previousBlockNumber) {
                        throw new InvalidBlockNumberException("Data is out of order");
                    }
                    previousBlockNumber = ackPacket.getBlockNumber() + 1;
                    //Send next block of file until there are no more blocks
                    if (ackPacket.getBlockNumber() < tftpReader.getNumberOfBlocks()) {
                        System.out.println("Sending DATA with block " + (previousBlockNumber));
                        sendPacketToClient(new DataPacket(previousBlockNumber, tftpReader.getFileBlock(previousBlockNumber)));
                    }
                    if (ackPacket.getBlockNumber() == tftpReader.getNumberOfBlocks()) {
                        endTransfer();
                    }
                }
            } catch (MalformedPacketException | PacketOverflowException | InvalidBlockNumberException e) {
                e.printStackTrace();
            }
        } else {
            verboseLog("Received ACK packet without receiving a WRQ packet first");
            verboseLog("Dropping ACK packet");
        }
    }

    private void sendPacketToClient(TFTPPacket tftpPacket) {
        if (allowTransfers) {
            //Send packet to client
            DatagramPacket sendPacket = new DatagramPacket(tftpPacket.getByteArray(), tftpPacket.getByteArray().length,
                    clientInetSocketAddress.getAddress(), clientInetSocketAddress.getPort());
            //printing out information about the packet
            verboseLog("Server: Sending packet");
            verboseLog("To host: " + sendPacket.getAddress());
            verboseLog("Destination host port: " + sendPacket.getPort());
            verboseLog("Length: " + sendPacket.getLength());
            verboseLog("Byte Array: " + TFTPPacket.toString(sendPacket.getData()));
            try {
                sendReceiveSocket.send(sendPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (tftpPacket instanceof ErrorPacket) {
                endTransfer();
            } else if (tftpPacket instanceof DataPacket) {
                lastDataPacketSent = (DataPacket) tftpPacket;
            }
        }
    }

    private void endTransfer() {
        verboseLog("Closing socket");
        allowTransfers = false;
        try {
            if (tftpWriter != null) {
                tftpWriter.closeHandle();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void verboseLog(String logMessage) {
        if (verbose) {
            System.out.println(logMessage);
        }
    }

    @Override
    public void run() {
        while (allowTransfers) {
            if (!(packetFromClient.getAddress() == null)) {
                sendAndReceive();
            }
            try {
                //Receive packet
                if (allowTransfers) {
                    packetFromClient = new DatagramPacket(dataBuffer, dataBuffer.length);
                    sendReceiveSocket.receive(packetFromClient);
                }
            } catch (SocketTimeoutException e) {
                verboseLog("\nClient took too long to respond");
                if (lastDataPacketSent == null) {
                    //This case should never happen
                    verboseLog("Null DATA packet detected");
                    endTransfer();
                } else {
                    verboseLog("Resending last DATA packet");
                    sendPacketToClient(lastDataPacketSent);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        sendReceiveSocket.close();
        verboseLog("Closed socket");
    }
}