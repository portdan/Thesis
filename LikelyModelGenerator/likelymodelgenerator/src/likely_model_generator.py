import likelymodelgenerator.src.helpers as helpers
from sklearn.feature_extraction.text import TfidfTransformer
from sklearn.feature_extraction.text import CountVectorizer

def four_pounds_tokenizer(s):
   return s.split(';')

def run():
    df = helpers.pre_process_traces()

    asd = df.groupby('Action')['Preconditions'].apply(sum)

    actions = df['Action'].unique()
    count_vectorizer = CountVectorizer(tokenizer=four_pounds_tokenizer)
    aaa = count_vectorizer.fit_transform(asd)
    tfidf_vectorizer = TfidfTransformer()
    bbb = tfidf_vectorizer.fit_transform(aaa)

    for action in actions:
        asd = df[(df['Action'] == action)]
        asd = asd['Preconditions'].sum()
        count_vectorizer.fit_transform(actions)
    print(actions)
