package dataview.workflowexecutor;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import javax.crypto.KeyAgreement;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import dataview.models.DWCRYPTO;
import dataview.models.Dataview;
import dataview.models.GlobalSchedule;

public class CodeProvisionAttestation {

	private ArrayList<String> IPs = new ArrayList<String>();
	private String destinationFolder;
	private VMProvisioner vmProvisioner;
	private int PORT_NO;
	private int totalSuccess;

	String taskExecutor_jar_SHA256 = null;
	String codeProvisioner_jar_SHA256 = null;
	String password = "";

	KeyAgreement aliceKeyAgree = null;
	private String SECRET_KEY = "confidentialInfo/dataview1.pem";


	private ArrayList<Socket> clientSockets = new ArrayList<Socket>();
	private ArrayList<BufferedReader> inputs = new ArrayList<BufferedReader>();
	private ArrayList<PrintStream> outputs = new ArrayList<PrintStream>();
	private ArrayList<Thread> threadList = new ArrayList<Thread>();

	private boolean isFinished;

	private static byte[] aliceSharedSecret;
	
	private boolean isOkay;
	private String alicePubKeyEncStr;
	private String bobPublicKeyEnc;
	private String aliceSharedSecretString;
	String workflowLibDir;

	public CodeProvisionAttestation(ArrayList<String> IPs, String workflowLibDir, String destinationFolder, VMProvisioner vmProvisioner, int PORT_NO) {
		this.IPs = IPs;
		this.workflowLibDir = workflowLibDir;
		this.destinationFolder = destinationFolder;
		this.vmProvisioner = vmProvisioner;
		this.PORT_NO = PORT_NO;
		totalSuccess = 0;
		isOkay = true;
		isFinished = false;
	}

	
	boolean isCodeProvisionOkay(ArrayList<String> IPs, GlobalSchedule gsch) {
		//runCodeProvision();
		createSHA256AndPassword(workflowLibDir);
		encryptTaskExecutorJar(workflowLibDir);
		sendingFilestoVM();
		startSocketProgramming(gsch);
		return isOkay;
	}


