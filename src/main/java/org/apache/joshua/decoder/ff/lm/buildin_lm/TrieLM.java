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
package org.apache.joshua.decoder.ff.lm.buildin_lm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Scanner;

import org.apache.joshua.corpus.Vocabulary;
import  org.apache.joshua.decoder.ff.lm.AbstractLM;
import  org.apache.joshua.decoder.ff.lm.ArpaFile;
import  org.apache.joshua.decoder.ff.lm.ArpaNgram;
import  org.apache.joshua.util.Bits;
import  org.apache.joshua.util.Regex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Relatively memory-compact language model
 * stored as a reversed-word-order trie.
 * <p>
 * The trie itself represents language model context.
 * <p>
 * Conceptually, each node in the trie stores a map 
 * from conditioning word to log probability.
 * <p>
 * Additionally, each node in the trie stores 
 * the backoff weight for that context.
 * 
 * @author Lane Schwartz
 * @see <a href="http://www.speech.sri.com/projects/srilm/manpages/ngram-discount.7.html">SRILM ngram-discount documentation</a>
 */
public class TrieLM extends AbstractLM { //DefaultNGramLanguageModel {

  private static final Logger LOG = LoggerFactory.getLogger(TrieLM.class);

  /**
   * Node ID for the root node.
   */
  private static final int ROOT_NODE_ID = 0;


  /** 
   * Maps from (node id, word id for child) --> node id of child. 
   */
  private final Map<Long,Integer> children;

  /**
   * Maps from (node id, word id for lookup word) --> 
   * log prob of lookup word given context 
   * 
   * (the context is defined by where you are in the tree).
   */
  private final Map<Long,Float> logProbs;

  /**
   * Maps from (node id) --> 
   * backoff weight for that context 
   * 
   * (the context is defined by where you are in the tree).
   */
  private final Map<Integer,Float> backoffs;

  public TrieLM(Vocabulary vocab, String file) throws FileNotFoundException {
    this(new ArpaFile(file,vocab));
  }

  /**
   * Constructs a language model object from the specified ARPA file.
   * 
   * @param arpaFile input ARPA file
   * @throws FileNotFoundException if the input file cannot be located
   */
  public TrieLM(ArpaFile arpaFile) throws FileNotFoundException {
    super(arpaFile.getVocab().size(), arpaFile.getOrder());

    int ngramCounts = arpaFile.size();
    LOG.debug("ARPA file contains {} n-grams", ngramCounts);

    this.children = new HashMap<Long,Integer>(ngramCounts);
    this.logProbs = new HashMap<Long,Float>(ngramCounts);
    this.backoffs = new HashMap<Integer,Float>(ngramCounts);

    int nodeCounter = 0;

    int lineNumber = 0;
    for (ArpaNgram ngram : arpaFile) {
      lineNumber += 1;
      if (lineNumber % 100000 == 0){
        LOG.info("Line: {}", lineNumber);
      }

      LOG.debug("{}-gram: ({} | {})", ngram.order(), ngram.getWord(),
          Arrays.toString(ngram.getContext()));
      int word = ngram.getWord();

      int[] context = ngram.getContext();

      {
        // Find where the log prob should be stored
        int contextNodeID = ROOT_NODE_ID;
        {
          for (int i=context.length-1; i>=0; i--) {
            long key = Bits.encodeAsLong(contextNodeID, context[i]);
            int childID;
            if (children.containsKey(key)) {
              childID = children.get(key);
            } else {
              childID = ++nodeCounter;
              LOG.debug("children.put({}:{}, {})", contextNodeID, context[i], childID);
              children.put(key, childID);
            }
            contextNodeID = childID;
          }
        }

        // Store the log prob for this n-gram at this node in the trie
        {
          long key = Bits.encodeAsLong(contextNodeID, word);
          float logProb = ngram.getValue();
          LOG.debug("logProbs.put({}:{}, {}", contextNodeID, word, logProb);
          this.logProbs.put(key, logProb);
        }
      }

      {
        // Find where the backoff should be stored
        int backoffNodeID = ROOT_NODE_ID;
        { 
          long backoffNodeKey = Bits.encodeAsLong(backoffNodeID, word);
          int wordChildID;
          if (children.containsKey(backoffNodeKey)) {
            wordChildID = children.get(backoffNodeKey);
          } else {
            wordChildID = ++nodeCounter;
            LOG.debug("children.put({}: {}, {})", backoffNodeID, word, wordChildID);
            children.put(backoffNodeKey, wordChildID);
          }
          backoffNodeID = wordChildID;

          for (int i=context.length-1; i>=0; i--) {
            long key = Bits.encodeAsLong(backoffNodeID, context[i]);
            int childID;
            if (children.containsKey(key)) {
              childID = children.get(key);
            } else {
              childID = ++nodeCounter;
              LOG.debug("children.put({}:{}, {})", backoffNodeID, context[i], childID);
              children.put(key, childID);
            }
            backoffNodeID = childID;
          }
        }

        // Store the backoff for this n-gram at this node in the trie
        {
          float backoff = ngram.getBackoff();
          LOG.debug("backoffs.put({}:{}, {})", backoffNodeID, word, backoff);
          this.backoffs.put(backoffNodeID, backoff);
        }
      }

    }
  }


