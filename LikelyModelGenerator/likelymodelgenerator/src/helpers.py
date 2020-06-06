import pandas as pd
import os
import likelymodelgenerator


def pre_process_traces():
    root_dir = os.path.dirname(os.path.abspath(likelymodelgenerator.__file__))  # This is Project Root
    data_dir = root_dir + "/data"  # This is Data Root
    out_dir = root_dir + "/preprocessed"  # This is Preprocessed Root

    df = pd.DataFrame(columns=['Agent', 'Action', 'Preconditions', 'Effects'])

    for dir_path, dir_names, file_names in os.walk(data_dir):
        data_file = data_dir + "/" + file_names[0]

        with open(data_file) as file:
            for line in file:
                row = pre_process_sas(line)
                df = df.append(row, ignore_index=True)

            df.to_csv(out_dir + "/preprocessed.csv", index=False)
            break

    return df


def pre_process_append_new_traces(new_traces_list):
    root_dir = os.path.dirname(os.path.abspath(likelymodelgenerator.__file__))  # This is Project Root
    preprocessed_file = root_dir + "/preprocessed/preprocessed.csv"  # This is Preprocessed Root

    df = pd.read_csv(preprocessed_file)

    for line in new_traces_list:
        row = pre_process_sas(line)
        df = df.append(row, ignore_index=True)

    df.to_csv(preprocessed_file, index=False)


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
