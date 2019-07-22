import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import dataview.models.DWCRYPTO;

public class Driver {
	public static void main(String[] args) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, FileNotFoundException, IOException {

		DWCRYPTO dwcrypto = new DWCRYPTO();

/*
		Random rand =new Random();
		double x[] = new double[1024*1024*100];
		String bigfile = "bigfile.txt";
		try (FileOutputStream out1 = new FileOutputStream(bigfile)) {
			int size = x.length;
			for (int i = 0; i < size; i++) {
				if (i % (10*1024*1024) == 0) {
					System.out.println("Produced total size : " + (i / (10*1024*1024)) + "0 MB");
				}
				double d = rand.nextDouble();
				out1.write((int)d);
			}
		}
*/
		/*long start = System.currentTimeMillis();
		if (dwcrypto.encryptFile("bigdata.enc", "abc", "bigfile.txt", "secretKey") == 0) {
			System.out.println("Success encryption");
			long end = System.currentTimeMillis();
			System.out.println("Encryption time : " + (end - start) + " ms");
			start = System.currentTimeMillis();
			if (dwcrypto.decryptFile("bigdata.enc", "bigfile1.txt", "abc", "secretKey") == 0) {
				end = System.currentTimeMillis();
				System.out.println("Success decreption");
				System.out.println("Decryption time : " + (end - start) + " ms");
			} else {
				System.out.print("Decryption is not ok");
			}
		} else {
			System.out.print("Encryption is not ok");
		}*/
		
		long start = System.currentTimeMillis();
		int sizePatients = 100000;
		//String plainFile = "/home/ishtiaq/Dropbox/aaaaaa-Saeid-Ishtiaq/usenix19-results/diagnosis-test/" + sizePatients + 
			//	"/originalInput_" + sizePatients + ".txt";
//		String plainFile = "originalInput.txt";
		String plainFile = "/home/ishtiaq/Dropbox/aaaaaa-Saeid-Ishtiaq/usenix19-results/diagnosis-test/10000/parameter.txt";
//		String encryptedFile = "originalInput.enc";
		String encryptedFile = "/home/ishtiaq/Dropbox/aaaaaa-Saeid-Ishtiaq/usenix19-results/diagnosis-test/10000/parameter.enc";
		//String encryptedFile = "/home/ishtiaq/Dropbox/aaaaaa-Saeid-Ishtiaq/usenix19-results/diagnosis-test/" + sizePatients + 
			//	"/originalInput.enc";
		
		if (dwcrypto.encryptFile(encryptedFile, "abc", plainFile, "abc") == 0) {
			System.out.println("Success encryption");
			long end = System.currentTimeMillis();
			System.out.println("Encryption time : " + (end - start) + " ms");
			start = System.currentTimeMillis();
			if (dwcrypto.decryptFile(encryptedFile, "decrypted.txt", "abc", "abc") == 0) {
				end = System.currentTimeMillis();
				System.out.println("Success decreption");
				System.out.println("Decryption time : " + (end - start) + " ms");
			} else {
				System.out.print("Decryption is not ok");
			}
		} else {
			System.out.print("Encryption is not ok");
		}
	}
}
