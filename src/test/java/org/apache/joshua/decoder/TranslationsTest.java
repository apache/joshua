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
package org.apache.joshua.decoder;

import static org.testng.Assert.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeTest;
import org.apache.joshua.decoder.io.TranslationRequestStream;
import org.testng.annotations.AfterTest;

public class TranslationsTest {
  private final JoshuaConfiguration joshuaConfiguration = new JoshuaConfiguration();
  @BeforeTest
  public void beforeTest() {
  }

  @AfterTest
  public void afterTest() {
  }


  @Test(enabled = false)
  public void Translations() {
    throw new RuntimeException("Test not implemented");
  }

  /**
   * Test method for {@link joshua.decoder.io.TranslationRequest#next()}.
   */
  @Test(enabled = false)
  public void testNext() {
    fail("Not yet implemented");
  }

  @Test(enabled = false)
  public void iterator() {
    throw new RuntimeException("Test not implemented");
  }

  // @Test(expectedExceptions = TestException.class)
  @Test(enabled = false)
  public void next() {
    byte[] data = "1\n2\n".getBytes();
    ByteArrayInputStream input = new ByteArrayInputStream(data);
    TranslationRequestStream request = new TranslationRequestStream(
        new BufferedReader(new InputStreamReader(input, Charset.defaultCharset())), joshuaConfiguration);
    Translations translations = new Translations(request);
    assertEquals(translations.next().getSourceSentence().source(), "1");
    // Remove the next two.
    assertEquals(translations.next().getSourceSentence().source(), "2");
    // Should throw exception
    translations.next();
    translations.next();
  }

  @Test(enabled = false)
  public void record() {
    throw new RuntimeException("Test not implemented");
  }

  @Test(enabled = false)
  public void remove() {
    throw new RuntimeException("Test not implemented");
  }
}
