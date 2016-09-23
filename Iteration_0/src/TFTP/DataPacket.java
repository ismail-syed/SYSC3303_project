import java.util.ArrayList;
import java.nio.*;
import java.io.*;

public class DataPacket {
	
	private ByteArrayOutputStream byteStream;
	private int blockNumber;
	
	public boolean validate(byte[] data){
		byte[] temp = new byte[2];
		if(data[0]!=0) return false;
		else if (data[1]!=3) return false;
	    else if (data.length > 516) return false;
	    else if(hasZeros(data)) return false;
		temp[0] = data[2]; temp[1] = data[3];
		ByteBuffer buf = ByteBuffer.wrap(temp);
		blockNumber = buf.getInt();
		return true;
	}
	
	
	/**
	 * @author shast
	 * @param blockNum
	 * @return
	 */
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
	
	public int getBlockNumber(){
		return blockNumber;
	}
	
	private boolean hasZeros(byte[] array){	
		for(int j = 4;j<array.length;j++) { 
            if (array[j] == 0) return false;
        }
		return true;
	}
}
