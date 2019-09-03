
import dataview.models.*;
import dataview.planners.WorkflowPlanner;
import dataview.planners.WorkflowPlanner_E2C2D;

import java.util.ArrayList;

import dataview.workflowexecutor.WorkflowExecutor;
import dataview.workflowexecutor.WorkflowExecutorAlphaBackup;
import dataview.workflowexecutor.WorkflowExecutorAlphaScheild;
import dataview.workflowexecutor.WorkflowExecutor_Alpha;
/**
 * Testing the workflow scheduler
 * @author ishtiaqahmed
 *
 */

public class DriverSimpleWorkflow {
	/**
	 * Main method
	 * @param args
	 */

	public static void main(String[] args) {
		
		new DriverSimpleWorkflow().run(); 
	}
	
	/**
	 * Launching the Test initiator
	 */
	void run() {
		
		SimpleWorkflow w = new SimpleWorkflow();
		w.design();
		
		TaskSchedule taskSchedule1 = w.getTaskSchedule(w.getTask(0));
		TaskSchedule taskSchedule2 = w.getTaskSchedule(w.getTask(1));
		
		
		LocalSchedule localSchedule1 = new LocalSchedule();
		localSchedule1.addTaskSchedule(taskSchedule1);
		localSchedule1.setVmType("AMD");
		
		
		
		
		
		LocalSchedule localSchedule2 = new LocalSchedule();
		localSchedule2.addTaskSchedule(taskSchedule2);
		localSchedule2.setVmType("SGX");
		
		
		GlobalSchedule globalSchedule = new GlobalSchedule();
		globalSchedule.addLocalSchedule(localSchedule1);
		globalSchedule.addLocalSchedule(localSchedule2);
		
		System.out.println(globalSchedule.getSpecification());
		
		int sizeOfIteration = 1;
		
		Dataview.executionTimes = new ArrayList<Long>();
		for (int i = 1; i <= sizeOfIteration; i++) {
			Dataview.debugger.logSuccessfulMessage("Starting workflow executor for iteration " + i);
			WorkflowExecutor workflowExecutor = new WorkflowExecutor_Alpha("workflowTaskDir", "workflowLibDir", globalSchedule);
//			WorkflowExecutor workflowExecutor = new WorkflowExecutorAlpha("workflowTaskDir", "workflowLibDir", gsch);
			workflowExecutor.execute();
		}
		
	}
}
