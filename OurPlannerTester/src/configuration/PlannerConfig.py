'''
Created on Apr 6, 2019

@author: daniel
'''

class PlannerConfig(object):
    '''
    classdocs
    '''
    def __init__(self, domainFileName: str, problemFileName: str, groundedDirPath: str, 
                 localViewDirPath: str, tracesDirPath : str, numOfTracesToUse : int,
                 agentsFilePath: str, outputCopyDirPath: str, testOutputCSVFilePath: str,
                 agentsFileName: str, pythonScriptsPath : str, sasFilePath : str, outputDirPath : str, 
                 outputLearningDirPath: str, outputTempDirPath : str ):
        '''
        Constructor
        '''
        self.domainFileName = domainFileName
        self.agentsFileName = agentsFileName
        self.problemFileName = problemFileName
        self.groundedDirPath = groundedDirPath
        self.localViewDirPath = localViewDirPath
        self.tracesDirPath = tracesDirPath
        self.numOfTracesToUse = numOfTracesToUse
        self.agentsFilePath = agentsFilePath
        self.outputCopyDirPath = outputCopyDirPath
        self.testOutputCSVFilePath = testOutputCSVFilePath
        self.pythonScriptsPath = pythonScriptsPath
        self.sasFilePath = sasFilePath
        self.outputDirPath = outputDirPath
        self.outputLearningDirPath = outputLearningDirPath
        self.outputTempDirPath = outputTempDirPath