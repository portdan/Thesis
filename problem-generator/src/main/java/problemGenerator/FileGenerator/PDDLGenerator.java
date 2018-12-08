package problemGenerator.FileGenerator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PDDLGenerator extends FileGenerator{

	private static final String FILE_TYPE = "pddl";

	private String fileName = "";

	public  boolean generateFile(String folderPath, String fileName) {
			
		this.fileName = fileName;
		return generateFile(folderPath, fileName, FILE_TYPE);
	}
	
	public void generateRandomProblem(String problemText, String humanizedState) {	
		
		String newProblemText = createProblemString(problemText);
		String newProblemGoal = createGoalString(humanizedState);

		writeToFile(newProblemText+newProblemGoal);
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
		
		//String a = Pattern.quote(oldFileName);
		//String b = Matcher.quoteReplacement(oldFileName + fileNameIteration)
		//problemText.replaceAll(a, b);
		
		/* 
		// this part replaces problem name with new one
		Pattern ptn = Pattern.compile("problem\\s");
		
		Matcher mtch = ptn.matcher(problemText);
		
		int sIndex=0;
		int eIndex=0;
		
		if (mtch.find())	
			sIndex = mtch.end();
		
		eIndex = sIndex + problemText.substring(sIndex).indexOf(')');
		
		StringBuilder sb = new StringBuilder();
		
		sb.append(problemText.substring( 0, sIndex));
		sb.append(fileName);
		sb.append(problemText.substring(eIndex));

		return sb.toString().split(".+?(?=goal)")[0];

		*/
					
		return problemText.split(".+?(?=goal)")[0];
	}

}
