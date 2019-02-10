'''
Created on Mar 25, 2018

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

    agents_folder = 'temp'
    
    domain_name = os.path.splitext(os.path.basename(args.domain))[0]

    if os.path.exists(agents_folder):
        shutil.rmtree(agents_folder)
    os.makedirs(agents_folder)
        
    if os.path.exists(args.traces):
        shutil.rmtree(args.traces)
    os.makedirs(args.traces)
        
    if os.path.exists("out.csv"):
        os.remove("out.csv")

    for dirpath, dirnames, filenames in os.walk(args.problems):
        for filename in filenames:
            if filename.endswith(".pddl"):
                #print(filename)
                
                problem_name = os.path.splitext(filename)[0]

                if(problem_name != domain_name):
                                                        
                    agents_path = str(agents_folder +"/" + problem_name +".addl")  
                    
                    processList = ["./run_local.sh" 
                                , args.creator 
                                , args.domain 
                                , os.path.join(args.problems, filename) 
                                , agents_path
                                , args.heuristic
                                , str(args.recursion)
                                , str(args.timeout) ]
                    
                    print(', '.join(processList))
                        
                    
                    process = subprocess.Popen(processList)
                    process.wait()                
                        
                    #subprocess.call(processList)
    
                    if os.path.exists("out.plan"):
                        print(args.traces + "/" + problem_name + ".plan")
                        
                        old_trace_path = 'out.plan'
                        new_trace_folder = args.traces + "/" + problem_name
                        new_trace_path = new_trace_folder + "/" + problem_name + ".plan"
                        
                        old_problem_path = os.path.join(args.problems, filename)
                        new_problem_path = new_trace_folder + "/" + problem_name + ".pddl"
                        
                        os.makedirs(new_trace_folder)
                        shutil.move(old_trace_path, new_trace_path)
                        shutil.copy(old_problem_path, new_problem_path)

    if os.path.exists("output"):
        os.remove("output")
                
    if os.path.exists("output.sas"):
        os.remove("output.sas")
        
    if os.path.exists("out.csv"):
        os.remove("out.csv")
        
    if os.path.exists("temp"):
        shutil.rmtree("temp")
                                                
    end = time.time() 
    
    print("Done! (time - %0.4f" %(end - start)+")")
    

def parse_args():
    
    argparser = argparse.ArgumentParser()
    
    argparser.add_argument(
        "creator", help="creator class", type=str)
    argparser.add_argument(
        "domain", help="path to domain file", type=str)
    argparser.add_argument(
        "problems", help="path to problems directory", type=str)
    argparser.add_argument(
        "traces", help="path to traces folder", type=str)
    argparser.add_argument(
        "heuristic", help="heuristic", type=str)
    argparser.add_argument(
        "recursion", help="recursion level", type=int)
    argparser.add_argument(
        "timeout", help="timeout (min)", type=int)
        
    return argparser.parse_args()


if __name__ == '__main__':
    main()
    
