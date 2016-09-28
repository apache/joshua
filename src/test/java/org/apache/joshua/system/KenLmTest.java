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

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.decoder.LmPool;
import org.apache.joshua.decoder.ff.lm.KenLM;
import org.apache.joshua.util.io.KenLmTestUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.apache.joshua.corpus.Vocabulary.registerLanguageModel;
import static org.apache.joshua.corpus.Vocabulary.unregisterLanguageModels;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;

/**
 * KenLM JNI interface tests.
 * Loads libken.{so,dylib}.
 * If run in Eclipse, add -Djava.library.path=./lib to JVM arguments
 * of the run configuration.
 */

public class KenLmTest {

  private static final String LANGUAGE_MODEL_PATH = "src/test/resources/kenlm/oilers.kenlm";
  private KenLM kenLm;

  @Test
  public void givenKenLm_whenQueryingForNgramProbability_thenProbIsCorrect() {
    // GIVEN
    KenLmTestUtil.Guard(() -> kenLm = new KenLM(3, LANGUAGE_MODEL_PATH));

    int[] words = Vocabulary.addAll("Wayne Gretzky");
    registerLanguageModel(kenLm);

    // WHEN
    float probability = kenLm.prob(words);

    // THEN
    assertEquals("Found the wrong probability for 2-gram \"Wayne Gretzky\"", -0.99f, probability,
            Float.MIN_VALUE);
  }

  @Test
  public void givenKenLm_whenQueryingForNgramProbability_thenIdAndStringMethodsReturnTheSame() {
    // GIVEN
    KenLmTestUtil.Guard(() -> kenLm = new KenLM(LANGUAGE_MODEL_PATH));

    registerLanguageModel(kenLm);
    String sentence = "Wayne Gretzky";
    String[] words = sentence.split("\\s+");
    int[] ids = Vocabulary.addAll(sentence);

    // WHEN
    float prob_string = kenLm.prob(words);
    float prob_id = kenLm.prob(ids);

    // THEN
    assertEquals("ngram probabilities differ for word and id based n-gram query", prob_string, prob_id,
            Float.MIN_VALUE);
  }

  @Test
  public void givenKenLm_whenQueryingWithState_thenStateAndProbReturned() {
    // GIVEN
    KenLmTestUtil.Guard(() -> kenLm = new KenLM(LANGUAGE_MODEL_PATH));

    registerLanguageModel(kenLm);
    String sentence = "Wayne Gretzky";
    String[] words = sentence.split("\\s+");
    int[] ids = Vocabulary.addAll(sentence);
    long[] longIds = new long[ids.length];

    for (int i = 0; i < words.length; i++) {
      longIds[i] = Vocabulary.id(words[i]);
    }

    // WHEN
    KenLM.StateProbPair result;
    try (LmPool poolPointer = kenLm.createLMPool()) {
      result = kenLm.probRule(longIds, poolPointer);
    }

    // THEN
    assertThat(result, is(notNullValue()));
    assertThat(result.state.getState(), is(1L));
    assertThat(result.prob, is(-3.7906885f));
  }

  @Test
  public void givenKenLm_whenIsKnownWord_thenReturnValuesAreCorrect() {
    KenLmTestUtil.Guard(() -> kenLm = new KenLM(LANGUAGE_MODEL_PATH));
    assertTrue(kenLm.isKnownWord("Wayne"));
    assertFalse(kenLm.isKnownWord("Wayne2222"));
  }

  @BeforeMethod
  public void setUp() throws Exception {
    Vocabulary.clear();
    unregisterLanguageModels();
  }

  @AfterMethod
  public void tearDown() throws Exception {
    Vocabulary.clear();
    unregisterLanguageModels();
  }
}
