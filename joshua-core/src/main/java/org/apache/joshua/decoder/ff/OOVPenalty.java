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

import static org.apache.joshua.util.Constants.OOV_OWNER;

import java.util.List;

import org.apache.joshua.decoder.chart_parser.SourcePath;
import org.apache.joshua.decoder.ff.state_maintenance.DPState;
import org.apache.joshua.decoder.ff.tm.OwnerId;
import org.apache.joshua.decoder.ff.tm.OwnerMap;
import org.apache.joshua.decoder.ff.tm.Rule;
import org.apache.joshua.decoder.hypergraph.HGNode;
import org.apache.joshua.decoder.segment_file.Sentence;
import org.apache.joshua.util.Constants;

import com.typesafe.config.Config;

/**
 * This feature is fired when an out-of-vocabulary word (with respect to the translation model) is
 * entered into the chart. OOVs work in the following manner: for each word in the input that is OOV
 * with respect to the translation model, we create a rule that pushes that word through
 * untranslated (the suffix "_OOV" can optionally be appended according to the runtime parameter
 * "mark-oovs") . These rules are all stored in a grammar whose owner is "oov". The OOV feature
 * function template then fires the "OOVPenalty" feature whenever it is asked to score an OOV rule.
 * 
 * @author Matt Post post@cs.jhu.edu
 */
public class OOVPenalty extends StatelessFF {
  private final OwnerId ownerID;
  private static final String NAME = "OOVPenalty";
  private static final float DEFAULT_VALUE = -100f;

  public OOVPenalty(Config featureConfig, FeatureVector weights) {
    super(NAME, featureConfig, weights);
    ownerID = OwnerMap.register(OOV_OWNER);
  }
  
  /**
   * OOV rules cover exactly one word, and such rules belong to a grammar whose owner is "oov". Each
   * OOV fires the OOVPenalty feature with a value of 1, so the cost is simply the weight, which was
   * cached when the feature was created.
   */
  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      Sentence sentence, Accumulator acc) {
    
    if (rule != null && this.ownerID.equals(rule.getOwner())) {
      acc.add(featureId, DEFAULT_VALUE);
    }

    return null;
  }
  
  /**
   * It's important for the OOV feature to contribute to the rule's estimated cost, so that OOV
   * rules (which are added for all words, not just ones without translation options) get sorted
   * to the bottom during cube pruning.
   * 
   * Important! estimateCost returns the *weighted* feature value.
   */
  @Override
  public float estimateCost(Rule rule, Sentence sentence) {
    if (rule != null && this.ownerID.equals(rule.getOwner())) {
      return weights.getOrDefault(featureId) * DEFAULT_VALUE;
    }
    return 0.0f;
  }
}
