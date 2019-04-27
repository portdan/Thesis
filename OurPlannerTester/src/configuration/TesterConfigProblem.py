'''
Created on Apr 6, 2019

@author: daniel
'''

class TesterConfigProblem(object):
    '''
    classdocs
    '''
    def __init__(self, problemName: str, maxTracesToUse: int, minTracesToUse : int, 
                 solvedRangeSplit : int, unsolvedRangeSplit : int):
        '''
        Constructor
        '''
        self.problemName = problemName
        self.maxTracesToUse = maxTracesToUse
        self.minTracesToUse = minTracesToUse 
        self.solvedRangeSplit = solvedRangeSplit
        self.unsolvedRangeSplit = unsolvedRangeSplit 
        
    def __str__(self):
        
        res = ""
        phrases = []
        
        phrases.append("\tProblem Name: " + str(self.problemName) + "\n")
        phrases.append("\tMax Traces To Use: " + str(self.maxTracesToUse) + "\n")
        phrases.append("\tMin Traces To Use: " + str(self.minTracesToUse) + "\n")
        
        for s in phrases:
            res += s
        
        return res