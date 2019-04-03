package dataview.workflowexecutor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.KeyPair;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.amazonaws.services.ec2.*;

import dataview.models.*;


public class VMProvisioner {
	/*String accessKey = "AKIAIOHJWYUV34P6NRHQ";
	String secretKey = "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"; 
	String autoGenKeyName = "ishtiaq_key1";
	String autoGenGroupName = "dataview1";
	String imageID = "ami-1ba1db64";
	String instanceType = "t2.micro";
	int instanceNumber = 1;
	String pemFileLocation = null;
	AWSCredentials credentials;
	Region region;
	AmazonEC2Client ec2client;
	AmazonRDSClient rdsclient;*/
	
	String accessKey;
	String secretKey; 
	String autoGenKeyName;
	String autoGenGroupName;
	String imageID;
	String instanceType;
	int instanceNumber;
	String pemFileLocation;
	AWSCredentials credentials;
	Region region;
	AmazonEC2Client ec2client;
	AmazonRDSClient rdsclient;
	
	ArrayList<String> runningInstanceId = new ArrayList<String>();



	public VMProvisioner(int instanceNumber) {
		/*Dataview.debugger.logSuccessfulMessage("VMProvisioner constructor is called with instance number # " + instanceNumber);
		accessKey = "AKIAIOHJWYUV34P6NRHQ";
		secretKey = "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"; 
		autoGenKeyName = "dataview1";
		autoGenGroupName = "dataview1";
		imageID = "ami-1ba1db64"; //saeid
		//		String imageID = "ami-959c69ef";
		instanceType = "t2.micro";
		this.instanceNumber = instanceNumber;
		region = Region.getRegion(Regions.US_EAST_1);
		pemFileLocation = autoGenKeyName + ".pem";*/
	}


	public VMProvisioner(int instanceNumber, String accessKey, String secretKey, String autoGenKeyName, String autoGenGroupName,
			String imageID, String instanceType, String region) {
		this.instanceNumber = instanceNumber;
		this.accessKey = accessKey;
		this.secretKey = secretKey;
		this.autoGenKeyName = autoGenKeyName;
		this.autoGenGroupName = autoGenGroupName;
		this.imageID = imageID;
		this.instanceType = instanceType;
		pemFileLocation = autoGenKeyName + ".pem";

		if (region.equalsIgnoreCase("US_EAST_1")) {
			this.region = Region.getRegion(Regions.US_EAST_1);
		} else if (region.equalsIgnoreCase("US_WEST_1")) {
			this.region = Region.getRegion(Regions.US_WEST_1);
		}  else if (region.equalsIgnoreCase("US_WEST_2")) {
			this.region = Region.getRegion(Regions.US_WEST_2);
		} else {
			// default value
			this.region = Region.getRegion(Regions.US_EAST_1);
		}

	}


	public static void main(String[] args) {

		/*VMProvisioner networkAssistant = new VMProvisioner();
		try {
			networkAssistant.process();
		} catch (Exception e) {
			System.out.println("Exception caught in main method : " + e);
		}*/
	}

	ArrayList<String> startVMs(){
		credentials = new BasicAWSCredentials(accessKey, secretKey);
		ec2client = new AmazonEC2Client(credentials);
		ec2client.setRegion(region);
		ArrayList<String> ips = null;
		try {
			ips = provisionVMs(instanceNumber); 

			//mandatory sleeping
			System.out.println("Sleeping for 50s...");
			Thread.sleep(50000);
		} catch (Exception exception) {
			Dataview.debugger.logErrorMessage("Caught exception while VM provisioning. " + exception);
		}

		return ips;
	}

	
	void terminateInstances() {
		TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest();
		terminateInstancesRequest.setInstanceIds(runningInstanceId);
		TerminateInstancesResult terminateInstancesResult = ec2client.terminateInstances(terminateInstancesRequest);
		try {
			Dataview.debugger.logSuccessfulMessage("Terminating instances. Waiting for 90 seconds");
			Thread.sleep(90000);
		} catch (Exception e) {
			Dataview.debugger.logErrorMessage("Found Exception while waiting for 90 seconds");
			
		}

	}



	void executingTask(int taskId, String ip) {
		System.out.println("Starting execution in VM IP : " + ip);
		ArrayList<String> commands = new ArrayList<String>();
		commands.add("javac A.java");
		commands.add("java A " + taskId);
		commands.add("exit");
		executeShellCommands(ip, "ubuntu",  "", commands);

		System.out.println("Finished execution");
		Dataview.debugger.logSuccessfulMessage("Finished execution");

	}

