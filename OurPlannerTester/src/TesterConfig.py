'''
Created on Apr 6, 2019

@author: daniel
'''

from typing import List
from TesterConfigProblem import TesterConfigProblem

class TesterConfig:
    '''
    classdocs
    '''
    def __init__(self, domainPath: str, domainName: str, problemsPath: str, problems: List[TesterConfigProblem],
                 problemGrounderInput: str, problemGrounderOutput: str, problemGeneratorInput: str, 
                 problemGeneratorOutput: str, problemGrounderConfig: str, problemGeneratorConfig: str ,
                 problemGrounderJAR: str, problemGeneratorJAR: str, problemGrounderCreator: str, 
                 problemGeneratorCreator: str, outputDestination: str, originDestination: str,
                 problemGrounderOutputDestination : str, problemGeneratorOutputDestination : str ):
        '''
        Constructor
        '''
        self.domainPath = domainPath
        self.domainName = domainName
        self.problemsPath = problemsPath
        self.problems = problems
        
        self.outputDestination = outputDestination
        self.originDestination = originDestination

        self.problemGrounderInput = problemGrounderInput
        self.problemGrounderOutput = problemGrounderOutput
        self.problemGrounderConfig = problemGrounderConfig
        self.problemGrounderJAR = problemGrounderJAR
        self.problemGrounderCreator = problemGrounderCreator
        self.problemGrounderOutputDestination = problemGrounderOutputDestination
        
        self.problemGeneratorInput = problemGeneratorInput
        self.problemGeneratorOutput = problemGeneratorOutput
        self.problemGeneratorConfig = problemGeneratorConfig
        self.problemGeneratorJAR = problemGeneratorJAR
        self.problemGeneratorCreator = problemGeneratorCreator
        self.problemGeneratorOutputDestination = problemGeneratorOutputDestination
        
        
    def __str__(self):
        
        res = ""
        phrases = []
        
        phrases.append("Domain File Path: " + str(self.domainPath) + "\n" )
        phrases.append("Domain File Name: " + str(self.domainName) + "\n" )
        phrases.append("Problems To Test Path: " + str(self.problemsPath) + "\n" )
        
        phrases.append("Output Destination Path: " + str(self.outputDestination) + "\n" )
        phrases.append("Origin Destination Path: " + str(self.originDestination) + "\n" )

        
        phrases.append("Grounder Input Folder Path: " + str(self.problemGrounderInput) + "\n" )
        phrases.append("Grounder Output Folder Path: " + str(self.problemGrounderOutput) + "\n" )
        phrases.append("Grounder Config File Path: " + str(self.problemGrounderConfig) + "\n" )
        phrases.append("Grounder JAR File Path: " + str(self.problemGrounderJAR) + "\n" )
        phrases.append("Grounder Creator Name: " + str(self.problemGrounderCreator) + "\n" )
        phrases.append("Grounder Destination Path: " + str(self.problemGrounderOutputDestination) + "\n" )

        phrases.append("Generator Input Folder Path: " + str(self.problemGeneratorInput) + "\n" )
        phrases.append("Generator Output Folder Path: " + str(self.problemGeneratorOutput) + "\n" )
        phrases.append("Generator Config File Path: " + str(self.problemGeneratorConfig) + "\n" )
        phrases.append("Generator JAR File Path: " + str(self.problemGeneratorJAR) + "\n" )
        phrases.append("Generator Creator Name: " + str(self.problemGeneratorCreator) + "\n" ) 
        phrases.append("Grounder Destination Path: " + str(self.problemGeneratorOutputDestination) + "\n" )

        phrases.append("Problems To Test List: " + "\n" )
        
        for tcp in self.problems:
            phrases.append(str(tcp))
            
        for s in phrases:
            res += s
        
        return res