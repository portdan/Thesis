'''
Created on Apr 6, 2019

@author: daniel
'''
from typing import List

class TesterConfigProblemExperimentSetup(object):
    '''
    classdocs
    '''
    def __init__(self,tracesAmountMax: int, randomWalkSteps: int, TestSizeOfTracesAmaount: List[float],
                  numberOfExperiments: int, timeoutInMS: int, TestSizeInPercent: bool):
        '''
        Constructor
        '''
        self.tracesAmountMax = tracesAmountMax
        self.randomWalkSteps = randomWalkSteps
        self.TestSizeOfTracesAmaount = TestSizeOfTracesAmaount
        self.TestSizeInPercent = TestSizeInPercent
        self.numberOfExperiments = numberOfExperiments
        self.timeoutInMS = timeoutInMS
        
    def __str__(self):
        
        res = ""
        phrases = []
        
        phrases.append("\t\tRraces Amount Max: " + str(self.tracesAmountMax) + "\n")
        phrases.append("\t\tRandom Walk Steps: " + str(self.randomWalkSteps) + "\n")
        phrases.append("\t\tNumber Of Experiments: " + str(self.numberOfExperiments) + "\n")
        phrases.append("\t\tTracse In Percent: " + str(self.TestSizeInPercent) + "\n")
        phrases.append("\t\tAmount Of Traces: " + str(self.TestSizeOfTracesAmaount) + "\n")
        phrases.append("\t\tTimeout in MS: " + str(self.timeoutInMS) + "\n")

        for s in phrases:
            res += s
        
        return res