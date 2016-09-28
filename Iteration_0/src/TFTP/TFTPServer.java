package TFTP;

import FileIO.TFTPReader;
import FileIO.TFTPWriter;
import TFTPPackets.*;

import java.io.*;
import java.net.*;
import java.util.*;

import static TFTPPackets.TFTPPacket.*;

/**
 * The {@link TFTP.TFTPServer} class represents a TFTP Server
 * (based on Assignment 1 solution)
 *
 * @author Team 3000000
 * @author Aritra Sengupta
 * @author Shasthra Ranasinghe
 * @version 1.0
 */

public class TFTPServer {
    // UDP datagram packets and sockets used to send / receive
    private DatagramPacket sendPacket, receivePacket;
    private DatagramSocket receiveSocket, sendSocket;
    private static String filePath;

    private TFTPServer() {
        try {
            // Construct a datagram socket and bind it to port 69
            // on the local host machine. This socket will be used to
            // receive UDP Datagram packets.
            receiveSocket = new DatagramSocket(69);
        } catch (SocketException se) {
            se.printStackTrace();
            System.exit(1);
        }
    }

    private TFTPReader tftpReader;
    private TFTPWriter tftpWriter;

    /**
     * This method sends or receives files from the client
     *
     * @since 1.0
     */
    private void receivePacketFromClient() {
        byte dataBuffer[] = new byte[MAX_SIZE];
        byte[] data;
        try {
            sendSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        receivePacket = new DatagramPacket(dataBuffer, dataBuffer.length);
        try {
            //Receive packet
            receiveSocket.receive(receivePacket);
            //Create byte array of proper size
            data = new byte[receivePacket.getLength()];
            System.arraycopy(dataBuffer, 0, data, 0, data.length);

            //Process the received datagram.
            System.out.println("Server: Packet received:");
            System.out.println("From host: " + receivePacket.getAddress());
            System.out.println("Host port: " + receivePacket.getPort());
            int len = receivePacket.getLength();
            System.out.println("Length: " + len);
            System.out.println("Containing: ");

            //Get opcode
            Opcode opcode = Opcode.asEnum((int) data[1]);
            //Initialize the TFTPPacket that will hold the response going to the client
            TFTPPacket tftpPacket = new TFTPPacket();

            if (opcode == Opcode.READ) {
                System.out.println("Opcode: READ");
                //Parse RRQ packet
                RRQPacket rrqPacket = new RRQPacket(data);
                //Read from File
                tftpReader = new TFTPReader(new File(filePath + rrqPacket.getFilename()).getPath());
                //Create DATA packet with first block of file
                tftpPacket = new DataPacket(1, tftpReader.getFileBlock(1));
            } else if (opcode == Opcode.WRITE) {
                System.out.println("Opcode: WRITE");
                //Parse WRQ packet
                WRQPacket wrqPacket = new WRQPacket(data);
                //Open file
                tftpWriter = new TFTPWriter(new File(filePath + wrqPacket.getFilename()).getPath(), false);
                //Create ACK packet with block number 0
                tftpPacket = new ACKPacket(0);
            } else if (opcode == Opcode.DATA) {
                System.out.println("Opcode: DATA");
                //Parse DATA packet
                DataPacket dataPacket = new DataPacket(data);
                //Write the data from the DATA packet
                tftpWriter.writeToFile(dataPacket.getData());
                //Create an ACK packet for corresponding block number
                tftpPacket = new ACKPacket(dataPacket.getBlockNumber());
            } else if (opcode == Opcode.ACK) {
                System.out.println("Opcode: ACK");
                //Parse ACK packet
                ACKPacket ackPacket = new ACKPacket(data);
                //Send next block of file until there are no more blocks
                if (ackPacket.getBlockNumber() <= tftpReader.getNumberOfBlocks()) {
                    tftpPacket = new DataPacket(ackPacket.getBlockNumber() + 1, tftpReader.getFileBlock(ackPacket.getBlockNumber() + 1));
                }
            }
            //Send packet to client
            sendPacket = new DatagramPacket(tftpPacket.getByteArray(), tftpPacket.getByteArray().length,
                    receivePacket.getAddress(), receivePacket.getPort());
            sendSocket.send(sendPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) throws Exception {
        //Requests the user to input a filepath for the directory you want to work with
        Scanner in = new Scanner(System.in);
        System.out.println("Enter the Directory Path");
        filePath = in.nextLine();
        System.out.println("You have entered a Directory Path");
        filePath += "\\";
        //Start the main program
        TFTPServer c = new TFTPServer();
        //Loop forever
        for (; ; ) {
            c.receivePacketFromClient();
        }
    }
}