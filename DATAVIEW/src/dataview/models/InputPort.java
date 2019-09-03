package dataview.models;
import java.io.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.Vector;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import dataview.workflowexecutor.TaskExecutor;

public class InputPort extends Port{

	public InputPort(String portname, int porttype, String description)
	{
		super(portname, porttype, description);
	}




	/*
	 * read() will read the data from this input port, based on the type of this inputport, it will return an object
	 * of the corresponding type.
	 */

	
	/*
	 * read() will read the data from this input port, based on the type of this inputport, it will return an object
	 * of the corresponding type.
	 */

	public Object read1() {


		System.out.println("Filename:" + this.getFileName());
		DATAVIEW_BigFile f = null;  // this is the input file object.
		
		if (this.getFileName().contains(".enc")) {
			DWCRYPTO dwcrypto = new DWCRYPTO();
			String plainTextName = this.getFileName().replace(".enc", ".txt");
			try {
				if (dwcrypto.decryptFile(this.getFileName(), plainTextName, TaskExecutor.associatedData, TaskExecutor.secretKey) == 0) {
					Dataview.debugger.logSuccessfulMessage("The file is decrypted successfully");
					f = new DATAVIEW_BigFile(plainTextName); // input file object is changed to plain text by decryption and instantiated. 
				} else {
					Dataview.debugger.logErrorMessage("Decyption is broken");
					return null;
				}
			} catch (Exception e) {
				Dataview.debugger.logException(e);
			}	
		} else { // This is where task is not confidential
			f = new DATAVIEW_BigFile(this.getFileName());
		}

		if(porttype ==  Dataview.datatype.DATAVIEW_int)
			return f.getInteger();

		if(porttype ==  Dataview.datatype.DATAVIEW_double)
			return  f.getDouble();

		if(porttype == Dataview.datatype.DATAVIEW_String)
			return f.getString();	     

		if(porttype == Dataview.datatype.DATAVIEW_HashMap)
			return f.getHashMap();						

		if(porttype == Dataview.datatype.DATAVIEW_MathVector)
			return f.getMathVector();

		if(porttype == Dataview.datatype.DATAVIEW_Table)
			return f.getTable();			    							    	 

		if(porttype == Dataview.datatype.DATAVIEW_BigFile)
			return f;

		if(porttype == Dataview.datatype.DATAVIEW_Boolean)
			return f.getBoolean();



		Dataview.debugger.logErrorMessage("Inputport type: " + porttype + " is not yet supported in this DATAVIEW release.");
		return null;
	}
	

	/*
	 * read() will read the data from this input port, based on the type of this inputport, it will return an object
	 * of the corresponding type.
	 */

	public Object read() {


		System.out.println("Filename:" + this.getFileName());
		DATAVIEW_BigFile f = new DATAVIEW_BigFile(this.getFileName());


		if(porttype ==  Dataview.datatype.DATAVIEW_int)
			return f.getInteger();

		if(porttype ==  Dataview.datatype.DATAVIEW_double)
			return  f.getDouble();

		if(porttype == Dataview.datatype.DATAVIEW_String)
			return f.getString();	     

		if(porttype == Dataview.datatype.DATAVIEW_HashMap)
			return f.getHashMap();						

		if(porttype == Dataview.datatype.DATAVIEW_MathVector)
			return f.getMathVector();

		if(porttype == Dataview.datatype.DATAVIEW_Table)
			return f.getTable();			    							    	 

		if(porttype == Dataview.datatype.DATAVIEW_BigFile)
			return f;

		if(porttype == Dataview.datatype.DATAVIEW_Boolean)
			return f.getBoolean();



		Dataview.debugger.logErrorMessage("Inputport type: " + porttype + " is not yet supported in this DATAVIEW release.");
		return null;
	}
}
