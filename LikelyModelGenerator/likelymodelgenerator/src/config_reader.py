import yaml


class Configuration:

    def __init__(self, config_path=None):
        # input
        self.traces_file_path = ""
        self.actions_file_path = ""
        self.preconditions_file_path = ""
        # working
        self.data_file_path = ""
        # preprocess
        self.preprocess_path = ""
        self.preprocess_write_format = ""
        self.preprocess_traces = False
        self.load_data = False
        self.update_data = False

        if config_path is not None:
            self.read_config(config_path)

    def read_config(self, config_path):
        with open(config_path) as file:
            ymal_config = yaml.safe_load(file)

            root_dir = ymal_config['rootDir']
            data_dir = root_dir + '/' + ymal_config['input']['path']
            working_dir = root_dir + '/' + ymal_config['working']['path']

            self.traces_file_path = data_dir + '/' + ymal_config['input']['traces']['fileName']
            self.actions_file_path = data_dir + '/' + ymal_config['input']['actions']['fileName']
            self.preconditions_file_path = data_dir + '/' + ymal_config['input']['preconditions']['fileName']

            self.data_file_path = working_dir + '/' + ymal_config['working']['fileName']

            self.preprocess_traces = ymal_config['workMode']['preprocess']
            self.load_data = ymal_config['workMode']['load']
            self.update_data = ymal_config['workMode']['update']
