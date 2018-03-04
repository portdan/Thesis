package problem_generator.file_generator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class FileGenerator {

	public File outFile = null;
	public PrintWriter writer = null;

	public boolean generateFile(String folderPath, String fileName, String fileType) {

		outFile = new File(folderPath+fileName+fileType);

		if (!outFile.exists()) {
			try {
				outFile.createNewFile();
			} 
			catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}

		return true;
	}

	public boolean WriteToFile(String str) {

		if(outFile!=null) {

			try {
				writer = new PrintWriter(new BufferedWriter(new FileWriter(outFile,true)));
				writer.write(str + "\n");
				writer.flush();
				writer.close();
			} 
			catch (IOException ex){
				ex.printStackTrace();

				if(writer!=null) {
					writer.close();
					return false;
				} 
			}
			return true;
		}
		return false;
	}
}