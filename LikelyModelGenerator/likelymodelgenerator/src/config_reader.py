import yaml


class Configuration:

    def __init__(self):
        self.traces_file_path = ""
        self.preprocessed_path = ""
        self.preprocessed_read_format = ""
        self.preprocess_path = ""
        self.preprocess_write_format = ""
        self.preprocess = False
        self.load_preprocessed = False
        self.update_preprocessed = False


def read_config(config_path):
    config = Configuration()

    with open(config_path) as file:
        ymal_config = yaml.safe_load(file)

        traces_read_format = ymal_config['input']['traces']['fileFormat']

        config.traces_file_path = ymal_config['input']['traces']['path'] + '/' \
                                  + ymal_config['input']['traces']['fileName'] + '.' \
                                  + traces_read_format

        config.preprocessed_read_format = ymal_config['input']['preprocessed']['fileFormat']

        config.preprocessed_path = ymal_config['input']['preprocessed']['path'] + '/' \
                                  + ymal_config['input']['preprocessed']['fileName'] + '.' \
                                  + config.preprocessed_read_format

        config.preprocess_write_format = ymal_config['output']['preprocess']['fileFormat']

        config.preprocess_path = ymal_config['output']['preprocess']['path'] + '/' \
                                  + ymal_config['output']['preprocess']['fileName'] + '.' \
                                  + config.preprocess_write_format

        config.preprocess = ymal_config['workMode']['preprocess']
        config.load_preprocessed = ymal_config['workMode']['load']
        config.update_preprocessed = ymal_config['workMode']['update']


    return config
