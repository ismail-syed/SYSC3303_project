import java.util.ArrayList;
import java.nio.*;
import java.io.*;

public class DataPacket {
	
	private ByteArrayOutputStream byteStream;
	
	public boolean validate(byte[] data){
		return true;
	}
	
	public byte[] createPacketData(byte[] blockNum){
		ArrayList<byte[]> tempArray = new ArrayList<byte[]>();
		ByteBuffer buffer = ByteBuffer.wrap(blockNum);
		int num = buffer.getInt();
		
		byteStream = new ByteArrayOutputStream();
		byteStream.write(0);byteStream.write(3); byteStream.write(num);
		
		Reader read = new Reader("C:\\Users\\shast\\Desktop\\src.txt");
		tempArray = read.fileToByteArrays();
		
		try {
			byteStream.write(tempArray.get(num-1));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return byteStream.toByteArray();
	}
}
