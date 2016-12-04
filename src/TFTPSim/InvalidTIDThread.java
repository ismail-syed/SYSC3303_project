package TFTPSim;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;

import TFTPPackets.TFTPPacket;
import TFTPPackets.TFTPPacket.Opcode;

public class InvalidTIDThread implements Runnable{

	private DatagramPacket sendPacket, receivePacket, initialPacket;
	private DatagramSocket sendReceiveSocket, clientSocket;
	private byte[] data;
	private boolean printSpecialError = false, fileNotFound = false;
	private int previousPort, serverPort;
	private int sleepTime;
	private Opcode currentOpcode;


	public InvalidTIDThread(DatagramPacket dataPacket, int previousPort) {
		initialPacket = dataPacket;
		this.previousPort = previousPort;
		currentOpcode = Opcode.asEnum((dataPacket.getData()[1]));
		if(currentOpcode == Opcode.DATA || currentOpcode == Opcode.ACK){
			printSpecialError = true;
		}else if(currentOpcode == Opcode.WRITE){
			fileNotFound = true;
		}
		
		if(currentOpcode == Opcode.READ || currentOpcode == Opcode.WRITE){
			sleepTime = 10;
		}else{
			sleepTime = 0;
		}
		try {
			clientSocket = new DatagramSocket();
			sendReceiveSocket = new DatagramSocket();
			System.out.println("Creating Thread for invalid TID... \n");
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
        //printing out information about the packet
		sendPacket = new DatagramPacket(initialPacket.getData(), initialPacket.getLength(), initialPacket.getAddress(),
				initialPacket.getPort());
		
		if(currentOpcode == Opcode.ERROR){
			System.out.println("An Error packet will be sent to The client with an invalid TID\n"
					+ "Receiving this means the side that sent it has already shut down\n"
					+ "If the valid TID packet gets to the client first, it will shut down,\n"
					+ "Then the invalid TID packet wont be received\n\n"
					+ "If the invalid TID packet gets there first the client will send an error 5 back,\n"
					+ "Then shut down once the valid TID error gets there\n"
					+ "Therefore we have decided to halt the Error packet here\n"
					+ "because the invalid TID packet will alwasy be send to the client second");
			closeThread();
			return;
		}
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        try {
            sendReceiveSocket.send(sendPacket);
            System.out.println("\nSimulator Invalid TID Thread: Sending packet...\n");
            printVerbose(sendPacket);
            System.out.println("Destination port: " + previousPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        
        System.out.println("\nSimulator Invalid TID Thread: Waiting for packet...\n");
		data = new byte[516];
		receivePacket = new DatagramPacket(data, data.length);
		
		try {
			sendReceiveSocket.receive(receivePacket);
			printVerboseReceive(receivePacket);
			serverPort = receivePacket.getPort();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		if(fileNotFound){
			System.out.println("\nThe File already Exists Therefore the Server Thread has already shut down\n"
					+ "The error packet will be sent to Client 2 or Server 2, which will terminate");
			closeThread();
			return;
		}
			
		if(printSpecialError){
			System.out.println("\nThe packet received will be send to Client 2 or Server 2, which will terminate.\n");
			closeThread();
			return;
		}
		
		sendPacket = new DatagramPacket(data, receivePacket.getLength(), receivePacket.getAddress(), previousPort);
        try {
        	System.out.println("\nSimulator Invalid TID Thread: Sending packet...\n");
        	clientSocket.send(sendPacket);
        	printVerbose(sendPacket);
        	System.out.println("Destination port: " + previousPort);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("\nSimulator Invalid TID Thread: Waiting...\n");
		data = new byte[516];
		receivePacket = new DatagramPacket(data, data.length);
		try {
			clientSocket.receive(receivePacket);
			printVerboseReceive(receivePacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
        
		sendPacket = new DatagramPacket(data, receivePacket.getLength(),receivePacket.getAddress(), serverPort);
        try {
        	System.out.println("\nSimulator Invalid TID Thread: Sending packet...\n");
        	sendReceiveSocket.send(sendPacket);
        	printVerbose(sendPacket);
        	System.out.println("Destination port: " + serverPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
		//close
        closeThread();
	}

	synchronized private void printVerboseReceive(DatagramPacket receivePacket) {
		System.out.println("\nTID Thread: Simulator Invalid TID Thread: Packet received");
		System.out.println("TID Thread: From host: " + receivePacket.getAddress());
		System.out.println("TID Thread: Length: " + receivePacket.getLength());
		System.out.println("TID Thread: Containing: ");
		System.out.println("Byte Array: " + TFTPPacket.toString(Arrays.copyOfRange(receivePacket.getData(), 0, receivePacket.getLength())) + "\n");
	}
	
	synchronized private void printVerbose(DatagramPacket packet){
		System.out.println("\nTID Thread: Simulator Invalid TID Thread: Packet sent");
		System.out.println("TID Thread: To host: " + packet.getAddress());
		System.out.println("TID Thread: Destination host port: " + packet.getPort());
		System.out.println("TID Thread: Length: " + packet.getLength());
		System.out.println("Byte Array: " + TFTPPacket.toString(Arrays.copyOfRange(packet.getData(), 0, packet.getLength())) + "\n");
	}
	
	private void closeThread(){
		sendReceiveSocket.close();
        clientSocket.close();
        System.out.println("\nSimulator Invalid TID Thread: Closing...\n");
	}

}
