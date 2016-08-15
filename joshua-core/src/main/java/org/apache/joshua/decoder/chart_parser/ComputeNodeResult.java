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
package org.apache.joshua.decoder.chart_parser;

import java.util.ArrayList;

import java.util.List;

import org.apache.joshua.decoder.Decoder;
import org.apache.joshua.decoder.ff.StatefulFF;
import org.apache.joshua.decoder.ff.FeatureFunction;
import org.apache.joshua.decoder.ff.FeatureVector;
import org.apache.joshua.decoder.ff.state_maintenance.DPState;
import org.apache.joshua.decoder.ff.tm.Rule;
import org.apache.joshua.decoder.hypergraph.HGNode;
import org.apache.joshua.decoder.hypergraph.HyperEdge;
import org.apache.joshua.decoder.segment_file.Sentence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class computes the cost of applying a rule.
 * 
 * @author Matt Post post@cs.jhu.edu
 * @author Zhifei Li, zhifei.work@gmail.com
 */

public class ComputeNodeResult {

  private static final Logger LOG = LoggerFactory.getLogger(ComputeNodeResult.class);

  // The cost incurred by the rule itself (and all associated feature functions)
  private float transitionCost;

  // transitionCost + the Viterbi costs of the tail nodes.
  private float viterbiCost;

  // viterbiCost + a future estimate (outside cost estimate).
  private float pruningCostEstimate;

  // The StateComputer objects themselves serve as keys.
  private List<DPState> dpStates;

