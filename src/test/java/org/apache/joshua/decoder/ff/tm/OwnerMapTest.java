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

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class OwnerMapTest {

  @BeforeMethod
  public void setUp() throws Exception {
    OwnerMap.clear();
  }

  @AfterMethod
  public void tearDown() throws Exception {
    OwnerMap.clear();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void given_invalidId_thenThrowsException() {
    OwnerMap.getOwner(new OwnerId(3));
  }

  @Test
  public void givenOwner_whenRegisteringOwner_thenMappingIsCorrect() {
    // GIVEN
    String owner = "owner";

    // WHEN
    OwnerId id = OwnerMap.register(owner);
    OwnerId id2 = OwnerMap.register(owner);

    // THEN
    assertEquals(id, id2);
    assertEquals(owner, OwnerMap.getOwner(id));
  }

}
