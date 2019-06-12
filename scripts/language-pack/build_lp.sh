#!/usr/bin/bash
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

# This script assembles a language pack.
# 

langpair=$1
config=$2
mem=$3
credits=$4
benchmark=$5
example=$6

date=$(date +%Y-%m-%d)

if [[ -z $3 ]]; then
    echo "Usage: $0 langpair config mem credits-file benchmark-file example"
    echo "where"
    echo "  langpair is the language pair, (e.g., es-en)"
    echo "  config is the tuned Joshua config, (1/tune/joshua.config.final)"
    echo "  mem is the amount of memory the decoder needs"
    echo "  [optional] credits-file is a file describing how the model was built (1/CREDITS"
    echo "  [optional] benchmark-file is a file describing model performance on test sets (1/BENCHMARK)"
    echo "  [optional] example is a path prefix to a pair of small (~10 lines) example files"
    exit 1
fi

set -u
set -e

JOSHUA=$(dirname $0)/../..
ISO=$JOSHUA/scripts/misc/iso639
date=$(date +%Y-%m-%d)
dest=releases/apache-joshua-$langpair-$date
source_abbr=$(echo $langpair | cut -d- -f1)
target_abbr=$(echo $langpair | cut -d- -f2)
source=$($ISO $source_abbr)
target=$($ISO $target_abbr)

# Create the jar file if it's not there
JARFILE=$(ls -tr $JOSHUA/target/joshua-*-jar-with-dependencies.jar | tail -n1)
if [[ ! -e "$JARFILE" ]]; then
    (cd $JOSHUA && mvn clean package)
fi

# Create the bundle
# ... --copy-config-options "-lower-case true -project-case true"
$JOSHUA/scripts/language-pack/copy_model.py \
    --force \
    --verbose \
    --mem $mem \
    --copy-config-options \
      '-top-n 1 -output-format %S -mark-oovs false -lower-case true -project-case true' \
    $config \
    $dest

copy_template() {
  cat $1 \
    | perl -pe "s/<SOURCE>/$source/g" \
    | perl -pe "s/<TARGET>/$target/g" \
    | perl -pe "s/<SRC>/$source_abbr/g" \
    | perl -pe "s/<TRG>/$target_abbr/g" \
    | perl -pe "s/<MEM>/$mem/g" \
    | perl -pe "s/<DATE>/$date/g" \
    > $2
}

# Create the target directory and copy over the jarfile
[[ ! -d "$dest/target" ]] && mkdir -p "$dest/target"
cp $JARFILE $dest/target

# create the LP config file
version=3
echo "version = $version" > $dest/lp.conf

# Copy over the web demonstration
cp -a $JOSHUA/demo $dest/web

# Copy over preprocessing scripts
cp -a $JOSHUA/scripts/preparation $dest/scripts
copy_template "$JOSHUA/scripts/language-pack/prepare.sh" "$dest/prepare.sh"
chmod 555 $dest/prepare.sh

# Create the README
copy_template "$JOSHUA/scripts/language-pack/README.template" "$dest/README"
chmod 444 $dest/README

# Copy the credits file
if [[ ! -z $credits ]]; then
    cat $credits > $dest/CREDITS
    chmod 444 $dest/CREDITS
fi

# Summarize test set performance for the README
if [[ ! -z $benchmark ]]; then
    cat $benchmark > $dest/BENCHMARK
    chmod 444 $dest/BENCHMARK
fi

# Copy over the example files
if [[ ! -z $example ]]; then
    for ext in $source_abbr $target_abbr; do
        [[ ! -s $example.$ext ]] && echo "Can't find example file $example.$ext, quitting" && exit
        cp $example.$ext $dest/example.$ext
        chmod 444 $dest/example.$ext
    done
fi
