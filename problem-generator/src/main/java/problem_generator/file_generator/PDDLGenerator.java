package problem_generator.file_generator;

public class PDDLGenerator {

	private static final String FOLDER_PATH = "./gen/pddl/";
	private static final String FILE_TYPE = ".pddl";

	private FileGenerator fg = new FileGenerator();

	public  boolean generateFile(String fileName) {
		return fg.generateFile(FOLDER_PATH, fileName, FILE_TYPE);
	}
}
