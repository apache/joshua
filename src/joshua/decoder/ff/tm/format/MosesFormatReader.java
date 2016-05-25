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
package joshua.decoder.ff.tm.format;

import java.io.IOException;

import joshua.corpus.Vocabulary;
import joshua.decoder.ff.tm.Rule;
import joshua.util.Constants;
import joshua.util.FormatUtils;
import joshua.util.io.LineReader;

/***
 * This class reads in the Moses phrase table format, with support for the source and target side,
 * list of features, and word alignments. It works by
 * 
 * - casting the phrase-based rules to left-branching hierarchical rules and passing them on \
 *   to its parent class, {@HieroFormatReader}.
 * - converting the probabilities to -log probabilities
 * 
 * There is also a tool to convert the grammars directly, so that they can be suitably packed. Usage:
 * 
 * <pre>
 *     cat PHRASE_TABLE | java -cp $JOSHUA/class joshua.decoder.ff.tm.format.MosesFormatReader > grammar
 * </pre>
 * 
 * @author Matt Post <post@cs.jhu.edu>
 *
 */

public class MosesFormatReader extends HieroFormatReader {

  public MosesFormatReader(String grammarFile) throws IOException {
    super(grammarFile);
    Vocabulary.id(Constants.defaultNT);
  }
  
  public MosesFormatReader() {
    super();
    Vocabulary.id(Constants.defaultNT);
  }
  
  /**
   * When dealing with Moses format, this munges a Moses-style phrase table into a grammar.
   * 
   *    mots francaises ||| French words ||| 1 2 3 ||| 0-1 1-0
   *    
   * becomes
   * 
   *    [X] ||| [X,1] mots francaises ||| [X,1] French words ||| 1 2 3  ||| 0-1 1-0
   *    
   * For thrax-extracted phrasal grammars, it transforms
   * 
   *    [X] ||| mots francaises ||| French words ||| 1 2 3 ||| 0-1 1-0
   *
   * into
   * 
   *    [X] ||| [X,1] mots francaises ||| [X,1] French words ||| 1 2 3 ||| 0-1 1-0
   */
  @Override
  public Rule parseLine(String line) {
    String[] fields = line.split(Constants.fieldDelimiter);
    
    String nt = FormatUtils.cleanNonTerminal(Constants.defaultNT);
    StringBuffer hieroLine = new StringBuffer(Constants.defaultNT + " ||| [" + nt + ",1] " + fields[0] + " ||| [" + nt + ",1] " + fields[1] + " |||");

    String mosesFeatureString = fields[2];
    for (String value: mosesFeatureString.split(" ")) {
      float f = Float.parseFloat(value);
      hieroLine.append(String.format(" %f", f <= 0.0 ? -100 : -Math.log(f)));
    }

    // alignments
    if (fields.length >= 4)
      hieroLine.append(" ||| " + fields[3]);

    return super.parseLine(hieroLine.toString());
  }
  
  /**
   * Converts a Moses phrase table to a Joshua grammar. 
   * 
   * @param args
   */
  public static void main(String[] args) {
    MosesFormatReader reader = new MosesFormatReader();
    for (String line: new LineReader(System.in)) {
      Rule rule = reader.parseLine(line);
      System.out.println(rule.textFormat());
    }    
  }
}
