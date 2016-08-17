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

import static java.util.stream.Collectors.joining;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Simplified version of a sparse feature vector.
 * @author fhieber
 */
public class FeatureVector extends AbstractMap<Integer, Float> {

  private static final float DEFAULT_VALUE = 0.0f;
  private final Map<Integer, Float> internalMap;

  public FeatureVector(int size) {
    internalMap = new HashMap<Integer, Float>(size);
  }

  /**
   * Copy constructor
   */
  public FeatureVector(Map<Integer, Float> m) {
    internalMap = new HashMap<Integer, Float>(m);
  }
  
  /**
   * Returns a Map of feature names to values.
   * @return map from strings to floats
   */
  public Map<String, Float> toStringMap() {
    final Map<String, Float> result = new HashMap<>(this.size());
    for (Map.Entry<Integer, Float> entry : this.entrySet()) {
      result.put(FeatureMap.getFeature(entry.getKey()), entry.getValue());
    }
    return result;
  }

  /**
   * Like Map.getOrDefault but with default value 0.0f.
   */
  public Float getOrDefault(Integer key) {
    return internalMap.getOrDefault(key, DEFAULT_VALUE);
  }
  
  public Float get(Integer key) {
    return getOrDefault(key);
  }
  
  public boolean containsKey(Integer key) {
    return internalMap.containsKey(key);
  };
  
  @Override
  public Float put(Integer featureId, Float value) {
    return internalMap.put(featureId, value);
  }

  /**
   * Adds values of other to this. Returns a reference to this.
   * 
   * @param other
   * @return this
   */
  public FeatureVector addInPlace(FeatureVector other) {
    for (Entry<Integer, Float> e : other.entrySet()) {
      add(e.getKey(), e.getValue());
    }
    return this;
  }

  public void add(Integer key, float value) {
    this.put(key, getOrDefault(key) + value);
  }

  /**
   * Computes dot product of this and other FeatureVector.
   */
  public float innerProduct(FeatureVector other) {
    float product = 0.0f;
    if (other.size() >= this.size()) {
      for (Entry<Integer, Float> e : this.entrySet()) {
        product += e.getValue() * other.getOrDefault(e.getKey());
      }
    } else {
      for (Entry<Integer, Float> e : other.entrySet()) {
        product += e.getValue() * this.getOrDefault(e.getKey());
      }
    }
    return product;
  }

  @Override
  public Set<Map.Entry<Integer, Float>> entrySet() {
    return internalMap.entrySet();
  }

  @Override
  public Collection<Float> values() {
    return internalMap.values();
  }

  /**
   * Prunes elements from the vector whose absolute values are smaller than
   * threshold.
   * 
   * @return the pruned feature vector
   */
  public FeatureVector prune(final float threshold) {
    for (Iterator<Map.Entry<Integer, Float>> it = internalMap.entrySet().iterator(); it.hasNext();) {
      if (Math.abs(it.next().getValue()) < threshold) {
        it.remove();
      }
    }
    return this;
  }
  
  public String textFormat() {
    return internalMap.entrySet()
      .stream()
      .map(e -> String.format("%s=%.6f", FeatureMap.getFeature(e.getKey()), e.getValue())  )
      .collect(joining(" "));
  }

}
