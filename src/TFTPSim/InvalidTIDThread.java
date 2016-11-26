package TFTPSim;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;

import TFTPPackets.TFTPPacket;
import TFTPPackets.TFTPPacket.Opcode;

public class InvalidTIDThread implements Runnable{

	private DatagramPacket packet, receivePacket;
	private DatagramSocket sendReceiveSocket, clientSocket;
	private byte[] data;
	private boolean printSpecialError = false;

	
	/**
	 * Receives the delay time from ErrorSim
	 * @param clientPort 
	 * 
	 * @param delayLength
	 * @param data
	 * @param errS
	 */
	public InvalidTIDThread(DatagramPacket dataPacket) {
		this.packet = dataPacket;
		if(Opcode.asEnum((packet.getData()[1])) == Opcode.DATA || Opcode.asEnum((packet.getData()[1])) == Opcode.ACK){
			printSpecialError = true;
		}
		try {
			clientSocket = new DatagramSocket();
			sendReceiveSocket = new DatagramSocket();
			System.out.println("Creating Thread for invalid TID with port " + sendReceiveSocket.getPort() + "... \n");
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
        //printing out information about the packet
		printVerbose(packet);
        try {
        	System.out.println("\nSimulator Invalid TID Thread: Sending packet to server...\n");
            sendReceiveSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        
        System.out.println("\nSimulator Invalid TID Thread: Waiting for packet from server...\n");
		data = new byte[516];
		receivePacket = new DatagramPacket(data, data.length);
		
		try {
			sendReceiveSocket.receive(receivePacket);
			System.out.println("Simulator Invalid TID Thread: Packet received");
			System.out.println("From host: " + receivePacket.getAddress());
			System.out.println("Length: " + receivePacket.getLength());
			System.out.println("Containing: ");
			System.out
					.println("Byte Array: " + TFTPPacket.toString(Arrays.copyOfRange(data, 0, receivePacket.getLength())) + "\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		if(printSpecialError){
			System.out.println("\nThe packet received will be send to Client 2, which will terminate.\n");
			sendReceiveSocket.close();
			clientSocket.close();
			return;
		}
		
		DatagramPacket sendPacket = new DatagramPacket(data, data.length,
                packet.getAddress(), packet.getPort());
		printVerbose(sendPacket);
        try {
        	System.out.println("\nSimulator Invalid TID Thread: Sending packet to Client...\n");
        	clientSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("\nSimulator Invalid TID Thread: Waiting on Client...\n");
		data = new byte[516];
		receivePacket = new DatagramPacket(data, data.length);
		try {
			clientSocket.receive(receivePacket);
			System.out.println("Simulator Invalid TID Thread: Packet received");
			System.out.println("From host: " + receivePacket.getAddress());
			System.out.println("Length: " + receivePacket.getLength());
			System.out.println("Containing: ");
			System.out
					.println("Byte Array: " + TFTPPacket.toString(Arrays.copyOfRange(data, 0, receivePacket.getLength())) + "\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
        
		packet = new DatagramPacket(data, data.length,
				receivePacket.getAddress(), receivePacket.getPort());
		printVerbose(packet);
        try {
        	System.out.println("\nSimulator Invalid TID Thread: Sending packet to Server...\n");
        	sendReceiveSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
		//close
        sendReceiveSocket.close();
        clientSocket.close();
	}
	
	private void printVerbose(DatagramPacket packet){
		System.out.println("To host: " + packet.getAddress());
		System.out.println("Destination host port: " + packet.getPort());
		System.out.println("Length: " + packet.getLength());
		System.out.println("Byte Array: " + TFTPPacket.toString(packet.getData()));
	}

}
