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

/**
 * Ensures that the decoder trims inputs when and only when it should
 */
public class TooLongTest {
  private static final String INPUT1 = "as kingfishers draw fire";
  private static final String GOLD1 = "as kingfishers ||| tm_glue_0=2.000 ||| 0.000";
  private static final String INPUT2 = "dragonflies draw flame";
  private static final String GOLD2 = "dragonflies ||| tm_glue_0=1.000 ||| 0.000";
  private static final String INPUT3 = "(((as tumbled over rim in roundy wells stones ring";
  private static final String GOLD3 = "(((as tumbled over rim in roundy wells stones ||| tm_glue_0=8.000 ||| 0.000";
  private static final String INPUT4 = "(((like each tucked string tells";
  private static final String GOLD4 = "|||  ||| 0.000";

  private JoshuaConfiguration joshuaConfig;
  private Decoder decoder;

  @Test
  public void givenInput_whenMaxLen2_thenOutputCorrect() throws Exception {
    setUp(2, false);
    String output = translate(INPUT1, decoder, joshuaConfig);
    assertEquals(output.trim(), GOLD1);
  }

  @Test
  public void givenInput_whenMaxLen1AndLatticeDecoding_thenOutputCorrect() throws Exception {
    setUp(1, true);
    String output = translate(INPUT2, decoder, joshuaConfig);
    assertEquals(output.trim(), GOLD2);
  }

  @Test
  public void givenInput_whenMaxLen8_thenOutputCorrect() throws Exception {
    setUp(8, false);
    String output = translate(INPUT3, decoder, joshuaConfig);
    assertEquals(output.trim(), GOLD3);
  }

  @Test
  public void givenInput_whenMaxLen3AndLatticeDecoding_thenOutputCorrect() throws Exception {
    setUp(3, true);
    String output = translate(INPUT4, decoder, joshuaConfig);
    assertEquals(output.trim(), GOLD4);
  }

  public void setUp(int maxLen, boolean latticeDecoding) throws Exception {
    joshuaConfig = new JoshuaConfiguration();
    joshuaConfig.outputFormat = "%s ||| %f ||| %c";
    joshuaConfig.maxlen = maxLen;
    joshuaConfig.lattice_decoding = latticeDecoding;
    decoder = new Decoder(joshuaConfig, "");
  }

  @AfterMethod
  public void tearDown() throws Exception {
    decoder.cleanUp();
    decoder = null;
  }
}
