package Configuration;

import enums.IterationMethod;
import enums.PlannerMode;
import enums.PlanningModel;
import enums.VerificationModel;

public class OurPlannerConfiguration {

	public String agentsFileName;
	public String domainFileName;
	public String inputAgentsDirName;
	public String inputDirPath;
	public String inputGoundedDirName;
	public String inputLocalViewDirName;
	public String inputTracesDirName;
	public int numOfTracesToUse;
	public int timeoutInMS;
	public String outputSoundModelLearningDirName;
	public String outputCopyDirPath;
	public String outputDirPath;
	public String outputSASFileName;
	public String outputSafeModelLearningDirName;
	public String outputTempDirPath;
	public String outputUnSafeModelLearningDirName;
	public PlanningModel planningModel;
	public String problemFileName;
	public String pythonScriptsPath;
	public String testOutputCSVFilePath;
	public int tracesLearinigInterval;
	public VerificationModel verificationModel;
	public PlannerMode plannerMode;
	public IterationMethod iterationMethod;

}
