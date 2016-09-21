import java.io.*;

public class Writer {
	
	BufferedOutputStream out;
	
	public Writer(String filename){
		try {
			out = new BufferedOutputStream(new FileOutputStream(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public boolean writeToFile(byte[] data){
		try {
			out.write(data,0,data.length);
			return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
}
