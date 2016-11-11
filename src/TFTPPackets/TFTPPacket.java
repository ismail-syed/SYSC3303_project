package TFTPPackets;

import Exceptions.PacketOverflowException;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * The {@link TFTPPacket} is the class from which all other TFTP packet classes inherit from
 * and contains the ByteArrayOutputStream which backs all the packets
 *
 * @author Team 3000000
 * @author Aritra Sengupta
 * @version 1.0
 * @see ACKPacket
 * @see DataPacket
 * @see RRQWRQPacketCommon
 */
public class TFTPPacket {
    /**
     * The maximum size of any TFTP packet
     */
    public static final int MAX_SIZE = 1024;

    /**
     * The ByteArrayOutputStream that backs the TFTP packets
     */
    private ByteArrayOutputStream packetByteArrayOutputStream;

    /**
     * Constructor
     * <p>
     * Initialize the packetByteArrayOutputStream to get ready to write to it
     *
     */
    public TFTPPacket() {
        packetByteArrayOutputStream = new ByteArrayOutputStream();
    }

    //Based on Arrays toString method
    /**
     * Return a string representation of the TFTP packet
     *
     * @return string representation of the TFTP packet
     * @since 1.0
     */
    public static String toString(byte[] data) {
        if (data == null)
            return "null";
        int iMax = data.length - 1;
        if (iMax == -1)
            return "[]";
        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            //Convert to unsigned byte
            b.append(data[i] & 0xFF);
            if (i == iMax)
                return b.append(']').toString();
            b.append(", ");
        }
    }

    /**
     * This method appends a byte to the packetByteArrayOutputStream
     *
     * @param b the byte to append to the packetByteArrayOutputStream
     * @since 1.0
     */
    void addByte(byte b) throws PacketOverflowException {
        if (packetByteArrayOutputStream.size() < MAX_SIZE) {
            packetByteArrayOutputStream.write(b);
        } else {
            throw new PacketOverflowException("Packet is larger than allowed maximum size.");
        }
    }

    /**
     * This method appends a byte array to the end of the packetByteArrayOutputStream
     *
     * @param ba the byte array to append to the packetByteArrayOutputStream
     * @since 1.0
     */
    void addBytes(byte[] ba) throws PacketOverflowException {
        for (byte aBa : ba) {
            addByte(aBa);
        }
    }

    /**
     * Gets the byte array representation of the packet
     *
     * @return a byte array representation of a TFTP packet
     * @since 1.0
     */
    public byte[] getByteArray() {
        return packetByteArrayOutputStream.toByteArray();
    }

    /**
     * This enum holds all the possible opcodes for TFTP packets
     * The opcodes are mapped to their string representation on startup
     * allowing the retrieval of the {@link Opcode} enum as an enum or
     * its string representation
     * @since 1.0
     */
    public enum Opcode {
        UNKNOWN(-1), READ(1), WRITE(2), DATA(3), ACK(4), ERROR(5);
        private static Map<Integer, Opcode> map = new HashMap<>();

        //map to string representation on startup
        static {
            for (Opcode opcode : Opcode.values()) {
                map.put(opcode.opCodeNumber, opcode);
            }
        }

        private int opCodeNumber;

        //Create opcodes from string
        Opcode(final int opcode) {
            opCodeNumber = opcode;
        }

        //return as enum
        public static Opcode asEnum(int opcodeNumber) {
            if (map.get(opcodeNumber) == null) {
                return Opcode.UNKNOWN;
            }
            return map.get(opcodeNumber);
        }

        //return as string
        public int getOpCodeNumber() {
            return opCodeNumber;
        }
    }
}