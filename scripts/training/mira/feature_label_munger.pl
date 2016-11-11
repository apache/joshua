#!/usr/bin/perl
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

# Joshua outputs features labels in the form of a list of "key=value"
# pairs, while the Moses scripts expect "key= value" and also require
# that (all and only) sparse feature names contain an underscore. This
# script does the conversion so that we can use Moses' sparse training
# code.

use strict;
use warnings;

while (my $line = <>) {
  my @tokens = split(/ \|\|\| /, $line);

  if (@tokens > 1) {

    # Insert an assignment space
    $tokens[2] =~ s/=/= /g;

    # Remove underscores from dense features so they'll not get treated as sparse
    $tokens[2] =~ s/tm_(\w+)_(\d+)=/tm-$1-$2=/g;
    $tokens[2] =~ s/lm_(\d+)=/lm-$1=/g;

    # Add underscores to sparse features so they'll not get treated as dense
    $tokens[2] =~ s/OOVPenalty=/OOV_Penalty=/g;

    print join(" ||| ", @tokens);
  }
}
