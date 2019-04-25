'''
Created on Apr 6, 2019

@author: daniel
'''

import argparse
import pyckson
import logging.config
import time
import os
import shutil

from utils.Utils import clear_directory
from configuration import TesterConfig
from grounding import Grounder
from generating import Generator


logging.config.fileConfig("Configuration/logger_config.conf", disable_existing_loggers=False)
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


def run_tests(config, origin_domain_file_path, origin_problem_path_list):
    
    logger.info("run_tests")

    clear_directory(config.outputDestination)
    
    grounder = Grounder(config)
    generator = Generator(config)

    for problem_name, problem_file_path in origin_problem_path_list:
        generate_test_output_folder(origin_domain_file_path, problem_name, problem_file_path, config)
        grounder.ground_problem(origin_domain_file_path, problem_name ,problem_file_path)
        generator.generate_problems_and_traces(grounder.grounded_output_path, problem_name)
        

def main():
    
    start = time.time()
    
    args = parse_args()
    
    config = load_testing_configuration(args)
    
    origin_domain_file_path, origin_problem_path_list = generate_testing_list(config) 
    
    run_tests(config, origin_domain_file_path, origin_problem_path_list)
    
    end = time.time() 
        
    logger.info("Done! (time - %0.4f" % (end - start) + ")")

if __name__ == '__main__':
    main()
