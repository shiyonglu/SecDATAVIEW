

import dataview.models.Task;
import dataview.models.Workflow;

public class MapReduceWorkflow extends Workflow {

	public MapReduceWorkflow() {
		super("Diagnosis Recommendation", " This workflow is for doctor to make some recommendation for a patient");
	}
	public void design()
	{

		// create and add all the tasks

		Task T1 = addTask("Input");

		Task T2 = addTask("Splitting");
		Task T3 = addTask("Splitting");
		Task T4 = addTask("Splitting");

		Task T5 = addTask("Mapping");
		Task T6 = addTask("Mapping");
		Task T7 = addTask("Mapping");


		Task T8 = addTask("Shuffling");
		Task T9 = addTask("Shuffling");
		Task T10 = addTask("Shuffling");
		Task T11 = addTask("Shuffling");



		Task T12 = addTask("Reducing");
		Task T13 = addTask("Reducing");
		Task T14 = addTask("Reducing");
		Task T15 = addTask("Reducing");

		Task T16 = addTask("FinalResult");


		addEdge("originalInput.enc", T1, 0);
		addEdge(T1, 0, T2, 0);
		addEdge(T1, 1, T3, 0);
		addEdge(T1, 2, T4, 0);

		// Splitting
		addEdge(T2, 0, T5, 0);
		addEdge(T3, 0, T6, 0);
		addEdge(T4, 0, T7, 0);

		// Mapping
		addEdge(T5, 0, T8, 0);
		addEdge(T5, 1, T9, 0);
		addEdge(T5, 2, T10, 0);
		addEdge(T5, 3, T11, 0);

		addEdge(T6, 0, T8, 1);
		addEdge(T6, 1, T9, 1);
		addEdge(T6, 2, T10, 1);
		addEdge(T6, 3, T11, 1);


		addEdge(T7, 0, T8, 2);
		addEdge(T7, 1, T9, 2);
		addEdge(T7, 2, T10, 2);
		addEdge(T7, 3, T11, 2);


		//Shuffling

		addEdge(T8, 0, T12, 0);

		addEdge(T9, 0, T13, 0);

		addEdge(T10, 0, T14, 0);

		addEdge(T11, 0, T15, 0);

		// Reducing

		addEdge(T12, 0, T16, 0);

		addEdge(T13, 0, T16, 1);

		addEdge(T14, 0, T16, 2);

		addEdge(T15, 0, T16, 3);



		// FinalResult
		addEdge(T16, 0, "workflowOutput.enc");



	}

}
