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

import org.apache.joshua.decoder.chart_parser.SourcePath;
import org.apache.joshua.decoder.ff.state_maintenance.DPState;
import org.apache.joshua.decoder.ff.tm.Rule;
import org.apache.joshua.decoder.hypergraph.HGNode;
import org.apache.joshua.decoder.segment_file.Sentence;

import com.typesafe.config.Config;

/**
 * This feature returns the scored path through the source lattice, which is recorded in a
 * SourcePath object.
 * 
 * @author Chris Dyer redpony@umd.edu
 * @author Matt Post post@cs.jhu.edu
 */
public final class SourcePathFF extends StatelessFF {

  /*
   * This is a single-value feature template, so we cache the weight here.
   */
  public SourcePathFF(Config featureConfig, FeatureVector weights) {
    super("SourcePath", featureConfig, weights);
  }

  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      Sentence sentence, Accumulator acc) {

    acc.add(featureId,  sourcePath.getPathCost());
    return null;
  }
}
