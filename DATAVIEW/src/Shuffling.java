import java.util.*;
import java.util.Map.Entry;

import dataview.models.Dataview;
import dataview.models.InputPort;
import dataview.models.OutputPort;
import dataview.models.Task;

public class Shuffling extends Task{

	public Shuffling() {
		super("Shuffling", "This is a shuffling Task");
		ins = new InputPort[3];
		outs = new OutputPort[1];
		ins[0] = new InputPort("in0", Dataview.datatype.DATAVIEW_String, "This is the first number");
		ins[1] = new InputPort("in1", Dataview.datatype.DATAVIEW_String, "This is the first number");
		ins[2] = new InputPort("in2", Dataview.datatype.DATAVIEW_String, "This is the first number");
		
		
		outs[0] = new OutputPort("out0", Dataview.datatype.DATAVIEW_String, "This is the output");
		
	}

	@Override
	public void run() {

		StringBuilder stringBuilder = new StringBuilder();

		TreeMap<String, Integer> treeMap = new TreeMap<>();
		
		for (int j = 0; j < ins.length; j++) {
			
			String input0 = (String) ins[j].read1();
			Dataview.debugger.logSuccessfulMessage("here is the input0" + input0);

			if (input0 != null && !input0.isEmpty()) {
				String lines[] = input0.split("\\r?\\n");
				int numofline = lines.length;
				
				for (int i = 0; i < numofline; i++) {
					String[] parts = lines[i].split(",");
					int count = Integer.parseInt(parts[1]);
					treeMap.put(parts[0], treeMap.getOrDefault(parts[0], 0) + count);
				}
			}
		}
		
		Iterator iterator = treeMap.entrySet().iterator();
		while(iterator.hasNext()) {
			Map.Entry<String, Integer> pair = (Entry<String, Integer>) iterator.next();
			stringBuilder.append(pair.getKey()).append(",").append(pair.getValue()).append("\n");
		}
		int len0 = stringBuilder.length();
		if (len0 > 0 && stringBuilder.charAt(len0 - 1) == '\n') stringBuilder.deleteCharAt(len0 - 1);
		
		
		outs[0].write1(stringBuilder.toString());
	}

}
