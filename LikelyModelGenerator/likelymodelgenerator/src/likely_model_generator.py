import likelymodelgenerator.src.data_processor as dp
import likelymodelgenerator.src.config_reader as cr

from sklearn.feature_extraction.text import TfidfTransformer
import time
import numpy as np
import _collections


def dummy(s):
    return s


def run(args):
    config_path = args.config
    config = cr.Configuration(config_path)

    # start = time.clock()
    # df = dp.pre_process_data_csv(data_file, pre_processed_file_csv)
    # print("\npre_process_data time: ", time.clock() - start)
    #
    # start = time.clock()
    # df = dp.load_pre_process_data_csv(pre_processed_file_csv)
    # print("\nload_pre_process_data time: ", time.clock() - start)
    #
    # filtered = (df.groupby('Action', sort=False)['Preconditions'].apply(sum)).to_frame().reset_index()
    #
    # count_vectorizer = CountVectorizer(tokenizer=dummy, preprocessor=dummy)
    # count = count_vectorizer.fit_transform(filtered['Preconditions'])
    #
    # tfidf_transformer = TfidfTransformer()
    # tfidf = tfidf_transformer.fit_transform(count)

    if config.preprocess_traces:
        start = time.clock()
        data = dp.preprocess_data(config)
        print("\npreprocess_data time: ", time.clock() - start)

    if config.load_data:
        start = time.clock()
        data = dp.load_preprocessed_data(config)
        print("\nload_preprocessed_data time: ", time.clock() - start)

    if config.update_data:
        '''        
        start = time.clock()
        data = dp.update_preprocessed_data(data, [
            'StateActionState [ pre[ at(tru1, pos1) ; in(obj1, tru1) ; at(apn1, apt1) ]  ; '
            'action[ unload-truck-agent-tru1 tru1 obj1 pos1 ]  ; '
            'actionOwner[ tru1 ]  ; '
            'post[ at(tru1, pos1) ; at(obj1, pos1) ; at(apn1, apt1) ]  ; '
            'traceNum[ 2 ] ]'
        ],
                                           'StateActionState [ pre[ at(tru1, pos1) ; in(obj1, tru1) ; at(apn1, apt1) ]  ; '
                                           'action[ unload-truck-agent-tru1 tru1 obj1 pos1 ]  ; '
                                           'actionOwner[ tru1 ]  ; '
                                           'post[ at(tru1, pos1) ; at(obj1, pos1) ; at(apn1, apt1) ]  ; '
                                           'traceNum[ 2 ] ]')
        print("\nupdate_preprocessed_data time: ", time.clock() - start)
        
        '''

        start = time.clock()
        model = dp.create_model(data)
        print("\ncreate_model time: ", time.clock() - start)