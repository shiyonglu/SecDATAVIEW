import java.io.*;
import dataview.models.*;


/* FileSplitter will split a file into K files that are (almost) equal in size */
/* task is called a parameterized task, the parameter K must be instantiated to a concrete value by a constructor. */
public class DisFileSplitter extends Task{
	private int M = 3; 

	public DisFileSplitter ()
	{
		super("DisFileSplitter", "FileSplitter will split a file into M files that are (almost) equal in size ");
		ins = new InputPort[1];
		ins[0] = new InputPort("in0", Dataview.datatype.DATAVIEW_String, "This is the first number");
		outs = new OutputPort[M * 2];
		for(int i=0; i< M * 2; i++) {
			outs[i] = new OutputPort("out"+i, Dataview.datatype.DATAVIEW_String, "This is the "+i+"th output");
		}
	}

	@Override
	public void run() {
		String input0 = (String) ins[0].read1();
		Dataview.debugger.logSuccessfulMessage("here is the input for DisFileSplitter" + input0);

		String lines[] = input0.split("\\r?\\n");

		int equalPortionSize = lines.length / M;

		String output = "";
		String[] outputs = new String[M];
		int index = 0;
		for (int i = 1; i <= lines.length; i++) {
			output += lines[i];
			if (i != lines.length) output += "\n";
			if (i == equalPortionSize || i == lines.length) {
				outputs[index++] = output;
				output = "";
			}
		}


		// wiriting
		index = 0;
		for (int i = 0; i < outputs.length; i++) {
			outs[index++].write1(outputs[i]);
			outs[index++].write1(outputs[i]);
		}

	}
}
