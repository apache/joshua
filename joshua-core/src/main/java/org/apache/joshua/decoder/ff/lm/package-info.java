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
/**
 * <p>Provides abstraction and support for the language model 
 * feature function typically used in hierarchical phrase-based 
 * decoding for statistical machine translation.</p>
 * <p>The classes contained within this directory are 
 * responsible for two tasks: implementing the feature function, 
 * and representing the language model itself.  The class 
 * `LanguageModelFF` implements the feature function by exending 
 * the class `DefaultStatefulFF`.  One of these is instantiated 
 * for each language model present in the decoder.</p>
 * <p>The language models themselves are implemented as a 
 * combination of an interface (`NGramLanguageModel`), a default 
 * implementation (`DefaultNgramLangaugeModel`), and an abstract
 * implementation of the default (`AbstractLM`).</p>
 *
 * <pre>
 *  DefaultStatefulFF
 *  |- LanguageModelFF
 *
 *  DefaultNgramLanguageModel implements interface NGramLanguageModel
 *  |- AbstractLM
 * </pre>
 */
package org.apache.joshua.decoder.ff.lm;
