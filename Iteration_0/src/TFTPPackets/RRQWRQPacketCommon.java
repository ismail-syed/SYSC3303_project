package TFTPPackets;

import Exceptions.MalformedPacketException;
import Exceptions.PacketOverflowException;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The {@link RRQWRQPacketCommon} holds common elements of the RRQ and WRQ packet as per the TFTP PROTOCOL (RFC 1350)
 *
 * @author Team 3000000
 * @author Aritra Sengupta
 * @version 1.0
 * @see WRQPacket
 * @see RRQPacket
 */
public class RRQWRQPacketCommon extends TFTPPacket {
    /**
     * The opcode in the RRQ or WRQ packet
     */
    private Opcode opcode;

    /**
     * The file name in the RRQ or WRQ packet
     */
    private String fileName;

    /**
     * The mode in the RRQ or WRQ packet
     */
    private Mode mode;

    /**
     * Gets the {@link Opcode} in the RRQ or WRQ packet
     *
     * @return the {@link Opcode} in the RRQ or WRQ packet
     * @since 1.0
     */
    public TFTPPacket.Opcode getOpcode() {
        return opcode;
    }

    /**
     * Gets the file name in the RRQ or WRQ packet
     *
     * @return the file name in the RRQ or WRQ packet
     */
    public String getFilename() {
        return fileName;
    }

    /**
     * Gets the mode in the RRQ or WRQ packet
     *
     * @return the mode in the RRQ or WRQ packet
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * Creates a new RRQ or WRQ packet from byte array representation
     *
     * @param packetAsByteArray byte array representation of packet
     */
    void createPacket(byte[] packetAsByteArray) throws MalformedPacketException, PacketOverflowException {
        StringBuilder sb = new StringBuilder();

        // Temporary fields used for testing the packet structure
        Mode mode;

        if (packetAsByteArray.length > MAX_SIZE) {
            throw new MalformedPacketException("Packet is larger than then maximum allowed size");
        }

        ByteBuffer bb = ByteBuffer.wrap(packetAsByteArray);
        bb.rewind();

        // First check first byte is 0
        if (bb.get() != (byte) 0) {
            throw new MalformedPacketException("Packet does not start with a 0 byte");
        }
        // Get opcode from second byte
        byte opcodeAsByte = bb.get();
        if (Opcode.asEnum((int) opcodeAsByte) == Opcode.WRITE) {
            this.opcode = Opcode.WRITE;
        } else if (Opcode.asEnum((int) opcodeAsByte) == Opcode.READ) {
            this.opcode = Opcode.READ;
        } else {
            throw new MalformedPacketException("RRQPacket does not have a valid opcode");
        }
        // Get filename between opcode byte and second zero byte

        while (Byte.compare(bb.get(), (byte) 0) != 0 && bb.hasRemaining()) {
            char c = (char) (bb.get(bb.position() - 1) & 0xFF);
            sb.append(c);
        }
        // Check if valid file name
        if (!Objects.equals(sb.toString(), "")) {
            fileName = sb.toString();
        } else {
            throw new MalformedPacketException("Packet does not contain a valid file name");
        }

        // Get mode between second zero byte and third zero byte
        sb.setLength(0);
        while (Byte.compare(bb.get(), (byte) 0) != 0 && bb.hasRemaining()) {
            char c = (char) (bb.get(bb.position() - 1) & 0xFF);
            sb.append(c);
        }

        mode = Mode.asEnum(sb.toString().toUpperCase());
        if (mode != Mode.UNKNOWN) {
            this.mode = mode;
        } else {
            throw new MalformedPacketException("Packet does not have a valid mode");
        }

        // Get third 0 byte
        bb.position(bb.position() - 1);
        if (Byte.compare(bb.get(), (byte) 0) != 0) {
            throw new MalformedPacketException("Packet does not end with a 0 byte");
        }

        createPacket(Opcode.WRITE, fileName, mode);
    }

    /**
     * Creates a new RRQ or WRQ packet with the specified opcode, filename and mode
     *
     * @param opcode   the opcode to go in the RRQ or WRQ packet
     * @param fileName the filename to go in the RRQ or WRQ packet
     * @param mode     the mode to go in the RRQ or WRQ packet
     */
    void createPacket(Opcode opcode, String fileName, Mode mode) throws PacketOverflowException {
        if (opcode == Opcode.READ || opcode == Opcode.WRITE) {
            this.opcode = opcode;
        }

        this.fileName = fileName;
        this.mode = mode;

        // add first zero byte
        this.addByte((byte) 0);
        // add opcode
        this.addByte((byte) opcode.getOpCodeNumber());
        // add filename
        for (int i = 0; i < fileName.length(); i++) {
            this.addByte((byte) fileName.charAt(i));
        }
        // add second zero byte
        this.addByte((byte) 0);
        // add mode
        for (int i = 0; i < mode.toString().length(); i++) {
            this.addByte((byte) Character.toLowerCase(mode.getModeString().charAt(i)));
        }
        // add third zero byte
        this.addByte((byte) 0);
    }

    /**
     * This enum holds all the possible modes for a RRQ or WRQ packet
     * The modes are mapped to their string representation on startup
     * allowing the retrieval of the {@link Mode} enum as an enum or
     * its string representation
     */
    public enum Mode {
        UNKNOWN("UNKNOWN"), NETASCII("NETASCII"), OCTET("OCTET");
        private static Map<String, Mode> map = new HashMap<>();

        //map to string representation on startup
        static {
            for (Mode mode : Mode.values()) {
                map.put(mode.modeString, mode);
            }
        }

        private String modeString;

        //Create modes from string
        Mode(final String mode) {
            modeString = mode;
        }

        //return as enum
        public static Mode asEnum(String modeString) {
            if (map.get(modeString) == null) {
                return Mode.UNKNOWN;
            }
            return map.get(modeString);
        }

        //return as string
        public String getModeString() {
            return modeString;
        }
    }
}
