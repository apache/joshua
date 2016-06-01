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
package org.apache.joshua.decoder.ff.tm;

import static org.apache.joshua.decoder.ff.tm.packed.PackedGrammar.VOCABULARY_FILENAME;
import static org.apache.joshua.util.FormatUtils.cleanNonTerminal;
import static org.apache.joshua.util.FormatUtils.isNonterminal;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.util.io.LineReader;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateGlueGrammar {


  private static final Logger LOG = LoggerFactory.getLogger(CreateGlueGrammar.class);

  private final Set<String> nonTerminalSymbols = new HashSet<>();

  @Option(name = "--grammar", aliases = {"-g"}, required = true, usage = "provide grammar to determine list of NonTerminal symbols.")
  private String grammarPath;
  
  @Option(name = "--goal", aliases = {"-goal"}, required = false, usage = "specify custom GOAL symbol. Default: 'GOAL'")
  private String goalSymbol = cleanNonTerminal(new JoshuaConfiguration().goal_symbol);

  /* Rule templates */
  // [GOAL] ||| <s> ||| <s> ||| 0
  private static final String R_START = "[%1$s] ||| <s> ||| <s> ||| 0";
  // [GOAL] ||| [GOAL,1] [X,2] ||| [GOAL,1] [X,2] ||| -1
  private static final String R_TWO = "[%1$s] ||| [%1$s,1] [%2$s,2] ||| [%1$s,1] [%2$s,2] ||| -1";
  // [GOAL] ||| [GOAL,1] </s> ||| [GOAL,1] </s> ||| 0
  private static final String R_END = "[%1$s] ||| [%1$s,1] </s> ||| [%1$s,1] </s> ||| 0";
  // [GOAL] ||| <s> [X,1] </s> ||| <s> [X,1] </s> ||| 0
  private static final String R_TOP = "[%1$s] ||| <s> [%2$s,1] </s> ||| <s> [%2$s,1] </s> ||| 0";
  
  private void run() throws IOException {
    
    File grammar_file = new File(grammarPath);
    if (!grammar_file.exists()) {
      throw new IOException("Grammar file doesn't exist: " + grammarPath);
    }

    // in case of a packedGrammar, we read the serialized vocabulary,
    // collecting all cleaned nonTerminal symbols.
    if (grammar_file.isDirectory()) {
      Vocabulary.read(new File(grammarPath + File.separator + VOCABULARY_FILENAME));
      for (int i = 0; i < Vocabulary.size(); ++i) {
        final String token = Vocabulary.word(i);
        if (isNonterminal(token)) {
          nonTerminalSymbols.add(cleanNonTerminal(token));
        }
      }
    // otherwise we collect cleaned left-hand sides from the rules in the text grammar.
    } else { 
      final LineReader reader = new LineReader(grammarPath);
      while (reader.hasNext()) {
        final String line = reader.next();
        int lhsStart = line.indexOf("[") + 1;
        int lhsEnd = line.indexOf("]");
        if (lhsStart < 1 || lhsEnd < 0) {
          LOG.info("malformed rule: {}\n", line);
          continue;
        }
        final String lhs = line.substring(lhsStart, lhsEnd);
        nonTerminalSymbols.add(lhs);
      }
    }
    
    LOG.info("{} nonTerminal symbols read: {}", nonTerminalSymbols.size(),
        nonTerminalSymbols.toString());

    // write glue rules to stdout
    
    System.out.println(String.format(R_START, goalSymbol));
    
    for (String nt : nonTerminalSymbols)
      System.out.println(String.format(R_TWO, goalSymbol, nt));
    
    System.out.println(String.format(R_END, goalSymbol));
    
    for (String nt : nonTerminalSymbols)
      System.out.println(String.format(R_TOP, goalSymbol, nt));

  }
  
  public static void main(String[] args) throws IOException {
    final CreateGlueGrammar glueCreator = new CreateGlueGrammar();
    final CmdLineParser parser = new CmdLineParser(glueCreator);

    try {
      parser.parseArgument(args);
      glueCreator.run();
    } catch (CmdLineException e) {
      LOG.error(e.getMessage(), e);
      parser.printUsage(System.err);
      System.exit(1);
    }
   }
}
