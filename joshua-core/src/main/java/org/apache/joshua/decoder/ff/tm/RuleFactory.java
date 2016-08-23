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
package org.apache.joshua.decoder.ff.tm;

import static org.apache.joshua.decoder.ff.FeatureMap.hashFeature;
import static org.apache.joshua.util.Constants.labeledFeatureSeparator;
import static org.apache.joshua.util.Constants.spaceSeparator;

import org.apache.joshua.decoder.ff.FeatureVector;

/**
 * Provides static functions to instantiate rules from its dependencies.
 * @author fhieber
 *
 */
public class RuleFactory {
  
  /**
   * Parses an alignment string of the form '0-0 1-1 2-1'
   * into a byte array {0, 0, 1, 1, 2, 1}.
   * @param alignmentString a string of the form '0-0 1-1 2-1'
   * @return byte[] containing alignment indices or null if alignmentString is null.
   */
  public static byte[] parseAlignmentString(String alignmentString) {
    byte[] alignment = null;
    if (alignmentString != null) {
      String[] tokens = alignmentString.split("[-\\s]+");
      if (tokens.length % 2 != 0) {
        throw new RuntimeException(
            String.format("Can not parse alignment string: '%s'", alignmentString));
      }
      alignment = new byte[tokens.length];
      for (int i = 0; i < tokens.length; i++) {
        alignment[i] = (byte) Short.parseShort(tokens[i]);
      }
    }
    return alignment;
  }
  
  /**
   * Creates a {@link FeatureVector} from a string of the form '0.4 <name>=-1 ...'.
   * This means, features can be either labeled or unlabeled.
   * In the latter case the feature name will be an increasing index.
   * Further, the feature names are prepended by the <owner> prefix before being hashed.
   * @param featureString
   * @param ownerId the owner id
   * @return a {@link FeatureVector} with ids corresponding to tm-owned feature names.
   */
  public static FeatureVector parseFeatureString(final String featureString, final OwnerId ownerId) {
    final String[] fields = featureString.split(spaceSeparator);
    final FeatureVector result = new FeatureVector(fields.length);
    int unlabeledFeatureIndex = 0;
    String featureName;
    float featureValue;

    for (final String token : fields) {
      final int splitIndex = token.indexOf(labeledFeatureSeparator);
      final boolean isUnlabeledFeature = (splitIndex == -1);
      if (isUnlabeledFeature) {
        featureName = Integer.toString(unlabeledFeatureIndex);
        featureValue = Float.parseFloat(token);
        unlabeledFeatureIndex++;
      } else {
        featureName = token.substring(0, splitIndex);
        featureValue = Float.parseFloat(token.substring(splitIndex + 1));
      }
      result.put(hashFeature(featureName, ownerId), featureValue);
    }
    return result;
  }
  
}