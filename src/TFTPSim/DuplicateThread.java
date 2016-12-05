package TFTPSim;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

import TFTPPackets.TFTPPacket;

public class DuplicateThread implements Runnable {

	private DatagramSocket receiveSocket, sendSocket;
	private DatagramPacket receivePacket, sendPacket;
	private int sendPort;
	private byte[] data;
	
	public DuplicateThread(DatagramSocket receiveSocket, DatagramSocket sendSocket, int sendPort) {
		this.receiveSocket = receiveSocket;
		this.sendSocket = sendSocket;
		this.sendPort = sendPort;
	}

	@Override
	public void run() {
		System.out.println("\nDuplicate Thread: Waiting for packet...\n");
		data = new byte[516];
		receivePacket = new DatagramPacket(data, data.length);
		
		try {
			receiveSocket.receive(receivePacket);
			System.out.println("duplicate thread received something\n");
			printVerboseReceive(receivePacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		sendPacket = new DatagramPacket(data, receivePacket.getLength(), receivePacket.getAddress(), sendPort);
        try {
        	System.out.println("\nDuplicate Thread: Sending packet...\n");
        	sendSocket.send(sendPacket);
        	printVerbose(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	synchronized private void printVerboseReceive(DatagramPacket receivePacket) {
		System.out.println("\nDuplicate Thread: Simulator Invalid TID Thread: Packet received");
		System.out.println("Duplicate Thread: From host: " + receivePacket.getAddress());
		System.out.println("Duplicate Thread: Length: " + receivePacket.getLength());
		System.out.println("Duplicate Thread: Containing: ");
		System.out.println("Duplicate Thread Byte Array: " + TFTPPacket.toString(Arrays.copyOfRange(receivePacket.getData(), 0, receivePacket.getLength())) + "\n");
	}
	
	synchronized private void printVerbose(DatagramPacket packet){
		System.out.println("\nDuplicate Thread: Simulator Invalid TID Thread: Packet sent");
		System.out.println("Duplicate Thread: To host: " + packet.getAddress());
		System.out.println("Duplicate Thread: Destination host port: " + packet.getPort());
		System.out.println("Duplicate Thread: Length: " + packet.getLength());
		System.out.println("Duplicate Thread Byte Array: " + TFTPPacket.toString(Arrays.copyOfRange(packet.getData(), 0, packet.getLength())) + "\n");
	}

}
