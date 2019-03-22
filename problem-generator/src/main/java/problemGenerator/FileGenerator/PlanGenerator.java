package problemGenerator.FileGenerator;

import java.util.List;

public class PlanGenerator extends FileGenerator{

	private static final String FILE_TYPE = "plan";


	public  boolean generateFile(String folderPath, String fileName) {

		return generateFile(folderPath, fileName, FILE_TYPE);
	}

	public void generatePlan(List<String> plan) {	

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
