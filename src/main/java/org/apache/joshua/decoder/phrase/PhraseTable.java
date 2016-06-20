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

import static org.apache.joshua.decoder.ff.tm.OwnerMap.UNKNOWN_OWNER;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.decoder.ff.FeatureFunction;
import org.apache.joshua.decoder.ff.tm.Grammar;
import org.apache.joshua.decoder.ff.tm.OwnerId;
import org.apache.joshua.decoder.ff.tm.Rule;
import org.apache.joshua.decoder.ff.tm.RuleCollection;
import org.apache.joshua.decoder.ff.tm.Trie;
import org.apache.joshua.decoder.ff.tm.hash_based.MemoryBasedBatchGrammar;
import org.apache.joshua.decoder.ff.tm.packed.PackedGrammar;

/**
 * Represents a phrase table, and is implemented as a wrapper around either a {@link PackedGrammar}
 * or a {@link MemoryBasedBatchGrammar}.
 * 
 * TODO: this should all be implemented as a two-level trie (source trie and target trie).
 */
public class PhraseTable implements Grammar {
  
  private JoshuaConfiguration config;
  private Grammar backend;
  
  /**
   * Chain to the super with a number of defaults. For example, we only use a single nonterminal,
   * and there is no span limit.
   * 
   * @param grammarFile file path parent directory
   * @param owner used to set phrase owners
   * @param type the grammar specification keyword (e.g., "thrax" or "moses")
   * @param config a populated {@link org.apache.joshua.decoder.JoshuaConfiguration}
   * @throws IOException if there is an error reading the grammar file
   */
  public PhraseTable(String grammarFile, String owner, String type, JoshuaConfiguration config) 
      throws IOException {
    this.config = config;
    int spanLimit = 0;
    
    if (grammarFile != null && new File(grammarFile).isDirectory()) {
      this.backend = new PackedGrammar(grammarFile, spanLimit, owner, type, config);
      if (this.backend.getMaxSourcePhraseLength() == -1) {
        String msg = "FATAL: Using a packed grammar for a phrase table backend requires that you "
            + "packed the grammar with Joshua 6.0.2 or greater";
        throw new RuntimeException(msg);
      }

    } else {
      this.backend = new MemoryBasedBatchGrammar(type, grammarFile, owner, "[X]", spanLimit, config);
    }
  }
  
  public PhraseTable(String owner, JoshuaConfiguration config) {
    this.config = config;
    this.backend = new MemoryBasedBatchGrammar(owner, config, 20);
  }
      
  /**
   * Returns the longest source phrase read. Because phrases have a dummy nonterminal prepended to
   * them, we need to subtract 1.
   * 
   * @return the longest source phrase read.
   */
  @Override
  public int getMaxSourcePhraseLength() {
    return this.backend.getMaxSourcePhraseLength() - 1;
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
      pointer = pointer.match(Vocabulary.id("[X]"));
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
    ((MemoryBasedBatchGrammar)backend).addRule(rule);
  }
  
  @Override
  public void addOOVRules(int sourceWord, List<FeatureFunction> featureFunctions) {
    // TODO: _OOV shouldn't be outright added, since the word might not be OOV for the LM (but now almost
    // certainly is)
    int targetWord = config.mark_oovs
        ? Vocabulary.id(Vocabulary.word(sourceWord) + "_OOV")
        : sourceWord;   

    int nt_i = Vocabulary.id("[X]");
    Rule oovRule = new Rule(nt_i, new int[] { nt_i, sourceWord },
        new int[] { -1, targetWord }, "", 1, UNKNOWN_OWNER);
    addRule(oovRule);
    oovRule.estimateRuleCost(featureFunctions);
        
//    String ruleString = String.format("[X] ||| [X,1] %s ||| [X,1] %s", 
//        Vocabulary.word(sourceWord), Vocabulary.word(targetWord));
//    Rule oovRule = new HieroFormatReader().parseLine(ruleString);
//    oovRule.setOwner(Vocabulary.id("oov"));
//    addRule(oovRule);
//    oovRule.estimateRuleCost(featureFunctions);
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

  @Override
  public int getNumDenseFeatures() {
    return backend.getNumDenseFeatures();
  }
}
