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
package org.apache.joshua.zmert;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.apache.joshua.metrics.BLEU;
import org.apache.joshua.metrics.EvaluationMetric;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * Unit tests for BLEU class.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class BLEUTest {

  @Test
  public void metricName() {

    // Setup the EvaluationMetric class
    EvaluationMetric.set_numSentences(0);
    EvaluationMetric.set_refsPerSen(1);
    EvaluationMetric.set_refSentences(null);

    BLEU bleu = new BLEU();

    Assert.assertEquals(bleu.get_metricName(), "BLEU");

  }

  @Test
  public void defaultConstructor() {

    // Setup the EvaluationMetric class
    EvaluationMetric.set_numSentences(0);
    EvaluationMetric.set_refsPerSen(1);
    EvaluationMetric.set_refSentences(null);

    BLEU bleu = new BLEU();

    // Default constructor should use a maximum n-gram length of 4
    Assert.assertEquals(bleu.getMaxGramLength(), 4);

    // Default constructor should use the closest reference
    Assert.assertEquals(bleu.getEffLengthMethod(), BLEU.EffectiveLengthMethod.CLOSEST);

  }

  @Test
  public void simpleTest() {

    String ref = "this is the fourth chromosome whose sequence has been completed to date . it comprises more than 87 million pairs of dna .";
    String test = "this is the fourth chromosome to be fully sequenced up till now and it comprises of over 87 million pairs of deoxyribonucleic acid ( dna ) .";

    // refSentences[i][r] stores the r'th reference of the i'th sentence
    String[][] refSentences = new String[1][1];
    refSentences[0][0] = ref;

    EvaluationMetric.set_numSentences(1);
    EvaluationMetric.set_refsPerSen(1);
    EvaluationMetric.set_refSentences(refSentences);

    BLEU bleu = new BLEU();

    // testSentences[i] stores the candidate translation for the i'th sentence
    String[] testSentences = new String[1];
    testSentences[0] = test;
    try {
      // Check BLEU score matches
      double actualScore = bleu.score(testSentences);
      double expectedScore = 0.2513;
      double acceptableScoreDelta = 0.00001f;

      Assert.assertEquals(actualScore, expectedScore, acceptableScoreDelta);

      // Check sufficient statistics match
      int[] actualSS = bleu.suffStats(testSentences);
      int[] expectedSS = {14,27,8,26,5,25,3,24,27,23};

      Assert.assertEquals(actualSS[0], expectedSS[0], 0); // 1-gram matches
      Assert.assertEquals(actualSS[1], expectedSS[1], 0); // 1-gram total
      Assert.assertEquals(actualSS[2], expectedSS[2], 0); // 2-gram matches
      Assert.assertEquals(actualSS[3], expectedSS[3], 0); // 2-gram total
      Assert.assertEquals(actualSS[4], expectedSS[4], 0); // 3-gram matches
      Assert.assertEquals(actualSS[5], expectedSS[5], 0); // 3-gram total
      Assert.assertEquals(actualSS[6], expectedSS[6], 0); // 4-gram matches
      Assert.assertEquals(actualSS[7], expectedSS[7], 0); // 4-gram total
      Assert.assertEquals(actualSS[8], expectedSS[8], 0); // candidate length
      Assert.assertEquals(actualSS[9], expectedSS[9], 0); // reference length
    } catch (Exception e) {
      Assert.fail();
    }
  }

  @Parameters({"referenceFile","testFile"})
  @Test(enabled=false)
  public void fileTest(String referenceFile, String testFile) throws FileNotFoundException {

    //TODO You can now read in the files, and do something useful with them.

    @SuppressWarnings("resource")
    Scanner refScanner = new Scanner(new File(referenceFile));

    while (refScanner.hasNextLine()) {

      @SuppressWarnings("unused")
      String refLine = refScanner.nextLine();
    }
  }
}
