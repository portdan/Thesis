#!/usr/bin/python2.7

import sys
import os
import shutil
import argparse
import time
import copy
import collections

# from sets import Set

DFILE_KEYWORDS = ["requirements", "types", "predicates", "action", "private", "functions", "constants"]
DFILE_REQ_KEYWORDS = ["typing", "strips", "multi-agent", "unfactored-privacy"]
DFILE_SUBKEYWORDS = ["parameters", "precondition", "effect", "duration"]
PFILE_KEYWORDS = ["objects", "init", "goal", "private", "metric"]
AFILE_KEYWORDS = ["agents"]

PUBLIC = "public"

verbose = False

class Predicate(object):
    """A loose interpretation of a predicate used for all similar collections.

    Without a name it is a parameter list.
    It can be typed (or not).
        If typed then args = [[var, type], ...]
        Else args = [var, ...]
    It can be negated.
    It may contain variables or objects in its arguments.
    """

    def __init__(self, name, args, is_typed, is_negated):
        self.name = name
        self.args = args
        self.arity = len(args)
        self.is_typed = is_typed
        self.is_negated = is_negated
        self.ground_facts = set()
        self.agent_param = -1

    def pddl_rep(self):
        """Returns the PDDL version of the instance."""
        rep = ''
        if self.is_negated:
            rep += "(not "
        if self.name != "":
            rep += "(" + self.name + " "
        else:
            rep += "("
        for argument in self.args:
            if self.is_typed:
                rep += argument[0] + " - " + argument[1] + " "
            else:
                rep += argument + " "
        rep = rep[:-1]
        rep += ")"
        if self.is_negated:
            rep += ")"
        return rep
    
    def pddl_rep_sub(self, from_index):
        """Returns the PDDL version of the instance."""
        rep = ''
        if self.is_negated:
            rep += "(not "
        if self.name != "":
            rep += "(" + self.name + " "
        else:
            rep += "("
        for argument in self.args[from_index:]:
            if self.is_typed:
                rep += argument[0] + " - " + argument[1] + " "
            else:
                rep += argument + " "
        rep = rep[:-1]
        rep += ")"
        if self.is_negated:
            rep += ")"
        return rep    

    def __repr__(self):
        return self.pddl_rep()

class Action(object):
    """Represents a simple non-temporal action."""

    def __init__(self, name, parameters, precondition, effect):
        self.name = name
        self.parameters = parameters
        self.precondition = precondition
        self.effect = effect
        self.duration = 1  
        self.agent_param = parameters.args[0][0]
        self.agent_type = parameters.args[0][1]

    def pddl_rep(self):
        """Returns the PDDL version of the instance."""
        rep = ''
        rep += "(:action " + self.name + "\n"
        rep += "\t:parameters " + str(self.parameters) + "\n"
        if len(self.precondition) > 1:
            rep += "\t:precondition (and\n"
        else:
            rep += "\t:precondition \n"
        for precon in self.precondition:
            rep += "\t\t" + str(precon) + "\n"
        if len(self.precondition) > 1:
            rep += "\t)\n"
        if len(self.effect) > 1:
            rep += "\t:effect (and\n"
        else:
            rep += "\t:effect \n"
        for eff in self.effect:
            rep += "\t\t" + str(eff) + "\n"
        if len(self.effect) > 1:
            rep += "\t)\n"
        rep += ")\n"
        return rep

    def ma_pddl_rep(self):
        """Returns the MA-PDDL version of the instance."""
        rep = ''
        rep += "(:action " + self.name + "\n"
        rep += "\t:agent " + str(self.agent_param) + " - " + str(self.agent_type) + "\n"
        rep += "\t:parameters " + str(self.parameters.pddl_rep_sub(1)) + "\n"
        if len(self.precondition) > 1:
            rep += "\t:precondition (and\n"
        else:
            rep += "\t:precondition \n"
        for precon in self.precondition:
            rep += "\t\t" + str(precon) + "\n"
        if len(self.precondition) > 1:
            rep += "\t)\n"
        if len(self.effect) > 1:
            rep += "\t:effect (and\n"
        else:
            rep += "\t:effect \n"
        for eff in self.effect:
            rep += "\t\t" + str(eff) + "\n"
        if len(self.effect) > 1:
            rep += "\t)\n"
        rep += ")\n"
        return rep

    def __repr__(self):
        return self.name  # + str(self.parameters)

