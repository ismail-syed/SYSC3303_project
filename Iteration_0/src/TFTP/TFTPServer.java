package TFTP;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Scanner;

import static TFTPPackets.TFTPPacket.MAX_SIZE;

/**
 * The {@link TFTP.TFTPServer} class represents a TFTP Server
 * (based on Assignment 1 solution)
 *
 * @author Team 3000000
 * @author Aritra Sengupta
 * @author Shasthra Ranasinghe
 * @version 1.0
 */

public class TFTPServer implements Runnable {
    // UDP datagram packets and sockets used to send / receive
    private DatagramPacket sendPacket, receivePacket;
    private DatagramSocket receiveSocket, sendSocket;
    private static String filePath;
    private static boolean verbose;
    private static volatile boolean acceptingNewConnections;
    private static ArrayList<Thread> transferThreadsList = new ArrayList<>();
    private TFTPServer() {
        try {
            // Construct a datagram socket and bind it to port 69
            // on the local host machine. This socket will be used to
            // receive UDP Datagram packets.
            receiveSocket = new DatagramSocket(69);
            acceptingNewConnections = true;
        } catch (SocketException se) {
            se.printStackTrace();
            System.exit(1);
        }
    }


    /**
     * This method sends or receives files from the client
     *
     * @since 1.0
     */
    private void receivePacketFromClient() {
        byte dataBuffer[] = new byte[MAX_SIZE];
        receivePacket = new DatagramPacket(dataBuffer, dataBuffer.length);
        try {
            receiveSocket.receive(receivePacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Thread fileTransferThread = new Thread(new TFTPServerTransferThread(receivePacket, filePath, verbose));
        transferThreadsList.add(fileTransferThread);
        fileTransferThread.start();
    }

    public static void main(String args[]) throws Exception {
        //Requests the user to input a filepath for the directory you want to work with
        Scanner in = new Scanner(System.in);
        //request the user for a path
        System.out.println("Enter the Directory Path:");
        System.out.println("Type \"DEFAULT\" to use the relative director or Enter the filepath of the directory");
        for(;;){
            filePath = in.nextLine();
            if(filePath.equals("DEFAULT")){
                //if default print the dir and finish
                System.out.println("You are now in: " + System.getProperty("user.dir"));
                filePath = "";
                break;
            }else{
                if(new File (filePath).isDirectory()){
                    //is the path was provided finish
                    filePath += "\\";
                    System.out.println("You have entered a valid Directory Path\n");
                    break;
                }else{
                    //if the directory does not exist, ask for an input again
                    System.out.println("Invalid Directory\nPlease Try Again.");
                }
            }
        }
        String userInput;
        for(;;){
            //request user for verbose or quiet mode
            System.out.println("Verbose(Y/N)?");
            userInput = in.nextLine();
            if(userInput.equals("Y")){
                verbose = true;
                System.out.println("You have chosen Verbose mode");
                break;
            }else if(userInput.equals("N")){
                verbose = false;
                System.out.println("You have chosen Quiet mode");
                break;
            }//if input is invalid, ask again
        }

        //Start the main program
        Thread serverSocketListeningThread = new Thread(new TFTPServer());
        serverSocketListeningThread.start();
        for(;;){
            System.out.println("Type \"QUIT\" to quit the server");
            userInput = in.nextLine();
            if(userInput.equals("QUIT")){
                acceptingNewConnections = false;
                in.close();
                System.out.println("Waiting for current file transfers to finish");
                for (Thread aTransferThreadsList : transferThreadsList) {
                    aTransferThreadsList.join();
                }
                System.out.println("Shutting down server");
                System.exit(0);
                in.close();
                break;
            }
        }
    }

    @Override
    public void run() {
        //Loop forever
        for (; ; ) {
            if(acceptingNewConnections)
                this.receivePacketFromClient();
        }
    }
}
