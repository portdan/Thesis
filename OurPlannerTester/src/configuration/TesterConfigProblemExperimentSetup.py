'''
Created on Apr 6, 2019

@author: daniel
'''
from typing import List

class TesterConfigProblemExperimentSetup(object):
    '''
    classdocs
    '''
    def __init__(self, amountOfTraces: List[float], numberOfExperiments: int, timeoutInMS: int):
        '''
        Constructor
        '''
        self.amountOfTraces = amountOfTraces
        self.numberOfExperiments = numberOfExperiments
        self.timeoutInMS = timeoutInMS
        
    def __str__(self):
        
        res = ""
        phrases = []
        
        phrases.append("\t\tNumber Of Experiments: " + str(self.numberOfExperiments) + "\n")
        phrases.append("\t\tAmount Of Traces: " + str(self.amountOfTraces) + "\n")
        phrases.append("\t\tTimeout in MS: " + str(self.timeoutInMS) + "\n")

        for s in phrases:
            res += s
        
        return res