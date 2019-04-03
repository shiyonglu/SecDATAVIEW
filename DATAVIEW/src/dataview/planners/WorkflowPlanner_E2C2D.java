package dataview.planners;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import dataview.models.*;
import dataview.planners.WorkflowPlanner;

class VMI implements Comparable<VMI> {
	double[] EST;
	double[] EFT;
	double[] LFT;
	double cost;
	int assignedVMIndex;
	ArrayList<Integer> path;
	VMI previous;
	double additionalCost;

	@Override
	public int compareTo(VMI vmi) {
		if (this.previous != null) {
			return (int)(this.additionalCost - vmi.additionalCost);
		}
		return (int) (this.cost - vmi.cost);
	}
}


public class WorkflowPlanner_E2C2D extends WorkflowPlanner{
	private double deadline = 70;
	private int totalVMs = 3;
	private double billingCycle = 30;
	private int totalTasks;


	private final double INF = Integer.MAX_VALUE;
	private final double MIN = Integer.MIN_VALUE; 


	//TODO fixed for Paper workflow
	double[][] transferTime = {
			// 0     1     2      3      4       5      6       7     8      9      10    11
			{INF,   0,     0,     0,     INF,   INF,   INF,    INF,  INF,   INF,   INF, INF},// 0
			{INF,   INF,   INF,   INF,   2,     INF,   INF,    INF,  INF,   INF,   INF, INF},// 1
			{INF,   INF,   INF,   INF,   INF,   2,     INF,    INF,  INF,   INF,   INF, INF},// 2
			{INF,   INF,   INF,   INF,   INF,   INF,   INF,    INF,  INF,   3,     INF, INF},// 3
			{INF,   INF,   INF,   INF,   INF,   INF,   2,      1,    INF,   INF,   INF, INF},// 4
			{INF,   INF,   INF,   INF,   INF,   INF,   INF,    2,    2,     2,     INF, INF},// 5
			{INF,   INF,   INF,   INF,   INF,   INF,   INF,    INF,  INF,   INF,   INF, 0},  // 6
			{INF,   INF,   INF,   INF,   INF,   INF,   3,      INF,  INF,   INF,   INF, INF},// 7
			{INF,   INF,   INF,   INF,   INF,   INF,   INF,    INF,  INF,   INF,   INF, 0  },// 8
			{INF,   INF,   INF,   INF,   INF,   INF,   INF,    INF,  INF,   INF,   1,   INF},// 9
			{INF,   INF,   INF,   INF,   INF,   INF,   INF,    INF,  INF,   INF,   INF, 0},//10
	};

	/*
	 * first one is the fastest machine while the last one is the cheapest.
	 */
	double[] costPerBillingCycle = {13, 8, 5}; 
	int[] taskAllocationMap;
	double[] EST;
	double[] EFT;
	double[] LFT;

	private boolean[][] graph;
	boolean isVisited[];

	/*for topological sorting*/
	boolean isColored[];

	ArrayList<Integer> list = new ArrayList<Integer>();
	ArrayList<Integer> confidentialTasks = new ArrayList<Integer>();

	private HashMap<Integer, Boolean> isSourceNode = new HashMap<Integer, Boolean>();
	private HashMap<Integer, Boolean> isDestinationNode = new HashMap<Integer, Boolean>();
	private HashMap<String, HashMap<Integer, Double>> execTime;
	ArrayList<VMI> existingAllocations = new ArrayList<VMI>();

	//TODO
	//private HashMap<Integer, Map<Integer, Double>> edgeMap;     



	public static void main(String[] args) {

	}

	public WorkflowPlanner_E2C2D(Workflow workflow) {
		super(workflow);
	}

	public GlobalSchedule plan() {
		Dataview.debugger.logSuccessfulMessage("SGX-E2C2D is started");
		scheduleWorkflow();
		assignChildren();
		GlobalSchedule globalSchedule = createGlobalSchedule();
		return globalSchedule;
	}