class Function(object):

    def __init__(self, obj_list):
        self.obj_list = obj_list

    def pddl_rep(self):
        """Returns the PDDL version of the instance."""
        rep = '('
        for argument in self.obj_list:
            rep += argument + " "
        rep = rep[:-1]
        rep += ") - number"
        return rep

    def __repr__(self):
        return self.pddl_rep()

class GroundFunction(object):

    def __init__(self, obj_list):
        self.obj_list = obj_list

    def pddl_rep(self):
        """Returns the PDDL version of the instance."""
        rep = '(' + self.obj_list[0] + " ("
        for argument in self.obj_list[1:-1]:
            rep += argument + " "
        rep = rep[:-1]
        rep += ") " + self.obj_list[-1] + ") "
        return rep

    def __repr__(self):
        return self.pddl_rep()

class Grounder(object):

    def __init__(self, domainfile, problemfile, agentfile):
        """Initialize all Data structures."""      
        # From domainfile
        self.domain = ''  # String
        self.requirements = set()  # {String}
        self.type_list = set()  # {String}
        self.type_list.add('object')
        self.types = {}  # Key = supertype_name, Value = type
        self.predicates = []  # [Predicate]
        self.functions = []
        self.ground_functions = []
        self.predicate_map = {}  # key = name, value = Predicate
        self.agent_predicates = {}  # key = agent/public, value = Predicate
        self.agent_facts = {}  # key = agent/public, value = Fact
        self.actions = []  # [Action]      
        self.agent_types = set()  # {String}
        self.constants = {}  # Key = type, Value = object_name
        
        # From problemfile
        self.problem = ''  # String
        self.object_list = set()  # {String}
        self.objects = {}  # Key = type, Value = object_name
        self.init = []  # List of Predicates
        self.goal = []  # List of Predicates
        self.metric = False
        self.agent_to_objects = {}  # key = agent/public, value = Object
        self.agent_to_objects[PUBLIC] = {}
        self.agent_init = {}  # key = agent/public, value = Init
        self.agent_goal = {}  # key = agent/public, value = Goal
        
        # From agentfile
        self.types_to_agents = {}  # Key = type_name, Value = agent_of_type
        self.agent_list = []  # {String}

        # Parsing
        self.parse_domain(domainfile)
        self.parse_problem(problemfile)        
        self.parse_agents_types(agentfile)
        
    def parse_domain(self, domainfile):
        """Parses a PDDL domain file."""
        
        with open(domainfile) as dfile:
            dfile_array = self._get_file_as_array(dfile)
        # Deal with front/end define, problem, :domain
        if dfile_array[0:4] != ['(', 'define', '(', 'domain']:
            print 'PARSING ERROR: Expected (define (domain ... at start of domain file'
            sys.exit()
        self.domain = dfile_array[4]

        dfile_array = dfile_array[6:-1]
        opencounter = 0
        keyword = ''
        
        agent_pred = PUBLIC
        
        obj_list = []
        is_obj_list = True
        for word in dfile_array:
            if word == '(':
                opencounter += 1
            elif word == ')':
                opencounter -= 1
            elif word.startswith(':'):
                if word[1:] not in DFILE_KEYWORDS:
                    pass
                elif keyword != 'requirements':
                    keyword = word[1:]
            if opencounter == 0:
                if keyword == 'action':
                    self.actions.append(obj_list)
                    obj_list = []
                if keyword == 'types':
                    for element in obj_list:
                        self.types.setdefault('object', []).append(element)
                        self.type_list.add('object')
                        self.type_list.add(element)
                    obj_list = []
                keyword = ''

            if keyword == 'requirements':  # Requirements list
                if word != ':requirements':
                    if not word.startswith(':'):
                        print 'PARSING ERROR: Expected requirement to start with :'
                        sys.exit()
                    elif word[1:] not in DFILE_REQ_KEYWORDS:
                        print 'WARNING: Unknown Rquierement ' + word[1:]
                        # print 'Requirements must only be: ' + str(DFILE_REQ_KEYWORDS)
                        # sys.exit()
                    else:
                        self.requirements.add(word[1:])
            elif keyword == 'action':
                obj_list.append(word)
            elif not word.startswith(':'):
                if keyword == 'types':  # Typed list of objects
                    if is_obj_list:
                        if word == '-':
                            is_obj_list = False
                        else:
                            obj_list.append(word)
                    else:
                        # word is type
                        for element in obj_list:
                            if not word in self.type_list:
                                self.types.setdefault('object', []).append(word)
                                self.type_list.add(word)
                            self.types.setdefault(word, []).append(element)
                            self.type_list.add(element)
                            self.type_list.add(word)
                        is_obj_list = True
                        obj_list = []
                elif keyword == 'constants':  # Typed list of objects
                    if is_obj_list:
                        if word == '-':
                            is_obj_list = False
                        else:
                            obj_list.append(word)
                    else:
                        # word is type
                        for element in obj_list:
                            if word in self.type_list:
                                self.constants.setdefault(word, []).append(element)
                                # self.object_list.add(element)
                            else:
                                print self.type_list
                                print "ERROR unknown type " + word
                                sys.exit()
                        is_obj_list = True
                        obj_list = []
                elif keyword == 'predicates' or keyword == 'private':  # Internally typed predicates
                    if word == ')':
                                                
                        if keyword == 'private':
                            # print "...skip agent: " +    str(obj_list[:3])
                            
                            agent_pred = obj_list[2]
                            
                            obj_list = obj_list[3:]
                            keyword = 'predicates'
                        if len(obj_list) == 0:
                            # print "...skip )"

                            agent_pred = PUBLIC
                            continue
                        p_name = obj_list[0]
                        # print "parse predicate: " + p_name + " " + str(obj_list)
                        pred_list = self._parse_name_type_pairs(obj_list[1:], self.type_list)
                        self.predicates.append(Predicate(p_name, pred_list, True, False))
                        
                        # add public/agent predicates to list
                        if not agent_pred in self.agent_predicates.keys():
                            self.agent_predicates[agent_pred] = set()
                        self.agent_predicates[agent_pred].add(Predicate(p_name, pred_list, True, False))
                        
                        obj_list = []
                    elif word != '(':
                        obj_list.append(word)
                elif keyword == 'functions':  # functions
                    if word == ')':
                        p_name = obj_list[0]
                        if obj_list[0] == '-':
                            obj_list = obj_list[2:]
                        # print "function: " + word + " - " + str(obj_list)
                        self.functions.append(Function(obj_list))
                        obj_list = []
                    elif word != '(':
                        obj_list.append(word)

        # Work on the actions
        new_actions = []
        for action in self.actions:
            if action[0] == '-':
                action = action[2:]
            act_name = action[1]
            act = {}
            action = action[2:]
            keyword = ''
            for word in action:
                if word.startswith(':'):
                    keyword = word[1:]
                else:
                    act.setdefault(keyword, []).append(word)
            self.agent_types.add(act.get('agent')[2])
            agent = self._parse_name_type_pairs(act.get('agent'), self.type_list)
            param_list = agent + self._parse_name_type_pairs(act.get('parameters')[1:-1], self.type_list)
            up_params = Predicate('', param_list, True, False)
            pre_list = self._parse_unground_propositions(act.get('precondition'))
            eff_list = self._parse_unground_propositions(act.get('effect'))
            new_act = Action(act_name, up_params, pre_list, eff_list)

            new_actions.append(new_act)
        self.actions = new_actions

    def parse_problem(self, problemfile):
        """The main method for parsing a PDDL files."""

        with open(problemfile) as pfile:
            pfile_array = self._get_file_as_array(pfile)
        # Deal with front/end define, problem, :domain
        if pfile_array[0:4] != ['(', 'define', '(', 'problem']:
            print 'PARSING ERROR: Expected (define (problem ... at start of problem file'
            sys.exit()
        self.problem = pfile_array[4]
        if pfile_array[5:8] != [')', '(', ':domain']:
            print 'PARSING ERROR: Expected (:domain ...) after (define (problem ...)'
            sys.exit()
        if self.domain != pfile_array[8]:
            print 'ERROR - names don\'t match between domain and problem file.'
            # sys.exit()
        if pfile_array[9] != ')':
            print 'PARSING ERROR: Expected end of domain declaration'
            sys.exit()
        pfile_array = pfile_array[10:-1]

        opencounter = 0
        keyword = ''
        is_obj_list = True
        is_function = False
        
        agent = PUBLIC
        
        obj_list = []
        int_obj_list = []
        int_opencounter = 0
        for word in pfile_array:
            if word == '(':
                opencounter += 1
            elif word == ')':
                if keyword == 'objects':
                    obj_list = []
                    
                    agent = PUBLIC
                    
                opencounter -= 1
            elif word.startswith(':'):
                if word[1:] not in PFILE_KEYWORDS:
                    print 'PARSING ERROR: Unknown keyword: ' + word[1:]
                    print 'Known keywords: ' + str(PFILE_KEYWORDS)
                else:
                    keyword = word[1:]
            if opencounter == 0:
                keyword = ''

            if not word.startswith(':'):
                if keyword == 'objects' or keyword == 'private':  # Typed list of objects
                    # print "word: " + word
                    # print "obj_list: " + str(obj_list)
                    if keyword == 'private':
                            # print "...skip agent: " +    word
                            obj_list = []
                            keyword = 'objects'
                            
                            agent = word
                            
                            # create agent private objects list
                            if not agent in self.agent_to_objects.keys():
                                self.agent_to_objects[agent] = {}
                
                            continue
                    if is_obj_list:
                        if word == '-':
                            is_obj_list = False
                        else:
                            obj_list.append(word)
                    else:
                        # word is type
                        for element in obj_list:
                            if word in self.type_list:
                                self.objects.setdefault(word, []).append(element)
                                self.object_list.add(element)
                                
                                # add agent private / public objects to list
                                # if not word in self.agent_to_objects[agent]:
                                #    self.agent_to_objects[agent][word]={}
                                self.agent_to_objects[agent].setdefault(word, []).append(element)

                            else:
                                print self.type_list
                                print "ERROR unknown type " + word
                                sys.exit()
                        is_obj_list = True
                        obj_list = []
                elif keyword == 'init':
                    if word == ')':
                        if obj_list[0] == '=' and is_function == False:
                            is_function = True
                        else:
                            if is_function:
                                # print "function: " + str(obj_list)
                                self.ground_functions.append(GroundFunction(obj_list))
                                is_function = False
                            else:
                                # print "predicate: " + str(obj_list)
                                self.init.append(Predicate(obj_list[0], obj_list[1:], False, False))
                                
                                # add init to agent bucket                                                           
                                possible_agents = set()
                                for i in obj_list[1:]:
                                    possible_agents.add(self._finditem(self.agent_to_objects, i))
                                
                                if PUBLIC in possible_agents:    
                                    possible_agents.remove(PUBLIC)
                                
                                if possible_agents:
                                    for ag in possible_agents:
                                        self.agent_init.setdefault(ag, []).append(Predicate(obj_list[0], obj_list[1:], False, False))                                
                                else:
                                    self.agent_init.setdefault(PUBLIC, []).append(Predicate(obj_list[0], obj_list[1:], False, False))
                                    
                            obj_list = []
                    elif word != '(':
                        obj_list.append(word)
                elif keyword == 'goal':
                    if word == '(':
                        int_opencounter += 1
                    elif word == ')':
                        int_opencounter -= 1
                    obj_list.append(word)
                    if int_opencounter == 0:
                            self.goal = self._parse_unground_propositions(obj_list)

                            # add goal to agent bucket                           
                            for g in self.goal:
                            
                                possible_agents = set()
                                for i in g.args:
                                    possible_agents.add(self._finditem(self.agent_to_objects, i))
                                
                                if PUBLIC in possible_agents:    
                                    possible_agents.remove(PUBLIC)
                                
                                if possible_agents:
                                    for ag in possible_agents:
                                        self.agent_goal.setdefault(ag, []).append(g)                                
                                else:
                                    self.agent_goal.setdefault(PUBLIC, []).append(g)
                                
                            '''    
                                possible_agents = set(g.args).intersection(self.agent_to_objects.keys())
                                if possible_agents:
                                    for ag in possible_agents:
                                        self.agent_goal.setdefault(ag,[]).append(g)                                
                                else:
                                    self.agent_goal.setdefault(PUBLIC,[]).append(g)                
                            '''
                            obj_list = []
                elif keyword == 'metric':
                    self.metric = True
                    obj_list = []

    # recursively finds key of the given 'val'
    def _finditem(self, obj, val):
        if isinstance(obj, list):
            for v in obj:
                if v == val:
                    return val
            return None       
        else:
            for k, v in obj.items():
                if k == val or v == val:
                    return k
                if isinstance(v, collections.Iterable):
                    item = self._finditem(v, val)
                    if item is not None:
                        return k

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
    
            if not word.startswith(':'):
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
                        self.agent_list.append(agent)
    
    def _get_file_as_array(self, file_):
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
    
    def _parse_name_type_pairs(self, array, types):
        """
        Parses array creating paris of form (name, type).
        Expects array such as [?a, -, agent, ...].
        """
        pred_list = []
        if len(array) % 3 != 0:
            print "Expected predicate to be typed " + str(array)
            sys.exit()
            
        for i in range(0, len(array) / 3):
            if array[3 * i + 1] != '-':
                print "Expected predicate to be typed"
                sys.exit()
            if array[3 * i + 2] in types:
                pred_list.append((array[3 * i], array[3 * i + 2]))
            else:
                print "PARSING ERROR {} not in types list".format(array[3 * i + 2])
                print "Types list: {}".format(self.type_list)
                sys.exit()
                    
        return pred_list
    
    def _parse_unground_propositions(self, array):
        """
        Parses possibly conjunctive list of unground propositions.
        Expects array such as [(and, (, at, ?a, ?x, ), ...].
        """
        prop_list = []
        if array[0:3] == ['(', 'and', '(']:
            array = array[2:-1]
        # Split array into blocks
        opencounter = 0
        prop = []
        
        for word in array:
            if word == '(':
                opencounter += 1
            if word == ')':
                opencounter -= 1
            prop.append(word)
            if opencounter == 0:
                prop_list.append(self._parse_unground_proposition(prop))
                prop = []
        # print array[:array.index(')') + 1]
        return prop_list

    def _parse_unground_proposition(self, array):
        """Parses a variable proposition returning dict."""
        negative = False
        
        if array[1] == 'not':
            negative = True
            array = array[2:-1]
            
        return Predicate(array[1], array[2:-1], False, negative)

    def ground_domain(self):
        """ grounds the unfactored MA-PDDL domain file."""
        
        new_actions = []  # [new actions to add]
        new_types = copy.deepcopy(self.types)  # {new types to add}
        new_types_list = copy.deepcopy(self.type_list)  # {new type_list to add}

        for type, agents in self.types_to_agents.iteritems():
            for agent in agents:
                # Add spesific agents types
                new_types.setdefault(type, []).append(agent)
                new_types_list.add(type)
                new_types_list.add(agent)
                
                for action in self.actions:
                    if action.agent_type == type:
                        new_action = copy.deepcopy(action);
                        new_action.name += "-" + agent
                        new_action.agent_type = agent                    
                        new_actions.append(new_action) 
                        
        self.types = new_types
        self.type_list = new_types_list
        self.actions = new_actions
    
    def ground_domain_for_agent(self, agent_name):
        """ grounds the unfactored MA-PDDL domain file for a specific agent."""
        
        new_actions = []  # [new actions to add]
                  
        for action in self.actions:
            if action.agent_type == agent_name:                 
                new_actions.append(action) 

        self.actions = new_actions

                                 
    def write_grounded_domain(self, output_file):
        """Writes an grounded unfactored MA-PDDL domain file."""
        
        file_ = open(output_file, 'w')
        to_write = "(define (domain " + self.domain + ")\n"
        # Requirements
        to_write += "\t(:requirements :typing :multi-agent :unfactored-privacy)\n"
        # Types
        to_write += "(:types\n"
        for type_ in self.types:
            to_write += "\t"
            for key in self.types.get(type_):
                to_write += key + " "
            to_write += "- " + type_
            to_write += "\n"
        to_write += ")\n"
        # Constants
        if len(self.constants) > 0:
            to_write += "(:constants\n"
            for t in self.constants.iterkeys():
                to_write += "\t"
                for c in self.constants[t]:
                    to_write += c + " "
                to_write += " - " + t + "\n" 
            to_write += ")\n"
        # Public predicates
        to_write += "(:predicates\n"
        for predicate in self.agent_predicates[PUBLIC]:
            to_write += "\t{}\n".format(predicate.pddl_rep())
        for agent_type in self.agent_predicates.iterkeys():
            if agent_type != PUBLIC and len(self.agent_predicates[agent_type]) > 0:
                to_write += "\n\t{}\n".format("(:private ?agent - " + agent_type)
                for predicate in self.agent_predicates[agent_type]:
                    to_write += "\t\t{}\n".format(predicate.pddl_rep())
                to_write += "\t)\n"
        to_write += ")\n"
        # Functions
        if len(self.functions) > 0:
            to_write += "(:functions\n"
            for function in self.functions:
                to_write += "\t{}\n".format(function.pddl_rep())
            to_write += ")\n"
        # Actions
        for action in self.actions:
            to_write += "\n{}\n".format(action.ma_pddl_rep())
        
        # Endmatter
        to_write += ")"  # Close domain defn
        file_.write(to_write)
        file_.close()

    def ground_problem(self):
        """ grounds the unfactored MA-PDDL problem file."""
        
        new_agent_to_objects = {}  # key = agent/public, value = Object = []

        for agent_name, object_type_to_name in self.agent_to_objects.iteritems():
            new_object_type_to_name = copy.deepcopy(object_type_to_name) 
            for object_name in new_object_type_to_name:
                if object_name in self.types_to_agents:
                    new_object_type_to_name[agent_name] = new_object_type_to_name.pop(object_name)
                new_agent_to_objects[agent_name] = new_object_type_to_name

        self.agent_to_objects = new_agent_to_objects
        
    def remove_privacy_from_problem(self):
        """ removes all privacy from unfactored MA-PDDL problem file."""
        
        new_agent_to_objects = {}  # key = agent/public, value = Object = []
        new_agent_to_objects[PUBLIC] = {}

        for agent_name, object_type_to_name in self.agent_to_objects.iteritems():
            for object_type, object_names in object_type_to_name.iteritems():
                for obj_name in object_names:
                    new_agent_to_objects[PUBLIC].setdefault(object_type, []).append(obj_name)
            
        self.agent_to_objects = new_agent_to_objects
        
                                 
    def write_grounded_problem(self, output_file):
        """Writes an grounded unfactored MA-PDDL problem file."""

        file_ = open(output_file, 'w')
        to_write = "(define (problem " + self.problem + ") "
        to_write += "(:domain " + self.domain + ")\n"
        # Objects
        to_write += "(:objects\n"
        
        # writing objects. first public than private (per agent)
        
        for t, objects in self.agent_to_objects[PUBLIC].iteritems():
            for obj in objects:
                if not t in self.constants.iterkeys() or not obj in self.constants[t]:
                    to_write += "\t" + obj + " - " + t + "\n"
                    
        for agent in self.agent_to_objects:
            if not agent == PUBLIC:
                to_write += "\n\t(:private " + agent + "\n"
                for t, objects in self.agent_to_objects[agent].iteritems():
                    for obj in objects:
                        if not t in self.constants.iterkeys() or not obj in self.constants[t]:
                            to_write += "\t\t" + obj + " - " + t + "\n"
                to_write += "\t)\n"         
        to_write += ")\n"
        
        to_write += "(:init\n"
        for predicate in self.init:
            to_write += "\t{}\n".format(predicate)
        for function in self.ground_functions:
            to_write += "\t{}\n".format(function)
        to_write += ")\n"
        
        to_write += "(:goal\n\t(and\n"
        for goal in self.goal:
            to_write += "\t\t{}\n".format(goal)
        to_write += "\t)\n)\n"
        
        if self.metric:
            to_write += "(:metric minimize (total-cost))\n" 
            
        # Endmatter
        to_write += ")"
        file_.write(to_write)
        file_.close()
       
