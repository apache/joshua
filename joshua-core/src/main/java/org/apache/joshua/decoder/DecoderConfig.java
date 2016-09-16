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
package org.apache.joshua.decoder;

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.decoder.ff.FeatureFunction;
import org.apache.joshua.decoder.ff.FeatureMap;
import org.apache.joshua.decoder.ff.FeatureVector;
import org.apache.joshua.decoder.ff.tm.Grammar;
import org.apache.joshua.decoder.ff.tm.OwnerMap;

import com.google.common.collect.ImmutableList;

/**
 * This is the central state object that holds references to relevant attributes
 * of the decoder (features, grammars, etc.).
 * A sentence-specific instance of this object is created before
 * translating a single sentence.
 * 
 * @author Felix Hieber, felix.hieber@gmail.com
 */
public class DecoderConfig {
  
  /** Decoder feature functions */
  private final ImmutableList<FeatureFunction> featureFunctions;
  
  /** Decoder grammars/phrase tables */
  private final ImmutableList<Grammar> grammars;
  
  /** Decoder custom grammar where rules can be added */
  private final Grammar customGrammar;
  
  /** Decoder vocabulary */
  private final Vocabulary vocabulary;
  
  /** Decoder weights */
  private final FeatureVector weights;
  
  /** Decoder feature mapping */
  private final FeatureMap featureMap;
  
  /** Decoder grammar owner mapping */
  private final OwnerMap ownerMap;
  
  private final SearchAlgorithm searchAlgorithm;
  
  public DecoderConfig(
      final SearchAlgorithm searchAlgorithm,
      final ImmutableList<FeatureFunction> featureFunctions,
      final ImmutableList<Grammar> grammars,
      final Grammar customGrammar,
      final Vocabulary vocabulary,
      final FeatureVector weights,
      final FeatureMap featureMap,
      final OwnerMap ownerMap) {
    this.searchAlgorithm = searchAlgorithm;
    this.featureFunctions = featureFunctions;
    this.grammars = grammars;
    this.customGrammar = customGrammar;
    this.vocabulary = vocabulary;
    this.weights = weights;
    this.featureMap = featureMap;
    this.ownerMap = ownerMap;
  }

  public ImmutableList<FeatureFunction> getFeatureFunctions() {
    return featureFunctions;
  }

  public ImmutableList<Grammar> getGrammars() {
    return grammars;
  }
  
  public Grammar getCustomGrammar() {
    return customGrammar;
  }

  public Vocabulary getVocabulary() {
    return vocabulary;
  }

  public FeatureVector getWeights() {
    return weights;
  }

  public FeatureMap getFeatureMap() {
    return featureMap;
  }

  public OwnerMap getOwnerMap() {
    return ownerMap;
  }
  
  public SearchAlgorithm getSearchAlgorithm() {
    return searchAlgorithm;
  }

}
