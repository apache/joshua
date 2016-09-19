#!/usr/bin/python
"""
Converts Joshua 6 config files to the Joshua 7 format. It is not smart about paths,
so make sure that you run it in the root directory of any relative paths.

Usage:
  cat joshua-v6.config | $JOSHUA/scripts/compat/sevenize_my_conf_plz.py \
    > joshua.conf
"""

import os
import re
import sys

weights = {}
tms = []
features = []

def smooth_key(key):
    return key.replace('-', '_').replace('maxspan', 'span_limit').replace('search', 'search_algorithm')

def moses_phrasetable_error():
    sys.stderr.write('MOSES phrase table format (tm keyword "moses") is no longer support')
    sys.stderr.write('Use $JOSHUA/scripts/support/phrase2hiero.py to convert it to Joshua\'s format')
    sys.stderr.write('Then change the type to "phrase" and try again')
    sys.exit(1)

def parse_args(line):
    found = {}
    
    """Assume the argument string is "-key value" pairs. Don't bother with error checking."""
    tokens = line.split(' ')
    type = tokens.pop(0)
    for i in range(0, len(tokens), 2):
        key = smooth_key(tokens[i][1:]) # strip leading -
        val = tokens[i+1]

        found[key] = val

        if key == 'path':
            if type == 'thrax' or type == 'hiero':
                if os.path.isdir(val):
                    type = 'PackedGrammar'
                    found['rule_cache_size'] = 10000
                elif type == 'moses':
                    moses_phrasetable_error()
                elif type == 'phrase':
                    type = 'PhraseTable'
                else:
                    type = 'TextGrammar'

    found['class'] = type

    return found

def parse_args_to_string(line):
    return ', '.join(['%s = %s' % (k,v) for k,v in parse_args(line).iteritems()])

for line in sys.stdin:
    line = line.rstrip()

    if line.startswith('#') or re.match(r'^\s*$', line):
        continue

    if line.find('=') == -1:
        name, weight = line.split(' ', 1)
        weights[name] = weight

    elif line.startswith('tm'):
        """Two types of tm lines are supported. Ones that look like this:

               tm = thrax pt 12 src/test/resources/decoder/constrained/grammar.gz

            and ones that look like this:

               tm = thrax -owner pt -maxlen 12 -path src/test/resources/decoder/constrained/grammar.gz
        """

        _, tm = re.split(r'\s*=\s*', line, 1)

        if tm.find("-path") == -1:
            # first kind -- old format where all values are listed

            if classType == 'moses':
                moses_phrasetable_error()

            elif (classType == 'phrase'):
                className = 'PhraseTable'

            else:
                classType, owner, maxlen, path = tm.split(' ')
                className = 'TextGrammar'
                if os.path.isdir(path):
                    className = 'PackedGrammar'

            tms.append('class = %s, owner = %s, span_limit = %s, path = %s' % (className, owner, maxlen, path))

        else:
            # second kind
            tms.append(parse_args_to_string(tm))

    elif line.startswith('lm'):
        """Backwards compatibility for old LM specification method"""

        _, lm = re.split(r'\s*=\s*', line, 1)
        lm_type, order, left, right, default, path = lm.split(' ')

        className = 'LanguageModel'
        if left == 'true':
            className = 'StateMinimizingLanguageModel'

        features.append('class = %s, lm_type = %s, lm_order = %s, lm_file = %s' % (className, lm_type, order, path))

    elif line.startswith('feature-function') or line.startswith('feature_function'):
        _, feature = re.split(r'\s*=\s*', line, 1)

        features.append(parse_args_to_string(feature))

    else:
        key, value = re.split(r'\s*=\s*', line, 1)
        key = smooth_key(key)
        print key, '=', value

print
print 'feature_functions = ['
for feature in features:
    print '  {', feature, '}'
print ']'

print
print 'grammars = ['
for tm in tms:
    print '  {', tm, '}'
print ']'

print
print 'weights = {'
for weight in sorted(weights.keys()):
    print ' ', weight, '=', weights[weight]
print '}'

"""
top_n = 0
use_unique_nbest = false
output_format = %s | %a


feature_functions = [
  {class=OOVPenalty}
]

grammars=[
  {class=TextGrammar, owner=pt, span_limit=20, path=src/test/resources/wa_grammar}
  {class=TextGrammar, owner=glue, span_limit=-1, path=src/test/resources/grammar.glue}
]

weights = {
  pt_0=-1
  pt_1=-1
  pt_2=-1
  pt_3=-1
  pt_4=-1
  pt_5=-1
  glue_0=-1
  OOVPenalty=2
}
"""
