import os
import pickle
import _collections
import copy
import math

from sklearn.feature_extraction.text import TfidfTransformer


class Data:

    def __init__(self, preconditions, actions, actions_safe_preconditions, missing_actions,
                 actions_preconditions_traces_counts, actions_preconditions_score_deltas,
                 actions_preconditions_used_counts, iteration):
        self.preconditions = preconditions
        self.actions = actions
        self.actions_safe_preconditions = actions_safe_preconditions
        self.missing_actions = missing_actions
        self.actions_preconditions_traces_counts = actions_preconditions_traces_counts
        self.actions_preconditions_score_deltas = actions_preconditions_score_deltas
        self.actions_preconditions_used_counts = actions_preconditions_used_counts
        self.iteration = iteration


def get_vocabulary(unique_preconditions):
    vocabulary = _collections.OrderedDict()
    index = 0
    for pre in sorted(unique_preconditions):
        vocabulary[pre] = index
        index += 1

    return vocabulary


def preprocess_data(config):
    preconditions = read_preconditions(config)
    actions = read_actions(config)

    preconditions_dict = _collections.OrderedDict.fromkeys(preconditions, 0)
    actions_preconditions_dict = _collections.OrderedDict.fromkeys(actions)
    actions_safe_preconditions = _collections.OrderedDict.fromkeys(actions)
    missing_actions = copy.deepcopy(set(actions))

    for action in actions:
        actions_preconditions_dict[action] = copy.deepcopy(preconditions_dict)
        actions_safe_preconditions[action] = set(preconditions)

    actions_preconditions_traces_counts = _collections.OrderedDict(copy.deepcopy(actions_preconditions_dict))
    actions_preconditions_score_deltas = _collections.OrderedDict(copy.deepcopy(actions_preconditions_dict))
    actions_preconditions_used_counts = _collections.OrderedDict(copy.deepcopy(actions_preconditions_dict))

    learn_safe_model_and_count_preconditions(actions_preconditions_traces_counts, missing_actions,
                                             actions_safe_preconditions, config, preconditions)

    data = Data(preconditions, actions, actions_safe_preconditions, missing_actions,
                actions_preconditions_traces_counts, actions_preconditions_score_deltas,
                actions_preconditions_used_counts, config.iteration)

    with open(config.data_file_path, 'wb') as file:
        pickle.dump(data, file, pickle.HIGHEST_PROTOCOL)

    return data


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

                    missing_actions.remove(sas_action)

                    actions_safe_preconditions[sas_action] = \
                        actions_safe_preconditions[sas_action] & set(sas_preconditions)

                    for precondition in sas_preconditions:
                        actions_preconditions_traces_counts[sas_action][precondition] += 1

                    num_used_traces += 1
    for action_name in actions_safe_preconditions.keys():
        if actions_safe_preconditions[action_name] is None:
            actions_safe_preconditions[action_name] = set(preconditions)


def read_actions(config):
    actions = set()
    if os.path.exists(config.actions_file_path):
        with open(config.actions_file_path, 'r') as file:
            lines = file.read().splitlines()
            for line in lines:
                actions.add(line)

    return sorted(list(actions))


def read_preconditions(config):
    preconditions = set()
    if os.path.exists(config.preconditions_file_path):
        with open(config.preconditions_file_path, 'r') as file:
            lines = file.read().splitlines()
            for line in lines:
                preconditions.add(line)

    return sorted(list(preconditions))


def load_preprocessed_data(config):
    data = None
    last_model = None
    if os.path.exists(config.data_file_path):
        with open(config.data_file_path, 'rb') as file:
            data = pickle.load(file)
    if os.path.exists(config.last_model_file_path):
        with open(config.last_model_file_path, 'rb') as file:
            last_model = pickle.load(file)

    return data, last_model


def update_preprocessed_data(config, data, last_model, new_traces, failed_trace):
    update_preprocessed_data_with_successful_traces(data, new_traces)
    update_preprocessed_data_with_failed_trace(config, data, failed_trace, last_model)

    with open(config.data_file_path, 'wb') as file:
        pickle.dump(data, file, pickle.HIGHEST_PROTOCOL)

    return data


