package dataview.workflowexecutor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import dataview.models.Dataview;
import dataview.models.JSONArray;
import dataview.models.JSONObject;
import dataview.models.JSONParser;
import dataview.workflowexecutor.CloudResourceManagement;

public class VMProvisionerAMD extends CloudResourceManagement{

	@Override
	List<String> getAvailableIPs(int totalMachine) {
		String input = "";
		File file = new File("confidentialInfo/IPPool.txt");
		BufferedReader bufferredReader = null;
		try {
			bufferredReader = new BufferedReader(new FileReader(file));
			String line;
			while ((line = bufferredReader.readLine()) != null) {
				input += line;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		JSONParser parser = new JSONParser(input);
		JSONObject taskObjects = parser.parseJSONObject();
		JSONArray ipPoolJSONArray = taskObjects.get("IPPool").toJSONArray();

		List<String> IPs = new ArrayList<>();
		
		for (int i = 0; i < ipPoolJSONArray.size(); i++) {
			JSONObject machineType = ipPoolJSONArray.get(i).toJSONObject();
			String IP = null;
			try {
				IP = (String) machineType.get("AMD").toString().replace("\"", "");
				IPs.add(IP);
			} catch (Exception e) {
				// don't need to do
			}
			
		}
		return IPs;
	}


	@Override
	void executeCommands(String strHostName, String strUserName,String password, List<String> commands) {		
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
	

	public static void executeCommandsHost(String strHostName, String strUserName,String password) {		
		try {
			List<String> commands = new ArrayList<>();
			commands.add("chmod +x /home/mofrad-s/vm1-launch-dataview-sev.sh");
			commands.add("/home/mofrad-s/vm1-launch-dataview-sev.sh");
			
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
			try {
				Thread.sleep(Integer.MAX_VALUE);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}										
			session.disconnect();
			channel.disconnect();
		} catch (Exception e) { 
			e.printStackTrace();
		}
	}

	
	public void copyFileVMhost(String SourceDIR, String DestinationDIR, String strHostName) {

		boolean isFoundException = true;
		int countIteration = 15;
		System.out.println("Trying to send file : " + SourceDIR + " to " + strHostName );
		while(isFoundException && countIteration-- > 0) {
			String SFTPHOST = strHostName;
			int SFTPPORT = 22;
			String SFTPUSER = WorkflowExecutor_Alpha.AMD_SERVER_USER_NAME;
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
				session.setPassword(WorkflowExecutor_Alpha.AMD_SERVER_PASSWORD);
				session.setConfig(config);
				session.setPort(SFTPPORT);
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
		
		
	}

	
	@Override
	void copyFileVM(String SourceDIR, String DestinationDIR, String strHostName) {

		boolean isFoundException = true;
		int countIteration = 15;
		System.out.println("Trying to send file : " + SourceDIR + " to " + strHostName );
		while(isFoundException && countIteration-- > 0) {
			String SFTPHOST = strHostName;
			String SFTPWORKINGDIR = DestinationDIR;
			String FILETOTRANSFER = SourceDIR;
			Session session = null;
			Channel channel = null;
			ChannelSftp channelSftp = null;
			try {
				JSch jsch = new JSch();
				//		jsch.addIdentity(pemFileLocation);
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
		
	}

	@Override
	List<String> getBashCommands() {
		List<String> commands = new ArrayList<String>();
//		commands.add("nohup java -Djava.class.path=/home/ubuntu/ -jar /home/ubuntu/sshd.jar &> /home/ubuntu/log.txt &");
		
		commands.add("nohup ./run.sh &> /home/ubuntu/log.txt &");
		return commands;
	}


	public static List<String> getBashCommandsForHost() {
		List<String> commands = new ArrayList<String>();
		commands.add("chmod +x /home/mofrad-s/vm1-launch-dataview-sev.sh");
		//commands.add("nohup /home/mofrad-s/vm1-launch-dataview-sev.sh > /home/mofrad-s/log.txt 2>&1&");
		//commands.add("/home/mofrad-s/vm1-launch-dataview-sev.sh");
		
		return commands;
	}


	@Override
	void sendDiskImage(String SourceDIR, String DestinationDIR,
			String strHostName) {
		// TODO Auto-generated method stub
		copyFileVM(SourceDIR, DestinationDIR, strHostName);
		
	}
	public int initVM(String ip,String machineType){
		if (machineType.equals("AMD")) {
			//WCPAC.step 2: send(diskImage)

								this.copyFileVMhost(WorkflowExecutor_Alpha.AMD_IMG_SRC_FILE, WorkflowExecutor_Alpha.AMD_IMG_DST_FOLDER, WorkflowExecutor_Alpha.AMD_SERVER_IP);
			//WCPAC.step 3: send(machineScript(VPNPubKey_M)); AMD machineScript  does not implement VPN at this moment since it follows a different attestation mechanism that we did not implement for TIFS journal

								this.copyFileVMhost(WorkflowExecutor_Alpha.AMD_SCRIPT_SRC_FILE, WorkflowExecutor_Alpha.AMD_SCRIPT_DST_FOLDER, WorkflowExecutor_Alpha.AMD_SERVER_IP);

								Thread executeCommandHostThread = new Thread(new Runnable() {
									@Override
									public void run() {
			//WCPAC.step 4: message(initVM) this message launch SEV VM

										VMProvisionerAMD.executeCommandsHost(WorkflowExecutor_Alpha.AMD_SERVER_IP, WorkflowExecutor_Alpha.AMD_SERVER_USER_NAME,
												WorkflowExecutor_Alpha.AMD_SERVER_PASSWORD);
									}
								});
								executeCommandHostThread.start();

								try {
									System.out.println(
											"System is halt after running the execute commannds of AMD_SCRIPT for 20 seconds");
									Thread.sleep(20 * 1000);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
			//WCPAC.step 4: message(initVM), this message launches SSHD.jar in the running SEV VM

								this.executeCommands(ip, WorkflowExecutor_Alpha.SEV_AWS_USER_NAME, WorkflowExecutor_Alpha.SEV_AWS_USER_PASSWORD,
										this.getBashCommands());
		
	}
		else {return 1;}
		
		return 0;
	}
}
