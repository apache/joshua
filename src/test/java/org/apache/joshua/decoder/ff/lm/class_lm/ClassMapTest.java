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
package org.apache.joshua.decoder.ff.lm.class_lm;

import static org.testng.Assert.assertEquals;

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.decoder.Decoder;
import org.apache.joshua.decoder.ff.lm.ClassMap;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


public class ClassMapTest {

  private static final int EXPECTED_CLASS_MAP_SIZE = 5140;

  @BeforeMethod
  public void setUp() {
    Decoder.resetGlobalState();
  }

  @AfterMethod
  public void tearDown() {
    Decoder.resetGlobalState();
  }

  @Test
  public void givenClassMapFile_whenClassMapRead_thenEntriesAreRead() {
    // GIVEN
    final String classMapFile = "./src/test/resources/lm/class_lm/class.map";

    // WHEN
    final ClassMap classMap = new ClassMap(classMapFile);

    // THEN
    assertEquals(classMap.size(), EXPECTED_CLASS_MAP_SIZE);
    assertEquals(
      Vocabulary.word(
        classMap.getClassID(
          Vocabulary.id("professionalism"))),
      "13");
    assertEquals(
      Vocabulary.word(
        classMap.getClassID(
          Vocabulary.id("convenience"))),
      "0");
  }

}
