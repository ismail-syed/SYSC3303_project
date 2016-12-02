package TFTPPackets;

import Exceptions.MalformedPacketException;
import Exceptions.PacketOverflowException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * @author Kunall Banerjee (100978717)
 */
public class ErrorPacket extends TFTPPacket {
	private ErrorCode errorCode;
	private String msg;

	public enum ErrorCode {
		FILE_NOT_FOUND(1), ACCESS_VIOLATION(2), DISC_FULL_OR_ALLOCATION_EXCEEDED(3), ILLEGAL_OPERATION(4), UNKNOWN_TID(
				5), FILE_ALREADY_EXISTS(6);

		private int code;

		ErrorCode(int code) {
			this.code = code;
		}

		int getCode() {
			return code;
		}

		static ErrorCode getErrorCode(int code) throws IllegalArgumentException {
			if (code == 1)
				return FILE_NOT_FOUND;
			if (code == 2)
				return ACCESS_VIOLATION;
			if (code == 3)
				return DISC_FULL_OR_ALLOCATION_EXCEEDED;
			if (code == 4)
				return ILLEGAL_OPERATION;
			if (code == 5)
				return UNKNOWN_TID;
			if (code == 6)
				return FILE_ALREADY_EXISTS;
			else
				throw new IllegalArgumentException("Invalid error code");
		}

	}

	/**
	 * @param type
	 * @param msg
	 */
	public ErrorPacket(ErrorCode type, String msg) throws IllegalArgumentException {
		super();
		if (type == null)
			throw new IllegalArgumentException("Please specify an error type along with your message");
		this.errorCode = type;
		this.msg = msg;
		createPacket(type, msg);
	}

	public ErrorPacket(byte[] data) throws MalformedPacketException, PacketOverflowException, IOException {
		super();
		createPacket(data);
	}

	/**
	 * @return The error type
	 */
	public ErrorCode getErrorCode() {
		return this.errorCode;
	}

	void createPacket(byte[] packetAsByteArray) throws MalformedPacketException, PacketOverflowException {
		StringBuilder sb = new StringBuilder();

		if (packetAsByteArray.length > MAX_SIZE) {
			throw new MalformedPacketException("Packet is larger than the maximum allowed size");
		}

		ByteBuffer bb = ByteBuffer.wrap(packetAsByteArray);
		bb.rewind();

		// First check first byte is 0
		if (bb.get() != (byte) 0) {
			throw new MalformedPacketException("Packet does not start with a 0 byte");
		}
		// Get opcode from second byte
		byte opcodeAsByte = bb.get();
		if (Opcode.asEnum((int) opcodeAsByte) != Opcode.ERROR) {
			throw new MalformedPacketException("Not an error packet");
		}

		if (bb.get() != (byte) 0) {
			throw new MalformedPacketException("Packet does not contain a valid code");
		}

		// Get opcode from second byte
		opcodeAsByte = bb.get();
		if (ErrorCode.getErrorCode((int) opcodeAsByte) != ErrorCode.ACCESS_VIOLATION
				&& ErrorCode.getErrorCode((int) opcodeAsByte) != ErrorCode.DISC_FULL_OR_ALLOCATION_EXCEEDED
				&& ErrorCode.getErrorCode((int) opcodeAsByte) != ErrorCode.FILE_ALREADY_EXISTS
				&& ErrorCode.getErrorCode((int) opcodeAsByte) != ErrorCode.FILE_NOT_FOUND
				&& ErrorCode.getErrorCode((int) opcodeAsByte) != ErrorCode.ILLEGAL_OPERATION
				&& ErrorCode.getErrorCode((int) opcodeAsByte) != ErrorCode.UNKNOWN_TID) {
			throw new MalformedPacketException("Packet does not contain valid error code");
		} else {
			this.errorCode = ErrorCode.getErrorCode((int) opcodeAsByte);
		}

		// Get message between opcode byte and last zero byte
		while (Byte.compare(bb.get(), (byte) 0) != 0 && bb.hasRemaining()) {
			char c = (char) (bb.get(bb.position() - 1) & 0xFF);
			sb.append(c);
		}

		// Check if valid file name
		if (!Objects.equals(sb.toString(), "")) {
			this.msg = sb.toString();
		} else {
			throw new MalformedPacketException("Packet does not contain a valid message");
		}

		// Get last 0 byte
		bb.position(bb.position() - 1);
		if (Byte.compare(bb.get(), (byte) 0) != 0) {
			throw new MalformedPacketException("Packet does not end with a 0 byte");
		}

        if(bb.hasRemaining()){
            throw new MalformedPacketException("There are trailing bytes in the Error Packet");
        } else {
            createPacket(errorCode, msg);
        }
	}

	private void createPacket(ErrorCode type, String msg) {
		try {
			// add first zero byte
			this.addByte((byte) 0);
			// add ACK opcode
			this.addByte((byte) 5);
			this.addByte((byte) 0);
			this.addByte((byte) type.getCode());
			this.addBytes(msg.getBytes());
			// add final 0
			this.addByte((byte) 0);
		} catch (PacketOverflowException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return A human-readable message for better logging
	 */
	public String getErrorMessage() {
		return this.msg;
	}
}
