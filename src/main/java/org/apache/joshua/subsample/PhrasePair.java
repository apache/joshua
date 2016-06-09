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
package org.apache.joshua.subsample;

import org.apache.joshua.corpus.Phrase;


/**
 * Phrase-aligned tuple class associating an F phrase, E phrase, and (possibly null)
 * word-alignments. This is primarily for maintaining sentence-alignment.
 * 
 * @author UMD (Jimmy Lin, Chris Dyer, et al.)
 * @author wren ng thornton wren@users.sourceforge.net
 * @version $LastChangedDate$
 */
public class PhrasePair {
  // Making these final requires Java6, not Java5
  private final Phrase f;
  private final Phrase e;
  private final Alignment a;

  // ===============================================================
  // Constructors
  // ===============================================================
  public PhrasePair(Phrase f_, Phrase e_) {
    this(f_, e_, null);
  }

  public PhrasePair(Phrase f, Phrase e, Alignment a) {
    this.f = f;
    this.e = e;
    this.a = a;
  }

  // ===============================================================
  // Attributes
  // ===============================================================
  public Phrase getF() {
    return f;
  }

  public Phrase getE() {
    return e;
  }

  public Alignment getAlignment() {
    return a;
  }

  // ===============================================================
  // Methods
  // ===============================================================
  public float ratioFtoE() {
    return ((float) this.f.size()) / ((float) this.e.size());
  }
}
