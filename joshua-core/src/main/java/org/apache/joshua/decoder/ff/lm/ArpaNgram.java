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

/**
 * Represents a single n-gram line  
 * from an ARPA language model file. 
 *  
 * @author Lane Schwartz 
 */ 
public class ArpaNgram { 


  /** Indicates an invalid probability value. */ 
  public static final float INVALID_VALUE = Float.NaN; 

  /** Default backoff value. */ 
  public static final float DEFAULT_BACKOFF = 0.0f; 

  private final int word; 
  private final int[] context; 
  private final float value; 
  private final float backoff; 
  // private final int id; 

  public ArpaNgram(int word, int[] context, float value, float backoff) { 
    this.word = word; 
    this.context = context; 
    this.value = value; 
    this.backoff = backoff; 
    //  this.id = id; 
  } 

  // public int getID() { 
  //  return id; 
  // } 

  public int order() { 
    return context.length + 1; 
  } 

  public int getWord() { 
    return word; 
  } 

  public int[] getContext() { 
    return context; 
  } 

  public float getValue() { 
    return value; 
  } 

  public float getBackoff() { 
    return backoff; 
  } 
}