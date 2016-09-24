import java.io.*;
import java.util.*;

public class Reader {
	
	private BufferedInputStream in;
	private ArrayList<byte[]> arrayOfDataArrays;

	
	/*public static void main( String args[] )
	{
	   ArrayList<byte[]> testArray = new ArrayList<byte[]>();
	   Reader read = new Reader("C:\\Users\\shast\\Desktop\\src.txt");
	   Writer write = new Writer("C:\\Users\\shast\\Desktop\\dest.txt");
	   testArray = read.fileToByteArrays();
	   for(byte[] array: testArray){
		   if(array[0]!=0){
			   write.writeToFile(array);
		   }
	   }
	   write.closeFile();
	}*/
	
	public Reader(String filename){
		File file = new File(filename);
		createInput(file);
		fileToByteArrays();
	}
	
	public Reader(String filename, String location){
		File filepath = new File(location+filename);
		createInput(filepath);
		fileToByteArrays();
	}
	
	public byte[] getData(int blockNum){
		return arrayOfDataArrays.get(blockNum);
	}
	
	public ArrayList<byte[]> fileToByteArrays(){
		byte[] data = new byte[512];
		arrayOfDataArrays = new ArrayList<byte[]>();
		byte[] finalData;
		int len;
		
		try {
			while(in.read(data) != -1){
				if(data[511]==0){
					for(len=0;len<data.length;len++) {
			              if (data[len] == 0) break;
			        }
					finalData = Arrays.copyOfRange(data,0,len);
				}else{
					finalData = data;
				}
				arrayOfDataArrays.add(finalData);
				//System.out.println(new String(finalData));
				data = new byte[512];
				if(finalData.length == 512 && in.available() == 0){
					arrayOfDataArrays.add(new byte[] {0});
				}
			}
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//Collections.reverse(arrayOfDataArrays);
		return arrayOfDataArrays;
	}
	
	private void createInput(File path){
		try {
			in = new BufferedInputStream(new FileInputStream(path));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
