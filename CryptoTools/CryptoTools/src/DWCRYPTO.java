import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class DWCRYPTO {

	private final int AES_KEY_SIZE = 256; // in bits
	private final int GCM_NONCE_LENGTH = 12; // in bytes
	private final int GCM_TAG_LENGTH = 16; // in bytes
	private Cipher cipher;
	//	private byte[] iv;


	public DWCRYPTO() {
		try {
			/*sRandom = SecureRandom.getInstanceStrong();
			KeyGenerator keyGen = KeyGenerator.getInstance("AES");
			keyGen.init(AES_KEY_SIZE, sRandom);
			key = keyGen.generateKey(); // this is secret key
			//			iv = new byte[GCM_NONCE_LENGTH];

			 */
			KeyGenerator keyGen = KeyGenerator.getInstance("AES");
			keyGen.init(AES_KEY_SIZE); // for example


		} catch (Exception e) {

		}
	}


	byte[] toBytes(int i)
	{
		byte[] result = new byte[4];

		result[0] = (byte) (i >> 24);
		result[1] = (byte) (i >> 16);
		result[2] = (byte) (i >> 8);
		result[3] = (byte) (i /*>> 0*/);

		return result;
	}

	public int encryptFile(String cipherTextFileName, String aadStr, String plainTextFileName, String secretKeyStr){
		int status = -1;
		try {
			
			try (FileInputStream in = new FileInputStream(plainTextFileName);
					FileOutputStream out = new FileOutputStream(cipherTextFileName)) {

				byte[] iv = new byte[GCM_NONCE_LENGTH];

				cipher = Cipher.getInstance("AES/GCM/NoPadding", "SunJCE");
				//sRandom.nextBytes(iv);

				new Random().nextBytes(iv);


				// Writing IV file size and IV in cipherTextFile
				//			

				out.write(toBytes(iv.length));
				/*byte[] sizeInByte = ByteBuffer.allocate(4).putInt(iv.length).array();
			out.write(sizeInByte);*/




				out.write(iv);
				//			System.out.println(new String(iv));
				/////////////

				/*IV file write*/

				/*String ivFile = cipherTextFileName + ".iv";
			try (FileOutputStream out1 = new FileOutputStream(ivFile)) {				
				out1.write(iv);
			}*/


				/**************************************/
				byte[] key = (secretKeyStr).getBytes("UTF-8");
				MessageDigest sha = MessageDigest.getInstance("SHA-1");
				key = sha.digest(key);
				key = Arrays.copyOf(key, 16); // use only first 128 bit

				SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");


				/*********************************************/


				GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);



				cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, parameterSpec);

				byte[] aad = aadStr.getBytes(); // this is authenticate associate data
				cipher.updateAAD(aad);

				byte[] ibuf = new byte[1024];
				int len;
				while ((len = in.read(ibuf)) != -1) {
					byte[] obuf = cipher.update(ibuf, 0, len);
					if ( obuf != null ) out.write(obuf);
				}


				byte[] obuf = cipher.doFinal();
				if ( obuf != null ) out.write(obuf);
				out.close();
				status = 0;
			}

			
		}
		catch(Exception e) {
//			Dataview.debugger.logException(e);
		}
		return status;
	}




	public int decryptFile(String cipherTextFileName, String plainTextFileName, String aadStr, String secretKeyStr){
		int status = -1;
		try {

			cipher = Cipher.getInstance("AES/GCM/NoPadding", "SunJCE");
			byte[] key = (secretKeyStr).getBytes("UTF-8");
			MessageDigest sha = MessageDigest.getInstance("SHA-1");
			key = sha.digest(key);
			key = Arrays.copyOf(key, 16); // use only first 128 bit

			SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");


			/* 
			 * Reading IV file
			 */
			FileInputStream in = new FileInputStream(cipherTextFileName);
			byte[] buffer1 = new byte[4];
			byte[] iv = null;


			if (in.read(buffer1, 0, 4) != -1) {
				int sizeIV = new BigInteger(buffer1).intValue();
				iv = new byte[GCM_NONCE_LENGTH]; 
				in.read(iv, 0, 12);
				//				System.out.println(new String(iv));

			}


			GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);


			cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, parameterSpec);


			byte[] aad = aadStr.getBytes(); // this is authenticate associate data
			cipher.updateAAD(aad);

			byte[] ibuf = new byte[128 * 1024];
			int len;

			FileOutputStream out = new FileOutputStream(plainTextFileName);
			while ((len = in.read(ibuf)) != -1) {
				//				long updateStart = System.currentTimeMillis();
				byte[] obuf = cipher.update(ibuf, 0, len);
				//				long updateEnd = System.currentTimeMillis();
				//				System.out.println("updating time : " + (updateEnd - updateStart));
				//				long writeStart = System.currentTimeMillis();
				if ( obuf != null ) out.write(obuf);
				//				long writeEnd = System.currentTimeMillis();
				//				System.out.println("writing time : " + (writeEnd - writeStart));
			}


			byte[] obuf = cipher.doFinal();
			if ( obuf != null ) out.write(obuf);
			out.close();
			status = 0;

		} catch(Exception e) {
//			Dataview.debugger.logException(e);
		}
		return status;
	}

}