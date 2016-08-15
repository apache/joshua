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
package org.apache.joshua.util;

import org.testng.Assert;
import org.testng.annotations.Test;

public class CacheTest {

  @Test
  public void test() {

    Cache<String,Integer> cache = new Cache<String,Integer>(5);

    cache.put("a", 1);
    cache.put("b", 2);
    cache.put("c", 3);
    cache.put("d", 4);
    cache.put("e", 5);

    Assert.assertTrue(cache.containsKey("a"));
    Assert.assertTrue(cache.containsKey("b"));
    Assert.assertTrue(cache.containsKey("c"));
    Assert.assertTrue(cache.containsKey("d"));
    Assert.assertTrue(cache.containsKey("e"));

    // Access the "a" element in the cache
    cache.get("a");

    // Now add a new element that exceeds the capacity of the cache
    cache.put("f", 6);

    Assert.assertTrue(cache.containsKey("a"));

  }

}
