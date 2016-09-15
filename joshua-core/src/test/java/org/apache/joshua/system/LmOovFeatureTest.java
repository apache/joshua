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

import java.io.File;
import java.io.IOException;

import org.apache.joshua.decoder.Decoder;
import org.apache.joshua.decoder.Translation;
import org.apache.joshua.decoder.segment_file.Sentence;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;

public class LmOovFeatureTest {

  private static final File CONFIG = new File("src/test/resources/lm_oov/joshua.config");
  private static final String INPUT = "a chat-rooms full";
  // expecting 2 lm oovs ('a' & 'full') and 2 grammar OOVs ('chat-rooms' & 'full') and score -198.000
  private static final String EXPECTED_FEATURES = "pt_0=-2.000000 lm_0_oov=2.000000 lm_0=-206.718124 glue_0=3.000000 OOVPenalty=-200.000000 | -198.000";
  
  private static final Config FLAGS = Decoder.createDecoderFlagsFromFile(CONFIG).withValue("output_format", ConfigValueFactory.fromAnyRef("%f | %c"));
  private Decoder decoder = null;

  @BeforeMethod
  public void setUp() throws Exception {
    decoder = new Decoder(FLAGS);
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
    final Sentence sentence = new Sentence(input, 0, decoder.getDecoderConfig().getFlags());
    return decoder.decode(sentence);
  }
  
  public static void main(String[] args) throws Exception {
    
    LmOovFeatureTest test = new LmOovFeatureTest();
    test.setUp();
    test.givenInputWithDifferentOovTypes_whenDecode_thenFeaturesAreAsExpected();
    test.tearDown();
  }
}
