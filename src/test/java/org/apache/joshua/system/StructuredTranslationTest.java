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
package org.apache.joshua.system;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.joshua.decoder.Decoder;
import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.decoder.StructuredTranslation;
import org.apache.joshua.decoder.Translation;
import org.apache.joshua.decoder.segment_file.Sentence;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration test for the complete Joshua decoder using a toy grammar that translates
 * a bunch of capital letters to lowercase letters. Rules in the test grammar
 * drop and generate additional words and simulate reordering of rules, so that
 * proper extraction of word alignments and other information from the decoder
 * can be tested.
 *
 * @author fhieber
 */
public class StructuredTranslationTest {

  private JoshuaConfiguration joshuaConfig = null;
  private Decoder decoder = null;
  private static final String INPUT = "A K B1 U Z1 Z2 B2 C";
  private static final String EXPECTED_TRANSLATION = "a b n1 u z c1 k1 k2 k3 n1 n2 n3 c2";
  private static final List<String> EXPECTED_TRANSLATED_TOKENS = asList(EXPECTED_TRANSLATION.split("\\s+"));
  private static final String EXPECTED_WORD_ALIGNMENT_STRING = "0-0 2-1 6-1 3-3 4-4 5-4 7-5 1-6 1-7 1-8 7-12";
  private static final List<List<Integer>> EXPECTED_WORD_ALIGNMENT = asList(
      asList(0), asList(2, 6), asList(), asList(3),
      asList(4, 5), asList(7), asList(1),
      asList(1), asList(1), asList(), asList(),
      asList(), asList(7));
  private static final double EXPECTED_SCORE = -17.0;
  private static final Map<String,Float> EXPECTED_FEATURES = new HashMap<>();
  private static final int EXPECTED_NBEST_LIST_SIZE = 8;
  static {
    EXPECTED_FEATURES.put("tm_glue_0", 1.0f);
    EXPECTED_FEATURES.put("tm_pt_0", -3.0f);
    EXPECTED_FEATURES.put("tm_pt_1", -3.0f);
    EXPECTED_FEATURES.put("tm_pt_2", -3.0f);
    EXPECTED_FEATURES.put("tm_pt_3", -3.0f);
    EXPECTED_FEATURES.put("tm_pt_4", -3.0f);
    EXPECTED_FEATURES.put("tm_pt_5", -3.0f);
    EXPECTED_FEATURES.put("OOV", 7.0f);
    EXPECTED_FEATURES.put("OOVPenalty", 0.0f);
  }

  @Before
  public void setUp() throws Exception {
    joshuaConfig = new JoshuaConfiguration();
    joshuaConfig.search_algorithm = "cky";
    joshuaConfig.mark_oovs = false;
    joshuaConfig.pop_limit = 100;
    joshuaConfig.use_unique_nbest = false;
    joshuaConfig.include_align_index = false;
    joshuaConfig.topN = 0;
    joshuaConfig.tms.add("thrax -owner pt -maxspan 20 -path resources/wa_grammar");
    joshuaConfig.tms.add("thrax -owner glue -maxspan -1 -path resources/grammar.glue");
    joshuaConfig.goal_symbol = "[GOAL]";
    joshuaConfig.default_non_terminal = "[X]";
    joshuaConfig.features.add("OOVPenalty");
    joshuaConfig.weights.add("tm_pt_0 1");
    joshuaConfig.weights.add("tm_pt_1 1");
    joshuaConfig.weights.add("tm_pt_2 1");
    joshuaConfig.weights.add("tm_pt_3 1");
    joshuaConfig.weights.add("tm_pt_4 1");
    joshuaConfig.weights.add("tm_pt_5 1");
    joshuaConfig.weights.add("tm_glue_0 1");
    joshuaConfig.weights.add("OOVPenalty 1");
    decoder = new Decoder(joshuaConfig, ""); // second argument (configFile
                                             // is not even used by the
                                             // constructor/initialize)
  }

