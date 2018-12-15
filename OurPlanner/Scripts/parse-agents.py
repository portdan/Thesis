#!/usr/bin/python2.7

import sys
import os
import argparse

AFILE_KEYWORDS = ["agents"]

verbose = False

def main():
                
        args = parse_args()
        
        agents = parse_agents_types(args.agents,args.domain,args.problem)
        
        for agent in agents:
            print (agent)        

def parse_agents_types(agent_file,domain_name,problem_name):
    """The main method for parsing a agents files."""

    with open(agent_file) as afile:
        pfile_array = get_file_as_array(afile)
    # Deal with front/end define, problem, :domain
    if pfile_array[0:4] != ['(', 'define', '(', 'problem']:
        print 'PARSING ERROR: Expected (define (problem ... at start of agents file'
        sys.exit(1)
    problem = pfile_array[4]
    if problem != problem_name:
        print 'ERROR - names don\'t match between problem and agent file.'
        sys.exit(1)
    if pfile_array[5:8] != [')', '(', ':domain']:
        print 'PARSING ERROR: Expected (:domain ...) after (define (problem ...)'
        sys.exit(1)
    domain = pfile_array[8]
    '''
    if domain != domain_name:
        print 'ERROR - names don\'t match between domain and agent file.'
        sys.exit(1)
    '''
    if pfile_array[9] != ')':
        print 'PARSING ERROR: Expected end of domain declaration'
        sys.exit(1)
        
    pfile_array = pfile_array[10:-1]

    opencounter = 0
    keyword = ''
        
    agent = ''
    type = ''
        
    was_makaf = False
    
    types_to_agents = {}  # Key = type_name, Value = agent_of_type
    agent_list = []  # {String}
    
    for word in pfile_array:
            
        if word == '(':
            opencounter += 1
        elif word == ')':
            opencounter -= 1
        elif word.startswith(':'):
            if word[1:] not in AFILE_KEYWORDS:
                print 'PARSING ERROR: Unknown keyword: ' + word[1:]
                print 'Known keywords: ' + str(AFILE_KEYWORDS)
            else:
                keyword = word[1:]
    
        if opencounter == 0:
            keyword = ''
    
        if not word.startswith(':'):
            if keyword == 'agents':                    
                if word == '-':
                    was_makaf = True       
                elif was_makaf:
                    type = word
                    if type not in types_to_agents:
                        types_to_agents[type] = set()
                    types_to_agents[type].add(agent)
                    was_makaf = False
                else:
                    agent = word
                    agent_list.append(agent)
                    
    return agent_list
    
def get_file_as_array(file_):
    """
    Returns the file split into array of words.
    Removes comments and separates parenthesis.
    """
    file_as_string = ""
        
    for line in file_:
            
        if ";" in line:
            line = line[:line.find(";")]
                
        line = (line.replace('\t', '').replace('\n', ' ')
                .replace('(', ' ( ').replace(')', ' ) '))
            
        file_as_string += line
            
    file_.close()
        
    return file_as_string.strip().split()
    
def parse_args():
        
        argparser = argparse.ArgumentParser()
        
        argparser.add_argument(
                "agents", help="path to agents file", type=str)
        argparser.add_argument(
                "domain", help="domain name", type=str)
        argparser.add_argument(
                "problem", help="problem name", type=str)
        
        return argparser.parse_args()


if __name__ == "__main__":
        main()
