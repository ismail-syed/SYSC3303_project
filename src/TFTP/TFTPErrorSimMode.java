package TFTP;

/**
 * The {@link TFTPErrorSimMode} class manages the state of the Error Simulation handling modes.
 * 
 * @author Team 3000000
 * @author Ismail Syed
 * @version 1.0
 */

public class TFTPErrorSimMode {
	
	// The state which the Simulation run in 
	public enum ErrorSimState { NORMAL, LOST_PACKET, DELAY_PACKET, DUPLICATE_PACKET };
	
	// Operation mode for the Error Sim
	public enum ErrorSimTransferMode { RRQ, WRQ };
	
	// The required properties to manage the Error simulator
	private ErrorSimState simState;
	private ErrorSimTransferMode transferMode;
	private int packetNumer;
	private int delayLength; 
	
	/*
	 * CONSTRUCTOR
	 * Create the Error sim mode which is passed into the TFTPSim.passOnTFTP()
	 * state: state to run the error sim in (NORMAL, LOSE_PACKET, DELAY_PACKET, DUPLICATE_PACKET)
	 * mode: the operation mode for the error sim (RRQ, WRQ)
	 * packetNum: the packet number for which we want the error sim to operate on 
	 * delayLen: The delay length for the delay of packets in the DELAY_PACKET mode
	 */
	public TFTPErrorSimMode(ErrorSimState state, ErrorSimTransferMode mode, int packetNum, int delayLen){
		this.simState = state; 
		this.transferMode = mode;
		this.packetNumer = packetNum; 
		this.delayLength = delayLen;
	}

	/*
	 * GETTERS
	 */
	
	public ErrorSimState getSimState() {
		return simState;
	}	
	
	public ErrorSimTransferMode getTransferMode() {
		return transferMode;
	}	

	public int getPacketNumer() {
		return packetNumer;
	}

	public int getDelayLength() {
		return delayLength;
	}

}
