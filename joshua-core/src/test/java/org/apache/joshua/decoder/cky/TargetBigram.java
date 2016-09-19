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

import static com.typesafe.config.ConfigFactory.parseString;
import static org.apache.joshua.decoder.cky.TestUtil.translate;
import static org.testng.Assert.assertEquals;

import org.apache.joshua.decoder.Decoder;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import com.typesafe.config.Config;

public class TargetBigram {

  private static final String INPUT = "this is a test";
  private static final String GOLD_TOPN2 = "this is a test ||| glue_0=-4.000000 TargetBigram_this_is=1.000000 TargetBigram_is_UNK=1.000000 TargetBigram_UNK_UNK=1.000000 TargetBigram_UNK_</s>=1.000000 TargetBigram_<s>_this=1.000000 ||| 0.000";
  private static final String GOLD_TOPN3_THRESHOLD20 = "this is a test ||| glue_0=-4.000000 TargetBigram_a_UNK=1.000000 TargetBigram_UNK_a=1.000000 TargetBigram_UNK_UNK=1.000000 TargetBigram_UNK_</s>=1.000000 TargetBigram_<s>_UNK=1.000000 ||| 0.000";
  private static final String GOLD_THRESHOLD10 = "this is a test ||| glue_0=-4.000000 TargetBigram_is_a=1.000000 TargetBigram_a_UNK=1.000000 TargetBigram_UNK_is=1.000000 TargetBigram_UNK_</s>=1.000000 TargetBigram_<s>_UNK=1.000000 ||| 0.000";

  private static final String VOCAB_PATH = "src/test/resources/decoder/target-bigram/vocab";
  private static final String CONF_TOPN2 = "output_format = %s ||| %f ||| %c \n feature_functions = [ { class = TargetBigram, vocab = "
      + VOCAB_PATH + ", top-n = 2 } ]";
  private static final String CONF_TOPN3_THRESHOLD20 = "output_format = %s ||| %f ||| %c \n feature_functions = [ { class = TargetBigram, vocab = "
      + VOCAB_PATH + ", top-n = 3, threshold = 20 } ]";
  private static final String CONF_THRESHOLD10 = "output_format = %s ||| %f ||| %c \n feature_functions = [ { class = TargetBigram, vocab = "
      + VOCAB_PATH + ", threshold = 10 } ]";

  private Decoder decoder;

  @Test
  public void givenInput_whenDecodingWithTargetBigramAndTopN2_thenOutputCorrect() {
    setUp(CONF_TOPN2);
    String output = translate(INPUT, decoder).trim();
    assertEquals(output, GOLD_TOPN2);
  }

  @Test
  public void givenInput_whenDecodingWithTargetBigramAndTopN3Threshold20_thenOutputCorrect() {
    setUp(CONF_TOPN3_THRESHOLD20);
    String output = translate(INPUT, decoder).trim();
    assertEquals(output, GOLD_TOPN3_THRESHOLD20);
  }

  @Test
  public void givenInput_whenDecodingWithTargetBigramThreshold10_thenOutputCorrect2() {
    setUp(CONF_THRESHOLD10);
    String output = translate(INPUT, decoder).trim();
    assertEquals(output, GOLD_THRESHOLD10);
  }

  public void setUp(String configuration) {
    Config config = parseString(configuration).withFallback(Decoder.getDefaultFlags());
    decoder = new Decoder(config);
  }

  @AfterMethod
  public void tearDown() throws Exception {
    decoder.cleanUp();
    decoder = null;
  }

}
