
import dataview.models.Dataview;
import dataview.models.InputPort;
import dataview.models.OutputPort;
import dataview.models.Task;

public class B extends Task {

	public B() {
		super("B",
				"This is the first algorithm. It has two inputs and three outputport.");
		ins = new InputPort[3];
		outs = new OutputPort[3];
		ins[0] = new InputPort("in0", Dataview.datatype.DATAVIEW_int, "This is the first input");
		ins[1] = new InputPort("in1", Dataview.datatype.DATAVIEW_int, "This is the second input");
		ins[2] = new InputPort("in2", Dataview.datatype.DATAVIEW_int, "This is the second input");
		
		outs[0] = new OutputPort("out0", Dataview.datatype.DATAVIEW_int, "This is the output");
		outs[1] = new OutputPort("out1", Dataview.datatype.DATAVIEW_int, "This is the output");
		outs[2] = new OutputPort("out2", Dataview.datatype.DATAVIEW_int, "This is the output");
	}

	@Override
	public void run(){
		int input0 = (int) ins[0].read1();
		int input1 = (int) ins[1].read1();
		int input2 = (int) ins[2].read1();
		
		int output0 = input0 * 2;
		int output1 = input1 * 2;
		int output2 = input2 * 2;
				
		outs[0].write1(output0);	
		outs[1].write1(output1);
		outs[2].write1(output2);
			
	}
}