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
package org.apache.joshua.decoder;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for decoder thread.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class DecoderThreadTest {

  @Test
  public void setup() {

    String[] sourceSentences = {
        "a b c d",
        "a b c d",
        "a b c d"
    };

    String[] targetSentences = {
        "w x y z",
        "w t u v",
        "s x y z"
    };

    String[] alignmentLines = {
        "0-0 1-1 2-2 3-3",
        "0-0 1-1 2-2 3-3",
        "0-0 1-1 2-2 3-3"
    };

    String[] testSentences = {
        "a b c"	
    };

    try {

      // Set up source corpus
      File sourceFile = File.createTempFile("source", new Date().toString());
      PrintStream sourcePrintStream = new PrintStream(sourceFile, "UTF-8");
      for (String sentence : sourceSentences) {
        sourcePrintStream.println(sentence);
      }
      sourcePrintStream.close();
      String sourceCorpusFileName = sourceFile.getAbsolutePath();

//      Vocabulary vocabulary = new Vocabulary();
//      int[] sourceLengths = Vocabulary.initializeVocabulary(sourceCorpusFileName, vocabulary, true);
//      Assert.assertEquals(sourceLengths.length, 2);
//      int numberOfSentences = sourceLengths[1];
//
//      Corpus sourceCorpus = SuffixArrayFactory.createCorpusArray(sourceCorpusFileName, vocabulary, sourceLengths[0], sourceLengths[1]);


      // Set up target corpus
      File targetFile = File.createTempFile("target", new Date().toString());
      PrintStream targetPrintStream = new PrintStream(targetFile, "UTF-8");
      for (String sentence : targetSentences) {
        targetPrintStream.println(sentence);
      }
      targetPrintStream.close();
      String targetCorpusFileName = targetFile.getAbsolutePath();

//      int[] targetLengths = Vocabulary.initializeVocabulary(targetCorpusFileName, vocabulary, true);
//      Assert.assertEquals(targetLengths.length, sourceLengths.length);
//      for (int i=0, n=targetLengths.length; i<n; i++) {
//        Assert.assertEquals(targetLengths[i], sourceLengths[i]);
//      }
//
//      Corpus targetCorpus = SuffixArrayFactory.createCorpusArray(targetCorpusFileName, vocabulary, targetLengths[0], targetLengths[1]);


      // Construct alignments data structure
      File alignmentsFile = File.createTempFile("alignments", new Date().toString());
      PrintStream alignmentsPrintStream = new PrintStream(alignmentsFile, "UTF-8");
      for (String sentence : alignmentLines) {
        alignmentsPrintStream.println(sentence);
      }
      alignmentsPrintStream.close();
      String alignmentFileName = alignmentsFile.getAbsolutePath();

//      AlignmentGrids grids = new AlignmentGrids(
//          new Scanner(alignmentsFile), 
//          sourceCorpus, 
//          targetCorpus, 
//          numberOfSentences);


      // Set up test corpus
      File testFile = File.createTempFile("test", new Date().toString());
      PrintStream testPrintStream = new PrintStream(testFile, "UTF-8");
      for (String sentence : testSentences) {
        testPrintStream.println(sentence);
      }
      testPrintStream.close();
      String testFileName = testFile.getAbsolutePath();

      // Filename of the extracted rules file.
      String rulesFileName; {	
        File rulesFile = File.createTempFile("rules", new Date().toString());
        rulesFileName = rulesFile.getAbsolutePath();
      }

      String joshDirName; {
        File joshDir = File.createTempFile(new Date().toString(), "josh");
        joshDirName = joshDir.getAbsolutePath();
        joshDir.delete();
      }


//      Compile compileJoshDir = new Compile();
//      compileJoshDir.setSourceCorpus(sourceCorpusFileName);
//      compileJoshDir.setTargetCorpus(targetCorpusFileName);
//      compileJoshDir.setAlignments(alignmentFileName);
//      compileJoshDir.setOutputDir(joshDirName);
//      compileJoshDir.execute();
//
//      ExtractRules extractRules = new ExtractRules();
//      extractRules.setJoshDir(joshDirName);
//      extractRules.setTestFile(testFileName);
//      extractRules.setOutputFile(rulesFileName);
//      extractRules.execute();

    } catch (IOException e) {
      Assert.fail("Unable to write temporary file. " + e.toString());
    }
//    } catch (ClassNotFoundException e) {
//      Assert.fail("Unable to extract rules. " + e.toString());
//    }
  }

  @Test
  public void basicSuffixArrayGrammar() {

    // Write configuration to temp file on disk
    //		String configFile;


    //		JoshuaDecoder decoder = 
    //			JoshuaDecoder.getUninitalizedDecoder(configFile);



  }

}
