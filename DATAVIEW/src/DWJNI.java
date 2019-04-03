import java.lang.reflect.Field;
public class DWJNI {
			
	static {
			
		try {
			
    	
			System.loadLibrary("jnibridge");// Load native library at runtime
			// Name should follow the name format ==>  jnibridge.dll (Windows) or libjnibridge.so (Unixes)
			
		} catch (UnsatisfiedLinkError e) {

		  System.err.println("DATAVIEW Native code library failed to load.\n" + e);
		  System.exit(1);

		}
	}
	 
	   // Declare native methods signature where the definition will be in the jnibridge. 
	   public native int jniAlgorithmOne(String in1LabeledDataset,String in2UnLabledDataset,String oututFileName, String taskId);// in1 and in2 are name of encrypted files
	   
	   
	// JNI Algorithm 1 is calling the quicksort for random workflow
	   
	/*public static void main(String[] args) {
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		
		double avg = 0;
		double totalSum = 0;
		DWJNI vm1 = null;
		int totalIteration = 1;
		for (int i = 0; i < totalIteration; i++) {
			vm1 = new DWJNI();
			double startTime = System.currentTimeMillis();
			int returnStatus = vm1.jniAlgorithmOne("labeled.txt", "unlabeled.txt","output.txt", "Alg1c");
			if (returnStatus != 0) {
				System.out.println("Exception is generated in JNI");
				break;
			}
			double endTime = System.currentTimeMillis();

			double totalExecutionTimeInMinutes= (endTime - startTime);
			if (totalExecutionTimeInMinutes < min) {
				min = totalExecutionTimeInMinutes;
			}
			if (totalExecutionTimeInMinutes > max) {
				max = totalExecutionTimeInMinutes;
			}
			totalSum += totalExecutionTimeInMinutes;
			System.out.println("Total execution time in miliseconds : " + totalExecutionTimeInMinutes);
			System.out.println("In iteration " + (i) + " total sum is " + totalSum);
			vm1 = null;
		} 
		avg = totalSum / totalIteration;
		System.out.println("Average execution time in miliseconds : " + avg);
		System.out.println("Minimum execution time is " + min + " and maximum execution time is " + max);
	}*/


}