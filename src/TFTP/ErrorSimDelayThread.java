package TFTP;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class ErrorSimDelayThread implements Runnable{

	private int delayLength;
	private DatagramPacket packet;
	private DatagramSocket socket;
	private TFTPSim errorSim;
	
	/**
	 * Receives the delay time from ErrorSim
	 * 
	 * @param delayLength
	 * @param data
	 * @param errS
	 */
	public ErrorSimDelayThread(TFTPSim errorSim, DatagramSocket socket, DatagramPacket dataPacket, int delay) {
		this.errorSim = errorSim;
		this.socket = socket; 
		this.packet = dataPacket;
		this.delayLength = delay;
	}
	
	@Override
	public void run() {
		try{
			Thread.sleep(delayLength);
		} catch(InterruptedException e){
			System.out.println("Error occured while trying to delay packet.");
		}
		errorSim.sendPacketThroughSocket(socket, packet);
		System.out.println("DELAYED PACKET SENT: After waiting " + delayLength + "ms");
	}

}
