package problemGenerator.FileGenerator;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import problemGenerator.StateActionState;

public class SASGenerator extends FileGenerator{

	private static final String FILE_TYPE = "txt";

	private BufferedOutputStream stream;

	public boolean generateFile(String folderPath, String fileName) {

		boolean isGenerated = generateFile(folderPath, fileName, FILE_TYPE);

		try {
			stream = new BufferedOutputStream(new FileOutputStream(outFile));
		} catch (FileNotFoundException e) {
			return false;
		}

		return isGenerated;
	}

	public boolean appendSASList(List<StateActionState> sasList, int traceCounter) {	
		try {
			
			String traceStart = "trace_" + traceCounter + ": {\n";
			stream.write(traceStart.getBytes());
			
			for (int i = 0; i < sasList.size(); i++) {

				byte bytes[] = sasList.get(i).toString().getBytes();    

				stream.write(bytes);
				stream.write("\n".getBytes());
			}
			
			String traceEnd = "}\n";
			stream.write(traceEnd.getBytes());
			
			stream.flush();

		} catch (IOException e) {
			return false;
		}    

		return true;
	}

	public boolean appendSASList(List<StateActionState> sasList, String actionName) {	
		try {
			
			String traceStart = "actionName - " + actionName + " : {\n";
			stream.write(traceStart.getBytes());
			
			for (int i = 0; i < sasList.size(); i++) {

				byte bytes[] = sasList.get(i).toString().getBytes();    

				stream.write(bytes);
				stream.write("\n".getBytes());
			}
			
			String traceEnd = "}\n";
			stream.write(traceEnd.getBytes());
			
			stream.flush();

		} catch (IOException e) {
			return false;
		}    

		return true;
	}

	
	public boolean close() {

		try {
			stream.close();
		} catch (IOException e) {
			return false;
		}

		return true;
	}
}
