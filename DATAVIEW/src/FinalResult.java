import java.util.*;

import dataview.models.Dataview;
import dataview.models.InputPort;
import dataview.models.OutputPort;
import dataview.models.Task;

public class FinalResult extends Task{

	public FinalResult() {
		super("Shuffling", "This is a FinalResult Task");
		ins = new InputPort[4];
		outs = new OutputPort[1];
		ins[0] = new InputPort("in0", Dataview.datatype.DATAVIEW_String, "This is the first number");
		ins[1] = new InputPort("in1", Dataview.datatype.DATAVIEW_String, "This is the first number");
		ins[2] = new InputPort("in2", Dataview.datatype.DATAVIEW_String, "This is the first number");
		ins[3] = new InputPort("in3", Dataview.datatype.DATAVIEW_String, "This is the first number");
		
		
		outs[0] = new OutputPort("out0", Dataview.datatype.DATAVIEW_String, "This is the output");
		
	}

	@Override
	public void run() {
		// step 1: read from the input ports
		String result = "";
		String input0 = (String) ins[0].read1();
//		Dataview.debugger.logSuccessfulMessage("here is the input0" + input0);

		String input1 = (String) ins[1].read1();
//		Dataview.debugger.logSuccessfulMessage("here is the input0" + input1);
		
		String input2 = (String) ins[2].read1();
//		Dataview.debugger.logSuccessfulMessage("here is the input0" + input2);

		String input3 = (String) ins[3].read1();
//		Dataview.debugger.logSuccessfulMessage("here is the input0" + input3);

		ArrayList<String> list = new ArrayList<>();
		
	
		if (input0 != null && !input0.isEmpty()) {
			if (input0.charAt(input0.length() - 1) == '\n') {
				input0 = input0.substring(0, input0.length() - 1);
			}
			
			list.add(input0);
		}
		if (input1 != null && !input1.isEmpty()) {
			if (input1.charAt(input1.length() - 1) == '\n') {
				input1 = input1.substring(0, input1.length() - 1);
			}
			list.add(input1);
		}
		if (input2 != null && !input2.isEmpty()) {
			if (input2.charAt(input2.length() - 1) == '\n') {
				input2 = input2.substring(0, input2.length() - 1);
			}
			list.add(input2);
		}
		if (input3 != null && !input3.isEmpty()) {
			if (input3.charAt(input3.length() - 1) == '\n') {
				input3 = input3.substring(0, input3.length() - 1);
			}
			list.add(input3);
		}
		
		Collections.sort(list);
		int len = list.size();
		
		for (int i = 0; i < len; i++) {
			result += list.get(i);
			if (i < len - 1) result += "\n";
		}
		
		outs[0].write1(result);

	}

}
