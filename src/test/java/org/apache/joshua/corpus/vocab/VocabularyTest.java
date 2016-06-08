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
package org.apache.joshua.corpus.vocab;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import org.apache.joshua.corpus.Vocabulary;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * 
 * @author Lane Schwartz
 */
public class VocabularyTest {

  /** [X], [X,1], [X,2], [S], [S,1] <unk>, <s>, </s>, -pau-*/
  int numBuiltInSymbols = 9;

  /** <unk>, <s>, </s>, -pau- */
  int numBuiltInTerminals = 4;

  @Test
  public void basicVocabTest() {

    Vocabulary vocab1 = new Vocabulary();
    Vocabulary vocab2 = new Vocabulary();

    Assert.assertEquals(vocab1, vocab2);

    Assert.assertFalse(vocab1.size() == 0);
    //Assert.assertTrue(vocab1.intToString.get(0)==Vocabulary.UNKNOWN_WORD_STRING);
    //Assert.assertFalse(vocab1.getWords().isEmpty());
    //    Assert.assertTrue(vocab1.getWords(0)==Vocabulary.UNKNOWN_WORD_STRING);
    //    Assert.assertEquals(vocab1.getWords(), vocab1.intToString.values());

    Assert.assertNotEquals(vocab1.size(), numBuiltInSymbols);
    //    Assert.assertEquals(vocab1.getWord(Vocabulary.UNKNOWN_WORD), Vocabulary.UNKNOWN_WORD_STRING);

    //Assert.assertEquals(vocab1.getID("sample"), Vocabulary.UNKNOWN_WORD);
    //Assert.assertEquals(vocab1.getID(null), Vocabulary.UNKNOWN_WORD);

    //    Assert.assertFalse(vocab1.terminalToInt.isEmpty());
    //    Assert.assertEquals(vocab1.terminalToInt.size(), this.numBuiltInTerminals);
    //    Assert.assertFalse(vocab1.isFixed);
    //
    //    vocab1.fixVocabulary();
    //    Assert.assertTrue(vocab1.isFixed);

    //    Assert.assertEquals(vocab1.getID(Vocabulary.X_STRING), -1);
    //    Assert.assertEquals(vocab1.getID(Vocabulary.X1_STRING), -2);
    //    Assert.assertEquals(vocab1.getID(Vocabulary.X2_STRING), -3);
    //
    //    Assert.assertEquals(vocab1.getWord(-1), Vocabulary.X_STRING);
    //    Assert.assertEquals(vocab1.getWord(-2), Vocabulary.X1_STRING);
    //    Assert.assertEquals(vocab1.getWord(-3), Vocabulary.X2_STRING);



    //    Assert.assertFalse(vocab2.intToString.isEmpty());
    //		Assert.assertTrue(vocab2.intToString.get(0)==Vocabulary.UNKNOWN_WORD_STRING);
    //    Assert.assertFalse(vocab2.getWords().isEmpty());
    //		Assert.assertTrue(vocab2.getWord(0)==Vocabulary.UNKNOWN_WORD_STRING);
    //    Assert.assertEquals(vocab2.getWords(), vocab2.intToString.values());

    Assert.assertNotEquals(vocab2.size(), numBuiltInSymbols);
    //    Assert.assertEquals(vocab2.getWord(Vocabulary.UNKNOWN_WORD), Vocabulary.UNKNOWN_WORD_STRING);

    //		Assert.assertEquals(vocab2.getID("sample"), Vocabulary.UNKNOWN_WORD);
    //		Assert.assertEquals(vocab2.getID(null), Vocabulary.UNKNOWN_WORD);

    //    Assert.assertFalse(vocab2.terminalToInt.isEmpty());
    //    Assert.assertEquals(vocab2.terminalToInt.size(), this.numBuiltInTerminals);
    //		Assert.assertTrue(vocab2.isFixed);
  }

