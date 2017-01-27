#!/bin/bash
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

# The tools to install. This is overridden if there is a command-line argument.
tools="kenlm berkeleylm thrax giza berkeleyaligner"
[[ ! -z $1 ]] && tools=$1

echo "Warning: this script downloads many tools used in building and running Joshua."
echo "Not all of them are Apache Licensed. If you wish to continue, type 'y' and hit Enter".
echo -n "Continue? (y to continue) "
read j
if [[ $j != 'y' ]]; then
    echo "Quitting."
fi

echo "Installing $tools..."

for tool in $tools; do
    echo "Trying to install tool '$tool'..."
    case $tool in 
	      kenlm)
            git clone https://github.com/kpu/kenlm.git ext/kenlm
            (cd ext/kenlm; git checkout e6a600c8ac4062a9e6644f91232d3ba09469c4f4)
            ./jni/build_kenlm.sh
            ;;
        berkeleylm)
            git clone https://github.com/joshua-decoder/berkeleylm.git ext/berkeleylm
            (cd ext/berkeleylm; ant)
            ;;
        thrax)
            git clone https://github.com/joshua-decoder/thrax.git
            (cd thrax; ant)
            ;;
        giza)
            git clone https://github.com/joshua-decoder/giza-pp.git ext/giza-pp
            (make -j4 -C ext/giza-pp all install)
        
            git clone https://github.com/joshua-decoder/symal.git ext/symal
            (make -C ext/symal all)
            ;;
        berkeleyaligner)
            git clone https://github.com/joshua-decoder/berkeleyaligner ext/berkeleyaligner
            (cd ext/berkeleyaligner; ant)
            ;;
        *)
            echo "No such tool '$tool'"
            ;;
    esac
done

