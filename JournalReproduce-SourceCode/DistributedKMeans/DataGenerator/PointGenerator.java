import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Random;


public class PointGenerator {
	public static void main(String[] args) {
		new PointGenerator().init();
	}

	public int getRandomNumberUsingNextInt(int min, int max) {
		Random random = new Random();
		return random.nextInt(max - min) + min;
	}

	public void init() {
		int total = 100;
		BufferedWriter bw = null;
		FileWriter fw = null;
		String fileName = "twoD_points.txt";
		try {			
			fw = new FileWriter(fileName);
			bw = new BufferedWriter(fw);
			for (int i = 0; i < total; i++) {				
				bw.write(getRandomNumberUsingNextInt(0, 1000) + " " + getRandomNumberUsingNextInt(0, 1000) + "\n");				
			}
			bw.close();
			fw.close();
			System.out.println(fileName + " write is completed");
		} catch (Exception exception) {
			System.out.println("Exception " + exception);
		}
	}
}


