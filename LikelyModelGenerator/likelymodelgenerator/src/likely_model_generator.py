import likelymodelgenerator.src.helpers as helpers
from sklearn.feature_extraction.text import CountVectorizer

def run():
    df = helpers.pre_process_traces()
    actions = df['Action'].unique()

    for action in actions:
        asd = df[(df['Action'] == action)]

    count_vectorizer = CountVectorizer()
    count_vectorizer.fit_transform(actions)
    print(actions)
