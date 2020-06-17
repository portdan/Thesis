import likelymodelgenerator.src.likely_model_generator as lmg
import argparse

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Process some integers.')
    parser.add_argument('config', type=str, help='configuration file path')

    args = parser.parse_args()

    lmg.run(args)
