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

import static com.typesafe.config.ConfigFactory.parseResources;
import static org.apache.joshua.decoder.cky.TestUtil.decodeList;
import static org.testng.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.joshua.decoder.Decoder;
import org.apache.joshua.util.io.KenLmTestUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;

/**
 * Ensures that derivations are unique for the phrase-based decoder.
 */
public class UniqueHypothesesTest {

  public static final String INPUT = "una estrategia republicana para obstaculizar la reelección de Obama";

  private Decoder decoder = null;
  
  @BeforeMethod
  public void setUp() throws Exception {
    Config config = parseResources(this.getClass(), "UniqueHypothesesTest.conf")
        .withFallback(Decoder.getDefaultFlags());
    KenLmTestUtil.Guard(() -> decoder = new Decoder(config));
  }

  @Test
  public void givenInputSentence_whenDecodingWithUniqueHypotheses_thenAllHypothesesUnique()
      throws Exception {
    
    List<String> decodedStrings = decodeList(Arrays.asList(new String[] { INPUT }), decoder);

    assertEquals(decodedStrings.size(), 300);

    // if all strings are unique than the set should have the same size as the
    // list
    Set<String> uniqueDecodedStrings = new HashSet<>(decodedStrings);
    assertEquals(decodedStrings.size(), uniqueDecodedStrings.size());
  }

  @AfterMethod
  public void tearDown() throws Exception {
    if (decoder != null) {
      decoder.cleanUp();
      decoder = null;
    }
  }

}
