import os
import likelymodelgenerator
import likelymodelgenerator.src.helpers as helpers
from sklearn.feature_extraction.text import TfidfTransformer
from sklearn.feature_extraction.text import CountVectorizer


def dummy(s):
    return s


def run():
    root_dir = os.path.dirname(os.path.abspath(likelymodelgenerator.__file__))  # This is Project Root
    data_file = root_dir + "/data/p03-pfile3-3_Traces_20000.txt"  # This is Data File
    pre_processed_file_csv = root_dir + "/preprocessed/preprocessed.csv"  # This is Preprocessed Root
    pre_processed_file_pkl = root_dir + "/preprocessed/preprocessed.pkl"  # This is Preprocessed Root

    #df = helpers.pre_process_data(data_file, pre_processed_file)
    #df = helpers.load_pre_process_data(pre_processed_file_csv)
    a = helpers.pre_process_data2(data_file, pre_processed_file_pkl)
    b = helpers.load_pre_process_data2(pre_processed_file_pkl)
    c = helpers.pre_process_append_new_traces2(pre_processed_file_pkl, ['StateActionState [ pre[ a ]  ; action[ '
                                                                        'switch_on-agent-satellite0 satellite0 '
                                                                        'instrument0 ]  ; actionOwner[ satellite0 ]  '
                                                                        '; post[ b ]  ; traceNum[ 438 ] ]'])

    filtered = (df.groupby('Action', sort=False)['Preconditions'].apply(sum)).to_frame().reset_index()

    count_vectorizer = CountVectorizer(tokenizer=dummy, preprocessor=dummy)
    count = count_vectorizer.fit_transform(filtered['Preconditions'])

    tfidf_transformer = TfidfTransformer()
    tfidf = tfidf_transformer.fit_transform(count)