	String createAlicePubKeyEnc() {
		byte[] alicePubKeyEnc = null;
		/*
		 * Alice creates her own EC key pair with 256-bit key size
		 */
		System.out.println("ALICE: Generate EC keypair ...");
		KeyPairGenerator aliceKpairGen;
		try {
			aliceKpairGen = KeyPairGenerator.getInstance("EC");
			aliceKpairGen.initialize(256);
			KeyPair aliceKpair = aliceKpairGen.generateKeyPair();

			// Alice creates and initializes her ECDH KeyAgreement object
			System.out.println("ALICE: Initialization ...");
			aliceKeyAgree = KeyAgreement.getInstance("ECDH");
			aliceKeyAgree.init(aliceKpair.getPrivate());

			// Alice encodes her public key, and sends it over to Bob.
			alicePubKeyEnc = aliceKpair.getPublic().getEncoded();			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return byteArrayToHex(alicePubKeyEnc);
	}

	void executePhase1(String bobPubKeyEncStr) {
		/*
		 * Alice uses Bob's public key for the first (and only) phase of her version of
		 * the DH protocol. Before she can do so, she has to instantiate a DH public key
		 * from Bob's encoded key material.
		 */

		byte[] bobPubKeyEnc = hexStringToByteArray(bobPubKeyEncStr);

		KeyFactory aliceKeyFac;
		try {
			aliceKeyFac = KeyFactory.getInstance("EC");
			X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(bobPubKeyEnc);
			PublicKey bobPubKey = aliceKeyFac.generatePublic(x509KeySpec);
			System.out.println("ALICE: Execute PHASE1 ...");
			aliceKeyAgree.doPhase(bobPubKey, true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	String generateSharedSecret() {
		aliceSharedSecret = aliceKeyAgree.generateSecret();		
		return byteArrayToHex(aliceSharedSecret);
	}

	/*
	 * Converts a hex string to byte array
	 */
	public byte[] hexStringToByteArray(String s) {
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
	public String byteArrayToHex(byte[] a) {
		StringBuilder sb = new StringBuilder(a.length * 2);
		for(byte b: a)
			sb.append(String.format("%02x", b));
		return sb.toString();
	}




	public void createSHA256AndPassword(String workflowLibDir) {
		try {
			generatePassword();

			taskExecutor_jar_SHA256 = SHA256Hash.getSHA256Checksum("confidentialInfo/TaskExecutor.jar");
			Dataview.debugger.logSuccessfulMessage("TaskExecutor.jar SHA256 value # " + taskExecutor_jar_SHA256);
			codeProvisioner_jar_SHA256 = SHA256Hash.getSHA256Checksum(workflowLibDir + "/CodeProvisioner.jar");
			Dataview.debugger.logSuccessfulMessage("CodeProvision.jar SHA256 value # " + codeProvisioner_jar_SHA256);

		} catch (Exception e) {
			Dataview.debugger.logException(e);
		}
	}




	private void generatePassword() {
		Random r = new Random();
		int low = 0;
		int high = 25;
		for (int i = 0; i < 6; i++) {
			int result = r.nextInt(high-low) + low;
			password += ((char)('a' + result));
		}

	}




	public void encryptTaskExecutorJar(String workflowLibDir) {
		DWCRYPTO dwcrypto = new DWCRYPTO();
		try {
			Dataview.debugger.logSuccessfulMessage("Encryption password is " + password);
			dwcrypto.encryptFile(workflowLibDir + "/TaskExecutor.enc", password, "confidentialInfo/TaskExecutor.jar", password);
			
			for (int i = 0; i < IPs.size(); i++) {
				vmProvisioner.copyFileVM(workflowLibDir + "/TaskExecutor.enc", destinationFolder, IPs.get(i));
			}
			
		} catch (Exception e) {
			Dataview.debugger.logException(e);
		}		
	}


	public void sendingFilestoVM() {

		Dataview.debugger.logSuccessfulMessage("Retrieving necessary files workflowLibDir is started");
		ArrayList<String> originalfiles = retrieveFileNames(workflowLibDir);
		Dataview.debugger.logSuccessfulMessage("Retrieving necessary files workflowTaskDir is finished");
		Dataview.debugger.logSuccessfulMessage("Retrieving necessary files workflowLibDir is started");
		
		Dataview.debugger.logSuccessfulMessage("Retrieving necessary files workflowLibDir is finished");

		for (int i = 0; i < IPs.size(); i++) {
			ArrayList<String> files = new ArrayList<String>(originalfiles);
			Iterator<String> itr = files.iterator();
			boolean isFoundException = false;
			String file = null;
			while (itr.hasNext()) {
				try {
					if (!isFoundException) {
						file = itr.next();
					}
					Dataview.debugger.logSuccessfulMessage("VM " + IPs.get(i) + " ");

					vmProvisioner.copyFileVM(file, destinationFolder, IPs.get(i));
					itr.remove();
					isFoundException = false;
				} catch(Exception exception) {
					Dataview.debugger.logException(exception);
					Dataview.debugger.logErrorMessage("File sending exception is caught " + exception);
					Dataview.debugger.logErrorMessage("Trying to resend " + file);
					isFoundException = true;
					try {
						Dataview.debugger.logSuccessfulMessage("Waiting for 15 seconds..");
						Thread.sleep(15000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}


	ArrayList<String> retrieveFileNames(String directory) {
		ArrayList<String> fileNames = new ArrayList<String>();
		File folder = new File(directory);
		File[] listOfFiles = folder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				Dataview.debugger.logSuccessfulMessage("File " + listOfFiles[i].getName());
				fileNames.add(directory + "/" + listOfFiles[i].getName().trim());
			}

		}
		return fileNames;
	}




	public void startSocketProgramming(GlobalSchedule gsch) {
		
		
		try {
			Dataview.debugger.logSuccessfulMessage("Waiting for two minutes to run the codeProviioner.jar");
			Thread.sleep(2 * 60 * 1000);
		} catch(Exception exception) {
			Dataview.debugger.logException(exception);
		}

		for(int  i = 0; i < IPs.size(); i++) {
			try {
				KeyStore ks = KeyStore.getInstance("JKS");
				ks.load(new FileInputStream("workflowLibDir/codeprovisioner.jks"), "compass".toCharArray());
				KeyManagerFactory kmf = KeyManagerFactory.getInstance("sunX509");
				kmf.init(ks, "compass".toCharArray());
				TrustManagerFactory tmf = TrustManagerFactory.getInstance("sunX509");
				tmf.init(ks);
				SSLContext sc = SSLContext.getInstance("TLS");
				TrustManager[] trustManagers = tmf.getTrustManagers();
				sc.init(kmf.getKeyManagers(), trustManagers, null);
				SSLSocketFactory ssf = sc.getSocketFactory();
				SSLSocket clientSocket = (SSLSocket) ssf.createSocket(IPs.get(i), PORT_NO);
				Dataview.debugger.logSuccessfulMessage("SSL Client Socket started in ip" + IPs.get(i));
				Dataview.debugger.logSuccessfulMessage("SSL Client Socket started handshake in ip" + IPs.get(i));
				clientSocket.startHandshake();
				Dataview.debugger.logSuccessfulMessage("SSL Client Socket ready!");
				
				
				
				BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				PrintStream output = new PrintStream(clientSocket.getOutputStream());

				clientSockets.add(clientSocket);
				inputs.add(input);
				outputs.add(output);
			} catch (Exception e1) {
				Dataview.debugger.logException(e1);
			}
		}

		
		
		for (int i = 0; i < gsch.length(); i++) {
			runThread(i, IPs);
		}

		for (Thread thread : threadList) {
			thread.start();
		}

		for (Thread thread : threadList) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		for (int i = 0; i < clientSockets.size(); i++) {
			try {
				clientSockets.get(i).close();
				inputs.get(i).close();
				outputs.get(i).close();
			} catch (Exception e) {
				Dataview.debugger.logException(e);
			}
		}
		
		Dataview.debugger.logSuccessfulMessage("End of the CodeProvisionAttestation");
	}

	void runThread(int vm, ArrayList<String> IPs) {
		alicePubKeyEncStr = createAlicePubKeyEnc();
		
		Thread localScheduleThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					// Sending settings initialization to CodeProvision
					Dataview.debugger.logSuccessfulMessage("Sending settings initialization to CodeProvision at : " + inputs.get(vm));
					outputs.get(vm).println("initializationXX");
					
					
					

					while(!isFinished && clientSockets.get(vm).isConnected()) {
						String messageInput = inputs.get(vm).readLine();
						if (messageInput == null) {
							break;
						}
						
						if (messageInput.equals("terminateThread")) {
							Dataview.debugger.logSuccessfulMessage("Terminnating thread " + vm);
							break;
						}

						Dataview.debugger.logSuccessfulMessage("Server run " + vm + " : " + messageInput);
						Dataview.debugger.logSuccessfulMessage("Message received from : " + IPs.get(vm) + " " + messageInput);
						String[] messages = messageInput.split("XX");
						
						if (messages[0].equalsIgnoreCase("codeProvisionJarSHA256Value")) { 
							Dataview.debugger.logSuccessfulMessage("codeProvisionJarSHA256Value = " + messages[1]);
							if (codeProvisioner_jar_SHA256.equals(messages[1])) {
								outputs.get(vm).println("createBobPubKeyEncXX" + alicePubKeyEncStr);
							} else {
								isOkay = false;
							}
						}
						
						
						if (messages[0].equalsIgnoreCase("bobPubKeyEnc")) {
							Dataview.debugger.logSuccessfulMessage("bobPubKeyEnc = " + messages[1]);
							bobPublicKeyEnc = messages[1];
							executePhase1(bobPublicKeyEnc);
							outputs.get(vm).println("executePhase1XX");
						}
						
						
						if (messages[0].equalsIgnoreCase("executePhase1Complete")) {
							Dataview.debugger.logSuccessfulMessage("executePhase1 is Completed on other side");
							aliceSharedSecretString = generateSharedSecret();
							Dataview.debugger.logSuccessfulMessage("AliceSharedSecretString # " + aliceSharedSecretString);
							outputs.get(vm).println("generateSharedSecretXX");
						}
						
						if (messages[0].equalsIgnoreCase("bobSharedSecretStringComplete")) {
//							writePasswordIntoBinaryFile(workflowLibDir + "/CodePass.txt");
							writePasswordFile("confidentialInfo/CodePass.txt");
							DWCRYPTO dwcrypto = new DWCRYPTO();
							dwcrypto.encryptFile(workflowLibDir + "/CodePass.enc", "dataview", "confidentialInfo/CodePass.txt", aliceSharedSecretString);
							vmProvisioner.copyFileVM(workflowLibDir + "/CodePass.enc", destinationFolder, IPs.get(vm));
							outputs.get(vm).println("decryptCodeSecretKeyXX");
							// send file to other end
//							sendingFilestoVM(vmProvisioner, destinationFolder);
							//vmProvisioner.copyFileVM(workflowLibDir + "/CodePass.enc", destinationFolder, IPs.get(vm));
							
							/*if (!aliceSharedSecretString.equals(bobSharedSecretString)) {
								isOkay = false;
							} else {
								totalSuccess++;
							}*/
							
						}
						
						if (messages[0].equalsIgnoreCase("decryptionComplete")) {
							totalSuccess++;
							
							vmProvisioner.copyFileVM("confidentialInfo/config.txt", destinationFolder, IPs.get(vm));
							Dataview.debugger.logSuccessfulMessage("File transfer is completed for conifg.txt");
							
							vmProvisioner.copyFileVM("confidentialInfo/clientkeystore", destinationFolder, IPs.get(vm));
							Dataview.debugger.logSuccessfulMessage("File transfer is completed for clientkeystore");
							
							vmProvisioner.copyFileVM(SECRET_KEY, destinationFolder, IPs.get(vm));
							Dataview.debugger.logSuccessfulMessage("File transfer is completed for amazon pem file");
							
							
							outputs.get(vm).println("reply");
						}
						
						if (totalSuccess == IPs.size()) {
							isFinished = true;
							String reply = "terminateXX";
							
							for (PrintStream out : outputs) {
								out.println(reply);
							}
						}
					}

				} catch(Exception exception) {
					Dataview.debugger.logException(exception);
				}
			}

			private void writePasswordFile(String fileName) {
				BufferedWriter bw = null;
				FileWriter fw = null;

				try {
					fw = new FileWriter(fileName);
					bw = new BufferedWriter(fw);
					bw.write(password);

					System.out.println("Done");

				} catch (Exception e) {
					Dataview.debugger.logException(e);

				} finally {

					try {

						if (bw != null)
							bw.close();

						if (fw != null)
							fw.close();

					} catch (Exception ex) {

						Dataview.debugger.logException(ex);

					}

				}
			}

			@SuppressWarnings("unused")
			private void writePasswordIntoBinaryFile(String fileName) {
				try {
					FileOutputStream out = new FileOutputStream(fileName);
					byte[] key = (password).getBytes("UTF-8");
					out.write(key);
					out.close();
				} catch (Exception e) {
					Dataview.debugger.logException(e);
				}
			}
		});
		threadList.add(localScheduleThread);
	}

}