
import java.util.ArrayList;

import dataview.models.Dataview;
import dataview.models.GlobalSchedule;
import dataview.models.LocalSchedule;
import dataview.models.TaskSchedule;
import dataview.workflowexecutor.WorkflowExecutor;
import dataview.workflowexecutor.WorkflowExecutor_Alpha;
/**
 * Testing the workflow scheduler
 * @author Ishtiaq Ahmed & Saeid Mofrad
 *
 */

public class DriverNN {
	/**
	 * Main method
	 * @param args
	 */

	public static void main(String[] args) {
		
		new DriverNN().run(); 
	}
	
	/**
	 * Launching the Test initiator
	 */
	void run() {
		
//		MontageWorkflow w = new MontageWorkflow();
		
		NeuralNetWorkflow w = new NeuralNetWorkflow();
		w.design();
		
		TaskSchedule taskSchedule1 = w.getTaskSchedule(w.getTask(0));
		
		
		LocalSchedule localSchedule1 = new LocalSchedule();
		localSchedule1.addTaskSchedule(taskSchedule1);
		localSchedule1.setVmType("AMD"); 
				
		GlobalSchedule globalSchedule = new GlobalSchedule();
		globalSchedule.addLocalSchedule(localSchedule1);

		System.out.println(globalSchedule.getSpecification());
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