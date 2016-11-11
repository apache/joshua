#!/usr/bin/perl -w
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

#input is filtered grammar file
#output is english vocabulary
my %eng_voc=();


#add lm vocabulary here
$eng_voc{"<unk>"}=1;
$eng_voc{"<s>"}=1;
$eng_voc{"</s>"}=1;
$eng_voc{"-pau-"}=1;


while(my $line=<>){
    chomp($line);
    my @fds=split(/\s+\|{3}\s+/,$line);
    my $eng=$fds[2];
    my @eng_wrds=split(/\s+/, $eng);
    foreach my $wrd (@eng_wrds){
	$wrd =~ s/^\s+//g;
	$wrd =~ s/\s+$//g;
	if($wrd =~ m/^\[[a-zA-Z]+,\d+\]$/){ #[PHRASE,1]
	    next;
	}
	$eng_voc{$wrd}=1;
    }	
}

foreach my $wrd (keys %eng_voc){
    print "$wrd\n";
}
