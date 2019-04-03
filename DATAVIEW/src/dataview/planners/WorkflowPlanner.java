package dataview.planners;
import java.util.ArrayList;
import java.util.List;

import dataview.models.*;

public class WorkflowPlanner {
	public static final int WorkflowPlanner_Naive1 = 0;
	public static final int WorkflowPlanner_Naive2 = 1;
	public static final int WorkflowPlanner_T_Cluster = 2;

	protected Workflow w;
	
	// this is the graph of the workflow:
	protected int numnode;     // number of tasks
	protected List<Integer>[] alist; // adjacency list
		

	public WorkflowPlanner()
	{
	}
	

	
	/**
	 * The constructor will create an adjacency list representation of the given workflow.
	 *  
	 * @param w
	 */
	public WorkflowPlanner(Workflow w)
	{
	    this.w = w;
	    this.numnode = w.getNumOfTasks();
		alist = new List[numnode];
		for(int i=0; i< numnode; i++) {
			alist[i] = new ArrayList<Integer>();
		}	    
	   
		for(WorkflowEdge e: w.getEdges()) {
			if(e.srcTask != null && e.destTask != null) { // add an edge in the adjacency list
			     int i = w.getIndexOfTask(e.srcTask);
			     int j = w.getIndexOfTask(e.destTask);
			     alist[i].add(j);
			}
		}		
	}

	public GlobalSchedule plan()
	{
		return null;
	}
}
