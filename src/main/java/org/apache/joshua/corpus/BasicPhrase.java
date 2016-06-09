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
package org.apache.joshua.corpus;

import java.util.ArrayList;

/**
 * The simplest concrete implementation of Phrase.
 * 
 * @author wren ng thornton wren@users.sourceforge.net
 * @version $LastChangedDate$
 */
public class BasicPhrase extends AbstractPhrase {
  private byte language;
  private int[] words;


  public BasicPhrase(byte language, String sentence) {
    this.language = language;
    this.words = splitSentence(sentence);
  }

  private BasicPhrase() {}

  public int[] getWordIDs() {
    return words;
  }

  /* See Javadoc for Phrase interface. */
  public BasicPhrase subPhrase(int start, int end) {
    BasicPhrase that = new BasicPhrase();
    that.language = this.language;
    that.words = new int[end - start + 1];
    System.arraycopy(this.words, start, that.words, 0, end - start + 1);
    return that;
  }

  /* See Javadoc for Phrase interface. */
  public ArrayList<Phrase> getSubPhrases() {
    return this.getSubPhrases(this.size());
  }

  /* See Javadoc for Phrase interface. */
  public ArrayList<Phrase> getSubPhrases(int maxLength) {
    ArrayList<Phrase> phrases = new ArrayList<Phrase>();
    int len = this.size();
    for (int n = 1; n <= maxLength; n++)
      for (int i = 0; i <= len - n; i++)
        phrases.add(this.subPhrase(i, i + n - 1));
    return phrases;
  }

  /* See Javadoc for Phrase interface. */
  public int size() {
    return (words == null ? 0 : words.length);
  }

  /* See Javadoc for Phrase interface. */
  public int getWordID(int position) {
    return words[position];
  }

  /**
   * Returns a human-readable String representation of the phrase.
   * <p>
   * The implementation of this method is slightly more efficient than that inherited from
   * <code>AbstractPhrase</code>.
   * 
   * @return a human-readable String representation of the phrase.
   */
  public String toString() {
    StringBuffer sb = new StringBuffer();
    if (words != null) {
      for (int i = 0; i < words.length; ++i) {
        if (i != 0) sb.append(' ');
        sb.append(Vocabulary.word(words[i]));
      }
    }
    return sb.toString();
  }
}
