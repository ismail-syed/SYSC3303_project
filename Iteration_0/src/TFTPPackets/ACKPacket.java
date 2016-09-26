package TFTPPackets;

import Exceptions.InvalidBlockNumberException;
import Exceptions.MalformedPacketException;
import Exceptions.PacketOverflowException;

import java.nio.ByteBuffer;

/**
 * The {@link ACKPacket} class represents an ACK packet as per the TFTP PROTOCOL (RFC 1350)
 *
 * @author Team 3000000
 * @author Aritra Sengupta
 * @version 1.0
 */
public class ACKPacket extends TFTPPacket {
    /**
     * The maximum block count possible
     * Note: The maximum value an unsigned short (2 bytes) can hold
     * 2^16 - 1 = 65535
     */
    private static final int MAX_ACK_BLOCK_COUNT = Short.MAX_VALUE * 2 + 1; //65535

    /**
     * The block number in the ACK packet
     */
    private int blockNumber;

    /**
     * Constructor
     * <p>
     * Parses and verifies a byte array representation of an ACK packet
     * and then creates a new {@link ACKPacket} from the parsed block number
     *
     * @param packetAsByteArray a byte array holding the data in the ACK packet
     * @since 1.0
     */
    public ACKPacket(byte[] packetAsByteArray) throws MalformedPacketException, InvalidBlockNumberException {
        super();
        int blockNumber;
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
        if (Opcode.asEnum((int) opcodeAsByte) != Opcode.ACK) {
            throw new MalformedPacketException("RRQPacket does not have a valid opcode");
        }

        // Get block number
        blockNumber = (bb.getShort() & 0xFFFF);

        //Check there are no more bytes
        if (bb.hasRemaining()) {
            throw new MalformedPacketException("There are trailing bytes in the ACK Packet");
        } else {
            createPacket(blockNumber);
        }
    }

    /**
     * Constructor
     * <p>
     * Creates a new {@link ACKPacket} with the specified block number
     *
     * @param blockNumber the block number to put into the ACK packet
     * @since 1.0
     */
    public ACKPacket(int blockNumber) throws InvalidBlockNumberException {
        createPacket(blockNumber);
    }

    /**
     * This method creates a new {@link ACKPacket} with the specified block number
     *
     * @param blockNumber the block number to put into the ACK packet
     */
    private void createPacket(int blockNumber) throws InvalidBlockNumberException {
        if (blockNumber < 0 || blockNumber > MAX_ACK_BLOCK_COUNT) {
            throw new InvalidBlockNumberException("Block number must be between 0 and " + Integer.toString(MAX_ACK_BLOCK_COUNT));
        } else {
            this.blockNumber = blockNumber;
            try {
                // add first zero byte
                this.addByte((byte) 0);
                // add ACK opcode
                this.addByte((byte) Opcode.ACK.getOpCodeNumber());
                // add blockNumber
                byte[] blockNumberAsByteArray = ByteBuffer.allocate(4).putInt(blockNumber).array();
                this.addByte(blockNumberAsByteArray[2]);
                this.addByte(blockNumberAsByteArray[3]);
            } catch (PacketOverflowException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets the block number in the ACK packet
     *
     * @return the block number in the ACK packet
     * @since 1.0
     */
    public int getBlockNumber() {
        return this.blockNumber;
    }
}