  /**
   * Computes the new state(s) that are produced when applying the given rule to the list of tail
   * nodes. Also computes a range of costs of doing so (the transition cost, the total (Viterbi)
   * cost, and a score that includes a future cost estimate).
   * 
   * Old version that doesn't use the derivation state.
   * @param featureFunctions {@link java.util.List} of {@link org.apache.joshua.decoder.ff.FeatureFunction}'s
   * @param rule {@link org.apache.joshua.decoder.ff.tm.Rule} to use when computing th node result
   * @param tailNodes {@link java.util.List} of {@link org.apache.joshua.decoder.hypergraph.HGNode}'s
   * @param i todo
   * @param j todo
   * @param sourcePath information about a path taken through the source lattice
   * @param sentence the lattice input
   */
  public ComputeNodeResult(List<FeatureFunction> featureFunctions, Rule rule, List<HGNode> tailNodes,
      int i, int j, SourcePath sourcePath, Sentence sentence) {

    // The total Viterbi cost of this edge. This is the Viterbi cost of the tail nodes, plus
    // whatever costs we incur applying this rule to create a new hyperedge.
    float viterbiCost = 0.0f;
    
    if (LOG.isDebugEnabled()) {
      LOG.debug("ComputeNodeResult():");
      LOG.debug("-> RULE {}", rule);
    }

    /*
     * Here we sum the accumulated cost of each of the tail nodes. The total cost of the new
     * hyperedge (the inside or Viterbi cost) is the sum of these nodes plus the cost of the
     * transition. Note that this could and should all be generalized to whatever semiring is being
     * used.
     */
    if (null != tailNodes) {
      for (HGNode item : tailNodes) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("-> item.bestedge: {}", item);
          LOG.debug("-> TAIL NODE {}", item);
        }
        viterbiCost += item.bestHyperedge.getBestDerivationScore();
      }
    }

    List<DPState> allDPStates = new ArrayList<DPState>();

    // The transition cost is the new cost incurred by applying this rule
    float transitionCost = 0.0f;

    // The future cost estimate is a heuristic estimate of the outside cost of this edge.
    float futureCostEstimate = 0.0f;

    /*
     * We now iterate over all the feature functions, computing their cost and their expected future
     * cost.
     */
    for (FeatureFunction feature : featureFunctions) {
      FeatureFunction.ScoreAccumulator acc = feature.new ScoreAccumulator(); 

      DPState newState = feature.compute(rule, tailNodes, i, j, sourcePath, sentence, acc);
      transitionCost += acc.getScore();


      if (LOG.isDebugEnabled()) {
        LOG.debug("FEATURE {} = {} * {} = {}", feature.getName(),
            acc.getScore() / Decoder.weights.getSparse(feature.getName()),
            Decoder.weights.getSparse(feature.getName()), acc.getScore());
      }

      if (feature.isStateful()) {
        futureCostEstimate += feature.estimateFutureCost(rule, newState, sentence);
        allDPStates.add(((StatefulFF)feature).getStateIndex(), newState);
      }
    }
    viterbiCost += transitionCost;
    if (LOG.isDebugEnabled())
      LOG.debug("-> COST = {}", transitionCost);
    // Set the final results.
    this.pruningCostEstimate = viterbiCost + futureCostEstimate;
    this.viterbiCost = viterbiCost;
    this.transitionCost = transitionCost;
    this.dpStates = allDPStates;
  }

  /**
   * This is called from {@link org.apache.joshua.decoder.chart_parser.Cell} 
   * when making the final transition to the goal state.
   * This is done to allow feature functions to correct for partial estimates, since
   * they now have the knowledge that the whole sentence is complete. Basically, this
   * is only used by LanguageModelFF, which does not score partial n-grams, and therefore
   * needs to correct for this when a short sentence ends. KenLMFF corrects for this by
   * always scoring partial hypotheses, and subtracting off the partial score when longer
   * context is available. This would be good to do for the LanguageModelFF feature function,
   * too: it makes search better (more accurate at the beginning, for example), and would
   * also do away with the need for the computeFinal* class of functions (and hooks in
   * the feature function interface).
   * 
   * @param featureFunctions {@link java.util.List} of {@link org.apache.joshua.decoder.ff.FeatureFunction}'s
   * @param tailNodes {@link java.util.List} of {@link org.apache.joshua.decoder.hypergraph.HGNode}'s
   * @param i todo
   * @param j todo
   * @param sourcePath information about a path taken through the source lattice
   * @param sentence the lattice input
   * @return the final cost for the Node
   */
  public static float computeFinalCost(List<FeatureFunction> featureFunctions,
      List<HGNode> tailNodes, int i, int j, SourcePath sourcePath, Sentence sentence) {

    float cost = 0;
    for (FeatureFunction ff : featureFunctions) {
      cost += ff.computeFinalCost(tailNodes.get(0), i, j, sourcePath, sentence);
    }
    return cost;
  }

  public static FeatureVector computeTransitionFeatures(List<FeatureFunction> featureFunctions,
      HyperEdge edge, int i, int j, Sentence sentence) {

    // Initialize the set of features with those that were present with the rule in the grammar.
    FeatureVector featureDelta = new FeatureVector();

    // === compute feature logPs
    for (FeatureFunction ff : featureFunctions) {
      // A null rule signifies the final transition.
      if (edge.getRule() == null)
        featureDelta.add(ff.computeFinalFeatures(edge.getTailNodes().get(0), i, j, edge.getSourcePath(), sentence));
      else {
        featureDelta.add(ff.computeFeatures(edge.getRule(), edge.getTailNodes(), i, j, edge.getSourcePath(), sentence));
      }
    }

    return featureDelta;
  }

  public float getPruningEstimate() {
    return this.pruningCostEstimate;
  }

  /**
   *  The complete cost of the Viterbi derivation at this point
   *  @return float representing cost
   */
  public float getViterbiCost() {
    return this.viterbiCost;
  }

  public float getBaseCost() {
    return getViterbiCost() - getTransitionCost();
  }

  /**
   * The cost incurred by this edge alone
   * 
   * @return float representing cost
   */
  public float getTransitionCost() {
    return this.transitionCost;
  }

  public List<DPState> getDPStates() {
    return this.dpStates;
  }

  public void printInfo() {
    System.out.println("scores: " + transitionCost + "; " + viterbiCost + "; "
        + pruningCostEstimate);
  }
}
