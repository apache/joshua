/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.joshua.decoder.cky;

import static org.apache.joshua.decoder.cky.TestUtil.translate;
import static org.testng.Assert.assertEquals;

import java.util.List;

import org.apache.joshua.decoder.Decoder;
import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.util.io.KenLmTestUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class SparseFeatureTest {

  private JoshuaConfiguration joshuaConfig;
  private Decoder decoder;
  private String INPUT_STRING = "el chico";
  private String EXPECTED_OUTPUT = "0 ||| the boy ||| tm_pt_0=1.000 tm_glue_0=1.000 sparse_test_feature=1.000 svd=1.000 the_boy=1.000 ||| 1.000\n";

  @AfterMethod
  public void tearDown() throws Exception {
    if (decoder != null) {
      decoder.cleanUp();
      decoder = null;
    }
  }
  
  @DataProvider(name = "configurationFiles")
  public Object[][] configFiles() {
    return new Object[][]{{"src/test/resources/grammar/sparse-features/joshua.config"},
      {"src/test/resources/grammar/sparse-features/joshua-packed.config"}};
  }

  @Test(dataProvider = "configurationFiles")
  public void givenGrammar_whenDecoding_thenScoreAndTranslationCorrect(String configFile) throws Exception {
    configureDecoder(configFile);

    String decodedString = translate(INPUT_STRING, decoder, joshuaConfig);

    assertEquals(decodedString, EXPECTED_OUTPUT);
  }

  public void configureDecoder(String pathToConfig) throws Exception {
    joshuaConfig = new JoshuaConfiguration();
    joshuaConfig.readConfigFile(pathToConfig);
    KenLmTestUtil.Guard(() -> decoder = new Decoder(joshuaConfig, ""));
  }
}
