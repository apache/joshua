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
package org.apache.joshua.decoder.ff.lm.berkeley_lm;

import static org.testng.Assert.assertEquals;

import org.apache.joshua.decoder.Decoder;
import org.apache.joshua.decoder.Translation;
import org.apache.joshua.decoder.segment_file.Sentence;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Replacement for test/lm/berkeley/test.sh regression test
 */

public class LMGrammarBerkeleyTest {

  private static final String INPUT = "the chat-rooms";
  private static final String EXPECTED_OUTPUT = "lm_0=-7.152632 glue_0=-2.000000\n";
  private static final String EXPECTED_OUTPUT_WITH_OOV = "lm_0_oov=0.000000 lm_0=-7.152632 glue_0=-2.000000\n";
  private static final Config DECODER_FLAGS = ConfigFactory.parseString("output_format=%f").withFallback(Decoder.getDefaultFlags());

  private Decoder decoder;

  @DataProvider(name = "languageModelFiles")
  public Object[][] lmFiles() {
    return new Object[][]{{"src/test/resources/berkeley_lm/lm"},
            {"src/test/resources/berkeley_lm/lm.gz"},
            {"src/test/resources/berkeley_lm/lm.berkeleylm"},
            {"src/test/resources/berkeley_lm/lm.berkeleylm.gz"}};
  }
  
  @BeforeMethod
  public void setUp() {
    Decoder.resetGlobalState();
  }

  @AfterMethod
  public void tearDown() throws Exception {
    decoder.cleanUp();
  }

  @Test(dataProvider = "languageModelFiles")
  public void verifyLM(String lmFile) {
    final Config config = ConfigFactory
        .parseString(
            String.format(
                "feature_functions=[{ class=LanguageModel, lm_type=berkeleylm, lm_order=2, lm_file=%s }]",
                lmFile))
        .withFallback(DECODER_FLAGS);
    decoder = new Decoder(config);
    final String translation = decode(INPUT).toString();
    assertEquals(translation, EXPECTED_OUTPUT);
  }

  private Translation decode(String input) {
    final Sentence sentence = new Sentence(input, 0, decoder.getDecoderConfig().getFlags());
    return decoder.decode(sentence);
  }

  @Test
  public void givenLmWithOovFeature_whenDecoder_thenCorrectFeaturesReturned() {
    final Config config = ConfigFactory
        .parseString("feature_functions=[{ class=LanguageModel, oov_feature=true, lm_type=berkeleylm, lm_order=2, lm_file=src/test/resources/berkeley_lm/lm }]")
        .withFallback(DECODER_FLAGS);
    decoder = new Decoder(config);
    final String translation = decode(INPUT).toString();
    assertEquals(translation, EXPECTED_OUTPUT_WITH_OOV);
  }

}
