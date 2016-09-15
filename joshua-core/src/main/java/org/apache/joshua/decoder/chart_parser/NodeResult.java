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

import java.util.List;

import org.apache.joshua.decoder.ff.state_maintenance.DPState;

/**
 * This class represents the cost of applying a rule.
 * 
 * @author Matt Post post@cs.jhu.edu
 * @author Zhifei Li, zhifei.work@gmail.com
 */
public class NodeResult {

  // The cost incurred by the rule itself (and all associated feature functions)
  private final float transitionCost;

  // transitionCost + the Viterbi costs of the tail nodes.
  private final float viterbiCost;

  // The future or outside cost (estimated)
  private final float futureCostEstimate;
  
  // The StateComputer objects themselves serve as keys.
  private final List<DPState> dpStates;

  public NodeResult(float transitionCost, float viterbiCost, float futureCostEstimate, List<DPState> dpStates) {
    this.transitionCost = transitionCost;
    this.viterbiCost = viterbiCost;
    this.futureCostEstimate = futureCostEstimate;
    this.dpStates = dpStates;
  }

  public float getFutureEstimate() {
    return this.futureCostEstimate;
  }

  public float getPruningEstimate() {
    return getViterbiCost() + getFutureEstimate();
  }

  /**
   *  The complete cost of the Viterbi derivation at this point.
   *
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

}