	String executeShellCommands(String hostName, String strUserName,
			String strPassword, List<String> commands) {
		String strLogMessages = "";
		try {
			JSch jsch = new JSch();
			Session session = jsch.getSession(strUserName, hostName, 22);
			//			session.setPassword(strPassword);
			jsch.addIdentity(pemFileLocation);
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.connect();

			Channel channel = session.openChannel("shell");// only shell
			OutputStream inputstream_for_the_channel = channel
					.getOutputStream();
			PrintStream shellStream = new PrintStream(
					inputstream_for_the_channel, true);
			channel.connect();
			for (String command : commands) {
				shellStream.println(command);
				shellStream.flush();
			}

			shellStream.close();

			InputStream outputstream_from_the_channel = channel
					.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(
					outputstream_from_the_channel));
			String line;

			while ((line = br.readLine()) != null) {
				strLogMessages = strLogMessages + line + "\n";
			}

			do {

			} while (!channel.isEOF());

			outputstream_from_the_channel.close();
			br.close();
			session.disconnect();
			channel.disconnect();

			return strLogMessages;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return strLogMessages;
	}

	ArrayList<String> provisionVMs(int noOfInstances) throws Exception {
		ArrayList<String> initialVMs = getAvailableInstIds();
		justLaunchVMs(noOfInstances);
		ArrayList<String> runningAndPendingVMs = getAvailableAndPendingInstIds();
		runningInstanceId = runningAndPendingVMs;
		runningAndPendingVMs.removeAll(initialVMs);
		waitUntilAllPendingBecomeRunning(runningAndPendingVMs);
		ArrayList<String> ipAddresses = getIPaddresses(runningAndPendingVMs);
		Dataview.debugger.logSuccessfulMessage("Provisioned machines successfully....");
		return ipAddresses;
	}

	ArrayList<String> getIPaddresses(ArrayList<String> instIds) {
		ArrayList<String> resultList = new ArrayList<String>();
		DescribeInstancesResult result = ec2client.describeInstances();
		Iterator<Reservation> i = result.getReservations().iterator();
		while (i.hasNext()) {
			Reservation r = i.next();
			List<Instance> instances = r.getInstances();
			for (Instance ii : instances) {
				for (String instId : instIds) {
					if (ii.getInstanceId().trim().equals(instId))
						resultList.add(ii.getPublicIpAddress());
				}
			}
		}
		return resultList;
	}

	void waitUntilAllPendingBecomeRunning(ArrayList<String> pendingInstIds) throws Exception {
		int noOfPending = pendingInstIds.size();
		boolean isWaiting = true;
		while (isWaiting) {
			Thread.sleep(1000);
			DescribeInstancesResult r = ec2client.describeInstances();
			Iterator<Reservation> ir = r.getReservations().iterator();
			while (ir.hasNext()) {
				Reservation rr = ir.next();
				List<Instance> instances = rr.getInstances();
				for (Instance ii : instances) {
					for (String pendingVMInstId : pendingInstIds) {
						if (ii.getInstanceId().trim().equals(pendingVMInstId.trim())) {
							if (ii.getState().getName().trim().equals("running")
									&& ii.getInstanceId().trim().equals(pendingVMInstId.trim())) {
								noOfPending--;
								if (noOfPending == 0)
									isWaiting = false;
							}
						}
					}
				}
			}
		}

	}


	ArrayList<String> getAvailableAndPendingInstIds() {
		ArrayList<String> resultList = new ArrayList<String>();
		DescribeInstancesResult result = ec2client.describeInstances();
		Iterator<Reservation> i = result.getReservations().iterator();
		while (i.hasNext()) {
			Reservation r = i.next();
			List<Instance> instances = r.getInstances();
			for (Instance ii : instances) {
				if (ii.getState().getName().equals("running") || ii.getState().getName().equals("pending")) {
					resultList.add(ii.getInstanceId());
				}
			}
		}
		return resultList;
	}

	void justLaunchVMs(int noOfInstances) {
		deleteSecurityGroup(autoGenGroupName);
		deleteKeyPair(autoGenKeyName);
		createSecurityGroup(autoGenGroupName);
		createKeyPair("", autoGenKeyName);
		createEC2Instance(noOfInstances);
	}


