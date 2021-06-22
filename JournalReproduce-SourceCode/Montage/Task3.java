import dataview.models.*;

public class Task3 extends Task{

	public Task3() {
		super("Task3", "This is a task that implements AND operation. It has two inputports and one outputport.");
		ins = new InputPort[1];
		outs = new OutputPort[1];
		ins[0] = new InputPort("in0", Dataview.datatype.DATAVIEW_Boolean, "This is the first input boolean value");
		outs[0] = new OutputPort("out0", Dataview.datatype.DATAVIEW_Boolean, "This is the first output boolean value");
	}
	

	@Override
	public void run() {
		
		Boolean input0 = (Boolean) ins[0].read1();
		
		Boolean output0 = null;
		if (input0 != null) {
			output0 = !input0;
			
		} else {
			Dataview.debugger.logErrorMessage("Found null at " + taskName);
			System.exit(0);
		}
		outs[0].write1(output0);
	}

}