	private GlobalSchedule createGlobalSchedule() {
		/*Displaying result*/
		double totalCost = 0;
		for(VMI vmi : existingAllocations) {
			System.out.println("Path : ");
			for (int i = 0; i < vmi.path.size(); i++) {
				System.out.print(vmi.path.get(i) + " ");
			}
			System.out.println("Assigned Machine : " + vmi.assignedVMIndex + " Cost : " + vmi.cost);
			totalCost += vmi.cost;
		}
		System.out.println("Total cost : " + totalCost);
		for(int i = 1; i < EST.length - 1; i++) {
			System.out.println("Task " + i + " " + EST[i] + " " + EFT[i] + " " + LFT[i]);
		}
		
		
		GlobalSchedule globalSchedule = new GlobalSchedule();
		for(int i = 0; i <= totalVMs; i++) { /* last VM will be considered for SGX*/
			LocalSchedule localSchedule = new LocalSchedule();
			for (int task = 1; task < totalTasks - 2; task++) {
				if (taskAllocationMap[task] == i) {
					TaskSchedule taskSchedule = w.getTaskSchedule(w.getTask(task - 1));
					localSchedule.addTaskSchedule(taskSchedule);
				}
			}
			if(localSchedule.length() != 0) {
				globalSchedule.addLocalSchedule(localSchedule);
			}
		}
		return null;
	}

	private void assignChildren() {
		ArrayList<Integer> list = topologicalSort(); 
		for (Integer i : list) {
			System.out.println(i);
		}
		while(!list.isEmpty()) {
			ArrayList<Integer> criticalPath = new ArrayList<Integer>();
			int task = list.get(0);
			criticalPath.add(task);
			while(hasChild(task)) {
				task = criticalChild(task);
				criticalPath.add(task);
			}

			ArrayList<ArrayList<Integer>> paths = splitCriticalPathByConfidentialTask(criticalPath);

			for(ArrayList<Integer> path : paths) {
				allocateVirtualMachine(path);
				for (Integer t : path) {
					isVisited[t] = true;
				}
				for(int t : path) {
					calculateEST_EFT(t);
					calculateLFT(t);
				}

				/* removing all tasks and edges invloving tasks in path from G
				 * removing all tasks involving in path from list*/
				for (Integer t : path) {
					isVisited[t] = true;
					list.remove(t);
				}
			}			
		}

	}

	private void allocateVirtualMachine(ArrayList<Integer> path) {
		if(isSecureEnvironmentNeeded(path)) {
			//TODO launch a new vm instance of type VMT^SGX
			for (int i = 0; i < path.size(); i++) {
				taskAllocationMap[path.get(i)] = totalVMs;
			}
			return;
		}
		/*
		 * Now creating instances without using unused time
		 */
		ArrayList<VMI> instancesWithoutUnusedTime = createVMIsWithoutUnusedTime(path);
		Collections.sort(instancesWithoutUnusedTime);
		if(existingAllocations.isEmpty()) {
			updateGlobalEST_EFT_LFT(instancesWithoutUnusedTime.get(0), path);
			return;
		}

		/*Allocation*/
		ArrayList<VMI> vmiUtilizedRemainingTime = createVMIsWithUnusedTime(instancesWithoutUnusedTime);
		if(vmiUtilizedRemainingTime.isEmpty()) {
			updateGlobalEST_EFT_LFT(instancesWithoutUnusedTime.get(0), path);
		} else {
			Collections.sort(vmiUtilizedRemainingTime);
			if (instancesWithoutUnusedTime.get(0).cost < vmiUtilizedRemainingTime.get(0).additionalCost) {
				updateGlobalEST_EFT_LFT(instancesWithoutUnusedTime.get(0), path);
			} else {
				updateGlobalEST_EFT_LFT(vmiUtilizedRemainingTime.get(0), path);
			}
		}
	}

	private void updateGlobalEST_EFT_LFT(VMI instance, ArrayList<Integer> path) {
		existingAllocations.add(instance);
		/*Deleting existing instance*/
		if (instance.previous != null) {
			existingAllocations.remove(instance.previous);
		}
		
		EST = Arrays.copyOf(instance.EST, EST.length);
		EFT = Arrays.copyOf(instance.EFT, EFT.length);
		LFT = Arrays.copyOf(instance.LFT, LFT.length);
		for (int i = 0; i < path.size(); i++) {
			taskAllocationMap[path.get(i)] = instance.assignedVMIndex;
		}
		
	}

