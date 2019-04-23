'''
Created on Apr 6, 2019

@author: daniel
'''

class GrounderConfig:
    '''
    classdocs
    '''
    def __init__(self, domainPath: str, problemPath: str, agentsOutputPath: str, localViewOutputPath: str,
                  groundedOutputPath: str, pythonScriptsPath: str):
        '''
        Constructor
        '''
        self.domainPath = domainPath;
        self.problemPath = problemPath;
        self.agentsOutputPath = agentsOutputPath;
        self.localViewOutputPath = localViewOutputPath;
        self.groundedOutputPath = groundedOutputPath;
        self.pythonScriptsPath = pythonScriptsPath