import os
import pickle
import _collections
import copy
import math
import re
import random

from sklearn.feature_extraction.text import TfidfTransformer


class Data:

    def __init__(self, preconditions, facts_groups, actions, actions_safe_preconditions, missing_actions,
                 actions_preconditions_traces_counts, actions_preconditions_score_deltas,
                 actions_preconditions_used_counts, iteration, actions_thresholds):
        self.preconditions = preconditions
        self.facts_groups = facts_groups
        self.actions = actions
        self.actions_safe_preconditions = actions_safe_preconditions
        self.missing_actions = missing_actions
        self.actions_preconditions_traces_counts = actions_preconditions_traces_counts
        self.actions_preconditions_score_deltas = actions_preconditions_score_deltas
        self.actions_preconditions_used_counts = actions_preconditions_used_counts
        self.iteration = iteration
        self.actions_thresholds = actions_thresholds


def get_vocabulary(unique_preconditions):
    vocabulary = _collections.OrderedDict()
    index = 0
    for pre in sorted(unique_preconditions):
        vocabulary[pre] = index
        index += 1

    return vocabulary


def calculate_agent_actions_present(agents, missing_actions):
    agent_actions_present = _collections.OrderedDict.fromkeys(agents, True)

    for action in missing_actions:
        split = action.split()
        split = split[0].split("-agent-")
        agent = split[1]
        agent_actions_present[agent] = False

    return agent_actions_present


def preprocess_data(config):
    preconditions, facts_groups = read_facts_groups(config)
    actions = read_file_lines(config.actions_file_path)
    agents = read_file_lines(config.agents_file_path)

    preconditions_dict = _collections.OrderedDict.fromkeys(preconditions, 0)
    actions_preconditions_dict = _collections.OrderedDict.fromkeys(actions)
    actions_safe_preconditions = _collections.OrderedDict.fromkeys(actions)
    actions_thresholds = _collections.OrderedDict.fromkeys(actions)
    missing_actions = copy.deepcopy(set(actions))

    for action in actions:
        actions_preconditions_dict[action] = copy.deepcopy(preconditions_dict)
        actions_safe_preconditions[action] = set(preconditions)

    actions_preconditions_traces_counts = _collections.OrderedDict(copy.deepcopy(actions_preconditions_dict))
    actions_preconditions_score_deltas = _collections.OrderedDict(copy.deepcopy(actions_preconditions_dict))
    actions_preconditions_used_counts = _collections.OrderedDict(copy.deepcopy(actions_preconditions_dict))

    learn_safe_model_and_count_preconditions(actions_preconditions_traces_counts, missing_actions,
                                             actions_safe_preconditions, config, preconditions)

    calculate_initial_actions_thresholds(config, actions, preconditions,
                                         actions_preconditions_score_deltas,
                                         actions_preconditions_traces_counts,
                                         actions_preconditions_used_counts, actions_thresholds,
                                         missing_actions)

    data = Data(preconditions, facts_groups, actions, actions_safe_preconditions, missing_actions,
                actions_preconditions_traces_counts, actions_preconditions_score_deltas,
                actions_preconditions_used_counts, config.iteration, actions_thresholds)

    with open(config.data_file_path, 'wb') as file:
        pickle.dump(data, file, pickle.HIGHEST_PROTOCOL)

    agent_actions_present = calculate_agent_actions_present(agents, missing_actions)

    write_status_to_output(config, agent_actions_present)

    return data


def calculate_initial_actions_thresholds(config, actions, preconditions, actions_preconditions_score_deltas,
                                         actions_preconditions_traces_counts, actions_preconditions_used_counts,
                                         actions_thresholds, missing_actions):
    actions_preconditions_Q_value = compute_actions_preconditions_q_value(config, actions, preconditions,
                                                                          actions_preconditions_score_deltas,
                                                                          actions_preconditions_traces_counts)
    for action in actions:

        if action in missing_actions:
            actions_thresholds[action] = config.thresholds_base
            continue

        max_Q = 0
        for precondition in preconditions:
            q = actions_preconditions_Q_value[action][precondition]
            naf = actions_preconditions_used_counts[action][precondition]
            n = config.iteration
            c = config.c_value

            value = compute_fact_value(q, naf, n, c)
            if value > max_Q:
                max_Q = value

        actions_thresholds[action] = config.thresholds_base * max_Q


def learn_safe_model_and_count_preconditions(actions_preconditions_traces_counts, missing_actions,
                                             actions_safe_preconditions, config, preconditions):
    num_used_traces = 0
    if os.path.exists(config.traces_file_path):

        if config.traces_to_use > 0:
            with open(config.traces_file_path, 'r') as file:
                for line in file:
                    if num_used_traces > config.traces_to_use:
                        break

                    sas = process_sas_string(line)
                    sas_action = sas['Action']
                    sas_preconditions = set(sas['Preconditions'])

                    missing_actions.discard(sas_action)

                    actions_safe_preconditions[sas_action] = \
                        actions_safe_preconditions[sas_action] & set(sas_preconditions)

                    for precondition in sas_preconditions:
                        actions_preconditions_traces_counts[sas_action][precondition] += 1

                    num_used_traces += 1
    for action_name in actions_safe_preconditions.keys():
        if actions_safe_preconditions[action_name] is None:
            actions_safe_preconditions[action_name] = set(preconditions)


