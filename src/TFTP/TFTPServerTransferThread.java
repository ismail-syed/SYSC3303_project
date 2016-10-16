package TFTP;

import java.net.SocketTimeoutException;

import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;

import java.nio.file.NoSuchFileException;
import java.util.*;
import java.util.regex.Pattern;

import Exceptions.InvalidBlockNumberException;
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

import static TFTPPackets.TFTPPacket.MAX_SIZE;

/**
 * The {@link TFTPServerTransferThread} class represents a thread which is started
 * by the {@link TFTPServer} to send or receive files from the client
 * (based on Assignment 1 solution)
 *
 * @author Team 3000000
 * @author Aritra Sengupta
 * @author Shasthra Ranasinghe
 * @version 1.0
 */

public class TFTPServerTransferThread implements Runnable {

    private String filePath;
    private boolean verbose; //verbose or quiet
    private int previousBlockNumber; // keeps track of the block numbers to ensure blocks received are in order
    private Boolean transferFinished = false;
    private DatagramPacket sendPacket;
    private DatagramPacket packetFromClient;
    private DatagramSocket sendReceiveSocket;
    private TFTPReader tftpReader;
    private TFTPWriter tftpWriter;
    private byte dataBuffer[] = new byte[MAX_SIZE];

    public TFTPServerTransferThread(DatagramPacket packetFromClient, String filePath, boolean verbose) {
        this.packetFromClient = packetFromClient;
        this.filePath = filePath;
        this.verbose = verbose;
        try {
            sendReceiveSocket = new DatagramSocket();
            //set a timeout of 10s
            sendReceiveSocket.setSoTimeout(10000);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void sendAndReceive() {
        try {
            //Create byte array of proper size
            byte[] data = new byte[packetFromClient.getLength()];
            System.arraycopy(packetFromClient.getData(), 0, data, 0, data.length);

            //Process the received datagram.
            System.out.println("\nServer: Packet received:");
            if(verbose){
                System.out.println("From host: " + packetFromClient.getAddress());
                System.out.println("Host port: " + packetFromClient.getPort());
                int len = packetFromClient.getLength();
                System.out.println("Length: " + len);
                System.out.println("Containing: ");
                System.out.println(new String(Arrays.copyOfRange(data,0,len)));
                System.out.println("Byte Array: " + TFTPPacket.toString(Arrays.copyOfRange(data,0,len))+"\n");
            }
            //Get opcode
            TFTPPacket.Opcode opcode = TFTPPacket.Opcode.asEnum((packetFromClient.getData()[1]));
            //Initialize the TFTPPacket that will hold the response going to the client
            TFTPPacket tftpPacket = new TFTPPacket();

            if (opcode == TFTPPacket.Opcode.READ) {
                System.out.println("Opcode: READ");
                //Parse RRQ packet
                RRQPacket rrqPacket = new RRQPacket(data);
                //Read from File
                try{
                    tftpReader = new TFTPReader(new File(filePath + rrqPacket.getFilename()).getPath());
                    //Create DATA packet with first block of file
                    System.out.println("Sending block 1");
                    tftpPacket = new DataPacket(1, tftpReader.getFileBlock(1));
                    previousBlockNumber = 1;
                } catch (NoSuchFileException e) {
                    tftpPacket = new ErrorPacket(ErrorPacket.ErrorCode.FILE_NOT_FOUND, "file not found");
                    System.out.println("file not found");
                } catch (FileNotFoundException e) {
                	tftpPacket = new ErrorPacket(ErrorPacket.ErrorCode.FILE_NOT_FOUND, "File not found");
                	System.out.println("file not found");
                } catch (AccessDeniedException e) {
                	tftpPacket = new ErrorPacket(ErrorPacket.ErrorCode.ACCESS_VIOLATION, "Access violation");
                	System.out.println("Access Violation");
                }
            } else if (opcode == TFTPPacket.Opcode.WRITE) {
                System.out.println("Opcode: WRITE");
                //Parse WRQ packet
                WRQPacket wrqPacket = new WRQPacket(data);
                //Open file
                try{
                	File file = new File(filePath + wrqPacket.getFilename());
                	if(file.exists()) {
                        // handle file output already exists
                        throw new FileAlreadyExistsException("File already exist");
                    }
                	tftpWriter = new TFTPWriter(file.getPath(), false);
                	
                    //Create ACK packet with block number 0
                    System.out.println("Sending ACK with block 0");
                    tftpPacket = new ACKPacket(0);
                    previousBlockNumber = 0;
                } catch ( FileAlreadyExistsException e) {
                    tftpPacket = new ErrorPacket(ErrorCode.FILE_ALREADY_EXISTS, "File already exists");
                    System.out.println("File already Exist");
                } catch ( FileSystemException e) {
                        tftpPacket = new ErrorPacket(ErrorCode.DISC_FULL_OR_ALLOCATION_EXCEEDED, "Disk full");
                        System.out.println("Disc full");
                }
                
            } else if (opcode == TFTPPacket.Opcode.DATA) {
                System.out.println("Opcode: DATA");
                try{
                	//Parse DATA packet
                	DataPacket dataPacket = new DataPacket(data);
	                //Write the data from the DATA packet
	                if(dataPacket.getBlockNumber() != previousBlockNumber + 1){
	                	throw new InvalidBlockNumberException("Data is out of order");
	                }
	                tftpWriter.writeToFile(dataPacket.getData());
	                previousBlockNumber = dataPacket.getBlockNumber();
	                //Create an ACK packet for corresponding block number
	                tftpPacket = new ACKPacket(previousBlockNumber);
	                System.out.println("Sending ACK with block " + previousBlockNumber);
	                if (dataPacket.getData().length < DataPacket.MAX_DATA_SIZE) {
	                    //transfer finished for WRQ
	                    sendPacketToClient(tftpPacket);
	                    tftpWriter.closeHandle();
	                    transferFinished = true;
	                }
                } catch ( IOException e) {
                        tftpPacket = new ErrorPacket(ErrorCode.DISC_FULL_OR_ALLOCATION_EXCEEDED, "Disk full");
                        System.out.println("Disc full");
                }

            } else if (opcode == TFTPPacket.Opcode.DATA) {
                System.out.println("Opcode: DATA");
                try{
                    //Parse DATA packet
                    DataPacket dataPacket = new DataPacket(data);
                    //Write the data from the DATA packet
                    if(dataPacket.getBlockNumber() != previousBlockNumber + 1){
                        throw new InvalidBlockNumberException("Data is out of order");
                    }
                    tftpWriter.writeToFile(dataPacket.getData());
                    previousBlockNumber = dataPacket.getBlockNumber();
                    //Create an ACK packet for corresponding block number
                    tftpPacket = new ACKPacket(previousBlockNumber);
                    System.out.println("Sending ACK with block " + previousBlockNumber);
                    if (dataPacket.getData().length < DataPacket.MAX_DATA_SIZE) {
                        //transfer finished for WRQ
                        sendPacketToClient(tftpPacket);
                        tftpWriter.closeHandle();
                        transferFinished = true;
                    }
                } catch ( IOException e) {
                    if(NO_SPACE_LEFT.matcher(e.getMessage()).find()){
                        tftpPacket = new ErrorPacket(ErrorCode.DISC_FULL_OR_ALLOCATION_EXCEEDED, "Disk full");
                        System.out.println("Disc full");
                    }
                }
            } else if (opcode == TFTPPacket.Opcode.ACK) {
                System.out.println("Opcode: ACK");
                //Parse ACK packet
                ACKPacket ackPacket = new ACKPacket(data);
                if(ackPacket.getBlockNumber() != previousBlockNumber){
                    throw new InvalidBlockNumberException("Data is out of order");
                }
                previousBlockNumber = ackPacket.getBlockNumber()+1;
                //Send next block of file until there are no more blocks
                if (ackPacket.getBlockNumber() < tftpReader.getNumberOfBlocks()) {
                    tftpPacket = new DataPacket(previousBlockNumber, tftpReader.getFileBlock(previousBlockNumber));
                    System.out.println("Sending DATA with block " + (previousBlockNumber));
                }
                if(ackPacket.getBlockNumber()==tftpReader.getNumberOfBlocks()) transferFinished = true;
            }

            if (!transferFinished) {
                sendPacketToClient(tftpPacket);
                if(tftpPacket instanceof ErrorPacket) transferFinished = true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendPacketToClient(TFTPPacket tftpPacket) {
        //Send packet to client
        sendPacket = new DatagramPacket(tftpPacket.getByteArray(), tftpPacket.getByteArray().length,
                packetFromClient.getAddress(), packetFromClient.getPort());
        //printing out information about the packet
        System.out.println( "Server: Sending packet");
        if(verbose){
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
    }

    @Override
    public void run() {
        while (!transferFinished) {
            sendAndReceive();
            try {
                //Receive packet
                if (!transferFinished) {
                    packetFromClient = new DatagramPacket(dataBuffer, dataBuffer.length);
                    sendReceiveSocket.receive(packetFromClient);
                }
            }
            catch (SocketTimeoutException e) {
                if(verbose){
                    System.out.println("Client took too long to respond");
                }
                transferFinished = true;
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(verbose){
            System.out.println("Closing socket");
        }
        sendReceiveSocket.close();
    }
}