	private ArrayList<VMI> createVMIsWithUnusedTime(ArrayList<VMI> instancesWithoutUnusedTime) {
		ArrayList<VMI> combinedVMIs = new ArrayList<VMI>();

		for (int i = 0; i < existingAllocations.size(); i++) {
			VMI existingVMI = existingAllocations.get(i);
			for (int j = 0; j < instancesWithoutUnusedTime.size(); j++) {
				VMI token = instancesWithoutUnusedTime.get(j);
				if (token.assignedVMIndex == existingVMI.assignedVMIndex) {
					VMI begin = insert(token, existingVMI); // insert beginning
					if (begin != null) combinedVMIs.add(begin);
					VMI middle = insertMIddle(token, existingVMI);
					if (middle != null) combinedVMIs.add(middle);
					VMI end = insert(existingVMI, token); // insert end
					if (end != null) combinedVMIs.add(end);
				}
			}
		}
		return combinedVMIs;
	}

	private VMI insertMIddle(VMI token, VMI existingVMI) {
		// TODO Auto-generated method stub
		return null;
	}
	
	

	private VMI insert(VMI vmi1, VMI vmi2) {
		/*Checking token last task of LFT <= existing first task EST*/
		if (vmi1.LFT[vmi1.path.size() - 1] <= vmi2.EST[0]) {
			double startTime = vmi1.EST[0] + vmi2.EST[0] - vmi1.LFT[vmi1.path.size() - 1];
			for(double start = startTime; start >= vmi1.EST[0]; start--) {
				boolean canFit = true;
				double newEFT = start + execTime.get(vmi1.assignedVMIndex + "").get(vmi1.path.get(0));
				for (int i = 0; i < vmi1.path.size(); i++) {
					if(newEFT > vmi1.LFT[i]) {
						canFit = false;
						break;
					}
					newEFT += execTime.get(vmi1.assignedVMIndex + "").get(vmi1.path.get(i));
				}
				if (canFit) {
					VMI instance = new VMI();
					instance.EST = EST.clone();
					instance.EFT = EFT.clone();
					instance.LFT = LFT.clone();
					instance.assignedVMIndex = vmi1.assignedVMIndex;
					ArrayList<Integer> path = new ArrayList<Integer>();
					for (Integer t : vmi1.path) {
						path.add(t);
						instance.EST[t] = vmi1.EST[t];
						instance.EFT[t] = vmi1.EFT[t];
						instance.LFT[t] = vmi1.LFT[t];
					}
					newEFT = start + execTime.get(vmi1.assignedVMIndex + "").get(vmi1.path.get(0));
					double s = vmi2.EST[vmi2.path.get(0)];
					for (Integer t : vmi2.path) {
						path.add(t);
						instance.EST[t] = s;
						instance.EFT[t] = start + execTime.get(vmi1.assignedVMIndex + "").get(t);
						start = instance.EFT[t];
					}
					instance.cost = findCost(instance);
					return instance;
				}
			}
		}
		return null;
	}
	
	
	private double findCost(VMI instance) {
		double totalExecution = instance.EFT[instance.path.size() - 1] - instance.EST[0];
		double cost = Math.ceil(totalExecution / billingCycle) * costPerBillingCycle[instance.assignedVMIndex];
		return cost;
	}

	private ArrayList<VMI> createVMIsWithoutUnusedTime(ArrayList<Integer> path) {
		ArrayList<VMI> instancesWithoutUnusedTime = new ArrayList<VMI>();
		for (int vm = 0; vm < totalVMs; vm++) {
			if(isSatisfy(path, vm)) {
				VMI instance = new VMI();
				instance.EST = EST.clone();
				instance.EFT = EFT.clone();
				instance.LFT = LFT.clone();
				instance.assignedVMIndex = vm;
				instance.path = new ArrayList<Integer>(path);
				dummyAllocation(instance);
				instancesWithoutUnusedTime.add(instance);
			}
		}
		return instancesWithoutUnusedTime;
	}

