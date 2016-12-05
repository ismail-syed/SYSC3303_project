package TFTPSim;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

import TFTPPackets.TFTPPacket;

public class ErrorSimDelayThread implements Runnable{
	
	private DatagramSocket sendSocket;
	private DatagramPacket sendPacket;
	private int delayLength, sendPort;	

	/**
	 * Receives the delay time from ErrorSim
	 * 
	 * @param delayLength
	 * @param data
	 * @param errS
	 */
	public ErrorSimDelayThread(DatagramSocket sendSocket, DatagramPacket sendPacket, int port, int delay) {
		this.sendSocket = sendSocket; 
		this.sendPacket = sendPacket;
		this.delayLength = delay;
		this.sendPort = port;
	}
	
	@Override
	public void run() {
		try{
			Thread.sleep(delayLength);
			sendSocket.send(sendPacket);
			printSendPacketInfo(sendPacket, sendPacket.getData());
		} catch(InterruptedException e){
			System.out.println("Error occured while trying to delay packet.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
		System.out.print("DELAYED PACKET SENT: After waiting " + delayLength + "ms\n");
	}

	private void printSendPacketInfo(DatagramPacket packet, byte[] data) {
		System.out.println("Simulator: Sent packet.");
		System.out.println("To host: " + packet.getAddress());
		System.out.println("Destination host port: " + packet.getPort());
		System.out.println("Length: " + packet.getLength());
		System.out
				.println("Byte Array: " + TFTPPacket.toString(Arrays.copyOfRange(data, 0, packet.getLength())) + "\n");
	}

}
