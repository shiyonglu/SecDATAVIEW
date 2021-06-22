import dataview.models.*;

public class Task7 extends Task{

	public Task7() {
		super("Task7", "This is a task that implements (Input1 XOR Input2) AND Input3 operation. It has three inputports and one outputport.");
		ins = new InputPort[3];
		outs = new OutputPort[1];
		ins[0] = new InputPort("in0", Dataview.datatype.DATAVIEW_Boolean, "This is the first input boolean value");
		ins[1] = new InputPort("in1", Dataview.datatype.DATAVIEW_Boolean, "This is the second input boolean value");
		ins[2] = new InputPort("in2", Dataview.datatype.DATAVIEW_Boolean, "This is the third input boolean value");
		outs[0] = new OutputPort("out0", Dataview.datatype.DATAVIEW_Boolean, "This is the first output boolean value");
	}


	@Override
	public void run() {
		Boolean input0 = (Boolean) ins[0].read1();
		Boolean input1 = (Boolean) ins[1].read1();
		Boolean input2 = (Boolean) ins[2].read1();
		
		Boolean output0 = null;
		if (input0 != null && input1 != null && input2 != null) {
			output0 = (input0 ^ input1) & input2;

		} else {
			Dataview.debugger.logErrorMessage("Found null at " + taskName);
			System.exit(0);
		}
		outs[0].write1(output0);
	}

}
