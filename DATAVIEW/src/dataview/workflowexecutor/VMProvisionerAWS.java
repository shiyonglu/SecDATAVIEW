package dataview.workflowexecutor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
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
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.KeyPair;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.elasticloadbalancing.model.InstanceState;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import dataview.models.Dataview;
import dataview.models.JSONArray;
import dataview.models.JSONObject;
import dataview.models.JSONParser;
import dataview.workflowexecutor.CloudResourceManagement;

public class VMProvisionerAWS extends CloudResourceManagement{

	String accessKey;
	String secretKey; 
	String autoGenKeyName;
	String autoGenGroupName;
	String imageID;
	String instanceType;
	
	String pemFileLocation;
	AWSCredentials credentials;
	Region region;
	AmazonEC2Client ec2client;
	AmazonRDSClient rdsclient;
	
	ArrayList<String> runningInstanceId = new ArrayList<String>();
	HashSet<String> instanceIdSet = new HashSet<>();
	
	
	public VMProvisionerAWS() {
		Dataview.debugger.logSuccessfulMessage("VMProvisioner constructor is called with instance number");
		accessKey = "YOUR_ACCESS_KEY";
		secretKey = "YOUR_SECRET_KEY"; 
		autoGenKeyName = "dataview1";
		autoGenGroupName = "dataview1";
		imageID = "ami-0c6014e1ee2dcd28c"; //saeid
		//		String imageID = "ami-959c69ef";
		instanceType = "t2.micro";
//		this.instanceNumber = instanceNumber;
		region = Region.getRegion(Regions.US_EAST_1);
		pemFileLocation = "confidentialInfo/" + autoGenKeyName + ".pem";
	}
	
