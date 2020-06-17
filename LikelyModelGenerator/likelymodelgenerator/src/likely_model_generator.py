import likelymodelgenerator.src.data_processor as dp
import likelymodelgenerator.src.config_reader as cr

from sklearn.feature_extraction.text import TfidfTransformer
import time
from sklearn.cluster import KMeans
import numpy as np
import _collections


def dummy(s):
    return s


def run(args):
    config_path = args.config

    config = cr.read_config(config_path)

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

    start = time.clock()

    if config.preprocess:
        if config.preprocessed_read_format == "pkl":
            actions_preprocessed, preconditions_preprocessed = dp.pre_process_data_pkl(config)
        elif config.preprocessed_read_format == "csv":
            actions_preprocessed, preconditions_preprocessed = dp.pre_process_data_csv(config)

    print("\npre_process_data time: ", time.clock() - start)

    start = time.clock()

    if config.load_preprocessed:
        if config.preprocessed_read_format == "pkl":
            actions_loaded, preconditions_loaded = dp.load_pre_process_data_pkl(config)
        elif config.preprocessed_read_format == "csv":
            actions_loaded, preconditions_loaded = dp.load_pre_process_data_csv(config)

    print("\nload_pre_process_data time: ", time.clock() - start)

    start = time.clock()

    if config.update_preprocessed:
        if config.preprocessed_read_format == "pkl":
            actions_loaded, preconditions_loaded = dp.pre_process_append_new_traces_pkl(config, [
                'StateActionState [ pre[ b ]  ; action[ asd ]  '
                '; actionOwner[ satellite0 ]  ; post[ a ]  ; '
                'traceNum[ 438 ] ]'])
        elif config.preprocessed_read_format == "csv":
            actions_loaded, preconditions_loaded = dp.pre_process_append_new_traces_csv(config, [
                'StateActionState [ pre[ b ]  ; action[ asd ]  '
                '; actionOwner[ satellite0 ]  ; post[ a ]  ; '
                'traceNum[ 438 ] ]'])

    print("\npre_process_append_new_traces time: ", time.clock() - start)

    start = time.clock()
    vocabulary = dp.get_vocabulary(preconditions_loaded)
    print("\nget_vocabulary time: ", time.clock() - start)

    start = time.clock()
    counts = dp.get_counts_as_arrays(actions_loaded, vocabulary.keys())
    tfidf_transformer = TfidfTransformer()
    tfidf = tfidf_transformer.fit_transform(counts)
    print("\ntfidf_transformer fit_transform time: ", time.clock() - start)

    asd = tfidf.toarray()
    asdasd = _collections.OrderedDict()

    for l in range(0, len(actions_loaded)):
        max = np.max(asd[l])
        aaa = np.argwhere(asd[l] > 0.8 * max)

        bbb = []
        for pre, index in vocabulary.items():
            for i in aaa:
                if i == index:
                    bbb.append(pre)

        asdasd[list(actions_loaded.keys())[l]] = bbb

    # a = [[v] for v in asd[2] ]
    # kmeans = KMeans(n_clusters=5, random_state=0).fit(a)
    # print(kmeans.labels_)
