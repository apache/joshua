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

import static com.typesafe.config.ConfigFactory.parseResources;
import static com.typesafe.config.ConfigValueFactory.fromAnyRef;

import org.apache.joshua.decoder.Decoder;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;

/**
 * Ensures that the decoder trims inputs when and only when it should
 */
public class TooLongTest {
  private static final String INPUT1 = "as kingfishers draw fire";
  private static final String GOLD1 = "as kingfishers ||| glue_0=-2.000000 ||| 0.000";
  private static final String INPUT2 = "dragonflies draw flame";
  private static final String GOLD2 = "dragonflies ||| glue_0=-1.000000 ||| 0.000";
  private static final String INPUT3 = "(((as tumbled over rim in roundy wells stones ring";
  private static final String GOLD3 = "(((as tumbled over rim in roundy wells stones ||| glue_0=-8.000000 ||| 0.000";
  private static final String INPUT4 = "(((like each tucked string tells";
  private static final String GOLD4 = "|||  ||| 0.000";

  private Decoder decoder;
  
  @DataProvider(name = "params")
  public Object[][] lmFiles() {
    return new Object[][]{
      {INPUT1, 2, false, GOLD1},
      {INPUT2, 1, true, GOLD2},
      {INPUT3, 8, false, GOLD3},
      {INPUT4, 3, true, GOLD4}
    };
  }

  @Test(dataProvider = "params")
  public void producesCorrectOutput(String input, int maxlen, boolean latticeDecoding, String gold) throws Exception {
    setUp(maxlen, latticeDecoding);
    String output = translate(input, decoder);
    assertEquals(output.trim(), gold);
  }

  private void setUp(int maxLen, boolean latticeDecoding) throws Exception {
    Config config = Decoder.getDefaultFlags()
        .withValue("output_format", ConfigValueFactory.fromAnyRef("%s ||| %f ||| %c"))
        .withValue("maximum_sentence_length", ConfigValueFactory.fromAnyRef(maxLen))
        .withValue("lattice_decoding", ConfigValueFactory.fromAnyRef(latticeDecoding));

    decoder = new Decoder(config);
  }

  @AfterMethod
  public void tearDown() throws Exception {
    decoder.cleanUp();
    decoder = null;
  }
}
