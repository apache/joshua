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
 * <p>Provides an implementation of the linear feature functions 
 * typically used in hierarchical phrase-based decoding for 
 * statistical machine translation.</p>
 * <p>The following is a note from Juri describing some of the 
 * functionality of the feature functions interfaces and default 
 * abstract classes.</p>
 * <pre>
 * The equality that I intended for is ff.transitionLogP() =
 * ff.estimateLogP() + ff.reEstimateTransitionLogP(). The re-estimate
 * fixes the estimate to be the true transition cost that takes into
 * account the state. Before decoding the cost of applying a rule is
 * estimated via estimateLogP() and yields the phrasal feature costs plus
 * an LM estimate of the cost of the lexical portions of the rule.
 * transitionLogP() takes rule and state and computes everything from
 * scratch, whereas reEstimateTransitionLogP() adds in the cost of new
 * n-grams that result from combining the rule with the LM states and
 * subtracts out the cost of superfluous less-than-n-grams that were
 * overridden by the updated cost calculation.
 * 
 * Hope this helps.
 * </pre>
 */
package org.apache.joshua.decoder.ff;
