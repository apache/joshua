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

import static com.google.common.cache.CacheBuilder.newBuilder;
import static org.apache.joshua.decoder.ff.tm.OwnerMap.UNKNOWN_OWNER_ID;

import java.util.List;

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.decoder.chart_parser.SourcePath;
import org.apache.joshua.decoder.ff.state_maintenance.DPState;
import org.apache.joshua.decoder.ff.tm.OwnerId;
import org.apache.joshua.decoder.ff.tm.OwnerMap;
import org.apache.joshua.decoder.ff.tm.Rule;
import org.apache.joshua.decoder.hypergraph.HGNode;
import org.apache.joshua.decoder.segment_file.Sentence;

import com.google.common.cache.Cache;

/**
 *  This feature fires for rule ids.
 *  Firing can be restricted to rules from a certain owner, and rule ids
 *  can be generated from source side and/or target side. 
 */
public class RuleFF extends StatelessFF {

  private enum Sides { SOURCE, TARGET, BOTH };
  
  private static final String NAME = "RuleFF";
  // value to fire for features
  private static final int VALUE = 1;
  // whether this feature is restricted to a certain grammar/owner
  private final boolean ownerRestriction;
  // the grammar/owner this feature is restricted to fire
  private final OwnerId owner;
  // what part of the rule should be extracted;
  private final Sides sides;
  // Strings separating words and rule sides 
  private static final String SEPARATOR = "~";
  private static final String SIDES_SEPARATOR = "->";
  
  private final Cache<Rule, String> featureCache;
  
  public RuleFF(FeatureVector weights, String[] args, JoshuaConfiguration config) {
    super(weights, NAME, args, config);
    
    ownerRestriction = (parsedArgs.containsKey("owner")) ? true : false;
    owner = ownerRestriction ? OwnerMap.register(parsedArgs.get("owner")) : UNKNOWN_OWNER_ID;
    
    if (parsedArgs.containsKey("sides")) {
      final String sideValue = parsedArgs.get("sides");
      if (sideValue.equalsIgnoreCase("source")) {
        sides = Sides.SOURCE;
      } else if (sideValue.equalsIgnoreCase("target")) {
        sides = Sides.TARGET;
      } else if (sideValue.equalsIgnoreCase("both")){
        sides = Sides.BOTH;
      } else {
        throw new RuntimeException("Unknown side value.");
      }
    } else {
      sides = Sides.BOTH;
    }
    
    // initialize cache
    if (parsedArgs.containsKey("cacheSize")) {
      featureCache = newBuilder().maximumSize(Integer.parseInt(parsedArgs.get("cacheSize"))).build();
    } else {
      featureCache = newBuilder().maximumSize(config.cachedRuleSize).build();
    }
  }

  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      Sentence sentence, Accumulator acc) {
    
    if (ownerRestriction && !rule.getOwner().equals(owner)) {
      return null;
    }

    String featureName = featureCache.getIfPresent(rule);
    if (featureName == null) {
      featureName = getRuleString(rule);
      featureCache.put(rule, featureName);
    }
    acc.add(featureName, VALUE);
    
    return null;
  }
  
  /**
   * Obtains the feature id for the given rule.
   * @param rule
   * @return String representing the feature name.s
   */
  private String getRuleString(final Rule rule) {
    final StringBuilder sb = new StringBuilder(Vocabulary.word(rule.getLHS()))
      .append(SIDES_SEPARATOR);
    if (sides == Sides.SOURCE || sides == Sides.BOTH) {
      sb.append(Vocabulary.getWords(rule.getFrench(), SEPARATOR));
    }
    sb.append(SIDES_SEPARATOR);
    if (sides == Sides.TARGET || sides == Sides.BOTH) {
      sb.append(Vocabulary.getWords(rule.getEnglish(), SEPARATOR));
    }
    return sb.toString();
  }
}
