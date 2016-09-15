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

import static org.apache.joshua.decoder.cky.TestUtil.decodeList;
import static org.apache.joshua.decoder.cky.TestUtil.loadStringsFromFile;
import static org.testng.Assert.assertEquals;

import java.util.List;

import org.apache.joshua.decoder.Decoder;
import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.util.io.KenLmTestUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * Tests that num_translation_options is enforced for hierarchical decoders
 */
public class NumTranslationOptionsTest {
  private JoshuaConfiguration joshuaConfig;
  private Decoder decoder;

  @AfterMethod
  public void tearDown() throws Exception {
    if (decoder != null) {
      decoder.cleanUp();
      decoder = null;
    }
  }

  @Test
  public void givenInput_whenDecodingWithNumTranslationOptions3_thenScoreAndTranslationCorrect()
      throws Exception {
    // Given
    List<String> inputStrings = loadStringsFromFile(
        "src/test/resources/decoder/num_translation_options/input");

    // When
    configureDecoder("src/test/resources/decoder/num_translation_options/joshua.config", true);
    List<String> decodedStrings = decodeList(inputStrings, decoder, joshuaConfig);

    // Then
    List<String> goldStrings = loadStringsFromFile(
        "src/test/resources/decoder/num_translation_options/output.gold");
    assertEquals(decodedStrings, goldStrings);
  }

  @Test
  public void givenInput_whenDecodingWithNumTranslationOptions3AndNoDotChart_thenScoreAndTranslationCorrect()
      throws Exception {
    // Given
    List<String> inputStrings = loadStringsFromFile(
        "src/test/resources/decoder/num_translation_options/input");

    // When
    configureDecoder("src/test/resources/decoder/num_translation_options/joshua.config", false);
    List<String> decodedStrings = decodeList(inputStrings, decoder, joshuaConfig);

    // Then
    List<String> goldStrings = loadStringsFromFile(
        "src/test/resources/decoder/num_translation_options/output-no-dot-chart.gold");
    assertEquals(decodedStrings, goldStrings);
  }

  @Test
  public void givenInput_whenDecodingWithNumTranslationOptions3AndPacked_thenScoreAndTranslationCorrect()
      throws Exception {
    // Given
    List<String> inputStrings = loadStringsFromFile(
        "src/test/resources/decoder/num_translation_options/input");

    // When
    configureDecoder("src/test/resources/decoder/num_translation_options/joshua-packed.config",
        true);
    List<String> decodedStrings = decodeList(inputStrings, decoder, joshuaConfig);

    // Then
    List<String> goldStrings = loadStringsFromFile(
        "src/test/resources/decoder/num_translation_options/output-packed.gold");
    assertEquals(decodedStrings, goldStrings);
  }

  public void configureDecoder(String pathToConfig, boolean useDotChart) throws Exception {
    joshuaConfig = new JoshuaConfiguration();
    joshuaConfig.readConfigFile(pathToConfig);
    joshuaConfig.use_dot_chart = useDotChart;
    KenLmTestUtil.Guard(() -> decoder = new Decoder(joshuaConfig, ""));
  }
}
