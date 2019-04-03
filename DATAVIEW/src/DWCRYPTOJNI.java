import java.io.*;
import java.util.Random;

public class DWCRYPTOJNI {
	static {
		try {
    	
			System.loadLibrary("dwcrypto");// Load native library at runtime
			// Name should follow the name format ==>  jnibridge.dll (Windows) or libjnibridge.so (Unixes)
			
		} catch (UnsatisfiedLinkError e) {

		  System.err.println("DATAVIEW Crypto Native code library failed to load.\n" + e);
		  System.exit(1);

		}
	}
	 
	   // Declare native methods signature where the definition will be in the jnibridge. 
	   public native int jniEncrypt(String fileName, String password, String aad);
	   public native int jniDecrypt(String cipherFile, String sizeFile, String tagFile, String password, String aad);
	   
	   
		// driverprogram for performance measurement
	   
	   //generate textfile with random numbers
	   public static void generateFile(String fileName) {
 
		try{
	 
			PrintWriter out = new PrintWriter(new File("file1.txt"));
	 
			Random generator = new Random(9);
			long size= Integer.MAX_VALUE-16;
			long limit=(long)0;
			System.out.println("The input file size is "+size/1024.0/1024.0/1024.0+" GB");
			while ( limit<size) {
				int num = generator.nextInt(9); //generate a random number
				out.print(num);
				limit++;
			}
	 
			out.close();
	 
			}
	 
			catch (IOException e)
			{
				e.printStackTrace();
			} 
	 
		}
	
	 	
	
	/*public static void main(String[] args) {
		System.out.println("Hello from  DATAVIEW Crypto JAVA Class!");
		//DWCRYPTO.generateFile("file1.txt");
		DWCRYPTO a =new DWCRYPTO();
		int ret=-1;
		//long EncryptStartTime = System.currentTimeMillis();
		ret=a.jniEncrypt("labeled.txt", "PassWord123", "Authenticate with it");
		//long EncryptFinishTime = System.currentTimeMillis();
		ret=a.jniEncrypt("unlabeled.txt", "PassWord123", "Authenticate with it");
		long DecryptStartTime = System.currentTimeMillis();
		ret=a.jniDecrypt("labeled.txt.dat","labeled.txt.size","labeled.txt.tag", "PassWord123", "Authenticate with it");
		ret=a.jniDecrypt("unlabeled.txt.dat","unlabeled.txt.size","unlabeled.txt.tag", "PassWord123", "Authenticate with it");
		ret=a.jniDecrypt("output.txt.dat","output.txt.size","output.txt.tag", "PassWord123", "Authenticate with it");
		long DecryptFinishTime = System.currentTimeMillis();

		//System.out.println("Encryption took "+(EncryptFinishTime-EncryptStartTime)/1000.0+" Seconds");
		//System.out.println("Decryption took "+(DecryptFinishTime-DecryptStartTime)/1000.0+" Seconds");
		//if(ret!=0)System.out.println("Error!");
	}   */
}
