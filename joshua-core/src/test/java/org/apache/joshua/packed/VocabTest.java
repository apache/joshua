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
package org.apache.joshua.packed;

import java.io.File;
import java.io.IOException;

import org.apache.joshua.corpus.Vocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VocabTest {

  private static final Logger LOG = LoggerFactory.getLogger(VocabTest.class);

  //FIXME: no main() in automated test case,
  public static void main(String args[]) {

    int numWords = 0;
    try {
      String dir = args[0];

      boolean read = Vocabulary.read(new File(dir + "/vocabulary"));
      if (! read) {
        System.err.println("VocabTest: Failed to read the vocabulary.");
        System.exit(1);
      }

      int id = 0;
      while (Vocabulary.hasId(id)) {
        String word = Vocabulary.word(id);
        System.out.println(String.format("VOCAB: %d\t%s", id, word));
        numWords++;
        id++;
      }
    } catch (IOException e) {
      LOG.error(e.getMessage(), e);
    }

    System.out.println("read " + numWords + " words");
  }
}
