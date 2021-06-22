import dataview.models.*;

public class Task5 extends Task{
	
	public Task5() {
		super("Task5", "This is a task that implements AND operation. It has two inputports and one outputport.");
		ins = new InputPort[2];
		outs = new OutputPort[1];
		ins[0] = new InputPort("in0", Dataview.datatype.DATAVIEW_Boolean, "This is the first input boolean value");
		ins[1] = new InputPort("in1", Dataview.datatype.DATAVIEW_Boolean, "This is the first input boolean value");
		outs[0] = new OutputPort("out0", Dataview.datatype.DATAVIEW_Boolean, "This is the first output boolean value");
	}
	

	@Override
	public void run() {
		
		Boolean input0 = (Boolean) ins[0].read1();
		Boolean input1 = (Boolean) ins[1].read1();
		
		Boolean output0 = null;
		if (input0 != null && input1 != null) {
			output0 = input0 ^ input1;
			
		} else {
			Dataview.debugger.logErrorMessage("Found null at " + taskName);
			System.exit(0);
		}
		outs[0].write1(output0);
	}

}
