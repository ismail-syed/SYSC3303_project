package TFTPPackets;

import Exceptions.MalformedPacketException;
import Exceptions.PacketOverflowException;

/**
 * The {@link WRQPacket} class represents a WRQ packet as per the TFTP PROTOCOL (RFC 1350)
 *
 * @author Team 3000000
 * @author Aritra Sengupta
 * @version 1.0
 */

public class RRQPacket extends RRQWRQPacketCommon {
    /**
     * Constructor
     * <p>
     * Parses and verifies a byte array representation of a RRQ packet
     * and then creates a new {@link WRQPacket} from the parsed file name and mode
     *
     * @param packetAsByteArray a byte array holding the RRQ packet
     * @since 1.0
     */
    public RRQPacket(byte[] packetAsByteArray) throws MalformedPacketException, PacketOverflowException {
        super();
        createPacket(packetAsByteArray);
    }

    /**
     * Constructor
     * <p>
     * Creates a new {@link WRQPacket} with the specified file name and mode
     *
     * @param fileName the file name to go in the RRQ packet
     * @param mode the mode to go in the RRQ packet
     * @since 1.0
     */
    public RRQPacket(String fileName, Mode mode) throws PacketOverflowException {
        super();
        createPacket(Opcode.READ,fileName, mode);
    }
}
