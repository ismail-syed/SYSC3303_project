package TFTPSim;

import java.net.DatagramPacket;

import TFTPPackets.TFTPPacket;
import TFTPPackets.TFTPPacket.Opcode;
import TFTPSim.TFTPErrorSimMode.ErrorSimState;

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
	
	// Helper method to check if the errSimState requires an
	// DATA and ACK selection prompt
	public static boolean requiresDataOrAckPrompt(ErrorSimState errSimState){
		return (errSimState == ErrorSimState.DATA_OR_ACK_MISSING_BLOCK_NUMBER || 
				errSimState == ErrorSimState.DATA_OR_ACK_INVALID_BLOCK_NUMBER ||
				errSimState == ErrorSimState.DATA_MISSING_DATA );
	}
	
	// Helper method to check if the errSimState requires an
	// a prompt for WRQ, RRQ, DATA, ACK or ERROR
	public static boolean requiresAllTypePrompts(ErrorSimState errSimState){
		return (errSimState == ErrorSimState.INVALID_OPCODE || 
				errSimState == ErrorSimState.EXTRA_DATA_AT_END );
	}
	
	// Helper method to check if the simState is in a ERROR packet type mode
	public boolean isInvalidErrorPacketType(){
		return (simState == ErrorSimState.ERROR_INVALID_ERROR_CODE || 
				simState == ErrorSimState.ERROR_MISSING_ERROR_CODE ||
				simState == ErrorSimState.ERROR_MISSING_ERROR_MESSAGE ||
				simState == ErrorSimState.ERROR_MISSING_ZERO );
	}
	
	// Helper method to if state is a valid ERROR 4 type
	public boolean isInvalidPacketType(){
		return (simState != ErrorSimState.NORMAL || 
				simState != ErrorSimState.LOST_PACKET ||
				simState != ErrorSimState.DELAY_PACKET ||
				simState != ErrorSimState.DUPLICATE_PACKET
		);
	}
	
	// Helper method to check if the simState is in a mode
	// that generates an invalid packet (RRQ/WRQ) packet
	public boolean isInvalidPacketTypeRequest(){
		return (simState == ErrorSimState.RQ_MISSING_FILENAME || 
				simState == ErrorSimState.RQ_MISSING_FIRST_ZERO ||
				simState == ErrorSimState.RQ_MISSING_MODE ||
				simState == ErrorSimState.RQ_INVALID_MODE ||
				simState == ErrorSimState.RQ_MISSING_SECOND_ZERO
		);
	}
	
	// Helper method to check if the simState is in a mode
	// that generates an ERROR type packet
	private boolean isInvalidPacketErrorType(){
		return (simState == ErrorSimState.ERROR_INVALID_ERROR_CODE ||
				simState == ErrorSimState.ERROR_MISSING_ERROR_CODE ||
				simState == ErrorSimState.ERROR_MISSING_ERROR_MESSAGE ||
				simState == ErrorSimState.ERROR_MISSING_ZERO);
	}
	
	/**
	 * Helper method to check if the simStateToCheck and packet meet the
	 * requirements for the properties saved in the errorSimMode
	 * 
	 * @param simStateToCheck
	 * @param errorSimMode
	 * @param packet
	 * @return
	 */
	public boolean checkPacketToCreateNetworkError(ErrorSimState simStateToCheck, DatagramPacket packet) {
		Opcode currentOpCode = Opcode.asEnum((packet.getData()[1]));
		if (this.simState == simStateToCheck && this.packetType == currentOpCode) {
			if (currentOpCode == Opcode.READ || currentOpCode == Opcode.WRITE)
				return true;

			// Get the current block number by concatenating the two byte values
			// and
			// parsing that String into an Int
			int currentBlockNumber = TFTPPacket.getBlockNumber(packet.getData());
			if (this.packetNumer == currentBlockNumber) {
				return currentBlockNumber == this.packetNumer;
			}
		}
		return false;
	}
	
	public boolean checkPacketToCreateInvalidPacket(ErrorSimState simStateToCheck, DatagramPacket packet){
		if(isInvalidPacketType()){
			
		}
		return false; 
	}
}