  @Override
  protected double logProbabilityOfBackoffState_helper(
      int[] ngram, int order, int qtyAdditionalBackoffWeight
      ) {
    throw new UnsupportedOperationException("probabilityOfBackoffState_helper undefined for TrieLM");
  }

  @Override
  protected float ngramLogProbability_helper(int[] ngram, int order) {

//    float logProb = (float) -JoshuaConfiguration.lm_ceiling_cost;//Float.NEGATIVE_INFINITY; // log(0.0f)
    float backoff = 0.0f; // log(1.0f)

    int i = ngram.length - 1;
    int word = ngram[i];
    i -= 1;

    int nodeID = ROOT_NODE_ID;

    while (true) {

      {
        long key = Bits.encodeAsLong(nodeID, word);
        if (logProbs.containsKey(key)) {
//          logProb = logProbs.get(key);
          backoff = 0.0f; // log(0.0f)
        }
      }

      if (i < 0) {
        break;
      }

      {
        long key = Bits.encodeAsLong(nodeID, ngram[i]);

        if (children.containsKey(key)) {
          nodeID = children.get(key);

          backoff += backoffs.get(nodeID);

          i -= 1;

        } else {
          break;
        }
      }

    }

//    double result = logProb + backoff;
//    if (result < -JoshuaConfiguration.lm_ceiling_cost) {
//      result = -JoshuaConfiguration.lm_ceiling_cost;
//    }
//
//    return result;
    return (Float) null;
  }

  public Map<Long,Integer> getChildren() {
    return this.children;
  }

  public static void main(String[] args) throws IOException {

    LOG.info("Constructing ARPA file");
    ArpaFile arpaFile = new ArpaFile(args[0]);

    LOG.info("Getting symbol table");
    Vocabulary vocab = arpaFile.getVocab();

    LOG.info("Constructing TrieLM");
    TrieLM lm = new TrieLM(arpaFile);

    int n = Integer.valueOf(args[2]);
    LOG.info("N-gram order will be {}", n);

    Scanner scanner = new Scanner(new File(args[1]));

    LinkedList<String> wordList = new LinkedList<String>();
    LinkedList<String> window = new LinkedList<String>();

    LOG.info("Starting to scan {}", args[1]);
    while (scanner.hasNext()) {

      LOG.info("Getting next line...");
      String line = scanner.nextLine();
      LOG.info("Line: {}", line);

      String[] words = Regex.spaces.split(line);
      wordList.clear();

      wordList.add("<s>");
      for (String word : words) {
        wordList.add(word);
      }
      wordList.add("</s>");

      ArrayList<Integer> sentence = new ArrayList<Integer>();
      //        int[] ids = new int[wordList.size()];
      for (int i=0, size=wordList.size(); i<size; i++) {
        sentence.add(vocab.id(wordList.get(i)));
        //          ids[i] = ;
      }



      while (! wordList.isEmpty()) {
        window.clear();

        {
          int i=0;
          for (String word : wordList) {
            if (i>=n) break;
            window.add(word);
            i++;
          }
          wordList.remove();
        }

        {
          int i=0;
          int[] wordIDs = new int[window.size()];
          for (String word : window) {
            wordIDs[i] = vocab.id(word);
            i++;
          }

          LOG.info("logProb {} = {}", window, lm.ngramLogProbability(wordIDs, n));
        }
      }

      double logProb = lm.sentenceLogProbability(sentence, n, 2);//.ngramLogProbability(ids, n);
      double prob = Math.exp(logProb);

      LOG.info("Total logProb = {}", logProb);
      LOG.info("Total    prob = {}",  prob);
    }

  }


}