#!/bin/bash
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
#
# Strings together the preprocessing scripts

# This script takes
# - a source and target language
# - a language pack directory (which should contain scripts 'prepare.sh' and 'joshua')
# - a prefix point to a test set (e.g., a line from ~mpost/hybrid16/parallel/$lang/test.txt)
#
# It then decodes the test set with the specified model. It assumes the
# model pointed to in the tuned config is the unfiltered, fully packed
# model, which is nice, because you don't have to filter and re-pack.
#
# e.g.,
# qsub test.sh es en apache-joshua-es-en ~mpost/hybrid16/parallel/ar/test/globalvoices-test
#
# The results will be written to a directory evaluation/TESTSET within the language pack.
# The output file will be stored in a file named "output" and the BLEU score in a file
# named "bleu"

# QSUB PARAMETERS
#$ -S /bin/bash -V
#$ -cwd
#$ -j y -o logs
#$ -l mem_free=32G,h_rt=4:00:00,num_proc=8
#$ -m aes

. ~/.bashrc

: ${TMP=/scratch}

# Adjust this to your own model root! Assumes $lang/$rundir
sourcelang=$1
targetlang=$2
lpdir=$3
testset=$4

if [[ -z $4 ]]; then
    echo "Usage: test-lp SOURCE TARGET LANGUAGE_PACK TEST_PREFIX"
    echo "where"
    echo "  SOURCE is the source language extension (e.g., es)"
    echo "  TARGET is the target language extension (e.g., en)"
    echo "  LANGUAGE_PACK points to a language pack directory"
    echo "  TEST_PREFIX is the path prefix to a test set"
    exit
fi

set -u

# Ensure test set exists
if [[ ! -s $testset.$sourcelang ]]; then 
    echo "* FATAL: can't find test set '$testset.$sourcelang'"
    exit
fi

test_name=$(basename $testset)

rundir=$lpdir/evaluation/$test_name
[[ ! -d $rundir ]] && mkdir -p $rundir

# Make sure there's a config file
binary=$lpdir/joshua
config=$lpdir/joshua.config
prepare=$lpdir/prepare.sh
for file in $binary $config $prepare; do
    if [[ ! -s "$file" ]]; then
        echo "* FATAL: '$lpdir' doesn't look like a language pack (can't find $file)."
        exit
    fi
done

# Decode
echo "Decoding $test_name with $lpdir..."
for file in $testset.{$sourcelang,$targetlang}; do
    [[ ! -e $rundir/$(basename $file) ]] && cp $file $rundir/$(basename $file)
done
cat $rundir/$test_name.$sourcelang | $lpdir/prepare.sh | $lpdir/joshua > $rundir/out 2> $rundir/log

tokenize() {
    cat $1 | lang=$2 $prepare | $lpdir/scripts/lowercase.pl > $3
}

if [[ -x $JOSHUA/bin/bleu ]]; then
    echo -n "Scoring with BLEU..."
    tokenize $rundir/out $targetlang $rundir/out.tok
    tokenize $rundir/$test_name.$targetlang $targetlang $rundir/ref.tok
    $JOSHUA/bin/bleu $rundir/out.tok $rundir/ref.tok > $rundir/bleu
    cat $rundir/bleu | grep "BLEU =" | awk '{print $NF}'
fi

