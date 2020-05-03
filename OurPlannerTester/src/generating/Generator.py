'''
Created on Apr 24, 2019

@author: daniel
'''

import pyckson
import subprocess
import shutil
import os

from configuration import GeneratorConfig
from utils.Utils import clear_directory

import logging
logger = logging.getLogger(__name__)

class Generator(object):
    '''
    classdocs
    '''

    def __init__(self, config, log_output=False):
        '''
        Constructor
        '''
        self.config = config
        self.generator_output_traces_folder = None
        self.log_output = log_output
        self.max_traces_bucket = 0
        
    def get_num_of_random_walk_steps(self):
        
        generator_config = None
        
        with open(self.config.problemGeneratorConfig, 'r') as generatorConfigJson:
            generator_config = pyckson.load(GeneratorConfig, generatorConfigJson)
            
        return generator_config.numOfRandomWalkSteps

        
    def prepere_to_generate(self, grounded_output_path, problem_name, 
                            num_of_traces_to_generate, num_of_random_walk_steps):
     
        logger.info("prepere_to_generate")
           
        clear_directory(self.config.problemGeneratorInput, delete=True)
        clear_directory(self.config.problemGeneratorOutput)
        
        logger.info("problemGeneratorInput : " + self.config.problemGeneratorInput)
    
        shutil.copytree(grounded_output_path, self.config.problemGeneratorInput)

        domain_file_path_input = self.config.problemGeneratorInput + "/" + self.config.domainFileName
        problem_file_path_input = self.config.problemGeneratorInput + "/" + problem_name
        
        generator_config = None
        
        with open(self.config.problemGeneratorConfig, 'r') as generatorConfigJson:
            generator_config = pyckson.load(GeneratorConfig, generatorConfigJson)
            
        generator_config.domainFilePath = domain_file_path_input
        generator_config.problemFilePath = problem_file_path_input
        generator_config.numOfTracesToGenerate = num_of_traces_to_generate
        generator_config.numOfRandomWalkSteps = num_of_random_walk_steps
        
        self.max_traces_bucket = generator_config.numOfRandomWalkSteps * generator_config.numOfTracesToGenerate
        
        self.generator_output_traces_folder = generator_config.tracesDirPath
        
        with open(self.config.problemGeneratorConfig, 'w+') as generatorConfigJson:
            pyckson.dump(generator_config, generatorConfigJson)
            
    def run_generation(self):
    
        logger.info("run_generation")
        
        processList = ['java', '-jar', self.config.problemGeneratorJAR, self.config.problemGeneratorCreator, 
                       self.config.problemGeneratorConfig]
        
        print(', '.join(processList))
        
        if self.log_output:
            process = subprocess.Popen(processList, stdout=subprocess.PIPE)
            out, err = process.communicate()
            logger.info(str(out.decode('utf-8')))
        else:
            process = subprocess.Popen(processList)
            process.wait()

    def copy_generation_output(self, problem_name, experiment_details):

        logger.info("copy_generation_output")
        
        '''
        problem_name = os.path.basename(problem_file_path)
        problem_name = os.path.splitext(problem_name)[0]
        '''
                
        dst = self.config.outputDestination + "/" + problem_name + "/" + self.config.problemGeneratorOutputDestination + "/" + experiment_details
    
        logger.info("dst - " + dst)
    
        shutil.copytree(self.config.problemGeneratorOutput, dst)
            
    def delete_output(self):
        clear_directory(self.config.problemGeneratorInput)
        clear_directory(self.config.problemGeneratorOutput)
        
    def generate_traces(self, grounded_output_path, problem_name,
                         num_of_traces_to_generate, num_of_random_walk_steps):
        
        logger.info("generate traces")
        
        self.prepere_to_generate(grounded_output_path, problem_name, 
                                 num_of_traces_to_generate, num_of_random_walk_steps)

        self.run_generation() 
        
        total_num_of_traces = 0
        
        for dirpath, dirnames, filenames in os.walk(self.config.problemGeneratorOutput):
            for filename in filenames:
                str = filename.split("_")[-1]
                total_num_of_traces = int(str.split(".")[0])
        
        #self.copy_generation_output(problem_name)
        
        return total_num_of_traces