/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.joshua.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrammarPackerCli {
  
  private static final Logger LOG = LoggerFactory.getLogger(GrammarPackerCli.class);
  
  // Input grammars to be packed (with a joint vocabulary)
  @Option(name = "--grammars", aliases = {"-g", "-i"}, handler = StringArrayOptionHandler.class, required = true, usage = "list of grammars to pack (jointly, i.e. they share the same vocabulary)")
  private final List<String> grammars = new ArrayList<>();
  
  // Output grammars
  @Option(name = "--outputs", aliases = {"-p", "-o"}, handler = StringArrayOptionHandler.class, required = true, usage = "output directories of packed grammars.")
  private final List<String> outputs = new ArrayList<>();
  
  // Output grammars
  @Option(name = "--alignments", aliases = {"-a", "--fa"}, handler = StringArrayOptionHandler.class, required = false, usage = "alignment files")
  private final List<String> alignments_filenames = new ArrayList<>();
  
  // Config filename
  @Option(name = "--config_file", aliases = {"-c"}, required = false, usage = "(optional) packing configuration file")
  private String config_filename;
  
  @Option(name = "--dump_files", aliases = {"-d"}, handler = StringArrayOptionHandler.class, usage = "(optional) dump feature stats to file")
  private final List<String> featuredump_filenames = new ArrayList<>();
  
  @Option(name = "--ga", usage = "whether alignments are present in the grammar")
  private final boolean grammar_alignments = false;
  
  @Option(name = "--slice_size", aliases = {"-s"}, required = false, usage = "approximate slice size in # of rules (default=1000000)")
  private final int slice_size = 1000000;
  
  
  private void run() throws IOException {

    final List<String> missingFilenames = new ArrayList<>(grammars.size());
    missingFilenames.addAll(grammars.stream().filter(g -> !new File(g).exists()).collect(Collectors.toList()));
    if (!missingFilenames.isEmpty()) {
      throw new IOException("Input grammar files not found: " + missingFilenames.toString());
    }
    
    if (config_filename != null && !new File(config_filename).exists()) {
      throw new IOException("Config file not found: " + config_filename);
    }

    if (!outputs.isEmpty()) {
      if (outputs.size() != grammars.size()) {
        throw new IOException("Must provide an output directory for each grammar");
      }
      final List<String> existingOutputs = new ArrayList<>(outputs.size());
      existingOutputs
          .addAll(outputs.stream().filter(o -> new File(o).exists()).collect(Collectors.toList()));
      if (!existingOutputs.isEmpty()) {
        throw new IOException("These output directories already exist (will not overwrite): " + existingOutputs.toString());
      }
    }
    if (outputs.isEmpty()) {
      outputs.addAll(grammars.stream().map(g -> g + ".packed").collect(Collectors.toList()));
    }
    
    if (!alignments_filenames.isEmpty()) {
      final List<String> missingAlignmentFiles = new ArrayList<>(alignments_filenames.size());
      missingAlignmentFiles.addAll(alignments_filenames.stream().filter(a -> !new File(a).exists())
          .collect(Collectors.toList()));
      if (!missingAlignmentFiles.isEmpty()) {
        throw new IOException("Alignment files not found: " + missingAlignmentFiles.toString());
      }
    }

    // create Packer instances for each grammar
    final List<GrammarPacker> packers = new ArrayList<>(grammars.size());
    for (int i = 0; i < grammars.size(); i++) {
      LOG.info("Starting GrammarPacker for {}",  grammars.get(i));
      final String alignment_filename = alignments_filenames.isEmpty() ? null : alignments_filenames.get(i);
      final String featuredump_filename = featuredump_filenames.isEmpty() ? null : featuredump_filenames.get(i);
      final GrammarPacker packer = new GrammarPacker(
          grammars.get(i),
          config_filename,
          outputs.get(i),
          alignment_filename,
          featuredump_filename,
          grammar_alignments,
          slice_size);
      packers.add(packer);
    }
    
    // run all packers in sequence, accumulating vocabulary items
    for (final GrammarPacker packer : packers) {
      LOG.info("Starting GrammarPacker for {}", packer.getGrammar());
      packer.pack();
      LOG.info("PackedGrammar located at {}", packer.getOutputDirectory());
    }
    
    // for each packed grammar, overwrite the internally serialized vocabulary with the current global one.
    for (final GrammarPacker packer : packers) {
      LOG.info("Writing final common Vocabulary to {}",  packer.getOutputDirectory());
      packer.writeVocabulary();
    }
  }

  public static void main(String[] args) throws IOException {
    final GrammarPackerCli cli = new GrammarPackerCli();
    final CmdLineParser parser = new CmdLineParser(cli);

    try {
      parser.parseArgument(args);
      cli.run();
    } catch (CmdLineException e) {
      LOG.error(e.getMessage(), e);
      parser.printUsage(System.err);
      System.exit(1);
    }
  }

}
