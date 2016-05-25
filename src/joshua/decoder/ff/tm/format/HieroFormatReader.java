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
import joshua.decoder.ff.tm.GrammarReader;
import joshua.decoder.ff.tm.Rule;
import joshua.util.Constants;
import joshua.util.FormatUtils;

/**
 * This class implements reading files in the format defined by David Chiang for Hiero. 
 * 
 * @author Unknown
 * @author Matt Post <post@cs.jhu.edu>
 */

public class HieroFormatReader extends GrammarReader<Rule> {

  static {
    description = "Original Hiero format";
  }

  public HieroFormatReader() {
    super();
  }

  public HieroFormatReader(String grammarFile) throws IOException {
    super(grammarFile);
  }

  @Override
  public Rule parseLine(String line) {
    String[] fields = line.split(Constants.fieldDelimiter);
    if (fields.length < 3) {
      throw new RuntimeException(String.format("Rule '%s' does not have four fields", line));
    }

    int lhs = Vocabulary.id(fields[0]);

    /**
     * On the foreign side, we map nonterminals to negative IDs, and terminals to positive IDs.
     */
    int arity = 0;
    String[] sourceWords = fields[1].split("\\s+");
    int[] sourceIDs = new int[sourceWords.length];
    for (int i = 0; i < sourceWords.length; i++) {
      /* NOTE: This redundantly creates vocab items for terms like [X,1]. This might actually
       * be necessary, so don't try to turn this into an if/else.
       */
      sourceIDs[i] = Vocabulary.id(sourceWords[i]);
      if (FormatUtils.isNonterminal(sourceWords[i])) {
        sourceIDs[i] = Vocabulary.id(FormatUtils.stripNonTerminalIndex(sourceWords[i]));
        arity++;
        
        // TODO: the arity here (after incrementing) should match the rule index. Should
        // check that arity == FormatUtils.getNonterminalIndex(foreignWords[i]), throw runtime
        // error if not
      }
    }

    /**
     * The English side maps terminal symbols to positive IDs. Nonterminal symbols are linked
     * to the index of the source-side nonterminal they are linked to. So for a rule
     * 
     * [X] ||| [X,1] [X,2] [X,3] ||| [X,2] [X,1] [X,3] ||| ...
     * 
     * the English side nonterminals will be -2, -1, -3. This assumes that the source side of
     * the rule is always listed monotonically.
     */
    String[] targetWords = fields[2].split("\\s+");
    int[] targetIDs = new int[targetWords.length];
    for (int i = 0; i < targetWords.length; i++) {
      targetIDs[i] = Vocabulary.id(targetWords[i]);
      if (FormatUtils.isNonterminal(targetWords[i])) {
        targetIDs[i] = -FormatUtils.getNonterminalIndex(targetWords[i]);
      }
    }

    String sparse_features = (fields.length > 3 ? fields[3] : "");
    String alignment = (fields.length > 4) ? fields[4] : null;

    return new Rule(lhs, sourceIDs, targetIDs, sparse_features, arity, alignment);
  }
  
  public static boolean isNonTerminal(final String word) {
    return FormatUtils.isNonterminal(word);
  }
}
