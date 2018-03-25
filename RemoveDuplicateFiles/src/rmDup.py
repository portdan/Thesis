'''
Created on Mar 24, 2018

@author: dan
'''

import os
import hashlib
import time
import argparse


def main():
    
    start = time.time()

    args = parse_args()
    
    duplicateFileNames = ()
    
    if args.hashType:   
        duplicateFileNames = check_for_duplicates(args.folderPath,args.hashType)
    else:
        duplicateFileNames = check_for_duplicates(args.folderPath)
    
    for fileName in duplicateFileNames:    
        os.remove(fileName)
        
    end = time.time() 
    
    print("Done! (time - %0.4f" %(end - start)+")")
    
def chunk_reader(fobj, chunk_size=1024):
    """Generator that reads a file in chunks of bytes"""
    while True:
        chunk = fobj.read(chunk_size)
        if not chunk:
            return
        yield chunk


def get_hash(filename, first_chunk_only=False, hash=hashlib.sha1):
    hashobj = hash()
    file_object = open(filename, 'rb')

    if first_chunk_only:
        hashobj.update(file_object.read(1024))
    else:
        for chunk in chunk_reader(file_object):
            hashobj.update(chunk)
    hashed = hashobj.digest()

    file_object.close()
    return hashed


def check_for_duplicates(folderPath, hash=hashlib.sha1):
    hashes_by_size = {}
    hashes_on_1k = {}
    hashes_full = {}
    
    duplicateFileNames = []

    for dirpath, dirnames, filenames in os.walk(folderPath):
        for filename in filenames:
            full_path = os.path.join(dirpath, filename)
            try:
                file_size = os.path.getsize(full_path)
            except (OSError,):
                # not accessible (permissions, etc) - pass on
                pass
            
            duplicate = hashes_by_size.get(file_size)

            if duplicate:
                hashes_by_size[file_size].append(full_path)
            else:
                hashes_by_size[file_size] = []  # create the list for this file size
                hashes_by_size[file_size].append(full_path)

    # For all files with the same file size, get their hash on the 1st 1024 bytes
    for __, files in hashes_by_size.items():
        if len(files) < 2:
            continue    # this file size is unique, no need to spend cpy cycles on it

        for filename in files:
            small_hash = get_hash(filename, first_chunk_only=True)

            duplicate = hashes_on_1k.get(small_hash)
            if duplicate:
                hashes_on_1k[small_hash].append(filename)
            else:
                hashes_on_1k[small_hash] = []          # create the list for this 1k hash
                hashes_on_1k[small_hash].append(filename)

    # For all files with the hash on the 1st 1024 bytes, get their hash on the full file - collisions will be duplicates
    for __, files in hashes_on_1k.items():
        if len(files) < 2:
            continue    # this hash of fist 1k file bytes is unique, no need to spend cpy cycles on it

        for filename in files:
            
            full_hash = get_hash(filename, first_chunk_only=False)

            duplicate = hashes_full.get(full_hash)
            if duplicate:
                print ("Duplicate found: %s and %s" % (filename, duplicate))
                duplicateFileNames.append(filename)
            else:
                hashes_full[full_hash] = filename
    
    return duplicateFileNames


def parse_args():
    
    argparser = argparse.ArgumentParser()
    
    argparser.add_argument(
        "folderPath", help="path to folder", type=str)
    argparser.add_argument(
        "-H", "--hashType", help="type of hashing to be used", type=str)
    
    return argparser.parse_args()


if __name__ == '__main__':
    main()
    
