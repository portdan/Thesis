'''
Created on Mar 4, 2020

@author: daniel
'''
import csv
import argparse
import logging.config
import time
import os
import shutil
from _ast import Num


logging.config.fileConfig("Configuration/logger_config.conf", disable_existing_loggers=False)
# Get the logger specified in the file
logger = logging.getLogger(__name__)

def parse_args():
    
    logger.info("parse_args")
    
    argparser = argparse.ArgumentParser()
    argparser.add_argument("inputFolder", help="path to input folder", type=str)
    argparser.add_argument("domainName", help="name of the domain", type=str)
    argparser.add_argument("outputFolder", help="path to output folder", type=str)
    argparser.add_argument("--agent", help="unify by number of agents",
                    action="store_true")
    argparser.add_argument("--ultra", help="unify by ultra",
                    action="store_true")
    
    args = argparser.parse_args()
    
    log = "args:\n"
    
    for arg in vars(args):
        log += arg + " - " + str(getattr(args, arg)) + "\n"
        
    logger.info(log)
    
    return args

def unify_by_agents(args):
    
    resultsByAgents = dict()
    resultsByAgentsHeaders = dict()
    
    input_path = args.inputFolder + args.domainName
    output_path = args.outputFolder + args.domainName
    
    if os.path.exists(output_path):
        shutil.rmtree(output_path)
    os.makedirs(output_path)

    for filename in sorted(os.listdir(input_path)):
        if filename.endswith(".csv"):
            with open(input_path + '/' + filename) as csv_file:
                
                header_line = ""
                num_of_agents = 0
                
                csv_reader = csv.reader(csv_file, delimiter=',')
                line_count = 0
                for row in csv_reader:
                    if line_count == 0:
                        header_line = row
                    else:
                        if line_count == 1:
                            num_of_agents = row[2]
                            if num_of_agents not in resultsByAgents:
                                resultsByAgents[num_of_agents] = list()
                                resultsByAgentsHeaders[num_of_agents] = header_line
                                
                        row[0] = args.domainName
                        resultsByAgents[num_of_agents].append(row)
                            
                    line_count += 1
            
    for key, value in resultsByAgents.items():
        
        with open(output_path + '/' + args.domainName + "_" + key + "_agents.csv", mode='w') as csv_file:
        
            writer = csv.writer(csv_file)
        
            writer.writerow(resultsByAgentsHeaders[key])
            
            for val in value:
                writer.writerow(val)
                    
   
def unify(args):
    
    header = list()
    rows = list()
    
    input_path = args.inputFolder + args.domainName
    output_path = args.outputFolder + args.domainName
    
    if os.path.exists(output_path):
        shutil.rmtree(output_path)
    os.makedirs(output_path)
    
    for filename in sorted(os.listdir(input_path)):
        if filename.endswith(".csv"):
            with open(input_path + '/' + filename) as csv_file:
                
                num_of_agents = 0
                
                csv_reader = csv.reader(csv_file, delimiter=',')
                line_count = 0
                for row in csv_reader:
                    if line_count == 0:
                        header = row
                    else:
                        if line_count == 1:
                            num_of_agents = row[2]
                            del header[16: 16 + 5*int(num_of_agents)]
                        
                        del row[16: 16 + 5*int(num_of_agents)]
                        row[0] = args.domainName
                        rows.append(row)
                            
                    line_count += 1
                    
    with open(output_path + '/' + args.domainName + ".csv", mode='w') as csv_file:
    
        writer = csv.writer(csv_file)
    
        writer.writerow(header)
        
        for row in rows:
            writer.writerow(row)

def ultra_unify(args):
    
    header = list()
    rows = list()
    
    input_path = args.inputFolder
    output_path = args.outputFolder
    
    if os.path.exists(output_path):
        shutil.rmtree(output_path)
    os.makedirs(output_path)
    
    for dirpath, dirnames, filenames in os.walk(input_path):
        for filename in filenames:
            if filename.endswith(".csv"):
                with open(dirpath + '/' + filename) as csv_file:
                                    
                    csv_reader = csv.reader(csv_file, delimiter=',')
                    line_count = 0
                    for row in csv_reader:
                        if line_count == 0:
                            header = row
                        else:
                            rows.append(row)
                                
                        line_count += 1
                    
    with open(output_path + '/' + args.domainName + ".csv", mode='w') as csv_file:
    
        writer = csv.writer(csv_file)
    
        writer.writerow(header)
        
        for row in rows:
            writer.writerow(row)

   
def main():
    
    start = time.time()
    
    args = parse_args()
    
    if args.ultra:
        ultra_unify(args)
    elif args.agent:
        unify_by_agents(args)
    else:
        unify(args)
    
    end = time.time() 
        
    logger.info("Done! (time - %0.4f" % (end - start) + ")")

if __name__ == '__main__':
    main()
