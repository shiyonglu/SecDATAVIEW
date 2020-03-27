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
import java.util.HashMap;
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
import dataview.workflowexecutor.SHA256Hash;
import dataview.workflowexecutor.CloudResourceManagement;
import dataview.workflowexecutor.WorkflowExecutor_Alpha;

/*
 * This class executes in the master node and a trusted premise. The control flow comes into isCodeProvisionOkay method from WorkflowExecutor. The purpose
 * of this class is to establish the remote attestation for the code and data in every worker node. It sends and guaratees the integrity of the code provioner at
 * the remote side. Also, it sends all the necessary workflow related files and code within the TaskExecutor to every trusted worker nodes.  
 * We need to getRemoteAttestationStatus() method at first for the understanding. Its the driver method for this class.  
 * 
 * The CodeProvioner module will be deployed at each worker node. The module is for secure transfer and integrity 
 * of code and data from the master node to the worker node.
 * 
 * 
 * Step 1: generate random password (random six characters)for encrypting TaskExecutor.jar
 * Step 2: generate SHA256 digest for CodeProvisioner.jar
 * Step 3: Encrypting the TaskExecutor.jar by the generated password.
 * Step 4: Copying the encrypted TaskExecutor.enc, CodeProvisioner.jar and codeProvisioner.jks to each remote worker nodes.
 * Step 5: Establishing SSL socket connection with each of the remote worker nodes
 * Step 6: Comparing the SHA256 digest of CodeProvioner at the master node and CodeProvioner with the worker node  
 * Step 7: Sending workflowDataDir and workflowConfidentialInfo (config.txt, clientKeyStore for TaskExecutor) and password for TaskExecutor
 */


public class CodeProvisioningAttestation {

	private ArrayList<String> IPs = new ArrayList<String>();
	private String destinationFolder;
//	private VMProvisionerOriginal vmProvisioner;
	
	private CloudResourceManagement sgxProvisioner;
	private CloudResourceManagement amdProvisioner;
	private CloudResourceManagement awsProvisioner;
	
	private HashMap<String, String> ipPool = new HashMap<>();
	
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
	private ArrayList<Thread> threadForLibDir = new ArrayList<>();
	private ArrayList<Thread> threadForDataDir = new ArrayList<>();

	private boolean isFinished;

	private static byte[] aliceSharedSecret;
	
	private boolean isOkay;
	private String alicePubKeyEncStr;
	private String bobPublicKeyEnc;
	private String aliceSharedSecretString;
	String workflowLibDir;

	GlobalSchedule gsch;
	
