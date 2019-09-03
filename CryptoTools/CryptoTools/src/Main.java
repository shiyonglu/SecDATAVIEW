import java.io.File;

public class Main {
	public static void main(String[] args) {
		// arg0 = mode
		// arg1 = secret_key
		// arg2 = associated_data
		//arg3 = source_folder_location
		//arg4 = destination_folder_location
		
		
		
		if (args.length < 5) {
			System.out.println("Insufficient arguments");
			System.out.println("ARG0 : mode\nARG1 : secret_Key\nARG2 : associated_data\nARG3 : source_folder_location\nARG4: destination_folder_location\n");
			return;
		}
		
		String mode = args[0];
		String secretKey = args[1];
		String associatedData = args[2];
		String sourceFolderDirectory = args[3];
		String destinationFolderDirectory = args[4];
		final File sourceFolder =  new File(sourceFolderDirectory);
		

		File[] listOfFiles = sourceFolder.listFiles();

		DWCRYPTO dwcrypto = new DWCRYPTO();
		
		
		if (mode.equalsIgnoreCase("enc") || mode.equalsIgnoreCase("0")) {
			
			for (File file : listOfFiles) {
			    if (file.isFile()) {
			        String encryptedFileName = destinationFolderDirectory + "/" + file.getName() + ".enc";
					if (dwcrypto.encryptFile(encryptedFileName, associatedData, file.getAbsolutePath(), secretKey) == 0) {
						System.out.println("Encryption is completed for " + file.getName());
					} else {
						System.out.println("Encryption is not completed for " + file.getName());
					}
			    }
			}
			
		} else {
			for (File file : listOfFiles) {
			    if (file.isFile()) {
			        String plainFileName = destinationFolderDirectory + "/" + file.getName().replaceFirst(".enc", "");
					if (dwcrypto.decryptFile(file.getAbsolutePath(), plainFileName, associatedData, secretKey) == 0) {
						System.out.println("Decryption is completed for " + file.getName());
					} else {
						System.out.println("Decryption is not completed for " + file.getName());
					}
			    }
			}
		}

		
		
	}
}
