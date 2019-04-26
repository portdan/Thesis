'''
Created on Mar 1, 2019

@author: dan
'''

import os
import time
import argparse
import subprocess
import shutil
import csv
import json

tested_amounts = []

def get_ourplanner_results(test_csv):
    
    solved = 0
    timeout = 0
    agents = 0
    
    with open(test_csv, "rb" ) as outputCSV:
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

def run_outplanner(args, problem):
    
    processList = ['java', '-jar', args.jar, args.creator, 
        args.grounded, 
        args.localview, 
        args.trajectories, 
        args.domain, 
        problem, 
        args.agents, 
        args.output, 
        args.outputtest]
    
    #print(', '.join(processList))
    
    process = subprocess.Popen(processList)
    process.wait()
    
    return get_ourplanner_results(args.outputtest)

def clear_last_traces(args):
    
    traces_folder = os.listdir(args.trajectories)
            
    for dir in traces_folder:
        trc = args.trajectories + '/' + dir
        shutil.rmtree(trc)

def copy_testing_traces(args, testing_traces, amount):
            
    for dir in testing_traces[:amount]:
        src = args.trajectoriessource + '/' + dir
        dst = args.trajectories + '/' + dir
        shutil.copytree(src, dst)

def get_solved_threshold(args, testing_traces, problemlist, num_of_problems, min_traces_range, max_traces_range):
    
    min_traces = min_traces_range
    max_traces = max_traces_range
    
    while min_traces < max_traces:
        
        solved_counter = 0
        timeout_counter = 0
        num_of_agents = 0

        current_traces_amount = (min_traces + max_traces) // 2
        
        clear_last_traces(args)
        
        copy_testing_traces(args, testing_traces, current_traces_amount)
        
        for problem in problemlist:
            if problem.endswith(".pddl"):
                agents, solved, timeout = run_outplanner(args, problem)
                
                solved_counter += solved
                timeout_counter += timeout
                num_of_agents = agents
                
                tested_amounts.append(current_traces_amount)
        
        if solved_counter == num_of_problems:
            max_traces = current_traces_amount - 1
        else:
            min_traces = current_traces_amount + 1
            
    return min_traces


def test_range_traces(args, testing_traces, problemlist, range_start, range_end, range_split):
    
    if (range_end-range_start) < range_split:
        range_step = 1
    else:
        range_step = (range_end-range_start) // range_split 
        
    range_splited = list(range(range_start, range_end, range_step))
    
    if range_end not in range_splited:
        range_splited.append(range_end)
    
    # test between 0 - solved_threshold
    
    for traces_amount in range_splited:
        
        if traces_amount not in tested_amounts:
                
            clear_last_traces(args)
            
            copy_testing_traces(args, testing_traces, traces_amount)
            
            for problem in problemlist:
                if problem.endswith(".pddl"):
                    run_outplanner(args, problem)
                    tested_amounts.append(traces_amount)

def main():
    
    start = time.time()

    args = parse_args()
    
    testing_traces = os.listdir(args.trajectoriessource)

    with open(args.problemlist) as file:
        problemlist = [line.strip() for line in file]
        
    num_of_problems = len(problemlist)
    
    min_traces_range = 0
    max_traces_range = 0
    unsolved_all_range_split = 0
    solved_all_range_split = 0    
        
    with open(args.testsetup) as json_file:
        data = json.load(json_file)
        min_traces_range = data["min_traces_range"]
        max_traces_range = data["max_traces_range"]
        unsolved_all_range_split = data["traces_range_split"][0]
        solved_all_range_split = data["traces_range_split"][1]
            
    solved_threshold = get_solved_threshold(args, testing_traces, problemlist, num_of_problems,min_traces_range, max_traces_range)
      
    test_range_traces(args, testing_traces, problemlist, 0, solved_threshold -1, unsolved_all_range_split)
                      
    test_range_traces(args, testing_traces, problemlist, solved_threshold, max_traces_range, solved_all_range_split)
                                                
    end = time.time() 
    
    print(("Done! (time - %0.4f" %(end - start)+")"))
    

def parse_args():
    
    argparser = argparse.ArgumentParser()
    
    argparser.add_argument(
        "creator", help="creator class", type=str)
    argparser.add_argument(
        "grounded", help="path to grounded problem directory", type=str)
    argparser.add_argument(
        "localview", help="path to localview directory", type=str)
    argparser.add_argument(
        "trajectories", help="path to trajectories directory", type=str)
    argparser.add_argument(
        "domain", help="domain file name", type=str)
    argparser.add_argument(
        "problem", help="problem file name", type=str)
    argparser.add_argument(
        "agents", help="path to agents file", type=str)
    argparser.add_argument(
        "output", help="path to output directory", type=str)
    argparser.add_argument(
        "outputtest", help="path to test output directory", type=str)
    argparser.add_argument(
        "jar", help="path to jar file", type=str)
    argparser.add_argument(
        "problemlist", help="path to problem list file", type=str)
    argparser.add_argument(
        "trajectoriessource", help="path to test setup file", type=str)
    argparser.add_argument(
        "testsetup", help="path to test setup file", type=str)
        
    return argparser.parse_args()


if __name__ == '__main__':
    main()
    