	private void deleteKeyPair(String autoGenKeyName) {
		try {
			DeleteKeyPairRequest request = new DeleteKeyPairRequest().withKeyName(autoGenKeyName);
			ec2client.deleteKeyPair(request);
			Dataview.debugger.logSuccessfulMessage("\nWaiting for 30 seconds to delete the key pair " + autoGenKeyName);
			Thread.sleep(30000);
			Dataview.debugger.logSuccessfulMessage("Successfully deleted security group with key pair " + autoGenKeyName);
		} catch(Exception exception) {
			Dataview.debugger.logErrorMessage("Exception found in deleting key pair");
			Dataview.debugger.logException(exception);
		}

	}


	private void deleteSecurityGroup(String autoGenGroupName2) {
		try {
			DeleteSecurityGroupRequest deleteSecurityGroupRequest = new DeleteSecurityGroupRequest().withGroupName(autoGenGroupName2);
			ec2client.deleteSecurityGroup(deleteSecurityGroupRequest);
			Dataview.debugger.logSuccessfulMessage("\nWaiting for 30 seconds to delete the security groupname " +  autoGenGroupName2);
			Thread.sleep(30000);
			Dataview.debugger.logSuccessfulMessage("\nSuccessfully deleted security group with security groupname " + autoGenGroupName2);
		} catch(Exception exception) {
			Dataview.debugger.logErrorMessage("Exception found in deleting group");
			Dataview.debugger.logException(exception);
		}

	}
	
	/*void justLaunchVMs(int noOfInstances) {
		deleteSecurityGroup(autoGenGroupName);
		deleteKeyPair(autoGenKeyName);
		
		try {
			Dataview.debugger.logSuccessfulMessage("Waiting for 30 seconds after deletion of groupName : " + autoGenGroupName + " and keyName : " + autoGenKeyName);
			Thread.sleep(30000);
		} catch(Exception exception) {
			Dataview.debugger.logErrorMessage("Found exception while waiting for deletion of group name and key name : " + exception);
		}
		
		
		createSecurityGroup(autoGenGroupName);
		createKeyPair("", autoGenKeyName);
		createEC2Instance(noOfInstances);
	}


	private void deleteKeyPair(String autoGenKeyName) {
		try {
			DeleteKeyPairRequest request = new DeleteKeyPairRequest().withKeyName(autoGenKeyName);
			ec2client.deleteKeyPair(request);			
			Dataview.debugger.logSuccessfulMessage("Successfully deleted key  " + autoGenKeyName);
		} catch(Exception exception) {
			Dataview.debugger.logErrorMessage("Exception found in deleting key : " + exception);
		}

	}


	private void deleteSecurityGroup(String autoGenGroupName2) {
		try {
			DeleteSecurityGroupRequest deleteSecurityGroupRequest = new DeleteSecurityGroupRequest().withGroupName(autoGenGroupName2);
			ec2client.deleteSecurityGroup(deleteSecurityGroupRequest);
			Dataview.debugger.logSuccessfulMessage("Successfully deleted security group : " + autoGenGroupName2);
		} catch(Exception exception) {
			Dataview.debugger.logErrorMessage("Exception found in deleting group : " + exception);
		}

	}*/


