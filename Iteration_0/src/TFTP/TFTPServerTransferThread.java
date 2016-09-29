package TFTP;

import java.util.*;

import Exceptions.InvalidBlockNumberException;
import FileIO.TFTPReader;
import FileIO.TFTPWriter;
import TFTPPackets.*;

import java.io.File;
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
    private boolean verbose;
    private int previousBlockNumber;
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
            	System.out.println("Byte Array: " + Arrays.toString(Arrays.copyOfRange(data,0,len))+"\n");
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
                tftpReader = new TFTPReader(new File(filePath + rrqPacket.getFilename()).getPath());
                //Create DATA packet with first block of file
                System.out.println("Sending block 1");
                tftpPacket = new DataPacket(1, tftpReader.getFileBlock(1));
                previousBlockNumber = 1;
            } else if (opcode == TFTPPacket.Opcode.WRITE) {
                System.out.println("Opcode: WRITE");
                //Parse WRQ packet
                WRQPacket wrqPacket = new WRQPacket(data);
                //Open file
                tftpWriter = new TFTPWriter(new File(filePath + wrqPacket.getFilename()).getPath(), false);
                //Create ACK packet with block number 0
                System.out.println("Sending ACK with block 0");
                tftpPacket = new ACKPacket(0);
                previousBlockNumber = 0;
            } else if (opcode == TFTPPacket.Opcode.DATA) {
                System.out.println("Opcode: DATA");
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
            } else if (opcode == TFTPPacket.Opcode.ACK) {
                System.out.println("Opcode: ACK");
                //Parse ACK packet
                ACKPacket ackPacket = new ACKPacket(data);
                if(ackPacket.getBlockNumber() != previousBlockNumber + 1){
                	throw new InvalidBlockNumberException("Data is out of order");
                }
                //Send next block of file until there are no more blocks
                if (ackPacket.getBlockNumber() <= tftpReader.getNumberOfBlocks()) {
                    tftpPacket = new DataPacket(ackPacket.getBlockNumber() + 1, tftpReader.getFileBlock(ackPacket.getBlockNumber() + 1));
                }
                System.out.println("Sending DATA with block " + (ackPacket.getBlockNumber() + 1));

                if (ackPacket.getBlockNumber() == tftpReader.getNumberOfBlocks()) {
                    //transfer finished for RRQ;
                    sendPacketToClient(tftpPacket);
                    transferFinished = true;
                }
            }

            try {
                sendReceiveSocket = new DatagramSocket();
            } catch (SocketException e) {
                e.printStackTrace();
            }

            if (!transferFinished) {
                sendPacketToClient(tftpPacket);
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
        	System.out.println("Byte Array: " + Arrays.toString(sendPacket.getData()));
        }
        try {
            sendReceiveSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    //TODO: Add ackPacket check: DONE
    //TODO: quitting Aritra
    //TODO: Verbose Quiet: DONE
    //TODO: Max block number check: DONE
    //TODO: Add timeout Aritra
    //TODO: relative directory default: DONE
    public void run() {
        while (!transferFinished) {
            sendAndReceive();
            try {
                //Receive packet
                if (!transferFinished) {
                    packetFromClient = new DatagramPacket(dataBuffer, dataBuffer.length);
                    sendReceiveSocket.receive(packetFromClient);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
