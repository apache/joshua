#!/usr/bin/python
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
Converts a Moses phrase table to a Joshua phrase table. The differences are
(a) adding an LHS and (b) applying -log() to all the model weights.

Usage: gzip -cd grammar.gz | phrase2hiero.py | gzip -9n > grammar.new.gz

Author: Matt Post <post@cs.jhu.edu>
Date:   June 2016
"""

import sys
import math
import codecs

reload(sys)
sys.setdefaultencoding('utf-8')
sys.stdin = codecs.getreader('utf-8')(sys.stdin)
sys.stdout = codecs.getwriter('utf-8')(sys.stdout)
sys.stdout.encoding = 'utf-8'

def maybelog(value):
    """Takes a feature value and returns -log(x) if it is a scalar"""
    try:
        return str(-1.0 * math.log(float(value)))
    except ValueError:
        return value

for line in sys.stdin:
    moses = False

    # Moses phrase tables do not have a left-hand side symbol, add that
    if not line.startswith('['):
        line = '[X] ||| ' + line
        moses = True

    # Get all the fields
    tokens = line.split(r' ||| ')

    # take the -log() of each input token
    if moses and len(tokens) >= 4:
        tokens[3] = ' '.join(map(maybelog, tokens[3].split(' ')))

    print ' ||| '.join(tokens),
