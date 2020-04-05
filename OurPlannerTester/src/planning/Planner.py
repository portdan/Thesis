'''
Created on Apr 25, 2019

@author: daniel
'''

import pyckson
import subprocess
import shutil
import csv
import math
import os

from configuration import PlannerConfig
from utils.Utils import clear_directory,copy_tree

import logging
logger = logging.getLogger(__name__)

planning_mode = "Planning"
planning_and_learning_mode = "PlanningAndLearning"

Monte_Carlo_Reliability_Heuristic = "Monte_Carlo_Reliability_Heuristic"
Monte_Carlo_Plan_Length_Heuristic = "Monte_Carlo_Plan_Length_Heuristic"
Monte_Carlo_Goal_Proximity_Heuristic = "Monte_Carlo_Goal_Proximity_Heuristic"
    
Plan_Length_And_Reliability_Heuristic = "Plan_Length_And_Reliability_Heuristic"
Goal_Proximity_Heuristic = "Goal_Proximity_Heuristic"
Reliability_Heuristic = "Reliability_Heuristic"
Plan_Length_Heuristic = "Plan_Length_Heuristic"
Random = "Random"
BFS = "BFS"
DFS = "DFS"
Offline_Learning = "Offline_Learning"

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
        self.planner_traces_folder_path = None
        self.planner_ssv_file_path = None
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

    def prepere_planning_config(self, problem_name, num_of_traces_to_use, total_traces_bucket, planner_mode, experiment_details, timeoutInMS, planner_iteration_method = None, C_value = 2):
     
        logger.info("prepere_planning_config")
        
        clear_directory(self.config.problemPlannerOutput)
  
        planner_config = None
        
        with open(self.config.problemPlannerConfig, 'r') as plannerConfigJson:
            planner_config = pyckson.load(PlannerConfig, plannerConfigJson)
                        
        self.preprocess_runner_script_write(planner_config)
            
        planner_config.domainFileName = self.config.domainName
        planner_config.problemFileName = problem_name
        planner_config.numOfTracesToUse = num_of_traces_to_use
        planner_config.totalTracesBucket = total_traces_bucket
        planner_config.agentsFileName = problem_name.split(".")[0] + ".agents"
        planner_config.verificationModel = self.config.problemPlannerVerificationModel
        planner_config.planningModel = self.config.problemPlannerPlanningModel
        planner_config.plannerMode = planner_mode
        planner_config.testOutputCSVFilePath = self.config.outputDestination + "/" + problem_name.split(".")[0] + ".csv"
        planner_config.experimentDetails = experiment_details
        planner_config.timeoutInMS = timeoutInMS

        if planner_iteration_method is not None:
            planner_config.iterationMethod = planner_iteration_method
            
        planner_config.cValue = C_value

        self.planner_output_folder = self.config.outputDestination + "/" + problem_name + "/" + self.config.problemPlannerOutputDestination
        self.planner_traces_folder_path = self.config.problemPlannerInput + "/" + planner_config.inputTracesDirName
        self.planner_ssv_file_path = planner_config.testOutputCSVFilePath;

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

    def plan(self, problem_name, num_of_traces_to_use, total_traces_bucket, planner_mode, experiment_details, timeoutInMS, planner_iteration_method=None, C_value = 2):
           
        logger.info("plan")
        
        self.prepere_planning_config(problem_name, num_of_traces_to_use, total_traces_bucket, planner_mode, experiment_details, timeoutInMS, planner_iteration_method, C_value)
        
        self.run_planning()   
        
        self.delete_output()
       
    '''    
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
      
    '''  
            
    def plan_and_learn_by_iteration_method(self,problem_config, problem_name, max_traces_to_generate, 
                                           grounded_output_path, traces_generator):
            
        experiment_setup = problem_config.experimentSetup    
        iteration_methods = problem_config.iterationMethods    

        thresholdTimeoutInMS = problem_config.thresholdSearchSetup.timeoutInMS
        experimentTimeoutInMS = problem_config.experimentSetup.timeoutInMS
        
        max_traces_bucket = traces_generator.get_num_of_random_walk_steps() * max_traces_to_generate

        experiment_traces_buckets = [int(t * max_traces_bucket) for t in experiment_setup.amountOfTraces]
        experiment_traces_buckets.sort(reverse=False)
        
        experiment_traces_amounts = [int(t * max_traces_to_generate) for t in experiment_setup.amountOfTraces]
        experiment_traces_amounts.sort(reverse=False)
               
        experiment_counter = 0
        
        for experiment_traces_bucket in experiment_traces_buckets: 
            
            experiment_traces_amount =  experiment_traces_amounts[experiment_counter]          
            
            experiment_counter+=1
    
            for i in range(0 , experiment_setup.numberOfExperiments):
                
                generated_traces_amount = traces_generator.generate_traces(grounded_output_path, problem_name, experiment_traces_amount)
                
                amount_of_traces_to_use = min(experiment_traces_bucket ,generated_traces_amount)
                
                self.copy_input_files()                                                                       
                                           
                #experiment_details = "Experiment #" + str(experiment_counter + i) + " - traces: " + str(amount_of_traces_to_use)        
                experiment_details = "Experiment #" + str(i) + " traces: " + str(amount_of_traces_to_use) + " bucket: " + str(experiment_traces_bucket)       
 
                traces_generator.copy_generation_output(problem_name, experiment_details)               
                                                
                self.plan(problem_name, amount_of_traces_to_use, experiment_traces_bucket, planning_mode, experiment_details, thresholdTimeoutInMS, Offline_Learning)
                agents, solved, timeout = self.get_ourplanner_results()
                
                if solved == 0:     
                    for iteration_method in iteration_methods:
                        self.plan(problem_name, amount_of_traces_to_use, experiment_traces_bucket, planning_and_learning_mode, experiment_details, experimentTimeoutInMS, iteration_method)

                    #self.plan(problem_name, amount_of_traces_to_use, experiment_traces_bucket, planning_and_learning_mode, experiment_details, experimentTimeoutInMS, BFS)
                    #self.plan(problem_name, amount_of_traces_to_use, experiment_traces_bucket, planning_and_learning_mode, experiment_details, experimentTimeoutInMS, Random)
                    #self.plan(problem_name, amount_of_traces_to_use, experiment_traces_bucket, planning_and_learning_mode, experiment_details, experimentTimeoutInMS, Plan_Length_And_Reliability_Heuristic)
                    #self.plan(problem_name, amount_of_traces_to_use, experiment_traces_bucket, planning_and_learning_mode, experiment_details, experimentTimeoutInMS, Monte_Carlo_Reliability_Heuristic, 2)
                    #self.plan(problem_name, amount_of_traces_to_use, experiment_traces_bucket, planning_and_learning_mode, experiment_details, experimentTimeoutInMS, Monte_Carlo_Reliability_Heuristic, 100)
                    #self.plan(problem_name, amount_of_traces_to_use, experiment_traces_bucket, planning_and_learning_mode, experiment_details, experimentTimeoutInMS, Monte_Carlo_Goal_Proximity_Heuristic, 2)
                    #self.plan(problem_name, amount_of_traces_to_use, experiment_traces_bucket, planning_and_learning_mode, experiment_details, experimentTimeoutInMS, Monte_Carlo_Goal_Proximity_Heuristic, 100)
                    #self.plan(problem_name, amount_of_traces_to_use, experiment_traces_bucket, planning_and_learning_mode, experiment_details, experimentTimeoutInMS, Monte_Carlo_Plan_Length_Heuristic, 2)
                    #self.plan(problem_name, amount_of_traces_to_use, experiment_traces_bucket, planning_and_learning_mode, experiment_details, experimentTimeoutInMS, Monte_Carlo_Plan_Length_Heuristic, 100)
                
                traces_generator.delete_output()

                #shuffled_traces_copy_path = self.planner_output_folder + "/" + experiment_details
                #self.shuffle_traces(shuffled_traces_copy_path)

    def search_solved_threshold(self, problem_config, problem_name, total_num_of_traces):
           
        logger.info("search_solved_threshold")
        
        min_traces = problem_config.thresholdSearchSetup.minTracesToUse
        max_traces = total_num_of_traces
        timeoutInMS = problem_config.thresholdSearchSetup.timeoutInMS
                
        self.tested_amounts = []
                
        while min_traces <= max_traces:
            
            solved_counter = 0
            timeout_counter = 0
    
            current_traces_amount = math.ceil((min_traces + max_traces) / 2)
            
            details = "Searching threshold"
                        
            self.plan(problem_name, current_traces_amount, planning_mode, details, timeoutInMS, Offline_Learning)
            agents, solved, timeout = self.get_ourplanner_results()
            
            solved_counter += solved
            timeout_counter += timeout
            
            self.tested_amounts.append(current_traces_amount)
            
            if solved_counter >= 1:
                max_traces = current_traces_amount - 1
            else:
                min_traces = current_traces_amount + 1
                
        return min(min_traces,total_num_of_traces) 
    
    def get_ourplanner_results(self):
    
        solved = 0
        timeout = 0
        agents = 0
        
        with open(self.planner_ssv_file_path, "rt" ) as outputCSV:
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
    
    def write_line_to_ourplanner_results(self, line):
            
        with open(self.planner_ssv_file_path, "at" ) as outputCSV:
            writer = csv.writer(outputCSV, lineterminator = '\n')           
            writer.writerow([line])

    
    def shuffle_traces(self, copy_path):
    
        logger.info("run shuffle traces")
                
        for dirpath, dirnames, filenames in os.walk(self.planner_traces_folder_path):
            for filename in filenames:
        
                trace_file_path = self.planner_traces_folder_path + "/" + filename
                
                processList = ['shuf', trace_file_path, '--output=' + trace_file_path]
                
                print(', '.join(processList))   
                
                process = subprocess.Popen(processList)
                process.wait()
                
        if not os.path.exists(copy_path):
            os.makedirs(copy_path)
        
        copy_tree(self.planner_traces_folder_path, copy_path)
