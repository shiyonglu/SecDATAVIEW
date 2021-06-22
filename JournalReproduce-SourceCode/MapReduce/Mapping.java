import java.util.*;

import dataview.models.Dataview;
import dataview.models.InputPort;
import dataview.models.OutputPort;
import dataview.models.Task;

public class Mapping extends Task{

	public Mapping() {
		super("Mapping", "This is a mapping Task");
		ins = new InputPort[1];
		outs = new OutputPort[4];
		ins[0] = new InputPort("in0", Dataview.datatype.DATAVIEW_String, "This is the first number");
		outs[0] = new OutputPort("out0", Dataview.datatype.DATAVIEW_String, "This is the output");
		outs[1] = new OutputPort("out0", Dataview.datatype.DATAVIEW_String, "This is the output");
		outs[2] = new OutputPort("out0", Dataview.datatype.DATAVIEW_String, "This is the output");
		outs[3] = new OutputPort("out0", Dataview.datatype.DATAVIEW_String, "This is the output");
	}

	@Override
	public void run() {
		// step 1: read from the input ports
		String input0 = (String) ins[0].read1();
		Dataview.debugger.logSuccessfulMessage("here is the input0" + input0);

		StringBuilder mapping0 = new StringBuilder();
		StringBuilder mapping1 = new StringBuilder();
		StringBuilder mapping2 = new StringBuilder();
		StringBuilder mapping3 = new StringBuilder();
		
		String lines[] = input0.split("\\r?\\n");
		int numofline = lines.length;
		
		for (int i = 0; i < numofline; i++) {
			char firstChar = lines[i].charAt(0);
			int charToInt = Character.isUpperCase(firstChar) ? firstChar - 'A' : firstChar - 'a';
			if (charToInt < 26 / 4) {
				mapping0.append(lines[i] + "\n");
			} else if (charToInt < 26 * 2 / 4) {
				mapping1.append(lines[i] + "\n");
			} else if (charToInt < 26 * 3 / 4) {
				mapping2.append(lines[i] + "\n");
			} else {
				mapping3.append(lines[i] + "\n");
			}
		}
		
		// eliminating last newLine character
		int len0 = mapping0.length();
		int len1 = mapping1.length();
		int len2 = mapping2.length();
		int len3 = mapping3.length();
		
		if (len0 > 0 && mapping0.charAt(len0 - 1) == '\n') mapping0.deleteCharAt(len0 - 1);
		if (len1 > 0 && mapping1.charAt(len1 - 1) == '\n') mapping1.deleteCharAt(len1 - 1);
		if (len2 > 0 && mapping2.charAt(len2 - 1) == '\n') mapping2.deleteCharAt(len2 - 1);
		if (len3 > 0 && mapping3.charAt(len3 - 1) == '\n') mapping3.deleteCharAt(len3 - 1);
		
		outs[0].write1(mapping0.toString());
		outs[1].write1(mapping1.toString());
		outs[2].write1(mapping2.toString());
		outs[3].write1(mapping3.toString());
	}

}
