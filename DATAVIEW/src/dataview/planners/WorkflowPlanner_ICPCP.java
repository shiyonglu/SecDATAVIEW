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

public class WorkflowPlanner_ICPCP extends WorkflowPlanner{
	private double deadline = 30;
	private int totalVMs = 3;
	private double billingCycle = 10;
	private int totalTasks;


	private final double INF = Integer.MAX_VALUE;
	private final double MIN = Integer.MIN_VALUE; 
	private final boolean FAL = false;
	private final boolean TRU = true;

	//TODO fixed for Paper workflow
	double[][] transferTime = {
			// 0   1     2      3      4       5      6      7     8      9      10
			{INF, 0,     0,     0,     INF,  INF,   INF,   INF, INF,   INF,   INF},// 0
			{INF, INF,   INF,   INF,   1,    INF,   INF,   INF, INF,   INF,   INF}, //1
			{INF, INF,   INF,   INF,   INF,  2,     2,     INF, INF,   INF,   INF}, //2
			{INF, INF,   INF,   INF,   INF,  INF,   2,     INF, INF,   INF,   INF}, //3
			{INF, INF,   INF,   INF,   INF,  INF,   INF,   1,   1,     INF,   INF}, //4
			{INF, INF,   INF,   INF,   INF,  INF,   INF,   INF, 4,     INF,   INF}, //5
			{INF, INF,   INF,   INF,   INF,  INF,   INF,   INF, INF,   3,     INF}, //6
			{INF, INF,   INF,   INF,   INF,  INF,   INF,   INF, INF,   INF,   0  }, //7
			{INF, INF,   INF,   INF,   INF,  INF,   INF,   INF, INF,   INF,   0  }, //8
			{INF, INF,   INF,   INF,   INF,  INF,   INF,   INF, INF,   INF,   0  }, //9
			{INF, INF,   INF,   INF,   INF,  INF,   INF,   INF, INF,   INF,   INF}, //10


	};

	private boolean[][] graph = {
			// 0   1     2      3      4       5      6      7     8      9      10
			{FAL, TRU,   TRU,   TRU,   FAL,  FAL,   FAL,   FAL, FAL,   FAL,   FAL},// 0
			{FAL, FAL,   FAL,   FAL,   TRU,  FAL,   FAL,   FAL, FAL,   FAL,   FAL}, //1
			{FAL, FAL,   FAL,   FAL,   FAL,  TRU,   TRU,   FAL, FAL,   FAL,   FAL}, //2
			{FAL, FAL,   FAL,   FAL,   FAL,  FAL,   TRU,   FAL, FAL,   FAL,   FAL}, //3
			{FAL, FAL,   FAL,   FAL,   FAL,  FAL,   FAL,   TRU, TRU,   FAL,   FAL}, //4
			{FAL, FAL,   FAL,   FAL,   FAL,  FAL,   FAL,   FAL, TRU,   FAL,   FAL}, //5
			{FAL, FAL,   FAL,   FAL,   FAL,  FAL,   FAL,   FAL, FAL,   TRU,   FAL}, //6
			{FAL, FAL,   FAL,   FAL,   FAL,  FAL,   FAL,   FAL, FAL,   FAL,   TRU}, //7
			{FAL, FAL,   FAL,   FAL,   FAL,  FAL,   FAL,   FAL, FAL,   FAL,   TRU}, //8
			{FAL, FAL,   FAL,   FAL,   FAL,  FAL,   FAL,   FAL, FAL,   FAL,   TRU}, //9
			{FAL, FAL,   FAL,   FAL,   FAL,  FAL,   FAL,   FAL, FAL,   FAL,   FAL}, //10
	};

