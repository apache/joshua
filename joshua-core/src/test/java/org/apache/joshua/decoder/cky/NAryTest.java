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
import static org.apache.joshua.decoder.cky.TestUtil.decodeAndAssertDecodedOutputEqualsGold;

import org.apache.joshua.decoder.Decoder;
import org.apache.joshua.util.io.KenLmTestUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import com.typesafe.config.Config;

public class NAryTest {
  private Decoder decoder;

  @AfterMethod
  public void tearDown() throws Exception {
    if (decoder != null) {
      decoder.cleanUp();
      decoder = null;
    }
  }

  @Test
  public void givenInput_whenNAryDecoding_thenScoreAndTranslationCorrect() throws Exception {
    String inputPath = this.getClass().getResource("NAryTest.in").getFile();
    String goldPath = this.getClass().getResource("NAryTest.gold").getFile();
    Config config = parseResources(this.getClass(), "NAryTest.conf")
        .withFallback(Decoder.getDefaultFlags());
    KenLmTestUtil.Guard(() -> decoder = new Decoder(config));

    decodeAndAssertDecodedOutputEqualsGold(inputPath, decoder, goldPath);
  }

}
