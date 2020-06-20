import os
import pickle
import _collections
import copy


class Data:

    def __init__(self, preconditions, actions, actions_safe_preconditions,
                 actions_preconditions_traces_counts, actions_preconditions_score_deltas,
                 actions_preconditions_used_counts):
        self.preconditions = preconditions
        self.actions = actions
        self.actions_safe_preconditions = actions_safe_preconditions
        self.actions_preconditions_traces_counts = actions_preconditions_traces_counts
        self.actions_preconditions_score_deltas = actions_preconditions_score_deltas
        self.actions_preconditions_used_counts = actions_preconditions_used_counts


def get_vocabulary(unique_preconditions):
    vocabulary = _collections.OrderedDict()
    index = 0
    for pre in sorted(unique_preconditions):
        vocabulary[pre] = index
        index += 1

    return vocabulary


def get_actions_preconditions_counts_as_arrays(data):
    actions_counts = []

    for action_name, preconditions_count in data.actions_preconditions_traces_counts.items():
        action_count = []
        for precondition, count in preconditions_count.items():
            action_count.append(count)

        actions_counts.append(action_count)

    return actions_counts


def preprocess_data(config):
    preconditions = read_preconditions(config)
    actions = read_actions(config)

    preconditions_dict = _collections.OrderedDict.fromkeys(preconditions, 0)
    actions_preconditions_dict = _collections.OrderedDict.fromkeys(actions)

    for action in actions:
        actions_preconditions_dict[action] = copy.deepcopy(preconditions_dict)

    actions_preconditions_traces_counts = _collections.OrderedDict(copy.deepcopy(actions_preconditions_dict))
    actions_preconditions_score_deltas = _collections.OrderedDict(copy.deepcopy(actions_preconditions_dict))
    actions_preconditions_used_counts = _collections.OrderedDict(copy.deepcopy(actions_preconditions_dict))

    actions_safe_preconditions = _collections.OrderedDict.fromkeys(actions)

    if os.path.exists(config.traces_file_path):
        with open(config.traces_file_path, 'r') as file:
            for line in file:
                row = process_sas_string(line)

                if actions_safe_preconditions[row['Action']] is None:
                    actions_safe_preconditions[row['Action']] = set(row['Preconditions'])
                else:
                    actions_safe_preconditions[row['Action']] = \
                        actions_safe_preconditions[row['Action']] & set(row['Preconditions'])

                for precondition in row['Preconditions']:
                    a = actions_preconditions_traces_counts[row['Action']]
                    a[precondition] += 1

    for action_name in actions_safe_preconditions.keys():
        if actions_safe_preconditions[action_name] is None:
            actions_safe_preconditions[action_name] = set(preconditions)

    data = Data(preconditions, actions, actions_safe_preconditions,
                actions_preconditions_traces_counts, actions_preconditions_score_deltas,
                actions_preconditions_used_counts)

    with open(config.data_file_path, 'wb') as file:
        pickle.dump(data, file, pickle.HIGHEST_PROTOCOL)

    return data


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
    if os.path.exists(config.data_file_path):
        with open(config.data_file_path, 'rb') as file:
            data = pickle.load(file)
            return data
    else:
        return None


def update_preprocessed_data(data, new_traces_list, failed_trace):
    for line in new_traces_list:
        row = process_sas_string(line)

        data.actions_safe_preconditions[row['Action']] = \
            data.actions_safe_preconditions[row['Action']] & set(row['Preconditions'])

        for precondition in row['Preconditions']:
            (data.actions_preconditions_traces_counts[row['Action']])[precondition] += 1

    return data


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
