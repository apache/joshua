#!/usr/bin/env perl
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

# Takes (1) a list of feature=value pairs and (2) a weight vector file and produces the score.

use strict;
use warnings;

my %weights;

my $weights_file = shift or die "Usage: score-hypothesis <weights-file>";
open WEIGHTS, $weights_file or die "can't read weights file '$weights_file'";
while (my $line = <WEIGHTS>) {
  chomp($line);
  next if $line =~ /^\s*$/;
  my ($key,$value) = split(' ', $line);

  $weights{$key} = $value;
}
close(WEIGHTS);

my $sum = 0.0;
while (my $line = <>) {
  chomp($line);

  my @pairs = split(' ', $line);
  foreach my $pair (@pairs) {
    my ($key,$value) = split('=', $pair);
    $sum += $weights{$key} * $value if exists $weights{$key};
  }
}

print "$sum\n";
