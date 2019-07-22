

import dataview.models.Task;
import dataview.models.Workflow;

public class SimpleWorkflow extends Workflow {

	public SimpleWorkflow() {
		super("Simple Workflow", "Two tasks. First one is AND and second one NOT");
	}
	public void design()
	{

        // create and add all the tasks
		
		Task T1 = addTask("Task1");
		Task T2 = addTask("Task2");
		
		addEdge("input1.txt", T1, 0);
		addEdge("input2.txt", T1, 1);
		
		addEdge(T1, 0, T2, 0);

		addEdge(T2, 0, "result.txt");	    
	}
		
}
