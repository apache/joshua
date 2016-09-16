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
package org.apache.joshua.decoder.phrase;

import static org.apache.joshua.util.Constants.defaultNT;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.decoder.ff.FeatureFunction;
import org.apache.joshua.decoder.ff.FeatureVector;
import org.apache.joshua.decoder.ff.tm.Grammar;
import org.apache.joshua.decoder.ff.tm.OwnerId;
import org.apache.joshua.decoder.ff.tm.Rule;
import org.apache.joshua.decoder.ff.tm.RuleCollection;
import org.apache.joshua.decoder.ff.tm.Trie;
import org.apache.joshua.decoder.ff.tm.hash_based.TextGrammar;
import org.apache.joshua.decoder.ff.tm.packed.PackedGrammar;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;

/**
 * Represents a phrase table, and is implemented as a wrapper around either a {@link PackedGrammar}
 * or a {@link TextGrammar}.
 * 
 * TODO: this should all be implemented as a two-level trie (source trie and target trie).
 */
public class PhraseTable implements Grammar {
  
  private final Grammar backend;
  private final Optional<String> path;
  
  /**
   * Chain to the super with a number of defaults. For example, we only use a single nonterminal,
   * and there is no span limit.
   */
  public PhraseTable(Config config) {
    // override span_limit to 0
    final Config newConfig = config.withValue("span_limit", ConfigValueFactory.fromAnyRef(0));
    this.path = newConfig.hasPath("path") ? Optional.of(newConfig.getString("path")) : Optional.empty();
    if (path.isPresent() && new File(path.get()).isDirectory()) {
      this.backend = new PackedGrammar(newConfig);
    } else {
      this.backend = new TextGrammar(newConfig);
    }
  }
      
  /**
   * Returns the longest source phrase read.
   * 
   * @return the longest source phrase read.
   */
  @Override
  public int getMaxSourcePhraseLength() {
    return this.backend.getMaxSourcePhraseLength();
  }

  /**
   * Collect the set of target-side phrases associated with a source phrase.
   * 
   * @param sourceWords the sequence of source words
   * @return the rules
   */
  public RuleCollection getPhrases(int[] sourceWords) {
    if (sourceWords.length != 0) {
      Trie pointer = getTrieRoot();
      int i = 0;
      while (pointer != null && i < sourceWords.length)
        pointer = pointer.match(sourceWords[i++]);

      if (pointer != null && pointer.hasRules()) {
        return pointer.getRuleCollection();
      }
    }

    return null;
  }

  /**
   * Adds a rule to the grammar. Only supported when the backend is a MemoryBasedBatchGrammar.
   * 
   * @param rule the rule to add
   */
  public void addRule(Rule rule) {
    backend.addRule(rule);
  }
  
  @Override
  public void addOOVRules(int sourceWord, Config sentenceFlags, List<FeatureFunction> featureFunctions) {
    // TODO: _OOV shouldn't be outright added, since the word might not be OOV for the LM (but now almost
    // certainly is)
    int targetWord = sentenceFlags.getBoolean("mark_oovs")
        ? Vocabulary.id(Vocabulary.word(sourceWord) + "_OOV")
        : sourceWord;   

    int nt_i = Vocabulary.id(defaultNT);
    Rule oovRule = new Rule(
        nt_i,
        new int[] { nt_i, sourceWord },
        new int[] { -1, targetWord },
        1,
        new FeatureVector(0),
        new byte[] {0,0}, backend.getOwner());
    addRule(oovRule);
    oovRule.estimateRuleCost(featureFunctions);
  }

  @Override
  public Trie getTrieRoot() {
    return backend.getTrieRoot();
  }

  @Override
  public void sortGrammar(List<FeatureFunction> models) {
    backend.sortGrammar(models);    
  }

  @Override
  public boolean isSorted() {
    return backend.isSorted();
  }

  /**
   * This should never be called. 
   */
  @Override
  public boolean hasRuleForSpan(int startIndex, int endIndex, int pathLength) {
    return true;
  }

  @Override
  public int getNumRules() {
    return backend.getNumRules();
  }

  @Override
  public OwnerId getOwner() {
    return backend.getOwner();
  }
}
