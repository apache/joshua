#!/usr/bin/env perl -C31
# Matt Post <post@cs.jhu.edu>
# June 2016

# Prepends nonterminals to source and target side of phrase rules.
# This allows them to be used in the phrase-based decoder.
#
# Usage: gzip -cd grammar.gz | phrase2hiero.pl | gzip -9n > grammar.new.gz

use strict;
use warnings;
use File::Basename;
use Getopt::Std;

binmode STDOUT, ':utf8';
binmode STDIN, ':utf8';

while (my $line = <>) {
  my @tokens = split(/ \|\|\| /, $line);
  $tokens[1] = "[X,1] $tokens[1]";
  $tokens[2] = "[X,1] $tokens[2]";
  print join(" ||| ", @tokens);
}