	private void dummyAllocation(VMI instance) {
		double cost = 0;
		ArrayList<Integer> path = instance.path;
		int vm = instance.assignedVMIndex;
		double newStartTime = instance.EST[path.get(0)];
		
		for (int i = 0; i < path.size(); i++) {
			boolean hasIncomingEdge = false;
			if (i > 0 && newStartTime < instance.EST[path.get(i)]) {
				if(hasIncomingEdge(path, path.get(i))) {
					hasIncomingEdge = true;
				}
			}
			if (!hasIncomingEdge) {
				instance.EST[path.get(i)] = newStartTime;
			}
			instance.EFT[path.get(i)] = instance.EST[path.get(i)] + execTime.get(vm + "").get(path.get(i));

			newStartTime = instance.EFT[path.get(i)]; 
		}

		double newFinishTime = instance.LFT[path.get(path.size() - 1)];
		for (int i = path.size() - 1; i >= 0; i--) {
			if (instance.LFT[path.get(i)] > newFinishTime) {
				instance.LFT[path.get(i)] = newFinishTime;
			}
			newFinishTime = instance.LFT[path.get(i)] - execTime.get(vm + "").get(path.get(i));
		}
		double totalExecutionTime = instance.EFT[path.get(path.size() - 1)] - instance.EST[0];
		cost = Math.ceil(totalExecutionTime / billingCycle) * costPerBillingCycle[vm];
		instance.cost = cost;
	}

	private boolean hasIncomingEdge(ArrayList<Integer> path, Integer task) {
		for (int parent = 1; parent < totalTasks - 1; parent++) {
			if (parent == task) {
				continue;
			}
			if (graph[parent][task] && !path.contains(parent)) {
				return true;
			}
		}
		return false;
	}

	private boolean isSatisfy(ArrayList<Integer> path, int vm) {
		double newStartTime = EST[path.get(0)];
		for (int i = 0; i < path.size(); i++) {
			
			boolean isFound = false;
			double minChildEST = Integer.MAX_VALUE;
			int minChild = -1;
			for (int child = 1; child < totalTasks - 1; child++) {
				if (graph[path.get(i)][child] && isVisited[child]) {
					if (minChildEST > EST[child]) {
						minChildEST = EST[child];
						minChild = child;
						isFound = true;
					}
				}
			}
			if (isFound && newStartTime + execTime.get(vm + "").get(path.get(i)) > minChildEST + transferTime[path.get(i)][minChild]) {
				return false;
			}
			
			if(newStartTime + execTime.get(vm + "").get(path.get(i)) <= LFT[path.get(i)]) {
				newStartTime = newStartTime + execTime.get(vm + "").get(path.get(i));
			} else {
				return false;
			}
		}
		return true;
	}


	private boolean isSecureEnvironmentNeeded(ArrayList<Integer> path) {
		if(!confidentialTasks.isEmpty() & confidentialTasks.contains(path.get(0))) {
			return true;
		}
		return false;
	}

	private ArrayList<ArrayList<Integer>> splitCriticalPathByConfidentialTask(ArrayList<Integer> criticalPath) {
		ArrayList<ArrayList<Integer>> paths = new ArrayList<ArrayList<Integer>>();
		if (confidentialTasks.isEmpty()) {
			paths.add(criticalPath);
			return paths;
		}

		ArrayList<Integer> path = new ArrayList<Integer>();
		for (int i = 0; i < criticalPath.size(); i++) {
			if(confidentialTasks.contains(criticalPath.get(i))) {
				if(!path.isEmpty()) {
					paths.add(path);
				}

				path = new ArrayList<Integer>();
				/*Checking consecutive confidentialTasks*/
				for (int j = i; j < criticalPath.size(); j++) {
					if(confidentialTasks.contains(criticalPath.get(i))) {
						path.add(criticalPath.get(i));
					} else {
						paths.add(path);
						i = j;
						break;
					}
				}
				/*clearing the path list for next use*/
				path = new ArrayList<Integer>();
			} else {
				path.add(criticalPath.get(i));
			}
		}
		return paths;
	}

