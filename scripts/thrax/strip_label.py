#!/usr/bin/env python

"""
Removes labels (if present) from features.

e.g.,

    [X] ||| le ||| the ||| e_given_f_lex=1

becomes

    [X] ||| le ||| the ||| 1
"""

import re
import sys
import codecs

reload(sys)
sys.setdefaultencoding('utf-8')
sys.stdin = codecs.getreader('utf-8')(sys.stdin)
sys.stdout = codecs.getwriter('utf-8')(sys.stdout)
sys.stdout.encoding = 'utf-8'

for line in sys.stdin:
    tokens = line.split(' ||| ')
    tokens[3] = re.sub(r'\S*=', '', tokens[3])

    print ' ||| '.join(tokens),
