'''
Created on Mar 1, 2019

@author: dan
'''

import os
import time
import argparse
import subprocess
import shutil

ROOT_DIR = os.path.dirname(os.path.abspath(__file__))

def main():
    
    start = time.time()

    args = parse_args()
    
    with open(args.problemlist) as file:
        problemlist = [line.strip() for line in file]
        
    with open(args.testsetup) as file:
        lines = [line.strip() for line in file]
    
    testsetup = []
    
    for line in lines:
        testsetup.append([ int(vals) for vals in line.split()])
        
    dirnames = os.listdir(args.trajectoriessource)
        
    used_counter = 0
    
    dirlist = []

    for testline in testsetup:
        
        copy_size = testline[0]
        amount_of_copies = testline[1]
                
        for i in range(0, amount_of_copies):               
            dirlist.append(dirnames[used_counter:used_counter + copy_size])
            used_counter += copy_size;
    
    for dirs_to_copy in dirlist:
        
        for dir in dirs_to_copy:
            src = args.trajectoriessource + '/' + dir
            dst = args.trajectories + '/' + dir
            shutil.copytree(src, dst)

        for problem in problemlist:     
            if problem.endswith(".pddl"):       
                processList = ['java', '-jar', args.jar, 
                               args.creator,
                               args.grounded,
                               args.localview,
                               args.trajectories,
                               args.domain,
                               problem,
                               args.agents,
                               args.output,
                               args.outputtest
                               ]
                
                print(', '.join(processList))
                    
                process = subprocess.Popen(processList)
                process.wait()                
                                                
    end = time.time() 
    
    print("Done! (time - %0.4f" %(end - start)+")")
    

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
    
