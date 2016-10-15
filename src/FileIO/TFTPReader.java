package FileIO;

import Exceptions.InvalidBlockNumberException;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

/**
 * The {@link TFTPReader} class reads a file in segments of 512 bytes (or less if it is the last block)
 * and stores it in a map which maps the segments to a block number. This allows the retrieval of an
 * arbitrary block in the file and ensures the file is only read into memory once
 *
 * @author Team 3000000
 * @author Aritra Sengupta
 * @author Shasthra Ranasinghe
 * @version 1.0
 */
public class TFTPReader {
    /**
     * The map which associates block numbers with data segments
     */
    private Map<Integer, byte[]> blocksFromFile = new HashMap<>();
    /**
     * The maximum size of the data segment in a block
     */
    private static final int MAX_BLOCK_SIZE = 512;
    /**
     * The max number of blocks in the file
     */
    private static final int MAX_BLOCK_NUMBER = Short.MAX_VALUE * 2 + 1; //65535

    /**
     * Constructor
     * <p>
     * Creates a new {@link TFTPReader} with the specified file path
     *
     * @param filePath specifies the path to the file to read
     * @since 1.0
     */
    public TFTPReader(String filePath) throws IOException {
        try {
            //read the whole file into memory
            byte[] fileAsByteArray = Files.readAllBytes(new File(filePath).toPath());
            //Calculate the number of blocks from the file
            int numberOfBlocks = (fileAsByteArray.length / MAX_BLOCK_SIZE)+1;
            if(numberOfBlocks > MAX_BLOCK_NUMBER){
            	throw new InvalidBlockNumberException("Block number must be between 0 and " + Integer.toString(MAX_BLOCK_NUMBER));
            }
            for (int i = 0; i < numberOfBlocks; i++) {
                if (i == numberOfBlocks-1) {
                    //if last block then read to the end of file
                    blocksFromFile.put(i + 1, Arrays.copyOfRange(fileAsByteArray, i * MAX_BLOCK_SIZE, fileAsByteArray.length));
                } else {
                    //read in 512 byte chunks
                    blocksFromFile.put(i + 1, Arrays.copyOfRange(fileAsByteArray, i * MAX_BLOCK_SIZE, (i * MAX_BLOCK_SIZE) + MAX_BLOCK_SIZE));
                }
            }
        } catch (InvalidBlockNumberException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets a specific block of data specified by a block number
     *
     * @param blockNumber the block number from which to get the data
     * @return the block associated with the block number
     * @since 1.0
     */
    public byte[] getFileBlock(int blockNumber) throws Exception {
        if (blockNumber > 0 & blockNumber <= blocksFromFile.size()) {
            return blocksFromFile.get(blockNumber);
        } else {
            throw new InvalidBlockNumberException("Block number does not exist");
        }
    }

    /**
     * Gets the total number of block in the file
     *
     * @return the total number of blocks in the file
     * @since 1.0
     */
    public int getNumberOfBlocks() {
        return this.blocksFromFile.size();
    }
}