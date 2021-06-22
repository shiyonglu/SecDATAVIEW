import dataview.models.*;
import dataview.workflowexecutor.WorkflowExecutor;
import dataview.workflowexecutor.WorkflowExecutor_Alpha;

import java.util.ArrayList;
/**
 * Testing the workflow scheduler
 * @author ishtiaqahmed
 *
 */

public class DriverMontage {
	/**
	 * Main method
	 * @param args
	 */

	public static void main(String[] args) {

		new DriverMontage().run(); 
	}

	/**
	 * Launching the Test intiator
	 */
	void run() {
		WorkflowVisualization frame = new WorkflowVisualization();
		MontageWorkflow w = new MontageWorkflow();
		w.design();
		frame.drawWorkflowGraph(w);

		int totalTasks = 1 + (DisKMeansWorkflow.M + 1 + DisKMeansWorkflow.M) * DisKMeansWorkflow.iteration;

		TaskSchedule[] taskSchedules = new TaskSchedule[totalTasks];
		LocalSchedule localSchedule = new LocalSchedule();
		for (int i = 0; i < taskSchedules.length; i++) {
			taskSchedules[i] = w.getTaskSchedule(w.getTask(i));
			localSchedule.addTaskSchedule(taskSchedules[i]);
		}

		localSchedule.setVmType("AMD"); // SGX
		GlobalSchedule globalSchedule = new GlobalSchedule();
		globalSchedule.addLocalSchedule(localSchedule);


		System.out.println(globalSchedule.getSpecification());

		int sizeOfIteration = 1;

		Dataview.executionTimes = new ArrayList<Long>();
		for (int i = 1; i <= sizeOfIteration; i++) {
			Dataview.debugger.logSuccessfulMessage("Starting workflow executor for iteration " + i);
			WorkflowExecutor workflowExecutor = new WorkflowExecutor_Alpha("workflowTaskDir", "workflowLibDir", globalSchedule);
			workflowExecutor.execute();
		}

	}
}