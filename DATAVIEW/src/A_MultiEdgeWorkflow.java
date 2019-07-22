

import dataview.models.Task;
import dataview.models.Workflow;

public class A_MultiEdgeWorkflow extends Workflow {

	public A_MultiEdgeWorkflow() {
		super("A_MultiEdge", " This workflow demonstrates multiple outgoing edges to the same destination task");
	}
	public void design()
	{

        // create and add all the tasks
		
		Task T1 = addTask("A");
		Task T2 = addTask("B");
		Task T3 = addTask("C");
		
		
		
		// add edges
		addEdge("input0.txt", T1, 0);
		addEdge("input1.txt", T1, 1);
		addEdge(T1, 0, T2, 0);
		addEdge(T1, 1, T2, 1);
		addEdge(T1, 2, T2, 2);
		
		
		addEdge(T2, 0, T3, 0);
		addEdge(T2, 1, T3, 1);
		addEdge(T2, 2, T3, 2);
		
		
		addEdge(T3, 0, "multipleOutputResult.txt");	    
	}
		
}
