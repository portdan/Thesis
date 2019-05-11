package problemGenerator.FileGenerator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class FileGenerator {

	private static final double KILO_BYTE = (Math.pow(2, 10));
	private static final double MEGA_BYTE = (Math.pow(2, 20));

	//private static final double BUFFER_SIZE = 4 * MEGA_BYTE;
	private static final double BUFFER_SIZE = 8 * KILO_BYTE;

	public File outFile = null;
	public BufferedWriter writer = null;

	public void generateFile(String folderPath, String fileName, String fileType) throws IOException {

		outFile = new File(folderPath + "/" + fileName + "." + fileType);

		if (!outFile.exists()) {

			new File(folderPath).mkdirs();
			outFile.createNewFile();

		} else
			clearFile();
	}

	/*
	private void appendToFile(List<String> strings) throws IOException{

		writer = new BufferedWriter(new FileWriter(outFile, true), (int)BUFFER_SIZE);

		for (String str: strings) {
			writer.append(str);
		}

		writer.flush();
		writer.close();
	}

	public void appendToFile(String str) throws IOException{

		if (outFile != null) {

			writer = new BufferedWriter(new FileWriter(outFile, true), (int)BUFFER_SIZE);
			writer.append(str);
			writer.flush();
			writer.close();
		} 
	}
	*/

	public void writeToFile(List<String> strings) throws IOException{

		if (outFile != null) {
						
			writer = new BufferedWriter(new FileWriter(outFile, true), (int)BUFFER_SIZE);
			
			for (String str: strings) {
				writer.write(str);
			}

			writer.flush();
			writer.close();
		} 
	}
	
	public void writeToFile(String str) throws IOException{

		if (outFile != null) {

			writer = new BufferedWriter(new FileWriter(outFile, true), (int)BUFFER_SIZE);
			writer.write(str);
			writer.flush();
			writer.close();
		} 
	}


	public void clearFile() throws IOException{

		writeToFile("");
	}
}