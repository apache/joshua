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

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.decoder.chart_parser.Chart;
import org.apache.joshua.decoder.ff.tm.Grammar;
import org.apache.joshua.decoder.hypergraph.ForestWalker;
import org.apache.joshua.decoder.hypergraph.GrammarBuilderWalkerFunction;
import org.apache.joshua.decoder.hypergraph.HyperGraph;
import org.apache.joshua.decoder.phrase.Stacks;
import org.apache.joshua.decoder.segment_file.Sentence;
import org.apache.joshua.util.FormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * This class handles decoding of individual Sentence objects (which can represent plain sentences
 * or lattices). A single sentence can be decoded by a call to translate() and, if an InputHandler
 * is used, many sentences can be decoded in a thread-safe manner via a single call to
 * translateAll(), which continually queries the InputHandler for sentences until they have all been
 * consumed and translated.
 * 
 * @author Matt Post post@cs.jhu.edu
 * @author Zhifei Li, zhifei.work@gmail.com
 * @author Felix Hieber, felix.hieber@gmail.com
 */
public class DecoderTask {
  
  private static final Logger LOG = LoggerFactory.getLogger(DecoderTask.class);
  
  /** sentence-specific DecoderConfig,
   * mostly shared with the global decoderConfig, but can have adaptations
   */
  private final DecoderConfig sentenceConfig;
  private final Sentence sentence;
  private final boolean segmentOovs;
  private final boolean useDotChart;
  private final boolean doParsing;
  private final String goalSymbol;

  public DecoderTask(final DecoderConfig sentenceConfig, final Sentence sentence) {
    this.sentenceConfig = sentenceConfig;
    this.sentence = sentence;
    this.segmentOovs = sentence.getFlags().getBoolean("segment_oovs");
    this.useDotChart = sentence.getFlags().getBoolean("use_dot_chart");
    this.doParsing = sentence.getFlags().getBoolean("parse");
    this.goalSymbol = FormatUtils.ensureNonTerminalBrackets(sentence.getFlags().getString("goal_symbol"));
  }

  /**
   * Translate the sentence.
   * @return translation of the sentence {@link org.apache.joshua.decoder.Translation}
   */
  public Translation translate() {

    LOG.info("Input {}: {}", sentence.id(), sentence.fullSource());

    if (sentence.target() != null) {
      LOG.info("Input {}: Constraining to target sentence '{}'",
          sentence.id(), sentence.target());
    }

    // skip blank sentences
    if (sentence.isEmpty()) {
      LOG.info("Translation {}: Translation took 0 seconds", sentence.id());
      return new Translation(sentence, null, sentenceConfig);
    }

    long startTime = System.currentTimeMillis();

    // TODO(fhieber): this should be done in the constructor maybe?
    // But it should not modify the sentence object.
    if (segmentOovs) {
      sentence.segmentOOVs(sentenceConfig.getGrammars());
    }

    /*
     * Joshua supports (as of September 2014) both phrase-based and hierarchical decoding. Here
     * we build the appropriate chart. The output of both systems is a hypergraph, which is then
     * used for further processing (e.g., k-best extraction).
     */
    final HyperGraph hypergraph = createHypergraph();

    float decodingTime = (System.currentTimeMillis() - startTime) / 1000.0f;
    float usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000.0f;
    LOG.info("Input {}: Translation took {} seconds", sentence.id(), decodingTime);
    LOG.info("Input {}: Memory used is {} MB", sentence.id(), usedMemory);

    /* Return the translation unless we're doing synchronous parsing. */
    if (!doParsing || hypergraph == null) {
      return new Translation(sentence, hypergraph, sentenceConfig);
    } else {
      return parse(hypergraph);
    }
  }
  
  private HyperGraph createHypergraph() {
    try {
      switch (sentenceConfig.getSearchAlgorithm()) {
      case stack:
        final Stacks stacks = new Stacks(sentence, sentenceConfig);
        return stacks.search();
      case cky:
        final Chart chart = new Chart(sentence, sentenceConfig);
        return useDotChart ? chart.expand() : chart.expandSansDotChart();
      default:
        return null;
      }
    } catch (java.lang.OutOfMemoryError e) {
      return null;
    }
  }
  
  /**
   * Synchronous parsing.
   */
  private Translation parse(final HyperGraph hypergraph) {
    long startTime = System.currentTimeMillis();
    // Step 1. Traverse the hypergraph to create a grammar for the second-pass parse.
    final Grammar newGrammar = getGrammarFromHyperGraph(goalSymbol, hypergraph);
    newGrammar.sortGrammar(sentenceConfig.getFeatureFunctions());
    long sortTime = System.currentTimeMillis();
    LOG.info("Sentence {}: New grammar has {} rules.", sentence.id(),
        newGrammar.getNumRules());

    /* Step 2. Create a new chart and parse with the instantiated grammar. */
    final Sentence targetSentence = new Sentence(sentence.target(), sentence.id(), sentence.getFlags());
    final Chart chart = new Chart(targetSentence, sentenceConfig);
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
    return new Translation(sentence, englishParse, sentenceConfig); // or do something else
  }

  private static Grammar getGrammarFromHyperGraph(String goal, HyperGraph hg) {
    final Config grammarConfig = ConfigFactory.parseMap(
        ImmutableMap.of("owner", "pt", "span_limit", "1000"), "");
    GrammarBuilderWalkerFunction f = new GrammarBuilderWalkerFunction(
        goal, grammarConfig);
    ForestWalker walker = new ForestWalker();
    walker.walk(hg.goalNode, f);
    return f.getGrammar();
  }
  
}
