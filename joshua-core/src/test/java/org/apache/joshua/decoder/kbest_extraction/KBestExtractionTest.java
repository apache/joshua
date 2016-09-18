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
package org.apache.joshua.decoder.kbest_extraction;

import static com.google.common.base.Charsets.UTF_8;
import static java.nio.file.Files.readAllBytes;
import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.joshua.decoder.Decoder;
import org.apache.joshua.decoder.Translation;
import org.apache.joshua.decoder.segment_file.Sentence;
import org.apache.joshua.util.io.KenLmTestUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;

/**
 * Reimplements the kbest extraction regression test.
 */
public class KBestExtractionTest {

  private static final String CONFIG = "src/test/resources/kbest_extraction/joshua.config";
  private static final String INPUT = "a b c d e";
  private static final Path GOLD_PATH = Paths.get("src/test/resources/kbest_extraction/output.scores.gold");
  private Decoder decoder = null;

  @BeforeMethod
  public void setUp() throws Exception {
    Config config = Decoder.getFlagsFromFile(new File(CONFIG))
        .withValue("output_format", ConfigValueFactory.fromAnyRef("%i ||| %s ||| %c"));
    KenLmTestUtil.Guard(() -> decoder = new Decoder(config));
  }

  @AfterMethod
  public void tearDown() throws Exception {
    decoder.cleanUp();
    decoder = null;
  }

  @Test
  public void givenInput_whenKbestExtraction_thenOutputIsAsExpected() throws IOException {
    final String translation = decode(INPUT).toString();
    final String gold = new String(readAllBytes(GOLD_PATH), UTF_8);
    assertEquals(translation, gold);
  }

  private Translation decode(String input) {
    final Sentence sentence = new Sentence(input, 0, decoder.getFlags());
    return decoder.decode(sentence);
  }

}
