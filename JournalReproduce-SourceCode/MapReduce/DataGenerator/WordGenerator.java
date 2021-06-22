import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Random;


public class WordGenerator {
	public static void main(String[] args) {
		new WordGenerator().init();
	}

	public String getRandomTwoLengthWord() {
		Random random = new Random();
		char first = (char)(random.nextInt(25) + 'a');
		char second = (char)(random.nextInt(25) + 'a');
		char[] chars = {first, second};
		return  String.valueOf(chars);
	}

	public void init() {
		int total = 100;
		BufferedWriter bw = null;
		FileWriter fw = null;
		String fileName = "words.txt";
		try {			
			fw = new FileWriter(fileName);
			bw = new BufferedWriter(fw);
			for (int i = 0; i < total; i++) {				
				bw.write(getRandomTwoLengthWord() + "\n");				
			}
			bw.close();
			fw.close();
			System.out.println(fileName + " write is completed");
		} catch (Exception exception) {
			System.out.println("Exception " + exception);
		}
	}
}


