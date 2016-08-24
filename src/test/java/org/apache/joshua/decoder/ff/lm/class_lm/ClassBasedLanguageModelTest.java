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
package org.apache.joshua.decoder.ff.lm.class_lm;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.decoder.Decoder;
import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.decoder.ff.FeatureVector;
import org.apache.joshua.decoder.ff.lm.LanguageModelFF;
import org.apache.joshua.decoder.ff.tm.OwnerMap;
import org.apache.joshua.decoder.ff.tm.Rule;
import org.apache.joshua.util.io.KenLmTestUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * This unit test relies on KenLM.  If the KenLM library is not found when the test is run all tests will be skipped.
 */
public class ClassBasedLanguageModelTest {

  private static final float WEIGHT = 0.5f;

  private LanguageModelFF ff;

  @BeforeMethod
  public void setUp() {
    Decoder.resetGlobalState();

    FeatureVector weights = new FeatureVector();
    weights.set("lm_0", WEIGHT);
    String[] args = { "-lm_type", "kenlm", "-lm_order", "9",
      "-lm_file", "src/test/resources/lm/class_lm/class_lm_9gram.gz",
      "-class_map", "src/test/resources/lm/class_lm/class.map" };

    JoshuaConfiguration config = new JoshuaConfiguration();
    KenLmTestUtil.Guard(() -> ff = new LanguageModelFF(weights, args, config));
  }

  @AfterMethod
  public void tearDown() {
    Decoder.resetGlobalState();
  }

  @Test
  public void givenLmDefinition_whenInitialized_thenInitializationIsCorrect() {
    assertTrue(ff.isClassLM());
    assertTrue(ff.isStateful());
  }

  @Test
  public void givenRuleWithSingleWord_whenGetRuleId_thenIsMappedToClass() {
    final int[] target = Vocabulary.addAll(new String[] { "professionalism" });
    final Rule rule = new Rule(0, null, target, new FeatureVector(), 0, OwnerMap.register(OwnerMap.UNKNOWN_OWNER));
    assertEquals(Vocabulary.word(ff.getRuleIds(rule)[0]), "13");
  }
}
