package TFTP;

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
    private Boolean transferFinished = false;
    private DatagramPacket sendPacket;
    private DatagramPacket packetFromClient;
    private DatagramSocket sendReceiveSocket;
    private TFTPReader tftpReader;
    private TFTPWriter tftpWriter;

    private byte dataBuffer[] = new byte[MAX_SIZE];

    public TFTPServerTransferThread(DatagramPacket packetFromClient, String filePath) {
        this.packetFromClient = packetFromClient;
        this.filePath = filePath;
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
            System.out.println("From host: " + packetFromClient.getAddress());
            System.out.println("Host port: " + packetFromClient.getPort());
            int len = packetFromClient.getLength();
            System.out.println("Length: " + len);
            System.out.println("Containing: ");

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
            } else if (opcode == TFTPPacket.Opcode.WRITE) {
                System.out.println("Opcode: WRITE");
                //Parse WRQ packet
                WRQPacket wrqPacket = new WRQPacket(data);
                //Open file
                tftpWriter = new TFTPWriter(new File(filePath + wrqPacket.getFilename()).getPath(), false);
                //Create ACK packet with block number 0
                System.out.println("Sending ACK with block 0");
                tftpPacket = new ACKPacket(0);
            } else if (opcode == TFTPPacket.Opcode.DATA) {
                System.out.println("Opcode: DATA");
                //Parse DATA packet
                DataPacket dataPacket = new DataPacket(data);
                //Write the data from the DATA packet
                tftpWriter.writeToFile(dataPacket.getData());
                //Create an ACK packet for corresponding block number
                tftpPacket = new ACKPacket(dataPacket.getBlockNumber());
                System.out.println("Sending ACK with block " + dataPacket.getBlockNumber());
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
        try {
            sendReceiveSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    //TODO: Add ackPacket check Aritra/Shasthra
    //TODO: quitting Aritra
    //TODO: Verbose Quiet Shasthra
    //TODO: Max block number check Shasthra
    //TODO: Add timeout Aritra
    //TODO: relative directory default Shasthra
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
