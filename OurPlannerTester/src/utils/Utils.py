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
        
def copy_tree(src, dst, symlinks=False, ignore=None):
    
    logger.info("copy_tree directory : " + src + " to " + dst)

    for item in os.listdir(src):
        s = os.path.join(src, item)
        d = os.path.join(dst, item)
        if os.path.isdir(s):
            shutil.copytree(s, d, symlinks, ignore)
        else:
            shutil.copy2(s, d)
