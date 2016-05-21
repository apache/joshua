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
import java.util.List; 
import java.util.Map; 

import org.apache.joshua.corpus.SymbolTable; 
import org.apache.joshua.decoder.ff.FeatureFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this class implements MonolingualRule 
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com> 
 * @version $LastChangedDate: 2010-02-10 09:59:38 -0600 (Wed, 10 Feb 2010) $ 
 */ 
public class MonolingualRule extends Rule { 

  public static final Logger LOG = LoggerFactory.getLogger(MonolingualRule.class);

  //=============================================================== 
  // Instance Fields 
  //=============================================================== 

  /* The string format of Rule is:
   * [Phrase] ||| french ||| english ||| feature scores 
   */ 
  private int ruleID; 
  private int lhs; // tag of this rule 
  private int[] pFrench; //pointer to the RuleCollection, as all the rules under it share the same Source side 
  private int arity; 
  private float[] featScores; // the feature scores for this rule 

  /* a feature function will be fired for this rule
   * only if the owner of the rule matches the owner of the feature function 
   */ 
  private int owner; 

  // TODO: consider remove this from the general class, and 
  // create a new specific Rule class 
  private float latticeCost;  

  /**
   * estimate_cost depends on rule itself: statelesscost + 
   * transition_cost(non-stateless/non-contexual* models), 
   * we need this variable in order to provide sorting for 
   * cube-pruning 
   */ 
  private float est_cost = 0; 

  //=============================================================== 
  // Static Fields 
  //=============================================================== 

  // TODO: Ideally, we shouldn't have to have dummy rule IDs 
  // and dummy owners. How can this need be eliminated? 
  public static final int DUMMY_RULE_ID = 1; 
  public static final int DUMMY_OWNER = 1; 


  //=============================================================== 
  // Constructors 
  //=============================================================== 

  /**
   * Constructs a new rule using the provided parameters. The 
   * owner and rule id for this rule are undefined. 
   *  
   * @param lhs Left-hand side of the rule. 
   * @param sourceRhs Source language right-hand side of the rule. 
   * @param featureScores Feature value scores for the rule. 
   * @param arity Number of nonterminals in the source language 
   *              right-hand side. 
   * @param owner 
   * @param latticeCost 
   * @param ruleID 
   */ 
  public MonolingualRule(int lhs, int[] sourceRhs, float[] featureScores, int arity, int owner, float latticeCost, int ruleID) { 
    this.lhs          = lhs; 
    this.pFrench     = sourceRhs; 
    this.featScores  = featureScores; 
    this.arity        = arity; 
    this.latticeCost = latticeCost; 
    this.ruleID      = ruleID; 
    this.owner        = owner; 
  } 


  // called by class who does not care about lattice_cost, 
  // rule_id, and owner 
  public MonolingualRule(int lhs_, int[] source_rhs, float[] feature_scores, int arity_) { 
    this.lhs         = lhs_; 
    this.pFrench    = source_rhs; 
    this.featScores = feature_scores; 
    this.arity       = arity_; 

    //==== dummy values 
    this.latticeCost = 0; 
    this.ruleID      = DUMMY_RULE_ID; 
    this.owner        = DUMMY_OWNER; 
  } 


  //=============================================================== 
  // Attributes 
  //=============================================================== 

  public final void setRuleID(int id) { this.ruleID = id; } 

  public final int getRuleID() { return this.ruleID; } 


  public final void setArity(int arity) { this.arity = arity; } 

  public final int getArity() { return this.arity; } 


  public final void setOwner(int owner) { this.owner = owner; } 

  public final int getOwner() { return this.owner; } 


  public final void setLHS(int lhs) { this.lhs = lhs; } 

  public final int getLHS() { return this.lhs; } 


  public void setEnglish(int[] eng) { 
    //TODO: do nothing 
  } 

  public int[] getEnglish() { 
    //TODO 
    return null; 
  } 


  public final void setFrench(int[] french) { this.pFrench = french; } 

  public final int[] getFrench() { return this.pFrench; } 


  public final void setFeatureScores(float[] scores) { 
    this.featScores = scores; 
  } 

  public final float[] getFeatureScores() { 
    return this.featScores; 
  } 


  public final void setLatticeCost(float cost) { this.latticeCost = cost; } 

  public final float getLatticeCost() { return this.latticeCost; } 


