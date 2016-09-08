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

/*** 
 * A candidate represents a translation hypothesis that may possibly be added to the translation
 * hypergraph. It groups together (a) a set of translation hypotheses all having the same coverage
 * vector and (b) a set of compatible phrase extensions that all cover the same source span. A 
 * Candidate object therefore denotes a particular precise coverage vector. When a Candidate is
 * instantiated, it has values in ranks[] that are indices into these two lists representing
 * the current cube prune state.
 * 
 * For any particular (previous hypothesis) x (translation option) combination (a selection from
 * both lists), there is no guarantee about whether this is a (m)onotonic, (s)wap, or (d)iscontinuous
 * rule application. This must be inferred from the span (recording the portion of the input being
 * translated) and the last index of the previous hypothesis under consideration.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.joshua.corpus.Span;
import org.apache.joshua.decoder.chart_parser.ComputeNodeResult;
import org.apache.joshua.decoder.ff.FeatureFunction;
import org.apache.joshua.decoder.ff.state_maintenance.DPState;
import org.apache.joshua.decoder.ff.tm.Rule;
import org.apache.joshua.decoder.hypergraph.HGNode;
import org.apache.joshua.decoder.segment_file.Sentence;

public class Candidate implements Comparable<Candidate> {
  
  private List<FeatureFunction> featureFunctions;
  private Sentence sentence;
  
  // the set of hypotheses that can be paired with phrases from this span 
  private final List<Hypothesis> hypotheses;

  // the list of target phrases gathered from a span of the input
  private PhraseNodes phrases;
  
  // future cost of applying phrases to hypotheses
  private float future_delta;
  
  // indices into the hypotheses and phrases arrays (used for cube pruning)
  private final int[] ranks;
  
  // the reordering rule used by an instantiated Candidate
  private Rule rule;
  
  /* 
   * Stores the inside cost of the current phrase, as well as the computed dynamic programming
   * state. Expensive to compute so there is an option of delaying it.
   */
  private ComputeNodeResult computedResult;

  /**
   * When candidate objects are extended, the new one is initialized with the same underlying
   * "phrases" and "hypotheses" and "span" objects. So these all have to be equal, as well as
   * the ranks.
   * 
   * This is used to prevent cube pruning from adding the same candidate twice, having reached
   * a point in the cube via different paths.
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Candidate) {
      Candidate other = (Candidate) obj;
      if (hypotheses != other.hypotheses || phrases != other.phrases)
        return false;
      
      if (ranks.length != other.ranks.length)
        return false;
      
      for (int i = 0; i < ranks.length; i++)
        if (ranks[i] != other.ranks[i])
          return false;
          
      return true;
    }
    return false;
  }
  
  @Override
  public int hashCode() {
    return 17 * hypotheses.size() 
        + 23 * phrases.size() 
        + 117 * Arrays.hashCode(ranks);
//    return hypotheses.hashCode() * phrases.hashCode() * span.hashCode() * Arrays.hashCode(ranks);
  }
  
  @Override
  public String toString() {
    return String.format("CANDIDATE(hyp %d/%d, phr %d/%d) %.3f [%s] phrase=[%s] span=%s",
        ranks[0], hypotheses.size(), ranks[1], phrases.size(), score(),
        getHypothesis(), getPhraseNode().bestHyperedge.getRule().getEnglishWords(), getSpan());
  }

  public Candidate(List<FeatureFunction> featureFunctions, Sentence sentence, 
      List<Hypothesis> hypotheses, PhraseNodes phrases, float delta, int[] ranks) {
    this.featureFunctions = featureFunctions;
    this.sentence = sentence;
    this.hypotheses = hypotheses;
    this.phrases = phrases;
    this.future_delta = delta;
    this.ranks = ranks;
    this.rule = isMonotonic() ? Hypothesis.INORDER_RULE : Hypothesis.INVERTED_RULE;
//    this.score = hypotheses.get(ranks[0]).score + phrases.get(ranks[1]).getEstimatedCost();

    this.computedResult = null;
    
    // TODO: compute this proactively or lazily according to a parameter
    computeResult();
  }
  
  /**
   * Determines whether the current previous hypothesis extended with the currently selected
   * phrase represents a straight or inverted rule application.
   * 
   * @return
   */
  private boolean isMonotonic() {
//    System.err.println(String.format("isMonotonic(); %d < %d -> %s", 
//        getLastCovered(), getPhraseEnd(), getLastCovered() < getPhraseEnd()));
    return getLastCovered() < getPhraseEnd();
  }
  
  /**
   * Extends the cube pruning dot in both directions and returns the resulting set. Either of the
   * results can be null if the end of their respective lists is reached.
   * 
   * @return The neighboring candidates (possibly null)
   */
  public Candidate[] extend() {
    return new Candidate[] { extendHypothesis(), extendPhrase() };
  }
  
  /**
   * Extends the cube pruning dot along the dimension of existing hypotheses.
   * 
   * @return the next candidate, or null if none
   */
  public Candidate extendHypothesis() {
    if (ranks[0] < hypotheses.size() - 1) {
      return new Candidate(featureFunctions, sentence, hypotheses, phrases, future_delta, new int[] { ranks[0] + 1, ranks[1] });
    }
    return null;
  }
  
  /**
   * Extends the cube pruning dot along the dimension of candidate target sides.
   * 
   * @return the next Candidate, or null if none
   */
  public Candidate extendPhrase() {
    if (ranks[1] < phrases.size() - 1) {
      return new Candidate(featureFunctions, sentence, hypotheses, phrases, future_delta, new int[] { ranks[0], ranks[1] + 1 });
    }
    
    return null;
  }
  
  /**
   * Returns the input span from which the phrases for this candidates were gathered.
   * 
   * @return the span object
   */
  public Span getSpan() {
    return new Span(this.phrases.i, this.phrases.j);
  }
  
  /**
   * A candidate is a (hypothesis, target phrase) pairing. The hypothesis and target phrase are
   * drawn from a list that is indexed by (ranks[0], ranks[1]), respectively. This is a shortcut
   * to return the hypothesis of the candidate pair.
   * 
   * @return the hypothesis at position ranks[0]
   */
  public Hypothesis getHypothesis() {
    return this.hypotheses.get(ranks[0]);
  }
  
  /**
   * A candidate is a (hypothesis, target phrase) pairing. The hypothesis and target phrase are
   * drawn from a list that is indexed by (ranks[0], ranks[1]), respectively. This is a shortcut
   * to return the rule representing the terminal phrase production of the candidate pair.
   * 
   * @return the phrase rule at position ranks[1]
   */
  public Rule getPhraseRule() {
    Rule rule = getPhraseNode().bestHyperedge.getRule();
    return rule;
  }
  
  /**
   * This returns a new Hypothesis (HGNode) representing the phrase being added, i.e., a terminal
   * production in the hypergraph. The score and DP state are computed only here on demand.
   * 
   * @return a new hypergraph node representing the phrase translation
   */
  public HGNode getPhraseNode() {
    return this.phrases.get(ranks[1]);
  }
  
  /**
   * Ensures that the cost of applying the edge has been 
   * computed. This is tucked away in an accessor so that 
   * we can do it lazily if we wish.
   * 
   * @return the computed result.
   */
  public ComputeNodeResult computeResult() {
    if (computedResult == null) {
      // add the rule
      // TODO: sourcepath
      computedResult = new ComputeNodeResult(featureFunctions, getRule(), getTailNodes(), getLastCovered(), getPhraseEnd(), null, sentence);
    }
    
    return computedResult;
  }
    
  /**
   * This returns the rule being applied (straight or inverted)
   * 
   * @return the phrase at position ranks[1]
   */
  public Rule getRule() {
    return this.rule;
  }
  
  /**
   * The hypotheses list is a list of tail pointers. This function returns the tail pointer
   * currently selected by the value in ranks.
   * 
   * @return a list of size one, wrapping the tail node pointer
   */
  public List<HGNode> getTailNodes() {
    List<HGNode> tailNodes = new ArrayList<HGNode>();
    if (isMonotonic()) {
      tailNodes.add(getHypothesis());
      tailNodes.add(getPhraseNode());
    } else {
      tailNodes.add(getPhraseNode());
      tailNodes.add(getHypothesis());
    }
    return tailNodes;
  }
  
  /**
   * Returns the bit vector of this hypothesis. The bit vector is computed by ORing the coverage
   * vector of the tail node (hypothesis) and the source span of phrases in this candidate.
   * @return the bit vector of this hypothesis
   */
  public Coverage getCoverage() {
    Coverage cov = new Coverage(getHypothesis().getCoverage());
    cov.set(getSpan());
    return cov;
  }

  /**
   * This returns the sum of two costs: the Viterbi cost of the edge represented by the current
   * cube pruning state, plus the difference to the future cost incurred by translating the
   * current phrase.
   * 
   * Note that in phrase-based decoding, the Hypothesis scores include the future cost estimate,
   * which means that the Viterbi cost (which includes only the inside estimates) is not the complete
   * cost; instead, you have to chain the calls to the hypothesis. This should be fixed and cleaned
   * up to formally separate the "inside" and "outside" costs.
   * 
   * @return the inside + outside cost
   */
  public float score() {
//    float score = computedResult.getViterbiCost() + future_delta;
    float score = getHypothesis().getScore() + getPhraseNode().getScore() + future_delta + computedResult.getTransitionCost();
    return score;
  }
  
  public float getFutureEstimate() {
    return getHypothesis().getScore() + future_delta;
  }
  
  public List<DPState> getStates() {
    return computeResult().getDPStates();
  }
  
  public int getLastCovered() {
    return getHypothesis().getLastSourceIndex();
  }
  
  public int getPhraseEnd() {
    return phrases.j;
  }

  @Override
  public int compareTo(Candidate other) {
    return Float.compare(other.score(), score());
  }
}
