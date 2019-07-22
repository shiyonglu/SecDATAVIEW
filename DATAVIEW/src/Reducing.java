import java.util.*;

import dataview.models.Dataview;
import dataview.models.InputPort;
import dataview.models.OutputPort;
import dataview.models.Task;

public class Reducing extends Task{

	public Reducing() {
		super("Shuffling", "This is a shuffling Task");
		ins = new InputPort[1];
		outs = new OutputPort[1];
		ins[0] = new InputPort("in0", Dataview.datatype.DATAVIEW_String, "This is the first number");
		
		
		outs[0] = new OutputPort("out0", Dataview.datatype.DATAVIEW_String, "This is the output");
		
	}

	@Override
	public void run() {
		// step 1: read from the input ports
		String input0 = (String) ins[0].read1();
		Dataview.debugger.logSuccessfulMessage("here is the input0" + input0);
		if (input0 != null && input0.length() > 0) input0 = input0.substring(0, input0.length() - 1);
		outs[0].write1(input0);
		
		

	}

}
