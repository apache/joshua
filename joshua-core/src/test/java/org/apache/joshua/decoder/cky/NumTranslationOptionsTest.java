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

import static com.typesafe.config.ConfigFactory.parseResources;
import static org.apache.joshua.decoder.cky.TestUtil.decodeAndAssertDecodedOutputEqualsGold;

import org.apache.joshua.decoder.Decoder;
import org.apache.joshua.util.io.KenLmTestUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.typesafe.config.Config;

/**
 * Tests that num_translation_options is enforced for hierarchical decoders
 */
public class NumTranslationOptionsTest {
  private Decoder decoder;

  @AfterMethod
  public void tearDown() throws Exception {
    if (decoder != null) {
      decoder.cleanUp();
      decoder = null;
    }
  }

  @DataProvider(name = "testFiles")
  public Object[][] lmFiles() {
    return new Object[][] {
        { "NumTranslationOptionsTest.conf", "NumTranslationOptionsTest.in",
            "NumTranslationOptionsTest.gold" },
        { "NumTranslationOptionsNoDotChartTest.conf", "NumTranslationOptionsTest.in",
            "NumTranslationOptionsNoDotChartTest.gold" },
        { "NumTranslationOptionsPackedTest.conf", "NumTranslationOptionsTest.in",
            "NumTranslationOptionsPackedTest.gold" } };
  }

  @Test(dataProvider = "testFiles")
  public void givenInput_whenDecodingWithNumTranslationOptions_thenScoreAndTranslationCorrect(
      String confFile, String inFile, String goldFile) throws Exception {
    String inputPath = this.getClass().getResource(inFile).getFile();
    String goldPath = this.getClass().getResource(goldFile).getFile();
    Config config = parseResources(this.getClass(), confFile)
        .withFallback(Decoder.getDefaultFlags());
    KenLmTestUtil.Guard(() -> decoder = new Decoder(config));

    decodeAndAssertDecodedOutputEqualsGold(inputPath, decoder, goldPath);
  }

}
