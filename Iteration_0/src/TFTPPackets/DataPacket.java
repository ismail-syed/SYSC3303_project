package TFTPPackets;

import Exceptions.MalformedPacketException;
import Exceptions.PacketOverflowException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * The {@link DataPacket} class represents a DATA packet as per the TFTP PROTOCOL (RFC 1350)
 *
 * @author Team 3000000
 * @author Aritra Sengupta
 * @author Shasthra Ranasinghe
 * @version 1.0
 */
public class DataPacket extends TFTPPacket {
    /**
     * The maximum size of the data field in the DATA packet
     */
    public static final int MAX_DATA_SIZE = 512;

    /**
     * The block number in the DATA packet
     */
    private int blockNumber;

    /**
     * The data in the data field of the DATA packet
     */
    private byte[] data;

    /**
     * Constructor
     * <p>
     * Parses and verifies a byte array representation of a DATA packet
     * and then creates a new {@link DataPacket} from the parsed block number and data field
     *
     * @param packetAsByteArray a byte array holding the DATA packet
     * @since 1.0
     */
    public DataPacket(byte[] packetAsByteArray) throws MalformedPacketException, PacketOverflowException, IOException {
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
        if (!(Opcode.asEnum((int) opcodeAsByte) == Opcode.DATA)) {
            throw new MalformedPacketException("Invalid Opcode");
        }

        // Get block number
        blockNumber = (bb.getShort() & 0xFFFF);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        // Get data between opcode byte and end of packet
        while (bb.hasRemaining()) {
            byteArrayOutputStream.write(bb.get());
        }
        data = byteArrayOutputStream.toByteArray();

        createPacket(blockNumber, data);

    }

    /**
     * Constructor
     * <p>
     * Creates a new {@link DataPacket} with the specified block number and data
     *
     * @param blockNumber the block number to put in the DATA packet
     * @param data        a byte array holding the data to go in the data field of the DATA packet
     * @since 1.0
     */
    public DataPacket(int blockNumber, byte[] data) throws PacketOverflowException, IOException, MalformedPacketException {
        super();
        createPacket(blockNumber, data);
    }

    /**
     * This method creates a new {@link DataPacket} with the specified block number and data array
     *
     * @param blockNumber the block number to put in the DATA packet
     * @param data        the byte array holding the data to go in the data field of the DATA packet
     * @since 1.0
     */
    private void createPacket(int blockNumber, byte[] data) throws PacketOverflowException, IOException, MalformedPacketException {
        if (data.length >= 0 && data.length <= MAX_DATA_SIZE) {
            this.blockNumber = blockNumber;
            this.data = data;
            // add first zero byte
            this.addByte((byte) 0);
            // add opcode
            this.addByte((byte) Opcode.DATA.getOpCodeNumber());
            byte[] blockNumberAsByteArray = ByteBuffer.allocate(4).putInt(blockNumber).array();
            //add block number
            this.addByte(blockNumberAsByteArray[2]);
            this.addByte(blockNumberAsByteArray[3]);
            // add data
            this.addBytes(data);
        } else {
            throw new MalformedPacketException("The data in the DATA Packet must be greater than 0 bytes and less than equal to 512 bytes");
        }
    }

    /**
     * Gets the data in the data field in the DATA packet
     *
     * @return the data in the DATA packet
     * @since 1.0
     */
    public byte[] getData() {
        return this.data;
    }

    /**
     * Gets the block number in the DATA packet
     *
     * @return the block number in the DATA packet
     * @since 1.0
     */
    public int getBlockNumber() {
        return this.blockNumber;
    }
}