  @Test
  public void verifyWordIDs() throws IOException {

    // Adam Lopez's example...
    String corpusString = "it makes him and it mars him , it sets him on and it takes him off .";
    //		String queryString = "it persuades him and it disheartens him";

    String sourceFileName;
    {
      File sourceFile = File.createTempFile("source", new Date().toString());
      PrintStream sourcePrintStream = new PrintStream(sourceFile, "UTF-8");
      sourcePrintStream.println(corpusString);
      sourcePrintStream.close();
      sourceFileName = sourceFile.getAbsolutePath();
    }

    Vocabulary vocab = new Vocabulary();
    //    Vocabulary.initializeVocabulary(sourceFileName, vocab, true);

//    Assert.assertEquals(vocab.getWords(Vocabulary.id("it")), "it");
//    Assert.assertEquals(vocab.getWord(vocab.getID("makes")), "makes");
//    Assert.assertEquals(vocab.getWord(vocab.getID("him")), "him");
//    Assert.assertEquals(vocab.getWord(vocab.getID("and")), "and");
//    Assert.assertEquals(vocab.getWord(vocab.getID("mars")), "mars");
//    Assert.assertEquals(vocab.getWord(vocab.getID(",")), ",");
//    Assert.assertEquals(vocab.getWord(vocab.getID("sets")), "sets");
//    Assert.assertEquals(vocab.getWord(vocab.getID("on")), "on");
//    Assert.assertEquals(vocab.getWord(vocab.getID("takes")), "takes");
//    Assert.assertEquals(vocab.getWord(vocab.getID("off")), "off");

    //		Assert.assertEquals(vocab.getWord(vocab.getID("persuades")), Vocabulary.UNKNOWN_WORD_STRING);
    //		Assert.assertEquals(vocab.getWord(vocab.getID("disheartens")), Vocabulary.UNKNOWN_WORD_STRING);
  }

  @SuppressWarnings("static-access")
  @Test(enabled=false)
  public void loadVocabFromFile() {

    String filename = "data/tiny.en";
    int numSentences = 5;  // Should be 5 sentences in tiny.en
    int numWords = 89;     // Should be 89 words in tiny.en
    int numUniqWords = 60; // Should be 60 unique words in tiny.en

    Vocabulary vocab = new Vocabulary();
    Vocabulary vocab2 = new Vocabulary();

    Assert.assertTrue(vocab.equals(vocab2));
    Assert.assertTrue(vocab2.equals(vocab));
    Assert.assertEquals(vocab, vocab2);

    try {
      vocab.read(new File(getClass().getClassLoader().getResource(filename).getFile()));
      //int[] result = Vocabulary.initializeVocabulary(filename, vocab, true);
      Assert.assertNotNull(vocab);
      Assert.assertEquals(vocab.size(), 2);
      //Assert.assertEquals(vocab.getWords(numWords), numWords); 
      // Assert.assertEquals(result[1], numSentences);  

      //Assert.assertTrue(vocab.isFixed);
      Assert.assertEquals(Vocabulary.size(), numUniqWords+numBuiltInSymbols);

    } catch (IOException e) {
      Assert.fail("Error processing " + filename +"; Reason: " + e);
    }

    Assert.assertFalse(vocab.equals(vocab2));

    try {
      vocab2.read(new File(filename));
      //int[] result = Vocabulary.initializeVocabulary(filename, vocab2, true);
      Assert.assertNotNull(vocab2);
      Assert.assertEquals(vocab2.size(), 2);
      //      Assert.assertEquals(result[0], numWords); 
      //      Assert.assertEquals(result[1], numSentences);  

      //			Assert.assertTrue(vocab2.isFixed);
      Assert.assertEquals(Vocabulary.size(), numUniqWords+numBuiltInSymbols);

    } catch (IOException e) {
      Assert.fail("Could not load file " + filename);
    }

    Assert.assertEquals(vocab, vocab2);
  }
}
