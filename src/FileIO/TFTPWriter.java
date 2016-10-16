package FileIO;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * The {@link TFTPWriter} class appends byte segments to a file
 *
 * @author Team 3000000
 * @author Aritra Sengupta
 * @author Shasthra Ranasinghe
 * @version 1.0
 */
public class TFTPWriter {
    /**
     * The FileOutputStream that handles writing to a file
     */
    private FileOutputStream outputStream;

    /**
     * Constructor
     * <p>
     * Creates a new {@link TFTPWriter} with the specified file path
     *
     * @param filePath specifies the file to write to
     * @param append   specifies whether to append to file or start from beginning
     * @since 1.0
     */
    public TFTPWriter(String filePath, boolean append) throws IOException {
        //create a file at filePath if file does not exist and append to it
        outputStream = new FileOutputStream(filePath, append);
    }

    /**
     * This method writes a byte array to the file opened in the outputStream
     *
     * @param data the byte array to write the file
     * @since 1.0
     */
    public void writeToFile(byte[] data) throws Exception {
        outputStream.write(data);
    }

    /**
     * This method closes the outputStream and releases any system resources associated with it
     * @since 1.0
     */
    public void closeHandle() throws IOException {
        outputStream.close();
    }
}
