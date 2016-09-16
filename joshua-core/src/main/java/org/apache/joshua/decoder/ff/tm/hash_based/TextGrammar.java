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
package org.apache.joshua.decoder.ff.tm.hash_based;

import static org.apache.joshua.util.FormatUtils.ensureNonTerminalBrackets;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.decoder.ff.FeatureFunction;
import org.apache.joshua.decoder.ff.FeatureVector;
import org.apache.joshua.decoder.ff.tm.AbstractGrammar;
import org.apache.joshua.decoder.ff.tm.Rule;
import org.apache.joshua.decoder.ff.tm.Trie;
import org.apache.joshua.decoder.ff.tm.format.HieroFormatReader;
import org.apache.joshua.util.FormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.typesafe.config.Config;

/**
 * This class implements a memory-based bilingual BatchGrammar.
 * <p>
 * The rules are stored in a trie. Each trie node has: (1) RuleBin: a list of rules matching the
 * french sides so far (2) A HashMap of next-layer trie nodes, the next french word used as the key
 * in HashMap
 * 
 * @author Zhifei Li zhifei.work@gmail.com
 * @author Matt Post post@cs.jhu.edu
 */
public class TextGrammar extends AbstractGrammar {

  private static final Logger LOG = LoggerFactory.getLogger(TextGrammar.class);

  /* The number of rules read. */
  private int qtyRulesRead = 0;

  /* The number of distinct source sides. */
  private int qtyRuleBins = 0;

  /* The trie root. */
  private final MemoryBasedTrie root = new MemoryBasedTrie();

  /* The path containing the grammar. */
  private final Optional<String> path;

  public TextGrammar(final Config config) {
    super(config);
    this.path = config.hasPath("path") ? Optional.of(config.getString("path")) : Optional.empty();
    
    // if path is configured, actually load the grammar
    if (this.path.isPresent()) {
      this.loadGrammar(this.path.get());
      this.printGrammar();
    }
  }
  
  private void loadGrammar(final String path) {
    try(final HieroFormatReader reader = new HieroFormatReader(path, getOwner());) {
      for (Rule rule : reader) {
        if (rule != null) {
          addRule(rule);
        }
      }
    } catch (IOException e) {
      Throwables.propagate(e);
    }
  }

  @Override
  public int getNumRules() {
    return this.qtyRulesRead;
  }

  /**
   * if the span covered by the chart bin is greater than the limit, then return false
   */
  public boolean hasRuleForSpan(int i, int j, int pathLength) {
    if (this.spanLimit == -1) { // mono-glue grammar
      return (i == 0);
    } else {
      // System.err.println(String.format("%s HASRULEFORSPAN(%d,%d,%d)/%d = %s",
      // Vocabulary.word(this.owner), i, j, pathLength, spanLimit, pathLength <= this.spanLimit));
      return (pathLength <= this.spanLimit);
    }
  }

  public Trie getTrieRoot() {
    return this.root;
  }

  /**
   * Adds a rule to the grammar.
   */
  public void addRule(Rule rule) {

    this.qtyRulesRead++;

    // === identify the position, and insert the trie nodes as necessary
    MemoryBasedTrie pos = root;
    int[] french = rule.getSource();

    maxSourcePhraseLength = Math.max(maxSourcePhraseLength, french.length);

    for (int curSymID : french) {
      /*
       * Note that the nonTerminal symbol in the french is not cleaned (i.e., will be sth like
       * [X,1]), but the symbol in the Trie has to be cleaned, so that the match does not care about
       * the markup (i.e., [X,1] or [X,2] means the same thing, that is X) if
       * (Vocabulary.nt(french[k])) { curSymID = modelReader.cleanNonTerminal(french[k]); if
       * (logger.isLoggable(Level.FINEST)) logger.finest("Amended to: " + curSymID); }
       */

      MemoryBasedTrie nextLayer = (MemoryBasedTrie) pos.match(curSymID);
      if (null == nextLayer) {
        nextLayer = new MemoryBasedTrie();
        if (pos.hasExtensions() == false) {
          pos.childrenTbl = new HashMap<>();
        }
        pos.childrenTbl.put(curSymID, nextLayer);
      }
      pos = nextLayer;
    }

    // === add the rule into the trie node
    if (!pos.hasRules()) {
      pos.ruleBin = new MemoryBasedRuleBin(rule.getArity(), rule.getSource());
      this.qtyRuleBins++;
    }
    pos.ruleBin.addRule(rule);
  }

  protected void printGrammar() {
    LOG.info("{}: Read {} rules with {} distinct source sides from '{}'",
        this.getClass().getName(), this.qtyRulesRead, this.qtyRuleBins, path);
  }

  /***
   * Takes an input word and creates an OOV rule in the current grammar for that word.
   * 
   * @param sourceWord integer representation of word
   * @param featureFunctions {@link java.util.List} of {@link org.apache.joshua.decoder.ff.FeatureFunction}'s
   */
  @Override
  public void addOOVRules(int sourceWord, Config sentenceFlags, List<FeatureFunction> featureFunctions) {

    // TODO: _OOV shouldn't be outright added, since the word might not be OOV for the LM (but now
    // almost
    // certainly is)
    final int targetWord = sentenceFlags.getBoolean("mark_oovs") ? Vocabulary.id(Vocabulary
        .word(sourceWord) + "_OOV") : sourceWord;
    final int lhs = Vocabulary.id(ensureNonTerminalBrackets(sentenceFlags.getString("default_non_terminal")));

    final int[] sourceWords = { sourceWord };
    final int[] targetWords = { targetWord };
    final byte[] alignment = { 0, 0 };
    final FeatureVector features = new FeatureVector(0);

    final Rule oovRule = new Rule(
          lhs,
          sourceWords,
          targetWords,
          0,
          features,
          alignment,
          getOwner());
    addRule(oovRule);
    oovRule.estimateRuleCost(featureFunctions);
  }

  /**
   * Adds a default set of glue rules.
   * 
   * @param featureFunctions an {@link java.util.ArrayList} of {@link org.apache.joshua.decoder.ff.FeatureFunction}'s
   */
  public void addGlueRules(List<FeatureFunction> featureFunctions, Config config) {
    String goalNT = FormatUtils.cleanNonTerminal(config.getString("goal_symbol"));
    String defaultNT = FormatUtils.cleanNonTerminal(config.getString("default_non_terminal"));

    String[] ruleStrings = new String[] {
        String.format("[%s] ||| %s ||| %s ||| 0", goalNT, Vocabulary.START_SYM,
            Vocabulary.START_SYM),
        String.format("[%s] ||| [%s,1] [%s,2] ||| [%s,1] [%s,2] ||| -1", goalNT, goalNT, defaultNT,
            goalNT, defaultNT),
        String.format("[%s] ||| [%s,1] %s ||| [%s,1] %s ||| 0", goalNT, goalNT,
            Vocabulary.STOP_SYM, goalNT, Vocabulary.STOP_SYM) };

    try(final HieroFormatReader reader = new HieroFormatReader(getOwner());) {
      for (String ruleString : ruleStrings) {
        Rule rule = reader.parseLine(ruleString);
        addRule(rule);
        rule.estimateRuleCost(featureFunctions);
      }
    }
  }

}
