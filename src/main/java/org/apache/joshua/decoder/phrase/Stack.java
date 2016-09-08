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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.decoder.ff.tm.Rule;
import org.apache.joshua.decoder.segment_file.Sentence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Organizes all hypotheses containing the same number of source words. 
 *
 */
public class Stack extends ArrayList<Hypothesis> {

  private static final Logger LOG = LoggerFactory.getLogger(Stack.class);

  private static final long serialVersionUID = 7885252799032416068L;

  private final HashMap<Coverage, ArrayList<Hypothesis>> coverages;
  
  private Sentence sentence;
  private JoshuaConfiguration config;

  /* The list of states we've already visited. */
  private final HashSet<Candidate> visitedStates;
  
  /* A list of candidates sorted for consideration for entry to the chart (for cube pruning) */
  private final PriorityQueue<Candidate> candidates;
  
  /* Short-circuits adding a cube-prune state more than once */
  private final HashMap<Hypothesis, Hypothesis> deduper;
  
  /**
   * Create a new stack. Stacks are organized one for each number of source words that are covered.
   * 
   * @param sentence input for a {@link org.apache.joshua.lattice.Lattice}
   * @param config populated {@link org.apache.joshua.decoder.JoshuaConfiguration}
   */
  public Stack(Sentence sentence, JoshuaConfiguration config) {
    this.sentence = sentence;
    this.config = config;
    
    this.candidates = new PriorityQueue<Candidate>(1);
    this.coverages = new HashMap<Coverage, ArrayList<Hypothesis>>();
    this.visitedStates = new HashSet<Candidate>();
    this.deduper = new HashMap<Hypothesis,Hypothesis>();
  }

  /**
   * A Stack is an ArrayList; here, we intercept the add so we can maintain a list of the items
   * stored under each distinct coverage vector
   * @param hyp a {@link org.apache.joshua.decoder.phrase.Hypothesis} to add to the {@link org.apache.joshua.decoder.phrase.Stack}
   * @return true if the {@link org.apache.joshua.decoder.phrase.Hypothesis} is appended to the list
   */
  @Override
  public boolean add(Hypothesis hyp) {
    
    if (! coverages.containsKey((hyp.getCoverage())))
      coverages.put(hyp.getCoverage(), new ArrayList<>());
    coverages.get(hyp.getCoverage()).add(hyp);
    
    return super.add(hyp);
  }
  
  /**
   * Intercept calls to remove() so that we can reduce the coverage vector
   */
  @Override
  public boolean remove(Object obj) {
    boolean found = super.remove(obj);
    if (found) {
      Hypothesis item = (Hypothesis) obj;
      Coverage cov = item.getCoverage();
      assert coverages.get(cov).remove(obj);
      if (coverages.get(cov).size() == 0)
        coverages.remove(cov);
    }
    return found;
  }
  
  /** 
   * Returns the set of coverages contained in this stack. This is used to iterate over them
   * in the main decoding loop in Stacks.java.
   * @return a {@link java.util.Set} of {@link org.apache.joshua.decoder.phrase.Coverage}'s
   */
  public Set<Coverage> getCoverages() {
    return coverages.keySet();
  }
  
  /**
   * Get all items with the same coverage vector.
   * 
   * @param cov the {@link org.apache.joshua.decoder.phrase.Coverage} vector to get
   * @return an {@link java.util.ArrayList} of {@link org.apache.joshua.decoder.phrase.Hypothesis}'
   */
  public ArrayList<Hypothesis> get(Coverage cov) {
    ArrayList<Hypothesis> list = coverages.get(cov);
    Collections.sort(list);
    return list;
  }
  
  /**
   * Receives a partially-initialized translation candidate and places it on the
   * priority queue after scoring it with all of the feature functions. In this
   * respect it is like {@link org.apache.joshua.decoder.chart_parser.CubePruneState} (it could make use of that class with
   * a little generalization of spans / coverage).
   * 
   * This function is also used to (fairly concisely) implement constrained decoding. Before
   * adding a candidate, we ensure that the sequence of English words match the sentence. If not,
   * the code extends the dot in the cube-pruning chart to the next phrase, since that one might
   * be a match.
   * @param cand a partially-initialized translation {@link org.apache.joshua.decoder.phrase.Candidate}
   */
  public void addCandidate(Candidate cand) {
    if (visitedStates.contains(cand))
      return;
    
    visitedStates.add(cand);

    // Constrained decoding
    if (sentence.target() != null) {

      /* Get the rule. If if it's a swap or monolingual rule, find the right backpointer */
      Rule rule = cand.getHypothesis().getRule();
      if (rule == Hypothesis.INORDER_RULE)
        rule = cand.getHypothesis().bestHyperedge.getTailNodes().get(1).bestHyperedge.getRule();
      else if (rule == Hypothesis.INVERTED_RULE)
        rule = cand.getHypothesis().bestHyperedge.getTailNodes().get(0).bestHyperedge.getRule();
      String oldWords = rule.getEnglishWords();

      String newWords = cand.getPhraseRule().getEnglishWords();
          
      boolean allowed = sentence.fullTarget().contains(oldWords + " " + newWords);
      
      // If the string is not found in the target sentence, explore the cube neighbors
      if (! allowed) {
        Candidate next = cand.extendPhrase();
        if (next != null)
          addCandidate(next); 
        
        return;
      }
    }

    candidates.add(cand);
  }
  
  /**
   * Cube pruning. Repeatedly pop the top candidate, creating a new hyperedge from it, adding it to
   * the k-best list, and then extending the list of candidates with extensions of the current
   * candidate.
   */
  public void search() {
    int to_pop = config.pop_limit;
    
    if (LOG.isDebugEnabled()) {
      LOG.debug("Stack::search(): pop: {} size: {}", to_pop, candidates.size());
      for (Candidate c: candidates)
        LOG.debug("{}", c);
    }
    while (to_pop > 0 && !candidates.isEmpty()) {
      Candidate got = candidates.poll();
      if (got != null) {
        addHypothesis(got);
        --to_pop;
        
        for (Candidate c : got.extend())
          if (c != null) {
            addCandidate(c);
          }
      }
    }
  }

  /**
   * Adds a popped candidate to the chart / main stack. This is a candidate we have decided to
   * keep around.
   * 
   * @param complete a completely-initialized translation {@link org.apache.joshua.decoder.phrase.Candidate}
   * 
   */
  public void addHypothesis(Candidate complete) {
    Hypothesis added = new Hypothesis(complete);

    String taskName;
    if (deduper.containsKey(added)) {
      taskName = "recombining hypothesis";
      Hypothesis existing = deduper.get(added);
      existing.absorb(added);
    } else {
      taskName = "creating new hypothesis";
      add(added);
      deduper.put(added, added);
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("{} from ( ... {} )", taskName, complete.getHypothesis().getRule().getEnglishWords());
      LOG.debug("        base score {}", complete.computeResult().getBaseCost());
      LOG.debug("        covering {}-{}", complete.getSpan().start - 1, complete.getSpan().end - 2);
      LOG.debug("        translated as: {}", complete.getPhraseRule().getEnglishWords());
      LOG.debug("        score {} + future cost {} = {}",
          complete.computeResult().getTransitionCost(), complete.getFutureEstimate(),
          complete.computeResult().getTransitionCost() + complete.getFutureEstimate());
    }
  }
}
