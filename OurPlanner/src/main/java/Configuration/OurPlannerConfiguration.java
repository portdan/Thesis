package Configuration;

import OurPlanner.PlanningModel;
import OurPlanner.VerificationModel;

public class OurPlannerConfiguration {

	public String domainFileName;
	public String agentsFileName;
	public String problemFileName;
	public String groundedDirPath;
	public String localViewDirPath;
	public String tracesDirPath;
	public int numOfTracesToUse;
	public int tracesLearinigInterval;
	public String agentsFilePath;
	public String outputCopyDirPath;
	public String testOutputCSVFilePath;
	public String pythonScriptsPath;
	public String sasFilePath;
	public String outputDirPath;
	public String outputSafeModelLearningDirPath;
	public String outputUnSafeModelLearningDirPath;
	public String outputTempDirPath;
	public VerificationModel verificationModel;
	public PlanningModel planningModel;
}
