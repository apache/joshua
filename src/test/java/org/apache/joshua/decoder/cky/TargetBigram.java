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
package org.apache.joshua.decoder.cky;

import static org.apache.joshua.decoder.cky.TestUtil.translate;
import static org.testng.Assert.assertEquals;

import org.apache.joshua.decoder.Decoder;
import org.apache.joshua.decoder.JoshuaConfiguration;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class TargetBigram {

  private static final String INPUT = "this is a test";
  private static final String GOLD_TOPN2 = "this is a test ||| tm_glue_0=4.000 TargetBigram_<s>_this=1.000 TargetBigram_UNK_</s>=1.000 TargetBigram_UNK_UNK=1.000 TargetBigram_is_UNK=1.000 TargetBigram_this_is=1.000 ||| 0.000";
  private static final String GOLD_TOPN3_THRESHOLD20 = "this is a test ||| tm_glue_0=4.000 TargetBigram_<s>_UNK=1.000 TargetBigram_UNK_</s>=1.000 TargetBigram_UNK_UNK=1.000 TargetBigram_UNK_a=1.000 TargetBigram_a_UNK=1.000 ||| 0.000";
  private static final String GOLD_THRESHOLD10 = "this is a test ||| tm_glue_0=4.000 TargetBigram_<s>_UNK=1.000 TargetBigram_UNK_</s>=1.000 TargetBigram_UNK_is=1.000 TargetBigram_a_UNK=1.000 TargetBigram_is_a=1.000 ||| 0.000";

  private static final String VOCAB_PATH = "src/test/resources/decoder/target-bigram/vocab";

  private JoshuaConfiguration joshuaConfig;
  private Decoder decoder;

  @Test
  public void givenInput_whenNotUsingSourceAnnotations_thenOutputCorrect() throws Exception {
    setUp("TargetBigram -vocab " + VOCAB_PATH + " -top-n 2");
    String output = translate(INPUT, decoder, joshuaConfig);
    assertEquals(output.trim(), GOLD_TOPN2);
  }

  @Test
  public void givenInput_whenUsingSourceAnnotations_thenOutputCorrect() throws Exception {
    setUp("TargetBigram -vocab " + VOCAB_PATH + " -top-n 3 -threshold 20");
    String output = translate(INPUT, decoder, joshuaConfig);
    assertEquals(output.trim(), GOLD_TOPN3_THRESHOLD20);
  }

  @Test
  public void givenInput_whenUsingSourceAnnotations_thenOutputCorrect2() throws Exception {
    setUp("TargetBigram -vocab " + VOCAB_PATH + " -threshold 10");
    String output = translate(INPUT, decoder, joshuaConfig);
    assertEquals(output.trim(), GOLD_THRESHOLD10);
  }

  public void setUp(String featureFunction) throws Exception {
    joshuaConfig = new JoshuaConfiguration();
    joshuaConfig.features.add(featureFunction);
    joshuaConfig.outputFormat = "%s ||| %f ||| %c";
    decoder = new Decoder(joshuaConfig, "");
  }

  @AfterMethod
  public void tearDown() throws Exception {
    decoder.cleanUp();
    decoder = null;
  }

}
