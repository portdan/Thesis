package problemGenerator.FileGenerator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class FileGenerator {

	public File outFile = null;
	public PrintWriter writer = null;

	public boolean generateFile(String folderPath, String fileName, String fileType) {

		outFile = new File(folderPath + "/" + fileName + "." + fileType);

		if (!outFile.exists()) {
			try {
				new File(folderPath).mkdirs();
				outFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		} else {

			clearFile();
		}

		return true;
	}

	public boolean writeToFile(String str) {

		if (outFile != null) {

			try {
				writer = new PrintWriter(new BufferedWriter(new FileWriter(outFile, true)));
				writer.write(str);
				writer.flush();
				writer.close();
			} catch (IOException ex) {
				ex.printStackTrace();

				if (writer != null) {
					writer.close();
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public boolean clearFile() {

		if (outFile != null) {

			try {
				writer = new PrintWriter(new BufferedWriter(new FileWriter(outFile, false)));
				writer.write("");
				writer.flush();
				writer.close();
			} catch (IOException ex) {
				ex.printStackTrace();

				if (writer != null) {
					writer.close();
					return false;
				}
			}
			return true;
		}
		return false;
	}

}