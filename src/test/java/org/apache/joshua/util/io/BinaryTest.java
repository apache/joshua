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
package org.apache.joshua.util.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;
import java.util.Set;

import org.apache.joshua.corpus.Vocabulary;
import org.testng.Assert;
import org.testng.annotations.Test;

public class BinaryTest {

  @Test
  public void externalizeVocabulary() throws IOException, ClassNotFoundException {

    Set<String> words = new HashSet<String>();

    for (char c1='a'; c1<='z'; c1++) {
      words.add(new String(new char[]{c1}));
      for (char c2='a'; c2<='z'; c2++) {
        words.add(new String(new char[]{c1,c2}));
      }	
    }

    Vocabulary vocab = new Vocabulary();
    vocab.addAll(words.toArray(new String[words.size()]));

    try {

      File tempFile = File.createTempFile(BinaryTest.class.getName(), "vocab");
      FileOutputStream outputStream = new FileOutputStream(tempFile);
      @SuppressWarnings({ "unused", "resource" })
      ObjectOutput out = new BinaryOut(outputStream, true);
      vocab.write(tempFile.toString());

      @SuppressWarnings("resource")
      ObjectInput in = new BinaryIn(tempFile.getAbsolutePath(), Vocabulary.class);
      Object o = in.readObject();
      Assert.assertTrue(o instanceof Vocabulary);

      Vocabulary newVocab = (Vocabulary) o;

      Assert.assertNotNull(newVocab);
      Assert.assertEquals(newVocab.size(), vocab.size());

      Assert.assertTrue(newVocab.equals(vocab));

    } catch (SecurityException e) {
      Assert.fail("Operating system is unable to create a temp file required by this unit test: " + e);
    }
  }
}
