import java.io.File;
import java.io.FileInputStream;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import dataview.models.Dataview;

public class FileTransfer {
	public static void send (String SourceDIR, String DestinationDIR, String newFileName, String strHostName) {
	    String SFTPHOST = strHostName;
	    int SFTPPORT = 8000;
	    String SFTPUSER = "ubuntu";
	    String SFTPPASS = "dataview";
	    String SFTPWORKINGDIR = DestinationDIR;

	    Session session = null;
	    Channel channel = null;
	    ChannelSftp channelSftp = null;
	    System.out.println("preparing the host information for sftp.");

	    try {
	        JSch jsch = new JSch();
	        session = jsch.getSession(SFTPUSER, SFTPHOST, SFTPPORT);
	        session.setPassword(SFTPPASS);
	        java.util.Properties config = new java.util.Properties();
	        config.put("StrictHostKeyChecking", "no");
	        session.setConfig(config);
	        session.connect();
	        System.out.println("Host connected.");
	        channel = session.openChannel("sftp");
	        channel.connect();
	        System.out.println("sftp channel opened and connected.");
	        channelSftp = (ChannelSftp) channel;
	        channelSftp.cd(SFTPWORKINGDIR);
//	        File f = new File("/home/ubuntu/Desktop/jvmtop-0.8.0.tar.gz");
//	        File f = new File("/home/ubuntu/Desktop/dummycopy.jar");
//	        channelSftp.put(new FileInputStream(f), f.getName());
	        File f = new File(SourceDIR);
	        channelSftp.put(new FileInputStream(f), newFileName);
	    } catch (Exception ex) {
	        System.out.println("Exception found while tranfer the response. : " + ex);
	    } finally {
	        channelSftp.exit();
	        System.out.println("sftp Channel exited.");
	        channel.disconnect();
	        System.out.println("Channel disconnected.");
	        session.disconnect();
	        System.out.println("Host Session disconnected.");
	    }
	}
	
	public static void send (String SourceDIR, String DestinationDIR, String newFileName, String strHostName, String userName, String password, int port) {
	    String SFTPHOST = strHostName;
	    int SFTPPORT = port;
	    String SFTPUSER = userName;
	    String SFTPPASS = password;
	    String SFTPWORKINGDIR = DestinationDIR;

	    Session session = null;
	    Channel channel = null;
	    ChannelSftp channelSftp = null;
	    System.out.println("preparing the host information for sftp.");

	    try {
	        JSch jsch = new JSch();
	        session = jsch.getSession(SFTPUSER, SFTPHOST, SFTPPORT);
	        session.setPassword(SFTPPASS);
	        java.util.Properties config = new java.util.Properties();
	        config.put("StrictHostKeyChecking", "no");
	        session.setConfig(config);
	        session.connect();
	        System.out.println("Host connected.");
	        channel = session.openChannel("sftp");
	        channel.connect();
	        System.out.println("sftp channel opened and connected.");
	        channelSftp = (ChannelSftp) channel;
	        channelSftp.cd(SFTPWORKINGDIR);
//	        File f = new File("/home/ubuntu/Desktop/jvmtop-0.8.0.tar.gz");
//	        File f = new File("/home/ubuntu/Desktop/dummycopy.jar");
//	        channelSftp.put(new FileInputStream(f), f.getName());
	        File f = new File(SourceDIR);
	        channelSftp.put(new FileInputStream(f), newFileName);
	    } catch (Exception ex) {
	        System.out.println("Exception found while tranfer the response. : " + ex);
	    } finally {
	        channelSftp.exit();
	        System.out.println("sftp Channel exited.");
	        channel.disconnect();
	        System.out.println("Channel disconnected.");
	        session.disconnect();
	        System.out.println("Host Session disconnected.");
	    }
	}
}
