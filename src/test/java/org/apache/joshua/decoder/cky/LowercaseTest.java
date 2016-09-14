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

public class LowercaseTest {

  private static final String INPUT_ALL_UPPERCASED = "ELLA";
  private static final String INPUT_CAPITALIZED = "Ella";

  private static final String GOLD_UNTRANSLATED_ALL_UPPERCASED = "ELLA";
  private static final String GOLD_LOWERCASED = "she";
  private static final String GOLD_CAPITALIZED = "She";
  private static final String GOLD_ALL_UPPERCASED = "SHE";
  
  private static final String JOSHUA_CONFIG_PATH = "src/test/resources/decoder/lowercaser/joshua.config";

  private JoshuaConfiguration joshuaConfig;
  private Decoder decoder;

  /**
   * No match in phrase table (only contains ella), therefore passed through
   * untranslated.
   * @throws Exception 
   */
  @Test
  public void givenAllUppercasedInput_whenNotLowercasing_thenLowercasedRuleNotFound() throws Exception {
    setUp(false, false, false);
    String output = translate(INPUT_ALL_UPPERCASED, decoder, joshuaConfig);
    assertEquals(output.trim(), GOLD_UNTRANSLATED_ALL_UPPERCASED);
  }
  
  /**
   * Match in phrase table (only contains ella), therefore translated.
   * @throws Exception
   */
  @Test
  public void givenAllUppercasedInput_whenLowercasing_thenLowercasedRuleFound() throws Exception {
    setUp(true, false, false);
    String output = translate(INPUT_ALL_UPPERCASED, decoder, joshuaConfig);
    assertEquals(output.trim(), GOLD_LOWERCASED);
  }
  
  /**
   * Matches phrase table, not capitalized because projected from first word of sentence
   * @throws Exception
   */
  @Test
  public void givenCapitalizedInput_whenLowercasingAndProjecting_thenLowercased() throws Exception {
    setUp(true, true, false);
    String output = translate(INPUT_CAPITALIZED, decoder, joshuaConfig);
    assertEquals(output.trim(), GOLD_LOWERCASED);
  }
  
  /**
   * Matches phrase table, capitalized because of output-format
   * @throws Exception
   */
  @Test
  public void givenCapitalizedInput_whenLowercasingAndOutputFormatCapitalization_thenCapitalized() throws Exception {
    setUp(true, true, true);
    String output = translate(INPUT_CAPITALIZED, decoder, joshuaConfig);
    assertEquals(output.trim(), GOLD_CAPITALIZED);
  }
  
  /**
   * Matches phrase table, capitalized because of output-format
   * @throws Exception
   */
  @Test
  public void givenAllUppercasedInput_whenLowercasingAndProjecting_thenAllUppercased() throws Exception {
    setUp(true, true, false);
    String output = translate(INPUT_ALL_UPPERCASED, decoder, joshuaConfig);
    assertEquals(output.trim(), GOLD_ALL_UPPERCASED);
  }

  public void setUp(boolean lowercase, boolean projectCase, boolean capitalize) throws Exception {
    joshuaConfig = new JoshuaConfiguration();
    joshuaConfig.readConfigFile(JOSHUA_CONFIG_PATH);
    joshuaConfig.lowercase = lowercase;
    joshuaConfig.project_case = projectCase;
    joshuaConfig.outputFormat = capitalize ? "%S" : "%s";
    decoder = new Decoder(joshuaConfig, "");
  }
  
  @AfterMethod
  public void tearDown() throws Exception {
    decoder.cleanUp();
    decoder = null;
  }
  
}
