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
    
    if os.path.exists(agents_folder):
        shutil.rmtree(agents_folder)
    else:
        os.makedirs(agents_folder)

    for dirpath, dirnames, filenames in os.walk(args.input):
        for filename in filenames:
            if filename.endswith(".pddl"):
                #print(filename)
                
                problem_name = os.path.splitext(filename)[0]
                
                if(filename != args.domain_name):
                
                    agents_path = str(agents_folder +"/" + problem_name +".addl")  
                
                    processList = ["./run_local.sh" 
                                 , args.creator 
                                 , args.input + "/" + args.domain_name
                                 , os.path.join(args.input, filename) 
                                 , agents_path
                                 , args.heuristic
                                 , str(args.recursion)
                                 , str(args.timeout) ]
                
                    print(', '.join(processList))
                
                    subprocess.call(processList)
                    
                    os.remove("out.csv") 
                    os.remove("output")
                    os.remove("output.sas")
                    
                    shutil.move('out.plan', args.traces + "/" + problem_name + ".plan")
    
    shutil.rmtree(agents_folder)
    
    end = time.time() 
    
    print("Done! (time - %0.4f" %(end - start)+")")
    

def parse_args():
    
    argparser = argparse.ArgumentParser()
    
    argparser.add_argument(
        "creator", help="creator class", type=str)
    argparser.add_argument(
        "input", help="path to input directory", type=str)
    argparser.add_argument(
        "domain_name", help="domain file name", type=str)
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
    
