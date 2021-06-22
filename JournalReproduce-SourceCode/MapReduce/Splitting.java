import java.util.*;

import dataview.models.Dataview;
import dataview.models.InputPort;
import dataview.models.OutputPort;
import dataview.models.Task;

public class Splitting extends Task{

	public Splitting() {
		super("Splitting", "This is a splitting Task");
		ins = new InputPort[1];
		outs = new OutputPort[1];
		ins[0] = new InputPort("in0", Dataview.datatype.DATAVIEW_String, "This is the first number");
		outs[0] = new OutputPort("out0", Dataview.datatype.DATAVIEW_String, "This is the output");
	}

	@Override
	public void run() {
		// step 1: read from the input ports
		String input0 = (String) ins[0].read1();
//		Dataview.debugger.logSuccessfulMessage("here is the input0" + input0);

		
		
		String lines[] = input0.split("\\r?\\n");
		int numofline = lines.length;
		
		TreeMap<String, Integer> treeMap = new TreeMap<>(); 
		
		for (int i = 0; i < numofline; i++) {
			String[] words = lines[i].split(" ");
			for (String word : words) {
				treeMap.put(word, treeMap.getOrDefault(word, 0) + 1);
			}
		}
		Iterator iterator = treeMap.entrySet().iterator();
		StringBuilder result = new StringBuilder();
		int i = 0;
		while(iterator.hasNext()) {
			Map.Entry pair = (Map.Entry)iterator.next();

			result.append(pair.getKey() + "," + pair.getValue());
			if (i < treeMap.size() - 1) result.append("\n");
			i++;
		}
		
		outs[0].write1(result.toString());
		

	}

}
