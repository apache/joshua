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

# Matt Post <post@cs.jhu.edu>

# Takes a list of tab-separated strings on STDIN and a single argument N, a threshold. If either of
# the first two fields has mroe than N tokens, the line is skipped.

# e.g.,
# paste corpus.en corpus.fr | trim_parallel_corpus.pl 40 | split2files en.trimmed.40 fr.trimmed.40

my $thresh = shift || 100;

while (my $line = <>) {
  my ($line1,$line2,$rest) = split(/\t/,$line,3);

  # Make sure they're both defined
  next unless (defined $line1 and defined $line2);

  # Skip if either side is over the threshold
  my @tokens1 = split(' ', $line1);
  my @tokens2 = split(' ', $line2);
  next if (@tokens1 > $thresh || @tokens2 > $thresh) || @tokens1 == 0 || @tokens2 == 0;

  # Otherwise print the whole line
  print $line;
}
