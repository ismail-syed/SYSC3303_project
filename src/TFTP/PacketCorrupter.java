package TFTP;

import java.nio.ByteBuffer;
import java.util.Arrays;

import TFTP.TFTPErrorSimMode.ErrorSimState;

public class PacketCorrupter {
	
	private PacketCorrupter(){
		
	}
	
	/**
	 * Corrupts the data given in a given way and returns it
	 * 
	 * @param packetAsByteArray packet as an array of data to be manipulated
	 * @param option what kind of corruption would you like to do
	 * @return the corrupted data array
	 */
	public final static byte[] corruptPacket(byte[] packetAsByteArray, ErrorSimState option){
        byte[] data = null;
		
		switch(option){
		case DATA_MISSING_DATA:
			data = dataMissingData(packetAsByteArray);
			break;
		case DATA_OR_ACK_MISSING_BLOCK_NUMBER:
			data = missingBlockNumber(packetAsByteArray);
			break;
		case DATA_OR_ACK_INVALID_BLOCK_NUMBER:
			data = invalidBlockNumber(packetAsByteArray);
			break;
		case ERROR_INVALID_ERROR_CODE:
			data = invalidErrorCode(packetAsByteArray);
			break;
		case ERROR_MISSING_ERROR_CODE:
			data = missingErrorCode(packetAsByteArray);
			break;
		case ERROR_MISSING_ERROR_MESSAGE:
			data = missingErrorMessage(packetAsByteArray);
			break;
		case ERROR_MISSING_ZERO:
			data = missingLastZero(packetAsByteArray);
			break;
		case EXTRA_DATA_AT_END:
			data = extraData(packetAsByteArray);
			break;
		case INVALID_OPCODE:
			data = invalidOpcode(packetAsByteArray);
			break;
		case RQ_INVALID_MODE:
			data = invalidMode(packetAsByteArray);
			break;
		case RQ_MISSING_FILENAME:
			data = missingFilename(packetAsByteArray);
			break;
		case RQ_MISSING_FIRST_ZERO:
			data = missingFirstZero(packetAsByteArray);
			break;
		case RQ_MISSING_MODE:
			data = missingMode(packetAsByteArray);
			break;
		case RQ_MISSING_SECOND_ZERO:
			data = missingLastZero(packetAsByteArray);
			break;
		default:
			data = packetAsByteArray;
			break;
		}
		
		return data;
	}

	/**
	 * Changed the last zero of Error/RRQ/WRQ  to 1
	 * @param data byteBuffer containing the data
	 * @return the corrupted data array
	 */
	private static byte[] missingLastZero(byte[] data) {
		byte[] array = Arrays.copyOf(data, data.length);
		array[array.length-1] = (byte)1;
		return array;
	}

	/**
	 * Remove the mode from RRQ/WRQ
	 * @param data byteBuffer containing the data
	 * @return the corrupted data array
	 */
	private static byte[] missingMode(byte[] data) {
		int i = zeroPosition(data);
		byte[] slice1 = Arrays.copyOfRange(data, 0, i+1);
		byte[] finalArray = new byte[slice1.length + 1];
		System.arraycopy(slice1, 0, finalArray, 0, slice1.length);
		finalArray[finalArray.length-1] = (byte)0;
		
		return finalArray;
	}

	/**
	 * Change the zero between Filename and Mode to 1
	 * @param data byteBuffer containing the data
	 * @return the corrupted data array
	 */
	private static byte[] missingFirstZero(byte[] data) {
		int i = zeroPosition(data);
		byte[] array = Arrays.copyOf(data, data.length);
		array[i] = (byte)1;
		return array;
	}

	/**
	 * Remove the filename from the data
	 * @param data byteBuffer containing the data
	 * @return the corrupted data array
	 */
	private static byte[] missingFilename(byte[] data) {
		int i = zeroPosition(data);
		byte[] slice1 = Arrays.copyOfRange(data, 0, 2);
		byte[] slice2 = Arrays.copyOfRange(data, i, data.length);
		byte[] finalArray = new byte[slice1.length + slice2.length];
		System.arraycopy(slice1, 0, finalArray, 0, slice1.length);
		System.arraycopy(slice2, 0, finalArray, slice1.length, slice2.length);
		
		return finalArray;
	}

