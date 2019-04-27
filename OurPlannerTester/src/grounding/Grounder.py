'''
Created on Apr 24, 2019

@author: daniel
'''

import pyckson
import subprocess
import shutil

from configuration import GrounderConfig
from utils.Utils import clear_directory

import logging
logger = logging.getLogger(__name__)

class Grounder(object):
    '''
    classdocs
    '''

    def __init__(self, config, log_output=False):
        '''
        Constructor
        '''
        
        self.config = config
        self.grounded_output_path = None
        self.agent_output_path = None
        self.localView_output_path = None
        self.log_output = log_output
    
    
    def prepere_to_ground(self, domain_file_path, problem_file_path):
     
        logger.info("prepere_to_ground")
           
        clear_directory(self.config.problemGrounderInput)
        clear_directory(self.config.problemGrounderOutput)
        
        logger.info("problemGrounderInput : " + self.config.problemGrounderInput)
    
        domain_file_path_input = shutil.copy2(domain_file_path, self.config.problemGrounderInput)
        problem_file_path_input = shutil.copy2(problem_file_path, self.config.problemGrounderInput)
        
        grounder_config = None
        
        with open(self.config.problemGrounderConfig, 'r') as grounderConfigJson:
            grounder_config = pyckson.load(GrounderConfig, grounderConfigJson)
            
        grounder_config.domainPath = domain_file_path_input
        grounder_config.problemPath = problem_file_path_input
        
        self.grounded_output_path = grounder_config.groundedOutputPath
        self.agent_output_path = grounder_config.agentsOutputPath
        self.localView_output_path = grounder_config.localViewOutputPath
        
        with open(self.config.problemGrounderConfig, 'w+') as grounderConfigJson:
            pyckson.dump(grounder_config, grounderConfigJson)
            
    def run_grounding(self):
    
        logger.info("run_grounding")
        
        processList = ['java', '-jar', self.config.problemGrounderJAR, self.config.problemGrounderCreator, 
                       self.config.problemGrounderConfig]
        
        print(', '.join(processList))
        
        '''
        process = subprocess.Popen(processList)
        process.wait()
        '''
        
        process = subprocess.Popen(processList, stdout=subprocess.PIPE)
        out, err = process.communicate()
        
        if self.log_output:
            logger.info(str(out.decode('utf-8')))

    def copy_generation_output(self, problem_name):

        logger.info("copy_generation_output")
        
        '''
        problem_name = os.path.basename(problem_file_path)
        problem_name = os.path.splitext(problem_name)[0]
        '''
                
        dst = self.config.outputDestination + "/" + problem_name + "/" + self.config.problemGrounderOutputDestination
    
        logger.info("dst - " + dst)
    
        shutil.copytree(self.config.problemGrounderOutput, dst)
        
    
    def delete_output(self):
        clear_directory(self.config.problemGrounderInput)
        clear_directory(self.config.problemGrounderOutput)
       
    def ground_problem(self, domain_file_path, problem_name, problem_file_path):
        
        logger.info("ground_problem")
        
        self.prepere_to_ground(domain_file_path, problem_file_path)
        
        self.run_grounding()   
        
        self.copy_generation_output(problem_name)