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
package org.apache.joshua.decoder.segment_file;

import org.testng.annotations.Test;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import static org.testng.Assert.*;

import org.apache.joshua.decoder.JoshuaConfiguration;

public class AlmostTooLongSentenceTest {
  private JoshuaConfiguration joshuaConfiguration;
  private String almostTooLongInput;
  private Sentence sentencePlusTarget;

  @BeforeMethod
  public void setUp() {
    joshuaConfiguration = new JoshuaConfiguration();
    almostTooLongInput = concatStrings(".", joshuaConfiguration.maxlen);
    sentencePlusTarget = new Sentence(this.almostTooLongInput + " ||| target side", 0,joshuaConfiguration);
  }

  @AfterMethod
  public void tearDown() {
  }

  @Test
  public void testConstructor() {
    Sentence sent = new Sentence("", 0,joshuaConfiguration);
    assertNotNull(sent);
  }

  @Test
  public void testEmpty() {
    assertTrue(new Sentence("", 0,joshuaConfiguration).isEmpty());
  }

  @Test
  public void testNotEmpty() {
    assertFalse(new Sentence("hello , world", 0, joshuaConfiguration).isEmpty());
  }

  /**
   * Return a string consisting of repeatedToken concatenated MAX_SENTENCE_NODES times.
   *
   * @param repeatedToken
   * @param repeatedTimes
   * @return
   */
  private String concatStrings(String repeatedToken, int repeatedTimes) {
    String result = "";
    for (int i = 0; i < repeatedTimes; i++) {
      result += repeatedToken;
    }
    return result;
  }

  @Test
  public void testAlmostButNotTooManyTokensSourceOnlyNotEmpty() {
    assertFalse(new Sentence(this.almostTooLongInput, 0, joshuaConfiguration).isEmpty());
  }

  @Test
  public void testAlmostButNotTooManyTokensSourceOnlyTargetNull() {
    assertNull(new Sentence(this.almostTooLongInput, 0, joshuaConfiguration).target);
  }

  @Test
  public void testAlmostButNotTooManyTokensSourceAndTargetTargetIsNotEmpty() {
    assertFalse(this.sentencePlusTarget.isEmpty());
  }

  @Test
  public void testAlmostButNotTooManyTokensSourceAndTargetTargetNull() {
    assertEquals(this.sentencePlusTarget.target, "target side");
  }

}