def generate_grounded_problem(args, grounder):
    
    if os.path.exists(args.ground_output):
        shutil.rmtree(args.ground_output)  #removes all the subdirectories!
    os.makedirs(args.ground_output)  
        
    domain_file_name = os.path.basename(args.domain)
    grounder.ground_domain()
    
    grounder.write_grounded_domain(args.ground_output + "/" + domain_file_name)
    
    problem_file_name = os.path.basename(args.problem)
    grounder.ground_problem()
    
    # TODO - remove in the future
    grounder.remove_privacy_from_problem()
    
    grounder.write_grounded_problem(args.ground_output + "/" + problem_file_name)
    
def generate_local_views(args, grounder):
    
    if os.path.exists(args.local_view_output):
        shutil.rmtree(args.local_view_output)  #removes all the subdirectories!
    os.makedirs(args.local_view_output)
    
    for agent in grounder.agent_list:
        # create directory for agent
        path = args.local_view_output + "/" + agent
        os.makedirs(path) 
        
        tmp = copy.deepcopy(grounder)
        
        # create and ground domain file
        domain_file_name = os.path.basename(args.domain)
        agent_domain = path + "/" + domain_file_name
        tmp.ground_domain_for_agent(agent) 
        tmp.write_grounded_domain(agent_domain)
        
        # create problem file
        problem_file_name = os.path.basename(args.problem)
        agent_problem = path + "/" + problem_file_name
        tmp.write_grounded_problem(agent_problem)

def main():
        
        start = time.time()
        
        args = parse_args()
        
        grounder = Grounder(args.domain, args.problem, args.agents)
        
        generate_grounded_problem(args, grounder)
        
        generate_local_views(args, grounder)
        
        end = time.time() 
        
        print("Done! (time - %0.4f" % (end - start) + ")")        


def parse_args():
        
        argparser = argparse.ArgumentParser()
        
        argparser.add_argument(
                "domain", help="path to domain file", type=str)
        argparser.add_argument(
                "problem", help="path to problem file", type=str)
        argparser.add_argument(
                "agents", help="path to agents file", type=str)
        argparser.add_argument(
                "ground_output", help="grounding output path", type=str)
        argparser.add_argument(
                "local_view_output", help="local view output path", type=str)
        
        return argparser.parse_args()


if __name__ == "__main__":
        main()
