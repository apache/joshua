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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.decoder.JoshuaConfiguration.OOVItem;
import org.apache.joshua.decoder.ff.state_maintenance.DPState;
import org.apache.joshua.decoder.ff.tm.OwnerId;
import org.apache.joshua.decoder.ff.tm.OwnerMap;
import org.apache.joshua.decoder.ff.tm.Rule;
import org.apache.joshua.decoder.hypergraph.HGNode;
import org.apache.joshua.decoder.segment_file.Sentence;
import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.decoder.chart_parser.SourcePath;

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
  
  /* The default value returned for OOVs. Can be overridden with -oov-list */
  private final float defaultValue = -100f;
  private final HashMap<Integer,Float> oovWeights;

  public OOVPenalty(FeatureVector weights, String[] args, JoshuaConfiguration config) {
    super(weights, "OOVPenalty", args, config);

    ownerID = OwnerMap.register("oov");
    oovWeights = new HashMap<Integer,Float>();
    
    if (config.oovList != null) {
      for (OOVItem item: config.oovList) { 
        oovWeights.put(Vocabulary.id(item.label), item.weight);
      }
    }
  }
  
  @Override
  public ArrayList<String> reportDenseFeatures(int index) {
    denseFeatureIndex = index;
    
    ArrayList<String> names = new ArrayList<>(1);
    names.add(name);
    return names;
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
      acc.add(denseFeatureIndex, getValue(rule.getLHS()));
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
    if (rule != null && this.ownerID.equals(rule.getOwner()))
      return weights.getDense(denseFeatureIndex) * getValue(rule.getLHS());
    return 0.0f;
  }
  
  private float getValue(int lhs) {
    return oovWeights.containsKey(lhs) ? oovWeights.get(lhs) : defaultValue;
  }
}
