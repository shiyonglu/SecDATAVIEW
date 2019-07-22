package dataview.workflowexecutors;

import java.util.*;

public abstract class VMProvisioner {
	
	abstract List<String> getAvailableIPs(int totalMachine);
	abstract void executeCommands(String strHostName, String strUserName,String password, List<String> commands);
	abstract void copyFileVM(String SourceDIR, String DestinationDIR, String strHostName);
	abstract List<String> getBashCommands();
	abstract void sendDiskImage(String SourceDIR, String DestinationDIR, String strHostName);
}
