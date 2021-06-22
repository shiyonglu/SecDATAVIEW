import dataview.models.*;

public class Task2 extends Task{

	public Task2() {
		super("Task2", "This is a task that implements AND operation. It has two inputports and one outputport.");
		ins = new InputPort[1];
		outs = new OutputPort[2];
		ins[0] = new InputPort("in0", Dataview.datatype.DATAVIEW_Boolean, "This is the first input boolean value");
		outs[0] = new OutputPort("out0", Dataview.datatype.DATAVIEW_Boolean, "This is the first output boolean value");
		outs[1] = new OutputPort("out1", Dataview.datatype.DATAVIEW_Boolean, "This is the first output boolean value");
	}
	

	@Override
	public void run() {
		
		Boolean input0 = (Boolean) ins[0].read1();
		
		Boolean output0 = null;
		Boolean output1 = null;
		if (input0 != null) {
			output0 = output1 = !input0;	
		} else {
			Dataview.debugger.logErrorMessage("Found null at " + taskName);
			System.exit(0);
		}
		outs[0].write1(output0);
		outs[1].write1(output1);
	}


}
