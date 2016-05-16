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
package org.apache.joshua.util.quantization;

import java.io.BufferedInputStream; 
import java.io.BufferedOutputStream; 
import java.io.DataInputStream; 
import java.io.DataOutputStream; 
import java.io.File; 
import java.io.FileInputStream; 
import java.io.FileOutputStream; 
import java.io.IOException; 
import java.util.ArrayList; 
import java.util.HashMap; 
import java.util.List; 
import java.util.Map; 

import org.apache.joshua.corpus.Vocabulary; 

public class QuantizerConfiguration { 

  private static final Quantizer DEFAULT; 

  private ArrayList<Quantizer> quantizers; 
  private Map<Integer, Integer> quantizerByFeatureId; 

  static { 
    DEFAULT = new BooleanQuantizer(); 
  } 

  public QuantizerConfiguration() { 
    quantizers = new ArrayList<Quantizer>(); 
    quantizerByFeatureId = new HashMap<Integer, Integer>(); 
  } 

  public void add(String quantizer_key, List<Integer> feature_ids) { 
    Quantizer q = QuantizerFactory.get(quantizer_key); 
    quantizers.add(q); 
    int index = quantizers.size() - 1; 
    for (int feature_id : feature_ids) 
      quantizerByFeatureId.put(feature_id, index); 
  } 

  public void initialize() { 
    for (Quantizer q : quantizers) 
      q.initialize(); 
  } 

  public void finalize() { 
    for (Quantizer q : quantizers) 
      q.finalize(); 
  } 

  public final Quantizer get(int feature_id) { 
    Integer index = quantizerByFeatureId.get(feature_id); 
    return (index != null ? quantizers.get(index) : DEFAULT); 
  } 

  public void read(String file_name) throws IOException { 
    quantizers.clear(); 
    quantizerByFeatureId.clear(); 

    File quantizer_file = new File(file_name); 
    DataInputStream in_stream = 
        new DataInputStream(new BufferedInputStream(new FileInputStream(quantizer_file))); 
    int num_quantizers = in_stream.readInt(); 
    quantizers.ensureCapacity(num_quantizers); 
    for (int i = 0; i < num_quantizers; i++) { 
      String key = in_stream.readUTF(); 
      Quantizer q = QuantizerFactory.get(key); 
      q.readState(in_stream); 
      quantizers.add(q); 
    } 
    int num_mappings = in_stream.readInt(); 
    for (int i = 0; i < num_mappings; i++) { 
      String feature_name = in_stream.readUTF(); 
      int feature_id = Vocabulary.id(feature_name); 
      int quantizer_index = in_stream.readInt(); 
      if (quantizer_index >= num_quantizers) { 
        throw new RuntimeException("Error deserializing QuanitzerConfig. " + "Feature " 
            + feature_name + " referring to quantizer " + quantizer_index + " when only " 
            + num_quantizers + " known."); 
      } 
      this.quantizerByFeatureId.put(feature_id, quantizer_index); 
    } 
    in_stream.close(); 
  } 

  public void write(String file_name) throws IOException { 
    File vocab_file = new File(file_name); 
    DataOutputStream out_stream = 
        new DataOutputStream(new BufferedOutputStream(new FileOutputStream(vocab_file))); 
    out_stream.writeInt(quantizers.size()); 
    for (int index = 0; index < quantizers.size(); index++) 
      quantizers.get(index).writeState(out_stream); 
    out_stream.writeInt(quantizerByFeatureId.size()); 
    for (int feature_id : quantizerByFeatureId.keySet()) { 
      out_stream.writeUTF(Vocabulary.word(feature_id)); 
      out_stream.writeInt(quantizerByFeatureId.get(feature_id)); 
    } 
    out_stream.close(); 
  } 
}