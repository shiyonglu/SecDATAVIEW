 package dataview.workflowexecutor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import dataview.models.Dataview;
import dataview.models.GlobalSchedule;
import dataview.models.LocalSchedule;
import dataview.workflowexecutor.CodeProvisionAttestation;
import dataview.workflowexecutor.VMProvisioner;
import dataview.workflowexecutor.WorkflowExecutor;



/**
 * This is the first class file to have a thorough understanding of the overall communication process.
 * The first method is execute. 
 * The Alpha workflow executor is supporting multi-thread task submission to the task executor correspondingly.
 * the workflow input is read from workflowlibdir which is the project folder path. 
 *
 */


public class WorkflowExecutor_Alpha extends WorkflowExecutor{
	private int totalSuccess = 0;
	private final int PORT_NO = 7700;
	private int totalTasks = 0;
	
	public static final String SGX = "SGX";
	public static final String AMD = "AMD";
	public static final String AWS = "AWS";
	
	public static final String SGX_IMG_SRC_FILE = "/home/ishtiaq/sgxlkl-disk.img.enc";
	public static final String SGX_IMG_DST_FOLDER = "/home/ubuntu/";
	public static final String SGX_SCRIPT_SRC_FILE = "/home/ishtiaq/Desktop/ishtiaq_git/secureDW/machineScript/SGX/sgx-lkl-java-encrypted-dataview.sh";
	public static final String SGX_SCRIPT_DST_FOLDER = "/home/ubuntu/";
	
	public static final String AMD_IMG_SRC_FILE = "/home/ishtiaq/ubuntu-18.04.qcow2";
	public static final String AMD_IMG_DST_FOLDER = "/home/mofrad-s/";
	public static final String AMD_SCRIPT_SRC_FILE = "/home/ishtiaq/Desktop/ishtiaq_git/secureDW/machineScript/AMD/vm1-launch-dataview-sev.sh";
	public static final String AMD_SCRIPT_DST_FOLDER = "/home/mofrad-s/";
	
	public static final String SGX_SEV_AWS_USER_NAME = "ubuntu";
	public static final String SGX_SEV_AWS_USER_PASSWORD = "dataview";		
	
	public static final String AMD_SERVER_IP = "172.30.18.202";
	public static final String AMD_SERVER_USER_NAME = "root";
	public static final String AMD_SERVER_PASSWORD = "Compass$2019";
	
	private int totalSGXMachines = 0;
	private int totalAMDMachines = 0;
	private int totalAWSMachines = 0;
	
	long startTime;
	long finishTime;

	private boolean isFinished = false;

	private String workflowTaskDir;
	private String workflowLibDir;
	private final String destinationFolder = "/home/ubuntu/";

	private GlobalSchedule gsch;

	private ArrayList<SSLSocket> clientSockets = new ArrayList<>();
	private ArrayList<BufferedReader> inputs = new ArrayList<BufferedReader>();
	private ArrayList<PrintStream> outputs = new ArrayList<PrintStream>();

	private HashMap<String, String> taskVMip = new HashMap<String, String>();



	private ArrayList<Thread> threadList = new ArrayList<Thread>();
	private ArrayList<Thread> threadForInitVM = new ArrayList<>();
	
	private HashMap<String, String> ipPool = new HashMap<>();