	private String createEC2Instance(int noOfInstances) {
		try {
			RunInstancesRequest rir = new RunInstancesRequest();
			rir.withImageId(imageID);
			rir.withInstanceType(instanceType);
			rir.withMinCount(noOfInstances);
			rir.withMaxCount(noOfInstances);
			rir.withKeyName(autoGenKeyName);
			rir.withMonitoring(true);
			rir.withSecurityGroups(autoGenGroupName);
			RunInstancesResult riresult = ec2client.runInstances(rir); 
			riresult.toString();
			// return null;
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		return null;
	}

	boolean createKeyPair(String pemFilePath, String paramkeyName) {
		boolean result = false;
		try {
			CreateKeyPairRequest ckpr = new CreateKeyPairRequest();
			ckpr.withKeyName(paramkeyName);

			CreateKeyPairResult ckpresult = ec2client.createKeyPair(ckpr);
			KeyPair keypair = ckpresult.getKeyPair();
			String privateKey = keypair.getKeyMaterial();
			writePemFile(privateKey, pemFilePath, paramkeyName);
			System.out.println("Following key:" + paramkeyName + " is created.");
			result = true;
		} catch (Exception e) {
			System.out.println("KeyPair creation Failure...");
			System.out.println(e.toString());
			Dataview.debugger.logErrorMessage("KeyPair creation Failure...");
			Dataview.debugger.logException(e);
		}
		return result;
	}


	boolean createSecurityGroup(String paramSecGroupName) {
		boolean result = false;
		try {
			CreateSecurityGroupRequest csgr = new CreateSecurityGroupRequest();
			csgr.withGroupName(paramSecGroupName).withDescription(
					"My security group");
			ec2client.createSecurityGroup(csgr);
			IpPermission ipPermission1 = new IpPermission();
			ipPermission1.withIpRanges("0.0.0.0/0").withIpProtocol("tcp")
			.withFromPort(22).withToPort(22);

			IpPermission ipPermission2 = new IpPermission();
			ipPermission2.withIpRanges("0.0.0.0/0").withIpProtocol("tcp")
			.withFromPort(3306).withToPort(3306);


			IpPermission ipPermission3 = new IpPermission();
			ipPermission3.withIpRanges("0.0.0.0/0").withIpProtocol("tcp")
			.withFromPort(2004).withToPort(2004);


			IpPermission ipPermission4 = new IpPermission();
			ipPermission4.withIpRanges("0.0.0.0/0").withIpProtocol("tcp")
			.withFromPort(7700).withToPort(7700);


			AuthorizeSecurityGroupIngressRequest authorizeSecurityGroupIngressRequest = new AuthorizeSecurityGroupIngressRequest();
			authorizeSecurityGroupIngressRequest.withGroupName(
					paramSecGroupName).withIpPermissions(ipPermission1);
			authorizeSecurityGroupIngressRequest.withGroupName(
					paramSecGroupName).withIpPermissions(ipPermission2);
			authorizeSecurityGroupIngressRequest.withGroupName(
					paramSecGroupName).withIpPermissions(ipPermission3);
			authorizeSecurityGroupIngressRequest.withGroupName(
					paramSecGroupName).withIpPermissions(ipPermission4);


			ec2client
			.authorizeSecurityGroupIngress(authorizeSecurityGroupIngressRequest);

			System.out.println("Following group:" + paramSecGroupName
					+ " is created.");
			Dataview.debugger.logSuccessfulMessage("Following group:" + paramSecGroupName + " is created.");
			result = true;
		} catch (Exception e) {
			System.out.println("Failure...");
			Dataview.debugger.logException(e);
			System.out.println(e.toString());
		}
		return result;
	}

	void writePemFile(String privateKey, String pemFilePath,
			String keyname) {
		try {
			File f = new File("confidentialInfo/" + keyname + ".pem");
			if(f.exists()) {
				f.delete();
			}

			Runtime.getRuntime().exec("chmod 400 file");
			PrintWriter writer = new PrintWriter(keyname
					+ ".pem", "UTF-8");
			writer.print(privateKey);
			writer.close();
			File file = new File("confidentialInfo/" + keyname + ".pem");

			Set<PosixFilePermission> perms = new HashSet<>();
			perms.add(PosixFilePermission.OWNER_READ);

			Files.setPosixFilePermissions(file.toPath(), perms);

		} catch (Exception e) {
			Dataview.debugger.logException(e);
		}
	}




	ArrayList<String> getAvailableInstIds() {
		ArrayList<String> resultList = new ArrayList<String>();
		DescribeInstancesResult result = ec2client.describeInstances();
		Iterator<Reservation> i = result.getReservations().iterator();
		while (i.hasNext()) {
			Reservation r = i.next();
			List<Instance> instances = r.getInstances();
			for (Instance ii : instances) {
				if (ii.getState().getName().equals("running")) {
					resultList.add(ii.getInstanceId());
				}
			}
		}
		return resultList;
	}


	void moveCode(String codeLocation, ArrayList<String> ips) {
		System.out.println("Moving code to cloud");
		for (String ipAddress : ips) {
			try {
				copyFile(codeLocation, "/home/ubuntu/", ipAddress.trim());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	void copyFileNew(String SourceDIR, String DestinationDIR,
			String hostName) {

		String SFTPHOST = hostName;
		int SFTPPORT = 22;
		String SFTPUSER = "ubuntu";
		String SFTPWORKINGDIR = DestinationDIR;
		String FILETOTRANSFER = SourceDIR;
		Session session = null;
		Channel channel = null;
		ChannelSftp channelSftp = null;		
		String SFTPPASS = "dataview";

		System.out.println("preparing the host information for sftp.");
		try {
			JSch jsch = new JSch();
			jsch.addIdentity(pemFileLocation);
			session = jsch.getSession(SFTPUSER, SFTPHOST, SFTPPORT);
			//session.setPassword(SFTPPASS);
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.setPort(22);
			session.connect();
			System.out.println("Host connected.");
			channel = session.openChannel("sftp");
			channel.connect();
			System.out.println("sftp channel opened and connected.");
			channelSftp = (ChannelSftp) channel;
			channelSftp.cd(SFTPWORKINGDIR);
			File f = new File(FILETOTRANSFER);
			channelSftp.put(new FileInputStream(f), f.getName());
			System.out.println(f.getName() + " File transfered successfully to host.");
		} catch (Exception ex) {
			System.out.println("Exception found while tranfer the response. : " + ex);
		}
		finally{

			channelSftp.exit();
			System.out.println("sftp Channel exited.");
			channel.disconnect();
			System.out.println("Channel disconnected.");
			session.disconnect();
			System.out.println("Host Session disconnected.");
		}
	}


	void copyFile1(String SourceDIR, String DestinationDIR,
			String strHostName) {
		String SFTPHOST = strHostName;
		int SFTPPORT = 22;
		String SFTPUSER = "ubuntu";
		String SFTPWORKINGDIR = DestinationDIR;
		String FILETOTRANSFER = SourceDIR;
		Session session = null;
		Channel channel = null;
		ChannelSftp channelSftp = null;

		System.out.println("preparing the host information for sftp.");
		try {
			JSch jsch = new JSch();
			jsch.addIdentity(pemFileLocation);
			session = jsch.getSession(SFTPUSER, SFTPHOST, SFTPPORT);
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.setPort(22);
			session.connect();
			System.out.println("Host connected.");
			channel = session.openChannel("sftp");
			channel.connect();
			System.out.println("sftp channel opened and connected.");
			channelSftp = (ChannelSftp) channel;
			channelSftp.cd(SFTPWORKINGDIR);
			File f = new File(FILETOTRANSFER);
			channelSftp.put(new FileInputStream(f), f.getName());
			System.out.println("File " + f.getName() + " has been copied");
		} catch (Exception ex) {
			System.out.println("Exception found while tranfer the response. : " + ex);
		} finally {
			if (channelSftp != null) {
				channelSftp.exit();
				System.out.println("sftp Channel exited.");
			}
			if (channel != null) {
				channel.disconnect();
				System.out.println("Channel disconnected.");
			}
			if (session != null) {
				session.disconnect();
				System.out.println("Host Session disconnected.");
			}
		}

	}

	void copyFileVM(String SourceDIR, String DestinationDIR,
			String strHostName) {

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

	}

	
	
	void copyFile(String SourceDIR, String DestinationDIR,
			String strHostName) throws Exception {

		String SFTPHOST = strHostName;
		int SFTPPORT = 22;
		String SFTPUSER = "ubuntu";
		String SFTPWORKINGDIR = DestinationDIR;
		String FILETOTRANSFER = SourceDIR;
		Session session = null;
		Channel channel = null;
		ChannelSftp channelSftp = null;
		//try {
		JSch jsch = new JSch();
		jsch.addIdentity(pemFileLocation);
		session = jsch.getSession(SFTPUSER, SFTPHOST, SFTPPORT);
		java.util.Properties config = new java.util.Properties();
		config.put("StrictHostKeyChecking", "no");
		session.setConfig(config);
		session.setPort(22);
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
		//} catch (Exception ex) {
		//	ex.printStackTrace();

		//}

	}

	void getMachineReady(String pemFileLocation, ArrayList<String> ips) throws Exception{
		for (String ipAddress : ips) {
			System.out.println("Preparing the data node:"+ipAddress);
			ArrayList<String> commands = new ArrayList<String>();
			commands.add("sudo su -");
			commands.add("echo -e \"dataview\ndataview\" | (passwd ubuntu)");
			commands.add("exit");
			commands.add("sed -\"s/PasswordAuthentication no/PasswordAuthentication yes/g\" /etc/ssh/sshd_config");
			commands.add("sudo service ssh restart");
			commands.add("exit");
			System.out.println("Executing command in EC2");
			Dataview.debugger.logSuccessfulMessage("Executing command in EC2");
			executeCommandsInEC2(pemFileLocation, ipAddress.trim(), "ubuntu", commands);
		}
		System.out.println("All set to go...");
	}

	String executeCommandsInEC2( String pemFileLocation, String strHostName, String strUserName,
			List<String> commands){
		String strLogMessages = "";
		try {
			JSch jsch = new JSch();
			jsch.addIdentity(pemFileLocation);
			Session session = jsch.getSession(strUserName, strHostName, 22);
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.connect();
			Channel channel=session.openChannel("shell");//only shell
			OutputStream inputstream_for_the_channel = channel.getOutputStream();
			PrintStream shellStream = new PrintStream(inputstream_for_the_channel, true);
			channel.connect(); 
			for(String command: commands) {
				shellStream.println(command); 
				shellStream.flush();
			}
			shellStream.close();
			InputStream outputstream_from_the_channel = channel.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(outputstream_from_the_channel));
			String line;
			while ((line = br.readLine()) != null){
				strLogMessages = strLogMessages + line+"\n";
			}
			do {
			} while(!channel.isEOF());
			outputstream_from_the_channel.close();
			br.close();
			session.disconnect();
			channel.disconnect();
			return strLogMessages;
		} catch (Exception e) { 
			e.printStackTrace();
		}
		return strLogMessages;
	}


	public void runTaskExecutor(String ip, String destinationFolder) {
		ArrayList<String> commands = new ArrayList<String>();
		//TODO make changes for amazon vm
//		commands.add("export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/home/ubuntu");
		commands.add("export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:" + destinationFolder);
		//commands.add("export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/home/vm");
		//commands.add("javac -cp .:javax.servlet-5.1.12.jar:jsch-0.1.48.jar *.java");
//		commands.add("java DWJNI > dwjniresult.txt");
		commands.add("java -jar Dataview.jar >te.txt");
		//commands.add("java -cp .:javax.servlet-5.1.12.jar:jsch-0.1.48.jar TaskExecutor > te.txt");
		executeShellCommands(ip, "ubuntu",  "", commands);
	}
	
	
	
	public void runCodeProvision(String ip, String destinationFolder) {
		ArrayList<String> commands = new ArrayList<String>();
		commands.add("java -jar CodeProvisioner.jar > codeProvision.txt");
		executeShellCommands(ip, "ubuntu",  "", commands);
	}
	String executeCommandsInSGX(String strHostName, String strUserName,String password, List<String> commands)
	{
		String strLogMessages = "";
		try {
			JSch jsch = new JSch();
			//jsch.addIdentity(pemFileLocation);
			Session session = jsch.getSession(strUserName, strHostName, 22);
			session.setPassword(password);
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.connect();
			Channel channel=session.openChannel("shell");//only shell
			OutputStream inputstream_for_the_channel = channel.getOutputStream();
			PrintStream shellStream = new PrintStream(inputstream_for_the_channel, true);
			channel.connect(); 
			for(String command: commands) {
				shellStream.println(command); 
				shellStream.flush();
			}
			shellStream.close();
		/*	InputStream outputstream_from_the_channel = channel.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(outputstream_from_the_channel));
			String line;
		while ((line = br.readLine()) != null){
				strLogMessages = strLogMessages + line+"\n";
				System.out.println(strLogMessages);
			}
			do {
			} while(!channel.isEOF());
			outputstream_from_the_channel.close();
		br.close();			*/								
			try {
				Thread.sleep(1000*15);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}										
			session.disconnect();
			channel.disconnect();
			return strLogMessages;
		} catch (Exception e) { 
			e.printStackTrace();
		}
		return strLogMessages;
	}
	public static void main1(String[] args)
	{
		
		VMProvisioner a =new VMProvisioner(1);
		ArrayList<String> commands = new ArrayList<String>();
		//TODO make changes for SGX-lkl
//		commands.add("export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/home/ubuntu");
		//commands.add("ls");
		//commands.add("export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/home/vm");
		//commands.add("javac -cp .:javax.servlet-5.1.12.jar:jsch-0.1.48.jar *.java");
	commands.add("/home/ubuntu/sgx-lkl/apps/jvm/helloworld-java/runme.sh &> log.txt");
//		commands.add("java -version &> test.txt");
		//commands.add("java -cp .:javax.servlet-5.1.12.jar:jsch-0.1.48.jar TaskExecutor &> te.txt");
		a.executeCommandsInSGX("127.0.0.1", "vm",  "vm", commands);
		
		}

}
