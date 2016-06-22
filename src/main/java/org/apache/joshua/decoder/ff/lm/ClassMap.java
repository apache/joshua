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
package org.apache.joshua.decoder.ff.lm;

import java.io.IOException;

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.util.io.LineReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

public class ClassMap {

  private static final Logger LOG = LoggerFactory.getLogger(ClassMap.class);

  private static final int OOV_ID = Vocabulary.getUnknownId();
  private final ImmutableMap<Integer, Integer> mapping;

  public ClassMap(String file_name) {
    this.mapping = read(file_name);
    LOG.info("{} entries read from class map", this.mapping.size());
  }

  public int getClassID(int wordID) {
    return this.mapping.getOrDefault(wordID, OOV_ID);
  }

  public int size() {
    return mapping.size();
  }

  /**
   * Reads a class map from file_name
   */
  private static ImmutableMap<Integer, Integer> read(String file_name) {
    final ImmutableMap.Builder<Integer, Integer> builder = ImmutableMap.builder();
    int lineno = 0;
    try {
      for (String line : new LineReader(file_name, false)) {
        lineno++;
        String[] lineComp = line.trim().split("\\s+");
        try {
          builder.put(Vocabulary.id(lineComp[0]), Vocabulary.id(lineComp[1]));
        } catch (java.lang.ArrayIndexOutOfBoundsException e) {
          LOG.warn("bad vocab line #{} '{}'. skipping!", lineno, line);
          LOG.warn(e.getMessage(), e);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return builder.build();
  }

}
