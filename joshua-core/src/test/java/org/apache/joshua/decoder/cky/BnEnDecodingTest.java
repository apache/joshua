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

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import static com.typesafe.config.ConfigFactory.parseResources;
import static org.apache.joshua.decoder.cky.TestUtil.decodeList;
import static org.apache.joshua.decoder.cky.TestUtil.loadStringsFromFile;
import static org.testng.Assert.assertEquals;

import java.util.List;

import org.apache.joshua.decoder.Decoder;
import org.apache.joshua.decoder.Translation;
import org.apache.joshua.util.io.KenLmTestUtil;
import org.testng.annotations.DataProvider;

import com.typesafe.config.Config;

public class BnEnDecodingTest {

  private Decoder decoder;
  Translation translation = null;

  @BeforeMethod
  public void setUp() throws Exception {
    /*
     * TODO: @DataProvider doesn't seem to play well with our KenLM guard test, so we
     * should try to directly load a KenLM model here, to check that it works.
     */
    //    KenLmTestUtil.Guard(() -> decoder = new Decoder(joshuaConfig));
  }
  
  @DataProvider(name = "testFiles")
  public Object[][] lmFiles() {
    return new Object[][]{
      {"BnEnHieroTest.conf", "BnEn.in", "BnEnHieroTest.gold"},
      {"BnEnBerkeleyLMTest.conf", "BnEn.in", "BnEnBerkeleyLMTest.gold"},
      {"BnEnClassLMTest.conf" , "BnEn.in", "BnEnClassLMTest.gold"},
      {"BnEnPackedTest.conf", "BnEn.in", "BnEnPackedTest.gold"},
      {"BnEnSAMTTest.conf", "BnEn.in", "BnEnSAMTTest.gold"}
      };
  }
  
  @AfterMethod
  public void tearDown() throws Exception {
    if (decoder != null) {
      decoder.cleanUp();
      decoder = null;
    }
    translation = null;
  }

  @Test(dataProvider = "testFiles")
  public void givenBnEnInput_whenDecoding_thenScoreAndTranslationCorrect(String confFile, String inFile, String goldFile) throws Exception {
    // Given
    List<String> inputStrings = loadStringsFromFile(this.getClass().getResource(inFile).getFile());

    // When
    Config config = parseResources(this.getClass(), confFile)
        .withFallback(Decoder.getDefaultFlags());
    decoder = new Decoder(config);

    List<String> decodedStrings = decodeList(inputStrings, decoder);

    // Then
    List<String> goldStrings = loadStringsFromFile(this.getClass().getResource(goldFile).getFile());

//    System.err.println("\n\n\nTEST OUTPUT");
//    for (String line: decodedStrings)
//      System.err.println(line);

    assertEquals(decodedStrings, goldStrings);
  }
}
