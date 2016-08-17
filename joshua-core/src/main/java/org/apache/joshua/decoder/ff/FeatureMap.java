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

import java.util.concurrent.locks.StampedLock;

import org.apache.joshua.decoder.ff.tm.OwnerId;
import org.apache.joshua.decoder.ff.tm.OwnerMap;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * FeatureMap maintains a decoder-wide bi-directional mapping between feature names and
 * corresponding IDs, typed as ints.
 * This separates feature ids (i.e. simple hashes) from word ids.
 * The retrieval of the feature name given a feature id is strict, in that it throws 
 * a {@link RuntimeException} if the feature was not hashed/added to this mapping before.
 * 
 * @author fhieber
 *
 */
public class FeatureMap {
  
  /** bi-directional mapping between feature ids and feature names */
  private static BiMap<Integer, String> map = HashBiMap.create();
  
  private static final StampedLock lock = new StampedLock();
  
  static {
    map.clear();
  }
  
  /**
   * Return a feature id for the given featureName. If the id does not exist
   * yet, it is added to the mapping.
   * @param featureName the featureName to be hashed
   * @return
   */
  public static synchronized int hashFeature(String featureName) {
    if (map.inverse().containsKey(featureName)) {
      return map.inverse().get(featureName);
    }
    final int newId = map.size();
    map.put(newId, featureName);
    return newId;
  }

  /**
   * Returns a feature id corresponding to a feature prepended an owner string if ownerId != OwnerMap.UNKNOWN_OWNER_ID.
   * This function is used to hash features precomputed on rules (i.e. dense & sparse features stored in the grammar).
   * @param featureName the featureName to be hashed
   * @param ownerId the ownerId of the grammar owning this feature.
   * @return feature id corresponding to the (owner-prefixed) feature name.
   */
  public static int hashFeature(final String featureName, final OwnerId ownerId) {
    if (ownerId.equals(OwnerMap.UNKNOWN_OWNER_ID)) {
      return hashFeature(featureName);
    } else {
      return hashFeature(OwnerMap.getOwner(ownerId) + "_" + featureName);
    }
  }
  
  /**
   * Reverse lookup a feature id to retrieve the stored feature name.
   * Throws a {@link RuntimeException} if mapping is not present.
   * @param id a feature id that must be contained in the mapping.
   * @return featureName corresponding to the given feature id.
   */
  public static String getFeature(final int id) {
    long lock_stamp = lock.readLock();
    try {
      if (map.containsKey(id)) {
        return map.get(id);
      }
      throw new IllegalArgumentException(
          String.format("FeatureMap does not contain mapping for %s", id));
    } finally {
      lock.unlockRead(lock_stamp);
    }
  }
  
  public static boolean hasFeature(final String featureName) {
    return map.inverse().containsKey(featureName);
  }
  
  public static synchronized void clear() {
    map.clear();
  }

}
