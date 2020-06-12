import pandas as pd
import os
from ast import literal_eval
import pickle
import _collections


def get_vocabulary(unique_preconditions):
    vocabulary = _collections.OrderedDict()
    index = 0
    for pre in sorted(unique_preconditions):
        vocabulary[pre] = index
        index += 1

    return vocabulary


def get_counts_as_arrays(actions_preconditions_counts, unique_preconditions):
    actions_counts = []

    for action, preconditions_count in actions_preconditions_counts.items():
        action_count = []
        for precondition in unique_preconditions:
            if precondition not in preconditions_count:
                action_count.append(0)
            else:
                action_count.append(preconditions_count[precondition])

        actions_counts.append(action_count)

    return actions_counts


def pre_process_data_pkl(data_file, pre_processed_file):
    unique_preconditions = set()
    actions_preconditions_counts = _collections.OrderedDict()

    if os.path.exists(data_file):
        with open(data_file) as file:
            for line in file:
                row = pre_process_sas(line)
                unique_preconditions = unique_preconditions.union(set(row['Preconditions']))
                if row['Action'] not in actions_preconditions_counts:
                    actions_preconditions_counts[row['Action']] = _collections.OrderedDict()
                for precondition in row['Preconditions']:
                    if precondition in actions_preconditions_counts[row['Action']]:
                        (actions_preconditions_counts[row['Action']])[precondition] += 1
                    else:
                        (actions_preconditions_counts[row['Action']])[precondition] = 1

    with open(pre_processed_file, 'wb') as file:
        pickle.dump(actions_preconditions_counts, file, pickle.HIGHEST_PROTOCOL)

    return actions_preconditions_counts, unique_preconditions


def load_pre_process_data_pkl(pre_processed_file):
    if os.path.exists(pre_processed_file):
        with open(pre_processed_file, 'rb') as file:
            actions_preconditions_counts = pickle.load(file)
            unique_preconditions = set()
            for action, preconditions_count in actions_preconditions_counts.items():
                unique_preconditions = unique_preconditions.union(preconditions_count.keys())

            return actions_preconditions_counts, unique_preconditions
    else:
        return None


def pre_process_append_new_traces_pkl(pre_processed_file, new_traces_list):
    if os.path.exists(pre_processed_file):
        actions_preconditions_counts, unique_preconditions = load_pre_process_data_pkl(pre_processed_file)
        for line in new_traces_list:
            row = pre_process_sas(line)
            unique_preconditions = unique_preconditions.union(row['Preconditions'])
            if row['Action'] not in actions_preconditions_counts:
                actions_preconditions_counts[row['Action']] = _collections.OrderedDict()
            for precondition in row['Preconditions']:
                if precondition in actions_preconditions_counts[row['Action']]:
                    (actions_preconditions_counts[row['Action']])[precondition] += 1
                else:
                    (actions_preconditions_counts[row['Action']])[precondition] = 1
        return actions_preconditions_counts, unique_preconditions
    else:
        return None


def pre_process_data_csv(data_file, pre_processed_file):
    df = pd.DataFrame(columns=['Agent', 'Action', 'Preconditions', 'Effects'])

    if os.path.exists(data_file):
        with open(data_file) as file:
            for line in file:
                row = pre_process_sas(line)
                df = df.append(row, ignore_index=True)

    df.to_csv(pre_processed_file, index=False)
    return df


def load_pre_process_data_csv(pre_processed_file):
    if os.path.exists(pre_processed_file):
        converter = {"Preconditions": literal_eval, "Effects": literal_eval}
        df = pd.read_csv(pre_processed_file, converters=converter)
        return df
    else:
        return None


def pre_process_append_new_traces_csv(pre_processed_file, new_traces_list):
    if os.path.exists(pre_processed_file):
        converter = {"Preconditions": literal_eval, "Effects": literal_eval}
        df = pd.read_csv(pre_processed_file, converters=converter)

        for line in new_traces_list:
            row = pre_process_sas(line)
            df = df.append(row, ignore_index=True)

        df.to_csv(pre_processed_file, index=False)

        return df
    else:
        return None


def pre_process_sas(data):
    data = data.replace('[', ':')
    data = data.replace(']', ':')
    line_split = [x.strip() for x in data.split(':')]
    agent = line_split[6]
    action = line_split[4].replace(' ', '-param-')
    preconditions = [x.strip() for x in line_split[2].split(';')]
    effects = [x.strip() for x in line_split[8].split(';')]
    row = {'Agent': agent, 'Action': action, 'Preconditions': preconditions,
           'Effects': effects}
    return row
