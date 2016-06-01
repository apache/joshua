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
package org.apache.joshua.decoder.ff.lm; 

import org.apache.joshua.decoder.Support; 
import java.util.List; 

/**
 * This class implements NGramLanguageModel by creating wrappers 
 * around the necessary functions to capture common errors. Most 
 * methods are declared final, in an attempt to limit what subclasses 
 * may be defined. 
 * 
 * @author Zhifei Li, zhifei.work@gmail.com
 * @version $LastChangedDate: 2009-12-30 10:10:38 -0600 (Wed, 30 Dec 2009) $ 
 */ 
public abstract class AbstractLM extends DefaultNGramLanguageModel { 

  public AbstractLM(int symbolTable, int order) { 
    super(symbolTable, order); 
  } 

  @SuppressWarnings("null")
  public final double sentenceLogProbability( 
      List<Integer> sentence, int order, int startIndex 
      ) { 
    //return super.sentenceLogProbability(sentence.stream().toArray(int[]::new) , order, startIndex); 
    return (Double) null;
  } 

  public final float ngramLogProbability(int[] ngram) { 
    return super.ngramLogProbability(ngram); 
  } 

  public final float ngramLogProbability(int[] ngram, int order) { 
    if (ngram.length > order) { 
      throw new RuntimeException("ngram length is greather than the max order"); 
    } 
    //  if (ngram.length==1 && "we".equals(symbolTable.getWord(ngram[0]))) { 
    //   System.err.println("Something weird is about to happen"); 
    //  } 

    int historySize = ngram.length - 1; 
    if (historySize >= order || historySize < 0) { 
      // BUG: use logger or exception. Don't zero default 
      throw new RuntimeException("Error: history size is " + historySize); 
      //   return 0; 
    } 
    double probability = ngramLogProbability_helper(ngram, order); 
//    if (probability < -JoshuaConfiguration.lm_ceiling_cost) { 
//      probability = -JoshuaConfiguration.lm_ceiling_cost; 
//    } 
    return (float) probability; 
  } 

  protected abstract float ngramLogProbability_helper(int[] ngram, int order); 

  @Deprecated 
  public final double logProbOfBackoffState(List<Integer> ngram, int order, int qtyAdditionalBackoffWeight) { 
    return logProbabilityOfBackoffState( 
        Support.subIntArray(ngram, 0, ngram.size()), 
        order, qtyAdditionalBackoffWeight); 
  } 


  public final double logProbabilityOfBackoffState(int[] ngram, int order, int qtyAdditionalBackoffWeight) { 
    if (ngram.length > order) { 
      throw new RuntimeException("ngram length is greather than the max order"); 
    } 
    if (ngram[ngram.length-1] != LanguageModelFF.LM_INDEX) { 
      throw new RuntimeException("last wrd is not <bow>"); 
    } 
    if (qtyAdditionalBackoffWeight > 0) { 
      return logProbabilityOfBackoffState_helper( 
          ngram, order, qtyAdditionalBackoffWeight); 
    } else { 
      return 0.0; 
    } 
  } 


  protected abstract double logProbabilityOfBackoffState_helper( 
      int[] ngram, int order, int qtyAdditionalBackoffWeight); 


  // BUG: We should have different classes based on the configuration in use 
  public int[] leftEquivalentState(int[] originalState, int order, 
      double[] cost 
      ) { 
//    if (JoshuaConfiguration.use_left_equivalent_state) 
//      throw new UnsupportedOperationException("getLeftEquivalentState is not overwritten by a concrete class"); 

    return originalState; 
  } 


  // BUG: We should have different classes based on the configuration in use 
  public int[] rightEquivalentState(int[] originalState, int order) { 
//    if ( !JoshuaConfiguration.use_right_equivalent_state 
//        || originalState.length != this.ngramOrder-1) { 
      return originalState; 
//    } else { 
//      throw new UnsupportedOperationException("getRightEquivalentState is not overwritten by a concrete class"); 
//    } 
  } 
}