	private int criticalChild(int task) {
		int criticalChild = -1;
		double minimumLFT = Integer.MAX_VALUE;
		for (int i = 1; i < totalTasks - 1; i++) {
			if (graph[task][i] && !isVisited[i]) {
				if (LFT[i] < minimumLFT) {
					minimumLFT = LFT[i];
					criticalChild = i;
				}
			}
		}
		return criticalChild;
	}

	private boolean hasChild(int task) {
		for (int i = 1; i < totalTasks - 1; i++) {
			if (graph[task][i] && !isVisited[i]) {
				return true;
			}
		}
		return false;
	}

	ArrayList<Integer> topologicalSort() {
		isColored = new boolean[totalTasks];
		isColored[0] = isColored[totalTasks - 1] = true;
		/*for (int i = totalTasks -1; i >= 0; i--) {
			if(!isColored[i]) {
				findTopologicalOrder(i);
			}
		}*/
		for (int i = 1; i < totalTasks - 1; i++) {
			if(!isColored[i]) {
				findTopologicalOrder(i);
			}
		}
		int size = list.size();
		for (int i = 0; i <  size / 2; i++) {
			int temp = list.get(i);
			list.set(i, list.get(size - 1 - i));
			list.set(size - 1 - i, temp);
		}
		return list;
	}
	void findTopologicalOrder(int task) {
		for(int i = 1; i < totalTasks; i++) {
			if (graph[task][i] && !isColored[i]) {
				findTopologicalOrder(i);
			}
		}
		list.add(task);
		isColored[task] = true;
	}

	private void scheduleWorkflow() {
		preprocessing();

		EST = new double[totalTasks];
		Arrays.fill(EST, MIN);
		EST[0] = 0;

		EFT = new double[totalTasks];
		Arrays.fill(EFT, MIN);
		EFT[0] = 0;

		LFT = new double[totalTasks];
		Arrays.fill(LFT, INF);
		LFT[totalTasks - 1] = deadline;

		calculateEST_EFT(0);
		calculateLFT(totalTasks - 1);

		/*Removing start and end nodes from the graph*/
		isVisited[0] = isVisited[totalTasks - 1] = true;
	}

	private void calculateEST_EFT(int parent) {
		for (int child = 0; child < totalTasks; child++) {
			if (graph[parent][child] && !isVisited[child]) {				
				if (EST[child] < EFT[parent] + transferTime[parent][child]) {
					EST[child] = EFT[parent] + transferTime[parent][child];
					EFT[child] = EST[child] + findMinimumExecution(child);
					calculateEST_EFT(child);
				}
			}
		}
	}

	void calculateLFT(int child) {
		for (int parent = 0; parent < totalTasks; parent++) {
			if (graph[parent][child] && !isVisited[parent]) {
				if (LFT[parent] > LFT[child] -  findMinimumExecution(child) - transferTime[parent][child]) {
					LFT[parent] = LFT[child] -  findMinimumExecution(child) - transferTime[parent][child];
					calculateLFT(parent);
				}
			}
		}
	}

	private double findMinimumExecution(int task) {
		if (isVisited[task]) {
			return execTime.get(taskAllocationMap[task] + "").get(task);
		}
		double minimumExecution = Integer.MAX_VALUE;
		for (int vm = 0; vm < totalVMs; vm++) {
			if (minimumExecution > execTime.get(vm + "").get(task)) {
				minimumExecution = execTime.get(vm + "").get(task);
			}
		}
		return minimumExecution;
	}

