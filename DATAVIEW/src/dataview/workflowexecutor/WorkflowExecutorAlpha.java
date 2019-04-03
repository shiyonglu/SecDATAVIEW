package dataview.workflowexecutor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import dataview.models.Dataview;
import dataview.models.GlobalSchedule;
import dataview.models.LocalSchedule;


public class WorkflowExecutorAlpha extends WorkflowExecutor{
	private int totalSuccess = 0;
	private final int PORT_NO = 7700;
	private int totalTasks = 0;
	long startTime;
	long finishTime;

	private boolean isFinished = false;

	private String workflowTaskDir;
	private String workflowLibDir;
//	private final String destinationFolder = "/home/ubuntu/";
	private final String destinationFolder = "/home/ubuntu/";

	private GlobalSchedule gsch;

//	private ArrayList<Socket> clientSockets = new ArrayList<Socket>();
	private ArrayList<SSLSocket> clientSockets = new ArrayList<>();
	private ArrayList<BufferedReader> inputs = new ArrayList<BufferedReader>();
	private ArrayList<PrintStream> outputs = new ArrayList<PrintStream>();

	private HashMap<String, String> taskVMip = new HashMap<String, String>();

	private String SECRET_KEY = "dataview1.pem";


	private ArrayList<Thread> threadList = new ArrayList<Thread>();



	public static void main(String[] args) {

	}

	public WorkflowExecutorAlpha(String workflowTaskDir, String workflowLibDir, GlobalSchedule gsch) {
		this.workflowTaskDir = workflowTaskDir;
		this.workflowLibDir = workflowLibDir;
		this.gsch = gsch;
	}

	public void execute() {
		Dataview.debugger.logSuccessfulMessage("WorkflowExecutor has started");
		Dataview.debugger.logSuccessfulMessage("Going to provision machines");
		

		VMProvisioner vmProvisioner = new VMProvisioner(gsch.length());
		Dataview.debugger.logSuccessfulMessage("Provisioning machine is successful");

		Dataview.debugger.logSuccessfulMessage("Starting VMs");
		//TODO We need to update this method to add the SGX support
		/*ArrayList<String> IPs = vmProvisioner.startVMs();*/

		ArrayList<String> IPs = new ArrayList<String>();
//		IPs.add("35.16.36.109"); IPs.add("35.16.38.254");
//		IPs.add("192.168.228.131");
		//IPs.add("172.30.17.183");
		//IPs.add("141.217.114.67");
		IPs.add("172.30.18.187");
		//IPs.add("10.0.1.1");

		for(int i = 0; i < gsch.length(); i++){
			LocalSchedule ls = gsch.getLocalSchedule(i);
			ls.setIP(IPs.get(i));
		}


		gsch.completeIPAssignment();
		
		
		taskVMip = gsch.mapTaskToVM();
		totalTasks = gsch.getTotalNumberOfTasks();


		//TODO # need to send files to vm which settings might be different compare to Amazon VM
//		sendingFilestoVM(IPs, vmProvisioner);

		//Dataview.debugger.logSuccessfulMessage("File transfer is complete");
		//new Thread(new Runnable() {//thread runs command and sshd on worker node
			//@Override
			//public void run() {
				
					ArrayList<String> commands = new ArrayList<String>();
					//commands.add("nohup java -Djava.class.path=/home/ubuntu/ -jar /home/ubuntu/sshd.jar &> /home/ubuntu/log.txt &");
					commands.add("cd /home/ubuntu/sgx-lkl/apps/jvm/helloworld-java/;");
				    commands.add("nohup ./run.sh &> log.txt &");
					System.out.println("sending bash script"); 
					vmProvisioner.executeCommandsInSGX("172.30.18.187", "ubuntu",  "dataview", commands);
				//} catch (Exception e) {
			////		Dataview.debugger.logException(e);
			//	}
			//}
		//}).start();
		try {
			Thread.sleep(1000*5);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.out.println("SGX Worker Node is Ready");
		startTime = System.currentTimeMillis(); //timer for evaluation

		//***********************************
		//                New changes for CodeProvisioner
		CodeProvisionAttestation codeProvisionAttestation = new CodeProvisionAttestation(IPs, workflowLibDir, destinationFolder, vmProvisioner, PORT_NO);
		if (!codeProvisionAttestation.isCodeProvisionOkay(IPs, gsch)) {
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


				
//		starting SSL Socket 
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
				
				// we replaced socket with ssl socket	
				
				/*Socket clientSocket = new Socket(IPs.get(i), PORT_NO);
				BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				PrintStream output = new PrintStream(clientSocket.getOutputStream());

				clientSockets.add(clientSocket);
				inputs.add(input);
				outputs.add(output);*/

			} catch (Exception e) {
				Dataview.debugger.logException(e);
			}
		}


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


	private void sendingFilestoVM(ArrayList<String> IPs, VMProvisioner vmProvisioner) {

		Dataview.debugger.logSuccessfulMessage("Retrieving necessary files workflowLibDir is started");
		ArrayList<String> originalfiles = retrieveFileNames(workflowTaskDir);
		Dataview.debugger.logSuccessfulMessage("Retrieving necessary files workflowTaskDir is finished");
		Dataview.debugger.logSuccessfulMessage("Retrieving necessary files workflowLibDir is started");
		originalfiles.add(SECRET_KEY);
		originalfiles.addAll(retrieveFileNames(workflowLibDir));
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

					vmProvisioner.copyFile(file, destinationFolder, IPs.get(i));
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
}
