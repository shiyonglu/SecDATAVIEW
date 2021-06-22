import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;


public class FileGenerator {
	public static void main(String[] args) {
		new FileGenerator().init();
	}
	int sizePatients = 100000;
	String fileName = "/home/ishtiaq/Dropbox/aaaaaa-Saeid-Ishtiaq/usenix19-results/diagnosis-test/" + sizePatients + "/originalInput_" + sizePatients + ".txt";
	
	
	int age[][] = {
			{11, 20},
			{21, 30},
			{31, 40},
			{41, 50},
			{51, 60},
			{61, 70},
			{71, 80},
			{81, 90},
			{91, 100}
	};
	int heartRate[][] = {
			{101, 110},
			{91, 100},
			{81, 90},
			{71, 80},
			{61, 70},
			{51, 60},
			{41, 50},
			{31, 40},
			{21, 30}
	};
	int glucose[][] = {
			{201, 210},
			{191, 200},
			{181, 190},
			{171, 180},
			{161, 170},
			{151, 160},
			{141, 150},
			{131, 140},
			{121, 130}
	};

	boolean[] isSmoke = {false, false, true, true, true, true, false, false, false};
	boolean[] isObese = {false, false, true, true, true, true, true, false, false};

	int[][] diagnosis = {
			{1,  10},
			{11,  20},
			{21, 30},
			{31, 40},
			{41, 50},
			{51, 60},
			{61, 70},
			{71, 80},
			{81, 90}
			
	};


	void init() {
		for (sizePatients = 10000; sizePatients <= 100000; sizePatients += 10000) {
			fileName = "/home/ishtiaq/Dropbox/aaaaaa-Saeid-Ishtiaq/usenix19-results/diagnosis-test/" + sizePatients + "/originalInput_" + sizePatients + ".txt";
			process();
		}
	}

	void process() {
		Random random = new Random();
		ArrayList<PatientDetails> patientList = new ArrayList<PatientDetails>();
		for (int group = 0; group < 9; group++) {
			for (int instance = 0; instance < (sizePatients / 9) + 1; instance++) {
				PatientDetails patient = new PatientDetails();
				patient.setAge(random.nextInt(age[group][1] + 1 - age[group][0]) + age[group][0]);
				patient.setGlucose(random.nextInt(glucose[group][1] + 1 - glucose[group][0]) + glucose[group][0]);
				patient.setHeartRate(random.nextInt(heartRate[group][1] + 1 - heartRate[group][0]) + heartRate[group][0]);
				patient.setSmoke(isSmoke[group]);
				patient.setObese(isObese[group]);
//				int noOfDiagnosisLabels = random.nextInt(3 + 1 - 1) + 1; // upper limit 3, lower limit 1
				ArrayList<String> diagnosisLabels = new ArrayList<String>();
				while(diagnosisLabels.size() < 5) {
					String diagnosisLabel =random.nextInt(diagnosis[group][1] + 1 - diagnosis[group][0]) + diagnosis[group][0] + "";
					if (!diagnosisLabels.contains(diagnosisLabel)) {
						diagnosisLabels.add(diagnosisLabel);
					}
				}
				patient.setDiagnosisLabels(diagnosisLabels);
				patientList.add(patient);
//				System.out.println(patientList.get(instance).getDiagnosisLabels());
			}
		}
		writefile(fileName, patientList);
	}




	private void writefile(String fileName, ArrayList<PatientDetails> patientList) {
		BufferedWriter bw = null;
		FileWriter fw = null;

		try {
			fw = new FileWriter(fileName);
			bw = new BufferedWriter(fw);
			Collections.shuffle(patientList);
			int i = 1;
			int size = patientList.size();
			for (PatientDetails patientDetails : patientList) {
				bw.write("Patient is " + patientDetails.getAge() + " years old. ");
				bw.write("HeartRate is " + patientDetails.getHeartRate() + ". ");
//				patientDetails.isObese() ? "yes" : "no"
				if(patientDetails.isObese) {
					bw.write("Patient has obesity. ");
				} else {
					bw.write("Patient does not have obesity. ");
				}
				if (patientDetails.isSmoke()) {
					bw.write("Patient is smoker. ");
				} else {
					bw.write("Patient is not smoker. ");
				}
				bw.write("Glucose is " + patientDetails.getGlucose() + "");
				bw.write("This patient has these diagnosis list : ");
				for (String diagnosisLabel : patientDetails.diagnosisLabels) {
					bw.write(diagnosisLabel + "_");
				}
				if (i < sizePatients) {
					bw.write("\n");
				} else if (i == sizePatients) {
					break;
				}
				i++;
			}
			bw.close();
			fw.close();
			System.out.println(fileName + " write is completed");
		} catch (Exception exception) {
			System.out.println("Exception " + exception);
		}
	}
}
