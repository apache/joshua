#!/usr/bin/env perl

# This script packs a grammar dense, unlabeled grammar, where we don't care what labels we give to
# the feature functions.
#
# Usage:
#
#    grammar-packer.pl input-grammar [output-dir [packer-config]]
#
# where
#
#    input-grammar is the input grammar to be packed
#    output-dir is the packed grammar directory to write to (default: grammar.packed)
#    packer-config is the packer config file (default: all floats)
#
# This script *requires* the grammar to be in Hiero format. If you are working with a
# phrase-based model (either extracted from Thrax or from Moses) you must first convert it
# to Hiero format using the script $JOSHUA/scripts/support/phrase2hiero.py.

use strict;
use warnings;
use Getopt::Std;
use File::Temp qw/tempfile/;
use File::Basename qw/basename/;

my %opts = (
  a => 0,         # whether alignments are included in the grammar(s)
  g => '',        # comma-separated list of grammars to pack
  o => '',        # comma-separated list of grammar output directories
  m => '8g',      # amount of memory to give the packer
  T => '/tmp',    # location of temporary space
  v => 0,         # verbose
  s => 0,         # grammars have already been UNIX sorted, skip sorting
);
getopts("am:T:vg:o:s", \%opts) || die usage();
die usage() if (@ARGV);

my $JOSHUA = $ENV{JOSHUA} or die "you must defined \$JOSHUA";
my $CAT    = "$JOSHUA/scripts/training/scat";

sub usage {
  print "Usage: grammar-packer.pl [-a] [-m MEM] [-T /path/to/tmp] -g 'grammar [grammar2 ...]' -o 'grammar.packed [grammar2.packed ...]'\n";
  exit 1;
}

my @grammars = split(' ', $opts{g});
my @outputs = split(' ', $opts{o});

# make sure outputs is same size as inputs, or 0
die usage() if (scalar(@outputs) == 0 || scalar(@grammars) == 0 || scalar(@outputs) != scalar(@grammars));

# if no outputs given, generate default names
if (scalar(@outputs) < scalar(@grammars)) {
  for (my $i = 1; $i < @grammars; $i++) {
    push(@outputs, $i == 1 ? "grammar.packed" : "grammar$i.packed");
  }
}

my $grammar_no = 0;
my @sorted_grammars;
foreach my $grammar (@grammars) {
  $grammar_no++;
  if (! -e $grammar) {
    print "* FATAL: Can't find grammar '$grammar'\n";
    exit 1;
  }

  if ($opts{s}) {
    # The user *swears* the grammars are already sorted! Trust her.
    print STDERR "You claim the grammars are already sorted. Okay, I'll believe you!\n";
    push(@sorted_grammars, $grammar);

  } else {
    # Sort the grammar or phrase table
    my $name = basename($grammar);
    my (undef,$sorted_grammar) = tempfile("${name}XXXX", DIR => $opts{T}, UNLINK => 1);
    print STDERR "Sorting grammar to $sorted_grammar...\n" if $opts{v};

    # regular grammar
    if (system("$CAT $grammar | sed 's/ ||| /\t/g' | LC_ALL=C sort -t'\t' -k2,2 -k3,3 --buffer-size=$opts{m} -T $opts{T} | sed 's/\t/ ||| /g' | gzip -9n > $sorted_grammar")) {
      print STDERR "* FATAL: Couldn't sort the grammar (not enough memory? short on tmp space?)\n";
      exit 2;
    }

    push(@sorted_grammars, $sorted_grammar);
  }
}


# Do the packing using the config.
my $grammars = join(" ", @sorted_grammars);
my $outputs  = join(" ", @outputs);
my $alignments = $opts{a} ? "--ga" : "";
my $cmd = "java -Xmx$opts{m} -cp $JOSHUA/target/joshua-*-jar-with-dependencies.jar org.apache.joshua.tools.GrammarPackerCli -g $grammars --outputs $outputs $alignments";
print STDERR "Packing with $cmd...\n" if $opts{v};

my $retval = system($cmd);

if ($retval == 0) {
  map { unlink($_) } @sorted_grammars;
} else {
  print STDERR "* FATAL: Couldn't pack the grammar.\n";
  print STDERR "* Copying sorted grammars ($grammars) to current directory.\n";
  system("cp $grammars .");
  exit 1;
}
