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

import static org.testng.Assert.assertEquals;

import java.io.IOException;

import org.apache.joshua.decoder.Decoder;
import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.decoder.Translation;
import org.apache.joshua.decoder.segment_file.Sentence;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class LmOovFeatureTest {

  private static final String CONFIG = "src/test/resources/lm_oov/joshua.config";
  private static final String INPUT = "a chat-rooms full";
  // expecting 2 lm oovs ('a' & 'full') and 2 grammar OOVs ('chat-rooms' & 'full') and score -198.000
  private static final String EXPECTED_FEATURES = "tm_pt_0=-2.000 tm_glue_0=3.000 lm_0=-206.718 lm_0_oov=2.000 OOVPenalty=-200.000 | -198.000";

  private JoshuaConfiguration joshuaConfig = null;
  private Decoder decoder = null;

  @BeforeMethod
  public void setUp() throws Exception {
    joshuaConfig = new JoshuaConfiguration();
    joshuaConfig.readConfigFile(CONFIG);
    joshuaConfig.outputFormat = "%f | %c";
    decoder = new Decoder(joshuaConfig, "");
  }

  @AfterMethod
  public void tearDown() throws Exception {
    decoder.cleanUp();
    decoder = null;
  }

  @Test
  public void givenInputWithDifferentOovTypes_whenDecode_thenFeaturesAreAsExpected() throws IOException {
    final String translation = decode(INPUT).toString().trim();
    System.out.println(translation);
    assertEquals(translation, EXPECTED_FEATURES);
  }

  private Translation decode(String input) {
    final Sentence sentence = new Sentence(input, 0, joshuaConfig);
    return decoder.decode(sentence);
  }
  
  public static void main(String[] args) throws Exception {
    
    LmOovFeatureTest test = new LmOovFeatureTest();
    test.setUp();
    test.givenInputWithDifferentOovTypes_whenDecode_thenFeaturesAreAsExpected();
    test.tearDown();
  }
}