def read_file_lines(path):
    result = set()
    if os.path.exists(path):
        with open(path, 'r') as file:
            lines = file.read().splitlines()
            for line in lines:
                result.add(line)

    return sorted(list(result))


def read_facts_groups(config):
    preconditions = set()
    facts_groups = _collections.OrderedDict()
    if os.path.exists(config.facts_groups_file_path):
        with open(config.facts_groups_file_path, 'r') as file:
            lines = file.read().splitlines()
            for line in lines:
                line_split = re.split(':|;', line)
                line_split_cleaned = list(filter(None, line_split))
                group = line_split_cleaned[0]
                facts = line_split_cleaned[1:]
                preconditions.update(facts)
                facts_groups[group] = set(facts)

    return sorted(list(preconditions)), facts_groups


def load_working_data(config):
    data = None
    last_model = None
    plan_traces = None
    failed_trace = None
    if os.path.exists(config.data_file_path):
        with open(config.data_file_path, 'rb') as file:
            data = pickle.load(file)
    if os.path.exists(config.last_model_file_path):
        with open(config.last_model_file_path, 'rb') as file:
            last_model = pickle.load(file)
    if os.path.exists(config.plan_traces_path):
        with open(config.plan_traces_path, 'r') as file:
            plan_traces = file.readlines()
    if os.path.exists(config.failed_trace_path):
        with open(config.failed_trace_path, 'r') as file:
            failed_trace = file.read()

    return data, last_model, plan_traces, failed_trace


def update_preprocessed_data(config, data, last_model, plan_traces, failed_trace):
    if plan_traces is not None:
        update_preprocessed_data_with_successful_traces(data, plan_traces)
    if failed_trace is not None:
        update_preprocessed_data_with_failed_trace(config, data, failed_trace, last_model)

    with open(config.data_file_path, 'wb') as file:
        pickle.dump(data, file, pickle.HIGHEST_PROTOCOL)

    return data


def update_preprocessed_data_with_failed_trace(config, data, failed_trace, last_model):
    failed_sas = process_sas_string(failed_trace)
    failed_action = failed_sas['Action']

    data.missing_actions.discard(failed_action)

    # failed_preconditions = set(failed_sas['Preconditions'])
    # failed_safe_preconditions = failed_preconditions & data.actions_safe_preconditions[failed_action]

    failed_safe_preconditions = data.actions_safe_preconditions[failed_action]
    possibly_missing_preconditions = failed_safe_preconditions - last_model[failed_action]

    for precondition in possibly_missing_preconditions:
        data.actions_preconditions_score_deltas[failed_action][precondition] += 1

    # actions_preconditions_tfidf = compute_actions_preconditions_Q_value(config, data)
    # max_tfidf = 0
    # most_likely_precondition = None
    #
    # for precondition in possibly_missing_preconditions:
    #     if actions_preconditions_tfidf[failed_action][precondition] >= max_tfidf:
    #         max_tfidf = actions_preconditions_tfidf[failed_action][precondition]
    #         most_likely_precondition = precondition
    #
    # if most_likely_precondition is not None:
    #     data.actions_preconditions_score_deltas[failed_action][most_likely_precondition] += 1


def update_preprocessed_data_with_successful_traces(data, new_traces_list):
    for line in new_traces_list:
        sas = process_sas_string(line)
        sas_action = sas['Action']
        sas_preconditions = set(sas['Preconditions'])

        data.missing_actions.discard(sas_action)

        data.actions_safe_preconditions[sas_action] = \
            data.actions_safe_preconditions[sas_action] & sas_preconditions

        for precondition in sas['Preconditions']:
            (data.actions_preconditions_traces_counts[sas_action])[precondition] += 1
            (data.actions_preconditions_score_deltas[sas_action])[precondition] = 0


