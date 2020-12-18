import os.path
import time
import csv
import pandas
import copy

from src import config_reader as cr
from src import data_processor as dp

if not os.path.exists("./Log/"):
    os.makedirs(".Log/")

log_csv_filename = "./Log/Log.csv"


def run(args):
    config = cr.Configuration(args)

    if config.preprocess_traces:
        start = time.clock()
        data = dp.preprocess_data(config)
        print("\npreprocess_data time: ", time.clock() - start)
        # log_data_to_csv(data, log_csv_filename)

    if config.update_data:
        start = time.clock()
        data, last_model, plan_traces, failed_trace = dp.load_working_data(config)
        print("\nload_working_data time: ", time.clock() - start)

        start = time.clock()
        data = dp.update_preprocessed_data(config, data, last_model, plan_traces, failed_trace)
        print("\nupdate_preprocessed_data time: ", time.clock() - start)
        # log_data_to_csv(data, log_csv_filename)

    if config.create_model:
        start = time.clock()
        data, last_model, plan_traces, failed_trace = dp.load_working_data(config)
        print("\nload_working_data time: ", time.clock() - start)

        start = time.clock()
        model, actions_preconditions_final_values = dp.create_model(config, data)
        print("\ncreate_model time: ", time.clock() - start)
        log_data_to_csv(data, log_csv_filename, actions_preconditions_final_values, model)


def log_data_to_csv(data, filename, actions_preconditions_final_values, model):
    with open(filename, 'a') as f:
        writer = csv.writer(f)
        writer.writerow(['iteration: ' + str(data.iteration-1)])

    pre_values = pandas.DataFrame(actions_preconditions_final_values).T

    thresholds = pandas.DataFrame([data.actions_thresholds]).T
    thresholds.columns = ['Threshold']

    model_df = copy.deepcopy(model)
    for key, value in model.items():
        model_df[key] = sorted(list(value))

    model_pre = pandas.DataFrame.from_dict(model_df, orient='index')
    model_pre.columns = [''] * len(model_pre.columns)

    result = pre_values.join(thresholds).join(model_pre)
    result.to_csv(filename, mode='a')