	VMProvisioner sgxProvisioner = null;
	VMProvisioner amdProvisioner = null;
	VMProvisioner awsProvisioner = null;
	

	
	/**
	 * The constructor is used to set the path of two folders and global schedule"
	 * 
	 * @param workflowTaskDir
	 * @param workflowLibDir
	 * @param gsch
	 */	
	public WorkflowExecutor_Alpha(String workflowTaskDir, String workflowLibDir, GlobalSchedule gsch) {
		this.workflowTaskDir = workflowTaskDir;
		this.workflowLibDir = workflowLibDir;
		this.gsch = gsch;
		sgxProvisioner = new VMProvisionerSGX();
		amdProvisioner = new VMProvisionerAMD();
		awsProvisioner = new VMProvisionerAWS();
	}

	
	/*
	 * Step 1: Collecting the IP addresses of secure remote machines into data structure, IPs, from the Cloud Resource Management module
	 *       
	 * Step 2: Mapping ip addresses, IPs, to each local schedule.
	 * 
	 *  Q1: is step 2 based on step 1? 
	 *  Q2: which step provisions the worker nodes? // provision the bare machines, not the software services.
	 *  focused on "what" issue, not "how" and not "where", "who", what does each step do?
	 *  Q3: what did we do with the workflow inputs and workflow outputs?
	 *  
	 * Step 3: Running sshd in each worker node. Running sshd.jar
	 *         The initiate cloud resource management to provision the machines.
	 *         And wait for cloud resource management to provision the machines and return the control flow to the workflow executor.
	 *         
	 * Step 4: Halting the system for couple of seconds [5 seconds]
	 * 
	 * 
	 * 
	 * Step 5: Securely sends the Task Executor code and data to each worker nodes, verify their integrity after transfer and run the Task Executor
	 * 
	 * Transferring the control flow to the Code Provision Attestation 
	 * Checking the every worker node whether it is correctly provisioned / integrity and secured.
	 * 
	 * Step 6: Setting up a messaging socket connection to each Task Executor at each worker node.
	 * 
	 * Step 7: Sending each local schedule through SSL socket to its corresponding worker node and then communicate with each Task Executor to execute each 
	 * local schedule.
	 * 
	 * 
	 */
	public void execute() {
		Dataview.debugger.logSuccessfulMessage("WorkflowExecutor has started");
		Dataview.debugger.logSuccessfulMessage("Going to provision machines");
		
		// Step 1: Collecting the IP addresses of secure remote machines into data structure, IPs, from the Cloud Resource Management module
		Dataview.debugger.logSuccessfulMessage("Step 1: Collecting the IP addresses of secure remote machines into data structure, IPs, from the Cloud Resource Management module: "); // new addition
		ArrayList<String> IPs = generateIPPool();
		
		
		Dataview.debugger.logSuccessfulMessage("Provisioning machine is successful");

		Dataview.debugger.logSuccessfulMessage("Starting VMs");

		
		
		/**
		 * Step 2: Mapping ip addresses, IPs, to each local schedule:
		 * 
		 * This method is necessary to propagate all IPS to the outgoing data channels of each task schedule before
		 * we send each local schedule to each VM.
		 * Completing incoming and outgoing data channels IP assignment. This can only be done when each task is assignend to an IP.
		 */
		Dataview.debugger.logSuccessfulMessage("Step 2: Mapping ip addresses, IPs, to each local schedule"); // new addition
		gsch.completeIPAssignment();
		
		
		/**
		 * This method maps the ip address to each task. 
		 */
		taskVMip = gsch.mapTaskToVM();
		totalTasks = gsch.getTotalNumberOfTasks();

		
		/*
		 * Step 3: Running sshd in each worker node:
		 * 
		 * Executing commands for each of the local schedule that is assigned to different VM
		 */
		Dataview.debugger.logSuccessfulMessage("Step 3: Running sshd in each worker node:"); // new addition
		initVM();
		
		/*
		 * Step 4: Halting the system for couple of seconds [5 seconds]
		 * Sleeping for 5 seconds.
		 */
		try {
			Thread.sleep(1000*5);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		Dataview.debugger.logSuccessfulMessage("Remote Worker Node is Ready");
		

		//***********************************
		//                New changes for CodeProvisioner
//		CodeProvisionAttestation codeProvisionAttestation = new CodeProvisionAttestation(IPs, workflowLibDir, destinationFolder, vmProvisioner, PORT_NO);
		
		/* Step 5: Securely sends the code and data to the remote worker nodes and verify their integrity after transfer
		 * 
		 * Transferring the control flow to the Code Provision Attestation 
		 * Checking the every worker node whether it is correctly provisioned / integrity and secured.
		 * */
		Dataview.debugger.logSuccessfulMessage("Step 5: Starting code provision attestation to securely send the code and data to the remote worker nodes and verify their integrity after transfer");
		
		CodeProvisionAttestation codeProvisionAttestation = new CodeProvisionAttestation(IPs, workflowLibDir, destinationFolder, 
				sgxProvisioner, amdProvisioner, awsProvisioner, PORT_NO, ipPool, gsch);
		if (!codeProvisionAttestation.getRemoteAttestationStatus(IPs)) {
			Dataview.debugger.logErrorMessage("Code provision is broken and program is terminate!");
			return;
		}
		
		
		Dataview.debugger.logSuccessfulMessage("Code provision is authenitaced and trustworth!");
		
		
	
		//Wait until the task executor in the vm starts
		try {
			Dataview.debugger.logSuccessfulMessage("Waiting for 30 seconds..");
			Thread.sleep(30000);
		} catch (Exception e) {
			Dataview.debugger.logException(e);
		}


		startTime = System.currentTimeMillis(); //timer for evaluation
		
				
		/*
		 * starting SSL Socket connection to each of the VM 
		 */
		/*Step 6: Setting up a messaging SSL socket connection to each Task Executor at each worker node.*/
		Dataview.debugger.logSuccessfulMessage("Step 6: Setting up a messaging SSL socket connection to each Task Executor at each worker node.");
		
		
		for(int  i = 0; i < IPs.size(); i++) {
			try {
				
				KeyStore ks = KeyStore.getInstance("JKS");
				ks.load(new FileInputStream("confidentialInfo/clientkeystore"), "dataview".toCharArray());
				KeyManagerFactory kmf = KeyManagerFactory.getInstance("sunX509");
				kmf.init(ks, "dataview".toCharArray());
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
			
			} catch (Exception e) {
				Dataview.debugger.logException(e);
			}
		}

		
		/*Step 7: Sending each local schedule through SSL socket to its corresponding worker node and then communicate with each Task Executor to execute each 
		 * local schedule.
		 */ 

		for (int i = 0; i < gsch.length(); i++) {
			runThread(gsch.getLocalSchedule(i).getSpecification().toString(), i, IPs);
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
		Dataview.debugger.logSuccessfulMessage("End of the Workflow Execution");

		//vmProvisioner.terminateInstances();

	}

	private void initVM() {
		
		for (int i = 0; i < gsch.length(); i++) {
			initVMThread(gsch.getLocalSchedule(i));
		}
		
		for (Thread thread : threadForInitVM) {
			thread.start();
		}

		for (Thread thread : threadForInitVM) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	private void initVMThread(LocalSchedule localSchedule) {
		Thread localScheduleThread = new Thread(new Runnable() {
			
			@Override
			public void run() {

				if (localSchedule.getVmType().equals(SGX)) {
					sgxProvisioner.copyFileVMhost(SGX_IMG_SRC_FILE, SGX_IMG_DST_FOLDER, localSchedule.getIP());
					sgxProvisioner.copyFileVMhost(SGX_SCRIPT_SRC_FILE, SGX_SCRIPT_DST_FOLDER, localSchedule.getIP());
					sgxProvisioner.executeCommands(localSchedule.getIP(), SGX_SEV_AWS_USER_NAME,  SGX_SEV_AWS_USER_PASSWORD, sgxProvisioner.getBashCommands());
					try {
						System.out.println("System is halt after running the execute commannds of SGX_SCRIPT for 90 seconds");
						Thread.sleep(90*1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				} else if (localSchedule.getVmType().equals(AMD)){
					amdProvisioner.copyFileVMhost(AMD_IMG_SRC_FILE, AMD_IMG_DST_FOLDER, AMD_SERVER_IP);
					amdProvisioner.copyFileVMhost(AMD_SCRIPT_SRC_FILE, AMD_SCRIPT_DST_FOLDER, AMD_SERVER_IP);
					
					Thread executeCommandHostThread = new Thread(new Runnable() {
						@Override
						public void run() {
							VMProvisionerAMD.executeCommandsHost(AMD_SERVER_IP, AMD_SERVER_USER_NAME,  AMD_SERVER_PASSWORD);
						}
					});
					executeCommandHostThread.start();

					
					try {
						System.out.println("System is halt after running the execute commannds of AMD_SCRIPT for 90 seconds");
						Thread.sleep(90*1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					amdProvisioner.executeCommands(localSchedule.getIP(), SGX_SEV_AWS_USER_NAME,  SGX_SEV_AWS_USER_PASSWORD, amdProvisioner.getBashCommands());
				} else if (localSchedule.getVmType().equals(AWS)) {
					awsProvisioner.executeCommands(localSchedule.getIP(), SGX_SEV_AWS_USER_NAME,  SGX_SEV_AWS_USER_PASSWORD, awsProvisioner.getBashCommands());
				}
			}
		});
		threadForInitVM.add(localScheduleThread);
		
		
	}


	private ArrayList<String> generateIPPool() {
		
		ArrayList<String> allocatedIPs = new ArrayList<String>();
		
		for (int i = 0; i < gsch.length(); i++) {
			LocalSchedule localSchedule = gsch.getLocalSchedule(i);
			// Machine type is decided based on the local Schedule
			if (localSchedule.getVmType().equals(SGX)) totalSGXMachines++;
			else if (localSchedule.getVmType().equals(AMD)) totalAMDMachines++;
			else if (localSchedule.getVmType().equals(AWS)) totalAWSMachines++;
		}
		
		
		
		
		
		List<String> SGXIPs = new ArrayList<>();
		List<String> AMDIPs = new ArrayList<>();
		List<String> AWSIPs = new ArrayList<>();
		
		
		if (totalSGXMachines > 0) {
			SGXIPs = sgxProvisioner.getAvailableIPs(totalSGXMachines);
			for (int i = 0; i < totalSGXMachines; i++) {
				ipPool.put(SGXIPs.get(i), SGX);
			}
		}
		
		if (totalAMDMachines > 0) {
			AMDIPs = amdProvisioner.getAvailableIPs(totalAMDMachines);
			for (int i = 0; i < totalAMDMachines; i++) {
				ipPool.put(AMDIPs.get(i), AMD);
			}
		}
		
		if (totalAWSMachines > 0) {
			AWSIPs = awsProvisioner.getAvailableIPs(totalAWSMachines);
			for (int i = 0; i < totalAWSMachines; i++) {
				ipPool.put(AWSIPs.get(i), AWS);
			}
		}
		

		int indexSGXIPs = 0;
		int indexAMDIPs = 0;
		int indexAWSIPs = 0;
		
		for (int i = 0; i < gsch.length(); i++) {
			LocalSchedule localSchedule = gsch.getLocalSchedule(i);
			if (localSchedule.getVmType().equals(SGX)) {
				allocatedIPs.add(SGXIPs.get(indexSGXIPs));
				localSchedule.setIP(SGXIPs.get(indexSGXIPs++));
			}
			else if (localSchedule.getVmType().equals(AMD)) {
				allocatedIPs.add(AMDIPs.get(indexAMDIPs));
				localSchedule.setIP(AMDIPs.get(indexAMDIPs++));
			}
			else if (localSchedule.getVmType().equals(AWS)) {
				allocatedIPs.add(AWSIPs.get(indexAWSIPs));
				localSchedule.setIP(AWSIPs.get(indexAWSIPs++));
			}
		}
		
		return allocatedIPs;
	}
	


	void runThread(String localScheduleSpec, int vm, ArrayList<String> IPs) {
		Thread localScheduleThread = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					// Sending settings initialization to TaskExecutor
					Dataview.debugger.logSuccessfulMessage("Sending settings initialization to TaskExecutor at : " + inputs.get(vm));
					Dataview.debugger.logSuccessfulMessage("local schedule spec : " + localScheduleSpec);

					//Sending string with new line through socket programming splits into multiple msg Hence replacing new lines with special character $$&&

					String replacedString = localScheduleSpec.replace("\n", "$$&&");
					outputs.get(vm).println("initializationXX" + replacedString);	
				} catch(Exception exception) {
					Dataview.debugger.logException(exception);
				}

				try {
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
						String taskName = messages[1];
						if (messages[0].equalsIgnoreCase("Success")) { // SuccessXXTask1
							Dataview.debugger.logSuccessfulMessage(taskName + " is finished.");

							// Sending signal to the children who are associate with other VM
							if (messages.length > 2) {
								for (int i = 2; i < messages.length; i++) {
									String childName = messages[i];
									if(!taskVMip.get(taskName).equalsIgnoreCase(childName)) {
										int localScheduleIndex = gsch.getLocalScheduleIDNumber(childName);
										outputs.get(localScheduleIndex).println("signalXX" + childName + "XX" + taskName);
										Dataview.debugger.logSuccessfulMessage("Sending signal to start " + childName);
									}

								}
							}
							totalSuccess++;
						}

						if (messages[0].equalsIgnoreCase("ExecutionTime")) { 
							Dataview.debugger.logSuccessfulMessage("Execution time of " + messages[1] + " is " + messages[2] + " in miliseconds");
						}

						if (totalSuccess == totalTasks) {
							Dataview.debugger.logSuccessfulMessage("All tasks are finished");
							String reply = "terminate";
							isFinished = true;
							for (PrintStream out : outputs) {
								Dataview.debugger.logSuccessfulMessage("Sending stop signal to terminate Task Executor");
								out.println(reply);
							}
							finishTime = System.currentTimeMillis();
							long totalTime = finishTime - startTime;
							Dataview.totalExecutionTime += totalTime;
							Dataview.debugger.logSuccessfulMessage("Total execution time " + totalTime);
							Dataview.executionTimes.add(totalTime);
							Dataview.debugger.logSuccessfulMessage("Terminnating thread " + vm);
							break;
							//System.exit(0);
						}
					}

				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		});
		threadList.add(localScheduleThread);
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
}