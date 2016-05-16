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
package org.apache.joshua.decoder.ff.lm.berkeley_lm;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.berkeley.nlp.lm.ArrayEncodedNgramLanguageModel;

public class LMBerkeleySentenceProbablityTest {

  @Test
  public void verifySentenceLogProbability() {
    LMGrammarBerkeley grammar = new LMGrammarBerkeley(2, "resources/berkeley_lm/lm");
    grammar.registerWord("the", 2);
    grammar.registerWord("chat-rooms", 3);
    grammar.registerWord("<unk>", 0);

    ArrayEncodedNgramLanguageModel<String> lm = grammar.getLM();
    float expected =
        lm.getLogProb(new int[] {}, 0, 0)
        + lm.getLogProb(new int[] {0}, 0, 1)
        + lm.getLogProb(new int[] {0, 2}, 0, 2)
        + lm.getLogProb(new int[] {2, 3}, 0, 2)
        + lm.getLogProb(new int[] {3, 0}, 0, 2);

    float result = grammar.sentenceLogProbability(new int[] {0, 2, 3, 0}, 2, 0);
    assertEquals(expected, result, 0.0);
  }
}
