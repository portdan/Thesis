'''
Created on Apr 6, 2019

@author: daniel
'''
from typing import List
from configuration.TesterConfigProblemExperimentSetup import TesterConfigProblemExperimentSetup
from configuration.TesterConfigProblemThresholdSearchSetup import TesterConfigProblemThresholdSearchSetup

class TesterConfigProblem(object):
    '''
    classdocs
    '''
    def __init__(self, problemNames: List[str], iterationMethods: List[str], experimentSetup: TesterConfigProblemExperimentSetup, 
                 thresholdSearchSetup: TesterConfigProblemThresholdSearchSetup):
        '''
        Constructor
        '''
        self.problemNames = problemNames
        self.iterationMethods = iterationMethods
        self.experimentSetup = experimentSetup
        self.thresholdSearchSetup = thresholdSearchSetup

    def __str__(self):
        
        res = ""
        phrases = []
        
        phrases.append("\tProblem Names: " + "\n" )
        
        for pn in self.problemNames:
            phrases.append("\t\t" + str(pn) +"\n")
            
        for im in self.iterationMethods:
            phrases.append("\t\t" + str(im) +"\n")    
                        
        phrases.append("\tExperiment Setup: " + str(self.experimentSetup) + "\n")
        phrases.append("\tThreshold Search Setup: " + str(self.thresholdSearchSetup) + "\n")

        for s in phrases:
            res += s
        
        return res