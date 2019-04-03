package dataview.models;
import java.util.ArrayList;
import java.util.HashSet;

import dataview.planners.*;

public class Dataview {
	// create some global objects for the whole system to use, such as debugger
	// now people can call Dataview.debugger.
	public static final Debugger debugger = new Debugger("dataview.log");
	public static final Port   datatype = new Port();
	public static final WorkflowPlanner planner = new WorkflowPlanner();
	public static long totalExecutionTime = 0;
	public static ArrayList<Long> executionTimes = new ArrayList<Long>();
	public static HashSet<String> confidentialTasks = new HashSet<String>();
}
