#!/bin/bash

# This script assembles a language pack.
# 

langpair=$1
config=$2
credits=$3
benchmark=$4

date=$(date +%Y-%m-%d)

if [[ -z $4 ]]; then
    echo "Usage: $0 langpair config credits-file benchmark-file"
    echo "where"
    echo "  langpair is the language pair, (e.g., es-en)"
    echo "  config is the tuned Joshua config, (1/tune/joshua.config.final)"
    echo "  credits-file is a file describing how the model was built (1/CREDITS"
    echo "  benchmark-file is a file describing model performance on test sets (1/BENCHMARK)"
    exit 1
fi

JOSHUA=$(dirname $0/../..)
date=$(date +%Y-%m-%d)
dest=$langpair/releases/apache-joshua-$langpair-$date
source=$(echo $langpair | cut -d- -f1)
target=$(echo $langpair | cut -d- -f2)

# Create the jar file
(cd $JOSHUA && mvn compile assembly:single)

# Copy over critical infrastructure files
[[ ! -d "$dest/target" ]] && mkdir "$dest/target"
[[ ! -d "$dest/bin" ]] && mkdir "$dest/bin"
cp $JOSHUA/target/joshua-*-jar-with-dependencies.jar $dest/target
cp $JOSHUA/bin/joshua $dest/bin

# Copy over the web demonstration
cp -a $JOSHUA/demo web

# Create the bundle
# ... --copy-config-options "-lower-case true -project-case true"
$JOSHUA/scripts/support/run_bundler.py \
    --force \
    --verbose \
    --root $langpair/$modelno \
    --copy-config-options \
      '-top-n 1 -output-format %S -mark-oovs false -lowercase true -projectcase true' \
    $config \
    $dest

# Copy over preprocessing scripts
cp -a $langpair/$modelno/scripts $dest/scripts

# Copy the credits file
cat $credits \
    > $dest/CREDITS

# Summarize test set performance for the README
cat $benchmark \
    > $dest/BENCHMARK

# Create the README
cat $JOSHUA/scripts/language-pack/README.template \
    | perl -pe "s/<SOURCE>/$source/g" \
    | perl -pe "s/<TARGET>/$target/g" \
    | perl -pe "s/<DATE>/$date/g" \
    > $dest/README


