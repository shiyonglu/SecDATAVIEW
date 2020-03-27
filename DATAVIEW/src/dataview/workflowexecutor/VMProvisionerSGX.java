package dataview.workflowexecutor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import dataview.models.Dataview;
import dataview.models.JSONArray;
import dataview.models.JSONObject;
import dataview.models.JSONParser;

public class VMProvisionerSGX extends CloudResourceManagement{

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
				IP = (String) machineType.get("SGX").toString().replace("\"", "");
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
		} catch (Exception e) { 
			e.printStackTrace();
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

	public void copyFileVMhost(String SourceDIR, String DestinationDIR, String strHostName) {

		boolean isFoundException = true;
		int countIteration = 15;
		System.out.println("Trying to send file : " + SourceDIR + " to " + strHostName );
		while(isFoundException && countIteration-- > 0) {
			String SFTPHOST = strHostName;
			int SFTPPORT = 22;
			String SFTPUSER = WorkflowExecutor_Alpha.SGX_SERVER_USER_NAME;//"ubuntu";
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
				session.setPassword(WorkflowExecutor_Alpha.SGX_SERVER_PASSWORD);
				//session.setPassword("dataview");
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
	List<String> getBashCommands() {
		List<String> commands = new ArrayList<String>();
		///
		
		//commands.addAll(readScriptFile());
		//
		//commands.add("nohup java -Djava.class.path=/home/ubuntu/ -jar /home/ubuntu/sshd.jar &> /home/ubuntu/log.txt &");
		//commands.add("cd /home/ubuntu/sgx-lkl/apps/jvm/helloworld-java/;");
		//commands.add("nohup ./runEncrypted.sh &> log.txt &");
		//	    commands.add("nohup ./run.sh &> log.txt &");
		//commands.add("chmod +x /home/ubuntu/sgx-lkl-java-encrypted-dataview.sh");
		commands.add("chmod +x /home/ubuntu/sgx-lkl-server-remote-launch.sh");
		commands.add("nohup /home/ubuntu/sgx-lkl-server-remote-launch.sh > /home/ubuntu/log.txt 2>&1&");
		/*commands.add("SGXLKL_HEAP=2014M SGXLKL_KEY=\"/home/ubuntu/sgx-lkl/build/config/enclave_debug.key\" "
				+ "SGXLKL_VERBOSE=1 SGXLKL_TAP=sgxlkl_tap0 SGXLKL_REMOTE_CONFIG=1  SGXLKL_REMOTE_CMD_ETH0=1 "
				+ "SGXLKL_IAS_SPID=12345678910 SGXLKL_IAS_SUBSCRIPTION_KEY=12345678910 "
				+ "SGXLKL_REPORT_NONCE=10867864710722948371 SGXLKL_IAS_SPID=12345678910 SGXLKL_MMAP_FILES=Shared SGXLKL_"
				+ "STHREADS=4 SGXLKL_ETHREADS=4 /home/ubuntu/sgx-lkl/build/sgx-lkl-run ./SecDATAVIEW-v3-img.enc";*/
		
		Dataview.debugger.logSuccessfulMessage("bash commands");
		return commands;
	}


	List<String> readScriptFile() {
		List<String> commands = new ArrayList<>();
		try (FileReader reader = new FileReader(WorkflowExecutor_Alpha.SGX_SCRIPT_SRC_FILE);
				BufferedReader br = new BufferedReader(reader)) {
			String line;
			while ((line = br.readLine()) != null) {
				commands.add(line);
			}
		} catch (Exception e) {
			System.err.format("IOException: %s%n", e);
		}
		return commands;
	}

	@Override
	void sendDiskImage(String SourceDIR, String DestinationDIR,
			String strHostName) {
		// TODO Auto-generated method stub
		copyFileVM(SourceDIR, DestinationDIR, strHostName);

	}
	// This method runs the sgx-lkl-ctl // port should be 56001. Update the env and bashcommand string related to your path settings in the master node
	public int executeSGXremoteControl(String SGXworkerIp,String remoteCTLPort){
		String[] env = {"PATH=/bin:/usr/bin/:/home/ubuntu/:/home/ubuntu/sgx-lkl/build/:/home/ishtiaq/:/home/ishtiaq/sgx-lkl/build/:/home/ubuntu/secureDW/machineScript/SGX/"};
		String bashCommand ="/home/mofrad-s/sgx-lkl/build/sgx-lkl-ctl --server="+SGXworkerIp+":"+remoteCTLPort+" run --app=/home/mofrad-s/secureDW/machineScript/SGX/secdataview.app.conf"; //e.g test.sh -dparam1 -oout.txt
		 String s = null;
		try {
			Process process = Runtime.getRuntime().exec(bashCommand, env);
			BufferedReader stdInput = new BufferedReader(new 
	                 InputStreamReader(process.getInputStream()));

	            BufferedReader stdError = new BufferedReader(new 
	                 InputStreamReader(process.getErrorStream()));

	            // read the output from the command
	          //  System.out.println("Here is the standard output of the command:\n");
	           // while ((s = stdInput.readLine()) != null) {
	            //    System.out.println(s);
	            //}
	            
	            // read any errors from the attempted command
	            System.out.println("Here is the standard error of the command (if any):\n");
	            while ((s = stdError.readLine()) != null) {
	                System.out.println(s);
	            }
	            
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(e);
		}
		
		return 0;
	}

	//Update the bashcommand string related to your path settings in the master node
	public synchronized int executeSGXremoteAttestation(String SGXworkerIp,String remoteCTLPort){
		String[] env = {"PATH=/bin:/usr/bin/:/home/ubuntu/:/home/ubuntu/sgx-lkl/build/:/home/mofrad-s/:/home/ishtiaq/sgx-lkl/build/:/home/mofrad-s/secureDW/machineScript/SGX/"};
		//String bashCommand ="/home/ubuntu/sgx-lkl/build/sgx-lkl-ctl --server="+SGXworkerIp+":"+remoteCTLPort+" run --app=/home/ubuntu/secureDW/machineScript/SGX/secdataview.app.conf"; //e.g test.sh -dparam1 -oout.txt
		//String bashCommand="/home/ubuntu/sgx-lkl/build/sgx-lkl-ctl --ias-spid=12345678910 --ias-skey=12345678910 --mrenclave=f935aa698f54a508032f26fc8c28d28c0a2cea5779e2a282049623fb22d5ec92 --mrsigner=f3b24a591ba692e90bd8a02ca4241a2a73b2128e90041d048fc84cf5fba5d7f6 --ias-quote-type=\"unlinkable\" --nonce=10867864710722948371 --server="+SGXworkerIp+":"+remoteCTLPort+" attest --app=/home/ubuntu/secureDW/machineScript/SGX/secdataview.app.conf";
		String bashCommand="/home/mofrad-s/sgx-lkl/build/sgx-lkl-ctl --ias-spid="+WorkflowExecutor_Alpha.IAS_SPID+" --ias-skey="+WorkflowExecutor_Alpha.IAS_SKEY+" --mrenclave="+WorkflowExecutor_Alpha.SGX_MRENCLVE+" --mrsigner="+WorkflowExecutor_Alpha.SGX_MRSIGNER+" --ias-quote-type=\"unlinkable\" --nonce=10867864710722948371 --server="+SGXworkerIp+":"+remoteCTLPort+" attest --app=/home/mofrad-s/secureDW/machineScript/SGX/secdataview.app.conf";
		
		//--mrenclave=f935aa698f54a508032f26fc8c28d28c0a2cea5779e2a282049623fb22d5ec92 --mrsigner=f3b24a591ba692e90bd8a02ca4241a2a73b2128e90041d048fc84cf5fba5d7f6
		String s = null;
	            int exitVal=0; 
		try {
			//
			Process process = Runtime.getRuntime().exec(bashCommand, env);
			BufferedReader stdInput = new BufferedReader(new 
	                 InputStreamReader(process.getInputStream()));

	            BufferedReader stdError = new BufferedReader(new 
	                 InputStreamReader(process.getErrorStream()));

	            // read the output from the command
	          //  System.out.println("Here is the standard output of the command:\n");
	           // while ((s = stdInput.readLine()) != null) {
	            //    System.out.println(s);
	            //}
	            
	            // read any errors from the attempted command
	            System.out.println("Here is the standard error of the command (if any):\n");
	            while ((s = stdError.readLine()) != null) {
	                System.out.println(s);
	            }
		           //process.destroy();
		           process.waitFor();  
	           exitVal= process.exitValue();
	            
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(e);
			
		}
		
//WCPAC step 9- (attestationReport==satisfactoryValues)? Continue : Terminate
		 if (0 != exitVal) {
			Dataview.debugger.logErrorMessage("SGX remote attestation has failed! for worker "
					+ SGXworkerIp + " workflow execution is now being terminated");
			System.exit(1);
		}
//implementing WCPAC step 10- VPNaddPeer(VPNpubKey_W) 11-executeTEEremoteControl(ip) 12-VPNmessage(TEELAunchApp(JVM_config,diskImageCryptographyKey) 13-VPNremovePeer(VPNpubKey_W)
// WCPAC step 10-11-12-13 are atomic and appropriate functions is called inside the wgAddPeer() function respectively.

		if (0 == wgAddPeer(SGXworkerIp, "56002", WorkflowExecutor_Alpha.MASTER_NODE_PASSWORD, "wg-key.txt")) {
			Dataview.debugger.logSuccessfulMessage("enclave WG-VPN peer added successfuly");
		} else {
			Dataview.debugger.logErrorMessage("enclave WG-VPN peer could not be added!");
		};
		return exitVal;
	}
	
	
	public int wgAddPeer(String WGpeerIP,String peerPort, String SecDWMasterSudoPass, String peerPublicKeyFilePath) {
		String WGenclPubKey;

		StringBuilder contentBuilder = new StringBuilder();
	    try (Stream<String> stream = Files.lines( Paths.get(peerPublicKeyFilePath), StandardCharsets.UTF_8)) 
	    {
	        stream.forEach(s -> contentBuilder.append(s).append(""));
	    }
	    catch (IOException e) 
	    {
	        e.printStackTrace();
	    }
	    WGenclPubKey =  contentBuilder.toString();
		
		
		
		String wgCommands="/usr/bin/wg set wgsgx0 peer "+WGenclPubKey+" allowed-ips 10.0.4.1/32 endpoint "+WGpeerIP+":"+peerPort;
			//wg set wgsgx0 peer 2bC2HX3NCn2aICXladL5Rcy7PJmLcvffW1ro7pYjw2o= allowed-ips 10.0.2.1/32 endpoint 192.168.10.1:56002	
	            int exitVal=0; 
		try {
			String[] cmd = {"/bin/sh","-c","echo " +SecDWMasterSudoPass+ " | sudo -S " + wgCommands  };
		    Process pb = Runtime.getRuntime().exec(cmd);

		    String line;
		    String Error;
		    BufferedReader input = new BufferedReader(new InputStreamReader(pb.getInputStream()));
		    while ((line = input.readLine()) != null) {
		        System.out.print(line);
		    }
		    
		    BufferedReader error = new BufferedReader(new InputStreamReader(pb.getErrorStream()));
		    while ((Error = error.readLine()) != null) {
		        System.out.print(Error);
		    }
		    error.close();
		    input.close();
		    pb.waitFor();
	           exitVal= pb.exitValue();
	           if(exitVal==0) {
	           executeSGXremoteControl("10.0.4.1", "56001");}
	           else
	           {  Dataview.debugger.logErrorMessage("Error in WireGuard peer setup");}
	           if (0 == wgRemovePeer(WorkflowExecutor_Alpha.MASTER_NODE_PASSWORD, "wg-key.txt")) {
					Dataview.debugger.logSuccessfulMessage("enclave WG-VPN peer removed successfuly");

				}
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(e);
			
		}
		
		return exitVal;
	}
	public int wgRemovePeer(String SecDWMasterSudoPass, String peerPublicKeyFilePath) {
		
		
		String WGenclPubKey;

		StringBuilder contentBuilder = new StringBuilder();
	    try (Stream<String> stream = Files.lines( Paths.get(peerPublicKeyFilePath), StandardCharsets.UTF_8)) 
	    {
	        stream.forEach(s -> contentBuilder.append(s).append(""));
	    }
	    catch (IOException e) 
	    {
	        e.printStackTrace();
	    }
	    WGenclPubKey =  contentBuilder.toString();
		
		String wgCommands="/usr/bin/wg set wgsgx0 peer "+WGenclPubKey+" remove";
			//wg set wgsgx0 peer 2bC2HX3NCn2aICXladL5Rcy7PJmLcvffW1ro7pYjw2o= remove	
	            int exitVal=0; 
		try {
			String[] cmd = {"/bin/sh","-c","echo " +SecDWMasterSudoPass+ " | sudo -S " + wgCommands  };
		    Process pb = Runtime.getRuntime().exec(cmd);

		    String line;
		    String Error;
		    BufferedReader input = new BufferedReader(new InputStreamReader(pb.getInputStream()));
		    while ((line = input.readLine()) != null) {
		        System.out.print(line);
		    }
		    BufferedReader error = new BufferedReader(new InputStreamReader(pb.getErrorStream()));
		    while ((Error = error.readLine()) != null) {
		        System.out.print(Error);
		    }
		    error.close();
		    input.close();
		    pb.waitFor();
	           exitVal= pb.exitValue();
	            
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(e);
			
		}
		
		return exitVal;
	}
	// call received from WCPAC.step1:  step 1- initVM(machineType,ip) that is a function call to the Cloud Resource Management from Workflow Executor

		/*
		 *
		 *    a) send diskImage to each worker
		 *    b) send machineScript to each worker and the VPN public key to each worker node for SGX workers
		 *    c) execute remote attestation. upon successful attestation the VPN public key of enclave is received from enclave.
		 *    d) add VPN peer between master node and the worker node using enclave VPN public key
		 *    e) execute TEE remote control that sends disk image decryption key and enclave app configuration through the VPN tunnel
		 *    f) remove the VPN peer at master node
		 *    g) return the caller which is the workflow executor
		 *    
		 */
	public int initVM(String ip,String machineType)
	{
		if (machineType.equals("SGX")) {
			//WCPAC.step 2: send(diskImage) this function send the SGX-LKL disk image to the worker node. The disk image includes the LibOS, JVM and SSHD.jar

								this.copyFileVMhost(WorkflowExecutor_Alpha.SGX_IMG_SRC_FILE, WorkflowExecutor_Alpha.SGX_IMG_DST_FOLDER, ip);
								
			//WCPAC.step 3: send(machineScript(VPNPubKey_M)); VPNPubKey_M is part of the machineScript. this function sends the machine script to the worker node. VPN public key of master nod in included with the script.  

								this.copyFileVMhost(WorkflowExecutor_Alpha.SGX_SCRIPT_SRC_FILE, WorkflowExecutor_Alpha.SGX_SCRIPT_DST_FOLDER, ip); 
			//WCPAC.step 4- message(initVM); this function send message to the worker node OS to launch the machine script.
																											
								this.executeCommands(ip, WorkflowExecutor_Alpha.SGX_SERVER_USER_NAME, WorkflowExecutor_Alpha.SGX_SERVER_PASSWORD,
										this.getBashCommands()); // send message to execute commands in the worker node wait 10 seconds to launch process is completed
								try {
									System.out.println(
											"System is halt after running the execute commands of SGX_SCRIPT for 10 seconds");
									Thread.sleep(10 * 1000);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
			/*WCPAC.step 5, WCPAC.step 6, WCPAC.step 7, WCPAC.step 8, WCPAC.step 9,  WCPAC.step 10, WCPAC.step 11, WCPAC.step 12 are implemented during following function call
			* this functions use  SGX-LKL-CLT to launch the remote attestation. SGX-LKL-CTL running in the master node communicate with the SGX-LKL running in the worker and asking the attestation Qoute value, then the Qoute including the enclave public VPN key is sent tot the SGx-LKL-CTL. At this moment
			*					 SGX-LKL-CTL communicate with the IAS to attest the Qoute. after receiving the attestation report, SGX-LKL-CTL evaluate the report and return 0 if attestation is successful other wise terminates the execution. details of the SGX-LKL control is found in the SGX-LKL Github repository 
			*/ 
								
								if (0 != this.executeSGXremoteAttestation(ip, "56000")) {
									Dataview.debugger.logErrorMessage("SGX remote attestation has failed! for worker "
											+ ip + " workflow execution is now being terminated");
									System.exit(1);
								}
								
								} 
		else {return 1;}
			
		
		return 0;
		
	}
	
}
