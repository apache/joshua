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
set -u

export KENLM_MAX_ORDER=10
export CXX=${CXX:-g++}
export LDFLAGS+=" -lz -lbz2 -llzma"
#CXXFLAGS+=" -O3 -fPIC -DHAVE_ZLIB"
export CXXFLAGS+=" -I. -O3 -fPIC -DNDEBUG"

export JOSHUA=$($JOSHUA/scripts/misc/canonical_path $(dirname $0)/..)
echo "Using JOSHUA=$JOSHUA"

cd $JOSHUA/ext/kenlm
[[ ! -d build ]] && mkdir build
cd build
cmake .. -DKENLM_MAX_ORDER=$KENLM_MAX_ORDER
#Use this if cmake fails with boost errors
#cmake .. -DBoost_NO_BOOST_CMAKE=TRUE -DBOOST_ROOT=/opt/boost -DKENLM_MAX_ORDER=$KENLM_MAX_ORDER
make -j
cp bin/{query,lmplz,build_binary} $JOSHUA/bin

if [ "$(uname)" == Darwin ]; then
  SUFFIX=dylib
else
  SUFFIX=so
  CXXFLAGS+=" -lrt"
fi

#objects=""
#for i in util/double-conversion/*.cc util/*.cc lm/*.cc $ADDED_PATHS; do
#  if [ "${i%test.cc}" == "$i" ] && [ "${i%main.cc}" == "$i" ]; then
#    $CXX $CXXFLAGS -c $i -o ${i%.cc}.o
#    objects="$objects ${i%.cc}.o"
#  fi
#done

[[ ! -d "$JOSHUA/lib" ]] && mkdir "$JOSHUA/lib"
$CXX -std=gnu++11 -I. -DKENLM_MAX_ORDER=$KENLM_MAX_ORDER -I$JAVA_HOME/include -I$JOSHUA/ext/kenlm -I$JAVA_HOME/include/linux -I$JAVA_HOME/include/darwin $JOSHUA/jni/kenlm_wrap.cc lm/CMakeFiles/kenlm.dir/*.o util/CMakeFiles/kenlm_util.dir/*.o util/CMakeFiles/kenlm_util.dir/double-conversion/*.o -shared -o $JOSHUA/lib/libken.$SUFFIX $CXXFLAGS $LDFLAGS