	/**
	 * Change one of the bytes in the mode so it becomes an invalid mode
	 * @param data byteBuffer containing the data
	 * @return the corrupted data array
	 */
	private static byte[] invalidMode(byte[] data) {
		int i = zeroPosition(data);
		byte[] array = Arrays.copyOf(data, data.length);
		array[i+1] = (byte)1;
		return array;
	}

	/**
	 * add a 1 to the first zero and change the second byte to 9
	 * @param data byteBuffer containing the data
	 * @return the corrupted data array
	 */
	private static byte[] invalidOpcode(byte[] data) {
		byte[] array = Arrays.copyOf(data, data.length);
		array[0] = (byte)1;
		array[1] = (byte)9;
		return array;
	}

	/**
	 * add an extra byte to the end of the data
	 * @param data byteBuffer containing the data
	 * @return the corrupted data array
	 */
	private static byte[] extraData(byte[] data) {
		byte[] array = Arrays.copyOf(data, data.length);
		byte[] finalArray = new byte[array.length + 1];
		System.arraycopy(array, 0, finalArray, 0, array.length);
		finalArray[finalArray.length-1] = (byte)1;

		return finalArray;
	}

	/**
	 * remove the error message from error packets
	 * @param data byteBuffer containing the data
	 * @return the corrupted data array
	 */
	private static byte[] missingErrorMessage(byte[] data) {
		byte[] array = Arrays.copyOfRange(data, 0, 4);
		byte[] finalArray = new byte[array.length + 1];
		System.arraycopy(array, 0, finalArray, 0, array.length);
		finalArray[finalArray.length-1] = (byte)0;
		
		return finalArray;
	}

	/**
	 * remove the error code from the error message
	 * @param data byteBuffer containing the data
	 * @return the corrupted data array
	 */
	private static byte[] missingErrorCode(byte[] data) {
		byte[] slice1 = Arrays.copyOfRange(data, 0, 2);
		byte[] slice2 = Arrays.copyOfRange(data, 4, data.length);
		byte[] finalArray = new byte[slice1.length + slice2.length];
		System.arraycopy(slice1, 0, finalArray, 0, slice1.length);
		System.arraycopy(slice2, 0, finalArray, slice1.length, slice2.length);
		return finalArray;
	}

	/**
	 * change the error code to 9(invalid error code)
	 * @param data byteBuffer containing the data
	 * @return the corrupted data array
	 */
	private static byte[] invalidErrorCode(byte[] data) {
		byte[] array = Arrays.copyOf(data, data.length);
		array[2] = (byte)1;
		array[3] = (byte)9;
		return array;
	}

	/**
	 * increase the block number by 3
	 * @param data byteBuffer containing the data
	 * @return the corrupted data array
	 */
	private static byte[] invalidBlockNumber(byte[] data) {
		byte[] array = Arrays.copyOf(data, data.length);
		ByteBuffer bb = ByteBuffer.wrap(array);
		short blockNumber = (bb.getShort(2));
		blockNumber += 3;
		byte[] blockNumberAsByteArray = ByteBuffer.allocate(4).putShort(blockNumber).array();
		array[2] = blockNumberAsByteArray[0];
		array[3] = blockNumberAsByteArray[1];
		return array;
	}

	/**
	 * remove the block number from DATA and ACK
	 * @param data byteBuffer containing the data
	 * @return the corrupted data array
	 */
	private static byte[] missingBlockNumber(byte[] data) {
		byte[] slice1 = Arrays.copyOfRange(data, 0, 2);
		byte[] slice2 = Arrays.copyOfRange(data, 4, data.length);
		byte[] finalArray = new byte[slice1.length + slice2.length];
		System.arraycopy(slice1, 0, finalArray, 0, slice1.length);
		System.arraycopy(slice2, 0, finalArray, slice1.length, slice2.length);
		return finalArray;
	}

	/**
	 * remove the data from DATA
	 * @param data byteBuffer containing the data
	 * @return the corrupted data array
	 */
	private static byte[] dataMissingData(byte[] data) {
		byte[] array = Arrays.copyOfRange(data, 0, 4);
		return array;
	}
	
	/**
	 * return the position of the zero between filename and mode
	 * @param data byteBuffer containing the data
	 * @return the position of the zero
	 */
	private static int zeroPosition(byte[] data){
		ByteBuffer bb = ByteBuffer.wrap(data);
		bb.rewind();
		bb.position(1);
		while (Byte.compare(bb.get(), (byte) 0) != 0 && bb.hasRemaining()){}
		return bb.position()-1;
	}
}
