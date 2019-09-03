
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

public class DriverMapReduceNew {
	/**
	 * Main method
	 * @param args
	 */

	public static void main(String[] args) {
		
		new DriverMapReduceNew().run(); 
	}
	
	/**
	 * Launching the Test intiator
	 */
	void run() {
		
//		MontageWorkflow w = new MontageWorkflow();
//		WorkflowVisualization frame = new WorkflowVisualization();
		
		MapReduceWorkflow w = new MapReduceWorkflow();
		w.design();
		
//		frame.drawWorkflowGraph(w);
		
		
		TaskSchedule taskSchedule1 = w.getTaskSchedule(w.getTask(0));
		TaskSchedule taskSchedule2 = w.getTaskSchedule(w.getTask(1));
		TaskSchedule taskSchedule3 = w.getTaskSchedule(w.getTask(2));
		TaskSchedule taskSchedule4 = w.getTaskSchedule(w.getTask(3));
		TaskSchedule taskSchedule5 = w.getTaskSchedule(w.getTask(4));
		TaskSchedule taskSchedule6 = w.getTaskSchedule(w.getTask(5));
		
		TaskSchedule taskSchedule7 = w.getTaskSchedule(w.getTask(6));
		TaskSchedule taskSchedule8 = w.getTaskSchedule(w.getTask(7));
		TaskSchedule taskSchedule9 = w.getTaskSchedule(w.getTask(8));
		TaskSchedule taskSchedule10 = w.getTaskSchedule(w.getTask(9));
		TaskSchedule taskSchedule11 = w.getTaskSchedule(w.getTask(10));
		TaskSchedule taskSchedule12 = w.getTaskSchedule(w.getTask(11));
		
		
		TaskSchedule taskSchedule13 = w.getTaskSchedule(w.getTask(12));
		TaskSchedule taskSchedule14 = w.getTaskSchedule(w.getTask(13));
		TaskSchedule taskSchedule15 = w.getTaskSchedule(w.getTask(14));
		TaskSchedule taskSchedule16 = w.getTaskSchedule(w.getTask(15));
		
		
		LocalSchedule localSchedule1 = new LocalSchedule();
		localSchedule1.setVmType("AWS"); // SGX
		localSchedule1.addTaskSchedule(taskSchedule1);
		localSchedule1.addTaskSchedule(taskSchedule2);
		localSchedule1.addTaskSchedule(taskSchedule3);
		
		LocalSchedule localSchedule2 = new LocalSchedule();
		localSchedule2.setVmType("AWS"); // SGX
		localSchedule2.addTaskSchedule(taskSchedule4);
		localSchedule2.addTaskSchedule(taskSchedule5);
		localSchedule2.addTaskSchedule(taskSchedule6);

		LocalSchedule localSchedule3 = new LocalSchedule();
		localSchedule3.setVmType("AWS"); // SGX
		localSchedule3.addTaskSchedule(taskSchedule7);
		localSchedule3.addTaskSchedule(taskSchedule8);
		localSchedule3.addTaskSchedule(taskSchedule9);
		
		LocalSchedule localSchedule4 = new LocalSchedule();
		localSchedule4.setVmType("AWS"); // SGX
		localSchedule4.addTaskSchedule(taskSchedule10);
		localSchedule4.addTaskSchedule(taskSchedule11);
		localSchedule4.addTaskSchedule(taskSchedule12);
		
		
		LocalSchedule localSchedule5 = new LocalSchedule();
		localSchedule5.setVmType("AWS"); // SGX
		localSchedule5.addTaskSchedule(taskSchedule13);
		localSchedule5.addTaskSchedule(taskSchedule14);
		localSchedule5.addTaskSchedule(taskSchedule15);
		localSchedule5.addTaskSchedule(taskSchedule16);
		
		
		
		GlobalSchedule globalSchedule = new GlobalSchedule();
		globalSchedule.addLocalSchedule(localSchedule1);
		globalSchedule.addLocalSchedule(localSchedule2);
		globalSchedule.addLocalSchedule(localSchedule3);
		globalSchedule.addLocalSchedule(localSchedule4);
		globalSchedule.addLocalSchedule(localSchedule5);
		
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
