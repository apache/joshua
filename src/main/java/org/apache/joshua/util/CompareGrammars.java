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
package org.apache.joshua.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import org.apache.joshua.decoder.ff.tm.format.HieroFormatReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class allows two grammars (loaded from disk) to be compared.
 * 
 * @author Lane Schwartz
 */
public class CompareGrammars {

  private static final Logger LOG = LoggerFactory.getLogger(CompareGrammars.class);

  /**
   * Gets a set containing all unique instances of the specified field.
   * 
   * @param grammarFile File containing a grammar.
   * @param fieldDelimiter Regular expression to split each line
   * @param fieldNumber Field from each rule to extract
   * @return set containing all unique instances of the specified field
   * @throws FileNotFoundException
   */
  public static Set<String> getFields(File grammarFile, String fieldDelimiter, int fieldNumber)
      throws FileNotFoundException {

    Scanner grammarScanner = new Scanner(grammarFile);

    Set<String> set = new HashSet<String>();

    while (grammarScanner.hasNextLine()) {

      String line = grammarScanner.nextLine();

      String[] fields = line.split(fieldDelimiter);

      set.add(fields[fieldNumber]);
    }
    
    grammarScanner.close();

    return set;
  }

  public static void compareValues(File grammarFile1, File grammarFile2, String fieldDelimiter,
      int fieldNumber, String scoresDelimiter, int scoresFieldNumber, float delta)
      throws FileNotFoundException {

    Scanner grammarScanner1 = new Scanner(grammarFile1);
    Scanner grammarScanner2 = new Scanner(grammarFile2);

    Set<String> set = new HashSet<String>();

    int counter = 0;
    float totalOverDiffs = 0.0f;
    while (grammarScanner1.hasNextLine() && grammarScanner2.hasNextLine()) {

      counter++;

      String line1 = grammarScanner1.nextLine();
      String[] fields1 = line1.split(fieldDelimiter);
      String[] scores1 = fields1[fieldNumber].split(scoresDelimiter);
      float score1 = Float.valueOf(scores1[scoresFieldNumber]);

      String line2 = grammarScanner2.nextLine();
      String[] fields2 = line2.split(fieldDelimiter);
      String[] scores2 = fields2[fieldNumber].split(scoresDelimiter);
      float score2 = Float.valueOf(scores2[scoresFieldNumber]);

      if (fields1[0].endsWith(fields2[0]) && fields1[1].endsWith(fields2[1])
          && fields1[1].endsWith(fields2[1])) {

        float diff1 = Math.abs(score1 - score2);
        float diff2 = Math.abs(score2 - score1);
        float diff = (diff1 < diff2) ? diff1 : diff2;

        if (diff > delta) {
          LOG.debug("Line {}:  Score mismatch: {} vs {}", counter, score1, score2);
          set.add(line1);
          totalOverDiffs += diff;
        } else {
          LOG.debug("Line {}: Scores MATCH: {} vs ", counter, score1, score2);
        }
      } else {
        throw new RuntimeException("Lines don't match: " + line1 + " and " + line2);
      }
    }
    
    grammarScanner1.close();
    grammarScanner2.close();
    
    if (set.isEmpty()) {
      LOG.info("No score mismatches");
    } else {
      LOG.warn("Number of mismatches: {} out of {}", set.size(), counter);
      LOG.warn("Total mismatch logProb mass: {} ({}) ({})", totalOverDiffs,
          totalOverDiffs / set.size(),  totalOverDiffs / counter);
    }
  }

  /**
   * Main method.
   * 
   * @param args names of the two grammars to be compared
   * @throws FileNotFoundException
   */
  public static void main(String[] args) throws FileNotFoundException {

    if (args.length != 2) {
      LOG.error("Usage: {} grammarFile1 grammarFile2", CompareGrammars.class.toString());
      System.exit(-1);
    }

    // Tell standard in and out to use UTF-8
    FormatUtils.useUTF8();
    LOG.debug("Using UTF-8");

    LOG.info("Comparing grammar files {} and {}", args[0], args[1]);

    File grammarFile1 = new File(args[0]);
    File grammarFile2 = new File(args[1]);

    String fieldDelimiter = HieroFormatReader.getFieldDelimiter();

    boolean compareScores = true;

    // Compare left-hand sides
    {
      Set<String> leftHandSides1 = getFields(grammarFile1, fieldDelimiter, 0);
      Set<String> leftHandSides2 = getFields(grammarFile2, fieldDelimiter, 0);

      if (leftHandSides1.equals(leftHandSides2)) {
        LOG.info("Grammar files have the same set of left-hand sides");
      } else {
        LOG.warn("Grammar files have differing sets of left-hand sides");
        compareScores = false;
      }
    }

    // Compare source right-hand sides
    {
      Set<String> sourceRHSs1 = getFields(grammarFile1, fieldDelimiter, 1);
      Set<String> sourceRHSs2 = getFields(grammarFile2, fieldDelimiter, 1);

      if (sourceRHSs1.equals(sourceRHSs2)) {
        LOG.info("Grammar files have the same set of source right-hand sides");
      } else {
        LOG.warn("Grammar files have differing sets of source right-hand sides");
        compareScores = false;
      }
    }


    // Compare target right-hand sides
    {
      Set<String> targetRHSs1 = getFields(grammarFile1, fieldDelimiter, 2);
      Set<String> targetRHSs2 = getFields(grammarFile2, fieldDelimiter, 2);

      if (targetRHSs1.equals(targetRHSs2)) {
        LOG.info("Grammar files have the same set of target right-hand sides");
      } else {
        LOG.warn("Grammar files have differing sets of target right-hand sides");
        compareScores = false;
      }
    }

    // Compare translation probs
    if (compareScores) {
      float delta = 0.001f;
      compareValues(grammarFile1, grammarFile2, fieldDelimiter, 3, "\\s+", 0, delta);
      compareValues(grammarFile1, grammarFile2, fieldDelimiter, 3, "\\s+", 1, delta);
      compareValues(grammarFile1, grammarFile2, fieldDelimiter, 3, "\\s+", 2, delta);
    }
  }
}
