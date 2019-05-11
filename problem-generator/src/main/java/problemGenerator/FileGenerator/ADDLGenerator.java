package problemGenerator.FileGenerator;

import java.io.IOException;

public class ADDLGenerator{

	private static final String FOLDER_PATH = "./gen/addl/";
	private static final String FILE_TYPE = ".addl";

	private FileGenerator fg = new FileGenerator();
	private String fileName = null;

	public void generateADDLFile(String fileName) throws IOException{

		this.fileName = fileName;

		generateFile(fileName);

		defineDomain();
	}

	private void defineDomain() throws IOException {
		fg.writeToFile("(define (problem "+fileName+")");
	}

	private void generateFile(String fileName) throws IOException{

		fg.generateFile(FOLDER_PATH, fileName, FILE_TYPE);
	}
}
