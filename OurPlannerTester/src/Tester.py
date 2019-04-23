'''
Created on Apr 6, 2019

@author: daniel
'''

import argparse
import pyckson
from TesterConfig import TesterConfig
from GrounderConfig import GrounderConfig
import subprocess

import logging.config

import time

import shutil
import os

logging.config.fileConfig("Configuration/logger_config.conf")
# Get the logger specified in the file
logger = logging.getLogger(__name__)

def parse_args():
    
    logger.info("parse_args")
    
    argparser = argparse.ArgumentParser()
    
    argparser.add_argument("config", help=".json configuration file", type=str)
    
    args = argparser.parse_args()
    
    log = "args:\n"
    
    for arg in vars(args):
        log += arg + " - " + getattr(args, arg)+ "\n"
        
    logger.info(log)
    
    return args


def load_testing_configuration(args):
    
    logger.info("load_testing_configuration")
    
    with open(args.config) as configJsonFile:
        config = pyckson.load(TesterConfig, configJsonFile)
        logger.info("configuration file:\n" + str(config))
        return config
    
def generate_testing_list(config):
    
    logger.info("generate_testing_list")

    res = []
    
    domain_path = config.domainPath + '/' + config.domainName
    
    log = "testing list:\n"
    for problem in config.problems:
        
        problem_file_path = config.problemsPath + '/' + problem.problemName
        r = (problem.problemName , problem_file_path)
        log += str(r) + "\n"
        res.append(r)
                 
    logger.info(log)  
    return domain_path, res

def clear_directory(dirpath):
    
    logger.info("clear_directory : " + dirpath)
    
    if os.path.exists(dirpath):
        shutil.rmtree(dirpath)
    os.mkdir(dirpath)


def prepere_to_ground(domain_file_path, problem_file_path, config):
 
    logger.info("prepere_to_ground")
       
    clear_directory(config.problemGrounderInput)
    clear_directory(config.problemGrounderOutput)
    
    logger.info("problemGrounderInput : " + config.problemGrounderInput)

    domain_file_path_input = shutil.copy2(domain_file_path, config.problemGrounderInput)
    problem_file_path_input = shutil.copy2(problem_file_path, config.problemGrounderInput)
    
    grounder_config = None
    
    with open(config.problemGrounderConfig, 'r') as grounderConfigJson:
        grounder_config = pyckson.load(GrounderConfig, grounderConfigJson)
        
    grounder_config.domainPath = domain_file_path_input
    grounder_config.problemPath = problem_file_path_input
    
    with open(config.problemGrounderConfig, 'w+') as grounderConfigJson:
        pyckson.dump(grounder_config, grounderConfigJson)
        

def run_grounding(config):

    logger.info("run_grounding")
    
    processList = ['java', '-jar', config.problemGrounderJAR, config.problemGrounderCreator, config.problemGrounderConfig]
    
    print(', '.join(processList))
    
    process = subprocess.Popen(processList)
    process.wait()
    

def ground_problem(domain_file_path, problem_name, problem_file_path, config):
    
    logger.info("ground_problem")
    
    prepere_to_ground(domain_file_path, problem_file_path, config)
    
    run_grounding(config)   
    
    generate_grounding_output_folder(domain_file_path, problem_name ,problem_file_path, config)       


def generate_test_output_folder(domain_file_path, problem_name, problem_file_path, config):

    logger.info("generate_test_output_folder")
    
    ''' 
    problem_name = os.path.basename(problem_file_path)
    problem_name = os.path.splitext(problem_name)[0]
    '''
    
    logger.info("problem_name : " + problem_name)
    
    dst = config.outputDestination + "/" + problem_name

    clear_directory(dst)

    origin_dst = dst + "/" + config.originDestination
    logger.info("origin_dst - " + origin_dst)

    os.makedirs(origin_dst)

    shutil.copy2(domain_file_path, origin_dst)
    shutil.copy2(problem_file_path, origin_dst )

def generate_grounding_output_folder(domain_file_path, problem_name, problem_file_path, config):

    logger.info("generate_grounding_output_folder")
    
    '''
    problem_name = os.path.basename(problem_file_path)
    problem_name = os.path.splitext(problem_name)[0]
    '''
    
    logger.info("problem_name : " + problem_name)
    
    dst = config.outputDestination + "/" + problem_name + "/" + config.problemGrounderOutputDestination

    logger.info("dst - " + dst)

    shutil.copytree(config.problemGrounderOutput, dst)


def run_tests(config, domain_file_path, problem_path_list):
    
    logger.info("run_tests")

    clear_directory(config.outputDestination)

    for problem_name, problem_file_path in problem_path_list:
        generate_test_output_folder(domain_file_path, problem_name, problem_file_path, config)
        ground_problem(domain_file_path, problem_name ,problem_file_path, config)
        

def main():
    
    start = time.time()
    
    args = parse_args()
    
    config = load_testing_configuration(args)
    
    domain_file_path, problem_path_list = generate_testing_list(config) 
    
    run_tests(config, domain_file_path, problem_path_list)
    
    end = time.time() 
        
    logger.info("Done! (time - %0.4f" % (end - start) + ")")

if __name__ == '__main__':
    main()
