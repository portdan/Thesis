package problemGenerator.FileGenerator;

public class ADDLGenerator{

	private static final String FOLDER_PATH = "./gen/addl/";
	private static final String FILE_TYPE = ".addl";

	private FileGenerator fg = new FileGenerator();
	private String fileName = null;

	public boolean generateADDLFile(String fileName) {

		this.fileName = fileName;

		if(!generateFile(fileName))
			return false;
		if(!defineDomain())
			return false;
		return true;
	}

	private boolean defineDomain() {
		return fg.writeToFile("(define (problem "+fileName+")");
	}

	private boolean generateFile(String fileName) {
		return fg.generateFile(FOLDER_PATH, fileName, FILE_TYPE);
	}
}
