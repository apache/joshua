#!/usr/bin/python

"""
Prepends nonterminals to source and target side of phrase rules, and also
increments the alignment points (if present) to match.
This allows them to be used in the phrase-based decoder.

Usage: gzip -cd grammar.gz | phrase2hiero.py [-moses] | gzip -9n > grammar.new.gz

If you specify "-moses", it will also apply -log() to each of the model weights.

Author: Matt Post <post@cs.jhu.edu>
Date:   June 2016
"""

import sys
import math
import codecs
import argparse

reload(sys)
sys.setdefaultencoding('utf-8')
sys.stdin = codecs.getreader('utf-8')(sys.stdin)
sys.stdout = codecs.getwriter('utf-8')(sys.stdout)
sys.stdout.encoding = 'utf-8'

def incr(alignment):
    """Takes an alignment point (0-1) and increments both sides"""
    points = alignment.split('-')
    return '%d-%d' % (int(points[0]) + 1, int(points[1]) + 1)

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
    tokens[1] = '[X,1] ' + tokens[1]
    tokens[2] = '[X,1] ' + tokens[2]

    # take the -log() of each input token
    if moses and len(tokens) >= 4:
        tokens[3] = ' '.join(map(maybelog, tokens[3].split(' ')))

    if len(tokens) >= 5:
        tokens[4] = ' '.join(map(incr, tokens[4].split(' ')))

    print ' ||| '.join(tokens)
