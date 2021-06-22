
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

public class DriverMapReduce {
	/**
	 * Main method
	 * @param args
	 */

	public static void main(String[] args) {
		
		new DriverMapReduce().run(); 
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
		localSchedule1.addTaskSchedule(taskSchedule1);
		localSchedule1.addTaskSchedule(taskSchedule2);
		localSchedule1.addTaskSchedule(taskSchedule3);
		localSchedule1.addTaskSchedule(taskSchedule4);
		localSchedule1.addTaskSchedule(taskSchedule5);
		localSchedule1.addTaskSchedule(taskSchedule6);

		localSchedule1.addTaskSchedule(taskSchedule7);
		localSchedule1.addTaskSchedule(taskSchedule8);
		localSchedule1.addTaskSchedule(taskSchedule9);
		localSchedule1.addTaskSchedule(taskSchedule10);
		localSchedule1.addTaskSchedule(taskSchedule11);
		localSchedule1.addTaskSchedule(taskSchedule12);
		
		localSchedule1.addTaskSchedule(taskSchedule13);
		localSchedule1.addTaskSchedule(taskSchedule14);
		localSchedule1.addTaskSchedule(taskSchedule15);
		localSchedule1.addTaskSchedule(taskSchedule16);
		
		localSchedule1.setVmType("SGX"); // SGX
		
		GlobalSchedule globalSchedule = new GlobalSchedule();
		globalSchedule.addLocalSchedule(localSchedule1);
		
		
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
