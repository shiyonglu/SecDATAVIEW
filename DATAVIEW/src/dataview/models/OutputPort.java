package dataview.models;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import dataview.workflowexecutor.TaskExecutor;

public class OutputPort extends Port{

	public OutputPort(String portname, int porttype, String description)
	{
		super(portname, porttype, description);
	}

	public void write1(Object o)
	{
		BufferedWriter writer;

		if (location.contains(".enc")) {
			DWCRYPTO dwcrypto = new DWCRYPTO();
			try {
				String plainTextName = this.getFileName().replace(".enc", ".txt");
				try {
					writer = new BufferedWriter(new FileWriter(plainTextName));
					writer.write(o.toString());	     			
					writer.close();			
				} catch (Exception e) {
					Dataview.debugger.logException(e);
				}
				String cypherTextName = location;
				if (dwcrypto.encryptFile(cypherTextName, "abc", plainTextName, "abc") == 0) {
					Dataview.debugger.logSuccessfulMessage("Successful encryption");
				} else {
					Dataview.debugger.logErrorMessage("encryption is broken");
				}
			} catch (Exception e) {
				Dataview.debugger.logException(e);
			}
		} else {
			// write the string representation of the object to the output file
			try {
				writer = new BufferedWriter(new FileWriter(location));
				writer.write(o.toString());	     			
				writer.close();			
			} catch (IOException e) {
				Dataview.debugger.logException(e);
			}
		}
	}

	public void write(Object o)
	{
		BufferedWriter writer;

		// write the string representation of the object to the output file
		try {
			writer = new BufferedWriter(new FileWriter(location));
			writer.write(o.toString());	     			
			writer.close();			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	} // end of write()
} // end of class
