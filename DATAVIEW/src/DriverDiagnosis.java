
import dataview.models.*;
import dataview.planners.WorkflowPlanner;
import dataview.planners.WorkflowPlanner_E2C2D;

import java.util.ArrayList;

import dataview.workflowexecutor.WorkflowExecutor;
import dataview.workflowexecutor.WorkflowExecutorAlphaBackup;
import dataview.workflowexecutor.WorkflowExecutorAlphaScheild;
import dataview.workflowexecutor.WorkflowExecutorAlpha;
/**
 * Testing the workflow scheduler
 * @author ishtiaqahmed
 *
 */

public class DriverDiagnosis {
	/**
	 * Main method
	 * @param args
	 */

	public static void main(String[] args) {
		
		new DriverDiagnosis().run(); 
	}
	
	/**
	 * Launching the Test initiator
	 */
	void run() {
		
//		MontageWorkflow w = new MontageWorkflow();
		DiagnosisWorkflow w = new DiagnosisWorkflow();
		w.design();
		
		
		TaskSchedule taskSchedule1 = w.getTaskSchedule(w.getTask(0));
		TaskSchedule taskSchedule2 = w.getTaskSchedule(w.getTask(1));
		TaskSchedule taskSchedule3 = w.getTaskSchedule(w.getTask(2));
		TaskSchedule taskSchedule4 = w.getTaskSchedule(w.getTask(3));
		TaskSchedule taskSchedule5 = w.getTaskSchedule(w.getTask(4));
		TaskSchedule taskSchedule6 = w.getTaskSchedule(w.getTask(5));
		
		
		LocalSchedule localSchedule1 = new LocalSchedule();
		localSchedule1.addTaskSchedule(taskSchedule1);
		localSchedule1.addTaskSchedule(taskSchedule2);
		localSchedule1.addTaskSchedule(taskSchedule3);
		localSchedule1.addTaskSchedule(taskSchedule4);
		localSchedule1.addTaskSchedule(taskSchedule5);
		localSchedule1.addTaskSchedule(taskSchedule6);
		
		
		GlobalSchedule globalSchedule = new GlobalSchedule();
		globalSchedule.addLocalSchedule(localSchedule1);
		
		
		System.out.println(globalSchedule.getSpecification());
		
		int sizeOfIteration = 1;
		
		Dataview.executionTimes = new ArrayList<Long>();
		for (int i = 1; i <= sizeOfIteration; i++) {
			Dataview.debugger.logSuccessfulMessage("Starting workflow executor for iteration " + i);
			WorkflowExecutor workflowExecutor = new WorkflowExecutorAlpha("workflowTaskDir", "workflowLibDir", globalSchedule);
			workflowExecutor.execute();
		}
		
	}
}