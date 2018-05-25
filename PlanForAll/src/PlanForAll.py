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
    
    for dirpath, dirnames, filenames in os.walk(args.problems):
        for filename in filenames:
            if filename.endswith(".pddl") :
                #print(filename)
                
                agent = 'temp'

                if os.path.exists(agent):
                    shutil.rmtree(agent)
                else:
                    os.makedirs(agent)
                
                agent = str(agent +"/" + (os.path.splitext(filename))[0] +".addl")  
                
                processList = ["./run_local.sh" 
                                 , args.creator 
                                 , args.domain
                                 , os.path.join(args.problems, filename) 
                                 , agent
                                 , args.heuristic
                                 , str(args.recursion)
                                 , str(args.timeout) ]
                
                print(', '.join(processList))
                
                subprocess.call(processList)
                
                
            
    end = time.time() 
    
    print("Done! (time - %0.4f" %(end - start)+")")
    

def parse_args():
    
    argparser = argparse.ArgumentParser()
    
    argparser.add_argument(
        "creator", help="creator class", type=str)
    argparser.add_argument(
        "domain", help="path to domain file", type=str)
    argparser.add_argument(
        "problems", help="path to problems folder", type=str)
    argparser.add_argument(
        "heuristic", help="heuristic", type=str)
    argparser.add_argument(
        "recursion", help="recursion level", type=int)
    argparser.add_argument(
        "timeout", help="timeout (min)", type=int)
        
    return argparser.parse_args()


if __name__ == '__main__':
    main()
    
