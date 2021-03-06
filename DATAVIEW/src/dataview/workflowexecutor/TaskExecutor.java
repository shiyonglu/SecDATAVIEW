package dataview.workflowexecutor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import dataview.models.Dataview;
import dataview.models.InputPort;
import dataview.models.JSONArray;
import dataview.models.JSONObject;
import dataview.models.JSONParser;
import dataview.models.SshServerMain2;
import dataview.models.Task;
import dataview.workflowexecutor.TaskExecutor;


/**	A general introduction to TaskExecutor.
 * 	The task executor will bind the data products with input ports and output ports
 *  It will call a specific task to read data from the input port, do data processing and write data to corresponding outputs.
 *  
 *  Each input port of a task is like an incoming mailbox. The task executor will put data products at the input ports of a task,then the
 *  task will read the data products from the input ports, process them and then write the results as data products to the task's output 
 *  ports, which server as outgoing mailboxex. Afterwards, the task executor will move the data products at the output ports to the VMs running 
 *  child tasks. 
 *  
 *  
 */


/* The first method is "run" to start with this class . 
 * Step 1 : Set up the SSH server for file transfer
 * Step 2 : Loading the confidentiality information according to local schedule assignment
 * Step 3 : Set up the SSL socket connection to the Workflow Executor, then receives the workflow related signals.
 * 
 *
 * 
 * Step 4: (also known as WCPAC 30 (Execute Job),  Run the tasks in this local schedule in parallel (multi-threading)
 *          Implementation details: 1) check all input data are ready for a task, 2) run the task, and 3)
 *          send output data and signals to all children in parallel
 *  
 *       
 * step 5: close all resources
 */


public class TaskExecutor {
	private final Lock lock = new ReentrantLock();
	private SSLSocket server;
	private SSLServerSocket serverSocket;
	private BufferedReader input;
	private PrintStream output;
	private HashMap<String, String> childrenMap = new HashMap<String, String>();
	private HashMap<String, String> parentMap = new HashMap<String, String>();
	private HashMap<String, Condition> mapCondition = new HashMap<String, Condition>();
	private HashMap<String, Boolean> isExecuted = new HashMap<String, Boolean>();
	private HashMap<Task, String> tasks = new HashMap<Task, String>();
	private HashMap<String, String> taskVMmap = new HashMap<String, String>();
//	private HashSet<String> AWS_IPs = new HashSet<>();
	private final int PORT_NO = 7700;
	private List<String> firstTasksAssociatedInputAndLastTaskAssociatedOutputFiles = new ArrayList<>();
	
	public static String secretKey = "abc";
	public static String associatedData = "abc";
	
	

	public static HashSet<String> confidentialTasks = new HashSet<String>();

	private long startTime;
	private long finishTime;

	public static void main(String[] args) throws Exception {
		Dataview.debugger.logSuccessfulMessage("Starting TaskExecutor Ver 1.1");
		TaskExecutor taskExecutor = new TaskExecutor();
		/*
		 * Initiate server connection
		 */
		taskExecutor.runSShServer();
		/*
		 * load confidential configuration. 
		 */
		taskExecutor.loadConfidentialTaskNames();
		
		/*
		 * Regular communication established with Workflow executor 
		 */
		taskExecutor.connectWithWorkflowExecutor();
		
	}
	// This function is called by the codeprovisioner.jar
	public void run() throws Exception {
		Dataview.debugger.logSuccessfulMessage("Starting TaskExecutor Ver 1.1");
		TaskExecutor taskExecutor = new TaskExecutor();
		/*
		 * Initiate server connection
		 */
		taskExecutor.runSShServer();
		/*
		 * load confidential configuration. 
		 */
		taskExecutor.loadConfidentialTaskNames();
		
		/*
		 * Regular communication established with Workflow executor 
		 */
		taskExecutor.connectWithWorkflowExecutor();
	}
	
