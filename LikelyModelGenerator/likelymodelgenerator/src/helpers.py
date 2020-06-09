import pandas as pd
import os
from ast import literal_eval
import pickle


def pre_process_data2(data_file, pre_processed_file):
    unique_pre = set()
    action_pre_count = dict()

    if os.path.exists(data_file):
        with open(data_file) as file:
            for line in file:
                row = pre_process_sas(line)
                unique_pre = unique_pre.union(set(row['Preconditions']))
                if not row['Action'] in action_pre_count:
                    action_pre_count[row['Action']] = dict()
                for pre in row['Preconditions']:
                    if pre in action_pre_count[row['Action']]:
                        (action_pre_count[row['Action']])[pre] += 1
                    else:
                        (action_pre_count[row['Action']])[pre] = 1

    with open(pre_processed_file, 'wb') as file:
        pickle.dump(action_pre_count, file, pickle.HIGHEST_PROTOCOL)

    return action_pre_count


def load_pre_process_data2(pre_processed_file):
    if os.path.exists(pre_processed_file):
        with open(pre_processed_file, 'rb') as file:
            return pickle.load(file)
    else:
        return None


def pre_process_append_new_traces2(pre_processed_file, new_traces_list):
    if os.path.exists(pre_processed_file):
        action_pre_count = load_pre_process_data2(pre_processed_file)
        for line in new_traces_list:
            row = pre_process_sas(line)
            if not row['Action'] in action_pre_count:
                action_pre_count[row['Action']] = dict()
            for pre in row['Preconditions']:
                if pre in action_pre_count[row['Action']]:
                    (action_pre_count[row['Action']])[pre] += 1
                else:
                    (action_pre_count[row['Action']])[pre] = 1
        return action_pre_count
    else:
        return None


def pre_process_data(data_file, pre_processed_file):
    df = pd.DataFrame(columns=['Agent', 'Action', 'Preconditions', 'Effects'])

    if os.path.exists(data_file):
        with open(data_file) as file:
            for line in file:
                row = pre_process_sas(line)
                df = df.append(row, ignore_index=True)

    df.to_csv(pre_processed_file, index=False)
    return df


def load_pre_process_data(pre_processed_file):
    if os.path.exists(pre_processed_file):
        converter = {"Preconditions": literal_eval, "Effects": literal_eval}
        df = pd.read_csv(pre_processed_file, converters=converter)
        return df
    else:
        return None


def pre_process_append_new_traces(pre_processed_file, new_traces_list):
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
    pre = [x.strip() for x in line_split[2].split(';')]
    post = [x.strip() for x in line_split[8].split(';')]
    row = {'Agent': agent, 'Action': action, 'Preconditions': pre,
           'Effects': post}
    return row
