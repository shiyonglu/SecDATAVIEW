
import dataview.models.*;
import dataview.workflowexecutor.WorkflowExecutor;
import dataview.workflowexecutor.WorkflowExecutor_Alpha;

import java.util.ArrayList;
/**
 * Testing the workflow scheduler
 * @author ishtiaqahmed
 *
 */

public class DriverA_MultiEdge {

	public static void main(String[] args) {
		
		new DriverA_MultiEdge().run(); 
	}
	
	void run() {
		
//		MontageWorkflow w = new MontageWorkflow();
		
		A_MultiEdgeWorkflow w = new A_MultiEdgeWorkflow();
		w.design();
		
		TaskSchedule taskSchedule1 = w.getTaskSchedule(w.getTask(0));
		TaskSchedule taskSchedule2 = w.getTaskSchedule(w.getTask(1));
		TaskSchedule taskSchedule3 = w.getTaskSchedule(w.getTask(2));
		
		
		LocalSchedule localSchedule1 = new LocalSchedule();
		localSchedule1.addTaskSchedule(taskSchedule1);
		localSchedule1.addTaskSchedule(taskSchedule2);
		localSchedule1.addTaskSchedule(taskSchedule3);
		localSchedule1.setVmType("AMD");
		
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
