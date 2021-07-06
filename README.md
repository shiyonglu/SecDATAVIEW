SecDATAVIEW: A Secure Big Data Workflow Management System for Heterogeneous Computing Environments
==================================================================================================
SecDATAVIEW is a secure big data workflow management system compatible with the heterogeneous computing environment. It leverages hardware-assisted TEEs such as Intel SGX and AMD SEV to protect the execution of workflows in the untrusted cloud. 
The SecDATAVIEW paper has appeared in proceedings of The 35th Annual Computer Security Applications Conference 
(ACSAC'19), San Juan, Puerto Rico, December 2019. You may access paper here https://dl.acm.org/doi/10.1145/3359789.3359845
The first release of SecDATAVIEW implemented the artifacts of the ACSAC'19 paper. You may download and use the first release corresponding to ACSAC'19 paper.
We have enhanced the SecDATAVIEW with additional security measurements to address the attacks that mainly fake the presence of TEE with leveraging real-time Intel-based SGX attestation and attacks that mainly happen after the workflow execution is finished (e.g., when data owner shutdown VPCs and left the cloud environments). All together is available as the current edition. A new paper regarding modifications with a set of additional experimental results is currently under review. We will provide the venue name and the link to the paper later here.   

Seqence diagram of new Workflow Code Provisioning and Communication (WCPAC) protocol
------------------------------------------------------------------------------------
SecDATAVIEW implemented WCPAC protocol for (1) provisioning and attesting secure worker nodes, (2) securely provisioning the code for the ```Task Executor``` and workflow tasks on each participating worker node, (3) establishing a secure communication channel between the master node and each worker node, and (4) establishing secure communication channels among worker nodes for secure data transfer.
The detail of the WCPAC will be available in the new paper. Also WCPAC steps are mapped at code level and may be located through comments.
![](/WCPAC/WCPAC.png)

Following Video explains WCPAC integration

[![IMAGE ALT TEXT HERE](https://img.youtube.com/vi/JZJtPtqN_xU/0.jpg)](https://www.youtube.com/watch?v=JZJtPtqN_xU)

Prerequisites
-------------
SecDATAVIEW has been tested on Ubuntu 16.04 LTS for SGX worker nodes and 18.04 LTS for SEV worker node. 

Setting up the SGX worker node
------------------------------
On every SGX worker node, install Ubuntu 16.04 LTS then follow the instruction below.

1- SecDATAVIEW default SGX server's username and password is user="ubuntu" and password="dataview" respectively. Also, SecDATAVIEW tries to send the sgx-lkl disk image and scripts into the ```/home/ubuntu``` folder in the SGX servers by default. It is recommended to create the same user credential in every sgx nodes for a smooth test of provided workflows in this repository. Also, the worker's credential setting can be updated within the SecDATAVIEW at any time.  
2- Install the Intel SGX driver https://github.com/01org/linux-sgx-driver is required. 
We have tested SecDATAVIEW with driver versions 2.0.

3- Install the SGX-LKL Library OS from https://github.com/lsds/sgx-lkl build it in hardware mode and sign the enclave following its instruction; make sure you can run the sample java application provided with SGX-LKL library OS.

4- Instal OpenSSH-SERVER and make sure you can make an ssh connection to your SGX machine.
```bash
sudo apt install openssh-server
ssh <your user name>@<Your SGX server ip> 
```
5- Download the pre-created sgx-lkl disk image from the link below and save the disk image in the SecDATAVIEW Master node.
 https://www.dropbox.com/sh/hywa7du0sr70nec/AABAyHqD_4tPYXVUNdw2bQAEa?dl=0



Networking support for SGX worker node
---------------------------------------

In order for SecDATAVIEW SGX-LKL application to send and receive packets via the network, a TAP interface is needed on the host. Besides, all the network traffic towards the worker node on TCP port 8000, TCP port 7700, TCP port 56000 and UDP port 56002 should be forwarded to the TAP interface. Create and configure the TAP interface as follows:
```bash
#Lets create a TAP interface
sudo ip tuntap del dev sgxlkl_tap0 mode tap
sudo ip tuntap add dev sgxlkl_tap0 mode tap user `whoami`
sudo ip link set dev sgxlkl_tap0 up
sudo ip addr add dev sgxlkl_tap0 10.0.1.254/24
```
To forward the network packets, the OS iptables should be updated with the following commands: 
```bash
#Lets backup the OS iptables first. Keep this file to restore the iptables rules at a later time if something got wrong

sudo iptables-save > ~/iptables-bak

#Rules to update the PREROUTING table for NAT

sudo iptables -t nat -A PREROUTING ! -s 10.0.1.0/24 -p tcp -m tcp --dport 8000 -j DNAT --to-destination 10.0.1.1:8000
sudo iptables -t nat -A PREROUTING ! -s 10.0.1.0/24 -p tcp -m tcp --dport 7700 -j DNAT --to-destination 10.0.1.1:7700
# Forward tcp traffic to enclave attestation endpoint
sudo iptables -t nat -A PREROUTING ! -s 10.0.1.0/24 -p tcp -m tcp --dport 56000 -j DNAT --to-destination 10.0.1.1:56000
# Forward udp traffic to enclave Wireguard endpoint (from the SGX-LKL network setup instructions)
sudo iptables -t nat -I PREROUTING -p udp -i eth0 --dport 56002 -j DNAT --to-destination 10.0.1.1:56002

#Rule to update the POSTROUTING table that provides internet access for worker node

sudo iptables -t nat -A POSTROUTING -s 10.0.1.0/24 ! -d 10.0.1.0/24 -j MASQUERADE

#Rules to update the FORWARD table for packet forwarding shoud be available in both master and work nodes

sudo iptables -I FORWARD -m state -d 10.0.0.0/8 --state NEW,RELATED,ESTABLISHED -j ACCEPT
sudo iptables -I FORWARD -m state -s 10.0.0.0/8 --state NEW,RELATED,ESTABLISHED -j ACCEPT

#You must enable the packet forwarding in the system module unless packet routing does not work.
sudo sysctl -w net.ipv4.ip_forward=1
```
Also, you can make an executable bash script for the network setup with above commands (copy and paste all the commands in your script) and execute it in your SGX worker. 

Setting up the SEV worker node
------------------------------
SEV worker node requires an AMD server that supports the SEV feature. SEV feature must be enabled in the BIOS of the server, and instructions provided below should be followed.

1- Install and prepare the SEV HOST for the UBUNTU 18 OS by following the https://github.com/AMDESE/AMDSEV

2- On the host machine, install OpenSSH-SERVER, then set the 'root' password. SecDATAVIEW uses 'root' account to remotely launch the SEV VMs in the AMD server.
```bash
sudo apt get install openssh-server
sudo su
passwd "set the password for 'root'"
```
3-give the 'root' account the SSH access by following the instruction below

https://linuxconfig.org/allow-ssh-root-login-on-ubuntu-18-04-bionic-beaver-linux 

4- make sure you can get ssh access to the AMD server with 'root' account.
```bash
ssh root@"Your AMD server IP" 
```
5- Download the pre-created SEV disk image <sev-image.qcow2> from the link below and save the disk image in your SecDATAVIEW Master node.

 https://www.dropbox.com/sh/hywa7du0sr70nec/AABAyHqD_4tPYXVUNdw2bQAEa?dl=0


Networking support for SEV worker node
--------------------------------------

In order SecDATAVIEW SEV VM to send and receive packets via the network, a TAP interface is needed on the host AMD server for every single SEV VM. The TAP interface should be bridged to the ethernet interface with internet access so that every SEV VM receives its IP addresses from the LAN DHCP server. Create and configure each TAP interface as follows:
```bash
sudo  brctl addbr br0
#replace "enp98s0" with your ethernet interface name
sudo ip addr flush dev enp98s0
sudo brctl addif br0 enp98s0
sudo ip tuntap add dev tap0 mode tap user `whoami`
sudo brctl addif br0 tap0
sudo ifconfig enp98s0 up
sudo ifconfig tap0 hw ether cb:9a:78:56:34:12 up
sudo ifconfig br0 hw ether 12:34:56:78:9a:bc up
sudo  dhclient -v br0
```
Also, you can make an executable bash script for the network setup with above commands (copy and paste all the commands in your script) and execute it in your AMD server. 

Follow the article below for more information regarding network configuration in KVM and QEMU that is used by SEV VM  
https://gist.github.com/extremecoders-re/e8fd8a67a515fee0c873dcafc81d811c

Setting up SecDATAVIEW master node
-----------------------------------

We have tested SecDATAVIEW master on Ubuntu 16.04 LTS
Please follow the instruction below to setup the master node.

1- Install OpenJDK 1.8
```bash
sudo apt install openjdk-8-jdk
```

2- Install Eclipse ide for JAVA from the official Eclipse website. We have tested the SecDATAVIEW with (Oxygen.3a Release (4.7.3a)

3- Install git on your master node. 
```bash
sudo apt install git
```

4- Clone the SecDATAVIEW project from git on your master node. 
```bash
git clone https://github.com/shiyonglu/SecDATAVIEW.git
```

5- Download and copy the binaries of modified SGX-LKL in your home folder. The current release uses the modified version of SGX-LKL for Intel SGX attestation purposes.  You may find the binaries and source codes in https://www.dropbox.com/sh/hywa7du0sr70nec/AABAyHqD_4tPYXVUNdw2bQAEa?dl=0

6- Install Wire Guard VPN on the master node
``` bash
sudo add-apt-repository ppa:wireguard/wireguard
sudo apt update
sudo apt install wireguard
#From the /machineScript/SGX/WireGuard_VPN_Setup/ folder run wg-sgxlkl-client.sh to setup master node wire guard endpoint.
#wgclient.priv and wgclient.pub key files in /WireGuard_VPN_Setup/ folder should be available to wg-sgxlkl-client.sh.
#Make sure the wire guard is functioning and correctly by calling into following status command and get a result similar to the below image.
sudo wg
```
![](machineScript/SGX/WireGuard_VPN_Setup/wg-image.png)

7- Create a new workspace on your Eclipse IDE

8- Import the SecDATAVIEW project into your Eclipse workspace. Subfolder ```DATAVIEW``` should be imported into the Eclipse workspace.
9- Update your workflow; consult Tutorials in https://github.com/shiyonglu/DATAVIEW to learn about creating a workflow. Also, this repository contains two pre-created workflow for the test purpose.

10- Update the ```config.txt``` file in ```confidentialIinfo``` folder by enlisting all the confidential tasks in the following format
```json
{
  "confidentialTasks":
    [
      {
        "taskName" : "A"
      },
      {
        "taskName" : "B"
      }
    ]
}
```
11- Encrypt the workflow input dataset (for confidential tasks) with the provided ```CryptoTools``` application.
 Update ```secretkey``` and ```associatedData ``` values in the ```TaskExecutor.java``` with your cryptography setting. 
 ```java
 public static String secretKey = "abc";
 public static String associatedData = "abc";
 ```
12- Put the encrypted workflow input files in ```workflowDataDir```folder.

13- Update the IP addresses for SEV and SGX in ```IPPool.txt```, that is located in ```confidentialIinfo``` in the following format. No duplicated IP is allowed in this list.
```json
{
  "IPPool":
    [
      {
        "AMD" : "172.30.18.183"
      },
      {
        "AMD" : "172.30.18.184"
      },
      {
        "SGX" : "172.30.18.185"
      }
    ]
}
```
14- Create all the tasks in the following format. (or follow tutorial link for more information)

14.1- Define the size of the InputPort, OutPort, and type of dataset in the constructor of the class.
  
14.2- Overwrite your task in run() method. 
  
14.3- Put all the workflow tasks in the default package.

14. Export the runnable jar ```TaskExecutor.jar``` to ```confidentialInfo``` folder by selecting ```TaskExecutor.java``` file. This executable jar file should be created with Eclipse IDE.

15. Run the corresponding driver class to start the WorkflowExecutor.  

16- You also may export the runnable jar ```YourWorkflowDriver.jar``` into ```DATAVIEW``` folder by selecting ```<YourWorkflowDriver>.java``` file. This executable jar file should be created with Eclipse IDE and should include all the neccessary libraries in the jar. In this way you can use your jar file to execute the workflow with the below command.

```bash
java -jar <YourWorkflowDriver>.jar
```
When using the runaable jar file to execute the workflow tree structure bellow must be available for the jar file otherwise it cannot find neccessary files.

```console 
> machineScript
> DATAVIEW
>         ├── <YourWorkflow>.jar
>         │   ├── confidentialInfo
>         │   ├── workflowDataDir
>         │   └── workflowLibDir
```

Setting up SecDATAVIEW master node
----------------------------------
1- Update the values of the variables below in the ```WorkflowExecutor_Alfa.java``` based on the following information

Update the value of ```SGX_IMG_SRC_FILE```  with the path for sgx-lkl disk image  
```java
public static final String SGX_IMG_SRC_FILE = "/home/mofrad-s/SecDATAVIEW-v3-img.enc";
```

Update the value for ```SGX_SCRIPT_SRC_FILE``` relative to your project path
```java
public static final String SGX_SCRIPT_SRC_FILE = "/home/mofrad-s/SecDATAVIEW/machineScript/SGX/sgx-lkl-server-remote-launch.sh";
```

Update the value of ```AMD_IMG_SRC_FILE```  with the path for AMD SEV disk image 
```java
public static final String AMD_IMG_SRC_FILE = "/home/mofrad-s/sev-image.qcow2";
```

Update the value of ```AMD_IMG_DST_FOLDER```  with the path for home folder of your account on the AMD host server  
```java
public static final String AMD_IMG_DST_FOLDER = "/home/mofrad-s/";
```

Update the value for ```AMD_SCRIPT_SRC_FILE``` relative to your project path
```java
public static final String AMD_SCRIPT_SRC_FILE = "/home/mofrad-s/SecDATAVIEW/machineScript/AMD/vm1-launch-dataview-sev.sh";
```

Update the value of ```AMD_SCRIPT_DST_FOLDER```  with the path for the home folder of your account on the AMD host server  

```java
public static final String AMD_SCRIPT_DST_FOLDER = "/home/mofrad-s/";
```

Update the value of ```AMD_SERVER_IP```  with the IP address of your AMD host server  

```java 
public static final String AMD_SERVER_IP = "172.30.18.202";
```
keep the value of ```AMD_SERVER_USER_NAME```  as root. SecDATAVIEW requires root access to the machine to launch SEV VMs on the AMD server

```java
public static final String AMD_SERVER_USER_NAME = "root";
```
Update the value of ```AMD_SERVER_PASSWORD```  for 'root' account in the AMD server   

```java
public static final String AMD_SERVER_PASSWORD = "acsac19";
```
Update the value of ```SGX_SERVER_USER_NAME```  with the user name of your SGX host server  
```java
public static final String SGX_SERVER_USER_NAME = "ubuntu";
```
Update the value of ```SGX_SERVER_PASSWORD```  with the password of your SGX host server  
```java
public static final String SGX_SERVER_PASSWORD = "dataview";
```
Update the value of ```SGX_MRENCLVE```  with the expected value 
```java
public static final String SGX_MRENCLVE = "9b6cb5e8e67c7ae5bfce1ec9d60363e259261c041f9a46cf284ac1fe714405c1"; 
```
Update the value of ```SGX_MRSIGNER```  with the expected value 
```java
public static final String SGX_MRSIGNER = "f3b24a591ba692e90bd8a02ca4241a2a73b2128e90041d048fc84cf5fba5d7f6";
 ```
You may learn expected SGX_MRENCLVE and SGX_MRSIGNER values by manually running sgx-lkl-server-remote-launch.sh on your SGX hardware similar to below image.

![](machineScript/SGX/mrenclave-signervalues.png)

Update the value of ```IAS_SPID ```  with the expected value 
```java
public static final String IAS_SPID = "use your Intel SPID here"; //use the same values in the SGX machine script file
 ```
Update the value of ```IAS_SKEY  ```  with the expected value 
```java
public static final String IAS_SKEY = "use one of your Intel Primary or Secondary key here"; 
 ```
Update the path of ```bashCommand``` and ```env``` strings for ```executeSGXremoteControl``` and  ```executeSGXremoteAttestation``` methods in the VMProvisionerSGX class and with the path to SGX-LKL folder on the master. Also, update the path to ```secdataview.app.conf``` file in the ```executeSGXremoteControl``` method. The file is located in ```/SecDATAVIEW/machineScript/SGX/secdataview.app.conf``` in this repository. 
 
Update all worker's script files path values in the ```/machineScript/AMD/vm1-launch-dataview-sev.sh```  and  ```/machineScript/SGX/sgx-lkl-server-remote-launch.sh```  based on your SGX and AMD folders and user credentials settings. For SGX script update the SPID value in the script similar to your IAS_SPID

CryptoTools instructions
------------------------
Put all the workflow input files in ```source_folder_location``` and then run the ```CryptoTool``` by the following format
```
java -jar CryptoTool.jar "enc" "associated_data" "secret_key" "source_folder_location" "destination_folder_location"
```
The parementer order are as follows:``` "mode" "secret_Key" "associated_data" "source_folder_location" "destination_folder_location"```. The "mode" is consisted with two types: a) "enc" b) "dec"


Sample Workflows
----------------

1- Running Diagnosis Recommendation Workflow: (Example of the hybrid workflow; SGX and SEV workers)
---------------------------------------------------------------------------------------------------
Import the SecDATAVIEW project into Eclipse IDE and execute the ```DriverDiagnosisNew.java``` file as a driver class for Diagnosis Recommendation Workflow. This driver class is invoked with an SGX and a SEV machines. All necessary inputs and files have been provided in this repository. Since Diagnosis Recommendation Workflow involves six tasks, the first four tasks are assigned to SGX machines, and the rest of the tasks are allocated to a SEV machine. The output file for this workflow will be assigned to the machine that is associated with the last tasks "Evaluation."


2- Running Diagnosis Remommendation Workflow: (Example of SGX only workflows)
-----------------------------------------------------------------------------
1- Import the SecDATAVIEW project into Eclipse IDE and execute the ```DriverDiagnosisSGXonly.java``` file as the driver class for running the workflow with only one SGX worker. All the tasks associated with this workflow is configured to assign to only one SGX machine. Use ```DriverDiagnosis.java``` file as the driver class to execute the workflow with five SGX workers; all the tasks associated with this workflow is configured to assign between five SGX machines. 

3- Journal Exprimental Source Codes, the Scenarios and Parameter Setup:
---------------------------------
Source code for experimental Workflows and their data generator that have been used to produce results in the Journal version of the SecDATAVIEW system is available in the "JournalReproduce-SourceCode" folder.

Journal paper section 4.1.1. The Diagnosis Recommendation workflow:
The raw textual dataset can be generated through the dataset generator programm. For each of the scenarios we used 75% random dataset for training and the rest of them are for testing. The corresponding parameters for various training and testing data size are enlisted in Figure 5 (a) legends in the paper.  

Journal paper section 4.1.2. The Word Count (Map-Reduce) workflow:
The dataset for various size can be generated through the source code that we provided. In this experiment the experiment conductor needs to select the word length and the total number of words in the provided code and then execute the workflow. The corresponding parameters for various size are enlisted in Figure 5 (b) legends in the paper. 

Journal paper section 4.1.3. The Distributed K-means workflow:
In order to conduct the experiment, we generated various points (2-dimensional) dataset through the source code. To initiate the execution, we need to put on various points. When we obtain the corresponding dataset, we can feed this to our publicly available distributed k-means workflow. The corresponding parameters are enlisted in Figure 5 (c) legends in the paper. 

Journal paper section 4.1.4. The MONTAGE workflow:
In order to conduct the experiment, we randomly generate integers and the input should be the total number of integers that we need to generate. The corresponding parameters are enlisted in Figure 6 (a) legends in the paper. 

Journal paper section 4.1.5. The Neural Network (NN) workflow:
To replicate the experiment result, the source code for NN network workflow has been given. In order to vary the result, we need to specify the total number of nodes and the rest of the steps are fully automated through the workflow source code. The corresponding parameters are enlisted in Figure 6 (b) legends in the paper. 



