import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dataview.models.*;

public class Task1 extends Task{
	/*
	 * The constructor will decide how many inputports and how many outputports and the detailed information of each port.
	 */
	ArrayList<ArrayList<String>> trueDiagnosisList = new ArrayList<ArrayList<String>>();
	ArrayList<ArrayList<String>> recommendedDiagnosisList = new ArrayList<ArrayList<String>>();
	public Task1()
	{
		super("Task1", "AND Operation");
		ins = new InputPort[2];
		outs = new OutputPort[1];
		ins[0] = new InputPort("in0", Dataview.datatype.DATAVIEW_Boolean, "This is the first input");
		ins[1] = new InputPort("in1", Dataview.datatype.DATAVIEW_Boolean, "This is the second input");
		outs[0] = new OutputPort("out0", Dataview.datatype.DATAVIEW_Boolean, "This is the output");	
		
	}
	
	public void run() 
	{
		
		
		// step 1: read from the input ports
		boolean input0 = (boolean) ins[0].read1();
		boolean input1 = (boolean) ins[1].read1();
		
		System.out.println("The Task1(AND) input0: " + input0 + " input1: " +  input1);
		boolean result = input0 & input1;
		
		System.out.println("The Task1(AND) operation result is : " + result);
		outs[0].write1(result);	
		
	}	
}
