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

import static com.typesafe.config.ConfigFactory.parseResources;
import static org.testng.Assert.assertEquals;

import java.io.IOException;

import org.apache.joshua.decoder.Decoder;
import org.apache.joshua.decoder.Translation;
import org.apache.joshua.decoder.segment_file.Sentence;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;

/**
 * Reimplements the constrained phrase decoding test
 */
public class PhraseDecodingTest {

  private static final String CONFIG = "PhraseDecodingTest.conf";
  private static final String INPUT = "una estrategia republicana para obstaculizar la reelección de Obama";
  private static final String OUTPUT = "0 ||| a strategy republican to hinder reelection Obama ||| pt_3=-8.555386 pt_2=-7.542729 pt_1=-10.799793 pt_0=-9.702445 lm_0=-19.116861 WordPenalty=-3.040061 PhrasePenalty=5.000000 Distortion=0.000000 ||| -7.496"; 
  private static final String OUTPUT_WITH_ALIGNMENTS = "0 ||| a strategy |0-1| republican |2-2| to hinder |3-4| reelection |5-6| Obama |7-8| ||| Distortion=0.000000 WordPenalty=-3.040061 PhrasePenalty=5.000000 pt_0=-9.702445 pt_1=-10.799793 pt_2=-7.542729 pt_3=-8.555386 lm_0=-19.116861 ||| -7.496";
  
  private Decoder decoder = null;

  @BeforeMethod
  public void setUp() throws Exception {
    Config config = parseResources(this.getClass(), CONFIG)
        .withFallback(Decoder.getDefaultFlags());
//    KenLmTestUtil.Guard(() -> decoder = new Decoder(config));
      decoder = new Decoder(config);
  }

  @AfterMethod
  public void tearDown() throws Exception {
    decoder.cleanUp();
    decoder = null;
  }

  @Test
  public void givenInput_whenPhraseDecoding_thenOutputIsAsExpected() throws IOException {
    final String translation = decode(INPUT, "%i ||| %s ||| %f ||| %c").toString().trim();
    final String gold = OUTPUT;
    assertEquals(translation, gold);
  }
  
  /**
   * Phrase alignment output is currently not available, has been removed until we refactor the
   * output (MJP, Sept. 2016)
   * 
   * @throws IOException
   */
  @Test(enabled = false)
  public void givenInput_whenPhraseDecodingWithAlignments_thenOutputHasAlignments() throws IOException {
    final String translation = decode(INPUT, "%i ||| %s ||| %f ||| %c").toString().trim();
    final String gold = OUTPUT_WITH_ALIGNMENTS;
    assertEquals(translation, gold);
  }
  
  @Test(enabled = false)
  public void givenInput_whenPhraseDecoding_thenInputCanBeRetrieved() throws IOException {
    final String translation = decode(INPUT, "%e").toString().trim();
    final String gold = INPUT;
    assertEquals(translation, gold);
  }

  private Translation decode(String input, String outputFormat) {
    final Config flags = decoder.getFlags().withValue("output_format", ConfigValueFactory.fromAnyRef(outputFormat));
    final Sentence sentence = new Sentence(input, 0, flags);
    return decoder.decode(sentence);
  }

}
