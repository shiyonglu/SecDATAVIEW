import java.util.ArrayList;
import java.util.List;

import dataview.models.Workflow;
import dataview.models.*;

public class MontageWorkflow extends Workflow{		
	public MontageWorkflow()
	{
		super("WordCount_workflow", "This workflow counts the frequency of a huge input file.");		
	}


	public void design()
	{
		
		
		
		// Creating all the tasks
		Task task1 = addTask("Task1");
		Task task2 = addTask("Task2");
		Task task3 = addTask("Task3");
		Task task4 = addTask("Task4");
		Task task5 = addTask("Task5");
		Task task6 = addTask("Task6");
		Task task7 = addTask("Task7");
		Task task8 = addTask("Task8");
		Task task9 = addTask("Task9");
		Task task10 = addTask("Task10");

		// Creating edges between task
		// First row
		addEdge("Task1_in0.txt", task1, 0);
		addEdge(task1, 0, task6, 0);
		addEdge(task1, 1, task3, 0);
		addEdge(task1, 2, task4, 0);
		
		addEdge("Task2_in0.txt", task2, 0);
		addEdge(task2, 0, task3, 1);
		addEdge(task2, 1, task4, 1);
		addEdge(task2, 2, task8, 1);
		
//		second row
		addEdge(task3, 0, task5, 0);
	
		addEdge(task4, 0, task5, 1);
		
		//third row
		addEdge(task5, 0, task6, 1);
		addEdge(task5, 1, task7, 0);
		addEdge(task5, 2, task8, 0);
		
		//fourth row
		addEdge(task6, 0, task9, 0);
		
		addEdge(task7, 0, task9, 1);
		
		addEdge(task8, 0, task9, 2);
		
		// fifth row
		addEdge(task9, 0, task10, 0);
		
		// last row
		addEdge(task10, 0, "workflowOutput.txt");
		
		
	}
}
