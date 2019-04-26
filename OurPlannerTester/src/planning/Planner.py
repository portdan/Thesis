'''
Created on Apr 25, 2019

@author: daniel
'''

import pyckson
import subprocess
import shutil

from configuration import PlannerConfig
from utils.Utils import clear_directory,copy_tree

import logging
logger = logging.getLogger(__name__)


class Planner(object):
    '''
    classdocs
    '''

    def __init__(self, config):
        '''
        Constructor
        '''
        
        self.config = config
        self.planner_output_folder = None

    

    def preprocess_runner_script_write(self, planner_config):
        
        logger.info("preprocess_runner_script_write")
        
        preprocess_runner_file_name = planner_config.pythonScriptsPath + "/preprocess/preprocess-runner"
        with open(preprocess_runner_file_name, 'w+') as preprocess_runner_script:
            
            shebang = "#!/bin/bash"
            preprocess_script_path = planner_config.pythonScriptsPath + "/preprocess/preprocess"
            preprocess_sas_file_input = " < " + self.config.problemPlannerOutput + "/output.sas"
            
            preprocess_runner_script.write(shebang)
            preprocess_runner_script.write('\n\n')
            preprocess_runner_script.write(preprocess_script_path)
            preprocess_runner_script.write(preprocess_sas_file_input)

    def prepere_to_plan(self, problem_name, num_of_traces_to_use):
     
        logger.info("prepere_to_plan")
        
        clear_directory(self.config.problemPlannerInput, delete=True)
        clear_directory(self.config.problemPlannerOutput)
        
        logger.info("problemPlannerInput : " + self.config.problemPlannerInput)
    
        shutil.copytree(self.config.problemGrounderOutput, self.config.problemPlannerInput)
        copy_tree(self.config.problemGeneratorOutput, self.config.problemPlannerInput)
  
        planner_config = None
        
        with open(self.config.problemPlannerConfig, 'r') as plannerConfigJson:
            planner_config = pyckson.load(PlannerConfig, plannerConfigJson)
            
        self.preprocess_runner_script_write(planner_config)
            
        planner_config.domainFileName = self.config.domainName
        planner_config.problemFileName = problem_name
        planner_config.numOfTracesToUse = num_of_traces_to_use
        planner_config.agentsFileName = problem_name.split(".")[0] + ".agents"
        
        self.planner_output_folder = self.config.outputDestination + "/" + problem_name + "/" + self.config.problemPlannerOutputDestination

        planner_config.outputCopyDirPath = self.planner_output_folder
        
        with open(self.config.problemPlannerConfig, 'w+') as plannerConfigJson:
            pyckson.dump(planner_config, plannerConfigJson)
       
                  
    def run_planning(self):
    
        logger.info("run_planning")
        
        processList = ['java', '-jar', self.config.problemPlannerJAR, self.config.problemPlannerCreator, 
                       self.config.problemPlannerConfig]
        
        print(', '.join(processList))
        
        '''
        process = subprocess.Popen(processList)
        process.wait()
        '''
        
        process = subprocess.Popen(processList, stdout=subprocess.PIPE)
        out, err = process.communicate()
        
        logger.info(str(out.decode('utf-8')))
        
        
    def delete_output(self):
        clear_directory(self.config.problemPlannerInput)
        clear_directory(self.config.problemPlannerOutput)

    def plan(self, problem_name, num_of_traces_to_use):
           
        logger.info("plan")
        
        self.prepere_to_plan(problem_name, num_of_traces_to_use)
        
        self.run_planning()   