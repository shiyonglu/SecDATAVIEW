import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import dataview.models.Dataview;
import dataview.models.InputPort;
import dataview.models.OutputPort;
import dataview.models.Task;
import weka.clusterers.SimpleKMeans;
import weka.core.Instances;

public class Algorithm1 extends Task {

	public Algorithm1() {
		super("Algorithm1",
				"This is the first algorithm. It has three inputports and one outputport.");
		ins = new InputPort[3];
		outs = new OutputPort[1];
		ins[0] = new InputPort("in0", Dataview.datatype.DATAVIEW_String, "This is the first input");
		ins[1] = new InputPort("in1", Dataview.datatype.DATAVIEW_String, "This is the second input");
		ins[2] = new InputPort("in2", Dataview.datatype.DATAVIEW_String, "This is the third input");
		// ins[1] = new InputPort("2in", "Integer", "This is the second
		// number");
		outs[0] = new OutputPort("out0", Dataview.datatype.DATAVIEW_String, "This is the output");
	}

	@Override
	public void run(){
		// TODO Auto-generated method stub
		
		String input0 = (String) ins[0].read1();
		String input1 = (String) ins[1].read1();
//		Double.parseDouble(ins[2].read1());
//		Double input2 = ((Double) ins[2].read1()).doubleValue();
		Double input2 = Double.parseDouble((String) ins[2].read1());
		
		
		SimpleKMeans model = constructModel(input0);
		ArrayList<ArrayList<String>> diagnosisLabels = null;
		try {
			diagnosisLabels = readDiagnosisLabels(input0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		HashMap<Integer, ClusterInfo> hashMap = new HashMap<Integer, ClusterInfo>();
	
		try {
			int[] assignments = model.getAssignments();
			for (int i = 0; i < assignments.length; i++) {
				ClusterInfo clusterInfo;
				if (hashMap.containsKey(assignments[i])) {
					clusterInfo = hashMap.get(assignments[i]);
				} else {
					clusterInfo = new ClusterInfo();
					clusterInfo.clusterNo = assignments[i];
				}

				for(String label : diagnosisLabels.get(i)) {
					if (clusterInfo.hMap.containsKey(label)) {
						clusterInfo.hMap.put(label, clusterInfo.hMap.get(label) + 1);
					} else {
						clusterInfo.hMap.put(label, 1);
					}
				}
				clusterInfo.clusterSize++;
				hashMap.put(assignments[i],clusterInfo);
			}
		} catch (Exception exception) {
			System.out.println("Exception algorithm1 : " + exception);
		}
		Iterator<?> iteratorHashMap = hashMap.entrySet().iterator();
		while(iteratorHashMap.hasNext()) {
			@SuppressWarnings("rawtypes")
			Map.Entry pair = (Map.Entry)iteratorHashMap.next();
			ClusterInfo clusterInfo = (ClusterInfo) pair.getValue();
			Iterator<?> iteratorHMap = clusterInfo.hMap.entrySet().iterator();
			ArrayList<SortedDiagnosis> sortedDiagnosisList = new ArrayList<SortedDiagnosis>();
			
			
			while(iteratorHMap.hasNext()) {
				@SuppressWarnings("rawtypes")
				Map.Entry pairInternal = (Map.Entry)iteratorHMap.next();
				String key = (String)pairInternal.getKey();
				int value = (Integer)pairInternal.getValue();
				if(value >= (int)( input2 * clusterInfo.clusterSize)) {
					SortedDiagnosis sortedDiagnosis = new SortedDiagnosis(key, value);
					sortedDiagnosisList.add(sortedDiagnosis);
				}
			}
			Collections.sort(sortedDiagnosisList);
			for (SortedDiagnosis sortedDiagnosis : sortedDiagnosisList) {
				clusterInfo.recommendedDiagnosis.add(sortedDiagnosis.diagnosis);
			}
//			
		}
		//labelingUnlabeledInstances(hashMap, fileUnlabeledDataset, model, "bPrime.arff");
		ArrayList<ArrayList<String>> recommendedDiagnosis = new ArrayList<ArrayList<String>>();
		String content = "@relation dataset\n@attribute age numeric\n"
				+ "@attribute heartRate numeric\n@attribute isObesity {No, Yes}\n@attribute isSmoke {No, Yes}\n"
				+ "@attribute glucose numeric\n@data\n";
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(content);
		
		InputStream inputStream = new ByteArrayInputStream(input1.getBytes(Charset.forName("UTF-8")));
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
		
		Instances testInstance = null;
		try {
			testInstance = new Instances(bufferedReader);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		for (int i = 0; i < testInstance.numInstances(); i++) {
			int clusterNo = 0;
			try {
				clusterNo = model.clusterInstance(testInstance.instance(i));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			recommendedDiagnosis.add(hashMap.get(clusterNo).recommendedDiagnosis);
//			System.out.println("Predicted cluster no. : " + clusterNo);
			// Writing attribute
			for (int j = 0; j < testInstance.numAttributes(); j++) {
				String type = testInstance.instance(i).attribute(j) + ""; 
				if (type.contains("numeric")) {
//					System.out.println(test.instance(i).value(j));
					stringBuilder.append(testInstance.instance(i).value(j)+",");
				} else {
					stringBuilder.append(testInstance.instance(i).stringValue(j)+",");
				}
			}
			
			stringBuilder.append("\"");
			for(int k = 0; k < hashMap.get(clusterNo).recommendedDiagnosis.size(); k++) {
				stringBuilder.append(hashMap.get(clusterNo).recommendedDiagnosis.get(k));
				if (k < hashMap.get(clusterNo).recommendedDiagnosis.size() - 1) {
					stringBuilder.append("_");
				} 
			}
			stringBuilder.append("\n");
			
		}
		String output0 = stringBuilder.toString(); 
		
		outs[0].write1(output0);	
			
	}
	
	class SortedDiagnosis implements Comparable <SortedDiagnosis> {
		String diagnosis;
		int frequency;
		@Override
		public int compareTo(SortedDiagnosis sortedDiagnosis) {
			return sortedDiagnosis.frequency - this.frequency;
		}
		
		public SortedDiagnosis(String diagnosis, int frequency) {
			this.diagnosis = diagnosis;
			this.frequency = frequency;
		}
	}

	
	class ClusterInfo {
		double clusterSize;
		int clusterNo;
		HashMap<String, Integer> hMap = new HashMap<String, Integer>();
		ArrayList<String> recommendedDiagnosis = new ArrayList<String>();
	}

	

	private ArrayList<ArrayList<String>> readDiagnosisLabels(String a) throws IOException {
		// TODO Auto-generated method stub
			ArrayList<ArrayList<String>> diagnosisLabels = new ArrayList<ArrayList<String>>();
			InputStream inputStream = new ByteArrayInputStream(a.getBytes(Charset.forName("UTF-8")));
			BufferedReader bufferedReader1 = new BufferedReader(new InputStreamReader(inputStream));
			String line;
			boolean isDataFound = false;
				while ((line = bufferedReader1.readLine()) != null) {
					if (line.equals("@data")) {
						isDataFound = true;
						continue;
					}
					if(isDataFound) {
						line = line.split("\"")[1];
						ArrayList<String> labels = new ArrayList<String>(Arrays.asList(line.split("_")));
						diagnosisLabels.add(labels);
					}
				}
			
			return diagnosisLabels;
		
	}

	private SimpleKMeans constructModel(String a) {
		// TODO Auto-generated method stub
		int totalNumberOfSeeds = 20;
		int totalNumberOfClusters = 9;
		SimpleKMeans simpleKMeans = new SimpleKMeans();
		try {
			InputStream inputStream = new ByteArrayInputStream(a.getBytes(Charset.forName("UTF-8")));
			BufferedReader bufferedReader1 = new BufferedReader(new InputStreamReader(inputStream));
			Instances train = new Instances(bufferedReader1);
			
			bufferedReader1.close();
//			System.out.println("Number of attribute " + train.numAttributes());
//			System.out.println(train.get(4).attribute(train.numAttributes()));
			simpleKMeans.setSeed(totalNumberOfSeeds);
			simpleKMeans.setPreserveInstancesOrder(true);
			simpleKMeans.setNumClusters(totalNumberOfClusters);
			//			train.setClassIndex(train.numAttributes() - 1);
			simpleKMeans.buildClusterer(train);
		} catch (Exception exception) {
			System.out.println("Exception constructCluster : " + exception);
		}
		return simpleKMeans;
	}
	 class PatientDetails {
		int age;
		int heartRate;
		int glucose;
		boolean isSmoke;
		boolean isObese;
		ArrayList<String> diagnosisLabels;
		public int getAge() {
			return age;
		}
		public void setAge(int age) {
			this.age = age;
		}
		public int getHeartRate() {
			return heartRate;
		}
		public void setHeartRate(int heartRate) {
			this.heartRate = heartRate;
		}
		public int getGlucose() {
			return glucose;
		}
		public void setGlucose(int glucose) {
			this.glucose = glucose;
		}
		public boolean isSmoke() {
			return isSmoke;
		}
		public void setSmoke(boolean isSmoke) {
			this.isSmoke = isSmoke;
		}
		public boolean isObese() {
			return isObese;
		}
		public void setObese(boolean isObese) {
			this.isObese = isObese;
		}
		public ArrayList<String> getDiagnosisLabels() {
			return diagnosisLabels;
		}
		public void setDiagnosisLabels(ArrayList<String> diagnosisLabels) {
			this.diagnosisLabels = diagnosisLabels;
		}
		
	}

}