  @After
  public void tearDown() throws Exception {
    decoder.cleanUp();
    decoder = null;
  }

  private Translation decode(String input) {
    Sentence sentence = new Sentence(input, 0, joshuaConfig);
    return decoder.decode(sentence);
  }

  @Test
  public void givenInput_whenRegularOutputFormat_thenExpectedOutput() {
    // GIVEN
    joshuaConfig.use_structured_output = false;
    joshuaConfig.outputFormat = "%s | %a ";

    // WHEN
    final String translation = decode(INPUT).toString().trim();

    // THEN
    assertEquals(EXPECTED_TRANSLATION + " | " + EXPECTED_WORD_ALIGNMENT_STRING, translation);
  }

  @Test
  public void givenInput_whenRegularOutputFormatWithTopN1_thenExpectedOutput() {
    // GIVEN
    joshuaConfig.use_structured_output = false;
    joshuaConfig.outputFormat = "%s | %e | %a | %c";
    joshuaConfig.topN = 1;

    // WHEN
    final String translation = decode(INPUT).toString().trim();

    // THEN
    assertEquals(EXPECTED_TRANSLATION + " | " + INPUT + " | " + EXPECTED_WORD_ALIGNMENT_STRING + String.format(" | %.3f", EXPECTED_SCORE),
        translation);
  }

  @Test
  public void givenInput_whenStructuredOutputFormatWithTopN0_thenExpectedOutput() {
    // GIVEN
    joshuaConfig.use_structured_output = true;
    joshuaConfig.topN = 0;

    // WHEN
    final Translation translation = decode(INPUT);
    final StructuredTranslation structuredTranslation = translation.getStructuredTranslations().get(0);
    final String translationString = structuredTranslation.getTranslationString();
    final List<String> translatedTokens = structuredTranslation.getTranslationTokens();
    final float translationScore = structuredTranslation.getTranslationScore();
    final List<List<Integer>> wordAlignment = structuredTranslation.getTranslationWordAlignments();
    final Map<String,Float> translationFeatures = structuredTranslation.getTranslationFeatures();

    // THEN
    assertTrue(translation.getStructuredTranslations().size() == 1);
    assertEquals(EXPECTED_TRANSLATION, translationString);
    assertEquals(EXPECTED_TRANSLATED_TOKENS, translatedTokens);
    assertEquals(EXPECTED_SCORE, translationScore, 0.00001);
    assertEquals(EXPECTED_WORD_ALIGNMENT, wordAlignment);
    assertEquals(wordAlignment.size(), translatedTokens.size());
    assertEquals(EXPECTED_FEATURES.entrySet(), translationFeatures.entrySet());
  }

  @Test
  public void givenInput_whenStructuredOutputFormatWithTopN1_thenExpectedOutput() {
    // GIVEN
    joshuaConfig.use_structured_output = true;
    joshuaConfig.topN = 1;

    // WHEN
    final Translation translation = decode(INPUT);
    final List<StructuredTranslation> structuredTranslations = translation.getStructuredTranslations();
    final StructuredTranslation structuredTranslation = structuredTranslations.get(0);
    final String translationString = structuredTranslation.getTranslationString();
    final List<String> translatedTokens = structuredTranslation.getTranslationTokens();
    final float translationScore = structuredTranslation.getTranslationScore();
    final List<List<Integer>> wordAlignment = structuredTranslation.getTranslationWordAlignments();
    final Map<String,Float> translationFeatures = structuredTranslation.getTranslationFeatures();

    // THEN
    assertTrue(structuredTranslations.size() == 1);
    assertEquals(EXPECTED_TRANSLATION, translationString);
    assertEquals(EXPECTED_TRANSLATED_TOKENS, translatedTokens);
    assertEquals(EXPECTED_SCORE, translationScore, 0.00001);
    assertEquals(EXPECTED_WORD_ALIGNMENT, wordAlignment);
    assertEquals(wordAlignment.size(), translatedTokens.size());
    assertEquals(EXPECTED_FEATURES.entrySet(), translationFeatures.entrySet());
  }

