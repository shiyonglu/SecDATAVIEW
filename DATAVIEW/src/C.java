
import dataview.models.Dataview;
import dataview.models.InputPort;
import dataview.models.OutputPort;
import dataview.models.Task;

public class C extends Task {

	public C() {
		super("Algorithm1",
				"This is the first algorithm. It has two inputs and three outputport.");
		ins = new InputPort[3];
		outs = new OutputPort[1];
		ins[0] = new InputPort("in0", Dataview.datatype.DATAVIEW_int, "This is the first input");
		ins[1] = new InputPort("in1", Dataview.datatype.DATAVIEW_int, "This is the second input");
		ins[2] = new InputPort("in2", Dataview.datatype.DATAVIEW_int, "This is the second input");
		
		outs[0] = new OutputPort("out0", Dataview.datatype.DATAVIEW_int, "This is the output");
		
	}

	@Override
	public void run(){
		int input0 = (int) ins[0].read1();
		int input1 = (int) ins[1].read1();
		int input2 = (int) ins[2].read1();
		
		int output0 = input0 * input1 * input2;
				
		outs[0].write1(output0);	
		FileTransfer.send("/home/ubuntu/multipleOutputResult.txt", "/home/ishtiaq/", "multipleOutputResult.txt", "172.30.17.203", "ishtiaq", "123", 22);
	}
}