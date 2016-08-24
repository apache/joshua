#!/bin/bash

git clone https://github.com/kpu/kenlm.git ext/kenlm
(cd ext/kenlm; git checkout 56fdb5c44fca34d5a2e07d96139c28fb163983c5)
./jni/build_kenlm.sh

git clone https://github.com/joshua-decoder/berkeleylm.git ext/berkeleylm
(cd ext/berkeleylm; ant)

git clone https://github.com/joshua-decoder/thrax.git
(cd thrax; ant)

git clone https://github.com/joshua-decoder/giza-pp.git ext/giza-pp
(make -j4 -C ext/giza-pp all install)

git clone https://github.com/joshua-decoder/symal.git ext/symal
(make -C ext/symal all)

git clone https://github.com/joshua-decoder/berkeleyaligner ext/berkeleyaligner
(cd ext/berkeleyaligner; ant)

