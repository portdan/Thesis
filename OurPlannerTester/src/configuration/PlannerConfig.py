'''
Created on Apr 6, 2019

@author: daniel
'''

class PlannerConfig(object):
    '''
    classdocs
    '''
    def __init__(self, agentsFileName : str, domainFileName : str,domainName : str, inputAgentsDirName : str, inputDirPath : str, 
                inputGoundedDirName : str, inputLocalViewDirName : str, inputTracesDirName : str, 
                numOfTracesToUse : int, totalTracesBucket : int, outputSoundModelLearningDirName : str, 
                outputCopyDirPath : str, outputDirPath : str, outputSASFileName : str, 
                outputSafeModelLearningDirName : str, outputTempDirPath : str, outputUnSafeModelLearningDirName : str,
                planningModel : str, problemFileName : str, pythonScriptsPath : str, testOutputCSVFilePath : str,
                tracesLearinigInterval : int, verificationModel : str, plannerMode : str, timeoutInMS : int,
                iterationMethod : str, experimentDetails : str, cValue : int, planningTimeoutInMS : int): 
        '''
        Constructor
        '''
        
        self.agentsFileName = agentsFileName
        self.domainFileName = domainFileName
        self.domainName = domainName
        self.inputAgentsDirName =inputAgentsDirName
        self.inputDirPath = inputDirPath
        self.inputGoundedDirName = inputGoundedDirName
        self.inputLocalViewDirName = inputLocalViewDirName
        self.inputTracesDirName = inputTracesDirName
        self.numOfTracesToUse = numOfTracesToUse                
        self.totalTracesBucket = totalTracesBucket
        self.outputSoundModelLearningDirName = outputSoundModelLearningDirName
        self.outputCopyDirPath = outputCopyDirPath
        self.outputDirPath = outputDirPath
        self.outputSASFileName = outputSASFileName
        self.outputSafeModelLearningDirName = outputSafeModelLearningDirName
        self.outputTempDirPath = outputTempDirPath
        self.outputUnSafeModelLearningDirName = outputUnSafeModelLearningDirName
        self.planningModel = planningModel
        self.problemFileName = problemFileName
        self.pythonScriptsPath = pythonScriptsPath
        self.testOutputCSVFilePath = testOutputCSVFilePath
        self.tracesLearinigInterval = tracesLearinigInterval
        self.verificationModel = verificationModel
        self.plannerMode = plannerMode
        self.timeoutInMS = timeoutInMS
        self.iterationMethod = iterationMethod
        self.experimentDetails = experimentDetails
        self.cValue = cValue
        self.planningTimeoutInMS = planningTimeoutInMS