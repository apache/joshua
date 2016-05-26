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
package org.apache.joshua.decoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.joshua.decoder.chart_parser.Chart;
import org.apache.joshua.decoder.ff.FeatureFunction;
import org.apache.joshua.decoder.ff.FeatureVector;
import org.apache.joshua.decoder.ff.SourceDependentFF;
import org.apache.joshua.decoder.ff.tm.Grammar;
import org.apache.joshua.decoder.hypergraph.ForestWalker;
import org.apache.joshua.decoder.hypergraph.GrammarBuilderWalkerFunction;
import org.apache.joshua.decoder.hypergraph.HyperGraph;
import org.apache.joshua.decoder.phrase.Stacks;
import org.apache.joshua.decoder.segment_file.Sentence;
import org.apache.joshua.corpus.Vocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles decoding of individual Sentence objects (which can represent plain sentences
 * or lattices). A single sentence can be decoded by a call to translate() and, if an InputHandler
 * is used, many sentences can be decoded in a thread-safe manner via a single call to
 * translateAll(), which continually queries the InputHandler for sentences until they have all been
 * consumed and translated.
 * 
 * The DecoderFactory class is responsible for launching the threads.
 * 
 * @author Matt Post post@cs.jhu.edu
 * @author Zhifei Li, zhifei.work@gmail.com
 */

public class DecoderThread extends Thread {
  private static final Logger LOG = LoggerFactory.getLogger(DecoderThread.class);

  private final JoshuaConfiguration joshuaConfiguration;
  /*
   * these variables may be the same across all threads (e.g., just copy from DecoderFactory), or
   * differ from thread to thread
   */
  private final List<Grammar> allGrammars;
  private final List<FeatureFunction> featureFunctions;


  // ===============================================================
  // Constructor
  // ===============================================================
  public DecoderThread(List<Grammar> grammars, FeatureVector weights,
      List<FeatureFunction> featureFunctions, JoshuaConfiguration joshuaConfiguration) throws IOException {

    this.joshuaConfiguration = joshuaConfiguration;
    this.allGrammars = grammars;

    this.featureFunctions = new ArrayList<FeatureFunction>();
    for (FeatureFunction ff : featureFunctions) {
      if (ff instanceof SourceDependentFF) {
        this.featureFunctions.add(((SourceDependentFF) ff).clone());
      } else {
        this.featureFunctions.add(ff);
      }
    }
  }

  // ===============================================================
  // Methods
  // ===============================================================

  @Override
  public void run() {
    // Nothing to do but wait.
  }

  /**
   * Translate a sentence.
   * 
   * @param sentence The sentence to be translated.
   * @return the sentence {@link org.apache.joshua.decoder.Translation}
   */
  public Translation translate(Sentence sentence) {

    LOG.info("Input {}: {}", sentence.id(), sentence.fullSource());

    if (sentence.target() != null)
      LOG.info("Input {}: Constraining to target sentence '{}'",
          sentence.id(), sentence.target());

    // skip blank sentences
    if (sentence.isEmpty()) {
      LOG.info("Translation {}: Translation took 0 seconds", sentence.id());
      return new Translation(sentence, null, featureFunctions, joshuaConfiguration);
    }

    long startTime = System.currentTimeMillis();

    int numGrammars = allGrammars.size();
    Grammar[] grammars = new Grammar[numGrammars];

    for (int i = 0; i < allGrammars.size(); i++)
      grammars[i] = allGrammars.get(i);

    if (joshuaConfiguration.segment_oovs)
      sentence.segmentOOVs(grammars);

    /**
     * Joshua supports (as of September 2014) both phrase-based and hierarchical decoding. Here
     * we build the appropriate chart. The output of both systems is a hypergraph, which is then
     * used for further processing (e.g., k-best extraction).
     */
    HyperGraph hypergraph = null;
    try {

      if (joshuaConfiguration.search_algorithm.equals("stack")) {
        Stacks stacks = new Stacks(sentence, this.featureFunctions, grammars, joshuaConfiguration);

        hypergraph = stacks.search();
      } else {
        /* Seeding: the chart only sees the grammars, not the factories */
        Chart chart = new Chart(sentence, this.featureFunctions, grammars,
            joshuaConfiguration.goal_symbol, joshuaConfiguration);

        hypergraph = (joshuaConfiguration.use_dot_chart) 
            ? chart.expand() 
                : chart.expandSansDotChart();
      }

    } catch (java.lang.OutOfMemoryError e) {
      LOG.error("Input {}: out of memory", sentence.id());
      hypergraph = null;
    }

    float seconds = (System.currentTimeMillis() - startTime) / 1000.0f;
    LOG.info("Input {}: Translation took {} seconds", sentence.id(), seconds);
    LOG.info("Input {}: Memory used is {} MB", sentence.id(), (Runtime
        .getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000.0);

    /* Return the translation unless we're doing synchronous parsing. */
    if (!joshuaConfiguration.parse || hypergraph == null) {
      return new Translation(sentence, hypergraph, featureFunctions, joshuaConfiguration);
    }

    /*****************************************************************************************/

    /*
     * Synchronous parsing.
     * 
     * Step 1. Traverse the hypergraph to create a grammar for the second-pass parse.
     */
    Grammar newGrammar = getGrammarFromHyperGraph(joshuaConfiguration.goal_symbol, hypergraph);
    newGrammar.sortGrammar(this.featureFunctions);
    long sortTime = System.currentTimeMillis();
    LOG.info("Sentence {}: New grammar has {} rules.", sentence.id(),
        newGrammar.getNumRules());

    /* Step 2. Create a new chart and parse with the instantiated grammar. */
    Grammar[] newGrammarArray = new Grammar[] { newGrammar };
    Sentence targetSentence = new Sentence(sentence.target(), sentence.id(), joshuaConfiguration);
    Chart chart = new Chart(targetSentence, featureFunctions, newGrammarArray, "GOAL",joshuaConfiguration);
    int goalSymbol = GrammarBuilderWalkerFunction.goalSymbol(hypergraph);
    String goalSymbolString = Vocabulary.word(goalSymbol);
    LOG.info("Sentence {}: goal symbol is {} ({}).", sentence.id(),
        goalSymbolString, goalSymbol);
    chart.setGoalSymbolID(goalSymbol);

    /* Parsing */
    HyperGraph englishParse = chart.expand();
    long secondParseTime = System.currentTimeMillis();
    LOG.info("Sentence {}: Finished second chart expansion ({} seconds).",
        sentence.id(), (secondParseTime - sortTime) / 1000);
    LOG.info("Sentence {} total time: {} seconds.\n", sentence.id(),
        (secondParseTime - startTime) / 1000);
    LOG.info("Memory used after sentence {} is {} MB", sentence.id(), (Runtime
        .getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000.0);
    return new Translation(sentence, englishParse, featureFunctions, joshuaConfiguration); // or do something else
  }

  private Grammar getGrammarFromHyperGraph(String goal, HyperGraph hg) {
    GrammarBuilderWalkerFunction f = new GrammarBuilderWalkerFunction(goal,joshuaConfiguration);
    ForestWalker walker = new ForestWalker();
    walker.walk(hg.goalNode, f);
    return f.getGrammar();
  }
}