def create_model(config, data):
    c_value = config.c_value

    model = _collections.OrderedDict.fromkeys(data.actions)

    preconditions_dict = _collections.OrderedDict.fromkeys(data.preconditions, 0)
    actions_preconditions_final_values = _collections.OrderedDict.fromkeys(data.actions)

    for action in data.actions:
        actions_preconditions_final_values[action] = copy.deepcopy(preconditions_dict)

    for action in data.actions:
        model[action] = set()

    actions_preconditions_Q_value = compute_actions_preconditions_q_value(config, data.actions, data.preconditions,
                                                                          data.actions_preconditions_score_deltas,
                                                                          data.actions_preconditions_traces_counts)
    for action, preconditions in data.actions_safe_preconditions.items():

        if action in data.missing_actions:
            # model[action].update(data.preconditions)
            continue

        action_precondition_values = dict.fromkeys(preconditions)

        for precondition in preconditions:
            q = actions_preconditions_Q_value[action][precondition]
            naf = data.actions_preconditions_used_counts[action][precondition]
            n = data.iteration
            c = c_value

            value = compute_fact_value(q, naf, n, c)
            action_precondition_values[precondition] = value
            actions_preconditions_final_values[action] = value

        max_Q = max(action_precondition_values.values())

        # normalised_precondition_values = normalise_values(action_precondition_values)
        # normalised_max_Q = max(normalised_precondition_values.values())

        used_variables = []

        shuffled_preconditions = list(preconditions)
        random.shuffle(shuffled_preconditions)

        for precondition in shuffled_preconditions:
            var = get_key_from_value(data.facts_groups, precondition)

            if action_precondition_values[precondition] > data.actions_thresholds[action] and var not in used_variables:
                # if normalised_precondition_values[precondition] > threshold * normalised_max_Q and var not in used_variables:
                model[action].add(precondition)
                data.actions_preconditions_used_counts[action][precondition] += 1
                used_variables.append(var)

    with open(config.last_model_file_path, 'wb') as file:
        pickle.dump(model, file, pickle.HIGHEST_PROTOCOL)

    write_model_to_output(config, model)

    data.iteration = data.iteration + 1
    with open(config.data_file_path, 'wb') as file:
        pickle.dump(data, file, pickle.HIGHEST_PROTOCOL)

    return model, actions_preconditions_final_values


def write_model_to_output(config, model):
    model_str_list = []
    for action, preconditions in model.items():
        model_str_list.append(action + ':')
        for precondition in sorted(preconditions):
            model_str_list.append(precondition)
            model_str_list.append(';')
        model_str_list.append('\n')

    model_str = ''.join([line for line in model_str_list])

    with open(config.model_file_path, 'w') as file:
        file.write(model_str)


def write_status_to_output(config, agent_actions_present):
    status_str_list = ['agents all actions present:\n']

    for agent, value in agent_actions_present.items():
        if value:
            status_str_list.append('\t' + agent + ' : yes\n')
        else:
            status_str_list.append('\t' + agent + ' : no\n')

    status_str = ''.join([line for line in status_str_list])

    with open(config.status_file_path, 'w') as file:
        file.write(status_str)


def compute_fact_value(q, naf, n, c):
    if naf == 0:
        # return q
        return q + c * math.sqrt(math.log2(n))
    else:
        return q / naf + c * math.sqrt(math.log2(n / naf))


def compute_actions_preconditions_q_value(config, actions, preconditions, actions_preconditions_score_deltas,
                                          actions_preconditions_traces_counts):
    counts = extract_counts_as_array(actions_preconditions_traces_counts)
    tfidf_transformer = TfidfTransformer()
    tfidf_values = tfidf_transformer.fit_transform(counts).toarray()
    return tfidf_plus_delta_array_to_dict(config, actions, preconditions, actions_preconditions_score_deltas,
                                          tfidf_values)


def extract_counts_as_array(actions_preconditions_traces_counts):
    actions_counts_array = []

    for action_name, preconditions_count in actions_preconditions_traces_counts.items():
        action_count = []
        for precondition, count in preconditions_count.items():
            action_count.append(count)

        actions_counts_array.append(action_count)

    return actions_counts_array


def tfidf_plus_delta_array_to_dict(config, actions, preconditions, actions_preconditions_score_deltas, tfidf_array):
    preconditions_dict = _collections.OrderedDict.fromkeys(preconditions, 0)
    actions_preconditions_dict = _collections.OrderedDict.fromkeys(actions)

    for action in actions:
        actions_preconditions_dict[action] = copy.deepcopy(preconditions_dict)

    actions_preconditions_tfidf_dict = _collections.OrderedDict(copy.deepcopy(actions_preconditions_dict))

    action_row_index = 0
    for action in actions:
        precondition_index = 0
        for precondition in preconditions:
            delta_count = actions_preconditions_score_deltas[action][precondition]
            tfidf_value = float(tfidf_array[action_row_index][precondition_index])
            # actions_preconditions_tfidf_dict[action][precondition] = tfidf_value * math.pow(1 + config.delta_base, delta_count)
            actions_preconditions_tfidf_dict[action][precondition] = tfidf_value + delta_count * config.delta_base
            precondition_index += 1
        action_row_index += 1

    return actions_preconditions_tfidf_dict


def process_sas_string(sas_string):
    sas_string = sas_string.replace('[', ':')
    sas_string = sas_string.replace(']', ':')
    line_split = [x.strip() for x in sas_string.split(':')]
    agent = line_split[6]
    action = line_split[4]  # line_split[4].replace(' ', '-param-')
    preconditions = [x.strip() for x in line_split[2].split(';')]
    effects = [x.strip() for x in line_split[8].split(';')]
    row = {'Agent': agent, 'Action': action, 'Preconditions': preconditions,
           'Effects': effects}
    return row


def get_key_from_value(dictionary, value):
    for key, values in dictionary.items():
        if value in values:
            return key


def normalise_values(dictionary):
    normalised_dict = copy.deepcopy(dictionary)
    factor = 1.0 / sum(normalised_dict.values())
    for key, value in normalised_dict.items():
        normalised_dict[key] = value * factor

    return normalised_dict
