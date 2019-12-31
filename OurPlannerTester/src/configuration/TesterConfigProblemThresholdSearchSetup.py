'''
Created on Apr 6, 2019

@author: daniel
'''

class TesterConfigProblemThresholdSearchSetup(object):
    '''
    classdocs
    '''
    def __init__(self, maxTracesToUse: int, minTracesToUse : int, timeoutInMS: int):
        '''
        Constructor
        '''
        self.maxTracesToUse = maxTracesToUse
        self.minTracesToUse = minTracesToUse
        self.timeoutInMS = timeoutInMS
        
    def __str__(self):
        
        res = ""
        phrases = []
        
        phrases.append("\t\tMax Traces To Use: " + str(self.maxTracesToUse) + "\n")
        phrases.append("\t\tMin Traces To Use: " + str(self.minTracesToUse) + "\n")
        phrases.append("\t\tTimeout in MS: " + str(self.timeoutInMS) + "\n")

        for s in phrases:
            res += s
        
        return res