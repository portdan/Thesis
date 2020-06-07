import likelymodelgenerator.src.helpers as helpers
from sklearn.feature_extraction.text import TfidfTransformer
from sklearn.feature_extraction.text import CountVectorizer
import pandas as pd


def dummy(s):
    return s


def run():
    df = helpers.pre_process_traces()

    filtered = (df.groupby('Action', sort=False)['Preconditions'].apply(sum)).to_frame().reset_index()

    count_vectorizer = CountVectorizer(tokenizer=dummy, preprocessor=dummy)
    aaa = count_vectorizer.fit_transform(filtered['Preconditions'])
    tfidf_vectorizer = TfidfTransformer()
    bbb = tfidf_vectorizer.fit_transform(aaa)

    actions = df['Action'].unique()
    for action in actions:
        asd = df[(df['Action'] == action)]
        asd = asd['Preconditions'].sum()
        count_vectorizer.fit_transform(actions)
    print(actions)
