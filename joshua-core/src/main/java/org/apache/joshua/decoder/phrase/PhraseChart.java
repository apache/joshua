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

import java.util.ArrayList;	
import java.util.Arrays;
import java.util.List;

import org.apache.joshua.decoder.Decoder;
import org.apache.joshua.decoder.ff.FeatureFunction;
import org.apache.joshua.decoder.ff.tm.Rule;
import org.apache.joshua.decoder.ff.tm.RuleCollection;
import org.apache.joshua.decoder.segment_file.Sentence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents a bundle of phrase tables that have been read in,
 * reporting some stats about them. Probably could be done away with.
 */
public class PhraseChart {

  private static final Logger LOG = LoggerFactory.getLogger(PhraseChart.class);

  private int sentence_length;
  private int max_source_phrase_length;

  // Banded array: different source lengths are next to each other.
  private List<TargetPhrases> entries;

  // number of translation options
  int numOptions = 20;
  private List<FeatureFunction> features;

  /**
   * Create a new PhraseChart object, which represents all phrases that are
   * applicable against the current input sentence. These phrases are extracted
   * from all available grammars.
   * 
   * @param tables input array of {@link org.apache.joshua.decoder.phrase.PhraseTable}'s
   * @param features {@link java.util.List} of {@link org.apache.joshua.decoder.ff.FeatureFunction}'s
   * @param source input to {@link org.apache.joshua.lattice.Lattice}
   * @param num_options number of translation options (typically set to 20)
   */
  public PhraseChart(PhraseTable[] tables, List<FeatureFunction> features, Sentence source,
      int num_options) {

    float startTime = System.currentTimeMillis();

    this.numOptions = num_options;
    this.features = features;

    max_source_phrase_length = 0;
    for (int i = 0; i < tables.length; i++)
      max_source_phrase_length = Math.max(max_source_phrase_length,
          tables[i].getMaxSourcePhraseLength());
    sentence_length = source.length();

//    System.err.println(String.format(
//        "PhraseChart()::Initializing chart for sentlen %d max %d from %s", sentence_length,
//        max_source_phrase_length, source));

    entries = new ArrayList<TargetPhrases>();
    for (int i = 0; i < sentence_length * max_source_phrase_length; i++)
      entries.add(null);

    // There's some unreachable ranges off the edge. Meh.
    for (int begin = 0; begin != sentence_length; ++begin) {
      for (int end = begin + 1; (end != sentence_length + 1)
          && (end <= begin + max_source_phrase_length); ++end) {
        if (source.hasPath(begin, end)) {
          for (PhraseTable table : tables)
            addToRange(begin, end,
                table.getPhrases(Arrays.copyOfRange(source.getWordIDs(), begin, end)));
        }

      }
    }

    for (TargetPhrases phrases : entries) {
      if (phrases != null)
        phrases.finish(features, Decoder.weights, num_options);
    }

    LOG.info("Input {}: Collecting options took {} seconds", source.id(),
        (System.currentTimeMillis() - startTime) / 1000.0f);
    
    if (LOG.isDebugEnabled()) {
      for (int i = 1; i < sentence_length - 1; i++) {
        for (int j = i + 1; j < sentence_length && j <= i + max_source_phrase_length; j++) {
          if (source.hasPath(i, j)) {
            TargetPhrases phrases = getRange(i, j);
            if (phrases != null) {
              LOG.debug("{} ({}-{})", source.source(i,j), i, j);
              for (Rule rule: phrases)
                LOG.debug("    {} :: est={}", rule.getEnglishWords(), rule.getEstimatedCost());
            }
          }
        }
      }
    }
  }

  public int SentenceLength() {
    return sentence_length;
  }

  // c++: TODO: make this reflect the longest source phrase for this sentence.
  public int MaxSourcePhraseLength() {
    return max_source_phrase_length;
  }

  /**
   * Maps two-dimensional span into a one-dimensional array.
   * 
   * @param i beginning of span
   * @param j end of span
   * @return offset into private list of TargetPhrases
   */
  private int offset(int i, int j) {
    return i * max_source_phrase_length + j - i - 1;
  }

  /**
   * Returns phrases from all grammars that match the span.
   * 
   * @param begin beginning of span
   * @param end end of span
   * @return the {@link org.apache.joshua.decoder.phrase.TargetPhrases} at the specified position in this list.
   */
  public TargetPhrases getRange(int begin, int end) {
    int index = offset(begin, end);
    // System.err.println(String.format("PhraseChart::Range(%d,%d): found %d entries",
    // begin, end,
    // entries.get(index) == null ? 0 : entries.get(index).size()));
    // if (entries.get(index) != null)
    // for (Rule phrase: entries.get(index))
    // System.err.println("  RULE: " + phrase);

    if (index < 0 || index >= entries.size() || entries.get(index) == null)
      return null;

    return entries.get(index);
  }

  /**
   * Add a set of phrases from a grammar to the current span.
   * 
   * @param begin beginning of span
   * @param end end of span
   * @param to a {@link org.apache.joshua.decoder.ff.tm.RuleCollection} to be used in scoring and sorting.
   */
  private void addToRange(int begin, int end, RuleCollection to) {
    if (to != null) {
      /*
       * This first call to getSortedRules() is important, because it is what
       * causes the scoring and sorting to happen. It is also a synchronized call,
       * which is necessary because the underlying grammar gets sorted. Subsequent calls to get the
       * rules will just return the already-sorted list. Here, we score, sort,
       * and then trim the list to the number of translation options. Trimming provides huge
       * performance gains --- the more common the word, the more translations options it is
       * likely to have (often into the tens of thousands).
       */
      List<Rule> rules = to.getSortedRules(features);
      if (numOptions > 0 && rules.size() > numOptions)
        rules = rules.subList(0,  numOptions);
//        to.getRules().subList(numOptions, to.getRules().size()).clear();

      try {
        int offset = offset(begin, end);
        if (entries.get(offset) == null)
          entries.set(offset, new TargetPhrases(rules));
        else
          entries.get(offset).addAll(rules);
      } catch (java.lang.IndexOutOfBoundsException e) {
        LOG.error("Whoops! {} [{}-{}] too long ({})", to, begin, end, entries.size());
        LOG.error(e.getMessage(), e);
      }
    }
  }
}
