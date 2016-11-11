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

# Remove HTML codes from data.

binmode(STDIN,  ":utf-8");
binmode(STDOUT, ":utf-8");

my %map = (
  "&#39;" => "'",
  "&#44;" => ",",
  "&amp;" => "&",
  "&gt;"  => ">",
  "&lt;"  => "<",
  "&quot;" => "\"",
  "&#257;" => "ā",
  "&#257:" => "ā",
  "&#257 " => "ā",
  "&#7751;" => "Ṇ",
  "&#7751:" => "Ṇ",
  "&#7779;" => "Ṣ",
  "&#7779:" => "Ṣ",
  "&#;" => "",
);

while (my $line = <>) {
  foreach my $key (keys %map) {
    $line =~ s/$key/$map{$key}/g;
  }

  print $line;
}
