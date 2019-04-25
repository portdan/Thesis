'''
Created on Apr 6, 2019

@author: daniel
'''

class GeneratorConfig(object):
    '''
    classdocs
    '''
    def __init__(self, domainFilePath: str, problemFilePath: str, 
                 numOfRandomWalkSteps: int, numOfTracesToGenerate : int, 
                 sasFilePath: str, tempDirPath: str, tracesDirPath : str, pythonScriptsPath : str):
        '''
        Constructor
        '''  
        self.domainFilePath = domainFilePath
        self.problemFilePath = problemFilePath
        self.numOfRandomWalkSteps = numOfRandomWalkSteps
        self.numOfTracesToGenerate = numOfTracesToGenerate
        self.sasFilePath = sasFilePath
        self.tempDirPath = tempDirPath
        self.tracesDirPath = tracesDirPath
        self.pythonScriptsPath = pythonScriptsPath