  public final float getEstCost() { 
    if (est_cost <= Double.NEGATIVE_INFINITY) { 
      LOG.warn("The est cost is neg infinity; must be bad rule; rule is:\n {}", this);
    } 
    return est_cost; 
  } 


  /** 
   * Set a lower-bound estimate inside the rule returns full 
   * estimate. 
   */ 
  public final float estimateRuleCost(List<FeatureFunction> featureFunctions) { 
    if (null == featureFunctions) { 
      return 0; 
    } else { 
      float estcost = 0.0f; 
      for (FeatureFunction ff : featureFunctions) { 
        double mdcost = - ff.estimateLogP(this, -1) * ff.getWeight(); 
        estcost += mdcost; 
      } 

      this.est_cost = estcost; 
      return estcost; 
    } 
  } 

  //=============================================================== 
  // Methods 
  //=============================================================== 

  public float incrementFeatureScore(int column, double score) { 
    synchronized(this) { 
      featScores[column] += score; 
      return featScores[column]; 
    } 
  } 


  public void setFeatureCost(int column, float score) { 
    synchronized(this) { 
      featScores[column] = score; 
    } 
  } 


  public float getFeatureCost(int column) { 
    synchronized(this) { 
      return featScores[column]; 
    } 
  } 

  //=============================================================== 
  // Serialization Methods 
  //=============================================================== 
  // BUG: These are all far too redundant. Should be refactored to share. 

  // Caching this method significantly improves performance 
  // We mark it transient because it is, though cf 
  // java.io.Serializable 
  private transient String cachedToString = null; 

  @Deprecated 
  public String toString(Map<Integer,String> ntVocab, SymbolTable sourceVocab, SymbolTable targetVocab) { 
    if (null == this.cachedToString) { 
      StringBuffer sb = new StringBuffer(); 
      sb.append(ntVocab.get(this.lhs)); 
      sb.append(" ||| "); 
      sb.append(sourceVocab.getWords(this.pFrench,true)); 
      sb.append(" |||"); 
      for (int i = 0; i < this.featScores.length; i++) { 
        //sb.append(String.format(" %.4f", this.feat_scores[i])); 
        sb.append(' ').append(Float.toString(this.featScores[i])); 
      } 
      this.cachedToString = sb.toString(); 
    } 
    return this.cachedToString; 
  } 


  //print the rule in terms of Ingeters 
  @Deprecated 
  public String toString() { 
    if (null == this.cachedToString) { 
      StringBuffer sb = new StringBuffer(); 
      sb.append(this.lhs); 
      sb.append(" ||| "); 
      sb.append(Arrays.toString(this.pFrench)); 
      sb.append(" |||"); 
      for (int i = 0; i < this.featScores.length; i++) { 
        sb.append(String.format(" %.4f", this.featScores[i])); 
      } 
      this.cachedToString = sb.toString(); 
    } 
    return this.cachedToString; 
  } 


  //do not use cachedToString 
  @Deprecated 
  public String toString(SymbolTable symbolTable) { 
    StringBuffer sb = new StringBuffer(); 
    sb.append(symbolTable.getWord(this.lhs)); 
    sb.append(" ||| "); 
    sb.append(symbolTable.getWords(this.pFrench)); 
    sb.append(" |||"); 
    for (int i = 0; i < this.featScores.length; i++) { 
      sb.append(String.format(" %.4f", this.featScores[i])); 
    } 
    return sb.toString(); 
  } 


  @Deprecated 
  public String toStringWithoutFeatScores(SymbolTable symbolTable) { 
    StringBuffer sb = new StringBuffer(); 
    if(symbolTable==null) 
      sb.append(this.getLHS()); 
    else 
      sb.append(symbolTable.getWord(this.getLHS())); 

    return sb.append(" ||| ") 
        .append(convertToString(this.getFrench(), symbolTable)) 
        .toString(); 
  } 

  public String convertToString(int[] words, SymbolTable symbolTable){   
    StringBuffer sb = new StringBuffer(); 
    for (int i = 0; i < words.length; i++) { 
      if(symbolTable!=null) 
        sb.append( symbolTable.getWord(words[i]) ); 
      else 
        sb.append(words[i]); 

      if(i<words.length-1) 
        sb.append(" "); 
    } 
    return sb.toString(); 
  } 
}