  @Test
  public void givenInput_whenStructuredOutputFormatWithKBest_thenExpectedOutput() {
    // GIVEN
    joshuaConfig.use_structured_output = true;
    joshuaConfig.topN = 100;

    // WHEN
    final Translation translation = decode(INPUT);
    final List<StructuredTranslation> structuredTranslations = translation.getStructuredTranslations();
    final StructuredTranslation viterbiTranslation = structuredTranslations.get(0);
    final StructuredTranslation lastKBest = structuredTranslations.get(structuredTranslations.size() - 1);

    // THEN
    assertEquals(structuredTranslations.size(), EXPECTED_NBEST_LIST_SIZE);
    assertTrue(structuredTranslations.size() > 1);
    assertEquals(EXPECTED_TRANSLATION, viterbiTranslation.getTranslationString());
    assertEquals(EXPECTED_TRANSLATED_TOKENS, viterbiTranslation.getTranslationTokens());
    assertEquals(EXPECTED_SCORE, viterbiTranslation.getTranslationScore(), 0.00001);
    assertEquals(EXPECTED_WORD_ALIGNMENT, viterbiTranslation.getTranslationWordAlignments());
    assertEquals(EXPECTED_FEATURES.entrySet(), viterbiTranslation.getTranslationFeatures().entrySet());
    // last entry in KBEST is all input words untranslated, should have 8 OOVs.
    assertEquals(INPUT, lastKBest.getTranslationString());
    assertEquals(-800.0, lastKBest.getTranslationFeatures().get("OOVPenalty"), 0.0001);

  }

  @Test
  public void givenEmptyInput_whenStructuredOutputFormat_thenEmptyOutput() {
    // GIVEN
    joshuaConfig.use_structured_output = true;

    // WHEN
    final Translation translation = decode("");
    final StructuredTranslation structuredTranslation = translation.getStructuredTranslations().get(0);
    final String translationString = structuredTranslation.getTranslationString();
    final List<String> translatedTokens = structuredTranslation.getTranslationTokens();
    final float translationScore = structuredTranslation.getTranslationScore();
    final List<List<Integer>> wordAlignment = structuredTranslation.getTranslationWordAlignments();

    // THEN
    assertEquals("", translationString);
    assertTrue(translatedTokens.isEmpty());
    assertEquals(0, translationScore, 0.00001);
    assertTrue(wordAlignment.isEmpty());
  }

  @Test
  public void givenOOVInput_whenStructuredOutputFormat_thenOOVOutput() {
    // GIVEN
    joshuaConfig.use_structured_output = true;
    final String input = "gabarbl";

    // WHEN
    final Translation translation = decode(input);
    final StructuredTranslation structuredTranslation = translation.getStructuredTranslations().get(0);
    final String translationString = structuredTranslation.getTranslationString();
    final List<String> translatedTokens = structuredTranslation.getTranslationTokens();
    final float translationScore = structuredTranslation.getTranslationScore();
    final List<List<Integer>> wordAlignment = structuredTranslation.getTranslationWordAlignments();

    // THEN
    assertEquals(input, translationString);
    assertTrue(translatedTokens.contains(input));
    assertEquals(-99.0, translationScore, 0.00001);
    assertTrue(wordAlignment.contains(asList(0)));
  }

  @Test
  public void givenEmptyInput_whenRegularOutputFormat_thenNewlineOutput() {
    // GIVEN
    joshuaConfig.use_structured_output = false;
    joshuaConfig.outputFormat = "%s";

    // WHEN
    final Translation translation = decode("");
    final String translationString = translation.toString();

    // THEN
    assertEquals("\n", translationString);
  }

}
