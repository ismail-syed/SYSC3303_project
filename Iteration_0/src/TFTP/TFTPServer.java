package TFTP; /**
 * The {@link TFTP.TFTPServer} class represents a TFTP Server
 * (based on Assignment 1 solution)
 * @author Team 3000000
 * @version 1.0
 */
import FileIO.TFTPReader;
import TFTPPackets.ACKPacket;
import TFTPPackets.DataPacket;
import TFTPPackets.RRQPacket;

import java.io.*;
import java.net.*;

import static TFTPPackets.TFTPPacket.*;

public class TFTPServer {
    // UDP datagram packets and sockets used to send / receive
    private DatagramPacket sendPacket, receivePacket;
    private DatagramSocket receiveSocket, sendSocket;

    public TFTPServer()
    {
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

    private TFTPReader tftpReader = null;

    /**
     * This method can handle RRQ and ACK packets from the client
     * TODO: Add the ability to handle WRQ packets and data packets
     * @author Aritra Sengupta
     * @version 1.0
     */
    private void receivePacketFromClient(){
        byte dataBuffer[] = new byte[MAX_SIZE];
        byte[] data = null;
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
            System.out.println("\nServer: Received Packet.");

            // Process the received datagram.
            System.out.println("Server: Packet received:");
            System.out.println("From host: " + receivePacket.getAddress());
            System.out.println("Host port: " + receivePacket.getPort());
            int len = receivePacket.getLength();
            System.out.println("Length: " + len);
            System.out.println("Containing: ");

            //Get opcode
            Opcode opcode = Opcode.asEnum((int) data[1]);

            if(opcode == Opcode.READ){
                System.out.println("Opcode: READ");
                //Parse RRQ
                RRQPacket rrqPacket = new RRQPacket(data);
                //Read from File
                tftpReader = new TFTPReader(new File(".").getCanonicalPath()+ "\\Server\\" + rrqPacket.getFilename());
                //send first block of file
                DataPacket dataPacket = new DataPacket(1, tftpReader.getFileBlock(1));
                sendPacket = new DatagramPacket(dataPacket.getByteArray(), dataPacket.getByteArray().length,
                    receivePacket.getAddress(), receivePacket.getPort());
                sendSocket.send(sendPacket);
            }
            else if(opcode == Opcode.WRITE){
                System.out.println("Opcode: WRITE");
            }
            else if(opcode == Opcode.DATA){
                System.out.println("Opcode: DATA");
            }
            else if(opcode == Opcode.ACK){
                System.out.println("Opcode: ACK");
                ACKPacket ackPacket = new ACKPacket(data);
                //send next block of file until there are no more blocks
                if(ackPacket.getBlockNumber() != tftpReader.getNumberOfBlocks()){
                    DataPacket dataPacket = new DataPacket(ackPacket.getBlockNumber() + 1, tftpReader.getFileBlock(ackPacket.getBlockNumber() + 1));
                    sendPacket = new DatagramPacket(dataPacket.getByteArray(), dataPacket.getByteArray().length,
                    receivePacket.getAddress(), receivePacket.getPort());
                    sendSocket.send(sendPacket);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main( String args[] ) throws Exception
    {
        TFTPServer c = new TFTPServer();
        //loop for ever
        for(;;){
            c.receivePacketFromClient();
        }
    }
}