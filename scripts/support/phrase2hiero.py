#!/usr/bin/python

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
