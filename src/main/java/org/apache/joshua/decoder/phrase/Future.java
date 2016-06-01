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
package org.apache.joshua.decoder.phrase;

import org.apache.joshua.util.ChartSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Future {

  private static final Logger LOG = LoggerFactory.getLogger(Future.class);

  // Square matrix with half the values ignored.
  private ChartSpan<Float> entries;

  private int sentlen;

  /**
   * Computes bottom-up the best way to cover all spans of the input sentence, using the phrases
   * that have been assembled in a {@link org.apache.joshua.decoder.phrase.PhraseChart}.
   * Requires that there be a translation at least for every word (which can be 
   * accomplished with a pass-through grammar).
   * 
   * @param chart an input {@link org.apache.joshua.decoder.phrase.PhraseChart}
   */
  public Future(PhraseChart chart) {

    sentlen = chart.SentenceLength();
    entries = new ChartSpan<Float>(sentlen + 1, Float.NEGATIVE_INFINITY);

    /*
     * The sentence is represented as a sequence of words, with the first and last words set
     * to <s> and </s>. We start indexing at 1 because the first word (<s>) is always covered.
     */
    for (int begin = 1; begin <= chart.SentenceLength(); begin++) {
      // Nothing is nothing (this is a useful concept when two phrases abut)
      setEntry(begin, begin,  0.0f);
      // Insert phrases
      int max_end = Math.min(begin + chart.MaxSourcePhraseLength(), chart.SentenceLength());
      for (int end = begin + 1; end <= max_end; end++) {

        // Moses doesn't include the cost of applying </s>, so force it to zero
        if (begin == sentlen - 1 && end == sentlen) 
          setEntry(begin, end, 0.0f);
        else {
          TargetPhrases phrases = chart.getRange(begin, end);
          if (phrases != null)
            setEntry(begin, end, phrases.get(0).getEstimatedCost());
        }
      }
    }

    // All the phrases are in, now do minimum dynamic programming.  Lengths 0 and 1 were already handled above.
    for (int length = 2; length <= chart.SentenceLength(); length++) {
      for (int begin = 1; begin <= chart.SentenceLength() - length; begin++) {
        for (int division = begin + 1; division < begin + length; division++) {
          setEntry(begin, begin + length, Math.max(getEntry(begin, begin + length), getEntry(begin, division) + getEntry(division, begin + length)));
        }
      }
    }

    if (LOG.isDebugEnabled()) {
      for (int i = 1; i < chart.SentenceLength(); i++) {
        for (int j = i + 1; j < chart.SentenceLength(); j++) {
          LOG.debug("future cost from {} to {} is {}", i - 1, j - 2, getEntry(i, j));
        }
      }
    }
  }

  public float Full() {
    //    System.err.println("Future::Full(): " + Entry(1, sentlen));
    return getEntry(1, sentlen);
  }

  /**
   * Calculate change in rest cost when the given coverage is to be covered.
   * @param coverage input {@link org.apache.joshua.decoder.phrase.Coverage} vector
   * @param begin word at which to begin within a sentence
   * @param end word at which to end within a sentence
   * @return a float value representing a {@link Future} entry
   */
  public float Change(Coverage coverage, int begin, int end) {
    int left = coverage.leftOpening(begin);
    int right = coverage.rightOpening(end, sentlen);
    //    System.err.println(String.format("Future::Change(%s, %d, %d) left %d right %d %.3f %.3f %.3f", coverage, begin, end, left, right,
    //        Entry(left, begin), Entry(end, right), Entry(left, right)));
    return getEntry(left, begin) + getEntry(end, right) - getEntry(left, right);
  }

  private float getEntry(int begin, int end) {
    assert end >= begin;
    assert end < this.sentlen;
    return entries.get(begin, end);
  }

  private void setEntry(int begin, int end, float value) {
    assert end >= begin;
    assert end < this.sentlen;
    //    System.err.println(String.format("future cost from %d to %d is %.5f", begin, end, value));
    entries.set(begin, end, value);
  }
}
