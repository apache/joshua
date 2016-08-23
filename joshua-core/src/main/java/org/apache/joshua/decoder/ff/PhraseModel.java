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
package org.apache.joshua.decoder.ff;

import java.util.List;
import java.util.Map.Entry;

import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.decoder.chart_parser.SourcePath;
import org.apache.joshua.decoder.ff.state_maintenance.DPState;
import org.apache.joshua.decoder.ff.tm.Grammar;
import org.apache.joshua.decoder.ff.tm.OwnerId;
import org.apache.joshua.decoder.ff.tm.OwnerMap;
import org.apache.joshua.decoder.ff.tm.Rule;
import org.apache.joshua.decoder.hypergraph.HGNode;
import org.apache.joshua.decoder.segment_file.Sentence;

/**
 * This feature handles the list of features that are stored with grammar rules in the grammar file.
 * These are by convention bound to the PhraseModel feature function and will be prepended by the owner of this
 * PhraseModel instance, i.e. 'p_e_given_f' will be hashed as '<owner>_p_e_given_f'.
 * If multiple grammars exist and feature sharing is needed, one must implement a separate feature function for this.
 * 
 * @author Matt Post post@cs.jhu.edu
 * @author Zhifei Li zhifei.work@gmail.com
 */

public class PhraseModel extends StatelessFF {
  
  private final OwnerId owner;

  public PhraseModel(FeatureVector weights, String[] args, JoshuaConfiguration config, Grammar g) {
    // name of this feature is the owner of the grammar
    super(weights, OwnerMap.getOwner(g.getOwner()), args, config);
    this.owner = g.getOwner();
  }

  /**
   * Estimates the cost of applying this rule, which is just the score of the precomputable feature
   * functions.
   */
  @Override
  public float estimateCost(final Rule rule, Sentence sentence) {
    // check if the rule belongs to this PhraseModel
    if (rule != null && rule.getOwner().equals(owner)) {
      return rule.getFeatureVector().innerProduct(weights);
    }
    return 0.0f;
  }

  /**
   * Accumulates the cost of applying this rule if it belongs to the owner.
   */
  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      Sentence sentence, Accumulator acc) {

    if (rule != null && rule.getOwner().equals(owner)) {
      for (Entry<Integer, Float> entry : rule.getFeatureVector().entrySet()) {
        final int featureId = entry.getKey();
        final float featureValue = entry.getValue();
        acc.add(featureId, featureValue);
      }
    }
    return null;
  }

  public String toString() {
    return name;
  }
}
