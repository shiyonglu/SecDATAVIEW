import java.util.ArrayList;

import dataview.models.Dataview;
import dataview.models.InputPort;
import dataview.models.OutputPort;
import dataview.models.Task;

public class Input extends Task{

	public Input() {
		super("Input", "This is a input Task");
		ins = new InputPort[1];
		outs = new OutputPort[3];
		ins[0] = new InputPort("in0", Dataview.datatype.DATAVIEW_String, "This is the first number");
		//ins[1] = new InputPort("2in", "Integer", "This is the second number");
		outs[0] = new OutputPort("out0", Dataview.datatype.DATAVIEW_String, "This is the output");		
		outs[1] = new OutputPort("out1", Dataview.datatype.DATAVIEW_String, "This is the output");
		outs[2] = new OutputPort("out2", Dataview.datatype.DATAVIEW_String, "This is the output");
	}

	@Override
	public void run() {
		// step 1: read from the input ports
		String input0 = (String) ins[0].read1();
//		Dataview.debugger.logSuccessfulMessage("here is the input0" + input0);

		// step 2: computation of the function

		StringBuilder[] stringBuilders = new StringBuilder[3];
		for (int i = 0; i < stringBuilders.length; i++) {
			stringBuilders[i] = new StringBuilder();
		}
		
		
		String lines[] = input0.split("\\r?\\n");
		int numofline = lines.length;
		
		boolean isFirst0 = true;
		boolean isFirst1 = true;
		boolean isFirst2 = true;
		
		for (int i = 0; i < numofline; i++) {
			if (i < numofline / 3) {
				if (!isFirst0) stringBuilders[0].append("\n");
				stringBuilders[0].append(lines[i]);
				isFirst0 = false;
			} else if (i < numofline * 2 / 3) {
				if (!isFirst1) stringBuilders[1].append("\n");
				stringBuilders[1].append(lines[i]);
				isFirst1 = false;
			} else {
				if (!isFirst2) stringBuilders[2].append("\n");
				stringBuilders[2].append(lines[i]);
				isFirst2 = false;
			}
		}
		
		
		outs[0].write1(stringBuilders[0].toString());
		outs[1].write1(stringBuilders[1].toString());
		outs[2].write1(stringBuilders[2].toString());

	}

}
