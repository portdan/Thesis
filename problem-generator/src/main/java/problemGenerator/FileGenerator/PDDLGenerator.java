package problemGenerator.FileGenerator;

public class PDDLGenerator extends FileGenerator{

	private static final String FILE_TYPE = "pddl";


	public  boolean generateFile(String folderPath, String fileName) {

		return generateFile(folderPath, fileName, FILE_TYPE);
	}

	public void generateRandomProblem(String problemText, String newProblemName ,String humanizedState) {	

		String newProblemText = createProblemString(problemText, newProblemName);
		String newProblemGoal = createGoalString(humanizedState);

		writeToFile(newProblemText+newProblemGoal);
	}


	private String createGoalString(String humanizedState) {

		String res = "(:goal\n" + "\t(and\n";
		String str = new String(humanizedState);

		int start = str.indexOf('=');

		while(start != -1) {	
			boolean isNegated = false;

			int end = str.indexOf(')');

			String var = str.substring(start + 1,end + 1);

			if(var.startsWith("Negated")) {
				isNegated = true;
				var = var.replace("Negated", "");
			}

			var = var.replace("(", " ");
			var = var.replace(",", "");
			var = var.replace(")", "");

			if(isNegated)
				res += "\t\t(not (" + var + "))\n";
			else
				res += "\t\t(" + var + ")\n";

			str = str.substring(end + 1, str.length());		

			start = str.indexOf('=');
		}

		res += "\t)\n" + ")\n"+")";

		return res;
	}

	private String createProblemString(String problemText, String newProblemName) {

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
		
		String toFind = "(problem ";
		
		int startIndex = problemText.indexOf(toFind) + toFind.length();
		int endIndex = startIndex + problemText.substring(startIndex).indexOf(")");
		
		StringBuilder sb = new StringBuilder();

		sb.append(problemText.substring(0, startIndex));
		sb.append(newProblemName);
		sb.append(problemText.substring(endIndex));
		
		problemText = sb.toString();

		return problemText.split(".+?(?=goal)")[0];
	}

}
