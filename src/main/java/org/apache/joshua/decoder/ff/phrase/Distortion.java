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
package org.apache.joshua.decoder.ff.phrase;

import java.util.ArrayList;
import java.util.List;	

import org.apache.joshua.decoder.JoshuaConfiguration;
import org.apache.joshua.decoder.chart_parser.SourcePath;
import org.apache.joshua.decoder.ff.FeatureVector;
import org.apache.joshua.decoder.ff.StatelessFF;
import org.apache.joshua.decoder.ff.state_maintenance.DPState;
import org.apache.joshua.decoder.ff.tm.Rule;
import org.apache.joshua.decoder.hypergraph.HGNode;
import org.apache.joshua.decoder.phrase.Hypothesis;
import org.apache.joshua.decoder.segment_file.Sentence;

public class Distortion extends StatelessFF {

  public Distortion(FeatureVector weights, String[] args, JoshuaConfiguration config) {
    super(weights, "Distortion", args, config);
    
    if (! config.search_algorithm.equals("stack")) {
      String msg = "* FATAL: Distortion feature only application for phrase-based decoding. "
          + "Use -search phrase or remove this feature";
      throw new RuntimeException(msg);
    }
  }
  
  @Override
  public ArrayList<String> reportDenseFeatures(int index) {
    denseFeatureIndex = index;
    
    ArrayList<String> names = new ArrayList<>();
    names.add(name);
    return names;
  }

  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      Sentence sentence, Accumulator acc) {

    if (rule == Hypothesis.INORDER_RULE) {
      int last_phrase_end = tailNodes.get(0).j;
      int new_phrase_start = tailNodes.get(1).i;
      int jump_size = Math.abs(last_phrase_end - new_phrase_start);

      //        System.err.println(String.format("DISTORTION_mono(%d -> %d) = %d", 
      //            last_phrase_end, new_phrase_start, jump_size));

      acc.add(denseFeatureIndex, -jump_size);
    } else if (rule == Hypothesis.INVERTED_RULE) {
      int last_phrase_end = tailNodes.get(1).j;
      int new_phrase_start = tailNodes.get(0).i;
      int jump_size = Math.abs(last_phrase_end - new_phrase_start);

      //        System.err.println(String.format("DISTORTION_swap(%d -> %d) = %d", 
      //            last_phrase_end, new_phrase_start, jump_size));

      acc.add(denseFeatureIndex, -jump_size);    
    }
    
    return null;
  }
}
