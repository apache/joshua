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

import java.util.concurrent.locks.StampedLock;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * OwnerMap maintains a decoder-wide mapping between 'owner' strings and
 * corresponding IDs, typed as OwnerId. Using this more strongly typed mapping,
 * we can separate wordIDs in {@link org.apache.joshua.corpus.Vocabulary} from 
 * {@link org.apache.joshua.decoder.ff.tm.OwnerId}s. For example, this prevents 
 * packed grammars to overwrite the owner mappings from previously loaded packaged 
 * grammars.
 * 
 * @author fhieber
 *
 */
public class OwnerMap {

  // bi-directional mapping between OwnerId and Owner strings
  private static final BiMap<OwnerId, String> map = HashBiMap.create();

  public static final OwnerId UNKNOWN_OWNER_ID = new OwnerId(0);
  public static final String UNKNOWN_OWNER = "<unowned>";

  private static final StampedLock lock = new StampedLock();

  static {
    clear();
  }

  /**
   * Register or get OwnerId for given ownerString. This is only called during
   * feature function and grammar initalization and thus does not require
   * sophisticated locking.
   * @param ownerString the OwnerId to register or get
   * @return the registered or existing OwnerId
   */
  public static synchronized OwnerId register(String ownerString) {
    if (map.inverse().containsKey(ownerString)) {
      return map.inverse().get(ownerString);
    }

    final OwnerId newId = new OwnerId(map.size());
    map.put(newId, ownerString);
    return newId;
  }

  public static String getOwner(final OwnerId id) {
    long lock_stamp = lock.readLock();
    try {
      if (map.containsKey(id)) {
        return map.get(id);
      }
      throw new IllegalArgumentException(
          String.format("OwnerMap does not contain mapping for %s", id));
    } finally {
      lock.unlockRead(lock_stamp);
    }
  }

  public static synchronized void clear() {
    map.clear();
    map.put(UNKNOWN_OWNER_ID, UNKNOWN_OWNER);
  }

}
