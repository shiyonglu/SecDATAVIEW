
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

public class DriverDiagnosis {
	
	public static void main(String[] args) {
		
		new DriverDiagnosis().run(); 
	}
	
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
		localSchedule1.setVmType("SGX"); 
		localSchedule1.addTaskSchedule(taskSchedule1);
		
		LocalSchedule localSchedule2 = new LocalSchedule();
		localSchedule2.setVmType("SGX"); 
		localSchedule2.addTaskSchedule(taskSchedule2);
		
		LocalSchedule localSchedule3 = new LocalSchedule();
		localSchedule3.setVmType("SGX"); 
		localSchedule3.addTaskSchedule(taskSchedule3);
		
		LocalSchedule localSchedule4 = new LocalSchedule();
		localSchedule4.setVmType("SGX"); 
		localSchedule4.addTaskSchedule(taskSchedule4);
		
		
		
		LocalSchedule localSchedule5 = new LocalSchedule();
		localSchedule5.setVmType("SGX"); 
		localSchedule5.addTaskSchedule(taskSchedule5);
		
//		LocalSchedule localSchedule6 = new LocalSchedule();
//		localSchedule6.setVmType("SGX"); 
		localSchedule5.addTaskSchedule(taskSchedule6);
		
		GlobalSchedule globalSchedule = new GlobalSchedule();
		globalSchedule.addLocalSchedule(localSchedule1);
		globalSchedule.addLocalSchedule(localSchedule2);
		globalSchedule.addLocalSchedule(localSchedule3);
		globalSchedule.addLocalSchedule(localSchedule4);
		globalSchedule.addLocalSchedule(localSchedule5);
//		globalSchedule.addLocalSchedule(localSchedule6);

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
