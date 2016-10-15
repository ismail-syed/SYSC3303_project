package TFTPPackets;

import java.io.IOException;
import java.nio.ByteBuffer;

import Exceptions.InvalidBlockNumberException;
import Exceptions.MalformedPacketException;
import Exceptions.PacketOverflowException;
import TFTPPackets.TFTPPacket.Opcode;

/**
 * @author Kunall Banerjee (100978717)
 */
public class ErrorPacket extends TFTPPacket {
    private ErrorType type;
    private String msg;

    public enum ErrorType {
        FILE_NOT_FOUND(1), ACCESS_VIOLATION(2), DISC_FULL_OR_ALLOCATION_EXCEEDED(3), FILE_ALREADY_EXISTS(6);

        private int code;

        ErrorType(int code) {
            this.code = code;
        }

        int getCode() {
            return code;
        }
    }

    /**
     * @param type
     * @param msg
     */
    ErrorPacket(ErrorType type, String msg) throws IllegalArgumentException {
        if (type == null) throw new IllegalArgumentException("Please specify an error type along with your message");
        this.type = type;
        this.msg = msg;
        createPacket(type, msg);
    }

    /**
     * @return The error type
     */
    public ErrorType getErrorType() {
        return this.type;
    }
    
    private void createPacket(ErrorType type, String msg) throws IOException{
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
     * @return The code associated with a given error type
     */
    public int getCode() {
        return this.type.getCode();
    }

    /**
     * @return A human-readable message for better logging
     */
    public String getErrorMessage() {
        return this.msg;
    }
}
