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
package org.apache.joshua.decoder.ff.lm;

import static org.apache.joshua.util.FormatUtils.isNonterminal;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.decoder.chart_parser.SourcePath;
import org.apache.joshua.decoder.ff.FeatureVector;
import org.apache.joshua.decoder.ff.lm.KenLM.StateProbPair;
import org.apache.joshua.decoder.ff.state_maintenance.DPState;
import org.apache.joshua.decoder.ff.state_maintenance.KenLMState;
import org.apache.joshua.decoder.ff.tm.Rule;
import org.apache.joshua.decoder.hypergraph.HGNode;
import org.apache.joshua.decoder.segment_file.Sentence;

/**
 * Wrapper for KenLM LMs with left-state minimization. We inherit from the regular
 *
 * @author Matt Post post@cs.jhu.edu
 * @author Juri Ganitkevitch juri@cs.jhu.edu
 */
public class StateMinimizingLanguageModel extends LanguageModelFF {

  // maps from sentence numbers to KenLM-side pools used to allocate state
  private static final ConcurrentHashMap<Integer, Long> poolMap = new ConcurrentHashMap<Integer, Long>();

  public StateMinimizingLanguageModel(FeatureVector weights, String[] args, JoshuaConfiguration config) {
    super(weights, args, config);
    this.type = "kenlm";
    if (parsedArgs.containsKey("lm_type") && ! parsedArgs.get("lm_type").equals("kenlm")) {
      String msg = "* FATAL: StateMinimizingLanguageModel only supports 'kenlm' lm_type backend"
          + "*        Remove lm_type from line or set to 'kenlm'";
      throw new RuntimeException(msg);
    }
  }

  /**
   * Initializes the underlying language model.
   */
  @Override
  public void initializeLM() {

    // Override type (only KenLM supports left-state minimization)
    this.languageModel = new KenLM(ngramOrder, path);

    Vocabulary.registerLanguageModel(this.languageModel);
    Vocabulary.id(config.default_non_terminal);

  }

  /**
   * Estimates the cost of a rule. We override here since KenLM can do it more
   * efficiently than the default {@link LanguageModelFF} class.
   */
  @Override
  public float estimateCost(Rule rule, Sentence sentence) {

    int[] ruleWords = getRuleIds(rule);

    // map to ken lm ids
    final long[] words = mapToKenLmIds(ruleWords, null, true);

    // Get the probability of applying the rule and the new state
    return weight * ((KenLM) languageModel).estimateRule(words);
  }

  /**
   * Computes the features incurred along this edge. Note that these features are unweighted costs
   * of the feature; they are the feature cost, not the model cost, or the inner product of them.
   */
  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      Sentence sentence, Accumulator acc) {

    if (rule == null ) {
      return null;
    }

    int[] ruleWords;
    if (config.source_annotations) {
      // get source side annotations and project them to the target side
      ruleWords = getTags(rule, i, j, sentence);
    } else {
      ruleWords = getRuleIds(rule);
    }

     // map to ken lm ids
    final long[] words = mapToKenLmIds(ruleWords, tailNodes, false);

    final int sentID = sentence.id();
    // Since sentId is unique across threads, next operations are safe, but not atomic!
    if (!poolMap.containsKey(sentID)) {
      poolMap.put(sentID, KenLM.createPool());
    }

    // Get the probability of applying the rule and the new state
    final StateProbPair pair = ((KenLM) languageModel).probRule(words, poolMap.get(sentID));

    // Record the prob
    acc.add(denseFeatureIndex, pair.prob);

    // Return the state
    return pair.state;
  }

  /**
   * Maps given array of word/class ids to KenLM ids. For estimating cost and computing,
   * state retrieval differs slightly.
   */
  private long[] mapToKenLmIds(int[] ids, List<HGNode> tailNodes, boolean isOnlyEstimate) {
    // The IDs we will to KenLM
    long[] kenIds = new long[ids.length];
    for (int x = 0; x < ids.length; x++) {
      int id = ids[x];

      if (isNonterminal(id)) {

        if (isOnlyEstimate) {
          // For the estimate, we can just mark negative values
          kenIds[x] = -1;
        } else {
          // Nonterminal: retrieve the KenLM long that records the state
          int index = -(id + 1);
          final KenLMState state = (KenLMState) tailNodes.get(index).getDPState(stateIndex);
          kenIds[x] = -state.getState();
        }

      } else {
        // Terminal: just add it
        kenIds[x] = id;
      }
    }
    return kenIds;
  }

  /**
   * Destroys the pool created to allocate state for this sentence. Called from the
   * {@link org.apache.joshua.decoder.Translation} class after outputting the sentence or k-best list. Hosting
   * this map here in KenLMFF statically allows pools to be shared across KenLM instances.
   *
   * @param sentId a key in the poolmap table to destroy
   */
  public void destroyPool(int sentId) {
    if (poolMap.containsKey(sentId))
      KenLM.destroyPool(poolMap.get(sentId));
    poolMap.remove(sentId);
  }

  /**
   * This function differs from regular transitions because we incorporate the cost of incomplete
   * left-hand ngrams, as well as including the start- and end-of-sentence markers (if they were
   * requested when the object was created).
   *
   * KenLM already includes the prefix probabilities (of shorter n-grams on the left-hand side), so
   * there's nothing that needs to be done.
   */
  @Override
  public DPState computeFinal(HGNode tailNode, int i, int j, SourcePath sourcePath, Sentence sentence,
      Accumulator acc) {
    // The state is the same since no rule was applied
    return new KenLMState();
  }

  /**
   * KenLM probs already include the prefix probabilities (they are substracted out when merging
   * states), so this doesn't need to do anything.
   */
  @Override
  public float estimateFutureCost(Rule rule, DPState currentState, Sentence sentence) {
    return 0.0f;
  }
}
