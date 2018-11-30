#!/usr/bin/python2.7

import sys
import os
import argparse
import time
# from sets import Set

AFILE_KEYWORDS = ["agents"]

verbose = False


class DomainGrounder(object):

  def __init__(self, domainfile, agentfile):
    self.domain = ''  # String
    self.types_to_agents = {}  # Key = type_name, Value = agent_of_type

    self.parse_domain(domainfile)
    self.parse_agents_types(agentfile)
      
  def parse_domain(self, domainfile):
    """Parses a PDDL domain file."""
    
    with open(domainfile) as dfile:
      dfile_array = self._get_file_as_array(dfile)
    # Deal with front/end define, problem, :domain
    if dfile_array[0:4] != ['(', 'define', '(', 'domain']:
      print ('PARSING ERROR: Expected (define (domain ... at start of domain file')
      sys.exit()
    self.domain = dfile_array[4]
  
  def parse_agents_types(self, agentfile):
    """The main method for parsing a agents files."""

    with open(agentfile) as afile:
      pfile_array = self._get_file_as_array(afile)
    # Deal with front/end define, problem, :domain
    if pfile_array[0:4] != ['(', 'define', '(', 'problem']:
      print 'PARSING ERROR: Expected (define (problem ... at start of problem file'
      sys.exit()
    self.problem = pfile_array[4]
    if pfile_array[5:8] != [')', '(', ':domain']:
      print 'PARSING ERROR: Expected (:domain ...) after (define (problem ...)'
      sys.exit()
    if self.domain != pfile_array[8]:
      print 'ERROR - names don\'t match between domain and agent file.'
      # sys.exit()
    if pfile_array[9] != ')':
      print 'PARSING ERROR: Expected end of domain declaration'
      sys.exit()
    pfile_array = pfile_array[10:-1]

    opencounter = 0
    keyword = ''
    
    agent = ''
    type = ''
    
    was_makaf = False
    
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

      if keyword == 'agents':
          
        if word == '-':
          was_makaf = True       
        elif was_makaf:
          type = word
          if type not in self.types_to_agents:
            self.types_to_agents[type] = set()
          self.types_to_agents[type].add(agent)
          was_makaf = False
        else:
          agent = word      
    
  # Get string of file with comments removed - comments are rest of line after ';'
  def _get_file_as_array(self, file_):
    """Returns the file split into array of words.

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
          
  def ground_domain(self, domain_file, output_file):
    """Writes an grounded unfactored MA-PDDL domain file."""
    
    # Read in the file
    with open(domain_file) as file :
      filedata = file.read()
      file.close()

    # Replace the target string
    # filedata = filedata.replace('ram', 'abcd')
    
    start_index = filedata.find('(:types')
    end_index = filedata.find(')', start_index)
    
    added_agents_types = ''
    replaced_agents_types = ''
    
    for type, agents in self.types_to_agents.iteritems():
      for agent in agents:
        added_agents_types += "\t" + agent + " - " + type + "\n"
        
    replaced_agents_types = filedata[end_index:].replace('truck', 'tru1')
      
    filedata = filedata[:end_index] + added_agents_types + replaced_agents_types
    
    # Write the file out again
    with open(output_file, 'w') as file:
      file.write(filedata)
      file.close()

    
def main():
    
    start = time.time()

    args = parse_args()
    
    dg = DomainGrounder(args.domain, args.agents)

    if not os.path.exists(args.output):
      os.makedirs(args.output)
              
    dg.ground_domain(args.domain, args.output + "/" + dg.domain)

    end = time.time() 
    
    print("Done! (time - %0.4f" % (end - start) + ")")    


def parse_args():
    
    argparser = argparse.ArgumentParser()
    
    argparser.add_argument(
        "domain", help="path to domain file", type=str)
    argparser.add_argument(
        "agents", help="path to agents file", type=str)
    argparser.add_argument(
        "output", help="output path", type=str)
    
    return argparser.parse_args()


if __name__ == "__main__":
    main()
