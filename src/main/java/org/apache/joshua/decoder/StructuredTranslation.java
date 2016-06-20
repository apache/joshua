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

import java.util.List;
import java.util.Map;

import org.apache.joshua.decoder.ff.FeatureVector;
import org.apache.joshua.decoder.hypergraph.KBestExtractor.DerivationState;
import org.apache.joshua.decoder.io.DeNormalize;
import org.apache.joshua.decoder.segment_file.Sentence;
import org.apache.joshua.decoder.segment_file.Token;
import org.apache.joshua.util.FormatUtils;

/**
 * A StructuredTranslation instance provides a more structured access to
 * translation results than the string-based Translation class.
 * This is useful if the decoder is encapsulated in a larger project, instead
 * of simply writing to a file or stdout.
 * StructuredTranslation encodes all relevant information about a derivation,
 * namely output string, tokens, score, features, and word alignment.
 * 
 * @author fhieber
 */
public class StructuredTranslation {
  
  private final Sentence sourceSentence;
  private final String translationString;
  private final List<String> translationTokens;
  private final float translationScore;
  private final List<List<Integer>> translationWordAlignments;
  private final FeatureVector translationFeatures;
  private final float extractionTime;
  
  public StructuredTranslation(
      final Sentence sourceSentence,
      final String translationString,
      final List<String> translationTokens,
      final float translationScore,
      final List<List<Integer>> translationWordAlignments,
      final FeatureVector translationFeatures,
      final float extractionTime) {
    this.sourceSentence = sourceSentence;
    this.translationString = translationString;
    this.translationTokens = translationTokens;
    this.translationScore = translationScore;
    this.translationWordAlignments = translationWordAlignments;
    this.translationFeatures = translationFeatures;
    this.extractionTime = extractionTime;
  }
  
  public Sentence getSourceSentence() {
    return sourceSentence;
  }

  public int getSentenceId() {
    return sourceSentence.id();
  }

  /**
   * Produces the raw translation hypothesis (still tokenized).
   * 
   * @return the raw translation hypothesis
   */
  public String getTranslationString() {
    return translationString;
  }
  
  /**
   * Produces the translation formatted according to the value of {@value JoshuaConfiguration.output_format}.
   * Also includes formatting options such as {@value JoshuaConfiguration.project_case}.
   * 
   * @return
   */
  public String getFormattedTranslationString() {
    JoshuaConfiguration config = sourceSentence.config;
    String outputString = config.outputFormat
        .replace("%s", getTranslationString())
        .replace("%S", DeNormalize.processSingleLine(maybeProjectCase(getTranslationString())))
        .replace("%i", Integer.toString(getSentenceId()))
        .replace("%f", config.moses ? translationFeatures.mosesString() : translationFeatures.toString())
        .replace("%c", String.format("%.3f", getTranslationScore()));
    return outputString;
  }

  public List<String> getTranslationTokens() {
    return translationTokens;
  }

  public float getTranslationScore() {
    return translationScore;
  }

  /**
   * Returns a list of target to source alignments.
   * @return a list of target to source alignments
   */
  public List<List<Integer>> getTranslationWordAlignments() {
    return translationWordAlignments;
  }
  
  public Map<String,Float> getTranslationFeatures() {
    return translationFeatures.getMap();
  }
  
  /**
   * Time taken to build output information from the hypergraph.
   * @return the time taken to build output information from the hypergraph
   */
  public Float getExtractionTime() {
    return extractionTime;
  }
  
  /**
   * If requested, projects source-side lettercase to target, and appends the alignment from
   * to the source-side sentence in ||s.
   * 
   * @param hypothesis todo
   * @param state todo
   * @return source-side lettercase to target, and appends the alignment from to the source-side sentence in ||s
   */
  private String maybeProjectCase(String hypothesis) {
    String output = hypothesis;

    JoshuaConfiguration config = sourceSentence.config;
    if (config.project_case) {
      String[] tokens = hypothesis.split("\\s+");
      List<List<Integer>> points = getTranslationWordAlignments();
      for (int i = 0; i < points.size(); i++) {
        List<Integer> target = points.get(i);
        for (int source: target) {
          Token token = sourceSentence.getTokens().get(source + 1); // skip <s>
          String annotation = "";
          if (token != null && token.getAnnotation("lettercase") != null)
            annotation = token.getAnnotation("lettercase");
          if (source != 0 && annotation.equals("upper"))
            tokens[i] = FormatUtils.capitalize(tokens[i]);
          else if (annotation.equals("all-upper"))
            tokens[i] = tokens[i].toUpperCase();
        }
      }

      output = String.join(" ",  tokens);
    }

    return output;
  }
}
