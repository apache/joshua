#!/bin/bash

# Easy way to start up Joshua

set -u

$JOSHUA/bin/joshua \
  -server-type http -server-port 5674 \
  -feature-function OOVPenalty \
  -feature-function "PhrasePenalty -owner custom" \
  -weight-overwrite "OOVPenalty 1 PhrasePenalty -1" \
  -lowercase -project-case \
  -mark-oovs