	public CodeProvisioningAttestation(ArrayList<String> IPs, String workflowLibDir, String destinationFolder, CloudResourceManagement sgxProvisioner, 
			CloudResourceManagement amdProvisioner, CloudResourceManagement awsProvisioner, int PORT_NO, HashMap<String, String> ipPool, GlobalSchedule gsch) {
		this.IPs = IPs;
		this.workflowLibDir = workflowLibDir;
		this.destinationFolder = destinationFolder;
		
		this.sgxProvisioner = sgxProvisioner;
		this.amdProvisioner = amdProvisioner;
		this.awsProvisioner = awsProvisioner;
		this.ipPool = ipPool;
		this.gsch = gsch;
		
		this.PORT_NO = PORT_NO;
		totalSuccess = 0;
		isOkay = true;
		isFinished = false;
	}

	
	boolean getRemoteAttestationStatus(ArrayList<String> IPs) {
		//runCodeProvision();
		createSHA256AndPassword(workflowLibDir);
		encryptTaskExecutorJar(workflowLibDir);
		sendFilesFromWorkflowLibDir(); 
		startSocketProgramming();
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


//WCPAC.step 16: DigestCodeProvisionerM=sha256(codeProvisioner.jar)   this step calculates the value of codeProvisioner.jr in the TEE 
//WCPAC.step 17: codePass=keyGen() this step generates a new random password to be used to encrypt TaskExecutor.jar 

	public void createSHA256AndPassword(String workflowLibDir) {
		try {
//			Step 1: generate random password for encrypting TaskExecutor.jar
			generatePassword();

//			taskExecutor_jar_SHA256 = SHA256Hash.getSHA256Checksum("confidentialInfo/TaskExecutor.jar");
//			Dataview.debugger.logSuccessfulMessage("TaskExecutor.jar SHA256 value # " + taskExecutor_jar_SHA256);
			
//			Step 2: generate SHA256 digest for CodeProvisioner.jar
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
//WCPAC.step 18:  encrypt(codePass, AD , taskExecutor.jar)->taskExecutor.enc, this step encrypt the TaskExecutor.jar to the TaskExecutor.end using password generated in WCPAc.step 17
//			Step 3: Encrypting the TaskExecutor.jar by the generated password.
			Dataview.debugger.logSuccessfulMessage("Encryption password is " + password);
			dwcrypto.encryptFile(workflowLibDir + "/TaskExecutor.enc", password, "confidentialInfo/TaskExecutor.jar", password);
			Dataview.debugger.logSuccessfulMessage("TaskExecutor has been encrypted.");
//			Step 4: Copying the encrypted TaskExecutor.enc to each remote worker nodes. 
			for (int i = 0; i < IPs.size(); i++) {
				if (ipPool.get(IPs.get(i)).equals(WorkflowExecutor_Alpha.SGX)) {
					sgxProvisioner.copyFileVM(workflowLibDir + "/TaskExecutor.enc", destinationFolder, IPs.get(i));
				} else if (ipPool.get(IPs.get(i)).equals(WorkflowExecutor_Alpha.AMD)) {
					amdProvisioner.copyFileVM(workflowLibDir + "/TaskExecutor.enc", destinationFolder, IPs.get(i));
				} else if (ipPool.get(IPs.get(i)).equals(WorkflowExecutor_Alpha.AWS)) {
					awsProvisioner.copyFileVM(workflowLibDir + "/TaskExecutor.enc", destinationFolder, IPs.get(i));
				}
			}
			
		} catch (Exception e) {
			Dataview.debugger.logException(e);
		}		
	}

//	Step 4: Copying the encrypted TaskExecutor.enc, CodeProvisioner.jar and codeProvisioner.jks to each remote worker nodes.
	public void sendFilesFromWorkflowLibDir() {

		Dataview.debugger.logSuccessfulMessage("Retrieving necessary files workflowLibDir is started");
		ArrayList<String> originalfiles = retrieveFileNames(workflowLibDir);
		Dataview.debugger.logSuccessfulMessage("Retrieving necessary files workflowTaskDir is finished");
		Dataview.debugger.logSuccessfulMessage("Retrieving necessary files workflowLibDir is started");
		
		Dataview.debugger.logSuccessfulMessage("Retrieving necessary files workflowLibDir is finished");

		for (int i = 0; i < IPs.size(); i++) {
			threadForWorkflowLibDir(originalfiles, IPs.get(i));
		}
		
		for (Thread thread : threadForLibDir) {
			thread.start();
		}

		for (Thread thread : threadForLibDir) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				Dataview.debugger.logException(e);
			}
		}
		
	}

	
	
	
	
	private void threadForWorkflowLibDir(ArrayList<String> originalfiles, String ip ) {
		
		/*Thread localScheduleThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				ArrayList<String> files = new ArrayList<String>(originalfiles);
				Iterator<String> itr = files.iterator();
				while (itr.hasNext()) {
					Dataview.debugger.logSuccessfulMessage("VM " + ip + " ");

					if (ipPool.get(ip).equals(WorkflowExecutor_Alpha.SGX)) {
						sgxProvisioner.copyFileVM(itr.next(), destinationFolder, ip);
					} else if (ipPool.get(ip).equals(WorkflowExecutor_Alpha.AMD)) {
						amdProvisioner.copyFileVM(itr.next(), destinationFolder, ip);
					} else if (ipPool.get(ip).equals(WorkflowExecutor_Alpha.AWS)) {
						awsProvisioner.copyFileVM(itr.next(), destinationFolder, ip);
					}
					itr.remove();
				}

			}
		});*/
		//WCPAC.step 19: send(codeProvisioner.jar,taskExecutor.enc,codeProvisionerSSLCertificate). this step copies codeprovisoner.jar taskexecutor.enc and codeprovisoner's ssl certificate file to the TEE
		
		Thread localScheduleThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				if (ipPool.get(ip).equals(WorkflowExecutor_Alpha.SGX)) {
					sgxProvisioner.copyFileVM("workflowLibDir/CodeProvisioner.jar", destinationFolder, ip);
					sgxProvisioner.copyFileVM("workflowLibDir/codeprovisioner.jks", destinationFolder, ip);
					sgxProvisioner.copyFileVM("workflowLibDir/TaskExecutor.enc", destinationFolder, ip);
				} else if (ipPool.get(ip).equals(WorkflowExecutor_Alpha.AMD)) {
					amdProvisioner.copyFileVM("workflowLibDir/CodeProvisioner.jar", destinationFolder, ip);
					amdProvisioner.copyFileVM("workflowLibDir/codeprovisioner.jks", destinationFolder, ip);
					amdProvisioner.copyFileVM("workflowLibDir/TaskExecutor.enc", destinationFolder, ip);
				} else if (ipPool.get(ip).equals(WorkflowExecutor_Alpha.AWS)) {
					//System.out.printl
					awsProvisioner.copyFileVM("workflowLibDir/CodeProvisioner.jar", destinationFolder, ip);
					awsProvisioner.copyFileVM("workflowLibDir/codeprovisioner.jks", destinationFolder, ip);
					awsProvisioner.copyFileVM("workflowLibDir/TaskExecutor.enc", destinationFolder, ip);
				} 
				
			}
		});
		threadForLibDir.add(localScheduleThread);
		
	}


	public void sendFilesFromWorkflowDataDir(String ip) {

		Dataview.debugger.logSuccessfulMessage("Retrieving necessary files workflowDataDir is started");
		ArrayList<String> originalfiles = retrieveFileNames("workflowDataDir");
		Dataview.debugger.logSuccessfulMessage("Retrieving necessary files workflowDataDir is finished");
		
/// new changes
		
		ArrayList<String> files = new ArrayList<String>(originalfiles);
		Iterator<String> itr = files.iterator();
		while (itr.hasNext()) {
			if (ipPool.get(ip).equals(WorkflowExecutor_Alpha.SGX)) {
				sgxProvisioner.copyFileVM(itr.next(), destinationFolder, ip);
			} else if (ipPool.get(ip).equals(WorkflowExecutor_Alpha.AMD)) {
				amdProvisioner.copyFileVM(itr.next(), destinationFolder, ip);
			} else if (ipPool.get(ip).equals(WorkflowExecutor_Alpha.AWS)) {
				awsProvisioner.copyFileVM(itr.next(), destinationFolder, ip);
			}
			itr.remove();
		} 

		
		/*for (int i = 0; i < IPs.size(); i++) {
			ArrayList<String> files = new ArrayList<String>(originalfiles);
			Iterator<String> itr = files.iterator();
			while (itr.hasNext()) {
				if (ipPool.get(IPs.get(i)).equals(WorkflowExecutor_Alpha.SGX)) {
					sgxProvisioner.copyFileVM(itr.next(), destinationFolder, IPs.get(i));
				} else if (ipPool.get(IPs.get(i)).equals(WorkflowExecutor_Alpha.AMD)) {
					amdProvisioner.copyFileVM(itr.next(), destinationFolder, IPs.get(i));
				} else if (ipPool.get(IPs.get(i)).equals(WorkflowExecutor_Alpha.AWS)) {
					awsProvisioner.copyFileVM(itr.next(), destinationFolder, IPs.get(i));
				}
				itr.remove();
			}
		}*/
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



	public void startSocketProgramming() {
		
		
		try {
			Dataview.debugger.logSuccessfulMessage("Waiting for one minutes to run the Code Provisoner.jar");
			Thread.sleep(1 * 60 * 1000);
		} catch(Exception e) {
			Dataview.debugger.logException(e);
		}
//		Step 5: Establishing SSL socket connection with each of the remote worker nodes
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
				Dataview.debugger.logException(e);
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

					/*
					 * WCPAC.step 21: start Code Provisioner attestation at the Worker Node, this step start the code provisoner in the worker node to
					 * 
					 *    
					 *  a) Conduct integrity check of (CodeProvisioner.jar), required for remote code attestation for CodeProvisioner.jar
					 *
					 *  b) Receive the password and other necessary files for Task Executor , and Workflow input data.
					 *  
					 *  c) Decrypt Task Executor jar file and run it.
					 * 
					 */
				//	WCPAC.step 21: message(startCodeProvisionerAttestation),  Sending  message to code provisioner to start its attestation process
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
//WCPAC.step 23: message(DigestCodeProvisionerW), this step the digest of of codeprovisioner.jar in the worker is received
						Dataview.debugger.logSuccessfulMessage("Server run " + vm + " : " + messageInput);
						Dataview.debugger.logSuccessfulMessage("Message received from : " + IPs.get(vm) + " " + messageInput);
						String[] messages = messageInput.split("XX");
//WCPAC.step 24: (DigestCodeProvisionerM== DigestCodeProvisionerW) ? Continue : Terminate , Comparing the SHA256 digest of CodeProvioner at the master node and CodeProvioner with the worker node
						//Step 6: Comparing the SHA256 digest of CodeProvioner at the master node and CodeProvioner with the worker node
						if (messages[0].equalsIgnoreCase("codeProvisionJarSHA256Value")) { 
							Dataview.debugger.logSuccessfulMessage("codeProvisionJarSHA256Value = " + messages[1]);
							if (codeProvisioner_jar_SHA256.equals(messages[1])) {
//								outputs.get(vm).println("createBobPubKeyEncXX" + alicePubKeyEncStr);                      /// ------------------------->NO NEED
//								Step 7: Sending workflowDataDir and workflowConfidentialInfo (config.txt, clientKeyStore for TaskExecutor) and password for TaskExecutor
//WCPAC.step 26: send(TaskExecutorSSLCertificate,WorkflowInputData,TaskExecutorConfig), this step send config.txt, workflow input data and Taskexecutor's ssl certificate to the worker
								
								sendFilesFromWorkflowDataDir(IPs.get(vm));
								if (ipPool.get(IPs.get(vm)).equals(WorkflowExecutor_Alpha.SGX)) {
									sgxProvisioner.copyFileVM("confidentialInfo/config.txt", destinationFolder, IPs.get(vm));
									
									Dataview.debugger.logSuccessfulMessage("File transfer is completed for config.txt");
									sgxProvisioner.copyFileVM("confidentialInfo/clientkeystore", destinationFolder, IPs.get(vm));
									Dataview.debugger.logSuccessfulMessage("File transfer is completed for clientkeystore");
								} else if (ipPool.get(IPs.get(vm)).equals(WorkflowExecutor_Alpha.AMD)) {
									amdProvisioner.copyFileVM("confidentialInfo/config.txt", destinationFolder, IPs.get(vm));
									
									Dataview.debugger.logSuccessfulMessage("File transfer is completed for config.txt");
									amdProvisioner.copyFileVM("confidentialInfo/clientkeystore", destinationFolder, IPs.get(vm));
									Dataview.debugger.logSuccessfulMessage("File transfer is completed for clientkeystore");
								} else if (ipPool.get(IPs.get(vm)).equals(WorkflowExecutor_Alpha.AWS)) {
									awsProvisioner.copyFileVM("confidentialInfo/config.txt", destinationFolder, IPs.get(vm));
									
									Dataview.debugger.logSuccessfulMessage("File transfer is completed for config.txt");
									awsProvisioner.copyFileVM("confidentialInfo/clientkeystore", destinationFolder, IPs.get(vm));
									Dataview.debugger.logSuccessfulMessage("File transfer is completed for clientkeystore");
//									awsProvisioner.copyFileVM(SECRET_KEY, destinationFolder, IPs.get(vm));
//									
//									Dataview.debugger.logSuccessfulMessage("File transfer is completed for amazon pem file");
								}
//WCPAC.step 25: message(codePass), this step sends TaskExecutor.enc password to the worker node 			
								outputs.get(vm).println("decryptTaskExecutorXX" + password);
							} else {
								isOkay = false;
							}
						}
						
						
						if (messages[0].equalsIgnoreCase("decryptionComplete")) {
							totalSuccess++;
						
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

				} catch(Exception e) {
					Dataview.debugger.logException(e);
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