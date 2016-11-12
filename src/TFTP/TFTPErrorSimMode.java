package TFTP;

import TFTPPackets.TFTPPacket.Opcode;

/**
 * The {TFTPErrorSimMode} class manages the state of the Error Simulation
 * handling modes
 * 
 * @author Team 3000000
 * @since 3.0
 * @version 1.0
 */

public class TFTPErrorSimMode {

	private ErrorSimState simState;
	private Opcode packetType;
	private int packetNumer;
	private int delayLength;

	// The state which the Simulation run in
	protected enum ErrorSimState {
		NORMAL, LOST_PACKET, DELAY_PACKET, DUPLICATE_PACKET
	};


	protected enum ErrorSimPacketType {
		DATA, ACK, RRQ, WRQ;
	}
	
	/**
	 * 
	 * @param state
	 *            Either one of the 4 possible simulator states
	 * @param mode
	 *            Either one of the 2 possible transfer modes
	 * @param packetNum
	 *            The packet on which the error simulator operates on
	 * @param delayLen
	 *            Only used in DELAY_PACKET error mode. Specifies the time (in
	 *            ms) by which the transfer of a packet is delayed
	 *            
	 * @param packetType
	 *            Duplicate packet type specified for ErrorSimState.DUPLICATE_PACKET 
	 *            
	 */
	public TFTPErrorSimMode(ErrorSimState state,  Opcode packetType, int packetNum, int delayLen) {
		this.simState = state;
		this.packetNumer = packetNum;
		this.delayLength = delayLen;
		this.packetType = packetType;
	}
	
	public ErrorSimState getSimState() {
		return simState;
	}

	public int getPacketNumer() {
		return packetNumer;
	}

	public int getDelayLength() {
		return delayLength;
	}
	
	public Opcode getPacketType() {
		return packetType;
	}

}
