'''
Created on Apr 24, 2019

@author: daniel
'''

import shutil
import os

import logging
logger = logging.getLogger(__name__)

def clear_directory(dirpath, delete=False):
    
    logger.info("clear_directory : " + dirpath)
    
    if os.path.exists(dirpath):
        shutil.rmtree(dirpath)
    if not delete:
        os.makedirs(dirpath)
