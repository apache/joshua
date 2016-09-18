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

import static org.apache.joshua.decoder.cky.TestUtil.decodeList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.joshua.decoder.Decoder;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;

import static org.testng.Assert.assertEquals;

public class DoNotCrashTest {

  private Decoder decoder = null;

  private String[] NASTIES = { 
      "[]", "[X]", "|||", "|", "(((", "|| | |", "|| |", "| asdf|", "||", "| ?| test" 
  };

  @BeforeMethod
  public void setUp() throws Exception {
    Config config = Decoder.getDefaultFlags()
        .withValue("output_format", ConfigValueFactory.fromAnyRef("%s"));
    decoder = new Decoder(config);
  }

  @AfterMethod
  public void tearDown() throws Exception {
    decoder.cleanUp();
    decoder = null;
  }

  @Test
  public void givenProblematicInput_whenDecoding_thenNoCrash() throws IOException {
    List<String> inputs = new ArrayList<String>();
    for (String nasty: this.NASTIES)
      inputs.add(nasty);
    
    // When
    List<String> outputs = decodeList(inputs, decoder);

    // Then
    assertEquals(outputs, inputs);
  }
}
