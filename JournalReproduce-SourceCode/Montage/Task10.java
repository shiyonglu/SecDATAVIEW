import dataview.models.*;

public class Task10 extends Task{

	public Task10() {
		super("Task10", "This is a task that implements (Input1 XOR Input2) AND Input3 operation. It has three inputports and one outputport.");
		ins = new InputPort[2];
		outs = new OutputPort[1];
		ins[0] = new InputPort("in0", Dataview.datatype.DATAVIEW_Boolean, "This is the first input boolean value");
		ins[1] = new InputPort("in1", Dataview.datatype.DATAVIEW_Boolean, "This is the second input boolean value");
		outs[0] = new OutputPort("out0", Dataview.datatype.DATAVIEW_Boolean, "This is the first output boolean value");		
	}


	@Override
	public void run() {
		DWCRYPTOJNI dwcrypto = new DWCRYPTOJNI();
		int ret = -1;
		Dataview.debugger.logSuccessfulMessage("In line 24");
		ret=dwcrypto.jniDecrypt("output.txt.dat","output.txt.size","output.txt.tag", "PassWord123", "Authenticate with it");
		Dataview.debugger.logSuccessfulMessage("In line 26");
		if (ret != 0) {
			Dataview.debugger.logErrorMessage("Program terminated abnormally");
		}
		SortingAlgorithm.run();

		Boolean oo = new Boolean(true);
		outs[0].write(oo);
	}


}
