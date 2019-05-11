package problemGenerator.FileGenerator;

import java.io.IOException;
import java.util.List;

public class PlanGenerator extends FileGenerator{

	private static final String FILE_TYPE = "plan";


	public void generateFile(String folderPath, String fileName) throws IOException {

		generateFile(folderPath, fileName, FILE_TYPE);
	}

	public void generatePlan(List<String> plan) throws IOException {	

		String planText = createPlanString(plan);

		writeToFile(planText);
	}


	private String createPlanString(List<String> plan) {

		String res = "";

		for (int i = 0; i < plan.size(); i++) {

			res += i + ": (" + plan.get(i) + ")";

			if(i+1 != plan.size())
				res += "\n";
		}

		return res;
	}

}
