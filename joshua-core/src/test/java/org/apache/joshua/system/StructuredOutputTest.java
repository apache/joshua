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

import static com.typesafe.config.ConfigFactory.parseString;
import static com.typesafe.config.ConfigValueFactory.fromAnyRef;

import java.util.Arrays;
import java.util.List;

import org.apache.joshua.decoder.Decoder;
import org.apache.joshua.decoder.Translation;
import org.apache.joshua.decoder.segment_file.Sentence;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.typesafe.config.Config;

/**
 * Integration test for the complete Joshua decoder using a toy grammar that translates
 * a bunch of capital letters to lowercase letters. Rules in the test grammar
 * drop and generate additional words and simulate reordering of rules, so that
 * proper extraction of word alignments can be tested.
 *
 * @author fhieber
 */
public class StructuredOutputTest {

  private Decoder decoder = null;
  private Translation translation = null;
  private static final String input = "A K B1 U Z1 Z2 B2 C";
  private static final String expectedTranslation = "a b n1 u z c1 k1 k2 k3 n1 n2 n3 c2";
  private static final String expectedWordAlignmentString = "0-0 2-1 6-1 3-3 4-4 5-4 7-5 1-6 1-7 1-8 7-12";
  private static final List<List<Integer>> expectedWordAlignment = Arrays.asList(
      Arrays.asList(0), Arrays.asList(2, 6), Arrays.asList(), Arrays.asList(3),
      Arrays.asList(4, 5), Arrays.asList(7), Arrays.asList(1),
      Arrays.asList(1), Arrays.asList(1), Arrays.asList(), Arrays.asList(),
      Arrays.asList(), Arrays.asList(7));
  private static final double expectedScore = -17.0;

  @BeforeMethod
  public void setUp() throws Exception {
    Config weights = parseString(
        "weights = {pt_0=-1, pt_1=-1, pt_2=-1, pt_3=-1, pt_4=-1, pt_5=-1, glue_0=-1, OOVPenalty=2}");
    Config features = parseString("feature_functions = [{class=OOVPenalty}]");
    Config grammars = parseString("grammars=[{class=TextGrammar, owner=pt, span_limit=20, path=src/test/resources/wa_grammar},"
        + "{class=TextGrammar, owner=glue, span_limit=-1, path=src/test/resources/grammar.glue}]");
    Config flags = weights
        .withFallback(features)
        .withFallback(grammars)
        .withFallback(Decoder.getDefaultFlags())
        .withValue("top_n", fromAnyRef(0))
        .withValue("use_unique_nbest", fromAnyRef(false))
        .withValue("output_format", fromAnyRef("%s | %a"));
    decoder = new Decoder(flags);
  }

  @AfterMethod
  public void tearDown() throws Exception {
    decoder.cleanUp();
    decoder = null;
    translation = null;
  }

  private Translation decode(String input, Config flags) {
    Sentence sentence = new Sentence(input, 0, flags);
    return decoder.decode(sentence);
  }

  @Test
  public void test() {

    // test standard output
    translation = decode(input,
        decoder.getFlags().withValue("use_structured_output", fromAnyRef(false)));
    Assert.assertEquals(translation.toString().trim(), expectedTranslation + " | " + expectedWordAlignmentString);

    // test structured output
    translation = decode(input, decoder.getFlags().withValue("use_structured_output", fromAnyRef(true)));
    Assert.assertEquals(translation.getStructuredTranslations().get(0).getTranslationString(), expectedTranslation);
    Assert.assertEquals(translation.getStructuredTranslations().get(0).getTranslationTokens(), Arrays.asList(expectedTranslation.split("\\s+")));
    Assert.assertEquals(translation.getStructuredTranslations().get(0).getTranslationScore(), expectedScore, 0.00001);
    Assert.assertEquals(translation.getStructuredTranslations().get(0).getTranslationWordAlignments(), expectedWordAlignment);
    Assert.assertEquals(translation.getStructuredTranslations().get(0).getTranslationWordAlignments().size(), translation
        .getStructuredTranslations().get(0).getTranslationTokens().size());
  }
}
