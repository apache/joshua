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
package org.apache.joshua.decoder.ff;

import java.util.List;

import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.decoder.chart_parser.SourcePath;
import org.apache.joshua.decoder.ff.state_maintenance.DPState;
import org.apache.joshua.decoder.ff.tm.Rule;
import org.apache.joshua.decoder.hypergraph.HGNode;
import org.apache.joshua.decoder.segment_file.Sentence;
import org.apache.joshua.util.FormatUtils;

/*
 * Implements the RuleShape feature for source, target, and paired source+target sides.
 */
public class RuleShape extends StatelessFF {

  public RuleShape(FeatureVector weights, String[] args, JoshuaConfiguration config) {
    super(weights, "RuleShape", args, config);
  }

  private enum WordType {
    N("N"), T("x"), P("+");
    private final String string;
    private boolean repeats;

    private WordType(final String string) {
      this.string = string;
      this.repeats = false;
    }
    
    private void setRepeats() {
      repeats = true;
    }

    @Override
    public String toString() {
      if (repeats) {
        return this.string + "+";
      }
      return this.string;
    }
  }

  private WordType getWordType(int id) {
    if (FormatUtils.isNonterminal(id)) {
      return WordType.N;
    } else {
      return WordType.T;
    }
  }
  
  /**
   * Returns a String describing the rule pattern.
   */
  private String getRulePattern(int[] ids) {
    final StringBuilder pattern = new StringBuilder();
    WordType currentType = getWordType(ids[0]);
    for (int i = 1; i < ids.length; i++) {
      if (getWordType(ids[i]) != currentType) {
        pattern.append(currentType.toString());
        currentType = getWordType(ids[i]);
      } else {
        currentType.setRepeats();
      }
    }
    pattern.append(currentType.toString());
    return pattern.toString();
  }
  
  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i_, int j, SourcePath sourcePath,
      Sentence sentence, Accumulator acc) {
    final String sourceShape = getRulePattern(rule.getFrench());
    final String targetShape = getRulePattern(rule.getEnglish());
    acc.add(name + "_source_" + sourceShape, 1);
    acc.add(name + "_target_" + sourceShape, 1);
    acc.add(name + "_sourceTarget_" + sourceShape + "_" + targetShape, 1);
    return null;
  }
}
