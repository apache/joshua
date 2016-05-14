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
package org.apache.joshua.decoder.io;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.apache.joshua.decoder.JoshuaConfiguration;

import org.testng.annotations.*;
import static org.testng.Assert.*;
import static org.mockito.Mockito.*;

/**
 * This class verifies the following behaviors:
 * 
 * - A blank input, i.e. "", does not cause a translation to be created.
 * 
 * - A non-blank input that is not followed by a newline, e.g. "1", causes a translation to be
 * created.
 * 
 * - An input that contains whitespace or nothing followed by a newline causes a translation to be
 * created, with "" as the source.
 */
public class TranslationRequestTest {

  private final JoshuaConfiguration joshuaConfiguration = new JoshuaConfiguration();
  @BeforeMethod
  public void createTranslationRequest() throws Exception {
  }

  /**
   * @throws java.lang.Exception
   */
  @BeforeMethod
  protected void setUp() throws Exception {
  }

  /**
   * @throws java.lang.Exception
   */
  @AfterMethod
  protected void tearDown() throws Exception {
  }

  /**
   * Test method for {@link joshua.decoder.io.TranslationRequest#TranslationRequest(java.io.InputStream)}.
   */
  @Test(enabled = false)
  public void testTranslationRequest() {
    fail("Not yet implemented");
  }

  /**
   * Test method for {@link joshua.decoder.io.TranslationRequest#size()}.
   */
  @Test(enabled = true)
  public void testSize_uponConstruction() {
    InputStream in = mock(InputStream.class);
    TranslationRequestStream request = new TranslationRequestStream(
        new BufferedReader(new InputStreamReader(in, Charset.defaultCharset())), joshuaConfiguration);
    assertEquals(request.size(), 0);
  }

  /**
   * Test method for {@link joshua.decoder.io.TranslationRequest#size()}.
   * @throws Exception 
   */
  @Test(enabled = true)
  public void testSize_1() throws Exception {
    byte[] data = "1".getBytes();
    ByteArrayInputStream input = new ByteArrayInputStream(data);
    TranslationRequestStream request = new TranslationRequestStream(
        new BufferedReader(new InputStreamReader(input, Charset.defaultCharset())), joshuaConfiguration);
    request.next();
    assertEquals(request.size(), 1);
  }

  /**
   * Test method for {@link joshua.decoder.io.TranslationRequest#size()}.
   * @throws Exception 
   */
  @Test(enabled = true)
  public void testSize_newline() throws Exception {
    byte[] data = "\n".getBytes();
    ByteArrayInputStream input = new ByteArrayInputStream(data);
    TranslationRequestStream request = new TranslationRequestStream(
        new BufferedReader(new InputStreamReader(input, Charset.defaultCharset())), joshuaConfiguration);
    request.next();
    assertEquals(request.size(), 1);
  }

  /**
   * Test method for {@link joshua.decoder.io.TranslationRequest#size()}.
   * @throws Exception 
   */
  @Test(enabled = true)
  public void testSize_2newlines() throws Exception {
    byte[] data = "\n\n".getBytes();
    ByteArrayInputStream input = new ByteArrayInputStream(data);
    TranslationRequestStream request = new TranslationRequestStream(
        new BufferedReader(new InputStreamReader(input, Charset.defaultCharset())), joshuaConfiguration);
    request.next();
    request.next();
    assertEquals(request.size(), 2);
  }

  /**
   * Test method for {@link joshua.decoder.io.TranslationRequest#next()}.
   * @throws Exception 
   */
  @Test(enabled = true)
  public void testNext_2Newlines() throws Exception {
    byte[] data = "\n\n".getBytes();
    ByteArrayInputStream input = new ByteArrayInputStream(data);
    TranslationRequestStream request = new TranslationRequestStream(
        new BufferedReader(new InputStreamReader(input, Charset.defaultCharset())), joshuaConfiguration);
    assertEquals(request.next().source(), "");
    assertEquals(request.next().source(), "");
  }

  /**
   * Test method for {@link joshua.decoder.io.TranslationRequest#remove()}.
   */
  @Test(enabled = false)
  public void testRemove() {
    fail("Not yet implemented");
  }

}
