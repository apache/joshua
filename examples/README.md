# Joshua Examples

The examples in this directory demonstrate how to exercise different
Joshua features. If you have any comments or questions please submit 
them to [our mailing lists](https://cwiki.apache.org/confluence/display/JOSHUA/Support).

Bugs or source code issues should be logged in our 
[Issue Tracker](https://issues.apache.org/jira/browse/joshua).

The decoding examples and model training examples in the subdirectories of this
directory assume you have downloaded the Fisher Spanish--English dataset, which
contains speech-recognizer output paired with English translations. This data
can be downloaded by running the [download.sh](https://github.com/apache/incubator-joshua/blob/master/examples/download.sh) script.

# Building a Spanish --> English Translation Model using the Fisher Spanish CALLHOME corpus

An example of how to build a model using the Fisher Spanish CALLHOME corpus

A) Download the corpus:
```
$ mkdir $HOME/git
$ cd $HOME/git
$ curl -o fisher-callhome-corpus.zip https://codeload.github.com/joshua-decoder/fisher-callhome-corpus/legacy.zip/master
$ unzip fisher-callhome-corpus.zip
$ export SPANISH=$HOME/git/fisher-callhome-corpus
$ mv joshua-decoder-*/ fisher-callhome-corpus
```

B) Download and install Joshua as per the [Quickstart](https://github.com/apache/incubator-joshua#quick-start).

C) Train the model:
```
$ mkdir -p $HOME/expts/joshua && cd $HOME/expts/joshua
$ $JOSHUA/bin/pipeline.pl \
        --type hiero \
        --rundir 1 \
        --readme "Baseline Hiero run" \
        --source es \
        --target en \
        --witten-bell \
        --corpus $SPANISH/corpus/asr/callhome_train \
        --corpus $SPANISH/corpus/asr/fisher_train \
        --tune  $SPANISH/corpus/asr/fisher_dev \
        --test  $SPANISH/corpus/asr/callhome_devtest \
        --lm-order 3
```
