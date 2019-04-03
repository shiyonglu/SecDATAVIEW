package dataview.workflowexecutor;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.interfaces.ECKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.KeyAgreement;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import dataview.models.DWCRYPTO;
import dataview.models.Dataview;
import dataview.models.SshServerMain1;

public class CodeProvision {	
	private Socket server;
	private ServerSocket serverSocket;
	private BufferedReader input;
	private PrintStream output;

	PublicKey alicePubKey = null;
	KeyAgreement bobKeyAgree = null;
	private static byte[] bobSharedSecret;


	String bobSharedSecretString = null;


	public static void main(String[] args) throws Exception{
		
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				String[] args = new String[0];
				try {
					SshServerMain1.main(args);
				} catch (Exception e) {
					Dataview.debugger.logException(e);
				}
			}
		}).start();
		CodeProvision codeProvision = new CodeProvision();
		codeProvision.connectWithWorkflowExecutor();
	}

	
	public void run1 () {
		
		String actualSha256OfSshd = "c2c57221f1e2d04768a0252f0f5a58b6dcfa29f4617abfdcf99bc37435775258";// sh256 value of the standalone SSHD.jar
		String sha256ValueSsh;
		try {
			sha256ValueSsh = SHA256Hash.getSHA256Checksum("/home/ubuntu/sshd.jar");
			Dataview.debugger.logSuccessfulMessage("SSHD.jar sha256 value is being tested.");
			
			if (!actualSha256OfSshd.equals(sha256ValueSsh)) {
				Dataview.debugger.logErrorMessage("SSHD.jar sha256 value is not matched.");
				System.exit(0);
			}
			Dataview.debugger.logSuccessfulMessage("SSHD.jar sha256 value is trusted.");
		} catch (Exception e1) {
			Dataview.debugger.logException(e1);
		}
		
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				String[] args = new String[0];
				try {
					SshServerMain1.main(args);
				} catch (Exception e) {
					Dataview.debugger.logException(e);
				}
			}
		}).start();
		CodeProvision codeProvision = new CodeProvision();
		try {
			codeProvision.connectWithWorkflowExecutor();
		} catch (Exception e) {
			Dataview.debugger.logException(e);
		}
	}

	void connectWithWorkflowExecutor() throws Exception {
		try {
			Dataview.debugger.logSuccessfulMessage("Creating SSL connection thorugh port 7700");
			// loading certificate keystore from a given file
			KeyStore ks = KeyStore.getInstance("JKS");
			ks.load(new FileInputStream("/home/ubuntu/codeprovisioner.jks"), "compass".toCharArray());
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("sunX509");
			kmf.init(ks, "compass".toCharArray());
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("sunX509");
			tmf.init(ks);
			SSLContext sc = SSLContext.getInstance("TLS");
			TrustManager[] trustManagers = tmf.getTrustManagers();
			sc.init(kmf.getKeyManagers(), trustManagers, null);
			SSLServerSocketFactory ssf = sc.getServerSocketFactory();
			serverSocket= (SSLServerSocket) ssf.createServerSocket(7700);
			System.out.println("SSL Server Socket started");
			server = (SSLSocket) serverSocket.accept();
			System.out.println("SSL Server Socket Accepted");

			input = new BufferedReader(new InputStreamReader(server.getInputStream()));
			output = new PrintStream(server.getOutputStream());

		} catch(Exception exception) {
			Dataview.debugger.logException(exception);
		}

		while(server.isConnected()) {
			String message = input.readLine();
			if (message != null) {
				Dataview.debugger.logSuccessfulMessage("Client : " + message);

				String[] parts = message.split("XX");

				if (parts[0].equals("initialization")) {
					Dataview.debugger.logSuccessfulMessage("Creating SHA256 for codeProvisioner.jar");
					String SHA_value = SHA256Hash.getSHA256Checksum("/home/ubuntu/CodeProvisioner.jar");
					Dataview.debugger.logSuccessfulMessage("CodeProvision.jar SHA256 value # " + SHA_value);
					Dataview.debugger.logSuccessfulMessage("Returning SHA256");
					output.println("codeProvisionJarSHA256ValueXX" + SHA_value);

				} else if (parts[0].equals("createBobPubKeyEnc")) {
					Dataview.debugger.logSuccessfulMessage("Creating Bob PubKeyEnc");
					String bobPublicKeyEnc = createBobPubKeyEnc(parts[1]);
					Dataview.debugger.logSuccessfulMessage("Returning Bob public key");
					output.println("bobPubKeyEncXX" + bobPublicKeyEnc);

				} else if (parts[0].equals("executePhase1")) {
					Dataview.debugger.logSuccessfulMessage("Execute phase1");
					executePhase1();
					output.println("executePhase1CompleteXX");

				}  else if (parts[0].equals("generateSharedSecret")) {
					Dataview.debugger.logSuccessfulMessage("generate shared secret Key");
					bobSharedSecretString = generateSharedSecret();
					Dataview.debugger.logSuccessfulMessage("returning bobSharedSecretString # " + bobSharedSecretString);
					output.println("bobSharedSecretStringCompleteXX");
				} else if (parts[0].equals("decryptCodeSecretKey")) {
					decryptCodeSecretKey();
					output.println("decryptionCompleteXX");
				} else if (parts[0].equals("terminate")) {
					Dataview.debugger.logSuccessfulMessage("Terminating Task executor");
					output.println("terminateThread");
					server.close();
					serverSocket.close();

					URLClassLoader child = new URLClassLoader (new URL[] {new URL("file:///home/ubuntu/TaskExecutor.jar")}, CodeProvision.class.getClassLoader());
					@SuppressWarnings("rawtypes")
					Class classToLoad = Class.forName("dataview.workflowexecutor.TaskExecutor", true, child);
					@SuppressWarnings("unchecked")
					Method method = classToLoad.getDeclaredMethod("run");
					Object instance = classToLoad.newInstance();
					@SuppressWarnings("unused")
					Object result = method.invoke(instance);
				}
			} 
		}
		serverSocket.close();
	}



	void decryptCodeSecretKey() {
		// We assumed the encrypted secret key file is transferred by through socket programming.

		DWCRYPTO dwcrypto = new DWCRYPTO();
		try {
			if (dwcrypto.decryptFile("/home/ubuntu/CodePass.enc", "/home/ubuntu/CodePass.txt", "dataview", bobSharedSecretString) == 0) {
				Dataview.debugger.logSuccessfulMessage("TaskExecutorKey.enc is successfully decrypted to TaskExecutorKey.txt");

				String key = readTaskExecutorKey("/home/ubuntu/CodePass.txt");

				if (dwcrypto.decryptFile("/home/ubuntu/TaskExecutor.enc", "/home/ubuntu/TaskExecutor.jar", key, key) == 0) {
					Dataview.debugger.logSuccessfulMessage("TaskExecutor.enc is successfully decrypted to TaskExecutor.jar");
				} else {
					Dataview.debugger.logErrorMessage("TaskExecutor.enc decryption is not successful");
				}

			} else {
				Dataview.debugger.logErrorMessage("TaskExecutorKey.enc decryption is not successful");
			}
		} catch (Exception e) {
			Dataview.debugger.logErrorMessage("Error occured when decrypting TaskExecutorKey.enc / TaskExecutor.enc");
			Dataview.debugger.logException(e);
		}
	}


	String readTaskExecutorKey(String fileName) {
		BufferedReader br = null;
		FileReader fr = null;

		try {

			//br = new BufferedReader(new FileReader(FILENAME));
			fr = new FileReader(fileName);
			br = new BufferedReader(fr);

			String sCurrentLine;

			while ((sCurrentLine = br.readLine()) != null) {
				System.out.println(sCurrentLine);
				return sCurrentLine;
			}

		} catch (IOException e) {

			e.printStackTrace();

		} finally {

			try {

				if (br != null)
					br.close();

				if (fr != null)
					fr.close();

			} catch (IOException ex) {

				ex.printStackTrace();

			}

		}
		return null;
		//return sCurrentLine;
	}


	String createBobPubKeyEnc(String alicePubKeyEncStr) {
		byte[] alicePubKeyEnc = hexStringToByteArray(alicePubKeyEncStr);
		byte[] bobPubKeyEnc = null;
		/*
		 * Let's turn over to Bob. Bob has received Alice's public key in encoded
		 * format. He instantiates a EC public key from the encoded key material.
		 */

		//		byte[] alicePubKeyEnc = hexStringToByteArray(alicePubKeyEncString);
		KeyFactory bobKeyFac;
		try {
			bobKeyFac = KeyFactory.getInstance("EC");
			X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(alicePubKeyEnc);

			// bob creates AlicePubKey
			alicePubKey = bobKeyFac.generatePublic(x509KeySpec);

			/*
			 * Bob gets the DH parameters associated with Alice's public key. He must use
			 * the same parameters when he generates his own key pair.
			 */
			ECParameterSpec dhParamFromAlicePubKey = ((ECKey) alicePubKey).getParams();

			// Bob creates his own DH key pair
			System.out.println("BOB: Generate EC keypair ...");
			KeyPairGenerator bobKpairGen = KeyPairGenerator.getInstance("EC");
			bobKpairGen.initialize(dhParamFromAlicePubKey);
			KeyPair bobKpair = bobKpairGen.generateKeyPair();

			// Bob creates and initializes his ECDH KeyAgreement object
			System.out.println("BOB: Initialization ...");
			bobKeyAgree = KeyAgreement.getInstance("ECDH");
			bobKeyAgree.init(bobKpair.getPrivate());

			// Bob encodes his public key, and sends it over to Alice.
			bobPubKeyEnc = bobKpair.getPublic().getEncoded();			

		} catch (Exception e) {

			e.printStackTrace();
		}
		return byteArrayToHex(bobPubKeyEnc);
	}

	void executePhase1() {
		/*
		 * Bob uses Alice's public key for the first (and only) phase of his version of
		 * the DH protocol.
		 */

		System.out.println("BOB: Execute PHASE1 ...");
		try {
			bobKeyAgree.doPhase(alicePubKey, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	String generateSharedSecret() {
		try {
			bobSharedSecret = bobKeyAgree.generateSecret();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return byteArrayToHex(bobSharedSecret);
	}


	/*
	 * Converts a hex string to byte array
	 */
	public static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
					+ Character.digit(s.charAt(i+1), 16));
		}
		return data;
	}

	/*
	 * Converts byte array to hex
	 */
	public static String byteArrayToHex(byte[] a) {
		StringBuilder sb = new StringBuilder(a.length * 2);
		for(byte b: a)
			sb.append(String.format("%02x", b));
		return sb.toString();
	}


}