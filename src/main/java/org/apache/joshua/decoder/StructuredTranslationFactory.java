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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.apache.joshua.decoder.hypergraph.ViterbiExtractor.getViterbiFeatures;
import static org.apache.joshua.decoder.hypergraph.ViterbiExtractor.getViterbiString;
import static org.apache.joshua.decoder.hypergraph.ViterbiExtractor.getViterbiWordAlignmentList;
import static org.apache.joshua.util.FormatUtils.removeSentenceMarkers;

import java.util.List;

import org.apache.joshua.decoder.ff.FeatureFunction;
import org.apache.joshua.decoder.ff.FeatureVector;
import org.apache.joshua.decoder.hypergraph.HyperGraph;
import org.apache.joshua.decoder.hypergraph.KBestExtractor.DerivationState;
import org.apache.joshua.decoder.segment_file.Sentence;

/**
 * This factory provides methods to create StructuredTranslation objects
 * from either Viterbi derivations or KBest derivations.
 * 
 * @author fhieber
 */
public class StructuredTranslationFactory {
  
  /**
   * Returns a StructuredTranslation instance from the Viterbi derivation.
   * 
   * @param sourceSentence the source sentence
   * @param hypergraph the hypergraph object
   * @param featureFunctions the list of active feature functions
   * @return A StructuredTranslation object representing the Viterbi derivation.
   */
  public static StructuredTranslation fromViterbiDerivation(
      final Sentence sourceSentence,
      final HyperGraph hypergraph,
      final List<FeatureFunction> featureFunctions) {
    final long startTime = System.currentTimeMillis();
    final String translationString = removeSentenceMarkers(getViterbiString(hypergraph));
    return new StructuredTranslation(
        sourceSentence,
        translationString,
        extractTranslationTokens(translationString),
        extractTranslationScore(hypergraph),
        getViterbiWordAlignmentList(hypergraph),
        getViterbiFeatures(hypergraph, featureFunctions, sourceSentence),
        (System.currentTimeMillis() - startTime) / 1000.0f);
  }
  
  /**
   * Returns a StructuredTranslation from an empty decoder output
   * @param sourceSentence the source sentence
   * @return a StructuredTranslation object
   */
  public static StructuredTranslation fromEmptyOutput(final Sentence sourceSentence) {
        return new StructuredTranslation(
                sourceSentence, "", emptyList(), 0, emptyList(), new FeatureVector(), 0f);
      }
  
  /**
   * Returns a StructuredTranslation instance from a KBest DerivationState. 
   * @param sourceSentence Sentence object representing the source.
   * @param derivationState the KBest DerivationState.
   * @return A StructuredTranslation object representing the derivation encoded by derivationState.
   */
  public static StructuredTranslation fromKBestDerivation(
      final Sentence sourceSentence,
      final DerivationState derivationState) {
    final long startTime = System.currentTimeMillis();
    final String translationString = removeSentenceMarkers(derivationState.getHypothesis());
    return new StructuredTranslation(
        sourceSentence,
        translationString,
        extractTranslationTokens(translationString),
        derivationState.getModelCost(),
        derivationState.getWordAlignmentList(),
        derivationState.getFeatures(),
        (System.currentTimeMillis() - startTime) / 1000.0f);
  }
  
  private static float extractTranslationScore(final HyperGraph hypergraph) {
    if (hypergraph == null) {
      return 0;
    } else {
      return hypergraph.goalNode.getScore();
    }
  }
  
  private static List<String> extractTranslationTokens(final String translationString) {
    if (translationString.isEmpty()) {
      return emptyList();
    } else {
      return asList(translationString.split("\\s+"));
    }
  }
  

}
