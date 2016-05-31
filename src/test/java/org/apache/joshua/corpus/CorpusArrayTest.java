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

import java.util.logging.Logger;

public class CorpusArrayTest {

  /** Logger for this class. */
  private static Logger logger =
      Logger.getLogger(CorpusArrayTest.class.getName());
}

//  @Test
//  public void writePartsToDisk() {
//
//    String filename = "data/tiny.en";
//    int numSentences = 5;  // Should be 5 sentences in tiny.en
//    int numWords = 89;     // Should be 89 words in tiny.en
//
//
//    try {
//
//      // FIX: can't use createVocabulary(String) because we set numWords and numSentences
//      Vocabulary vocab = new Vocabulary();
//      SuffixArrayFactory.createVocabulary(filename, vocab);
//      Corpus corpus = SuffixArrayFactory.createCorpusArray(filename, vocab, numWords, numSentences);
//
//      corpus.writeWordIDsToFile(filename+".bin");
//      corpus.writeSentenceLengthsToFile(filename+".sbin");
//
//      MemoryMappedCorpusArray mmCorpus = new MemoryMappedCorpusArray(corpus.getVocabulary(), filename+".bin", numWords*4, filename+".sbin", numSentences*4);
//
//      // For each word in the corpus,
//      for (int i=0; i<corpus.size(); i++) {
//
//        // Verify that the memory-mapped corpus and the in-memory corpus have the same value
//        Assert.assertEquals(mmCorpus.getWordID(i), corpus.getWordID(i));
//      }
//
//
//      // For each sentence in the corpus
//      for (int i=0; i<corpus.sentences.length; i++) {
//
//        // Verify that the sentence position in the memory-mapped corpus and the in-memory corpus have the same value
//        Assert.assertEquals(corpus.getSentencePosition(i), mmCorpus.getSentencePosition(i));
//      }
//
//    } catch (IOException e) {
//      Assert.fail(e.getLocalizedMessage());
//    }
//
//  }
//
//  @Test
//  public void iterate() {
//
//    String[] sentences = {
//        "scientists complete sequencing of the chromosome linked to early dementia",
//        "( afp , paris , january 2 ) an international team of scientists said that they have completed the sequencing of human chromosome 14 that is linked to many diseases , including the early-onset alzheimer's that may strike people in their 30s .",
//        "this is the fourth chromosome whose sequence has been completed to date . it comprises more than 87 million pairs of dna .",
//        "this study published in the weekly british scientific journal nature illustrates that the sequence of chromosome 14 comprises 1,050 genes and gene fragments .",
//        "the goal of geneticists is to provide diagnostic tools to identify defective genes that cause diseases so as to arrive eventually at treatments that can prevent those genes from malfunctioning ."
//    };
//
//
//
//    // Tell System.out and System.err to use UTF8
//    FormatUtil.useUTF8();
//
//    try {
//
//      File sourceFile = File.createTempFile("source", new Date().toString());
//      PrintStream sourcePrintStream = new PrintStream(sourceFile, "UTF-8");
//      for (String sentence : sentences) {
//        sourcePrintStream.println(sentence);
//      }
//      sourcePrintStream.close();
//      String corpusFileName = sourceFile.getAbsolutePath();
//
//      Vocabulary vocabulary;
//
//      logger.fine("Constructing vocabulary from file " + corpusFileName);
//      vocabulary = new Vocabulary();
//      int[] lengths = Vocabulary.initializeVocabulary(corpusFileName, vocabulary, true);
//
//      logger.fine("Constructing corpus array from file " + corpusFileName);
//      Corpus corpus = SuffixArrayFactory.createCorpusArray(corpusFileName, vocabulary, lengths[0], lengths[1]);
//
//      int expectedIndex = 0;
//      for (int actualIndex : corpus.corpusPositions()) {
//        Assert.assertEquals(actualIndex, expectedIndex);
//        expectedIndex += 1;
//      }
//
//      Assert.assertEquals(corpus.size(), expectedIndex);
//
//
//    } catch (IOException e) {
//      Assert.fail("Unable to write temporary file. " + e.toString());
//    }
//
//
//
//  }
//
//
//  @Test
//  public void writeAllToDisk() throws ClassNotFoundException {
//
//    String filename = "data/tiny.en";
//    int numSentences = 5;  // Should be 5 sentences in tiny.en
//    int numWords = 89;     // Should be 89 words in tiny.en
//
//
//    try {
//
//      // FIX: can't use createVocabulary(String) because we set numWords and numSentences
//      Vocabulary vocab = new Vocabulary();
//      Vocabulary.initializeVocabulary(filename, vocab, true);
//      CorpusArray corpus = SuffixArrayFactory.createCorpusArray(filename, vocab, numWords, numSentences);
//
//      corpus.write(filename+".corpus", filename+".vocab", "UTF-8");
//
//      MemoryMappedCorpusArray mmCorpus = new MemoryMappedCorpusArray(filename+".corpus", filename+".vocab");
//
//      Assert.assertEquals(mmCorpus.size(), corpus.size());
//      Assert.assertEquals(mmCorpus.getNumSentences(), corpus.getNumSentences());
//
//      // For each word in the corpus,
//      for (int i=0; i<corpus.size(); i++) {
//
//        // Verify that the memory-mapped corpus and the in-memory corpus have the same value
//        Assert.assertEquals(mmCorpus.getWordID(i), corpus.getWordID(i));
//      }
//
//
//      // For each sentence in the corpus
//      for (int i=0; i<corpus.sentences.length; i++) {
//
//        // Verify that the sentence start position in the memory-mapped corpus and the in-memory corpus have the same value
//        Assert.assertEquals(mmCorpus.getSentencePosition(i), corpus.getSentencePosition(i));
//
//        // Verify that the sentence end position in the memory-mapped corpus and the in-memory corpus have the same value
//        Assert.assertEquals(mmCorpus.getSentenceEndPosition(i), corpus.getSentenceEndPosition(i));
//
//        // Verify that the phrase corresponding to this sentence is the same
//        Phrase sentence = corpus.getSentence(i);
//        Phrase mmSentence = mmCorpus.getSentence(i);
//        Assert.assertNotNull(sentence);
//        Assert.assertNotNull(mmSentence);
//        Assert.assertEquals(mmSentence, sentence);
//      }
//
//    } catch (IOException e) {
//      Assert.fail(e.getLocalizedMessage());
//    }
//
//  }
//
//}
