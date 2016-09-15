/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.joshua.decoder.cky;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.joshua.decoder.Decoder;
import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.decoder.segment_file.Sentence;

public class TestUtil {

  public static final String N_BEST_SEPARATOR = "\n";

  /**
   * Loads a text file and returns a list containing one string per line in the file.
   * 
   * @param pathToFile
   * @return
   * @throws IOException
   */
  public static List<String> loadStringsFromFile(String pathToFile) throws IOException {
    List<String> inputLines = Files.lines(Paths.get(pathToFile)).collect(Collectors.toList());
    return inputLines;
  }

  /**
   * 
   * @param inputStrings A list of strings that should be decoded,
   * @param decoder An initialized decoder,
   * @param joshuaConfig The JoshuaConfiguration corresponding to the decoder.
   * @return A list of decoded strings. If the decoder produces a n-best list (separated by
   *         N_BEST_SEPARATOR), then each translation of the n-best list has its own entry in the
   *         returned list.
   */
  public static List<String> decodeList(List<String> inputStrings, Decoder decoder,
      JoshuaConfiguration joshuaConfig) {
    final List<String> decodedStrings = new ArrayList<>();

    for (String inputString : inputStrings) {
      final Sentence sentence = new Sentence(inputString, 0, joshuaConfig);
      final String[] nBestList = decoder.decode(sentence).toString().split(N_BEST_SEPARATOR);
      decodedStrings.addAll(Arrays.asList(nBestList));
    }

    return decodedStrings;
  }

  /**
   * Translates the given input string and returns the translation converted into a string.
   * 
   * @param input
   * @param decoder
   * @param joshuaConfig
   * @return
   */
  public static String translate(String input, Decoder decoder, JoshuaConfiguration joshuaConfig) {
    final Sentence sentence = new Sentence(input, 0, joshuaConfig);
    return decoder.decode(sentence).toString();
  }

}
