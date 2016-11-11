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

# Takes a corpus of words on STDIN and builds a vocabulary with word
# counts, writing them to STDOUT in the format
#
# ID WORD COUNT

use utf8;
use warnings;
use strict;

binmode(STDIN,  ":utf8");
binmode(STDOUT, ":utf8");

my %count;
while (my $line = <>) {
  chomp($line);
  my @tokens = split(' ', $line);
  map { $count{$_}++ } @tokens;
}

my $id = 1;
map { print $id++ . " $_ $count{$_}\n" } (sort { $count{$b} <=> $count{$a} } keys %count);
