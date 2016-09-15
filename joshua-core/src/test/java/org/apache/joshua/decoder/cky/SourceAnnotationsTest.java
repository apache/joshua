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
import org.apache.joshua.util.io.KenLmTestUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class SourceAnnotationsTest {

  private static final String INPUT = "mis[tag=ADJ;num=PL;class=OOV] amigos me llaman";
  private static final String GOLD_WITHOUT_ANNOTATIONS = "my friends call me ||| tm_pt_0=-3.000 tm_glue_0=3.000 lm_0=-11.974 OOVPenalty=0.000 WordPenalty=-2.606 ||| -7.650";
  private static final String GOLD_WITH_ANNOTATIONS = "my friends call me ||| tm_pt_0=-3.000 tm_glue_0=3.000 lm_0=-111.513 OOVPenalty=0.000 WordPenalty=-2.606 ||| -107.189";

  private static final String JOSHUA_CONFIG_PATH = "src/test/resources/decoder/source-annotations/joshua.config";

  private JoshuaConfiguration joshuaConfig;
  private Decoder decoder;

  @Test
  public void givenInput_whenNotUsingSourceAnnotations_thenOutputCorrect() throws Exception {
    setUp(false);
    String output = translate(INPUT, decoder, joshuaConfig);
    assertEquals(output.trim(), GOLD_WITHOUT_ANNOTATIONS);
  }

  @Test
  public void givenInput_whenUsingSourceAnnotations_thenOutputCorrect() throws Exception {
    setUp(true);
    String output = translate(INPUT, decoder, joshuaConfig);
    assertEquals(output.trim(), GOLD_WITH_ANNOTATIONS);
  }

  public void setUp(boolean sourceAnnotations) throws Exception {
    joshuaConfig = new JoshuaConfiguration();
    joshuaConfig.readConfigFile(JOSHUA_CONFIG_PATH);
    joshuaConfig.source_annotations = sourceAnnotations;
    KenLmTestUtil.Guard(() -> decoder = new Decoder(joshuaConfig, ""));
  }

  @AfterMethod
  public void tearDown() throws Exception {
    if (decoder != null) {
      decoder.cleanUp();
      decoder = null;
    }
  }

}
