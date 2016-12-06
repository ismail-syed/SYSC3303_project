package TFTPSim;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

import TFTPPackets.TFTPPacket;

public class ErrorSimDelayThread implements Runnable{
	
	private DatagramSocket sendSocket;
	private DatagramPacket sendPacket;
	private int delayLength;	

	/**
	 * Receives the delay time from ErrorSim
	 * 
	 * @param delayLength
	 * @param data
	 * @param errS
	 */
	public ErrorSimDelayThread(DatagramSocket sendSocket, DatagramPacket sendPacket, int delay) {
		this.sendSocket = sendSocket; 
		this.sendPacket = sendPacket;
		this.delayLength = delay;
	}
	
	@Override
	public void run() {
		try{
			Thread.sleep(delayLength);
			sendSocket.send(sendPacket);
			printSendPacketInfo(sendPacket);
		} catch(InterruptedException e){
			System.out.println("Error occured while trying to delay packet.\n");
		} catch (IOException e) {
			System.out.println("Error occured while trying to send the delayed packet.\n");
		}	
	}

	private void printSendPacketInfo(DatagramPacket packet) {
		synchronized(System.out){
			System.out.println("**************************************************************************\n");
			System.out.println("Delayed Thread:Sent packet.");
			System.out.println("Delayed Thread: To host: " + packet.getAddress());
			System.out.println("Delayed Thread: Destination host port: " + packet.getPort());
			System.out.println("Delayed Thread: Length: " + packet.getLength());
			System.out.println("Delayed Thread: Byte Array: " + TFTPPacket.toString(Arrays.copyOfRange(sendPacket.getData(), 0, packet.getLength())) + "\n");
			System.out.println("DELAYED PACKET SENT: After waiting " + delayLength + "ms\n");
			System.out.println("**************************************************************************\n");
		}
	}

}
