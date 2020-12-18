import yaml


class Configuration:

    def __init__(self, args):
        # input
        self.traces_file_path = ""
        self.traces_to_use = 0
        self.actions_file_path = ""
        self.agents_file_path = ""
        self.facts_groups_file_path = ""
        # working
        self.data_file_path = ""
        self.last_model_file_path = ""
        self.plan_traces_path = ""
        self.failed_trace_path = ""
        self.iteration = 1
        self.delta_base = 0.1
        self.thresholds_base = 0.8
        self.c_value = 0.2
        # output
        self.model_file_path = ""
        self.status_file_path = ""

        self.read_config(args.config_path)

        # preprocess
        self.preprocess_traces = args.preprocess
        self.update_data = args.update
        self.create_model = args.create

    def read_config(self, config_path):
        with open(config_path) as file:
            yaml_config = yaml.safe_load(file)

            root_dir = yaml_config['rootDir']
            input_dir = root_dir + '/' + yaml_config['input']['path']
            working_dir = root_dir + '/' + yaml_config['working']['path']
            output_dir = root_dir + '/' + yaml_config['output']['path']

            self.traces_file_path = input_dir + '/' + yaml_config['input']['traces']['fileName']
            self.traces_to_use = yaml_config['input']['traces']['tracesToUse']
            self.actions_file_path = input_dir + '/' + yaml_config['input']['actions']['fileName']
            self.agents_file_path = input_dir + '/' + yaml_config['input']['agents']['fileName']
            self.facts_groups_file_path = input_dir + '/' + yaml_config['input']['factsGroups']['fileName']

            self.iteration = yaml_config['working']['iteration']
            self.delta_base = yaml_config['working']['deltaBase']
            self.thresholds_base = yaml_config['working']['thresholdsBase']
            self.c_value = yaml_config['working']['cValue']

            self.data_file_path = working_dir + '/' + yaml_config['working']['data']['fileName']
            self.last_model_file_path = working_dir + '/' + yaml_config['working']['model']['fileName']
            self.plan_traces_path = working_dir + '/' + yaml_config['working']['planTraces']['fileName']
            self.failed_trace_path = working_dir + '/' + yaml_config['working']['failedTrace']['fileName']

            self.model_file_path = output_dir + '/' + yaml_config['output']['likelyModel']['fileName']
            self.status_file_path = output_dir + '/' + yaml_config['output']['status']['fileName']
