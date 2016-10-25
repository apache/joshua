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

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.decoder.LmPool;
import org.apache.joshua.decoder.ff.state_maintenance.KenLMState;
import org.apache.joshua.util.FormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.apache.joshua.util.Constants.LONG_SIZE_IN_BYTES;

/**
 * JNI wrapper for KenLM. This version of KenLM supports two use cases, implemented by the separate
 * feature functions KenLMFF and LanguageModelFF. KenLMFF uses the RuleScore() interface in
 * lm/left.hh, returning a state pointer representing the KenLM state, while LangaugeModelFF handles
 * state by itself and just passes in the ngrams for scoring.
 *
 * @author Kenneth Heafield
 * @author Matt Post post@cs.jhu.edu
 */

public class KenLM implements NGramLanguageModel, Comparable<KenLM> {

  private static final Logger LOG = LoggerFactory.getLogger(KenLM.class);

  // Maximum number of terminal and non-terminal symbols on a rule's target side
  private static final int MAX_TARGET_LENGTH = 256;

  private final long pointer;

  // this is read from the config file, used to set maximum order
  private final int ngramOrder;
  // inferred from model file (may be larger than ngramOrder)
  private final int N;

  public KenLM(int order, String file_name) {
    pointer = initializeSystemLibrary(file_name);
    ngramOrder = order;
    N = order(pointer);
  }

  /**
   * Constructor if order is not known.
   * Order will be inferred from the model.
   *
   * @param file_name string path to an input file
   */
  public KenLM(String file_name) {
    pointer = initializeSystemLibrary(file_name);
    N = order(pointer);
    ngramOrder = N;
  }

  private static native long construct(String file_name);

  private static native void destroy(long ptr);

  private static native int order(long ptr);

  private static native boolean registerWord(long ptr, String word, int id);

  private static native float prob(long ptr, int words[]);

  private static native float probForString(long ptr, String[] words);

  private static native boolean isKnownWord(long ptr, String word);

  private static native boolean isLmOov(long ptr, int word);

  private static native long probRule(long ptr, long pool);

  private static native float estimateRule(long ptr, long words[]);

  private static native float probString(long ptr, int words[], int start);

  private static native long createPool(ByteBuffer wordsBuffer);

  private static native void destroyPool(long pointer);

  private long initializeSystemLibrary(String file_name) {
    try {
      System.loadLibrary("ken");
      return construct(file_name);
    } catch (UnsatisfiedLinkError e) {
      LOG.error("Can't find libken.so (libken.dylib on OS X) on the Java library path.");
      throw new KenLMLoadException(e);
    }
  }

  public LmPool createLMPool() {
    ByteBuffer ngramBuffer = ByteBuffer.allocateDirect(MAX_TARGET_LENGTH * LONG_SIZE_IN_BYTES);
    ngramBuffer.order(LITTLE_ENDIAN);
    long pool = createPool(ngramBuffer);
    return new KenLMPool(pool, ngramBuffer);
  }

  public void destroyLMPool(long pointer) {
    destroyPool(pointer);
  }

  public void destroy() {
    destroy(pointer);
  }

  @Override
  public int getOrder() {
    return ngramOrder;
  }

  @Override
  public boolean registerWord(String word, int id) {
    return registerWord(pointer, word, id);
  }

  public float prob(int[] words) {
    return prob(pointer, words);
  }

  /**
   * Query for n-gram probability using strings.
   *
   * @param words a string array of words
   * @return float value denoting probability
   */
  public float prob(String[] words) {
    return probForString(pointer, words);
  }

  // Apparently Zhifei starts some array indices at 1. Change to 0-indexing.
  public float probString(int words[], int start) {
    return probString(pointer, words, start - 1);
  }

  /**
   * This function is the bridge to the interface in kenlm/lm/left.hh, which has KenLM score the
   * whole rule. It takes an array of words and states retrieved from tail nodes (nonterminals in the
   * rule). Nonterminals have a negative value so KenLM can distinguish them. The sentence number is
   * needed so KenLM knows which memory pool to use. When finished, it returns the updated KenLM
   * state and the LM probability incurred along this rule.
   *
   * @param words       array of words
   * @param poolWrapper an object that wraps a pool reference returned from KenLM createPool
   * @return the updated {@link org.apache.joshua.decoder.ff.lm.KenLM.StateProbPair} e.g.
   * KenLM state and the LM probability incurred along this rule
   */
  public StateProbPair probRule(long[] words, LmPool poolWrapper) {

    poolWrapper.setBufferLength(words.length);
    for (int i = 0; i < words.length; i++) {
      poolWrapper.writeIdToBuffer(i, words[i]);
    }

    long packedResult = probRule(pointer, poolWrapper.getPool());
    int state = (int) (packedResult >> 32);
    float probVal = Float.intBitsToFloat((int) packedResult);

    return new StateProbPair(state, probVal);
  }

  /**
   * Public facing function that estimates the cost of a rule, which value is used for sorting
   * rules during cube pruning.
   *
   * @param words array of words
   * @return the estimated cost of the rule (the (partial) n-gram probabilities of all words in the rule)
   */
  public float estimateRule(long[] words) {
    float estimate = 0.0f;
    try {
      estimate = estimateRule(pointer, words);
    } catch (NoSuchMethodError e) {
      throw new RuntimeException(e);
    }

    return estimate;
  }

  /**
   * The start symbol for a KenLM is the Vocabulary.START_SYM.
   *
   * @return "&lt;s&gt;"
   */
  public String getStartSymbol() {
    return Vocabulary.START_SYM;
  }

  /**
   * Returns whether the given Vocabulary ID is unknown to the
   * KenLM vocabulary. This can be used for a LanguageModel_OOV features
   * and does not need to convert to an intermediate string.
   */
  @Override
  public boolean isOov(int wordId) {
    if (FormatUtils.isNonterminal(wordId)) {
      throw new IllegalArgumentException("Should not query for nonterminals!");
    }
    return isLmOov(pointer, wordId);
  }

  public boolean isKnownWord(String word) {
    return isKnownWord(pointer, word);
  }

  @Override
  public int compareTo(KenLM other) {
    if (this == other)
      return 0;
    else
      return -1;
  }

  /**
   * These functions are used if KenLM is invoked under LanguageModelFF instead of KenLMFF.
   */
  @Override
  public float sentenceLogProbability(int[] sentence, int order, int startIndex) {
    return probString(sentence, startIndex);
  }

  @Override
  public float ngramLogProbability(int[] ngram, int order) {
    if (order != N && order != ngram.length)
      throw new RuntimeException("Lower order not supported.");
    return prob(ngram);
  }

  @Override
  public float ngramLogProbability(int[] ngram) {
    return prob(ngram);
  }

  public static class KenLMLoadException extends RuntimeException {

    public KenLMLoadException(UnsatisfiedLinkError e) {
      super(e);
    }
  }

  /**
   * Inner class used to hold the results returned from KenLM with left-state minimization. Note
   * that inner classes have to be static to be accessible from the JNI!
   */
  public static class StateProbPair {
    public final KenLMState state;
    public final float prob;

    public StateProbPair(long state, float prob) {
      this.state = new KenLMState(state);
      this.prob = prob;
    }
  }

  private class KenLMPool extends LmPool {
    protected KenLMPool(long pool, ByteBuffer ngramBuffer) {
      super(pool, KenLM.this, ngramBuffer);
    }
  }
}
