'''
Created on Apr 6, 2019

@author: daniel
'''

class TesterConfigProblem(object):
    '''
    classdocs
    '''
    def __init__(self, problemName: str, maxTracesToUse: int, minTracesToUse : int):
        '''
        Constructor
        '''
        self.problemName = problemName
        self.maxTracesToUse = maxTracesToUse
        self.minTracesToUse = minTracesToUse  
        
    def __str__(self):
        phrase1 = "\tProblem Name: " + str(self.problemName) + "\n" 
        phrase2 = "\tMax Traces To Use: " + str(self.maxTracesToUse) + "\n"
        phrase3 = "\tMin Traces To Use: " + str(self.minTracesToUse) + "\n"
        
        return str(phrase1+phrase2+phrase3)