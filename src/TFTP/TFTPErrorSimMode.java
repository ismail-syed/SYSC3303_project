package TFTP;

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
	private ErrorSimTransferMode transferMode;
	private ErrorSimDuplicatePacketType duplicatePacketType;
	private int packetNumer;
	private int delayLength;

	// The state which the Simulation run in
	protected enum ErrorSimState {
		NORMAL, LOST_PACKET, DELAY_PACKET, DUPLICATE_PACKET
	};

	// Operation mode for the Error Sim
	protected enum ErrorSimTransferMode {
		RRQ, WRQ
	};

	protected enum ErrorSimDuplicatePacketType {
		DATA, ACK, REQUEST;
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
	public TFTPErrorSimMode(ErrorSimState state, ErrorSimTransferMode mode,  int packetNum, int delayLen, ErrorSimDuplicatePacketType packetType) {
		this.simState = state;
		this.transferMode = mode;
		this.packetNumer = packetNum;
		this.delayLength = delayLen;
		this.duplicatePacketType = packetType;
	}
	
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
	
	public ErrorSimDuplicatePacketType getduplicatePacketType() {
		return duplicatePacketType;
	}

}
