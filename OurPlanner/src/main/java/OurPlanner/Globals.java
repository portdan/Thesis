package OurPlanner;

public class Globals {

	public static String INPUT_PATH;
	public static String INPUT_GROUNDED_PATH;
	public static String INPUT_LOCAL_VIEW_PATH;
	public static String INPUT_AGENTS_PATH;
	public static String INPUT_TRACES_PATH;

	public static String OUTPUT_PATH;
	public static String OUTPUT_TEMP_PATH;
	public static String OUTPUT_SAFE_MODEL_PATH;
	public static String OUTPUT_UNSAFE_MODEL_PATH;
	public static String OUTPUT_SOUND_MODEL_PATH;
	public static String OUTPUT_COPY_PATH;
	public static String OUTPUT_TEST_FILE_PATH;

	public static String SAS_OUTPUT_FILE_PATH;
	public static String PROCESSED_SAS_OUTPUT_FILE_PATH;
	
	public static String LIKELY_MODEL_GENERATOR_PATH;
	public static final String LIKELY_MODEL_GENERATOR_INPUT_FOLDER = "/input";
	public static final String LIKELY_MODEL_GENERATOR_OUTPUT_FOLDER= "/output";
	public static final String LIKELY_MODEL_GENERATOR_WORKING_FOLDER= "/working";

	public static final String INIT_KEYWORD = "init\n";
	public static final String GOAL_KEYWORD = "goal\n";
	public static final String NEGATED_KEYWORD = "Negated";
	public static final String NONE_KEYWORD = "NONE";

	public static final String TRAJECTORY_FILE_EXTENSION = "plan";

	public static final String ADD_NEW_FACT_INDICATION = "-add-new-fact-";
	public static final String REMOVE_OLD_FACT_INDICATION = "-remove-old-fact-";
	public static final String PARAMETER_INDICATION = "-param-";
	public static final String AGENT_INDICATION = "-agent-";

	public static String PYTHON_SCRIPTS_FOLDER;
	
	public final static boolean IGNORE_OFFLINE_LEARNING_TIMEOUT = true;
	public final static double MEMORY_OVER_USAGE_RATIO = 0.9;

}
