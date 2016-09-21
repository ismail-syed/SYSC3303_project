import java.io.*;
import java.util.*;

public class Reader {
	
	private BufferedInputStream in;
	private ArrayList<byte[]> arrayOfDataArrays;

	public Reader(String filename){
		try {
			in = new BufferedInputStream(new FileInputStream(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public ArrayList<byte[]> fileToByteArrays(){
		byte[] data = new byte[512];
		int n;
		try {
			while((n = in.read(data)) != -1){
				arrayOfDataArrays.add(data);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return arrayOfDataArrays;
	}
}