	private void runSShServer( ) {
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				String[] args = new String[0];
				try {
					SshServerMain2.main(args);
				} catch (Exception e) {
					Dataview.debugger.logException(e);
				}
			}
		}).start();
	}

	private void loadConfidentialTaskNames() {
		
		try {
			String input = "";
			File file = new File("/home/ubuntu/config.txt");
			BufferedReader bufferredReader = null;
			bufferredReader = new BufferedReader(new FileReader(file));
			String line;
			while ((line = bufferredReader.readLine()) != null) {
				input += line;
			}
			bufferredReader.close();
			
			JSONParser parser = new JSONParser(input);
			JSONObject taskObjects = parser.parseJSONObject();
			JSONArray confidentialTasksJSONArray = taskObjects.get("confidentialTasks").toJSONArray();

			for (int i = 0; i < confidentialTasksJSONArray.size(); i++) {
				JSONObject task = confidentialTasksJSONArray.get(i).toJSONObject();
				String taskName = (String) task.get("taskName").toString().replace("\"", "");
				;
				confidentialTasks.add(taskName);
			}
		} catch (Exception e) {
			Dataview.debugger.logException(e);
		}

		
	}
	
	void initialize(String localScheduleSpec) {
		// replaced special character by new line to construct original
		// localSchedulespec
		localScheduleSpec = localScheduleSpec.replace("$$&&", "\n");

		JSONParser parser = new JSONParser(localScheduleSpec);
		JSONObject lsch = parser.parseJSONObject();
		JSONArray tschs = lsch.get("taskSchedules").toJSONArray();

		int totalTasks = tschs.size();

		for (int i = 0; i < totalTasks; i++) {
			JSONObject taskspec = tschs.get(i).toJSONObject();
			String myTaskInstanceID = (String) taskspec.get("taskInstanceID").toString().replace("\"", "");

			// sample task instance id comes with sample '@32498'. Hence needs to trim for
			// colldcting task name
			String myTaskName = myTaskInstanceID.split("@")[0];

			try {
				Class<?> taskclass = Class.forName(myTaskName);
				Task myTask = (Task) taskclass.newInstance();

				tasks.put(myTask, myTaskInstanceID);
				Condition condition = lock.newCondition();
				mapCondition.put(myTaskInstanceID, condition);
				isExecuted.put(myTaskInstanceID, false);

				// for each input incoming data channel of t
				JSONArray indcs = taskspec.get("incomingDataChannels").toJSONArray();
				for (int j = 0; j < indcs.size(); j++) {

					JSONObject indc = indcs.get(j).toJSONObject();
					String parentTaskInstanceID = indc.get("srcTask").toString().replace("\"", "");
					if (!parentTaskInstanceID.isEmpty()) {
						String parentOutputPortIndex = indc.get("outputPortIndex").toString().replace("\"", "");
						parentMap.put(myTaskInstanceID + "_" + j, parentTaskInstanceID + "_" + parentOutputPortIndex);
						isExecuted.put(parentTaskInstanceID, false);

						if (confidentialTasks.contains(myTaskInstanceID.split("@")[0])
								|| confidentialTasks.contains(parentTaskInstanceID.split("@")[0])) {
							myTask.ins[j].setLocation("/home/ubuntu/" + myTaskInstanceID + "_in" + j + ".enc");
						} else {
							myTask.ins[j].setLocation("/home/ubuntu/" + myTaskInstanceID + "_in" + j + ".txt");
						}

					} else {
						String srcFilename = indc.get("srcFilename").toString().replace("\"", "");
						myTask.ins[j].setLocation("/home/ubuntu/" + srcFilename);
						if (confidentialTasks.contains(myTaskName)) {
							Dataview.debugger.logSuccessfulMessage(srcFilename);
							firstTasksAssociatedInputAndLastTaskAssociatedOutputFiles.add("/home/ubuntu/" + srcFilename.split("\\.")[0] + ".txt");
						}
					}
				}

				// for each outgoing data channel of t
				JSONArray outdcs = taskspec.get("outgoingDataChannels").toJSONArray();
				for (int j = 0; j < outdcs.size(); j++) {

					JSONObject outdc = outdcs.get(j).toJSONObject();
					String childTaskInstanceID = outdc.get("destTask").toString().replace("\"", "");
					;
					if (!childTaskInstanceID.isEmpty()) {
						String childInputPortIndex = outdc.get("inputPortIndex").toString().replace("\"", "");
						childrenMap.put(myTaskInstanceID + "_" + j, childTaskInstanceID + "_" + childInputPortIndex);
						String childVMIP = outdc.get("destIP").toString().replace("\"", "");
						taskVMmap.put(childTaskInstanceID, childVMIP);
						String val = myTaskInstanceID.split("@")[0];

						@SuppressWarnings("rawtypes")
						Iterator iterator = confidentialTasks.iterator();
						System.out.println(val + "XXXX");
						// check values
						while (iterator.hasNext()) {
							System.out.println("Value: " + iterator.next() + "XXX");
						}

						if (confidentialTasks.contains(val)
								|| confidentialTasks.contains(childTaskInstanceID.split("@")[0])) {
							myTask.outs[j].setLocation("/home/ubuntu/" + myTaskInstanceID + "_out" + j + ".enc");
						} else {
							myTask.outs[j].setLocation("/home/ubuntu/" + myTaskInstanceID + "_out" + j + ".txt");
						}

					} else {
						String srcFilename = outdc.get("destFilename").toString().replace("\"", "");
						myTask.outs[j].setLocation("/home/ubuntu/" + srcFilename);
						if (confidentialTasks.contains(myTaskName)) {
							Dataview.debugger.logSuccessfulMessage(srcFilename);
							firstTasksAssociatedInputAndLastTaskAssociatedOutputFiles.add("/home/ubuntu/" + srcFilename.split("\\.")[0] + ".txt");
						}
					}
				}

			} catch (Exception e) {
				
				Dataview.debugger.logException(e);
			}

		}

		startTasks();

	}

	void startTasks() {
		for (Task task : tasks.keySet()) {
			new Thread(new Runnable() {
				@Override
				public void run() {
//WCPAC.step 30: message(startJob), this step the workflow job schedules and configuration are received from Workflow Executor
					onTaskRunStart(task);
//WCPAC.step 31: Execute Job, this step the workflow job is executed.
					executeTask(task);
//WCPAC.step 34: message(terminateTaskExecutor), this step the signal request Task Executor to start shutdown routine and termiate itself 					
					onTaskRunFinish(task);

				}
			}).start();

		}
	}

	void connectWithWorkflowExecutor() throws IOException {



		try {
			Dataview.debugger.logSuccessfulMessage("Creating secure Socket connection thorugh port 7700");
			// loading certificate keystore from a given file
			KeyStore ks = KeyStore.getInstance("JKS");
			ks.load(new FileInputStream("/home/ubuntu/clientkeystore"), "dataview".toCharArray());
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("sunX509");
			kmf.init(ks, "dataview".toCharArray());
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("sunX509");
			tmf.init(ks);
			SSLContext sc = SSLContext.getInstance("TLS");
			TrustManager[] trustManagers = tmf.getTrustManagers();
			sc.init(kmf.getKeyManagers(), trustManagers, null);
			SSLServerSocketFactory ssf = sc.getServerSocketFactory();
			serverSocket= (SSLServerSocket) ssf.createServerSocket(PORT_NO);
			Dataview.debugger.logSuccessfulMessage("SSL ServerSocket started");
			server = (SSLSocket) serverSocket.accept();
			Dataview.debugger.logSuccessfulMessage("SSL ServerSocket Accepted");

			input = new BufferedReader(new InputStreamReader(server.getInputStream()));
			output = new PrintStream(server.getOutputStream());

		} catch(Exception e) {
			Dataview.debugger.logException(e);
		}





		// vmProvisioner = new VMProvisioner(1);

		while (server.isConnected()) {
			String message = input.readLine();
			if (message != null) {
				Dataview.debugger.logSuccessfulMessage("Client : " + message);

				String[] parts = message.split("XX");
				if (parts[0].equals("initialization")) {
					Dataview.debugger.logSuccessfulMessage("Initialization signal is received");
					initialize(parts[1]);
				} else if (parts[0].equals("signal")) {
					lock.lock();
					if (!isExecuted.get(parts[1])) {
						isExecuted.put(parts[2], true);
						Dataview.debugger.logSuccessfulMessage("Signal details : " + message);
						mapCondition.get(parts[1]).signal();
					}
					lock.unlock();

				} else if (parts[0].equals("terminate")) {
					// On terminate signal clean up the storage of plain text files
//WCPAC.step 35: clean-up, this step deletes all intermediate and residual files in the worker storage after the workflow job is done  					
								deleteAllConfidentialTextFiles();
							
					
					//
					Dataview.debugger.logSuccessfulMessage("Terminating Task Executor");
					output.println("terminateThread");
					server.close();
					serverSocket.close();
					System.exit(0);
				}

				// log += "Ending time : " + System.currentTimeMillis();
			}
		}
		serverSocket.close();
	}

	/**
	 * Checking whether parent task is finished
	 * 
	 * @param id
	 * @return boolean
	 */
	boolean isParentExecuted(Task task) {

		for (int i = 0; i < task.ins.length; i++) {
			String thisTaskInstanceID = tasks.get(task);

			// incoming port contains file
			if (!parentMap.containsKey(thisTaskInstanceID + "_" + i)) {
				continue;
			}
			// since parentMap values return parentIP with port, we need to eliminate the
			// port number
			String parentTaskInstanceID = parentMap.get(thisTaskInstanceID + "_" + i).split("_")[0];

			if (!isExecuted.get(parentTaskInstanceID)) {
				return false;
			}
		}

		return true;
	}

	private void onTaskRunStart(Task task) {

		while (!isParentExecuted(task)) {
			lock.lock();
			try {
				Dataview.debugger.logSuccessfulMessage("Waiting : " + tasks.get(task));
				mapCondition.get(tasks.get(task)).await();
				Dataview.debugger.logSuccessfulMessage("Woke up : " + tasks.get(task));
			} catch (Exception e) {
				Dataview.debugger.logException(e);
			}
			lock.unlock();
		}

	}

	void executeTask(Task task) {
		Dataview.debugger.logSuccessfulMessage("Starting execution : " + tasks.get(task));
		startTime = System.currentTimeMillis();
		task.run();
	}

	private void onTaskRunFinish(Task task) {
		lock.lock();
		Dataview.debugger.logSuccessfulMessage("Finished execution : " + tasks.get(task));
		HashSet<String> childTaskNamesAssociatedWithDifferentIP = new HashSet<>();
		HashSet<String> childrenWithinSameMachine = new HashSet<>();
		for (int i = 0; i < task.outs.length; i++) {

			String input = "";
		
			// write to the file
			Task child = null;
			String[] parts = null;
			// BufferedWriter writer = null;
			try {
				// output port is not connected to any task
				if (!childrenMap.containsKey(tasks.get(task) + "_" + i)) {
					continue;
				}
				String value = childrenMap.get(tasks.get(task) + "_" + i);
				parts = value.split("_");
				String location = null;
				for (Task t : tasks.keySet()) {
					if (tasks.get(t).equalsIgnoreCase(parts[0])) {
						child = t;
						break;
					}
				}

				if (child != null) {
					InputPort inputPort = child.ins[Integer.parseInt(parts[1])];
					location = inputPort.getFileName();

					////////// Code for adjusting the name that copies the old output as a new
					////////// input.

					try (FileInputStream in = new FileInputStream(task.outs[i].getFileName());
							FileOutputStream out = new FileOutputStream(location)) {
						byte[] ibuf = new byte[1];
						
						@SuppressWarnings("unused")
						int len;
						while ((len = in.read(ibuf)) != -1) {
							out.write(ibuf);
						}
						in.close();
						out.close();
					}
					Dataview.debugger.logSuccessfulMessage("new file  " + location + ":" + input);
				}
			} catch (Exception e) {
				Dataview.debugger.logException(e);
			}
			isExecuted.put(tasks.get(task), true);
			if (child != null && mapCondition.get(tasks.get(child)) != null) {
//				Dataview.debugger.logSuccessfulMessage("Sending sginal for starting " + tasks.get(child));
				childrenWithinSameMachine.add(tasks.get(child));
//				mapCondition.get(tasks.get(child)).signal();
			}

			// ******************
			// Sending output to remote
			else {
				childTaskNamesAssociatedWithDifferentIP.add(parts[0]);
				String transferredFileName = "";

				if (confidentialTasks.contains(parts[0].split("@")[0])
						|| confidentialTasks.contains(task.taskName.split("@")[0])) {
					transferredFileName = parts[0] + "_in" + parts[1] + ".enc";
					
				} else {
					transferredFileName = parts[0] + "_in" + parts[1] + ".txt";
				}
				String remoteMachineIP = taskVMmap.get(parts[0]);
				// copyFile(task.outs[i].getFileName(), "/home/ubuntu/", transferredFileName,
				// remoteMachineIP, "dataview1.pem");
				System.out.println("Source file : " + task.outs[i].getFileName() + " Destination File name: " + transferredFileName + " remote machine IP : " + remoteMachineIP);
				Dataview.debugger.logSuccessfulMessage("Source file : " + task.outs[i].getFileName() + " Destination File name: " + transferredFileName + " remote machine IP : " + remoteMachineIP);
				
				
				
				send(task.outs[i].getFileName(), "/home/ubuntu/", transferredFileName, remoteMachineIP);
				
				/*if (!AWS_IPs.contains(remoteMachineIP)) {
					send(task.outs[i].getFileName(), "/home/ubuntu/", transferredFileName, remoteMachineIP);
				} else {
					copyFileVM(task.outs[i].getFileName(), "/home/ubuntu/", transferredFileName, remoteMachineIP, "/home/ubuntu/dataview1.pem");
				}*/
				
				/*if (confidentialTasks.contains(parts[0].split("@")[0])) {
					send(task.outs[i].getFileName(), "/home/ubuntu/", transferredFileName, remoteMachineIP);
				} else {
					copyFileVM(task.outs[i].getFileName(), "/home/ubuntu/", transferredFileName, remoteMachineIP, "/home/ubuntu/dataview1.pem");
				}*/
				

				System.out.println(
						"Original file name : " + task.outs[i].getFileName() + "transferred file name : "
								+ transferredFileName + " transferred to remote machine : " + remoteMachineIP);
				Dataview.debugger.logSuccessfulMessage(
						"Original file name : " + task.outs[i].getFileName() + "transferred file name : "
								+ transferredFileName + " transferred to remote machine : " + remoteMachineIP);
			}
			
		}

		if (server != null && server.isConnected()) {
			
			for (String child : childrenWithinSameMachine) {
				Dataview.debugger.logSuccessfulMessage("Sending sginal for starting " + child);
				mapCondition.get(child).signal();
			}
			
			Dataview.debugger
			.logSuccessfulMessage("Sending sginal to the Master node for successfully finished execution of " + tasks.get(task));
			
			
			String signal = "SuccessXX" + tasks.get(task);
			for (String childTaskName : childTaskNamesAssociatedWithDifferentIP) {
				signal += ("XX" + childTaskName);
			}

			finishTime = System.currentTimeMillis();
			long executionTime = finishTime - startTime;
			output.println(signal);
			output.println("ExecutionTimeXX" + tasks.get(task) + "XX" + executionTime);
		}
		lock.unlock();
	}

	
	private void deleteAllConfidentialTextFiles() {
		Dataview.debugger.logSuccessfulMessage("Delete all residual confidential plaintext files..");
		File folder = new File("/home/ubuntu/");
		
		for (String name : confidentialTasks) {
			Dataview.debugger.logSuccessfulMessage("Connfidential Task Name : " + name);
		}
		File[] listOfFiles = folder.listFiles();
		for (int i = 0; i < listOfFiles.length; i++) {
			Dataview.debugger.logSuccessfulMessage("File Name : " + listOfFiles[i].getName().trim());
			String currentFileName = listOfFiles[i].getName().trim();
			
			String currentFileNameWithoutId = currentFileName.split("@")[0];
			Dataview.debugger.logSuccessfulMessage("File Name without id : " + currentFileNameWithoutId);
		
			
			if (listOfFiles[i].isFile() & confidentialTasks.contains(currentFileNameWithoutId) && currentFileName.contains(".txt")) {
				deleteFile(listOfFiles[i]);
			}
			if(listOfFiles[i].isFile() && (currentFileName.contains("TaskExecutor.jar") || currentFileName.contains("CodeProvisioner.jar") || currentFileName.contains("config.txt")
					|| currentFileName.contains("clientkeystore") || currentFileName.contains("codeprovisioner.jks") 
					|| currentFileName.contains("TaskExecutor.enc") || currentFileName.contains("key.pem")     ))
			{
				deleteFile(listOfFiles[i]);

			}

		}
		
		for (String fileName : firstTasksAssociatedInputAndLastTaskAssociatedOutputFiles) {
			File file = new File(fileName);
			deleteFile(file);
		}

	}
	
	private void deleteFile(File file) {
		try {
	        if(file.delete()) { 
	        	Dataview.debugger.logSuccessfulMessage("File: " + file.getName() + " deleted successfully");		 
	        } 
	        else { 
	        	Dataview.debugger.logErrorMessage("Failed to delete the file : " + file.getName()); 
	        }
		} catch(Exception e) {
			Dataview.debugger.logException(e);
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

	/*void copyFileVM1(String SourceDIR, String DestinationDIR, String strHostName) {

		String SFTPHOST = strHostName;
		int SFTPPORT = 8000;
		String SFTPUSER = "ubuntu";
		String SFTPWORKINGDIR = DestinationDIR;
		String FILETOTRANSFER = SourceDIR;
		Session session = null;
		Channel channel = null;
		ChannelSftp channelSftp = null;
		try {
			JSch jsch = new JSch();
			//		jsch.addIdentity(pemFileLocation);
			session = jsch.getSession(SFTPUSER, SFTPHOST, SFTPPORT);
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setPassword("dataview");
			session.setConfig(config);
			session.setPort(8000);
			session.connect();
			channel = session.openChannel("sftp");
			channel.connect();
			channelSftp = (ChannelSftp) channel;
			channelSftp.cd(SFTPWORKINGDIR);
			File f = new File(FILETOTRANSFER);
			channelSftp.put(new FileInputStream(f), f.getName());
			System.out.println("Sending file " + f.getName() + " is successful.");
			channel.disconnect();
			session.disconnect();
		} catch (Exception ex) {
			Dataview.debugger.logException(ex);
		}
	}*/
	
	/*void copyFileSGX(String SourceDIR, String DestinationDIR, String newFileName, String strHostName) {
		String SFTPHOST = strHostName;
		int SFTPPORT = 8000;
		String SFTPUSER = "ubuntu";
		String SFTPWORKINGDIR = DestinationDIR;
		String FILETOTRANSFER = SourceDIR;
		Session session = null;
		Channel channel = null;
		ChannelSftp channelSftp = null;
		
		
		boolean continueLoop = true;
		int count = 0;
		while(continueLoop && count < 4) {
			try {
				JSch jsch = new JSch();
				// jsch.addIdentity(pemFileLocation);
				session = jsch.getSession(SFTPUSER, SFTPHOST, SFTPPORT);
				session.setPassword("dataview");
				java.util.Properties config = new java.util.Properties();
				config.put("StrictHostKeyChecking", "no");
				session.setTimeout(0);
				session.setConfig(config);
				session.setPort(8000);
				session.connect();
				channel = session.openChannel("sftp");
				channel.connect();
				channelSftp = (ChannelSftp) channel;
				channelSftp.cd(SFTPWORKINGDIR);
				File f = new File(FILETOTRANSFER);
				channelSftp.put(new FileInputStream(f), newFileName);

				channel.disconnect();
				session.disconnect();
				continueLoop = false;
			} catch (Exception ex) {
				Dataview.debugger.logException(ex);
				System.out.println(ex);
				
				count++;
				
			}
		}
	}*/

	/*void copyFileVM(String SourceDIR, String DestinationDIR, String newFileName,
			String strHostName, String pemFileLocation) {
		String SFTPHOST = strHostName;
		int SFTPPORT = 22;
		String SFTPUSER = "ubuntu";
		String SFTPWORKINGDIR = DestinationDIR;
		String FILETOTRANSFER = SourceDIR;
		Session session = null;
		Channel channel = null;
		ChannelSftp channelSftp = null;
		try {
			JSch jsch = new JSch();
			jsch.addIdentity(pemFileLocation);
			session = jsch.getSession(SFTPUSER, SFTPHOST, SFTPPORT);
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setTimeout(0);
			session.setConfig(config);
			session.setPort(22);
			session.connect();
			channel = session.openChannel("sftp");
			channel.connect();
			channelSftp = (ChannelSftp) channel;
			channelSftp.cd(SFTPWORKINGDIR);
			File f = new File(FILETOTRANSFER);
			channelSftp.put(new FileInputStream(f), newFileName);
			Dataview.debugger.logSuccessfulMessage("File " + f.getName() + " has been copied");
			channel.disconnect();
			session.disconnect();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}*/
	
	
	// fileTransfer()
	public static void send (String SourceDIR, String DestinationDIR, String newFileName, String strHostName) {
		boolean isFoundException = true;
		int countIteration = 15;
		Dataview.debugger.logSuccessfulMessage("Trying to send file : " + SourceDIR + " to " + strHostName );
		while(isFoundException && countIteration-- > 0) {
			String SFTPHOST = strHostName;
		    int SFTPPORT = CloudResourceManagement.SSHD_SFTP_PORT;//8000;
		    String SFTPUSER = CloudResourceManagement.SSHD_USERNAME;   //"ubuntu";
		    String SFTPPASS = CloudResourceManagement.SSHD_PASSWORD;  //"dataview";
		    String SFTPWORKINGDIR = DestinationDIR;

		    Session session = null;
		    Channel channel = null;
		    ChannelSftp channelSftp = null;
		    Dataview.debugger.logSuccessfulMessage("preparing the host information for sftp.");

		    try {
		        JSch jsch = new JSch();
		        session = jsch.getSession(SFTPUSER, SFTPHOST, SFTPPORT);
		        session.setPassword(SFTPPASS);
		        java.util.Properties config = new java.util.Properties();
		        config.put("StrictHostKeyChecking", "no");
		        session.setConfig(config);
		        session.connect();
		        Dataview.debugger.logSuccessfulMessage("Host connected.");
		        channel = session.openChannel("sftp");
		        channel.connect();
		        Dataview.debugger.logSuccessfulMessage("sftp channel opened and connected.");
		        channelSftp = (ChannelSftp) channel;
		        channelSftp.cd(SFTPWORKINGDIR);
		        File f = new File(SourceDIR);
		        channelSftp.put(new FileInputStream(f), newFileName);
		        isFoundException = false;
		    } catch (Exception e) {
		    	Dataview.debugger.logErrorMessage("Exception found while tranfer the response.");
		        Dataview.debugger.logException(e);
				Dataview.debugger.logErrorMessage("File sending exception is caught " + e + " host: " + strHostName);
				Dataview.debugger.logErrorMessage("Trying to resend " + SourceDIR);
				isFoundException = true;
				try {
					Dataview.debugger.logSuccessfulMessage("Waiting for 15 seconds to resend the file");
					Thread.sleep(15000);
				} catch (InterruptedException e1) {
					Dataview.debugger.logException(e1);
				}
		    } finally {
		        channelSftp.exit();
		        Dataview.debugger.logSuccessfulMessage("sftp Channel exited.");
		        channel.disconnect();
		        Dataview.debugger.logSuccessfulMessage("Channel disconnected.");
		        session.disconnect();
		        Dataview.debugger.logSuccessfulMessage("Host Session disconnected.");
		    }
		}
		
	    
	}   
}
