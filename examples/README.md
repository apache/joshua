# Joshua Examples

The examples in this directory demonstrate how to exercise different
Joshua features. If you have any comments or questions please submit 
them to [our mailing lists](http://joshua.incubator.apache.org/support/).

Bugs or source code issues should be logged in our 
[Issue Tracker](https://issues.apache.org/jira/browse/joshua).

The decoding examples and model training examples in the subdirectories of this
directory assume you have downloaded the Fisher Spanish--English dataset, which
contains speech-recognizer output paired with English translations. This data
can be downloaded by running the [download.sh](https://github.com/apache/incubator-joshua/blob/master/src/examples/resources/download.sh) script.

# Building a Spanish --> English Translation Model using the Fisher Spanish CALLHOME corpus

An example of how to build a model using the Fisher Spanish CALLHOME corpus

A) Download the corpus:
    1) mkdir $HOME/git
    2) cd $HOME/git
    3) curl -o fisher-callhome-corpus.zip https://codeload.github.com/joshua-decoder/fisher-callhome-corpus/legacy.zip/master
    4) unzip fisher-callhome-corpus.zip
    5) # Set environment variable SPANISH=$HOME/git/fisher-callhome-corpus
    5) mv joshua-decoder-*/ fisher-callhome-corpus

B) Download and install Joshua:
    1) cd /directory/to/install/
    2) git clone https://github.com/apache/incubator-joshua.git
    3) cd incubator-joshua
    4) # Set environment variable JAVA_HOME=/path/to/java    # Try $(readlink -f /usr/bin/javac | sed "s:/bin/javac::")
    5) # Set environment variable JOSHUA=/directory/to/install/joshua
    6) mvn install

C) Train the model:
    1) mkdir -p $HOME/expts/joshua && cd $HOME/expts/joshua
    2) $JOSHUA/bin/pipeline.pl \
        --rundir 1 \
        --readme "Baseline Hiero run" \
        --source es \
        --target en \
        --lm-gen srilm \
        --witten-bell \
        --corpus $SPANISH/corpus/asr/callhome_train \
        --corpus $SPANISH/corpus/asr/fisher_train \
        --tune  $SPANISH/corpus/asr/fisher_dev \
        --test  $SPANISH/corpus/asr/callhome_devtest \
        --lm-order 3
