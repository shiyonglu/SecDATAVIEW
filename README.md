SecDATAVIEW: A Secure Big Data Workflow Management System for Heterogeneous Computing Environments
==================================================================================================
SecDATAVIEW is a secure big data workflow management system compatible with the heterogeneous computing environment. It leverages hardware-assisted TEEs such as Intel SGX and AMD SEV to protect the execution of workflows in the untrusted cloud. 
The SecDATAVIEW paper is appeared in proceedings of The 35th Annual Computer Security Applications Conference 
(ACSAC'19), San Juan, Puerto Rico, December, 2019. 

Prerequisites
-------------
SecDATAVIEW has been tested on Ubuntu 16.04 LTS for SGX worker nodes and 18.04 LTS for SEV worker node. 

Setting up the SGX worker node
------------------------------
On every SGX worker node, please install Ubuntu 16.04 LTS then follow the instruction below.

1- Install the Intel SGX driver https://github.com/01org/linux-sgx-driver is required. 
We have tested SecDATAVIEW with driver versions 2.0.

2- Install the SGX-LKL Library OS from https://github.com/lsds/sgx-lkl build it in hardware mode and sign the enclave following its instruction; make sure you can run the sample java application provided with SGX-LKL library OS.

3-Instal OpenSSH-SERVER and make sure you can make an ssh connection to your SGX machine.
```sudo apt get install openssh-server
ssh <your user name>@<Your SGX server ip> 
```
4- Download the pre-created sgx-lkl disk image from the link below and save the disk image in the SecDATAVIEW Master node.
 https://www.dropbox.com/sh/hywa7du0sr70nec/AABAyHqD_4tPYXVUNdw2bQAEa?dl=0



Networking support for SGX worker node
---------------------------------------

In order for SecDATAVIEW SGX-LKL application to send and receive packets via the network, a TAP interface is needed on the host. Besides, all the network traffic towards the worker node on TCP port 8000 and TCP port 7700 should be forwarded to the TAP interface. Create and configure the TAP interface as follows:
```
#Lets create a TAP interface
sudo ip tuntap del dev sgxlkl_tap0 mode tap
sudo ip tuntap add dev sgxlkl_tap0 mode tap user `whoami`
sudo ip link set dev sgxlkl_tap0 up
sudo ip addr add dev sgxlkl_tap0 10.0.1.254/24
```
To forward the network packets, the OS iptables should be updated with the following commands: 
```
#Lets backup the OS iptables first. Keep this file to restore the iptables rules at a later time

sudo iptables-save > ~/iptables-bak

#Rules to update the PREROUTING table for NAT

sudo iptables -t nat -A PREROUTING ! -s 10.0.1.0/24 -p tcp -m tcp --dport 8000 -j DNAT --to-destination 10.0.1.1:8000
sudo iptables -t nat -A PREROUTING ! -s 10.0.1.0/24 -p tcp -m tcp --dport 7700 -j DNAT --to-destination 10.0.1.1:7700

#Rule to update the POSTROUTING table that provides internet access for worker node

sudo iptables -t nat -A POSTROUTING -s 10.0.1.0/24 ! -d 10.0.1.0/24 -j MASQUERADE

#Rules to update the FORWARD table for packet forwarding

sudo iptables -I FORWARD -m state -d 10.0.1.0/24 --state NEW,RELATED,ESTABLISHED -j ACCEPT
sudo iptables -I FORWARD -m state -s 10.0.1.0/24 --state NEW,RELATED,ESTABLISHED -j ACCEPT

#You must enable the packet forwarding in the system module unless packet routing doesn't work.
sudo sysctl -w net.ipv4.ip_forward=1
```
Setting up the SEV worker node
------------------------------
SEV worker node requires an AMD server that supports the SEV feature. SEV feature must be enabled in the BIOS of the server, and instructions provided below should be followed.

1- Install and prepare the SEV HOST for the UBUNTU 18 OS by following the https://github.com/AMDESE/AMDSEV

2- On the host machine, install OpenSSH-SERVER, then set the 'root' password.
```
sudo apt get install openssh-server
sudo su
passwd "set the password for 'root'"
```
3-give the 'root' account the SSH access by following the instruction below

https://linuxconfig.org/allow-ssh-root-login-on-ubuntu-18-04-bionic-beaver-linux 

4- make sure you can get ssh access to the AMD server with 'root' account.
```
ssh root@"Your AMD server ip" 
```
5- Download the pre-created SEV disk image <ubuntu.18.0.4.qcow2> from the link below and save the disk image in your SecDATAVIEW Master node.

 https://www.dropbox.com/sh/hywa7du0sr70nec/AABAyHqD_4tPYXVUNdw2bQAEa?dl=0


Networking support for SEV worker node
--------------------------------------

In order SecDATAVIEW SEV VM to send and receive packets via the network, a TAP interface is needed on the host AMD server for every single SEV VM. The TAP interface should be bridged to the ethernet interface with internet access so that every SEV VM receives its IP addresses from the LAN DHCP server. Create and configure each TAP interface as follows:
```
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

#Follow the article below for more information regarding network configuration for KVM and QEMU
https://gist.github.com/extremecoders-re/e8fd8a67a515fee0c873dcafc81d811c

Setting up SecDATAVIEW master node
-----------------------------------

We have tested SecDATAVIEW master on Ubuntu 16 LTS
Please follow the instruction below to setup the master node.

1- Install OpenJDK 1.8
```
sudo apt install openjdk-8-jdk
```

2- Install Eclipse ide for JAVA (Oxygen.3a Release (4.7.3a) from official Eclipse website.

3- Install git on your master node. 
```
sudo apt-install git
```

4- Clone the SecDATAVIEW project from git on your master node. 
```
git clone https://github.com/Saeid2k/secureDW.git
```

5- Create a new workspace on your Eclipse IDE

6- Import the SecDATAVIEW project into your workspace.

7- Update your workflow

8- Update the ```config.txt``` file in ```confidentialIinfo``` folder enlisting all the confidential tasks in the following format
```
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
9- Encrypt the workflow input dataset (for confidential tasks) with the provided ```CryptoTools``` application.

10- Put the encrypted workflow input files in ```workflowDataDir```folder.

11- Update the IP addresses for SEV and SGX in ```IPPool.txt```, that is located in ```workflowLibDir``` in the following format
```
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
12- Create all the tasks in the following format.

  12.1- Define the size of the InputPort, OutPort, and type of dataset in the constructor of the class.
  
  12.2- Overwrite your task in run() method. 
  
13. Export the executable jar ```TaskExecutor.jar``` to ```confidentialInfo``` folder by selecting ```TaskExecutor.java``` file. This executable jar file should be created with Eclipse IDE.

14. Run the corresponding driver class to start the WorkflowExecutor.    



Setting up SecDATAVIEW master node
----------------------------------
1- Update the values of the variables below in the ```WorkflowExecutor_Alfa.java``` based on the following information

Update the value of ```SGX_IMG_SRC_FILE```  with the path for sgx-lkl disk image  
```
public static final String SGX_IMG_SRC_FILE = "/home/ishtiaq/sgxlkl-disk.img.enc";
```

Update the value for ```SGX_SCRIPT_SRC_FILE``` relative to your project path
```public static final String SGX_SCRIPT_SRC_FILE = "/home/ishtiaq/Desktop/ishtiaq_git/secureDW/machineScript/SGX/sgx-lkl-java-encrypted-dataview.sh";```

Update the value of ```AMD_IMG_SRC_FILE```  with the path for AMD SEV disk image 
```public static final String AMD_IMG_SRC_FILE = "/home/ishtiaq/ubuntu-18.04.qcow2";```

Update the value of ```AMD_IMG_DST_FOLDER```  with the path for home folder of your account on the AMD host server  
```public static final String AMD_IMG_DST_FOLDER = "/home/mofrad-s/";```

Update the value for ```AMD_SCRIPT_SRC_FILE``` relative to your project path
```public static final String AMD_SCRIPT_SRC_FILE = "/home/ishtiaq/Desktop/ishtiaq_git/secureDW/machineScript/AMD/vm1-launch-dataview-sev.sh";```

Update the value of ```AMD_SCRIPT_DST_FOLDER```  with the path for home folder of your account on the AMD host server  

```public static final String AMD_SCRIPT_DST_FOLDER = "/home/mofrad-s/";
```


CryptoTools instructions
------------------------
Put all the input files in ```source_folder_location``` and then run the ```CryptoTool``` by the following format
```
java -jar CryptoTool.jar "enc" "associated_data" "secret_key" "source_folder_location" "destination_folder_location"
```
The parementer order are as follows:``` "mode" "secret_Key" "associated_data" "source_folder_location" "destination_folder_location"```. The "mode" is consisted with two types: a) "enc" b) "dec"


Sample Workflows
----------------

1- Running Diagnosis Recommendation Workflow: (Example of hybrid workflow; SGX and SEV workers)
-----------------------------------------------------------------------------------------------
1- Import the SecDATAVIEW project into Eclipse IDE and execute the ```DriverDiagnosisNew.java``` file as a driver class for Diagnosis Recommendation Workflow. This driver class is invoked with a SGX and a SEV machines. Since Diagnosis Recommendation Workflow involves with six tasks, the first four tasks are assigned to SGX machines and the rest of the tasks are allocated to an SEV machines. The output file for this workflow will be assigned to the machine that is associated with the last tasks "Evaluation".


2- Running Word Count Workflow (Map/Reduce) Workflow: (Example of SGX only workflow)
------------------------------------------------------------------------------------
1- Import the SecDATAVIEW project into Eclipse IDE and execute the ```DriverMapReduce.java``` file as a driver class for Word Count Workflow. All the tasks associated with this workflow is configured to assigned to only one SGX machine. 
