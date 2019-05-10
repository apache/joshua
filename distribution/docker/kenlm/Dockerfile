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

FROM maven:latest

LABEL Description="Builds the KenLM library for use with language packs" Vendor="Apache Software Foundation"

RUN apt-get update && \
    apt-get install -y \
            cmake \
            git \
            g++ \
            libboost-all-dev \
            libbz2-dev \
            libeigen3-dev \
            liblzma-dev \            
            libz-dev \
            make \
            curl

# set environment variables
ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/
ENV JOSHUA=/code
ENV PORT=5674
ENV JOSHUA_ARGS=""
ENV LD_LIBRARY_PATH=$JOSHUA/lib

# download Joshua source and compile KenLM
RUN mkdir -p /code
WORKDIR /code
RUN curl -L https://api.github.com/repos/apache/joshua/tarball | tar --strip-components=1 -xzvf -
RUN echo y | bash download-deps.sh kenlm

# TODO: check that the LP version is correct

# start Joshua
ENTRYPOINT /model/joshua -config /model/joshua.config.kenlm -server-type http -server-port $PORT -v 1 $JOSHUA_ARGS
