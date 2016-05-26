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
package org.apache.joshua.tools;

import java.io.IOException;

import org.apache.joshua.corpus.Vocabulary;
import org.apache.joshua.corpus.syntax.ArraySyntaxTree;
import org.apache.joshua.util.io.LineReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finds labeling for a set of phrases.
 * 
 * @author Juri Ganitkevitch
 */
public class LabelPhrases {

  private static final Logger LOG = LoggerFactory.getLogger(LabelPhrases.class);

  /**
   * Main method.
   * 
   * @param args names of the two grammars to be compared
   * @throws IOException if there is an error reading the input grammars
   */
  public static void main(String[] args) throws IOException {

    if (args.length < 1 || args[0].equals("-h")) {
      System.err.println("Usage: " + LabelPhrases.class.toString());
      System.err.println("    -p phrase_file     phrase-sentence file to process");
      System.err.println();
      System.exit(-1);
    }

    String phrase_file_name = null;

    for (int i = 0; i < args.length; i++) {
      if ("-p".equals(args[i])) phrase_file_name = args[++i];
    }
    if (phrase_file_name == null) {
      LOG.error("a phrase file is required for operation");
      System.exit(-1);
    }

    LineReader phrase_reader = new LineReader(phrase_file_name);

    while (phrase_reader.ready()) {
      String line = phrase_reader.readLine();

      String[] fields = line.split("\\t");
      if (fields.length != 3 || fields[2].equals("()")) {
        System.err.println("[FAIL] Empty parse in line:\t" + line);
        continue;
      }

      String[] phrase_strings = fields[0].split("\\s");
      int[] phrase_ids = new int[phrase_strings.length];
      for (int i = 0; i < phrase_strings.length; i++)
        phrase_ids[i] = Vocabulary.id(phrase_strings[i]);

      ArraySyntaxTree syntax = new ArraySyntaxTree(fields[2]);
      int[] sentence_ids = syntax.getTerminals();

      int match_start = -1;
      int match_end = -1;
      for (int i = 0; i < sentence_ids.length; i++) {
        if (phrase_ids[0] == sentence_ids[i]) {
          match_start = i;
          int j = 0;
          while (j < phrase_ids.length && phrase_ids[j] == sentence_ids[i + j]) {
            j++;
          }
          if (j == phrase_ids.length) {
            match_end = i + j;
            break;
          }
        }
      }

      int label = syntax.getOneConstituent(match_start, match_end);
      if (label == 0) label = syntax.getOneSingleConcatenation(match_start, match_end);
      if (label == 0) label = syntax.getOneRightSideCCG(match_start, match_end);
      if (label == 0) label = syntax.getOneLeftSideCCG(match_start, match_end);
      if (label == 0) label = syntax.getOneDoubleConcatenation(match_start, match_end);
      if (label == 0) {
        System.err.println("[FAIL] No label found in line:\t" + line);
        continue;
      }

      System.out.println(Vocabulary.word(label) + "\t" + line);
    }
  }
}