	@Override
	List<String> getAvailableIPs(int totalMachine) {
		
		credentials = new BasicAWSCredentials(accessKey, secretKey);
		ec2client = new AmazonEC2Client(credentials);
		ec2client.setRegion(region);
		ArrayList<String> ips = null;
		try {
			ips = provisionVMs(totalMachine); 
			
			//HashSet<String> instanceIdSet = retrieveInstanceId(ips);
			/*for (String ip : runningInstanceId) {
				instanceIdSet.add(ip);
			}
			Iterator<String> iterator = instanceIdSet.iterator();
			while (iterator.hasNext()) {
			    String instanceId = iterator.next();
			    int statusCode = getInstanceStatus(instanceId);
			    if (statusCode == 16) iterator.remove(); // 16 represents the running status of the remote AWS VM
			    else {
			    	
			    	Thread.sleep(3000);  	
			    }
			    
			}*/
			Thread.sleep(90 * 1000);
			
		} catch (Exception exception) {
			Dataview.debugger.logErrorMessage("Caught exception while VM provisioning. " + exception);
		}
		rewriteIpPool(ips);
		return ips;
	}
	
	
	public static HashSet<String> retrieveInstanceId(List<String> ips) throws Exception {
		HashSet<String> hashSet = new HashSet<>();
		for (String ip : ips) {
			String EC2Id = "";
			String inputLine;
			
			
			URL EC2MetaData = new URL("http://" + ip + "/latest/meta-data/instance-id");
			URLConnection EC2MD = EC2MetaData.openConnection();
			BufferedReader in = new BufferedReader(
					new InputStreamReader(
							EC2MD.getInputStream()));
			while ((inputLine = in.readLine()) != null)
			{	
				EC2Id = inputLine;
			}
			in.close();
			hashSet.add(EC2Id);
		}
		return hashSet;
		
	}

	
	void rewriteIpPool(List<String> ips) {
		
		HashSet<String> AMDIps = new HashSet<>();
		HashSet<String> SGXIps = new HashSet<>();
		String input = "";
		File file = new File("confidentialInfo/IPPool.txt");
		BufferedReader bufferredReader = null;
		try {
			bufferredReader = new BufferedReader(new FileReader(file));
			String line;
			while ((line = bufferredReader.readLine()) != null) {
				input += line;
			}
			bufferredReader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		JSONParser parser = new JSONParser(input);
		JSONObject taskObjects = parser.parseJSONObject();
		JSONArray ipPoolJSONArray = taskObjects.get("IPPool").toJSONArray();

		
		for (int i = 0; i < ipPoolJSONArray.size(); i++) {
			JSONObject machineType = ipPoolJSONArray.get(i).toJSONObject();
			String IP = null;
			try {
				IP = (String) machineType.get("AMD").toString().replace("\"", "");
				AMDIps.add(IP);
			} catch (Exception e) {
				// don't need to do
			}	
		}
		
		for (int i = 0; i < ipPoolJSONArray.size(); i++) {
			JSONObject machineType = ipPoolJSONArray.get(i).toJSONObject();
			String IP = null;
			try {
				IP = (String) machineType.get("SGX").toString().replace("\"", "");
				SGXIps.add(IP);
			} catch (Exception e) {
				// don't need to do
			}	
		}
		
		
		try (FileWriter writer = new FileWriter("confidentialInfo/IPPool.txt");
				BufferedWriter bw = new BufferedWriter(writer)) {
			
			bw.write("{\n");
			bw.write(" \"IPPool\":\n");
			bw.write("  [\n");
			
			
			

			for (String ip : SGXIps) {
				bw.write("   {\n");
				bw.write("    \"SGX\" : \"" + ip + "\"\n");
				bw.write("   },\n");
			}
			
			for (String ip : AMDIps) {
				bw.write("   {\n");
				bw.write("    \"AMD\" : \"" + ip + "\"\n");
				bw.write("   },\n");
			}

			for (int i = 0; i < ips.size(); i++) {
				bw.write("   {\n");
				bw.write("    \"AWS\" : \"" + ips.get(i) + "\"\n");
				if (i == ips.size() - 1) 
					bw.write("   }\n");
				else 
					bw.write("   },\n");
			}
			bw.write("  ]\n");
			bw.write("}\n");
			bw.close();
		} catch (IOException e) {
			System.err.format("IOException: %s%n", e);
		}
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
	
	
	public Integer getInstanceStatus(String instanceId) {
	    DescribeInstancesRequest describeInstanceRequest = new DescribeInstancesRequest().withInstanceIds(instanceId);
	    DescribeInstancesResult describeInstanceResult = ec2client.describeInstances(describeInstanceRequest);
	    com.amazonaws.services.ec2.model.InstanceState state = describeInstanceResult.getReservations().get(0).getInstances().get(0).getState();
	    return state.getCode();
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
			
			
			IpPermission ipPermission5 = new IpPermission();
			ipPermission5.withIpRanges("0.0.0.0/0").withIpProtocol("tcp")
			.withFromPort(8000).withToPort(8000);
			
			


			AuthorizeSecurityGroupIngressRequest authorizeSecurityGroupIngressRequest = new AuthorizeSecurityGroupIngressRequest();
			authorizeSecurityGroupIngressRequest.withGroupName(
					paramSecGroupName).withIpPermissions(ipPermission1);
			authorizeSecurityGroupIngressRequest.withGroupName(
					paramSecGroupName).withIpPermissions(ipPermission2);
			authorizeSecurityGroupIngressRequest.withGroupName(
					paramSecGroupName).withIpPermissions(ipPermission3);
			authorizeSecurityGroupIngressRequest.withGroupName(
					paramSecGroupName).withIpPermissions(ipPermission4);
			authorizeSecurityGroupIngressRequest.withGroupName(
					paramSecGroupName).withIpPermissions(ipPermission5);


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
			PrintWriter writer = new PrintWriter("confidentialInfo/" + keyname
					+ ".pem", "UTF-8");
			writer.print(privateKey);
			writer.close();
			
			System.out.println("chmod 400 " + pemFilePath);
			File file = new File("confidentialInfo/" + keyname + ".pem");

			Set<PosixFilePermission> perms = new HashSet<>();
			perms.add(PosixFilePermission.OWNER_READ);

			Files.setPosixFilePermissions(file.toPath(), perms);
			
		} catch (Exception e) {
			Dataview.debugger.logException(e);
		}
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


	
	@Override
	void executeCommands(String strHostName, String strUserName,String password, List<String> commands) {		
		
		Dataview.debugger.logSuccessfulMessage("Executing commands in AWS");
		
		try {
			JSch jsch = new JSch();
			jsch.addIdentity(pemFileLocation);
			Session session = jsch.getSession(strUserName, strHostName, 22);
//			session.setPassword(password);
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
			try {
				Thread.sleep(1000*15);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}										
			session.disconnect();
			channel.disconnect();
		} catch (Exception e) { 
			e.printStackTrace();
		}
		
	}
	

	@Override
	void copyFileVM(String SourceDIR, String DestinationDIR, String strHostName) {
		String SFTPHOST = strHostName;
		String SFTPWORKINGDIR = DestinationDIR;
		String FILETOTRANSFER = SourceDIR;
		Session session = null;
		Channel channel = null;
		ChannelSftp channelSftp = null;
		
		boolean isFoundException = true;
		int countIteration = 15;
		System.out.println("Trying to send file : " + SourceDIR + " to " + strHostName );
		while(isFoundException && countIteration-- > 0) {
			try {
				JSch jsch = new JSch();
	//			jsch.addIdentity(pemFileLocation);
				session = jsch.getSession(SSHD_USERNAME, SFTPHOST, SSHD_SFTP_PORT);
				java.util.Properties config = new java.util.Properties();
				config.put("StrictHostKeyChecking", "no");
				session.setPassword(SSHD_PASSWORD);
				session.setConfig(config);
				session.setPort(SSHD_SFTP_PORT);
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
				isFoundException = false;
			} catch (Exception e) {
				Dataview.debugger.logException(e);
				Dataview.debugger.logErrorMessage("File sending exception is caught " + e + " host: " + strHostName);
				Dataview.debugger.logErrorMessage("Trying to resend " + SourceDIR);
				isFoundException = true;
				try {
					Dataview.debugger.logSuccessfulMessage("Waiting for 15 seconds to resend the file");
					Thread.sleep(15000);
				} catch (InterruptedException ex) {
					e.printStackTrace();
				}
			}
		}	
		
		if (countIteration < 0) {
			Dataview.debugger.logSuccessfulMessage("Worklfow executotion has been terminatted due to sending file failure");
			System.exit(0);
		}
	}

	@Override
	List<String> getBashCommands() {
		ArrayList<String> commands = new ArrayList<String>();
		commands.add("nohup ./run.sh &> /home/ubuntu/log.txt &");
		return commands;
	}

	@Override
	void sendDiskImage(String SourceDIR, String DestinationDIR,
			String strHostName) {
		try {
			copyFileVM(SourceDIR, DestinationDIR, strHostName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	public int initVM(String ip,String machineType){
		if (machineType.equals("AWS")) {
			this.executeCommands(ip, WorkflowExecutor_Alpha.SEV_AWS_USER_NAME, WorkflowExecutor_Alpha.SEV_AWS_USER_PASSWORD,
					this.getBashCommands());
		}
		else 
			{return 1;}
		
		return 0;
	}
	

}
