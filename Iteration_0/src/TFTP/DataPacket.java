
import java.nio.*;
import java.io.*;

public class DataPacket {
	
	private byte[] array;
	private ByteArrayOutputStream byteStream;
	private int blockNumber; // for what?

	
	public DataPacket(int blockNumber, byte[] data){
		this.blockNumber = blockNumber;
		array = data;
	}
	
	public DataPacket(byte[] data){
		array = data;
	}
	
	public boolean validate(){
		byte[] temp = new byte[2];
		if(array[0]!=0) return false;
		else if (array[1]!=3) return false;
	    else if (array.length > 516) return false;
	    else if(hasZeros(array)) return false;
		temp[0] = array[2]; temp[1] = array[3];
		ByteBuffer buf = ByteBuffer.wrap(temp);
		blockNumber = buf.getInt();
		return true;
	}
	
	
	/**
	 * @author shast
	 * @param blockNum
	 * @return
	 */
	public byte[] createPacketData(){
		//ArrayList<byte[]> tempArray = new ArrayList<byte[]>();
		//ByteBuffer buffer = ByteBuffer.wrap(blockNum);
		//int num = buffer.getInt();
		
		byteStream = new ByteArrayOutputStream();
		byteStream.write(0);byteStream.write(3); byteStream.write(blockNumber);
		
		//Reader read = new Reader("C:\\Users\\shast\\Desktop\\src.txt");
		//tempArray = read.fileToByteArrays();
		
		try {
			byteStream.write(array);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return byteStream.toByteArray();
	}
	
	/*public int getBlockNumber(){
		return blockNumber;
	}*/
	
	private boolean hasZeros(byte[] array){	
		for(int j = 4;j<array.length;j++) { 
            if (array[j] == 0) return false;
        }
		return true;
	}
}
