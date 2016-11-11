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
$| = 1;

my $JOSHUA;

BEGIN {
  $JOSHUA = $ENV{JOSHUA};
  unshift(@INC,"$JOSHUA/scripts/training/cachepipe");
}

use strict;
use warnings;
use CachePipe;

my $SCRIPTDIR = "$JOSHUA/scripts";
my $GIZA_TRAINER = "$SCRIPTDIR/training/run-giza.pl";

my %args;
while (@ARGV) {
  my $key = shift @ARGV;
  my $value = shift @ARGV;
  # TODO: error checking!
  $key =~ s/^-//;
  $args{lc $key} = $value;
}

my $aligner_conf = $args{conf} || "$JOSHUA/scripts/training/templates/alignment/word-align.conf";

my $cachepipe = new CachePipe();
$cachepipe->omit_cmd();

# Keep processing chunks until the caller passes us undef
while (my $chunkno = <>) {
  last unless $chunkno;
  chomp($chunkno);

# Create the alignment subdirectory
  my $chunkdir = "alignments/$chunkno";
  system("mkdir","-p", $chunkdir);

  if ($args{aligner} eq "giza") {
    run_giza($chunkdir, $chunkno, 0);
  } elsif ($args{aligner} eq "berkeley") {
    run_berkeley_aligner($chunkdir, $chunkno, $aligner_conf);
  } elsif ($args{aligner} eq "jacana") {
    run_jacana_aligner($chunkdir, $chunkno);
  }

  print "1\n";
}

# This function runs GIZA++, possibly doing both directions at the same time
sub run_giza {
  my ($chunkdir,$chunkno,$do_parallel) = @_;
  my $parallel = ($do_parallel == 1) ? "-parallel" : "";
  $cachepipe->cmd("giza-$chunkno",
                  "rm -f $chunkdir/corpus.0-0.*; $args{giza_trainer} --root-dir $chunkdir -e $args{target} -f $args{source} -corpus $args{train_dir}/splits/$chunkno/corpus -merge $args{giza_merge} $parallel > $chunkdir/giza.log 2>&1",
                  "$args{train_dir}/splits/$chunkno/corpus.$args{source}",
                  "$args{train_dir}/splits/$chunkno/corpus.$args{target}",
                  "$chunkdir/model/aligned.$args{giza_merge}");
}

sub run_berkeley_aligner {
  my ($chunkdir, $chunkno, $aligner_conf) = @_;

  # copy and modify the config file
  open FROM, $aligner_conf or die "can't read berkeley alignment template";
  open TO, ">", "alignments/$chunkno/word-align.conf" or die "can't write to 'alignments/$chunkno/word-align.conf'";
  while (<FROM>) {
    s/<SOURCE>/$args{source}/g;
    s/<TARGET>/$args{target}/g;
    s/<CHUNK>/$chunkno/g;
    s/<TRAIN_DIR>/$args{train_dir}/g;
    print TO;
  }
  close(TO);
  close(FROM);

  # run the job
  $cachepipe->cmd("berkeley-aligner-chunk-$chunkno",
                  "java -d64 -Xmx$args{aligner_mem} -jar $JOSHUA/ext/berkeleyaligner/distribution/berkeleyaligner.jar ++alignments/$chunkno/word-align.conf",
                  "alignments/$chunkno/word-align.conf",
                  "$args{train_dir}/splits/$chunkno/corpus.$args{source}",
                  "$args{train_dir}/splits/$chunkno/corpus.$args{target}",
                  "$chunkdir/training.align");
}

sub run_jacana_aligner {
  my ($chunkdir,$chunkno) = @_;
  my $jacana_home = "$JOSHUA/scripts/training/templates/alignment/jacana";

  # run the job
  $cachepipe->cmd("jacana-aligner-chunk-$chunkno",
                  "java -d64 -Xmx$args{aligner_mem} -DJACANA_HOME=$jacana_home -jar $JOSHUA/lib/jacana-xy.jar -m $jacana_home/resources/model/fr-en.model -src fr -tgt en -a $args{train_dir}/splits/$chunkno/corpus.$args{source} -b $args{train_dir}/splits/$chunkno/corpus.$args{target} -o $chunkdir/training.align");
}
