package dataview.workflowexecutor;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

public class SHA256Hash {

   public static byte[] createChecksum(String filename) throws Exception {
       InputStream fis =  new FileInputStream(filename);

       byte[] buffer = new byte[1024];
       MessageDigest complete = MessageDigest.getInstance("SHA-256");
       int numRead;

       do {
           numRead = fis.read(buffer);
           if (numRead > 0) {
               complete.update(buffer, 0, numRead);
           }
       } while (numRead != -1);

       fis.close();
       return complete.digest();
   }

   // see this How-to for a faster way to convert
   // a byte array to a HEX string
   public static String getSHA256Checksum(String filename) throws Exception {
       byte[] b = createChecksum(filename);
       String result = "";

       for (int i=0; i < b.length; i++) {
           result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
       }
       return result;
   }

   public static void main(String args[]) {
       try {
          // System.out.println(getMD5Checksum("sgx_linux_ubuntu16.04.1_x64_sdk_1.9.100.39124.bin"));
    	   System.out.println(getSHA256Checksum(args[0]));
           // output should be below:
           //   a7a61f6a640fcf7fde848a0be5d36128cf9703edc07ccdde1ee9b4067a43889f 
           // ref from 01.org SGX repository:
           //  sgx_linux_ubuntu16.04.1_x64_sdk_1.9.100.39124.bin
       }
       catch (Exception e) {
           e.printStackTrace();
       }
   }
}
