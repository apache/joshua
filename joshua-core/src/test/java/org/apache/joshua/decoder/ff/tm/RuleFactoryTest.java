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
package org.apache.joshua.decoder.ff.tm;

import static org.testng.Assert.*;

import java.util.Arrays;

import org.testng.annotations.Test;

public class RuleFactoryTest {
  
  @Test
  public void givenAlignmentString_whenParseAlignmentString_thenAlignmentsAreCorrect() {
    // GIVEN
    String alignmentString = "0-0 2-1 4-1 5-3 5-5";
    byte[] expectedAlignments = new byte[] {0, 0, 2, 1, 4, 1, 5, 3, 5, 5};
    
    // WHEN
    byte[] alignments = RuleFactory.parseAlignmentString(alignmentString);
    
    // THEN
    assertTrue(Arrays.equals(alignments, expectedAlignments));
  }

}
