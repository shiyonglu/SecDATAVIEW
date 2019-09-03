import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import dataview.models.Dataview;

public class Dummy {
	public static void main(String[] args) {
		List<String> commands = new ArrayList<>();
		commands.add("nohup ./run.sh &> /home/ubuntu/log.txt &");
		executeCommands("52.90.62.52", "ubuntu", "", commands);
	}

	static void executeCommands(String strHostName, String strUserName,String password, List<String> commands) {		

		Dataview.debugger.logSuccessfulMessage("Executing commands in AWS");

		try {
			JSch jsch = new JSch();
			jsch.addIdentity("confidentialInfo/dataview1.pem");
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

}
