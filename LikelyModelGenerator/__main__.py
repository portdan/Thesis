#!/usr/bin/env python3

from src import likely_model_generator
import argparse

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Process some integers.')
    parser.add_argument('config_path', type=str, help='configuration file path')
    parser.add_argument('-p', '--preprocess', help='Preprocess the traces', action="store_true")
    parser.add_argument('-u', '--update', help='Update Preprocessed data', action="store_true")
    parser.add_argument('-c', '--create', help='Create Likely Model', action="store_true")

    args = parser.parse_args()

    likely_model_generator.run(args)
