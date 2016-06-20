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
package org.apache.joshua.decoder.io;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.joshua.decoder.StructuredTranslation;
import org.apache.joshua.decoder.Translation;

/**
 * Represents a JSON object returned by the server. The object has the format
 * 
 * { data: { 
 *   translations: [
 *     { annotatedSource: "",
 *       translatedText: "",
 *       raw_nbest: [
 *         { hyp: "",
 *           totalScore: 0.0, } ]
 *       tokenization: { ... }
 *       translatedTextRaw: "",
 *       nbest: [
 *         { translatedText: "",
 *           translatedTextRaw: "",
 *           tokenization: { ... } } ] } ] } }
 * 
 * @author post
 */

public class JSONMessage {
  public Data data = null;
  public List<String> metadata = null;
  public JSONMessage() {
    metadata = new ArrayList<String>();
  }
  
  public class Data {
    public List<TranslationItem> translations;
    
    public Data() {
      translations = new ArrayList<TranslationItem>();
    }
  }
//
//  public class Metadata {
//    public String metadata = null;
//    public List<String> rules = null;
//
//    public Metadata() {
//      rules = new ArrayList<String>();
//    }
//  }

  public void addTranslation(Translation translation) {
    String viterbi = translation.getStructuredTranslations().get(0).getTranslationString();
    
    TranslationItem item = addTranslation(viterbi);

    for (StructuredTranslation hyp: translation.getStructuredTranslations()) {
      String text = hyp.getFormattedTranslationString();
      float score = hyp.getTranslationScore();

      item.addHypothesis(text, score);
    }
    
      // old string-based k-best output
  //    String[] results = translation.toString().split("\\n");
  //    if (results.length > 0) {
  //      String rawTranslation = results[0].split(" \\|\\|\\| ")[1];
  //      JSONMessage.TranslationItem item = message.addTranslation(rawTranslation);
  //
  //      for (String result: results) {
  //        String[] tokens = result.split(" \\|\\|\\| ");
  //        String rawResult = tokens[1];
  //        float score = Float.parseFloat(tokens[3]);
  //        item.addHypothesis(rawResult, score);
  //      }
  //    }
    }

  /**
   * Adds a new Translation to the JSON object. A Translation represents one or more hypotheses
   * (or k-best items)
   * 
   * @param text
   * @return the new TranslationItem object
   */
  public TranslationItem addTranslation(String text) {
    if (data == null)
      data = new Data();
    
    TranslationItem newItem = new TranslationItem(text);
    data.translations.add(newItem);
    return newItem;
  }
  
  public void addMetaData(String msg) {
    this.metadata.add(msg);
  }

  public class TranslationItem {
    public String translatedText;
    public List<NBestItem> raw_nbest;
    
    public TranslationItem(String value) {
      this.translatedText = value;
      this.raw_nbest = new ArrayList<NBestItem>();
    }
    
    /**
     * Adds a new item to the translation's list of k-best items
     * 
     * @param hyp the hypothesis
     * @param score its score
     */
    public void addHypothesis(String hyp, float score) {
      this.raw_nbest.add(new NBestItem(hyp, score));
    }
  }
  
  public class NBestItem {
    public String hyp;
    public float totalScore;
    
    public NBestItem(String hyp, float score) {
      this.hyp = hyp;
      this.totalScore = score;  
    }
  }
  
  public void addRule(String rule) {
    metadata.add("custom_rule " + rule);
  }

  public String toString() {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    return gson.toJson(this) + "\n";
  }
}
