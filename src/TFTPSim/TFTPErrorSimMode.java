package TFTPSim;

import java.net.DatagramPacket;

import TFTPPackets.TFTPPacket;
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

	/**
	 * The state of the simulation
	 *
	 */
	public enum ErrorSimState {
		NORMAL, 
		
		//Error 5
		INVALID_TID,
		
		// Network Errors
		LOST_PACKET, 
		DELAY_PACKET, 
		DUPLICATE_PACKET,
		
		// Invalid TFTP Packets
		INVALID_OPCODE,
		EXTRA_DATA_AT_END,
		
		RQ_MISSING_FILENAME,
		RQ_MISSING_FIRST_ZERO,
		RQ_MISSING_MODE,
		RQ_INVALID_MODE,
		RQ_MISSING_SECOND_ZERO,
		
		DATA_OR_ACK_MISSING_BLOCK_NUMBER,
		DATA_OR_ACK_INVALID_BLOCK_NUMBER,
		DATA_MISSING_DATA,
		
		ERROR_INVALID_ERROR_CODE,
		ERROR_MISSING_ERROR_CODE,
		ERROR_MISSING_ERROR_MESSAGE,
		ERROR_MISSING_ZERO;
	};

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
	 *            Duplicate packet type specified for
	 *            ErrorSimState.DUPLICATE_PACKET
	 * 
	 */
	public TFTPErrorSimMode(ErrorSimState state, Opcode packetType, int packetNum, int delayLen) {
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

	public void setSimState(ErrorSimState state) {
		this.simState = state;
	}
	
	// Helper method to if state is a valid ERROR 4 type
	public boolean isInvalidPacketType(){
		return (simState != ErrorSimState.NORMAL && 
				simState != ErrorSimState.LOST_PACKET &&
				simState != ErrorSimState.DELAY_PACKET &&
				simState != ErrorSimState.DUPLICATE_PACKET
		);
	}
	
	/**
	 * Helper method to check if the simStateToCheck and packet meet the
	 * requirements for the properties saved in the errorSimMode
	 * 
	 * @param simStateToCheck
	 * @param packet
	 * @return
	 */
	public boolean checkPacketToCreateError(ErrorSimState simStateToCheck, DatagramPacket packet) {
		Opcode currentOpCode = Opcode.asEnum((packet.getData()[1]));
		int currentBlockNumber = TFTPPacket.getBlockNumber(packet.getData());
		
		// Ensure that the simState is the state that we are comparing
		// and ensure the packet passed in contains the opcode that we are comparing
		if(this.simState == simStateToCheck && this.packetType == currentOpCode){
			if (currentOpCode == Opcode.READ || currentOpCode == Opcode.WRITE){
				return true;
			}
			if(this.packetNumer == currentBlockNumber) {
				return (this.packetType == currentOpCode && this.packetNumer == currentBlockNumber);
			}
			
		}
		return false;
	}
}
