package problemGenerator.FileGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import problemGenerator.StateActionState;

public class SASGenerator extends FileGenerator{

	private static final String FILE_TYPE = "txt";

	public void generateFile(String folderPath, String fileName) throws IOException{

		generateFile(folderPath, fileName, FILE_TYPE);
	}

	public void appendSASList(List<StateActionState> sasList, int traceCounter) throws IOException {	

		StringBuilder sb = new StringBuilder();
		List<String> sasListStr = new ArrayList<String>();

		sb.append("trace_");
		sb.append(traceCounter);
		sb.append(": {\n");

		String traceStart = sb.toString();
		sasListStr.add(traceStart);

		for (int i = 0; i < sasList.size(); i++) {

			sb.setLength(0);

			sb.append(sasList.get(i).toString());
			sb.append("\n");

			String sasStr = sb.toString();

			sasListStr.add(sasStr);
		}

		String traceEnd = "}\n";
		sasListStr.add(traceEnd);

		writeToFile(sasListStr);
	}


	public void appendSASList(List<StateActionState> sasList, String actionName) throws IOException {	

		StringBuilder sb = new StringBuilder();
		List<String> sasListStr = new ArrayList<String>();

		sb.append("actionName - ");
		sb.append(actionName);
		sb.append(": {\n");

		String traceStart = sb.toString();
		sasListStr.add(traceStart);

		for (int i = 0; i < sasList.size(); i++) {

			sb.setLength(0);

			sb.append(sasList.get(i).toString());
			sb.append("\n");

			String sasStr = sb.toString();

			sasListStr.add(sasStr);
		}

		String traceEnd = "}\n";
		sasListStr.add(traceEnd);

		writeToFile(sasListStr);

	}
}
