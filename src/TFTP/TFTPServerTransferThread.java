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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
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

    private static final int SOCKET_TIMEOUT_MS = 10000;
    private String filePath;
    private boolean verbose; //verbose or quiet
    private int previousBlockNumber; // keeps track of the block numbers to ensure blocks received are in order
    private Boolean allowTransfers;
    private DatagramPacket packetFromClient;
    private DatagramSocket sendReceiveSocket;
    private TFTPReader tftpReader;
    private TFTPWriter tftpWriter;
    private byte dataBuffer[] = new byte[MAX_SIZE];

    public TFTPServerTransferThread(DatagramPacket packetFromClient, String filePath, boolean verbose) {
        this.packetFromClient = packetFromClient;
        this.filePath = filePath;
        this.verbose = verbose;
        this.allowTransfers = true;
        try {
            sendReceiveSocket = new DatagramSocket();
            //set a timeout of 10s
            sendReceiveSocket.setSoTimeout(SOCKET_TIMEOUT_MS);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private void sendAndReceive() {
        try {
            //Create byte array of proper size
            byte[] data = new byte[packetFromClient.getLength()];
            System.arraycopy(packetFromClient.getData(), 0, data, 0, data.length);
            //Process the received datagram.
            System.out.println("\nServer: Packet received:");
            if (verbose) {
                System.out.println("From host: " + packetFromClient.getAddress());
                System.out.println("Host port: " + packetFromClient.getPort());
                int len = packetFromClient.getLength();
                System.out.println("Length: " + len);
                System.out.println("Containing: ");
                System.out.println(new String(Arrays.copyOfRange(data, 0, len)));
                System.out.println("Byte Array: " + TFTPPacket.toString(Arrays.copyOfRange(data, 0, len)) + "\n");
            }
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

    private void processReadPacket(byte[] packetData) throws IOException {
        if (verbose) {
            System.out.println("****************NEW TRANSFER****************\n");
        }
        System.out.println("Opcode: READ");
        try {
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
    }

    private void processWritePacket(byte[] packetData) throws IOException {
        if (verbose) {
            System.out.println("****************NEW TRANSFER****************\n");
        }
        System.out.println("Opcode: WRITE");
        try {
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
            System.out.println("Sending ACK with block 0");
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
    }

    private void processDataPacket(byte[] packetData) throws IOException {
        System.out.println("Opcode: DATA");
        try {
            //Parse DATA packet
            DataPacket dataPacket = new DataPacket(packetData);
            //Write the data from the DATA packet
            if (dataPacket.getBlockNumber() != previousBlockNumber + 1) {
                throw new InvalidBlockNumberException("Data is out of order");
            }
            tftpWriter.writeToFile(dataPacket.getData());
            previousBlockNumber = dataPacket.getBlockNumber();
            //Create an ACK packet for corresponding block number
            System.out.println("Sending ACK with block " + previousBlockNumber);
            sendPacketToClient(new ACKPacket(previousBlockNumber));
            if (dataPacket.getData().length < DataPacket.MAX_DATA_SIZE) {
                //transfer finished for WRQ
                tftpWriter.closeHandle();
                endTransfer();
            }
        } catch (IOException e) {
            if (e.getMessage().equals("There is not enough space on the disk")) {
                System.out.println("Disk full");
                sendPacketToClient(new ErrorPacket(ErrorCode.DISC_FULL_OR_ALLOCATION_EXCEEDED, "Disk full"));
            } else {
                throw e;
            }
        } catch (InvalidBlockNumberException | MalformedPacketException | PacketOverflowException e) {
            e.printStackTrace();
        }
    }

    private void processACKPacket(byte[] packetData) throws IOException {
        System.out.println("Opcode: ACK");
        try {
            //Parse ACK packet
            ACKPacket ackPacket = new ACKPacket(packetData);
            if (ackPacket.getBlockNumber() != previousBlockNumber) {
                throw new InvalidBlockNumberException("Data is out of order");
            }
            previousBlockNumber = ackPacket.getBlockNumber() + 1;
            //Send next block of file until there are no more blocks
            if (ackPacket.getBlockNumber() < tftpReader.getNumberOfBlocks()) {
                sendPacketToClient(new DataPacket(previousBlockNumber, tftpReader.getFileBlock(previousBlockNumber)));
                System.out.println("Sending DATA with block " + (previousBlockNumber));
            }
            if (ackPacket.getBlockNumber() == tftpReader.getNumberOfBlocks()) {
                endTransfer();
            }
        } catch (MalformedPacketException | PacketOverflowException | InvalidBlockNumberException e) {
            e.printStackTrace();
        }
    }

    private void sendPacketToClient(TFTPPacket tftpPacket) {
        if (allowTransfers) {
            //Send packet to client
            DatagramPacket sendPacket = new DatagramPacket(tftpPacket.getByteArray(), tftpPacket.getByteArray().length,
                    packetFromClient.getAddress(), packetFromClient.getPort());
            //printing out information about the packet
            System.out.println("Server: Sending packet");
            if (verbose) {
                System.out.println("To host: " + sendPacket.getAddress());
                System.out.println("Destination host port: " + sendPacket.getPort());
                int length = sendPacket.getLength();
                System.out.println("Length: " + length);
                System.out.println("Byte Array: " + TFTPPacket.toString(sendPacket.getData()));
            }
            try {
                sendReceiveSocket.send(sendPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (tftpPacket instanceof ErrorPacket) {
                endTransfer();
            }
        }
    }

    private void endTransfer() {
        if (verbose) {
            System.out.println("Closing socket");
        }
        allowTransfers = false;
        try {
            if(tftpWriter!=null){
                tftpWriter.closeHandle();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (allowTransfers) {
            sendAndReceive();
            try {
                //Receive packet
                if (allowTransfers) {
                    packetFromClient = new DatagramPacket(dataBuffer, dataBuffer.length);
                    sendReceiveSocket.receive(packetFromClient);
                }
            } catch (SocketTimeoutException e) {
                if (verbose) {
                    System.out.println("Client took too long to respond");
                }
                endTransfer();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        sendReceiveSocket.close();
        if (verbose) {
            if (sendReceiveSocket.isClosed()) {
                System.out.println("Closed socket");
            }
        }
    }
}