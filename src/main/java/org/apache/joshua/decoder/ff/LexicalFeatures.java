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

import java.util.ArrayList;
import java.util.List;

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.decoder.chart_parser.SourcePath;
import org.apache.joshua.decoder.ff.state_maintenance.DPState;
import org.apache.joshua.decoder.ff.tm.Rule;
import org.apache.joshua.decoder.hypergraph.HGNode;
import org.apache.joshua.decoder.segment_file.Sentence;
import org.apache.joshua.util.FormatUtils;

import com.google.common.cache.Cache;

/**
 *  Lexical alignment features denoting alignments, deletions, and insertions.
 */
public class LexicalFeatures extends StatelessFF {
  
  private final boolean useAlignments;
  private final boolean useDeletions;
  private final boolean useInsertions;
  
  private static final String NAME = "LexicalFeatures";
  // value to fire for features
  private static final int VALUE = 1;
  //whether this feature is restricted to a certain grammar/owner
  private final boolean ownerRestriction;
  // the grammar/owner this feature is restricted to fire
  private final int owner;
  // Strings separating words
  private static final String SEPARATOR = "~";
  
  private final Cache<Rule, List<String>> featureCache;
  
  public LexicalFeatures(FeatureVector weights, String[] args, JoshuaConfiguration config) {
    super(weights, NAME, args, config);
    
    ownerRestriction = (parsedArgs.containsKey("owner")) ? true : false;
    owner = ownerRestriction ? Vocabulary.id(parsedArgs.get("owner")) : 0;
    
    useAlignments = parsedArgs.containsKey("alignments");
    useDeletions = parsedArgs.containsKey("deletions");
    useInsertions = parsedArgs.containsKey("insertions");
    
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
    
    if (ownerRestriction && rule.getOwner() != owner) {
      return null;
    }

    List<String> featureNames = featureCache.getIfPresent(rule);
    if (featureNames == null) {
      featureNames = getFeatures(rule);
      featureCache.put(rule, featureNames);
    }
    for (String feature : featureNames) {
      acc.add(feature, VALUE);
    }
    
    return null;
  }
  
  /**
   * Obtains the feature ids for the given rule.
   * @param rule
   * @return String representing the feature name.s
   */
  private List<String> getFeatures(final Rule rule) {
    final List<String> result = new ArrayList<>();
    
    byte[] alignments = rule.getAlignment();
    if (alignments == null) {
      return result;
    }
    int[] sourceWords = rule.getFrench();
    int[] targetWords = rule.getEnglish();
    
    // sourceAligned & targetAligned indicate whether an index is covered by alignments
    boolean[] sourceAligned = new boolean[sourceWords.length];
    boolean[] targetAligned = new boolean[targetWords.length];
    
    // translations: aligned words
    for (int i = 0; i < alignments.length; i+=2) {
      byte sourceIndex = alignments[i];
      byte targetIndex = alignments[i + 1];
      sourceAligned[sourceIndex] = true;
      targetAligned[targetIndex] = true;
      if (useAlignments) {
        result.add(
            "T:" + 
            Vocabulary.word(sourceWords[sourceIndex]) + 
            SEPARATOR + 
            Vocabulary.word(targetWords[targetIndex]));
      }
    }
    
    // deletions: unaligned source words
    if (useDeletions) {
      for (int i = 0; i < sourceAligned.length; i++) {
        if (!sourceAligned[i] && ! FormatUtils.isNonterminal(sourceWords[i])) {
          result.add("D:" + Vocabulary.word(sourceWords[i]));
        }
      }
    }
    
    // insertions: unaligned target words
    if (useInsertions) {
      for (int i = 0; i < targetAligned.length; i++) {
        if (useInsertions && !targetAligned[i] && ! FormatUtils.isNonterminal(targetWords[i])) {
          result.add("I:" + Vocabulary.word(targetWords[i]));
        }
      }
    }
    
    return result;
  }
}
