#!/bin/bash

git clone https://github.com/kpu/kenlm.git ext/kenlm
(cd ext/kenlm; git checkout 56fdb5c44fca34d5a2e07d96139c28fb163983c5)
./jni/build_kenlm.sh

git clone https://github.com/joshua-decoder/berkeleylm.git ext/berkeleylm
(cd ext/berkeleylm; ant)
