package problemGenerator.FileGenerator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PDDLGenerator {

	private static final String FILE_TYPE = "pddl";

	private FileGenerator fg = new FileGenerator();

	public  boolean generateFile(String folderPath, String fileName) {
		return fg.generateFile(folderPath, fileName, FILE_TYPE);
	}
	
	public void generateRandomProblem(String problemText, String humanizedState) {	
		
		String newProblemText = createProblemString(problemText);
		String newProblemGoal = createGoalString(humanizedState);

		fg.WriteToFile(newProblemText+newProblemGoal);
	}

	
	private String createGoalString(String humanizedState) {

		String res = "(:goal\n" + "\t(and\n";

		Pattern ptn = Pattern
				.compile("[a-z][a-z0-9_]*[a-z][a-z0-9_]*\\([a-z][a-z]*[0-9]+[a-z0-9]*,.[a-z][a-z]*[0-9]+[a-z0-9]*\\)");
		Matcher mtch = ptn.matcher(humanizedState);

		while (mtch.find()) {

			String var = mtch.group();

			var = var.replace("(", " ");
			var = var.replace(",", "");
			var = var.replace(")", "");

			res += "\t\t(" + var + ")\n";
		}

		res += "\t)\n" + ")\n"+")";

		return res;
	}

	private String createProblemString(String problemText) {
		return problemText.split(".+?(?=goal)")[0];
	}

}
