import dataview.models.*;

public class Task9 extends Task{

	public Task9() {
		super("Task9", "This is a task that implements (Input1 XOR Input2) AND Input3 operation. It has three inputports and one outputport.");
		ins = new InputPort[1];
		outs = new OutputPort[1];
		ins[0] = new InputPort("in0", Dataview.datatype.DATAVIEW_Boolean, "This is the first input boolean value");
		outs[0] = new OutputPort("out0", Dataview.datatype.DATAVIEW_Boolean, "This is the first output boolean value");
	}


	@Override
	public void run() {
		int returnStatus = -1;
		DWJNI sgx = new DWJNI();
		returnStatus = sgx.jniAlgorithmOne("labeled.txt", "labeled.txt","output.txt", "Alg1c");
		if (returnStatus != 0) {
			Dataview.debugger.logErrorMessage("Program terminated abnormally");
		}
		
		
		Boolean oo = new Boolean(true);
		outs[0].write(oo);
	}


}
