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

import org.apache.joshua.decoder.JoshuaConfiguration;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import static org.testng.Assert.*;

public class SentenceTest {
  private String tooLongInput;
  private final JoshuaConfiguration joshuaConfiguration = new JoshuaConfiguration();
  
  

  @BeforeMethod
  public void setUp() {
    tooLongInput = concatTokens("*", joshuaConfiguration.maxlen * 2);
  }

  @AfterMethod
  public void tearDown() {
  }

  @Test
  public void testConstructor() {
    Sentence sent = new Sentence("", 0, joshuaConfiguration);
    assertNotNull(sent);
  }

  @Test
  public void testEmpty() {
    assertTrue(new Sentence("", 0, joshuaConfiguration).isEmpty());
  }

  @Test
  public void testNotEmpty() {
    assertFalse(new Sentence("hello , world", 0, joshuaConfiguration).isEmpty());
  }

  /**
   * Return a string consisting of repeatedToken concatenated MAX_SENTENCE_NODES times, joined by a
   * space.
   *
   * @param repeatedToken
   * @param repeatedTimes
   * @return
   */
  private String concatTokens(String repeatedToken, int repeatedTimes) {
    String result = "";
    for (int i = 0; i < repeatedTimes - 1; i++) {
      result += repeatedToken + " ";
    }
    result += repeatedToken;
    return result;
  }

  /**
   * The too long input sentence should be truncated from 799 to 202 characters
   * TODO is this a bug? maxlen is defined as 200 not 202 characters
   */
  @Test
  public void testTooManyTokensSourceTruncated() {
    assertTrue(new Sentence(this.tooLongInput, 0, joshuaConfiguration).length() == 202);
  }

  @Test
  public void testTooManyTokensSourceOnlyNotNull() {
    assertNotNull(new Sentence(this.tooLongInput, 0, joshuaConfiguration));
  }

  @Test
  public void testTooManyTokensSourceAndTargetIsEmpty() {
    Sentence sentence = new Sentence(this.tooLongInput + " ||| target side", 0, joshuaConfiguration);
    assertEquals(sentence.target, "");
  }

  @Test
  public void testTooManyTokensSourceAndTargetTruncated() {
    Sentence sentence = new Sentence(this.tooLongInput + " ||| target side", 0, joshuaConfiguration);
    assertTrue(sentence.length() == 202);
  }

  @Test
  public void testClearlyNotTooManyTokens() {
    // Concatenate MAX_SENTENCE_NODES, each shorter than the average length, joined by a space.
    String input = "token";
    assertFalse(new Sentence(input, 0, joshuaConfiguration).isEmpty());
  }

}
