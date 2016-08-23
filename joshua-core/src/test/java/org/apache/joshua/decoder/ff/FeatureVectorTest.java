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
package org.apache.joshua.decoder.ff;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

public class FeatureVectorTest {
    
    private static final FeatureVector INIT =
        new FeatureVector(ImmutableMap.of(0, 0.1f, 1, 500.0f));
    
    @Test
    public void givenFeatureVector_whenRequestingAbsentValue_thenDefaultValue() {
        FeatureVector v = new FeatureVector(0);
        assertEquals(v.getOrDefault(0), 0.0f);
    }
    
    @Test
    public void givenFeatureVector_whenCopyConstructor_thenIsCopied() {
        // GIVEN
        FeatureVector newVector = new FeatureVector(INIT);
        
        // WHEN
        newVector.add(0, 14f);
        
        // THEN
        assertEquals(INIT.get(0), 0.1f);
        assertEquals(newVector.get(0), 14.1f);
    }
    
    @Test
    public void givenFeatureVector_whenElementWiseAddition_thenEntriesAreCorrect() {
        // GIVEN
        FeatureVector v = new FeatureVector(INIT);
        
        // WHEN
        v.add(1, 500f);
        v.add(2, -1f);
        
        // THEN
        assertEquals(v.get(1), 1000f);
        assertEquals(v.get(2), -1f);
    }
    
    @Test
    public void givenFeatureVector_whenVectorAddition_thenVectorsAreCorrect() {
        // GIVEN
        FeatureVector v = new FeatureVector(INIT);
        
        // WHEN
        v.addInPlace(INIT);
        
        // THEN
        assertTrue(v.containsKey(0));
        assertTrue(v.containsKey(1));
        assertEquals(v.getOrDefault(0), 0.2f);
        assertEquals(v.getOrDefault(1), 1000f);
    }
    
    @Test
    public void givenFeatureVector_whenPrune_thenCorrectEntriesAreRemoved() {
        // GIVEN
        FeatureVector v = new FeatureVector(INIT);
        v.add(2, 0.0001f);
        
        // WHEN
        v.prune(0.001f).size();
        
        // THEN
        assertFalse(v.containsKey(2));
    }
    
    @Test
    public void givenFeatureVector_whenInnerProduct_thenResultIsCorrect() {
        // GIVEN
        FeatureVector v = new FeatureVector(INIT);
        v.put(2, 12f);
        float expectedDotProduct = 0.0f;
        for (float value : v.values()) {
            expectedDotProduct += value * value;
        }
        
        // WHEN
        float dotProduct = v.innerProduct(v);
        
        // THEN
        assertTrue(Math.abs(dotProduct - expectedDotProduct) < 0.00001);
    }

    @Test
    public void givenTwoFeatureVectors_thenEqualityIsCorrect() {
        // GIVEN
        FeatureVector v1 = new FeatureVector(INIT);
        FeatureVector v2 = new FeatureVector(INIT);
        assertEquals(v1, v2);
    }
    
}
