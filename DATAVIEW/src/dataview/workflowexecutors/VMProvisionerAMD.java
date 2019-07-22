package dataview.workflowexecutors;

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
import dataview.workflowexecutors.VMProvisioner;

public class VMProvisionerAMD extends VMProvisioner{

	@Override
	List<String> getAvailableIPs(int totalMachine) {
		String input = "";
		File file = new File("IPPool.txt");
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
	

	@Override
	void copyFileVM(String SourceDIR, String DestinationDIR, String strHostName) {

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

	@Override
	List<String> getBashCommands() {
		List<String> commands = new ArrayList<String>();
//		commands.add("nohup java -Djava.class.path=/home/ubuntu/ -jar /home/ubuntu/sshd.jar &> /home/ubuntu/log.txt &");
		commands.add("nohup ./run.sh &> /home/ubuntu/log.txt &");
		return commands;
	}


	@Override
	void sendDiskImage(String SourceDIR, String DestinationDIR,
			String strHostName) {
		// TODO Auto-generated method stub
		copyFileVM(SourceDIR, DestinationDIR, strHostName);
		
	}
}
