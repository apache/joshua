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
package org.apache.joshua.decoder;

/*
 * A list of OOV symbols in the form
 *
 * [X1] weight [X2] weight [X3] weight ...
 *
 * where the [X] symbols are nonterminals and the weights are weights. For each OOV word w in the
 * input sentence, Joshua will create rules of the form
 *
 * X1 -> w (weight)
 *
 * If this is empty, an unweighted default_non_terminal is used.
 */
public class OOVItem implements Comparable<OOVItem> {
  public final String label;

  public final float weight;

  OOVItem(String l, float w) {
    label = l;
    weight = w;
  }
  @Override
  public int compareTo(OOVItem other) {
    if (weight > other.weight)
      return -1;
    else if (weight < other.weight)
      return 1;
    return 0;
  }
}