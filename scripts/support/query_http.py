#!/bin/env python
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""
Allows a file to be queried against a Joshua HTTP server. The file should be tokenized
and normalized, with one sentence per line. This script takes that file, packages it up
into blocks of size 100 (changeable with -b), and sends it to the server. The JSON output
is dumped to STDOUT. If you wish to only dump the "curl" commands instead of calling them,
add "--dry-run".

Usage:

  query_http.py --dry-run -s localhost -p 5674 /path/to/corpus

"""

import sys
import urllib
import argparse
import subprocess

parser = argparse.ArgumentParser(description='Send a (tokenized) test set to a Joshua HTTP server')
parser.add_argument('-s', '--server', dest='server', default='localhost', help='server host')
parser.add_argument('-p', '--port', dest='port', type=int, default=5674, help='server port')
parser.add_argument('-b', '--blocksize', dest='size', type=int, default=100, help='number of sentences at a time')
parser.add_argument('--dry-run', default=None, action='store_true', help='print curl commands only (don\'t run')
parser.add_argument('test_file', help='the (tokenized) test file')
args = parser.parse_args()

sentences = []
def process(sentence = None):
    global sentences

    if sentence is None or len(sentences) == args.size:
        urlstr = '{}:{}/translate?{}'.format(args.server, args.port, urllib.urlencode(sentences))
        cmd = 'curl -s "{}"'.format(urlstr)
        if args.dry_run:
            print cmd
        else:
            subprocess.call(cmd, shell=True)
        sentences = []

    if sentence is not None:
        sentences.append(('q', sentence.rstrip()))

for line in open(args.test_file):
    process(line.rstrip())

process()

