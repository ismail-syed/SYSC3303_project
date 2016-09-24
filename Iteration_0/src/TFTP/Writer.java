import java.io.*;

public class Writer {
	/*
	public static void main( String args[] )
	{
	   String text = "hello world";
	   Writer write = new Writer("C:\\Users\\shast\\Desktop\\testing.txt");
	   write.writeToFile(text.getBytes());
	}
	*/
	
	BufferedOutputStream out;
	
	public Writer(String filename){
		File file = new File(filename);
		createOutput(file);
	}
	
	public Writer(String filename,String location){
		File file = new File(location+filename);
		createOutput(file);
	}
	
	private void createOutput(File path){
		try {
			out = new BufferedOutputStream(new FileOutputStream(path));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public boolean writeToFile(byte[] data){
		try {
			//System.out.println(new String(data));
			int n = data.length;
			out.write(data,0,n);
			return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	public void closeFile(){
		try {
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