def update_preprocessed_data_with_failed_trace(config, data, failed_trace, last_model):
    actions_preconditions_tfidf = compute_actions_preconditions_tfidf(config, data)

    failed_sas = process_sas_string(failed_trace)
    failed_action = failed_sas['Action']
    failed_preconditions = set(failed_sas['Preconditions'])

    failed_safe_preconditions = failed_preconditions & data.actions_safe_preconditions[failed_action]
    possibly_missing_preconditions = failed_safe_preconditions - last_model[failed_action]

    max_tfidf = 0
    most_likely_precondition = None

    for precondition in possibly_missing_preconditions:
        if actions_preconditions_tfidf[failed_action][precondition] >= max_tfidf:
            max_tfidf = actions_preconditions_tfidf[failed_action][precondition]
            most_likely_precondition = precondition

    if most_likely_precondition is not None:
        data.actions_preconditions_score_deltas[failed_action][most_likely_precondition] += 1


def update_preprocessed_data_with_successful_traces(data, new_traces_list):
    for line in new_traces_list:
        sas = process_sas_string(line)
        sas_action = sas['Action']
        sas_preconditions = set(sas['Preconditions'])

        data.missing_actions.remove(sas_action)

        data.actions_safe_preconditions[sas_action] = \
            data.actions_safe_preconditions[sas_action] & sas_preconditions

        for precondition in sas['Preconditions']:
            (data.actions_preconditions_traces_counts[sas_action])[precondition] += 1


def create_model(config, data):
    model = _collections.OrderedDict.fromkeys(data.actions)

    for action in data.actions:
        model[action] = set()

    actions_preconditions_tfidf = compute_actions_preconditions_tfidf(config, data)
    threshold = config.thresholds_base

    for action, preconditions in data.actions_safe_preconditions.items():

        if action in data.missing_actions:
            continue

        max_tfidf = max(actions_preconditions_tfidf[action].values())

        for precondition in preconditions:
            q = actions_preconditions_tfidf[action][precondition]
            naf = data.actions_preconditions_used_counts[action][precondition]
            n = data.iteration
            c = 2
            if compute_fact_value(q, naf, n, c) > threshold * max_tfidf:
                model[action].add(precondition)
                data.actions_preconditions_used_counts[action][precondition] += 1

    with open(config.last_model_file_path, 'wb') as file:
        pickle.dump(model, file, pickle.HIGHEST_PROTOCOL)

    write_model_to_output(config, model)

    return model


def write_model_to_output(config, model):
    model_str_list = []
    for action, preconditions in model.items():
        model_str_list.append(action + ':')
        for precondition in preconditions:
            model_str_list.append(precondition)
            model_str_list.append(';')
        model_str_list.append('\n')

    model_str = ''.join([line for line in model_str_list])
    with open(config.model_file_path, 'w') as file:
        file.write(model_str)


def compute_fact_value(q, naf, n, c):
    if naf == 0:
        return q
    else:
        return q / naf + c * math.sqrt(math.log2(n / naf))


def compute_actions_preconditions_tfidf(config, data):
    counts = extract_counts_as_array(data)
    tfidf_transformer = TfidfTransformer()
    tfidf_values = tfidf_transformer.fit_transform(counts).toarray()
    return tfidf_plus_delta_array_to_dict(config, data, tfidf_values)


def extract_counts_as_array(data):
    actions_counts_array = []

    for action_name, preconditions_count in data.actions_preconditions_traces_counts.items():
        action_count = []
        for precondition, count in preconditions_count.items():
            action_count.append(count)

        actions_counts_array.append(action_count)

    return actions_counts_array


def tfidf_plus_delta_array_to_dict(config, data, tfidf_array):
    preconditions_dict = _collections.OrderedDict.fromkeys(data.preconditions, 0)
    actions_preconditions_dict = _collections.OrderedDict.fromkeys(data.actions)

    for action in data.actions:
        actions_preconditions_dict[action] = copy.deepcopy(preconditions_dict)

    actions_preconditions_tfidf_dict = _collections.OrderedDict(copy.deepcopy(actions_preconditions_dict))

    action_row_index = 0
    for action in data.actions:
        precondition_index = 0
        for precondition in data.preconditions:
            delta_count = data.actions_preconditions_score_deltas[action][precondition]
            tfidf_value = float(tfidf_array[action_row_index][precondition_index])
            actions_preconditions_tfidf_dict[action][precondition] = \
                tfidf_value * math.pow(1+config.delta_base, delta_count)
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
