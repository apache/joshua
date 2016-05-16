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

import java.util.Arrays; 
import java.util.Map; 

import org.apache.joshua.corpus.SymbolTable; 


/**
 * Normally, the feature score in the rule should be *cost* (i.e., 
 * -LogP), so that the feature weight should be positive 
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com> 
 * @version $LastChangedDate: 2010-01-20 19:46:54 -0600 (Wed, 20 Jan 2010) $ 
 */ 
public class BilingualRule extends MonolingualRule { 

  private int[] english; 

  //=============================================================== 
  // Constructors 
  //=============================================================== 

  /**
   * Constructs a new rule using the provided parameters. The 
   * owner and rule id for this rule are undefined. 
   *  
   * @param lhs Left-hand side of the rule. 
   * @param sourceRhs Source language right-hand side of the rule. 
   * @param targetRhs Target language right-hand side of the rule. 
   * @param featureScores Feature value scores for the rule. 
   * @param arity Number of nonterminals in the source language 
   *              right-hand side. 
   * @param owner 
   * @param latticeCost 
   * @param ruleID 
   */ 
  public BilingualRule(int lhs, int[] sourceRhs, int[] targetRhs, float[] featureScores, int arity, int owner, float latticeCost, int ruleID) { 
    super(lhs, sourceRhs, featureScores, arity, owner, latticeCost, ruleID); 
    this.english = targetRhs;   
  } 

  //called by class who does not care about lattice_cost, rule_id, and owner 
  public BilingualRule(int lhs, int[] sourceRhs, int[] targetRhs, float[] featureScores, int arity) { 
    super(lhs, sourceRhs, featureScores, arity); 
    this.english = targetRhs; 
  } 


  //=============================================================== 
  // Attributes 
  //=============================================================== 

  public final void setEnglish(int[] eng) { 
    this.english = eng; 
  } 

  public final int[] getEnglish() { 
    return this.english; 
  } 


  //=============================================================== 
  // Serialization Methods 
  //=============================================================== 
  // TODO: remove these methods 

  // Caching this method significantly improves performance 
  // We mark it transient because it is, though cf java.io.Serializable 
  private transient String cachedToString = null; 

  public String toString(Map<Integer,String> ntVocab, SymbolTable sourceVocab, SymbolTable targetVocab) { 
    if (null == this.cachedToString) { 
      StringBuffer sb = new StringBuffer("["); 
      sb.append(ntVocab.get(this.getLHS())); 
      sb.append("] ||| "); 
      sb.append(sourceVocab.getWords(this.getFrench(),true)); 
      sb.append(" ||| "); 
      sb.append(targetVocab.getWords(this.english,false)); 
      //sb.append(java.util.Arrays.toString(this.english)); 
      sb.append(" |||"); 
      for (int i = 0; i < this.getFeatureScores().length; i++) { 
        //    sb.append(String.format(" %.12f", this.getFeatureScores()[i])); 
        sb.append(' '); 
        sb.append(Float.toString(this.getFeatureScores()[i])); 
      } 
      this.cachedToString = sb.toString(); 
    } 
    return this.cachedToString; 
  } 


  //print the rule in terms of Integers 
  public String toString() { 
    if (null == this.cachedToString) { 
      StringBuffer sb = new StringBuffer(); 
      sb.append(this.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(this))); 
      sb.append("~~~"); 
      sb.append(this.getLHS()); 
      sb.append(" ||| "); 
      sb.append(Arrays.toString(this.getFrench())); 
      sb.append(" ||| "); 
      sb.append(Arrays.toString(this.english)); 
      sb.append(" |||"); 
      for (int i = 0; i < this.getFeatureScores().length; i++) { 
        sb.append(String.format(" %.4f", this.getFeatureScores()[i])); 
      } 
      this.cachedToString = sb.toString(); 
    } 
    return this.cachedToString; 
  } 


  public String toString(SymbolTable symbolTable) { 
    if (null == this.cachedToString) { 
      StringBuffer sb = new StringBuffer(); 
      sb.append(symbolTable.getWord(this.getLHS())); 
      sb.append(" ||| "); 
      sb.append(symbolTable.getWords(this.getFrench())); 
      sb.append(" ||| "); 
      sb.append(symbolTable.getWords(this.english)); 
      sb.append(" |||"); 
      for (int i = 0; i < this.getFeatureScores().length; i++) { 
        sb.append(String.format(" %.4f", this.getFeatureScores()[i])); 
      } 
      this.cachedToString = sb.toString(); 
    } 
    return this.cachedToString; 
  } 

  public String toStringWithoutFeatScores(SymbolTable symbolTable) { 
    StringBuffer sb = new StringBuffer(); 
    if(symbolTable==null) 
      sb.append(this.getLHS()); 
    else 
      sb.append(symbolTable.getWord(this.getLHS())); 

    return sb.append(" ||| ") 
        .append(convertToString(this.getFrench(), symbolTable)) 
        .append(" ||| ") 
        .append(convertToString(this.getEnglish(), symbolTable)) 
        .toString(); 
  } 





}