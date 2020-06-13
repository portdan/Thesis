import os
import likelymodelgenerator
import likelymodelgenerator.src.helpers as helpers
from sklearn.feature_extraction.text import TfidfTransformer
import time
from sklearn.cluster import KMeans
import numpy as np
import _collections


def dummy(s):
    return s


def run():
    root_dir = os.path.dirname(os.path.abspath(likelymodelgenerator.__file__))  # This is Project Root
    data_file = root_dir + "/data/p03-pfile3-3_Traces_20000.txt"  # This is Data File
    # data_file = root_dir + "/data/probLOGISTICS-4-0-1_Traces_44.txt"  # This is Data File
    pre_processed_file_csv = root_dir + "/preprocessed/preprocessed.csv"  # This is Preprocessed Root
    pre_processed_file_pkl = root_dir + "/preprocessed/preprocessed.pkl"  # This is Preprocessed Root

    # start = time.clock()
    # df = helpers.pre_process_data_csv(data_file, pre_processed_file_csv)
    # print("\npre_process_data time: ", time.clock() - start)
    #
    # start = time.clock()
    # df = helpers.load_pre_process_data_csv(pre_processed_file_csv)
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
    actions_preprocessed, preconditions_preprocessed = helpers.pre_process_data_pkl(data_file, pre_processed_file_pkl)
    print("\npre_process_data2 time: ", time.clock() - start)

    start = time.clock()
    actions_loaded, preconditions_loaded = helpers.load_pre_process_data_pkl(pre_processed_file_pkl)
    print("\nload_pre_process_data2 time: ", time.clock() - start)

    start = time.clock()
    actions_loaded, preconditions_loaded = helpers.pre_process_append_new_traces_pkl(pre_processed_file_pkl, [
        'StateActionState [ pre[ b ]  ; action[ asd ]  '
        '; actionOwner[ satellite0 ]  ; post[ a ]  ; '
        'traceNum[ 438 ] ]'])
    print("\npre_process_append_new_traces_pkl time: ", time.clock() - start)

    start = time.clock()
    vocabulary = helpers.get_vocabulary(preconditions_loaded)
    print("\nget_vocabulary time: ", time.clock() - start)

    start = time.clock()
    counts = helpers.get_counts_as_arrays(actions_loaded, vocabulary.keys())
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