	/*
	 * first one is the fastest machine while the last one is the cheapest.
	 */
	double[] costPerBillingCycle = {5, 2, 1}; 
	int[] taskAllocationMap;
	double[] EST;
	double[] EFT;
	double[] LFT;


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
		new WorkflowPlanner_ICPCP(null).plan();
	}

	public WorkflowPlanner_ICPCP(Workflow workflow) {
		//super(workflow);
	}

	public GlobalSchedule plan() {
		Dataview.debugger.logSuccessfulMessage("SGX-E2C2D is started");
		scheduleWorkflow();
//		assignChildren();
		assignParent(totalTasks - 1);
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

	private void assignParent(int task) {
		while(hasUnassignedParent(task)) {
			ArrayList<Integer> pcp = new ArrayList<Integer>();
			int task_i = task;
			while(hasUnassignedParent(task_i)) {
				int criticalP = criticalParent(task_i); 
				pcp.add(criticalP);
				task_i = criticalP;
			}
			Collections.reverse(pcp);
			int a = 2;
			if (pcp.get(0) == 7) {
				a++;
			}
			allocateVirtualMachine(pcp);
			for (int i = 0; i < pcp.size(); i++) {
				isVisited[pcp.get(i)] = true;
			}
			for (int i = 0; i < pcp.size(); i++) {
				calculateEST_EFT(pcp.get(i));
				calculateLFT(pcp.get(i));
				assignParent(pcp.get(i));
			}
		}
	}

	private Integer criticalParent(int child) {
		int criticalP = -1;
		double max = Integer.MIN_VALUE;
		for(int parent = 0; parent < totalTasks; parent++) {
			if (graph[parent][child] && !isVisited[parent]) {
				if (max < EFT[parent] + transferTime[parent][child]) {
					max = EFT[parent] + transferTime[parent][child];
					criticalP = parent;
				}
			}
		}
		return criticalP;
	}

	private boolean hasUnassignedParent(int child) {
		for (int parent = 0; parent < totalTasks; parent++) {
			if (graph[parent][child] && !isVisited[parent]) {
				return true;
			}
		}
		return false;
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
		boolean isonlyParent = true;
		VMI backupVmi = new VMI();
		if (isOnlyParent(vmi1, vmi2)) {
			backupVmi = vmi2;
			isonlyParent = true;
			vmi2.EST[vmi2.path.get(0)] = vmi1.EFT[vmi1.path.get(vmi1.path.size() - 1)];
		} else {
			return null;
		}
		
		if (vmi1.EFT[vmi1.path.get(vmi1.path.size() - 1)] <= vmi2.EST[vmi2.path.get(0)]) {
			double startTime = vmi2.EST[vmi2.path.get(0)];
			boolean canFit = true;
			for(int i = 0; i < vmi2.path.size(); i++) {
				if (vmi2.LFT[vmi2.path.get(i)] >= startTime + execTime.get(vmi2.assignedVMIndex + "").get(vmi2.path.get(i))) {
					startTime = startTime + execTime.get(vmi1.assignedVMIndex + "").get(vmi1.path.get(0));
				} else {
					canFit = false;
					break;
				}
			}
			
			if (canFit) {
				VMI instance = new VMI();
				instance.EST = EST.clone();
				instance.EFT = EFT.clone();
				instance.LFT = LFT.clone();
				instance.assignedVMIndex = vmi1.assignedVMIndex;
				instance.previous = vmi1;
				instance.path = new ArrayList<Integer>();
				for (Integer t : vmi1.path) {
					instance.path.add(t);
					instance.EST[t] = vmi1.EST[t];
					instance.EFT[t] = vmi1.EFT[t];
					instance.LFT[t] = vmi1.LFT[t];
				}
				
				
				for (Integer t : vmi2.path) {
					instance.path.add(t);
					instance.EST[t] = vmi2.EST[t];
					instance.EFT[t] = vmi2.EFT[t];
					instance.LFT[t] = vmi2.LFT[t];
				}
				startTime = vmi2.EST[vmi2.path.get(0)];
				int i = 0;
				for (Integer t : vmi2.path) {
					
					if (vmi2.LFT[t] < startTime + execTime.get(vmi2.assignedVMIndex + "").get(vmi2.path.get(i++))) {
						instance.EST[t] = startTime; 
						instance.EFT[t] = startTime + execTime.get(vmi1.assignedVMIndex + "").get(t);
						startTime = instance.EFT[t];
					}
				}
				
				double finishTime = vmi2.LFT[vmi2.path.get(vmi2.path.size() - 1)];
				for (i = vmi2.path.size() - 1; i >= 0; i--) {
					instance.LFT[vmi2.path.get(i)] = finishTime;
					finishTime -= execTime.get(vmi2.assignedVMIndex + "").get(vmi2.path.get(i));	
				}
				instance.cost = findCost(instance);
				instance.additionalCost = instance.cost - vmi1.cost;
				return instance;
			}
		}
		if (isonlyParent) {
			// old assignment
			vmi2 = backupVmi;
		}
		return null;
	}


	private boolean isOnlyParent(VMI vmi1, VMI vmi2) {
		int count = 0;
		boolean parentMatch = false;
		int lastTaskVmi1 = vmi1.path.get(vmi1.path.size() - 1);
		int firstTaskvmi2 = vmi2.path.get(0);
		for (int parent = 1; parent < totalTasks -1; parent++) {
			if (graph[parent][firstTaskvmi2]) {
				if (lastTaskVmi1 == parent) {
					parentMatch = true;
				}
				count++;
			}
		}
		return (parentMatch && count == 1) ? true : false;
	}

	private double findCost(VMI instance) {
		double totalExecution = instance.EFT[instance.path.get(instance.path.size() - 1)] - instance.EST[instance.path.get(0)];
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
		totalTasks = 9;

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
//		graph = new boolean[totalTasks][totalTasks];

		/*
		 * Constructing Transfer time between tasks
		 */
		//TODO
		//		transferTime = new double[totalTasks][totalTasks];


		
		for (int i = 1; i <= totalTasks; i++) {
			if (i != 1 && i != 2 && i != 3) {
				isSourceNode.put(i, false);
			}
			if (i != 7 && i != 8 && i != 9) {
				isDestinationNode.put(i, false);
			}
		}
		
		

		/*
		 * Registering execution time
		 */

		/*
		 * This is for fixed execution time of ConcreteWorkflowOne, we considered we have only three different virtual machine
		 */
		//TODO  new HashMap<Integer(index of task), HashMap<String(vm type), Double(execution time)) 
		execTime = new HashMap<String, HashMap<Integer, Double>>();
		for (int i = 0; i < 3; i++) {
			HashMap<Integer, Double> et = new HashMap<Integer, Double>();
			/*if (i == 0) {
				et.put(0, 0d); et.put(1, 2d); et.put(2, 5d); et.put(3, 3d); et.put(4, 4d); et.put(5, 3d); et.put(6, 4d); et.put(7, 5d); et.put(8, 3d); et.put(9, 5d); et.put(10, 0d);
			} else if (i == 1) {
				et.put(0, 0d); et.put(1, 5d); et.put(2, 12d); et.put(3, 5d); et.put(4, 6d); et.put(5, 8d); et.put(6, 8d); et.put(7, 8d); et.put(8, 6d); et.put(9, 8d); et.put(10, 0d);
			} else {
				et.put(0, 0d); et.put(1, 8d); et.put(2, 16d); et.put(3, 9d); et.put(4, 10d); et.put(5, 11d); et.put(6, 11d); et.put(7, 11d); et.put(8, 8d); et.put(9, 14d); et.put(10, 0d);
			}*/
			if (i == 0) {
				et.put(0, 0d); et.put(1, 2d); et.put(2, 5d); et.put(3, 3d); et.put(4, 4d); et.put(5, 3d); et.put(6, 4d); et.put(7, 5d); et.put(8, 3d); et.put(9, 5d); et.put(10, 0d);
			} else if (i == 1) {
				et.put(0, 0d); et.put(1, 5d); et.put(2, 12d); et.put(3, 5d); et.put(4, 6d); et.put(5, 8d); et.put(6, 8d); et.put(7, 8d); et.put(8, 6d); et.put(9, 8d); et.put(10, 0d);
			} else {
				et.put(0, 0d); et.put(1, 8d); et.put(2, 16d); et.put(3, 9d); et.put(4, 10d); et.put(5, 11d); et.put(6, 11d); et.put(7, 11d); et.put(8, 8d); et.put(9, 14d); et.put(10, 0d);
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