	private void preprocessing() {
		totalTasks = w.getNumOfTasks();

		/*
		 * Initializing isSourceNode and isDestinationNode
		 */
		for (int i = 1; i <= totalTasks; i++) {
			isSourceNode.put(i, true);
			isDestinationNode.put(i, true);
		}

		/* 
		 * Since we added two extra tasks, 'start', 'end', we added with 2; Therefore we added by 1 with each adjacency task index so simulate
		 * start node starts with zero and end index start with totalTasks;
		 */

		/*
		 * Constructing graph
		 */
		totalTasks += 2;
		graph = new boolean[totalTasks][totalTasks];

		/*
		 * Constructing Transfer time between tasks
		 */
		//TODO
		//		transferTime = new double[totalTasks][totalTasks];


		/*
		 * iterating each of the adjacency list
		 */
		for (int i = 0; i < alist.length; i++) {
			List<Integer> dest = alist[i];
			for (int j = 0; j < dest.size(); j++) {
				Dataview.debugger.logSuccessfulMessage("source : " + (i + 1) + " destination : " + (dest.get(j) + 1) );
				graph[(i + 1)][dest.get(j) + 1] = true;

				//TODO Lets assume we have fixed number of transfer time for this experiment
				//				transferTime[(i + 1)][dest.get(j) + 1] = 5;

				isSourceNode.put(dest.get(j) + 1, false);
				isDestinationNode.put(i + 1, false);
			}
		}

		/*
		 * finding source Nodes
		 */
		ArrayList<Integer> sourceNodes = returnIndices(isSourceNode);

		/*
		 * finding destination Nodes
		 */
		ArrayList<Integer> destinationNodes = returnIndices(isDestinationNode);

		/*
		 * Now making connection between start node to source nodes
		 */
		for (int i = 0; i < sourceNodes.size(); i++) {
			Dataview.debugger.logSuccessfulMessage("source Node : " + sourceNodes.get(i));
			graph[0][sourceNodes.get(i)] = true;
			transferTime[0][sourceNodes.get(i)] = 0;
		}

		/*
		 * Now making connection between destination node to end nodes
		 */
		for (int i = 0; i < destinationNodes.size(); i++) {
			Dataview.debugger.logSuccessfulMessage("destination Node : " + destinationNodes.get(i));
			graph[destinationNodes.get(i)][totalTasks - 1] = true;
			transferTime[destinationNodes.get(i)][totalTasks - 1] = 0;
		}

		/*
		 * Registering execution time
		 */

		/*
		 * This is for fixed execution time of ConcreteWorkflowOne, we considered we have only three different virtual machine
		 */
		execTime = new HashMap<String, HashMap<Integer, Double>>();
		for (int i = 0; i < 3; i++) {
			HashMap<Integer, Double> et = new HashMap<Integer, Double>();
			if (i == 0) {
				et.put(0, 0d); et.put(1, 4d); et.put(2, 3d); et.put(3, 3d); et.put(4, 8d); et.put(5, 4d); et.put(6, 10d); et.put(7, 3d); et.put(8, 25d); et.put(9, 7d); et.put(10, 18d); et.put(totalTasks - 1, 0d);
			} else if (i == 1) {
				et.put(0, 0d); et.put(1, 10d); et.put(2, 6d); et.put(3, 6d); et.put(4, 12d); et.put(5, 9d); et.put(6, 15d); et.put(7, 7d); et.put(8, 36d); et.put(9, 10d); et.put(10, 20d); et.put(totalTasks - 1, 0d);
			} else {
				et.put(0, 0d); et.put(1, 16d); et.put(2, 10d); et.put(3, 9d); et.put(4, 20d); et.put(5, 12d); et.put(6, 18d); et.put(7, 11d); et.put(8, 50d); et.put(9, 16d); et.put(10, 25d); et.put(totalTasks - 1, 0d);
			}

			execTime.put(i + "", et);
		}

		/* this variable keeps track about visiting*/
		isVisited = new boolean[totalTasks];


		//TODO/*These are hardcoded confidential tasks*/
		//		confidentialTasks.add(-1);

		/*TaskAllocation to vm mapping initialization
		 */

		taskAllocationMap = new int[totalTasks];
		Arrays.fill(taskAllocationMap, -1);
	}

	ArrayList<Integer> returnIndices(HashMap<Integer, Boolean> map) {
		ArrayList<Integer> indices = new ArrayList<Integer>();
		Iterator<?> it = map.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry)it.next();
			if ((boolean)pair.getValue()) {
				indices.add((Integer) pair.getKey());
			}
		}
		return indices;
	}
}
