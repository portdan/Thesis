import yaml


class Configuration:

    def __init__(self, config_path=None):
        # input
        self.traces_file_path = ""
        self.traces_to_use = 0
        self.actions_file_path = ""
        self.preconditions_file_path = ""
        # working
        self.data_file_path = ""
        self.last_model_file_path = ""
        self.iteration = 1
        self.delta_base = 0.1
        self.thresholds_base = 0.8
        # output
        self.model_file_path = ""
        # preprocess
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
            output_dir = root_dir + '/' + ymal_config['output']['path']

            self.traces_file_path = data_dir + '/' + ymal_config['input']['traces']['fileName']
            self.traces_to_use = ymal_config['input']['traces']['tracesToUse']
            self.actions_file_path = data_dir + '/' + ymal_config['input']['actions']['fileName']
            self.preconditions_file_path = data_dir + '/' + ymal_config['input']['preconditions']['fileName']

            self.iteration = ymal_config['working']['iteration']
            self.delta_base = ymal_config['working']['deltaBase']
            self.thresholds_base = ymal_config['working']['thresholdsBase']

            self.data_file_path = working_dir + '/' + ymal_config['working']['data']['fileName']
            self.last_model_file_path = working_dir + '/' + ymal_config['working']['model']['fileName']

            self.model_file_path = output_dir + '/' + ymal_config['output']['fileName']

            self.preprocess_traces = ymal_config['workMode']['preprocess']
            self.load_data = ymal_config['workMode']['load']
            self.update_data = ymal_config['workMode']['update']
