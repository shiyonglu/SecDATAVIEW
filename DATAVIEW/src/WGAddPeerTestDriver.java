import java.io.IOException;

import dataview.workflowexecutor.VMProvisionerSGX;

public class WGAddPeerTestDriver {

	public static void main(String[] args) throws IOException {
		
		VMProvisionerSGX o =new VMProvisionerSGX();
		 
		if(0== o.wgAddPeer("172.30.18.187","56002","dataview" , "wg-key.txt"))
		{
			
			System.out.println("SGX-LKL enclave WG VPN public key has been added");

		}
		else
		{
			
			System.out.println("SGX-LKL enclave WG VPN public could not be added");

		}
		if(0==o.wgRemovePeer("dataview" , "wg-key.txt"))
		{
			
			System.out.println("SGX-LKL enclave WG VPN public key has been removed");

		}
		
		/*String[] cmd = {"/bin/bash","-c","echo dataview| sudo /usr/bin/wg"  };
	    Process pb = Runtime.getRuntime().exec(cmd);

	    String line;
	    BufferedReader input = new BufferedReader(new InputStreamReader(pb.getInputStream()));
	    while ((line = input.readLine()) != null) {
	        System.out.println(line);
	    }
	    input.close();
	}  */
	}
}


