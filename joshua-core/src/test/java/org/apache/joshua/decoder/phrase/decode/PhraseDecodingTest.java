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
 package org.apache.joshua.decoder.phrase.decode;

import static org.testng.Assert.assertEquals;

import java.io.IOException;

import org.apache.joshua.decoder.Decoder;
import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.decoder.Translation;
import org.apache.joshua.decoder.segment_file.Sentence;
import org.apache.joshua.util.io.KenLmTestUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Reimplements the constrained phrase decoding test
 */
public class PhraseDecodingTest {

  private static final String CONFIG = "src/test/resources/phrase_decoder/config";
  private static final String INPUT = "una estrategia republicana para obstaculizar la reelección de Obama";
  private static final String OUTPUT = "0 ||| a strategy republican to hinder reelection Obama ||| tm_pt_0=-9.702 tm_pt_1=-10.800 tm_pt_2=-7.543 tm_pt_3=-8.555 lm_0=-19.117 OOVPenalty=0.000 WordPenalty=-3.040 Distortion=0.000 PhrasePenalty=5.000 ||| -7.496";
  private static final String OUTPUT_WITH_ALIGNMENTS = "0 ||| a strategy |0-1| republican |2-2| to hinder |3-4| reelection |5-6| Obama |7-8| ||| tm_pt_0=-9.702 tm_pt_1=-10.800 tm_pt_2=-7.543 tm_pt_3=-8.555 lm_0=-19.117 OOVPenalty=0.000 WordPenalty=-3.040 Distortion=0.000 PhrasePenalty=5.000 ||| -7.496";
  
  private JoshuaConfiguration joshuaConfig = null;
  private Decoder decoder = null;

  @BeforeMethod
  public void setUp() throws Exception {
    joshuaConfig = new JoshuaConfiguration();
    joshuaConfig.readConfigFile(CONFIG);
    KenLmTestUtil.Guard(() -> decoder = new Decoder(joshuaConfig, ""));
  }

  @AfterMethod
  public void tearDown() throws Exception {
    decoder.cleanUp();
    decoder = null;
  }

  @Test(enabled = true)
  public void givenInput_whenPhraseDecoding_thenOutputIsAsExpected() throws IOException {
    final String translation = decode(INPUT).toString().trim();
    final String gold = OUTPUT;
    assertEquals(translation, gold);
  }
  
  @Test(enabled = false)
  public void givenInput_whenPhraseDecodingWithAlignments_thenOutputHasAlignments() throws IOException {
    final String translation = decode(INPUT).toString().trim();
    final String gold = OUTPUT_WITH_ALIGNMENTS;
    assertEquals(translation, gold);
  }
  
  @Test(enabled = true)
  public void givenInput_whenPhraseDecoding_thenInputCanBeRetrieved() throws IOException {
    String outputFormat = joshuaConfig.outputFormat;
    joshuaConfig.outputFormat = "%e";
    final String translation = decode(INPUT).toString().trim();
    joshuaConfig.outputFormat = outputFormat;
    final String gold = INPUT;
    assertEquals(translation, gold);
  }

  private Translation decode(String input) {
    final Sentence sentence = new Sentence(input, 0, joshuaConfig);
//    joshuaConfig.setVerbosity(2);
    return decoder.decode(sentence);
  }

}
