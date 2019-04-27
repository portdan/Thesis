'''
Created on Apr 24, 2019

@author: daniel
'''

import pyckson
import subprocess
import shutil

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
        
        
    def prepere_to_generate(self, grounded_output_path, problem_name, num_of_traces_to_generate):
     
        logger.info("prepere_to_generate")
           
        clear_directory(self.config.problemGeneratorInput, delete=True)
        clear_directory(self.config.problemGeneratorOutput)
        
        logger.info("problemGeneratorInput : " + self.config.problemGeneratorInput)
    
        shutil.copytree(grounded_output_path, self.config.problemGeneratorInput)

        domain_file_path_input = self.config.problemGeneratorInput + "/" + self.config.domainName
        problem_file_path_input = self.config.problemGeneratorInput + "/" + problem_name
        
        generator_config = None
        
        with open(self.config.problemGeneratorConfig, 'r') as generatorConfigJson:
            generator_config = pyckson.load(GeneratorConfig, generatorConfigJson)
            
        generator_config.domainFilePath = domain_file_path_input
        generator_config.problemFilePath = problem_file_path_input
        generator_config.numOfTracesToGenerate = num_of_traces_to_generate
        
        self.generator_output_traces_folder = generator_config.tracesDirPath
        
        with open(self.config.problemGeneratorConfig, 'w+') as generatorConfigJson:
            pyckson.dump(generator_config, generatorConfigJson)
            
    def run_generation(self):
    
        logger.info("run_generation")
        
        processList = ['java', '-jar', self.config.problemGeneratorJAR, self.config.problemGeneratorCreator, 
                       self.config.problemGeneratorConfig]
        
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
                
        dst = self.config.outputDestination + "/" + problem_name + "/" + self.config.problemGeneratorOutputDestination
    
        logger.info("dst - " + dst)
    
        shutil.copytree(self.config.problemGeneratorOutput, dst)
            
    def delete_output(self):
        clear_directory(self.config.problemGeneratorInput)
        clear_directory(self.config.problemGeneratorOutput)
        
    def generate_problems_and_traces(self, grounded_output_path, problem_name, num_of_traces_to_generate):
        
        logger.info("generate_problems_and_traces")
        
        self.prepere_to_generate(grounded_output_path, problem_name, num_of_traces_to_generate)
        
        self.run_generation()   
        
        self.copy_generation_output(problem_name)