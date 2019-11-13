'''
Created on Apr 25, 2019

@author: daniel
'''

import pyckson
import subprocess
import shutil
import csv
import math

from configuration import PlannerConfig
from utils.Utils import clear_directory,copy_tree

import logging
logger = logging.getLogger(__name__)

planning_mode = "Planning"
planning_and_learning_mode = "PlanningAndLearning"


class Planner(object):
    '''
    classdocs
    '''

    def __init__(self, config, log_output=False):
        '''
        Constructor
        '''
        
        self.config = config
        self.planner_output_folder = None
        self.test_output_CSV_file_path = None
        self.tested_amounts = None
        self.log_output = log_output
        

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

    def prepere_planning_config(self, problem_name, num_of_traces_to_use, planner_mode):
     
        logger.info("prepere_planning_config")
        
        clear_directory(self.config.problemPlannerOutput)
  
        planner_config = None
        
        with open(self.config.problemPlannerConfig, 'r') as plannerConfigJson:
            planner_config = pyckson.load(PlannerConfig, plannerConfigJson)
            
        self.test_output_CSV_file_path = planner_config.testOutputCSVFilePath
            
        self.preprocess_runner_script_write(planner_config)
            
        planner_config.domainFileName = self.config.domainName
        planner_config.problemFileName = problem_name
        planner_config.numOfTracesToUse = num_of_traces_to_use
        planner_config.agentsFileName = problem_name.split(".")[0] + ".agents"
        planner_config.verificationModel = self.config.problemPlannerVerificationModel
        planner_config.planningModel = self.config.problemPlannerPlanningModel
        planner_config.plannerMode = planner_mode

        self.planner_output_folder = self.config.outputDestination + "/" + problem_name + "/" + self.config.problemPlannerOutputDestination

        planner_config.outputCopyDirPath = self.planner_output_folder
        
        with open(self.config.problemPlannerConfig, 'w+') as plannerConfigJson:
            pyckson.dump(planner_config, plannerConfigJson)
            
        logger.info("prepering to plan for : " + str(planner_config.problemFileName) + "with " + str(planner_config.numOfTracesToUse) + " traces")
       
    def copy_input_files(self):
     
        logger.info("copy_input_files")
        
        clear_directory(self.config.problemPlannerInput, delete=True)
        
        logger.info("problemPlannerInput : " + self.config.problemPlannerInput)
    
        shutil.copytree(self.config.problemGrounderOutput, self.config.problemPlannerInput)
        copy_tree(self.config.problemGeneratorOutput, self.config.problemPlannerInput)
                  
    def run_planning(self):
    
        logger.info("run_planning")
        
        processList = ['java', '-Xmx10g', '-jar', self.config.problemPlannerJAR, self.config.problemPlannerCreator, 
                       self.config.problemPlannerConfig]
        
        print(', '.join(processList))
        
        
        if self.log_output:
            process = subprocess.Popen(processList, stdout=subprocess.PIPE)
            out, err = process.communicate()
            logger.info(str(out.decode('utf-8')))
        else:
            process = subprocess.Popen(processList)
            process.wait()
        
        
    def delete_output(self):
        clear_directory(self.config.problemPlannerOutput)

    def plan(self, problem_name, num_of_traces_to_use, planner_mode):
           
        logger.info("plan")
        
        self.prepere_planning_config(problem_name, num_of_traces_to_use, planner_mode)
        
        self.run_planning()   
        
        self.delete_output()
        
    def plan_range_traces(self,problem_name, range_start, range_end, range_split):
    
        if (range_end-range_start) < range_split:
            range_step = 1
        else:
            range_step = (range_end-range_start) // range_split 
            
        range_splited = list(range(range_start, range_end, range_step))
        
        if range_end not in range_splited:
            range_splited.append(range_end)
        
        # test between 0 - solved_threshold
        
        for traces_amount in range_splited:
            if traces_amount not in self.tested_amounts:    
                self.plan(problem_name, traces_amount)
                
    def plan_and_learn_range_traces(self,problem_name, range_start, range_end, range_split):
    
        if (range_end-range_start) < range_split:
            range_step = 1
        else:
            range_step = (range_end-range_start) // range_split 
            
        range_splited = list(range(range_start, range_end, range_step))
        
        if range_end not in range_splited:
            range_splited.append(range_end)
        
        # test between 0 - solved_threshold
        
        for traces_amount in reversed(range_splited):
            #if traces_amount not in self.tested_amounts:    
            self.plan(problem_name, traces_amount, planning_and_learning_mode)

        
    def search_solved_threshold(self, problem_name, min_traces, max_traces):
           
        logger.info("search_solved_threshold")
        
        self.tested_amounts = []
                
        while min_traces <= max_traces:
            
            solved_counter = 0
            timeout_counter = 0
    
            current_traces_amount = math.ceil((min_traces + max_traces) / 2)
                        
            self.plan(problem_name, current_traces_amount, planning_mode)
            agents, solved, timeout = self.get_ourplanner_results(self.test_output_CSV_file_path)
            
            solved_counter += solved
            timeout_counter += timeout
            
            self.tested_amounts.append(current_traces_amount)
            
            if solved_counter >= 1:
                max_traces = current_traces_amount - 1
            else:
                min_traces = current_traces_amount + 1
                
        return min_traces  
    
    def get_ourplanner_results(self, test_csv):
    
        solved = 0
        timeout = 0
        agents = 0
        
        with open(test_csv, "rt" ) as outputCSV:
            reader = csv.reader(outputCSV)
            
            header = next(reader, None)
            solved_index = header.index("Solved")
            timeout_index = header.index("Timeout")
            agents_index = header.index("Agents")
    
            for row in reader:
                last = row
                
            solved = int(last[solved_index])
            timeout = int(last[timeout_index])
            agents = int(last[agents_index])
            
        return agents, solved, timeout
